package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimEvidencePipelineTest {
    @Test
    void retrievesRelevantPageWithDifferentTitleAndDeduplicatesPageId() throws Exception {
        var http = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
            {"query":{"search":[{"title":"Earth's orbit","pageid":123},{"title":"Earth's orbit","pageid":123}]}}
            """, """
            {"query":{"pages":{"123":{"extract":"The Earth orbits the Sun."}}}}
            """);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var pipeline = new ClaimEvidencePipeline(new ObjectMapper(), http);
        var claims = pipeline.extract("The Earth orbits the Sun.");
        pipeline.search(claims);
        new ClaimVerdictService().assess(claims);

        assertEquals("Supported", claims.get(0).get("finding"));
        var evidence = (List<?>) claims.get(0).get("evidence");
        assertEquals(1, evidence.size());
        var item = (Map<?, ?>) evidence.get(0);
        assertEquals("Unknown", item.get("sourceType"));
        assertEquals("Wikipedia", item.get("independenceGroup"));
        assertEquals("The Earth orbits the Sun.", item.get("quote"));
        verify(http, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void preservesOriginalPassagesAndDistinguishesReportedFactsFromPredictions() {
        var pipeline = new ClaimEvidencePipeline(new ObjectMapper(), mock(HttpClient.class));
        String text = "  The council announced that it will open 12 clinics in 2027. It will rain tomorrow. I think it is beautiful. Is that true?";
        var claims = pipeline.extract(text);
        assertEquals(List.of("Potential factual claim", "Prediction", "Opinion", "Question or non-claim"),
            claims.stream().map(claim -> claim.get("claimType")).toList());
        for (var claim : claims) {
            assertEquals(claim.get("originalPassage"),
                text.substring((int) claim.get("startOffset"), (int) claim.get("endOffset")));
        }
    }
}
