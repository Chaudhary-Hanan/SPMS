package com.spms.util;

import com.spms.model.Assignment;
import com.spms.model.Reminder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/**
 * Factory for reusable, consistently-styled UI components.
 * All colours are drawn from the main.css theme.
 */
public final class UIFactory {

    // ── Colour palette (mirrors main.css) ────────────────────────────────────
    public static final String C_PRIMARY      = "#7c5cbf";
    public static final String C_PRIMARY_LT   = "#9b77e0";
    public static final String C_SECONDARY    = "#5bc4d8";
    public static final String C_SUCCESS      = "#4ade80";
    public static final String C_WARNING      = "#fbbf24";
    public static final String C_DANGER       = "#f87171";
    public static final String C_SURFACE      = "#1a1b2e";
    public static final String C_SURFACE2     = "#252641";
    public static final String C_TEXT         = "#e2e8f0";
    public static final String C_TEXT_MUTED   = "#94a3b8";
    public static final String C_BORDER       = "#353655";

    private UIFactory() {}

    // ── Cards ─────────────────────────────────────────────────────────────────

    public static VBox createCard(String title, Node content) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        if (title != null && !title.isBlank()) {
            Label lbl = new Label(title);
            lbl.getStyleClass().add("card-title");
            card.getChildren().addAll(lbl, content);
        } else {
            card.getChildren().add(content);
        }
        return card;
    }

    /**
     * @param colorClass  one of: stat-blue, stat-green, stat-purple, stat-orange, stat-red
     */
    public static VBox createStatCard(String value, String label, String emoji, String colorClass) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("stat-card", colorClass);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(200);
        card.setMinWidth(160);

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 28px;");

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("stat-value");

        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(emojiLbl, valueLbl, labelLbl);
        return card;
    }

    // ── Labels ───────────────────────────────────────────────────────────────

    public static Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    public static Label createSubTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("sub-title");
        return l;
    }

    public static Label createBadgeLabel(String text, String styleClass) {
        Label b = new Label(text);
        b.getStyleClass().addAll("badge", styleClass);
        b.setPadding(new Insets(2, 9, 2, 9));
        return b;
    }

    // ── Buttons ──────────────────────────────────────────────────────────────

    public static Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-primary");
        return btn;
    }

    public static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-secondary");
        return btn;
    }

    public static Button createDangerButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-danger");
        return btn;
    }

    public static Button createSmallButton(String text, String extraClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn-small", extraClass);
        return btn;
    }

    // ── Form controls ────────────────────────────────────────────────────────

    public static TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("input-field");
        return tf;
    }

    public static TextArea createTextArea(String prompt, int prefRows) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(prefRows);
        ta.setWrapText(true);
        ta.getStyleClass().add("input-field");
        return ta;
    }

    public static <T> ComboBox<T> createComboBox() {
        ComboBox<T> cb = new ComboBox<>();
        cb.getStyleClass().add("combo-field");
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    public static DatePicker createDatePicker() {
        DatePicker dp = new DatePicker();
        dp.getStyleClass().add("input-field");
        dp.setMaxWidth(Double.MAX_VALUE);
        return dp;
    }

    public static Label createFormLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    // ── Progress ─────────────────────────────────────────────────────────────

    public static ProgressBar createProgressBar(double progress, String extraClass) {
        ProgressBar pb = new ProgressBar(Math.min(1.0, Math.max(0.0, progress)));
        pb.getStyleClass().addAll("styled-progress", extraClass);
        pb.setMaxWidth(Double.MAX_VALUE);
        return pb;
    }

    // ── Empty state ──────────────────────────────────────────────────────────

    public static VBox createEmptyState(String emoji, String title, String msg) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 60, 40));

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 56px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("empty-title");

        Label msgLbl = new Label(msg);
        msgLbl.getStyleClass().add("empty-msg");
        msgLbl.setWrapText(true);
        msgLbl.setTextAlignment(TextAlignment.CENTER);
        msgLbl.setMaxWidth(340);

        box.getChildren().addAll(emojiLbl, titleLbl, msgLbl);
        return box;
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    public static boolean confirmDelete(String itemName) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm Delete");
        a.setHeaderText("Delete \"" + itemName + "\"?");
        a.setContentText("This action cannot be undone.");
        styleAlert(a);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success");
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    public static void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    public static void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private static void styleAlert(Alert a) {
        DialogPane dp = a.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1b2e; -fx-border-color: #353655;");
        if (dp.lookup(".content.label") != null)
            dp.lookup(".content.label").setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        if (dp.lookup(".header-panel") != null)
            dp.lookup(".header-panel").setStyle("-fx-background-color: #252641;");
    }

    // ── Priority / Urgency helpers ───────────────────────────────────────────

    public static String priorityBadgeClass(Assignment.Priority p) {
        return switch (p) {
            case LOW    -> "badge-success";
            case MEDIUM -> "badge-info";
            case HIGH   -> "badge-warning";
            case URGENT -> "badge-danger";
        };
    }

    public static String urgencyColor(Reminder.Urgency u) {
        return switch (u) {
            case LOW      -> C_SUCCESS;
            case MEDIUM   -> C_SECONDARY;
            case HIGH     -> C_WARNING;
            case CRITICAL -> C_DANGER;
        };
    }

    public static String urgencyBadgeClass(Reminder.Urgency u) {
        return switch (u) {
            case LOW      -> "badge-success";
            case MEDIUM   -> "badge-info";
            case HIGH     -> "badge-warning";
            case CRITICAL -> "badge-danger";
        };
    }

    // ── Separator ────────────────────────────────────────────────────────────

    public static Separator hSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("styled-separator");
        return sep;
    }

    // ── Scrollpane wrapper ───────────────────────────────────────────────────

    public static ScrollPane wrappedScrollPane(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("edge-to-edge");
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }
}
