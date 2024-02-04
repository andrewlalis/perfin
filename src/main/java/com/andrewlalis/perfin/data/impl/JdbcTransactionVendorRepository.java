package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.TransactionVendor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record JdbcTransactionVendorRepository(Connection conn) implements TransactionVendorRepository {
    @Override
    public Optional<TransactionVendor> findById(long id) {
        return DbUtil.findById(
                conn,
                "SELECT * FROM transaction_vendor WHERE id = ?",
                id,
                JdbcTransactionVendorRepository::parseVendor
        );
    }

    @Override
    public Optional<TransactionVendor> findByName(String name) {
        return DbUtil.findOne(
                conn,
                "SELECT * FROM transaction_vendor WHERE name = ?",
                List.of(name),
                JdbcTransactionVendorRepository::parseVendor
        );
    }

    @Override
    public List<TransactionVendor> findAll() {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_vendor ORDER BY name ASC",
                JdbcTransactionVendorRepository::parseVendor
        );
    }

    @Override
    public long insert(String name, String description) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_vendor (name, description) VALUES (?, ?)",
                List.of(name, description)
        );
    }

    @Override
    public long insert(String name) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_vendor (name) VALUES (?)",
                List.of(name)
        );
    }

    @Override
    public void update(long id, String name, String description) {
        DbUtil.doTransaction(conn, () -> {
            TransactionVendor vendor = findById(id).orElseThrow();
            if (!vendor.getName().equals(name)) {
                DbUtil.updateOne(
                        conn,
                        "UPDATE transaction_vendor SET name = ? WHERE id = ?",
                        name,
                        id
                );
            }
            if (!Objects.equals(vendor.getDescription(), description)) {
                DbUtil.updateOne(
                        conn,
                        "UPDATE transaction_vendor SET description = ? WHERE id = ?",
                        description,
                        id
                );
            }
        });
    }

    @Override
    public void deleteById(long id) {
        DbUtil.update(conn, "DELETE FROM transaction_vendor WHERE id = ?", List.of(id));
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static TransactionVendor parseVendor(ResultSet rs) throws SQLException {
        return new TransactionVendor(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
