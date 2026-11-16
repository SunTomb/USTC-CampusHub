package com.campushub.task;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:runner-committed;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunnerTaskCommittedIntegrationTest {
    @Autowired RunnerTaskService service;
    @Autowired UserRoleService roles;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired RewardTaskRepository taskRepository;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach
    void roles() {
        roles.assignRole(2L, "ROLE_RUNNER");
        roles.assignRole(3L, "ROLE_RUNNER");
        roles.assignRole(4L, "ROLE_MASTER_ADMIN");
    }

    private Long task(String mode) {
        return service.publish(1L, new CreateRunnerTaskRequest("独立事务测试", "只用于H2测试",
                BigDecimal.TEN, BigDecimal.ZERO, mode, "WEST", "CENTRAL", "A", "B",
                LocalDateTime.now().plusHours(2), "COMPLETION_CODE")).id();
    }

    private void delivered(Long id) {
        service.grab(id, 2L);
        for (String status : new String[]{"HEADING_TO_PICKUP", "PICKED_UP", "DELIVERING", "PENDING_CONFIRMATION"}) {
            service.advance(id, 2L, status, null);
        }
    }

    private String auth(Long id) { return "Bearer " + tokens.issueToken(id, "isolated-test"); }

    @Test
    void failedAttemptsCommitAndRegenerationRestoresVerification() {
        Long id = task("GRAB"); delivered(id);
        String code = service.issueCompletionCode(id, 1L).completionCode();
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> service.completeWithCode(id, 2L, new TaskActionRequest(null, "wrong")))
                    .isInstanceOf(BusinessException.class);
            assertThat(jdbc.queryForObject("select completion_code_attempts from reward_tasks where id=?", Integer.class, id))
                    .isEqualTo(i);
        }
        assertThatThrownBy(() -> service.completeWithCode(id, 2L, new TaskActionRequest(null, code)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("重新生成");
        String replacement = service.issueCompletionCode(id, 1L).completionCode();
        assertThat(jdbc.queryForObject("select completion_code_attempts from reward_tasks where id=?", Integer.class, id)).isZero();
        assertThat(service.completeWithCode(id, 2L, new TaskActionRequest(null, replacement)).workflowStatus()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select count(*) from reward_tasks where id=? and completion_code_hash is null", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void expiredAndMissingCodesNeverCompleteTasks() {
        Long id = task("GRAB"); delivered(id);
        assertThatThrownBy(() -> service.completeWithCode(id, 2L, new TaskActionRequest(null, "123456")))
                .isInstanceOf(BusinessException.class);
        String code = service.issueCompletionCode(id, 1L).completionCode();
        jdbc.update("update reward_tasks set completion_code_issued_at=? where id=?", LocalDateTime.now().minusHours(1), id);
        assertThatThrownBy(() -> service.completeWithCode(id, 2L, new TaskActionRequest(null, code)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("过期");
        assertThat(jdbc.queryForObject("select workflow_status from reward_tasks where id=?", String.class, id)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(jdbc.queryForObject("select count(*) from reward_tasks where id=? and completion_code_hash is null", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void simultaneousGrabsHaveExactlyOneWinner() throws Exception {
        Long id = task("GRAB");
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = pool.submit(() -> { start.await(); return attemptGrab(id, 2L); });
            var second = pool.submit(() -> { start.await(); return attemptGrab(id, 3L); });
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from task_applications where task_id=? and status='ACCEPTED'", Integer.class, id)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private int attemptGrab(Long taskId, Long userId) {
        try { service.grab(taskId, userId); return 1; }
        catch (BusinessException expected) { return 0; }
    }

    @Test
    void expiredTasksRejectGrabsApplicationsAndSelection() {
        Long grab = task("GRAB"), application = task("APPLICATION");
        Long applicantId = service.apply(application, 2L, new ApplyTaskRequest("申请")).id();
        jdbc.update("update reward_tasks set deadline=? where id in (?,?)", LocalDateTime.now().minusHours(1), grab, application);
        assertThatThrownBy(() -> service.grab(grab, 2L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.apply(application, 3L, new ApplyTaskRequest("申请"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.acceptApplication(application, applicantId, 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void administratorsCannotImpersonateParticipantsThroughLegacyQueryParameters() throws Exception {
        Long id = task("GRAB"); delivered(id);
        String code = service.issueCompletionCode(id, 1L).completionCode();
        mvc.perform(get("/api/tasks/{id}/workspace", id).header("Authorization", auth(4L))).andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks/{id}/confirm?publisherId=1", id).header("Authorization", auth(4L)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks/{id}/complete-code?runnerId=2", id).header("Authorization", auth(4L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completionCode\":\"" + code + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks/{id}/issues?reporterId=1", id).header("Authorization", auth(4L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"issueType\":\"OTHER\",\"description\":\"非本人\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select workflow_status from reward_tasks where id=?", String.class, id)).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void publisherHistoryIncludesCompletedTaskButNotAnotherPublishersTask() {
        Long id = task("GRAB"); delivered(id); service.confirmCompletion(id, 1L, null);
        assertThat(service.listByPublisher(1L)).anySatisfy(task -> {
            assertThat(task.id()).isEqualTo(id); assertThat(task.workflowStatus()).isEqualTo("COMPLETED");
        });
        assertThat(service.listByPublisher(2L)).noneMatch(task -> task.id().equals(id));
    }
    @Test
    void runnerCannotReadOtherApplicantsPrivateMessages() throws Exception {
        Long id = task("APPLICATION");
        service.apply(id, 3L, new ApplyTaskRequest("private-other-applicant-contact"));
        var accepted = service.apply(id, 2L, new ApplyTaskRequest("selected-runner-note"));
        service.acceptApplication(id, accepted.id(), 1L);
        mvc.perform(get("/api/tasks/{id}/workspace", id).header("Authorization", auth(2L)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-other-applicant-contact"))));
        mvc.perform(get("/api/tasks/{id}/workspace", id).header("Authorization", auth(1L)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("private-other-applicant-contact")));
    }

    @Test
    void adminClosureClearsCodeWithoutLosingAssignedRunnerAndCannotRewriteCompletion() throws Exception {
        Long id = task("GRAB"); delivered(id); service.issueCompletionCode(id, 1L);
        Long applicationId = jdbc.queryForObject("select accepted_application_id from reward_tasks where id=?", Long.class, id);
        mvc.perform(post("/api/admin/trade/tasks/{id}/close", id).header("Authorization", auth(4L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertThat(jdbc.queryForObject("select accepted_application_id from reward_tasks where id=?", Long.class, id)).isEqualTo(applicationId);
        assertThat(jdbc.queryForObject("select count(*) from reward_tasks where id=? and completion_code_hash is null", Integer.class, id)).isEqualTo(1);
        assertThat(service.workspace(id, 2L).task().workflowStatus()).isEqualTo("CANCELLED");
        Long completed = task("GRAB"); delivered(completed); service.confirmCompletion(completed, 1L, null);
        mvc.perform(post("/api/admin/trade/tasks/{id}/close", completed).header("Authorization", auth(4L)))
                .andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("select workflow_status from reward_tasks where id=?", String.class, completed)).isEqualTo("COMPLETED");
    }

    @Test
    void administratorWaitsForClaimTransactionAndKeepsItsAssignment() throws Exception {
        Long id = task("GRAB");
        var pool = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Integer>>();
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                taskRepository.findByIdForUpdate(id).orElseThrow();
                future.set(pool.submit(() -> {
                    started.countDown();
                    return mvc.perform(post("/api/admin/trade/tasks/{id}/close", id).header("Authorization", auth(4L)))
                            .andReturn().getResponse().getStatus();
                }));
                try {
                    assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> future.get().get(250, TimeUnit.MILLISECONDS))
                            .isInstanceOf(java.util.concurrent.TimeoutException.class);
                } catch (InterruptedException error) { throw new RuntimeException(error); }
                service.grab(id, 2L);
            });
            assertThat(future.get().get(15, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(service.workspace(id, 2L).runnerContact().userId()).isEqualTo(2L);
            assertThat(service.workspace(id, 2L).task().workflowStatus()).isEqualTo("CANCELLED");
        } finally { pool.shutdownNow(); }
    }

}
