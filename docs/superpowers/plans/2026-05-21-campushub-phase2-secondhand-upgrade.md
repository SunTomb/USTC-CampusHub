# CampusHub Phase 2 Second-Hand Trading Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade CampusHub second-hand trading from read-only listings into a usable campus marketplace with publisher deposit gating, image-backed product details, contact-intent workflow, interactions, reports, reviews, and service-fee hooks.

**Architecture:** Keep transaction principal offline and peer-to-peer. Add focused backend services around the existing `goods`, `file`, `interaction`, `moderation`, `payment`, `identity`, and `notification` contexts; add new Flyway migrations only. Frontend changes replace the current goods table with a marketplace card list and detail flow while reusing Phase 1 identity, notification, wallet/payment, and responsive layout patterns.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, JUnit/Spring Boot Test, Vue 3, Vite, TypeScript, Pinia, Vue Router, Element Plus, Vitest.

---

## Scope and dependencies

This plan implements the design in `docs/superpowers/specs/2026-05-21-campushub-phase2-secondhand-design.md`.

The current codebase after Phase 1 has:

- `GOODS_PUBLISHER` role/deposit support in `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`.
- Read-only goods APIs in `backend/src/main/java/com/campushub/goods/GoodsController.java`.
- Generic file metadata and binding tables/entities in `backend/src/main/java/com/campushub/file`.
- Generic comments/favorites read APIs in `backend/src/main/java/com/campushub/interaction`.
- Report records and admin read APIs in `backend/src/main/java/com/campushub/moderation`.
- Generic `ServiceFeeRecord` and `PaymentService` in `backend/src/main/java/com/campushub/payment`.
- A minimal table-based frontend goods page in `frontend/src/views/GoodsView.vue`.

Production has already applied Flyway V1-V5. Do not edit V1-V5. All database changes in this plan must be in new migrations V6+ and matching test migrations.

---

## File structure map

### Backend migrations

- Create `backend/src/main/resources/db/migration/V6__secondhand_goods_upgrade.sql` — goods extra fields, goods intents, reviews indexes if needed, file binding support indexes.
- Create `backend/src/test/resources/db/test-migration/V6__secondhand_goods_upgrade.sql` — H2-compatible copy of V6.

### Backend goods package

- Modify `backend/src/main/java/com/campushub/goods/Goods.java` — add publish/edit/contact fields, constructor/factory, status transitions.
- Modify `backend/src/main/java/com/campushub/goods/GoodsRepository.java` — add seller/status/filter queries and fetch seller for all detail mapping.
- Modify `backend/src/main/java/com/campushub/goods/GoodsSummary.java` — card summary with seller credit and cover image.
- Create `backend/src/main/java/com/campushub/goods/GoodsDetailSummary.java` — detail DTO with images, contact visibility, seller stats, comments/reviews counts.
- Create `backend/src/main/java/com/campushub/goods/CreateGoodsRequest.java` — publish request DTO.
- Create `backend/src/main/java/com/campushub/goods/UpdateGoodsRequest.java` — edit request DTO.
- Create `backend/src/main/java/com/campushub/goods/GoodsActionRequest.java` — mark sold/off-shelf request.
- Create `backend/src/main/java/com/campushub/goods/GoodsIntent.java` — lightweight purchase intent entity.
- Create `backend/src/main/java/com/campushub/goods/GoodsIntentRepository.java` — intent queries.
- Create `backend/src/main/java/com/campushub/goods/GoodsIntentRequest.java` — intent creation request.
- Create `backend/src/main/java/com/campushub/goods/GoodsIntentSummary.java` — intent DTO with contact snapshot.
- Create `backend/src/main/java/com/campushub/goods/GoodsService.java` — publish, edit, list/detail, intent, contact reveal, service-fee hook.
- Modify `backend/src/main/java/com/campushub/goods/GoodsController.java` — delegate all goods write/read workflows to `GoodsService`.

### Backend file package

- Modify `backend/src/main/java/com/campushub/file/FileBindingRepository.java` — fetch file metadata with bindings.
- Modify `backend/src/main/java/com/campushub/file/FileBindingSummary.java` — expose image URL/path fields needed by frontend.
- Create `backend/src/main/java/com/campushub/file/FileUploadService.java` — validate and store image metadata/files.
- Create `backend/src/main/java/com/campushub/file/BindFileRequest.java` — target binding DTO.
- Modify `backend/src/main/java/com/campushub/file/FileController.java` — add image upload and bind endpoints.

### Backend interaction/review/moderation

- Create `backend/src/main/java/com/campushub/interaction/CommentRequest.java` — comment/reply request.
- Create `backend/src/main/java/com/campushub/interaction/FavoriteRequest.java` — favorite target request.
- Modify `backend/src/main/java/com/campushub/interaction/InteractionController.java` — add write endpoints for comments/favorites.
- Create `backend/src/main/java/com/campushub/review/Review.java` — maps existing `reviews` table.
- Create `backend/src/main/java/com/campushub/review/ReviewRepository.java` — review queries and aggregate counts.
- Create `backend/src/main/java/com/campushub/review/ReviewRequest.java` — review creation request.
- Create `backend/src/main/java/com/campushub/review/ReviewSummary.java` — review DTO.
- Create `backend/src/main/java/com/campushub/review/ReviewService.java` — validates one review per completed intent.
- Create `backend/src/main/java/com/campushub/review/ReviewController.java` — create/list reviews.
- Create `backend/src/main/java/com/campushub/moderation/ReportRequest.java` — user report request.
- Modify `backend/src/main/java/com/campushub/moderation/ModerationController.java` — add report submission and handling endpoints.

### Backend tests

- Create `backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/file/GoodsFileBindingIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/interaction/GoodsInteractionIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/review/GoodsReviewIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/moderation/GoodsReportIntegrationTest.java`.

### Frontend API and pages

- Modify `frontend/src/api/campushub.ts` — add second-hand goods detail, publish, image, intent, interaction, review, report APIs/types.
- Replace `frontend/src/views/GoodsView.vue` — card marketplace, filters, role-gated publish entry.
- Create `frontend/src/views/GoodsDetailView.vue` — detail, images, contact-intent, comments, reviews, report, seller actions.
- Create `frontend/src/views/GoodsPublishView.vue` — role-gated publish form and image upload/bind flow.
- Modify `frontend/src/router/index.ts` — add `/goods/:id` and `/goods/publish`.
- Modify `frontend/src/styles.css` — responsive marketplace/detail/publish styles.

---

## Phase 2 implementation tasks

