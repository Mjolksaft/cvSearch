package com.cvsearch.job.dto;

import java.util.List;

public record SearchResponse(
    Total total,
    List<JobAd> hits
) {
    public record Total(int value) {}

}