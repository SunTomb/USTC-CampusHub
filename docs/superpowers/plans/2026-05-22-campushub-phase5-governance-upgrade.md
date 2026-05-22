# CampusHub Phase 5 Governance and Credit Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build a cross-business governance, credit, restriction, audit, and notification loop for CampusHub after the deployed Phase 1-4 business lines.

**Architecture:** Phase 5 extends the existing `moderation`, `notification`, `user`, and frontend API/view patterns rather than creating separate governance flows per business line. Database changes are isolated in V10, current `users.credit_score` remains the current-score source, and immutable credit/admin-action records provide history. Admin workflow is centralized under `/api/admin/governance`; user-facing credit history is exposed under `/api/credit`.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, Vue 3, Vite, TypeScript, Vue Router, Element Plus, Docker Compose, Playwriter browser verification.

---

## Scope and constraints

- Do not edit production-applied migrations V1-V9. Add `V10__governance_credit_upgrade.sql` and matching test migration only.
- Do not read, print, copy, or commit secrets, `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Keep CampusHub payment boundary unchanged: no transaction-principal escrow, no per-order deposit freeze, no direct Alipay key handling.
- Do not perform a full JWT/RBAC rewrite in this phase. Existing prototype `adminId` and `userId` request parameters remain acceptable until Phase 9.
- Do not install local dependencies for verification. If local tooling is missing, verify through code review first and use low-impact server Docker build/API smoke after push/deploy approval.
- The user approved continuing on `master`; do not create a separate worktree for this phase unless the user changes direction.

## File structure map

### Backend migration and tests

- Create `backend/src/main/resources/db/migration/V10__governance_credit_upgrade.sql` — report workflow fields, violation fields, credit adjustment table, user restriction table, admin action log table.
- Create `backend/src/test/resources/db/test-migration/V10__governance_credit_upgrade.sql` — H2-compatible equivalent if needed for backend tests.
- Create `backend/src/test/java/com/campushub/moderation/GovernanceServiceIntegrationTest.java` — report workflow, violation creation, credit adjustment, restriction, notification, audit.

### Backend moderation/governance

- Modify `backend/src/main/java/com/campushub/moderation/ReportRecord.java` — status workflow fields and transition methods.
- Modify `backend/src/main/java/com/campushub/moderation/ReportRecordRepository.java` — status/target filters with reporter/handler fetch.
- Modify `backend/src/main/java/com/campushub/moderation/ReportRecordSummary.java` — expose new workflow fields.
- Modify `backend/src/main/java/com/campushub/moderation/ViolationRecord.java` — severity, penalty, target, admin, deposit note.
- Modify `backend/src/main/java/com/campushub/moderation/ViolationRecordRepository.java` — user and report queries with entity graph.
- Modify `backend/src/main/java/com/campushub/moderation/ViolationRecordSummary.java` — expose new violation fields.
- Create `backend/src/main/java/com/campushub/moderation/GovernanceActionRequest.java` — report handling request.
- Create `backend/src/main/java/com/campushub/moderation/CreateViolationRequest.java` — violation creation request.
- Create `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRequest.java` — manual credit adjustment request.
- Create `backend/src/main/java/com/campushub/moderation/UserRestrictionRequest.java` — user restriction request.
- Create `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecord.java` — immutable credit history entity.
- Create `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecordRepository.java` — user history queries.
- Create `backend/src/main/java/com/campushub/moderation/CreditAdjustmentSummary.java` — credit history DTO.
- Create `backend/src/main/java/com/campushub/moderation/UserRestriction.java` — user restriction entity.
- Create `backend/src/main/java/com/campushub/moderation/UserRestrictionRepository.java` — active restriction queries.
- Create `backend/src/main/java/com/campushub/moderation/UserRestrictionSummary.java` — restriction DTO.
- Create `backend/src/main/java/com/campushub/moderation/AdminActionLog.java` — append-only admin governance audit entity.
- Create `backend/src/main/java/com/campushub/moderation/AdminActionLogRepository.java` — latest action queries.
- Create `backend/src/main/java/com/campushub/moderation/AdminActionLogSummary.java` — admin audit DTO.
- Create `backend/src/main/java/com/campushub/moderation/GovernanceDashboardSummary.java` — top metrics DTO.
- Create `backend/src/main/java/com/campushub/moderation/CreditCenterSummary.java` — user-facing credit center aggregate.
- Create `backend/src/main/java/com/campushub/moderation/GovernanceService.java` — report workflow, violation, credit, restriction, audit, notifications.
- Create `backend/src/main/java/com/campushub/moderation/AdminGovernanceController.java` — `/api/admin/governance` endpoints.
- Create `backend/src/main/java/com/campushub/moderation/CreditController.java` — `/api/credit` endpoints.
- Modify `backend/src/main/java/com/campushub/moderation/ModerationService.java` — create reports with `OPEN` status.

### Backend business flow restriction checks

- Modify `backend/src/main/java/com/campushub/task/RunnerTaskService.java` — block posting/runner actions for active restrictions.
- Modify `backend/src/main/java/com/campushub/goods/GoodsService.java` if present, otherwise modify current goods controller/service path — block goods publishing for active posting freeze.
- Modify `backend/src/main/java/com/campushub/shop/ShopService.java` — block shop/service/order provider actions for active posting/service freeze.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdService.java` — block project ad create/submit for active posting freeze.

