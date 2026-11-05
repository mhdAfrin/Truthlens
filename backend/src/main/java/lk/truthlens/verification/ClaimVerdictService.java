package lk.truthlens.verification;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ClaimVerdictService {
    private record Passage(String quote, boolean supports) {}

    public void assess(List<Map<String, Object>> claims) {
        for (var claim : claims) assessClaim(claim);
    }

    private void assessClaim(Map<String, Object> claim) {
        if (!"Potential factual claim".equals(claim.get("claimType"))) return;
        if ("SEARCH_FAILED".equals(claim.get("searchStatus"))) {
            finding(claim, "Not assessed", "Evidence search failed. This claim could not be assessed.");
            return;
        }
        if (!"RESULTS_FOUND".equals(claim.get("searchStatus"))
            || !(claim.get("evidence") instanceof List<?> sources)) {
            finding(claim, "Insufficient evidence", "No usable evidence was retrieved for this claim.");
            return;
        }

        String statement = value(claim, "text");
        List<Map<String, Object>> evidence = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> support = new ArrayList<>();
        List<String> against = new ArrayList<>();
        for (Object item : sources) {
            if (!(item instanceof Map<?, ?> source)) continue;
            String url = value(source, "url");
            if (!url.startsWith("https://") || !seen.add(url)) continue;
            List<Passage> matches = matchPassages(statement, value(source, "title"), value(source, "snippet"));
            if (matches.isEmpty()) {
                evidence.add(evidenceItem(source, "Context only", null));
                continue;
            }
            for (Passage passage : matches) {
                List<String> side = passage.supports() ? support : against;
                if (!side.contains(passage.quote())) side.add(passage.quote());
                evidence.add(evidenceItem(source, passage.supports() ? "Supports claim" : "Contradicts claim", passage.quote()));
            }
        }

        claim.put("evidence", evidence);
        claim.put("support", support);
        claim.put("against", against);
        if (!support.isEmpty() && !against.isEmpty()) {
            finding(claim, "Mixed evidence", "Retrieved passages give conflicting findings for this claim.");
        } else if (!support.isEmpty()) {
            finding(claim, "Supported", "A retrieved passage states this claim or gives the same year for the named event.");
        } else if (!against.isEmpty()) {
            finding(claim, "Contradicted", "A retrieved passage gives a different year for the same named entity and event.");
        } else {
            finding(claim, "Insufficient evidence", "The retrieved passages do not directly support or contradict this claim under the current assessment rules.");
        }
    }

    private List<Passage> matchPassages(String statement, String title, String extract) {
        var foundingClaim = FoundingStatement.parse(statement).filter(FoundingStatement::yearOnly);
        var sentences = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        sentences.setText(extract);
        List<Passage> matches = new ArrayList<>();
        for (int start = sentences.first(), end = sentences.next(); end != BreakIterator.DONE;
             start = end, end = sentences.next()) {
            String sentence = extract.substring(start, end).strip();
            if (!sentence.endsWith(".") && !sentence.endsWith("!")) continue;
            if (normalized(statement).equals(normalized(sentence))) {
                matches.add(new Passage(sentence, true));
                continue;
            }
            if (foundingClaim.isEmpty()) continue;
            var passage = FoundingStatement.parse(sentence);
            if (passage.isEmpty()) continue;
            var claim = foundingClaim.get();
            var fact = passage.get();
            if (claim.event().equals(fact.event())
                && FoundingStatement.name(claim.subject()).equals(FoundingStatement.name(title))
                && claim.matchesSubject(fact.subject(), title)) {
                matches.add(new Passage(sentence, claim.year().equals(fact.year())));
            }
        }
        return matches.stream().distinct().toList();
    }

    private Map<String, Object> evidenceItem(Map<?, ?> source, String relationship, String quote) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, val) -> result.put(String.valueOf(key), val));
        result.put("relationship", relationship);
        if (quote != null) result.put("quote", quote);
        return result;
    }

    private void finding(Map<String, Object> claim, String finding, String explanation) {
        claim.put("finding", finding);
        claim.put("explanation", explanation);
        claim.put("confidence", switch (finding) {
            case "Supported", "Contradicted", "Mixed evidence" -> "Low";
            default -> "Not assessed";
        });
    }

    private String normalized(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").replaceFirst("[.!]$", "");
    }

    private String value(Map<?, ?> map, String key) {
        return map.get(key) instanceof String text ? text : "";
    }
}
