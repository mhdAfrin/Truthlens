package lk.truthlens.integration.article;

import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import lk.truthlens.exception.ArticleImportException;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.service.SubmissionPolicy;
import org.springframework.stereotype.Service;

@Service
public class ArticleImporter {

    public record Article(String originalUrl, String resolvedUrl, String title, String content) {}

    private final ArticleDestinationPolicy destinations;
    private final ArticleTransport transport;
    private final ArticleExtractor extractor;
    private final SubmissionPolicy policy;
    private final Semaphore slots = new Semaphore(4);

    public ArticleImporter(
        ArticleDestinationPolicy destinations,
        ArticleTransport transport,
        ArticleExtractor extractor,
        SubmissionPolicy policy
    ) {
        this.destinations = destinations;
        this.transport = transport;
        this.extractor = extractor;
        this.policy = policy;
    }

    public Article fetch(String original) {
        String entered = original == null ? null : original.strip();
        URI current = destinations.parse(entered);
        if (!slots.tryAcquire()) throw new ArticleImportException(
            429,
            "IMPORT_BUSY",
            "Article importing is busy. Try again shortly or paste the text instead."
        );
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(12);
        Set<URI> visited = new HashSet<>();
        try {
            for (int hop = 0; hop <= 3; hop++) {
                if (!visited.add(current)) throw new ArticleImportException(
                    422,
                    "REDIRECT_FAILED",
                    "This article redirects in a loop. Paste the article text instead."
                );
                if (System.nanoTime() >= deadline) throw timeout();
                var target = destinations.resolve(current);
                long remaining = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                    deadline - System.nanoTime()
                );
                if (remaining <= 0) throw timeout();
                var page = transport.get(target, remaining);
                if (Set.of(301, 302, 303, 307, 308).contains(page.status())) {
                    if (hop == 3 || page.location().isBlank()) throw new ArticleImportException(
                        422,
                        "REDIRECT_FAILED",
                        "We couldn’t follow this article’s redirects. Paste the text instead."
                    );
                    current = destinations.parse(current.resolve(page.location()).toString());
                    continue;
                }
                if (page.status() < 200 || page.status() >= 300) throw new ArticleImportException(
                    422,
                    "ACCESS_FAILED",
                    "This page could not be accessed (HTTP " +
                        page.status() +
                        "). It may require sign-in or block automated access. Paste the article text instead."
                );
                var text = extractor.extract(page, current.toString());
                try {
                    policy.validate(text.content());
                } catch (InvalidRequestException e) {
                    throw new ArticleImportException(
                        422,
                        "ARTICLE_TOO_LONG",
                        e.getMessage() + " Paste a shorter excerpt instead."
                    );
                }
                return new Article(entered, current.toString(), text.title(), text.content());
            }
            throw timeout();
        } catch (ArticleImportException e) {
            if (isFacebook(entered)) throw new ArticleImportException(
                e.status,
                "FACEBOOK_ACCESS",
                "Facebook did not provide a readable public article. Posts may require sign-in, restrict automated access or load with JavaScript. Open the publisher's original article link, or paste the post text instead."
            );
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            throw timeout();
        } catch (Exception e) {
            if (isFacebook(entered)) throw new ArticleImportException(
                422,
                "FACEBOOK_ACCESS",
                "Facebook could not be read without a signed-in browser. Use the publisher's original article URL or paste the Facebook post text instead."
            );
            if (System.nanoTime() >= deadline) throw timeout();
            throw new ArticleImportException(
                422,
                "FETCH_FAILED",
                "We couldn’t safely read this website. Check the link or paste the article text instead."
            );
        } finally {
            slots.release();
        }
    }

    private static boolean isFacebook(String url) {
        try {
            String h = URI.create(url).getHost().toLowerCase(Locale.ROOT);
            return (
                h.equals("facebook.com") ||
                h.endsWith(".facebook.com") ||
                h.equals("fb.watch") ||
                h.equals("fb.com")
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static ArticleImportException timeout() {
        return new ArticleImportException(
            504,
            "FETCH_TIMEOUT",
            "The website took too long to respond. Try again later or paste the article text instead."
        );
    }
}