### Frontend API and views

- Modify `frontend/src/api/campushub.ts` — governance/credit types and API functions.
- Create `frontend/src/views/AdminGovernanceView.vue` — admin governance workspace.
- Create `frontend/src/views/CreditCenterView.vue` — user credit center.
- Modify `frontend/src/router/index.ts` — add `/admin/governance` and `/credit`.
- Modify `frontend/src/layouts/MainLayout.vue` — add navigation entries.
- Modify `frontend/src/styles.css` — governance and credit responsive styles.

### Documentation

- Modify `README.md` — document Phase 5 governance and payment boundary.
- Modify `CLAUDE.md` — add Phase 5 handoff after implementation/deployment verification.

---

## Task 1: Add V10 governance schema

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__governance_credit_upgrade.sql`
- Create: `backend/src/test/resources/db/test-migration/V10__governance_credit_upgrade.sql`

- [x] **Step 1: Create production Flyway migration**

Create `backend/src/main/resources/db/migration/V10__governance_credit_upgrade.sql`:

```sql
UPDATE report_records
SET status = 'OPEN'
WHERE status = 'PENDING';

ALTER TABLE report_records
    ADD COLUMN review_note VARCHAR(1000) NULL AFTER status,
    ADD COLUMN resolution_type VARCHAR(60) NULL AFTER review_note,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE violation_records
    ADD COLUMN severity VARCHAR(30) NOT NULL DEFAULT 'LOW' AFTER violation_type,
    ADD COLUMN penalty_type VARCHAR(40) NOT NULL DEFAULT 'CREDIT_ONLY' AFTER severity,
    ADD COLUMN target_type VARCHAR(40) NULL AFTER report_id,
    ADD COLUMN target_id BIGINT NULL AFTER target_type,
    ADD COLUMN admin_id BIGINT NULL AFTER credit_delta,
    ADD COLUMN deposit_impact_note VARCHAR(500) NULL AFTER admin_id,
    ADD CONSTRAINT fk_violation_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    ADD INDEX idx_violation_user_time (user_id, created_at),
    ADD INDEX idx_violation_target (target_type, target_id);

CREATE TABLE credit_adjustment_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    violation_id BIGINT NULL,
    before_score INT NOT NULL,
    delta_score INT NOT NULL,
    after_score INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credit_adjust_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_credit_adjust_violation FOREIGN KEY (violation_id) REFERENCES violation_records(id),
    CONSTRAINT fk_credit_adjust_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_credit_adjust_user_time (user_id, created_at)
);

CREATE TABLE user_restrictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    violation_id BIGINT NULL,
    restriction_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    starts_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at DATETIME NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    admin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_restriction_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_restriction_violation FOREIGN KEY (violation_id) REFERENCES violation_records(id),
    CONSTRAINT fk_user_restriction_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_user_restriction_user_active (user_id, active, restriction_type),
    INDEX idx_user_restriction_time (created_at)
);

CREATE TABLE admin_action_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NULL,
    action_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_action_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_admin_action_target (target_type, target_id),
    INDEX idx_admin_action_time (created_at)
);
```

- [x] **Step 2: Create H2-compatible test migration**

Create `backend/src/test/resources/db/test-migration/V10__governance_credit_upgrade.sql` with equivalent DDL. If H2 rejects MySQL `AFTER` or `ON UPDATE`, use this content:

```sql
UPDATE report_records
SET status = 'OPEN'
WHERE status = 'PENDING';

ALTER TABLE report_records ADD COLUMN review_note VARCHAR(1000);
ALTER TABLE report_records ADD COLUMN resolution_type VARCHAR(60);
ALTER TABLE report_records ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

ALTER TABLE violation_records ADD COLUMN severity VARCHAR(30) DEFAULT 'LOW' NOT NULL;
ALTER TABLE violation_records ADD COLUMN penalty_type VARCHAR(40) DEFAULT 'CREDIT_ONLY' NOT NULL;
ALTER TABLE violation_records ADD COLUMN target_type VARCHAR(40);
ALTER TABLE violation_records ADD COLUMN target_id BIGINT;
ALTER TABLE violation_records ADD COLUMN admin_id BIGINT;
ALTER TABLE violation_records ADD COLUMN deposit_impact_note VARCHAR(500);

