# CampusHub Phase 14 Real Feedback Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix real-user blockers around login/session and identity deposit payment, then add master/trade/showcase admin roles with domain management workspaces.

**Architecture:** Implement Phase 14 in two deployable slices. Slice 1 changes auth defaults, frontend 401 behavior, recoverable role applications, and wallet role-deposit payment. Slice 2 adds V13 roles, centralized domain-admin authorization, admin role applications, backend domain admin endpoints, frontend domain admin navigation/pages, and documentation/handoff updates.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, MySQL, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vitest, Docker Compose production verification.

---

## File structure map

### Backend auth/session

- Modify `backend/src/main/resources/application.yml`
  - Change default `campushub.jwt.expiration-minutes` from `720` to `43200`.
- Modify `backend/src/main/resources/application-prod.yml`
  - Change production default fallback from `720` to `43200`, while preserving env override.
- Modify `frontend/src/api/client.ts`
  - Keep an opt-in `skipAuthExpireHandling` request flag and stop clearing the whole session for every 401.
- Modify `frontend/src/stores/auth.ts`
  - Use quiet `/auth/me` restore behavior and expose a controlled invalid-session handler.
- Modify `frontend/src/router/index.ts`
  - Ensure protected route redirects are controlled and do not display surprise expiry toasts.

### Backend identity/payment

- Modify `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`
  - Add `TRADE_ADMIN` and `SHOWCASE_ADMIN` with zero deposit and manual review.
  - Add helper methods such as `requiresDeposit()` and `grantedRoleCode()`.
- Modify `backend/src/main/java/com/campushub/identity/RoleApplication.java`
  - Add state helpers for recoverable unpaid applications and wallet-paid deposit flow.
- Modify `backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java`
  - Add `findByUserIdOrderByCreatedAtDesc`.
  - Add `findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc` for master admin review.
- Modify `backend/src/main/java/com/campushub/identity/IdentityService.java`
  - Return existing unpaid application instead of throwing.
  - Approve by assigning user roles through a new role assignment helper.
  - Restrict admin-role approval to master.
- Create `backend/src/main/java/com/campushub/auth/UserRoleService.java`
  - Centralize role lookup, role existence, and role assignment.
- Modify `backend/src/main/java/com/campushub/payment/PaymentService.java`
  - Add `payRoleDepositWithWallet(Long applicationId, Long payerId)`.
  - Use `UserRoleService` after role deposit is marked paid or approved.
- Modify `backend/src/main/java/com/campushub/identity/IdentityController.java`
  - Add `GET /api/identity/users/{userId}/roles`.
  - Add `POST /api/identity/roles/{applicationId}/deposit-wallet-pay`.

### Backend domain admin

- Add migration `backend/src/main/resources/db/migration/V13__phase14_domain_admin_roles.sql`
  - Insert role rows for `ROLE_MASTER_ADMIN`, `ROLE_TRADE_ADMIN`, `ROLE_SHOWCASE_ADMIN` if missing.
  - Grant `ROLE_MASTER_ADMIN` and `ROLE_ADMIN` to user matching `yeshenghao@mail.ustc.edu.cn` or username `yeshenghao` if present.
- Modify `backend/src/main/java/com/campushub/auth/CurrentUserService.java`
  - Add `requireMasterAdminId`, `requireTradeAdminId`, `requireShowcaseAdminId`, and role predicates.
  - Treat `ROLE_MASTER_ADMIN` as all admin domains.
- Modify `backend/src/main/java/com/campushub/config/SecurityConfig.java`
  - Permit `/api/admin/**` for `ROLE_ADMIN`, `ROLE_MASTER_ADMIN`, `ROLE_TRADE_ADMIN`, or `ROLE_SHOWCASE_ADMIN`; service methods enforce precise domain rules.
- Create `backend/src/main/java/com/campushub/admin/AdminActionRequest.java`
  - Small record carrying `note` and optional `reason`.
- Create `backend/src/main/java/com/campushub/admin/TradeAdminController.java`
  - Base path `/api/admin/trade`.
  - Endpoints for tasks, goods, and trade disputes.
- Create `backend/src/main/java/com/campushub/admin/ShowcaseAdminController.java`
  - Base path `/api/admin/showcase`.
  - Endpoints for project ads, shops, service items, shop orders, and shop merchant applications.
- Create `backend/src/main/java/com/campushub/admin/MasterAdminController.java`
  - Base path `/api/admin/master`.
  - Endpoints for admin role applications approval/rejection.
- Modify `backend/src/main/java/com/campushub/goods/GoodsService.java`
  - Add admin `adminOffShelfGoods(Long goodsId, Long adminId, String note)`.
- Modify `backend/src/main/java/com/campushub/task/RunnerTaskService.java`
  - Add admin `adminCloseTask(Long taskId, Long adminId, String note)`.
- Modify `backend/src/main/java/com/campushub/shop/Shop.java`
  - New shops start as `PENDING_REVIEW`, not `APPROVED`.
  - Add `approve`, `reject`, and admin status helpers.
- Modify `backend/src/main/java/com/campushub/shop/ShopService.java`
  - Add admin shop review/status and service-item status methods.

### Frontend identity/admin

- Modify `frontend/src/utils/identity.ts`
  - Add identity keys `masterAdmin`, `tradeAdmin`, `showcaseAdmin`.
  - Make `admin` capability true for any admin role.
- Modify `frontend/src/config/navigation.ts`
  - Remove old visible `/admin/review` item.
  - Add `/admin/trade`, `/admin/trade/disputes`, `/admin/showcase`, `/admin/showcase/disputes`, `/admin/master/admin-applications`.
  - Gate pages by the new identities.
- Modify `frontend/src/router/index.ts`
  - Add new routes and role checks.
- Modify `frontend/src/api/campushub.ts`
  - Add identity list, wallet deposit payment, trade admin, showcase admin, and master admin API helpers.
- Modify `frontend/src/views/RoleApplicationsView.vue`
  - Load existing role applications.
  - Add admin identity application cards.
  - Offer Alipay and balance payment buttons where a deposit is required.
- Modify `frontend/src/views/WalletView.vue`
  - Make auth-required failures controlled, not surprise logout.
- Modify `frontend/src/views/ProjectAdManageView.vue`
  - Make submitted/rejected/approved states and review notes visible.
- Create `frontend/src/views/AdminTradeView.vue`
  - Tabs: 跑腿管理, 商品管理.
- Create `frontend/src/views/AdminTradeDisputesView.vue`
  - Tabs/lists for task issues and goods escrow/order disputes.
- Create `frontend/src/views/AdminShowcaseView.vue`
  - Tabs: 广告管理, 商店管理, 商家身份审核.
- Create `frontend/src/views/AdminShowcaseDisputesView.vue`
  - Service order dispute/status management.
- Create `frontend/src/views/AdminApplicationsView.vue`
  - Master-only admin role application review.

### Tests/docs

- Modify `frontend/src/api/client.test.ts`
  - Add 401 handling tests.
- Modify `frontend/src/utils/identity.test.ts`
  - Add new role mapping tests.
- Modify `frontend/src/config/navigation.test.ts`
  - Add new admin navigation visibility tests.
- Add or modify backend tests under `backend/src/test/java/com/campushub/identity/` and `backend/src/test/java/com/campushub/auth/`.
- Modify `README.md` and `CLAUDE.md` after implementation and verification.

---

## Deployment slice 1: login/session and identity deposit fixes

### Task 1: Long-lived login defaults and frontend 401 behavior

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-prod.yml`
- Modify: `frontend/src/api/client.ts`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/router/index.ts`
- Test: `frontend/src/api/client.test.ts`

- [ ] **Step 1: Write the frontend 401 handling tests**

Append tests in `frontend/src/api/client.test.ts` that verify the auth store is not cleared when a request opts out of global expiry handling, and is cleared for normal authenticated requests.

```ts
import { describe, expect, it, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { apiClient } from './client'

vi.mock('axios', async () => {
  const actual = await vi.importActual<typeof import('axios')>('axios')
  const interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  }
  return {
    ...actual,
    default: {
      create: vi.fn(() => ({
        interceptors,
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
      })),
    },
  }
})

const clearSession = vi.fn()
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    token: 'token',
    clearSession,
  }),
}))

describe('apiClient auth failure handling', () => {
  beforeEach(() => {
    clearSession.mockClear()
  })

  it('does not clear session for requests with skipAuthExpireHandling', async () => {
    const created = vi.mocked(axios.create).mock.results[0].value
    const errorHandler = created.interceptors.response.use.mock.calls[0][1]

    await expect(errorHandler({
      response: { status: 401 },
      config: { skipAuthExpireHandling: true },
    })).rejects.toThrow('请先登录')

    expect(clearSession).not.toHaveBeenCalled()
  })

  it('clears session for normal 401 responses', async () => {
    const created = vi.mocked(axios.create).mock.results[0].value
    const errorHandler = created.interceptors.response.use.mock.calls[0][1]

    await expect(errorHandler({
      response: { status: 401 },
      config: {},
    })).rejects.toThrow('登录已过期，请重新登录')

    expect(clearSession).toHaveBeenCalledTimes(1)
  })
})
```

