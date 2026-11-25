package com.pharoah.knowledge;

// SANCHIT: Implemented ResourceType enum.
// Categorizes persistent knowledge assets stored in PHAROAH's knowledge repository.
public enum ResourceType {
    URL("Web URL / Online Source"),
    NOTE("Research Note / Conceptual Insight"),
    CODE_SNIPPET("Code Snippet / Implementation Example"),
    PAPER("Academic Paper / PDF Reference"),
    REFERENCE("Citation / Bibliography Entry");

    private final String description;

    ResourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
