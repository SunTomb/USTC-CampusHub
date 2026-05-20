# CampusHub Platform Roadmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn CampusHub from a deployed course prototype into a real campus platform by delivering Phase 1 campus errands first, then using that foundation for second-hand trading, student shops, and project ads.

**Architecture:** Phase 1 adds the reusable platform foundation that errands need: contact requirements, role deposits, zone-based location capture, task acceptance modes, task workflow events, notifications, and an operations console. Existing Spring Boot bounded-context packages remain; new code is added in focused packages (`identity`, `notification`, expanded `task`) and the current Vue pages are upgraded into responsive user flows.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, JUnit/Spring Boot Test, Vue 3, Vite, TypeScript, Pinia, Vue Router, Element Plus, Vitest.

---

## Scope and decomposition

The approved design in `docs/superpowers/specs/2026-05-20-campushub-platform-design.md` covers multiple business lines. This implementation plan is therefore a staged roadmap with detailed executable tasks for Phase 1 only.

- Phase 1 detailed scope: campus errands / running tasks, identity/contact/deposit foundation, zone-based locations, station notifications, responsive task UI, and operations console.
- Phase 2 plan to write later: second-hand trading real-operation upgrade.
- Phase 3 plan to write later: student-shop merchant and appointment upgrade.
- Phase 4 plan to write later: project ads and campus showcase upgrade.

## File structure map

### Backend migrations

- Create `backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql` — contact fields, role applications/deposits, notifications.
- Create `backend/src/main/resources/db/migration/V5__runner_task_workflow.sql` — task acceptance mode, campus zones, workflow status fields, task events, task issues.

### Backend identity/deposit package

- Create `backend/src/main/java/com/campushub/identity/PlatformRoleType.java` — enum-like constants for `RUNNER`, `GOODS_PUBLISHER`, `SHOP_MERCHANT`.
- Create `backend/src/main/java/com/campushub/identity/RoleApplication.java` — JPA entity for role applications and deposit state.
- Create `backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java` — repository with user/role lookup methods.
- Create `backend/src/main/java/com/campushub/identity/RoleApplicationSummary.java` — API DTO.
- Create `backend/src/main/java/com/campushub/identity/ApplyRoleRequest.java` — request DTO.
- Create `backend/src/main/java/com/campushub/identity/IdentityService.java` — applies role/deposit rules.
- Create `backend/src/main/java/com/campushub/identity/IdentityController.java` — user-facing role application endpoints.
- Create `backend/src/main/java/com/campushub/identity/AdminIdentityController.java` — admin review endpoints for shop merchants.
- Modify `backend/src/main/java/com/campushub/user/User.java` — add `wechatContact`, `qqContact` fields.
- Modify `backend/src/main/java/com/campushub/auth/RegisterRequest.java` — require contact fields.
- Modify `backend/src/main/java/com/campushub/auth/RegistrationService.java` — persist contact fields.
- Modify `backend/src/main/java/com/campushub/auth/CurrentUserSummary.java` and `backend/src/main/java/com/campushub/user/UserSummary.java` — expose safe contact summary where needed.

### Backend notification package

- Create `backend/src/main/java/com/campushub/notification/StationNotification.java` — JPA notification entity.
- Create `backend/src/main/java/com/campushub/notification/StationNotificationRepository.java` — recipient queries.
- Create `backend/src/main/java/com/campushub/notification/StationNotificationSummary.java` — DTO.
- Create `backend/src/main/java/com/campushub/notification/NotificationService.java` — creates notification records.
- Create `backend/src/main/java/com/campushub/notification/NotificationController.java` — list/read notifications.

### Backend task package expansion

