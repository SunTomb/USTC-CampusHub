package com.campushub.task;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RunnerTaskBoundaryIntegrationTest {
    @Autowired RunnerTaskService service;
    @Autowired UserRoleService roles;
    @Autowired RewardTaskRepository tasks;
    @Autowired TaskApplicationRepository applications;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired ObjectMapper json;

    @BeforeEach
    void runners() {
        roles.assignRole(1L, "ROLE_RUNNER");
        roles.assignRole(2L, "ROLE_RUNNER");
        roles.assignRole(3L, "ROLE_RUNNER");
    }

    private RewardTaskSummary publish(String mode, String verification) {
        return service.publish(1L, request(mode, verification, BigDecimal.TEN, LocalDateTime.now().plusHours(3)));
    }

    private CreateRunnerTaskRequest request(String mode, String verification, BigDecimal amount, LocalDateTime deadline) {
        return new CreateRunnerTaskRequest("测试任务", "公开地点交接", amount, BigDecimal.ZERO,
                mode, "CENTRAL", "WEST", "图书馆", "食堂门口", deadline, verification, 500, null, null);
    }

    private String auth(Long id) { return "Bearer " + tokens.issueToken(id, "test-user"); }

    private void delivered(Long id) {
        service.advance(id, 2L, "HEADING_TO_PICKUP", null);
        service.advance(id, 2L, "PICKED_UP", null);
        service.advance(id, 2L, "DELIVERING", null);
        service.advance(id, 2L, "PENDING_CONFIRMATION", null);
    }

    @Test
    void rejectsOutOfOrderAndTerminalTransitionsWithoutMutatingTask() {
        var task = publish("GRAB", "COMPLETION_CODE");
        service.grab(task.id(), 2L);
        assertThatThrownBy(() -> service.advance(task.id(), 2L, "COMPLETED", null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.confirmCompletion(task.id(), 1L, null)).isInstanceOf(BusinessException.class);
        assertThat(tasks.findById(task.id()).orElseThrow().getWorkflowStatus()).isEqualTo("ACCEPTED");
        delivered(task.id());
        service.confirmCompletion(task.id(), 1L, null);
        assertThatThrownBy(() -> service.advance(task.id(), 2L, "HEADING_TO_PICKUP", null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.reportIssue(task.id(), 2L, new ReportTaskIssueRequest("OTHER", "已结束"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void selfApplicationsAndDuplicateApplicationsCannotCreateExtraRows() {
        var task = publish("APPLICATION", "PHOTO_AND_CONFIRMATION");
        assertThatThrownBy(() -> service.apply(task.id(), 1L, new ApplyTaskRequest("自己"))).isInstanceOf(BusinessException.class);
        var first = service.apply(task.id(), 2L, new ApplyTaskRequest("我可以"));
        var repeated = service.apply(task.id(), 2L, new ApplyTaskRequest("重试"));
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(applications.findByTaskIdOrderByCreatedAtDesc(task.id())).hasSize(1);
    }

    @Test
    void acceptingOneApplicantRejectsTheOtherPendingApplications() {
        var task = publish("APPLICATION", "PHOTO_AND_CONFIRMATION");
        var first = service.apply(task.id(), 2L, new ApplyTaskRequest("第一位"));
        var other = service.apply(task.id(), 3L, new ApplyTaskRequest("第二位"));
        service.acceptApplication(task.id(), first.id(), 1L);
        assertThat(applications.findById(other.id()).orElseThrow().getStatus()).isEqualTo("REJECTED");
        assertThatThrownBy(() -> service.acceptApplication(task.id(), other.id(), 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void publishRejectsNegativeAmountPastDeadlineAndOrderDeposits() {
        assertThatThrownBy(() -> service.publish(1L, request("GRAB", "PHOTO_AND_CONFIRMATION", new BigDecimal("-1"), LocalDateTime.now().plusHours(1)))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.publish(1L, request("GRAB", "PHOTO_AND_CONFIRMATION", BigDecimal.ONE, LocalDateTime.now().minusMinutes(1)))).isInstanceOf(BusinessException.class);
        var request = new CreateRunnerTaskRequest("保证金不开放", "测试", BigDecimal.TEN, BigDecimal.ONE, "GRAB", "CENTRAL", "WEST", "A", "B", LocalDateTime.now().plusHours(1), "COMPLETION_CODE", 500, null, null);
        assertThatThrownBy(() -> service.publish(1L, request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void strangerCannotCreateIssueOrReadPrivateWorkspace() throws Exception {
        var task = publish("GRAB", "PHOTO_AND_CONFIRMATION");
        service.grab(task.id(), 2L);
        mvc.perform(post("/api/tasks/{id}/issues", task.id()).header("Authorization", auth(3L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueType\":\"OTHER\",\"description\":\"非参与者\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/{id}/workspace", task.id()).header("Authorization", auth(3L))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/{id}/workspace", task.id())).andExpect(status().isUnauthorized());
        assertThat(tasks.findById(task.id()).orElseThrow().getWorkflowStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void publisherHistoryAndPrivateWorkspaceUseJwtParticipantsOnly() throws Exception {
        var task = publish("GRAB", "PHOTO_AND_CONFIRMATION");
        service.grab(task.id(), 2L);
        mvc.perform(get("/api/tasks/mine")).andExpect(status().isUnauthorized());
        String body = mvc.perform(get("/api/tasks/mine").header("Authorization", auth(1L)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode entries = json.readTree(body).path("data");
        assertThat(entries.findValuesAsText("id")).contains(task.id().toString());
        mvc.perform(get("/api/tasks/{id}/workspace", task.id()).header("Authorization", auth(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publisherContact.wechatContact").value("alice_campushub"))
                .andExpect(jsonPath("$.data.runnerContact.userId").value(2))
                .andExpect(jsonPath("$.data.events").isArray())
                .andExpect(jsonPath("$.data.completionCodeHash").doesNotExist());
        mvc.perform(get("/api/tasks/{id}", task.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publisherContact").doesNotExist())
                .andExpect(jsonPath("$.data.completionCodeHash").doesNotExist());
    }

    @Test
    void completionCodeIsIssuedOnlyToPublisherAndCheckedAgainstHash() throws Exception {
        var task = publish("GRAB", "COMPLETION_CODE");
        service.grab(task.id(), 2L);
        mvc.perform(post("/api/tasks/{id}/completion-code", task.id()).header("Authorization", auth(2L))).andExpect(status().isForbidden());
        String body = mvc.perform(post("/api/tasks/{id}/completion-code", task.id()).header("Authorization", auth(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String code = json.readTree(body).path("data").path("completionCode").asText();
        assertThat(code).matches("\\d{6}");
        assertThat(tasks.findById(task.id()).orElseThrow().getCompletionCodeHash()).isNotEqualTo(code).startsWith("$2");
        assertThatThrownBy(() -> service.completeWithCode(task.id(), 2L, new TaskActionRequest(null, code))).isInstanceOf(BusinessException.class);
        delivered(task.id());
        assertThatThrownBy(() -> service.completeWithCode(task.id(), 2L, new TaskActionRequest(null, "wrong"))).isInstanceOf(BusinessException.class);
        assertThat(tasks.findById(task.id()).orElseThrow().getWorkflowStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(service.completeWithCode(task.id(), 2L, new TaskActionRequest(null, code)).workflowStatus()).isEqualTo("COMPLETED");
        assertThat(tasks.findById(task.id()).orElseThrow().getCompletionCodeHash()).isNull();
    }

    @Test
    void wrongCompletionCodeAttemptsAreLimitedUntilPublisherRegenerates() throws Exception {
        var task = publish("GRAB", "COMPLETION_CODE");
        service.grab(task.id(), 2L); delivered(task.id());
        mvc.perform(post("/api/tasks/{id}/completion-code", task.id()).header("Authorization", auth(1L))).andExpect(status().isOk());
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/tasks/{id}/complete-code", task.id()).header("Authorization", auth(2L))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"completionCode\":\"wrong\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        }
        String body = mvc.perform(post("/api/tasks/{id}/complete-code", task.id()).header("Authorization", auth(2L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completionCode\":\"123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json.readTree(body).path("message").asText()).contains("重新生成");
        assertThat(tasks.findById(task.id()).orElseThrow().getWorkflowStatus()).isEqualTo("PENDING_CONFIRMATION");
    }
}
