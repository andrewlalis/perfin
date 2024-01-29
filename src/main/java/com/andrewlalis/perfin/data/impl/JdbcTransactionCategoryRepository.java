package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.util.ColorUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public record JdbcTransactionCategoryRepository(Connection conn) implements TransactionCategoryRepository {
    @Override
    public Optional<TransactionCategory> findById(long id) {
        return DbUtil.findById(
                conn,
                "SELECT * FROM transaction_category WHERE id = ?",
                id,
                JdbcTransactionCategoryRepository::parseCategory
        );
    }

    @Override
    public Optional<TransactionCategory> findByName(String name) {
        return DbUtil.findOne(
                conn,
                "SELECT * FROM transaction_category WHERE name = ?",
                List.of(name),
                JdbcTransactionCategoryRepository::parseCategory
        );
    }

    @Override
    public List<TransactionCategory> findAllBaseCategories() {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_category WHERE parent_id IS NULL ORDER BY name ASC",
                JdbcTransactionCategoryRepository::parseCategory
        );
    }

    @Override
    public List<TransactionCategory> findAll() {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_category ORDER BY parent_id ASC, name ASC",
                JdbcTransactionCategoryRepository::parseCategory
        );
    }

    @Override
    public long insert(long parentId, String name, Color color) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_category (parent_id, name, color) VALUES (?, ?, ?)",
                List.of(parentId, name, ColorUtil.toHex(color))
        );
    }

    @Override
    public long insert(String name, Color color) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_category (name, color) VALUES (?, ?)",
                List.of(name, ColorUtil.toHex(color))
        );
    }

    @Override
    public void deleteById(long id) {
        DbUtil.updateOne(conn, "DELETE FROM transaction_category WHERE id = ?", id);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static TransactionCategory parseCategory(ResultSet rs) throws SQLException {
        return new TransactionCategory(
                rs.getLong("id"),
                rs.getObject("parent_id", Long.class),
                rs.getString("name"),
                Color.valueOf("#" + rs.getString("color"))
        );
    }
}
