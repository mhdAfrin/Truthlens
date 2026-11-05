package lk.truthlens.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ClaimEvidencePipeline {

    private final ObjectMapper json;

    private final HttpClient http;

    private static final Pattern OPINION = Pattern.compile(
        "^(?:i think|i believe|in my opinion|i feel|i prefer)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PREDICTION = Pattern.compile(
        "\\b(?:will|going to|expected to|forecast to|predicted to|likely to|unlikely to)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REPORTED = Pattern.compile(
        "\\b(?:said|reported|announced|confirmed|stated|recorded|wrote)\\b",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    public ClaimEvidencePipeline(ObjectMapper json) {
        this(json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    }

    ClaimEvidencePipeline(ObjectMapper json, HttpClient http) {
        this.json = json;
        this.http = http;
    }

    // US-08: Extract claims without an artificial claim limit.
    public List<Map<String, Object>> extract(String content) {

        List<Map<String, Object>> claims = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return claims;
        }

        BreakIterator iterator =
                BreakIterator.getSentenceInstance(Locale.ENGLISH);

        iterator.setText(content);

        int start = iterator.first();

        for (int end = iterator.next();
             end != BreakIterator.DONE;
             start = end, end = iterator.next()) {

            String original = content.substring(start, end);
            String sentence = original.strip();

            if (sentence.isBlank()
                    || !sentence.matches("(?s).*\\p{L}.*")) {
                continue;
            }

            int leadingWhitespace = original.indexOf(sentence);
            int startOffset = start + leadingWhitespace;

            String claimType = classifyClaim(sentence);

            Map<String, Object> claim = new LinkedHashMap<>();

            claim.put("id", claims.size() + 1);
            claim.put("text", sentence);
            claim.put("originalPassage", sentence);
            claim.put("startOffset", startOffset);
            claim.put("endOffset", startOffset + sentence.length());
            claim.put("claimType", claimType);
            claim.put("finding", "Not assessed");

            if ("Opinion".equals(claimType)) {
                claim.put(
                        "explanation",
                        "This statement contains subjective language. " +
                                "It is not assigned a factual truth verdict."
                );
            } else if ("Prediction".equals(claimType)) {
                claim.put(
                        "explanation",
                        "This statement describes an uncertain or future " +
                                "event. Its truth cannot yet be established."
                );
            } else if ("Question or non-claim".equals(claimType)) {
                claim.put(
                        "explanation",
                        "This passage does not make a clear factual assertion."
                );
            } else {
                claim.put(
                        "explanation",
                        "A potentially checkable factual claim was extracted. " +
                                "Evidence discovery is pending."
                );
            }

            claim.put("support", List.of());
            claim.put("against", List.of());

            claim.put(
                    "context",
                    "Search results are leads, not proof. " +
                            "Confirm dates, context and independent corroboration."
            );

            claim.put("confidence", "Not assessed");
            claim.put("evidence", new ArrayList<>());

            claim.put(
                    "searchStatus",
                    "Potential factual claim".equals(claimType)
                            ? "PENDING"
                            : "NOT_APPLICABLE"
            );

            claims.add(claim);
        }

        if (claims.isEmpty()) {

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id", 1);
            item.put("text", content);
            item.put("originalPassage", content);
            item.put("startOffset", 0);
            item.put("endOffset", content.length());

            item.put(
                    "claimType",
                    "No clear factual claim detected"
            );

            item.put("finding", "Not assessed");

            item.put(
                    "explanation",
                    "No independently checkable statement was extracted."
            );

            item.put("support", List.of());
            item.put("against", List.of());

            item.put(
                    "context",
                    "Review the original text and submit a more specific statement."
            );

            item.put("confidence", "Not assessed");
            item.put("evidence", List.of());
            item.put("searchStatus", "NOT_APPLICABLE");

            claims.add(item);
        }

        return claims;
    }

    private String classifyClaim(String sentence) {

        if (sentence.matches("(?s).*\\?[\"”’)]*$")) {
            return "Question or non-claim";
        }

        if (OPINION.matcher(sentence).find()) {
            return "Opinion";
        }

        if (PREDICTION.matcher(sentence).find() && !REPORTED.matcher(sentence).find()) {
            return "Prediction";
        }

        return "Potential factual claim";
    }

    // US-09: Search public evidence and retrieve article extracts.
    public void search(List<Map<String, Object>> claims) {

        for (Map<String, Object> claim : claims) {

            if (!"PENDING".equals(claim.get("searchStatus"))) {
                continue;
            }

            String sentence = String.valueOf(claim.get("text"));

            try {

                String query = FoundingStatement.parse(sentence)
                    .map(FoundingStatement::subject)
                    .orElse(sentence);
                int length = Math.min(160, query.codePointCount(0, query.length()));
                query = query.substring(0, query.offsetByCodePoints(0, length));
                List<Map<String, Object>> evidence =
                        searchWikipedia(query);

                claim.put("evidence", evidence);

                claim.put(
                        "searchStatus",
                        evidence.isEmpty()
                                ? "NO_RESULTS"
                                : "RESULTS_FOUND"
                );


                claim.put(
                        "explanation",
                        evidence.isEmpty()
                                ? "The public-source search returned no results."
                                : "Related public evidence was retrieved. " +
                                "Automatic assessment is pending."
                );

            } catch (Exception ex) {

                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                claim.put("searchStatus", "SEARCH_FAILED");
                claim.put("finding", "Not assessed");

                claim.put(
                        "explanation",
                        "Evidence search failed or timed out. " +
                                "Partial claim extraction is preserved."
                );

                claim.put(
                        "searchError",
                        "Public evidence service unavailable or timed out"
                );
            }
        }
    }

    private List<Map<String, Object>> searchWikipedia(String query)
            throws Exception {

        String searchUrl =
                "https://en.wikipedia.org/w/api.php" +
                        "?action=query&list=search&srsearch=" +
                        URLEncoder.encode(
                                query,
                                StandardCharsets.UTF_8
                        ) +
                        "&srlimit=3&format=json";

        JsonNode searchResponse = requestJson(searchUrl);

        JsonNode results =
                searchResponse.path("query").path("search");

        if (!results.isArray()) {
            throw new IllegalStateException(
                    "Unexpected Wikipedia search response"
            );
        }

        List<Map<String, Object>> evidence = new ArrayList<>();
        Set<Long> pageIds = new HashSet<>();
        for (JsonNode result : results) {
            String title = result.path("title").asText("");
            long pageId = result.path("pageid").asLong();
            if (title.isBlank() || pageId <= 0 || !pageIds.add(pageId)) continue;

            String link = "https://en.wikipedia.org/?curid=" + pageId;
            String snippet = fetchArticleExtract(pageId);
            if (snippet.isBlank()) continue;

            Map<String, Object> source = new LinkedHashMap<>();

            source.put("title", title);
            source.put("url", link);
            source.put("snippet", snippet);

            source.put("sourceType", "Unknown");
            source.put("sourceTypeReason", "Encyclopedia summary. The underlying source's role has not been established for this claim.");
            source.put("independenceGroup", "Wikipedia");
            source.put("independenceNote", "All retrieved passages come from Wikipedia. Independent corroboration has not been established.");

            source.put(
                    "relationship",
                    "Context only"
            );

            source.put("publisher", "Wikipedia");
            source.put("publicationDate", "Unavailable");

            evidence.add(source);
        }

        return evidence;
    }

    private String fetchArticleExtract(long pageId) throws Exception {

        String extractUrl =
                "https://en.wikipedia.org/w/api.php" +
                        "?action=query&prop=extracts" +
                        "&exintro=1&explaintext=1" +
                        "&pageids=" + pageId + "&format=json";

        JsonNode response = requestJson(extractUrl);

        JsonNode pages =
                response.path("query").path("pages");

        if (!pages.isObject()) {
            throw new IllegalStateException("Unexpected Wikipedia article response");
        }

        for (JsonNode page : pages) {

            String extract = page.path("extract").asText("");

            if (!extract.isBlank()) {
                // Keep enough context to include founding details.
                return extract.length() > 3000
                        ? extract.substring(0, 3000)
                        : extract;
            }
        }

        return "";
    }

    private JsonNode requestJson(String url) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(url)
                )
                .header(
                        "User-Agent",
                        "TruthLensStudentPrototype/1.0 " +
                                "(educational evidence discovery)"
                )
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Public source returned HTTP " +
                            response.statusCode()
            );
        }

        return json.readTree(response.body());
    }
}
