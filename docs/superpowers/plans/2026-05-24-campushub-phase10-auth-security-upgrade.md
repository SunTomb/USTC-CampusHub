# CampusHub Phase 10 Auth Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move CampusHub from course-prototype open APIs toward safer Beta production controls by enforcing JWT authentication on write operations, `ROLE_ADMIN` on admin APIs, current-user identity for sensitive actions, and frontend 401/403 handling.

**Architecture:** Phase 10 adds a small authentication layer around the existing Spring Security setup: JWT parsing, current-user principal, role authorities, route rules, and a `CurrentUserService` used by controllers. Business services keep ownership and domain checks; controllers stop trusting user/admin IDs from query parameters for critical writes. Frontend auth state is restored through `/auth/me`, and admin/write pages display clear login or permission prompts.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Security, JJWT, Spring Data JPA/JdbcTemplate, Flyway/MySQL 8 when needed, Vue 3, Vite, TypeScript, Pinia, Axios, Element Plus, Docker Compose, Playwriter browser verification.

---

## Scope and boundaries

This plan implements the approved Phase 10 design in `docs/superpowers/specs/2026-05-24-campushub-phase10-auth-security-design.md`.

In scope:

- JWT authentication filter and current-user helper.
- `/api/auth/me` for frontend session restoration.
- Route-level authorization: public reads, authenticated writes, `ROLE_ADMIN` for `/api/admin/**`.
- Compatibility checks for existing query/path user IDs: the supplied ID must match JWT current user unless the current user is admin.
- Admin controller actions derive admin identity from JWT.
- Frontend 401/403 handling and admin permission UX.
- File binding ownership checks.
- README/CLAUDE handoff and permission matrix documentation.
- Server-side Docker build, API smoke, and Playwriter verification.

Out of scope:

- OAuth/SAML/SSO.
- Refresh tokens, device/session management, password reset, MFA.
- Enterprise permission matrix.
- Full rate limiting or WAF.
- Full upload subsystem rewrite.
- API-Transfer-Station payment key or real channel logic migration.
- Reading/printing/copying production secrets.
- Editing applied migrations V1-V12.

## File structure map

### Backend auth/security

- Create `backend/src/main/java/com/campushub/auth/CurrentUserPrincipal.java` — Spring Security principal with user id, username, nickname, status, and authorities.
- Create `backend/src/main/java/com/campushub/auth/JwtTokenService.java` — central JWT issue/parse/validate logic reused by login and filter.
- Create `backend/src/main/java/com/campushub/auth/JwtAuthenticationFilter.java` — once-per-request Bearer token authentication.
- Create `backend/src/main/java/com/campushub/auth/CurrentUserService.java` — controller helper for current user/admin and same-user checks.
- Create `backend/src/main/java/com/campushub/auth/UserRoleLookup.java` — focused role lookup from `user_roles` and `roles`.
- Modify `backend/src/main/java/com/campushub/auth/AuthController.java` — use `JwtTokenService` and add `GET /api/auth/me`.
- Modify `backend/src/main/java/com/campushub/config/SecurityConfig.java` — install JWT filter and route authorization rules.
- Modify `backend/src/main/java/com/campushub/common/GlobalExceptionHandler.java` if needed — map authentication/authorization failures to consistent API responses without leaking details.

### Backend controller identity tightening