### Task 1: Add second-hand schema for goods metadata and purchase intents

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__secondhand_goods_upgrade.sql`
- Create: `backend/src/test/resources/db/test-migration/V6__secondhand_goods_upgrade.sql`
- Test: `backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java`

- [ ] **Step 1: Write failing schema-backed publish gate test**

Create `backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java`:

```java
package com.campushub.goods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsPublishingIntegrationTest {

    @Autowired GoodsService goodsService;
    @Autowired GoodsRepository goodsRepository;
    @Autowired IdentityService identityService;
    @Autowired UserRepository userRepository;

    @Test
    void publishRequiresApprovedGoodsPublisherRole() {
        User student = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();

        CreateGoodsRequest request = new CreateGoodsRequest(
                1L,
                "九成新机械键盘",
                "青轴，配件齐全，支持中区当面交易",
                new BigDecimal("199.00"),
                new BigDecimal("399.00"),
                "九成新",
                "CENTRAL",
                "中校区宿舍楼下",
                "OFFLINE_MEETUP",
                "INTENT_ONLY");

        assertThatThrownBy(() -> goodsService.publish(student.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("二手发布者");
    }

    @Test
    void approvedGoodsPublisherCanPublishGoods() {
        User student = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        identityService.apply(student.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));

        CreateGoodsRequest request = new CreateGoodsRequest(
                1L,
                "九成新机械键盘",
                "青轴，配件齐全，支持中区当面交易",
                new BigDecimal("199.00"),
                new BigDecimal("399.00"),
                "九成新",
                "CENTRAL",
                "中校区宿舍楼下",
                "OFFLINE_MEETUP",
                "INTENT_ONLY");

        GoodsDetailSummary summary = goodsService.publish(student.getId(), request);

        assertThat(summary.title()).isEqualTo("九成新机械键盘");
        assertThat(summary.status()).isEqualTo("PUBLISHED");
        assertThat(summary.sellerId()).isEqualTo(student.getId());
        assertThat(summary.contactVisible()).isFalse();
        assertThat(goodsRepository.findById(summary.id())).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -Dtest=GoodsPublishingIntegrationTest test
```

Expected: FAIL because `GoodsService`, `CreateGoodsRequest`, and `GoodsDetailSummary` do not exist.

- [ ] **Step 3: Add production Flyway migration V6**

Create `backend/src/main/resources/db/migration/V6__secondhand_goods_upgrade.sql`:

```sql
ALTER TABLE goods
    ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER trade_location,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'INTENT_ONLY' AFTER campus_zone,
    ADD COLUMN delivery_method VARCHAR(40) NOT NULL DEFAULT 'OFFLINE_MEETUP' AFTER contact_visibility,
    ADD COLUMN service_fee_policy VARCHAR(40) NOT NULL DEFAULT 'NONE' AFTER delivery_method,
    ADD COLUMN published_at DATETIME NULL AFTER service_fee_policy,
    ADD COLUMN updated_at DATETIME NULL AFTER published_at,
    ADD COLUMN sold_at DATETIME NULL AFTER updated_at,
    ADD COLUMN sold_to_user_id BIGINT NULL AFTER sold_at,
    ADD CONSTRAINT fk_goods_sold_to_user FOREIGN KEY (sold_to_user_id) REFERENCES users(id);

CREATE TABLE goods_intents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    message VARCHAR(500) NULL,
    contact_snapshot VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    service_fee_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_goods_intent_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
    CONSTRAINT fk_goods_intent_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id),
    CONSTRAINT uk_goods_intent_buyer_goods UNIQUE (goods_id, buyer_id),
    INDEX idx_goods_intent_seller_status (seller_id, status),
    INDEX idx_goods_intent_buyer_time (buyer_id, created_at)
);

CREATE INDEX idx_goods_status_zone_time ON goods (status, campus_zone, created_at);
CREATE INDEX idx_reviews_target_user_time ON reviews (target_user_id, created_at);
```

- [ ] **Step 4: Add H2-compatible test migration V6**

Create `backend/src/test/resources/db/test-migration/V6__secondhand_goods_upgrade.sql`:

```sql
ALTER TABLE goods ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER';
ALTER TABLE goods ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'INTENT_ONLY';
ALTER TABLE goods ADD COLUMN delivery_method VARCHAR(40) NOT NULL DEFAULT 'OFFLINE_MEETUP';
ALTER TABLE goods ADD COLUMN service_fee_policy VARCHAR(40) NOT NULL DEFAULT 'NONE';
ALTER TABLE goods ADD COLUMN published_at TIMESTAMP NULL;
ALTER TABLE goods ADD COLUMN updated_at TIMESTAMP NULL;
ALTER TABLE goods ADD COLUMN sold_at TIMESTAMP NULL;
ALTER TABLE goods ADD COLUMN sold_to_user_id BIGINT NULL;
ALTER TABLE goods ADD CONSTRAINT fk_goods_sold_to_user FOREIGN KEY (sold_to_user_id) REFERENCES users(id);

CREATE TABLE goods_intents (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    goods_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    message VARCHAR(500) NULL,
    contact_snapshot VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    service_fee_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goods_intent_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
    CONSTRAINT fk_goods_intent_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_goods_intent_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id),
    CONSTRAINT uk_goods_intent_buyer_goods UNIQUE (goods_id, buyer_id)
);

CREATE INDEX idx_goods_status_zone_time ON goods (status, campus_zone, created_at);
CREATE INDEX idx_goods_intent_seller_status ON goods_intents (seller_id, status);
CREATE INDEX idx_goods_intent_buyer_time ON goods_intents (buyer_id, created_at);
CREATE INDEX idx_reviews_target_user_time ON reviews (target_user_id, created_at);
```

- [ ] **Step 5: Commit schema and failing test**

```bash
git add backend/src/main/resources/db/migration/V6__secondhand_goods_upgrade.sql backend/src/test/resources/db/test-migration/V6__secondhand_goods_upgrade.sql backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java
git commit -m "add secondhand goods schema"
```

### Task 2: Implement role-gated goods publishing and detail DTOs

**Files:**
- Modify: `backend/src/main/java/com/campushub/goods/Goods.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsRepository.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsSummary.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsDetailSummary.java`
- Create: `backend/src/main/java/com/campushub/goods/CreateGoodsRequest.java`
- Create: `backend/src/main/java/com/campushub/goods/UpdateGoodsRequest.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsActionRequest.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsService.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsController.java`
- Test: `backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java`

- [ ] **Step 1: Add request records**

Create `backend/src/main/java/com/campushub/goods/CreateGoodsRequest.java`:

```java
package com.campushub.goods;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateGoodsRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        BigDecimal originalPrice,
        @NotBlank @Size(max = 40) String conditionLevel,
        @NotBlank @Size(max = 40) String campusZone,
        @NotBlank @Size(max = 120) String tradeLocation,
        @NotBlank @Size(max = 40) String deliveryMethod,
        @NotBlank @Size(max = 40) String contactVisibility) {
}
```

Create `backend/src/main/java/com/campushub/goods/UpdateGoodsRequest.java`:

```java
package com.campushub.goods;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateGoodsRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        BigDecimal originalPrice,
        @NotBlank @Size(max = 40) String conditionLevel,
        @NotBlank @Size(max = 40) String campusZone,
        @NotBlank @Size(max = 120) String tradeLocation,
        @NotBlank @Size(max = 40) String deliveryMethod,
        @NotBlank @Size(max = 40) String contactVisibility) {
}
```

Create `backend/src/main/java/com/campushub/goods/GoodsActionRequest.java`:

```java
package com.campushub.goods;

public record GoodsActionRequest(Long userId, Long buyerId, String note) {
}
```

- [ ] **Step 2: Extend `Goods` entity**

Replace `backend/src/main/java/com/campushub/goods/Goods.java` with:

```java
package com.campushub.goods;

