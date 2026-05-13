package com.spms.view;

import com.spms.service.DatabaseService;
import com.spms.model.UserProfile;
import com.spms.util.UIFactory;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main application window: sidebar + content area.
 * Navigation swaps the centre pane with a fade transition.
 */
public class MainWindow {

    private final Stage       stage;
    private final BorderPane  root      = new BorderPane();
    private final StackPane   content   = new StackPane();
    private       Button      activeNav = null;

    // View instances (lazy init)
    private DashboardView    dashboardView;
    private StudyPlannerView studyPlannerView;
    private FocusTimerView   focusTimerView;
    private GoalsView        goalsView;
    private NotesView        notesView;
    private RemindersView    remindersView;
    private AnalyticsView    analyticsView;
    private ExamsView        examsView;
    private AssignmentsView  assignmentsView;

    private static MainWindow instance;
    public static MainWindow getInstance() { return instance; }

    public MainWindow(Stage stage) {
        instance = this;
        this.stage = stage;
        build();
    }

    public void show() {
        Scene scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(
                getClass().getResource("/com/spms/styles/main.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Student Productivity Management System");
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.show();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        root.setLeft(buildSidebar());
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(0));
        root.setCenter(content);
        navigateDashboard();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);

        // Logo
        VBox logoBox = new VBox(4);
        logoBox.getStyleClass().add("sidebar-logo");
        logoBox.setPadding(new Insets(24, 20, 20, 20));
        Label appName = new Label("SPMS");
        appName.getStyleClass().add("logo-text");
        Label appSub  = new Label("Student Productivity");
        appSub.getStyleClass().add("logo-sub");
        logoBox.getChildren().addAll(appName, appSub);

        // Navigation
        VBox navBox = new VBox(4);
        navBox.setPadding(new Insets(10, 12, 10, 12));

        Label navHeader = new Label("NAVIGATION");
        navHeader.getStyleClass().add("nav-section-header");
        navHeader.setPadding(new Insets(8, 8, 4, 8));

        Button btnDash     = navButton("🏠", "Dashboard",      this::navigateDashboard);
        Button btnPlanner  = navButton("📅", "Study Planner",   this::navigatePlanner);
        Button btnTimer    = navButton("⏱", "Focus Timer",     this::navigateTimer);
        Button btnGoals    = navButton("🎯", "Goals",           this::navigateGoals);
        Button btnNotes    = navButton("📝", "Notes",           this::navigateNotes);
        Button btnRemind   = navButton("🔔", "Reminders",       this::navigateReminders);
        Button btnTasks    = navButton("📋", "Tasks",           this::navigateTasks);
        Button btnAnalytics= navButton("📊", "Analytics",       this::navigateAnalytics);
        Button btnExams    = navButton("🎓", "Exams",           this::navigateExams);

        navBox.getChildren().addAll(navHeader,
                btnDash, btnPlanner, btnTasks, btnTimer, btnGoals,
                btnNotes, btnRemind, btnAnalytics, btnExams);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User profile card
        VBox profileBox = buildProfileCard();

        sidebar.getChildren().addAll(logoBox, navBox, spacer, profileBox);

        // Select dashboard by default
        setActive(btnDash);
        activeNav = btnDash;
        return sidebar;
    }

    private Button navButton(String icon, String label, Runnable action) {
        Button btn = new Button(icon + "  " + label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(e -> {
            if (activeNav != null) activeNav.getStyleClass().remove("nav-button-active");
            setActive(btn);
            activeNav = btn;
            action.run();
        });
        Tooltip.install(btn, new Tooltip(label));
        return btn;
    }

    private void setActive(Button btn) {
        btn.getStyleClass().remove("nav-button-active");
        btn.getStyleClass().add("nav-button-active");
    }

    private VBox buildProfileCard() {
        UserProfile profile = DatabaseService.getInstance().getUserProfile();
        VBox box = new VBox(8);
        box.getStyleClass().add("profile-card");
        box.setPadding(new Insets(14, 16, 18, 16));

        // Avatar circle with initials
        String initials = profile.getName().isEmpty() ? "S"
                : String.valueOf(profile.getName().charAt(0)).toUpperCase();
        StackPane avatar = new StackPane();
        Circle circle = new Circle(22, Color.web(UIFactory.C_PRIMARY));
        Label initial = new Label(initials);
        initial.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        avatar.getChildren().addAll(circle, initial);

        VBox info = new VBox(2);
        Label nameLbl   = new Label(profile.getName());
        nameLbl.getStyleClass().add("profile-name");
        Label streakLbl = new Label("🔥 " + profile.getStreakDays() + "-day streak");
        streakLbl.getStyleClass().add("profile-streak");
        info.getChildren().addAll(nameLbl, streakLbl);

        HBox row = new HBox(10, avatar, info);
        row.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(row);
        return box;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateDashboard() {
        if (dashboardView == null) dashboardView = new DashboardView();
        showView(dashboardView.build());
    }

    private void navigatePlanner() {
        if (studyPlannerView == null) studyPlannerView = new StudyPlannerView();
        showView(studyPlannerView.build());
    }

    public void navigateTimer() {
        if (focusTimerView == null) focusTimerView = new FocusTimerView();
        showView(focusTimerView.build());
    }

    public void navigateGoals() {
        if (goalsView == null) goalsView = new GoalsView();
        showView(goalsView.build());
    }

    private void navigateNotes() {
        if (notesView == null) notesView = new NotesView();
        showView(notesView.build());
    }

    private void navigateReminders() {
        if (remindersView == null) remindersView = new RemindersView();
        showView(remindersView.build());
    }

    private void navigateAnalytics() {
        if (analyticsView == null) analyticsView = new AnalyticsView();
        showView(analyticsView.build());
    }

    public void navigateExams() {
        if (examsView == null) examsView = new ExamsView();
        showView(examsView.build());
    }

    public void navigateTasks() {
        if (assignmentsView == null) assignmentsView = new AssignmentsView();
        showView(assignmentsView.build());
    }

    private void showView(Node view) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), view);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        content.getChildren().setAll(view);
        ft.play();
    }
}