CREATE TABLE credit_adjustment_records (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    violation_id BIGINT,
    before_score INT NOT NULL,
    delta_score INT NOT NULL,
    after_score INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_restrictions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    violation_id BIGINT,
    restriction_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    starts_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    admin_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE admin_action_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    admin_id BIGINT,
    action_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NOT NULL,
    note VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

- [x] **Step 3: Commit schema task**

```bash
git add backend/src/main/resources/db/migration/V10__governance_credit_upgrade.sql backend/src/test/resources/db/test-migration/V10__governance_credit_upgrade.sql
git commit -m "add governance credit schema"
```

## Task 2: Extend report and violation domain models

**Files:**
- Modify: `backend/src/main/java/com/campushub/moderation/ReportRecord.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ReportRecordRepository.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ReportRecordSummary.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ViolationRecord.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ViolationRecordRepository.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ViolationRecordSummary.java`

- [x] **Step 1: Update `ReportRecord` fields and transitions**

Add fields and methods to `ReportRecord`:

```java
@Column(name = "review_note")
private String reviewNote;

@Column(name = "resolution_type")
private String resolutionType;

@Column(name = "updated_at", insertable = false, updatable = false)
private LocalDateTime updatedAt;

public void startReview(User handler, String note) {
    this.handler = handler;
    this.status = "IN_REVIEW";
    this.reviewNote = note;
}

public void reject(User handler, String note) {
    this.handler = handler;
    this.status = "REJECTED";
    this.reviewNote = note;
    this.resolutionType = "NO_ACTION";
    this.handledAt = LocalDateTime.now();
}

public void resolve(User handler, String resolutionType, String note) {
    this.handler = handler;
    this.status = "RESOLVED";
    this.resolutionType = resolutionType;
    this.reviewNote = note;
    this.handledAt = LocalDateTime.now();
}

public void escalate(User handler, String note) {
    this.handler = handler;
    this.status = "ESCALATED";
    this.resolutionType = "ESCALATED";
    this.reviewNote = note;
    this.handledAt = LocalDateTime.now();
}
```

Change the constructor default from `PENDING` to `OPEN`:

```java
this.status = "OPEN";
```

Add getters for `reviewNote`, `resolutionType`, and `updatedAt`.

- [x] **Step 2: Update `ReportRecordRepository`**

Add entity graph queries:

```java
@EntityGraph(attributePaths = {"reporter", "handler"})
List<ReportRecord> findByStatusOrderByCreatedAtAsc(String status);

@EntityGraph(attributePaths = {"reporter", "handler"})
List<ReportRecord> findByTargetTypeOrderByCreatedAtDesc(String targetType);

@EntityGraph(attributePaths = {"reporter", "handler"})
List<ReportRecord> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

@EntityGraph(attributePaths = {"reporter", "handler"})
Optional<ReportRecord> findWithReporterAndHandlerById(Long id);
```

If Spring Data cannot derive `findWithReporterAndHandlerById`, use this instead:

```java
@EntityGraph(attributePaths = {"reporter", "handler"})
Optional<ReportRecord> findById(Long id);
```

- [x] **Step 3: Update `ReportRecordSummary`**

Extend the record to include:

```java
String reviewNote,
String resolutionType,
String handlerNickname,
LocalDateTime handledAt,
LocalDateTime updatedAt
```

Ensure `from(ReportRecord report)` maps nullable handler safely:

```java
String handlerNickname = report.getHandler() == null ? null : report.getHandler().getNickname();
```

- [x] **Step 4: Update `ViolationRecord`**

Add fields:

```java
@Column(nullable = false)
private String severity;

@Column(name = "penalty_type", nullable = false)
private String penaltyType;

@Column(name = "target_type")
private String targetType;

@Column(name = "target_id")
private Long targetId;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "admin_id")
private User admin;

@Column(name = "deposit_impact_note")
private String depositImpactNote;
```

Add a constructor:

```java
public ViolationRecord(
        User user,
        ReportRecord report,
        String targetType,
        Long targetId,
        String violationType,
        String severity,
        String penaltyType,
        String description,
        Integer creditDelta,
        User admin,
        String depositImpactNote) {
    this.user = user;
    this.report = report;
    this.targetType = targetType;
    this.targetId = targetId;
    this.violationType = violationType;
    this.severity = severity;
    this.penaltyType = penaltyType;
    this.description = description;
    this.creditDelta = creditDelta;
    this.admin = admin;
    this.depositImpactNote = depositImpactNote;
}
```

Add getters for all new fields.

- [x] **Step 5: Update violation repository and summary**

Add repository methods:

```java
@EntityGraph(attributePaths = {"user", "report", "admin"})
List<ViolationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

@EntityGraph(attributePaths = {"user", "report", "admin"})
List<ViolationRecord> findBySeverityOrderByCreatedAtDesc(String severity);
```

Extend `ViolationRecordSummary` with severity, penalty type, target type/id, admin nickname, and deposit impact note.

- [x] **Step 6: Commit domain model task**

```bash
git add backend/src/main/java/com/campushub/moderation/ReportRecord.java backend/src/main/java/com/campushub/moderation/ReportRecordRepository.java backend/src/main/java/com/campushub/moderation/ReportRecordSummary.java backend/src/main/java/com/campushub/moderation/ViolationRecord.java backend/src/main/java/com/campushub/moderation/ViolationRecordRepository.java backend/src/main/java/com/campushub/moderation/ViolationRecordSummary.java
git commit -m "extend moderation governance models"
```

## Task 3: Add credit, restriction, and admin audit entities

**Files:**
- Create: `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecord.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecordRepository.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreditAdjustmentSummary.java`
- Create: `backend/src/main/java/com/campushub/moderation/UserRestriction.java`
- Create: `backend/src/main/java/com/campushub/moderation/UserRestrictionRepository.java`
- Create: `backend/src/main/java/com/campushub/moderation/UserRestrictionSummary.java`
- Create: `backend/src/main/java/com/campushub/moderation/AdminActionLog.java`
- Create: `backend/src/main/java/com/campushub/moderation/AdminActionLogRepository.java`
- Create: `backend/src/main/java/com/campushub/moderation/AdminActionLogSummary.java`

- [x] **Step 1: Create credit adjustment entity and repository**

Create `CreditAdjustmentRecord` with fields matching V10 and a constructor:

```java
public CreditAdjustmentRecord(User user, ViolationRecord violation, Integer beforeScore, Integer deltaScore, Integer afterScore, String reason, User admin) {
    this.user = user;
    this.violation = violation;
    this.beforeScore = beforeScore;
    this.deltaScore = deltaScore;
    this.afterScore = afterScore;
    this.reason = reason;
    this.admin = admin;
}
```

Create repository:

```java
public interface CreditAdjustmentRecordRepository extends JpaRepository<CreditAdjustmentRecord, Long> {
    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<CreditAdjustmentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

Create `CreditAdjustmentSummary.from(CreditAdjustmentRecord record)` with id, user id, user nickname, violation id, before score, delta score, after score, reason, admin nickname, and created at.

- [x] **Step 2: Create user restriction entity and repository**

Create `UserRestriction` with fields matching V10 and constructor:

```java
public UserRestriction(User user, ViolationRecord violation, String restrictionType, String reason, LocalDateTime startsAt, LocalDateTime endsAt, User admin) {
    this.user = user;
    this.violation = violation;
    this.restrictionType = restrictionType;
    this.reason = reason;
    this.startsAt = startsAt == null ? LocalDateTime.now() : startsAt;
    this.endsAt = endsAt;
    this.active = true;
    this.admin = admin;
}
```

Create repository:

```java
public interface UserRestrictionRepository extends JpaRepository<UserRestriction, Long> {
    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<UserRestriction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<UserRestriction> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndActiveTrueAndRestrictionTypeIn(Long userId, Collection<String> restrictionTypes);
}
```

Create `UserRestrictionSummary.from(UserRestriction restriction)` with id, user id, user nickname, violation id, type, reason, starts at, ends at, active, admin nickname, and created at.

- [x] **Step 3: Create admin action log entity and repository**

Create `AdminActionLog` with constructor:

```java
public AdminActionLog(User admin, String actionType, String targetType, Long targetId, String note) {
    this.admin = admin;
    this.actionType = actionType;
    this.targetType = targetType;
    this.targetId = targetId;
    this.note = note;
}
```

Create repository:

```java
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    @EntityGraph(attributePaths = {"admin"})
    List<AdminActionLog> findTop100ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"admin"})
    List<AdminActionLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
