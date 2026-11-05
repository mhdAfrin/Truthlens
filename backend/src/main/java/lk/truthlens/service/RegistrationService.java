package lk.truthlens.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import lk.truthlens.dto.RegistrationRequest;
import lk.truthlens.exception.DuplicateEmailException;
import lk.truthlens.exception.RegistrationValidationException;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public UserAccount register(RegistrationRequest request) {
        String password = request.password();
        if (
            password == null ||
            password.length() < 8 ||
            password.getBytes(StandardCharsets.UTF_8).length > 72 ||
            !password.matches("(?s).*[A-Za-z].*") ||
            !password.matches("(?s).*[0-9].*")
        ) throw new RegistrationValidationException(
            Map.of(
                "password",
                "Use at least 8 characters, including a letter and a number (maximum 72 bytes)."
            )
        );
        String email = request.email().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        return users.saveAndFlush(new UserAccount(request.name(), email, encoder.encode(password)));
    }
}
