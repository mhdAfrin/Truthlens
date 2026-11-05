package lk.truthlens.integration.article;

import java.net.*;

public interface ArticleAddressResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
}
