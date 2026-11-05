package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lk.truthlens.exception.ArticleImportException;
import lk.truthlens.integration.article.ArticleDestinationPolicy;
import lk.truthlens.integration.article.ArticleExtractor;
import lk.truthlens.integration.article.ArticleImporter;
import lk.truthlens.integration.article.ArticleTransport;
import lk.truthlens.service.SubmissionPolicy;
import org.junit.jupiter.api.*;

class ArticleImportTest {

    static final String HTML =
        "<html><title>News</title><nav>Navigation secret</nav><article><h1>Research update</h1><p>Researchers published a detailed review of the available evidence about the reported changes in public transport.</p><p>The review describes its methods and limitations so that readers can evaluate the claims independently.</p><script>secret script</script></article></html>";
    ArticleDestinationPolicy policy = new ArticleDestinationPolicy(host -> new InetAddress[] {
        InetAddress.getByName("93.184.215.14"),
    });

    ArticleTransport.Page page(int status, String location, String html) {
        return new ArticleTransport.Page(
            status,
            location,
            "text/html; charset=UTF-8",
            "",
            html.getBytes(StandardCharsets.UTF_8)
        );
    }

    ArticleImporter importer(ArticleTransport transport) {
        return new ArticleImporter(
            policy,
            transport,
            new ArticleExtractor(),
            new SubmissionPolicy(20000)
        );
    }

    @Test
    void invalidFormatsRejected() {
        for (String value : List.of(
            "",
            "ftp://news.org/a",
            "https://user:password@news.org/a",
            "https://news.org/a b",
            "https://news.org\\private",
            "not a URL"
        ))
            assertThrows(ArticleImportException.class, () -> policy.parse(value), value);
    }

    @Test
    void localNamesAndPortsBlocked() {
        for (String value : List.of(
            "http://localhost/a",
            "http://service.internal/a",
            "https://news.org:8443/a",
            "http://singlelabel/a"
        ))
            assertThrows(ArticleImportException.class, () -> policy.parse(value), value);
    }

    @Test
    void specialAddressesBlocked() throws Exception {
        for (String ip : List.of(
            "0.0.0.0",
            "127.0.0.1",
            "10.1.2.3",
            "172.16.0.1",
            "192.168.1.1",
            "169.254.169.254",
            "100.64.0.1",
            "198.18.0.1",
            "192.0.2.1",
            "203.0.113.1",
            "168.63.129.16",
            "224.0.0.1",
            "::1",
            "fc00::1",
            "fe80::1",
            "64:ff9b::a00:1",
            "2001:db8::1",
            "2002:a00:1::1"
        ))
            assertFalse(ArticleDestinationPolicy.publicAddress(InetAddress.getByName(ip)), ip);
        assertTrue(
            ArticleDestinationPolicy.publicAddress(InetAddress.getByName("2606:4700:4700::1111"))
        );
    }

    @Test
    void mixedDnsAnswersRejected() {
        var p = new ArticleDestinationPolicy(host -> new InetAddress[] {
            InetAddress.getByName("93.184.215.14"),
            InetAddress.getByName("127.0.0.1"),
        });
        assertThrows(ArticleImportException.class, () -> p.resolve(p.parse("https://news.org/a")));
    }

    @Test
    void extractsArticleAndPreservesOriginal() {
        var article = importer((target, time) -> page(200, "", HTML)).fetch(
            "https://news.org/story#section"
        );
        assertEquals("https://news.org/story#section", article.originalUrl());
        assertEquals("https://news.org/story", article.resolvedUrl());
        assertEquals("Research update", article.title());
        assertTrue(article.content().contains("Researchers"));
        assertFalse(article.content().contains("secret"));
    }

    @Test
    void redirectsRetainOriginalAndRevalidate() {
        AtomicInteger calls = new AtomicInteger();
        var article = importer((target, time) ->
            calls.incrementAndGet() == 1 ? page(302, "/final", "") : page(200, "", HTML)
        ).fetch("https://news.org/start");
        assertEquals("https://news.org/final", article.resolvedUrl());
        assertEquals(2, calls.get());
    }

