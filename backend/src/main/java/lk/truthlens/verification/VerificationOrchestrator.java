package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

@Service
public class VerificationOrchestrator {
    private final SubmissionRepository submissions;
    private final ObjectMapper json;
    private final ClaimEvidencePipeline pipeline;
    private final ClaimVerdictService verdictService;

    public VerificationOrchestrator(
        SubmissionRepository submissions,
        ObjectMapper json,
        ClaimEvidencePipeline pipeline,
        ClaimVerdictService verdictService
    ) {
        this.submissions = submissions;
        this.json = json;
        this.pipeline = pipeline;
        this.verdictService = verdictService;
    }

    // Each save commits before the next stage starts. External requests run outside the database transaction.
    public synchronized void preparePending() {
        for (var submission : submissions.findTop10ByStatusOrderByCreatedAtAsc(Submission.Status.PROCESSING)) {
            process(submission);
        }
        for (var submission : submissions.findTop10ByStatusOrderByCreatedAtAsc(Submission.Status.QUEUED)) {
            submission.setStatus(Submission.Status.PROCESSING);
            submission.setReportJson(ReportSnapshotFactory.snapshot(
                submission, json, TextStatistics.measure(submission.getContent()), "EXTRACTING_CLAIMS"
            ));
            submissions.save(submission);
        }
    }

    private void process(Submission submission) {
        try {
            var claims = pipeline.extract(submission.getContent());
            submission.setReportJson(ReportSnapshotFactory.withClaims(submission, json, claims, "SEARCHING_EVIDENCE"));
            submissions.saveAndFlush(submission);

            pipeline.search(claims);
            submission.setReportJson(ReportSnapshotFactory.withClaims(submission, json, claims, "ASSESSING_CLAIMS"));
            submissions.saveAndFlush(submission);

            verdictService.assess(claims);
            boolean searchFailed = claims.stream().anyMatch(claim -> "SEARCH_FAILED".equals(claim.get("searchStatus")));
            submission.setStatus(searchFailed ? Submission.Status.FAILED : Submission.Status.COMPLETED);
            submission.setReportJson(ReportSnapshotFactory.withClaims(
                submission, json, claims, searchFailed ? "FAILED" : "REPORT_READY"
            ));
            if (searchFailed) {
                submission.setReportJson(ReportSnapshotFactory.failure(
                    submission, json, "Evidence search failed for one or more claims. Partial findings are available."
                ));
            }
        } catch (RuntimeException error) {
            submission.setStatus(Submission.Status.FAILED);
            submission.setReportJson(ReportSnapshotFactory.failure(
                submission, json, "Verification could not finish. Partial findings remain available."
            ));
        }
        submissions.save(submission);
    }
}
