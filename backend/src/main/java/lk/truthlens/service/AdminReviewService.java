package lk.truthlens.service;

import java.time.*;
import java.util.*;
import lk.truthlens.dto.AccountAccessRequest;
import lk.truthlens.dto.AdminOverview;
import lk.truthlens.dto.AdminUser;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.exception.SubmissionNotFoundException;
import lk.truthlens.model.Submission;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.AdminQueryRepository;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReviewService {

    private final UserRepository users;
    private final SubmissionRepository records;
    private final AdminQueryRepository queries;
    private final AuditService auditService;
    private final SubmissionPolicy policy;

    public AdminReviewService(
        UserRepository userRepository,
        SubmissionRepository submissionRepository,
        AdminQueryRepository queries,
        AuditService auditService,
        SubmissionPolicy policy
    ) {
        this.policy = policy;
        users = userRepository;
        records = submissionRepository;
        this.queries = queries;
        this.auditService = auditService;
    }

    private AdminUser user(UserAccount account) {
        return new AdminUser(
            account.getId(),
            account.getName(),
            account.getEmail(),
            account.getRole(),
            account.isActive(),
            account.getCreatedAt()
        );
    }

    public AdminOverview overview() {
        return new AdminOverview(
            users.count(),
            queries.activeUsers(),
            records.count(),
            queries.processingSubmissions(),
            queries.awaitingSubmissions(),
            queries.publishedNews()
        );
    }

    public Object users(String q, int page) {
        if (page < 0 || q.length() > 120) throw new InvalidRequestException(
            "Choose a valid user search."
        );
        String term = q
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        org.springframework.data.jpa.domain.Specification<UserAccount> spec = (root, cq, cb) ->
            cb.or(
                cb.like(cb.lower(root.get("email")), "%" + term + "%", '\\'),
                cb.like(cb.lower(root.get("name")), "%" + term + "%", '\\')
            );
        var matching = users.findAll(
            spec,
            PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return Map.of(
            "items",
            matching.getContent().stream().map(this::user).toList(),
            "total",
            matching.getTotalElements()
        );
    }

    @Transactional
    public Object access(String actor, UUID id, AccountAccessRequest change) {
        var account = users.findById(id).orElseThrow(SubmissionNotFoundException::new);
        if (change.active() == null) throw new InvalidRequestException("Choose an account status.");
        if (account.getRole().equals("ADMIN")) throw new InvalidRequestException(
            "Administrator access must be managed through the secure deployment process."
        );
        account.setActive(change.active());
        users.save(account);
        auditService.record(
            actor,
            account.isActive() ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED",
            id.toString()
        );
        return user(account);
    }

    public Object submissions(int page, String status) {
        if (page < 0) throw new InvalidRequestException("Choose a valid page.");
        org.springframework.data.jpa.domain.Specification<Submission> spec = (root, cq, cb) ->
            cb.conjunction();
        if (!status.isBlank()) {
            Submission.Status selected;
            try {
                selected = Submission.Status.valueOf(status);
            } catch (Exception e) {
                throw new InvalidRequestException("Choose a valid status.");
            }
            spec = (root, cq, cb) -> cb.equal(root.get("status"), selected);
        }
        var result = records.findAll(
            spec,
            PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return Map.of(
            "items",
            result
                .getContent()
                .stream()
                .map(submission -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("id", submission.getId());
                    m.put("title", submission.getTitle());
                    m.put("createdAt", submission.getCreatedAt());
                    m.put("status", submission.getStatus());
                    m.put("inputType", submission.getInputType());
                    m.put(
                        "ownership",
                        submission.getOwnerId() == null ? "Guest session" : "Account"
                    );
                    m.put("ownerId", submission.getOwnerId());
                    return m;
                })
                .toList(),
            "total",
            result.getTotalElements()
        );
    }

    @Transactional
    public Object submission(String actor, UUID id) {
        var submission = records.findById(id).orElseThrow(SubmissionNotFoundException::new);
        auditService.record(actor, "PRIVATE_SUBMISSION_VIEWED", id.toString());
        var m = new LinkedHashMap<String, Object>();
        m.put("id", submission.getId());
        m.put("title", submission.getTitle());
        m.put("content", submission.getContent());
        m.put("originalUrl", submission.getOriginalUrl());
        m.put("status", submission.getStatus());
        m.put("ownerId", submission.getOwnerId());
        return m;
    }

    public Object audit(int page) {
        return auditService.history(page);
    }

    public Object system() throws Exception {
        return queries.system(policy.maxCodePoints(), users.count(), records.count());
    }
}