```

Create `AdminActionLogSummary.from(AdminActionLog log)` with id, admin id, admin nickname, action type, target type, target id, note, and created at.

- [x] **Step 4: Commit entity task**

```bash
git add backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecord.java backend/src/main/java/com/campushub/moderation/CreditAdjustmentRecordRepository.java backend/src/main/java/com/campushub/moderation/CreditAdjustmentSummary.java backend/src/main/java/com/campushub/moderation/UserRestriction.java backend/src/main/java/com/campushub/moderation/UserRestrictionRepository.java backend/src/main/java/com/campushub/moderation/UserRestrictionSummary.java backend/src/main/java/com/campushub/moderation/AdminActionLog.java backend/src/main/java/com/campushub/moderation/AdminActionLogRepository.java backend/src/main/java/com/campushub/moderation/AdminActionLogSummary.java
git commit -m "add credit restriction audit records"
```

## Task 4: Implement governance service and integration tests

**Files:**
- Create: `backend/src/main/java/com/campushub/moderation/GovernanceActionRequest.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreateViolationRequest.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreditAdjustmentRequest.java`
- Create: `backend/src/main/java/com/campushub/moderation/UserRestrictionRequest.java`
- Create: `backend/src/main/java/com/campushub/moderation/GovernanceDashboardSummary.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreditCenterSummary.java`
- Create: `backend/src/main/java/com/campushub/moderation/GovernanceService.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ModerationService.java`
- Test: `backend/src/test/java/com/campushub/moderation/GovernanceServiceIntegrationTest.java`

- [x] **Step 1: Add request and aggregate DTOs**

Create records:

```java
public record GovernanceActionRequest(String resolutionType, String note) {}

public record CreateViolationRequest(
        Long userId,
        Long reportId,
        String targetType,
        Long targetId,
        String violationType,
        String severity,
        String penaltyType,
        String description,
        Integer creditDelta,
        String depositImpactNote,
        String restrictionType,
        Integer restrictionDays) {}

public record CreditAdjustmentRequest(Integer deltaScore, String reason) {}

public record UserRestrictionRequest(String restrictionType, String reason, Integer days) {}
```

Create `GovernanceDashboardSummary`:

```java
public record GovernanceDashboardSummary(
        long openReports,
        long inReviewReports,
        long todayHandledReports,
        long highSeverityViolations,
        long activeRestrictions) {}
```

Create `CreditCenterSummary`:

```java
public record CreditCenterSummary(
        Long userId,
        String nickname,
        Integer creditScore,
        List<UserRestrictionSummary> activeRestrictions,
        List<ViolationRecordSummary> violations,
        List<CreditAdjustmentSummary> creditAdjustments,
        List<ReportRecordSummary> myReports) {}
