package com.cvsearch.generation.dto;

import java.util.List;

public record TailoredProfileRequest(
        String summary,
        List<String> skills,
        List<ProjectEntry> projects,
        List<EducationEntry> education,
        List<String> languages,
        List<String> certifications) {

    public record ProjectEntry(
            String name,
            String description,
            List<String> technologies,
            List<String> highlights) {}

    public record EducationEntry(
            String degree,
            String school,
            String year) {}
}
