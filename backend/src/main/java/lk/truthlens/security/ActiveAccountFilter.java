package lk.truthlens.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import lk.truthlens.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Re-check active state and current role on every authenticated request. */
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public ActiveAccountFilter(UserRepository users) {
        this.users = users;
    }

    protected void doFilterInternal(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain
    ) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken && auth.isAuthenticated()) {
            var user = users.findByEmail(auth.getName()).orElse(null);
            if (user == null || !user.isActive()) {
                var session = req.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                SecurityResponses.reply(res, 401, "Please sign in to continue.");
                return;
            }
            var refreshed = UsernamePasswordAuthenticationToken.authenticated(
                auth.getPrincipal(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
            SecurityContextHolder.getContext().setAuthentication(refreshed);
        }
        chain.doFilter(req, res);
    }
}
