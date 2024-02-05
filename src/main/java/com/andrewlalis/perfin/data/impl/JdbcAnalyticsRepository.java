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
                WHERE transaction.currency = ? AND ae.type = 'CREDIT' AND transaction.timestamp >= ? AND transaction.timestamp <= ?
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
        return DbUtil.findAll(
                conn,
                """
                SELECT
                    SUM(transaction.amount) AS total,
                    tc.id, tc.parent_id, tc.name, tc.color
                FROM transaction
                LEFT JOIN transaction_category tc ON tc.id = transaction.category_id
                LEFT JOIN account_entry ae ON ae.transaction_id = transaction.id
                WHERE transaction.currency = ? AND ae.type = ? AND transaction.timestamp >= ? AND transaction.timestamp <= ?
                GROUP BY tc.id
                ORDER BY total DESC;""",
                List.of(currency.getCurrencyCode(), type.name(), range.start(), range.end()),
                rs -> {
                    BigDecimal total = rs.getBigDecimal(1);
                    TransactionCategory category = null;
                    long categoryId = rs.getLong(2);
                    if (!rs.wasNull()) {
                        Long parentId = rs.getLong(3);
                        if (rs.wasNull()) parentId = null;
                        String name = rs.getString(4);
                        Color color = Color.valueOf("#" + rs.getString(5));
                        category = new TransactionCategory(categoryId, parentId, name, color);
                    }
                    return new Pair<>(category, total);
                }
        );
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
}
