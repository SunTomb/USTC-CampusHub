# CampusHub Phase 4 Project Ads Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade project ads into a campus showcase workflow with publishing, review, expiration, tags, contact visibility, featured slots, interactions, and operations visibility.

**Architecture:** Extend the existing `projectad` bounded context instead of creating a separate community module. Reuse generic files, interactions, moderation reports, and station notifications through the existing `target_type = PROJECT_AD` pattern; add only project-ad-specific state, review, expiration, and contact-visibility logic. Schema changes use a new V9 Flyway migration because production has already applied V1-V8.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, JUnit/Spring Boot Test, Vue 3, Vite, TypeScript, Vue Router, Element Plus.

---

## Scope and decomposition

This plan implements the Phase 4 design in `docs/superpowers/specs/2026-05-21-campushub-phase4-project-ads-design.md`.

Included:

- Project ad publishing, editing, submit-for-review, close, approve, reject, feature, unfeature, block.
- Tags, ad type, campus zone, summary, expiration, contact visibility, review notes, featured priority.
- Public list/detail, featured list, publisher management list, operations monitoring list.
- Detail-page contact visibility, view count increment, image/file bindings, comments, favorites, and report entry points.
- Frontend project ads market, detail page, user management page, and admin operations tab.

Excluded:

- Transaction principal escrow, ad deposits, platform chat, complex recommendation algorithms, nested comments, following system, mini-program client.

## File structure map

### Backend migration

- Create `backend/src/main/resources/db/migration/V9__project_ads_showcase_upgrade.sql` — extends `project_ads` with Phase 4 fields and adds safe indexes.
- Create `backend/src/test/resources/db/migration/V9__project_ads_showcase_upgrade.sql` — H2-compatible equivalent for tests if the test profile uses isolated migrations.

### Backend projectad package

- Modify `backend/src/main/java/com/campushub/projectad/ProjectAd.java` — add fields, constructors, state transition methods, contact visibility logic helpers.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdRepository.java` — add public, featured, owner, admin, and expiring query methods.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdSummary.java` — expand card/detail fields and contact visibility outputs.
- Create `backend/src/main/java/com/campushub/projectad/ProjectAdDetailSummary.java` — richer detail DTO with files, favorite/comment counts, viewer state, and contact visibility.
- Create `backend/src/main/java/com/campushub/projectad/ProjectAdRequest.java` — create/edit request DTO.
- Create `backend/src/main/java/com/campushub/projectad/ProjectAdReviewRequest.java` — admin review/feature/block request DTO.
- Create `backend/src/main/java/com/campushub/projectad/ProjectAdQueryRequest.java` or use controller request params — list filters.
- Create `backend/src/main/java/com/campushub/projectad/ProjectAdService.java` — user/admin workflow, query filtering, detail aggregation, notifications.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdController.java` — expose user-facing and publisher endpoints.

### Backend operations package

- Modify `backend/src/main/java/com/campushub/ops/OperationsController.java` — add project-ad operations monitor and admin state actions.

### Backend tests

- Create `backend/src/test/java/com/campushub/projectad/ProjectAdServiceIntegrationTest.java` — publishing, review, visibility, expiration, featured ordering, ownership checks.

### Frontend API

- Modify `frontend/src/api/campushub.ts` — expand `ProjectAdSummary`, add detail/request types and functions for user/admin workflow.

### Frontend pages and routing

- Modify `frontend/src/views/ProjectAdsView.vue` — upgrade list into showcase market with filters and publish entry.
- Create `frontend/src/views/ProjectAdDetailView.vue` — detail page with files, contact visibility, favorites/comments/report actions.
- Create `frontend/src/views/ProjectAdManageView.vue` — publisher management page for create/edit/submit/close.
- Modify `frontend/src/views/AdminOperationsView.vue` — add project ads tab and admin actions.
- Modify `frontend/src/router/index.ts` — add `/project-ads/:id` and `/project-ads/manage`.
- Modify `frontend/src/layouts/MainLayout.vue` — add management route if consistent with existing navigation.
- Modify `frontend/src/styles.css` — responsive cards, detail grid, management form, admin table fixes.

### Docs

- Modify `README.md` — document Phase 4 and preserve payment/security boundary.
- Optionally modify `CLAUDE.md` after verification — add Phase 4 handoff only when implementation and verification are done.

---

## Task 1: Add Phase 4 project-ad schema and domain model

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__project_ads_showcase_upgrade.sql`
- Create: `backend/src/test/resources/db/migration/V9__project_ads_showcase_upgrade.sql`
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAd.java`
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdRepository.java`