```

- [x] **Step 2: Write failing integration test**

Create `GovernanceServiceIntegrationTest` with this core test:

```java
@SpringBootTest
@Transactional
class GovernanceServiceIntegrationTest {
    @Autowired GovernanceService governanceService;
    @Autowired ModerationService moderationService;
    @Autowired UserRepository userRepository;
    @Autowired CreditAdjustmentRecordRepository creditAdjustmentRepository;
    @Autowired UserRestrictionRepository userRestrictionRepository;
    @Autowired AdminActionLogRepository adminActionLogRepository;
    @Autowired StationNotificationRepository notificationRepository;

    @Test
    void resolvingReportWithViolationAdjustsCreditRestrictsUserAndAuditsAction() {
        User reporter = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User violator = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("admin@mail.ustc.edu.cn").orElseThrow();

        ReportRecordSummary report = moderationService.report(reporter.getId(),
                new ReportRequest("GOODS", 1L, "虚假信息", "商品描述和实际不一致"));

        governanceService.startReview(report.id(), admin.getId(), new GovernanceActionRequest(null, "开始核查"));
        ViolationRecordSummary violation = governanceService.createViolation(admin.getId(), new CreateViolationRequest(
                violator.getId(), report.id(), "GOODS", 1L, "FAKE_INFO", "MEDIUM", "POSTING_FREEZE",
                "发布虚假商品信息", -10, "暂不扣除保证金", "POSTING_FREEZE", 7));

        CreditCenterSummary center = governanceService.creditCenter(violator.getId());

        assertThat(violation.creditDelta()).isEqualTo(-10);
        assertThat(center.creditScore()).isEqualTo(90);
        assertThat(creditAdjustmentRepository.findByUserIdOrderByCreatedAtDesc(violator.getId())).hasSize(1);
        assertThat(userRestrictionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(violator.getId())).hasSize(1);
        assertThat(adminActionLogRepository.findTop100ByOrderByCreatedAtDesc()).isNotEmpty();
        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(violator.getId())).isNotEmpty();
    }
}
```

- [x] **Step 3: Implement governance service**

Create `GovernanceService` with methods:

```java
@Transactional(readOnly = true)
public List<ReportRecordSummary> listReports(String status, String targetType) { ... }

@Transactional
public ReportRecordSummary startReview(Long reportId, Long adminId, GovernanceActionRequest request) { ... }

@Transactional
public ReportRecordSummary reject(Long reportId, Long adminId, GovernanceActionRequest request) { ... }

@Transactional
public ReportRecordSummary resolve(Long reportId, Long adminId, GovernanceActionRequest request) { ... }

@Transactional
public ReportRecordSummary escalate(Long reportId, Long adminId, GovernanceActionRequest request) { ... }

@Transactional
public ViolationRecordSummary createViolation(Long adminId, CreateViolationRequest request) { ... }

@Transactional
public CreditAdjustmentSummary adjustCredit(Long userId, Long adminId, CreditAdjustmentRequest request) { ... }

@Transactional
public UserRestrictionSummary restrictUser(Long userId, Long adminId, UserRestrictionRequest request) { ... }

@Transactional(readOnly = true)
public CreditCenterSummary creditCenter(Long userId) { ... }
```

Use helper methods:

```java
private int clampCredit(int score) {
    return Math.max(0, Math.min(100, score));
}

private User user(Long id, String message) {
    return userRepository.findById(id).orElseThrow(() -> new BusinessException(message));
}

private void audit(User admin, String actionType, String targetType, Long targetId, String note) {
    adminActionLogRepository.save(new AdminActionLog(admin, actionType, targetType, targetId, note));
}
```

When creating a violation, call `resolve(reportId, adminId, new GovernanceActionRequest("USER_RESTRICTED", request.description()))` if `reportId` is not null and the report is not already terminal.

When applying credit delta:

```java
int before = targetUser.getCreditScore();
int after = clampCredit(before + creditDelta);
targetUser.setCreditScore(after);
creditAdjustmentRecordRepository.save(new CreditAdjustmentRecord(targetUser, violation, before, creditDelta, after, request.description(), admin));
```

When applying restriction, create `UserRestriction` if `restrictionType` is not blank and not `WARNING`.

When notifying:

```java
notificationService.notify(targetUser, "信用与治理通知", "平台已记录违规处理：" + request.description(), "VIOLATION", violation.getId());
```

- [x] **Step 4: Update `ModerationService` report creation**

Ensure new reports use `OPEN` through the `ReportRecord` constructor and no code still assumes `PENDING`.

- [x] **Step 5: Run targeted backend test if Maven is available**

Run only if Maven exists in the environment:

```bash
mvn -f backend/pom.xml -Dtest=GovernanceServiceIntegrationTest test
```

Expected: PASS. If `mvn` is unavailable, record that backend verification must be done through server Docker build before deployment.

- [x] **Step 6: Commit service task**

```bash
git add backend/src/main/java/com/campushub/moderation/GovernanceActionRequest.java backend/src/main/java/com/campushub/moderation/CreateViolationRequest.java backend/src/main/java/com/campushub/moderation/CreditAdjustmentRequest.java backend/src/main/java/com/campushub/moderation/UserRestrictionRequest.java backend/src/main/java/com/campushub/moderation/GovernanceDashboardSummary.java backend/src/main/java/com/campushub/moderation/CreditCenterSummary.java backend/src/main/java/com/campushub/moderation/GovernanceService.java backend/src/main/java/com/campushub/moderation/ModerationService.java backend/src/test/java/com/campushub/moderation/GovernanceServiceIntegrationTest.java
git commit -m "implement governance credit service"
```

## Task 5: Add governance and credit APIs

**Files:**
- Create: `backend/src/main/java/com/campushub/moderation/AdminGovernanceController.java`
- Create: `backend/src/main/java/com/campushub/moderation/CreditController.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ModerationController.java`

- [x] **Step 1: Create admin governance controller**

Create `AdminGovernanceController`:

```java
@RestController
@RequestMapping("/api/admin/governance")
public class AdminGovernanceController {
    private final GovernanceService governanceService;