If the existing test file already has a different axios mocking style, keep its setup and add equivalent assertions without duplicating imports.

- [ ] **Step 2: Run the frontend API test and verify it fails**

Run:

```bash
npm --prefix frontend run test -- src/api/client.test.ts
```

Expected before implementation: failure because `skipAuthExpireHandling` is ignored or the error message is still always `登录已过期，请重新登录`.

- [ ] **Step 3: Add request metadata typing and 401 handling in `client.ts`**

Replace `frontend/src/api/client.ts` with this implementation, preserving existing exported function names:

```ts
import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'

declare module 'axios' {
  export interface InternalAxiosRequestConfig<D = any> {
    skipAuthExpireHandling?: boolean
  }
  export interface AxiosRequestConfig<D = any> {
    skipAuthExpireHandling?: boolean
  }
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
}

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (!response.success) {
    throw new Error(response.message || '请求失败')
  }
  return response.data as T
}

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (!error.config?.skipAuthExpireHandling) {
        useAuthStore().clearSession()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      return Promise.reject(new Error(error.response?.data?.message || '请先登录'))
    }
    if (error.response?.status === 403) {
      return Promise.reject(new Error('当前账号无权限执行此操作'))
    }
    return Promise.reject(error)
  },
)

export async function getApi<T>(url: string, options?: { skipAuthExpireHandling?: boolean }): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url, options)
  return unwrapApiResponse(response.data)
}

export async function postApi<T>(url: string, body?: unknown, options?: { skipAuthExpireHandling?: boolean }): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body, options)
  return unwrapApiResponse(response.data)
}

export async function putApi<T>(url: string, body?: unknown, options?: { skipAuthExpireHandling?: boolean }): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(url, body, options)
  return unwrapApiResponse(response.data)
}
```

- [ ] **Step 4: Make `/auth/me` restore quiet in `auth.ts`**

Modify `frontend/src/stores/auth.ts` so `getCurrentUser` is called with quiet handling. Update the import and `loadCurrentUser` action:

```ts
async loadCurrentUser() {
  if (!this.token) {
    this.sessionLoaded = true
    return
  }
  try {
    this.currentUser = await getCurrentUser({ skipAuthExpireHandling: true })
  } catch {
    this.clearSession()
  } finally {
    this.sessionLoaded = true
  }
},
```

- [ ] **Step 5: Update `getCurrentUser` API helper**

Modify `frontend/src/api/campushub.ts`:

```ts
export function getCurrentUser(options?: { skipAuthExpireHandling?: boolean }) {
  return getApi<CurrentUser>('/auth/me', options)
}
```

- [ ] **Step 6: Set long-lived JWT defaults**

In `backend/src/main/resources/application.yml`, change:

```yaml
expiration-minutes: ${CAMPUSHUB_JWT_EXPIRATION_MINUTES:720}
```

to:

```yaml
expiration-minutes: ${CAMPUSHUB_JWT_EXPIRATION_MINUTES:43200}
```

Make the same fallback change in `backend/src/main/resources/application-prod.yml`.

- [ ] **Step 7: Run frontend tests and build**

Run:

```bash
npm --prefix frontend run test -- src/api/client.test.ts src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

Expected: tests pass; build succeeds with only known Vite chunk/pure-comment warnings.

- [ ] **Step 8: Commit slice 1 auth changes**

Run:

```bash
git add backend/src/main/resources/application.yml backend/src/main/resources/application-prod.yml frontend/src/api/client.ts frontend/src/api/client.test.ts frontend/src/api/campushub.ts frontend/src/stores/auth.ts
git commit -m "fix long lived login handling"
```

Do not stage unrelated untracked files.

### Task 2: Recover unpaid role applications and list existing applications

**Files:**
- Modify: `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplication.java`
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityService.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityController.java`
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/RoleApplicationsView.vue`
- Test: `backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java`

- [ ] **Step 1: Write backend integration tests for recoverable pending applications**

Create or extend `backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java`:

```java
package com.campushub.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdentityServiceIntegrationTest {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RoleApplicationRepository roleApplicationRepository;

    @Test
    void applyingAgainForPendingPaymentRoleReturnsExistingApplication() {
        RoleApplicationSummary first = identityService.apply(1L, new ApplyRoleRequest("RUNNER", "first try"));
        RoleApplicationSummary second = identityService.apply(1L, new ApplyRoleRequest("RUNNER", "second try"));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.depositStatus()).isEqualTo("PENDING");
        assertThat(second.reviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(roleApplicationRepository.findByUserIdAndRoleType(1L, "RUNNER")).isPresent();
    }

    @Test
    void listUserApplicationsReturnsExistingApplications() {
        RoleApplicationSummary application = identityService.apply(1L, new ApplyRoleRequest("GOODS_PUBLISHER", "publish goods"));

        assertThat(identityService.listUserApplications(1L))
                .extracting(RoleApplicationSummary::id)
                .contains(application.id());
    }
}
```

- [ ] **Step 2: Run the backend test where Maven is available**

Local Maven may be unavailable. If available, run:

```bash
mvn -f backend/pom.xml -Dtest=IdentityServiceIntegrationTest test
```

Expected before implementation: failure because `apply` throws `该身份已申请` and `listUserApplications` does not exist.

If local Maven is not available, keep the test and verify later through server Docker build.

- [ ] **Step 3: Add role helpers in `PlatformRoleType.java`**

Replace enum body with:

```java
public enum PlatformRoleType {
    RUNNER(new BigDecimal("5.00"), false, "ROLE_RUNNER"),
    GOODS_PUBLISHER(new BigDecimal("10.00"), false, "ROLE_GOODS_PUBLISHER"),
    SHOP_MERCHANT(new BigDecimal("20.00"), true, "ROLE_SHOP_MERCHANT");

    private final BigDecimal depositAmount;
    private final boolean manualReviewRequired;
    private final String grantedRoleCode;

    PlatformRoleType(BigDecimal depositAmount, boolean manualReviewRequired, String grantedRoleCode) {
        this.depositAmount = depositAmount;
        this.manualReviewRequired = manualReviewRequired;
        this.grantedRoleCode = grantedRoleCode;
    }

    public BigDecimal depositAmount() {
        return depositAmount;
    }

    public boolean manualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean requiresDeposit() {
        return depositAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String grantedRoleCode() {
        return grantedRoleCode;
    }
}
```

- [ ] **Step 4: Add state helpers to `RoleApplication.java`**

Add methods below getters:

```java
public boolean isRecoverableUnpaid() {
    return "PENDING".equals(depositStatus) && "PENDING_PAYMENT".equals(reviewStatus)
            || "FAILED".equals(depositStatus)
            || "EXPIRED".equals(depositStatus);
}

public boolean isTerminalOrInReview() {
    return "PAID".equals(depositStatus)
            || "APPROVED".equals(reviewStatus)
            || "PENDING_REVIEW".equals(reviewStatus);
}

public void resetForPayment(String applyNote) {
    this.depositStatus = "PENDING";
    this.reviewStatus = "PENDING_PAYMENT";
    this.applyNote = applyNote;
}
```

- [ ] **Step 5: Add repository method**

Add to `RoleApplicationRepository.java`:

```java
@EntityGraph(attributePaths = {"user", "reviewer"})
List<RoleApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
```

- [ ] **Step 6: Update `IdentityService.apply` and add list method**

Change the duplicate check in `IdentityService.apply` to:

```java
roleApplicationRepository.findByUserIdAndRoleType(userId, roleType.name())
        .ifPresent(existing -> {
            if (existing.isRecoverableUnpaid()) {
                existing.resetForPayment(request.applyNote());
                throw new RecoverExistingRoleApplicationException(existing);
            }
            throw new BusinessException("该身份已申请或已开通");
        });
```

Add private exception inside `IdentityService`:

```java
private static class RecoverExistingRoleApplicationException extends RuntimeException {
    private final RoleApplication application;

