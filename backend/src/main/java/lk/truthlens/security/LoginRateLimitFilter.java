package lk.truthlens.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/** Single-instance direct-IP throttling; do not trust unvalidated proxy headers. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private record Window(long start, int attempts) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    protected void doFilterInternal(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain
    ) throws ServletException, IOException {
        if (
            !req.getMethod().equals("POST") ||
            (!req.getServletPath().equals("/api/auth/login") &&
                !req.getRequestURI().equals("/api/auth/login"))
        ) {
            chain.doFilter(req, res);
            return;
        }
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() >= 600000);
        if (windows.size() >= 10000 && !windows.containsKey(req.getRemoteAddr())) {
            SecurityResponses.reply(res, 429, "Too many attempts. Please try again in 10 minutes.");
            return;
        }
        var w = windows.compute(req.getRemoteAddr(), (k, v) ->
            v == null ? new Window(now, 1) : new Window(v.start(), v.attempts() + 1)
        );
        if (w.attempts() > 20) {
            res.setHeader("Retry-After", "600");
            SecurityResponses.reply(res, 429, "Too many attempts. Please try again in 10 minutes.");
            return;
        }
        String email = req.getParameter("email"),
            password = req.getParameter("password");
        if (
            email == null ||
            email.length() > 254 ||
            password == null ||
            password.isEmpty() ||
            password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
        ) {
            SecurityResponses.reply(res, 401, "Unable to sign in with those credentials.");
            return;
        }
        chain.doFilter(req, res);
    }
}
