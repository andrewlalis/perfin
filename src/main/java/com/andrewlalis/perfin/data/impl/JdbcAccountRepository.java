package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.*;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.*;
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
    public List<Account> findTopNOrderedByRecentHistory(int n) {
        return DbUtil.findAll(
                conn,
                """
                SELECT DISTINCT ON (account.id) account.*, hi.timestamp AS _
                FROM account
                LEFT OUTER JOIN history_account ha ON ha.account_id = account.id
                LEFT OUTER JOIN history_item hi ON hi.history_id = ha.history_id
                WHERE NOT account.archived
                ORDER BY hi.timestamp DESC, account.created_at DESC
                LIMIT\s""" + n,
                JdbcAccountRepository::parseAccount
        );
    }

    @Override
    public List<Account> findTopNRecentlyActive(int n, int daysSinceLastActive) {
        LocalDateTime cutoff = DateUtil.nowAsUTC().minusDays(daysSinceLastActive);
        return DbUtil.findAll(
                conn,
                """
                SELECT DISTINCT ON (account.id) account.*, hi.timestamp AS _
                FROM account
                LEFT OUTER JOIN history_account ha ON ha.account_id = account.id
                LEFT OUTER JOIN history_item hi ON hi.history_id = ha.history_id
                WHERE NOT account.archived AND hi.timestamp >= ?
                ORDER BY hi.timestamp DESC, account.created_at DESC
                LIMIT\s""" + n,
                List.of(DbUtil.timestampFromUtcLDT(cutoff)),
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
    public List<Timestamped> findEventsBefore(long accountId, LocalDateTime utcTimestamp, int maxResults) {
        var entryRepo = new JdbcAccountEntryRepository(conn);
        var historyRepo = new JdbcHistoryRepository(conn);
        var balanceRecordRepo = new JdbcBalanceRecordRepository(conn, contentDir);
        String query = """
                SELECT id, type
                FROM (
                    SELECT id, timestamp, 'ACCOUNT_ENTRY' AS type, account_id
                    FROM account_entry
                        UNION ALL
                    SELECT id, timestamp, 'HISTORY_ITEM' AS type, account_id
                    FROM history_item
                    LEFT JOIN history_account ha ON history_item.history_id = ha.history_id
                        UNION ALL
                    SELECT id, timestamp, 'BALANCE_RECORD' AS type, account_id
                    FROM balance_record
                )
                WHERE account_id = ? AND timestamp <= ?
                ORDER BY timestamp DESC
                LIMIT\s""" + maxResults;
        try (var stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, accountId);
            stmt.setTimestamp(2, DbUtil.timestampFromUtcLDT(utcTimestamp));
            ResultSet rs = stmt.executeQuery();
            List<Timestamped> entities = new ArrayList<>();
            while (rs.next()) {
                long id = rs.getLong(1);
                String type = rs.getString(2);
                Timestamped entity = switch (type) {
                    case "HISTORY_ITEM" -> historyRepo.getItem(id).orElse(null);
                    case "ACCOUNT_ENTRY" -> entryRepo.findById(id).orElse(null);
                    case "BALANCE_RECORD" -> balanceRecordRepo.findById(id).orElse(null);
                    default -> null;
                };
                if (entity == null) {
                    log.warn("Failed to find entity with id {} and type {}.", id, type);
                } else {
                    entities.add(entity);
                }
            }
            return entities;
        } catch (SQLException e) {
            log.error("Failed to find account events.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void update(long accountId, AccountType type, String accountNumber, String name, Currency currency) {
        DbUtil.doTransaction(conn, () -> {
            Account account = findById(accountId).orElse(null);
            if (account == null) return;
            List<String> updateMessages = new ArrayList<>();
            if (account.getType() != type) {
                DbUtil.updateOne(conn, "UPDATE account SET account_type = ? WHERE id = ?", type, accountId);
                updateMessages.add(String.format("Updated account type from %s to %s.", account.getType().toString(), type.toString()));
            }
            if (!account.getAccountNumber().equals(accountNumber)) {
                DbUtil.updateOne(conn, "UPDATE account SET account_number = ? WHERE id = ?", accountNumber, accountId);
                updateMessages.add(String.format("Updated account number from %s to %s.", account.getAccountNumber(), accountNumber));
            }
            if (!account.getName().equals(name)) {
                DbUtil.updateOne(conn, "UPDATE account SET name = ? WHERE id = ?", name, accountId);
                updateMessages.add(String.format("Updated account name from \"%s\" to \"%s\".", account.getName(), name));
            }
            if (account.getCurrency() != currency) {
                DbUtil.updateOne(conn, "UPDATE account SET currency = ? WHERE id = ?", currency.getCurrencyCode(), accountId);
                updateMessages.add(String.format("Updated account currency from %s to %s.", account.getCurrency(), currency));
            }
            if (!updateMessages.isEmpty()) {
                var historyRepo = new JdbcHistoryRepository(conn);
                long historyId = historyRepo.getOrCreateHistoryForAccount(accountId);
                historyRepo.addTextItem(historyId, String.join("\n", updateMessages));
            }
        });
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
