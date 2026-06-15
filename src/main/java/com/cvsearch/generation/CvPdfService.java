package com.cvsearch.generation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.cvsearch.generation.dto.TailoredProfileRequest.EducationEntry;
import com.cvsearch.generation.dto.TailoredProfileRequest.ProjectEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.cvsearch.job.Job;
import com.cvsearch.job.JobRepository;
import com.cvsearch.user.User;
import com.cvsearch.user.UserRepository;
import com.cvsearch.userProfile.UserProfile;
import com.cvsearch.userProfile.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CvPdfService {

    private static final Logger log = LoggerFactory.getLogger(CvPdfService.class);

    private final JobRepository jobRepository;
    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    public CvPdfService(JobRepository jobRepository,
                        UserProfileRepository profileRepository,
                        UserRepository userRepository,
                        TemplateEngine templateEngine,
                        ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
    }

    public byte[] generateCvPdf(Long jobId, Long userId, String template) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<Map<String, Object>> projects = limitProjects(parseJsonListOfMaps(profile.getProjects()), 3, 4);
        List<String> skills = limitList(parseJsonList(profile.getSkills()), 15);
        List<Map<String, Object>> education = parseJsonListOfMaps(profile.getEducation());
        List<String> languages = parseJsonList(profile.getLanguages());
        List<String> certifications = parseJsonList(profile.getCertifications());

        Context context = buildBaseContext(
                user.getName(), job.getTitle(),
                job.getCompany() != null ? job.getCompany().getName() : "",
                safe(profile.getSummary()),
                skills, projects, education, languages, certifications);

        return generatePdf(context, template);
    }

    public byte[] generateTailoredCvPdf(Long jobId, Long userId,
                                         TailoredProfileRequest tailored,
                                         String template) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<Map<String, Object>> projectMaps = new ArrayList<>();
        if (tailored.projects() != null) {
            for (ProjectEntry p : tailored.projects()) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", safe(p.name()));
                map.put("description", safe(p.description()));
                map.put("technologies", p.technologies() != null ? p.technologies() : List.of());
                map.put("highlights", p.highlights() != null ? p.highlights() : List.of());
                projectMaps.add(map);
            }
        }

        List<Map<String, Object>> eduMaps = new ArrayList<>();
        if (tailored.education() != null) {
            for (EducationEntry e : tailored.education()) {
                Map<String, Object> map = new HashMap<>();
                map.put("degree", safe(e.degree()));
                map.put("school", safe(e.school()));
                map.put("year", safe(e.year()));
                eduMaps.add(map);
            }
        }

        Context context = buildBaseContext(
                user.getName(), job.getTitle(),
                job.getCompany() != null ? job.getCompany().getName() : "",
                safe(tailored.summary()),
                tailored.skills() != null ? tailored.skills() : List.of(),
                projectMaps,
                eduMaps,
                tailored.languages() != null ? tailored.languages() : List.of(),
                tailored.certifications() != null ? tailored.certifications() : List.of());

        return generatePdf(context, template);
    }

    private Context buildBaseContext(String name, String jobTitle, String companyName,
                                      String summary, List<String> skills,
                                      List<Map<String, Object>> projects,
                                      List<Map<String, Object>> education,
                                      List<String> languages, List<String> certifications) {
        Context context = new Context();
        context.setVariable("name", safe(name));
        context.setVariable("jobTitle", safe(jobTitle));
        context.setVariable("companyName", safe(companyName));
        context.setVariable("summary", safe(summary));
        context.setVariable("skills", skills != null ? skills : List.of());
        context.setVariable("projects", projects != null ? projects : List.of());
        context.setVariable("education", education != null ? education : List.of());
        context.setVariable("languages", languages != null ? languages : List.of());
        context.setVariable("certifications", certifications != null ? certifications : List.of());
        return context;
    }

    private byte[] generatePdf(Context context, String template) {
        boolean isSidebar = template != null && template.equals("sidebar");
        String templateName = isSidebar ? "cv-template2" : "cv-template";
        if (isSidebar) {
            context.setVariable("profileImage", loadPhotoDataUri());
        }
        String html = templateEngine.process(templateName, context);
        return renderPdf(html);
    }

    private String loadPhotoDataUri() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("templates/images/Davidtiny.png")) {
            if (is == null) {
                log.warn("Photo not found at templates/images/Davidtiny.png");
                return null;
            }
            byte[] bytes = is.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.warn("Failed to load photo: {}", e.getMessage());
            return null;
        }
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> parseJsonListOfMaps(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON list of maps: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private <T> List<T> limitList(List<T> list, int max) {
        if (list == null || list.isEmpty()) return list;
        return list.subList(0, Math.min(list.size(), max));
    }

    private List<Map<String, Object>> limitProjects(List<Map<String, Object>> projects,
                                                     int maxProjects, int maxHighlights) {
        if (projects == null || projects.isEmpty()) return projects;
        List<Map<String, Object>> limited = projects.subList(0, Math.min(projects.size(), maxProjects));
        for (Map<String, Object> project : limited) {
            @SuppressWarnings("unchecked")
            List<String> highlights = (List<String>) project.get("highlights");
            if (highlights != null && highlights.size() > maxHighlights) {
                project.put("highlights", highlights.subList(0, maxHighlights));
            }
        }
        return limited;
    }
}