- Modify `backend/src/main/java/com/campushub/task/RewardTask.java` — add acceptance mode, campus zones, detail text, workflow status, verification mode, selected application.
- Modify `backend/src/main/java/com/campushub/task/TaskApplication.java` — support application decision fields.
- Create `backend/src/main/java/com/campushub/task/CampusZone.java` — enum-like constants for approved zones.
- Create `backend/src/main/java/com/campushub/task/TaskAcceptanceMode.java` — `GRAB` / `APPLICATION`.
- Create `backend/src/main/java/com/campushub/task/TaskWorkflowStatus.java` — workflow state constants.
- Create `backend/src/main/java/com/campushub/task/TaskVerificationMode.java` — `COMPLETION_CODE` / `PHOTO_AND_CONFIRMATION`.
- Create `backend/src/main/java/com/campushub/task/TaskEvent.java` — immutable task event log entity.
- Create `backend/src/main/java/com/campushub/task/TaskEventRepository.java` — task event queries.
- Create `backend/src/main/java/com/campushub/task/TaskIssue.java` — abnormal/issue entity.
- Create `backend/src/main/java/com/campushub/task/TaskIssueRepository.java` — issue queries.
- Create `backend/src/main/java/com/campushub/task/CreateRunnerTaskRequest.java` — task publishing request.
- Create `backend/src/main/java/com/campushub/task/ApplyTaskRequest.java` — application request.
- Create `backend/src/main/java/com/campushub/task/TaskActionRequest.java` — generic action note/code request.
- Create `backend/src/main/java/com/campushub/task/ReportTaskIssueRequest.java` — issue request.
- Create `backend/src/main/java/com/campushub/task/RunnerTaskService.java` — validates and transitions task workflow.
- Modify `backend/src/main/java/com/campushub/task/RewardTaskController.java` — add publish/grab/apply/accept/advance/complete/issue endpoints.
- Modify `backend/src/main/java/com/campushub/task/RewardTaskSummary.java` — include mode, zones, workflow, contact visibility.
- Modify `backend/src/main/java/com/campushub/task/TaskApplicationSummary.java` — include decision status.

### Backend operations package

- Create `backend/src/main/java/com/campushub/ops/OperationsDashboardSummary.java` — aggregated metrics DTO.
- Create `backend/src/main/java/com/campushub/ops/OperationsController.java` — admin endpoints for task monitor, issues, deposits, metrics.

### Backend tests

- Create `backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/task/RunnerTaskFlowIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/notification/NotificationServiceIntegrationTest.java`.

### Frontend API and store

- Modify `frontend/src/api/campushub.ts` — add identity, notification, runner-task workflow APIs and types.
- Modify `frontend/src/stores/auth.ts` — store contact fields if returned by current user endpoint.

### Frontend pages

- Modify `frontend/src/views/AuthView.vue` — add WeChat/QQ contact fields during registration.
- Create `frontend/src/views/RoleApplicationsView.vue` — user role/deposit application page.
- Replace or heavily modify `frontend/src/views/TasksView.vue` — task hall with publish, grab/apply, filters.
- Create `frontend/src/views/TaskWorkspaceView.vue` — post-acceptance workflow page.
- Create `frontend/src/views/NotificationsView.vue` — station notification list.
- Modify `frontend/src/views/AdminReviewView.vue` or create `frontend/src/views/AdminOperationsView.vue` — operations dashboard tabs.
- Modify `frontend/src/router/index.ts` — add `/roles`, `/tasks/:id/workspace`, `/notifications`, `/admin/ops`.
- Modify `frontend/src/layouts/MainLayout.vue` — add navigation entries.
- Modify `frontend/src/styles.css` — mobile responsive layouts for task cards, publish flow, workspace.

---

## Phase 1: Errands foundation and workflow

### Task 1: Add contact fields to registration

**Files:**
- Modify: `backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql`
- Modify: `backend/src/main/java/com/campushub/user/User.java`
- Modify: `backend/src/main/java/com/campushub/auth/RegisterRequest.java`
- Modify: `backend/src/main/java/com/campushub/auth/RegistrationService.java`
- Modify: `backend/src/main/java/com/campushub/auth/CurrentUserSummary.java`
- Modify: `frontend/src/views/AuthView.vue`
- Test: `backend/src/test/java/com/campushub/auth/AuthRegistrationIntegrationTest.java`

- [x] **Step 1: Write failing backend registration test**

Add this test method to `backend/src/test/java/com/campushub/auth/AuthRegistrationIntegrationTest.java`:

```java
@Test
void registerRequiresAtLeastOneCampusContact() throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "email": "missing-contact@mail.ustc.edu.cn",
                              "password": "ChangeMe123!",
                              "emailCode": "123456"
                            }
                            """))
            .andExpect(status().isBadRequest());
}
```

- [x] **Step 2: Run the failing registration test**

Run: `mvn -f backend/pom.xml -Dtest=AuthRegistrationIntegrationTest#registerRequiresAtLeastOneCampusContact test`

Expected: FAIL because `RegisterRequest` does not require contact fields yet.

- [x] **Step 3: Add database columns**

Create `backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql` with:

```sql
ALTER TABLE users
    ADD COLUMN wechat_contact VARCHAR(120) NULL AFTER email,
    ADD COLUMN qq_contact VARCHAR(60) NULL AFTER wechat_contact;
```

- [x] **Step 4: Add fields to `User`**

In `backend/src/main/java/com/campushub/user/User.java`, add fields and getters:

```java
@Column(name = "wechat_contact")
private String wechatContact;

@Column(name = "qq_contact")
private String qqContact;

public String getWechatContact() {
    return wechatContact;
}

public String getQqContact() {
    return qqContact;
}

public void updateContact(String wechatContact, String qqContact) {
    this.wechatContact = wechatContact;
    this.qqContact = qqContact;
}
```

- [x] **Step 5: Extend registration request**

Change `backend/src/main/java/com/campushub/auth/RegisterRequest.java` to:

```java
package com.campushub.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String emailCode,
        String wechatContact,
        String qqContact) {
}
```

- [x] **Step 6: Validate and persist contact fields**

In `RegistrationService`, normalize contact fields and reject requests where both are blank:

```java
private void validateContact(RegisterRequest request) {
    boolean hasWechat = request.wechatContact() != null && !request.wechatContact().trim().isEmpty();
    boolean hasQq = request.qqContact() != null && !request.qqContact().trim().isEmpty();
    if (!hasWechat && !hasQq) {
        throw new BusinessException("请至少填写微信或 QQ 联系方式");
    }
}
```

Call `validateContact(request)` before creating the `User`, and after constructing the user call:

```java
user.updateContact(trimToNull(request.wechatContact()), trimToNull(request.qqContact()));
```

Add helper:

```java
private String trimToNull(String value) {
    if (value == null) {
        return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
}
```

- [x] **Step 7: Expose contact in current user summary**

Change `CurrentUserSummary` to include `wechatContact` and `qqContact`. Its `from(User user)` must pass `user.getWechatContact()` and `user.getQqContact()`.

- [x] **Step 8: Add frontend registration fields**

In `frontend/src/views/AuthView.vue`, add two optional inputs to the register form and require at least one before calling the API:

```ts
if (!registerForm.wechatContact && !registerForm.qqContact) {
  ElMessage.error('请至少填写微信或 QQ 联系方式')
  return
}
```

- [x] **Step 9: Verify test passes**

Run: `mvn -f backend/pom.xml -Dtest=AuthRegistrationIntegrationTest#registerRequiresAtLeastOneCampusContact test`

Expected: PASS.

- [x] **Step 10: Commit**

```bash
git add backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql backend/src/main/java/com/campushub/user/User.java backend/src/main/java/com/campushub/auth/RegisterRequest.java backend/src/main/java/com/campushub/auth/RegistrationService.java backend/src/main/java/com/campushub/auth/CurrentUserSummary.java backend/src/test/java/com/campushub/auth/AuthRegistrationIntegrationTest.java frontend/src/views/AuthView.vue
git commit -m "require campus contact during registration"
```

### Task 2: Add role application and deposit foundation

**Files:**
- Modify: `backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql`
- Create: `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`
- Create: `backend/src/main/java/com/campushub/identity/RoleApplication.java`
- Create: `backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java`
- Create: `backend/src/main/java/com/campushub/identity/RoleApplicationSummary.java`
- Create: `backend/src/main/java/com/campushub/identity/ApplyRoleRequest.java`
- Create: `backend/src/main/java/com/campushub/identity/IdentityService.java`
- Create: `backend/src/main/java/com/campushub/identity/IdentityController.java`
- Create: `backend/src/main/java/com/campushub/identity/AdminIdentityController.java`
- Test: `backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java`

- [ ] **Step 1: Extend migration with role applications**

Append to `V4__platform_identity_and_notifications.sql`:

```sql
CREATE TABLE role_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL,
    deposit_status VARCHAR(30) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    apply_note VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_app_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_app_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT uk_role_app_user_type UNIQUE (user_id, role_type)
);
```

- [ ] **Step 2: Write failing identity tests**

Create `backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java` with tests:

