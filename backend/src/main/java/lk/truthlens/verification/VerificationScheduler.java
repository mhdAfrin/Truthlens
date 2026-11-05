package lk.truthlens.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "truthlens.workflow.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class VerificationScheduler {

    private final VerificationOrchestrator workflow;

    public VerificationScheduler(VerificationOrchestrator workflow) {
        this.workflow = workflow;
    }

    @Scheduled(fixedDelayString = "${truthlens.workflow.poll-ms:1000}")
    public void process() {
        workflow.preparePending();
    }
}