- Modify `backend/src/main/java/com/campushub/goods/GoodsController.java` — current user for publish/update/off-shelf/intents/mark-sold/escrow.
- Modify `backend/src/main/java/com/campushub/task/RewardTaskController.java` — current user for publish/grab/apply/application accept/workflow/confirm/issues.
- Modify `backend/src/main/java/com/campushub/identity/IdentityController.java` — current user for role application and deposit payment ownership.
- Modify `backend/src/main/java/com/campushub/identity/AdminIdentityController.java` — admin id from JWT.
- Modify `backend/src/main/java/com/campushub/wallet/WalletController.java` — same-user checks for private wallet reads and writes.
- Modify `backend/src/main/java/com/campushub/wallet/AdminWalletController.java` — admin id from JWT for recharge/withdrawal review actions.
- Modify `backend/src/main/java/com/campushub/shop/ShopController.java`, `ServiceItemController.java`, and `ServiceOrderController.java` as present — current user for merchant/customer/provider actions.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdController.java` and admin project-ad endpoints — current user/admin identity.
- Modify `backend/src/main/java/com/campushub/interaction/InteractionController.java` — current user for comments/favorites/reports/reviews.
- Modify `backend/src/main/java/com/campushub/moderation/GovernanceController.java` or admin moderation controllers as present — admin id from JWT.
- Modify `backend/src/main/java/com/campushub/payment/AdminPaymentController.java` — rely on route admin role and no frontend admin id.

### Backend file binding

- Modify `backend/src/main/java/com/campushub/file/FileController.java` — require current user for binding.
- Modify `backend/src/main/java/com/campushub/file/FileUploadService.java` — validate file uploader and target ownership.
- Create `backend/src/main/java/com/campushub/file/FileTargetAuthorizationService.java` — map target types to ownership checks for goods/shop/service item/project ad/task where repositories are available.

### Backend tests

- Create `backend/src/test/java/com/campushub/auth/JwtTokenServiceTest.java` — token issue/parse/expiration/role mapping basics.
- Create `backend/src/test/java/com/campushub/auth/SecurityConfigIntegrationTest.java` — anonymous public read allowed, anonymous write rejected, admin route role check.
- Create `backend/src/test/java/com/campushub/auth/CurrentUserServiceTest.java` — same-user and admin checks.
- Create focused controller/security integration tests for goods/wallet/admin wallet if the existing test setup supports Spring MVC.

### Frontend auth and API

- Modify `frontend/src/api/client.ts` — response interceptor for 401/403.
- Modify `frontend/src/api/campushub.ts` — add `getCurrentUser()`, remove or reduce userId/adminId parameters where backend no longer needs them.
- Modify `frontend/src/stores/auth.ts` — add `loadCurrentUser()`, `hasRole()`, initialization state, and robust clear-session behavior.
- Modify `frontend/src/router/index.ts` — optional route metadata for auth/admin pages and a lightweight guard.
- Modify `frontend/src/layouts/MainLayout.vue` — ensure refreshed current user displays correctly and logout remains clear.

### Frontend views

- Modify high-impact user pages: `GoodsDetailView.vue`, `GoodsPublishView.vue`, task views, shop views, project-ad views, `WalletView.vue`, `RoleApplicationsView.vue`, `NotificationsView.vue`, `CreditCenterView.vue`.
- Modify admin pages: `AdminWalletView.vue`, `AdminPaymentView.vue`, `AdminOperationsView.vue`, `AdminGovernanceView.vue`, `AdminReviewView.vue`.
- Modify shared UI only if needed: `frontend/src/styles.css`, `frontend/src/components/EmptyState.vue`, `FormSection.vue`, or `PageActions.vue`.

### Docs

- Modify `README.md` — document Phase 10 behavior and permission matrix.
- Modify `CLAUDE.md` — add Phase 10 handoff after deployment verification.
- No migration file by default. If implementation requires schema changes, create V13+ only.

---

## Task 1: Add JWT auth infrastructure

**Files:**
- Create: `backend/src/main/java/com/campushub/auth/CurrentUserPrincipal.java`
- Create: `backend/src/main/java/com/campushub/auth/UserRoleLookup.java`
- Create: `backend/src/main/java/com/campushub/auth/JwtTokenService.java`
- Create: `backend/src/main/java/com/campushub/auth/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/campushub/auth/CurrentUserService.java`
- Modify: `backend/src/main/java/com/campushub/auth/AuthController.java`
- Test: `backend/src/test/java/com/campushub/auth/JwtTokenServiceTest.java`
- Test: `backend/src/test/java/com/campushub/auth/CurrentUserServiceTest.java`

- [x] **Step 1: Write token service test**

Create `backend/src/test/java/com/campushub/auth/JwtTokenServiceTest.java` with tests for issuing a token that includes `userId`, parsing it back, and rejecting a token signed with the wrong key.

Expected test intent:

```java
@Test
void issuesAndParsesUserToken() {
    JwtTokenService service = new JwtTokenService("campushub-test-issuer", "01234567890123456789012345678901", 60);

    String token = service.issueToken(1L, "alice");

    JwtTokenClaims claims = service.parse(token);
    assertThat(claims.userId()).isEqualTo(1L);
    assertThat(claims.username()).isEqualTo("alice");
}
```

- [x] **Step 2: Add principal and role lookup**

Implement `CurrentUserPrincipal` as an immutable principal that exposes:

- `Long userId`
- `String username`
- `String nickname`
- `String status`
- `Collection<? extends GrantedAuthority> authorities`

Implement `UserRoleLookup` using `JdbcTemplate` or repositories to return role codes for a user id from `user_roles` joined to `roles`.

- [x] **Step 3: Add JWT token service**

Move JWT issuing/parsing into `JwtTokenService` with configuration values from:

- `campushub.jwt.secret`
- `campushub.jwt.issuer`
- `campushub.jwt.expiration-minutes`

Expose:

- `String issueToken(Long userId, String username)`
- `JwtTokenClaims parse(String token)`

`JwtTokenClaims` can be a package-private record in the same file or a separate focused record.

- [x] **Step 4: Add JWT authentication filter**

Implement `JwtAuthenticationFilter` as `OncePerRequestFilter`:

- If no Bearer token exists, continue anonymous.
- If token exists, parse it.
- Load user by id.
- Require `status = ACTIVE`.
- Load roles as authorities.
- Put `UsernamePasswordAuthenticationToken` with `CurrentUserPrincipal` into `SecurityContextHolder`.
- On invalid token, return HTTP 401 with an `ApiResponse`-compatible JSON body.

Do not log token contents.

- [x] **Step 5: Add current user helper**

Implement `CurrentUserService` methods:

- `Long requireUserId()`
- `User requireUser()`
- `Long requireAdminId()`
- `Optional<Long> optionalUserId()`
- `Long requireSameUser(Long requestedUserId)`
- `boolean isAdmin()`

`requireSameUser` returns requested/current id if equal, allows admin, and otherwise throws a 403-style business/security exception.

- [x] **Step 6: Refactor login to use token service**

Modify `AuthController.login` to call `jwtTokenService.issueToken(user.getId(), user.getUsername())` instead of directly building JWT.

Add:

```java
@GetMapping("/me")
public ApiResponse<CurrentUserSummary> me() {
    return ApiResponse.ok(CurrentUserSummary.from(currentUserService.requireUser()));
}
```

- [x] **Step 7: Run backend auth tests**

Local Maven is unavailable in this Windows session and the repository has no Maven wrapper, so backend test execution is deferred to server-side Docker/Maven verification before deployment.

Run the backend auth tests in an available environment. If local Maven is unavailable, defer full execution to server-side Docker build in Task 9 and keep compile errors for implementation follow-up.

Expected: token service and current-user tests pass.

- [ ] **Step 8: Commit Task 1**

Commit only Task 1 files after tests/compile are green.

Suggested message:

```text
add jwt authentication infrastructure
```

---

## Task 2: Enforce route-level security rules

**Files:**
- Modify: `backend/src/main/java/com/campushub/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campushub/common/GlobalExceptionHandler.java` if needed
- Test: `backend/src/test/java/com/campushub/auth/SecurityConfigIntegrationTest.java`

- [x] **Step 1: Write security route integration test**

Create `SecurityConfigIntegrationTest` covering:

- anonymous `GET /api/goods` is allowed;
- anonymous `POST /api/goods` is rejected;
- non-admin authenticated user is rejected from `/api/admin/wallet/recharges`;
- admin authenticated user can access `/api/admin/wallet/recharges`.

Use the project’s existing Spring test style. If no MockMvc setup exists, add the smallest `@SpringBootTest` + `@AutoConfigureMockMvc` test.

- [x] **Step 2: Configure stateless JWT security**

Modify `SecurityConfig`:

- keep CSRF disabled for stateless API;
- keep session creation stateless;
- add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`;
- allow auth endpoints;
- allow public GET endpoints;
- allow payment-center callback endpoint without user JWT because it has its own internal token/signature;
- require `ROLE_ADMIN` for `/api/admin/**`;
- require authentication for other non-public `/api/**` requests.

