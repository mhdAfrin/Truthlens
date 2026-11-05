package lk.truthlens.integration.article;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import lk.truthlens.exception.ArticleImportException;
import org.apache.hc.client5.http.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

@Component
public class PinnedArticleTransport implements ArticleTransport {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "article-deadline");
        t.setDaemon(true);
        return t;
    });

    public Page get(ArticleDestinationPolicy.Target target, long remainingMillis)
        throws IOException {
        if (remainingMillis <= 0) throw new SocketTimeoutException();
        DnsResolver pinned = new DnsResolver() {
            public InetAddress[] resolve(String host) throws UnknownHostException {
                String clean = host.replace("[", "").replace("]", "");
                if (!clean.equalsIgnoreCase(target.host())) throw new UnknownHostException(
                    "Unexpected host"
                );
                return target.addresses().toArray(InetAddress[]::new);
            }

            public String resolveCanonicalHostname(String host) {
                return target.host();
            }
        };
        var manager = PoolingHttpClientConnectionManagerBuilder.create()
            .setDnsResolver(pinned)
            .setMaxConnTotal(1)
            .setMaxConnPerRoute(1)
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(Math.min(3000, remainingMillis)))
                    .setSocketTimeout(Timeout.ofMilliseconds(Math.min(3000, remainingMillis)))
                    .build()
            )
            .build();
        var config = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(Math.min(5000, remainingMillis)))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(remainingMillis))
            .setHardCancellationEnabled(true)
            .setAuthenticationEnabled(false)
            .setContentCompressionEnabled(false)
            .build();
        var request = new HttpGet(target.uri());
        request.setConfig(config);
        request.setHeader("Accept", "text/html,application/xhtml+xml,text/plain;q=0.8");
        request.setHeader("Accept-Encoding", "identity");
        request.setHeader("User-Agent", "TruthLens/1.0 ArticleReader");
        var cancel = timer.schedule(request::cancel, remainingMillis, TimeUnit.MILLISECONDS);
        try (
            var client = HttpClients.custom()
                .setConnectionManager(manager)
                .setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE))
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableContentCompression()
                .build()
        ) {
            var response = client.execute(request);
            try {
                int status = response.getCode();
                String location = header(response, "Location"),
                    type = header(response, "Content-Type"),
                    encoding = header(response, "Content-Encoding");
                if (status < 200 || status >= 300) return new Page(
                    status,
                    location,
                    type,
                    encoding,
                    new byte[0]
                );
                if (
                    !encoding.isEmpty() && !encoding.equalsIgnoreCase("identity")
                ) throw new ArticleImportException(
                    422,
                    "UNSUPPORTED_PAGE",
                    "This page uses an unsupported encoding. Paste the article text instead."
                );
                String mime = type.split(";")[0].strip().toLowerCase(java.util.Locale.ROOT);
                if (
                    !java.util.Set.of(
                        "text/html",
                        "application/xhtml+xml",
                        "text/plain",
                        "application/rss+xml",
                        "application/xml",
                        "text/xml"
                    ).contains(mime)
                ) throw new ArticleImportException(
                    422,
                    "UNSUPPORTED_PAGE",
                    "This link does not contain a supported HTML or text article. Paste its text instead."
                );
                var entity = response.getEntity();
                if (entity == null) throw new ArticleImportException(
                    422,
                    "EMPTY_PAGE",
                    "This page did not contain readable article text. Paste the text instead."
                );
                if (entity.getContentLength() > MAX_BYTES) throw tooLarge();
                byte[] bytes = entity.getContent().readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw tooLarge();
                return new Page(status, location, type, encoding, bytes);
            } finally {
                response.close(CloseMode.IMMEDIATE);
            }
        } finally {
            cancel.cancel(false);
            manager.close(CloseMode.IMMEDIATE);
        }
    }

    private static String header(org.apache.hc.core5.http.HttpResponse response, String name) {
        var header = response.getFirstHeader(name);
        return header == null ? "" : header.getValue();
    }

    private static ArticleImportException tooLarge() {
        return new ArticleImportException(
            422,
            "PAGE_TOO_LARGE",
            "This page is too large to import. Paste the article text instead."
        );
    }

    @PreDestroy
    public void close() {
        timer.shutdownNow();
    }
}
