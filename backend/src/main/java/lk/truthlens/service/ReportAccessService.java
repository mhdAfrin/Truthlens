package lk.truthlens.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import lk.truthlens.exception.SubmissionNotFoundException;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportAccessService {

    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final ObjectMapper json;

    public ReportAccessService(
        SubmissionRepository submissions,
        UserRepository users,
        ObjectMapper json
    ) {
        this.submissions = submissions;
        this.users = users;
        this.json = json;
    }

    private UUID findOwnerId(String email) {
        return users.findByEmail(email).orElseThrow(SubmissionNotFoundException::new).getId();
    }

    @Transactional(readOnly = true)
    public JsonNode report(String email, UUID guestScope, UUID id) {
        Submission submission;
        if (email != null) submission = submissions
            .findByIdAndOwnerId(id, findOwnerId(email))
            .orElse(null);
        else submission = null;
        if (submission == null && guestScope != null) submission = submissions
            .findByIdAndGuestScopeAndOwnerIdIsNull(id, guestScope)
            .orElse(null);
        if (submission == null) throw new SubmissionNotFoundException();
        try {
            var result = (ObjectNode) json.readTree(submission.getReportJson());
            result.put("status", submission.getStatus().name());
            result.put("inputType", submission.getInputType());
            result.put("reportAvailable", submission.getStatus() == Submission.Status.COMPLETED || submission.getStatus() == Submission.Status.FAILED);
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Stored report could not be read", e);
        }
    }
}
