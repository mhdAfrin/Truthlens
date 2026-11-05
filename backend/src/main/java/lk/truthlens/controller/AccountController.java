package lk.truthlens.controller;

import java.security.Principal;
import java.util.Map;
import lk.truthlens.service.AccountService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/api/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping({ "/api/auth/me", "/api/account/profile" })
    lk.truthlens.dto.AccountProfile profile(Principal principal) {
        return accounts.profile(principal.getName());
    }

    @GetMapping("/api/admin/session")
    Map<String, String> admin() {
        return Map.of("message", "Administrator access confirmed.");
    }
}
