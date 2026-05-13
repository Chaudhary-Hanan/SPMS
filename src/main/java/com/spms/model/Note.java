package com.spms.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Note {

    private int           id;
    private String        title;
    private String        content;
    private String        subject;
    private String        tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Note() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Note(String title, String content, String subject, String tags) {
        this();
        this.title   = title;
        this.content = content;
        this.subject = subject;
        this.tags    = tags;
    }

    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                     .map(String::trim)
                     .filter(t -> !t.isEmpty())
                     .toList();
    }

    public String getPreview() {
        if (content == null || content.isBlank()) return "No content";
        String single = content.replace('\n', ' ').trim();
        return single.length() > 80 ? single.substring(0, 77) + "..." : single;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }
    public String getContent()                      { return content; }
    public void setContent(String content)          { this.content = content; }
    public String getSubject()                      { return subject; }
    public void setSubject(String subject)          { this.subject = subject; }
    public String getTags()                         { return tags; }
    public void setTags(String tags)                { this.tags = tags; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)       { this.updatedAt = t; }

    @Override public String toString() { return title != null ? title : ""; }
}