- [ ] **Step 1: Create production migration**

Create `backend/src/main/resources/db/migration/V9__project_ads_showcase_upgrade.sql`:

```sql
ALTER TABLE project_ads
    ADD COLUMN ad_type VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER title,
    ADD COLUMN summary VARCHAR(500) NULL AFTER ad_type,
    ADD COLUMN tags VARCHAR(500) NULL AFTER summary,
    ADD COLUMN campus_zone VARCHAR(40) NULL AFTER tags,
    ADD COLUMN cover_file_id BIGINT NULL AFTER campus_zone,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'LOGIN_ONLY' AFTER contact_info,
    ADD COLUMN expires_at DATETIME NULL AFTER contact_visibility,
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE AFTER expires_at,
    ADD COLUMN featured_priority INT NOT NULL DEFAULT 0 AFTER featured,
    ADD COLUMN review_note VARCHAR(500) NULL AFTER featured_priority,
    ADD COLUMN reviewed_by BIGINT NULL AFTER review_note,
    ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by,
    ADD COLUMN published_at DATETIME NULL AFTER reviewed_at,
    ADD COLUMN closed_at DATETIME NULL AFTER published_at;

ALTER TABLE project_ads
    ADD CONSTRAINT fk_project_ads_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id),
    ADD CONSTRAINT fk_project_ads_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE INDEX idx_project_ads_public ON project_ads (status, featured, expires_at, created_at);
CREATE INDEX idx_project_ads_type_zone ON project_ads (ad_type, campus_zone);
CREATE INDEX idx_project_ads_publisher_status ON project_ads (publisher_id, status, created_at);
```

- [ ] **Step 2: Create H2-compatible test migration**

If `backend/src/test/resources/db/migration` exists, create the same V9 file there with H2-compatible syntax. If the test migration directory mirrors production successfully with MySQL mode, use the same SQL. If H2 rejects `AFTER`, remove `AFTER ...` clauses in the test migration only.

- [ ] **Step 3: Extend `ProjectAd` entity**

Add fields matching V9, getters, and workflow methods:

```java
public ProjectAd(User publisher, ProjectAdRequest request) {
    this.publisher = publisher;
    this.title = request.title().trim();
    this.adType = normalize(request.adType(), "OTHER");
    this.summary = trimToNull(request.summary());
    this.description = request.description().trim();
    this.tags = trimToNull(request.tags());
    this.campusZone = trimToNull(request.campusZone());
    this.linkUrl = trimToNull(request.linkUrl());
    this.contactInfo = request.contactInfo().trim();
    this.contactVisibility = normalize(request.contactVisibility(), "LOGIN_ONLY");
    this.expiresAt = request.expiresAt();
    this.status = "PENDING_REVIEW";
    this.viewCount = 0;
    this.featured = false;
    this.featuredPriority = 0;
}

public void update(ProjectAdRequest request) {
    this.title = request.title().trim();
    this.adType = normalize(request.adType(), "OTHER");
    this.summary = trimToNull(request.summary());
    this.description = request.description().trim();
    this.tags = trimToNull(request.tags());
    this.campusZone = trimToNull(request.campusZone());
    this.linkUrl = trimToNull(request.linkUrl());
    this.contactInfo = request.contactInfo().trim();
    this.contactVisibility = normalize(request.contactVisibility(), "LOGIN_ONLY");
    this.expiresAt = request.expiresAt();
    if ("APPROVED".equals(status)) {
        this.status = "PENDING_REVIEW";
        this.reviewNote = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.publishedAt = null;
        this.featured = false;
        this.featuredPriority = 0;
    }
}

public void approve(User reviewer, String note) {
    this.status = "APPROVED";
    this.reviewedBy = reviewer;
    this.reviewedAt = LocalDateTime.now();
    this.reviewNote = trimToNull(note);
    if (this.publishedAt == null) {
        this.publishedAt = this.reviewedAt;
    }
}

public void reject(User reviewer, String note) {
    this.status = "REJECTED";
    this.reviewedBy = reviewer;
    this.reviewedAt = LocalDateTime.now();
    this.reviewNote = trimToNull(note);
    this.featured = false;
    this.featuredPriority = 0;
}

public void feature(int priority) {
    this.featured = true;
    this.featuredPriority = priority;
}

public void unfeature() {
    this.featured = false;
    this.featuredPriority = 0;
}

public void closeByPublisher() {
    this.status = "CLOSED";
    this.closedAt = LocalDateTime.now();
    this.featured = false;
    this.featuredPriority = 0;
}

public void block(User reviewer, String note) {
    this.status = "BLOCKED";
    this.reviewedBy = reviewer;
    this.reviewedAt = LocalDateTime.now();
    this.reviewNote = trimToNull(note);
    this.closedAt = this.reviewedAt;
    this.featured = false;
    this.featuredPriority = 0;
}

public void increaseViewCount() {
    this.viewCount = this.viewCount == null ? 1 : this.viewCount + 1;
}
```

