# CampusHub Phase 6 Operations Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight operations analytics and CSV export subsystem for CampusHub Phase 6 without adding database migrations or changing payment/auth boundaries.

**Architecture:** Extend the existing `ops` backend package with date-range parsing, DTOs, real-time aggregation over existing repositories, and safe CSV responses. Enhance the existing Vue `/admin/ops` page with date filters, overview cards, funnel/zone/fee tabs, and export buttons while preserving existing monitor tabs.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway V1-V10 existing schema only, MySQL 8, Vue 3, Vite, TypeScript, Element Plus, Docker Compose, Playwriter.

---

## Scope and guardrails

- Do not create a V11 migration for Phase 6.
- Do not edit already-applied migrations V1-V10.
- Do not add export audit logs or analytics snapshot tables.
- Do not read, print, export, or copy secrets, `.env`, SMTP passwords, JWT secrets, payment tokens, or Alipay private/public key contents.
- Do not export password hashes, email-code hashes, session tokens, or complete WeChat/QQ contact values.
- Do not introduce payment-center hardening, RBAC hardening, or mobile UX overhaul.
- Do not install local dependencies for verification. Use server-side Docker build/API smoke/Playwriter later when deployment is explicitly authorized.
- Terminal commands below are plan instructions for future execution; when running from this Windows/CC Switch session, avoid PowerShell and use Bash only after user authorization.

## File structure map

### Backend ops package

- Create `backend/src/main/java/com/campushub/ops/AnalyticsDateRange.java` — validated date-range value object used by all analytics/export endpoints.
- Create `backend/src/main/java/com/campushub/ops/AnalyticsDateRangeParser.java` — converts optional `startDate`/`endDate` query params into `AnalyticsDateRange`.
- Create `backend/src/main/java/com/campushub/ops/MetricCardSummary.java` — generic label/value card DTO.
- Create `backend/src/main/java/com/campushub/ops/OperationsAnalyticsOverview.java` — overview DTO.
- Create `backend/src/main/java/com/campushub/ops/BusinessFunnelSummary.java` — business-line funnel DTO.
- Create `backend/src/main/java/com/campushub/ops/OperationsFunnelSummary.java` — all funnel DTOs.
- Create `backend/src/main/java/com/campushub/ops/ZoneMetricSummary.java` — single zone/route metric DTO.
- Create `backend/src/main/java/com/campushub/ops/OperationsZoneSummary.java` — all zone analytics DTO.
- Create `backend/src/main/java/com/campushub/ops/FeeAnalyticsSummary.java` — service-fee and role-deposit summary DTO.
- Create `backend/src/main/java/com/campushub/ops/CsvExport.java` — CSV body, file name, and content-type helper.
- Create `backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java` — central aggregation and CSV generation service.
- Modify `backend/src/main/java/com/campushub/ops/OperationsController.java` — add `/analytics/*` and `/exports/*.csv` endpoints.

### Existing repositories to extend only where needed

Prefer using `findAll()` for this Phase-6-sized implementation unless a page is clearly too large. Add simple Spring Data finder methods only when needed to avoid lazy-loading problems.

- Modify `backend/src/main/java/com/campushub/user/UserRepository.java` — optionally add date-range count query if implementing direct repository aggregation.
- Modify `backend/src/main/java/com/campushub/task/RewardTaskRepository.java` — optionally add date-range methods with `@EntityGraph`.
- Modify `backend/src/main/java/com/campushub/goods/GoodsRepository.java` — add `findAll()` `@EntityGraph` override if CSV needs seller nickname safely.
- Modify `backend/src/main/java/com/campushub/goods/GoodsIntentRepository.java` — add `findAll()` `@EntityGraph` override if intent aggregation uses goods/buyer/seller.
- Modify `backend/src/main/java/com/campushub/shop/ServiceOrderRepository.java` — already used by ops; add `findAll()` entity graph if needed.
- Modify `backend/src/main/java/com/campushub/projectad/ProjectAdRepository.java` — add admin-safe `findAll()` graph if needed.
- Modify `backend/src/main/java/com/campushub/moderation/ReportRecordRepository.java` and related Phase 5 repositories if needed for governance export.

### Backend tests

- Create `backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java` — service-level tests for date range, overview/funnels/fees, and CSV redaction.
- Create `backend/src/test/java/com/campushub/ops/OperationsAnalyticsControllerIntegrationTest.java` — MockMvc tests for API shape and CSV content type.

### Frontend

- Modify `frontend/src/api/campushub.ts` — add Phase 6 analytics DTO types, API functions, and export URL helpers.
- Modify `frontend/src/views/AdminOperationsView.vue` — add date range, analytics tabs, export buttons, and keep existing monitor tabs.
- Modify `frontend/src/styles.css` — add small reusable ops analytics styles only if current classes are insufficient.

### Docs

- Modify `README.md` — document Phase 6 analytics/export scope and privacy boundary.
- Modify `CLAUDE.md` — add Phase 6 handoff after implementation/deployment verification, not before.

---

## Task 1: Add analytics date range and DTO skeleton

**Files:**
- Create: `backend/src/main/java/com/campushub/ops/AnalyticsDateRange.java`
- Create: `backend/src/main/java/com/campushub/ops/AnalyticsDateRangeParser.java`
- Create: `backend/src/main/java/com/campushub/ops/MetricCardSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/OperationsAnalyticsOverview.java`
- Create: `backend/src/main/java/com/campushub/ops/BusinessFunnelSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/OperationsFunnelSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/ZoneMetricSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/OperationsZoneSummary.java`
- Create: `backend/src/main/java/com/campushub/ops/FeeAnalyticsSummary.java`
- Test: `backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java`

- [ ] **Step 1: Write failing tests for date-range defaults and validation**

Create `backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java`:

```java
package com.campushub.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class OperationsAnalyticsServiceIntegrationTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private final AnalyticsDateRangeParser parser = new AnalyticsDateRangeParser(clock);

    @Test
    void defaultsToRecentThirtyDaysWhenDatesAreMissing() {
        AnalyticsDateRange range = parser.parse(null, null);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 23));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 4, 23).atStartOfDay());
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2026, 5, 23).atStartOfDay());
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> parser.parse("2026-05-22", "2026-05-01"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("结束日期不能早于开始日期");
    }

    @Test
    void rejectsRangesLongerThanOneYear() {
        assertThatThrownBy(() -> parser.parse("2025-01-01", "2026-05-22"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("运营分析最多支持 366 天范围");
    }
}
```

- [ ] **Step 2: Run the failing date-range tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: FAIL because `AnalyticsDateRangeParser` and DTO classes do not exist.

- [ ] **Step 3: Implement `AnalyticsDateRange`**

Create `backend/src/main/java/com/campushub/ops/AnalyticsDateRange.java`:

```java
package com.campushub.ops;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalyticsDateRange(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive) {
}
```

- [ ] **Step 4: Implement `AnalyticsDateRangeParser`**

Create `backend/src/main/java/com/campushub/ops/AnalyticsDateRangeParser.java`:

```java
package com.campushub.ops;

import com.campushub.common.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsDateRangeParser {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;

    private final Clock clock;

    public AnalyticsDateRangeParser() {
        this(Clock.systemDefaultZone());
    }

    AnalyticsDateRangeParser(Clock clock) {
        this.clock = clock;
    }

    public AnalyticsDateRange parse(String startDateValue, String endDateValue) {
        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = parseDateOrDefault(endDateValue, today);
        LocalDate startDate = parseDateOrDefault(startDateValue, endDate.minusDays(DEFAULT_DAYS - 1L));
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_DAYS) {
            throw new BusinessException("运营分析最多支持 366 天范围");
        }
        return new AnalyticsDateRange(startDate, endDate, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private LocalDate parseDateOrDefault(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式必须为 yyyy-MM-dd");
        }
    }
}
```

- [ ] **Step 5: Add DTO records**

Create `backend/src/main/java/com/campushub/ops/MetricCardSummary.java`:

```java
package com.campushub.ops;

import java.math.BigDecimal;

public record MetricCardSummary(String key, String label, BigDecimal value, String unit) {
}
```

Create `backend/src/main/java/com/campushub/ops/OperationsAnalyticsOverview.java`:

```java
package com.campushub.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationsAnalyticsOverview(
        LocalDate startDate,
        LocalDate endDate,
        long newUsers,
        long activeUsers,
        long newTasks,
        long completedTasks,
        long taskIssues,
        long newGoods,
        long activeGoods,
        long goodsIntents,
        long newShopOrders,
        long completedShopOrders,
        long canceledShopOrders,
        long newProjectAds,
        long approvedProjectAds,
        long projectAdViews,
        long openReports,
        long pendingRoleApplications,
        long pendingProjectAds,
        BigDecimal paidServiceFeeAmount,
        BigDecimal roleDepositAmount,
        List<MetricCardSummary> cards) {
}
```

Create `backend/src/main/java/com/campushub/ops/BusinessFunnelSummary.java`:

```java
package com.campushub.ops;

import java.util.List;

public record BusinessFunnelSummary(String businessKey, String businessName, List<MetricCardSummary> steps) {
}
```

Create `backend/src/main/java/com/campushub/ops/OperationsFunnelSummary.java`:

```java
package com.campushub.ops;

import java.time.LocalDate;
import java.util.List;

public record OperationsFunnelSummary(LocalDate startDate, LocalDate endDate, List<BusinessFunnelSummary> funnels) {
}
```

Create `backend/src/main/java/com/campushub/ops/ZoneMetricSummary.java`:

```java
package com.campushub.ops;

public record ZoneMetricSummary(String key, String label, long count) {
}
```

Create `backend/src/main/java/com/campushub/ops/OperationsZoneSummary.java`:

```java
package com.campushub.ops;

import java.time.LocalDate;
import java.util.List;

public record OperationsZoneSummary(
        LocalDate startDate,
        LocalDate endDate,
        List<ZoneMetricSummary> taskOriginZones,
        List<ZoneMetricSummary> taskDestinationZones,
        List<ZoneMetricSummary> taskRoutes,
        List<ZoneMetricSummary> goodsZones,
        List<ZoneMetricSummary> shopZones,
        List<ZoneMetricSummary> projectAdZones) {
}
```

Create `backend/src/main/java/com/campushub/ops/FeeAnalyticsSummary.java`:

```java
package com.campushub.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FeeAnalyticsSummary(
        LocalDate startDate,
        LocalDate endDate,
        long serviceFeeCount,
        BigDecimal paidServiceFeeAmount,
        BigDecimal pendingServiceFeeAmount,
        List<MetricCardSummary> serviceFeesByTargetType,
        long roleApplicationCount,
        BigDecimal roleDepositAmount,
        List<MetricCardSummary> roleDepositsByType) {
}
```

