package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AnalyticsRepository;
import com.andrewlalis.perfin.data.TimestampRange;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.Pair;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.TransactionCategory;
import com.andrewlalis.perfin.model.TransactionVendor;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public record JdbcAnalyticsRepository(Connection conn) implements AnalyticsRepository {
    @Override
    public List<Pair<TransactionCategory, BigDecimal>> getSpendByCategory(TimestampRange range, Currency currency) {
        return getTransactionAmountByCategoryAndType(range, currency, AccountEntry.Type.CREDIT);
    }

    @Override
    public List<Pair<TransactionCategory, BigDecimal>> getSpendByRootCategory(TimestampRange range, Currency currency) {
        return groupByRootCategory(getSpendByCategory(range, currency));
    }

    @Override
    public List<Pair<TransactionCategory, BigDecimal>> getIncomeByCategory(TimestampRange range, Currency currency) {
        return getTransactionAmountByCategoryAndType(range, currency, AccountEntry.Type.DEBIT);
    }

    @Override
    public List<Pair<TransactionCategory, BigDecimal>> getIncomeByRootCategory(TimestampRange range, Currency currency) {
        return groupByRootCategory(getIncomeByCategory(range, currency));
    }

    @Override
    public List<Pair<TransactionVendor, BigDecimal>> getSpendByVendor(TimestampRange range, Currency currency) {
        return DbUtil.findAll(
                conn,
                """
                SELECT
                    SUM(transaction.amount) AS total,
                    tv.id, tv.name, tv.description
                FROM transaction
                LEFT JOIN transaction_vendor tv ON tv.id = transaction.vendor_id
                LEFT JOIN account_entry ae ON ae.transaction_id = transaction.id
                WHERE
                    transaction.currency = ? AND
                    transaction.timestamp >= ? AND
                    transaction.timestamp <= ? AND
                    ae.type = 'CREDIT' AND
                    '!exclude' NOT IN (
                        SELECT tt.name
                        FROM transaction_tag tt
                        LEFT JOIN transaction_tag_join ttj ON tt.id = ttj.tag_id
                        WHERE ttj.transaction_id = transaction.id
                    )
                GROUP BY tv.id
                ORDER BY total DESC""",
                List.of(currency.getCurrencyCode(), range.start(), range.end()),
                rs -> {
                    BigDecimal total = rs.getBigDecimal(1);
                    long vendorId = rs.getLong(2);
                    if (rs.wasNull()) return new Pair<>(null, total);
                    String name = rs.getString(3);
                    String description = rs.getString(4);
                    return new Pair<>(new TransactionVendor(vendorId, name, description), total);
                }
        );
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    private List<Pair<TransactionCategory, BigDecimal>> getTransactionAmountByCategoryAndType(TimestampRange range, Currency currency, AccountEntry.Type type) {
        // First find totals for each category, using only transactions without any line items (should be most).
        List<Pair<TransactionCategory, BigDecimal>> totalsBeforeLineItems = DbUtil.findAll(
                conn,
                """
                SELECT
                    SUM(transaction.amount) AS total,
                    tc.id, tc.parent_id, tc.name, tc.color
                FROM transaction
                LEFT JOIN transaction_category tc ON tc.id = transaction.category_id
                LEFT JOIN account_entry ae ON ae.transaction_id = transaction.id
                WHERE
                    transaction.currency = ? AND
                    ae.type = ? AND
                    transaction.timestamp >= ? AND
                    transaction.timestamp <= ? AND
                    '!exclude' NOT IN (
                        SELECT tt.name
                        FROM transaction_tag tt
                        LEFT JOIN transaction_tag_join ttj ON tt.id = ttj.tag_id
                        WHERE ttj.transaction_id = transaction.id
                    ) AND
                    (
                        SELECT COUNT(tli.id) = 0
                        FROM transaction_line_item tli
                        WHERE tli.transaction_id = transaction.id
                    )
                GROUP BY tc.id
                ORDER BY total DESC;""",
                List.of(currency.getCurrencyCode(), type.name(), range.start(), range.end()),
                this::parseAmountAndCategory
        );
        // Then augment the data for any transactions which do have line items.
        List<Pair<TransactionCategory, BigDecimal>> totalsFromLineItemsOnly = DbUtil.findAll(
                conn,
                """
                    SELECT SUM(tli.value_per_item * tli.quantity) AS s, tc.*
                    FROM transaction_line_item tli
                    LEFT JOIN transaction_category tc ON tc.id = tli.category_id
                    LEFT JOIN transaction t ON t.id = tli.transaction_id
                    LEFT JOIN account_entry ae ON ae.transaction_id = t.id
                    WHERE
                        t.currency = ? AND
                        ae.type = ? AND
                        t.timestamp >= ? AND
                        t.timestamp <= ? AND
                        '!exclude' NOT IN (
                            SELECT tt.name
                            FROM transaction_tag tt
                            LEFT JOIN transaction_tag_join ttj ON tt.id = ttj.tag_id
                            WHERE ttj.transaction_id = t.id
                        )
                    GROUP BY tli.category_id
                    ORDER BY s DESC""",
                List.of(currency.getCurrencyCode(), type.name(), range.start(), range.end()),
                this::parseAmountAndCategory
        );
        // Finally add data for any remaining value in transactions with line items, which wasn't accounted for in line items.
        List<Pair<TransactionCategory, BigDecimal>> totalsFromLeftoverTransactions = DbUtil.findAll(
                conn,
                """
                SELECT SUM(s), c_id, c_parent_id, c_name, c_color
                FROM (
                    SELECT transaction.amount - SUM(tli.value_per_item * tli.quantity) AS s,
                           tc.id AS c_id, tc.parent_id AS c_parent_id, tc.name AS c_name, tc.color AS c_color
                    FROM transaction
                    LEFT JOIN transaction_line_item tli ON tli.transaction_id = transaction.id
                    LEFT JOIN transaction_category tc ON tc.id = transaction.category_id
                    LEFT JOIN account_entry ae ON ae.transaction_id = transaction.id
                    WHERE
                        transaction.currency = ? AND
                        ae.type = ? AND
                        transaction.timestamp >= ? AND
                        transaction.timestamp <= ? AND
                        '!exclude' NOT IN (
                            SELECT tt.name
                            FROM transaction_tag tt
                            LEFT JOIN transaction_tag_join ttj ON tt.id = ttj.tag_id
                            WHERE ttj.transaction_id = transaction.id
                        ) AND
                        (
                            SELECT COUNT(tli.id) > 0
                            FROM transaction_line_item tli
                            WHERE tli.transaction_id = transaction.id
                        )
                    GROUP BY transaction.id
                )
                GROUP BY c_id""",
                List.of(currency.getCurrencyCode(), type.name(), range.start(), range.end()),
                this::parseAmountAndCategory
        );
        return combineCategorizedAmounts(List.of(
                totalsBeforeLineItems,
                totalsFromLineItemsOnly,
                totalsFromLeftoverTransactions
        ));
    }

    private List<Pair<TransactionCategory, BigDecimal>> groupByRootCategory(List<Pair<TransactionCategory, BigDecimal>> spendByCategory) {
        List<Pair<TransactionCategory, BigDecimal>> result = new ArrayList<>();
        Map<TransactionCategory, BigDecimal> rootCategorySpend = new HashMap<>();
        var categoryRepo = new JdbcTransactionCategoryRepository(conn);
        BigDecimal uncategorizedSpend = BigDecimal.ZERO;
        for (var spend : spendByCategory) {
            if (spend.first() == null) {
                uncategorizedSpend = uncategorizedSpend.add(spend.second());
            } else {
                TransactionCategory rootCategory = categoryRepo.findRoot(spend.first().id);
                if (rootCategory != null) {
                    BigDecimal categoryTotal = rootCategorySpend.getOrDefault(rootCategory, BigDecimal.ZERO);
                    rootCategorySpend.put(rootCategory, categoryTotal.add(spend.second()));
                }
            }
        }
        for (var entry : rootCategorySpend.entrySet()) {
            result.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        if (uncategorizedSpend.compareTo(BigDecimal.ZERO) > 0) {
            result.add(new Pair<>(null, uncategorizedSpend));
        }
        result.sort((p1, p2) -> p2.second().compareTo(p1.second()));
        return result;
    }

    private Pair<TransactionCategory, BigDecimal> parseAmountAndCategory(ResultSet rs) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(1);
        long categoryId = rs.getLong(2);
        if (rs.wasNull()) {
            return new Pair<>(null, amount);
        }
        Long parentId = rs.getLong(3);
        if (rs.wasNull()) parentId = null;
        String name = rs.getString(4);
        Color color = Color.valueOf("#" + rs.getString(5));
        return new Pair<>(new TransactionCategory(categoryId, parentId, name, color), amount);
    }

    private List<Pair<TransactionCategory, BigDecimal>> combineCategorizedAmounts(List<List<Pair<TransactionCategory, BigDecimal>>> lists) {
        BigDecimal uncategorizedAmount = BigDecimal.ZERO;
        Map<TransactionCategory, BigDecimal> categorizedAmounts = new HashMap<>();
        for (var list : lists) {
            for (var p : list) {
                if (p.first() == null) {
                    uncategorizedAmount = uncategorizedAmount.add(p.second());
                } else {
                    BigDecimal value = categorizedAmounts.computeIfAbsent(p.first(), category -> BigDecimal.ZERO);
                    categorizedAmounts.put(p.first(), value.add(p.second()));
                }
            }
        }
        List<Pair<TransactionCategory, BigDecimal>> amountsByCategory = new ArrayList<>();
        amountsByCategory.add(new Pair<>(null, uncategorizedAmount));
        for (var entry : categorizedAmounts.entrySet()) {
            amountsByCategory.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        amountsByCategory.sort((p1, p2) -> p2.second().compareTo(p1.second()));
        return amountsByCategory;
    }
}