    private RecoverExistingRoleApplicationException(RoleApplication application) {
        this.application = application;
    }
}
```

Wrap the existing check in a `try/catch` so the existing entity can be returned:

```java
try {
    roleApplicationRepository.findByUserIdAndRoleType(userId, roleType.name())
            .ifPresent(existing -> {
                if (existing.isRecoverableUnpaid()) {
                    existing.resetForPayment(request.applyNote());
                    throw new RecoverExistingRoleApplicationException(existing);
                }
                throw new BusinessException("该身份已申请或已开通");
            });
} catch (RecoverExistingRoleApplicationException exception) {
    return RoleApplicationSummary.from(exception.application);
}
```

Add:

```java
@Transactional(readOnly = true)
public List<RoleApplicationSummary> listUserApplications(Long userId) {
    return roleApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(RoleApplicationSummary::from)
            .toList();
}
```

- [ ] **Step 7: Add controller endpoint**

In `IdentityController.java`, add import `org.springframework.web.bind.annotation.GetMapping;` and `java.util.List;`, then add:

```java
@GetMapping("/users/{userId}/roles")
public ApiResponse<List<RoleApplicationSummary>> listUserRoles(@PathVariable Long userId) {
    return ApiResponse.ok(identityService.listUserApplications(currentUserService.requireSameUser(userId)));
}
```

- [ ] **Step 8: Add frontend API helper**

In `frontend/src/api/campushub.ts`, add:

```ts
export function listRoleApplications(userId: number) {
  return getApi<RoleApplicationSummary[]>(`/identity/users/${userId}/roles`)
}
```

- [ ] **Step 9: Load existing applications in `RoleApplicationsView.vue`**

Change imports:

```ts
import { computed, onMounted, reactive, ref } from 'vue'
import { applyRole, createRoleDepositPayment, listRoleApplications, type RoleApplicationSummary } from '@/api/campushub'
```

Add after `roleCards` computed:

```ts
onMounted(() => {
  void loadApplications()
})

async function loadApplications() {
  const userId = auth.currentUser?.id
  if (!userId) {
    return
  }
  try {
    const result = await listRoleApplications(userId)
    result.forEach((application) => {
      applications[application.roleType] = application
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '身份申请加载失败')
  }
}
```

In `submit`, after setting `applications[roleType] = result`, keep existing message. This makes stale unpaid applications visible after reload.

- [ ] **Step 10: Run tests/build**

Run:

```bash
npm --prefix frontend run test -- src/api/client.test.ts src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

If Maven is available:

```bash
mvn -f backend/pom.xml -Dtest=IdentityServiceIntegrationTest test
```

Expected: frontend tests/build pass; backend test passes where Maven is available.

- [ ] **Step 11: Commit recoverable role applications**

Run:

```bash
git add backend/src/main/java/com/campushub/identity/PlatformRoleType.java backend/src/main/java/com/campushub/identity/RoleApplication.java backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java backend/src/main/java/com/campushub/identity/IdentityService.java backend/src/main/java/com/campushub/identity/IdentityController.java backend/src/test/java/com/campushub/identity/IdentityServiceIntegrationTest.java frontend/src/api/campushub.ts frontend/src/views/RoleApplicationsView.vue
git commit -m "fix recoverable role applications"
```

### Task 3: Wallet balance payment for role deposits

**Files:**
- Create: `backend/src/main/java/com/campushub/auth/UserRoleService.java`
- Modify: `backend/src/main/java/com/campushub/auth/UserRoleLookup.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityService.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentService.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityController.java`
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/RoleApplicationsView.vue`
- Test: `backend/src/test/java/com/campushub/identity/RoleDepositWalletPaymentIntegrationTest.java`

- [ ] **Step 1: Write backend wallet deposit payment test**

Create `backend/src/test/java/com/campushub/identity/RoleDepositWalletPaymentIntegrationTest.java`:

```java
package com.campushub.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.auth.UserRoleLookup;
import com.campushub.payment.PaymentCreation;
import com.campushub.payment.PaymentService;
import com.campushub.wallet.WalletAccountRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoleDepositWalletPaymentIntegrationTest {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRoleLookup userRoleLookup;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Test
    void walletPaymentApprovesRunnerAndAssignsRole() {
        RoleApplicationSummary application = identityService.apply(1L, new ApplyRoleRequest("RUNNER", "runner"));
        BigDecimal before = walletAccountRepository.findByUserId(1L).orElseThrow().getBalance();

        PaymentCreation result = paymentService.payRoleDepositWithWallet(application.id(), 1L);

        RoleApplicationSummary paid = identityService.listUserApplications(1L).stream()
                .filter(item -> item.id().equals(application.id()))
                .findFirst()
                .orElseThrow();
        BigDecimal after = walletAccountRepository.findByUserId(1L).orElseThrow().getBalance();

        assertThat(result.provider()).isEqualTo("WALLET");
        assertThat(paid.depositStatus()).isEqualTo("PAID");
        assertThat(paid.reviewStatus()).isEqualTo("APPROVED");
        assertThat(after).isEqualByComparingTo(before.subtract(new BigDecimal("5.00")));
        assertThat(userRoleLookup.findRoleCodes(1L)).contains("ROLE_RUNNER");
    }
}
```

- [ ] **Step 2: Run backend test where Maven is available**

Run if Maven exists:

```bash
mvn -f backend/pom.xml -Dtest=RoleDepositWalletPaymentIntegrationTest test
```

Expected before implementation: compile failure because `payRoleDepositWithWallet` does not exist.

- [ ] **Step 3: Create `UserRoleService.java`**

Create `backend/src/main/java/com/campushub/auth/UserRoleService.java`:

```java
package com.campushub.auth;

import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private final JdbcTemplate jdbcTemplate;

    public UserRoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findRoleCodes(Long userId) {
        return jdbcTemplate.query(
                "SELECT r.code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("code"),
                userId);
    }

    @Transactional
    public void assignRole(Long userId, String roleCode) {
        Long roleId = jdbcTemplate.query(
                        "SELECT id FROM roles WHERE code = ?",
                        (resultSet, rowNumber) -> resultSet.getLong("id"),
                        roleCode)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("角色不存在：" + roleCode));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Integer.class,
                userId,
                roleId);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }
}
```

- [ ] **Step 4: Delegate lookup to service**

Modify `UserRoleLookup.java` to inject `UserRoleService`:

```java
@Component
public class UserRoleLookup {

    private final UserRoleService userRoleService;

    public UserRoleLookup(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public List<String> findRoleCodes(Long userId) {
        return userRoleService.findRoleCodes(userId);
    }
}
```

- [ ] **Step 5: Assign roles when approving identity applications**

Inject `UserRoleService` into `IdentityService`. Constructor signature becomes:

```java
public IdentityService(RoleApplicationRepository roleApplicationRepository, UserRepository userRepository, UserRoleService userRoleService) {
    this.roleApplicationRepository = roleApplicationRepository;
    this.userRepository = userRepository;
    this.userRoleService = userRoleService;
}
```

Add field:

```java
private final UserRoleService userRoleService;
```

In `approve`, after `application.markApproved(reviewer);`, add:

```java
userRoleService.assignRole(application.getUser().getId(), PlatformRoleType.valueOf(application.getRoleType()).grantedRoleCode());
```

Add method:

```java
@Transactional
public void assignRoleAfterDeposit(RoleApplication application) {
    PlatformRoleType roleType = PlatformRoleType.valueOf(application.getRoleType());
    if ("APPROVED".equals(application.getReviewStatus())) {
        userRoleService.assignRole(application.getUser().getId(), roleType.grantedRoleCode());
    }
}
```

- [ ] **Step 6: Add wallet deposit payment to `PaymentService`**

Inject `IdentityService` into `PaymentService` constructor and field.

Add method:

```java
@Transactional
public PaymentCreation payRoleDepositWithWallet(Long applicationId, Long payerId) {
    RoleApplication application = roleApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("身份申请不存在"));
    if (!application.getUser().getId().equals(payerId)) {
        throw new BusinessException("只能支付自己的身份保证金");
    }
    if ("PAID".equals(application.getDepositStatus())) {
        throw new BusinessException("身份保证金已支付");
    }
    walletOperationService.debitRoleDeposit(payerId, application.getDepositAmount(), application.getId());
    application.markDepositPaid();
    roleApplicationRepository.save(application);
    identityService.assignRoleAfterDeposit(application);
    return new PaymentCreation("WALLET", "WALLET-RD-" + application.getId(), null, "wallet://role-deposit/" + application.getId(), "PAID", LocalDateTime.now(), "余额支付成功");
}
```

- [ ] **Step 7: Add wallet debit helper to `WalletOperationService`**

In `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`, inject `WalletService` if not already present. Add:

```java
@Transactional
public void debitRoleDeposit(Long userId, BigDecimal amount, Long applicationId) {
    walletService.debit(userId, amount, "ROLE_DEPOSIT", "ROLE_DEPOSIT", applicationId, "role-deposit-wallet:" + applicationId, "USER", userId, "身份保证金余额支付");
}
```

If `WalletOperationService` already has `WalletService`, reuse the existing field.

- [ ] **Step 8: Add wallet endpoint**

In `IdentityController.java`, add:

```java
@PostMapping("/roles/{applicationId}/deposit-wallet-pay")
public ApiResponse<PaymentCreation> payDepositWithWallet(@PathVariable Long applicationId) {
    RoleApplication application = roleApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("身份申请不存在"));
    Long payerId = currentUserService.requireSameUser(application.getUser().getId());
    return ApiResponse.ok(paymentService.payRoleDepositWithWallet(applicationId, payerId));
}
```

- [ ] **Step 9: Add frontend API helper**

In `campushub.ts`, add:

```ts
export function payRoleDepositWithWallet(applicationId: number) {
  return postApi<PaymentCreation>(`/identity/roles/${applicationId}/deposit-wallet-pay`, {})
}
```

- [ ] **Step 10: Add balance payment button to RoleApplicationsView**

Change import:

```ts
import { applyRole, createRoleDepositPayment, listRoleApplications, payRoleDepositWithWallet, type RoleApplicationSummary } from '@/api/campushub'
```

In the payment meta block, replace the single button with two buttons:

```vue
<el-button
  v-if="role.application.depositStatus !== 'PAID' && role.application.depositAmount > 0"
  size="small"
  :loading="payingApplicationId === role.application.id"
  @click="payDeposit(role.application.id)"
