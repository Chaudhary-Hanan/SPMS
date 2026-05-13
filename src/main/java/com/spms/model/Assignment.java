package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Assignment {

    public enum Priority { LOW, MEDIUM, HIGH, URGENT }
    public enum Status   { PENDING, IN_PROGRESS, COMPLETED, OVERDUE }

    private int           id;
    private String        title;
    private String        subject;
    private String        description;
    private LocalDate     dueDate;
    private Priority      priority;
    private Status        status;
    private LocalDateTime createdAt;

    public Assignment() {
        this.priority  = Priority.MEDIUM;
        this.status    = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Assignment(String title, String subject, String description,
                      LocalDate dueDate, Priority priority) {
        this();
        this.title       = title;
        this.subject     = subject;
        this.description = description;
        this.dueDate     = dueDate;
        this.priority    = priority;
    }

    public boolean isOverdue() {
        return dueDate != null
                && LocalDate.now().isAfter(dueDate)
                && status != Status.COMPLETED;
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }
    public String getSubject()                  { return subject; }
    public void setSubject(String subject)      { this.subject = subject; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
    public LocalDate getDueDate()               { return dueDate; }
    public void setDueDate(LocalDate dueDate)   { this.dueDate = dueDate; }
    public Priority getPriority()               { return priority; }
    public void setPriority(Priority priority)  { this.priority = priority; }
    public Status getStatus()                   { return status; }
    public void setStatus(Status status)        { this.status = status; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    @Override public String toString() { return title != null ? title : ""; }
}
