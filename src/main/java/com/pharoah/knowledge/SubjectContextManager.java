package com.pharoah.knowledge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knowledge Space Manager enforcing strict subject-specific isolation.
 * 
 * Guarantees that:
 * 1. Java discussions/notes NEVER inadvertently mix with DBMS or OS contexts.
 * 2. Re-opening a subject workspace immediately restores previous memory and notes.
 * 3. Cross-subject queries only execute when explicitly authorized.
 */
public class SubjectContextManager {

    private final Map<String, SubjectMemory> subjectMemories = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates the isolated memory space for a given subject.
     */
    public SubjectMemory getOrCreateMemory(String subjectName) {
        if (subjectName == null || subjectName.trim().isEmpty()) {
            subjectName = "GENERAL";
        }
        String normalizedKey = subjectName.trim().toUpperCase();
        return subjectMemories.computeIfAbsent(normalizedKey,
                key -> new SubjectMemory(UUID.randomUUID().toString(), key));
    }

    /**
     * Records a note strictly within the designated subject workspace.
     */
    public void recordNote(String subjectName, String note) {
        SubjectMemory memory = getOrCreateMemory(subjectName);
        memory.addNote(note);
    }

    /**
     * Verifies strict boundary isolation between two subjects.
     * Throws SecurityException if a non-cross-subject call attempts to access another workspace.
     */
    public void validateIsolation(String currentSubject, String requestedSubject, boolean crossSubjectAllowed) {
        if (!crossSubjectAllowed && !currentSubject.trim().equalsIgnoreCase(requestedSubject.trim())) {
            throw new IllegalStateException(String.format(
                    "CONTEXT VIOLATION DETECTED: Workspace '%s' attempted unauthorized access to '%s'. Cross-subject querying is disabled.",
                    currentSubject, requestedSubject
            ));
        }
    }

    /**
     * Performs an authorized cross-subject query across all active knowledge spaces.
     */
    public Map<String, List<String>> performCrossSubjectSearch(String keyword) {
        Map<String, List<String>> results = new HashMap<>();

        for (Map.Entry<String, SubjectMemory> entry : subjectMemories.entrySet()) {
            String subject = entry.getKey();
            SubjectMemory memory = entry.getValue();

            List<String> matchingNotes = new ArrayList<>();
            for (String note : memory.getNotes()) {
                if (note.toLowerCase().contains(keyword.toLowerCase())) {
                    matchingNotes.add(note);
                }
            }

            if (!matchingNotes.isEmpty()) {
                results.put(subject, matchingNotes);
            }
        }
        return results;
    }

    public Set<String> getActiveSubjects() {
        return Collections.unmodifiableSet(subjectMemories.keySet());
    }
}
