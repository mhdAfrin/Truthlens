package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.*;
import lk.truthlens.exception.ArticleImportException;
import lk.truthlens.integration.article.ArticleDestinationPolicy;
import lk.truthlens.integration.article.PinnedArticleTransport;
import org.junit.jupiter.api.*;

class ArticleTransportTest {

    HttpServer server;
    PinnedArticleTransport transport;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                var t = new Thread(r);
                t.setDaemon(true);
                return t;
            })
        );
        server.start();
        transport = new PinnedArticleTransport();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        transport.close();
    }

    ArticleDestinationPolicy.Target target(String path) throws Exception {
        return new ArticleDestinationPolicy.Target(
            URI.create("http://fixture.news.org:" + server.getAddress().getPort() + path),
            "fixture.news.org",
            List.of(InetAddress.getByName("127.0.0.1"))
        );
    }

    @Test
    void usesPinnedAddressAndOriginalHostWithoutCookies() throws Exception {
        server.createContext("/article", e -> {
            assertEquals(
                "fixture.news.org:" + server.getAddress().getPort(),
                e.getRequestHeaders().getFirst("Host")
            );
            assertNull(e.getRequestHeaders().getFirst("Cookie"));
            assertNull(e.getRequestHeaders().getFirst("Authorization"));
            byte[] body = ArticleImportTest.HTML.getBytes();
            e.getResponseHeaders().set("Content-Type", "text/html");
            e.sendResponseHeaders(200, body.length);
            e.getResponseBody().write(body);
            e.close();
        });
        assertEquals(200, transport.get(target("/article"), 2000).status());
    }

    @Test
    void rejectsOversizedStreamingResponse() throws Exception {
        server.createContext("/large", e -> {
            e.getResponseHeaders().set("Content-Type", "text/html");
            e.sendResponseHeaders(200, 0);
            try {
                e.getResponseBody().write(new byte[PinnedArticleTransport.MAX_BYTES + 1]);
            } finally {
                e.close();
            }
        });
        assertEquals(
            "PAGE_TOO_LARGE",
            assertThrows(ArticleImportException.class, () ->
                transport.get(target("/large"), 2000)
            ).code
        );
    }

    @Test
    void rejectsUnsupportedMimeAndEncoding() throws Exception {
        server.createContext("/pdf", e -> {
            e.getResponseHeaders().set("Content-Type", "application/pdf");
            e.sendResponseHeaders(200, 1);
            e.getResponseBody().write(1);
            e.close();
        });
        assertThrows(ArticleImportException.class, () -> transport.get(target("/pdf"), 2000));
        server.createContext("/gzip", e -> {
            e.getResponseHeaders().set("Content-Type", "text/html");
            e.getResponseHeaders().set("Content-Encoding", "gzip");
            e.sendResponseHeaders(200, 1);
            e.getResponseBody().write(1);
            e.close();
        });
        assertThrows(ArticleImportException.class, () -> transport.get(target("/gzip"), 2000));
    }

    @Test
    void doesNotAutomaticallyFollowRedirects() throws Exception {
        server.createContext("/redirect", e -> {
            e.getResponseHeaders().set("Location", "http://127.0.0.1/private");
            e.sendResponseHeaders(302, -1);
            e.close();
        });
        assertEquals(302, transport.get(target("/redirect"), 2000).status());
    }

    @Test
    void deadlineCancelsSlowResponse() throws Exception {
        server.createContext("/slow", e -> {
            try {
                Thread.sleep(1000);
                e.sendResponseHeaders(200, -1);
            } catch (Exception ignored) {
            } finally {
                e.close();
            }
        });
        long started = System.nanoTime();
        assertThrows(java.io.IOException.class, () -> transport.get(target("/slow"), 100));
        assertTrue((System.nanoTime() - started) / 1000000 < 1500);
    }
}
