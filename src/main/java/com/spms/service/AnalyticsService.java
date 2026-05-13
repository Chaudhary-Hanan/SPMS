package com.spms.service;

import com.spms.model.Assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Computes analytics metrics by querying DatabaseService.
 */
public class AnalyticsService {

    private static AnalyticsService instance;
    private final DatabaseService db = DatabaseService.getInstance();

    private AnalyticsService() {}

    public static AnalyticsService getInstance() {
        if (instance == null) instance = new AnalyticsService();
        return instance;
    }

    // ── Core metrics ─────────────────────────────────────────────────────────

    public double getTodayStudyHours()  { return db.getTodayStudyHours(); }
    public double getWeekStudyHours()   { return db.getWeekStudyHours(); }
    public int getActiveGoalsCount()    { return db.getActiveGoalsCount(); }
    public int getCompletedTasksCount() { return db.getCompletedAssignmentsCount(); }
    public int getPendingTasksCount()   { return db.getPendingAssignmentsCount(); }
    public int getStreakDays()          { return db.getUserProfile().getStreakDays(); }

    public double getTaskCompletionRate() {
        int total = db.getAssignmentCount();
        if (total == 0) return 0.0;
        return (double) db.getCompletedAssignmentsCount() / total * 100.0;
    }

    public Map<String, Double> getWeeklyStudyHours() {
        return db.getWeeklyStudyHoursMap();
    }

    public Map<String, Double> getSubjectDistribution() {
        return db.getSubjectHoursMap();
    }

    /**
     * Productivity score 0–100 based on: task completion rate, streak, weekly hours.
     */
    public double getProductivityScore() {
        double completionRate = getTaskCompletionRate();          // 0–100 → weight 40%
        int    streak         = getStreakDays();                  // weight 30%  (cap 10 days)
        double weekHours      = getWeekStudyHours();              // weight 30%  (cap 14h)

        double streakScore    = Math.min(streak * 3.0, 30.0);
        double hoursScore     = Math.min(weekHours * 2.1, 30.0);
        double completionScr  = completionRate * 0.40;

        return Math.min(100.0, completionScr + streakScore + hoursScore);
    }

    /**
     * Returns subjects where completion rate < 40 % (based on assignment status).
     */
    public List<String> getWeakSubjects() {
        List<String> weak = new ArrayList<>();
        Map<String, Long> total     = new java.util.HashMap<>();
        Map<String, Long> completed = new java.util.HashMap<>();

        for (Assignment a : db.getAllAssignments()) {
            String subj = a.getSubject();
            if (subj == null || subj.isBlank()) continue;
            total.merge(subj, 1L, Long::sum);
            if (a.getStatus() == Assignment.Status.COMPLETED)
                completed.merge(subj, 1L, Long::sum);
        }
        total.forEach((subj, cnt) -> {
            long done = completed.getOrDefault(subj, 0L);
            if (cnt > 0 && (double) done / cnt < 0.40) weak.add(subj);
        });
        return weak;
    }

    public String getInsightMessage(double score) {
        if (score >= 80) return "Excellent! You're at your productivity peak. Keep the momentum going!";
        if (score >= 60) return "Good progress! Stay consistent – you're building strong habits.";
        if (score >= 40) return "Room for improvement. Try completing more tasks and studying daily.";
        return "Let's turn things around. Start with small daily goals and build up gradually.";
    }
}
