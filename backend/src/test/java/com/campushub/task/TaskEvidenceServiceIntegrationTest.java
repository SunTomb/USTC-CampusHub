package com.campushub.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.file.TaskDeliveryPhotoRepository;
import com.campushub.file.TaskDeliveryPhotoSummary;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TaskEvidenceServiceIntegrationTest {

    private static final Path FILE_ROOT = createTempRoot();

    @Autowired com.campushub.auth.UserRoleService roles;

    @org.junit.jupiter.api.BeforeEach
    void assignRunner() { roles.assignRole(2L, "ROLE_RUNNER"); }

    @Autowired
    private RunnerTaskService runnerTaskService;

    @Autowired
    private TaskEvidenceService taskEvidenceService;

    @Autowired
    private TaskDeliveryPhotoRepository taskDeliveryPhotoRepository;

    @Autowired
    private RewardTaskRepository rewardTaskRepository;

    @Autowired
    private TaskEventRepository taskEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired private org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired private com.campushub.auth.JwtTokenService tokens;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;

    private String bearer(long id) { return "Bearer " + tokens.issueToken(id, "test-participant"); }

    @Test
    void publicHttpContractRequiresIntegerGramsAndHidesPrivateFields() throws Exception {
        var input = json.valueToTree(request("COMPLETION_CODE"));
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) input;
        body.remove("description");
        for (var invalid : new String[]{"null", "0", "100001", "1.25"}) {
            body.set("weightGrams", json.readTree(invalid));
            var response = mvc.perform(post("/api/tasks").header("Authorization", bearer(1)).contentType("application/json").content(body.toString())).andReturn().getResponse();
            assertThat(response.getStatus() == 400 || !json.readTree(response.getContentAsString()).path("success").asBoolean(true)).isTrue();
        }
        body.put("weightGrams", 1500);
        var published = mvc.perform(post("/api/tasks").header("Authorization", bearer(1)).contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value(""))
                .andExpect(jsonPath("$.data.weightGrams").value(1500)).andReturn();
        long id = json.readTree(published.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        TaskDeliveryPhotoSummary photo = taskEvidenceService.uploadRequest(id, 1L, new MockMultipartFile("file", "item.png", "image/png", png()), "photo");
        for (String path : new String[]{"/api/tasks", "/api/tasks/" + id}) {
            String result = mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(result).contains("weightGrams", "notes").doesNotContain("PICK-42", "pickupCode", "evidence", "sha256", "storageKey");
        }
        String url = photo.downloadPath();
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization", bearer(3))).andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization", bearer(1))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes(png()));
        runnerTaskService.grab(id, 2L);
        mvc.perform(get(url).header("Authorization", bearer(2))).andExpect(status().isOk()).andExpect(content().bytes(png()));
        mvc.perform(multipart("/api/tasks/{id}/request-images", id).file(new MockMultipartFile("file", "i.png", "image/png", png()))
                .param("clientRequestId", "spoof").param("publisherId", "1").header("Authorization", bearer(2))).andExpect(status().isForbidden());
    }

    @Test
    void combinedTaskPhotoQuotaAppliesAcrossBothEvidenceTypes() {
        Long id = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION")).id();
        byte[] fourMiB = new byte[4 * 1024 * 1024];
        fourMiB[0] = (byte) 0x89;
        fourMiB[1] = 0x50;
        fourMiB[2] = 0x4e;
        fourMiB[3] = 0x47;
        fourMiB[4] = 0x0d;
        fourMiB[5] = 0x0a;
        fourMiB[6] = 0x1a;
        fourMiB[7] = 0x0a;
        var large = new MockMultipartFile("file", "large.png", "image/png", fourMiB);
        taskEvidenceService.uploadRequest(id, 1L, large, "quota-1");
        taskEvidenceService.uploadRequest(id, 1L, large, "quota-2");
        runnerTaskService.grab(id, 2L);
        advanceToDelivering(id);
        taskEvidenceService.upload(id, 2L, large, "delivery-quota", null);
        assertThatThrownBy(() -> taskEvidenceService.uploadRequest(id, 1L, large, "over-quota"))
                .hasMessageContaining("15 MiB");
    }


    static void fileProperties(DynamicPropertyRegistry registry) {
        registry.add("campushub.file-storage.root", () -> FILE_ROOT.toString());
    }

    @Test
    void publicSummaryRedactsPrivatePickupDataAndWorkspaceExposesEvidenceOnlyToParticipants() throws Exception {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());

        TaskDeliveryPhotoSummary photo = taskEvidenceService.upload(
                published.id(),
                2L,
                new MockMultipartFile("file", "proof.png", "image/png", png()),
                "client-1",
                "private delivery note");

        assertThat(photo.downloadPath()).isEqualTo("/api/tasks/" + published.id() + "/evidence/" + photo.id());
        assertThat(RewardTaskSummary.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("pickupCode", "privatePickupNotes", "evidence");
        TaskWorkspaceSummary workspace = runnerTaskService.workspace(published.id(), 2L);
        assertThat(workspace.privateDetails().pickupCode()).isEqualTo("PICK-42");
        assertThat(workspace.evidence()).extracting(TaskDeliveryPhotoSummary::id).containsExactly(photo.id());
        assertThatThrownBy(() -> runnerTaskService.workspace(published.id(), 3L))
                .hasMessageContaining("只有任务参与方");
    }

    @Test
    void uploadIsIdempotentAndPhotoSubmissionRequiresEvidence() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());

        assertThatThrownBy(() -> runnerTaskService.submitDeliveryPhoto(
                published.id(), 2L, new TaskActionRequest("empty", null)))
                .hasMessageContaining("至少上传一张图片");

        TaskDeliveryPhotoSummary first = taskEvidenceService.upload(
                published.id(), 2L,
                new MockMultipartFile("file", "proof.png", "image/png", png()),
                "same-client-key", null);
        TaskDeliveryPhotoSummary duplicate = taskEvidenceService.upload(
                published.id(), 2L,
                new MockMultipartFile("file", "different.png", "image/png", png()),
                "same-client-key", null);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(taskDeliveryPhotoRepository.countByTaskId(published.id())).isEqualTo(1);
        RewardTaskSummary submitted = runnerTaskService.submitDeliveryPhoto(
                published.id(), 2L, new TaskActionRequest("delivered", null));
        assertThat(submitted.workflowStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(taskEventRepository.findByTaskIdOrderByCreatedAtAsc(published.id()))
                .extracting(TaskEvent::getEventType)
                .contains("PHOTO_UPLOADED", "PHOTO_SUBMITTED");
    }

    @Test
    void photoTaskCannotBeCompletedThroughGenericAdvance() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());

        assertThatThrownBy(() -> runnerTaskService.advance(
                published.id(), 2L, "COMPLETED", null))
                .hasMessageContaining("不可推进");

        taskEvidenceService.upload(published.id(), 2L,
                new MockMultipartFile("file", "proof.png", "image/png", png()),
                "advance-guard", null);
        runnerTaskService.submitDeliveryPhoto(
                published.id(), 2L, new TaskActionRequest("submitted", null));
        assertThatThrownBy(() -> runnerTaskService.advance(
                published.id(), 2L, "COMPLETED", null))
                .hasMessageContaining("不可推进");

        RewardTaskSummary completed = runnerTaskService.confirmCompletion(published.id(), 1L, null);
        assertThat(completed.workflowStatus()).isEqualTo("COMPLETED");
        assertThatThrownBy(() -> runnerTaskService.submitDeliveryPhoto(
                published.id(), 2L, new TaskActionRequest("reopen", null)))
                .hasMessageContaining("不可推进");
        assertThatThrownBy(() -> runnerTaskService.advance(
                published.id(), 2L, "DELIVERING", null))
                .hasMessageContaining("不可推进");
    }

    @Test
    void cancelledPhotoTaskCannotSubmitOrAdvanceAgain() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        RewardTask task = rewardTaskRepository.findById(published.id()).orElseThrow();
        task.moveTo(TaskWorkflowStatus.CANCELLED.name());
        rewardTaskRepository.saveAndFlush(task);

        assertThatThrownBy(() -> runnerTaskService.submitDeliveryPhoto(
                published.id(), 2L, new TaskActionRequest("reopen", null)))
                .hasMessageContaining("不可推进");
        assertThatThrownBy(() -> runnerTaskService.advance(
                published.id(), 2L, "DELIVERING", null))
                .hasMessageContaining("不可推进");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transactionRollbackRemovesFinalFileAfterMetadataWasFlushed() throws Exception {
        long before = countStoredFiles();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
            runnerTaskService.grab(published.id(), 2L);
            advanceToDelivering(published.id());
            taskEvidenceService.upload(published.id(), 2L,
                    new MockMultipartFile("file", "rollback.png", "image/png", png()),
                    "rollback-key", null);
            status.setRollbackOnly();
        });

        assertThat(countStoredFiles()).isEqualTo(before);
    }

    @Test
    void downloadReturnsExactBytesToParticipantsButNotStrangers() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());
        byte[] bytes = png();
        TaskDeliveryPhotoSummary photo = taskEvidenceService.upload(
                published.id(), 2L,
                new MockMultipartFile("file", "proof.png", "image/png", bytes),
                "download-key", null);

        assertThat(taskEvidenceService.download(published.id(), photo.id(), 1L).bytes())
                .containsExactly(bytes);
        assertThat(taskEvidenceService.download(published.id(), photo.id(), 2L).bytes())
                .containsExactly(bytes);
        assertThatThrownBy(() -> taskEvidenceService.download(published.id(), photo.id(), 3L))
                .hasMessageContaining("只有任务参与方");
    }

    @Test
    void enforcesThreePhotoLimitAndRejectsUploadAfterCompletion() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());
        for (int i = 0; i < 3; i++) {
            taskEvidenceService.upload(published.id(), 2L,
                    new MockMultipartFile("file", "proof-" + i + ".png", "image/png", png()),
                    "limit-" + i, null);
        }
        assertThatThrownBy(() -> taskEvidenceService.upload(published.id(), 2L,
                new MockMultipartFile("file", "proof-3.png", "image/png", png()),
                "limit-3", null)).hasMessageContaining("最多上传 3 张");

        runnerTaskService.submitDeliveryPhoto(published.id(), 2L, new TaskActionRequest("done", null));
        runnerTaskService.confirmCompletion(published.id(), 1L, null);
        assertThatThrownBy(() -> taskEvidenceService.upload(published.id(), 2L,
                new MockMultipartFile("file", "late.png", "image/png", png()),
                "late", null)).hasMessageContaining("不能上传凭证");
    }
    @Test
    void rejectsInvalidTypeAndDoesNotLeaveStorageMetadata() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
        advanceToDelivering(published.id());

        assertThatThrownBy(() -> taskEvidenceService.upload(
                published.id(), 2L,
                new MockMultipartFile("file", "proof.txt", "text/plain", "not image".getBytes()),
                "bad-type", null))
                .hasMessageContaining("图片格式");
        assertThat(taskDeliveryPhotoRepository.countByTaskId(published.id())).isZero();
    }

    @Test
    void publisherPhotosArePrivateAndNeverCountAsDeliveryProof() {
        Long id = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION")).id();
        var file = new MockMultipartFile("file", "item.png", "image/png", png());
        var first = taskEvidenceService.uploadRequest(id, 1L, file, "item-1");
        assertThat(first.evidenceType()).isEqualTo("REQUEST_IMAGE");
        assertThatThrownBy(() -> taskEvidenceService.download(id, first.id(), 2L)).hasMessageContaining("403");
        runnerTaskService.grab(id, 2L);
        assertThat(taskEvidenceService.uploadRequest(id, 1L, file, "item-1").id()).isEqualTo(first.id());
        taskEvidenceService.uploadRequest(id, 1L, file, "item-2");
        taskEvidenceService.uploadRequest(id, 1L, file, "item-3");
        assertThatThrownBy(() -> taskEvidenceService.uploadRequest(id, 1L, file, "item-4")).hasMessageContaining("3 张");
        advanceToDelivering(id);
        assertThatThrownBy(() -> runnerTaskService.submitDeliveryPhoto(id, 2L, null)).hasMessageContaining("至少上传一张");
        assertThatThrownBy(() -> taskEvidenceService.remove(id, first.id(), 2L)).hasMessageContaining("403");
        taskEvidenceService.remove(id, first.id(), 1L);
        assertThatThrownBy(() -> taskEvidenceService.download(id, first.id(), 1L)).hasMessageContaining("404");
        var proof = taskEvidenceService.upload(id, 2L, file, "proof", null);
        assertThat(proof.evidenceType()).isEqualTo("DELIVERY_PHOTO");
        taskEvidenceService.remove(id, proof.id(), 2L);
        assertThatThrownBy(() -> runnerTaskService.submitDeliveryPhoto(id, 2L, null)).hasMessageContaining("至少上传一张");
        proof = taskEvidenceService.upload(id, 2L, file, "proof-2", null);
        runnerTaskService.submitDeliveryPhoto(id, 2L, null);
        Long submitted = proof.id();
        assertThatThrownBy(() -> taskEvidenceService.remove(id, submitted, 2L)).hasMessageContaining("不能上传凭证");
        runnerTaskService.confirmCompletion(id, 1L, null);
        assertThat(taskEvidenceService.download(id, submitted, 2L).bytes()).containsExactly(png());
    }

    @Test
    void codeModeAllowsPublisherImagesButNotRunnerPhotos() {
        Long id = runnerTaskService.publish(1L, request("COMPLETION_CODE")).id();
        var file = new MockMultipartFile("file", "item.png", "image/png", png());
        var image = taskEvidenceService.uploadRequest(id, 1L, file, "item");
        runnerTaskService.grab(id, 2L);
        advanceToDelivering(id);
        assertThat(taskEvidenceService.download(id, image.id(), 2L).bytes()).containsExactly(png());
        assertThatThrownBy(() -> taskEvidenceService.upload(id, 2L, file, "runner", null)).hasMessageContaining("不能上传凭证");
    }

    private void advanceToDelivering(Long taskId) {
        runnerTaskService.advance(taskId, 2L, "HEADING_TO_PICKUP", null);
        runnerTaskService.advance(taskId, 2L, "PICKED_UP", null);
        runnerTaskService.advance(taskId, 2L, "DELIVERING", null);
    }

    private static CreateRunnerTaskRequest request(String verificationMode) {
        return new CreateRunnerTaskRequest(
                "送达证明任务",
                "请完成校园内取送",
                new BigDecimal("8.00"),
                BigDecimal.ZERO,
                "GRAB",
                "WEST",
                "CENTRAL",
                "西区",
                "中区",
                LocalDateTime.now().plusHours(4),
                verificationMode,
                1500,
                "公开备注",
                "PICK-42");
    }

    private static byte[] png() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static long countStoredFiles() throws Exception {
        if (!Files.exists(FILE_ROOT)) {
            return 0;
        }
        try (var paths = Files.walk(FILE_ROOT)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private static Path createTempRoot() {
        try {
            return Files.createTempDirectory("campushub-task-evidence");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