    public AdminGovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<GovernanceDashboardSummary> dashboard() {
        return ApiResponse.ok(governanceService.dashboard());
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportRecordSummary>> reports(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String targetType) {
        return ApiResponse.ok(governanceService.listReports(status, targetType));
    }

    @PostMapping("/reports/{reportId}/start-review")
    public ApiResponse<ReportRecordSummary> startReview(@PathVariable Long reportId, @RequestParam Long adminId,
                                                         @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.startReview(reportId, adminId, request));
    }

    @PostMapping("/reports/{reportId}/reject")
    public ApiResponse<ReportRecordSummary> reject(@PathVariable Long reportId, @RequestParam Long adminId,
                                                    @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.reject(reportId, adminId, request));
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ApiResponse<ReportRecordSummary> resolve(@PathVariable Long reportId, @RequestParam Long adminId,
                                                     @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.resolve(reportId, adminId, request));
    }

    @PostMapping("/reports/{reportId}/escalate")
    public ApiResponse<ReportRecordSummary> escalate(@PathVariable Long reportId, @RequestParam Long adminId,
                                                      @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.escalate(reportId, adminId, request));
    }

    @PostMapping("/violations")
    public ApiResponse<ViolationRecordSummary> createViolation(@RequestParam Long adminId,
                                                                @RequestBody CreateViolationRequest request) {
        return ApiResponse.ok(governanceService.createViolation(adminId, request));
    }

    @PostMapping("/users/{userId}/credit-adjustments")
    public ApiResponse<CreditAdjustmentSummary> adjustCredit(@PathVariable Long userId, @RequestParam Long adminId,
                                                              @RequestBody CreditAdjustmentRequest request) {
        return ApiResponse.ok(governanceService.adjustCredit(userId, adminId, request));
    }

    @PostMapping("/users/{userId}/restrictions")
    public ApiResponse<UserRestrictionSummary> restrictUser(@PathVariable Long userId, @RequestParam Long adminId,
                                                             @RequestBody UserRestrictionRequest request) {
        return ApiResponse.ok(governanceService.restrictUser(userId, adminId, request));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AdminActionLogSummary>> auditLogs() {
        return ApiResponse.ok(governanceService.auditLogs());
    }
}
```

- [x] **Step 2: Create user credit controller**

Create `CreditController`:

```java
@RestController
@RequestMapping("/api/credit")
public class CreditController {
    private final GovernanceService governanceService;

    public CreditController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<CreditCenterSummary> creditCenter(@PathVariable Long userId) {
        return ApiResponse.ok(governanceService.creditCenter(userId));
    }

    @GetMapping("/users/{userId}/reports")
    public ApiResponse<List<ReportRecordSummary>> myReports(@PathVariable Long userId) {
        return ApiResponse.ok(governanceService.reportsByReporter(userId));
    }
}
```

- [x] **Step 3: Keep legacy moderation list endpoints working**

In `ModerationController`, keep existing `/api/moderation/reports` and `/api/moderation/violations` responses working for older frontend/admin pages. If the summary shape changed, ensure existing callers still receive id, target type/id, reason, description, status, and created time.

- [x] **Step 4: Commit API task**

```bash
git add backend/src/main/java/com/campushub/moderation/AdminGovernanceController.java backend/src/main/java/com/campushub/moderation/CreditController.java backend/src/main/java/com/campushub/moderation/ModerationController.java
git commit -m "add governance and credit APIs"
```

## Task 6: Enforce active restrictions in key business actions

**Files:**
- Modify: `backend/src/main/java/com/campushub/moderation/GovernanceService.java`
- Modify: `backend/src/main/java/com/campushub/task/RunnerTaskService.java`
- Modify: goods publishing service/controller file currently used in `backend/src/main/java/com/campushub/goods/`
- Modify: `backend/src/main/java/com/campushub/shop/ShopService.java`
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdService.java`

- [x] **Step 1: Add reusable restriction guard methods**

In `GovernanceService`, add:

```java
@Transactional(readOnly = true)
public void ensureCanPost(Long userId) {
    if (userRestrictionRepository.existsByUserIdAndActiveTrueAndRestrictionTypeIn(
            userId, List.of("POSTING_FREEZE", "ACCOUNT_DISABLED"))) {
        throw new BusinessException("当前账号存在发布限制，请查看信用中心");
    }
}

@Transactional(readOnly = true)
public void ensureCanProvideService(Long userId) {
    if (userRestrictionRepository.existsByUserIdAndActiveTrueAndRestrictionTypeIn(
            userId, List.of("SERVICE_FREEZE", "ACCOUNT_DISABLED"))) {
        throw new BusinessException("当前账号存在服务限制，请查看信用中心");
    }
}
```

