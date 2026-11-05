package lk.truthlens.integration.news;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lk.truthlens.integration.article.ArticleDestinationPolicy;
import lk.truthlens.integration.article.ArticleTransport;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

@Component
public class PublisherFeedClient {

    private final ArticleDestinationPolicy destinations;
    private final ArticleTransport transport;
    private List<Map<String, Object>> feed = List.of();
    private Instant checked = Instant.EPOCH;
    private String feedStatus = "Not refreshed yet";

    public PublisherFeedClient(ArticleDestinationPolicy destinations, ArticleTransport transport) {
        this.destinations = destinations;
        this.transport = transport;
    }

    public synchronized Map<String, Object> latest() {
        refreshFeed();
        return Map.of("items", feed, "checkedAt", checked, "feedStatus", feedStatus);
    }

    private synchronized void refreshFeed() {
        if (checked.isAfter(Instant.now().minusSeconds(600))) return;
        checked = Instant.now();
        try {
            var uri = destinations.parse("https://www.factcheck.org/feed/");
            var page = transport.get(destinations.resolve(uri), 5000);
            if (page.status() != 200) throw new IllegalStateException();
            var doc = Jsoup.parse(
                new String(page.body(), java.nio.charset.StandardCharsets.UTF_8),
                "",
                Parser.xmlParser()
            );
            List<Map<String, Object>> items = new ArrayList<>();
            for (var item : doc.select("item")) {
                String link = item.select("link").text(),
                    title = item.select("title").text();
                var parsed = java.net.URI.create(link);
                if (
                    !"https".equals(parsed.getScheme()) ||
                    !Set.of("www.factcheck.org", "factcheck.org").contains(parsed.getHost())
                ) continue;
                Instant date = ZonedDateTime.parse(
                    item.select("pubDate").text(),
                    DateTimeFormatter.RFC_1123_DATE_TIME
                ).toInstant();
                if (date.isAfter(Instant.now().plusSeconds(300))) continue;
                items.add(
                    Map.of(
                        "id",
                        link,
                        "title",
                        title,
                        "summary",
                        "Read the independent fact-check and its cited evidence.",
                        "sourceUrl",
                        link,
                        "publisher",
                        "FactCheck.org",
                        "publishedAt",
                        date.toString(),
                        "breaking",
                        false,
                        "origin",
                        "Independent publisher"
                    )
                );
                if (items.size() == 9) break;
            }
            feed = List.copyOf(items);
            feedStatus = "Updated";
        } catch (Exception e) {
            feedStatus = feed.isEmpty()
                ? "Publisher feed unavailable"
                : "Showing cached publisher items";
        }
    }
}
