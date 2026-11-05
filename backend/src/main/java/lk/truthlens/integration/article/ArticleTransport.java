package lk.truthlens.integration.article;

import java.io.IOException;

public interface ArticleTransport {
    record Page(int status, String location, String contentType, String encoding, byte[] body) {}

    Page get(ArticleDestinationPolicy.Target target, long remainingMillis) throws IOException;
}
