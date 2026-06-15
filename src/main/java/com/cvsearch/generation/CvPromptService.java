package com.cvsearch.generation;

import org.springframework.stereotype.Service;

import com.cvsearch.job.Job;
import com.cvsearch.job.JobRepository;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CvPromptService {

    private final JobRepository jobRepository;
    private final UserProfileRepository profileRepository;

    public CvPromptService(JobRepository jobRepository, UserProfileRepository profileRepository) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
    }

    public String buildCvPrompt(Long jobId, Long userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

        return """
                I need you to filter and tailor my profile for the following job.
                Do NOT write a full CV. Instead, return ONLY a JSON object with my profile
                fields rewritten to highlight what is most relevant for this specific job.

                ## Important Instructions
                - Do not invent professional work experience.
                - Prioritize projects, education, and technical coursework over work experience.
                - Write for a junior / graduate developer profile.
                - Match the job requirements honestly.
                - Emphasize transferable experience from projects.
                - Use Swedish if the job ad is in Swedish.
                - Keep the summary concise (2-3 sentences).
                - List at most 10 skills, ordered by relevance to this job.
                - Include at most 3 projects, ordered by relevance.
                - At most 4 highlights per project.
                - Do not claim experience with technologies not in my profile.

                Return your response as valid JSON in this exact format (no markdown, no code blocks):
                {
                  "summary": "rewritten summary here",
                  "skills": ["skill1", "skill2"],
                  "projects": [
                    {
                      "name": "Project name",
                      "description": "Brief description focusing on relevant aspects",
                      "technologies": ["tech1", "tech2"],
                      "highlights": ["relevant highlight 1", "relevant highlight 2"]
                    }
                  ],
                  "education": [
                    {
                      "degree": "Degree name",
                      "school": "School name",
                      "year": "2024"
                    }
                  ],
                  "languages": ["Language (level)"],
                  "certifications": ["Certification name"]
                }

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

                Return ONLY the JSON object, nothing else.
                """
                .formatted(
                        safe(job.getTitle()),
                        safe(job.getCompany() != null ? job.getCompany().getName() : ""),
                        safe(job.getLocation()),
                        safe(job.getDescription()),
                        safe(profile.getSummary()),
                        safe(profile.getSkills()),
                        safe(profile.getProjects()),
                        safe(profile.getEducation()),
                        safe(profile.getLanguages()),
                        safe(profile.getCertifications()));
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
                        safe(job.getDescription()),
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
}
