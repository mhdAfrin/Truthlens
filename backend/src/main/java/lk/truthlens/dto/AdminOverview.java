package lk.truthlens.dto;

public record AdminOverview(
    long users,
    long activeUsers,
    long submissions,
    long processing,
    long awaiting,
    long publishedNews
) {}
