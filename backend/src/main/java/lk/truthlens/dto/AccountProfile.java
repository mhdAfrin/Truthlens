package lk.truthlens.dto;

import java.util.UUID;

public record AccountProfile(UUID id, String name, String email, String role, boolean active) {}
