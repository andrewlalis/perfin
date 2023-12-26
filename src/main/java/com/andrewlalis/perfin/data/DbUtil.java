package com.andrewlalis.perfin.data;

import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class DbUtil {
    private DbUtil() {}

    public static void setArgs(PreparedStatement stmt, List<Object> args) {
        for (int i = 0; i < args.size(); i++) {
            try {
                stmt.setObject(i + 1, args.get(i));
            } catch (SQLException e) {
                throw new UncheckedSqlException("Failed to set parameter " + (i + 1) + " to " + args.get(i), e);
            }
        }
    }

    public static <T> List<T> findAll(Connection conn, String query, List<Object> args, ResultSetMapper<T> mapper) {
        try (var stmt = conn.prepareStatement(query)) {
            setArgs(stmt, args);
            var rs = stmt.executeQuery();
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static <T> List<T> findAll(Connection conn, String query, ResultSetMapper<T> mapper) {
        return findAll(conn, query, Collections.emptyList(), mapper);
    }

    public static <T> Optional<T> findOne(Connection conn, String query, List<Object> args, ResultSetMapper<T> mapper) {
        try (var stmt = conn.prepareStatement(query)) {
            setArgs(stmt, args);
            var rs = stmt.executeQuery();
            if (!rs.next()) return Optional.empty();
            return Optional.of(mapper.map(rs));
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static <T> Optional<T> findById(Connection conn, String query, long id, ResultSetMapper<T> mapper) {
        return findOne(conn, query, List.of(id), mapper);
    }

    public static void updateOne(Connection conn, String query, List<Object> args) {
        try (var stmt = conn.prepareStatement(query)) {
            setArgs(stmt, args);
            int updateCount = stmt.executeUpdate();
            if (updateCount != 1) throw new UncheckedSqlException("Update count is " + updateCount + "; expected 1.");
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static long insertOne(Connection conn, String query, List<Object> args) {
        try (var stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            setArgs(stmt, args);
            int result = stmt.executeUpdate();
            if (result != 1) throw new UncheckedSqlException("Insert query did not update 1 row.");
            var rs = stmt.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static Timestamp timestampFromUtcLDT(LocalDateTime utc) {
        return Timestamp.from(utc.toInstant(ZoneOffset.UTC));
    }

    public static Timestamp timestampFromUtcNow() {
        return Timestamp.from(Instant.now(Clock.systemUTC()));
    }

    public static LocalDateTime utcLDTFromTimestamp(Timestamp ts) {
        return ts.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public static <T extends AutoCloseable> void useClosable(Supplier<T> supplier, ThrowableConsumer<T> consumer) {
        try (T t = supplier.get()) {
            consumer.accept(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
