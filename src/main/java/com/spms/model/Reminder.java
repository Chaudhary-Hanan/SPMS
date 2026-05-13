package com.spms.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Reminder {

    public enum Urgency         { LOW, MEDIUM, HIGH, CRITICAL }
    public enum ReminderStatus  { ACTIVE, SNOOZED, DISMISSED, DONE }

    private int            id;
    private String         title;
    private String         message;
    private LocalDateTime  dueDateTime;
    private Urgency        urgency;
    private ReminderStatus status;
    private LocalDateTime  snoozedUntil;
    private LocalDateTime  createdAt;

    public Reminder() {
        this.urgency   = Urgency.MEDIUM;
        this.status    = ReminderStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Reminder(String title, String message,
                    LocalDateTime dueDateTime, Urgency urgency) {
        this();
        this.title       = title;
        this.message     = message;
        this.dueDateTime = dueDateTime;
        this.urgency     = urgency;
    }

    public boolean isOverdue() {
        return dueDateTime != null
                && LocalDateTime.now().isAfter(dueDateTime)
                && status == ReminderStatus.ACTIVE;
    }

    public String getTimeUntilDue() {
        if (dueDateTime == null) return "No date set";
        Duration d = Duration.between(LocalDateTime.now(), dueDateTime);
        if (d.isNegative()) return "Overdue!";
        long hours = d.toHours();
        if (hours < 1)  return d.toMinutes() + " min remaining";
        if (hours < 24) return hours + "h remaining";
        return d.toDays() + " days remaining";
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }
    public String getTitle()                            { return title; }
    public void setTitle(String title)                  { this.title = title; }
    public String getMessage()                          { return message; }
    public void setMessage(String message)              { this.message = message; }
    public LocalDateTime getDueDateTime()               { return dueDateTime; }
    public void setDueDateTime(LocalDateTime t)         { this.dueDateTime = t; }
    public Urgency getUrgency()                         { return urgency; }
    public void setUrgency(Urgency urgency)             { this.urgency = urgency; }
    public ReminderStatus getStatus()                   { return status; }
    public void setStatus(ReminderStatus status)        { this.status = status; }
    public LocalDateTime getSnoozedUntil()              { return snoozedUntil; }
    public void setSnoozedUntil(LocalDateTime t)        { this.snoozedUntil = t; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }

    @Override public String toString() { return title != null ? title : ""; }
}