- [ ] **Step 4: Extend repository queries**

Add repository methods:

```java
@EntityGraph(attributePaths = {"publisher"})
List<ProjectAd> findByPublisherIdOrderByCreatedAtDesc(Long publisherId);

@EntityGraph(attributePaths = {"publisher"})
List<ProjectAd> findByStatusOrderByFeaturedDescFeaturedPriorityDescCreatedAtDesc(String status);

@EntityGraph(attributePaths = {"publisher"})
List<ProjectAd> findByFeaturedTrueAndStatusOrderByFeaturedPriorityDescCreatedAtDesc(String status);
```

For richer filtering, use service-side filtering on these lists first to avoid overbuilding query specifications.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V9__project_ads_showcase_upgrade.sql backend/src/test/resources/db/migration/V9__project_ads_showcase_upgrade.sql backend/src/main/java/com/campushub/projectad/ProjectAd.java backend/src/main/java/com/campushub/projectad/ProjectAdRepository.java
git commit -m "add project ad showcase schema"
```

## Task 2: Implement project-ad service workflow and DTOs

**Files:**
- Create: `backend/src/main/java/com/campushub/projectad/ProjectAdRequest.java`
- Create: `backend/src/main/java/com/campushub/projectad/ProjectAdReviewRequest.java`
- Create: `backend/src/main/java/com/campushub/projectad/ProjectAdDetailSummary.java`
- Create: `backend/src/main/java/com/campushub/projectad/ProjectAdService.java`
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdSummary.java`
- Test: `backend/src/test/java/com/campushub/projectad/ProjectAdServiceIntegrationTest.java`

- [ ] **Step 1: Write failing service tests**

Create `ProjectAdServiceIntegrationTest` with tests:

```java
@Test
void createdProjectAdIsPendingReviewAndHiddenFromPublicList() { ... }

@Test
void approvedProjectAdAppearsInPublicListAndNotifiesPublisher() { ... }

@Test
void expiredProjectAdIsHiddenFromPublicList() { ... }

@Test
void loginOnlyContactIsVisibleOnlyToLoggedInViewer() { ... }

@Test
void publisherCannotCloseAnotherUsersProjectAd() { ... }
```

Use seeded users `student1@mail.ustc.edu.cn` and `student2@mail.ustc.edu.cn`. Assert status strings, list membership, contactVisible values, and `BusinessException` for ownership violations.

- [ ] **Step 2: Run failing tests**

Run: `mvn -f backend/pom.xml -Dtest=ProjectAdServiceIntegrationTest test`

Expected: FAIL because DTOs/service do not exist.

- [ ] **Step 3: Add request DTOs**

Create `ProjectAdRequest.java`:

```java
package com.campushub.projectad;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ProjectAdRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String adType,
        @Size(max = 500) String summary,
        @NotBlank String description,
        @Size(max = 500) String tags,
        String campusZone,
        String linkUrl,
        @NotBlank @Size(max = 255) String contactInfo,
        @NotBlank String contactVisibility,
        @Future LocalDateTime expiresAt) {
}
```

Create `ProjectAdReviewRequest.java`:

```java
package com.campushub.projectad;

public record ProjectAdReviewRequest(String note, Integer featuredPriority) {
}
```

- [ ] **Step 4: Expand summary DTOs**

