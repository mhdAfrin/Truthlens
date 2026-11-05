package lk.truthlens.verification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClaimVerdictServiceTest {
    private static final String CLAIM = "Example University was founded in 1990.";

    @Test
    void ignoresPredecessorYearAndQuotesTheMatchingEntity() {
        var result = assess(CLAIM, source("Example University",
            "Its predecessor college was founded in 1900. Example University was founded in 1990."));
        assertEquals("Supported", result.get("finding"));
        assertEquals(List.of(CLAIM), result.get("support"));
        assertEquals(List.of(), result.get("against"));
        var evidence = (Map<?, ?>) ((List<?>) result.get("evidence")).get(0);
        assertEquals("https://en.wikipedia.org/?curid=123", evidence.get("url"));
        assertEquals(CLAIM, evidence.get("quote"));
    }

    @Test
    void predecessorAloneDoesNotEstablishTheFoundingYear() {
        assertEquals("Insufficient evidence", assess(CLAIM, source("Example University",
            "Its predecessor college was founded in 1900.")).get("finding"));
    }

    @Test
    void sameNamedEventWithDifferentYearContradicts() {
        var result = assess(CLAIM, source("Example University", "Example University was established in 1991."));
        assertEquals("Contradicted", result.get("finding"));
        assertEquals(List.of("Example University was established in 1991."), result.get("against"));
    }

    @Test
    void retainsConflictingPassagesWithoutCountingThemAsIndependentSources() {
        var result = assess(CLAIM, source("Example University",
            "Example University was founded in 1990. Example University was established in 1991."));
        assertEquals("Mixed evidence", result.get("finding"));
        assertEquals("Low", result.get("confidence"));
        assertEquals(2, ((List<?>) result.get("evidence")).size());
    }

    @Test
    void canLinkDirectEvidenceForAnOrdinaryFactualClaim() {
        var result = assess("The Earth orbits the Sun.", source("Earth's orbit", "The Earth orbits the Sun."));
        assertEquals("Supported", result.get("finding"));
    }

    @Test
    void doesNotUseTheYearAloneToAssessAnExactDayOrADifferentEvent() {
        var source = source("Example University", "Example University was established on 7 April 1990.");
        assertEquals("Insufficient evidence",
            assess("Example University was established on 8 April 1990.", source).get("finding"));
        assertEquals("Insufficient evidence",
            assess("Example University was formed in 1990.", source).get("finding"));
    }

    private Map<String, Object> assess(String text, Map<String, Object> source) {
        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("text", text);
        claim.put("claimType", "Potential factual claim");
        claim.put("searchStatus", "RESULTS_FOUND");
        claim.put("evidence", List.of(source));
        new ClaimVerdictService().assess(List.of(claim));
        return claim;
    }

    private Map<String, Object> source(String title, String passage) {
        return Map.of("title", title, "url", "https://en.wikipedia.org/?curid=123",
            "snippet", passage, "publisher", "Wikipedia", "independenceGroup", "Wikipedia");
    }
}