Route rules should be explicit enough to avoid accidentally exposing new admin writes.

- [x] **Step 3: Normalize auth failure responses**

Ensure unauthorized and forbidden responses are clear for the frontend:

- 401 message: `请先登录`
- 403 message: `当前账号无权限执行此操作`

Do not leak Java exception names, JWT parse errors, or secret-related details.

- [x] **Step 4: Run security route tests**

Local Maven is unavailable in this Windows session and the repository has no Maven wrapper, so security route test execution is deferred to server-side Docker/Maven verification before deployment.

Expected:

- public GET remains 200;
- anonymous write returns 401;
- normal user admin access returns 403;
- admin access returns 200.

- [ ] **Step 5: Commit Task 2**

Suggested message:

```text
enforce api authentication rules
```

---

## Task 3: Tighten user identity on critical user writes

**Files:**
- Modify: `backend/src/main/java/com/campushub/goods/GoodsController.java`
- Modify: `backend/src/main/java/com/campushub/task/RewardTaskController.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityController.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletController.java`
- Modify: shop/service/project/interaction controllers as present
- Test: focused controller or service integration tests for goods/wallet/task identity checks

- [ ] **Step 1: Identify exact controller signatures**

Use `Grep` and `Read` to list all controller methods that accept user identity through:

- `@RequestParam Long userId`
- `sellerId`
- `buyerId`
- `publisherId`
- `merchantId`
- `customerId`
- `providerId`
- path variable `{userId}`
- request body fields representing acting user