- [ ] **Step 6: Verify DTO/date-range tests pass**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/ops/AnalyticsDateRange.java backend/src/main/java/com/campushub/ops/AnalyticsDateRangeParser.java backend/src/main/java/com/campushub/ops/MetricCardSummary.java backend/src/main/java/com/campushub/ops/OperationsAnalyticsOverview.java backend/src/main/java/com/campushub/ops/BusinessFunnelSummary.java backend/src/main/java/com/campushub/ops/OperationsFunnelSummary.java backend/src/main/java/com/campushub/ops/ZoneMetricSummary.java backend/src/main/java/com/campushub/ops/OperationsZoneSummary.java backend/src/main/java/com/campushub/ops/FeeAnalyticsSummary.java backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java
git commit -m "add operations analytics date range models"
```

## Task 2: Implement backend overview and funnel analytics

**Files:**
- Create: `backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java`
- Modify: `backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java`
- Modify as needed: repository files listed in the file structure map

- [ ] **Step 1: Extend service tests for overview and funnel metrics**

Replace `OperationsAnalyticsServiceIntegrationTest.java` with this full test class:

```java
package com.campushub.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OperationsAnalyticsServiceIntegrationTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private final AnalyticsDateRangeParser parser = new AnalyticsDateRangeParser(clock);

    @Autowired OperationsAnalyticsService analyticsService;

    @Test
    void defaultsToRecentThirtyDaysWhenDatesAreMissing() {
        AnalyticsDateRange range = parser.parse(null, null);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 23));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 4, 23).atStartOfDay());
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2026, 5, 23).atStartOfDay());
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> parser.parse("2026-05-22", "2026-05-01"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("结束日期不能早于开始日期");
    }

    @Test
    void rejectsRangesLongerThanOneYear() {
        assertThatThrownBy(() -> parser.parse("2025-01-01", "2026-05-22"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("运营分析最多支持 366 天范围");
    }

    @Test
    void buildsOverviewFromExistingSeedData() {
        AnalyticsDateRange range = parser.parse("2020-01-01", "2026-05-22");

        OperationsAnalyticsOverview overview = analyticsService.overview(range);

        assertThat(overview.newUsers()).isGreaterThan(0);
        assertThat(overview.activeUsers()).isGreaterThan(0);
        assertThat(overview.newTasks()).isGreaterThanOrEqualTo(0);
        assertThat(overview.newGoods()).isGreaterThanOrEqualTo(0);
        assertThat(overview.paidServiceFeeAmount()).isNotNull();
        assertThat(overview.roleDepositAmount()).isNotNull();
        assertThat(overview.cards()).extracting(MetricCardSummary::key)
                .contains("newUsers", "activeUsers", "newTasks", "newGoods", "openReports");
    }

    @Test
    void buildsBusinessFunnelsWithStableKeys() {
        AnalyticsDateRange range = parser.parse("2020-01-01", "2026-05-22");

        OperationsFunnelSummary funnels = analyticsService.funnels(range);

        assertThat(funnels.funnels()).extracting(BusinessFunnelSummary::businessKey)
                .containsExactly("tasks", "goods", "shops", "projectAds");
        assertThat(funnels.funnels().get(0).steps()).extracting(MetricCardSummary::key)
                .contains("published", "grabMode", "applicationMode", "accepted", "completed", "issues");
    }

    @Test
    void buildsFeeSummaryWithoutChangingPaymentBoundary() {
        AnalyticsDateRange range = parser.parse("2020-01-01", "2026-05-22");

        FeeAnalyticsSummary fees = analyticsService.fees(range);

        assertThat(fees.paidServiceFeeAmount()).isNotNull();
        assertThat(fees.pendingServiceFeeAmount()).isNotNull();
        assertThat(fees.roleDepositAmount()).isNotNull();
        assertThat(fees.roleDepositsByType()).extracting(MetricCardSummary::key)
                .contains("RUNNER", "GOODS_PUBLISHER", "SHOP_MERCHANT");
    }
}
```

- [ ] **Step 2: Run failing analytics tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: FAIL because `OperationsAnalyticsService` does not exist.

- [ ] **Step 3: Implement `OperationsAnalyticsService` constructor and helpers**

Create `backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java` with this initial structure:

```java
package com.campushub.ops;

import com.campushub.goods.Goods;
import com.campushub.goods.GoodsIntent;
import com.campushub.goods.GoodsIntentRepository;
import com.campushub.goods.GoodsRepository;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.interaction.Comment;
import com.campushub.interaction.CommentRepository;
import com.campushub.interaction.Favorite;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.moderation.ReportRecord;
import com.campushub.moderation.ReportRecordRepository;
import com.campushub.payment.ServiceFeeRecord;
import com.campushub.payment.ServiceFeeRecordRepository;
import com.campushub.projectad.ProjectAd;
import com.campushub.projectad.ProjectAdRepository;
import com.campushub.shop.ServiceItemRepository;
import com.campushub.shop.ServiceOrder;
import com.campushub.shop.ServiceOrderRepository;
import com.campushub.shop.Shop;
import com.campushub.shop.ShopRepository;
import com.campushub.task.RewardTask;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.TaskApplicationRepository;
import com.campushub.task.TaskIssueRepository;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationsAnalyticsService {

    private final UserRepository userRepository;
    private final RewardTaskRepository rewardTaskRepository;
    private final TaskApplicationRepository taskApplicationRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsIntentRepository goodsIntentRepository;
    private final ShopRepository shopRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ProjectAdRepository projectAdRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final ReportRecordRepository reportRecordRepository;
    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final RoleApplicationRepository roleApplicationRepository;

    public OperationsAnalyticsService(
            UserRepository userRepository,
            RewardTaskRepository rewardTaskRepository,
            TaskApplicationRepository taskApplicationRepository,
            TaskIssueRepository taskIssueRepository,
            GoodsRepository goodsRepository,
            GoodsIntentRepository goodsIntentRepository,
            ShopRepository shopRepository,
            ServiceItemRepository serviceItemRepository,
            ServiceOrderRepository serviceOrderRepository,
            ProjectAdRepository projectAdRepository,
            FavoriteRepository favoriteRepository,
            CommentRepository commentRepository,
            ReportRecordRepository reportRecordRepository,
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            RoleApplicationRepository roleApplicationRepository) {
        this.userRepository = userRepository;
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskApplicationRepository = taskApplicationRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.goodsRepository = goodsRepository;
        this.goodsIntentRepository = goodsIntentRepository;
        this.shopRepository = shopRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.projectAdRepository = projectAdRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.roleApplicationRepository = roleApplicationRepository;
    }

    private boolean inRange(LocalDateTime value, AnalyticsDateRange range) {
        return value != null && !value.isBefore(range.startInclusive()) && value.isBefore(range.endExclusive());
    }

    private MetricCardSummary card(String key, String label, long value) {
        return new MetricCardSummary(key, label, BigDecimal.valueOf(value), "count");
    }

    private MetricCardSummary moneyCard(String key, String label, BigDecimal value) {
        return new MetricCardSummary(key, label, value == null ? BigDecimal.ZERO : value, "CNY");
    }

    private BigDecimal sumAmounts(List<BigDecimal> amounts) {
        return amounts.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

If a repository import does not match the actual package/class name, inspect the current file and correct only that import/name.

- [ ] **Step 4: Add overview implementation**

Add this method inside `OperationsAnalyticsService`:

```java
public OperationsAnalyticsOverview overview(AnalyticsDateRange range) {
    List<User> users = userRepository.findAll();
    List<RewardTask> tasks = rewardTaskRepository.findAll();
    List<Goods> goods = goodsRepository.findAll();
    List<GoodsIntent> goodsIntents = goodsIntentRepository.findAll();
    List<ServiceOrder> shopOrders = serviceOrderRepository.findAll();
    List<ProjectAd> projectAds = projectAdRepository.findAll();
    List<ReportRecord> reports = reportRecordRepository.findAll();
    List<ServiceFeeRecord> serviceFees = serviceFeeRecordRepository.findAll();
    List<RoleApplication> roleApplications = roleApplicationRepository.findAll();

    long newUsers = users.stream().filter(user -> inRange(user.getCreatedAt(), range)).count();
    long newTasks = tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).count();
    long completedTasks = tasks.stream().filter(task -> inRange(task.getUpdatedAt(), range)).filter(task -> "COMPLETED".equals(task.getWorkflowStatus())).count();
    long taskIssues = taskIssueRepository.findAll().stream().filter(issue -> inRange(issue.getCreatedAt(), range)).count();
    long newGoods = goods.stream().filter(item -> inRange(item.getCreatedAt(), range)).count();
    long activeGoods = goods.stream().filter(item -> "ON_SALE".equals(item.getStatus())).count();
    long goodsIntentCount = goodsIntents.stream().filter(intent -> inRange(intent.getCreatedAt(), range)).count();
    long newShopOrders = shopOrders.stream().filter(order -> inRange(order.getCreatedAt(), range)).count();
    long completedShopOrders = shopOrders.stream().filter(order -> inRange(order.getCompletedAt(), range)).filter(order -> "COMPLETED".equals(order.getStatus())).count();
    long canceledShopOrders = shopOrders.stream().filter(order -> inRange(order.getCanceledAt(), range)).count();
    long newProjectAds = projectAds.stream().filter(ad -> inRange(ad.getCreatedAt(), range)).count();
    long approvedProjectAds = projectAds.stream().filter(ad -> inRange(ad.getPublishedAt(), range)).filter(ad -> "APPROVED".equals(ad.getStatus())).count();
    long projectAdViews = projectAds.stream().mapToLong(ProjectAd::getViewCount).sum();
    long openReports = reports.stream().filter(report -> "OPEN".equals(report.getStatus()) || "IN_REVIEW".equals(report.getStatus())).count();
    long pendingRoleApplications = roleApplications.stream().filter(application -> "PENDING_REVIEW".equals(application.getReviewStatus())).count();
    long pendingProjectAds = projectAds.stream().filter(ad -> "PENDING_REVIEW".equals(ad.getStatus())).count();
    BigDecimal paidServiceFeeAmount = sumAmounts(serviceFees.stream()
            .filter(fee -> "PAID".equals(fee.getStatus()))
            .filter(fee -> inRange(fee.getPaidAt(), range) || inRange(fee.getCreatedAt(), range))
            .map(ServiceFeeRecord::getAmount)
            .toList());
    BigDecimal roleDepositAmount = sumAmounts(roleApplications.stream()
            .filter(application -> inRange(application.getCreatedAt(), range))
            .map(RoleApplication::getDepositAmount)
            .toList());

    long activeUsers = activeUserCount(range, tasks, goods, goodsIntents, shopOrders, projectAds, reports);
    List<MetricCardSummary> cards = List.of(
            card("newUsers", "新增用户", newUsers),
            card("activeUsers", "活跃用户近似", activeUsers),
            card("newTasks", "新增跑腿", newTasks),
            card("newGoods", "新增商品", newGoods),
            card("newShopOrders", "新增预约", newShopOrders),
            card("newProjectAds", "新增项目广告", newProjectAds),
            card("openReports", "待处理举报", openReports),
            moneyCard("paidServiceFeeAmount", "已付服务费", paidServiceFeeAmount),
            moneyCard("roleDepositAmount", "身份保证金", roleDepositAmount));
    return new OperationsAnalyticsOverview(
            range.startDate(), range.endDate(), newUsers, activeUsers, newTasks, completedTasks, taskIssues,
            newGoods, activeGoods, goodsIntentCount, newShopOrders, completedShopOrders, canceledShopOrders,
            newProjectAds, approvedProjectAds, projectAdViews, openReports, pendingRoleApplications,
            pendingProjectAds, paidServiceFeeAmount, roleDepositAmount, cards);
}

private long activeUserCount(
        AnalyticsDateRange range,
        List<RewardTask> tasks,
        List<Goods> goods,
        List<GoodsIntent> goodsIntents,
        List<ServiceOrder> shopOrders,
        List<ProjectAd> projectAds,
        List<ReportRecord> reports) {
    Set<Long> userIds = new HashSet<>();
    tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).forEach(task -> userIds.add(task.getPublisher().getId()));
    goods.stream().filter(item -> inRange(item.getCreatedAt(), range)).forEach(item -> userIds.add(item.getSeller().getId()));
    goodsIntents.stream().filter(intent -> inRange(intent.getCreatedAt(), range)).forEach(intent -> userIds.add(intent.getBuyer().getId()));
    shopOrders.stream().filter(order -> inRange(order.getCreatedAt(), range)).forEach(order -> userIds.add(order.getCustomer().getId()));
    projectAds.stream().filter(ad -> inRange(ad.getCreatedAt(), range)).forEach(ad -> userIds.add(ad.getPublisher().getId()));
    reports.stream().filter(report -> inRange(report.getCreatedAt(), range)).forEach(report -> userIds.add(report.getReporter().getId()));
    return userIds.size();
}
```

If any getter name differs, use the entity's current getter and keep the returned DTO fields unchanged.

- [ ] **Step 5: Add funnel implementation**

Add this method inside `OperationsAnalyticsService`:

```java
public OperationsFunnelSummary funnels(AnalyticsDateRange range) {
    List<RewardTask> tasks = rewardTaskRepository.findAll();
    List<Goods> goods = goodsRepository.findAll();
    List<GoodsIntent> goodsIntents = goodsIntentRepository.findAll();
    List<ServiceOrder> shopOrders = serviceOrderRepository.findAll();
    List<ProjectAd> projectAds = projectAdRepository.findAll();

    BusinessFunnelSummary taskFunnel = new BusinessFunnelSummary("tasks", "跑腿任务", List.of(
            card("published", "发布任务", tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).count()),
            card("grabMode", "抢单模式", tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).filter(task -> "GRAB".equals(task.getAcceptanceMode())).count()),
            card("applicationMode", "申请模式", tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).filter(task -> "APPLICATION".equals(task.getAcceptanceMode())).count()),
            card("applications", "申请数", taskApplicationRepository.findAll().stream().filter(application -> inRange(application.getCreatedAt(), range)).count()),
            card("accepted", "已接单", tasks.stream().filter(task -> "ACCEPTED".equals(task.getWorkflowStatus()) || "COMPLETED".equals(task.getWorkflowStatus())).count()),
            card("completed", "已完成", tasks.stream().filter(task -> "COMPLETED".equals(task.getWorkflowStatus())).count()),
            card("issues", "异常/纠纷", taskIssueRepository.findAll().stream().filter(issue -> inRange(issue.getCreatedAt(), range)).count())));

    BusinessFunnelSummary goodsFunnel = new BusinessFunnelSummary("goods", "二手交易", List.of(
            card("published", "新增商品", goods.stream().filter(item -> inRange(item.getCreatedAt(), range)).count()),
            card("onSale", "当前在售", goods.stream().filter(item -> "ON_SALE".equals(item.getStatus())).count()),
            card("favorites", "收藏", favoriteRepository.findAll().stream().filter(favorite -> inRange(favorite.getCreatedAt(), range)).filter(favorite -> "GOODS".equals(favorite.getTargetType())).count()),
            card("comments", "留言评论", commentRepository.findAll().stream().filter(comment -> inRange(comment.getCreatedAt(), range)).filter(comment -> "GOODS".equals(comment.getTargetType())).count()),
            card("intents", "联系意向", goodsIntents.stream().filter(intent -> inRange(intent.getCreatedAt(), range)).count()),
            card("closed", "已下架/成交", goods.stream().filter(item -> !"ON_SALE".equals(item.getStatus())).count())));

    BusinessFunnelSummary shopFunnel = new BusinessFunnelSummary("shops", "学生店铺", List.of(
            card("shops", "店铺数", shopRepository.count()),
            card("items", "服务项目", serviceItemRepository.count()),
            card("requested", "预约请求", shopOrders.stream().filter(order -> inRange(order.getCreatedAt(), range)).count()),
            card("accepted", "已接受", shopOrders.stream().filter(order -> "ACCEPTED".equals(order.getStatus()) || "IN_SERVICE".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())).count()),
            card("inService", "服务中", shopOrders.stream().filter(order -> "IN_SERVICE".equals(order.getStatus())).count()),
            card("completed", "已完成", shopOrders.stream().filter(order -> "COMPLETED".equals(order.getStatus())).count()),
            card("canceled", "取消/拒绝", shopOrders.stream().filter(order -> "CANCELED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus())).count())));

    BusinessFunnelSummary projectAdFunnel = new BusinessFunnelSummary("projectAds", "项目广告", List.of(
            card("created", "新增广告", projectAds.stream().filter(ad -> inRange(ad.getCreatedAt(), range)).count()),
            card("pending", "待审核", projectAds.stream().filter(ad -> "PENDING_REVIEW".equals(ad.getStatus())).count()),
            card("approved", "已通过", projectAds.stream().filter(ad -> "APPROVED".equals(ad.getStatus())).count()),
            card("closed", "拒绝/关闭/屏蔽", projectAds.stream().filter(ad -> "REJECTED".equals(ad.getStatus()) || "CLOSED".equals(ad.getStatus()) || "BLOCKED".equals(ad.getStatus())).count()),
            card("views", "浏览量", projectAds.stream().mapToLong(ProjectAd::getViewCount).sum()),
            card("favorites", "收藏", favoriteRepository.findAll().stream().filter(favorite -> inRange(favorite.getCreatedAt(), range)).filter(favorite -> "PROJECT_AD".equals(favorite.getTargetType())).count()),
            card("comments", "评论", commentRepository.findAll().stream().filter(comment -> inRange(comment.getCreatedAt(), range)).filter(comment -> "PROJECT_AD".equals(comment.getTargetType())).count())));

    return new OperationsFunnelSummary(range.startDate(), range.endDate(), List.of(taskFunnel, goodsFunnel, shopFunnel, projectAdFunnel));
}
```

- [ ] **Step 6: Add fee implementation**

Add this method inside `OperationsAnalyticsService`:

```java
public FeeAnalyticsSummary fees(AnalyticsDateRange range) {
    List<ServiceFeeRecord> serviceFees = serviceFeeRecordRepository.findAll().stream()
            .filter(fee -> inRange(fee.getCreatedAt(), range) || inRange(fee.getPaidAt(), range))
            .toList();
    List<RoleApplication> roleApplications = roleApplicationRepository.findAll().stream()
            .filter(application -> inRange(application.getCreatedAt(), range))
            .toList();
    BigDecimal paid = sumAmounts(serviceFees.stream()
            .filter(fee -> "PAID".equals(fee.getStatus()))
            .map(ServiceFeeRecord::getAmount)
            .toList());
    BigDecimal pending = sumAmounts(serviceFees.stream()
            .filter(fee -> "PENDING".equals(fee.getStatus()))
            .map(ServiceFeeRecord::getAmount)
            .toList());
    BigDecimal deposits = sumAmounts(roleApplications.stream().map(RoleApplication::getDepositAmount).toList());

    List<MetricCardSummary> serviceFeesByTargetType = serviceFees.stream()
            .collect(java.util.stream.Collectors.groupingBy(ServiceFeeRecord::getTargetType))
            .entrySet().stream()
            .map(entry -> moneyCard(entry.getKey(), entry.getKey(), sumAmounts(entry.getValue().stream().map(ServiceFeeRecord::getAmount).toList())))
            .sorted(java.util.Comparator.comparing(MetricCardSummary::key))
            .toList();
    List<MetricCardSummary> roleDepositsByType = List.of(
            moneyCard("RUNNER", "跑腿接单者", sumRoleDeposits(roleApplications, "RUNNER")),
            moneyCard("GOODS_PUBLISHER", "二手发布者", sumRoleDeposits(roleApplications, "GOODS_PUBLISHER")),
            moneyCard("SHOP_MERCHANT", "店铺商家", sumRoleDeposits(roleApplications, "SHOP_MERCHANT")));
    return new FeeAnalyticsSummary(range.startDate(), range.endDate(), serviceFees.size(), paid, pending,
            serviceFeesByTargetType, roleApplications.size(), deposits, roleDepositsByType);
}

private BigDecimal sumRoleDeposits(List<RoleApplication> applications, String roleType) {
    return sumAmounts(applications.stream()
            .filter(application -> roleType.equals(application.getRoleType()))
            .map(RoleApplication::getDepositAmount)
            .toList());
}
```

- [ ] **Step 7: Fix repository/entity graph compile issues only if tests reveal them**

If tests fail with lazy-loading from `GoodsRepository.findAll()` or similar, add an override with `@EntityGraph` to the specific repository. For example, in `GoodsRepository` add:

```java
@Override
@EntityGraph(attributePaths = {"seller", "soldToUser"})
List<Goods> findAll();
```

Do the same narrow fix only for repositories that fail in tests.

- [ ] **Step 8: Verify analytics tests pass**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java backend/src/main/java/com/campushub/goods/GoodsRepository.java backend/src/main/java/com/campushub/goods/GoodsIntentRepository.java backend/src/main/java/com/campushub/shop/ServiceOrderRepository.java backend/src/main/java/com/campushub/projectad/ProjectAdRepository.java backend/src/main/java/com/campushub/moderation/ReportRecordRepository.java
git commit -m "add operations analytics aggregations"
```

If some repository files were not changed, omit them from `git add`.

## Task 3: Add zone analytics and safe CSV export backend

**Files:**
- Create: `backend/src/main/java/com/campushub/ops/CsvExport.java`
- Modify: `backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java`
- Modify: `backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java`

- [ ] **Step 1: Add failing tests for zones and CSV redaction**

Append these tests to `OperationsAnalyticsServiceIntegrationTest`:

```java
@Test
void buildsZoneAnalyticsFromStructuredCampusFields() {
    AnalyticsDateRange range = parser.parse("2020-01-01", "2026-05-22");

    OperationsZoneSummary zones = analyticsService.zones(range);

    assertThat(zones.taskOriginZones()).isNotNull();
    assertThat(zones.taskDestinationZones()).isNotNull();
    assertThat(zones.taskRoutes()).isNotNull();
    assertThat(zones.goodsZones()).isNotNull();
    assertThat(zones.shopZones()).isNotNull();
    assertThat(zones.projectAdZones()).isNotNull();
}

@Test
void exportsCsvWithBomAndWithoutSensitiveContactValues() {
    AnalyticsDateRange range = parser.parse("2020-01-01", "2026-05-22");

    CsvExport export = analyticsService.exportGoods(range);

    assertThat(export.fileName()).contains("goods");
    assertThat(export.contentType()).isEqualTo("text/csv; charset=UTF-8");
    assertThat(export.body()).startsWith("﻿");
    assertThat(export.body()).contains("联系方式开放");
    assertThat(export.body()).doesNotContain("wechat");
    assertThat(export.body()).doesNotContain("qq_contact");
    assertThat(export.body()).doesNotContain("password");
    assertThat(export.body()).doesNotContain("secret");
    assertThat(export.body()).doesNotContain("token");
}
```

- [ ] **Step 2: Run failing tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: FAIL because `CsvExport`, `zones`, and export methods do not exist.

- [ ] **Step 3: Add `CsvExport`**

Create `backend/src/main/java/com/campushub/ops/CsvExport.java`:

```java
package com.campushub.ops;

public record CsvExport(String fileName, String contentType, String body) {

    public static CsvExport of(String fileName, String bodyWithoutBom) {
        return new CsvExport(fileName, "text/csv; charset=UTF-8", "﻿" + bodyWithoutBom);
    }
}
```

- [ ] **Step 4: Add zone analytics implementation**

Add these methods to `OperationsAnalyticsService`:

```java
public OperationsZoneSummary zones(AnalyticsDateRange range) {
    List<RewardTask> tasks = rewardTaskRepository.findAll().stream().filter(task -> inRange(task.getCreatedAt(), range)).toList();
    List<Goods> goods = goodsRepository.findAll().stream().filter(item -> inRange(item.getCreatedAt(), range)).toList();
    List<Shop> shops = shopRepository.findAll();
    List<ProjectAd> projectAds = projectAdRepository.findAll().stream().filter(ad -> inRange(ad.getCreatedAt(), range)).toList();
    return new OperationsZoneSummary(
            range.startDate(),
            range.endDate(),
            zoneCounts(tasks.stream().map(RewardTask::getOriginZone).toList()),
            zoneCounts(tasks.stream().map(RewardTask::getDestinationZone).toList()),
            zoneCounts(tasks.stream().map(task -> task.getOriginZone() + " -> " + task.getDestinationZone()).toList()),
            zoneCounts(goods.stream().map(Goods::getCampusZone).toList()),
            zoneCounts(shops.stream().map(Shop::getCampusZone).toList()),
            zoneCounts(projectAds.stream().map(ProjectAd::getCampusZone).toList()));
}

private List<ZoneMetricSummary> zoneCounts(List<String> values) {
    return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.groupingBy(value -> value, java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new ZoneMetricSummary(entry.getKey(), entry.getKey(), entry.getValue()))
            .sorted(java.util.Comparator.comparing(ZoneMetricSummary::count).reversed().thenComparing(ZoneMetricSummary::key))
            .limit(20)
            .toList();
}
```

- [ ] **Step 5: Add CSV escaping helpers**

Add these helper methods to `OperationsAnalyticsService`:

```java
private String csv(String... values) {
    return java.util.Arrays.stream(values)
            .map(this::escapeCsv)
            .collect(java.util.stream.Collectors.joining(",")) + "\n";
}

private String escapeCsv(String value) {
    String safe = value == null ? "" : value;
    if (safe.contains("\"") || safe.contains(",") || safe.contains("\n") || safe.contains("\r")) {
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
}

private String text(Object value) {
    return value == null ? "" : String.valueOf(value);
}

private String yesNo(boolean value) {
    return value ? "是" : "否";
}
```

- [ ] **Step 6: Add CSV export methods**

Add these methods to `OperationsAnalyticsService`:

```java
public CsvExport exportTasks(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("ID", "标题", "模式", "状态", "起点校区", "终点校区", "报酬", "发布者", "创建时间"));
    rewardTaskRepository.findAll().stream()
            .filter(task -> inRange(task.getCreatedAt(), range))
            .forEach(task -> body.append(csv(text(task.getId()), task.getTitle(), task.getAcceptanceMode(), task.getWorkflowStatus(),
                    task.getOriginZone(), task.getDestinationZone(), text(task.getRewardAmount()), task.getPublisher().getNickname(), text(task.getCreatedAt()))));
    return CsvExport.of("tasks-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}

public CsvExport exportGoods(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("ID", "标题", "状态", "校区", "价格", "卖家", "浏览量", "联系方式开放", "创建时间"));
    goodsRepository.findAll().stream()
            .filter(item -> inRange(item.getCreatedAt(), range))
            .forEach(item -> body.append(csv(text(item.getId()), item.getTitle(), item.getStatus(), item.getCampusZone(),
                    text(item.getPrice()), item.getSeller().getNickname(), text(item.getViewCount()), yesNo(!"HIDDEN".equals(item.getContactVisibility())), text(item.getCreatedAt()))));
    return CsvExport.of("goods-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}

public CsvExport exportShopOrders(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("ID", "订单号", "店铺", "服务", "顾客", "商家", "状态", "金额", "预约时间", "创建时间"));
    serviceOrderRepository.findAll().stream()
            .filter(order -> inRange(order.getCreatedAt(), range))
            .forEach(order -> body.append(csv(text(order.getId()), order.getOrderNo(), order.getShop().getName(), order.getServiceItem().getTitle(),
                    order.getCustomer().getNickname(), order.getProvider().getNickname(), order.getStatus(), text(order.getAmount()),
                    text(order.getAppointmentTime()), text(order.getCreatedAt()))));
    return CsvExport.of("shop-orders-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}

public CsvExport exportProjectAds(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("ID", "标题", "类型", "状态", "校区", "发布者", "浏览量", "精选", "创建时间"));
    projectAdRepository.findAll().stream()
            .filter(ad -> inRange(ad.getCreatedAt(), range))
            .forEach(ad -> body.append(csv(text(ad.getId()), ad.getTitle(), ad.getAdType(), ad.getStatus(), ad.getCampusZone(),
                    ad.getPublisher().getNickname(), text(ad.getViewCount()), yesNo(ad.isFeatured()), text(ad.getCreatedAt()))));
    return CsvExport.of("project-ads-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}

public CsvExport exportGovernance(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("ID", "举报人", "目标类型", "目标ID", "原因", "状态", "处理人", "处理时间", "创建时间"));
    reportRecordRepository.findAll().stream()
            .filter(report -> inRange(report.getCreatedAt(), range))
            .forEach(report -> body.append(csv(text(report.getId()), report.getReporter().getNickname(), report.getTargetType(), text(report.getTargetId()),
                    report.getReason(), report.getStatus(), report.getHandler() == null ? "" : report.getHandler().getNickname(),
                    text(report.getHandledAt()), text(report.getCreatedAt()))));
    return CsvExport.of("governance-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}

public CsvExport exportFees(AnalyticsDateRange range) {
    StringBuilder body = new StringBuilder();
    body.append(csv("类型", "ID", "用户", "目标/身份", "金额", "状态", "创建时间", "支付/审核时间"));
    serviceFeeRecordRepository.findAll().stream()
            .filter(fee -> inRange(fee.getCreatedAt(), range) || inRange(fee.getPaidAt(), range))
            .forEach(fee -> body.append(csv("服务费", text(fee.getId()), fee.getPayer().getNickname(), fee.getTargetType() + ":" + fee.getTargetId(),
                    text(fee.getAmount()), fee.getStatus(), text(fee.getCreatedAt()), text(fee.getPaidAt()))));
    roleApplicationRepository.findAll().stream()
            .filter(application -> inRange(application.getCreatedAt(), range))
            .forEach(application -> body.append(csv("身份保证金", text(application.getId()), application.getUser().getNickname(), application.getRoleType(),
                    text(application.getDepositAmount()), application.getDepositStatus() + "/" + application.getReviewStatus(),
                    text(application.getCreatedAt()), text(application.getReviewedAt()))));
    return CsvExport.of("fees-" + range.startDate() + "-" + range.endDate() + ".csv", body.toString());
}
```

If entity getter names differ, inspect the entity and adjust only the getter call.

- [ ] **Step 7: Verify service tests pass**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/ops/CsvExport.java backend/src/main/java/com/campushub/ops/OperationsAnalyticsService.java backend/src/test/java/com/campushub/ops/OperationsAnalyticsServiceIntegrationTest.java
git commit -m "add operations zone analytics and CSV exports"
```

## Task 4: Add analytics and CSV endpoints

**Files:**
- Modify: `backend/src/main/java/com/campushub/ops/OperationsController.java`
- Create: `backend/src/test/java/com/campushub/ops/OperationsAnalyticsControllerIntegrationTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `backend/src/test/java/com/campushub/ops/OperationsAnalyticsControllerIntegrationTest.java`:

```java
package com.campushub.ops;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OperationsAnalyticsControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void returnsOverviewAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/ops/analytics/overview")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newUsers").exists())
                .andExpect(jsonPath("$.data.cards").isArray());
    }

    @Test
    void rejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/admin/ops/analytics/overview")
                        .param("startDate", "2026-05-22")
                        .param("endDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("结束日期不能早于开始日期"));
    }

    @Test
    void returnsCsvExportWithDownloadHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/ops/exports/goods.csv")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("联系方式开放")));
    }
}
```

- [ ] **Step 2: Run failing controller tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsControllerIntegrationTest test
```

Expected: FAIL because endpoints are not wired.

- [ ] **Step 3: Inject analytics service and parser into `OperationsController`**

Modify `OperationsController` fields and constructor to include:

```java
private final OperationsAnalyticsService operationsAnalyticsService;
private final AnalyticsDateRangeParser analyticsDateRangeParser;
```

Constructor parameters:

```java
OperationsAnalyticsService operationsAnalyticsService,
AnalyticsDateRangeParser analyticsDateRangeParser
```

Assignments:

```java
this.operationsAnalyticsService = operationsAnalyticsService;
this.analyticsDateRangeParser = analyticsDateRangeParser;
```

- [ ] **Step 4: Add JSON analytics endpoints**

Add these methods to `OperationsController`:

```java
@GetMapping("/analytics/overview")
public ApiResponse<OperationsAnalyticsOverview> analyticsOverview(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return ApiResponse.ok(operationsAnalyticsService.overview(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/analytics/funnels")
public ApiResponse<OperationsFunnelSummary> analyticsFunnels(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return ApiResponse.ok(operationsAnalyticsService.funnels(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/analytics/zones")
public ApiResponse<OperationsZoneSummary> analyticsZones(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return ApiResponse.ok(operationsAnalyticsService.zones(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/analytics/fees")
public ApiResponse<FeeAnalyticsSummary> analyticsFees(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return ApiResponse.ok(operationsAnalyticsService.fees(analyticsDateRangeParser.parse(startDate, endDate)));
}
```

Add imports if the IDE/compiler requires them:

```java
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
```

- [ ] **Step 5: Add CSV response helper and endpoints**

Add helper to `OperationsController`:

```java
private ResponseEntity<String> csv(CsvExport export) {
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(export.body());
}
```

Add endpoints:

```java
@GetMapping("/exports/tasks.csv")
public ResponseEntity<String> exportTasks(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportTasks(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/exports/goods.csv")
public ResponseEntity<String> exportGoods(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportGoods(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/exports/shop-orders.csv")
public ResponseEntity<String> exportShopOrders(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportShopOrders(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/exports/project-ads.csv")
public ResponseEntity<String> exportProjectAds(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportProjectAds(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/exports/governance.csv")
public ResponseEntity<String> exportGovernance(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportGovernance(analyticsDateRangeParser.parse(startDate, endDate)));
}

@GetMapping("/exports/fees.csv")
public ResponseEntity<String> exportFees(
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {
    return csv(operationsAnalyticsService.exportFees(analyticsDateRangeParser.parse(startDate, endDate)));
}
```

- [ ] **Step 6: Verify controller tests pass**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsControllerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Run all ops tests**

Run:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest,OperationsAnalyticsControllerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/ops/OperationsController.java backend/src/test/java/com/campushub/ops/OperationsAnalyticsControllerIntegrationTest.java
git commit -m "expose operations analytics APIs"
```

## Task 5: Add frontend analytics API types and export helpers

**Files:**
- Modify: `frontend/src/api/campushub.ts`

- [ ] **Step 1: Add frontend analytics types**

In `frontend/src/api/campushub.ts`, after `OperationsDashboardSummary`, add:

```ts
export interface MetricCardSummary {
  key: string
  label: string
  value: number
  unit: string
}

export interface OperationsAnalyticsOverview {
  startDate: string
  endDate: string
  newUsers: number
  activeUsers: number
  newTasks: number
  completedTasks: number
  taskIssues: number
  newGoods: number
  activeGoods: number
  goodsIntents: number
  newShopOrders: number
  completedShopOrders: number
  canceledShopOrders: number
  newProjectAds: number
  approvedProjectAds: number
  projectAdViews: number
  openReports: number
  pendingRoleApplications: number
  pendingProjectAds: number
  paidServiceFeeAmount: number
  roleDepositAmount: number
  cards: MetricCardSummary[]
}

export interface BusinessFunnelSummary {
  businessKey: string
  businessName: string
  steps: MetricCardSummary[]
}

export interface OperationsFunnelSummary {
  startDate: string
  endDate: string
  funnels: BusinessFunnelSummary[]
}

export interface ZoneMetricSummary {
  key: string
  label: string
  count: number
}

export interface OperationsZoneSummary {
  startDate: string
  endDate: string
  taskOriginZones: ZoneMetricSummary[]
  taskDestinationZones: ZoneMetricSummary[]
  taskRoutes: ZoneMetricSummary[]
  goodsZones: ZoneMetricSummary[]
  shopZones: ZoneMetricSummary[]
  projectAdZones: ZoneMetricSummary[]
}

export interface FeeAnalyticsSummary {
  startDate: string
  endDate: string
  serviceFeeCount: number
  paidServiceFeeAmount: number
  pendingServiceFeeAmount: number
  serviceFeesByTargetType: MetricCardSummary[]
  roleApplicationCount: number
  roleDepositAmount: number
  roleDepositsByType: MetricCardSummary[]
}

export interface OpsAnalyticsParams {
  startDate?: string
  endDate?: string
}
```

- [ ] **Step 2: Add frontend API functions**

After `getOpsDashboard()`, add:

```ts
export function getOpsAnalyticsOverview(params?: OpsAnalyticsParams) {
  return getApi<OperationsAnalyticsOverview>(`/admin/ops/analytics/overview${buildQuery(params)}`)
}

export function getOpsAnalyticsFunnels(params?: OpsAnalyticsParams) {
  return getApi<OperationsFunnelSummary>(`/admin/ops/analytics/funnels${buildQuery(params)}`)
}

export function getOpsAnalyticsZones(params?: OpsAnalyticsParams) {
  return getApi<OperationsZoneSummary>(`/admin/ops/analytics/zones${buildQuery(params)}`)
}

export function getOpsAnalyticsFees(params?: OpsAnalyticsParams) {
  return getApi<FeeAnalyticsSummary>(`/admin/ops/analytics/fees${buildQuery(params)}`)
}

export function buildOpsExportUrl(kind: 'tasks' | 'goods' | 'shop-orders' | 'project-ads' | 'governance' | 'fees', params?: OpsAnalyticsParams) {
  return `/api/admin/ops/exports/${kind}.csv${buildQuery(params)}`
}
```

This uses browser navigation for CSV files instead of Axios because the app only needs downloads.

- [ ] **Step 3: Build frontend type check**

Run:

```bash
npm --prefix frontend run build
```

Expected: PASS with the known Element Plus chunk-size warning only.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/campushub.ts
git commit -m "add operations analytics frontend APIs"
```

## Task 6: Enhance `/admin/ops` analytics UI

**Files:**
- Modify: `frontend/src/views/AdminOperationsView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Update imports and state**

In `AdminOperationsView.vue`, update the import from `@/api/campushub` to include:

```ts
  buildOpsExportUrl,
  getOpsAnalyticsFees,
  getOpsAnalyticsFunnels,
  getOpsAnalyticsOverview,
  getOpsAnalyticsZones,
  type BusinessFunnelSummary,
  type FeeAnalyticsSummary,
  type MetricCardSummary,
  type OperationsAnalyticsOverview,
  type OperationsFunnelSummary,
  type OperationsZoneSummary,
  type OpsAnalyticsParams,
  type ZoneMetricSummary,
```

Add state after existing refs:

```ts
const analyticsLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const overview = ref<OperationsAnalyticsOverview | null>(null)
const funnels = ref<OperationsFunnelSummary | null>(null)
const zones = ref<OperationsZoneSummary | null>(null)
const fees = ref<FeeAnalyticsSummary | null>(null)
```

- [ ] **Step 2: Add analytics loading functions**

Add these functions before `onMounted(load)`:

```ts
function analyticsParams(): OpsAnalyticsParams {
  return {
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
  }
}

async function loadAnalytics() {
  analyticsLoading.value = true
  try {
    const params = analyticsParams()
    const [overviewData, funnelData, zoneData, feeData] = await Promise.all([
      getOpsAnalyticsOverview(params),
      getOpsAnalyticsFunnels(params),
      getOpsAnalyticsZones(params),
      getOpsAnalyticsFees(params),
    ])
    overview.value = overviewData
    funnels.value = funnelData
    zones.value = zoneData
    fees.value = feeData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运营分析加载失败')
  } finally {
    analyticsLoading.value = false
  }
}

function exportCsv(kind: 'tasks' | 'goods' | 'shop-orders' | 'project-ads' | 'governance' | 'fees') {
  window.open(buildOpsExportUrl(kind, analyticsParams()), '_blank')
}

function formatMetricValue(card: MetricCardSummary) {
  if (card.unit === 'CNY') {
    return `¥${Number(card.value).toFixed(2)}`
  }
  return Number(card.value).toLocaleString()
}

function topZoneCount(items: ZoneMetricSummary[]) {
  return Math.max(...items.map((item) => item.count), 1)
}
```

Change `onMounted(load)` to:

```ts
onMounted(async () => {
  await Promise.all([load(), loadAnalytics()])
})
```

- [ ] **Step 3: Add analytics header and overview cards to template**

In the page heading action area, replace the single refresh button with:

```vue
<div class="ops-heading-actions">
  <el-date-picker
    v-model="dateRange"
    type="daterange"
    value-format="YYYY-MM-DD"
    start-placeholder="开始日期"
    end-placeholder="结束日期"
    range-separator="至"
    @change="loadAnalytics"
  />
  <el-button :loading="analyticsLoading" @click="loadAnalytics">刷新分析</el-button>
  <el-button :loading="loading" @click="load">刷新监控</el-button>
</div>
```

After the existing `.ops-metrics` dashboard block, add:

```vue
<div class="ops-analytics-panel" v-loading="analyticsLoading">
  <div class="section-heading">
    <div>
      <p class="eyebrow">Analytics</p>
      <h3>运营分析总览</h3>
      <p v-if="overview">统计范围：{{ overview.startDate }} 至 {{ overview.endDate }}</p>
    </div>
  </div>
  <div class="ops-metrics analytics-grid" v-if="overview">
    <div v-for="card in overview.cards" :key="card.key">
      <span>{{ card.label }}</span>
      <strong>{{ formatMetricValue(card) }}</strong>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Add analytics tabs before existing monitor tabs**

Inside `<el-tabs class="tabs-surface" v-loading="loading">`, add these panes before the current `任务监控` pane:

```vue
<el-tab-pane label="业务漏斗">
  <div class="funnel-grid" v-if="funnels">
    <div v-for="funnel in funnels.funnels" :key="funnel.businessKey" class="funnel-card">
      <h3>{{ funnel.businessName }}</h3>
      <div class="funnel-step" v-for="step in funnel.steps" :key="step.key">
        <span>{{ step.label }}</span>
        <strong>{{ formatMetricValue(step) }}</strong>
      </div>
    </div>
  </div>
  <el-empty v-else description="当前时间范围暂无业务漏斗数据" />
</el-tab-pane>
<el-tab-pane label="校区分析">
  <div class="zone-grid" v-if="zones">
    <div class="zone-card">
      <h3>跑腿起点</h3>
      <div v-for="item in zones.taskOriginZones" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.taskOriginZones)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
    <div class="zone-card">
      <h3>跑腿终点</h3>
      <div v-for="item in zones.taskDestinationZones" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.taskDestinationZones)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
    <div class="zone-card">
      <h3>高频路线</h3>
      <div v-for="item in zones.taskRoutes" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.taskRoutes)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
    <div class="zone-card">
      <h3>商品校区</h3>
      <div v-for="item in zones.goodsZones" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.goodsZones)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
    <div class="zone-card">
      <h3>店铺校区</h3>
      <div v-for="item in zones.shopZones" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.shopZones)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
    <div class="zone-card">
      <h3>项目广告校区</h3>
      <div v-for="item in zones.projectAdZones" :key="item.key" class="zone-row">
        <span>{{ item.label }}</span><el-progress :percentage="Math.round((item.count / topZoneCount(zones.projectAdZones)) * 100)" /><strong>{{ item.count }}</strong>
      </div>
    </div>
  </div>
  <el-empty v-else description="当前时间范围暂无校区数据" />
</el-tab-pane>
<el-tab-pane label="费用与导出">
  <div class="fee-grid" v-if="fees">
    <div class="funnel-card">
      <h3>服务费</h3>
      <div class="funnel-step"><span>记录数</span><strong>{{ fees.serviceFeeCount }}</strong></div>
      <div class="funnel-step"><span>已支付</span><strong>¥{{ Number(fees.paidServiceFeeAmount).toFixed(2) }}</strong></div>
      <div class="funnel-step"><span>待支付</span><strong>¥{{ Number(fees.pendingServiceFeeAmount).toFixed(2) }}</strong></div>
      <div class="funnel-step" v-for="item in fees.serviceFeesByTargetType" :key="item.key"><span>{{ item.label }}</span><strong>{{ formatMetricValue(item) }}</strong></div>
    </div>
    <div class="funnel-card">
      <h3>身份保证金</h3>
      <div class="funnel-step"><span>申请数</span><strong>{{ fees.roleApplicationCount }}</strong></div>
      <div class="funnel-step"><span>总金额</span><strong>¥{{ Number(fees.roleDepositAmount).toFixed(2) }}</strong></div>
      <div class="funnel-step" v-for="item in fees.roleDepositsByType" :key="item.key"><span>{{ item.label }}</span><strong>{{ formatMetricValue(item) }}</strong></div>
    </div>
  </div>
  <div class="export-grid">
    <el-button @click="exportCsv('tasks')">导出跑腿任务 CSV</el-button>
    <el-button @click="exportCsv('goods')">导出二手商品 CSV</el-button>
    <el-button @click="exportCsv('shop-orders')">导出店铺预约 CSV</el-button>
    <el-button @click="exportCsv('project-ads')">导出项目广告 CSV</el-button>
    <el-button @click="exportCsv('governance')">导出举报治理 CSV</el-button>
    <el-button @click="exportCsv('fees')">导出费用保证金 CSV</el-button>
  </div>
  <p class="hint">CSV 只导出运营字段，不包含密钥、token、密码哈希或完整联系方式。</p>
</el-tab-pane>
```

Remove unused imported types if the build reports them.

- [ ] **Step 5: Add styles**

Append to `frontend/src/styles.css`:

```css
.ops-heading-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.ops-analytics-panel {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.06);
}

.analytics-grid {
  margin-top: 14px;
}

.funnel-grid,
.zone-grid,
.fee-grid,
.export-grid {
  display: grid;
  gap: 16px;
}

.funnel-grid,
.zone-grid,
.fee-grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.export-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-top: 16px;
}

.funnel-card,
.zone-card {
  border: 1px solid #edf0f5;
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.funnel-card h3,
.zone-card h3 {
  margin: 0 0 12px;
}

.funnel-step,
.zone-row {
  display: grid;
  grid-template-columns: minmax(90px, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 8px 0;
  border-top: 1px solid #f2f4f8;
}

.zone-row {
  grid-template-columns: minmax(96px, 1fr) minmax(120px, 2fr) auto;
}
```

- [ ] **Step 6: Build frontend**

Run:

```bash
npm --prefix frontend run build
```

Expected: PASS with the known Element Plus chunk-size warning only.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/AdminOperationsView.vue frontend/src/styles.css
git commit -m "enhance operations analytics dashboard"
```

## Task 7: Update docs and handoff notes

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update README Phase 6 section**

Add a Phase 6 section near the existing phase documentation in `README.md`:

```markdown
### Phase 6：运营数据与导出

Phase 6 将已上线的跑腿、二手、店铺、项目广告和治理数据汇总到运营后台，用于内测和 Beta 决策。后台在 `/admin/ops` 提供日期范围筛选、平台总览、业务漏斗、校区分析、服务费/身份保证金汇总和 CSV 导出。

本阶段采用轻量实时聚合，不新增 Flyway 迁移，不创建分析快照表或导出日志表。CSV 只导出运营可见字段，不包含 `.env`、SMTP 密码、JWT secret、支付 token、支付宝密钥、密码哈希、邮箱验证码哈希、登录 token 或完整微信/QQ 联系方式。

支付边界保持不变：CampusHub 只汇总本地服务费记录和角色保证金申请记录，生产支付仍由外部 API-Transfer-Station 支付中心负责，CampusHub 不读取或保存支付宝密钥正文。
```

- [ ] **Step 2: Update CLAUDE handoff after implementation verification**

After implementation and verification, append this to `CLAUDE.md` and fill in exact commit/build facts from the actual run:

```markdown
## Latest Phase 6 deployment and Phase 7 handoff, 2026-05-22

Latest implemented Phase 6 focus: operations analytics and CSV export.

Implemented Phase 6:

- New docs: `docs/superpowers/specs/2026-05-22-campushub-phase6-ops-analytics-design.md` and `docs/superpowers/plans/2026-05-22-campushub-phase6-ops-analytics-upgrade.md`.
- No new Flyway migration was added; Phase 6 reads existing V1-V10 tables only.
- Backend ops package now exposes analytics overview, business funnels, campus-zone summaries, fee/deposit summaries, and CSV exports under `/api/admin/ops`.
- Frontend `/admin/ops` now includes date-range analytics, business funnel cards, campus-zone distribution, fee/deposit summary, and CSV export buttons.
- CSV exports are intentionally limited to operational fields and do not include secrets, payment tokens, password hashes, email-code hashes, login tokens, or full WeChat/QQ contacts.

Verified after Phase 6:

- Backend verification: <fill exact command and result>.
- Frontend verification: <fill exact command and result>.
- Production/server verification: <fill exact Docker/API/Playwriter result if deployed>.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V10; add V11+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.

Recommended Phase 7 start:

1. Use `docs/superpowers/plans/2026-05-22-campushub-overall-phased-roadmap.md` as the Phase 7 roadmap source.
2. Phase 7 should focus on responsive Web and user-experience polish, not payment-center hardening or auth/RBAC hardening.
```

Before committing, replace every `<fill ...>` placeholder with actual verification facts. Do not commit placeholder text.

- [ ] **Step 3: Verify no placeholder text remains**

Search the changed docs for placeholder markers using the dedicated search tool or an approved shell command. Expected: no `<fill`, `TBD`, or `TODO` in the Phase 6 handoff section.

- [ ] **Step 4: Commit docs**

```bash
git add README.md CLAUDE.md
git commit -m "document phase 6 operations analytics"
```

## Task 8: Verification, deployment, and browser smoke checkpoint

**Files:**
- Modify only if verification reveals real bugs.

- [ ] **Step 1: Run backend targeted tests where Maven is available**

Run in an environment with Maven, preferably server-side Docker build if local Maven is unavailable:

```bash
mvn -f backend/pom.xml -Dtest=OperationsAnalyticsServiceIntegrationTest,OperationsAnalyticsControllerIntegrationTest test
```

Expected: PASS.

- [ ] **Step 2: Run backend full package through Docker/server path before deployment**

After committing and pushing, use a low-impact server build only when authorized:

```bash
cd /opt/campushub
git pull --ff-only
docker compose -f docker-compose.prod.yml build campushub-backend
```

Expected: backend Docker build completes and Maven package reports `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend build through Docker/server path**

```bash
cd /opt/campushub
docker compose -f docker-compose.prod.yml build campushub-web
```

Expected: frontend Docker build completes; Vite may emit the known Element Plus chunk-size warning only.

- [ ] **Step 4: Restart only CampusHub backend and web**

```bash
cd /opt/campushub
docker compose -f docker-compose.prod.yml up -d campushub-backend campushub-web
```

Expected: MySQL remains healthy, backend and web containers are running.

- [ ] **Step 5: Server-local API smoke**

Run low-frequency checks from the server:

```bash
curl -sS -o /tmp/ops-overview.json -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/analytics/overview?startDate=2020-01-01&endDate=2026-05-22'
curl -sS -o /tmp/ops-funnels.json -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/analytics/funnels?startDate=2020-01-01&endDate=2026-05-22'
curl -sS -o /tmp/ops-zones.json -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/analytics/zones?startDate=2020-01-01&endDate=2026-05-22'
curl -sS -o /tmp/ops-fees.json -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/analytics/fees?startDate=2020-01-01&endDate=2026-05-22'
curl -sS -o /tmp/ops-goods.csv -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/exports/goods.csv?startDate=2020-01-01&endDate=2026-05-22'
curl -sS -o /tmp/ops-fees.csv -w '%{http_code}\n' 'http://127.0.0.1:18080/api/admin/ops/exports/fees.csv?startDate=2020-01-01&endDate=2026-05-22'
```

Expected: every command prints `200`. Do not print CSV contents if there is any concern about private operational data; only inspect headers/status and spot-check absence of forbidden field names if needed.

- [ ] **Step 6: Public browser smoke with Playwriter**

Verify these routes render without white screens or visible Element Plus errors:

- `https://ustc.suntomb.qzz.io/admin/ops`
- `https://ustc.suntomb.qzz.io/admin/governance`
- `https://ustc.suntomb.qzz.io/credit`
- `https://ustc.suntomb.qzz.io/goods`
- `https://ustc.suntomb.qzz.io/tasks`
- `https://ustc.suntomb.qzz.io/shops`
- `https://ustc.suntomb.qzz.io/project-ads`

On `/admin/ops`, verify:

- date-range picker is visible;
- overview analytics cards render;
- business funnel tab renders four business cards;
- campus-zone tab renders distribution rows or empty states;
- fees/export tab renders fee cards and six CSV buttons;
- clicking one CSV export initiates a download or opens a CSV response.

- [ ] **Step 7: Mobile smoke for `/admin/ops` basic layout**

Use a 390x844 viewport and check that `/admin/ops` has no obvious horizontal overflow:

```js
({ width: document.documentElement.scrollWidth, client: document.documentElement.clientWidth })
```

Expected: `width <= client + 2` or no user-visible horizontal scrolling.

- [ ] **Step 8: Final commit for verification fixes only**

If verification required fixes, commit them:

```bash
git add <fixed-files>
git commit -m "fix phase 6 verification issues"
```

Do not create an empty commit if no fixes were needed.

## Self-review

- Spec coverage: The plan covers real-time overview metrics, business-line funnels, campus-zone analytics, service-fee and role-deposit summaries, CSV exports, date-range filters, `/admin/ops` refinement, README/CLAUDE handoff, server Docker verification, API smoke, and Playwriter verification.
- No migration: The plan explicitly avoids V11 and uses existing V1-V10 tables only.
- Placeholder scan: The only placeholder-like strings appear in Task 7 as an instruction template and are explicitly forbidden from being committed until replaced with real verification facts.
- Type consistency: Backend DTO names match frontend TypeScript names: `MetricCardSummary`, `OperationsAnalyticsOverview`, `OperationsFunnelSummary`, `OperationsZoneSummary`, and `FeeAnalyticsSummary`.
- Scope: The plan stays within one major backend package expansion and one admin UI refinement; it does not add payment-center integration, auth/RBAC hardening, mobile UX overhaul, snapshot tables, export logs, or behavior tracking.
