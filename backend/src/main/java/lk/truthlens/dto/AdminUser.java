package lk.truthlens.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUser(
    UUID id,
    String name,
    String email,
    String role,
    boolean active,
    Instant createdAt
) {}