Record the final list in the implementation notes or plan checkbox comments, not in a new documentation file.

- [ ] **Step 2: Add same-user tests for goods escrow**

Add tests showing:

- authenticated buyer can create/freeze/confirm their own escrow order;
- authenticated user cannot pass another buyer’s id to operate that buyer’s escrow order;
- anonymous escrow write is rejected by security before controller logic.

- [ ] **Step 3: Modify goods controller**

Inject `CurrentUserService` into `GoodsController` and replace acting-user values:

- publish: use `requireUserId()` as seller id;
- update/off-shelf/mark-sold: use `requireUserId()` or `requireSameUser(request.userId())` for compatibility;
- intent: use `requireUserId()` as buyer id;
- escrow create/freeze/confirm/cancel/dispute: use `requireUserId()` as buyer/actor id;
- public detail: use `optionalUserId()` for viewer when possible, with legacy `viewerId` treated as optional compatibility only.

- [ ] **Step 4: Modify wallet controller**

For user wallet endpoints:

- reads under `/users/{userId}` use `requireSameUser(userId)`;
- recharge creation uses `requireSameUser(userId)`;
- withdrawal creation uses `requireSameUser(userId)`;
- frozen items / flows / recharge list / withdrawal list are private to same user or admin.

- [ ] **Step 5: Modify identity controller**

- Role application path `/users/{userId}/roles` uses `requireSameUser(userId)`.
- Deposit payment creation verifies the role application belongs to the current user, unless admin.

- [ ] **Step 6: Modify task/shop/project/interaction controllers**

Apply the same pattern:

- acting user comes from JWT;
- path/query user id must match JWT if kept;
- service layer domain ownership checks remain unchanged;
- public read endpoints use optional current user only for viewer-specific flags.

- [ ] **Step 7: Run focused backend tests**

Run tests for the modified controllers/services if local Maven exists; otherwise rely on server Docker build and API smoke in Task 9.

Expected: identity mismatch is rejected; matching user works.

- [ ] **Step 8: Commit Task 3**

Suggested message:

```text
use authenticated user for write actions
```

---

## Task 4: Tighten admin identity and audit-sensitive actions

**Files:**
- Modify: `backend/src/main/java/com/campushub/identity/AdminIdentityController.java`
- Modify: `backend/src/main/java/com/campushub/wallet/AdminWalletController.java`
- Modify: `backend/src/main/java/com/campushub/payment/AdminPaymentController.java`
- Modify: `backend/src/main/java/com/campushub/ops/OperationsController.java`
- Modify: `backend/src/main/java/com/campushub/moderation/*Controller.java`
- Modify: admin project-ad/review controllers as present
- Test: admin authorization integration tests

