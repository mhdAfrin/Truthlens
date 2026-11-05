package lk.truthlens.integration.article;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.util.*;
import org.jsoup.nodes.Document;

/** Reads public server-delivered JSON as data; never executes scripts or fetches attachments. */
public final class FacebookPostExtractor {

    public static final String NOTE =
        "Only the publicly available Facebook post text was imported. Text or figures inside attached images and videos have not been extracted; paste those details separately.";

    public static boolean facebook(String url) {
        try {
            String h = URI.create(url).getHost().toLowerCase(Locale.ROOT);
            return h.equals("facebook.com") || h.endsWith(".facebook.com");
        } catch (Exception e) {
            return false;
        }
    }

    static ArticleExtractor.Text extract(Document doc, String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();
        if (
            !(
                path.equals("/story.php") ||
                path.equals("/permalink.php") ||
                path.contains("/posts/") ||
                path.contains("/share/p/")
            )
        ) return null;
        String expected = "";
        if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals("story_fbid")) expected = URLDecoder.decode(
                kv[1],
                java.nio.charset.StandardCharsets.UTF_8
            );
        }
        String routeId = "";
        var match = java.util.regex.Pattern.compile("/posts/([^/]+)").matcher(path);
        if (match.find()) routeId = match.group(1);
        Map<String, String> candidates = new LinkedHashMap<>();
        ObjectMapper json = new ObjectMapper();
        for (var script : doc.select("script[type=application/json][data-sjs]")) {
            try {
                JsonNode data = json.readTree(script.data());
                for (JsonNode root : data.findValues("node_v2")) {
                    if (!root.path("__typename").asText().equals("Story")) continue;
                    String id = root.path("post_id").asText();
                    if (id.isBlank() || (!expected.isEmpty() && !expected.equals(id))) continue;
                    JsonNode story = root.path("comet_sections").path("content").path("story");
                    if (!id.equals(story.path("post_id").asText())) continue;
                    String permalink =
                        root.path("permalink_url").asText() +
                        story.path("wwwURL").asText() +
                        story
                            .path("comet_sections")
                            .path("message")
                            .path("story")
                            .path("url")
                            .asText();
                    if (!routeId.isEmpty() && !permalink.contains("/posts/" + routeId)) continue;
                    JsonNode message = story.path("message");
                    if (
                        !message.path("text").isTextual() ||
                        message.path("is_truncated").asBoolean(false)
                    ) continue;
                    String text = message.path("text").asText().strip();
                    if (text.codePointCount(0, text.length()) >= 80) candidates.put(id, text);
                }
            } catch (Exception ignored) {
                /* Unrelated or malformed hydration fragments are not article text. */
            }
        }
        if (candidates.size() != 1) return null;
        String title = doc.title().strip();
        if (title.isBlank()) title = "Facebook public post";
        return new ArticleExtractor.Text(title, candidates.values().iterator().next());
    }

    private FacebookPostExtractor() {}
}
