package com.spms.view;

import com.spms.model.StudySession;
import com.spms.service.DatabaseService;
import com.spms.service.GamificationService;
import com.spms.service.TimerService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pomodoro Focus Timer with mood check, session log, and gamification.
 */
public class FocusTimerView {

    private final TimerService       timer   = new TimerService();
    private final DatabaseService    db      = DatabaseService.getInstance();
    private final GamificationService gamify = GamificationService.getInstance();

    // UI refs
    private Arc    progressArc;
    private Label  timeLabel;
    private Label  sessionLabel;
    private Label  modeLabel;
    private Label  motivationLabel;
    private Button startPauseBtn;
    private Button resetBtn;
    private Button skipBtn;
    private ComboBox<String> subjectCb;
    private Label  sessionCountLabel;

    private ObservableList<StudySession> sessionHistory = FXCollections.observableArrayList();
    private LocalDateTime sessionStart;
    private boolean moodSelected = false;
    private String  selectedMood = "🙂 Good";

    public Node build() {
        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");
        page.getChildren().add(buildHeader());

        ScrollPane scroll = UIFactory.wrappedScrollPane(buildBody());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);

        setupTimerCallbacks();
        refreshSessionHistory();
        return page;
    }

    private Node buildHeader() {
        HBox h = new HBox();
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("⏱  Focus Timer");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button settingsBtn = UIFactory.createSecondaryButton("⚙ Settings");
        settingsBtn.setOnAction(e -> openSettingsDialog());
        h.getChildren().addAll(title, sp, settingsBtn);
        return h;
    }

    private Node buildBody() {
        HBox body = new HBox(24);
        body.setPadding(new Insets(24, 28, 28, 28));
        body.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = new VBox(20);
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setPrefWidth(480);
        leftCol.setMinWidth(440);
        leftCol.setMaxWidth(520);

        VBox rightCol = new VBox(20);
        rightCol.setMinWidth(320);
        rightCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        leftCol.getChildren().addAll(buildMoodSection(), buildTimerCircle(), buildControls());
        rightCol.getChildren().addAll(buildSubjectCard(), buildMotivationCard(), buildSessionHistoryCard());

        body.getChildren().addAll(leftCol, rightCol);
        return body;
    }

    // ── Mood selector ─────────────────────────────────────────────────────────

    private Node buildMoodSection() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER);

        Label lbl = UIFactory.createSectionTitle("How are you feeling today?");
        lbl.setAlignment(Pos.CENTER);

        FlowPane moods = new FlowPane(8, 8);
        moods.setAlignment(Pos.CENTER);
        String[] options = {"😄 Great", "🙂 Good", "😐 Okay", "😔 Tired", "😤 Stressed"};
        ToggleGroup tg = new ToggleGroup();

        for (String m : options) {
            ToggleButton tb = new ToggleButton(m);
            tb.setToggleGroup(tg);
            tb.getStyleClass().add("mood-button");
            if (m.equals(selectedMood)) { tb.setSelected(true); moodSelected = true; }
            tb.selectedProperty().addListener((obs, old, sel) -> {
                if (sel) { selectedMood = m; moodSelected = true; }
            });
            moods.getChildren().add(tb);
        }
        card.getChildren().addAll(lbl, moods);
        return card;
    }

    // ── Timer circle ──────────────────────────────────────────────────────────

    private Node buildTimerCircle() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER);

        // Circular progress ring
        Circle bgCircle = new Circle(110, Color.TRANSPARENT);
        bgCircle.setStroke(Color.web("#353655"));
        bgCircle.setStrokeWidth(12);

        progressArc = new Arc(0, 0, 110, 110, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web(UIFactory.C_PRIMARY));
        progressArc.setStrokeWidth(12);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);

        timeLabel = new Label("25:00");
        timeLabel.getStyleClass().add("timer-time-label");

        modeLabel = new Label("FOCUS");
        modeLabel.getStyleClass().add("timer-mode-label");

        VBox centerContent = new VBox(4, timeLabel, modeLabel);
        centerContent.setAlignment(Pos.CENTER);

        StackPane ringPane = new StackPane(bgCircle, progressArc, centerContent);
        ringPane.setPrefSize(260, 260);
        ringPane.setMinSize(240, 240);

        sessionCountLabel = new Label("Session " + ((timer.getSessionsCompleted() % 4) + 1) + " of 4");
        sessionCountLabel.getStyleClass().add("session-count-label");

        card.getChildren().addAll(ringPane, sessionCountLabel);
        return card;
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    private Node buildControls() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(0, 0, 4, 0));

        startPauseBtn = UIFactory.createPrimaryButton("▶  Start");
        startPauseBtn.setPrefWidth(130);
        startPauseBtn.setStyle(startPauseBtn.getStyle() + "-fx-font-size:14px;");

        resetBtn = UIFactory.createSecondaryButton("↺  Reset");
        resetBtn.setPrefWidth(100);
        resetBtn.setOnAction(e -> resetTimer());

        skipBtn = UIFactory.createSecondaryButton("⏭  Skip");
        skipBtn.setPrefWidth(100);
        skipBtn.setOnAction(e -> { timer.skipBreak(); updateUI(); });

        startPauseBtn.setOnAction(e -> toggleTimer());
        row.getChildren().addAll(startPauseBtn, resetBtn, skipBtn);
        return row;
    }

    // ── Subject card ─────────────────────────────────────────────────────────

    private Node buildSubjectCard() {
        subjectCb = new ComboBox<>(FXCollections.observableArrayList(
                "Mathematics", "Physics", "Computer Science", "History",
                "English Literature", "Chemistry", "Biology", "Other"));
        subjectCb.setEditable(true);
        subjectCb.getStyleClass().add("combo-field");
        subjectCb.setMaxWidth(Double.MAX_VALUE);
        subjectCb.getSelectionModel().select(0);

        VBox card = UIFactory.createCard("📚  Study Subject", subjectCb);
        card.getStyleClass().add("card");
        return card;
    }

    // ── Motivation card ───────────────────────────────────────────────────────

    private Node buildMotivationCard() {
        motivationLabel = new Label(gamify.getMotivationalMessage(timer.getSessionsCompleted()));
        motivationLabel.getStyleClass().add("motivation-label");
        motivationLabel.setWrapText(true);
        motivationLabel.setMaxWidth(Double.MAX_VALUE);
        motivationLabel.setAlignment(Pos.CENTER_LEFT);

        VBox card = UIFactory.createCard("💡  Motivation", motivationLabel);
        card.getStyleClass().add("card");
        return card;
    }

    // ── Session history ───────────────────────────────────────────────────────

    private Node buildSessionHistoryCard() {
        TableView<StudySession> table = new TableView<>(sessionHistory);
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(200);

        TableColumn<StudySession, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getSubject()));
        subjCol.setPrefWidth(160);

        TableColumn<StudySession, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatDuration(d.getValue().getDurationMinutes())));
        durCol.setPrefWidth(90);

        TableColumn<StudySession, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.formatDate(d.getValue().getSessionDate())));
        dateCol.setPrefWidth(130);

        table.getColumns().addAll(subjCol, durCol, dateCol);

        VBox card = UIFactory.createCard("📋  Session History (Today)", table);
        card.getStyleClass().add("card");
        return card;
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private void setupTimerCallbacks() {
        timer.setOnTick(remaining -> updateUI());
        timer.setOnSessionComplete(() -> {
            sessionCompleted();
            updateUI();
        });
        timer.setOnBreakComplete(() -> {
            modeLabel.setText("FOCUS");
            progressArc.setStroke(Color.web(UIFactory.C_PRIMARY));
            updateUI();
        });
    }

    private void toggleTimer() {
        if (!moodSelected) {
            UIFactory.showInfo("Set Your Mood",
                    "Please select your mood first — it helps us suggest the right study intensity.");
            return;
        }
        if (!timer.isRunning()) {
            if (!timer.isOnBreak()) {
                sessionStart = LocalDateTime.now();
                String subject = subjectCb.getValue();
                timer.setCurrentSubject(subject != null ? subject : "General");
                showMoodSuggestion();
            }
            timer.start();
            startPauseBtn.setText("⏸  Pause");
        } else {
            timer.pause();
            startPauseBtn.setText("▶  Resume");
        }
    }

    private void resetTimer() {
        timer.reset();
        startPauseBtn.setText("▶  Start");
        modeLabel.setText("FOCUS");
        progressArc.setStroke(Color.web(UIFactory.C_PRIMARY));
        timeLabel.setText("25:00");
        progressArc.setLength(0);
    }

    private void sessionCompleted() {
        int durationMins = timer.getFocusDurationMinutes();
        String subject   = timer.getCurrentSubject();

        StudySession session = new StudySession(subject, durationMins,
                "Focus session", true);
        session.setStartTime(sessionStart != null ? sessionStart : LocalDateTime.now().minusMinutes(durationMins));
        session.setEndTime(LocalDateTime.now());
        session.setSessionDate(LocalDate.now());
        db.saveStudySession(session);

        gamify.updateStreakAfterSession();
        gamify.addStudyMinutesToProfile(durationMins);

        sessionHistory.add(0, session);

        motivationLabel.setText(gamify.getMotivationalMessage(timer.getSessionsCompleted()));
        sessionCountLabel.setText("Session " + ((timer.getSessionsCompleted() % 4) + 1) + " of 4");

        modeLabel.setText("BREAK ☕");
        progressArc.setStroke(Color.web(UIFactory.C_SUCCESS));
        startPauseBtn.setText("▶  Start Break");

        UIFactory.showInfo("Session Complete! 🎉",
                "Great work! " + durationMins + " minutes of focused study complete.\n"
                + gamify.getMotivationalMessage(timer.getSessionsCompleted()));
    }

    private void updateUI() {
        timeLabel.setText(timer.getFormattedTime());
        double progress = timer.getProgress();
        progressArc.setLength(-(progress * 360));
        if (timer.isOnBreak()) {
            modeLabel.setText("BREAK ☕");
            progressArc.setStroke(Color.web(UIFactory.C_SUCCESS));
        } else {
            modeLabel.setText("FOCUS");
        }
    }

    private void refreshSessionHistory() {
        sessionHistory.setAll(
                db.getAllSessions().stream()
                  .filter(s -> LocalDate.now().equals(s.getSessionDate()))
                  .toList());
    }

    private void showMoodSuggestion() {
        String suggestion = gamify.getMoodSuggestion(selectedMood);
        UIFactory.showInfo("Ready to Study? " + selectedMood.split(" ")[0], suggestion);
    }

    // ── Settings dialog ───────────────────────────────────────────────────────

    private void openSettingsDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Timer Settings");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1b2e; -fx-border-color: #353655;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        Spinner<Integer> focusSp = new Spinner<>(1, 60, timer.getFocusDurationMinutes());
        Spinner<Integer> shortSp = new Spinner<>(1, 30, timer.getShortBreakMinutes());
        Spinner<Integer> longSp  = new Spinner<>(5, 60, timer.getLongBreakMinutes());
        focusSp.setEditable(true); shortSp.setEditable(true); longSp.setEditable(true);

        grid.add(UIFactory.createFormLabel("Focus Duration (min)"),       0, 0); grid.add(focusSp, 1, 0);
        grid.add(UIFactory.createFormLabel("Short Break (min)"),          0, 1); grid.add(shortSp, 1, 1);
        grid.add(UIFactory.createFormLabel("Long Break after 4 sessions"),0, 2); grid.add(longSp,  1, 2);

        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                timer.reset();
                timer.setFocusDurationMinutes(focusSp.getValue());
                timer.setShortBreakMinutes(shortSp.getValue());
                timer.setLongBreakMinutes(longSp.getValue());
                startPauseBtn.setText("▶  Start");
                timeLabel.setText(String.format("%02d:00", focusSp.getValue()));
                progressArc.setLength(0);
            }
            return null;
        });
        dlg.showAndWait();
    }
}