import com.campushub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods")
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "condition_level", nullable = false)
    private String conditionLevel;

    @Column(name = "trade_location", nullable = false)
    private String tradeLocation;

    @Column(name = "campus_zone", nullable = false)
    private String campusZone;

    @Column(name = "contact_visibility", nullable = false)
    private String contactVisibility;

    @Column(name = "delivery_method", nullable = false)
    private String deliveryMethod;

    @Column(name = "service_fee_policy", nullable = false)
    private String serviceFeePolicy;

    @Column(nullable = false)
    private String status;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_to_user_id")
    private User soldToUser;

    protected Goods() {
    }

    public Goods(User seller, CreateGoodsRequest request) {
        this.seller = seller;
        this.categoryId = request.categoryId();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.originalPrice = request.originalPrice();
        this.conditionLevel = request.conditionLevel().trim();
        this.tradeLocation = request.tradeLocation().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.deliveryMethod = request.deliveryMethod().trim();
        this.serviceFeePolicy = "NONE";
        this.status = "PUBLISHED";
        this.viewCount = 0;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = this.publishedAt;
    }

    public Long getId() { return id; }
    public User getSeller() { return seller; }
    public Long getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getConditionLevel() { return conditionLevel; }
    public String getTradeLocation() { return tradeLocation; }
    public String getCampusZone() { return campusZone; }
    public String getContactVisibility() { return contactVisibility; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public String getServiceFeePolicy() { return serviceFeePolicy; }
    public String getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public User getSoldToUser() { return soldToUser; }

    public void update(UpdateGoodsRequest request) {
        this.categoryId = request.categoryId();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.originalPrice = request.originalPrice();
        this.conditionLevel = request.conditionLevel().trim();
        this.tradeLocation = request.tradeLocation().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.deliveryMethod = request.deliveryMethod().trim();
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount = this.viewCount + 1;
    }

    public void markSold(User buyer) {
        this.status = "SOLD";
        this.soldToUser = buyer;
        this.soldAt = LocalDateTime.now();
        this.updatedAt = this.soldAt;
    }

    public void offShelf() {
        this.status = "OFF_SHELF";
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Extend repository**

Replace `backend/src/main/java/com/campushub/goods/GoodsRepository.java` with:

```java
package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    @Override
    @EntityGraph(attributePaths = {"seller", "soldToUser"})
    Optional<Goods> findById(Long id);

    @EntityGraph(attributePaths = "seller")
    List<Goods> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "seller")
    List<Goods> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
```

- [ ] **Step 4: Add summary DTOs**

Replace `backend/src/main/java/com/campushub/goods/GoodsSummary.java` with:

```java
package com.campushub.goods;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsSummary(
        Long id,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Long sellerId,
        String sellerNickname,
        Integer sellerCreditScore,
        String tradeLocation,
        String campusZone,
        String conditionLevel,
        String status,
        Integer viewCount,
        LocalDateTime createdAt,
        String coverUrl) {

    public static GoodsSummary from(Goods goods) {
        return new GoodsSummary(
                goods.getId(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getPrice(),
                goods.getOriginalPrice(),
                goods.getSeller().getId(),
                goods.getSeller().getNickname(),
                goods.getSeller().getCreditScore(),
                goods.getTradeLocation(),
                goods.getCampusZone(),
                goods.getConditionLevel(),
                goods.getStatus(),
                goods.getViewCount(),
                goods.getCreatedAt(),
                null);
    }
}
```

Create `backend/src/main/java/com/campushub/goods/GoodsDetailSummary.java`:

```java
package com.campushub.goods;

import com.campushub.file.FileBindingSummary;
import com.campushub.interaction.CommentSummary;
import com.campushub.review.ReviewSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsDetailSummary(
        Long id,
        Long sellerId,
        String sellerNickname,
        Integer sellerCreditScore,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        String conditionLevel,
        String tradeLocation,
        String campusZone,
        String deliveryMethod,
        String contactVisibility,
        String status,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        boolean contactVisible,
        String contactSnapshot,
        List<FileBindingSummary> images,
        List<CommentSummary> comments,
        List<ReviewSummary> sellerReviews,
        long favoriteCount,
        boolean favoritedByViewer) {

    public static GoodsDetailSummary from(
            Goods goods,
            boolean contactVisible,
            String contactSnapshot,
            List<FileBindingSummary> images,
            List<CommentSummary> comments,
            List<ReviewSummary> sellerReviews,
            long favoriteCount,
            boolean favoritedByViewer) {
        return new GoodsDetailSummary(
                goods.getId(),
                goods.getSeller().getId(),
                goods.getSeller().getNickname(),
                goods.getSeller().getCreditScore(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getPrice(),
                goods.getOriginalPrice(),
                goods.getConditionLevel(),
                goods.getTradeLocation(),
                goods.getCampusZone(),
                goods.getDeliveryMethod(),
                goods.getContactVisibility(),
                goods.getStatus(),
                goods.getViewCount(),
                goods.getCreatedAt(),
                goods.getPublishedAt(),
                goods.getUpdatedAt(),
                contactVisible,
                contactSnapshot,
                images,
                comments,
                sellerReviews,
                favoriteCount,
                favoritedByViewer);
    }
}
```

- [ ] **Step 5: Add `GoodsService` role gate and detail mapping**

Create `backend/src/main/java/com/campushub/goods/GoodsService.java`:

```java
package com.campushub.goods;

import com.campushub.common.BusinessException;
import com.campushub.file.FileBindingRepository;
import com.campushub.file.FileBindingSummary;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.interaction.CommentRepository;
import com.campushub.interaction.CommentSummary;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.review.ReviewRepository;
import com.campushub.review.ReviewSummary;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final FileBindingRepository fileBindingRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;

    public GoodsService(
            GoodsRepository goodsRepository,
            UserRepository userRepository,
            RoleApplicationRepository roleApplicationRepository,
            FileBindingRepository fileBindingRepository,
            CommentRepository commentRepository,
            FavoriteRepository favoriteRepository,
            ReviewRepository reviewRepository) {
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.fileBindingRepository = fileBindingRepository;
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<GoodsSummary> listPublished() {
        return goodsRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .map(GoodsSummary::from)
                .toList();
    }

    @Transactional
    public GoodsDetailSummary publish(Long sellerId, CreateGoodsRequest request) {
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new BusinessException("用户不存在"));
        ensureGoodsPublisher(sellerId);
        Goods goods = goodsRepository.save(new Goods(seller, request));
        return detailFor(goods, sellerId, false);
    }

    @Transactional
    public GoodsDetailSummary getDetail(Long goodsId, Long viewerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        goods.increaseViewCount();
        return detailFor(goods, viewerId, true);
    }

    @Transactional
    public GoodsDetailSummary update(Long goodsId, Long sellerId, UpdateGoodsRequest request) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        goods.update(request);
        return detailFor(goods, sellerId, false);
    }

    @Transactional
    public GoodsDetailSummary offShelf(Long goodsId, Long sellerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        goods.offShelf();
        return detailFor(goods, sellerId, false);
    }

    private void ensureGoodsPublisher(Long userId) {
        roleApplicationRepository.findByUserIdAndRoleType(userId, "GOODS_PUBLISHER")
                .filter(application -> "PAID".equals(application.getDepositStatus()))
                .filter(application -> "APPROVED".equals(application.getReviewStatus()))
                .orElseThrow(() -> new BusinessException("请先开通二手发布者身份"));
    }

    private void ensureSeller(Goods goods, Long sellerId) {
        if (!goods.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("只能操作自己的商品");
        }
    }

    private GoodsDetailSummary detailFor(Goods goods, Long viewerId, boolean includeViewerState) {
        boolean owner = viewerId != null && goods.getSeller().getId().equals(viewerId);
        List<FileBindingSummary> images = fileBindingRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc("GOODS", goods.getId()).stream()
                .map(FileBindingSummary::from)
                .toList();
        List<CommentSummary> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("GOODS", goods.getId()).stream()
                .map(CommentSummary::from)
                .toList();
        List<ReviewSummary> reviews = reviewRepository.findTop5ByTargetUserIdOrderByCreatedAtDesc(goods.getSeller().getId()).stream()
                .map(ReviewSummary::from)
                .toList();
        long favoriteCount = favoriteRepository.countByTargetTypeAndTargetId("GOODS", goods.getId());
        boolean favorited = includeViewerState && viewerId != null
                && favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(viewerId, "GOODS", goods.getId());
        String contactSnapshot = owner ? buildContactSnapshot(goods.getSeller()) : null;
        return GoodsDetailSummary.from(goods, owner, contactSnapshot, images, comments, reviews, favoriteCount, favorited);
    }

    private String buildContactSnapshot(User seller) {
        String wechat = seller.getWechatContact() == null ? "" : "微信:" + seller.getWechatContact();
        String qq = seller.getQqContact() == null ? "" : "QQ:" + seller.getQqContact();
        String joined = (wechat + " " + qq).trim();
        if (joined.isEmpty()) {
            throw new BusinessException("卖家联系方式缺失");
        }
        return joined;
    }
}
```

- [ ] **Step 6: Update controller**

Replace `backend/src/main/java/com/campushub/goods/GoodsController.java` with:

```java
package com.campushub.goods;

import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public ApiResponse<List<GoodsSummary>> listGoods() {
        return ApiResponse.ok(goodsService.listPublished());
    }

    @PostMapping
    public ApiResponse<GoodsDetailSummary> publish(@RequestParam Long sellerId, @Valid @RequestBody CreateGoodsRequest request) {
        return ApiResponse.ok(goodsService.publish(sellerId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> getGoods(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        return ApiResponse.ok(goodsService.getDetail(id, viewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> update(@PathVariable Long id, @RequestParam Long sellerId, @Valid @RequestBody UpdateGoodsRequest request) {
        return ApiResponse.ok(goodsService.update(id, sellerId, request));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<GoodsDetailSummary> offShelf(@PathVariable Long id, @RequestBody GoodsActionRequest request) {
        return ApiResponse.ok(goodsService.offShelf(id, request.userId()));
    }
}
```

- [ ] **Step 7: Run publishing test**

Run:

```bash
mvn -f backend/pom.xml -Dtest=GoodsPublishingIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/goods backend/src/test/java/com/campushub/goods/GoodsPublishingIntegrationTest.java
git commit -m "gate secondhand publishing by publisher deposit"
```

### Task 3: Implement goods purchase intent and contact reveal

**Files:**
- Create: `backend/src/main/java/com/campushub/goods/GoodsIntent.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsIntentRepository.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsIntentRequest.java`
- Create: `backend/src/main/java/com/campushub/goods/GoodsIntentSummary.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsService.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsController.java`
- Test: `backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java`

- [ ] **Step 1: Write failing contact visibility tests**

Create `backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java`:

```java
package com.campushub.goods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsIntentContactIntegrationTest {

    @Autowired GoodsService goodsService;
    @Autowired IdentityService identityService;
    @Autowired UserRepository userRepository;

    @Test
    void buyerSeesContactOnlyAfterCreatingIntent() {
        User seller = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User buyer = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        identityService.apply(seller.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));
        GoodsDetailSummary goods = goodsService.publish(seller.getId(), request());

        GoodsDetailSummary beforeIntent = goodsService.getDetail(goods.id(), buyer.getId());
        assertThat(beforeIntent.contactVisible()).isFalse();
        assertThat(beforeIntent.contactSnapshot()).isNull();

        GoodsIntentSummary intent = goodsService.createIntent(goods.id(), buyer.getId(), new GoodsIntentRequest("想今天晚上交易"));
        assertThat(intent.contactSnapshot()).contains("微信");

        GoodsDetailSummary afterIntent = goodsService.getDetail(goods.id(), buyer.getId());
        assertThat(afterIntent.contactVisible()).isTrue();
        assertThat(afterIntent.contactSnapshot()).contains("微信");
    }

    @Test
    void soldGoodsRejectsNewIntent() {
        User seller = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User buyer = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        identityService.apply(seller.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));
        GoodsDetailSummary goods = goodsService.publish(seller.getId(), request());
        goodsService.markSold(goods.id(), seller.getId(), buyer.getId());

        assertThatThrownBy(() -> goodsService.createIntent(goods.id(), buyer.getId(), new GoodsIntentRequest("还能买吗")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品不可交易");
    }

    private CreateGoodsRequest request() {
        return new CreateGoodsRequest(
                1L,
                "九成新机械键盘",
                "青轴，配件齐全，支持中区当面交易",
                new BigDecimal("199.00"),
                new BigDecimal("399.00"),
                "九成新",
                "CENTRAL",
                "中校区宿舍楼下",
                "OFFLINE_MEETUP",
                "INTENT_ONLY");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=GoodsIntentContactIntegrationTest test
```

Expected: FAIL because intent classes and service methods do not exist.

- [ ] **Step 3: Add intent entity and repository**

Create `backend/src/main/java/com/campushub/goods/GoodsIntent.java`:

```java
package com.campushub.goods;

import com.campushub.payment.ServiceFeeRecord;
import com.campushub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "goods_intents")
public class GoodsIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    private String message;

    @Column(name = "contact_snapshot", nullable = false)
    private String contactSnapshot;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_fee_id")
    private ServiceFeeRecord serviceFee;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected GoodsIntent() {
    }

    public GoodsIntent(Goods goods, User buyer, String message, String contactSnapshot) {
        this.goods = goods;
        this.buyer = buyer;
        this.seller = goods.getSeller();
        this.message = message;
        this.contactSnapshot = contactSnapshot;
        this.status = "OPEN";
    }

    public Long getId() { return id; }
    public Goods getGoods() { return goods; }
    public User getBuyer() { return buyer; }
    public User getSeller() { return seller; }
    public String getMessage() { return message; }
    public String getContactSnapshot() { return contactSnapshot; }
    public String getStatus() { return status; }
    public ServiceFeeRecord getServiceFee() { return serviceFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void complete() {
        this.status = "COMPLETED";
    }
}
```

Create `backend/src/main/java/com/campushub/goods/GoodsIntentRepository.java`:

```java
package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsIntentRepository extends JpaRepository<GoodsIntent, Long> {

    @EntityGraph(attributePaths = {"goods", "buyer", "seller", "serviceFee"})
    Optional<GoodsIntent> findByGoodsIdAndBuyerId(Long goodsId, Long buyerId);

    @EntityGraph(attributePaths = {"goods", "buyer", "seller", "serviceFee"})
    List<GoodsIntent> findByGoodsIdOrderByCreatedAtDesc(Long goodsId);
}
```

- [ ] **Step 4: Add intent DTOs**

Create `backend/src/main/java/com/campushub/goods/GoodsIntentRequest.java`:

```java
package com.campushub.goods;

import jakarta.validation.constraints.Size;

public record GoodsIntentRequest(@Size(max = 500) String message) {
}
```

Create `backend/src/main/java/com/campushub/goods/GoodsIntentSummary.java`:

```java
package com.campushub.goods;

import java.time.LocalDateTime;

public record GoodsIntentSummary(
        Long id,
        Long goodsId,
        Long buyerId,
        String buyerNickname,
        Long sellerId,
        String sellerNickname,
        String message,
        String contactSnapshot,
        String status,
        Long serviceFeeId,
        LocalDateTime createdAt) {

    public static GoodsIntentSummary from(GoodsIntent intent) {
        return new GoodsIntentSummary(
                intent.getId(),
                intent.getGoods().getId(),
                intent.getBuyer().getId(),
                intent.getBuyer().getNickname(),
                intent.getSeller().getId(),
                intent.getSeller().getNickname(),
                intent.getMessage(),
                intent.getContactSnapshot(),
                intent.getStatus(),
                intent.getServiceFee() == null ? null : intent.getServiceFee().getId(),
                intent.getCreatedAt());
    }
}
```

- [ ] **Step 5: Add intent methods to `GoodsService`**

Modify `GoodsService` constructor to accept `GoodsIntentRepository goodsIntentRepository` and `NotificationService notificationService`. Add fields and these methods:

```java
@Transactional
public GoodsIntentSummary createIntent(Long goodsId, Long buyerId, GoodsIntentRequest request) {
    Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
    User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("用户不存在"));
    if (!"PUBLISHED".equals(goods.getStatus())) {
        throw new BusinessException("商品不可交易");
    }
    if (goods.getSeller().getId().equals(buyerId)) {
        throw new BusinessException("不能对自己的商品提交购买意向");
    }
    goodsIntentRepository.findByGoodsIdAndBuyerId(goodsId, buyerId).ifPresent(existing -> {
        throw new BusinessException("已提交过购买意向");
    });
    GoodsIntent intent = goodsIntentRepository.save(new GoodsIntent(goods, buyer, request.message(), buildContactSnapshot(goods.getSeller())));
    notificationService.notify(goods.getSeller(), "收到二手购买意向", buyer.getNickname() + " 想要购买你的商品：" + goods.getTitle(), "GOODS", goods.getId());
    return GoodsIntentSummary.from(intent);
}

@Transactional(readOnly = true)
public List<GoodsIntentSummary> listIntents(Long goodsId, Long sellerId) {
    Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
    ensureSeller(goods, sellerId);
    return goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goodsId).stream()
            .map(GoodsIntentSummary::from)
            .toList();
}

@Transactional
public GoodsDetailSummary markSold(Long goodsId, Long sellerId, Long buyerId) {
    Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
    ensureSeller(goods, sellerId);
    User buyer = buyerId == null ? null : userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("买家不存在"));
    goods.markSold(buyer);
    if (buyer != null) {
        goodsIntentRepository.findByGoodsIdAndBuyerId(goodsId, buyerId).ifPresent(GoodsIntent::complete);
        notificationService.notify(buyer, "二手商品已标记成交", goods.getTitle() + " 已由卖家标记成交", "GOODS", goods.getId());
    }
    return detailFor(goods, sellerId, false);
}
```

Update `detailFor` contact logic:

```java
String contactSnapshot = null;
boolean contactVisible = owner;
if (owner) {
    contactSnapshot = buildContactSnapshot(goods.getSeller());
} else if (viewerId != null) {
    contactSnapshot = goodsIntentRepository.findByGoodsIdAndBuyerId(goods.getId(), viewerId)
            .map(GoodsIntent::getContactSnapshot)
            .orElse(null);
    contactVisible = contactSnapshot != null;
}
return GoodsDetailSummary.from(goods, contactVisible, contactSnapshot, images, comments, reviews, favoriteCount, favorited);
```

- [ ] **Step 6: Add controller endpoints**

Add to `GoodsController`:

```java
@PostMapping("/{id}/intents")
public ApiResponse<GoodsIntentSummary> createIntent(@PathVariable Long id, @RequestParam Long buyerId, @Valid @RequestBody GoodsIntentRequest request) {
    return ApiResponse.ok(goodsService.createIntent(id, buyerId, request));
}

@GetMapping("/{id}/intents")
public ApiResponse<List<GoodsIntentSummary>> listIntents(@PathVariable Long id, @RequestParam Long sellerId) {
    return ApiResponse.ok(goodsService.listIntents(id, sellerId));
}

@PostMapping("/{id}/mark-sold")
public ApiResponse<GoodsDetailSummary> markSold(@PathVariable Long id, @RequestBody GoodsActionRequest request) {
    return ApiResponse.ok(goodsService.markSold(id, request.userId(), request.buyerId()));
}
```

- [ ] **Step 7: Run intent tests**

```bash
mvn -f backend/pom.xml -Dtest=GoodsIntentContactIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/goods backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java
git commit -m "add secondhand purchase intents and contact reveal"
```

### Task 4: Add goods image upload and binding support

**Files:**
- Modify: `backend/src/main/java/com/campushub/file/FileBindingRepository.java`
- Modify: `backend/src/main/java/com/campushub/file/FileBindingSummary.java`
- Create: `backend/src/main/java/com/campushub/file/BindFileRequest.java`
- Create: `backend/src/main/java/com/campushub/file/FileUploadService.java`
- Modify: `backend/src/main/java/com/campushub/file/FileController.java`
- Test: `backend/src/test/java/com/campushub/file/GoodsFileBindingIntegrationTest.java`

- [ ] **Step 1: Write failing goods image binding test**

Create `backend/src/test/java/com/campushub/file/GoodsFileBindingIntegrationTest.java`:

```java
package com.campushub.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.goods.CreateGoodsRequest;
import com.campushub.goods.GoodsDetailSummary;
import com.campushub.goods.GoodsService;
import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsFileBindingIntegrationTest {

    @Autowired GoodsService goodsService;
    @Autowired IdentityService identityService;
    @Autowired FileUploadService fileUploadService;
    @Autowired UserRepository userRepository;

    @Test
    void bindsExistingImageToGoodsDetail() {
        User seller = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        identityService.apply(seller.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));
        GoodsDetailSummary goods = goodsService.publish(seller.getId(), request());

        FileBindingSummary binding = fileUploadService.bindExisting(new BindFileRequest(
                1L,
                "GOODS",
                goods.id(),
                "COVER",
                1));

        GoodsDetailSummary detail = goodsService.getDetail(goods.id(), seller.getId());
        assertThat(binding.targetType()).isEqualTo("GOODS");
        assertThat(detail.images()).hasSize(1);
        assertThat(detail.images().get(0).fileName()).isNotBlank();
    }

    private CreateGoodsRequest request() {
        return new CreateGoodsRequest(1L, "九成新机械键盘", "青轴，配件齐全", new BigDecimal("199.00"), new BigDecimal("399.00"), "九成新", "CENTRAL", "中校区宿舍楼下", "OFFLINE_MEETUP", "INTENT_ONLY");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=GoodsFileBindingIntegrationTest test
```

Expected: FAIL because `FileUploadService` and `BindFileRequest` do not exist.

- [ ] **Step 3: Add file binding request and service**

Create `backend/src/main/java/com/campushub/file/BindFileRequest.java`:

```java
package com.campushub.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BindFileRequest(
        @NotNull Long fileId,
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank String usageType,
        @NotNull Integer sortOrder) {
}
```

Create `backend/src/main/java/com/campushub/file/FileUploadService.java`:

```java
package com.campushub.file;

import com.campushub.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileUploadService {

    private final FileResourceRepository fileResourceRepository;
    private final FileBindingRepository fileBindingRepository;

    public FileUploadService(FileResourceRepository fileResourceRepository, FileBindingRepository fileBindingRepository) {
        this.fileResourceRepository = fileResourceRepository;
        this.fileBindingRepository = fileBindingRepository;
    }

    @Transactional
    public FileBindingSummary bindExisting(BindFileRequest request) {
        FileResource file = fileResourceRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException("文件不存在"));
        long existingCount = fileBindingRepository.countByTargetTypeAndTargetId(request.targetType(), request.targetId());
        if ("GOODS".equals(request.targetType()) && existingCount >= 9) {
            throw new BusinessException("每个商品最多上传 9 张图片");
        }
        FileBinding binding = new FileBinding(file, request.targetType(), request.targetId(), request.usageType(), request.sortOrder());
        return FileBindingSummary.from(fileBindingRepository.save(binding));
    }
}
```

- [ ] **Step 4: Update file repositories and DTOs**

Modify `FileBindingRepository` to include:

```java
@EntityGraph(attributePaths = "file")
List<FileBinding> findByTargetTypeAndTargetIdOrderBySortOrderAsc(String targetType, Long targetId);

long countByTargetTypeAndTargetId(String targetType, Long targetId);
```

Modify `FileBindingSummary` so the record includes file display fields:

```java
public record FileBindingSummary(
        Long id,
        Long fileId,
        String fileName,
        String storagePath,
        String contentType,
        String targetType,
        Long targetId,
        String usageType,
        Integer sortOrder) {
    public static FileBindingSummary from(FileBinding binding) {
        return new FileBindingSummary(
                binding.getId(),
                binding.getFile().getId(),
                binding.getFile().getOriginalName(),
                binding.getFile().getStoragePath(),
                binding.getFile().getContentType(),
                binding.getTargetType(),
                binding.getTargetId(),
                binding.getUsageType(),
                binding.getSortOrder());
    }
}
```

- [ ] **Step 5: Add binding endpoint**

Modify `FileController` constructor to accept `FileUploadService fileUploadService`, then add:

```java
@PostMapping("/bindings")
public ApiResponse<FileBindingSummary> bind(@Valid @RequestBody BindFileRequest request) {
    return ApiResponse.ok(fileUploadService.bindExisting(request));
}
```

- [ ] **Step 6: Run file binding test**

```bash
mvn -f backend/pom.xml -Dtest=GoodsFileBindingIntegrationTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/file backend/src/test/java/com/campushub/file/GoodsFileBindingIntegrationTest.java
git commit -m "add goods image binding support"
```

### Task 5: Add comments, favorites, reports, and reviews for goods

**Files:**
- Create: `backend/src/main/java/com/campushub/interaction/CommentRequest.java`
- Create: `backend/src/main/java/com/campushub/interaction/FavoriteRequest.java`
- Modify: `backend/src/main/java/com/campushub/interaction/Comment.java`
- Modify: `backend/src/main/java/com/campushub/interaction/CommentRepository.java`
- Modify: `backend/src/main/java/com/campushub/interaction/FavoriteRepository.java`
- Modify: `backend/src/main/java/com/campushub/interaction/InteractionController.java`
- Create: `backend/src/main/java/com/campushub/review/Review.java`
- Create: `backend/src/main/java/com/campushub/review/ReviewRepository.java`
- Create: `backend/src/main/java/com/campushub/review/ReviewRequest.java`
- Create: `backend/src/main/java/com/campushub/review/ReviewSummary.java`
- Create: `backend/src/main/java/com/campushub/review/ReviewService.java`
- Create: `backend/src/main/java/com/campushub/review/ReviewController.java`
- Create: `backend/src/main/java/com/campushub/moderation/ReportRequest.java`
- Modify: `backend/src/main/java/com/campushub/moderation/ModerationController.java`
- Test: `backend/src/test/java/com/campushub/interaction/GoodsInteractionIntegrationTest.java`
- Test: `backend/src/test/java/com/campushub/review/GoodsReviewIntegrationTest.java`
- Test: `backend/src/test/java/com/campushub/moderation/GoodsReportIntegrationTest.java`

- [ ] **Step 1: Write failing interaction test**

Create `backend/src/test/java/com/campushub/interaction/GoodsInteractionIntegrationTest.java`:

```java
package com.campushub.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsInteractionIntegrationTest {

    @Autowired InteractionService interactionService;
    @Autowired CommentRepository commentRepository;
    @Autowired FavoriteRepository favoriteRepository;
    @Autowired UserRepository userRepository;

    @Test
    void userCanCommentAndFavoriteGoods() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();

        CommentSummary comment = interactionService.comment(user.getId(), new CommentRequest("GOODS", 1L, null, "还在吗？"));
        interactionService.favorite(user.getId(), new FavoriteRequest("GOODS", 1L));

        assertThat(comment.content()).isEqualTo("还在吗？");
        assertThat(commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("GOODS", 1L)).hasSize(1);
        assertThat(favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(user.getId(), "GOODS", 1L)).isTrue();
        assertThat(favoriteRepository.countByTargetTypeAndTargetId("GOODS", 1L)).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Write failing review test**

Create `backend/src/test/java/com/campushub/review/GoodsReviewIntegrationTest.java`:

```java
package com.campushub.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.goods.CreateGoodsRequest;
import com.campushub.goods.GoodsDetailSummary;
import com.campushub.goods.GoodsIntentRequest;
import com.campushub.goods.GoodsIntentSummary;
import com.campushub.goods.GoodsService;
import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsReviewIntegrationTest {

    @Autowired GoodsService goodsService;
    @Autowired IdentityService identityService;
    @Autowired ReviewService reviewService;
    @Autowired UserRepository userRepository;

    @Test
    void buyerCanReviewSellerAfterCompletedIntent() {
        User seller = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User buyer = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
        identityService.apply(seller.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));
        GoodsDetailSummary goods = goodsService.publish(seller.getId(), request());
        GoodsIntentSummary intent = goodsService.createIntent(goods.id(), buyer.getId(), new GoodsIntentRequest("想要"));
        goodsService.markSold(goods.id(), seller.getId(), buyer.getId());

        ReviewSummary review = reviewService.create(buyer.getId(), new ReviewRequest(seller.getId(), "GOODS_INTENT", intent.id(), 5, "交易顺利"));

        assertThat(review.rating()).isEqualTo(5);
        assertThat(review.targetUserId()).isEqualTo(seller.getId());
        assertThat(reviewService.listForUser(seller.getId())).hasSize(1);
    }

    private CreateGoodsRequest request() {
        return new CreateGoodsRequest(1L, "九成新机械键盘", "青轴，配件齐全", new BigDecimal("199.00"), new BigDecimal("399.00"), "九成新", "CENTRAL", "中校区宿舍楼下", "OFFLINE_MEETUP", "INTENT_ONLY");
    }
}
```

- [ ] **Step 3: Write failing report test**

Create `backend/src/test/java/com/campushub/moderation/GoodsReportIntegrationTest.java`:

```java
package com.campushub.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsReportIntegrationTest {

    @Autowired ModerationService moderationService;
    @Autowired ReportRecordRepository reportRecordRepository;
    @Autowired UserRepository userRepository;

    @Test
    void userCanReportGoods() {
        User reporter = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();

        ReportRecordSummary summary = moderationService.report(reporter.getId(), new ReportRequest("GOODS", 1L, "虚假信息", "价格和描述不符"));

        assertThat(summary.targetType()).isEqualTo("GOODS");
        assertThat(summary.status()).isEqualTo("PENDING");
        assertThat(reportRecordRepository.findAll()).isNotEmpty();
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```bash
mvn -f backend/pom.xml -Dtest=GoodsInteractionIntegrationTest,GoodsReviewIntegrationTest,GoodsReportIntegrationTest test
```

Expected: FAIL because services/DTOs do not exist.

- [ ] **Step 5: Implement interaction write service**

Create `CommentRequest`, `FavoriteRequest`, and `InteractionService` with these public methods:

```java
@Transactional
public CommentSummary comment(Long userId, CommentRequest request) { ... }

@Transactional
public void favorite(Long userId, FavoriteRequest request) { ... }

@Transactional
public void unfavorite(Long userId, FavoriteRequest request) { ... }
```

The implementation should load `User`, optionally load parent `Comment`, create `Comment`, and save `Favorite` only if it does not already exist.

Add repository methods:

```java
boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
long countByTargetTypeAndTargetId(String targetType, Long targetId);
void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
```

- [ ] **Step 6: Implement reviews**

Create `review` package mapping the existing `reviews` table. `ReviewService.create(...)` must reject duplicate reviews for the same `reviewerId + targetType + targetId`, save rating/content, and return `ReviewSummary`. For Phase 2, validate rating is 1-5 and trust the completed intent gate through the passed `GOODS_INTENT` target.

- [ ] **Step 7: Implement report submission service**

Create `ReportRequest` and add `ModerationService.report(Long reporterId, ReportRequest request)`. It should create `ReportRecord` with `status = PENDING` and return `ReportRecordSummary`.

If `ModerationService` does not exist, create it and move existing read/list behavior behind it only as needed; keep controller API compatible.

- [ ] **Step 8: Add controller endpoints**

Add to `InteractionController`:

```java
@PostMapping("/comments")
public ApiResponse<CommentSummary> comment(@RequestParam Long userId, @Valid @RequestBody CommentRequest request) { ... }

@PostMapping("/favorites")
public ApiResponse<Void> favorite(@RequestParam Long userId, @Valid @RequestBody FavoriteRequest request) { ... }

@DeleteMapping("/favorites")
public ApiResponse<Void> unfavorite(@RequestParam Long userId, @Valid @RequestBody FavoriteRequest request) { ... }
```

Create `ReviewController` under `/api/reviews`:

```java
@PostMapping
@GetMapping("/users/{userId}")
```

Add to `ModerationController`:

```java
@PostMapping("/reports")
public ApiResponse<ReportRecordSummary> report(@RequestParam Long reporterId, @Valid @RequestBody ReportRequest request) { ... }
```

- [ ] **Step 9: Run interaction/review/report tests**

```bash
mvn -f backend/pom.xml -Dtest=GoodsInteractionIntegrationTest,GoodsReviewIntegrationTest,GoodsReportIntegrationTest test
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/campushub/interaction backend/src/main/java/com/campushub/review backend/src/main/java/com/campushub/moderation backend/src/test/java/com/campushub/interaction/GoodsInteractionIntegrationTest.java backend/src/test/java/com/campushub/review/GoodsReviewIntegrationTest.java backend/src/test/java/com/campushub/moderation/GoodsReportIntegrationTest.java
git commit -m "add goods interactions reports and reviews"
```

### Task 6: Add frontend API types for second-hand Phase 2

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Test: `frontend/src/api/client.test.ts`

- [ ] **Step 1: Add TypeScript types**

In `frontend/src/api/campushub.ts`, extend goods types:

```ts
export interface FileBindingSummary {
  id: number
  fileId: number
  fileName: string
  storagePath: string
  contentType: string
  targetType: string
  targetId: number
  usageType: string
  sortOrder: number
}

export interface CommentSummary {
  id: number
  targetType: string
  targetId: number
  userId: number
  userNickname: string
  content: string
  parentId?: number | null
  status: string
  createdAt: string
}

export interface ReviewSummary {
  id: number
  reviewerId: number
  reviewerNickname: string
  targetUserId: number
  targetType: string
  targetId: number
  rating: number
  content: string
  createdAt: string
}

export interface GoodsDetailSummary extends GoodsSummary {
  sellerId: number
  sellerCreditScore: number
  originalPrice?: number | null
  campusZone: string
  deliveryMethod: string
  contactVisibility: string
  status: string
  publishedAt?: string | null
  updatedAt?: string | null
  contactVisible: boolean
  contactSnapshot?: string | null
  images: FileBindingSummary[]
  comments: CommentSummary[]
  sellerReviews: ReviewSummary[]
  favoriteCount: number
  favoritedByViewer: boolean
}

export interface CreateGoodsPayload {
  categoryId: number
  title: string
  description: string
  price: number
  originalPrice?: number | null
  conditionLevel: string
  campusZone: string
  tradeLocation: string
  deliveryMethod: string
  contactVisibility: string
}

export interface GoodsIntentSummary {
  id: number
  goodsId: number
  buyerId: number
  buyerNickname: string
  sellerId: number
  sellerNickname: string
  message?: string | null
  contactSnapshot: string
  status: string
  serviceFeeId?: number | null
  createdAt: string
}
```

- [ ] **Step 2: Add API functions**

Add functions:

```ts
export function getGoodsDetail(id: number, viewerId?: number) {
  return apiGet<GoodsDetailSummary>(`/goods/${id}`, viewerId ? { viewerId } : undefined)
}

export function publishGoods(sellerId: number, payload: CreateGoodsPayload) {
  return apiPost<GoodsDetailSummary>(`/goods?sellerId=${sellerId}`, payload)
}

export function createGoodsIntent(goodsId: number, buyerId: number, message: string) {
  return apiPost<GoodsIntentSummary>(`/goods/${goodsId}/intents?buyerId=${buyerId}`, { message })
}

export function markGoodsSold(goodsId: number, userId: number, buyerId?: number) {
  return apiPost<GoodsDetailSummary>(`/goods/${goodsId}/mark-sold`, { userId, buyerId })
}

export function bindFileToTarget(payload: { fileId: number; targetType: string; targetId: number; usageType: string; sortOrder: number }) {
  return apiPost<FileBindingSummary>('/files/bindings', payload)
}

export function commentTarget(userId: number, payload: { targetType: string; targetId: number; parentId?: number | null; content: string }) {
  return apiPost<CommentSummary>(`/interactions/comments?userId=${userId}`, payload)
}

export function favoriteTarget(userId: number, payload: { targetType: string; targetId: number }) {
  return apiPost<void>(`/interactions/favorites?userId=${userId}`, payload)
}

export function reportTarget(reporterId: number, payload: { targetType: string; targetId: number; reason: string; description: string }) {
  return apiPost(`/moderation/reports?reporterId=${reporterId}`, payload)
}

export function createReview(reviewerId: number, payload: { targetUserId: number; targetType: string; targetId: number; rating: number; content: string }) {
  return apiPost<ReviewSummary>(`/reviews?reviewerId=${reviewerId}`, payload)
}
```

- [ ] **Step 3: Run frontend build**

```bash
npm --prefix frontend run build
```

Expected: PASS with existing Vite large chunk warning only.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/campushub.ts frontend/src/api/client.test.ts
git commit -m "add secondhand frontend API types"
```

### Task 7: Redesign goods marketplace list and publish flow

**Files:**
- Replace: `frontend/src/views/GoodsView.vue`
- Create: `frontend/src/views/GoodsPublishView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Replace goods list with card marketplace**

Replace `frontend/src/views/GoodsView.vue` with a card grid that:

- Calls `listGoods()` on mount.
- Shows cover placeholder if `coverUrl` is empty.
- Shows price, original price, condition, campus zone, seller credit, views.
- Links each card to `/goods/:id`.
- Has a publish button that routes to `/goods/publish`.

Use existing Element Plus components and `router.push`.

- [ ] **Step 2: Create publish view**

Create `frontend/src/views/GoodsPublishView.vue` with:

- `authStore.user` as the current seller.
- Form fields matching `CreateGoodsPayload`.
- Submit handler calling `publishGoods(authStore.user.id, form)`.
- If no logged-in user, show login guidance.
- If publish fails with the backend role gate message, show a button to `/roles`.
- After success, route to `/goods/${detail.id}`.

- [ ] **Step 3: Add routes**

Modify `frontend/src/router/index.ts`:

```ts
{
  path: '/goods/publish',
  name: 'goods-publish',
  component: () => import('@/views/GoodsPublishView.vue'),
},
{
  path: '/goods/:id',
  name: 'goods-detail',
  component: () => import('@/views/GoodsDetailView.vue'),
},
```

Add the detail route after publish so `publish` is not parsed as `:id`.

- [ ] **Step 4: Add responsive styles**

Add styles to `frontend/src/styles.css` for `.goods-grid`, `.goods-card`, `.goods-cover`, `.goods-meta`, `.goods-publish-form`, using mobile-first wrapping and no horizontal overflow.

- [ ] **Step 5: Build frontend**

```bash
npm --prefix frontend run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/GoodsView.vue frontend/src/views/GoodsPublishView.vue frontend/src/router/index.ts frontend/src/styles.css
git commit -m "redesign goods marketplace and publish flow"
```

### Task 8: Add goods detail page with contact, interactions, reports, and reviews

**Files:**
- Create: `frontend/src/views/GoodsDetailView.vue`
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/api/campushub.ts` if any type mismatch is found

- [ ] **Step 1: Create detail view**

Create `frontend/src/views/GoodsDetailView.vue` that:

- Reads `id` from route params.
- Reads current viewer from `authStore.user`.
- Calls `getGoodsDetail(id, viewerId)`.
- Displays image gallery, title, price, original price, condition, campus zone, location, description.
- Displays seller nickname, credit score, recent reviews.
- If `contactVisible` is false, shows “提交购买意向后可查看联系方式”.
- If `contactVisible` is true, shows `contactSnapshot`.

- [ ] **Step 2: Add intent and contact action**

In `GoodsDetailView.vue`, add:

- Message input dialog.
- Button “我想要”.
- Handler calling `createGoodsIntent(goods.id, viewerId, message)`.
- After success, reload detail and show success message.

- [ ] **Step 3: Add favorite, comment, report, and review actions**

In `GoodsDetailView.vue`, add:

- Favorite button calling `favoriteTarget(viewerId, { targetType: 'GOODS', targetId: goods.id })`.
- Comment form calling `commentTarget(viewerId, { targetType: 'GOODS', targetId: goods.id, content })`.
- Report dialog calling `reportTarget(viewerId, { targetType: 'GOODS', targetId: goods.id, reason, description })`.
- Review form shown after contact/intent exists, calling `createReview(viewerId, { targetUserId: goods.sellerId, targetType: 'GOODS', targetId: goods.id, rating, content })` if the backend permits it.

- [ ] **Step 4: Add seller actions**

If current user is seller:

- Hide “我想要”.
- Show contact snapshot.
- Show “标记售出” button calling `markGoodsSold(goods.id, viewerId)`.

- [ ] **Step 5: Add responsive styles**

Add styles for `.goods-detail`, `.goods-gallery`, `.seller-panel`, `.contact-panel`, `.interaction-panel`, `.review-list`, ensuring mobile viewport 390px has no horizontal overflow.

- [ ] **Step 6: Build frontend**

```bash
npm --prefix frontend run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/GoodsDetailView.vue frontend/src/styles.css frontend/src/api/campushub.ts
git commit -m "add secondhand goods detail experience"
```

### Task 9: Add service-fee hook for future second-hand monetization

**Files:**
- Modify: `backend/src/main/java/com/campushub/goods/GoodsService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-prod.yml`
- Test: `backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java`

- [ ] **Step 1: Add failing service-fee hook test**

Add to `GoodsIntentContactIntegrationTest`:

```java
@Test
void creatingIntentDoesNotForceServiceFeeByDefault() {
    User seller = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    User buyer = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();
    identityService.apply(seller.getId(), new ApplyRoleRequest("GOODS_PUBLISHER", "发布闲置物品"));
    GoodsDetailSummary goods = goodsService.publish(seller.getId(), request());

    GoodsIntentSummary intent = goodsService.createIntent(goods.id(), buyer.getId(), new GoodsIntentRequest("想要"));

    assertThat(intent.serviceFeeId()).isNull();
}
```

- [ ] **Step 2: Run test**

```bash
mvn -f backend/pom.xml -Dtest=GoodsIntentContactIntegrationTest test
```

Expected: PASS if no fee is created, or FAIL if earlier implementation accidentally creates a fee.

- [ ] **Step 3: Add configuration properties**

In `backend/src/main/resources/application.yml`, add under `campushub`:

```yaml
  secondhand:
    service-fee:
      enabled: false
      intent-amount: 0.00
```

In `backend/src/main/resources/application-prod.yml`, add env-driven overrides:

```yaml
  secondhand:
    service-fee:
      enabled: ${CAMPUSHUB_SECONDHAND_SERVICE_FEE_ENABLED:false}
      intent-amount: ${CAMPUSHUB_SECONDHAND_SERVICE_FEE_INTENT_AMOUNT:0.00}
```

- [ ] **Step 4: Inject service-fee configuration into `GoodsService`**

Add fields to `GoodsService`:

```java
@Value("${campushub.secondhand.service-fee.enabled:false}")
private boolean secondhandServiceFeeEnabled;

@Value("${campushub.secondhand.service-fee.intent-amount:0.00}")
private BigDecimal secondhandIntentFeeAmount;
```

In `createIntent`, keep default disabled behavior. If enabled and amount is greater than zero, create a `ServiceFeeRecord` with target type `GOODS_INTENT`, save it using a new `ServiceFeeRecordRepository` dependency, and attach it to `GoodsIntent` via a method `attachServiceFee(ServiceFeeRecord serviceFee)`.

- [ ] **Step 5: Run goods intent tests**

```bash
mvn -f backend/pom.xml -Dtest=GoodsIntentContactIntegrationTest test
```

Expected: PASS, proving default production-safe behavior does not force service fees.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/campushub/goods/GoodsService.java backend/src/main/resources/application.yml backend/src/main/resources/application-prod.yml backend/src/test/java/com/campushub/goods/GoodsIntentContactIntegrationTest.java
git commit -m "add secondhand service fee hook"
```

### Task 10: End-to-end verification and deployment checkpoint

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update README**

Add a Phase 2 section documenting:

- Goods publisher 10 CNY deposit gate.
- Goods publishing/list/detail.
- Contact reveal after purchase intent.
- Image binding.
- Comments/favorites/reports/reviews.
- Offline principal transaction boundary.
- Optional service-fee hook disabled by default.

- [ ] **Step 2: Update CLAUDE.md handoff**

Update the latest handoff section with Phase 2 implementation notes, verification commands, and reminder that V6 is applied once deployed and must not be edited afterward.

- [ ] **Step 3: Run backend tests**

Run locally if Maven is available:

```bash
mvn -f backend/pom.xml test
```

Expected: PASS.

If local Maven is unavailable, run the test suite once on the server using the existing Maven container pattern, without reading secrets and without aggressive retries.

- [ ] **Step 4: Run frontend build**

```bash
npm --prefix frontend run build
```

Expected: PASS with existing Vite large chunk warning only.

- [ ] **Step 5: Commit docs**

```bash
git add README.md CLAUDE.md
git commit -m "document secondhand phase 2 upgrade"
```

- [ ] **Step 6: Push to GitHub**

```bash
git push origin master
```

- [ ] **Step 7: Low-impact server deploy**

On the server:

```bash
cd /opt/campushub
git pull --ff-only origin master
docker compose -f docker-compose.prod.yml up -d --build campushub-backend campushub-web
```

- [ ] **Step 8: Server-local API smoke tests**

Run on the server:

```bash
curl -sS -o /tmp/campushub-goods.out -w "%{http_code}\n" http://127.0.0.1:18080/api/goods
curl -sS -o /tmp/campushub-goods-detail.out -w "%{http_code}\n" http://127.0.0.1:18080/api/goods/1
curl -sS -o /tmp/campushub-roles.out -w "%{http_code}\n" http://127.0.0.1:18080/api/identity/users/1/roles
curl -sS -o /tmp/campushub-ops.out -w "%{http_code}\n" http://127.0.0.1:18080/api/admin/ops/dashboard
```

Expected: no HTTP 500. Public prototype may return 200 for open APIs or validation errors for write endpoints depending on parameters.

- [ ] **Step 9: Browser verification**

Verify desktop and 390x844 mobile viewport:

- `/goods` card grid renders.
- `/goods/1` detail renders images or placeholder, seller credit, contact prompt, comments/reviews panels.
- `/goods/publish` shows publish form or role guidance.
- `/roles` still shows goods publisher 10 CNY deposit.
- Existing Phase 1 pages `/tasks`, `/notifications`, `/admin/ops` still render.

## Self-review

- Spec coverage: This plan covers goods publisher 10 CNY deposit gating, goods metadata/schema, detail redesign data, image attachment binding, contact reveal through purchase intent, comments/favorites, reports, reviews/credit display, optional service-fee hook, docs, tests, and deployment.
- Placeholder scan: No task uses TBD/TODO placeholders. Some implementation steps are summarized where they modify existing repositories/controllers, but each defines exact public methods and expected behavior.
- Type consistency: `GoodsDetailSummary`, `GoodsIntentSummary`, `CreateGoodsRequest`, `GoodsIntentRequest`, `FileBindingSummary`, `CommentRequest`, `FavoriteRequest`, `ReviewRequest`, and `ReportRequest` names are consistent across backend, tests, and frontend API plan steps.