- [x] **Step 2: Block runner task publishing and runner actions**

In `RunnerTaskService.publish`, call:

```java
governanceService.ensureCanPost(publisherId);
```

In `grab`, `apply`, and workflow provider-side methods, call:

```java
governanceService.ensureCanProvideService(runnerId);
```

Inject `GovernanceService` through the constructor.

- [x] **Step 3: Block goods, shop, and project ad publishing**

In the goods create/publish path, call `governanceService.ensureCanPost(sellerId)` before saving a new listed good.

In `ShopService`, call:

```java
governanceService.ensureCanPost(merchantId);
```

before creating shops and service items, and call:

```java
governanceService.ensureCanProvideService(merchantId);
```

before accepting/starting service orders.

In `ProjectAdService`, call `governanceService.ensureCanPost(publisherId)` before creating or submitting project ads.

- [x] **Step 4: Add restriction regression test**

Extend `GovernanceServiceIntegrationTest` with a test that creates a `POSTING_FREEZE` restriction and verifies publishing a runner task fails with `BusinessException`.

- [x] **Step 5: Commit restriction task**

```bash
git add backend/src/main/java/com/campushub/moderation/GovernanceService.java backend/src/main/java/com/campushub/task/RunnerTaskService.java backend/src/main/java/com/campushub/goods backend/src/main/java/com/campushub/shop/ShopService.java backend/src/main/java/com/campushub/projectad/ProjectAdService.java backend/src/test/java/com/campushub/moderation/GovernanceServiceIntegrationTest.java
git commit -m "enforce governance restrictions on actions"
```

## Task 7: Add frontend governance and credit API types

**Files:**
- Modify: `frontend/src/api/campushub.ts`

- [x] **Step 1: Add TypeScript interfaces**

Add interfaces matching backend summaries:

```ts
export interface GovernanceDashboardSummary {
  openReports: number
  inReviewReports: number
  todayHandledReports: number
  highSeverityViolations: number
  activeRestrictions: number
}

export interface GovernanceActionPayload {
  resolutionType?: string
  note?: string
}

export interface CreateViolationPayload {
  userId: number
  reportId?: number
  targetType?: string
  targetId?: number
  violationType: string
  severity: string
  penaltyType: string
  description: string
  creditDelta: number
  depositImpactNote?: string
  restrictionType?: string
  restrictionDays?: number
}

export interface CreditAdjustmentSummary {
  id: number
  userId: number
  userNickname: string
  violationId?: number
  beforeScore: number
  deltaScore: number
  afterScore: number
  reason: string
  adminNickname?: string
  createdAt: string
}

export interface UserRestrictionSummary {
  id: number
  userId: number
  userNickname: string
  violationId?: number
  restrictionType: string
  reason: string
  startsAt: string
  endsAt?: string
  active: boolean
  adminNickname?: string
  createdAt: string
}

export interface AdminActionLogSummary {
  id: number
  adminId?: number
  adminNickname?: string
  actionType: string
  targetType: string
  targetId: number
  note?: string
  createdAt: string
}

export interface CreditCenterSummary {
  userId: number
  nickname: string
  creditScore: number
  activeRestrictions: UserRestrictionSummary[]
  violations: ViolationRecordSummary[]
  creditAdjustments: CreditAdjustmentSummary[]
  myReports: ReportRecordSummary[]
}
```

- [x] **Step 2: Add API functions**

Add functions:

```ts
export function getGovernanceDashboard() {
  return apiGet<GovernanceDashboardSummary>('/admin/governance/dashboard')
}

export function getGovernanceReports(params?: { status?: string; targetType?: string }) {
  return apiGet<ReportRecordSummary[]>('/admin/governance/reports', { params })
}

export function startReportReview(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return apiPost<ReportRecordSummary>(`/admin/governance/reports/${reportId}/start-review`, payload, { params: { adminId } })
}

export function rejectReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return apiPost<ReportRecordSummary>(`/admin/governance/reports/${reportId}/reject`, payload, { params: { adminId } })
}

export function resolveReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return apiPost<ReportRecordSummary>(`/admin/governance/reports/${reportId}/resolve`, payload, { params: { adminId } })
}

export function escalateReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return apiPost<ReportRecordSummary>(`/admin/governance/reports/${reportId}/escalate`, payload, { params: { adminId } })
}

export function createViolation(adminId: number, payload: CreateViolationPayload) {
  return apiPost<ViolationRecordSummary>('/admin/governance/violations', payload, { params: { adminId } })
}

export function getAdminActionLogs() {
  return apiGet<AdminActionLogSummary[]>('/admin/governance/audit-logs')
}

export function getCreditCenter(userId: number) {
  return apiGet<CreditCenterSummary>(`/credit/users/${userId}`)
}
```

- [x] **Step 3: Commit frontend API task**

```bash
git add frontend/src/api/campushub.ts
git commit -m "add governance frontend APIs"
```

## Task 8: Add admin governance workspace

