package lk.truthlens.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_records")
public class Submission {

    public enum Status {
        AWAITING_VERIFICATION,
        PROCESSING,
        COMPLETED,
        FAILED,
    }

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "guest_scope")
    private UUID guestScope;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "report_json", nullable = false, columnDefinition = "TEXT")
    private String reportJson;

    @Column(name = "input_type", nullable = false, length = 8)
    private String inputType = "TEXT";

    @Column(name = "original_url", length = 2048)
    private String originalUrl;

    @Column(name = "resolved_url", length = 2048)
    private String resolvedUrl;

    public Submission() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getGuestScope() {
        return guestScope;
    }

    public void setGuestScope(UUID guestScope) {
        this.guestScope = guestScope;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getResolvedUrl() {
        return resolvedUrl;
    }

    public void setResolvedUrl(String resolvedUrl) {
        this.resolvedUrl = resolvedUrl;
    }
}
