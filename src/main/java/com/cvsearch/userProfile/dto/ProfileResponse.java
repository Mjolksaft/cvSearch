package com.cvsearch.userProfile.dto;

public record ProfileResponse(
        Long id,
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
        String country,
        Long userId) {
}
