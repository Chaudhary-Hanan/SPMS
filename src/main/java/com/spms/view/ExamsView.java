package com.spms.view;

import com.spms.model.Exam;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exam Countdown module: view upcoming exams with live timers, add/edit/delete.
 */
public class ExamsView {

    private final DatabaseService db = DatabaseService.getInstance();
    private VBox     bodyContainer;
    private Timeline countdownTimeline;

    public Node build() {
        // Stop any prior countdown if view is rebuilt
        if (countdownTimeline != null) countdownTimeline.stop();

        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");
        page.getChildren().add(buildHeader());

        bodyContainer = new VBox(24);
        bodyContainer.setPadding(new Insets(20, 28, 28, 28));
        rebuildBody();

        ScrollPane scroll = UIFactory.wrappedScrollPane(bodyContainer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);
        return page;
    }

    private Node buildHeader() {
        HBox h = new HBox(12);
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("📋  Exam Countdown");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = UIFactory.createPrimaryButton("+ Add Exam");
        addBtn.setOnAction(e -> openExamDialog(null));
        h.getChildren().addAll(title, sp, addBtn);
        return h;
    }

    /** Rebuilds the next-exam panel and the table. Called after any data change. */
    private void rebuildBody() {
        if (countdownTimeline != null) { countdownTimeline.stop(); countdownTimeline = null; }
        bodyContainer.getChildren().setAll(buildNextExamPanel(), buildExamTable());
    }

    // ── Big countdown ─────────────────────────────────────────────────────────

    private VBox buildNextExamPanel() {
        Exam next = db.getNextExam();
        VBox panel = new VBox(14);
        panel.getStyleClass().addAll("card", "next-exam-panel");
        panel.setPadding(new Insets(28));
        panel.setAlignment(Pos.CENTER);

        if (next == null) {
            panel.getChildren().add(
                    UIFactory.createEmptyState("🎉", "No upcoming exams",
                            "All clear! Add your exam schedule to stay prepared."));
            return panel;
        }

        Label nextLabel = new Label("NEXT EXAM");
        nextLabel.getStyleClass().add("next-exam-tag");

        Label subjectLbl = new Label(next.getSubject());
        subjectLbl.getStyleClass().add("next-exam-subject");
        subjectLbl.setWrapText(true);
        subjectLbl.setMaxWidth(640);
        subjectLbl.setAlignment(Pos.CENTER);

        Label dateLbl = new Label(DateUtil.formatDate(next.getExamDate())
                + (next.getExamTime() != null ? "   •   " + DateUtil.formatTime(next.getExamTime()) : ""));
        dateLbl.getStyleClass().add("next-exam-date");

        Label countdownLbl = new Label();
        countdownLbl.getStyleClass().add("next-exam-countdown");
        Runnable updateCd = () -> countdownLbl.setText(
                DateUtil.formatCountdown(next.getExamDateTime()));
        updateCd.run();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCd.run()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();

        long days = next.getDaysUntil();
        Label urgencyLbl = new Label(
                days == 0 ? "📍 TODAY!"
              : days == 1 ? "⚠ TOMORROW!"
              : days <= 3 ? "🔥 In " + days + " days"
                          : "📅 In " + days + " days");
        urgencyLbl.getStyleClass().addAll("badge",
                days <= 1 ? "badge-danger" : days <= 3 ? "badge-warning" : "badge-info");
        urgencyLbl.setStyle("-fx-font-size: 14px; -fx-padding: 6 16 6 16;");

        panel.getChildren().addAll(nextLabel, subjectLbl, dateLbl, countdownLbl, urgencyLbl);

        if (next.getLocation() != null && !next.getLocation().isBlank()) {
            Label locLbl = new Label("📍 " + next.getLocation());
            locLbl.getStyleClass().add("next-exam-location");
            panel.getChildren().add(locLbl);
        }
        return panel;
    }

    // ── Exam table ────────────────────────────────────────────────────────────

