package com.spms.view;

import com.spms.model.Goal;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Goal Setting module: create, track, edit, and complete goals.
 */
public class GoalsView {

    private final DatabaseService db = DatabaseService.getInstance();
    private FlowPane goalFlow;
    private String   activeFilter = "ALL";

    public Node build() {
        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");
        page.getChildren().add(buildHeader());

        ScrollPane scroll = UIFactory.wrappedScrollPane(buildBody());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);
        return page;
    }

    private Node buildHeader() {
        HBox h = new HBox(12);
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("🎯  Goals");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = UIFactory.createPrimaryButton("+ New Goal");
        addBtn.setOnAction(e -> openGoalDialog(null));
        h.getChildren().addAll(title, sp, addBtn);
        return h;
    }

    private Node buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(buildSummaryRow(), buildFilterTabs(), buildGoalGrid());
        return body;
    }

    // ── Summary row ──────────────────────────────────────────────────────────

    private Node buildSummaryRow() {
        List<Goal> all = db.getAllGoals();
        long total    = all.size();
        long active   = all.stream().filter(g -> !g.isCompleted()).count();
        long completed= all.stream().filter(Goal::isCompleted).count();

        HBox row = new HBox(16);
        VBox c1 = UIFactory.createStatCard(String.valueOf(total),     "Total Goals",     "🎯", "stat-purple");
        VBox c2 = UIFactory.createStatCard(String.valueOf(active),    "Active",          "⚡", "stat-blue");
        VBox c3 = UIFactory.createStatCard(String.valueOf(completed), "Completed",       "✅", "stat-green");
        for (VBox c : new VBox[]{c1, c2, c3}) HBox.setHgrow(c, Priority.ALWAYS);
        row.getChildren().addAll(c1, c2, c3);
        return row;
    }

    // ── Filter tabs ──────────────────────────────────────────────────────────

    private Node buildFilterTabs() {
        HBox tabs = new HBox(8);
        tabs.setAlignment(Pos.CENTER_LEFT);
        String[] filters = {"ALL", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM"};
        ToggleGroup tg = new ToggleGroup();
        for (String f : filters) {
            ToggleButton tb = new ToggleButton(f);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("filter-tab");
            if (f.equals(activeFilter)) tb.setSelected(true);
            tb.setOnAction(e -> {
                activeFilter = f;
                refreshGoals();
            });
            tabs.getChildren().add(tb);
        }
        return tabs;
    }

    // ── Goal grid ────────────────────────────────────────────────────────────

    private Node buildGoalGrid() {
        goalFlow = new FlowPane(16, 16);
        goalFlow.setPrefWrapLength(1000);
        refreshGoals();
        return goalFlow;
    }

    private void refreshGoals() {
        if (goalFlow == null) return;
        List<Goal> goals = db.getAllGoals();
        if (!activeFilter.equals("ALL")) {
            Goal.Type type = Goal.Type.valueOf(activeFilter);
            goals = goals.stream().filter(g -> g.getType() == type).collect(Collectors.toList());
        }
        goalFlow.getChildren().clear();
        if (goals.isEmpty()) {
            goalFlow.getChildren().add(
                    UIFactory.createEmptyState("🎯", "No goals yet",
                            "Set a goal to track your progress and stay motivated."));
        } else {
            for (Goal g : goals) goalFlow.getChildren().add(buildGoalCard(g));
        }
    }

    // ── Goal card ─────────────────────────────────────────────────────────────

    private Node buildGoalCard(Goal goal) {
        VBox card = new VBox(10);
        card.getStyleClass().add("goal-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(280);
        card.setMinWidth(240);

        // Header row: type badge + completed indicator
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        String typeClass = switch (goal.getType()) {
            case DAILY   -> "badge-info";
            case WEEKLY  -> "badge-warning";
            case MONTHLY -> "badge-success";
            case CUSTOM  -> "badge-primary";
        };
        Label typeBadge = UIFactory.createBadgeLabel(goal.getType().name(), typeClass);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        if (goal.isCompleted()) {
            Label doneLbl = UIFactory.createBadgeLabel("✓ Done", "badge-success");
            topRow.getChildren().addAll(typeBadge, sp, doneLbl);
        } else {
            topRow.getChildren().addAll(typeBadge, sp);
        }

        Label titleLbl = new Label(goal.getTitle());
        titleLbl.getStyleClass().add("goal-title");
        titleLbl.setWrapText(true);

        if (goal.getDescription() != null && !goal.getDescription().isBlank()) {
            Label descLbl = new Label(goal.getDescription());
            descLbl.getStyleClass().add("text-muted");
            descLbl.setWrapText(true);
            card.getChildren().addAll(topRow, titleLbl, descLbl);
        } else {
            card.getChildren().addAll(topRow, titleLbl);
        }

        // Progress bar
        double pct = goal.getProgressPercentage();
        ProgressBar pb = UIFactory.createProgressBar(pct / 100.0,
                pct >= 100 ? "progress-green" : pct >= 50 ? "progress-blue" : "progress-orange");

        // Current / target
        String progressText = goal.getCurrentValue() + " / " + goal.getTargetValue()
                + (goal.getUnit() != null ? " " + goal.getUnit() : "");
        HBox progressRow = new HBox(8);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        Label progressLbl = new Label(progressText);
        progressLbl.getStyleClass().add("text-muted-sm");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label pctLbl = new Label(String.format("%.0f%%", pct));
        pctLbl.getStyleClass().add(pct >= 100 ? "text-success" : "text-dim");
        progressRow.getChildren().addAll(progressLbl, sp2, pctLbl);

        card.getChildren().addAll(pb, progressRow);

        // Due date
        if (goal.getDueDate() != null) {
            Label dueLbl = new Label("Due: " + DateUtil.getRelativeDate(goal.getDueDate()));
            dueLbl.getStyleClass().add(goal.isExpired() ? "overdue-label" : "text-muted-sm");
            card.getChildren().add(dueLbl);
        }

        // Action buttons – two rows of two so labels never truncate
        Button progressBtn = UIFactory.createSmallButton("+ Update", "btn-primary");
        Button completeBtn = UIFactory.createSmallButton(
                goal.isCompleted() ? "Reopen" : "Done", "btn-secondary");
        Button editBtn     = UIFactory.createSmallButton("Edit",   "btn-secondary");
        Button deleteBtn   = UIFactory.createSmallButton("Delete", "btn-danger");

        progressBtn.setOnAction(e -> openProgressDialog(goal));
        completeBtn.setOnAction(e -> toggleComplete(goal));
        editBtn.setOnAction(e     -> openGoalDialog(goal));
        deleteBtn.setOnAction(e   -> {
            if (UIFactory.confirmDelete(goal.getTitle())) {
                db.deleteGoal(goal.getId());
                refreshGoals();
            }
        });

        for (Button b : new Button[]{progressBtn, completeBtn, editBtn, deleteBtn}) {
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }

        HBox row1 = new HBox(8, progressBtn, completeBtn);
        HBox row2 = new HBox(8, editBtn, deleteBtn);
        VBox actions = new VBox(8, row1, row2);
        actions.setPadding(new Insets(6, 0, 0, 0));
        card.getChildren().add(actions);

        // Visual indicator for expired/overdue
        if (goal.isExpired() && !goal.isCompleted()) {
            card.setStyle(card.getStyle() + "-fx-border-color: " + UIFactory.C_DANGER + "; -fx-border-width: 0 0 0 3;");
        }
        return card;
    }

    // ── Add/Edit dialog ───────────────────────────────────────────────────────

    private void openGoalDialog(Goal existing) {
        Dialog<Goal> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "New Goal" : "Edit Goal");
        dlg.setHeaderText(existing == null ? "🎯 Enter new goal details" : "🎯 Update goal details");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setPadding(new Insets(24));

        TextField titleFld = UIFactory.createTextField("Goal title *");
        TextArea  descFld  = UIFactory.createTextArea("Brief description (optional)", 2);
        ComboBox<Goal.Type> typeCb = new ComboBox<>(
                FXCollections.observableArrayList(Goal.Type.values()));
        typeCb.getStyleClass().add("combo-field");
        typeCb.setValue(Goal.Type.DAILY);
        TextField targetFld = UIFactory.createTextField("e.g. 2");
        TextField unitFld   = UIFactory.createTextField("e.g. hours, tasks");
        DatePicker dueDp    = UIFactory.createDatePicker();
        dueDp.setValue(LocalDate.now().plusDays(1));

        if (existing != null) {
            titleFld.setText(existing.getTitle());
            descFld.setText(existing.getDescription());
            typeCb.setValue(existing.getType());
            targetFld.setText(String.valueOf(existing.getTargetValue()));
            unitFld.setText(existing.getUnit());
            dueDp.setValue(existing.getDueDate());
        }

        int row = 0;
        grid.add(UIFactory.createFormLabel("Title *"),        0, row); grid.add(titleFld,  1, row++);
        grid.add(UIFactory.createFormLabel("Description"),    0, row); grid.add(descFld,   1, row++);
        grid.add(UIFactory.createFormLabel("Type"),           0, row); grid.add(typeCb,    1, row++);
        grid.add(UIFactory.createFormLabel("Target"),         0, row); grid.add(targetFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Unit"),           0, row); grid.add(unitFld,   1, row++);
        grid.add(UIFactory.createFormLabel("Due Date"),       0, row); grid.add(dueDp,     1, row);
        GridPane.setHgrow(titleFld, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);

        final javafx.scene.control.Button btOk = (javafx.scene.control.Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleFld.getText().isBlank()) {
                UIFactory.showError("Goal title is required.");
                event.consume();
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            double target = 1.0;
            try { target = Double.parseDouble(targetFld.getText().trim()); } catch (Exception ignored) {}

            Goal g = existing != null ? existing : new Goal();
            g.setTitle(titleFld.getText().trim());
            g.setDescription(descFld.getText().trim());
            g.setType(typeCb.getValue());
            g.setTargetValue(target);
            g.setUnit(unitFld.getText().trim());
            g.setDueDate(dueDp.getValue());
            return g;
        });

        dlg.showAndWait().ifPresent(g -> {
            if (g == null) return;
            if (existing == null) db.saveGoal(g);
            else                  db.updateGoal(g);
            refreshGoals();
        });
    }

    // ── Progress update dialog ────────────────────────────────────────────────

    private void openProgressDialog(Goal goal) {
        TextInputDialog dlg = new TextInputDialog(String.valueOf(goal.getCurrentValue()));
        dlg.setTitle("Update Progress");
        dlg.setHeaderText("Update progress for: " + goal.getTitle());
        dlg.setContentText("Current value (" + goal.getUnit() + "):");
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1b2e; -fx-border-color: #353655;");
        dlg.showAndWait().ifPresent(val -> {
            try {
                double v = Double.parseDouble(val.trim());
                goal.setCurrentValue(v);
                if (v >= goal.getTargetValue()) goal.setCompleted(true);
                db.updateGoal(goal);
                refreshGoals();
            } catch (NumberFormatException e) {
                UIFactory.showError("Please enter a valid number.");
            }
        });
    }

    private void toggleComplete(Goal goal) {
        goal.setCompleted(!goal.isCompleted());
        if (goal.isCompleted()) goal.setCurrentValue(goal.getTargetValue());
        db.updateGoal(goal);
        refreshGoals();
    }
}
