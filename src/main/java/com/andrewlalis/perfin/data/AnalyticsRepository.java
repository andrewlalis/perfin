package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.util.Pair;
import com.andrewlalis.perfin.model.TransactionCategory;
import com.andrewlalis.perfin.model.TransactionVendor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

public interface AnalyticsRepository extends Repository, AutoCloseable {
    List<Pair<TransactionCategory, BigDecimal>> getSpendByCategory(TimestampRange range, Currency currency);
    List<Pair<TransactionCategory, BigDecimal>> getSpendByRootCategory(TimestampRange range, Currency currency);
    List<Pair<TransactionCategory, BigDecimal>> getIncomeByCategory(TimestampRange range, Currency currency);
    List<Pair<TransactionCategory, BigDecimal>> getIncomeByRootCategory(TimestampRange range, Currency currency);
    List<Pair<TransactionVendor, BigDecimal>> getSpendByVendor(TimestampRange range, Currency currency);
}