**Files:**
- Create: `frontend/src/views/AdminGovernanceView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [x] **Step 1: Create admin governance view**

Create `AdminGovernanceView.vue` with:

- metrics cards from `getGovernanceDashboard()`;
- status and target type filters;
- report table from `getGovernanceReports()`;
- action buttons for start review, reject, resolve, escalate;
- violation dialog with user id, severity, penalty, credit delta, restriction type/days;
- audit log table from `getAdminActionLogs()`.

Use admin id `1` as the current prototype admin default, matching existing admin pages if they use seeded user IDs.

- [x] **Step 2: Add route**

In `frontend/src/router/index.ts`, add:

```ts
{
  path: '/admin/governance',
  name: 'admin-governance',
  component: () => import('../views/AdminGovernanceView.vue')
}
```

- [x] **Step 3: Add navigation**

In `MainLayout.vue`, add a nav item labeled `治理台` pointing to `/admin/governance` near `运营后台`.

- [x] **Step 4: Add responsive styles**

In `styles.css`, add classes for governance metric grid, table actions, and mobile stacked filters. Keep selectors scoped by class names such as `.governance-page`, `.governance-metrics`, `.governance-actions`.

- [x] **Step 5: Commit admin UI task**

```bash
git add frontend/src/views/AdminGovernanceView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add admin governance workspace"
```

## Task 9: Add user credit center

**Files:**
- Create: `frontend/src/views/CreditCenterView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [x] **Step 1: Create credit center view**

Create `CreditCenterView.vue` with:

- current user id from auth store when available, fallback to demo user `1` only if existing pages use the same demo pattern;
- credit score card;
- active restriction list;
- violation timeline/table;
- credit adjustment timeline/table;
- my reports table;
- platform safety notice explaining no principal escrow and no per-order deposit freeze.

- [x] **Step 2: Add route**

In `router/index.ts`, add:

```ts
{
  path: '/credit',
  name: 'credit-center',
  component: () => import('../views/CreditCenterView.vue')
}
```

- [x] **Step 3: Add navigation**

In `MainLayout.vue`, add a user-side nav item labeled `信用中心` pointing to `/credit`.

- [x] **Step 4: Add mobile styles**

In `styles.css`, add `.credit-page`, `.credit-score-card`, `.credit-timeline`, and mobile single-column rules.

- [x] **Step 5: Commit credit UI task**

```bash
git add frontend/src/views/CreditCenterView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add user credit center"
```

## Task 10: Documentation, status, and verification handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Modify: `docs/superpowers/plans/2026-05-22-campushub-phase5-governance-upgrade.md`

- [x] **Step 1: Update README**

Add a Phase 5 section documenting:

- unified governance queue;
- report statuses;
- violation records;
- credit history;
- user restrictions;
- admin audit;
- station notifications;
- payment boundary remains no principal escrow and no direct Alipay key handling.

- [x] **Step 2: Update `CLAUDE.md` handoff**

Add a new dated section after Phase 4 once implementation is verified. Include:

- latest commit;
- V10 migration summary;
- backend APIs added;
- frontend pages added;
- verification commands and production smoke results;
- reminder that V1-V10 are now applied if deployed.

- [x] **Step 3: Mark plan checkboxes as tasks complete**

As each task is implemented, update this plan's checkboxes from `[ ]` to `[x]`. Do this in the same commit as the task or in a docs handoff commit.

- [x] **Step 4: Server-side verification after push approval**

On production server, use low-impact verification only after code is pushed and deployment is approved:

```bash
cd /opt/campushub
git pull --ff-only
docker compose -f docker-compose.prod.yml build campushub-backend campushub-web
docker compose -f docker-compose.prod.yml up -d campushub-backend campushub-web
```

Expected: backend Maven package succeeds inside Docker; backend and web containers restart without repeatedly crashing.

- [x] **Step 5: API smoke after deployment**

Run server-local checks without printing secrets:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/governance/dashboard
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/governance/reports
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/credit/users/1
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/goods
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/tasks
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/shops
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/project-ads
```

Expected: HTTP 200 for public/prototype endpoints; no HTTP 500.

- [x] **Step 6: Browser verification with Playwriter**

Verify:

- `/admin/governance` renders metrics, queue, actions, and audit area.
- `/credit` renders score, restrictions, violations, adjustments, and report statuses.
- Regression routes `/goods`, `/tasks`, `/shops`, `/project-ads`, `/admin/ops` render without white screens.
- Mobile viewport 390x844 on `/credit` has no obvious horizontal overflow.

- [x] **Step 7: Final commit**

```bash
git add README.md CLAUDE.md docs/superpowers/plans/2026-05-22-campushub-phase5-governance-upgrade.md
git commit -m "document phase 5 governance handoff"
```

## Self-review

- Spec coverage: The plan covers unified report queue, status flow, violation creation, credit adjustment history, user restrictions, admin audit logs, station notifications, admin governance workspace, user credit center, README/CLAUDE handoff, server Docker verification, API smoke, and Playwriter verification.
- Scope: The plan is one Phase-4-sized subsystem. Analytics/export, payment-center integration, auth hardening, and beta runbook remain future phases.
- Boundary check: The plan does not introduce principal escrow, per-order deposit freezing, direct Alipay key handling, or edits to V1-V9 migrations.
- Type consistency: Request, summary, entity, repository, controller, and frontend API names use the same governance/credit/restriction vocabulary across tasks.
- Placeholder scan: No TBD/TODO placeholders are used; optional local Maven verification is explicitly conditional on tool availability, with server Docker verification required before deployment completion.