`ProjectAdSummary` should expose: `id`, `title`, `adType`, `summary`, `description`, `tags`, `campusZone`, `publisherId`, `publisherNickname`, `linkUrl`, `status`, `viewCount`, `featured`, `featuredPriority`, `expiresAt`, `publishedAt`, `createdAt`.

`ProjectAdDetailSummary` should additionally expose: `contactVisible`, `contactInfo`, `contactVisibility`, `reviewNote`, `favoriteCount`, `commentCount`, `favorited`, and `attachments` if reusable file summary type is available; otherwise add attachment integration in Task 4.

- [ ] **Step 5: Implement `ProjectAdService`**

Implement methods:

```java
List<ProjectAdSummary> listPublic(String type, String campusZone, String keyword, Boolean featured)
List<ProjectAdSummary> listFeatured()
ProjectAdDetailSummary getDetail(Long id, Long viewerId)
List<ProjectAdSummary> listByPublisher(Long publisherId)
ProjectAdSummary create(Long publisherId, ProjectAdRequest request)
ProjectAdSummary update(Long id, Long publisherId, ProjectAdRequest request)
ProjectAdSummary submit(Long id, Long publisherId)
ProjectAdSummary close(Long id, Long publisherId)
List<ProjectAdSummary> listForAdmin(String status)
ProjectAdSummary approve(Long id, Long adminId, ProjectAdReviewRequest request)
ProjectAdSummary reject(Long id, Long adminId, ProjectAdReviewRequest request)
ProjectAdSummary feature(Long id, Long adminId, ProjectAdReviewRequest request)
ProjectAdSummary unfeature(Long id, Long adminId)
ProjectAdSummary block(Long id, Long adminId, ProjectAdReviewRequest request)
```

Rules:

- Public list includes only `APPROVED` ads whose `expiresAt` is null or future.
- `getDetail` increments view count once per call.
- `LOGIN_ONLY` contact is visible when `viewerId != null`.
- `PUBLIC` contact is always visible.
- `HIDDEN` contact is visible only to publisher/admin path; detail can use publisher check.
- `INTERACTION_ONLY` can initially be visible when `viewerId != null`; Task 4 can tighten it after favorite/comment integration if straightforward.
- Create notifications for approve/reject/feature/block using `NotificationService`.

- [ ] **Step 6: Verify service tests pass**

Run: `mvn -f backend/pom.xml -Dtest=ProjectAdServiceIntegrationTest test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/projectad backend/src/test/java/com/campushub/projectad/ProjectAdServiceIntegrationTest.java
git commit -m "implement project ad workflow service"
```

## Task 3: Expose project-ad user and admin APIs

**Files:**
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdController.java`
- Modify: `backend/src/main/java/com/campushub/ops/OperationsController.java`
- Test: `backend/src/test/java/com/campushub/projectad/ProjectAdServiceIntegrationTest.java` or controller test if existing patterns prefer MockMvc.

- [ ] **Step 1: Replace repository-only controller with service controller**

Expose:

```java
@GetMapping
@GetMapping("/featured")
@GetMapping("/{id}")
@GetMapping("/users/{userId}")
@PostMapping
@PutMapping("/{id}")
@PostMapping("/{id}/submit")
@PostMapping("/{id}/close")
```

Use request params for `publisherId`, `viewerId`, filters, and status where existing prototype APIs use explicit IDs.

- [ ] **Step 2: Add admin operations endpoints**

In `OperationsController`, add:

```java
@GetMapping("/project-ads")
@PostMapping("/project-ads/{id}/approve")
@PostMapping("/project-ads/{id}/reject")
@PostMapping("/project-ads/{id}/feature")
@PostMapping("/project-ads/{id}/unfeature")
@PostMapping("/project-ads/{id}/block")
```

- [ ] **Step 3: Verify backend compiles/tests**

Run: `mvn -f backend/pom.xml -Dtest=ProjectAdServiceIntegrationTest test`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/campushub/projectad/ProjectAdController.java backend/src/main/java/com/campushub/ops/OperationsController.java backend/src/test/java/com/campushub/projectad/ProjectAdServiceIntegrationTest.java
git commit -m "expose project ad workflow APIs"
```

## Task 4: Aggregate files, favorites, comments, and reports for detail UI

