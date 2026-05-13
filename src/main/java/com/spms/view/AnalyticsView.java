package com.spms.view;

import com.spms.service.AnalyticsService;
import com.spms.service.GamificationService;
import com.spms.util.UIFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

import java.util.List;
import java.util.Map;

/**
 * Productivity Analytics: study hours chart, subject distribution, badges, insights.
 */
public class AnalyticsView {

    private final AnalyticsService   analytics = AnalyticsService.getInstance();
    private final GamificationService gamify   = GamificationService.getInstance();

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
        HBox h = new HBox();
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("📊  Analytics");
        title.getStyleClass().add("page-title");
        h.getChildren().add(title);
        return h;
    }

    private Node buildBody() {
        VBox body = new VBox(22);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(
                buildSummaryRow(),
                buildChartsRow(),
                buildInsightsAndBadges());
        return body;
    }

    // ── Summary row ──────────────────────────────────────────────────────────

    private Node buildSummaryRow() {
        double weekHours   = analytics.getWeekStudyHours();
        double compRate    = analytics.getTaskCompletionRate();
        int    streak      = analytics.getStreakDays();
        double score       = analytics.getProductivityScore();

        HBox row = new HBox(16);
        VBox c1 = UIFactory.createStatCard(String.format("%.1fh", weekHours),
                "Hours This Week", "📖", "stat-blue");
        VBox c2 = UIFactory.createStatCard(String.format("%.0f%%", compRate),
                "Task Completion Rate", "✅", "stat-green");
        VBox c3 = UIFactory.createStatCard(streak + " days",
                "Current Streak", "🔥", "stat-orange");
        VBox c4 = UIFactory.createStatCard(String.format("%.0f", score),
                "Productivity Score", "⚡", "stat-purple");
        for (VBox c : new VBox[]{c1, c2, c3, c4}) HBox.setHgrow(c, Priority.ALWAYS);
        row.getChildren().addAll(c1, c2, c3, c4);
        return row;
    }

    // ── Charts row ────────────────────────────────────────────────────────────

    private Node buildChartsRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        VBox weeklyCard = buildWeeklyChart();
        VBox subjectCard = buildSubjectChart();
        HBox.setHgrow(weeklyCard, Priority.ALWAYS);
        HBox.setHgrow(subjectCard, Priority.ALWAYS);
        row.getChildren().addAll(weeklyCard, subjectCard);
        return row;
    }

    private VBox buildWeeklyChart() {
        Map<String, Double> data = analytics.getWeeklyStudyHours();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Day of week");
        yAxis.setLabel("Hours");
        yAxis.setMinorTickVisible(false);
        yAxis.setForceZeroInRange(true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(260);
        chart.setBarGap(4);
        chart.setCategoryGap(14);
        chart.getStyleClass().add("spms-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((day, hours) -> series.getData().add(
                new XYChart.Data<>(day, Math.round(hours * 10.0) / 10.0)));
        chart.getData().add(series);

        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                Tooltip t = new Tooltip(d.getYValue() + " hours");
                Tooltip.install(d.getNode(), t);
                d.getNode().setStyle("-fx-cursor: hand;");
            }
        }

        return UIFactory.createCard("📅  Weekly Study Hours", chart);
    }

    private VBox buildSubjectChart() {
        Map<String, Double> data = analytics.getSubjectDistribution();

        if (data.isEmpty()) {
            VBox empty = UIFactory.createCard("📚  Subject Distribution",
                    UIFactory.createEmptyState("📚", "No data yet",
                            "Complete study sessions to see subject distribution."));
            return empty;
        }

        PieChart chart = new PieChart();
        chart.setAnimated(true);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);
        chart.getStyleClass().add("spms-chart");

        data.forEach((subj, hours) -> {
            PieChart.Data slice = new PieChart.Data(subj, Math.round(hours * 10.0) / 10.0);
            chart.getData().add(slice);
        });

        for (PieChart.Data d : chart.getData()) {
            if (d.getNode() != null) {
                Tooltip t = new Tooltip(d.getName() + "\n" + d.getPieValue() + " hours");
                Tooltip.install(d.getNode(), t);
                d.getNode().setStyle("-fx-cursor: hand;");
            }
        }

        return UIFactory.createCard("📚  Study Hours by Subject", chart);
    }

    // ── Insights + Badges ─────────────────────────────────────────────────────

    private Node buildInsightsAndBadges() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        VBox leftCol = new VBox(20);
        leftCol.getChildren().addAll(buildInsightsCard(), buildWeakSubjectsCard());
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox badgesCard = buildBadgesCard();
        badgesCard.setPrefWidth(360);
        badgesCard.setMinWidth(320);

        row.getChildren().addAll(leftCol, badgesCard);
        return row;
    }

    private VBox buildInsightsCard() {
        double score = analytics.getProductivityScore();
        String insight = analytics.getInsightMessage(score);

        VBox content = new VBox(16);
        content.setAlignment(Pos.TOP_LEFT);

        // Score ring
        Node ring = buildScoreRing(score);
        HBox ringWrapper = new HBox(ring);
        ringWrapper.setAlignment(Pos.CENTER);

        Label insightLbl = new Label(insight);
        insightLbl.getStyleClass().add("insight-text");
        insightLbl.setWrapText(true);
        insightLbl.setMaxWidth(Double.MAX_VALUE);

        // Stats row inside insights
        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label t1 = new Label("Today: " + String.format("%.1f h", analytics.getTodayStudyHours()));
        t1.getStyleClass().add("insight-stat");
        Label t2 = new Label("Tasks done: " + analytics.getCompletedTasksCount());
        t2.getStyleClass().add("insight-stat");
        Label t3 = new Label("Active goals: " + analytics.getActiveGoalsCount());
        t3.getStyleClass().add("insight-stat");
        stats.getChildren().addAll(t1, t2, t3);

        content.getChildren().addAll(ringWrapper, insightLbl, UIFactory.hSeparator(), stats);
        return UIFactory.createCard("💡  Performance Insights", content);
    }

    /**
     * Builds the productivity-score ring using absolute coordinates so the
     * arc's geometric centre is guaranteed to align with the background
     * circle's centre — StackPane positioning of arcs is unreliable because
     * the arc's bounding box changes with its length.
     */
    private Node buildScoreRing(double score) {
        final double size = 140;
        final double cx   = size / 2;
        final double cy   = size / 2;
        final double r    = 56;

        Pane pane = new Pane();
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);

        Circle bg = new Circle(cx, cy, r);
        bg.setFill(Color.TRANSPARENT);
        bg.setStroke(Color.web("#353655"));
        bg.setStrokeWidth(10);

        double pct = Math.min(100.0, Math.max(0.0, score));
        Arc arc = new Arc(cx, cy, r, r, 90, -(pct / 100.0 * 360));
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        String arcColor = score >= 80 ? UIFactory.C_SUCCESS
                : score >= 60 ? UIFactory.C_PRIMARY
                : score >= 40 ? UIFactory.C_WARNING : UIFactory.C_DANGER;
        arc.setStroke(Color.web(arcColor));
        arc.setStrokeWidth(10);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);

        Label scoreLbl = new Label(String.format("%.0f", score));
        scoreLbl.getStyleClass().add("score-big-label");
        Label pctLbl   = new Label("/100");
        pctLbl.getStyleClass().add("text-muted-sm");

        VBox center = new VBox(0, scoreLbl, pctLbl);
        center.setAlignment(Pos.CENTER);
        center.setPrefSize(size, size);
        center.setMinSize(size, size);
        center.setMaxSize(size, size);
        center.setLayoutX(0);
        center.setLayoutY(0);
        center.setMouseTransparent(true);

        pane.getChildren().addAll(bg, arc, center);
        return pane;
    }

    private VBox buildWeakSubjectsCard() {
        List<String> weak = analytics.getWeakSubjects();

        VBox content = new VBox(10);
        if (weak.isEmpty()) {
            Label ok = new Label("✅ No weak subjects detected!\nKeep it up.");
            ok.getStyleClass().add("text-success");
            ok.setWrapText(true);
            content.getChildren().add(ok);
        } else {
            Label hdr = new Label("These subjects need more attention:");
            hdr.getStyleClass().add("text-muted");
            hdr.setWrapText(true);
            content.getChildren().add(hdr);
            for (String subj : weak) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label dot = new Label("⚠");
                dot.setStyle("-fx-text-fill: " + UIFactory.C_WARNING + ";");
                Label lbl = new Label(subj);
                lbl.getStyleClass().add("weak-subject-label");
                row.getChildren().addAll(dot, lbl);
                content.getChildren().add(row);
            }
            Label rec = new Label("💡 Schedule extra study sessions for these subjects.");
            rec.getStyleClass().add("text-muted-sm");
            rec.setWrapText(true);
            content.getChildren().add(rec);
        }
        return UIFactory.createCard("⚠  Subject Watch", content);
    }

    private VBox buildBadgesCard() {
        List<GamificationService.Badge> badges = gamify.getAllBadges();
        long earned = badges.stream().filter(GamificationService.Badge::earned).count();

        VBox content = new VBox(8);
        Label summary = new Label("Earned: " + earned + " / " + badges.size());
        summary.getStyleClass().add("text-muted");
        content.getChildren().add(summary);
        content.getChildren().add(UIFactory.hSeparator());

        for (GamificationService.Badge b : badges) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 4, 6, 4));
            row.getStyleClass().add(b.earned() ? "badge-row-earned" : "badge-row-locked");

            Label emojiLbl = new Label(b.emoji());
            emojiLbl.setStyle("-fx-font-size: 22px;" + (b.earned() ? "" : "-fx-opacity:0.35;"));

            VBox info = new VBox(2);
            Label nameLbl = new Label(b.name());
            nameLbl.getStyleClass().add(b.earned() ? "badge-name-earned" : "badge-name-locked");
            Label descLbl = new Label(b.description());
            descLbl.getStyleClass().add("text-dim");
            descLbl.setWrapText(true);
            info.getChildren().addAll(nameLbl, descLbl);

            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label earnedMark = new Label(b.earned() ? "✓" : "🔒");
            earnedMark.setStyle("-fx-font-size: 16px; -fx-text-fill: "
                    + (b.earned() ? UIFactory.C_SUCCESS : UIFactory.C_TEXT_MUTED) + ";");

            row.getChildren().addAll(emojiLbl, info, sp, earnedMark);
            content.getChildren().add(row);
        }
        return UIFactory.createCard("🏆  Achievements", content);
    }
}