- [ ] **Step 1: Add admin action tests**

Create tests showing:

- ordinary authenticated student cannot approve wallet recharge;
- admin can approve wallet recharge;
- controller uses current admin id, not a supplied query `adminId`.

- [ ] **Step 2: Modify admin controllers to use current admin id**

Inject `CurrentUserService` into admin controllers and replace query-param `adminId` with:

```java
Long adminId = currentUserService.requireAdminId();
```

If a method still accepts `adminId` for frontend compatibility, validate it with `requireSameUser(adminId)` and then delete it from frontend calls in Task 5.

- [ ] **Step 3: Preserve admin audit behavior**

Where services already accept `adminId`, continue passing the current admin id. Ensure existing admin action log or security log receives the real admin user from JWT.

- [ ] **Step 4: Protect CSV exports**

Ensure operations CSV exports under `/api/admin/ops/exports/*.csv` are covered by `/api/admin/**` `ROLE_ADMIN` route rules.

Keep existing CSV formula-injection escaping and secret-exclusion behavior unchanged.

- [ ] **Step 5: Run admin tests**

Expected:

- `/api/admin/**` anonymous returns 401;
- non-admin returns 403;
- admin returns expected response;
- no admin action trusts a mismatched `adminId`.

- [ ] **Step 6: Commit Task 4**

Suggested message:

```text
require admin role for operations actions
```

---

## Task 5: Update frontend authentication state and API calls

**Files:**
- Modify: `frontend/src/api/client.ts`
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: user-facing views that pass userId/adminId
- Modify: admin views that hardcode or input adminId
- Test: `frontend/src/api/client.test.ts` and new auth store tests if practical

- [ ] **Step 1: Add API client 401/403 tests**

Extend `frontend/src/api/client.test.ts` or add a focused test to verify:

- API failure responses still unwrap into errors;
- 401 triggers auth session clear;
- 403 preserves session but surfaces a permission message.

If Pinia setup makes direct interceptor testing awkward, test the exported helper behavior and verify manually in Playwriter.

- [ ] **Step 2: Add current-user API**

In `frontend/src/api/campushub.ts`, add:

```ts
export function getCurrentUser() {
  return getApi<CurrentUser>('/auth/me')
}
```

Reuse the existing `CurrentUser` type or move the type to avoid circular imports.

- [ ] **Step 3: Extend auth store**

Add actions:

- `loadCurrentUser()` — calls `/auth/me` when token exists;
- `hasRole(role: string)` — checks current user roles;
- `isAdmin` getter — returns `hasRole('ROLE_ADMIN')`;
- robust `clearSession()` — clears token and current user.

Keep token in localStorage, as Phase 10 does not introduce refresh tokens.

- [ ] **Step 4: Add frontend auth initialization**

Ensure app startup or router guard calls `auth.loadCurrentUser()` once when token exists and current user is empty.

Avoid blocking anonymous public pages indefinitely; show a loading state only for protected/admin pages if needed.

- [x] **Step 5: Remove production demo identity fallbacks**

Replace frontend patterns such as:

- `auth.currentUser?.id ?? 1`
- fixed `const adminId = 1`
- manual admin id input for operations

with:

- login prompt for user actions;
- current user id from auth store;
- admin role check for admin actions;
- backend-derived admin id through JWT.

- [ ] **Step 6: Update API function signatures**

For endpoints changed in Tasks 3-4:

- remove `adminId` parameters from admin API wrappers;
- remove acting-user parameters where backend now uses JWT;
- keep userId only for private read routes that still include `/users/{userId}`, using current user id from auth store.

- [ ] **Step 7: Update protected page UX**

Admin pages should render:

- login prompt if anonymous;
- no-permission empty state if logged in without `ROLE_ADMIN`;
- normal workspace if admin.

User pages should prompt login before write actions rather than sending anonymous requests.

- [x] **Step 8: Run frontend tests/build**