**Files:**
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdService.java`
- Modify: `backend/src/main/java/com/campushub/projectad/ProjectAdDetailSummary.java`
- Modify: `frontend/src/api/campushub.ts`

- [ ] **Step 1: Inspect reusable APIs**

Use existing file binding, comments, favorites, and moderation endpoints from goods/shop pages as the integration pattern. Do not create project-ad-specific interaction tables.

- [ ] **Step 2: Add detail counts and attachment summaries**

In service detail mapping, include:

- favorite count for `PROJECT_AD` target.
- comment count for `PROJECT_AD` target.
- viewer favorite state if existing repository supports it.
- file bindings for `PROJECT_AD` target if existing file repository/service has a public method.

If existing services do not expose count methods cleanly, keep backend detail lightweight and let frontend call the generic endpoints separately.

- [ ] **Step 3: Tighten `INTERACTION_ONLY` contact visibility if cheap**

If favorite/comment repositories can answer whether viewer interacted with target, make `INTERACTION_ONLY` visible only after favorite or comment. Otherwise keep it as logged-in visibility and document that it is a Phase 4.1 tightening item.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/campushub/projectad/ProjectAdService.java backend/src/main/java/com/campushub/projectad/ProjectAdDetailSummary.java frontend/src/api/campushub.ts
git commit -m "aggregate project ad detail interactions"
```

## Task 5: Add frontend project-ad API types and functions

**Files:**
- Modify: `frontend/src/api/campushub.ts`

- [ ] **Step 1: Expand project ad types**

Add/modify:

```ts
export type ProjectAdType = 'TEAM_UP' | 'PORTFOLIO' | 'CLUB_RECRUITMENT' | 'CAMPUS_EVENT' | 'OTHER'
export type ProjectAdStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CLOSED' | 'BLOCKED'
export type ProjectAdContactVisibility = 'PUBLIC' | 'LOGIN_ONLY' | 'INTERACTION_ONLY' | 'HIDDEN'
```

Expand `ProjectAdSummary` and create `ProjectAdDetailSummary`, `ProjectAdPayload`, and `ProjectAdReviewPayload` interfaces matching backend DTOs.

- [ ] **Step 2: Add API functions**

Add:

```ts
export function listProjectAds(params?: ProjectAdListParams) { ... }
export function listFeaturedProjectAds() { ... }
export function getProjectAd(id: number, viewerId?: number) { ... }
export function listUserProjectAds(userId: number) { ... }
export function createProjectAd(publisherId: number, payload: ProjectAdPayload) { ... }
export function updateProjectAd(id: number, publisherId: number, payload: ProjectAdPayload) { ... }
export function submitProjectAd(id: number, publisherId: number) { ... }
export function closeProjectAd(id: number, publisherId: number) { ... }
export function listOpsProjectAds(status?: string) { ... }
export function approveProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) { ... }
export function rejectProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) { ... }
export function featureProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) { ... }
export function unfeatureProjectAd(id: number, adminId: number) { ... }
export function blockProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) { ... }
```

- [ ] **Step 3: Build frontend type check**

Run: `npm --prefix frontend run build`

Expected: PASS or fail only where pages still use old type fields; fix old references immediately.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/campushub.ts
git commit -m "add project ad frontend APIs"
```

## Task 6: Upgrade project ads market and detail pages

**Files:**
- Modify: `frontend/src/views/ProjectAdsView.vue`
- Create: `frontend/src/views/ProjectAdDetailView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Upgrade `/project-ads` list**

Implement filters for type, campus zone, keyword, and featured. Render cards with cover placeholder, type label, title, summary, tags, publisher, view count, favorite/comment counts if available, expiration, and featured badge.

- [ ] **Step 2: Add publish/manage entry**

Add button to `/project-ads/manage`. If no logged-in user exists in auth store, show login prompt and route to `/auth`.

- [ ] **Step 3: Add detail page**

Create `ProjectAdDetailView.vue` that loads `getProjectAd(id, currentUser?.id)`, displays project fields, contact visibility area, external link, attachments/generic image section if available, comments/favorites using existing generic APIs, and report entry.

- [ ] **Step 4: Add route**

Add `/project-ads/:id` route before any catch-all route.

