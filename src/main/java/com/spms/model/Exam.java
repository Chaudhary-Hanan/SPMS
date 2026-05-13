package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class Exam {

    private int           id;
    private String        subject;
    private LocalDate     examDate;
    private LocalTime     examTime;
    private String        location;
    private String        notes;
    private LocalDateTime createdAt;

    public Exam() {
        this.createdAt = LocalDateTime.now();
    }

    public Exam(String subject, LocalDate examDate, LocalTime examTime,
                String location, String notes) {
        this();
        this.subject  = subject;
        this.examDate = examDate;
        this.examTime = examTime;
        this.location = location;
        this.notes    = notes;
    }

    public long getDaysUntil() {
        if (examDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), examDate);
    }

    public boolean isPassed() {
        if (examDate == null) return false;
        if (LocalDate.now().isAfter(examDate)) return true;
        if (LocalDate.now().equals(examDate) && examTime != null) {
            return LocalTime.now().isAfter(examTime);
        }
        return false;
    }

    public LocalDateTime getExamDateTime() {
        if (examDate == null) return null;
        return LocalDateTime.of(examDate, examTime != null ? examTime : LocalTime.MIDNIGHT);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public String getSubject()                   { return subject; }
    public void setSubject(String subject)       { this.subject = subject; }
    public LocalDate getExamDate()               { return examDate; }
    public void setExamDate(LocalDate d)         { this.examDate = d; }
    public LocalTime getExamTime()               { return examTime; }
    public void setExamTime(LocalTime t)         { this.examTime = t; }
    public String getLocation()                  { return location; }
    public void setLocation(String location)     { this.location = location; }
    public String getNotes()                     { return notes; }
    public void setNotes(String notes)           { this.notes = notes; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime t)    { this.createdAt = t; }

    @Override public String toString() { return subject != null ? subject : ""; }
}
