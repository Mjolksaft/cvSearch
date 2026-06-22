package com.cvsearch.generation;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.cvsearch.generation.dto.TailoredProfileRequest;
import com.cvsearch.generation.dto.TailoredProfileRequest.EducationEntry;
import com.cvsearch.generation.dto.TailoredProfileRequest.ProjectEntry;

import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

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

        String email = user.getEmail() != null ? user.getEmail() : "";
        String jobLocation = job.getLocation() != null ? job.getLocation() : "";
        String phone = profile.getPhone() != null ? profile.getPhone() : "";
        String github = profile.getGithub() != null ? profile.getGithub() : "";
        String linkedin = profile.getLinkedin() != null ? profile.getLinkedin() : "";
        String city = profile.getCity() != null ? profile.getCity() : "";
        String country = profile.getCountry() != null ? profile.getCountry() : "";

        Context context = buildBaseContext(
                user.getName(), job.getTitle(),
                job.getCompany() != null ? job.getCompany().getName() : "",
                safe(profile.getSummary()),
                skills, projects, education, languages, certifications,
                email, jobLocation, phone, github, linkedin, city, country);

        return generatePdf(context, template);
    }

    public byte[] generateTailoredCvPdf(Long jobId, Long userId,
            TailoredProfileRequest tailored,
            String template) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for user id: " + userId));

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

        String email = user.getEmail() != null ? user.getEmail() : "";
        String jobLocation = job.getLocation() != null ? job.getLocation() : "";
        String phone = profile.getPhone() != null ? profile.getPhone() : "";
        String github = profile.getGithub() != null ? profile.getGithub() : "";
        String linkedin = profile.getLinkedin() != null ? profile.getLinkedin() : "";
        String city = profile.getCity() != null ? profile.getCity() : "";
        String country = profile.getCountry() != null ? profile.getCountry() : "";

        Context context = buildBaseContext(
                user.getName(), job.getTitle(),
                job.getCompany() != null ? job.getCompany().getName() : "",
                safe(tailored.summary()),
                tailored.skills() != null ? tailored.skills() : List.of(),
                projectMaps,
                eduMaps,
                tailored.languages() != null ? tailored.languages() : List.of(),
                tailored.certifications() != null ? tailored.certifications() : List.of(),
                email, jobLocation, phone, github, linkedin, city, country);

        return generatePdf(context, template);
    }

    private Context buildBaseContext(String name, String jobTitle, String companyName,
            String summary, List<String> skills,
            List<Map<String, Object>> projects,
            List<Map<String, Object>> education,
            List<String> languages, List<String> certifications,
            String email, String jobLocation,
            String phone, String github, String linkedin,
            String city, String country) {
        Context context = new Context();
        context.setVariable("name", safe(name));

        // Split name into first/last for styled display
        String n = safe(name);
        String firstName = n.contains(" ") ? n.substring(0, n.indexOf(" ")) : n;
        String lastName = n.contains(" ") ? n.substring(n.indexOf(" ") + 1) : "";
        context.setVariable("firstName", firstName);
        context.setVariable("lastName", lastName);

        context.setVariable("jobTitle", safe(jobTitle));
        context.setVariable("companyName", safe(companyName));
        context.setVariable("summary", safe(summary));
        context.setVariable("skills", skills != null ? skills : List.of());
        context.setVariable("projects", projects != null ? projects : List.of());
        context.setVariable("education", education != null ? education : List.of());
        context.setVariable("languages", languages != null ? languages : List.of());
        context.setVariable("certifications", certifications != null ? certifications : List.of());
        context.setVariable("email", email);
        context.setVariable("jobLocation", jobLocation);
        context.setVariable("phone", phone);
        context.setVariable("github", github);
        context.setVariable("linkedin", linkedin);
        context.setVariable("city", city);
        context.setVariable("country", country);
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

            // Read the original image
            BufferedImage original = ImageIO.read(is);
            if (original == null) {
                log.warn("Failed to decode photo");
                return null;
            }

            int size = Math.min(original.getWidth(), original.getHeight());

            // Create a square transparent canvas
            BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = circle.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Clip to a circle
                java.awt.geom.Ellipse2D.Double clip = new java.awt.geom.Ellipse2D.Double(0, 0, size, size);
                g2d.setClip(clip);

                // Draw the original image centered and cropped square
                int x = (original.getWidth() - size) / 2;
                int y = (original.getHeight() - size) / 2;
                g2d.drawImage(original, 0, 0, size, size, x, y, x + size, y + size, null);
            } finally {
                g2d.dispose();
            }

            // Encode the circular PNG to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(circle, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.warn("Failed to load photo: {}", e.getMessage());
            return null;
        }
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.toStream(baos);
            builder.run();
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
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
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
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse JSON list of maps: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private <T> List<T> limitList(List<T> list, int max) {
        if (list == null || list.isEmpty())
            return list;
        return list.subList(0, Math.min(list.size(), max));
    }

    private List<Map<String, Object>> limitProjects(List<Map<String, Object>> projects,
            int maxProjects, int maxHighlights) {
        if (projects == null || projects.isEmpty())
            return projects;
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