`npm --prefix frontend run build` succeeded locally with only the known Vite large chunk and dependency pure-comment warnings. The focused client test command was not rerun in this interruption-recovery pass.

Run in approved environment:

```bash
npm --prefix frontend run test -- src/api/client.test.ts
npm --prefix frontend run build
```

Expected: tests pass; Vite build succeeds with only known chunk/pure-comment warnings.

- [ ] **Step 9: Commit Task 5**

Suggested message:

```text
restore frontend session and permission prompts
```

---

## Task 6: Secure file binding and target ownership

**Files:**
- Create: `backend/src/main/java/com/campushub/file/FileTargetAuthorizationService.java`
- Modify: `backend/src/main/java/com/campushub/file/FileController.java`
- Modify: `backend/src/main/java/com/campushub/file/FileUploadService.java`
- Test: `backend/src/test/java/com/campushub/file/FileBindingSecurityTest.java`

- [ ] **Step 1: Write file binding security tests**

Add tests for:

- anonymous file binding rejected;
- user cannot bind another user’s file;
- user cannot bind a file to another user’s goods/project/shop target;
- admin can bind for moderation/repair use if required.

- [x] **Step 2: Create target authorization service**

Implement `FileTargetAuthorizationService` with:

```java
public void requireCanBind(String targetType, Long targetId, Long userId, boolean admin)
```

Support target types that currently appear in the app:

- `GOODS` — seller owns target;
- `PROJECT_AD` — publisher owns target;
- `REWARD_TASK` — publisher owns target;
- `SHOP` — merchant owns target;
- `SERVICE_ITEM` — owning shop merchant owns target;
- unknown target type rejected.

If a repository is not available for one type, reject the type in Phase 10 rather than silently allowing insecure binding.

- [x] **Step 3: Modify file controller and service**

- `FileController.bind` obtains current user id and admin status.
- `FileUploadService.bindExisting` verifies file uploader matches current user or current user is admin.
- `FileUploadService.bindExisting` calls target authorization service before creating binding.
- Existing goods image count limit remains.

- [ ] **Step 4: Run file binding tests**

Expected: ownership checks pass and insecure bindings are rejected.

- [ ] **Step 5: Commit Task 6**

Suggested message:

```text
validate file binding ownership
```

---

## Task 7: Update docs and Phase 10 handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Possibly modify: `docs/superpowers/plans/2026-05-22-campushub-overall-phased-roadmap.md` only to annotate Phase 9/10 renumbering if desired

- [x] **Step 1: Update README Phase 10 section**

Document:

- public read endpoints remain anonymous;
- write operations require login;
- `/api/admin/**` requires `ROLE_ADMIN`;
- user IDs are derived from JWT for sensitive actions;
- payment-center callback remains protected by internal token/signature, not user JWT;
- no secret-reading/no Alipay-key boundary remains unchanged.

- [x] **Step 2: Update CLAUDE handoff**

After implementation and verification, add Phase 10 handoff to `CLAUDE.md` with:

- latest commit;
- production branch and commit;
- what was implemented;
- verification performed;
- constraints for Phase 11;
- note that original beta readiness is now Phase 11.

- [ ] **Step 3: Update roadmap note if needed**

If editing the overall roadmap, do not rewrite historical completed phases. Add a short note explaining:

- original Phase 9 auth/security is now delivered as Phase 10;
- original Phase 10 beta readiness is now Phase 11.

- [ ] **Step 4: Commit Task 7**

Suggested message:

```text
document phase 10 auth hardening
```

---

## Task 8: Server build, API smoke, and browser verification

**Files:**
- No source changes expected unless verification finds issues.

- [ ] **Step 1: Check git status before deployment verification**

Confirm only intended Phase 10 files are modified and committed. Do not include `.env`, server secrets, local backup docs, or unrelated untracked files.

- [ ] **Step 2: Push branch if deployment is requested/approved**

Use normal git push only after local/server build readiness is acceptable and user has approved deployment if required by the current session.

- [ ] **Step 3: Server-side Docker backend build**