```java
package com.campushub.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdentityServiceIntegrationTest {

    @Autowired IdentityService identityService;
    @Autowired RoleApplicationRepository roleApplicationRepository;
    @Autowired UserRepository userRepository;

    @Test
    void runnerApplicationAutoApprovesWithFiveYuanDeposit() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        RoleApplicationSummary summary = identityService.apply(user.getId(), new ApplyRoleRequest("RUNNER", "愿意参与跑腿"));

        assertThat(summary.roleType()).isEqualTo("RUNNER");
        assertThat(summary.depositAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(summary.depositStatus()).isEqualTo("PAID");
        assertThat(summary.reviewStatus()).isEqualTo("APPROVED");
    }

    @Test
    void shopMerchantApplicationRequiresManualReviewWithTwentyYuanDeposit() {
        User user = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        RoleApplicationSummary summary = identityService.apply(user.getId(), new ApplyRoleRequest("SHOP_MERCHANT", "摄影服务"));

        assertThat(summary.depositAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(summary.depositStatus()).isEqualTo("PAID");
        assertThat(summary.reviewStatus()).isEqualTo("PENDING_REVIEW");
    }
}
```

- [ ] **Step 3: Run failing identity tests**

Run: `mvn -f backend/pom.xml -Dtest=IdentityServiceIntegrationTest test`

Expected: FAIL because identity package does not exist.

- [ ] **Step 4: Add role constants**

Create `PlatformRoleType.java`:

```java
package com.campushub.identity;

import java.math.BigDecimal;

public enum PlatformRoleType {
    RUNNER(new BigDecimal("5.00"), false),
    GOODS_PUBLISHER(new BigDecimal("10.00"), false),
    SHOP_MERCHANT(new BigDecimal("20.00"), true);

    private final BigDecimal depositAmount;
    private final boolean manualReviewRequired;

    PlatformRoleType(BigDecimal depositAmount, boolean manualReviewRequired) {
        this.depositAmount = depositAmount;
        this.manualReviewRequired = manualReviewRequired;
    }

    public BigDecimal depositAmount() {
        return depositAmount;
    }

    public boolean manualReviewRequired() {
        return manualReviewRequired;
    }
}
```

- [ ] **Step 5: Add role application entity and repository**

Create `RoleApplication.java` with fields matching the migration, `markApproved(User reviewer)`, and getters.

Create `RoleApplicationRepository.java`:

```java
package com.campushub.identity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {
    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<RoleApplication> findByUserIdAndRoleType(Long userId, String roleType);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<RoleApplication> findByReviewStatusOrderByCreatedAtAsc(String reviewStatus);
}
```

- [ ] **Step 6: Add DTOs**

Create `ApplyRoleRequest.java`:

```java
package com.campushub.identity;

import jakarta.validation.constraints.NotBlank;

public record ApplyRoleRequest(@NotBlank String roleType, String applyNote) {
}
```

Create `RoleApplicationSummary.java` with `from(RoleApplication application)` returning id, user id, user nickname, role type, deposit amount, deposit status, review status, apply note, reviewer nickname, created at, reviewed at.

- [ ] **Step 7: Add identity service**

Create `IdentityService.java` implementing:

```java
@Transactional
public RoleApplicationSummary apply(Long userId, ApplyRoleRequest request) {
    PlatformRoleType roleType = PlatformRoleType.valueOf(request.roleType());
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
    roleApplicationRepository.findByUserIdAndRoleType(userId, roleType.name()).ifPresent(existing -> {
        throw new BusinessException("该身份已申请");
    });
    String reviewStatus = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "APPROVED";
    RoleApplication application = new RoleApplication(user, roleType.name(), roleType.depositAmount(), "PAID", reviewStatus, request.applyNote());
    return RoleApplicationSummary.from(roleApplicationRepository.save(application));
}
```

- [ ] **Step 8: Add controllers**

Create `IdentityController` under `/api/identity`:

```java
@PostMapping("/users/{userId}/roles")
public ApiResponse<RoleApplicationSummary> apply(@PathVariable Long userId, @Valid @RequestBody ApplyRoleRequest request) {
    return ApiResponse.ok(identityService.apply(userId, request));
}
```

Create `AdminIdentityController` under `/api/admin/identity` with endpoints to list pending shop merchant applications and approve/reject them.

- [ ] **Step 9: Verify identity tests pass**

