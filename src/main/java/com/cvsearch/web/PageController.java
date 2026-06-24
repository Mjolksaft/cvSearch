package com.cvsearch.web;

import com.cvsearch.generation.AiContentService;
import com.cvsearch.generation.CvPdfService;
import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.cvsearch.job.JobService;
import com.cvsearch.job.dto.JobResponse;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;
import com.cvsearch.userProfile.UserProfileService;
import com.cvsearch.userProfile.dto.ProfileResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class PageController {

    private final JobService jobService;
    private final UserProfileService profileService;
    private final UserProfileRepository profileRepository;
    private final AiContentService aiContentService;
    private final CvPdfService cvPdfService;
    private final ObjectMapper objectMapper;

    public PageController(JobService jobService, UserProfileService profileService,
                          UserProfileRepository profileRepository,
                          AiContentService aiContentService, CvPdfService cvPdfService,
                          ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.profileService = profileService;
        this.profileRepository = profileRepository;
        this.aiContentService = aiContentService;
        this.cvPdfService = cvPdfService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/jobs";
    }

    @GetMapping("/jobs")
    public String jobList(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String saved,
            @RequestParam(required = false) String appliedBefore,
            @RequestParam(required = false) String appliedAfter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appliedDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        if (company != null && company.isBlank()) company = null;
        if (title != null && title.isBlank()) title = null;
        if (status != null && status.isBlank()) status = null;
        if (location != null && location.isBlank()) location = null;

        Boolean savedBool = null;
        if (saved != null && !saved.isBlank()) {
            savedBool = Boolean.parseBoolean(saved);
        }

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        LocalDate beforeDate = parseDate(appliedBefore);
        LocalDate afterDate = parseDate(appliedAfter);

        Page<JobResponse> jobPage;

        if (company != null || title != null || status != null
                || location != null || savedBool != null
                || beforeDate != null || afterDate != null) {
            jobPage = jobService.search(company, title, status, location, savedBool, beforeDate, afterDate, pageable);
        } else {
            jobPage = jobService.GetAllJobs(pageable);
        }

        model.addAttribute("jobs", jobPage.getContent());
        model.addAttribute("currentPage", jobPage.getNumber());
        model.addAttribute("totalPages", jobPage.getTotalPages());
        model.addAttribute("totalElements", jobPage.getTotalElements());
        model.addAttribute("pageSize", size);

        model.addAttribute("filterCompany", company);
        model.addAttribute("filterTitle", title);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterSaved", savedBool);

        return "jobs/list";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable Long id, Model model) {
        JobResponse job = jobService.getJobById(id);
        model.addAttribute("job", job);
        return "jobs/detail";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        try {
            ProfileResponse profile = profileService.getByUserId(1L);
            model.addAttribute("profile", profile);
        } catch (Exception e) {
        }
        model.addAttribute("userId", 1L);
        return "profile/index";
    }

    @GetMapping("/jobs/{jobId}/cv/edit")
    public String showCvEditPage(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "1") Long userId,
            Model model) {

        TailoredProfileRequest content = aiContentService.generateContent(jobId, userId);

        // Render the CV template as HTML for inline editing
        String cvHtml = cvPdfService.generateTailoredHtml(jobId, userId, content, "sidebar", true);

        // Load full profile skills for the "+" add skill dropdown
        List<String> allSkills = List.of();
        try {
            UserProfile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Profile not found"));
            allSkills = objectMapper.readValue(profile.getSkills(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // profile not found or skills not parseable — fallback to empty
        }

        model.addAttribute("jobId", jobId);
        model.addAttribute("userId", userId);
        model.addAttribute("cvHtml", cvHtml);

        // Serialize read-only data as JSON strings for JS
        try {
            String eduJson = objectMapper.writeValueAsString(content.education() != null ? content.education() : List.of());
            String langJson = objectMapper.writeValueAsString(content.languages() != null ? content.languages() : List.of());
            String certJson = objectMapper.writeValueAsString(content.certifications() != null ? content.certifications() : List.of());
            String allSkillsJson = objectMapper.writeValueAsString(allSkills);
            model.addAttribute("educationJson", eduJson);
            model.addAttribute("languagesJson", langJson);
            model.addAttribute("certificationsJson", certJson);
            model.addAttribute("allSkillsJson", allSkillsJson);
        } catch (Exception e) {
            model.addAttribute("educationJson", "[]");
            model.addAttribute("languagesJson", "[]");
            model.addAttribute("certificationsJson", "[]");
            model.addAttribute("allSkillsJson", "[]");
        }

        return "jobs/cv-edit";
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
