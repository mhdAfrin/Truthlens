package lk.truthlens.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.exception.SubmissionNotFoundException;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final SubmissionRepository submissions;
    private final UserRepository users;

    public HistoryService(SubmissionRepository submissions, UserRepository users) {
        this.submissions = submissions;
        this.users = users;
    }

    private UUID findOwnerId(String email) {
        return users.findByEmail(email).orElseThrow(SubmissionNotFoundException::new).getId();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(
        String email,
        LocalDate from,
        LocalDate to,
        String status,
        String query,
        int page,
        int size
    ) {
        if (page < 0 || size < 1 || size > 50) throw new InvalidRequestException(
            "Choose a valid page and a page size from 1 to 50."
        );
        if (from != null && to != null && from.isAfter(to)) throw new InvalidRequestException(
            "The start date must be on or before the end date."
        );
        if (query != null && query.length() > 120) throw new InvalidRequestException(
            "Keep the search to 120 characters."
        );
        UUID ownerId = findOwnerId(email);
        Specification<Submission> filters = (root, cq, cb) ->
            cb.equal(root.get("ownerId"), ownerId);
        if (from != null) {
            Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            filters = filters.and((root, cq, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), start)
            );
        }
        if (to != null) {
            Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            filters = filters.and((root, cq, cb) -> cb.lessThan(root.get("createdAt"), end));
        }
        if (status != null && !status.isBlank()) {
            Submission.Status selected;
            try {
                selected = Submission.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Choose a valid processing status.");
            }
            filters = filters.and((root, cq, cb) -> cb.equal(root.get("status"), selected));
        }
        if (query != null && !query.isBlank()) {
            String term = query
                .strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            filters = filters.and((root, cq, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + term + "%", '\\')
            );
        }
        var result = submissions.findAll(
            filters,
            PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );
        var items = result
            .getContent()
            .stream()
            .map(submission ->
                Map.<String, Object>of(
                    "id",
                    submission.getId(),
                    "title",
                    submission.getTitle(),
                    "createdAt",
                    submission.getCreatedAt(),
                    "status",
                    submission.getStatus(),
                    "inputType",
                    submission.getInputType(),
                    "reportAvailable",
                    submission.getStatus() != Submission.Status.PROCESSING
                )
            )
            .toList();
        return Map.of(
            "items",
            items,
            "page",
            result.getNumber(),
            "totalPages",
            result.getTotalPages(),
            "totalElements",
            result.getTotalElements()
        );
    }
}
