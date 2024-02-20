package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransactionCategoryRepository extends Repository, AutoCloseable {
    Optional<TransactionCategory> findById(long id);
    Optional<TransactionCategory> findByName(String name);
    List<TransactionCategory> findAllBaseCategories();
    List<TransactionCategory> findAll();
    TransactionCategory findRoot(long categoryId);
    long insert(long parentId, String name, Color color);
    long insert(String name, Color color);
    void update(long id, String name, Color color);
    void deleteById(long id);

    record CategoryTreeNode(TransactionCategory category, List<CategoryTreeNode> children) {
        public Set<Long> allIds() {
            Set<Long> ids = new HashSet<>();
            ids.add(category.id);
            for (var child : children) {
                ids.addAll(child.allIds());
            }
            return ids;
        }
    }

    List<CategoryTreeNode> findTree();
    CategoryTreeNode findTree(TransactionCategory root);
}
