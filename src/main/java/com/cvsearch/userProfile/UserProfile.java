package com.cvsearch.userProfile;

import com.cvsearch.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summary;
    private String projects;
    private String skills;
    private String education;
    private String languages;
    private String certifications;
    @OneToOne
    @JoinColumn(name = "user_id") 
    @JsonIgnoreProperties({"profile"})
    private User user;

    public UserProfile() {

    }

    public UserProfile(String summary, String projects, String skills, String education, String languages, String certifications, User user) {
        this.summary = summary;
        this.projects = projects;
        this.skills = skills;
        this.education = education;
        this.languages = languages;
        this.certifications = certifications;
        this.user = user;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProjects() {
        return this.projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getSkills() {
        return this.skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getEducation() {
        return this.education;
    }

    public void setEducation(String education) {
        this.education = education;
    }
    public String getLanguages() {
        return this.languages;
    }

    public void setLanguages(String languages) {
         this.languages = languages;
    }

    public String getCertifications() {
        return this.certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
