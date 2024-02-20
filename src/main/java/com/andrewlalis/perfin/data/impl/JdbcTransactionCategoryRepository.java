package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.util.ColorUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    public TransactionCategory findRoot(long categoryId) {
        TransactionCategory category = findById(categoryId).orElse(null);
        if (category == null || category.getParentId() == null) return category;
        return findRoot(category.getParentId());
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
    public void update(long id, String name, Color color) {
        DbUtil.doTransaction(conn, () -> {
            TransactionCategory category = findById(id).orElseThrow();
            if (!category.getName().equals(name)) {
                DbUtil.updateOne(
                        conn,
                        "UPDATE transaction_category SET name = ? WHERE id = ?",
                        name,
                        id
                );
            }
            if (!category.getColor().equals(color)) {
                DbUtil.updateOne(
                        conn,
                        "UPDATE transaction_category SET color = ? WHERE id = ?",
                        ColorUtil.toHex(color),
                        id
                );
            }
        });
    }

    @Override
    public void deleteById(long id) {
        DbUtil.updateOne(conn, "DELETE FROM transaction_category WHERE id = ?", id);
    }

    @Override
    public List<CategoryTreeNode> findTree() {
        List<TransactionCategory> rootCategories = DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_category WHERE parent_id IS NULL ORDER BY name ASC",
                JdbcTransactionCategoryRepository::parseCategory
        );
        List<CategoryTreeNode> rootNodes = new ArrayList<>(rootCategories.size());
        for (var category : rootCategories) {
            rootNodes.add(findTreeRecursive(category));
        }
        return rootNodes;
    }

    @Override
    public CategoryTreeNode findTree(TransactionCategory root) {
        return findTreeRecursive(root);
    }

    private CategoryTreeNode findTreeRecursive(TransactionCategory root) {
        CategoryTreeNode node = new CategoryTreeNode(root, new ArrayList<>());
        List<TransactionCategory> childCategories = DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_category WHERE parent_id = ? ORDER BY name ASC",
                List.of(root.id),
                JdbcTransactionCategoryRepository::parseCategory
        );
        for (var childCategory : childCategories) {
            node.children().add(findTreeRecursive(childCategory));
        }
        return node;
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static TransactionCategory parseCategory(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        Long parentId = rs.getLong("parent_id");
        if (rs.wasNull()) parentId = null;
        String name = rs.getString("name");
        Color color = Color.valueOf("#" + rs.getString("color"));
        return new TransactionCategory(id, parentId, name, color);
    }
}
