package com.spms.service;

import com.spms.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Singleton service that manages all SQLite persistence for SPMS.
 * All CRUD operations for every entity live here.
 */
public class DatabaseService {

    private static final Logger LOG = Logger.getLogger(DatabaseService.class.getName());
    private static DatabaseService instance;

    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter T_FMT  = DateTimeFormatter.ISO_LOCAL_TIME;

    private String dbUrl;

    private DatabaseService() {}

    public static synchronized DatabaseService getInstance() {
        if (instance == null) instance = new DatabaseService();
        return instance;
    }

    // ── Initialisation ───────────────────────────────────────────────────────

    public void initialize() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".spms");
            Files.createDirectories(dir);
            dbUrl = "jdbc:sqlite:" + dir.resolve("spms.db").toAbsolutePath();
            createTables();
            insertSampleDataIfEmpty();
        } catch (Exception e) {
            LOG.warning("Falling back to in-memory DB: " + e.getMessage());
            dbUrl = "jdbc:sqlite::memory:";
            try { createTables(); insertSampleDataIfEmpty(); }
            catch (Exception ex) { LOG.severe("In-memory DB failed: " + ex.getMessage()); }
        }
    }

    public void close() { /* SQLite connections are closed per-operation */ }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    // ── Schema ───────────────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS assignments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    subject TEXT,
                    description TEXT,
                    due_date TEXT,
                    priority TEXT DEFAULT 'MEDIUM',
                    status TEXT DEFAULT 'PENDING',
                    created_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    type TEXT DEFAULT 'DAILY',
                    target_value REAL DEFAULT 1.0,
                    current_value REAL DEFAULT 0.0,
                    unit TEXT,
                    due_date TEXT,
                    completed INTEGER DEFAULT 0,
                    created_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT,
                    subject TEXT,
                    tags TEXT,
                    created_at TEXT,
                    updated_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT,
                    due_datetime TEXT,
                    urgency TEXT DEFAULT 'MEDIUM',
                    status TEXT DEFAULT 'ACTIVE',
                    snoozed_until TEXT,
                    created_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS study_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT,
                    start_time TEXT,
                    end_time TEXT,
                    duration_minutes INTEGER,
                    notes TEXT,
                    session_date TEXT,
                    focus_mode INTEGER DEFAULT 0
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS exams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    exam_date TEXT,
                    exam_time TEXT,
                    location TEXT,
                    notes TEXT,
                    created_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS study_plans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    deadline TEXT,
                    difficulty INTEGER DEFAULT 3,
                    daily_hours REAL DEFAULT 2.0,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TEXT
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS study_blocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plan_id INTEGER,
                    block_date TEXT,
                    hours REAL,
                    topic TEXT,
                    completed INTEGER DEFAULT 0,
                    FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE
                )""");

            s.execute("""
                CREATE TABLE IF NOT EXISTS user_profile (
                    id INTEGER PRIMARY KEY,
                    name TEXT DEFAULT 'Student',
                    streak_days INTEGER DEFAULT 0,
                    last_study_date TEXT,
                    total_study_hours REAL DEFAULT 0.0,
                    total_completed_sessions INTEGER DEFAULT 0,
                    theme TEXT DEFAULT 'dark'
                )""");

            s.execute("INSERT OR IGNORE INTO user_profile (id, name) VALUES (1, 'Student')");
        }
    }

    // ── Sample data ──────────────────────────────────────────────────────────

    private void insertSampleDataIfEmpty() throws SQLException {
        if (getAssignmentCount() > 0) return;

        LocalDate today = LocalDate.now();

        saveAssignment(new Assignment("Linear Algebra Problem Set", "Mathematics",
                "Complete chapter 3-4 exercises on eigenvalues and eigenvectors",
                today.plusDays(3), Assignment.Priority.HIGH));
        saveAssignment(new Assignment("History Essay – WWI Causes", "History",
                "Write a 2 000-word essay on the causes of World War I",
                today.plusDays(7), Assignment.Priority.MEDIUM));
        saveAssignment(new Assignment("Physics Lab Report", "Physics",
                "Document findings from the pendulum experiment",
                today.plusDays(2), Assignment.Priority.URGENT));
        saveAssignment(new Assignment("BST Implementation", "Computer Science",
                "Implement a Binary Search Tree with insert, delete, and search",
                today.plusDays(5), Assignment.Priority.HIGH));
        saveAssignment(new Assignment("Shakespeare Analysis", "English Literature",
                "Analyse themes of power and betrayal in Hamlet",
                today.plusDays(14), Assignment.Priority.LOW));

        Goal g1 = new Goal("Study 2 hours daily", "Consistent daily study habit",
                Goal.Type.DAILY, 2.0, "hours", today);
        g1.setCurrentValue(1.5);
        saveGoal(g1);

        Goal g2 = new Goal("Complete 10 assignments this week", "Weekly productivity",
                Goal.Type.WEEKLY, 10.0, "assignments", today.plusDays(7));
        g2.setCurrentValue(4.0);
        saveGoal(g2);

        Goal g3 = new Goal("Read 3 textbook chapters", "Monthly reading target",
                Goal.Type.MONTHLY, 3.0, "chapters", today.plusDays(30));
        g3.setCurrentValue(1.0);
        saveGoal(g3);

        saveNote(new Note("Linear Algebra Key Concepts",
                "Eigenvalues: λ is an eigenvalue of A if det(A − λI) = 0\n" +
                "Eigenvectors: Av = λv\nDiagonalization: A = PDP⁻¹\n" +
                "Characteristic polynomial: det(A − λI) = 0",
                "Mathematics", "algebra,eigenvalues,linear"));
        saveNote(new Note("WWI Timeline",
                "1914: Assassination of Archduke Franz Ferdinand\n" +
                "1914: Austria-Hungary declares war on Serbia\n" +
                "1914: Germany declares war on France and Russia\n" +
                "1918: Armistice signed on 11 November",
                "History", "wwi,timeline,important"));
        saveNote(new Note("Algorithm Complexity Reference",
                "Binary Search: O(log n)\nQuick Sort: O(n log n) avg\n" +
                "Merge Sort: O(n log n)\nHeap Sort: O(n log n)\nBubble Sort: O(n²)\n" +
                "BFS / DFS: O(V + E)",
                "Computer Science", "algorithms,complexity,big-o"));

        saveReminder(new Reminder("Submit Physics Lab Report",
                "Lab report due in 2 days – start writing tonight!",
                LocalDateTime.now().plusDays(2), Reminder.Urgency.CRITICAL));
        saveReminder(new Reminder("Study Group Meeting",
                "Meet study group in Library Room 204",
                LocalDateTime.now().plusHours(3), Reminder.Urgency.MEDIUM));
        saveReminder(new Reminder("Pay Tuition Fee",
                "Semester tuition deadline approaching",
                LocalDateTime.now().plusDays(5), Reminder.Urgency.HIGH));

        saveExam(new Exam("Mathematics – Linear Algebra",
                today.plusDays(14), LocalTime.of(9, 0),
                "Hall A, Room 101", "Chapters 1–6 covered"));
        saveExam(new Exam("Physics – Classical Mechanics",
                today.plusDays(21), LocalTime.of(14, 0),
                "Science Building, Room 305", "Lab experiments included"));
        saveExam(new Exam("Computer Science – Data Structures",
                today.plusDays(30), LocalTime.of(10, 0),
                "Tech Centre, Lab 2", "Focus on trees and graphs"));

        // Past study sessions for analytics charts
        for (int i = 7; i >= 1; i--) {
            StudySession sess = new StudySession();
            sess.setSubject(i % 3 == 0 ? "Mathematics"
                    : i % 3 == 1 ? "Computer Science" : "Physics");
            LocalDate day = today.minusDays(i);
            sess.setSessionDate(day);
            sess.setStartTime(LocalDateTime.of(day, LocalTime.of(10, 0)));
            int mins = 45 + (i * 17 % 60);
            sess.setDurationMinutes(mins);
            sess.setEndTime(sess.getStartTime().plusMinutes(mins));
            sess.setNotes("Productive session");
            saveStudySession(sess);
        }

        // Sample study plan
        StudyPlan plan = new StudyPlan("Mathematics", today.plusDays(14), 4, 2.0);
        int planId = saveStudyPlanReturnId(plan);
        String[] topics = {"Eigenvalues & Eigenvectors", "Matrix Diagonalization",
                "Orthogonality", "Least Squares", "Practice Problems",
                "Mock Exam", "Revision"};
        for (int i = 0; i < topics.length; i++) {
            StudyBlock block = new StudyBlock(planId, today.plusDays(i), 2.0, topics[i]);
            block.setCompleted(i < 2);
            saveStudyBlock(block);
        }

        // Update profile totals
        UserProfile profile = getUserProfile();
        profile.setTotalStudyHours(12.5);
        profile.setTotalCompletedSessions(7);
        profile.setStreakDays(3);
        profile.setLastStudyDate(today);
        updateUserProfile(profile);
    }

    // ── Assignments ──────────────────────────────────────────────────────────

    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT * FROM assignments ORDER BY due_date ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAssignment(rs));
        } catch (SQLException e) { LOG.warning("getAllAssignments: " + e.getMessage()); }
        return list;
    }

    public List<Assignment> getUpcomingAssignments(int days) {
        List<Assignment> list = new ArrayList<>();
        String sql = """
            SELECT * FROM assignments
            WHERE due_date <= ? AND status != 'COMPLETED'
            ORDER BY due_date ASC LIMIT 10""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().plusDays(days).format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAssignment(rs));
            }
        } catch (SQLException e) { LOG.warning("getUpcomingAssignments: " + e.getMessage()); }
        return list;
    }

    public void saveAssignment(Assignment a) {
        String sql = """
            INSERT INTO assignments
            (title, subject, description, due_date, priority, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getSubject());
            ps.setString(3, a.getDescription());
            ps.setString(4, str(a.getDueDate()));
            ps.setString(5, a.getPriority().name());
            ps.setString(6, a.getStatus().name());
            ps.setString(7, str(a.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveAssignment: " + e.getMessage()); }
    }

    public void updateAssignment(Assignment a) {
        String sql = """
            UPDATE assignments SET title=?, subject=?, description=?, due_date=?,
            priority=?, status=? WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getSubject());
            ps.setString(3, a.getDescription());
            ps.setString(4, str(a.getDueDate()));
            ps.setString(5, a.getPriority().name());
            ps.setString(6, a.getStatus().name());
            ps.setInt(7, a.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateAssignment: " + e.getMessage()); }
    }

    public void deleteAssignment(int id) {
        exec("DELETE FROM assignments WHERE id=?", id);
    }

    public int getAssignmentCount() { return countRows("assignments"); }

    public int getCompletedAssignmentsCount() {
        return countWhere("assignments", "status='COMPLETED'");
    }

    public int getPendingAssignmentsCount() {
        return countWhere("assignments", "status!='COMPLETED'");
    }

    private Assignment mapAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setSubject(rs.getString("subject"));
        a.setDescription(rs.getString("description"));
        a.setDueDate(parseDate(rs.getString("due_date")));
        a.setPriority(parseEnum(Assignment.Priority.class, rs.getString("priority"), Assignment.Priority.MEDIUM));
        a.setStatus(parseEnum(Assignment.Status.class, rs.getString("status"), Assignment.Status.PENDING));
        a.setCreatedAt(parseDt(rs.getString("created_at")));
        return a;
    }

    // ── Goals ────────────────────────────────────────────────────────────────

    public List<Goal> getAllGoals() {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY created_at DESC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapGoal(rs));
        } catch (SQLException e) { LOG.warning("getAllGoals: " + e.getMessage()); }
        return list;
    }

    public void saveGoal(Goal g) {
        String sql = """
            INSERT INTO goals
            (title, description, type, target_value, current_value, unit, due_date, completed, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getTitle());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getType().name());
            ps.setDouble(4, g.getTargetValue());
            ps.setDouble(5, g.getCurrentValue());
            ps.setString(6, g.getUnit());
            ps.setString(7, str(g.getDueDate()));
            ps.setInt(8, g.isCompleted() ? 1 : 0);
            ps.setString(9, str(g.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) g.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveGoal: " + e.getMessage()); }
    }

    public void updateGoal(Goal g) {
        String sql = """
            UPDATE goals SET title=?, description=?, type=?, target_value=?,
            current_value=?, unit=?, due_date=?, completed=? WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getTitle());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getType().name());
            ps.setDouble(4, g.getTargetValue());
            ps.setDouble(5, g.getCurrentValue());
            ps.setString(6, g.getUnit());
            ps.setString(7, str(g.getDueDate()));
            ps.setInt(8, g.isCompleted() ? 1 : 0);
            ps.setInt(9, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateGoal: " + e.getMessage()); }
    }

    public void deleteGoal(int id) { exec("DELETE FROM goals WHERE id=?", id); }

    public int getActiveGoalsCount() {
        return countWhere("goals", "completed=0");
    }

    public int getCompletedGoalsCount() {
        return countWhere("goals", "completed=1");
    }

    private Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        g.setType(parseEnum(Goal.Type.class, rs.getString("type"), Goal.Type.DAILY));
        g.setTargetValue(rs.getDouble("target_value"));
        g.setCurrentValue(rs.getDouble("current_value"));
        g.setUnit(rs.getString("unit"));
        g.setDueDate(parseDate(rs.getString("due_date")));
        g.setCompleted(rs.getInt("completed") == 1);
        g.setCreatedAt(parseDt(rs.getString("created_at")));
        return g;
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    public List<Note> getAllNotes() {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM notes ORDER BY updated_at DESC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapNote(rs));
        } catch (SQLException e) { LOG.warning("getAllNotes: " + e.getMessage()); }
        return list;
    }

    public List<Note> searchNotes(String query) {
        List<Note> list = new ArrayList<>();
        String sql = """
            SELECT * FROM notes
            WHERE lower(title) LIKE ? OR lower(content) LIKE ? OR lower(tags) LIKE ?
            ORDER BY updated_at DESC""";
        String q = "%" + query.toLowerCase() + "%";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapNote(rs));
            }
        } catch (SQLException e) { LOG.warning("searchNotes: " + e.getMessage()); }
        return list;
    }

    public List<Note> getNotesBySubject(String subject) {
        List<Note> list = new ArrayList<>();
        String sql = "SELECT * FROM notes WHERE lower(subject)=? ORDER BY updated_at DESC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, subject.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapNote(rs));
            }
        } catch (SQLException e) { LOG.warning("getNotesBySubject: " + e.getMessage()); }
        return list;
    }

    public void saveNote(Note n) {
        String sql = """
            INSERT INTO notes (title, content, subject, tags, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getTitle());
            ps.setString(2, n.getContent());
            ps.setString(3, n.getSubject());
            ps.setString(4, n.getTags());
            ps.setString(5, str(n.getCreatedAt()));
            ps.setString(6, str(n.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveNote: " + e.getMessage()); }
    }

    public void updateNote(Note n) {
        n.setUpdatedAt(LocalDateTime.now());
        String sql = """
            UPDATE notes SET title=?, content=?, subject=?, tags=?, updated_at=?
            WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, n.getTitle());
            ps.setString(2, n.getContent());
            ps.setString(3, n.getSubject());
            ps.setString(4, n.getTags());
            ps.setString(5, str(n.getUpdatedAt()));
            ps.setInt(6, n.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateNote: " + e.getMessage()); }
    }

    public void deleteNote(int id) { exec("DELETE FROM notes WHERE id=?", id); }

    public int getNotesCount() { return countRows("notes"); }

    private Note mapNote(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getInt("id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setSubject(rs.getString("subject"));
        n.setTags(rs.getString("tags"));
        n.setCreatedAt(parseDt(rs.getString("created_at")));
        n.setUpdatedAt(parseDt(rs.getString("updated_at")));
        return n;
    }

    // ── Reminders ────────────────────────────────────────────────────────────

    public List<Reminder> getAllReminders() {
        List<Reminder> list = new ArrayList<>();
        String sql = "SELECT * FROM reminders ORDER BY due_datetime ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapReminder(rs));
        } catch (SQLException e) { LOG.warning("getAllReminders: " + e.getMessage()); }
        return list;
    }

    public List<Reminder> getActiveReminders() {
        List<Reminder> list = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE status='ACTIVE' ORDER BY due_datetime ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapReminder(rs));
        } catch (SQLException e) { LOG.warning("getActiveReminders: " + e.getMessage()); }
        return list;
    }

    public void saveReminder(Reminder r) {
        String sql = """
            INSERT INTO reminders
            (title, message, due_datetime, urgency, status, snoozed_until, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getTitle());
            ps.setString(2, r.getMessage());
            ps.setString(3, str(r.getDueDateTime()));
            ps.setString(4, r.getUrgency().name());
            ps.setString(5, r.getStatus().name());
            ps.setString(6, str(r.getSnoozedUntil()));
            ps.setString(7, str(r.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveReminder: " + e.getMessage()); }
    }

    public void updateReminder(Reminder r) {
        String sql = """
            UPDATE reminders SET title=?, message=?, due_datetime=?, urgency=?,
            status=?, snoozed_until=? WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getTitle());
            ps.setString(2, r.getMessage());
            ps.setString(3, str(r.getDueDateTime()));
            ps.setString(4, r.getUrgency().name());
            ps.setString(5, r.getStatus().name());
            ps.setString(6, str(r.getSnoozedUntil()));
            ps.setInt(7, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateReminder: " + e.getMessage()); }
    }

    public void deleteReminder(int id) { exec("DELETE FROM reminders WHERE id=?", id); }

    private Reminder mapReminder(ResultSet rs) throws SQLException {
        Reminder r = new Reminder();
        r.setId(rs.getInt("id"));
        r.setTitle(rs.getString("title"));
        r.setMessage(rs.getString("message"));
        r.setDueDateTime(parseDt(rs.getString("due_datetime")));
        r.setUrgency(parseEnum(Reminder.Urgency.class, rs.getString("urgency"), Reminder.Urgency.MEDIUM));
        r.setStatus(parseEnum(Reminder.ReminderStatus.class, rs.getString("status"), Reminder.ReminderStatus.ACTIVE));
        r.setSnoozedUntil(parseDt(rs.getString("snoozed_until")));
        r.setCreatedAt(parseDt(rs.getString("created_at")));
        return r;
    }

    // ── Study Sessions ───────────────────────────────────────────────────────

    public void saveStudySession(StudySession s) {
        String sql = """
            INSERT INTO study_sessions
            (subject, start_time, end_time, duration_minutes, notes, session_date, focus_mode)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getSubject());
            ps.setString(2, str(s.getStartTime()));
            ps.setString(3, str(s.getEndTime()));
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getNotes());
            ps.setString(6, str(s.getSessionDate()));
            ps.setInt(7, s.isFocusMode() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveStudySession: " + e.getMessage()); }
    }

    public List<StudySession> getAllSessions() {
        List<StudySession> list = new ArrayList<>();
        String sql = "SELECT * FROM study_sessions ORDER BY session_date DESC, start_time DESC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapSession(rs));
        } catch (SQLException e) { LOG.warning("getAllSessions: " + e.getMessage()); }
        return list;
    }

    public double getTodayStudyHours() {
        String sql = "SELECT COALESCE(SUM(duration_minutes),0) FROM study_sessions WHERE session_date=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1) / 60.0;
            }
        } catch (SQLException e) { LOG.warning("getTodayStudyHours: " + e.getMessage()); }
        return 0.0;
    }

    public double getWeekStudyHours() {
        String sql = "SELECT COALESCE(SUM(duration_minutes),0) FROM study_sessions WHERE session_date>=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().minusDays(7).format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1) / 60.0;
            }
        } catch (SQLException e) { LOG.warning("getWeekStudyHours: " + e.getMessage()); }
        return 0.0;
    }

    /** Returns a map of DayOfWeek short name → hours for the past 7 days. */
    public Map<String, Double> getWeeklyStudyHoursMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.getDayOfWeek().toString().substring(0, 3);
            map.put(key, 0.0);
        }
        String sql = """
            SELECT session_date, COALESCE(SUM(duration_minutes),0) as mins
            FROM study_sessions WHERE session_date>=?
            GROUP BY session_date""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, today.minusDays(6).format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = parseDate(rs.getString("session_date"));
                    if (d != null) {
                        String key = d.getDayOfWeek().toString().substring(0, 3);
                        map.put(key, rs.getDouble("mins") / 60.0);
                    }
                }
            }
        } catch (SQLException e) { LOG.warning("getWeeklyStudyHoursMap: " + e.getMessage()); }
        return map;
    }

    /** Returns subject → total hours */
    public Map<String, Double> getSubjectHoursMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
            SELECT subject, COALESCE(SUM(duration_minutes),0) as mins
            FROM study_sessions WHERE subject IS NOT NULL
            GROUP BY subject ORDER BY mins DESC""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String subj = rs.getString("subject");
                if (subj != null) map.put(subj, rs.getDouble("mins") / 60.0);
            }
        } catch (SQLException e) { LOG.warning("getSubjectHoursMap: " + e.getMessage()); }
        return map;
    }

    private StudySession mapSession(ResultSet rs) throws SQLException {
        StudySession s = new StudySession();
        s.setId(rs.getInt("id"));
        s.setSubject(rs.getString("subject"));
        s.setStartTime(parseDt(rs.getString("start_time")));
        s.setEndTime(parseDt(rs.getString("end_time")));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        s.setNotes(rs.getString("notes"));
        s.setSessionDate(parseDate(rs.getString("session_date")));
        s.setFocusMode(rs.getInt("focus_mode") == 1);
        return s;
    }

    // ── Exams ────────────────────────────────────────────────────────────────

    public List<Exam> getAllExams() {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT * FROM exams ORDER BY exam_date ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapExam(rs));
        } catch (SQLException e) { LOG.warning("getAllExams: " + e.getMessage()); }
        return list;
    }

    public List<Exam> getUpcomingExams() {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT * FROM exams WHERE exam_date>=? ORDER BY exam_date ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapExam(rs));
            }
        } catch (SQLException e) { LOG.warning("getUpcomingExams: " + e.getMessage()); }
        return list;
    }

    public Exam getNextExam() {
        String sql = "SELECT * FROM exams WHERE exam_date>=? ORDER BY exam_date ASC LIMIT 1";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().format(D_FMT));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapExam(rs);
            }
        } catch (SQLException e) { LOG.warning("getNextExam: " + e.getMessage()); }
        return null;
    }

    public void saveExam(Exam e) {
        String sql = """
            INSERT INTO exams (subject, exam_date, exam_time, location, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getSubject());
            ps.setString(2, str(e.getExamDate()));
            ps.setString(3, e.getExamTime() != null ? e.getExamTime().format(T_FMT) : null);
            ps.setString(4, e.getLocation());
            ps.setString(5, e.getNotes());
            ps.setString(6, str(e.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
        } catch (SQLException ex) { LOG.warning("saveExam: " + ex.getMessage()); }
    }

    public void updateExam(Exam e) {
        String sql = """
            UPDATE exams SET subject=?, exam_date=?, exam_time=?, location=?, notes=?
            WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getSubject());
            ps.setString(2, str(e.getExamDate()));
            ps.setString(3, e.getExamTime() != null ? e.getExamTime().format(T_FMT) : null);
            ps.setString(4, e.getLocation());
            ps.setString(5, e.getNotes());
            ps.setInt(6, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) { LOG.warning("updateExam: " + ex.getMessage()); }
    }

    public void deleteExam(int id) { exec("DELETE FROM exams WHERE id=?", id); }

    private Exam mapExam(ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setId(rs.getInt("id"));
        e.setSubject(rs.getString("subject"));
        e.setExamDate(parseDate(rs.getString("exam_date")));
        String tStr = rs.getString("exam_time");
        if (tStr != null) { try { e.setExamTime(LocalTime.parse(tStr, T_FMT)); } catch (Exception ignored) {} }
        e.setLocation(rs.getString("location"));
        e.setNotes(rs.getString("notes"));
        e.setCreatedAt(parseDt(rs.getString("created_at")));
        return e;
    }

    // ── Study Plans ──────────────────────────────────────────────────────────

    public List<StudyPlan> getAllStudyPlans() {
        List<StudyPlan> list = new ArrayList<>();
        String sql = "SELECT * FROM study_plans ORDER BY deadline ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StudyPlan p = mapPlan(rs);
                p.setBlocks(getBlocksForPlan(p.getId()));
                list.add(p);
            }
        } catch (SQLException e) { LOG.warning("getAllStudyPlans: " + e.getMessage()); }
        return list;
    }

    public int saveStudyPlanReturnId(StudyPlan p) {
        String sql = """
            INSERT INTO study_plans (subject, deadline, difficulty, daily_hours, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSubject());
            ps.setString(2, str(p.getDeadline()));
            ps.setInt(3, p.getDifficulty());
            ps.setDouble(4, p.getDailyHours());
            ps.setString(5, p.getStatus().name());
            ps.setString(6, str(p.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { int id = keys.getInt(1); p.setId(id); return id; }
            }
        } catch (SQLException e) { LOG.warning("saveStudyPlan: " + e.getMessage()); }
        return -1;
    }

    public void updateStudyPlan(StudyPlan p) {
        String sql = """
            UPDATE study_plans SET subject=?, deadline=?, difficulty=?,
            daily_hours=?, status=? WHERE id=?""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getSubject());
            ps.setString(2, str(p.getDeadline()));
            ps.setInt(3, p.getDifficulty());
            ps.setDouble(4, p.getDailyHours());
            ps.setString(5, p.getStatus().name());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateStudyPlan: " + e.getMessage()); }
    }

    public void deleteStudyPlan(int id) {
        exec("DELETE FROM study_blocks WHERE plan_id=?", id);
        exec("DELETE FROM study_plans WHERE id=?", id);
    }

    public List<StudyBlock> getBlocksForPlan(int planId) {
        List<StudyBlock> list = new ArrayList<>();
        String sql = "SELECT * FROM study_blocks WHERE plan_id=? ORDER BY block_date ASC";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBlock(rs));
            }
        } catch (SQLException e) { LOG.warning("getBlocksForPlan: " + e.getMessage()); }
        return list;
    }

    public void saveStudyBlock(StudyBlock b) {
        String sql = """
            INSERT INTO study_blocks (plan_id, block_date, hours, topic, completed)
            VALUES (?, ?, ?, ?, ?)""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getPlanId());
            ps.setString(2, str(b.getBlockDate()));
            ps.setDouble(3, b.getHours());
            ps.setString(4, b.getTopic());
            ps.setInt(5, b.isCompleted() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setId(keys.getInt(1));
            }
        } catch (SQLException e) { LOG.warning("saveStudyBlock: " + e.getMessage()); }
    }

    public void updateStudyBlock(StudyBlock b) {
        String sql = "UPDATE study_blocks SET completed=?, topic=?, hours=? WHERE id=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, b.isCompleted() ? 1 : 0);
            ps.setString(2, b.getTopic());
            ps.setDouble(3, b.getHours());
            ps.setInt(4, b.getId());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateStudyBlock: " + e.getMessage()); }
    }

    private StudyPlan mapPlan(ResultSet rs) throws SQLException {
        StudyPlan p = new StudyPlan();
        p.setId(rs.getInt("id"));
        p.setSubject(rs.getString("subject"));
        p.setDeadline(parseDate(rs.getString("deadline")));
        p.setDifficulty(rs.getInt("difficulty"));
        p.setDailyHours(rs.getDouble("daily_hours"));
        p.setStatus(parseEnum(StudyPlan.Status.class, rs.getString("status"), StudyPlan.Status.ACTIVE));
        p.setCreatedAt(parseDt(rs.getString("created_at")));
        return p;
    }

    private StudyBlock mapBlock(ResultSet rs) throws SQLException {
        StudyBlock b = new StudyBlock();
        b.setId(rs.getInt("id"));
        b.setPlanId(rs.getInt("plan_id"));
        b.setBlockDate(parseDate(rs.getString("block_date")));
        b.setHours(rs.getDouble("hours"));
        b.setTopic(rs.getString("topic"));
        b.setCompleted(rs.getInt("completed") == 1);
        return b;
    }

    // ── User Profile ─────────────────────────────────────────────────────────

    public UserProfile getUserProfile() {
        UserProfile p = new UserProfile();
        String sql = "SELECT * FROM user_profile WHERE id=1";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setStreakDays(rs.getInt("streak_days"));
                p.setLastStudyDate(parseDate(rs.getString("last_study_date")));
                p.setTotalStudyHours(rs.getDouble("total_study_hours"));
                p.setTotalCompletedSessions(rs.getInt("total_completed_sessions"));
                p.setTheme(rs.getString("theme"));
            }
        } catch (SQLException e) { LOG.warning("getUserProfile: " + e.getMessage()); }
        return p;
    }

    public void updateUserProfile(UserProfile p) {
        String sql = """
            UPDATE user_profile SET name=?, streak_days=?, last_study_date=?,
            total_study_hours=?, total_completed_sessions=?, theme=? WHERE id=1""";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getStreakDays());
            ps.setString(3, str(p.getLastStudyDate()));
            ps.setDouble(4, p.getTotalStudyHours());
            ps.setInt(5, p.getTotalCompletedSessions());
            ps.setString(6, p.getTheme());
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("updateUserProfile: " + e.getMessage()); }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void exec(String sql, int param) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            ps.executeUpdate();
        } catch (SQLException e) { LOG.warning("exec: " + e.getMessage()); }
    }

    private int countRows(String table) {
        try (Connection c = conn();
             ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOG.warning("countRows " + table + ": " + e.getMessage()); }
        return 0;
    }

    private int countWhere(String table, String where) {
        try (Connection c = conn();
             ResultSet rs = c.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOG.warning("countWhere: " + e.getMessage()); }
        return 0;
    }

    private String str(LocalDate d)      { return d  != null ? d.format(D_FMT)   : null; }
    private String str(LocalDateTime dt) { return dt != null ? dt.format(DT_FMT) : null; }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, D_FMT); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime parseDt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, DT_FMT); }
        catch (Exception e) { return null; }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String s, E fallback) {
        if (s == null) return fallback;
        try { return Enum.valueOf(cls, s); }
        catch (Exception e) { return fallback; }
    }
}
