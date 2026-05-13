package com.spms.view;

import com.spms.model.Reminder;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reminders module: create, view urgency-coded reminders, snooze/dismiss/done.
 */
public class RemindersView {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final DatabaseService db = DatabaseService.getInstance();
    private VBox reminderListContainer;
    private String activeFilter = "ACTIVE";

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
        Label title = new Label("🔔  Reminders");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = UIFactory.createPrimaryButton("+ New Reminder");
        addBtn.setOnAction(e -> openReminderDialog(null));
        h.getChildren().addAll(title, sp, addBtn);
        return h;
    }

    private Node buildBody() {
        VBox body = new VBox(18);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(buildStatsRow(), buildFilterTabs(), buildReminderList());
        return body;
    }

    // ── Stats row ────────────────────────────────────────────────────────────

    private Node buildStatsRow() {
        List<Reminder> all = db.getAllReminders();
        long active    = all.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.ACTIVE).count();
        long snoozed   = all.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.SNOOZED).count();
        long overdue   = all.stream().filter(Reminder::isOverdue).count();

        HBox row = new HBox(16);
        VBox c1 = UIFactory.createStatCard(String.valueOf(active),  "Active",   "🔔", "stat-blue");
        VBox c2 = UIFactory.createStatCard(String.valueOf(snoozed), "Snoozed",  "💤", "stat-orange");
        VBox c3 = UIFactory.createStatCard(String.valueOf(overdue), "Overdue",  "⚠",  "stat-red");
        for (VBox c : new VBox[]{c1, c2, c3}) HBox.setHgrow(c, Priority.ALWAYS);
        row.getChildren().addAll(c1, c2, c3);
        return row;
    }

    // ── Filter tabs ──────────────────────────────────────────────────────────

    private Node buildFilterTabs() {
        HBox tabs = new HBox(8);
        ToggleGroup tg = new ToggleGroup();
        String[] filters = {"ACTIVE", "SNOOZED", "DONE", "ALL"};
        for (String f : filters) {
            ToggleButton tb = new ToggleButton(f);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("filter-tab");
            if (f.equals(activeFilter)) tb.setSelected(true);
            tb.setOnAction(e -> { activeFilter = f; refreshList(); });
            tabs.getChildren().add(tb);
        }
        return tabs;
    }

    // ── Reminder list ─────────────────────────────────────────────────────────

    private Node buildReminderList() {
        reminderListContainer = new VBox(12);
        refreshList();
        return reminderListContainer;
    }

    private void refreshList() {
        if (reminderListContainer == null) return;
        List<Reminder> reminders = db.getAllReminders();
        if (!activeFilter.equals("ALL")) {
            Reminder.ReminderStatus statusFilter = Reminder.ReminderStatus.valueOf(activeFilter);
            reminders = reminders.stream()
                    .filter(r -> r.getStatus() == statusFilter)
                    .collect(Collectors.toList());
        }
        reminderListContainer.getChildren().clear();
        if (reminders.isEmpty()) {
            reminderListContainer.getChildren().add(
                    UIFactory.createEmptyState("🔔", "No reminders here",
                            "Create a reminder to stay on top of deadlines and important tasks."));
        } else {
            for (Reminder r : reminders) reminderListContainer.getChildren().add(buildReminderCard(r));
        }
    }

    // ── Reminder card ─────────────────────────────────────────────────────────

    private Node buildReminderCard(Reminder r) {
        HBox card = new HBox(0);
        card.getStyleClass().add("reminder-card");
        card.setMinHeight(80);

        // Urgency colour stripe
        Color stripeColor = Color.web(UIFactory.urgencyColor(r.getUrgency()));
        Rectangle stripe = new Rectangle(5, 80, stripeColor);
        stripe.setArcWidth(5); stripe.setArcHeight(5);

        // Content
        VBox content = new VBox(6);
        content.setPadding(new Insets(12, 14, 12, 14));
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl   = new Label(r.getTitle());
        titleLbl.getStyleClass().add("reminder-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label urgencyBadge = UIFactory.createBadgeLabel(r.getUrgency().name(),
                UIFactory.urgencyBadgeClass(r.getUrgency()));
        Label statusBadge  = UIFactory.createBadgeLabel(r.getStatus().name(), "badge-secondary");
        topRow.getChildren().addAll(titleLbl, sp, urgencyBadge, statusBadge);

        if (r.getMessage() != null && !r.getMessage().isBlank()) {
            Label msgLbl = new Label(r.getMessage());
            msgLbl.getStyleClass().add("reminder-message");
            msgLbl.setWrapText(true);
            content.getChildren().add(msgLbl);
        }

        // Time info row
        HBox timeRow = new HBox(10);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        Label dueLbl = new Label("⏰ " + DateUtil.formatDateTime(r.getDueDateTime()));
        dueLbl.getStyleClass().add(r.isOverdue() ? "overdue-label" : "text-muted-sm");
        Label timeLbl = new Label(r.getTimeUntilDue());
        timeLbl.getStyleClass().add("text-dim");
        timeRow.getChildren().addAll(dueLbl, timeLbl);

        // Action buttons (separate row prevents overlap with long messages)
        HBox btns = new HBox(6);
        btns.setAlignment(Pos.CENTER_LEFT);
        Button snoozeBtn = UIFactory.createSmallButton("💤 Snooze", "btn-secondary");
        Button doneBtn   = UIFactory.createSmallButton("✅ Done",   "btn-primary");
        Button editBtn   = UIFactory.createSmallButton("Edit",       "btn-secondary");
        Button delBtn    = UIFactory.createSmallButton("Delete",     "btn-danger");

        snoozeBtn.setOnAction(e -> snoozeReminder(r));
        doneBtn.setOnAction(e   -> markDone(r));
        editBtn.setOnAction(e   -> openReminderDialog(r));
        delBtn.setOnAction(e    -> {
            if (UIFactory.confirmDelete(r.getTitle())) { db.deleteReminder(r.getId()); refreshList(); }
        });
        btns.getChildren().addAll(snoozeBtn, doneBtn, editBtn, delBtn);

        // Combined bottom row: time on the left, actions pushed right
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        bottomRow.getChildren().addAll(timeRow, sp2, btns);

        content.getChildren().addAll(topRow, bottomRow);
        card.getChildren().addAll(stripe, content);
        return card;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void snoozeReminder(Reminder r) {
        r.setStatus(Reminder.ReminderStatus.SNOOZED);
        r.setSnoozedUntil(LocalDateTime.now().plusMinutes(30));
        db.updateReminder(r);
        refreshList();
        UIFactory.showInfo("Snoozed", "Reminder snoozed for 30 minutes.");
    }

    private void markDone(Reminder r) {
        r.setStatus(Reminder.ReminderStatus.DONE);
        db.updateReminder(r);
        refreshList();
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────

    private void openReminderDialog(Reminder existing) {
        Dialog<Reminder> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "New Reminder" : "Edit Reminder");
        dlg.setHeaderText(existing == null ? "🔔 Enter new reminder details" : "🔔 Update reminder details");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16); grid.setPadding(new Insets(24));

        TextField titleFld  = UIFactory.createTextField("Reminder title *");
        TextArea  msgFld    = UIFactory.createTextArea("Detailed message (optional)", 3);
        DatePicker dateDp   = UIFactory.createDatePicker();
        dateDp.setValue(java.time.LocalDate.now().plusDays(1));
        TextField timeFld   = UIFactory.createTextField("HH:MM (e.g. 09:00)");
        timeFld.setText("09:00");
        ComboBox<Reminder.Urgency> urgencyCb = new ComboBox<>(
                FXCollections.observableArrayList(Reminder.Urgency.values()));
        urgencyCb.getStyleClass().add("combo-field");
        urgencyCb.setValue(Reminder.Urgency.MEDIUM);

        if (existing != null) {
            titleFld.setText(existing.getTitle());
            msgFld.setText(existing.getMessage());
            if (existing.getDueDateTime() != null) {
                dateDp.setValue(existing.getDueDateTime().toLocalDate());
                timeFld.setText(existing.getDueDateTime().toLocalTime().format(HHMM));
            }
            urgencyCb.setValue(existing.getUrgency());
        }

        int row = 0;
        grid.add(UIFactory.createFormLabel("Title *"),    0, row); grid.add(titleFld,  1, row++);
        grid.add(UIFactory.createFormLabel("Message"),    0, row); grid.add(msgFld,    1, row++);
        grid.add(UIFactory.createFormLabel("Date *"),     0, row); grid.add(dateDp,    1, row++);
        grid.add(UIFactory.createFormLabel("Time (HH:MM)"), 0, row); grid.add(timeFld, 1, row++);
        grid.add(UIFactory.createFormLabel("Urgency"),    0, row); grid.add(urgencyCb, 1, row);
        GridPane.setHgrow(titleFld, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);

        final javafx.scene.control.Button btOk = (javafx.scene.control.Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleFld.getText().isBlank() || dateDp.getValue() == null) {
                UIFactory.showError("Title and date are required.");
                event.consume();
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            LocalDateTime due;
            try {
                due = LocalDateTime.of(dateDp.getValue(),
                        java.time.LocalTime.parse(timeFld.getText().trim(), HHMM));
            } catch (Exception e) {
                due = LocalDateTime.of(dateDp.getValue(), java.time.LocalTime.of(9, 0));
            }
            Reminder rem = existing != null ? existing : new Reminder();
            rem.setTitle(titleFld.getText().trim());
            rem.setMessage(msgFld.getText().trim());
            rem.setDueDateTime(due);
            rem.setUrgency(urgencyCb.getValue());
            if (existing == null) rem.setStatus(Reminder.ReminderStatus.ACTIVE);
            return rem;
        });

        dlg.showAndWait().ifPresent(rem -> {
            if (rem == null) return;
            if (existing == null) db.saveReminder(rem);
            else                  db.updateReminder(rem);
            refreshList();
        });
    }
}
