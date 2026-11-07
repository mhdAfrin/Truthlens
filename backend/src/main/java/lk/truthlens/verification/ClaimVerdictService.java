package lk.truthlens.verification;

import java.math.BigDecimal;
import java.net.URI;
import java.text.BreakIterator;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClaimVerdictService {

    private static final Pattern FUEL_PRICE_LINE = Pattern.compile(
        "^(?<supplier>Ceypetco|Lanka IOC|Sinopec)\\s+"
            + "Petrol\\s+(?<grade>92|95)\\s+price\\s+"
            + "(?:is|was)\\s+LKR\\s+(?<price>[0-9]+(?:\\.[0-9]{1,2})?)\\s+"
            + "per\\s+litre\\s+effective\\s+"
            + "(?<date>[0-9]{4}-[0-9]{2}-[0-9]{2})\\.?$",
        Pattern.CASE_INSENSITIVE
    );

    @Value("${TRUTHLENS_FUEL_OFFICIAL_HOSTS:}")
    private String fuelOfficialHosts = "";

    private record Passage(String quote, boolean supports) {}

    private record DatedFuelPrice(
        String supplier,
        String grade,
        BigDecimal price,
        LocalDate effectiveDate
    ) {}

    public void assess(List<Map<String, Object>> claims) {
        for (var claim : claims) {
            assessClaim(claim);
        }
    }

    private void assessClaim(Map<String, Object> claim) {
        if (!"Potential factual claim".equals(claim.get("claimType"))) {
            return;
        }

        if ("SEARCH_FAILED".equals(claim.get("searchStatus"))) {
            finding(claim, "Not assessed",
                "Evidence search failed. This claim could not be assessed.");
            return;
        }

        if (!"RESULTS_FOUND".equals(claim.get("searchStatus"))
                || !(claim.get("evidence") instanceof List<?> sources)) {
            finding(claim, "Insufficient evidence",
                "No usable evidence was retrieved for this claim.");
            return;
        }

        String statement = value(claim, "text");
        List<Map<String, Object>> evidence = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> support = new ArrayList<>();
        List<String> against = new ArrayList<>();

        for (Object item : sources) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }

            String url = value(source, "url");
            if (!url.startsWith("https://") || !seen.add(url)) {
                continue;
            }

            List<Passage> matches = matchPassages(
                statement, value(source, "title"), value(source, "snippet")
            );

            if (matches.isEmpty()) {
                evidence.add(evidenceItem(source, "Context only", null));
                continue;
            }

            for (Passage passage : matches) {
                List<String> side = passage.supports() ? support : against;
                if (!side.contains(passage.quote())) {
                    side.add(passage.quote());
                }

                evidence.add(evidenceItem(
                    source,
                    passage.supports() ? "Supports claim" : "Contradicts claim",
                    passage.quote()
                ));
            }
        }

        claim.put("evidence", evidence);
        claim.put("support", support);
        claim.put("against", against);

        if (!support.isEmpty() && !against.isEmpty()) {
            finding(claim, "Mixed evidence",
                "Retrieved passages give conflicting findings for this claim.");
        } else if (!support.isEmpty()) {
            finding(claim, "Supported",
                "A retrieved passage states this claim or gives the same year "
                    + "for the named event.");
        } else if (!against.isEmpty()) {
            finding(claim, "Contradicted",
                "A retrieved passage gives a different year for the same "
                    + "named entity and event.");
        } else {
            finding(claim, "Insufficient evidence",
                "The retrieved passages do not directly support or contradict "
                    + "this claim under the current assessment rules.");
        }
    }

    public void assessOfficialFuelEvidence(List<Map<String, Object>> claims) {
        for (var claim : claims) {
            Object status = claim.get("searchStatus");
            if (!"RESULTS_FOUND".equals(status) && !"NO_RESULTS".equals(status)) {
                continue;
            }

            DatedFuelPrice expected = parseDatedFuelPrice(value(claim, "text"));

            if (expected == null) {
                finding(claim, "Not assessed",
                    "Automatic assessment requires a supported supplier, petrol "
                        + "grade, LKR price per litre, and an effective date "
                        + "in YYYY-MM-DD format.");
                continue;
            }

            Set<String> allowedHosts = officialHostsFor(expected.supplier());

            if (allowedHosts.isEmpty()) {
                finding(claim, "Not assessed",
                    "No official source hosts are configured for this supplier.");
                continue;
            }

            List<Map<String, Object>> eligible = new ArrayList<>();
            List<Map<String, Object>> excluded = new ArrayList<>();
            List<String> support = new ArrayList<>();
            List<String> against = new ArrayList<>();

            if (claim.get("evidence") instanceof List<?> candidates) {
                for (Object candidate : candidates) {
                    if (!(candidate instanceof Map<?, ?> source)) {
                        continue;
                    }

                    String url = value(source, "url");
                    String excerpt = value(source, "snippet").strip();

                    if (!allowedHosts.contains(evidenceHost(url))) {
                        excluded.add(excludedItem(source,
                            "Host is not an approved official source for this supplier."));
                        continue;
                    }

                    DatedFuelPrice actual = parseDatedFuelPrice(excerpt);

                    if (actual == null) {
                        excluded.add(excludedItem(source,
                            "The excerpt has no explicit fuel price and effective "
                                + "date in the supported format."));
                        continue;
                    }

                    if (!expected.supplier().equals(actual.supplier())
                            || !expected.grade().equals(actual.grade())) {
                        excluded.add(excludedItem(source,
                            "Fuel supplier or petrol grade does not match."));
                        continue;
                    }

                    if (!expected.effectiveDate().equals(actual.effectiveDate())) {
                        excluded.add(excludedItem(source,
                            "Effective date does not match the claim."));
                        continue;
                    }

                    boolean matches = expected.price().compareTo(actual.price()) == 0;
                    Map<String, Object> result = evidenceItem(
                        source,
                        matches ? "Supports claim" : "Contradicts claim",
                        excerpt
                    );
                    result.put("effectiveDate", actual.effectiveDate().toString());
                    eligible.add(result);
                    (matches ? support : against).add(excerpt + " — " + url);
                }
            }

            String verdict;
            if (!support.isEmpty() && !against.isEmpty()) {
                verdict = "Mixed evidence";
            } else if (!support.isEmpty()) {
                verdict = "Supported";
            } else if (!against.isEmpty()) {
                verdict = "Contradicted";
            } else {
                verdict = "Insufficient evidence";
            }

            claim.put("evidence", eligible);
            claim.put("excludedEvidence", excluded);
            claim.put("support", support);
            claim.put("against", against);
            claim.put("finding", verdict);
            claim.put("confidence", "Not assessed");
            claim.put("assessmentStatus", "COMPLETED");
            claim.put("explanation",
                eligible.size() + " eligible official excerpt(s); "
                    + excluded.size() + " excluded. "
                    + "The verdict compares prices for the same supplier, "
                    + "petrol grade, unit, and effective date.");
            claim.put("context",
                "Provisional verdict based on Tavily excerpts. "
                    + "Open the original notice to check its full context.");
        }
    }

    private DatedFuelPrice parseDatedFuelPrice(String text) {
        var match = FUEL_PRICE_LINE.matcher(
            text.strip().replaceAll("\\s+", " ")
        );

        if (!match.matches()) {
            return null;
        }

        try {
            return new DatedFuelPrice(
                match.group("supplier").toLowerCase(Locale.ROOT),
                match.group("grade"),
                new BigDecimal(match.group("price")),
                LocalDate.parse(match.group("date"))
            );
        } catch (DateTimeParseException error) {
            return null;
        }
    }

    private Set<String> officialHostsFor(String supplier) {
        Set<String> hosts = new HashSet<>();

        for (String entry : fuelOfficialHosts.split(";")) {
            String[] parts = entry.split("=", 2);

            if (parts.length == 2 && supplier.equalsIgnoreCase(parts[0].strip())) {
                for (String host : parts[1].split(",")) {
                    if (!host.isBlank()) {
                        hosts.add(host.strip().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        return hosts;
    }

    private String evidenceHost(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                return "";
            }
            return uri.getHost().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException error) {
            return "";
        }
    }

    private Map<String, Object> excludedItem(Map<?, ?> source, String reason) {
        Map<String, Object> result = evidenceItem(source, "Context only", null);
        result.put("exclusionReason", reason);
        return result;
    }

    private List<Passage> matchPassages(
            String statement, String title, String extract) {
        var foundingClaim = FoundingStatement.parse(statement)
            .filter(FoundingStatement::yearOnly);
        var sentences = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        sentences.setText(extract);
        List<Passage> matches = new ArrayList<>();

        for (int start = sentences.first(), end = sentences.next();
                end != BreakIterator.DONE;
                start = end, end = sentences.next()) {
            String sentence = extract.substring(start, end).strip();

            if (!sentence.endsWith(".") && !sentence.endsWith("!")) {
                continue;
            }
            if (normalized(statement).equals(normalized(sentence))) {
                matches.add(new Passage(sentence, true));
                continue;
            }
            if (foundingClaim.isEmpty()) {
                continue;
            }

            var passage = FoundingStatement.parse(sentence);
            if (passage.isEmpty()) {
                continue;
            }

            var claim = foundingClaim.get();
            var fact = passage.get();

            if (claim.event().equals(fact.event())
                    && FoundingStatement.name(claim.subject())
                        .equals(FoundingStatement.name(title))
                    && claim.matchesSubject(fact.subject(), title)) {
                matches.add(new Passage(sentence, claim.year().equals(fact.year())));
            }
        }

        return matches.stream().distinct().toList();
    }

    private Map<String, Object> evidenceItem(
            Map<?, ?> source, String relationship, String quote) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, val) -> result.put(String.valueOf(key), val));
        result.put("relationship", relationship);
        if (quote != null) {
            result.put("quote", quote);
        }
        return result;
    }

    private void finding(
            Map<String, Object> claim, String verdict, String explanation) {
        claim.put("finding", verdict);
        claim.put("explanation", explanation);
        claim.put("confidence", switch (verdict) {
            case "Supported", "Contradicted", "Mixed evidence" -> "Low";
            default -> "Not assessed";
        });
    }

    private String normalized(String text) {
        return text.strip().toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ").replaceFirst("[.!]$", "");
    }

    private String value(Map<?, ?> map, String key) {
        return map.get(key) instanceof String text ? text : "";
    }
}