Run: `mvn -f backend/pom.xml -Dtest=IdentityServiceIntegrationTest test`

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql backend/src/main/java/com/campushub/identity backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java
git commit -m "add role deposit applications"
```

### Task 3: Add station notifications

**Files:**
- Modify: `backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql`
- Create: `backend/src/main/java/com/campushub/notification/StationNotification.java`
- Create: `backend/src/main/java/com/campushub/notification/StationNotificationRepository.java`
- Create: `backend/src/main/java/com/campushub/notification/StationNotificationSummary.java`
- Create: `backend/src/main/java/com/campushub/notification/NotificationService.java`
- Create: `backend/src/main/java/com/campushub/notification/NotificationController.java`
- Test: `backend/src/test/java/com/campushub/notification/NotificationServiceIntegrationTest.java`

- [ ] **Step 1: Extend migration with notifications**

Append to `V4__platform_identity_and_notifications.sql`:

```sql
CREATE TABLE station_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    target_type VARCHAR(40) NULL,
    target_id BIGINT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    INDEX idx_notification_recipient_time (recipient_id, created_at)
);
```

- [ ] **Step 2: Write failing notification test**

Create `NotificationServiceIntegrationTest.java`:

```java
@SpringBootTest
@Transactional
class NotificationServiceIntegrationTest {
    @Autowired NotificationService notificationService;
    @Autowired StationNotificationRepository repository;
    @Autowired UserRepository userRepository;

    @Test
    void createsUnreadNotificationForRecipient() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        notificationService.notify(user, "任务已被接单", "你的任务已有同学接单", "TASK", 1L);

        List<StationNotification> notifications = repository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getReadAt()).isNull();
    }
}
```

- [ ] **Step 3: Run failing notification test**

Run: `mvn -f backend/pom.xml -Dtest=NotificationServiceIntegrationTest test`

Expected: FAIL because notification package does not exist.

- [ ] **Step 4: Implement notification entity/repository/service/controller**

Create focused classes matching the migration. `NotificationService.notify(User recipient, String title, String content, String targetType, Long targetId)` saves an unread notification. `NotificationController` exposes:

```java
@GetMapping("/users/{userId}/notifications")
@PostMapping("/notifications/{id}/read")
```

- [ ] **Step 5: Verify notification test passes**

Run: `mvn -f backend/pom.xml -Dtest=NotificationServiceIntegrationTest test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V4__platform_identity_and_notifications.sql backend/src/main/java/com/campushub/notification backend/src/test/java/com/campushub/notification/NotificationServiceIntegrationTest.java
git commit -m "add station notifications"
```

### Task 4: Add runner task schema and domain types

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__runner_task_workflow.sql`
- Modify: `backend/src/main/java/com/campushub/task/RewardTask.java`
- Create: `backend/src/main/java/com/campushub/task/CampusZone.java`
- Create: `backend/src/main/java/com/campushub/task/TaskAcceptanceMode.java`
- Create: `backend/src/main/java/com/campushub/task/TaskWorkflowStatus.java`
- Create: `backend/src/main/java/com/campushub/task/TaskVerificationMode.java`
- Create: `backend/src/main/java/com/campushub/task/TaskEvent.java`
- Create: `backend/src/main/java/com/campushub/task/TaskEventRepository.java`
- Create: `backend/src/main/java/com/campushub/task/TaskIssue.java`
- Create: `backend/src/main/java/com/campushub/task/TaskIssueRepository.java`

- [ ] **Step 1: Create runner workflow migration**

Create `V5__runner_task_workflow.sql`:

```sql
ALTER TABLE reward_tasks
    ADD COLUMN acceptance_mode VARCHAR(30) NOT NULL DEFAULT 'GRAB',
    ADD COLUMN origin_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN destination_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN origin_detail VARCHAR(255) NULL,
    ADD COLUMN destination_detail VARCHAR(255) NULL,
    ADD COLUMN workflow_status VARCHAR(40) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN verification_mode VARCHAR(40) NOT NULL DEFAULT 'COMPLETION_CODE',
    ADD COLUMN completion_code_hash VARCHAR(120) NULL,
    ADD COLUMN accepted_application_id BIGINT NULL;

CREATE TABLE task_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    event_type VARCHAR(60) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_event_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_event_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_task_event_task_time (task_id, created_at)
);

CREATE TABLE task_issues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    issue_type VARCHAR(60) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    handler_id BIGINT NULL,
    handled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_issue_task FOREIGN KEY (task_id) REFERENCES reward_tasks(id),
    CONSTRAINT fk_task_issue_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_task_issue_handler FOREIGN KEY (handler_id) REFERENCES users(id),
    INDEX idx_task_issue_status_time (status, created_at)
);
```

