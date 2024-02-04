package com.andrewlalis.perfin.data.util;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;

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

    public static void setArgs(PreparedStatement stmt, Object... args) {
        setArgs(stmt, List.of(args));
    }

    public static long getGeneratedId(PreparedStatement stmt) {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("No generated keys available.");
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
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

    public static <T> Page<T> findAll(Connection conn, String query, PageRequest pagination, List<Object> args, ResultSetMapper<T> mapper) {
        List<T> items = findAll(conn, query + ' ' + pagination.toSQL(), args, mapper);
        return new Page<>(items, pagination);
    }

    public static <T> Page<T> findAll(Connection conn, String query, PageRequest pagination, ResultSetMapper<T> mapper) {
        return findAll(conn, query, pagination, Collections.emptyList(), mapper);
    }

    public static long count(Connection conn, String query, Object... args) {
        try (var stmt = conn.prepareStatement(query)) {
            setArgs(stmt, args);
            var rs = stmt.executeQuery();
            if (!rs.next()) throw new UncheckedSqlException("No count result available.");
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
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

    public static int update(Connection conn, String query, List<Object> args) {
        try (var stmt = conn.prepareStatement(query)) {
            setArgs(stmt, args);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static int update(Connection conn, String query, Object... args) {
        return update(conn, query, List.of(args));
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

    public static void updateOne(Connection conn, String query, Object... args) {
        updateOne(conn, query, List.of(args));
    }

    public static long insertOne(Connection conn, String query, List<Object> args) {
        try (var stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            setArgs(stmt, args);
            int result = stmt.executeUpdate();
            if (result != 1) throw new UncheckedSqlException("Insert query did not update 1 row.");
            return getGeneratedId(stmt);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    public static long insertOne(Connection conn, String query, Object... args) {
        return insertOne(conn, query, List.of(args));
    }

    public static Timestamp timestampFromUtcLDT(LocalDateTime utc) {
        return Timestamp.from(utc.toInstant(ZoneOffset.UTC));
    }

    public static Timestamp timestampFromUtcNow() {
        return Timestamp.from(Instant.now(Clock.systemUTC()));
    }

    public static Timestamp timestampFromInstant(Instant i) {
        return Timestamp.from(i);
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

    public static <T> T doTransaction(Connection conn, SQLSupplier<T> supplier) {
        try {
            conn.setAutoCommit(false);
            T result = supplier.offer();
            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException se) {
                System.err.println("ERROR: Failed to rollback after a failed transaction!");
                se.printStackTrace(System.err);
                throw new UncheckedSqlException(se);
            }
            throw new RuntimeException(e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("ERROR: Failed to set auto-commit to true after transaction!");
                e.printStackTrace(System.err);
            }
        }
    }

    public static void doTransaction(Connection conn, SQLRunnable runnable) {
        doTransaction(conn, () -> {
            runnable.run();
            return null;
        });
    }
}
