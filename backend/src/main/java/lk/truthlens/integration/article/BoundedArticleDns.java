package lk.truthlens.integration.article;

import jakarta.annotation.PreDestroy;
import java.net.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

@Component
public class BoundedArticleDns implements ArticleAddressResolver {

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
        4,
        4,
        0,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16),
        r -> {
            Thread t = new Thread(r, "article-dns");
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.AbortPolicy()
    );

    public InetAddress[] resolve(String host) throws UnknownHostException {
        Future<InetAddress[]> task;
        try {
            task = pool.submit(() -> InetAddress.getAllByName(host));
        } catch (RejectedExecutionException e) {
            throw new UnknownHostException("DNS capacity unavailable");
        }
        try {
            return task.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            task.cancel(true);
            pool.purge();
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new UnknownHostException("DNS resolution failed");
        }
    }

    @PreDestroy
    void close() {
        pool.shutdownNow();
    }
}
