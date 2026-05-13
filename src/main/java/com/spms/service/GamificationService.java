package com.spms.service;

import com.spms.model.UserProfile;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages streaks, badges, and motivational messages.
 */
public class GamificationService {

    /** Immutable badge record. */
    public record Badge(String name, String description, String emoji, boolean earned) {}

    private static GamificationService instance;
    private final DatabaseService db = DatabaseService.getInstance();

    private GamificationService() {}

    public static GamificationService getInstance() {
        if (instance == null) instance = new GamificationService();
        return instance;
    }

    // ── Badges ───────────────────────────────────────────────────────────────

    public List<Badge> getAllBadges() {
        UserProfile p  = db.getUserProfile();
        int streak     = p.getStreakDays();
        int sessions   = p.getTotalCompletedSessions();
        double hours   = p.getTotalStudyHours();
        int completedA = db.getCompletedAssignmentsCount();
        int notesCount = db.getNotesCount();
        int goalsComp  = db.getCompletedGoalsCount();

        return List.of(
            new Badge("First Steps",       "Complete your first study session",         "🎯", sessions  >= 1),
            new Badge("Note Taker",         "Create 5 notes",                            "📝", notesCount >= 5),
            new Badge("Consistent Learner", "Study 3 days in a row",                     "🔥", streak     >= 3),
            new Badge("Task Master",        "Complete 5 assignments",                    "✅", completedA >= 5),
            new Badge("Week Warrior",       "Maintain a 7-day streak",                   "⚡", streak     >= 7),
            new Badge("Study Marathon",     "Log 20 hours of total study time",          "📚", hours      >= 20),
            new Badge("Goal Crusher",       "Complete 5 goals",                          "🎖", goalsComp  >= 5),
            new Badge("Scholar",            "Complete 20 assignments",                   "🏅", completedA >= 20),
            new Badge("Iron Will",          "Maintain a 30-day streak",                  "🏆", streak     >= 30),
            new Badge("Century",            "Log 100 hours of total study time",         "💯", hours      >= 100)
        );
    }

    public int getEarnedBadgesCount() {
        return (int) getAllBadges().stream().filter(Badge::earned).count();
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    /**
     * Called after a study session is saved.
     * Increments streak if today is consecutive; resets if a day was skipped.
     */
    public void updateStreakAfterSession() {
        UserProfile profile = db.getUserProfile();
        LocalDate today     = LocalDate.now();
        LocalDate last      = profile.getLastStudyDate();

        if (last == null || last.isBefore(today.minusDays(1))) {
            profile.setStreakDays(1);
        } else if (last.isBefore(today)) {
            profile.setStreakDays(profile.getStreakDays() + 1);
        }
        // If last == today, streak unchanged (already counted)

        profile.setLastStudyDate(today);
        profile.setTotalCompletedSessions(profile.getTotalCompletedSessions() + 1);
        db.updateUserProfile(profile);
    }

    public void addStudyMinutesToProfile(int minutes) {
        UserProfile profile = db.getUserProfile();
        profile.setTotalStudyHours(profile.getTotalStudyHours() + minutes / 60.0);
        db.updateUserProfile(profile);
    }

    // ── Motivational messages ─────────────────────────────────────────────────

    public String getMotivationalMessage(int sessionsToday) {
        String[] messages = {
            "Ready to focus? Let's do this! 💪",
            "Great start! Keep going! 🚀",
            "You're building momentum! 🔥",
            "Halfway through — stay focused! ⚡",
            "Incredible dedication today! 🌟",
            "You're on fire! Keep it up! 🏆",
            "Legendary focus session! 🎯",
            "Absolutely unstoppable! 💯"
        };
        return messages[Math.min(sessionsToday, messages.length - 1)];
    }

    public String getMoodSuggestion(String mood) {
        return switch (mood) {
            case "😄 Great"    -> "You're energised! Tackle your hardest topic first.";
            case "🙂 Good"    -> "Solid energy – a standard Pomodoro session suits you.";
            case "😐 Okay"    -> "Steady pace – 20-min sessions with short breaks work best.";
            case "😔 Tired"   -> "Low energy today. Review notes or do lighter tasks.";
            case "😤 Stressed" -> "Breathe first. Start with something small to gain momentum.";
            default            -> "Choose a comfortable pace and enjoy the session!";
        };
    }
}
