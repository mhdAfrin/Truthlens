package lk.truthlens.security;

public final class SecurityResponses {

    public static void reply(
        jakarta.servlet.http.HttpServletResponse response,
        int status,
        String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private SecurityResponses() {}
}
