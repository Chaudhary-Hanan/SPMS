package com.spms.service;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Pomodoro-style timer service.
 * Drives a javafx.animation.Timeline for precise, UI-safe countdown ticks.
 */
public class TimerService {

    private int focusDuration      = 25 * 60;  // seconds
    private int shortBreakDuration =  5 * 60;
    private int longBreakDuration  = 15 * 60;

    private int     timeRemaining;
    private boolean running;
    private boolean onBreak;
    private int     sessionsCompleted;
    private String  currentSubject = "";

    private Timeline         timeline;
    private Consumer<Integer> onTick;          // receives remaining seconds
    private Runnable          onSessionComplete;
    private Runnable          onBreakComplete;

    public TimerService() {
        reset();
    }

    // ── Control ──────────────────────────────────────────────────────────────

    public void start() {
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            timeline.setCycleCount(Animation.INDEFINITE);
        }
        running = true;
        timeline.play();
    }

    public void pause() {
        if (timeline != null) timeline.pause();
        running = false;
    }

    public void resume() {
        if (timeline != null) timeline.play();
        running = true;
    }

    public void reset() {
        if (timeline != null) { timeline.stop(); timeline = null; }
        timeRemaining = focusDuration;
        running = false;
        onBreak = false;
    }

    public void fullReset() {
        reset();
        sessionsCompleted = 0;
    }

    public void skipBreak() {
        if (onBreak) {
            if (timeline != null) { timeline.stop(); timeline = null; }
            running = false;
            onBreak = false;
            timeRemaining = focusDuration;
            if (onBreakComplete != null) onBreakComplete.run();
        }
    }

    // ── Internal tick ────────────────────────────────────────────────────────

    private void tick() {
        timeRemaining--;
        if (onTick != null) onTick.accept(timeRemaining);

        if (timeRemaining <= 0) {
            if (!onBreak) {
                sessionsCompleted++;
                if (timeline != null) { timeline.stop(); timeline = null; }
                running = false;
                if (onSessionComplete != null) onSessionComplete.run();
                beginBreak();
            } else {
                if (timeline != null) { timeline.stop(); timeline = null; }
                running = false;
                onBreak = false;
                timeRemaining = focusDuration;
                if (onBreakComplete != null) onBreakComplete.run();
            }
        }
    }

    private void beginBreak() {
        onBreak = true;
        timeRemaining = (sessionsCompleted % 4 == 0) ? longBreakDuration : shortBreakDuration;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public double getProgress() {
        int total = onBreak
                ? (sessionsCompleted % 4 == 0 ? longBreakDuration : shortBreakDuration)
                : focusDuration;
        if (total == 0) return 0.0;
        return 1.0 - (double) timeRemaining / total;
    }

    public String getFormattedTime() {
        int m = timeRemaining / 60;
        int s = timeRemaining % 60;
        return String.format("%02d:%02d", m, s);
    }

    public boolean isRunning()           { return running; }
    public boolean isOnBreak()           { return onBreak; }
    public int getSessionsCompleted()    { return sessionsCompleted; }
    public String getCurrentSubject()    { return currentSubject; }
    public void setCurrentSubject(String s) { this.currentSubject = s; }

    // ── Configuration ────────────────────────────────────────────────────────

    public int getFocusDurationMinutes()               { return focusDuration / 60; }
    public void setFocusDurationMinutes(int m)         { this.focusDuration = m * 60; if (!running && !onBreak) timeRemaining = focusDuration; }
    public int getShortBreakMinutes()                  { return shortBreakDuration / 60; }
    public void setShortBreakMinutes(int m)            { this.shortBreakDuration = m * 60; }
    public int getLongBreakMinutes()                   { return longBreakDuration / 60; }
    public void setLongBreakMinutes(int m)             { this.longBreakDuration = m * 60; }

    public void setOnTick(Consumer<Integer> handler)    { this.onTick = handler; }
    public void setOnSessionComplete(Runnable r)        { this.onSessionComplete = r; }
    public void setOnBreakComplete(Runnable r)          { this.onBreakComplete = r; }
}
