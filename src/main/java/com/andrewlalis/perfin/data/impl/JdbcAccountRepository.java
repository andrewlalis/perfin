package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.*;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.BalanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public record JdbcAccountRepository(Connection conn, Path contentDir) implements AccountRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcAccountRepository.class);

    @Override
    public long insert(AccountType type, String accountNumber, String name, Currency currency) {
        return DbUtil.doTransaction(conn, () -> {
            long accountId = DbUtil.insertOne(
                    conn,
                    "INSERT INTO account (created_at, account_type, account_number, name, currency) VALUES (?, ?, ?, ?, ?)",
                    List.of(
                            DbUtil.timestampFromUtcNow(),
                            type.name(),
                            accountNumber,
                            name,
                            currency.getCurrencyCode()
                    )
            );
            // Insert a history item indicating the creation of the account.
            HistoryRepository historyRepo = new JdbcHistoryRepository(conn);
            long historyId = historyRepo.getOrCreateHistoryForAccount(accountId);
            historyRepo.addTextItem(historyId, "Account added to your Perfin profile.");
            return accountId;
        });
    }

    @Override
    public Page<Account> findAll(PageRequest pagination) {
        return DbUtil.findAll(conn, "SELECT * FROM account WHERE NOT archived", pagination, JdbcAccountRepository::parseAccount);
    }

    @Override
    public List<Account> findAllOrderedByRecentHistory() {
        return DbUtil.findAll(
                conn,
                """
                SELECT DISTINCT ON (account.id) account.*, hi.timestamp AS _
                FROM account
                LEFT OUTER JOIN history_account ha ON ha.account_id = account.id
                LEFT OUTER JOIN history_item hi ON hi.history_id = ha.history_id
                WHERE NOT account.archived
                ORDER BY hi.timestamp DESC, account.created_at DESC""",
                JdbcAccountRepository::parseAccount
        );
    }

    @Override
    public List<Account> findAllByCurrency(Currency currency) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM account WHERE currency = ? AND NOT archived ORDER BY created_at",
                List.of(currency.getCurrencyCode()),
                JdbcAccountRepository::parseAccount
        );
    }

    @Override
    public Optional<Account> findById(long id) {
        return DbUtil.findById(conn, "SELECT * FROM account WHERE id = ?", id, JdbcAccountRepository::parseAccount);
    }

    @Override
    public void updateName(long id, String name) {
        DbUtil.updateOne(conn, "UPDATE account SET name = ? WHERE id = ? AND NOT archived", List.of(name, id));
    }

    @Override
    public BigDecimal deriveBalance(long accountId, Instant timestamp) {
        // First find the account itself, since its properties influence the balance.
        Account account = findById(accountId).orElse(null);
        if (account == null) throw new EntityNotFoundException(Account.class, accountId);
        LocalDateTime utcTimestamp = timestamp.atZone(ZoneOffset.UTC).toLocalDateTime();
        BalanceRecordRepository balanceRecordRepo = new JdbcBalanceRecordRepository(conn, contentDir);
        AccountEntryRepository accountEntryRepo = new JdbcAccountEntryRepository(conn);
        // Find the most recent balance record before timestamp.
        Optional<BalanceRecord> closestPastRecord = balanceRecordRepo.findClosestBefore(account.id, utcTimestamp);
        if (closestPastRecord.isPresent()) {
            // Then find any entries on the account since that balance record and the timestamp.
            List<AccountEntry> entriesBetweenRecentRecordAndNow = accountEntryRepo.findAllByAccountIdBetween(
                    account.id,
                    closestPastRecord.get().getTimestamp(),
                    utcTimestamp
            );
            return computeBalanceWithEntries(account.getType(), closestPastRecord.get(), entriesBetweenRecentRecordAndNow);
        } else {
            // There is no balance record present before the given timestamp. Try and find the closest one after.
            Optional<BalanceRecord> closestFutureRecord = balanceRecordRepo.findClosestAfter(account.id, utcTimestamp);
            if (closestFutureRecord.isPresent()) {
                // Now find any entries on the account from the timestamp until that balance record.
                List<AccountEntry> entriesBetweenNowAndFutureRecord = accountEntryRepo.findAllByAccountIdBetween(
                        account.id,
                        utcTimestamp,
                        closestFutureRecord.get().getTimestamp()
                );
                return computeBalanceWithEntries(account.getType(), closestFutureRecord.get(), entriesBetweenNowAndFutureRecord);
            } else {
                // No balance records exist for the account! Assume balance of 0 when the account was created.
                log.warn("No balance record exists for account {}! Assuming balance was 0 at account creation.", account.getShortName());
                BalanceRecord placeholder = new BalanceRecord(-1, account.getCreatedAt(), account.id, BigDecimal.ZERO, account.getCurrency());
                List<AccountEntry> entriesSinceAccountCreated = accountEntryRepo.findAllByAccountIdBetween(account.id, account.getCreatedAt(), utcTimestamp);
                return computeBalanceWithEntries(account.getType(), placeholder, entriesSinceAccountCreated);
            }
        }
    }

    @Override
    public Set<Currency> findAllUsedCurrencies() {
        return new HashSet<>(DbUtil.findAll(
                conn,
                "SELECT currency FROM account WHERE NOT archived ORDER BY currency ASC",
                rs -> Currency.getInstance(rs.getString(1))
        ));
    }

    @Override
    public void update(Account account) {
        DbUtil.updateOne(
                conn,
                "UPDATE account SET name = ?, account_number = ?, currency = ?, account_type = ? WHERE id = ?",
                List.of(
                        account.getName(),
                        account.getAccountNumber(),
                        account.getCurrency().getCurrencyCode(),
                        account.getType().name(),
                        account.id
                )
        );
    }

    @Override
    public void delete(Account account) {
        DbUtil.updateOne(conn, "DELETE FROM account WHERE id = ?", List.of(account.id));
    }

    @Override
    public void archive(long accountId) {
        DbUtil.doTransaction(conn, () -> {
            DbUtil.updateOne(conn, "UPDATE account SET archived = TRUE WHERE id = ?", List.of(accountId));
            HistoryRepository historyRepo = new JdbcHistoryRepository(conn);
            long historyId = historyRepo.getOrCreateHistoryForAccount(accountId);
            historyRepo.addTextItem(historyId, "Account has been archived.");
        });
    }

    @Override
    public void unarchive(long accountId) {
        DbUtil.doTransaction(conn, () -> {
            DbUtil.updateOne(conn, "UPDATE account SET archived = FALSE WHERE id = ?", List.of(accountId));
            HistoryRepository historyRepo = new JdbcHistoryRepository(conn);
            long historyId = historyRepo.getOrCreateHistoryForAccount(accountId);
            historyRepo.addTextItem(historyId, "Account has been unarchived.");
        });
    }

    public static Account parseAccount(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        LocalDateTime createdAt = DbUtil.utcLDTFromTimestamp(rs.getTimestamp("created_at"));
        boolean archived = rs.getBoolean("archived");
        AccountType type = AccountType.valueOf(rs.getString("account_type").toUpperCase());
        String accountNumber = rs.getString("account_number");
        String name = rs.getString("name");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new Account(id, createdAt, archived, type, accountNumber, name, currency);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    private BigDecimal computeBalanceWithEntries(AccountType accountType, BalanceRecord balanceRecord, List<AccountEntry> entries) {
        List<AccountEntry> entriesBeforeRecord = entries.stream()
                .filter(entry -> entry.getTimestamp().isBefore(balanceRecord.getTimestamp()))
                .toList();
        List<AccountEntry> entriesAfterRecord = entries.stream()
                .filter(entry -> entry.getTimestamp().isAfter(balanceRecord.getTimestamp()))
                .toList();
        BigDecimal balance = balanceRecord.getBalance();
        for (AccountEntry entry : entriesBeforeRecord) {
            balance = balance.subtract(entry.getEffectiveValue(accountType));
        }
        for (AccountEntry entry : entriesAfterRecord) {
            balance = balance.add(entry.getEffectiveValue(accountType));
        }
        return balance;
    }
}