- [ ] **Step 5: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS with known chunk warning only.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/ProjectAdsView.vue frontend/src/views/ProjectAdDetailView.vue frontend/src/router/index.ts frontend/src/styles.css
git commit -m "upgrade project ads showcase pages"
```

## Task 7: Add project-ad publisher management page

**Files:**
- Create: `frontend/src/views/ProjectAdManageView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Create management page**

Build list of current user's ads via `listUserProjectAds(currentUser.id)` and a form for create/edit. Fields: type, title, summary, description, tags, campus zone, link, contact info, contact visibility, expires at.

- [ ] **Step 2: Add workflow buttons**

For each owned ad, show status and actions:

- edit for `DRAFT`, `PENDING_REVIEW`, `REJECTED`, `APPROVED`.
- submit for `DRAFT` or `REJECTED`.
- close for `APPROVED`, `PENDING_REVIEW`, `REJECTED`.

- [ ] **Step 3: Add route/nav**

Add `/project-ads/manage` route. Add a navigation entry only if the existing layout has similar user workbench entries; otherwise keep the entry button on `/project-ads`.

- [ ] **Step 4: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ProjectAdManageView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add project ad publisher workspace"
```

## Task 8: Add operations project-ad monitor UI

**Files:**
- Modify: `frontend/src/views/AdminOperationsView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add project ads tab**

Add a tab named `项目广告` with filters for status and a table showing title, type, publisher, status, featured, priority, expiresAt, viewCount, reviewNote.

- [ ] **Step 2: Add admin actions**

Actions: approve, reject, feature, unfeature, block. Use a small dialog for review note and featured priority.

- [ ] **Step 3: Build frontend**

Run: `npm --prefix frontend run build`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/AdminOperationsView.vue frontend/src/styles.css
git commit -m "add project ad operations monitor"
```

## Task 9: Documentation and handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update README**

Document Phase 4 features, the V9 migration, project-ad workflow, and preserved boundaries: no escrow, no Alipay key handling, station notifications first, responsive Web first.

- [ ] **Step 2: Update CLAUDE handoff**

Add a concise Phase 4 handoff only after implementation and verification are complete. Include latest commit range, verification commands, known caveats, and deployment notes.

- [ ] **Step 3: Commit docs**

```bash
git add README.md CLAUDE.md docs/superpowers/specs/2026-05-21-campushub-phase4-project-ads-design.md docs/superpowers/plans/2026-05-21-campushub-phase4-project-ads-upgrade.md
git commit -m "document project ads phase 4 upgrade"
```

## Task 10: Verification checkpoint

**Files:**
- No source edits unless verification reveals issues.

- [ ] **Step 1: Run frontend build**

Run: `npm --prefix frontend run build`

Expected: PASS with existing Element Plus chunk-size warning only.

- [ ] **Step 2: Run backend verification**

If Maven is available locally, run:

```bash
mvn -f backend/pom.xml -Dtest=ProjectAdServiceIntegrationTest test
```

Then, if time permits:

```bash
mvn -f backend/pom.xml test
```

Expected: PASS. If local Maven is unavailable, record that backend verification must run through low-impact Docker build/server build before deploy.

- [ ] **Step 3: Inspect git status**

Run: `git status --short --branch`.

Expected: clean worktree after commits, on Phase 4 branch/worktree.

- [ ] **Step 4: Deployment handoff**

Do not deploy automatically unless explicitly requested. Recommended production sequence after push/merge:

```bash
cd /opt/campushub
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build campushub-backend campushub-web
```

Then low-impact smoke endpoints:

```bash
curl -sS http://127.0.0.1:18080/api/project-ads
curl -sS http://127.0.0.1:18080/api/project-ads/featured
curl -sS http://127.0.0.1:18080/api/admin/ops/project-ads
```

Expected: no HTTP 500, JSON API responses.

## Self-review

- Spec coverage: This plan covers project posts, review, contact visibility, expiration, tags, favorites/comments via generic interactions, featured slots, detail/showcase pages, publisher management, and operations visibility.
- Placeholder scan: No TBD/TODO placeholders remain. Where optional attachment/count integration depends on existing service interfaces, the plan gives a concrete fallback that keeps scope testable.
- Type consistency: Backend DTO, service, API, and frontend function names consistently use `ProjectAd`, `ProjectAdRequest`, `ProjectAdReviewRequest`, and `ProjectAdDetailSummary`.
- Scope check: The plan is one coherent subsystem and can be implemented independently after Phase 1-3.
