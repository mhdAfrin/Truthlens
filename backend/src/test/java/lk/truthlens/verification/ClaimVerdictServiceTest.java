package lk.truthlens.verification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimVerdictServiceTest {

    @Test
    void conflictingFoundingYearsProduceMixedVerdict() {

        ClaimVerdictService service = new ClaimVerdictService();

        Map<String, Object> claim = new LinkedHashMap<>();

        claim.put("text",
                "The World Health Organization was established in 1948.");
        claim.put("claimType", "Potential factual claim");
        claim.put("searchStatus", "RESULTS_FOUND");

        List<Map<String, Object>> evidence = new ArrayList<>();

        evidence.add(source(
                "World Health Organization",
                "https://example.org/test-source-one",
                "The organization was established in 1948."
        ));

        evidence.add(source(
                "World Health Organization",
                "https://example.org/test-source-two",
                "The organization was established in 1950."
        ));

        claim.put("evidence", evidence);

        service.assess(List.of(claim));

        assertEquals("Mixed", claim.get("finding"));
        assertEquals("Low", claim.get("confidence"));
    }

    private Map<String, Object> source(
            String title,
            String url,
            String snippet
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("title", title);
        result.put("url", url);
        result.put("snippet", snippet);
        result.put("publisher", "Test source");

        return result;
    }
}