package com.spms.view;

import com.spms.model.Assignment;
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

/**
 * Tasks / Assignments module: view, add, edit, and delete assignments.
 */
public class AssignmentsView {

    private final DatabaseService db = DatabaseService.getInstance();
    private TableView<Assignment> table;

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
        Label title = new Label("📋  Tasks & Assignments");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = UIFactory.createPrimaryButton("+ Add Task");
        addBtn.setOnAction(e -> openAssignmentDialog(null));
        h.getChildren().addAll(title, sp, addBtn);
        return h;
    }

    private Node buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(buildStatsRow(), buildTable());
        return body;
    }

    private Node buildStatsRow() {
        int pending = db.getPendingAssignmentsCount();
        int completed = db.getCompletedAssignmentsCount();
        int total = db.getAssignmentCount();

        HBox row = new HBox(16);
        VBox c1 = UIFactory.createStatCard(String.valueOf(total), "Total Tasks", "📝", "stat-purple");
        VBox c2 = UIFactory.createStatCard(String.valueOf(pending), "Pending", "⚡", "stat-blue");
        VBox c3 = UIFactory.createStatCard(String.valueOf(completed), "Completed", "✅", "stat-green");
        
        for (VBox c : new VBox[]{c1, c2, c3}) HBox.setHgrow(c, Priority.ALWAYS);
        row.getChildren().addAll(c1, c2, c3);
        return row;
    }

    private Node buildTable() {
        List<Assignment> assignments = db.getAllAssignments();
        table = new TableView<>(FXCollections.observableArrayList(assignments));
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        TableColumn<Assignment, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(220);

        TableColumn<Assignment, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getSubject() != null ? d.getValue().getSubject() : ""));
        subjectCol.setPrefWidth(140);

        TableColumn<Assignment, String> dateCol = new TableColumn<>("Due Date");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatDate(d.getValue().getDueDate())));
        dateCol.setPrefWidth(120);

        TableColumn<Assignment, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getPriority().name()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Assignment a = (Assignment) getTableRow().getItem();
                setGraphic(UIFactory.createBadgeLabel(item, UIFactory.priorityBadgeClass(a.getPriority())));
            }
        });
        priorityCol.setPrefWidth(100);

        TableColumn<Assignment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String cls = item.equals("COMPLETED") ? "badge-success" : item.equals("OVERDUE") ? "badge-danger" : "badge-warning";
                setGraphic(UIFactory.createBadgeLabel(item, cls));
            }
        });
        statusCol.setPrefWidth(100);

        TableColumn<Assignment, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Assignment a = (Assignment) getTableRow().getItem();
                Button editBtn = UIFactory.createSmallButton("Edit", "btn-secondary");
                Button delBtn  = UIFactory.createSmallButton("Delete", "btn-danger");
                editBtn.setOnAction(e -> openAssignmentDialog(a));
                delBtn.setOnAction(e -> {
                    if (UIFactory.confirmDelete(a.getTitle())) {
                        db.deleteAssignment(a.getId());
                        refreshTable();
                    }
                });
                HBox box = new HBox(6, editBtn, delBtn);
                setGraphic(box);
            }
        });
        actionsCol.setPrefWidth(130);

        table.getColumns().addAll(titleCol, subjectCol, dateCol, priorityCol, statusCol, actionsCol);
        return UIFactory.createCard("📝  All Assignments", table);
    }

    private void refreshTable() {
        if (table != null) {
            table.setItems(FXCollections.observableArrayList(db.getAllAssignments()));
            table.refresh();
        }
    }

    private void openAssignmentDialog(Assignment existing) {
        Dialog<Assignment> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Add Task" : "Edit Task");
        dlg.setHeaderText(existing == null ? "📋 Enter new task details" : "📋 Update task details");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setPadding(new Insets(24));

        TextField titleFld = UIFactory.createTextField("Task title *");
        TextField subjectFld = UIFactory.createTextField("Subject (optional)");
        TextArea descFld = UIFactory.createTextArea("Description (optional)", 3);
        DatePicker dateDp = UIFactory.createDatePicker();
        dateDp.setValue(LocalDate.now().plusDays(1));
        
        ComboBox<Assignment.Priority> priorityCb = new ComboBox<>(FXCollections.observableArrayList(Assignment.Priority.values()));
        priorityCb.getStyleClass().add("combo-field");
        priorityCb.setValue(Assignment.Priority.MEDIUM);

        ComboBox<Assignment.Status> statusCb = new ComboBox<>(FXCollections.observableArrayList(Assignment.Status.values()));
        statusCb.getStyleClass().add("combo-field");
        statusCb.setValue(Assignment.Status.PENDING);

        if (existing != null) {
            titleFld.setText(existing.getTitle());
            subjectFld.setText(existing.getSubject());
            descFld.setText(existing.getDescription());
            dateDp.setValue(existing.getDueDate());
            priorityCb.setValue(existing.getPriority());
            statusCb.setValue(existing.getStatus());
        }

        int row = 0;
        grid.add(UIFactory.createFormLabel("Title *"), 0, row); grid.add(titleFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Subject"), 0, row); grid.add(subjectFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Description"), 0, row); grid.add(descFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Due Date *"), 0, row); grid.add(dateDp, 1, row++);
        grid.add(UIFactory.createFormLabel("Priority"), 0, row); grid.add(priorityCb, 1, row++);
        grid.add(UIFactory.createFormLabel("Status"), 0, row); grid.add(statusCb, 1, row);
        GridPane.setHgrow(titleFld, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);

        final Button btOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleFld.getText().isBlank() || dateDp.getValue() == null) {
                UIFactory.showError("Title and Date are required.");
                event.consume();
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Assignment a = existing != null ? existing : new Assignment();
            a.setTitle(titleFld.getText().trim());
            a.setSubject(subjectFld.getText().trim());
            a.setDescription(descFld.getText().trim());
            a.setDueDate(dateDp.getValue());
            a.setPriority(priorityCb.getValue());
            a.setStatus(statusCb.getValue());
            return a;
        });

        dlg.showAndWait().ifPresent(a -> {
            if (existing == null) db.saveAssignment(a);
            else db.updateAssignment(a);
            refreshTable();
            // In a more complex app, we might also want to refresh the stats row here.
        });
    }
}
