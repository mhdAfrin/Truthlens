package lk.truthlens.controller;

import java.security.Principal;
import java.util.UUID;
import lk.truthlens.dto.AccountAccessRequest;
import lk.truthlens.service.AdminReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminReviewService admin;

    public AdminController(AdminReviewService admin) {
        this.admin = admin;
    }

    @GetMapping("/overview")
    public Object overview() {
        return admin.overview();
    }

    @GetMapping("/users")
    public Object users(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page
    ) {
        return admin.users(q, page);
    }

    @PatchMapping("/users/{id}")
    public Object access(
        Principal principal,
        @PathVariable UUID id,
        @RequestBody AccountAccessRequest change
    ) {
        return admin.access(principal.getName(), id, change);
    }

    @GetMapping("/submissions")
    public Object submissions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "") String status
    ) {
        return admin.submissions(page, status);
    }

    @GetMapping("/submissions/{id}")
    public Object submission(Principal principal, @PathVariable UUID id) {
        return admin.submission(principal.getName(), id);
    }

    @GetMapping("/audit")
    public Object audit(@RequestParam(defaultValue = "0") int page) {
        return admin.audit(page);
    }

    @GetMapping("/system")
    public Object system() throws Exception {
        return admin.system();
    }
}
