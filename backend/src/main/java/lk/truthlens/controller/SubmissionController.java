package lk.truthlens.controller;

import jakarta.servlet.http.*;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import lk.truthlens.exception.AccountSessionRequiredException;
import lk.truthlens.exception.ArticleImportException;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.integration.article.ArticleImporter;
import lk.truthlens.service.HistoryService;
import lk.truthlens.service.ReportAccessService;
import lk.truthlens.service.SubmissionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
public class SubmissionController {

    private static final String GUEST_SCOPE = "truthlens.guestScope";
    private final SubmissionService service;
    private final ArticleImporter articles;
    private final HistoryService history;
    private final ReportAccessService reports;

    public SubmissionController(
        SubmissionService service,
        ArticleImporter articles,
        HistoryService history,
        ReportAccessService reports
    ) {
        this.service = service;
        this.articles = articles;
        this.history = history;
        this.reports = reports;
    }

    public record TextSubmissionRequest(com.fasterxml.jackson.databind.JsonNode content) {}

    @PostMapping
    public ResponseEntity<?> submit(
        Principal principal,
        HttpServletRequest request,
        @RequestBody TextSubmissionRequest body
    ) {
        if (
            principal == null && "required".equals(request.getHeader("X-TruthLens-Account"))
        ) throw new AccountSessionRequiredException();
        if (body.content() == null || body.content().isNull()) throw new InvalidRequestException(
            "Paste an article or copied text before starting verification."
        );
        if (!body.content().isTextual()) throw new InvalidRequestException(
            "Article text must be a text string."
        );
        UUID scope = null;
        if (principal == null) {
            var session = request.getSession(true);
            scope = (UUID) session.getAttribute(GUEST_SCOPE);
            if (scope == null) {
                scope = UUID.randomUUID();
                session.setAttribute(GUEST_SCOPE, scope);
            }
        }
        return ResponseEntity.accepted().body(
            service.submit(
                principal == null ? null : principal.getName(),
                scope,
                body.content().textValue()
            )
        );
    }

    public record UrlSubmission(com.fasterxml.jackson.databind.JsonNode url) {}

    @PostMapping("/url")
    public ResponseEntity<?> submitUrl(
        Principal principal,
        HttpServletRequest request,
        @RequestBody UrlSubmission body
    ) {
        if (
            principal == null && "required".equals(request.getHeader("X-TruthLens-Account"))
        ) throw new AccountSessionRequiredException();
        if (body.url() == null || !body.url().isTextual()) throw ArticleImportException.invalid();
        var article = articles.fetch(body.url().textValue());
        UUID scope = null;
        if (principal == null) {
            var session = request.getSession(true);
            scope = (UUID) session.getAttribute(GUEST_SCOPE);
            if (scope == null) {
                scope = UUID.randomUUID();
                session.setAttribute(GUEST_SCOPE, scope);
            }
        }
        return ResponseEntity.accepted().body(
            service.submitUrl(principal == null ? null : principal.getName(), scope, article)
        );
    }

    @GetMapping
    public Object history(
        Principal principal,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return history.history(principal.getName(), from, to, status, q, page, size);
    }

    @GetMapping("/{id}")
    public Object report(Principal principal, HttpServletRequest request, @PathVariable UUID id) {
        var session = request.getSession(false);
        UUID scope = session == null ? null : (UUID) session.getAttribute(GUEST_SCOPE);
        return reports.report(principal == null ? null : principal.getName(), scope, id);
    }
}
