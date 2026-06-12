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
                I need you to write a professional CV tailored to the following job.
                Highlight my relevant skills, experience, and projects that match what the employer is looking for.

                ## Important Instructions
                - Do not invent professional work experience.
                - If work experience is missing, prioritize projects, education, and technical coursework.
                - Write the CV for a junior / graduate developer profile.
                - Match the job requirements honestly.
                - Emphasize transferable experience from projects.
                - Use Swedish if the job ad is in Swedish.
                - Keep it concise and suitable for a one-page CV.
                - Do not claim experience with technologies that are not listed in my profile.

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

                Please write a tailored CV that emphasizes the experience and skills most relevant to this job.
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
