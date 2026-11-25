package com.pharoah.knowledge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Isolated contextual memory store for a single Subject Knowledge Space.
 */
public class SubjectMemory {

    private final String subjectId;
    private final String subjectName;
    private final List<String> notes = new ArrayList<>();
    private final List<String> conversationHistory = new ArrayList<>();
    private LocalDateTime lastAccessedAt;

    public SubjectMemory(String subjectId, String subjectName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public synchronized void addNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            notes.add("[" + LocalDateTime.now().toLocalTime().withNano(0) + "] " + note.trim());
            this.lastAccessedAt = LocalDateTime.now();
        }
    }

    public synchronized void addConversationMessage(String sender, String message) {
        if (message != null && !message.trim().isEmpty()) {
            conversationHistory.add(String.format("[%s] %s: %s",
                    LocalDateTime.now().toLocalTime().withNano(0), sender, message.trim()));
            this.lastAccessedAt = LocalDateTime.now();
        }
    }

    public synchronized List<String> getNotes() {
        return Collections.unmodifiableList(new ArrayList<>(notes));
    }

    public synchronized List<String> getConversationHistory() {
        return Collections.unmodifiableList(new ArrayList<>(conversationHistory));
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    /**
     * Synthesizes a structured memory snapshot to provide context to agents.
     */
    public synchronized String synthesizeContextSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Subject Context: ").append(subjectName).append(" ===\n");
        sb.append("Notes Count: ").append(notes.size()).append("\n");
        sb.append("Recent Notes:\n");
        int count = 0;
        for (int i = notes.size() - 1; i >= 0 && count < 3; i--, count++) {
            sb.append("  - ").append(notes.get(i)).append("\n");
        }
        sb.append("Conversation Turns: ").append(conversationHistory.size()).append("\n");
        return sb.toString();
    }
}
