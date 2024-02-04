package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository extends Repository, AutoCloseable {
    Optional<TransactionCategory> findById(long id);
    Optional<TransactionCategory> findByName(String name);
    List<TransactionCategory> findAllBaseCategories();
    List<TransactionCategory> findAll();
    long insert(long parentId, String name, Color color);
    long insert(String name, Color color);
    void update(long id, String name, Color color);
    void deleteById(long id);

    record CategoryTreeNode(TransactionCategory category, List<CategoryTreeNode> children){}
    List<CategoryTreeNode> findTree();
}
