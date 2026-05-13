package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Goal {

    public enum Type { DAILY, WEEKLY, MONTHLY, CUSTOM }

    private int           id;
    private String        title;
    private String        description;
    private Type          type;
    private double        targetValue;
    private double        currentValue;
    private String        unit;
    private LocalDate     dueDate;
    private boolean       completed;
    private LocalDateTime createdAt;

    public Goal() {
        this.type         = Type.DAILY;
        this.targetValue  = 1.0;
        this.currentValue = 0.0;
        this.completed    = false;
        this.createdAt    = LocalDateTime.now();
    }

    public Goal(String title, String description, Type type,
                double targetValue, String unit, LocalDate dueDate) {
        this();
        this.title       = title;
        this.description = description;
        this.type        = type;
        this.targetValue = targetValue;
        this.unit        = unit;
        this.dueDate     = dueDate;
    }

    public double getProgressPercentage() {
        if (targetValue <= 0) return 0.0;
        return Math.min(100.0, (currentValue / targetValue) * 100.0);
    }

    public boolean isExpired() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) && !completed;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }
    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }
    public Type getType()                           { return type; }
    public void setType(Type type)                  { this.type = type; }
    public double getTargetValue()                  { return targetValue; }
    public void setTargetValue(double targetValue)  { this.targetValue = targetValue; }
    public double getCurrentValue()                 { return currentValue; }
    public void setCurrentValue(double currentValue){ this.currentValue = currentValue; }
    public String getUnit()                         { return unit; }
    public void setUnit(String unit)                { this.unit = unit; }
    public LocalDate getDueDate()                   { return dueDate; }
    public void setDueDate(LocalDate dueDate)       { this.dueDate = dueDate; }
    public boolean isCompleted()                    { return completed; }
    public void setCompleted(boolean completed)     { this.completed = completed; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    @Override public String toString() { return title != null ? title : ""; }
}
