package lk.truthlens.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounds registration/submission JSON even with chunked transfer encoding. */
@Component
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        String path = request.getRequestURI();
        boolean adminForm =
            path.startsWith(request.getContextPath() + "/api/admin/") &&
            java.util.Set.of("POST", "PATCH", "PUT").contains(request.getMethod());
        boolean registration = path.equals(request.getContextPath() + "/api/auth/register");
        boolean urlSubmission = path.equals(request.getContextPath() + "/api/verifications/url");
        boolean submission = path.equals(request.getContextPath() + "/api/verifications");
        if (
            (!"POST".equals(request.getMethod()) && !adminForm) ||
            !(registration || submission || urlSubmission || adminForm)
        ) {
            chain.doFilter(request, response);
            return;
        }
        int limit = registration || urlSubmission || adminForm ? 8192 : 262144;
        byte[] body = request.getInputStream().readNBytes(limit + 1);
        if (body.length > limit) {
            response.setStatus(413);
            response.setContentType("application/json");
            response
                .getWriter()
                .write(
                    "{\"message\":\"The form is too large. Shorten your details and try again.\",\"errors\":{}}"
                );
            return;
        }
        chain.doFilter(
            new HttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() {
                    ByteArrayInputStream input = new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        @Override
                        public int read() {
                            return input.read();
                        }

                        @Override
                        public boolean isFinished() {
                            return input.available() == 0;
                        }

                        @Override
                        public boolean isReady() {
                            return true;
                        }

                        @Override
                        public void setReadListener(ReadListener listener) {
                            throw new UnsupportedOperationException(
                                "Synchronous registration endpoint"
                            );
                        }
                    };
                }
            },
            response
        );
    }
}