    @Test
    void internalRedirectNeverFetched() {
        AtomicInteger calls = new AtomicInteger();
        var p = new ArticleDestinationPolicy(host -> new InetAddress[] {
            InetAddress.getByName(host.equals("internal.org") ? "127.0.0.1" : "93.184.215.14"),
        });
        var service = new ArticleImporter(
            p,
            (target, time) -> {
                calls.incrementAndGet();
                return page(302, "http://internal.org/private", "");
            },
            new ArticleExtractor(),
            new SubmissionPolicy(20000)
        );
        assertThrows(ArticleImportException.class, () -> service.fetch("https://news.org/start"));
        assertEquals(1, calls.get());
    }

    @Test
    void redirectLoopsAndExcessiveHopsFail() {
        assertThrows(ArticleImportException.class, () ->
            importer((t, time) -> page(302, "/same", "")).fetch("https://news.org/same")
        );
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ArticleImportException.class, () ->
            importer((t, time) -> page(302, "/hop" + calls.incrementAndGet(), "")).fetch(
                "https://news.org/start"
            )
        );
        assertEquals(4, calls.get());
    }

    @Test
    void failuresExplainTextAlternative() {
        for (int status : List.of(401, 403, 404, 500)) {
            var e = assertThrows(ArticleImportException.class, () ->
                importer((t, time) -> page(status, "", "")).fetch("https://news.org/a")
            );
            assertEquals("ACCESS_FAILED", e.code);
            assertTrue(e.getMessage().contains("Paste"));
        }
    }

    @Test
    void noReadableArticleAndChallengesFail() {
        for (String html : List.of(
            "<html><body>Sign in</body></html>",
            "<title>Just a moment...</title>" + HTML
        )) {
            assertThrows(ArticleImportException.class, () ->
                importer((t, time) -> page(200, "", html)).fetch("https://news.org/a")
            );
        }
    }

    @Test
    void oversizedArticleRejectedWithoutTruncation() {
        var e = assertThrows(ArticleImportException.class, () ->
            importer((t, time) ->
                page(200, "", "<article><p>" + "x".repeat(20001) + "</p></article>")
            ).fetch("https://news.org/a")
        );
        assertEquals("ARTICLE_TOO_LONG", e.code);
    }

    @Test
    void timeoutsAreUnderstandable() {
        var e = assertThrows(ArticleImportException.class, () ->
            importer((t, time) -> {
                throw new SocketTimeoutException();
            }).fetch("https://news.org/a")
        );
        assertEquals(504, e.status);
        assertEquals("FETCH_TIMEOUT", e.code);
    }

    @Test
    void unknownHostExplainsFailure() {
        var p = new ArticleDestinationPolicy(host -> {
            throw new UnknownHostException();
        });
        assertThrows(ArticleImportException.class, () -> p.resolve(p.parse("https://news.org/a")));
    }

    @Test
    void facebookLoginPageIsNeverStoredAsArticle() {
        String login =
            "<title>Log in to Facebook</title><form action='/login'><input name='email'/><input name='pass'/></form>" +
            HTML;
        var e = assertThrows(ArticleImportException.class, () ->
            importer((t, time) -> page(200, "", login)).fetch(
                "https://www.facebook.com/example/posts/123"
            )
        );
        assertEquals("FACEBOOK_ACCESS", e.code);
        assertTrue(e.getMessage().contains("publisher"));
    }

    @Test
    void facebookNetworkFailureOffersOriginalPublisherOrText() {
        var e = assertThrows(ArticleImportException.class, () ->
            importer((t, time) -> {
                throw new java.io.IOException();
            }).fetch("https://www.facebook.com/example/posts/123")
        );
        assertEquals("FACEBOOK_ACCESS", e.code);
    }
}
