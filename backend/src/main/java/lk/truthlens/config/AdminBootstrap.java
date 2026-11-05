package lk.truthlens.config;

import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class AdminBootstrap {

    @Bean
    ApplicationRunner provisionAdmin(
        UserRepository users,
        PasswordEncoder encoder,
        @Value("${TRUTHLENS_ADMIN_EMAIL:}") String email,
        @Value("${TRUTHLENS_ADMIN_PASSWORD:}") String password
    ) {
        return args -> {
            if (email.isBlank() && password.isBlank()) return;
            if (
                !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") ||
                password.length() < 16 ||
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
            ) throw new IllegalStateException(
                "Configure an admin email and a 16+ character admin password."
            );
            if (
                users
                    .findAll()
                    .stream()
                    .anyMatch(u -> u.getRole().equals("ADMIN"))
            ) return;
            if (
                users.existsByEmail(email.toLowerCase(java.util.Locale.ROOT))
            ) throw new IllegalStateException(
                "Admin bootstrap email already belongs to an account. Choose a distinct email."
            );
            var user = new UserAccount(
                "TruthLens Administrator",
                email.toLowerCase(java.util.Locale.ROOT),
                encoder.encode(password)
            );
            user.setRole("ADMIN");
            users.saveAndFlush(user);
        };
    }
}
