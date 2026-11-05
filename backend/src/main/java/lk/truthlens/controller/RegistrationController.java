package lk.truthlens.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lk.truthlens.dto.RegistrationRequest;
import lk.truthlens.model.UserAccount;
import lk.truthlens.service.RegistrationService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        UserAccount u = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of(
                "message",
                "Your TruthLens account was created successfully.",
                "user",
                Map.of(
                    "id",
                    u.getId(),
                    "name",
                    u.getName(),
                    "email",
                    u.getEmail(),
                    "role",
                    u.getRole(),
                    "createdAt",
                    u.getCreatedAt()
                )
            )
        );
    }
}
