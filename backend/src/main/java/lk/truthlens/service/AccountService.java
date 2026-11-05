package lk.truthlens.service;

import lk.truthlens.dto.AccountProfile;
import lk.truthlens.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final UserRepository users;

    public AccountService(UserRepository users) {
        this.users = users;
    }

    public AccountProfile profile(String email) {
        var account = users.findByEmail(email).orElseThrow();
        return new AccountProfile(
            account.getId(),
            account.getName(),
            account.getEmail(),
            account.getRole(),
            account.isActive()
        );
    }
}
