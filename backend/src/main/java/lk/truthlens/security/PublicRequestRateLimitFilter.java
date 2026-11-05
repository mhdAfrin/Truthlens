package lk.truthlens.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicRequestRateLimitFilter extends OncePerRequestFilter {

    private record Window(long start, int attempts) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean registration = path.equals(request.getContextPath() + "/api/auth/register");
        boolean urlSubmission = path.equals(request.getContextPath() + "/api/verifications/url");
        boolean guestSubmission =
            path.equals(request.getContextPath() + "/api/verifications") &&
            request.getUserPrincipal() == null;
        if (
            !"POST".equals(request.getMethod()) ||
            !(registration || guestSubmission || urlSubmission)
        ) {
            chain.doFilter(request, response);
            return;
        }
        String key = path + ":" + request.getRemoteAddr();
        long now = clock.millis();
        windows.entrySet().removeIf(e -> now - e.getValue().start >= 600000);
        if (windows.size() >= 10000 && !windows.containsKey(key)) {
            reject(response);
            return;
        }
        Window w = windows.compute(key, (k, v) ->
            v == null ? new Window(now, 1) : new Window(v.start, v.attempts + 1)
        );
        if (w.attempts > 20) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse r) throws IOException {
        r.setStatus(429);
        r.setHeader("Retry-After", "600");
        r.setContentType("application/json");
        r.getWriter().write(
            "{\"message\":\"Too many attempts. Please try again in 10 minutes.\",\"errors\":{}}"
        );
    }
}
