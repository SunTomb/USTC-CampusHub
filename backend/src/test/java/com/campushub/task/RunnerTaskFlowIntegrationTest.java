package com.campushub.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RunnerTaskFlowIntegrationTest {

    @Autowired
    private RunnerTaskService runnerTaskService;

    @Test
    void grabTaskImmediatelyAcceptsRunnerForGrabMode() {
        RewardTaskSummary published = runnerTaskService.publish(1L, new CreateRunnerTaskRequest(
                "快递代取",
                "帮忙从西区快递点取件",
                new BigDecimal("6.00"),
                BigDecimal.ZERO,
                "GRAB",
                "WEST",
                "CENTRAL",
                "西区快递点",
                "中区宿舍",
                LocalDateTime.now().plusHours(3),
                "COMPLETION_CODE"));

        RewardTaskSummary accepted = runnerTaskService.grab(published.id(), 2L);

        assertThat(accepted.workflowStatus()).isEqualTo("ACCEPTED");
        assertThat(accepted.acceptedApplicationId()).isNotNull();
    }

    @Test
    void applicationModeRequiresPublisherToAcceptOneApplicant() {
        RewardTaskSummary published = runnerTaskService.publish(1L, new CreateRunnerTaskRequest(
                "资料代取",
                "帮忙取打印资料",
                new BigDecimal("8.00"),
                BigDecimal.ZERO,
                "APPLICATION",
                "EAST",
                "WEST",
                "东区打印店",
                "西区宿舍",
                LocalDateTime.now().plusHours(4),
                "PHOTO_AND_CONFIRMATION"));

        TaskApplicationSummary application = runnerTaskService.apply(published.id(), 2L, new ApplyTaskRequest("我现在在东区"));

        assertThat(application.status()).isEqualTo("PENDING");
        TaskApplicationSummary accepted = runnerTaskService.acceptApplication(published.id(), application.id(), 1L);
        assertThat(accepted.status()).isEqualTo("ACCEPTED");
    }
}
