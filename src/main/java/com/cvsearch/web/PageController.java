package com.cvsearch.web;

import com.cvsearch.job.JobService;
import com.cvsearch.job.dto.JobResponse;
import com.cvsearch.userProfile.UserProfileService;
import com.cvsearch.userProfile.dto.ProfileResponse;

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

@Controller
public class PageController {

    private final JobService jobService;
    private final UserProfileService profileService;

    public PageController(JobService jobService, UserProfileService profileService) {
        this.jobService = jobService;
        this.profileService = profileService;
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
            @RequestParam(required = false) String savedParam,
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

        Boolean saved = null;
        if (savedParam != null && !savedParam.isBlank()) {
            saved = Boolean.parseBoolean(savedParam);
        }

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        LocalDate beforeDate = parseDate(appliedBefore);
        LocalDate afterDate = parseDate(appliedAfter);

        Page<JobResponse> jobPage;

        if (company != null || title != null || status != null
                || location != null || saved != null
                || beforeDate != null || afterDate != null) {
            jobPage = jobService.search(company, title, status, location, saved, beforeDate, afterDate, pageable);
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
        model.addAttribute("filterSaved", saved);

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
            // No profile yet
        }
        model.addAttribute("userId", 1L);
        return "profile/index";
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
