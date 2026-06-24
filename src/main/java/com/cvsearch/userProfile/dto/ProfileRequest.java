package com.cvsearch.userProfile.dto;

public record ProfileRequest(
        String summary,
        String projects,
        String skills,
        String education,
        String languages,
        String certifications,
        String coursework,
        String phone,
        String github,
        String linkedin,
        String city,
        String country) {
}