- [ ] **Step 2: Add enum types**

Create `CampusZone.java`:

```java
package com.campushub.task;

public enum CampusZone {
    CENTRAL, WEST, EAST, NORTH, SOUTH, HIGH_TECH, ADVANCED_RESEARCH_INSTITUTE, SCIENCE_ISLAND, OTHER
}
```

Create `TaskAcceptanceMode.java`, `TaskWorkflowStatus.java`, and `TaskVerificationMode.java` with values from the spec.

- [ ] **Step 3: Extend `RewardTask`**

Add fields matching migration, getters, and methods:

```java
public void publishWorkflow(String acceptanceMode, String originZone, String destinationZone, String originDetail, String destinationDetail, String verificationMode) { ... }
public void markAccepted(TaskApplication application) { ... }
public void moveTo(String workflowStatus) { this.workflowStatus = workflowStatus; }
```

- [ ] **Step 4: Add event and issue entities**

Implement `TaskEvent` and `TaskIssue` as JPA entities with constructors for new events/issues and getters for all summary fields.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V5__runner_task_workflow.sql backend/src/main/java/com/campushub/task
git commit -m "add runner task workflow schema"
```

### Task 5: Implement runner task publishing, grab, and application flow

**Files:**
- Create: `backend/src/main/java/com/campushub/task/CreateRunnerTaskRequest.java`
- Create: `backend/src/main/java/com/campushub/task/ApplyTaskRequest.java`
- Create: `backend/src/main/java/com/campushub/task/TaskActionRequest.java`
- Create: `backend/src/main/java/com/campushub/task/RunnerTaskService.java`
- Modify: `backend/src/main/java/com/campushub/task/RewardTaskController.java`
- Modify: `backend/src/main/java/com/campushub/task/RewardTaskSummary.java`
- Test: `backend/src/test/java/com/campushub/task/RunnerTaskFlowIntegrationTest.java`

- [ ] **Step 1: Write failing flow tests**

Create `RunnerTaskFlowIntegrationTest.java` with tests for:

```java
@Test
void grabTaskImmediatelyAcceptsRunnerForGrabMode() { ... }

@Test
void applicationModeRequiresPublisherToAcceptOneApplicant() { ... }
```

The first test publishes a `GRAB` task and calls `runnerTaskService.grab(taskId, runnerId)`, expecting status `ACCEPTED`. The second publishes an `APPLICATION` task, applies as a runner, and expects status to remain `PUBLISHED` until publisher accepts.

- [ ] **Step 2: Run failing flow tests**

Run: `mvn -f backend/pom.xml -Dtest=RunnerTaskFlowIntegrationTest test`

Expected: FAIL because service and request DTOs do not exist.

- [ ] **Step 3: Add request DTOs**

Create request records with validation annotations:

```java
public record CreateRunnerTaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull BigDecimal rewardAmount,
    BigDecimal depositAmount,
    @NotBlank String acceptanceMode,
    @NotBlank String originZone,
    @NotBlank String destinationZone,
    String originDetail,
    String destinationDetail,
    @NotNull LocalDateTime deadline,
    @NotBlank String verificationMode) {}
```

Create `ApplyTaskRequest(String message)` and `TaskActionRequest(String note, String completionCode)`.

- [ ] **Step 4: Implement `RunnerTaskService`**

Implement methods:

```java
RewardTaskSummary publish(Long publisherId, CreateRunnerTaskRequest request)
RewardTaskSummary grab(Long taskId, Long runnerId)
TaskApplicationSummary apply(Long taskId, Long applicantId, ApplyTaskRequest request)
TaskApplicationSummary acceptApplication(Long taskId, Long applicationId, Long publisherId)
RewardTaskSummary advance(Long taskId, Long actorId, String nextStatus, TaskActionRequest request)
```

Each method writes a `TaskEvent` and creates station notifications for the counterparty.

- [ ] **Step 5: Add controller endpoints**

In `RewardTaskController`, add:

```java
@PostMapping
@PostMapping("/{taskId}/grab")
@PostMapping("/{taskId}/applications")
@PostMapping("/{taskId}/applications/{applicationId}/accept")
@PostMapping("/{taskId}/workflow/{nextStatus}")
```

- [ ] **Step 6: Verify flow tests pass**

Run: `mvn -f backend/pom.xml -Dtest=RunnerTaskFlowIntegrationTest test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/task backend/src/test/java/com/campushub/task/RunnerTaskFlowIntegrationTest.java
git commit -m "implement runner task acceptance flow"
```

### Task 6: Implement task completion, issues, and operations monitor

**Files:**
- Create: `backend/src/main/java/com/campushub/task/ReportTaskIssueRequest.java`
- Modify: `backend/src/main/java/com/campushub/task/RunnerTaskService.java`
- Modify: `backend/src/main/java/com/campushub/task/RewardTaskController.java`
- Create: `backend/src/main/java/com/campushub/ops/OperationsDashboardSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/OperationsController.java`
- Test: `backend/src/test/java/com/campushub/task/RunnerTaskFlowIntegrationTest.java`

- [ ] **Step 1: Add failing tests for completion and issues**

Extend `RunnerTaskFlowIntegrationTest` with:

```java
@Test
void completionCodeCompletesLowRiskTask() { ... }

