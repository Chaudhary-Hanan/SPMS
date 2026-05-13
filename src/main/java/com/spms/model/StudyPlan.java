package com.spms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudyPlan {

    public enum Status { ACTIVE, COMPLETED, PAUSED }

    private int           id;
    private String        subject;
    private LocalDate     deadline;
    private int           difficulty;   // 1–5
    private double        dailyHours;
    private Status        status;
    private LocalDateTime createdAt;
    private List<StudyBlock> blocks = new ArrayList<>();

    public StudyPlan() {
        this.difficulty = 3;
        this.dailyHours = 2.0;
        this.status     = Status.ACTIVE;
        this.createdAt  = LocalDateTime.now();
    }

    public StudyPlan(String subject, LocalDate deadline,
                     int difficulty, double dailyHours) {
        this();
        this.subject    = subject;
        this.deadline   = deadline;
        this.difficulty = difficulty;
        this.dailyHours = dailyHours;
    }

    public double getCompletionPercentage() {
        if (blocks.isEmpty()) return 0.0;
        long done = blocks.stream().filter(StudyBlock::isCompleted).count();
        return (double) done / blocks.size() * 100.0;
    }

    public int getTotalPlannedHours() {
        return (int) blocks.stream().mapToDouble(StudyBlock::getHours).sum();
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                                { return id; }
    public void setId(int id)                         { this.id = id; }
    public String getSubject()                        { return subject; }
    public void setSubject(String subject)            { this.subject = subject; }
    public LocalDate getDeadline()                    { return deadline; }
    public void setDeadline(LocalDate deadline)       { this.deadline = deadline; }
    public int getDifficulty()                        { return difficulty; }
    public void setDifficulty(int d)                  { this.difficulty = d; }
    public double getDailyHours()                     { return dailyHours; }
    public void setDailyHours(double h)               { this.dailyHours = h; }
    public Status getStatus()                         { return status; }
    public void setStatus(Status status)              { this.status = status; }
    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime t)         { this.createdAt = t; }
    public List<StudyBlock> getBlocks()               { return blocks; }
    public void setBlocks(List<StudyBlock> blocks)    { this.blocks = blocks; }

    @Override public String toString() { return subject != null ? subject : ""; }
}
