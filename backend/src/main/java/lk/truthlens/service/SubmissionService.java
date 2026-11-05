package lk.truthlens.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import lk.truthlens.exception.SubmissionNotFoundException;
import lk.truthlens.integration.article.ArticleImporter;
import lk.truthlens.integration.article.FacebookPostExtractor;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import lk.truthlens.verification.ReportSnapshotFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final ObjectMapper json;
    private final SubmissionPolicy policy;

    public SubmissionService(
        SubmissionRepository submissions,
        UserRepository users,
        ObjectMapper json,
        SubmissionPolicy policy
    ) {
        this.submissions = submissions;
        this.users = users;
        this.json = json;
        this.policy = policy;
    }

    private UUID findOwnerId(String email) {
        return users.findByEmail(email).orElseThrow(SubmissionNotFoundException::new).getId();
    }

    @Transactional
    public Map<String, Object> submit(String email, UUID guestScope, String content) {
        return store(email, guestScope, content, null);
    }

    @Transactional
    public Map<String, Object> submitUrl(
        String email,
        UUID guestScope,
        ArticleImporter.Article article
    ) {
        return store(email, guestScope, article.content(), article);
    }

    private Map<String, Object> store(
        String email,
        UUID guestScope,
        String content,
        ArticleImporter.Article article
    ) {
        policy.validate(content);
        var submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setOwnerId(email == null ? null : findOwnerId(email));
        submission.setGuestScope(email == null ? Objects.requireNonNull(guestScope) : null);
        submission.setContent(content);
        submission.setCreatedAt(Instant.now());
        submission.setStatus(Submission.Status.PROCESSING);
        String normalizedText = content.replaceAll("(?U)\\s+", " ").strip();
        int characterCount = normalizedText.codePointCount(0, normalizedText.length());
        submission.setTitle(
            normalizedText.substring(
                0,
                normalizedText.offsetByCodePoints(0, Math.min(90, characterCount))
            )
        );
        if (article != null) {
            submission.setInputType("URL");
            submission.setOriginalUrl(article.originalUrl());
            submission.setResolvedUrl(article.resolvedUrl());
            String title = article.title().strip();
            if (!title.isBlank()) submission.setTitle(
                title.substring(
                    0,
                    title.offsetByCodePoints(
                        0,
                        Math.min(90, title.codePointCount(0, title.length()))
                    )
                )
            );
        }
        submission.setReportJson(ReportSnapshotFactory.initialSnapshot(submission, json));
        submissions.saveAndFlush(submission);
        var receipt = new LinkedHashMap<String, Object>();
        receipt.put("id", submission.getId());
        receipt.put("requestId", submission.getId());
        receipt.put("status", "PROCESSING");
        receipt.put(
            "message",
            article == null
                ? "Submission received. Processing has started."
                : "Article imported. Processing has started."
        );
        receipt.put("reportAvailable", false);
        receipt.put("inputType", submission.getInputType());
        if (article != null) {
            receipt.put("originalUrl", submission.getOriginalUrl());
            receipt.put("resolvedUrl", submission.getResolvedUrl());
            if (FacebookPostExtractor.facebook(submission.getResolvedUrl())) receipt.put(
                "extractionNote",
                FacebookPostExtractor.NOTE
            );
        }
        return receipt;
    }
}
