package lk.truthlens.repository;

import java.util.UUID;
import lk.truthlens.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository
    extends
        JpaRepository<UserAccount, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<UserAccount>
{
    java.util.Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