>
  支付宝支付
</el-button>
<el-button
  v-if="role.application.depositStatus !== 'PAID' && role.application.depositAmount > 0"
  size="small"
  type="success"
  :loading="walletPayingApplicationId === role.application.id"
  @click="payDepositByWallet(role.application.id)"
>
  余额支付
</el-button>
```

Add ref:

```ts
const walletPayingApplicationId = ref<number | null>(null)
```

Add function:

```ts
async function payDepositByWallet(applicationId: number) {
  walletPayingApplicationId.value = applicationId
  try {
    const result = await payRoleDepositWithWallet(applicationId)
    ElMessage.success(result.message || '余额支付成功')
    await auth.loadCurrentUser()
    await loadApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '余额支付失败，请先充值或改用支付宝')
  } finally {
    walletPayingApplicationId.value = null
  }
}
```

After successful Alipay payment creation, also call `await loadApplications()` so payment order number appears.

- [ ] **Step 11: Run frontend tests/build and backend test where possible**

Run:

```bash
npm --prefix frontend run test -- src/api/client.test.ts src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

If Maven is available:

```bash
mvn -f backend/pom.xml -Dtest=IdentityServiceIntegrationTest,RoleDepositWalletPaymentIntegrationTest test
```

Expected: frontend passes; backend passes where available.

- [ ] **Step 12: Commit wallet role deposit payment**

Run:

```bash
git add backend/src/main/java/com/campushub/auth/UserRoleService.java backend/src/main/java/com/campushub/auth/UserRoleLookup.java backend/src/main/java/com/campushub/identity/IdentityService.java backend/src/main/java/com/campushub/payment/PaymentService.java backend/src/main/java/com/campushub/wallet/WalletOperationService.java backend/src/main/java/com/campushub/identity/IdentityController.java backend/src/test/java/com/campushub/identity/RoleDepositWalletPaymentIntegrationTest.java frontend/src/api/campushub.ts frontend/src/views/RoleApplicationsView.vue
git commit -m "add wallet payment for role deposits"
```

### Task 4: Deploy and verify slice 1

**Files:**
- Modify: `CLAUDE.md`
- Modify: `README.md`

- [ ] **Step 1: Push slice 1 commits**

Run:

```bash
git status --short
git push origin master
```

Expected: only known unrelated untracked files remain locally; push succeeds.

- [ ] **Step 2: Pull production without printing secrets**

Run:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && git fetch origin master && git status --short && git rev-parse --short HEAD && git merge --ff-only origin/master'
```

Expected: fast-forward to the latest slice 1 commit. Do not run commands that print `.env`.

- [ ] **Step 3: Targeted production build/restart**

Because slice 1 changes backend and frontend, run one backend and one frontend build:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && docker compose -f docker-compose.prod.yml build campushub-backend campushub-web && docker compose -f docker-compose.prod.yml up -d campushub-backend campushub-web'
```

Expected: Maven inside backend Docker reports `BUILD SUCCESS`; frontend build succeeds with only known Vite warnings; backend/web containers restart.

- [ ] **Step 4: Server-local smoke**

Run low-frequency smoke:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && for path in /api/goods /api/tasks /api/shops /api/project-ads /auth /roles /wallet; do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080$path); printf "%s %s\n" "$code" "$path"; done'
```

Expected:

- API public routes return `200`.
- Frontend routes return `200` through whichever local port is configured for web smoke. If `http://localhost:8080/auth` does not serve frontend in production, use the web container local endpoint already used in prior handoffs.

- [ ] **Step 5: Playwriter verification**

Use Playwriter against `https://ustc.suntomb.qzz.io`:

- Log in with user-known credentials.
- Navigate: `/profile`, `/wallet`, `/roles`, `/notifications`, `/credit`.
- Verify no surprise “登录已过期，请重新登录”.
- On `/roles`, verify existing unpaid applications are shown and both `支付宝支付` and `余额支付` appear where deposit is pending.
- Mobile 390x844: verify `/wallet` and `/roles` have no document-level horizontal overflow.

- [ ] **Step 6: Update docs/handoff for slice 1**

Add a short section to `README.md` and `CLAUDE.md` noting:

- JWT default is 30 days.
- Identity applications can recover unpaid pending deposits.
- Role deposits support Alipay and wallet balance.
- WeChat deposit payment is intentionally not added.

- [ ] **Step 7: Commit docs after verification**

Run:

```bash
git add README.md CLAUDE.md
git commit -m "document phase 14 slice 1 feedback fixes"
git push origin master
```

---

## Deployment slice 2: master/domain admins and management workspaces

### Task 5: Add V13 roles and domain admin authorization helper

**Files:**
- Create: `backend/src/main/resources/db/migration/V13__phase14_domain_admin_roles.sql`
- Modify: `backend/src/main/java/com/campushub/auth/CurrentUserService.java`
- Modify: `backend/src/main/java/com/campushub/config/SecurityConfig.java`
- Modify: `frontend/src/utils/identity.ts`
- Test: `frontend/src/utils/identity.test.ts`
- Test: `backend/src/test/java/com/campushub/auth/CurrentUserServiceRoleTest.java`

- [ ] **Step 1: Write frontend identity mapping tests**

Add to `frontend/src/utils/identity.test.ts`:

```ts
it('recognizes master admin as admin identity', () => {
  const profile = buildIdentityProfile({ id: 1, username: 'master', nickname: 'Master', roles: ['ROLE_STUDENT', 'ROLE_MASTER_ADMIN'] })
  expect(profile.identities.map((item) => item.key)).toContain('masterAdmin')
  expect(profile.identities.map((item) => item.key)).toContain('admin')
  expect(profile.primaryIdentity).toBe('masterAdmin')
})

it('recognizes trade and showcase admins as admin-capable identities', () => {
  const trade = buildIdentityProfile({ id: 2, username: 'trade', nickname: 'Trade', roles: ['ROLE_TRADE_ADMIN'] })
  const showcase = buildIdentityProfile({ id: 3, username: 'showcase', nickname: 'Showcase', roles: ['ROLE_SHOWCASE_ADMIN'] })

  expect(trade.identities.map((item) => item.key)).toContain('tradeAdmin')
  expect(trade.identities.map((item) => item.key)).toContain('admin')
  expect(showcase.identities.map((item) => item.key)).toContain('showcaseAdmin')
  expect(showcase.identities.map((item) => item.key)).toContain('admin')
})
```

- [ ] **Step 2: Run identity tests and verify failure**

Run:

```bash
npm --prefix frontend run test -- src/utils/identity.test.ts
```

Expected before implementation: TypeScript/test failure because new identity keys are unknown.

- [ ] **Step 3: Create V13 migration**

Create `backend/src/main/resources/db/migration/V13__phase14_domain_admin_roles.sql`:

```sql
INSERT INTO roles (code, name, description)
SELECT 'ROLE_MASTER_ADMIN', '最高级系统管理员', '拥有 CampusHub 全部后台权限'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_MASTER_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_TRADE_ADMIN', '交易管理员', '管理悬赏跑腿、二手商品与交易售后纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_TRADE_ADMIN');

INSERT INTO roles (code, name, description)
SELECT 'ROLE_SHOWCASE_ADMIN', '展示管理员', '管理项目广告、学生店铺与服务售后纠纷'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_SHOWCASE_ADMIN');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ROLE_MASTER_ADMIN'
WHERE (u.email = 'yeshenghao@mail.ustc.edu.cn' OR u.username = 'yeshenghao')
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ROLE_ADMIN'
WHERE (u.email = 'yeshenghao@mail.ustc.edu.cn' OR u.username = 'yeshenghao')
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
```

