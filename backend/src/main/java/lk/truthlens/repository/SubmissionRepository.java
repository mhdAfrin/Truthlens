package lk.truthlens.repository;

import java.util.*;
import lk.truthlens.model.Submission;
import org.springframework.data.jpa.repository.*;

public interface SubmissionRepository
    extends JpaRepository<Submission, UUID>, JpaSpecificationExecutor<Submission>
{
    Optional<Submission> findByIdAndOwnerId(UUID id, UUID ownerId);
    Optional<Submission> findByIdAndGuestScopeAndOwnerIdIsNull(UUID id, UUID guestScope);
    List<Submission> findTop10ByStatusOrderByCreatedAtAsc(Submission.Status status);
}
