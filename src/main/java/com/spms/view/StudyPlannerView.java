package com.spms.view;

import com.spms.model.StudyBlock;
import com.spms.model.StudyPlan;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * Study Planner: create plans, view blocks, mark blocks complete.
 */
public class StudyPlannerView {

    private final DatabaseService db = DatabaseService.getInstance();
    private ObservableList<StudyPlan>  plansData  = FXCollections.observableArrayList();
    private ObservableList<StudyBlock> blocksData = FXCollections.observableArrayList();
    private TableView<StudyPlan>  plansTable;
    private TableView<StudyBlock> blocksTable;
    private Label blocksHeader;

    public Node build() {
        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");
        page.getChildren().add(buildHeader());

        ScrollPane scroll = UIFactory.wrappedScrollPane(buildBody());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);
        refreshPlans();
        return page;
    }

    private Node buildHeader() {
        HBox h = new HBox(12);
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("📅  Study Planner");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = UIFactory.createPrimaryButton("+ New Plan");
        addBtn.setOnAction(e -> openPlanDialog(null));
        h.getChildren().addAll(title, sp, addBtn);
        return h;
    }

    private Node buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(buildPlansSection(), buildBlocksSection());
        return body;
    }

    // ── Plans table ──────────────────────────────────────────────────────────

    private Node buildPlansSection() {
        plansTable = new TableView<>(plansData);
        plansTable.getStyleClass().add("styled-table");
        plansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        plansTable.setPrefHeight(260);

        TableColumn<StudyPlan, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
        subjectCol.setPrefWidth(180);

        TableColumn<StudyPlan, String> deadlineCol = new TableColumn<>("Deadline");
        deadlineCol.setCellValueFactory(d -> new SimpleStringProperty(
                DateUtil.formatDate(d.getValue().getDeadline())));
        deadlineCol.setPrefWidth(130);

        TableColumn<StudyPlan, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(d -> new SimpleStringProperty(
                "★".repeat(d.getValue().getDifficulty()) + "☆".repeat(5 - d.getValue().getDifficulty())));
        diffCol.setPrefWidth(110);

        TableColumn<StudyPlan, String> hoursCol = new TableColumn<>("Daily Hours");
        hoursCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDailyHours() + "h"));
        hoursCol.setPrefWidth(100);

        TableColumn<StudyPlan, String> progressCol = new TableColumn<>("Progress");
        progressCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                StudyPlan p = (StudyPlan) getTableRow().getItem();
                ProgressBar pb = new ProgressBar(p.getCompletionPercentage() / 100.0);
                pb.getStyleClass().addAll("styled-progress", "progress-purple");
                pb.setMaxWidth(Double.MAX_VALUE);
                Label pct = new Label(String.format("%.0f%%", p.getCompletionPercentage()));
                pct.getStyleClass().add("text-muted-sm");
                HBox box = new HBox(8, pb, pct);
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(pb, Priority.ALWAYS);
                setGraphic(box);
            }
        });
        progressCol.setPrefWidth(160);

        TableColumn<StudyPlan, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String cls = switch(item) {
                    case "ACTIVE"    -> "badge-success";
                    case "COMPLETED" -> "badge-info";
                    default          -> "badge-warning";
                };
                setGraphic(UIFactory.createBadgeLabel(item, cls));
            }
        });
        statusCol.setPrefWidth(90);

        TableColumn<StudyPlan, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                StudyPlan plan = (StudyPlan) getTableRow().getItem();
                Button editBtn = UIFactory.createSmallButton("Edit", "btn-secondary");
                Button delBtn  = UIFactory.createSmallButton("Delete", "btn-danger");
                editBtn.setOnAction(e -> openPlanDialog(plan));
                delBtn.setOnAction(e -> deletePlan(plan));
                HBox box = new HBox(6, editBtn, delBtn);
                setGraphic(box);
            }
        });
        actionsCol.setPrefWidth(130);

        plansTable.getColumns().addAll(subjectCol, deadlineCol, diffCol,
                hoursCol, progressCol, statusCol, actionsCol);

        plansTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) loadBlocks(sel); });

        return UIFactory.createCard("📋  My Study Plans", plansTable);
    }

    // ── Blocks section ────────────────────────────────────────────────────────

    private Node buildBlocksSection() {
        blocksHeader = new Label("📆  Study Blocks");
        blocksHeader.getStyleClass().add("card-title");

        blocksTable = new TableView<>(blocksData);
        blocksTable.getStyleClass().add("styled-table");
        blocksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        blocksTable.setPrefHeight(220);

        TableColumn<StudyBlock, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                DateUtil.formatDate(d.getValue().getBlockDate())));
        dateCol.setPrefWidth(140);

        TableColumn<StudyBlock, String> topicCol = new TableColumn<>("Topic");
        topicCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTopic()));
        topicCol.setPrefWidth(260);

        TableColumn<StudyBlock, String> hoursCol = new TableColumn<>("Hours");
        hoursCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getHours() + "h"));
        hoursCol.setPrefWidth(80);

        TableColumn<StudyBlock, String> doneCol = new TableColumn<>("Done?");
        doneCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                StudyBlock block = (StudyBlock) getTableRow().getItem();
                CheckBox cb = new CheckBox();
                cb.setSelected(block.isCompleted());
                cb.setOnAction(e -> {
                    block.setCompleted(cb.isSelected());
                    db.updateStudyBlock(block);
                    refreshPlanProgress();
                });
                setGraphic(cb);
            }
        });
        doneCol.setPrefWidth(70);

        blocksTable.getColumns().addAll(dateCol, topicCol, hoursCol, doneCol);

        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.getChildren().addAll(blocksHeader, blocksTable);

        HBox hint = new HBox();
        hint.getChildren().add(new Label("← Select a plan above to view its daily blocks"));
        hint.getChildren().get(0).getStyleClass().add("text-muted");
        hint.setPadding(new Insets(4, 0, 0, 0));
        card.getChildren().add(hint);
        return card;
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────

    private void openPlanDialog(StudyPlan existing) {
        Dialog<StudyPlan> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Study Plan" : "Edit Study Plan");
        dialog.setHeaderText(existing == null ? "📅 Create a new study plan" : "📅 Update study plan");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setPadding(new Insets(24));

        TextField subjectFld  = UIFactory.createTextField("e.g. Mathematics");
        DatePicker deadlineDp = UIFactory.createDatePicker();
        ComboBox<Integer> diffCb = new ComboBox<>(
                FXCollections.observableArrayList(1, 2, 3, 4, 5));
        diffCb.getStyleClass().add("combo-field");
        diffCb.setValue(3);
        TextField hoursFld    = UIFactory.createTextField("e.g. 2.0");
        ComboBox<String> statusCb = new ComboBox<>(
                FXCollections.observableArrayList("ACTIVE", "PAUSED", "COMPLETED"));
        statusCb.getStyleClass().add("combo-field");
        statusCb.setValue("ACTIVE");

        if (existing != null) {
            subjectFld.setText(existing.getSubject());
            deadlineDp.setValue(existing.getDeadline());
            diffCb.setValue(existing.getDifficulty());
            hoursFld.setText(String.valueOf(existing.getDailyHours()));
            statusCb.setValue(existing.getStatus().name());
        }

        grid.add(UIFactory.createFormLabel("Subject *"),    0, 0); grid.add(subjectFld,  1, 0);
        grid.add(UIFactory.createFormLabel("Deadline *"),   0, 1); grid.add(deadlineDp, 1, 1);
        grid.add(UIFactory.createFormLabel("Difficulty"),   0, 2); grid.add(diffCb,     1, 2);
        grid.add(UIFactory.createFormLabel("Daily Hours"),  0, 3); grid.add(hoursFld,   1, 3);
        grid.add(UIFactory.createFormLabel("Status"),       0, 4); grid.add(statusCb,   1, 4);

        GridPane.setHgrow(subjectFld, Priority.ALWAYS);
        GridPane.setHgrow(deadlineDp, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);

        final javafx.scene.control.Button btOk = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String subj = subjectFld.getText().trim();
            if (subj.isEmpty() || deadlineDp.getValue() == null) {
                UIFactory.showError("Subject and Deadline are required.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            String subj = subjectFld.getText().trim();
            double hours = 2.0;
            try { hours = Double.parseDouble(hoursFld.getText().trim()); } catch (Exception ignored) {}

            StudyPlan p = existing != null ? existing : new StudyPlan();
            p.setSubject(subj);
            p.setDeadline(deadlineDp.getValue());
            p.setDifficulty(diffCb.getValue());
            p.setDailyHours(hours);
            p.setStatus(StudyPlan.Status.valueOf(statusCb.getValue()));
            return p;
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p == null) return;
            if (existing == null) {
                int id = db.saveStudyPlanReturnId(p);
                generateBlocks(p, id);
            } else {
                db.updateStudyPlan(p);
            }
            refreshPlans();
        });
    }

    /** Auto-generates daily study blocks from today to deadline. */
    private void generateBlocks(StudyPlan plan, int planId) {
        if (plan.getDeadline() == null || planId < 0) return;
        LocalDate cursor = LocalDate.now();
        LocalDate end    = plan.getDeadline();
        String[] topics  = {"Introduction & Overview", "Core Concepts", "Practice Problems",
                "Deep Dive", "Review & Revision", "Mock Exam", "Final Revision"};
        int idx = 0;
        while (!cursor.isAfter(end)) {
            StudyBlock block = new StudyBlock(planId, cursor, plan.getDailyHours(),
                    topics[idx % topics.length]);
            db.saveStudyBlock(block);
            cursor = cursor.plusDays(1);
            idx++;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshPlans() {
        plansData.setAll(db.getAllStudyPlans());
    }

    private void loadBlocks(StudyPlan plan) {
        blocksHeader.setText("📆  Study Blocks – " + plan.getSubject());
        blocksData.setAll(db.getBlocksForPlan(plan.getId()));
    }

    private void refreshPlanProgress() {
        // Re-load plans to update progress column
        plansData.setAll(db.getAllStudyPlans());
    }

    private void deletePlan(StudyPlan plan) {
        if (UIFactory.confirmDelete("Study Plan: " + plan.getSubject())) {
            db.deleteStudyPlan(plan.getId());
            blocksData.clear();
            refreshPlans();
        }
    }
}
