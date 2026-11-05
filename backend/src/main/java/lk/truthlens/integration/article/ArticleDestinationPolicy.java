package lk.truthlens.integration.article;

import java.net.*;
import java.util.*;
import lk.truthlens.exception.ArticleImportException;
import org.springframework.stereotype.Component;

@Component
public class ArticleDestinationPolicy {

    public record Target(URI uri, String host, List<InetAddress> addresses) {
        public Target {
            addresses = List.copyOf(addresses);
        }
    }

    private final ArticleAddressResolver dns;

    public ArticleDestinationPolicy(ArticleAddressResolver dns) {
        this.dns = dns;
    }

    public URI parse(String raw) {
        if (
            raw == null ||
            raw.isBlank() ||
            raw.length() > 2048 ||
            raw.chars().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c)) ||
            raw.contains("\\")
        ) throw ArticleImportException.invalid();
        try {
            URI input = new URI(raw);
            String scheme =
                input.getScheme() == null ? "" : input.getScheme().toLowerCase(Locale.ROOT);
            if (
                !Set.of("http", "https").contains(scheme) ||
                input.getRawAuthority() == null ||
                input.getRawAuthority().contains("@") ||
                input.getRawAuthority().contains("%")
            ) throw ArticleImportException.invalid();
            URL url = input.toURL();
            String host = url.getHost();
            if (host == null || host.isBlank()) throw ArticleImportException.invalid();
            if (host.startsWith("[") && host.endsWith("]")) host = host.substring(
                1,
                host.length() - 1
            );
            if (!host.contains(":")) host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(
                Locale.ROOT
            );
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            int port = url.getPort();
            if (
                port != -1 && port != (scheme.equals("https") ? 443 : 80)
            ) throw ArticleImportException.blocked();
            if (!host.contains(".") && !host.contains(":")) throw ArticleImportException.blocked();
            for (String suffix : List.of(
                "localhost",
                "local",
                "internal",
                "lan",
                "home",
                "test",
                "invalid",
                "example",
                "onion"
            ))
                if (
                    host.equals(suffix) || host.endsWith("." + suffix)
                ) throw ArticleImportException.blocked();
            String authority = host.contains(":") ? "[" + host + "]" : host;
            if (port != -1) authority += ":" + port;
            String path = input.getRawPath();
            URI result = new URI(
                scheme +
                    "://" +
                    authority +
                    (path == null || path.isEmpty() ? "/" : path) +
                    (input.getRawQuery() == null ? "" : "?" + input.getRawQuery())
            );
            if (result.toASCIIString().length() > 2048) throw ArticleImportException.invalid();
            return URI.create(result.toASCIIString());
        } catch (ArticleImportException e) {
            throw e;
        } catch (Exception e) {
            throw ArticleImportException.invalid();
        }
    }

    public Target resolve(URI uri) {
        String host = uri.getHost();
        if (host.startsWith("[")) host = host.substring(1, host.length() - 1);
        try {
            var addresses = dns.resolve(host);
            if (
                addresses.length == 0 || addresses.length > 16
            ) throw ArticleImportException.blocked();
            for (var address : addresses)
                if (!publicAddress(address)) throw ArticleImportException.blocked();
            return new Target(uri, host, Arrays.asList(addresses));
        } catch (UnknownHostException e) {
            throw new ArticleImportException(
                422,
                "FETCH_FAILED",
                "We couldn’t reach this website. Check the URL or paste the article text instead."
            );
        }
    }

    public static boolean publicAddress(InetAddress address) {
        if (
            address.isAnyLocalAddress() ||
            address.isLoopbackAddress() ||
            address.isLinkLocalAddress() ||
            address.isSiteLocalAddress() ||
            address.isMulticastAddress()
        ) return false;
        byte[] bytes = address.getAddress();
        int a = bytes[0] & 255,
            b = bytes[1] & 255;
        if (bytes.length == 4) {
            int c = bytes[2] & 255,
                d = bytes[3] & 255;
            return !(
                a == 0 ||
                a == 10 ||
                a == 127 ||
                a >= 224 ||
                (a == 100 && b >= 64 && b <= 127) ||
                (a == 169 && b == 254) ||
                (a == 172 && b >= 16 && b <= 31) ||
                (a == 192 && (b == 168 || b == 0 || (b == 88 && c == 99))) ||
                (a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100))) ||
                (a == 203 && b == 0 && c == 113) ||
                (a == 168 && b == 63 && c == 129 && d == 16)
            );
        }
        if ((a & 0xe0) != 0x20) return false;
        int c = bytes[2] & 255,
            d = bytes[3] & 255;
        if (a == 0x20 && b == 1 && (c < 2 || (c == 0x0d && d == 0xb8))) return false;
        if (a == 0x20 && b == 2) return false;
        return !(a == 0x3f && b == 0xff && (c & 0xf0) == 0);
    }
}
