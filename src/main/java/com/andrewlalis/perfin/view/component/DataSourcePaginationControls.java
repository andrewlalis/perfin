package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.CompletableFuture;

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
    public final ObservableList<Sort> sorts = FXCollections.observableArrayList();

    private final IntegerProperty itemsPerPage = new SimpleIntegerProperty();
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
        maxPagesText.visibleProperty().bind(maxPages.greaterThan(0));

        TextFlow pageText = new TextFlow(new Text("Page "), currentPageLabel, maxPagesText);

        AnchorPane pageTextContainer = new AnchorPane(pageText);
        AnchorPane.setTopAnchor(pageText, 4.0);
        AnchorPane.setRightAnchor(pageText, 0.0);
        AnchorPane.setBottomAnchor(pageText, 0.0);
        AnchorPane.setLeftAnchor(pageText, 0.0);


        Button previousPageButton = new Button("Previous Page");
        previousPageButton.disableProperty().bind(currentPage.lessThan(2).or(fetching));
        previousPageButton.setOnAction(event -> setPage(currentPage.get() - 1));

        Button nextPageButton = new Button("Next Page");
        nextPageButton.disableProperty().bind(fetching.or(currentPage.greaterThanOrEqualTo(maxPages)));
        nextPageButton.setOnAction(event -> setPage(currentPage.get() + 1));

        ChoiceBox<Integer> itemsPerPageChoice = new ChoiceBox<>();
        itemsPerPageChoice.getItems().addAll(5, 10, 20, 50, 100);
        itemsPerPageChoice.getSelectionModel().select(10);
        itemsPerPageChoice.setValue(10);
        itemsPerPage.bind(itemsPerPageChoice.valueProperty());
        itemsPerPage.addListener((observable, oldValue, newValue) -> setPage(1));

        BorderPane pageSizePane = new BorderPane();
        Label pageSizeLabel = new Label("Items Per Page");
        pageSizeLabel.getStyleClass().add("std-padding");
        BorderPane.setAlignment(pageSizeLabel, Pos.CENTER_LEFT);
        pageSizePane.setLeft(pageSizeLabel);
        pageSizePane.setCenter(itemsPerPageChoice);

        HBox hbox = new HBox(
                previousPageButton,
                pageTextContainer,
                nextPageButton,
                pageSizePane
        );
        hbox.getStyleClass().addAll("std-padding", "std-spacing");
        setCenter(hbox);
    }

    public CompletableFuture<Void> setPage(int page) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        fetching.set(true);
        PageRequest pagination = new PageRequest(page - 1, itemsPerPage.get(), sorts);
        Thread.ofVirtual().start(() -> {
            try {
                var p = fetcher.fetchPage(pagination);
                int totalResults = fetcher.getTotalCount();
                Platform.runLater(() -> {
                    target.setAll(p.items());
                    if (totalResults != -1) {
                        int max = totalResults / itemsPerPage.get();
                        if (totalResults % itemsPerPage.get() != 0) {
                            max += 1;
                        }
                        maxPages.set(max);
                    }
                    currentPage.set(page);
                    fetching.set(false);
                    cf.complete(null);
                });
            } catch (Exception e) {
                e.printStackTrace(System.err);
                Platform.runLater(() -> {
                    target.clear();
                    fetching.set(false);
                    cf.complete(null);
                });
            }
        });
        return cf;
    }

    public int getItemsPerPage() {
        return itemsPerPage.get();
    }
}
