package lk.truthlens.integration.article;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import lk.truthlens.exception.ArticleImportException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class ArticleExtractor {

    public record Text(String title, String content) {}

    public Text extract(ArticleTransport.Page page, String base) {
        try {
            if (
                page.contentType().toLowerCase(Locale.ROOT).contains("xml") &&
                !page.contentType().toLowerCase(Locale.ROOT).contains("xhtml")
            ) throw unreadable();
            if (page.contentType().toLowerCase(Locale.ROOT).startsWith("text/plain")) {
                String content = new String(page.body(), StandardCharsets.UTF_8).strip();
                if (content.codePointCount(0, content.length()) < 80) throw unreadable();
                return new Text("", content);
            }
            String charset = null;
            var matcher = java.util.regex.Pattern.compile(
                "charset\\s*=\\s*[\"']?([a-zA-Z0-9._-]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
            ).matcher(page.contentType());
            if (matcher.find()) charset = matcher.group(1);
            var doc = Jsoup.parse(new ByteArrayInputStream(page.body()), charset, base);
            String host = java.net.URI.create(base).getHost().toLowerCase(Locale.ROOT);
            if (
                (host.equals("facebook.com") || host.endsWith(".facebook.com")) &&
                (!doc.select("input[name=email],input[name=pass],form[action*=login]").isEmpty() ||
                    doc.title().toLowerCase(Locale.ROOT).contains("log in"))
            ) throw unreadable();
            if (FacebookPostExtractor.facebook(base)) {
                var post = FacebookPostExtractor.extract(doc, base);
                if (post != null) return post;
                throw unreadable();
            }
            String title = doc.title();
            String lower = title.toLowerCase(Locale.ROOT);
            if (
                lower.startsWith("just a moment") ||
                lower.equals("access denied") ||
                lower.startsWith("attention required")
            ) throw unreadable();
            doc.select(
                "script,style,noscript,nav,header,footer,aside,form,button,iframe,svg,canvas,video,audio,[hidden],[aria-hidden=true]"
            ).remove();
            for (var element : new ArrayList<>(doc.select("[style]"))) {
                String style = element
                    .attr("style")
                    .replaceAll("\\s+", "")
                    .toLowerCase(Locale.ROOT);
                if (
                    style.contains("display:none") || style.contains("visibility:hidden")
                ) element.remove();
            }
            var candidates = doc.select("article,[itemprop=articleBody]");
            if (candidates.isEmpty()) candidates = doc.select("main,[role=main]");
            boolean fallback = candidates.isEmpty();
            if (fallback) candidates.add(doc.body());
            Element best = null;
            int score = -1;
            for (var element : candidates) {
                int text = element.select("p").text().length(),
                    links = element.select("a").text().length();
                if (text - links * 2 > score) {
                    score = text - links * 2;
                    best = element;
                }
            }
            if (
                best == null || (fallback && (best.select("p").size() < 2 || score < 120))
            ) throw unreadable();
            if (!best.select("h1").isEmpty()) title = best.selectFirst("h1").text();
            List<String> blocks = new ArrayList<>();
            Set<String> tags = Set.of("p", "h1", "h2", "h3", "blockquote", "li");
            for (var element : best.select("p,h1,h2,h3,blockquote,li")) {
                boolean nested = false;
                for (var parent : element.parents()) {
                    if (parent == best) break;
                    if (tags.contains(parent.tagName())) {
                        nested = true;
                        break;
                    }
                }
                if (!nested && !element.text().isBlank()) blocks.add(element.text());
            }
            String content = String.join("\n\n", blocks).strip();
            if (content.codePointCount(0, content.length()) < 80) throw unreadable();
            return new Text(title, content);
        } catch (ArticleImportException e) {
            throw e;
        } catch (Exception e) {
            throw unreadable();
        }
    }

    private static ArticleImportException unreadable() {
        return new ArticleImportException(
            422,
            "EXTRACTION_FAILED",
            "We couldn’t find readable article text on this page. It may require sign-in, use a paywall or load content with JavaScript. Paste the article text instead."
        );
    }
}
