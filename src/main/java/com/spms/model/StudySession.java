package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class StudySession {

    private int           id;
    private String        subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int           durationMinutes;
    private String        notes;
    private LocalDate     sessionDate;
    private boolean       focusMode;

    public StudySession() {
        this.sessionDate = LocalDate.now();
        this.startTime   = LocalDateTime.now();
    }

    public StudySession(String subject, int durationMinutes,
                        String notes, boolean focusMode) {
        this();
        this.subject         = subject;
        this.durationMinutes = durationMinutes;
        this.notes           = notes;
        this.focusMode       = focusMode;
        this.endTime         = this.startTime.plusMinutes(durationMinutes);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                                   { return id; }
    public void setId(int id)                            { this.id = id; }
    public String getSubject()                           { return subject; }
    public void setSubject(String subject)               { this.subject = subject; }
    public LocalDateTime getStartTime()                  { return startTime; }
    public void setStartTime(LocalDateTime t)            { this.startTime = t; }
    public LocalDateTime getEndTime()                    { return endTime; }
    public void setEndTime(LocalDateTime t)              { this.endTime = t; }
    public int getDurationMinutes()                      { return durationMinutes; }
    public void setDurationMinutes(int d)                { this.durationMinutes = d; }
    public String getNotes()                             { return notes; }
    public void setNotes(String notes)                   { this.notes = notes; }
    public LocalDate getSessionDate()                    { return sessionDate; }
    public void setSessionDate(LocalDate d)              { this.sessionDate = d; }
    public boolean isFocusMode()                         { return focusMode; }
    public void setFocusMode(boolean fm)                 { this.focusMode = fm; }

    @Override
    public String toString() {
        return subject + " (" + durationMinutes + " min)";
    }
}