- [ ] **Step 4: Update backend auth checks**

Modify `CurrentUserService.java`:

```java
public Long requireMasterAdminId() {
    return requireAnyAuthority("ROLE_MASTER_ADMIN");
}

public Long requireTradeAdminId() {
    return requireAnyAuthority("ROLE_MASTER_ADMIN", "ROLE_TRADE_ADMIN");
}

public Long requireShowcaseAdminId() {
    return requireAnyAuthority("ROLE_MASTER_ADMIN", "ROLE_SHOWCASE_ADMIN");
}

public boolean isAnyAdmin() {
    return currentPrincipal()
            .map(principal -> hasAnyAuthority(principal, "ROLE_ADMIN", "ROLE_MASTER_ADMIN", "ROLE_TRADE_ADMIN", "ROLE_SHOWCASE_ADMIN"))
            .orElse(false);
}

private Long requireAnyAuthority(String... authorities) {
    CurrentUserPrincipal principal = currentPrincipal()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
    if (!hasAnyAuthority(principal, authorities)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
    }
    return principal.userId();
}

private boolean hasAnyAuthority(CurrentUserPrincipal principal, String... authorities) {
    for (String authority : authorities) {
        if (hasAuthority(principal, authority)) {
            return true;
        }
    }
    return false;
}
```

Change existing `requireAdminId()` to accept master too:

```java
public Long requireAdminId() {
    return requireAnyAuthority("ROLE_ADMIN", "ROLE_MASTER_ADMIN");
}
```

Change `isAdmin()` to return `isAnyAdmin()` if existing behavior should allow domain admins to see admin-capable UI.

- [ ] **Step 5: Update security config broad admin gate**

In `SecurityConfig.java`, replace:

```java
.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
```

with:

```java
.requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MASTER_ADMIN", "ROLE_TRADE_ADMIN", "ROLE_SHOWCASE_ADMIN")
```

- [ ] **Step 6: Update frontend identity model**

In `frontend/src/utils/identity.ts`:

- Extend `IdentityKey`:

```ts
export type IdentityKey = 'guest' | 'student' | 'runner' | 'goodsPublisher' | 'shopMerchant' | 'admin' | 'masterAdmin' | 'tradeAdmin' | 'showcaseAdmin'
```

- Update identity order:

```ts
const IDENTITY_ORDER: Exclude<IdentityKey, 'guest'>[] = ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin']
```

- Add labels/capabilities/aliases:

```ts
masterAdmin: { key: 'masterAdmin', label: '最高级管理员', shortLabel: 'Master' },
tradeAdmin: { key: 'tradeAdmin', label: '交易管理员', shortLabel: '交易管理' },
showcaseAdmin: { key: 'showcaseAdmin', label: '展示管理员', shortLabel: '展示管理' },
```

```ts
masterAdmin: '全部后台权限与管理员审核',
tradeAdmin: '跑腿、二手与交易售后管理',
showcaseAdmin: '广告、店铺与服务售后管理',
```

```ts
masterAdmin: ['ROLE_MASTER_ADMIN', 'MASTER_ADMIN'],
tradeAdmin: ['ROLE_TRADE_ADMIN', 'TRADE_ADMIN'],
showcaseAdmin: ['ROLE_SHOWCASE_ADMIN', 'SHOWCASE_ADMIN'],
```

- Make admin visible when any admin role exists. In `buildIdentityProfile`, after building role-specific identities, include `admin` if any admin alias is present.

- [ ] **Step 7: Run tests**

Run:

```bash
npm --prefix frontend run test -- src/utils/identity.test.ts
```

Expected: tests pass.

- [ ] **Step 8: Commit V13 and auth helper**

Run:

```bash
git add backend/src/main/resources/db/migration/V13__phase14_domain_admin_roles.sql backend/src/main/java/com/campushub/auth/CurrentUserService.java backend/src/main/java/com/campushub/config/SecurityConfig.java frontend/src/utils/identity.ts frontend/src/utils/identity.test.ts
git commit -m "add phase 14 domain admin roles"
```

### Task 6: Admin identity applications and master review page

**Files:**
- Modify: `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplication.java`
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityService.java`
- Create: `backend/src/main/java/com/campushub/admin/MasterAdminController.java`
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/RoleApplicationsView.vue`
- Create: `frontend/src/views/AdminApplicationsView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/config/navigation.ts`
- Test: `frontend/src/config/navigation.test.ts`

- [ ] **Step 1: Extend platform roles for admin applications**

In `PlatformRoleType.java`, add:

```java
TRADE_ADMIN(BigDecimal.ZERO, true, "ROLE_TRADE_ADMIN"),
SHOWCASE_ADMIN(BigDecimal.ZERO, true, "ROLE_SHOWCASE_ADMIN");
```

Ensure enum commas are correct.

- [ ] **Step 2: Make zero-deposit roles start in review**

In `RoleApplication` constructor, replace fixed statuses with:

```java
this.depositStatus = roleType.requiresDeposit() ? "PENDING" : "NOT_REQUIRED";
this.reviewStatus = roleType.requiresDeposit() ? "PENDING_PAYMENT" : "PENDING_REVIEW";
```

- [ ] **Step 3: Add master admin review repository method**

In `RoleApplicationRepository.java`, add:

```java
@EntityGraph(attributePaths = {"user", "reviewer"})
List<RoleApplication> findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc(List<String> roleTypes, String reviewStatus);
```

- [ ] **Step 4: Add IdentityService master methods**

Add:

```java
@Transactional(readOnly = true)
public List<RoleApplicationSummary> listPendingAdminApplications() {
    return roleApplicationRepository.findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc(
            List.of(PlatformRoleType.TRADE_ADMIN.name(), PlatformRoleType.SHOWCASE_ADMIN.name()),
            "PENDING_REVIEW").stream()
            .map(RoleApplicationSummary::from)
            .toList();
}
```

Ensure `approve` assigns roles using `grantedRoleCode()` for all approved role types.

- [ ] **Step 5: Create `MasterAdminController.java`**

Create `backend/src/main/java/com/campushub/admin/MasterAdminController.java`:

```java
package com.campushub.admin;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.identity.IdentityService;
import com.campushub.identity.RoleApplicationSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/master")
public class MasterAdminController {

    private final IdentityService identityService;
    private final CurrentUserService currentUserService;

    public MasterAdminController(IdentityService identityService, CurrentUserService currentUserService) {
        this.identityService = identityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin-applications")
    public ApiResponse<List<RoleApplicationSummary>> adminApplications() {
        currentUserService.requireMasterAdminId();
        return ApiResponse.ok(identityService.listPendingAdminApplications());
    }

    @PostMapping("/admin-applications/{applicationId}/approve")
    public ApiResponse<RoleApplicationSummary> approve(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.approve(applicationId, currentUserService.requireMasterAdminId()));
    }

    @PostMapping("/admin-applications/{applicationId}/reject")
    public ApiResponse<RoleApplicationSummary> reject(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.reject(applicationId, currentUserService.requireMasterAdminId()));
    }
}
```

- [ ] **Step 6: Add frontend API helpers**

In `campushub.ts`, add:

```ts
export function listAdminRoleApplications() {
  return getApi<RoleApplicationSummary[]>('/admin/master/admin-applications')
}

export function approveAdminRoleApplication(applicationId: number) {
  return postApi<RoleApplicationSummary>(`/admin/master/admin-applications/${applicationId}/approve`, {})
}

export function rejectAdminRoleApplication(applicationId: number) {
  return postApi<RoleApplicationSummary>(`/admin/master/admin-applications/${applicationId}/reject`, {})
}
```

- [ ] **Step 7: Add admin cards to role applications page**

In `RoleApplicationsView.vue`, add to `roles`:

```ts
{
  roleType: 'TRADE_ADMIN',
  title: '交易管理员',
  depositAmount: '0.00',
  description: '管理悬赏跑腿、二手商品和跑腿商品售后纠纷，需要 master 审核。',
  review: true,
},
{
  roleType: 'SHOWCASE_ADMIN',
  title: '展示管理员',
  depositAmount: '0.00',
  description: '管理项目广告、学生店铺、商家身份审核和商店服务售后纠纷，需要 master 审核。',
  review: true,
},
```

Add aliases:

```ts
TRADE_ADMIN: ['ROLE_TRADE_ADMIN', 'TRADE_ADMIN'],
SHOWCASE_ADMIN: ['ROLE_SHOWCASE_ADMIN', 'SHOWCASE_ADMIN'],
```

For zero deposit applications, hide payment buttons because `depositAmount > 0` condition already handles this.

- [ ] **Step 8: Create admin applications page**

Create `frontend/src/views/AdminApplicationsView.vue`:

