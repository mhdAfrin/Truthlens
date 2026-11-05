
package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationOrchestratorFailureTest {

    @Test
    void failedEvidenceSearchPreservesPartialClaims() throws Exception {
        SubmissionRepository repository = mock(SubmissionRepository.class);
        ClaimEvidencePipeline pipeline = mock(ClaimEvidencePipeline.class);
        ClaimVerdictService verdictService = mock(ClaimVerdictService.class);
        ObjectMapper json = new ObjectMapper();

        Submission submission = mock(Submission.class);

        when(submission.getContent())
                .thenReturn("The World Health Organization was founded in 1948.");

        when(submission.getReportJson())
                .thenReturn("""
                    {"claims":[],"stage":"EXTRACTING_CLAIMS"}
                    """);

        when(repository.findTop10ByStatusOrderByCreatedAtAsc(
                Submission.Status.PROCESSING))
                .thenReturn(List.of(submission));

        when(repository.findTop10ByStatusOrderByCreatedAtAsc(
                Submission.Status.QUEUED))
                .thenReturn(List.of());

        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("id", 1);
        claim.put("text", "The World Health Organization was founded in 1948.");
        claim.put("searchStatus", "PENDING");
        claim.put("finding", "Not assessed");

        List<Map<String, Object>> claims = new ArrayList<>();
        claims.add(claim);

        when(pipeline.extract(any())).thenReturn(claims);

        doAnswer(invocation -> {
            claim.put("searchStatus", "SEARCH_FAILED");
            claim.put("searchError", "Evidence service unavailable");
            return null;
        }).when(pipeline).search(any());

        when(submission.getReportJson()).thenAnswer(invocation -> {
            return savedReport[0];
        });

        doAnswer(invocation -> {
            savedReport[0] = invocation.getArgument(0);
            return null;
        }).when(submission).setReportJson(any());

        VerificationOrchestrator orchestrator =
                new VerificationOrchestrator(
                        repository, json, pipeline, verdictService);

        orchestrator.preparePending();

        verify(submission).setStatus(Submission.Status.FAILED);

        var report = json.readTree(savedReport[0]);

        assertEquals("FAILED", report.path("stage").asText());
        assertTrue(report.path("error").asText().contains("Partial"));
        assertEquals(1, report.path("claims").size());
        assertEquals("SEARCH_FAILED",
                report.path("claims").get(0).path("searchStatus").asText());
    }

    private final String[] savedReport = {
            "{\"claims\":[],\"stage\":\"EXTRACTING_CLAIMS\"}"
    };
}
