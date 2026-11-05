package lk.truthlens.dto;

import java.time.Instant;

public record NewsInput(
    String title,
    String summary,
    String sourceUrl,
    String publisher,
    Instant publishedAt,
    Boolean visible,
    Boolean breaking
) {}
