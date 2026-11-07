package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VerificationOrchestrator {

    private final SubmissionRepository submissions;
    private final ObjectMapper json;
    private final ClaimEvidencePipeline pipeline;
    private final ClaimVerdictService verdictService;
    private final WebEvidenceSearch webSearch;

    @Autowired
    public VerificationOrchestrator(
            SubmissionRepository submissions,
            ObjectMapper json,
            ClaimEvidencePipeline pipeline,
            ClaimVerdictService verdictService,
            WebEvidenceSearch webSearch) {
        this.submissions = submissions;
        this.json = json;
        this.pipeline = pipeline;
        this.verdictService = verdictService;
        this.webSearch = webSearch;
    }

    // Keeps the existing rules-based workflow tests compatible.
    VerificationOrchestrator(
            SubmissionRepository submissions,
            ObjectMapper json,
            ClaimEvidencePipeline pipeline,
            ClaimVerdictService verdictService) {
        this(submissions, json, pipeline, verdictService, null);
    }

    public synchronized void preparePending() {
        for (var submission : submissions.findTop10ByStatusOrderByCreatedAtAsc(
                Submission.Status.PROCESSING)) {
            process(submission);
        }

        for (var submission : submissions.findTop10ByStatusOrderByCreatedAtAsc(
                Submission.Status.QUEUED)) {
            submission.setStatus(Submission.Status.PROCESSING);
            submission.setReportJson(ReportSnapshotFactory.snapshot(
                submission,
                json,
                TextStatistics.measure(submission.getContent()),
                "EXTRACTING_CLAIMS"
            ));
            submissions.save(submission);
        }
    }

    private void process(Submission submission) {
        String currentStep = "Preparing the claim";

        try {
            var claims = prepareClaims(submission.getContent());

            if (!claims.isEmpty()) {
                currentStep = "Web evidence search";
                saveProgress(submission, claims, "SEARCHING_EVIDENCE");

                if (webSearch == null) {
                    pipeline.search(claims);
                } else {
                    searchWebEvidence(claims);
                }

                boolean hasAssessableClaims = claims.stream().anyMatch(claim ->
                    !"SEARCH_FAILED".equals(claim.get("searchStatus"))
                );

                if (hasAssessableClaims) {
                    currentStep = "Evidence assessment";
                    saveProgress(submission, claims, "ASSESSING_CLAIMS");

                    if (webSearch == null) {
                        verdictService.assess(claims);
                    } else {
                        verdictService.assessOfficialFuelEvidence(claims);
                    }
                }
            }

            currentStep = "Report creation";

            var failedClaim = claims.stream()
                .filter(claim -> "SEARCH_FAILED".equals(claim.get("searchStatus")))
                .findFirst();

            boolean failed = failedClaim.isPresent();
            submission.setStatus(failed
                ? Submission.Status.FAILED
                : Submission.Status.COMPLETED);

            submission.setReportJson(ReportSnapshotFactory.withClaims(
                submission,
                json,
                claims,
                failed ? "FAILED" : "REPORT_READY"
            ));

            if (failed) {
                Object detail = failedClaim.get().get("searchError");
                markFailed(submission, detail == null
                    ? "Web evidence search failed."
                    : "Web evidence search failed: " + detail);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            markFailed(submission, currentStep + " was interrupted.");
        } catch (RuntimeException error) {
            markFailed(submission, currentStep + " failed.");
        }

        submissions.save(submission);
    }

    private List<Map<String, Object>> prepareClaims(String input) {
        if (webSearch == null) {
            return pipeline.extract(input);
        }

        if (input == null || input.isBlank()) {
            return List.of();
        }

        String passage = input.strip();
        int start = input.indexOf(passage);

        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("id", 1);
        claim.put("text", passage);
        claim.put("originalPassage", passage);
        claim.put("startOffset", start);
        claim.put("endOffset", start + passage.length());
        claim.put("claimType", "User-submitted claim");
        claim.put("searchStatus", "PENDING");
        claim.put("finding", "Not assessed");
        claim.put("confidence", "Not assessed");
        claim.put("explanation", "Web evidence search is pending.");
        claim.put("context", "Automatic assessment supports dated fuel-price claims.");
        claim.put("evidence", List.of());
        claim.put("excludedEvidence", List.of());
        claim.put("support", List.of());
        claim.put("against", List.of());

        return List.of(claim);
    }

    private void searchWebEvidence(List<Map<String, Object>> claims)
            throws InterruptedException {

        for (var claim : claims) {
            if (!"PENDING".equals(claim.get("searchStatus"))) {
                continue;
            }

            String passage = (String) claim.get("text");
            List<String> queries = List.of(
                passage,
                passage + " Sri Lanka official fuel price notice effective date"
            );
            claim.put("searchQueries", queries);

            Map<String, Map<String, Object>> uniqueSources =
                new LinkedHashMap<>();

            try {
                for (String query : queries) {
                    for (var source : webSearch.search(query)) {
                        Object url = source.get("url");

                        if (url instanceof String link && !link.isBlank()) {
                            uniqueSources.putIfAbsent(link, source);
                        }
                    }
                }

                claim.put("evidence", new ArrayList<>(uniqueSources.values()));
                claim.put(
                    "searchStatus",
                    uniqueSources.isEmpty() ? "NO_RESULTS" : "RESULTS_FOUND"
                );
                claim.put("explanation", uniqueSources.isEmpty()
                    ? "Tavily returned no usable excerpts."
                    : "Tavily returned candidate sources for assessment.");

            } catch (IOException | IllegalArgumentException error) {
                // Keep any results already retrieved, without issuing a verdict.
                claim.put("evidence", new ArrayList<>(uniqueSources.values()));
                claim.put("searchStatus", "SEARCH_FAILED");
                claim.put("finding", "Not assessed");

                String message = error.getMessage();
                claim.put("searchError",
                    message == null || message.isBlank()
                        ? error.getClass().getSimpleName()
                        : message);

                claim.put("explanation",
                    "Web evidence search could not finish. "
                        + "Any partial results were not assessed.");
            }
        }
    }

    private void saveProgress(
            Submission submission,
            List<Map<String, Object>> claims,
            String stage) {
        submission.setReportJson(ReportSnapshotFactory.withClaims(
            submission, json, claims, stage
        ));
        submissions.saveAndFlush(submission);
    }

    private void markFailed(Submission submission, String message) {
        submission.setStatus(Submission.Status.FAILED);
        submission.setReportJson(
            ReportSnapshotFactory.failure(submission, json, message)
        );
    }
}