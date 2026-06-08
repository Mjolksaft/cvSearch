package com.cvsearch.job.dto;

import java.time.LocalDateTime;

public record JobAd(
        String id,
        String headline,
        String employerName,
        String workplace,
        String description,
        String municipality,
        String region,
        String employmentType,
        String occupation,
        String occupationGroup,
        String occupationField,
        boolean experienceRequired,
        LocalDateTime publicationDate,
        LocalDateTime applicationDeadline,
        String webpageUrl,
        String applicationEmail,
        String applicationUrl

) {}