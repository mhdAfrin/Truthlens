package lk.truthlens.verification;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClaimVerdictService {

    private static final Pattern YEAR =
            Pattern.compile("\\b(?:18|19|20)\\d{2}\\b");

    private static final Pattern ESTABLISHED_CLAIM =
            Pattern.compile(
                    "(?i)^(.+?)\\s+(?:was|were|is)\\s+" +
                            "(?:established|founded|formed|created)\\s+" +
                            "(?:in\\s+)?((?:18|19|20)\\d{2})\\.?$"
            );

    private static final Pattern FOUNDING_EVIDENCE =
            Pattern.compile(
                    "(?i)\\b(?:established|founded|formed|created|" +
                            "establishment|foundation|inception)\\b"
            );

    public void assess(List<Map<String, Object>> claims) {

        for (Map<String, Object> claim : claims) {

            String type = text(claim, "claimType");
            String status = text(claim, "searchStatus");

            if (!"Potential factual claim".equals(type)) {
                claim.put("finding", "Not assessed");
                claim.put("confidence", "Not assessed");
                continue;
            }

            if ("SEARCH_FAILED".equals(status)) {
                claim.put("finding", "Not assessed");
                claim.put("confidence", "Not assessed");
                claim.put(
                        "explanation",
                        "Evidence retrieval failed. The claim could not be assessed."
                );
                continue;
            }

            if (!"RESULTS_FOUND".equals(status)) {
                insufficient(
                        claim,
                        "No usable public evidence was retrieved."
                );
                continue;
            }

            String statement = text(claim, "text");

            Matcher claimMatcher =
                    ESTABLISHED_CLAIM.matcher(statement.trim());

            if (!claimMatcher.matches()) {
                insufficient(
                        claim,
                        "The available rule-based verifier cannot reliably " +
                                "assess this type of factual claim."
                );
                continue;
            }

            String subject = normalize(claimMatcher.group(1));
            String claimedYear = claimMatcher.group(2);

            Object rawEvidence = claim.get("evidence");

            if (!(rawEvidence instanceof List<?> sources)) {
                insufficient(claim, "No usable evidence was available.");
                continue;
            }

            List<String> supporting = new ArrayList<>();
            List<String> contradicting = new ArrayList<>();

            Set<String> processedUrls = new HashSet<>();

            for (Object item : sources) {

                if (!(item instanceof Map<?, ?> source)) {
                    continue;
                }

                String url = sourceValue(source, "url");

                if (url.isBlank() || !processedUrls.add(url)) {
                    continue;
                }

                String title = sourceValue(source, "title");
                String snippet = sourceValue(source, "snippet");

                System.out.println("CHECK SUBJECT: " + subject);
                System.out.println("CHECK TITLE: " + title);
                System.out.println("SUBJECT MATCH: " + sameSubject(subject, title));
                if (!sameSubject(subject, title)) {
                    continue;
                }

                if (!FOUNDING_EVIDENCE.matcher(snippet).find()) {
                    continue;
                }

                Pattern foundingDate = Pattern.compile(
                        "(?i)\\b(?:was|were)\\s+" +
                                "(?:established|founded|formed|created)\\s+" +
                                "(?:on\\s+\\d{1,2}\\s+[A-Za-z]+\\s+|in\\s+)?" +
                                "((?:18|19|20)\\d{2})\\b"
                );
                System.out.println("VERDICT DEBUG - Article: " + title);
                System.out.println("VERDICT DEBUG - Founding date text: " +
                        snippet.substring(0, Math.min(snippet.length(), 1200)));

                Matcher dateMatcher = foundingDate.matcher(snippet);

                if (!dateMatcher.find()) {
                    System.out.println("DATE MATCH FAILED: " + title);
                    continue;
                }

                System.out.println(
                        "DATE MATCH FOUND: " + title +
                                " | Evidence year: " + dateMatcher.group(1) +
                                " | Claimed year: " + claimedYear
                );

                String evidenceYear = dateMatcher.group(1);

                String sourceDescription = title + " — " + url;

                if (claimedYear.equals(evidenceYear)) {
                    supporting.add(sourceDescription);
                } else {
                    contradicting.add(sourceDescription);
                }
            }

            claim.put("candidateSupportCount", supporting.size());
            claim.put("candidateContradictionCount", contradicting.size());
            claim.put("candidateSupportingSources", supporting);
            claim.put("candidateContradictingSources", contradicting);

            if (!supporting.isEmpty() && !contradicting.isEmpty()) {

                claim.put("finding", "Mixed");
                claim.put("confidence", "Low");
                claim.put(
                        "explanation",
                        "Public-source snippets contain conflicting founding " +
                                "years for the named organization. Manual review is recommended."
                );

            } else if (!supporting.isEmpty()) {

                claim.put("finding", "Supported");
                claim.put("confidence", "Low");
                claim.put(
                        "explanation",
                        "A relevant public-source snippet mentions the same " +
                                "founding year as the claim. This is an automated " +
                                "preliminary assessment, not independent verification."
                );

            } else if (!contradicting.isEmpty()) {

                claim.put("finding", "Contradicted");
                claim.put("confidence", "Low");
                claim.put(
                        "explanation",
                        "A relevant public-source snippet mentions a different " +
                                "founding year. This is an automated preliminary " +
                                "assessment and should be manually verified."
                );

            } else {

                insufficient(
                        claim,
                        "The retrieved snippets do not clearly establish " +
                                "the organization's founding year."
                );
            }
        }
    }

    private void insufficient(
            Map<String, Object> claim,
            String explanation
    ) {
        claim.put("finding", "Insufficient evidence");
        claim.put("confidence", "Not assessed");
        claim.put("explanation", explanation);
    }

    private boolean sameSubject(String subject, String title) {

        String cleanSubject = normalize(subject)
                .replaceFirst("^the\\s+", "");

        String cleanTitle = normalize(title)
                .replaceFirst("^the\\s+", "");

        return cleanSubject.equals(cleanTitle);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> extractYears(String text) {

        Set<String> years = new LinkedHashSet<>();

        Matcher matcher = YEAR.matcher(text);

        while (matcher.find()) {
            years.add(matcher.group());
        }

        return years;
    }

    private String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    private String sourceValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }
}