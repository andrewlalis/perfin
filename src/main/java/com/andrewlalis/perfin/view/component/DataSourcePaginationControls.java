package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A pane that contains some controls for navigating a paginated data source.
 * That includes going to the next/previous page, setting the preferred page
 * size.
 */
public class DataSourcePaginationControls extends BorderPane {
    public interface PageFetcherFunction {
        Page<? extends Node> fetchPage(PageRequest pagination) throws Exception;
        default int getTotalCount() throws Exception {
            return -1;
        }
    }

    public final IntegerProperty currentPage = new SimpleIntegerProperty(1);
    public final IntegerProperty maxPages = new SimpleIntegerProperty(-1);
    public final IntegerProperty itemsPerPage = new SimpleIntegerProperty(5);
    public final ObservableList<Sort> sorts = FXCollections.observableArrayList();
    private final BooleanProperty fetching = new SimpleBooleanProperty(false);
    private final ObservableList<Node> target;
    private final PageFetcherFunction fetcher;

    public DataSourcePaginationControls(ObservableList<Node> target, PageFetcherFunction fetcher) {
        this.target = target;
        this.fetcher = fetcher;

        Text currentPageLabel = new Text();
        currentPageLabel.textProperty().bind(currentPage.asString());
        Text maxPagesLabel = new Text();
        maxPagesLabel.textProperty().bind(maxPages.asString());
        TextFlow maxPagesText = new TextFlow(new Text(" / "), maxPagesLabel);
        maxPagesText.managedProperty().bind(maxPagesText.visibleProperty());
        maxPagesText.visibleProperty().bind(maxPages.isNotEqualTo(-1));
        TextFlow pageText = new TextFlow(new Text("Page "), currentPageLabel, maxPagesText);


        Button previousPageButton = new Button("Previous Page");
        previousPageButton.disableProperty().bind(currentPage.lessThan(2).or(fetching));
        previousPageButton.setOnAction(event -> setPage(currentPage.get() - 1));
        Button nextPageButton = new Button("Next Page");
        nextPageButton.disableProperty().bind(fetching.or(currentPage.greaterThanOrEqualTo(maxPages)));
        nextPageButton.setOnAction(event -> setPage(currentPage.get() + 1));

        sorts.addListener((ListChangeListener<Sort>) c -> {
            setPage(1);
        });

        HBox hbox = new HBox(
                previousPageButton,
                pageText,
                nextPageButton
        );
        setCenter(hbox);
    }

    public void setPage(int page) {
        try {
            fetching.set(true);
            PageRequest pagination = new PageRequest(page - 1, itemsPerPage.get(), sorts);
            var p = fetcher.fetchPage(pagination);
            int totalResults = fetcher.getTotalCount();
            target.setAll(p.items());
            if (totalResults != -1) {
                int max = totalResults / itemsPerPage.get();
                if (totalResults % itemsPerPage.get() != 0) {
                    max += 1;
                }
                maxPages.set(max);
            }
            currentPage.set(page);
        } catch (Exception e) {
            target.clear();
            e.printStackTrace(System.err);
        } finally {
            fetching.set(false);
        }
    }
}
