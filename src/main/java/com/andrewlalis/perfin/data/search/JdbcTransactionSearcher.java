package com.andrewlalis.perfin.data.search;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JdbcTransactionSearcher extends JdbcEntitySearcher<Transaction> {
    public JdbcTransactionSearcher(Connection conn) {
        super(
                conn,
                "SELECT COUNT(transaction.id) FROM transaction",
                "SELECT transaction.* FROM transaction",
                JdbcTransactionSearcher::parseResultSet
        );
    }

    private static Transaction parseResultSet(ResultSet rs) throws SQLException {
        long id = rs.getLong(1);
        LocalDateTime timestamp = DbUtil.utcLDTFromTimestamp(rs.getTimestamp(2));
        BigDecimal amount = rs.getBigDecimal(3);
        Currency currency = Currency.getInstance(rs.getString(4));
        String description = rs.getString(5);
        Long vendorId = rs.getLong(6);
        if (rs.wasNull()) vendorId = null;
        Long categoryId = rs.getLong(7);
        if (rs.wasNull()) categoryId = null;
        return new Transaction(id, timestamp, amount, currency, description, vendorId, categoryId);
    }

    public static class FilterBuilder {
        private final List<SearchFilter> filters = new ArrayList<>();
        private final Set<String> joinTables = new HashSet<>();

        public List<SearchFilter> build() {
            return filters;
        }

        public FilterBuilder byAccounts(Collection<Account> accounts, boolean exclude) {
            if (accounts.isEmpty()) return this;
            var builder = new SearchFilter.Builder();
            addAccountEntryJoin(builder);
            String idsString = accounts.stream()
                    .map(a -> Long.toString(a.id)).distinct()
                    .collect(Collectors.joining(","));
            addInClause(builder, "account_entry.account_id", idsString, exclude);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byAccountTypes(Collection<AccountType> types, boolean exclude) {
            if (types.isEmpty()) return this;
            var builder = new SearchFilter.Builder();
            addAccountJoin(builder);
            String typesString = types.stream()
                    .map(t -> "'" + t.name() + "'").distinct()
                    .collect(Collectors.joining(","));
            addInClause(builder, "account.account_type", typesString, exclude);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byCategories(Collection<TransactionCategory> categories, boolean exclude) {
            if (categories.isEmpty()) return this;
            var builder = new SearchFilter.Builder();
            Set<Long> ids = Profile.getCurrent().dataSource().mapRepo(TransactionCategoryRepository.class, repo -> {
                Set<Long> categoryIds = new HashSet<>();
                for (var category : categories) {
                    var treeNode = repo.findTree(category);
                    categoryIds.addAll(treeNode.allIds());
                }
                return categoryIds;
            });
            String idsString = ids.stream()
                    .map(id -> Long.toString(id)).distinct()
                    .collect(Collectors.joining(","));
            addInClause(builder, "transaction.category_id", idsString, exclude);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byVendors(Collection<TransactionVendor> vendors, boolean exclude) {
             if (vendors.isEmpty()) return this;
             var builder = new SearchFilter.Builder();
             String idsString = vendors.stream()
                     .map(v -> Long.toString(v.id)).distinct()
                     .collect(Collectors.joining(","));
             addInClause(builder, "transaction.vendor_id", idsString, exclude);
             filters.add(builder.build());
             return this;
        }

        public FilterBuilder byTags(Collection<TransactionTag> tags, boolean exclude) {
            if (tags.isEmpty()) return this;
            var builder = new SearchFilter.Builder();
            addTagJoin(builder);
            var tagIdsString = tags.stream()
                    .map(t -> Long.toString(t.id)).distinct()
                    .collect(Collectors.joining(","));
            addInClause(builder, "transaction_tag_join.tag_id", tagIdsString, exclude);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byAmountGreaterThan(BigDecimal amount) {
            var builder = new SearchFilter.Builder();
            builder.where("transaction.amount > ?");
            builder.withArg(Types.NUMERIC, amount);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byAmountLessThan(BigDecimal amount) {
            var builder = new SearchFilter.Builder();
            builder.where("transaction.amount < ?");
            builder.withArg(Types.NUMERIC, amount);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byAmountEqualTo(BigDecimal amount) {
            var builder = new SearchFilter.Builder();
            builder.where("transaction.amount = ?");
            builder.withArg(Types.NUMERIC, amount);
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byEntryType(AccountEntry.Type type) {
            var builder = new SearchFilter.Builder();
            addAccountEntryJoin(builder);
            builder.where("account_entry.type = ?");
            builder.withArg(Types.VARCHAR, type.name());
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byHasAttachments(boolean hasAttachments) {
            var builder = new SearchFilter.Builder();
            String subQuery = "(SELECT COUNT(attachment_id) FROM transaction_attachment WHERE transaction_id = transaction.id)";
            if (hasAttachments) {
                builder.where(subQuery + " > 0");
            } else {
                builder.where(subQuery + " = 0");
            }
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byHasLineItems(boolean hasLineItems) {
            var builder = new SearchFilter.Builder();
            String subQuery = "(SELECT COUNT(id) FROM transaction_line_item WHERE transaction_id = transaction.id)";
            if (hasLineItems) {
                builder.where(subQuery + " > 0");
            } else {
                builder.where(subQuery + " = 0");
            }
            filters.add(builder.build());
            return this;
        }

        public FilterBuilder byCurrencies(Collection<Currency> currencies, boolean exclude) {
            if (currencies.isEmpty()) return this;
            var builder = new SearchFilter.Builder();
            String currenciesString = currencies.stream()
                    .map(c -> "'" + c.getCurrencyCode() + "'").distinct()
                    .collect(Collectors.joining(","));
            addInClause(builder, "transaction.currency", currenciesString, exclude);
            filters.add(builder.build());
            return this;
        }

        private void addAccountEntryJoin(SearchFilter.Builder builder) {
            if (!joinTables.contains("account_entry")) {
                builder.withJoin("LEFT JOIN account_entry ON account_entry.transaction_id = transaction.id");
                joinTables.add("account_entry");
            }
        }

        private void addAccountJoin(SearchFilter.Builder builder) {
            addAccountEntryJoin(builder);
            if (!joinTables.contains("account")) {
                builder.withJoin("LEFT JOIN account ON account.id = account_entry.account_id");
                joinTables.add("account");
            }
        }

        private void addCategoryJoin(SearchFilter.Builder builder) {
            if (!joinTables.contains("transaction_category")) {
                builder.withJoin("LEFT JOIN transaction_category ON transaction_category.id = transaction.category_id");
                joinTables.add("transaction_category");
            }
        }

        private void addTagJoin(SearchFilter.Builder builder) {
            if (!joinTables.contains("transaction_tag_join")) {
                builder.withJoin("LEFT JOIN transaction_tag_join ON transaction_tag_join.transaction_id = transaction.id");
                joinTables.add("transaction_tag_join");
            }
        }

        private void addInClause(SearchFilter.Builder builder, String valueExpr, String inExpr, boolean exclude) {
            if (exclude) {
                builder.where(valueExpr + " NOT IN (" + inExpr + ")");
            } else {
                builder.where(valueExpr + " IN (" + inExpr + ")");
            }
        }
    }
}