@Test
void reportingIssueMovesTaskToIssueHandling() { ... }
```

- [ ] **Step 2: Run failing tests**

Run: `mvn -f backend/pom.xml -Dtest=RunnerTaskFlowIntegrationTest test`

Expected: FAIL because completion and issue methods are missing.

- [ ] **Step 3: Add issue request and service methods**

Create `ReportTaskIssueRequest(String issueType, String description)`.

Add `completeWithCode`, `submitDeliveryPhoto`, `confirmCompletion`, and `reportIssue` methods to `RunnerTaskService`.

- [ ] **Step 4: Add controller endpoints**

Add:

```java
@PostMapping("/{taskId}/complete-code")
@PostMapping("/{taskId}/confirm")
@PostMapping("/{taskId}/issues")
```

- [ ] **Step 5: Add operations controller**

Create `/api/admin/ops` endpoints:

```java
@GetMapping("/dashboard")
@GetMapping("/tasks")
@GetMapping("/task-issues")
@GetMapping("/role-applications")
```

- [ ] **Step 6: Verify tests pass**

Run: `mvn -f backend/pom.xml -Dtest=RunnerTaskFlowIntegrationTest test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/task backend/src/main/java/com/campushub/ops backend/src/test/java/com/campushub/task/RunnerTaskFlowIntegrationTest.java
git commit -m "add runner task completion and operations monitor"
```

### Task 7: Add frontend API types for identity, tasks, notifications, ops

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Test: `frontend/src/api/client.test.ts`

- [ ] **Step 1: Add API type definitions**

In `campushub.ts`, add `CampusZone`, `TaskAcceptanceMode`, `TaskWorkflowStatus`, `RoleApplicationSummary`, `StationNotificationSummary`, `CreateRunnerTaskPayload`, `ApplyTaskPayload`, and `TaskIssuePayload` types.

- [ ] **Step 2: Add API functions**

Add functions:

```ts
export function applyRole(userId: number, payload: ApplyRolePayload) { ... }
export function listNotifications(userId: number) { ... }
export function publishRunnerTask(payload: CreateRunnerTaskPayload) { ... }
export function grabRunnerTask(taskId: number, userId: number) { ... }
export function applyRunnerTask(taskId: number, userId: number, payload: ApplyTaskPayload) { ... }
export function advanceRunnerTask(taskId: number, userId: number, nextStatus: string, payload: TaskActionPayload) { ... }
export function reportRunnerTaskIssue(taskId: number, userId: number, payload: TaskIssuePayload) { ... }
export function getOpsDashboard() { ... }
```

- [ ] **Step 3: Run frontend type check/build**

Run: `npm --prefix frontend run build`

Expected: PASS with existing Element Plus chunk-size warning only.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/campushub.ts frontend/src/api/client.test.ts
git commit -m "add runner platform frontend APIs"
```

### Task 8: Add role application and notification pages

**Files:**
- Create: `frontend/src/views/RoleApplicationsView.vue`
- Create: `frontend/src/views/NotificationsView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Create role applications page**

Build a page with three cards: 跑腿接单者 5 元, 二手发布者 10 元, 店铺商家 20 元 + 人工审核. Each card calls `applyRole`.

- [ ] **Step 2: Create notifications page**

Build a notification list showing title, content, target type, created time, read status, and a mark-read action.

- [ ] **Step 3: Add routes and navigation**

Add `/roles` and `/notifications` to `router/index.ts` and `MainLayout.vue`.

- [ ] **Step 4: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/RoleApplicationsView.vue frontend/src/views/NotificationsView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add role and notification pages"
```

