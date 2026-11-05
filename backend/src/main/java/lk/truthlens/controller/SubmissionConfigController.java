package lk.truthlens.controller;

import java.util.Map;
import lk.truthlens.service.SubmissionPolicy;
import org.springframework.web.bind.annotation.*;

@RestController
public class SubmissionConfigController {

    private final SubmissionPolicy policy;

    public SubmissionConfigController(SubmissionPolicy policy) {
        this.policy = policy;
    }

    @GetMapping("/api/config/submission")
    public Map<String, Object> config() {
        return Map.of(
            "maxCodePoints",
            policy.maxCodePoints(),
            "unit",
            "Unicode code points",
            "format",
            "text-v1"
        );
    }
}
