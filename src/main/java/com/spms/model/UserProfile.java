package com.spms.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class UserProfile {

    private int       id                     = 1;
    private String    name                   = "Student";
    private int       streakDays             = 0;
    private LocalDate lastStudyDate;
    private double    totalStudyHours        = 0.0;
    private int       totalCompletedSessions = 0;
    private String    theme                  = "dark";

    public UserProfile() {}

    public UserProfile(String name) {
        this.name = name;
    }

    public String getGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                                     { return id; }
    public void setId(int id)                              { this.id = id; }
    public String getName()                                { return name; }
    public void setName(String name)                       { this.name = name; }
    public int getStreakDays()                             { return streakDays; }
    public void setStreakDays(int s)                       { this.streakDays = s; }
    public LocalDate getLastStudyDate()                    { return lastStudyDate; }
    public void setLastStudyDate(LocalDate d)              { this.lastStudyDate = d; }
    public double getTotalStudyHours()                     { return totalStudyHours; }
    public void setTotalStudyHours(double h)               { this.totalStudyHours = h; }
    public int getTotalCompletedSessions()                 { return totalCompletedSessions; }
    public void setTotalCompletedSessions(int s)           { this.totalCompletedSessions = s; }
    public String getTheme()                               { return theme; }
    public void setTheme(String theme)                     { this.theme = theme; }
}