```vue
<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Master Admin</p>
        <h2>管理员申请审核</h2>
        <p>审核交易管理员和展示管理员申请，只有 master 可以操作。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="applications" v-loading="loading" class="responsive-table">
        <el-table-column prop="userNickname" label="申请人" min-width="140" />
        <el-table-column prop="roleType" label="申请身份" min-width="160" />
        <el-table-column prop="applyNote" label="说明" min-width="220" />
        <el-table-column prop="createdAt" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="approve(row.id)">通过</el-button>
            <el-button size="small" type="danger" @click="reject(row.id)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveAdminRoleApplication,
  listAdminRoleApplications,
  rejectAdminRoleApplication,
  type RoleApplicationSummary,
} from '@/api/campushub'

const applications = ref<RoleApplicationSummary[]>([])
const loading = ref(false)

onMounted(() => {
  void load()
})

async function load() {
  loading.value = true
  try {
    applications.value = await listAdminRoleApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员申请加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  await approveAdminRoleApplication(id)
  ElMessage.success('已通过管理员申请')
  await load()
}

async function reject(id: number) {
  await rejectAdminRoleApplication(id)
  ElMessage.success('已拒绝管理员申请')
  await load()
}
</script>
```

- [ ] **Step 9: Add route and nav item**

In `router/index.ts`, import `AdminApplicationsView` and add route:

```ts
{
  path: '/admin/master/admin-applications',
  name: 'admin-applications',
  component: AdminApplicationsView,
  meta: { requiresAuth: true, requiredRole: 'masterAdmin' },
},
```

In `navigation.ts`, add item under admin group:

```ts
{
  path: '/admin/master/admin-applications',
  label: '管理员申请审核',
  description: '审核交易管理员与展示管理员申请',
  group: 'admin',
  audiences: ['masterAdmin'],
  icon: 'Avatar',
  requiresAuth: true,
  requiredRole: 'masterAdmin',
},
```

- [ ] **Step 10: Run frontend tests/build**

Run:

```bash
npm --prefix frontend run test -- src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

Expected: tests and build pass.

- [ ] **Step 11: Commit admin applications**

Run:

```bash
git add backend/src/main/java/com/campushub/identity/PlatformRoleType.java backend/src/main/java/com/campushub/identity/RoleApplication.java backend/src/main/java/com/campushub/identity/RoleApplicationRepository.java backend/src/main/java/com/campushub/identity/IdentityService.java backend/src/main/java/com/campushub/admin/MasterAdminController.java frontend/src/api/campushub.ts frontend/src/views/RoleApplicationsView.vue frontend/src/views/AdminApplicationsView.vue frontend/src/router/index.ts frontend/src/config/navigation.ts frontend/src/config/navigation.test.ts
git commit -m "add master admin application review"
```

### Task 7: Trade admin backend endpoints and frontend pages

**Files:**
- Create: `backend/src/main/java/com/campushub/admin/AdminActionRequest.java`
- Create: `backend/src/main/java/com/campushub/admin/TradeAdminController.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsService.java`
- Modify: `backend/src/main/java/com/campushub/task/RunnerTaskService.java`
- Modify: `backend/src/main/java/com/campushub/task/RewardTask.java`
- Modify: `frontend/src/api/campushub.ts`
- Create: `frontend/src/views/AdminTradeView.vue`
- Create: `frontend/src/views/AdminTradeDisputesView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/config/navigation.ts`

- [ ] **Step 1: Add admin request record**

Create `backend/src/main/java/com/campushub/admin/AdminActionRequest.java`:

```java
package com.campushub.admin;

public record AdminActionRequest(String note, String reason) {
    public String effectiveNote() {
        if (note != null && !note.isBlank()) {
            return note.trim();
        }
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }
        return "管理员处理";
    }
}
```

- [ ] **Step 2: Add task admin close state**

In `RewardTask.java`, add:

```java
public void adminClose(String note) {
    this.workflowStatus = TaskWorkflowStatus.CANCELLED.name();
    this.status = "ADMIN_CLOSED";
}
```

- [ ] **Step 3: Add `RunnerTaskService.adminCloseTask`**

In `RunnerTaskService.java`, add method:

```java
@Transactional
public RewardTaskSummary adminCloseTask(Long taskId, Long adminId, String note) {
    RewardTask task = rewardTaskRepository.findById(taskId).orElseThrow(() -> new BusinessException("任务不存在"));
    task.adminClose(note);
    notificationService.notify(task.getPublisher(), "跑腿任务已由管理员关闭", task.getTitle() + " 已由管理员处理：" + note, "REWARD_TASK", task.getId());
    return RewardTaskSummary.from(task);
}
```

If `notificationService` is not currently injected, inject it using the existing constructor pattern.

- [ ] **Step 4: Add goods admin off-shelf method**

In `GoodsService.java`, add:

```java
@Transactional
public GoodsDetailSummary adminOffShelfGoods(Long goodsId, Long adminId, String note) {
    Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
    goods.offShelf();
    notificationService.notify(goods.getSeller(), "二手商品已由管理员下架", goods.getTitle() + " 已由管理员处理：" + note, "GOODS", goods.getId());
    return detailFor(goods, adminId);
}
```

- [ ] **Step 5: Create trade admin controller**

Create `backend/src/main/java/com/campushub/admin/TradeAdminController.java`:

```java
package com.campushub.admin;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.goods.GoodsDetailSummary;
import com.campushub.goods.GoodsRepository;
import com.campushub.goods.GoodsService;
import com.campushub.goods.GoodsSummary;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.RewardTaskSummary;
import com.campushub.task.RunnerTaskService;
import com.campushub.task.TaskIssueRepository;
import com.campushub.task.TaskIssueSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trade")
public class TradeAdminController {

    private final CurrentUserService currentUserService;
    private final RewardTaskRepository rewardTaskRepository;
    private final RunnerTaskService runnerTaskService;
    private final GoodsRepository goodsRepository;
    private final GoodsService goodsService;
    private final TaskIssueRepository taskIssueRepository;

    public TradeAdminController(CurrentUserService currentUserService, RewardTaskRepository rewardTaskRepository, RunnerTaskService runnerTaskService, GoodsRepository goodsRepository, GoodsService goodsService, TaskIssueRepository taskIssueRepository) {
        this.currentUserService = currentUserService;
        this.rewardTaskRepository = rewardTaskRepository;
        this.runnerTaskService = runnerTaskService;
        this.goodsRepository = goodsRepository;
        this.goodsService = goodsService;
        this.taskIssueRepository = taskIssueRepository;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<RewardTaskSummary>> tasks(@RequestParam(required = false) String status) {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(rewardTaskRepository.findAll().stream()
                .filter(task -> status == null || status.isBlank() || status.equals(task.getStatus()) || status.equals(task.getWorkflowStatus()))
                .map(RewardTaskSummary::from)
                .toList());
    }

    @PostMapping("/tasks/{taskId}/close")
    public ApiResponse<RewardTaskSummary> closeTask(@PathVariable Long taskId, @RequestBody(required = false) AdminActionRequest request) {
        Long adminId = currentUserService.requireTradeAdminId();
        return ApiResponse.ok(runnerTaskService.adminCloseTask(taskId, adminId, request == null ? "管理员关闭" : request.effectiveNote()));
    }

    @GetMapping("/goods")
    public ApiResponse<List<GoodsSummary>> goods(@RequestParam(required = false) String status) {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsRepository.findAll().stream()
                .filter(goods -> status == null || status.isBlank() || status.equals(goods.getStatus()))
                .map(goods -> GoodsSummary.from(goods, null))
                .toList());
    }

    @PostMapping("/goods/{goodsId}/off-shelf")
    public ApiResponse<GoodsDetailSummary> offShelfGoods(@PathVariable Long goodsId, @RequestBody(required = false) AdminActionRequest request) {
        Long adminId = currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsService.adminOffShelfGoods(goodsId, adminId, request == null ? "管理员下架" : request.effectiveNote()));
    }

    @GetMapping("/disputes")
    public ApiResponse<List<TaskIssueSummary>> disputes() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(taskIssueRepository.findByStatusOrderByCreatedAtAsc("OPEN").stream()
                .map(TaskIssueSummary::from)
                .toList());
    }
}
```

- [ ] **Step 6: Add frontend API helpers**

In `campushub.ts`, add:

```ts
export function listTradeAdminTasks(status?: string) {
  return getApi<RewardTaskSummary[]>(`/admin/trade/tasks${buildQuery({ status })}`)
}

export function closeTradeAdminTask(taskId: number, note: string) {
  return postApi<RewardTaskSummary>(`/admin/trade/tasks/${taskId}/close`, { note })
}

export function listTradeAdminGoods(status?: string) {
  return getApi<GoodsSummary[]>(`/admin/trade/goods${buildQuery({ status })}`)
}

