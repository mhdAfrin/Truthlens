package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.BreakIterator;
import java.util.*;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable intake worker. It prepares text metadata, not AI findings or evidence. */
@Service
public class VerificationOrchestrator {

    private final SubmissionRepository submissions;
    private final ObjectMapper json;

    public VerificationOrchestrator(SubmissionRepository submissions, ObjectMapper json) {
        this.submissions = submissions;
        this.json = json;
    }

    @Transactional
    public void preparePending() {
        for (var submission : submissions.findTop10ByStatusOrderByCreatedAtAsc(
            Submission.Status.PROCESSING
        )) {
            var metrics = TextStatistics.measure(submission.getContent());
            submission.setStatus(Submission.Status.AWAITING_VERIFICATION);
            submission.setReportJson(
                ReportSnapshotFactory.snapshot(submission, json, metrics, "TEXT_PREPARED")
            );
            submissions.save(submission);
        }
    }
}