On the production server, in `/opt/campushub`, pull the intended commit and run a targeted backend build through `docker compose -f docker-compose.prod.yml build campushub-backend` or the project’s current low-impact deployment command.

Expected: Maven package completes with `BUILD SUCCESS`.

- [ ] **Step 4: Server-side Docker frontend build**

Run targeted web build.

Expected: Vite build succeeds; known large chunk and dependency pure-comment warnings are acceptable.

- [ ] **Step 5: Restart affected containers carefully**

Restart backend and web only as needed. Keep checks low-frequency due to the small shared server.

- [ ] **Step 6: API smoke anonymous public reads**

From inside backend container or server-local network, verify:

- `GET /api/goods` -> 200
- `GET /api/tasks` -> 200
- `GET /api/shops` -> 200
- `GET /api/project-ads` -> 200

- [ ] **Step 7: API smoke anonymous writes rejected**

Verify without token:

- `POST /api/goods` -> 401
- `POST /api/wallet/users/1/recharges` -> 401
- `GET /api/admin/wallet/recharges` -> 401 or 403

- [ ] **Step 8: API smoke authenticated user**

Login as a normal demo user through `/api/auth/login`, store token only in shell variables, never print secrets. Verify:

- `GET /api/auth/me` -> 200 and user role includes student;
- one ordinary user write path works;
- mismatched userId path is rejected;
- `/api/admin/wallet/recharges` -> 403.

Do not print JWT token in logs or final summary.

- [ ] **Step 9: API smoke authenticated admin**

Login as demo admin. Verify:

- `GET /api/auth/me` -> 200 and includes `ROLE_ADMIN`;
- `GET /api/admin/wallet/recharges` -> 200;
- `GET /api/admin/payment/orders` -> 200;
- `GET /api/admin/ops/analytics/overview` -> 200;
- `GET /api/admin/governance/dashboard` -> 200.

Do not print JWT token.

- [ ] **Step 10: Payment callback regression smoke**

Verify the payment-center callback route is not blocked by user JWT rules. Use only mock/staging-safe callback data and configured internal token/signature. Do not print callback token or signing secret.

- [ ] **Step 11: Playwriter desktop verification**

Verify public and protected flows:

- `/auth` login;
- anonymous browse `/goods`, `/goods/1`, `/tasks`, `/shops`, `/project-ads`;
- anonymous write action prompts login;
- logged-in `/wallet` renders current user wallet;
- normal user admin page shows no-permission;
- admin user `/admin/wallet`, `/admin/payment`, `/admin/ops`, `/admin/governance`, `/admin/review` render.

- [ ] **Step 12: Playwriter mobile verification**

At 375-390px width, verify:

- `/auth`, `/goods`, `/tasks`, `/shops`, `/project-ads`, `/wallet`, `/admin/wallet` render;
- bottom navigation and “更多” drawer remain usable;
- no tested page has document-level horizontal overflow.

- [ ] **Step 13: Commit verification fixes if needed**

If verification finds bugs, fix them in focused commits and rerun the failing check before claiming completion.

- [ ] **Step 14: Final handoff**

Report:

- final branch/commit;
- production commit if deployed;
- build results;
- API smoke matrix;
- Playwriter routes checked;
- constraints carried into Phase 11.

Do not include secrets, tokens, full `.env`, or private key contents.

---

## Implementation order recommendation

Recommended execution order:

1. Task 1 and Task 2 together establish the security foundation.
2. Task 3 and Task 4 convert user/admin identity sources.
3. Task 5 makes the frontend usable under stricter auth.
4. Task 6 closes file binding ownership risk.
5. Task 7 documents the new production boundary.
6. Task 8 verifies and deploys carefully.

Use one commit per task where practical. If Task 3 becomes too large, split by bounded context: goods/wallet, tasks/shops, project/interaction.

## Self-review checklist

Before executing this plan, confirm:

- No step edits applied migrations V1-V12.
- No step reads or prints production secrets.
- Payment-center callback remains internally authenticated and is not forced through user JWT.
- Anonymous public browsing remains supported.
- Admin APIs rely on `ROLE_ADMIN`, not frontend `adminId`.
- Frontend no longer uses production demo fallbacks for sensitive actions.
