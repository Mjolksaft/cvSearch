package com.cvsearch.generation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvsearch.job.Job;
import com.cvsearch.job.JobRepository;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CvPromptService {

    private final JobRepository jobRepository;
    private final UserProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final JobDescriptionSummarizer summarizer;

    public CvPromptService(JobRepository jobRepository, UserProfileRepository profileRepository,
            ObjectMapper objectMapper, JobDescriptionSummarizer summarizer) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.summarizer = summarizer;
    }

    /** Fetch a job and return its description summarized. */
    private String summarizedDescription(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        return summarizer.summarize(job.getDescription());
    }

    /**
     * Step 1: Ask the AI to select the 1–3 most relevant projects for the job.
     */
    public String buildProjectSelectionPrompt(Long jobId, Long userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        return """
                CRITICAL INSTRUCTION — YOU MUST SELECT EXACTLY 1 TO 3 PROJECTS.

                Below is a job listing followed by a list of MY projects.
                You MUST choose 1 to 3 of MY projects that best match the job.
                DO NOT invent projects. DO NOT return projects not in the list.
                DO NOT return all projects — only the most relevant ones.

                How to decide relevance:
                - Match the technologies used in the project to the technologies
                  mentioned in the job description.
                - Match the domain (e.g. web, mobile, backend, frontend).
                - If a project's technologies overlap with the job requirements,
                  it is relevant. Otherwise, skip it.

                Return ONLY a JSON array of project names, e.g. ["Project Name"].
                Example: ["E-commerce Platform"]
                Do NOT include any explanation. Do NOT include markdown.
                ONLY a JSON array.

                --- JOB DETAILS ---

                Title: %s
                Company: %s
                Description:
                %s

                --- MY PROJECTS (select from these ONLY) ---

                %s

                --- END ---

                Return ONLY a JSON array of up to 3 project names from the list above.
                """
                .formatted(
                        safe(job.getTitle()),
                        safe(job.getCompany() != null ? job.getCompany().getName() : ""),
                        summarizer.summarize(job.getDescription()),
                        formatProjectsForPrompt(profile.getProjects()));
    }

    /**
     * Step 2: Build the full CV prompt with ONLY the pre-selected projects.
     */
    public String buildCvPrompt(Long jobId, Long userId, List<String> selectedProjectNames) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        return """
                Write a tailored CV section for the following job using ONLY the
                projects listed below. Return ONLY a valid JSON object.

                ## CRITICAL — HONESTY RULES

                - Do not invent professional experience.
                - Do not invent projects or technologies.
                - Do not claim experience with technologies not in my profile.
                - Match the job requirements honestly.

                ## PAGE LAYOUT CONSTRAINTS — MUST FIT ONE A4 PAGE

                • Summary:       max 50 words, 4 lines
                • Skills:        max 10, order by relevance
                • Per project:
                  - Description: max 60 words, 4–5 lines
                  - Highlights:  max 3, each max 12 words (1 line)
                  - Only pick projects that fit if it doesnt fit dont pick it 
                  - take 1-3 projects 
                • Coursework:    max 5 courses
                • Education:     return EXACTLY as provided (do NOT rewrite)
                • Languages:     return EXACTLY as provided (do NOT rewrite)
                • Certifications: return EXACTLY as provided (do NOT rewrite)

                ## LANGUAGE

                If the job ad is in Swedish → write summary, descriptions, and
                highlights in Swedish. If English → write in English.

                ## RESPONSE FORMAT

                {
                  "summary": "...",
                  "skills": ["skill1", "skill2"],
                  "projects": [
                    {
                      "name": "Project Name",
                      "description": "...",
                      "technologies": ["tech1", "tech2"],
                      "highlights": ["highlight1", "highlight2", "highlight3"]
                    }
                  ],
                  "education": [{ "degree": "...", "school": "...", "year": "..." }],
                  "languages": ["..."],
                  "certifications": ["..."],
                  "coursework": ["course1", "course2"]
                }

                ## Job Details

                Title: %s
                Company: %s
                Location: %s
                Description:
                %s

                ## My Profile (use ONLY these projects)

                Summary (current):
                %s

                Skills:
                %s

                Projects (these are the ONLY projects to include):
                %s

                Education (return EXACTLY as provided):
                %s

                Languages (return EXACTLY as provided):
                %s

                Certifications (return EXACTLY as provided):
                %s

                Coursework (select max 5 most relevant):
                %s

                Return ONLY the JSON object. Remember the page budget!
                """
                .formatted(
                        safe(job.getTitle()),
                        safe(job.getCompany() != null ? job.getCompany().getName() : ""),
                        safe(job.getLocation()),
                        summarizer.summarize(job.getDescription()),
                        safe(profile.getSummary()),
                        safe(profile.getSkills()),
                        formatSelectedProjectsForPrompt(profile.getProjects(), selectedProjectNames),
                        safe(profile.getEducation()),
                        safe(profile.getLanguages()),
                        safe(profile.getCertifications()),
                        safe(profile.getCoursework()));
    }

    public String buildCoverLetterPrompt(Long jobId, Long userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        return """
                I need you to write a professional cover letter tailored to the following job application.
                Highlight my relevant skills, experience, and projects that match what the employer is looking for.
                Address it to the hiring manager and make it compelling.

                ## Job Details
                Title: %s
                Company: %s
                Location: %s
                Description:
                %s

                ## My Profile
                Summary:
                %s

                Skills:
                %s

                Projects:
                %s

                Education:
                %s

                Languages:
                %s

                Certifications:
                %s

                Please write a tailored cover letter that emphasizes why I am a great fit for this role.
                """
                .formatted(
                        safe(job.getTitle()),
                        safe(job.getCompany() != null ? job.getCompany().getName() : ""),
                        safe(job.getLocation()),
                        summarizer.summarize(job.getDescription()),
                        safe(profile.getSummary()),
                        safe(profile.getSkills()),
                        safe(profile.getProjects()),
                        safe(profile.getEducation()),
                        safe(profile.getLanguages()),
                        safe(profile.getCertifications()));
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    // ================================================================
    //  4-Step prompt builders
    // ================================================================

    /** Step 1: Extract required and preferred technologies from the job ad. */
    public String buildExtractRequirementsPrompt(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        return """
                Extract the required and preferred technologies from this job posting.

                Return ONLY valid JSON in this format:
                {
                  "required": ["Java", "Spring Boot"],
                  "preferred": ["Oracle SQL", "OpenShift"]
                }

                - "required" = technologies explicitly mentioned as requirements.
                - "preferred" = technologies mentioned as nice-to-have, bonus, or preferred.
                - Include programming languages, frameworks, databases, tools, platforms.
                - If a technology category is not mentioned at all, omit it.
                - Return empty arrays if nothing is found.

                Job title: %s
                Description:
                %s

                Write ONLY the JSON. No explanation, no preface.
                """
                .formatted(
                        safe(job.getTitle()),
                        summarizer.summarize(job.getDescription()));
    }

    /** Step 2: Select 1-3 projects whose technologies overlap with the job requirements. */
    public String buildSelectProjectsPrompt(String requirementsJson, String projectsSummaryJson) {
        return """
                Select the 1-3 projects that best match the job requirements.
                Only use technology overlap to decide relevance.

                Job requirements:
                %s

                My projects (name + technologies only):
                %s

                Write ONLY a JSON array of project names, e.g. ["Project A", "Project B"].
                Pick at most 3. If none match, return an empty array [].

                No explanation, no preface — ONLY the JSON array.
                """
                .formatted(
                        safe(requirementsJson),
                        safe(projectsSummaryJson));
    }

    /** Step 3: Rewrite selected projects' descriptions and highlights to fit the job. */
    public String buildRewriteProjectsPrompt(String requirementsJson, String selectedProjectsJson,
                                              String spaceAllocation) {
        return """
                Rewrite the description of each selected project to be tailored
                to the job requirements below. Write ONLY a short description.

                Job requirements:
                %s

                Selected projects (current descriptions):
                %s

                SPACE ALLOCATION — each project's word budget for the description:
                %s

                RULES:
                - "description": ONE short sentence (max 15 words) summarising the
                  project and why it is relevant to this job. Do not write paragraphs.
                - "highlights": You MUST select 4-8 highlights from the original
                  project data below. Order them by relevance to this job.
                  Do NOT pick fewer than 4 — the CV will look empty.
                  You may ONLY use highlights that already exist in the project data
                  below — do NOT invent new ones.
                - The "technologies" array MUST be returned EXACTLY as given.
                  Do NOT add or remove any technologies.

                Write ONLY a JSON array in this format:
                [
                  {
                    "name": "Project Name",
                    "description": "Short sentence, max 15 words.",
                    "technologies": ["tech1", "tech2"],
                    "highlights": ["selected highlight 1", "selected highlight 2"]
                  }
                ]

                REMEMBER: valid JSON only, at least 4 highlights per project, order by relevance.
                """
                .formatted(
                        safe(requirementsJson),
                        safe(selectedProjectsJson),
                        safe(spaceAllocation));
    }

    /** Step 4: Write final CV profile (summary + skills) using rewritten projects + profile data. */
    public String buildFinalProfilePrompt(
            String requirementsJson,
            String rewrittenProjectsJson,
            String summary,
            String skills,
            String education,
            String languages,
            String certifications,
            String coursework) {

        return """
                Write a tailored CV profile for the job below.

                Job requirements:
                %s

                Rewritten projects (use these as-is):
                %s

                My current summary (rewrite to fit the job):
                %s

                My skills (select at most 10 most relevant, reorder):
                %s

                Education (keep EXACTLY as-is):
                %s

                Languages (keep EXACTLY as-is):
                %s

                Certifications (keep EXACTLY as-is):
                %s

                Coursework (select at most 5 most relevant):
                %s

                Write ONLY this JSON. No explanation, no preface:
                {
                  "summary": "2-3 sentence summary tailored to the job",
                  "skills": ["skill1", "skill2", ...]
                }

                - Summary: max 50 words, tailored to the job.
                - Skills: max 10, ordered by relevance.
                - Do NOT repeat project details in the summary.
                """
                .formatted(
                        safe(requirementsJson),
                        safe(rewrittenProjectsJson),
                        safe(summary),
                        safe(skills),
                        safe(education),
                        safe(languages),
                        safe(certifications),
                        safe(coursework));
    }

    // ================================================================
    //  Formatting helpers
    // ================================================================
    private String formatProjectsForPrompt(String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return "(no projects)";
        try {
            var projects = objectMapper.readValue(projectsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (projects.isEmpty()) return "(no projects)";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < projects.size(); i++) {
                Map<String, Object> p = projects.get(i);
                sb.append("Project ").append(i + 1).append(": ").append(p.getOrDefault("name", "")).append("\n");
                sb.append("  Technologies: ").append(p.getOrDefault("technologies", "")).append("\n");
                sb.append("  Description: ").append(p.getOrDefault("description", "")).append("\n");
                @SuppressWarnings("unchecked")
                List<String> highlights = (List<String>) p.get("highlights");
                if (highlights != null && !highlights.isEmpty()) {
                    sb.append("  Highlights:\n");
                    for (String h : highlights) {
                        sb.append("    • ").append(h).append("\n");
                    }
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return projectsJson;
        }
    }

    /** Format only the selected projects (by name) for the CV writing prompt. */
    public String formatSelectedProjectsForPrompt(String projectsJson, List<String> selectedNames) {
        if (projectsJson == null || projectsJson.isBlank()) return "(no projects)";
        if (selectedNames == null || selectedNames.isEmpty()) return "(no projects selected)";
        try {
            var allProjects = objectMapper.readValue(projectsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            var selectedNamesLower = selectedNames.stream()
                    .map(n -> n.trim().toLowerCase())
                    .collect(Collectors.toSet());

            List<Map<String, Object>> filtered = allProjects.stream()
                    .filter(p -> selectedNamesLower.contains(
                            String.valueOf(p.getOrDefault("name", "")).trim().toLowerCase()))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                // fallback: show all projects with a warning
                return "(WARNING: none of the selected projects were found in profile)\n"
                        + formatProjectsForPrompt(projectsJson);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("PRE-SELECTED PROJECTS (ONLY these must appear in the CV):\n\n");
            for (int i = 0; i < filtered.size(); i++) {
                Map<String, Object> p = filtered.get(i);
                sb.append("Project ").append(i + 1).append(": ").append(p.getOrDefault("name", "")).append("\n");
                sb.append("  Technologies: ").append(p.getOrDefault("technologies", "")).append("\n");
                sb.append("  Description: ").append(p.getOrDefault("description", "")).append("\n");
                @SuppressWarnings("unchecked")
                List<String> highlights = (List<String>) p.get("highlights");
                if (highlights != null && !highlights.isEmpty()) {
                    sb.append("  Highlights:\n");
                    for (String h : highlights) {
                        sb.append("    • ").append(h).append("\n");
                    }
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return formatProjectsForPrompt(projectsJson);
        }
    }

    /** Format projects with name + technologies only (no descriptions). Used for Step 2. */
    public String formatProjectSummariesForPrompt(String projectsJson) {
        if (projectsJson == null || projectsJson.isBlank()) return "(no projects)";
        try {
            var projects = objectMapper.readValue(projectsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (projects.isEmpty()) return "(no projects)";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < projects.size(); i++) {
                Map<String, Object> p = projects.get(i);
                sb.append("- ").append(p.getOrDefault("name", ""));
                sb.append(" [").append(p.getOrDefault("technologies", "")).append("]");
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return projectsJson;
        }
    }
}
