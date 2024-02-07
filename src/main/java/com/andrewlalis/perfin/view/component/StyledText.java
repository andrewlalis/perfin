package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.PerfinApp;
import javafx.beans.DefaultProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.geometry.Insets;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.helpRouter;
import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A component that renders markdown-ish text as a series of TextFlow elements,
 * styled according to some basic styles.
 */
@DefaultProperty("text")
public class StyledText extends VBox {
    private StringProperty text;
    private boolean initialized = false;

    public StyledText() {
        getStyleClass().add("spacing-extra");
    }

    public final void setText(String value) {
        initialized = false;
        if (value == null) value = "";
        textProperty().set(value);
        layoutChildren(); // Re-render the underlying text.
    }

    public final String getText() {
        return text == null ? "" : text.get();
    }

    public final StringProperty textProperty() {
        if (text == null) {
            text = new StringPropertyBase("") {
                @Override public Object getBean() { return StyledText.this; }
                @Override public String getName() { return "text"; }
                @Override  public void invalidated() {
                    notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
                }
            };
        }
        return text;
    }

    @Override
    protected void layoutChildren() {
        if (!initialized) {
            String s = getText();
            getChildren().clear();
            getChildren().addAll(renderText(s));
            initialized = true;
        }
        super.layoutChildren();
    }

    private List<TextFlow> renderText(String text) {
        return new TextFlowBuilder().build(text);
    }

    private static class TextFlowBuilder {
        private final List<TextFlow> flows = new ArrayList<>();
        private int idx = 0;
        private final StringBuilder currentRun = new StringBuilder();
        private TextFlow currentParagraph;

        public List<TextFlow> build(String text) {
            flows.clear();
            idx = 0;
            currentRun.setLength(0);
            currentParagraph = new TextFlow();
            currentParagraph.setStyle("-fx-text-fill: inherit;");

            while (idx < text.length()) {
                if (text.startsWith("**", idx)) {
                    parseStyledText(text, "**", "bold-text");
                } else if (text.startsWith("*", idx)) {
                    parseStyledText(text, "*", "italic-text");
                } else if (text.startsWith("`", idx)) {
                    parseStyledText(text, "`", "mono-font");
                } else if (text.startsWith(" -- ", idx)) {
                    parsePageBreak(text);
                } else if (text.startsWith("[", idx)) {
                    parseLink(text);
                } else if (text.startsWith("#", idx) && (idx == 0 || (idx > 0 && text.charAt(idx - 1) == ' '))) {
                    parseHeader(text);
                } else {
                    currentRun.append(text.charAt(idx));
                    idx++;
                }
            }
            appendTextIfPresent();
            appendParagraphIfPresent();
            return flows;
        }

        private void parsePageBreak(String text) {
            appendTextIfPresent();
            appendParagraphIfPresent();
            while (text.charAt(idx) == ' ') idx++;
            while (text.charAt(idx) == '-') idx++;
            while (text.charAt(idx) == ' ') idx++;
        }

        private void parseStyledText(String text, String marker, String styleClass) {
            appendTextIfPresent();
            int endIdx = text.indexOf(marker, idx + marker.length());
            Text textItem = new Text(text.substring(idx + marker.length(), endIdx));
            textItem.getStyleClass().add(styleClass);
            textItem.setStyle("-fx-text-fill: inherit;");
            currentParagraph.getChildren().add(textItem);
            idx = endIdx + marker.length();
        }

        private void parseLink(String text) {
            appendTextIfPresent();
            int labelEndIdx = text.indexOf(']', idx);
            String label = text.substring(idx + 1, labelEndIdx);
            idx = labelEndIdx + 1;
            final String link;
            if (text.charAt(labelEndIdx + 1) == '(') {
                int linkEndIdx = text.indexOf(')', labelEndIdx + 2);
                link = text.substring(labelEndIdx + 2, linkEndIdx);
                idx = linkEndIdx + 1;
            } else {
                link = null;
            }
            Hyperlink hyperlink = new Hyperlink(label);
            if (link != null) {
                if (link.startsWith("http")) {
                    hyperlink.setOnAction(event -> PerfinApp.instance.getHostServices().showDocument(link));
                } else if (link.startsWith("help:")) {
                    hyperlink.setOnAction(event -> helpRouter.navigate(link.substring(5).strip()));
                } else if (link.startsWith("app:")) {
                    hyperlink.setOnAction(event -> router.navigate(link.substring(4).strip()));
                }
            }
            hyperlink.setBorder(Border.EMPTY);
            hyperlink.setPadding(new Insets(0, 0, 0, 0));
            currentParagraph.getChildren().add(hyperlink);
        }

        private void parseHeader(String text) {
            appendTextIfPresent();
            appendParagraphIfPresent();
            int size = 0;
            while (text.charAt(idx) == '#') {
                idx++;
                size++;
            }
            int endIdx = text.indexOf("#".repeat(size), idx);
            Text header = new Text(text.substring(idx, endIdx).strip());
            idx = endIdx + size;
            while (text.charAt(idx) == ' ') idx++;
            String styleClass = switch(size) {
                case 1 -> "large-font";
                case 2 -> "largest-font";
                default -> "largest-font";
            };
            header.getStyleClass().addAll(styleClass, "bold-text");
            currentParagraph.getChildren().add(header);
            appendParagraphIfPresent();
        }

        private void appendTextIfPresent() {
            if (!currentRun.isEmpty()) {
                currentParagraph.getChildren().add(new Text(currentRun.toString()));
                currentRun.setLength(0);
            }
        }

        private void appendParagraphIfPresent() {
            if (!currentParagraph.getChildren().isEmpty()) {
                flows.add(currentParagraph);
                currentParagraph = new TextFlow();
                currentParagraph.setStyle("-fx-text-fill: inherit;");
            }
        }
    }
}
