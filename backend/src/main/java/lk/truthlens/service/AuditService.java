package lk.truthlens.service;

import java.util.*;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.repository.AuditRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository audit;

    public AuditService(AuditRepository audit) {
        this.audit = audit;
    }

    public void record(String actor, String action, String target) {
        audit.append(actor, action, target);
    }

    public Map<String, Object> history(int page) {
        if (page < 0) throw new InvalidRequestException("Choose a valid page.");
        return Map.of("items", audit.findPage(page), "total", audit.count());
    }
}
