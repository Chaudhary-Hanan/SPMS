package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudyBlock {

    private int           id;
    private int           planId;
    private LocalDate     blockDate;
    private double        hours;
    private String        topic;
    private boolean       completed;
    private LocalDateTime createdAt;

    public StudyBlock() {
        this.createdAt = LocalDateTime.now();
    }

    public StudyBlock(int planId, LocalDate blockDate, double hours, String topic) {
        this();
        this.planId    = planId;
        this.blockDate = blockDate;
        this.hours     = hours;
        this.topic     = topic;
        this.completed = false;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public int getPlanId()                       { return planId; }
    public void setPlanId(int planId)            { this.planId = planId; }
    public LocalDate getBlockDate()              { return blockDate; }
    public void setBlockDate(LocalDate d)        { this.blockDate = d; }
    public double getHours()                     { return hours; }
    public void setHours(double hours)           { this.hours = hours; }
    public String getTopic()                     { return topic; }
    public void setTopic(String topic)           { this.topic = topic; }
    public boolean isCompleted()                 { return completed; }
    public void setCompleted(boolean completed)  { this.completed = completed; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime t)    { this.createdAt = t; }

    @Override
    public String toString() { return topic != null ? topic : "Study Block"; }
}