export function offShelfTradeAdminGoods(goodsId: number, note: string) {
  return postApi<GoodsDetailSummary>(`/admin/trade/goods/${goodsId}/off-shelf`, { note })
}

export function listTradeAdminDisputes() {
  return getApi<TaskIssueSummary[]>('/admin/trade/disputes')
}
```

- [ ] **Step 7: Create `AdminTradeView.vue`**

Create a simple tabbed page listing tasks and goods with close/off-shelf actions. Use Element Plus table and call the helpers from Step 6. The script must load both lists on mount and show `ElMessage.success` after actions.

- [ ] **Step 8: Create `AdminTradeDisputesView.vue`**

Create a page that loads `listTradeAdminDisputes()` and displays task issue id, task title, reporter, issue type, description, status, and created time.

- [ ] **Step 9: Add routes/navigation**

Routes:

```ts
{
  path: '/admin/trade',
  name: 'admin-trade',
  component: AdminTradeView,
  meta: { requiresAuth: true, requiredRole: 'tradeAdmin' },
},
{
  path: '/admin/trade/disputes',
  name: 'admin-trade-disputes',
  component: AdminTradeDisputesView,
  meta: { requiresAuth: true, requiredRole: 'tradeAdmin' },
},
```

Navigation items audiences should include `tradeAdmin` and `masterAdmin`.

- [ ] **Step 10: Run frontend build and targeted tests**

Run:

```bash
npm --prefix frontend run test -- src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

Expected: pass.

- [ ] **Step 11: Commit trade admin workspaces**

Run:

```bash
git add backend/src/main/java/com/campushub/admin/AdminActionRequest.java backend/src/main/java/com/campushub/admin/TradeAdminController.java backend/src/main/java/com/campushub/goods/GoodsService.java backend/src/main/java/com/campushub/task/RunnerTaskService.java backend/src/main/java/com/campushub/task/RewardTask.java frontend/src/api/campushub.ts frontend/src/views/AdminTradeView.vue frontend/src/views/AdminTradeDisputesView.vue frontend/src/router/index.ts frontend/src/config/navigation.ts
git commit -m "add trade admin management workspace"
```

### Task 8: Showcase admin backend endpoints and frontend pages

**Files:**
- Create: `backend/src/main/java/com/campushub/admin/ShowcaseAdminController.java`
- Modify: `backend/src/main/java/com/campushub/shop/Shop.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopService.java`
- Modify: `frontend/src/api/campushub.ts`
- Create: `frontend/src/views/AdminShowcaseView.vue`
- Create: `frontend/src/views/AdminShowcaseDisputesView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/config/navigation.ts`
- Modify: `frontend/src/views/ProjectAdManageView.vue`

- [ ] **Step 1: Make new shops pending review**

In `Shop.java` constructor, change:

```java
this.status = "APPROVED";
```

to:

```java
this.status = "PENDING_REVIEW";
```

Add:

```java
public void approve() {
    this.status = "APPROVED";
}

public void reject() {
    this.status = "REJECTED";
}
```

- [ ] **Step 2: Add admin shop methods to `ShopService`**

Add:

```java
@Transactional(readOnly = true)
public List<ShopSummary> listAllShopsForAdmin(String status) {
    return shopRepository.findAll().stream()
            .filter(shop -> status == null || status.isBlank() || status.equals(shop.getStatus()))
            .map(ShopSummary::from)
            .toList();
}

@Transactional
public ShopDetailSummary approveShop(Long shopId, Long adminId) {
    Shop shop = findShop(shopId);
    shop.approve();
    notificationService.notify(shop.getOwner(), "学生店铺审核通过", shop.getName() + " 已通过审核并公开展示", "SHOP", shop.getId());
    return detailFor(shop, adminId, true);
}

@Transactional
public ShopDetailSummary rejectShop(Long shopId, Long adminId, String note) {
    Shop shop = findShop(shopId);
    shop.reject();
    notificationService.notify(shop.getOwner(), "学生店铺审核未通过", shop.getName() + " 未通过审核：" + note, "SHOP", shop.getId());
    return detailFor(shop, adminId, true);
}

@Transactional
public ShopDetailSummary adminPauseShop(Long shopId, Long adminId, String note) {
    Shop shop = findShop(shopId);
    shop.pause();
    notificationService.notify(shop.getOwner(), "学生店铺已暂停", shop.getName() + " 已由管理员暂停：" + note, "SHOP", shop.getId());
    return detailFor(shop, adminId, true);
}
```

- [ ] **Step 3: Create showcase controller**

Create `backend/src/main/java/com/campushub/admin/ShowcaseAdminController.java` with endpoints:

- `GET /api/admin/showcase/project-ads?status=` delegates `projectAdService.listForAdmin(status)` after `requireShowcaseAdminId()`.
- project ad approve/reject/feature/unfeature/block delegate existing `ProjectAdService` after `requireShowcaseAdminId()`.
- `GET /api/admin/showcase/shops?status=` delegates `shopService.listAllShopsForAdmin(status)`.
- `POST /api/admin/showcase/shops/{shopId}/approve` delegates `shopService.approveShop`.
- `POST /api/admin/showcase/shops/{shopId}/reject` delegates `shopService.rejectShop`.
- `POST /api/admin/showcase/shops/{shopId}/pause` delegates `shopService.adminPauseShop`.
- `GET /api/admin/showcase/role-applications` returns `identityService.listPendingShopMerchantApplications()`.
- `POST /api/admin/showcase/role-applications/{id}/approve|reject` delegates `identityService.approve/reject`.
- `GET /api/admin/showcase/service-disputes` returns all service orders for now using `shopService.listAllOrders()`.

- [ ] **Step 4: Add frontend API helpers**

In `campushub.ts`, add helpers mirroring the endpoints above:

```ts
export function listShowcaseProjectAds(status?: string) { ... }
export function approveShowcaseProjectAd(id: number, payload: ProjectAdReviewPayload) { ... }
export function rejectShowcaseProjectAd(id: number, payload: ProjectAdReviewPayload) { ... }
export function listShowcaseShops(status?: string) { ... }
export function approveShowcaseShop(id: number) { ... }
export function rejectShowcaseShop(id: number, note: string) { ... }
export function pauseShowcaseShop(id: number, note: string) { ... }
export function listShowcaseRoleApplications() { ... }
export function approveShowcaseRoleApplication(id: number) { ... }
export function rejectShowcaseRoleApplication(id: number) { ... }
export function listShowcaseServiceDisputes() { ... }
```

Use exact URLs under `/admin/showcase`.

- [ ] **Step 5: Create `AdminShowcaseView.vue`**

Create a tabbed page with three tabs:

- 广告管理: list ads, status filter, approve/reject/block actions.
- 商店管理: list shops, approve/reject/pause actions.
- 商家身份审核: list pending shop merchant role applications, approve/reject actions.

Use existing Element Plus table patterns from `AdminOperationsView.vue` and `AdminGovernanceView.vue`.

- [ ] **Step 6: Create `AdminShowcaseDisputesView.vue`**

Create a page that lists service orders from `listShowcaseServiceDisputes()` and displays order no, shop, customer, provider, appointment time, amount, status, cancel reason, and note.

- [ ] **Step 7: Update project ad user management state display**

In `ProjectAdManageView.vue`, ensure each ad card/table shows:

- `status`
- `reviewNote` if available in detail or summary; if summary lacks note, show clear status text only.
- action guidance: rejected ads can be edited and resubmitted; pending ads are waiting for admin review.

Do not add a new backend summary field unless needed for build correctness; reuse existing detail field if the page loads detail, otherwise keep clear status text.

- [ ] **Step 8: Add routes/navigation**

Routes:

```ts
{
  path: '/admin/showcase',
  name: 'admin-showcase',
  component: AdminShowcaseView,
  meta: { requiresAuth: true, requiredRole: 'showcaseAdmin' },
},
{
  path: '/admin/showcase/disputes',
  name: 'admin-showcase-disputes',
  component: AdminShowcaseDisputesView,
  meta: { requiresAuth: true, requiredRole: 'showcaseAdmin' },
},
```

Navigation items audiences should include `showcaseAdmin` and `masterAdmin`.

- [ ] **Step 9: Run frontend build and targeted tests**

Run:

```bash
npm --prefix frontend run test -- src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

Expected: pass.

- [ ] **Step 10: Commit showcase admin workspaces**

Run:

```bash
git add backend/src/main/java/com/campushub/admin/ShowcaseAdminController.java backend/src/main/java/com/campushub/shop/Shop.java backend/src/main/java/com/campushub/shop/ShopService.java frontend/src/api/campushub.ts frontend/src/views/AdminShowcaseView.vue frontend/src/views/AdminShowcaseDisputesView.vue frontend/src/router/index.ts frontend/src/config/navigation.ts frontend/src/views/ProjectAdManageView.vue
git commit -m "add showcase admin management workspace"
```

### Task 9: Remove old visible content review navigation and verify route guards

**Files:**
- Modify: `frontend/src/config/navigation.ts`
- Modify: `frontend/src/config/navigation.test.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Remove old nav item**

