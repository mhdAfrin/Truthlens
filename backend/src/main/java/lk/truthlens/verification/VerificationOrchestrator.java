package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void preparePending() {

        // Process submissions that are ready for verification.
        for (Submission submission :
                submissions.findTop10ByStatusOrderByCreatedAtAsc(
                        Submission.Status.PROCESSING)) {

            try {

                // Stage 1: Extract factual claims.
                List<Map<String, Object>> claims =
                        pipeline.extract(submission.getContent());

                // Save extracted claims before evidence retrieval.
                submission.setReportJson(
                        ReportSnapshotFactory.withClaims(
                                submission,
                                json,
                                claims,
                                "SEARCHING_EVIDENCE"
                        )
                );

                submissions.saveAndFlush(submission);

                // Stage 2: Retrieve public evidence.
                pipeline.search(claims);

                // Stage 3: Automatically assess each claim.
                verdictService.assess(claims);

                // Check whether any evidence search failed.
                boolean searchFailed = claims.stream()
                        .anyMatch(claim ->
                                "SEARCH_FAILED".equals(
                                        claim.get("searchStatus")
                                )
                        );

                if (searchFailed) {

                    // Keep partial results when verification fails.
                    submission.setStatus(Submission.Status.FAILED);

                    submission.setReportJson(
                            ReportSnapshotFactory.withClaims(
                                    submission,
                                    json,
                                    claims,
                                    "FAILED"
                            )
                    );

                    submission.setReportJson(
                            ReportSnapshotFactory.failure(
                                    submission,
                                    json,
                                    "Evidence search failed for one or more claims. "
                                            + "Partial findings are available."
                            )
                    );

                } else {

                    // All evidence searches finished successfully.
                    submission.setStatus(Submission.Status.COMPLETED);

                    submission.setReportJson(
                            ReportSnapshotFactory.withClaims(
                                    submission,
                                    json,
                                    claims,
                                    "REPORT_READY"
                            )
                    );
                }

            } catch (Exception ex) {

                // Preserve the latest available partial report.
                submission.setStatus(Submission.Status.FAILED);

                submission.setReportJson(
                        ReportSnapshotFactory.failure(
                                submission,
                                json,
                                "Verification could not finish. "
                                        + "Partial findings remain available."
                        )
                );
            }

            submissions.save(submission);
        }

        // Move queued submissions into processing.
        for (Submission submission :
                submissions.findTop10ByStatusOrderByCreatedAtAsc(
                        Submission.Status.QUEUED)) {

            submission.setStatus(Submission.Status.PROCESSING);

            submission.setReportJson(
                    ReportSnapshotFactory.snapshot(
                            submission,
                            json,
                            TextStatistics.measure(
                                    submission.getContent()
                            ),
                            "EXTRACTING_CLAIMS"
                    )
            );

            submissions.save(submission);
        }
    }
}