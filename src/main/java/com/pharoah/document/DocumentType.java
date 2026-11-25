package com.pharoah.document;

import java.util.List;

// SANCHIT: Implemented DocumentType enum.
// Defines structured document formats and their required sections.
public enum DocumentType {
    RESEARCH_PAPER("Academic Research Paper", List.of(
            "Abstract",
            "Introduction",
            "Literature Review",
            "Methodology",
            "Results and Findings",
            "Discussion",
            "Conclusion",
            "References"
    )),

    TECHNICAL_BLOG("Technical Blog Post", List.of(
            "Title & Hook",
            "Introduction & Motivation",
            "Core Architectural Concepts",
            "Technical Code Examples & Implementation",
            "Performance & Best Practices",
            "Conclusion & Next Steps"
    )),

    TECHNICAL_DOCUMENTATION("Technical System Documentation", List.of(
            "System Overview",
            "Architecture & Component Design",
            "Installation & Prerequisites",
            "Configuration & Environment",
            "Usage Guidelines",
            "API Documentation & Endpoints",
            "Troubleshooting & Maintenance"
    )),

    BUSINESS_PROPOSAL("Business & Project Proposal", List.of(
            "Executive Summary",
            "Problem Statement",
            "Proposed Solution",
            "Implementation Timeline & Milestones",
            "Resource & Financial Analysis",
            "Conclusion & Call to Action"
    ));

    private final String displayName;
    private final List<String> standardSections;

    DocumentType(String displayName, List<String> standardSections) {
        this.displayName = displayName;
        this.standardSections = standardSections;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getStandardSections() {
        return standardSections;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