    private Node buildExamTable() {
        List<Exam> exams = db.getAllExams();

        TableView<Exam> table = new TableView<>(FXCollections.observableArrayList(exams));
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(340);
        if (exams.isEmpty()) {
            table.setPlaceholder(new Label("No exams scheduled yet."));
        }

        TableColumn<Exam, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getSubject()));
        subjectCol.setPrefWidth(200);

        TableColumn<Exam, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatDate(d.getValue().getExamDate())));
        dateCol.setPrefWidth(130);

        TableColumn<Exam, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getExamTime() != null ? DateUtil.formatTime(d.getValue().getExamTime()) : "--"));
        timeCol.setPrefWidth(90);

        TableColumn<Exam, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getLocation() != null ? d.getValue().getLocation() : "--"));
        locationCol.setPrefWidth(160);

        TableColumn<Exam, String> countdownCol = new TableColumn<>("Time Remaining");
        countdownCol.setCellValueFactory(d -> {
            long days = d.getValue().getDaysUntil();
            String val = d.getValue().isPassed() ? "Passed" : days + " day" + (days == 1 ? "" : "s");
            return new javafx.beans.property.SimpleStringProperty(val);
        });
        countdownCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null); return;
                }
                Exam exam = getTableRow().getItem();
                long days = exam.getDaysUntil();
                String cls = exam.isPassed() ? "badge-secondary"
                        : days <= 1 ? "badge-danger"
                        : days <= 7 ? "badge-warning" : "badge-info";
                setGraphic(UIFactory.createBadgeLabel(item, cls));
                setText(null);
            }
        });
        countdownCol.setPrefWidth(130);

        TableColumn<Exam, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Exam exam = (Exam) getTableRow().getItem();
                Button editBtn = UIFactory.createSmallButton("Edit", "btn-secondary");
                Button delBtn  = UIFactory.createSmallButton("Delete", "btn-danger");
                editBtn.setOnAction(e -> openExamDialog(exam));
                delBtn.setOnAction(e  -> {
                    if (UIFactory.confirmDelete(exam.getSubject())) {
                        db.deleteExam(exam.getId());
                        rebuildBody();
                    }
                });
                HBox box = new HBox(6, editBtn, delBtn);
                setGraphic(box);
            }
        });
        actionsCol.setPrefWidth(130);

        table.getColumns().addAll(subjectCol, dateCol, timeCol, locationCol, countdownCol, actionsCol);

        return UIFactory.createCard("📋  All Exams", table);
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────

    private void openExamDialog(Exam existing) {
        Dialog<Exam> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Add Exam" : "Edit Exam");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1b2e; -fx-border-color: #353655;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField subjectFld  = UIFactory.createTextField("Subject name *");
        DatePicker dateDp     = UIFactory.createDatePicker();
        dateDp.setValue(LocalDate.now().plusDays(14));
        TextField timeFld     = UIFactory.createTextField("HH:MM (e.g. 09:00)");
        timeFld.setText("09:00");
        TextField locationFld = UIFactory.createTextField("Room / Hall (optional)");
        TextArea  notesFld    = UIFactory.createTextArea("Exam notes (optional)", 3);

        if (existing != null) {
            subjectFld.setText(existing.getSubject());
            dateDp.setValue(existing.getExamDate());
            if (existing.getExamTime() != null)
                timeFld.setText(existing.getExamTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            locationFld.setText(existing.getLocation() != null ? existing.getLocation() : "");
            notesFld.setText(existing.getNotes() != null ? existing.getNotes() : "");
        }

        int row = 0;
        grid.add(UIFactory.createFormLabel("Subject *"),  0, row); grid.add(subjectFld,  1, row++);
        grid.add(UIFactory.createFormLabel("Date *"),     0, row); grid.add(dateDp,      1, row++);
        grid.add(UIFactory.createFormLabel("Time"),       0, row); grid.add(timeFld,     1, row++);
        grid.add(UIFactory.createFormLabel("Location"),   0, row); grid.add(locationFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Notes"),      0, row); grid.add(notesFld,    1, row);
        GridPane.setHgrow(subjectFld, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (subjectFld.getText().isBlank() || dateDp.getValue() == null) {
                UIFactory.showError("Subject and date are required."); return null;
            }
            LocalTime examTime = LocalTime.of(9, 0);
            try {
                examTime = LocalTime.parse(timeFld.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception ignored) {}

            Exam e = existing != null ? existing : new Exam();
            e.setSubject(subjectFld.getText().trim());
            e.setExamDate(dateDp.getValue());
            e.setExamTime(examTime);
            e.setLocation(locationFld.getText().trim());
            e.setNotes(notesFld.getText().trim());
            return e;
        });

        dlg.showAndWait().ifPresent(e -> {
            if (e == null) return;
            if (existing == null) db.saveExam(e);
            else                  db.updateExam(e);
            rebuildBody();
        });
    }
}
