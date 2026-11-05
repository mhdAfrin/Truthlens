package lk.truthlens.service;

import java.time.*;
import java.util.*;
import lk.truthlens.dto.NewsInput;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.exception.SubmissionNotFoundException;
import lk.truthlens.integration.article.ArticleDestinationPolicy;
import lk.truthlens.integration.news.PublisherFeedClient;
import lk.truthlens.repository.NewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {

    private final NewsRepository news;
    private final PublisherFeedClient publisher;
    private final ArticleDestinationPolicy destinations;
    private final AuditService audit;

    public NewsService(
        NewsRepository news,
        PublisherFeedClient publisher,
        ArticleDestinationPolicy destinations,
        AuditService audit
    ) {
        this.news = news;
        this.publisher = publisher;
        this.destinations = destinations;
        this.audit = audit;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> latest() {
        var feed = publisher.latest();
        var items = new ArrayList<>(news.entries(true));
        items.addAll((List<Map<String, Object>>) feed.get("items"));
        items.sort(
            Comparator.comparing(
                m -> Instant.parse(m.get("publishedAt").toString()),
                Comparator.reverseOrder()
            )
        );
        return Map.of(
            "items",
            items.stream().limit(18).toList(),
            "checkedAt",
            feed.get("checkedAt"),
            "feedStatus",
            feed.get("feedStatus")
        );
    }

    public Map<String, Object> manage() {
        return Map.of("items", news.entries(false));
    }

    void validate(NewsInput n) {
        if (
            n.title() == null ||
            n.title().isBlank() ||
            n.title().length() > 180 ||
            n.summary() == null ||
            n.summary().length() > 600 ||
            n.publisher() == null ||
            n.publisher().isBlank() ||
            n.publisher().length() > 100 ||
            n.publishedAt() == null
        ) throw new InvalidRequestException(
            "Add a title, publisher, valid date and a summary within the displayed limits."
        );
        destinations.parse(n.sourceUrl());
    }

    @Transactional
    public Map<String, Object> create(String actor, NewsInput input) {
        validate(input);
        UUID id = UUID.randomUUID();
        news.insert(id, input);
        audit.record(actor, "NEWS_CREATED", id.toString());
        return Map.of("id", id, "message", "News item saved.");
    }

    @Transactional
    public Map<String, Object> update(String actor, UUID id, NewsInput input) {
        validate(input);
        if (news.update(id, input) == 0) throw new SubmissionNotFoundException();
        audit.record(actor, "NEWS_UPDATED", id.toString());
        return Map.of("message", "News item updated.");
    }
}
