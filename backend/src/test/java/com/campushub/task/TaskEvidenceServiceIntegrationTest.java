package com.campushub.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.file.TaskDeliveryPhotoRepository;
import com.campushub.file.TaskDeliveryPhotoSummary;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TaskEvidenceServiceIntegrationTest {

    private static final Path FILE_ROOT = createTempRoot();

    @Autowired
    private RunnerTaskService runnerTaskService;

    @Autowired
    private TaskEvidenceService taskEvidenceService;

    @Autowired
    private TaskDeliveryPhotoRepository taskDeliveryPhotoRepository;

    @Autowired
    private TaskEventRepository taskEventRepository;

    @DynamicPropertySource
    static void fileProperties(DynamicPropertyRegistry registry) {
        registry.add("campushub.file-storage.root", () -> FILE_ROOT.toString());
    }

    @Test
    void publicSummaryRedactsPrivatePickupDataAndWorkspaceExposesEvidenceOnlyToParticipants() throws Exception {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);

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
    void downloadReturnsExactBytesToParticipantsButNotStrangers() {
        RewardTaskSummary published = runnerTaskService.publish(1L, request("PHOTO_AND_CONFIRMATION"));
        runnerTaskService.grab(published.id(), 2L);
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

        assertThatThrownBy(() -> taskEvidenceService.upload(
                published.id(), 2L,
                new MockMultipartFile("file", "proof.txt", "text/plain", "not image".getBytes()),
                "bad-type", null))
                .hasMessageContaining("图片格式");
        assertThat(taskDeliveryPhotoRepository.countByTaskId(published.id())).isZero();
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
                new BigDecimal("1.5"),
                "公开备注",
                "PICK-42",
                "私密取件说明");
    }

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02};
    }

    private static Path createTempRoot() {
        try {
            return Files.createTempDirectory("campushub-task-evidence");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
