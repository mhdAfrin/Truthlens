package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebEvidenceSearch {

    private final ObjectMapper json;
    private final String apiKey;

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public WebEvidenceSearch(
        ObjectMapper json,
        @Value("${TAVILY_API_KEY:}") String apiKey
    ) {
        this.json = json;
        this.apiKey = apiKey.trim();
    }

    public List<Map<String, Object>> search(String query)
            throws IOException, InterruptedException {

        if (apiKey.isBlank()) {
            throw new IOException("TAVILY_API_KEY is not configured.");
        }

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty.");
        }

        String body = json.writeValueAsString(Map.of(
            "query", query,
            "max_results", 3,
            "include_answer", false
        ));

        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.tavily.com/search"))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response =
            http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                "Web search failed: HTTP " + response.statusCode()
            );
        }

        var root = json.readTree(response.body());

        if (root == null || !root.path("results").isArray()) {
            throw new IOException("Web search returned an invalid response.");
        }

        List<Map<String, Object>> evidence = new ArrayList<>();
        var seen = new HashSet<String>();

        for (var result : root.path("results")) {
            String url = result.path("url").asText("");
            String excerpt = result.path("content").asText("").trim();

            URI uri;
            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException error) {
                continue;
            }

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || excerpt.isBlank()
                    || !seen.add(url)) {
                continue;
            }

            Map<String, Object> source = new LinkedHashMap<>();
            source.put("title", result.path("title").asText(uri.getHost()));
            source.put("url", url);
            source.put("snippet", excerpt);
            source.put("publisher", uri.getHost());
            source.put("sourceType", "Unknown");
            source.put("publicationDate", "Unavailable");
            source.put("relationship", "Context only");

            evidence.add(source);
        }

        return evidence;
    }
}