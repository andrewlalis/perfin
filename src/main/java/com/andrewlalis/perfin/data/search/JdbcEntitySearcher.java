package com.andrewlalis.perfin.data.search;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.Pair;
import com.andrewlalis.perfin.data.util.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JdbcEntitySearcher<T> implements EntitySearcher<T> {
    private static final Logger logger = LoggerFactory.getLogger(JdbcEntitySearcher.class);

    private final Connection conn;
    private final String countExpression;
    private final String selectExpression;
    private final ResultSetMapper<T> resultSetMapper;

    public JdbcEntitySearcher(Connection conn, String countExpression, String selectExpression, ResultSetMapper<T> resultSetMapper) {
        this.conn = conn;
        this.countExpression = countExpression;
        this.selectExpression = selectExpression;
        this.resultSetMapper = resultSetMapper;
    }

    private Pair<String, List<Pair<Integer, Object>>> buildSearchQuery(List<SearchFilter> filters) {
        if (filters.isEmpty()) return new Pair<>("", Collections.emptyList());
        StringBuilder sb = new StringBuilder();
        List<Pair<Integer, Object>> args = new ArrayList<>();
        for (var filter : filters) {
            args.addAll(filter.args());
            for (var joinClause : filter.joinClauses()) {
                sb.append(joinClause).append('\n');
            }
        }
        sb.append("WHERE\n");
        for (int i = 0; i < filters.size(); i++) {
            sb.append(filters.get(i).whereClause());
            if (i < filters.size() - 1) {
                sb.append(" AND");
            }
            sb.append('\n');
        }
        return new Pair<>(sb.toString(), args);
    }

    private void applyArgs(PreparedStatement stmt, List<Pair<Integer, Object>> args) throws SQLException {
        for (int i = 1; i <= args.size(); i++) {
            Pair<Integer, Object> arg = args.get(i - 1);
            if (arg.second() == null) {
                stmt.setNull(i, arg.first());
            } else {
                stmt.setObject(i, arg.second(), arg.first());
            }
        }
    }

    @Override
    public Page<T> search(PageRequest pageRequest, List<SearchFilter> filters) {
        var baseQueryAndArgs = buildSearchQuery(filters);
        StringBuilder sqlBuilder = new StringBuilder(selectExpression);
        if (baseQueryAndArgs.first() != null && !baseQueryAndArgs.first().isBlank()) {
            sqlBuilder.append('\n').append(baseQueryAndArgs.first());
        }
        String pagingSql = pageRequest.toSQL();
        if (pagingSql != null && !pagingSql.isBlank()) {
            sqlBuilder.append('\n').append(pagingSql);
        }
        String sql = sqlBuilder.toString();
        logger.debug(
                "Searching with query:\n{}\nWith arguments: {}",
                sql,
                baseQueryAndArgs.second().stream()
                        .map(Pair::second)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "))
        );
        try (var stmt = conn.prepareStatement(sql)) {
            applyArgs(stmt, baseQueryAndArgs.second());
            ResultSet rs = stmt.executeQuery();
            List<T> results = new ArrayList<>(pageRequest.size());
            while (rs.next() && results.size() < pageRequest.size()) {
                results.add(resultSetMapper.map(rs));
            }
            return new Page<>(results, pageRequest);
        } catch (SQLException e) {
            logger.error("Search failed.", e);
            return new Page<>(Collections.emptyList(), pageRequest);
        }
    }

    @Override
    public long resultCount(List<SearchFilter> filters) {
        var baseQueryAndArgs = buildSearchQuery(filters);
        String sql = countExpression + "\n" + baseQueryAndArgs.first();
        try (var stmt = conn.prepareStatement(sql)) {
            applyArgs(stmt, baseQueryAndArgs.second());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) throw new SQLException("No count result.");
            return rs.getLong(1);
        } catch (SQLException e) {
            logger.error("Failed to get search result count.", e);
            return 0L;
        }
    }

    public static class Builder {

    }
}