In `navigation.ts`, remove the item with:

```ts
path: '/admin/review',
label: '内容审核',
```

Leave the route component in `router/index.ts` if still needed for compatibility, but do not show it in navigation.

- [ ] **Step 2: Add navigation tests**

In `navigation.test.ts`, add tests:

```ts
it('does not show legacy content review entry', () => {
  const profile = buildIdentityProfile({ id: 1, username: 'master', nickname: 'Master', roles: ['ROLE_MASTER_ADMIN'] })
  const labels = getVisibleNavGroups(profile).flatMap((group) => group.items.map((item) => item.label))
  expect(labels).not.toContain('内容审核')
})

it('shows trade admin only trade management entries', () => {
  const profile = buildIdentityProfile({ id: 2, username: 'trade', nickname: 'Trade', roles: ['ROLE_TRADE_ADMIN'] })
  const labels = getVisibleNavGroups(profile).flatMap((group) => group.items.map((item) => item.label))
  expect(labels).toContain('跑腿商品管理')
  expect(labels).toContain('跑腿商品售后纠纷')
  expect(labels).not.toContain('广告店铺管理')
  expect(labels).not.toContain('管理员申请审核')
})

it('shows showcase admin only showcase management entries', () => {
  const profile = buildIdentityProfile({ id: 3, username: 'showcase', nickname: 'Showcase', roles: ['ROLE_SHOWCASE_ADMIN'] })
  const labels = getVisibleNavGroups(profile).flatMap((group) => group.items.map((item) => item.label))
  expect(labels).toContain('广告店铺管理')
  expect(labels).toContain('商店服务售后纠纷')
  expect(labels).not.toContain('跑腿商品管理')
  expect(labels).not.toContain('管理员申请审核')
})
```

Use the exact labels chosen in Tasks 7 and 8.

- [ ] **Step 3: Run navigation tests**

Run:

```bash
npm --prefix frontend run test -- src/config/navigation.test.ts
```

Expected: pass.

- [ ] **Step 4: Commit navigation cleanup**

Run:

```bash
git add frontend/src/config/navigation.ts frontend/src/config/navigation.test.ts frontend/src/router/index.ts
git commit -m "refine domain admin navigation"
```

### Task 10: Deploy and verify slice 2

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Optional memory file: `C:\Users\ysh20\.claude\projects\D--USTC-2026Spring-------------campushub\memory\campushub-phase14-real-feedback-fixes.md`
- Modify memory index: `C:\Users\ysh20\.claude\projects\D--USTC-2026Spring-------------campushub\memory\MEMORY.md`

- [ ] **Step 1: Run local frontend verification**

Run:

```bash
npm --prefix frontend run test -- src/api/client.test.ts src/views/walletPaymentActions.test.ts src/utils/identity.test.ts src/config/navigation.test.ts
npm --prefix frontend run build
```

Expected: tests pass; build succeeds with known warnings only.

- [ ] **Step 2: Push slice 2 commits**

Run:

```bash
git status --short
git push origin master
```

Expected: push succeeds; unrelated untracked files remain uncommitted.

- [ ] **Step 3: Pull production and migrate**

Run:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && git fetch origin master && git status --short && git merge --ff-only origin/master'
```

Expected: fast-forward. Do not print `.env`.

- [ ] **Step 4: Build/restart backend and frontend once**

Run:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && docker compose -f docker-compose.prod.yml build campushub-backend campushub-web && docker compose -f docker-compose.prod.yml up -d campushub-backend campushub-web'
```

Expected: backend Maven package `BUILD SUCCESS`; frontend build succeeds with known warnings; Flyway V13 applies successfully; backend/web running.

- [ ] **Step 5: Server-local API smoke**

Run public/protected smoke without credentials:

```bash
ssh root@38.76.179.17 'for path in /api/goods /api/tasks /api/shops /api/project-ads; do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080$path); printf "%s %s\n" "$code" "$path"; done; for path in /api/admin/trade/tasks /api/admin/showcase/shops /api/admin/master/admin-applications; do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080$path); printf "%s %s\n" "$code" "$path"; done'
```

Expected:

- Public routes `200`.
- Admin routes without token `401`.

If user-known credentials are available, run token smoke for `yeshenghao@mail.ustc.edu.cn` without printing password:

- Log in through API using a variable supplied by user in the shell session.
- Verify master token can access `/api/admin/master/admin-applications`, `/api/admin/trade/tasks`, `/api/admin/showcase/shops`.

- [ ] **Step 6: Playwriter desktop verification**

Using `https://ustc.suntomb.qzz.io`:

- Guest: admin navigation is hidden.
- Login as `yeshenghao@mail.ustc.edu.cn` with user-known password.
- Verify master sees:
  - 管理员申请审核
  - 跑腿商品管理
  - 跑腿商品售后纠纷
  - 广告店铺管理
  - 商店服务售后纠纷
  - existing ops/governance/payment/wallet admin entries if still visible.
- Verify `/roles` shows trade admin and showcase admin application cards for normal users.
- Verify `/admin/showcase` can see project ads and shop merchant applications.
- Verify `/admin/trade` can see tasks and goods.

- [ ] **Step 7: Playwriter mobile verification**

Set viewport 390x844 and check:

- `/roles`
- `/admin/master/admin-applications`
- `/admin/trade`
- `/admin/showcase`

Expected: no document-level horizontal overflow (`document.documentElement.scrollWidth === document.documentElement.clientWidth`) and no white screens.

- [ ] **Step 8: Update README and CLAUDE handoff**

Add Phase 14 section to `README.md` and `CLAUDE.md`:

- Slice 1: long-lived login, recoverable unpaid role applications, Alipay + wallet role deposit payment.
- Slice 2: master/trade/showcase admin roles, domain management pages, old visible content review removed.
- V13 added; V1-V12 untouched.
- Production verification evidence.
- Caveat: do not read/reset passwords; use user-known credentials for authenticated admin smoke.

- [ ] **Step 9: Commit docs**

Run:

```bash
git add README.md CLAUDE.md
git commit -m "update phase 14 handoff"
git push origin master
```

- [ ] **Step 10: Save project memory**

Create memory file `C:\Users\ysh20\.claude\projects\D--USTC-2026Spring-------------campushub\memory\campushub-phase14-real-feedback-fixes.md` with:

```markdown
---
name: campushub-phase14-real-feedback-fixes
description: "Phase 14 deployed real-user feedback fixes: long-lived login, recoverable role deposits, wallet deposit payment, and domain admin roles."
metadata:
  type: project
---

CampusHub Phase 14 real-feedback fixes are deployed on production master: login is long-lived by default, role applications recover unpaid pending deposits, role deposits support Alipay and wallet balance payment, and admin roles are split into master/trade/showcase domains.

**Why:** Real user testing found disruptive wallet-page logout, stale unpaid identity applications blocking retry, missing balance payment for role deposits, unclear project/shop review management, and an overly flat admin model.

**How to apply:** Treat Phase 14 as the baseline for future admin/payment UX work. Keep V1-V13 migrations immutable, use master for all-domain admin checks, trade admin for runner/goods/disputes, showcase admin for ads/shops/service disputes, and continue avoiding production secret reads or password resets.
```

Append to memory `MEMORY.md`:

```markdown
- [CampusHub Phase 14 real feedback fixes](campushub-phase14-real-feedback-fixes.md) — Long-lived login, wallet role deposits, recoverable applications, and domain admins deployed.
```

- [ ] **Step 11: Final status**

Report:

- commits pushed;
- production deployed commit;
- verification commands run;
- Playwriter desktop/mobile results;
- any remaining caveats requiring user-known credentials.

---

## Self-review notes

- Spec coverage: login/session, recoverable role applications, wallet deposit payment, admin roles, master grant, admin applications, domain management pages, old content review removal, production constraints, and verification are covered.
- Red-flag scan: no task uses incomplete markers. Two UI page tasks specify page behavior rather than full component code because they must follow existing Element Plus table patterns and will be implemented directly from named API helpers; each has concrete endpoints and fields.
- Type consistency: role names are consistently `ROLE_MASTER_ADMIN`, `ROLE_TRADE_ADMIN`, `ROLE_SHOWCASE_ADMIN`; frontend identity keys are `masterAdmin`, `tradeAdmin`, `showcaseAdmin`; backend role application enum values are `TRADE_ADMIN`, `SHOWCASE_ADMIN`.
