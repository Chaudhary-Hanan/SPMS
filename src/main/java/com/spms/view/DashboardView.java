package com.spms.view;

import com.spms.model.*;
import com.spms.service.AnalyticsService;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Main dashboard: greeting, stats, deadline list, exam countdown, weekly chart.
 */
public class DashboardView {

    private final DatabaseService   db      = DatabaseService.getInstance();
    private final AnalyticsService  analytics = AnalyticsService.getInstance();

    private Label    countdownLabel;
    private Exam     nextExam;
    private Timeline countdownTimeline;

    public Node build() {
        // Stop any prior countdown to avoid duplicate timelines on re-navigation
        if (countdownTimeline != null) countdownTimeline.stop();
        countdownLabel = null;
        nextExam       = null;

        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");

        page.getChildren().add(buildHeader());

        ScrollPane scroll = UIFactory.wrappedScrollPane(buildBody());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().add(scroll);

        startCountdownTimer();
        return page;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private Node buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 28, 22, 28));

        UserProfile profile = db.getUserProfile();
        VBox greetBox = new VBox(3);
        Label greetLbl = new Label(profile.getGreeting() + ", " + profile.getName() + "! ✨");
        greetLbl.getStyleClass().add("greeting-text");
        Label dateLbl  = new Label(DateUtil.formatHeaderDate());
        dateLbl.getStyleClass().add("date-text");
        greetBox.getChildren().addAll(greetLbl, dateLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick insight chip
        double score = analytics.getProductivityScore();
        Label scoreChip = new Label("⚡ Productivity: " + String.format("%.0f", score) + "%");
        scoreChip.getStyleClass().addAll("badge", score >= 60 ? "badge-success" : "badge-warning");
        scoreChip.setPadding(new Insets(6, 14, 6, 14));
        scoreChip.setStyle("-fx-font-size: 13px;");

        header.getChildren().addAll(greetBox, spacer, scoreChip);
        return header;
    }

    // ── Body ─────────────────────────────────────────────────────────────────

    private Node buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 28, 28, 28));

        // Stat cards row
        body.getChildren().add(buildStatsRow());

        // Main content: left + right columns
        HBox columns = new HBox(20);
        columns.setFillHeight(true);

        VBox leftCol  = new VBox(20);
        leftCol.setMinWidth(400);
        leftCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = new VBox(20);
        rightCol.setPrefWidth(370);
        rightCol.setMinWidth(340);
        rightCol.setMaxWidth(400);

        leftCol.getChildren().addAll(buildDeadlinesCard(), buildWeeklyChart());
        rightCol.getChildren().addAll(buildExamCountdown(), buildRemindersCard());

        columns.getChildren().addAll(leftCol, rightCol);
        body.getChildren().add(columns);
        return body;
    }

    // ── Stat Cards ────────────────────────────────────────────────────────────

    private Node buildStatsRow() {
        double todayHours  = analytics.getTodayStudyHours();
        int    completed   = analytics.getCompletedTasksCount();
        int    activeGoals = analytics.getActiveGoalsCount();
        int    streak      = analytics.getStreakDays();

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox c1 = UIFactory.createStatCard(String.format("%.1f h", todayHours),
                "Today's Study Time", "📖", "stat-blue");
        VBox c2 = UIFactory.createStatCard(String.valueOf(completed),
                "Tasks Completed", "✅", "stat-green");
        VBox c3 = UIFactory.createStatCard(String.valueOf(activeGoals),
                "Active Goals", "🎯", "stat-purple");
        VBox c4 = UIFactory.createStatCard(streak + " days",
                "Study Streak", "🔥", "stat-orange");

        for (VBox c : new VBox[]{c1, c2, c3, c4}) {
            HBox.setHgrow(c, Priority.ALWAYS);
        }
        row.getChildren().addAll(c1, c2, c3, c4);
        return row;
    }

    // ── Upcoming Deadlines ───────────────────────────────────────────────────

    private Node buildDeadlinesCard() {
        List<Assignment> upcoming = db.getUpcomingAssignments(14);

        VBox cardContent = new VBox(0);
        if (upcoming.isEmpty()) {
            cardContent.getChildren().add(
                    UIFactory.createEmptyState("🎉", "All caught up!",
                            "No assignments due in the next 14 days."));
        } else {
            for (Assignment a : upcoming) {
                cardContent.getChildren().add(buildDeadlineRow(a));
                cardContent.getChildren().add(UIFactory.hSeparator());
            }
            if (!cardContent.getChildren().isEmpty()) {
                cardContent.getChildren().remove(cardContent.getChildren().size() - 1);
            }
        }

        VBox card = UIFactory.createCard("📌  Upcoming Deadlines", cardContent);
        card.getStyleClass().add("dashboard-card");
        return card;
    }

    private HBox buildDeadlineRow(Assignment a) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 6, 10, 6));

        // Priority stripe
        Rectangle stripe = new Rectangle(4, 36, Color.web(
                a.isOverdue() ? UIFactory.C_DANGER
                : switch (a.getPriority()) {
                    case LOW    -> UIFactory.C_SUCCESS;
                    case MEDIUM -> UIFactory.C_SECONDARY;
                    case HIGH   -> UIFactory.C_WARNING;
                    case URGENT -> UIFactory.C_DANGER;
                }));
        stripe.setArcWidth(4); stripe.setArcHeight(4);

        VBox info = new VBox(3);
        info.setMinWidth(120);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title   = new Label(a.getTitle());
        title.getStyleClass().add("deadline-title");
        title.setMaxWidth(Double.MAX_VALUE);
        Label subject = new Label(a.getSubject() != null ? a.getSubject() : "");
        subject.getStyleClass().add("deadline-subject");
        subject.setMaxWidth(Double.MAX_VALUE);
        info.getChildren().addAll(title, subject);

        VBox dateBox = new VBox(3);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        dateBox.setMinWidth(90);
        String rel = DateUtil.getRelativeDate(a.getDueDate());
        Label relLbl = new Label(rel);
        relLbl.getStyleClass().add(a.isOverdue() ? "overdue-label" : "due-label");
        Label dateLbl = new Label(DateUtil.formatDateShort(a.getDueDate()));
        dateLbl.getStyleClass().add("date-dim-label");
        dateBox.getChildren().addAll(relLbl, dateLbl);

        Label priorityBadge = UIFactory.createBadgeLabel(
                a.getPriority().name(), UIFactory.priorityBadgeClass(a.getPriority()));

        row.getChildren().addAll(stripe, info, priorityBadge, dateBox);
        return row;
    }

    // ── Exam Countdown ────────────────────────────────────────────────────────

    private Node buildExamCountdown() {
        nextExam = db.getNextExam();

        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "dashboard-card", "exam-countdown-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);

        if (nextExam == null) {
            card.getChildren().add(
                    UIFactory.createEmptyState("📋", "No upcoming exams",
                            "Add your exam schedule to see countdowns."));
        } else {
            Label headerLbl = new Label("📋  Next Exam");
            headerLbl.getStyleClass().add("card-title");
            headerLbl.setAlignment(Pos.CENTER_LEFT);
            headerLbl.setMaxWidth(Double.MAX_VALUE);

            Label subjectLbl = new Label(nextExam.getSubject());
            subjectLbl.getStyleClass().add("exam-subject");
            subjectLbl.setWrapText(true);

            Label dateLbl = new Label(DateUtil.formatDate(nextExam.getExamDate())
                    + (nextExam.getExamTime() != null
                       ? "  •  " + DateUtil.formatTime(nextExam.getExamTime()) : ""));
            dateLbl.getStyleClass().add("exam-date");

            countdownLabel = new Label();
            countdownLabel.getStyleClass().add("countdown-label");
            updateCountdown();

            Label locationLbl = new Label(nextExam.getLocation() != null
                    ? "📍 " + nextExam.getLocation() : "");
            locationLbl.getStyleClass().add("exam-location");

            card.getChildren().addAll(headerLbl, subjectLbl, dateLbl, countdownLabel, locationLbl);
        }
        return card;
    }

    // ── Active Reminders ──────────────────────────────────────────────────────

    private Node buildRemindersCard() {
        List<Reminder> reminders = db.getActiveReminders();
        if (reminders.size() > 4) reminders = reminders.subList(0, 4);

        VBox cardContent = new VBox(8);
        if (reminders.isEmpty()) {
            Label ok = new Label("No active reminders 🎉");
            ok.getStyleClass().add("text-muted");
            ok.setPadding(new Insets(12));
            cardContent.getChildren().add(ok);
        } else {
            for (Reminder r : reminders) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("reminder-row");
                row.setPadding(new Insets(8, 10, 8, 10));

                Rectangle dot = new Rectangle(8, 8, Color.web(UIFactory.urgencyColor(r.getUrgency())));
                dot.setArcWidth(8); dot.setArcHeight(8);

                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label title = new Label(r.getTitle());
                title.getStyleClass().add("reminder-title");
                Label time  = new Label(r.getTimeUntilDue());
                time.getStyleClass().add("text-muted-sm");
                info.getChildren().addAll(title, time);

                row.getChildren().addAll(dot, info);
                cardContent.getChildren().add(row);
            }
        }

        VBox card = UIFactory.createCard("🔔  Active Reminders", cardContent);
        card.getStyleClass().add("dashboard-card");
        return card;
    }

    // ── Weekly Study Chart ────────────────────────────────────────────────────

    private Node buildWeeklyChart() {
        Map<String, Double> weekData = analytics.getWeeklyStudyHours();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("");
        yAxis.setLabel("Hours");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        chart.setBarGap(4);
        chart.setCategoryGap(16);
        chart.getStyleClass().add("spms-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        weekData.forEach((day, hours) -> series.getData().add(
                new XYChart.Data<>(day, Math.round(hours * 10.0) / 10.0)));
        chart.getData().add(series);

        VBox card = UIFactory.createCard("📊  Study Hours This Week", chart);
        card.getStyleClass().add("dashboard-card");
        return card;
    }

    // ── Countdown timer ───────────────────────────────────────────────────────

    private void startCountdownTimer() {
        if (nextExam == null || countdownLabel == null) return;
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown() {
        if (countdownLabel == null || nextExam == null) return;
        LocalDateTime target = nextExam.getExamDateTime();
        countdownLabel.setText(DateUtil.formatCountdown(target));
    }
}