### Task 9: Upgrade task hall and add task workspace UI

**Files:**
- Modify: `frontend/src/views/TasksView.vue`
- Create: `frontend/src/views/TaskWorkspaceView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Upgrade task hall**

Replace the simple list with filter controls for origin zone, destination zone, type, acceptance mode, and task cards showing reward, deadline, zones, acceptance mode, verification mode, and publisher credit.

- [ ] **Step 2: Add publish task form**

Add a drawer or section that posts `CreateRunnerTaskPayload`, including origin/destination zone and detail fields.

- [ ] **Step 3: Add grab/apply actions**

Task cards should show `立即抢单` for `GRAB` and `申请接单` for `APPLICATION`.

- [ ] **Step 4: Add workspace page**

Create `TaskWorkspaceView.vue` showing current workflow step, next action buttons, issue reporting form, and contact hints.

- [ ] **Step 5: Add route**

Add `/tasks/:id/workspace` route.

- [ ] **Step 6: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/TasksView.vue frontend/src/views/TaskWorkspaceView.vue frontend/src/router/index.ts frontend/src/styles.css
git commit -m "upgrade runner task hall and workspace"
```

### Task 10: Add operations dashboard UI

**Files:**
- Create: `frontend/src/views/AdminOperationsView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Create operations dashboard**

Create tabs for dashboard metrics, task monitor, task issues, role applications, reports, and violations.

- [ ] **Step 2: Wire API calls**

Use `getOpsDashboard` and existing moderation APIs to populate tables.

- [ ] **Step 3: Add route/nav**

Add `/admin/ops` route and menu item.

- [ ] **Step 4: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/AdminOperationsView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add operations dashboard UI"
```

### Task 11: End-to-end verification and deployment checkpoint

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README with real-platform roadmap**

Add a section documenting Phase 1 errands, role deposits, station notifications, campus zones, and operational boundaries.

- [ ] **Step 2: Run backend tests**

Run: `mvn -f backend/pom.xml test`

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run: `npm --prefix frontend run build`

Expected: PASS with the known chunk-size warning only.

- [ ] **Step 4: Commit docs**

```bash
git add README.md
git commit -m "document CampusHub platform roadmap"
```

- [ ] **Step 5: Push**

Run: `git push origin master`.

- [ ] **Step 6: Server deploy**

On the production server, run a low-impact deploy:

```bash
cd /opt/campushub
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build campushub-backend campushub-web
```

- [ ] **Step 7: Server API smoke tests**

Run server-local checks:

```bash
curl -sS http://127.0.0.1:18080/api/tasks
curl -sS http://127.0.0.1:18080/api/wallet/users/1
curl -sS http://127.0.0.1:18080/api/notifications/users/1
curl -sS http://127.0.0.1:18080/api/admin/ops/dashboard
```

Expected: JSON responses with `success: true` or authenticated/authorization errors if auth is tightened later; no HTTP 500.

- [ ] **Step 8: Browser verification**

Open `https://ustc.suntomb.qzz.io` and verify: registration contact fields, role application page, task hall, publish task, grab/apply actions, task workspace, notifications page, and admin operations page render without white screens.

## Future plans to write after Phase 1

### Phase 2: Second-hand trading upgrade

Write a separate plan covering goods publisher role enforcement, 10 CNY deposit gate, seller contact visibility, product detail redesign, image attachments, reporting, evaluation, and service-fee hooks.

### Phase 3: Student shop upgrade

Write a separate plan covering shop merchant manual review, 20 CNY deposit gate, store homepage, service items, availability, booking workflow, merchant dashboard, ratings, and reports.

### Phase 4: Project ads and campus showcase

Write a separate plan covering project posts, review, contact visibility, expiration, tags, favorites, comments, featured slots, and portfolio pages.

## Self-review

- Spec coverage: Phase 1 covers errands, contact requirements, role deposits, no per-order escrow, campus-zone-first location collection, grab/application modes, completion/issue workflow, station notifications, operations dashboard, mobile-responsive web, and staged opening. Phase 2-4 are intentionally deferred into separate future plans because they are independent subsystems.
- Placeholder scan: No TBD/TODO placeholders are used. Where service-fee pricing is intentionally undecided, the plan requires configurable fee strategy rather than a missing value.
- Type consistency: Type and method names are consistent across backend DTOs, services, controllers, and frontend API function names.
