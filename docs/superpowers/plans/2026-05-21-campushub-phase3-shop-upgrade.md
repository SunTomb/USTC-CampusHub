# CampusHub Phase 3 Student Shop Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade CampusHub student shops from static approved listings into a usable campus service marketplace with merchant identity gating, shop homepages, service item management, lightweight booking workflow, notifications, reviews/reports reuse, and operations monitoring.

**Architecture:** Keep service principal offline and peer-to-peer. Add focused backend services around the existing `shop`, `identity`, `notification`, `file`, `interaction`, `review`, `moderation`, and `payment` contexts; add new Flyway migrations only. Frontend changes upgrade `/shops` into a service marketplace, add shop detail and merchant workspace flows, and reuse Phase 1 identity/notification patterns plus Phase 2 interaction/review/report patterns.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, JUnit/Spring Boot Test, Vue 3, Vite, TypeScript, Pinia, Vue Router, Element Plus, Vitest.

---

## Scope and dependencies

This plan implements the design in `docs/superpowers/specs/2026-05-21-campushub-phase3-shop-design.md`.

The current codebase after Phase 2 has:

- `SHOP_MERCHANT` role/deposit support in `backend/src/main/java/com/campushub/identity/PlatformRoleType.java`, with 20 CNY deposit and manual review.
- Basic shop, service item, and service order entities/controllers in `backend/src/main/java/com/campushub/shop`.
- Station notification support in `backend/src/main/java/com/campushub/notification`.
- Generic file binding, comments, favorites, reports, reviews, service fee, and payment contexts from earlier phases.
- A simple frontend shop page in `frontend/src/views/ShopsView.vue`.

Production has already applied Flyway V1-V6. Do not edit V1-V6. All database changes in this plan must be in new migrations V7+ and matching test migrations.

---

## File structure map

### Backend migrations

- Create `backend/src/main/resources/db/migration/V7__student_shop_upgrade.sql` — shop metadata, service item category/price range, service order contact/status fields, indexes.
- Create `backend/src/test/resources/db/test-migration/V7__student_shop_upgrade.sql` — H2-compatible copy of V7.

### Backend shop package

- Modify `backend/src/main/java/com/campushub/shop/Shop.java` — constructors, merchant fields, status transitions, contact visibility, cover/opening hours.
- Modify `backend/src/main/java/com/campushub/shop/ShopRepository.java` — owner/status/filter queries with owner fetch.
- Modify `backend/src/main/java/com/campushub/shop/ShopSummary.java` — marketplace card fields.
- Create `backend/src/main/java/com/campushub/shop/ShopDetailSummary.java` — detail DTO with service items, contact visibility, stats.
- Create `backend/src/main/java/com/campushub/shop/CreateShopRequest.java` — shop creation request.
- Create `backend/src/main/java/com/campushub/shop/UpdateShopRequest.java` — shop edit request.
- Create `backend/src/main/java/com/campushub/shop/ShopActionRequest.java` — shop state request.
- Modify `backend/src/main/java/com/campushub/shop/ServiceItem.java` — category, price range, unit, cover, state transitions.
- Modify `backend/src/main/java/com/campushub/shop/ServiceItemRepository.java` — shop/status queries.
- Modify `backend/src/main/java/com/campushub/shop/ServiceItemSummary.java` — card/detail fields.
- Create `backend/src/main/java/com/campushub/shop/CreateServiceItemRequest.java` — service item creation request.
- Create `backend/src/main/java/com/campushub/shop/UpdateServiceItemRequest.java` — service item edit request.
- Modify `backend/src/main/java/com/campushub/shop/ServiceOrder.java` — booking lifecycle methods, contact snapshot, service fee id, cancel reason.
- Modify `backend/src/main/java/com/campushub/shop/ServiceOrderRepository.java` — shop/provider/customer/status queries.
- Modify `backend/src/main/java/com/campushub/shop/ServiceOrderSummary.java` — booking workflow DTO.
- Create `backend/src/main/java/com/campushub/shop/CreateServiceOrderRequest.java` — booking request.
- Create `backend/src/main/java/com/campushub/shop/ServiceOrderActionRequest.java` — booking action request.
- Create `backend/src/main/java/com/campushub/shop/ShopService.java` — merchant gate, shop detail/list, service item management, booking workflow.
- Modify `backend/src/main/java/com/campushub/shop/ShopController.java` — delegate shop APIs to `ShopService`.
- Modify `backend/src/main/java/com/campushub/shop/ServiceItemController.java` — delegate item APIs to `ShopService`.
- Modify `backend/src/main/java/com/campushub/shop/ServiceOrderController.java` — delegate booking APIs to `ShopService`.

### Backend operations/tests

- Modify `backend/src/main/java/com/campushub/ops/OperationsController.java` — add shop/order monitor endpoints.
- Create `backend/src/test/java/com/campushub/shop/ShopMerchantGateIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/shop/ServiceItemManagementIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/shop/ServiceOrderWorkflowIntegrationTest.java`.
- Create `backend/src/test/java/com/campushub/shop/ShopContactVisibilityIntegrationTest.java`.

### Frontend API and pages

- Modify `frontend/src/api/campushub.ts` — add shop detail, merchant shop, service item, booking workflow APIs/types.
- Replace or heavily modify `frontend/src/views/ShopsView.vue` — service marketplace cards, filters, merchant entry.
- Create `frontend/src/views/ShopDetailView.vue` — shop homepage, service items, booking dialog, contact rules, reviews/reports.
- Create `frontend/src/views/ShopMerchantView.vue` — merchant workspace for profile, items, bookings.
- Modify `frontend/src/router/index.ts` — add `/shops/:id` and `/shops/merchant` routes.
- Modify `frontend/src/layouts/MainLayout.vue` if needed — add merchant shortcut or keep in shop page.
- Modify `frontend/src/styles.css` — responsive shop marketplace/detail/workspace styles.

---

## Phase 3 implementation tasks

### Task 0: Preflight browser-finding fixes

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/TasksView.vue`

- [x] **Step 1: Add route fallback and legacy login redirect**

Add `/login` redirect or alias to `/auth`, and add a catch-all frontend fallback so unknown paths do not white-screen.

- [x] **Step 2: Add unauthenticated task action feedback**

In the task hall, `立即抢单` and `申请接单` must not silently use demo fallback IDs when no user is logged in. Show a clear login message and route to `/auth`.

- [x] **Step 3: Add task publish validation feedback**

Add visible validation for title, description, positive reward, deadline, and origin/destination detail.

- [x] **Step 4: Build frontend**

Run `npm --prefix frontend run build`. Expected: PASS with known Element Plus/Vite large chunk warning only.

### Task 1: Add student shop schema upgrade

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__student_shop_upgrade.sql`
- Create: `backend/src/test/resources/db/test-migration/V7__student_shop_upgrade.sql`
- Test: `backend/src/test/java/com/campushub/shop/ShopMerchantGateIntegrationTest.java`

- [ ] **Step 1: Write failing merchant gate test**

Create a test proving a user without approved `SHOP_MERCHANT` role cannot create a shop, and a user with paid + approved `SHOP_MERCHANT` can create one.

- [ ] **Step 2: Run failing test**

Run `mvn -f backend/pom.xml -Dtest=ShopMerchantGateIntegrationTest test`.

Expected: FAIL because `ShopService` creation workflow and V7 fields do not exist yet.

- [ ] **Step 3: Add production Flyway migration V7**

Create `V7__student_shop_upgrade.sql` with additive changes only. Suggested production SQL:

```sql
ALTER TABLE shops
    ADD COLUMN campus_zone VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER service_area,
    ADD COLUMN contact_visibility VARCHAR(40) NOT NULL DEFAULT 'ORDER_ONLY' AFTER campus_zone,
    ADD COLUMN opening_hours VARCHAR(255) NULL AFTER contact_visibility,
    ADD COLUMN cover_file_id BIGINT NULL AFTER opening_hours,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_shop_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_items
    ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'OTHER' AFTER shop_id,
    ADD COLUMN min_price DECIMAL(10,2) NULL AFTER description,
    ADD COLUMN max_price DECIMAL(10,2) NULL AFTER min_price,
    ADD COLUMN price_unit VARCHAR(30) NOT NULL DEFAULT '次' AFTER price,
    ADD COLUMN cover_file_id BIGINT NULL AFTER price_unit,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_service_item_cover_file FOREIGN KEY (cover_file_id) REFERENCES file_resources(id);

ALTER TABLE service_orders
    ADD COLUMN contact_snapshot VARCHAR(255) NULL AFTER note,
    ADD COLUMN cancel_reason VARCHAR(500) NULL AFTER contact_snapshot,
    ADD COLUMN service_fee_id BIGINT NULL AFTER cancel_reason,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_service_order_fee FOREIGN KEY (service_fee_id) REFERENCES service_fee_records(id);

CREATE INDEX idx_shops_status_zone_rating ON shops (status, campus_zone, rating);
CREATE INDEX idx_service_items_shop_status ON service_items (shop_id, status);
CREATE INDEX idx_service_items_category_status ON service_items (category, status);
CREATE INDEX idx_service_orders_provider_status_time ON service_orders (provider_id, status, appointment_time);
CREATE INDEX idx_service_orders_customer_time ON service_orders (customer_id, appointment_time);
```

- [ ] **Step 4: Add H2-compatible test migration V7**

Create a semantically equivalent H2 migration with separate `ALTER TABLE ... ADD COLUMN ...` statements and compatible timestamp defaults.

- [ ] **Step 5: Commit schema and failing test**

Commit as `add student shop schema upgrade`.

### Task 2: Implement merchant-gated shop creation and detail APIs

**Files:**
- Modify: `backend/src/main/java/com/campushub/shop/Shop.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopRepository.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopSummary.java`
- Create: `backend/src/main/java/com/campushub/shop/ShopDetailSummary.java`
- Create: `backend/src/main/java/com/campushub/shop/CreateShopRequest.java`
- Create: `backend/src/main/java/com/campushub/shop/UpdateShopRequest.java`
- Create: `backend/src/main/java/com/campushub/shop/ShopActionRequest.java`
- Create: `backend/src/main/java/com/campushub/shop/ShopService.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopController.java`
- Test: `backend/src/test/java/com/campushub/shop/ShopMerchantGateIntegrationTest.java`

- [ ] **Step 1: Add request/summary records**

Add create/update/action request records with validation for name, description, service area, campus zone, contact visibility, and opening hours.

- [ ] **Step 2: Extend `Shop` entity**

Add constructor for new shops, update method, and state transitions: approve/live default for approved merchants, pause, resume, block, close. Keep status strings aligned with the design.

- [ ] **Step 3: Add repository queries**

Fetch owner for list/detail mapping. Add owner lookup so a merchant can retrieve/manage their own shop.

- [ ] **Step 4: Add `ShopService` merchant gate**

`createShop(ownerId, request)` must require approved `SHOP_MERCHANT` role. Reuse `RoleApplicationRepository` and throw `BusinessException("请先开通店铺商家身份")` or equivalent when missing.

- [ ] **Step 5: Update controller**

Expose list, create, detail, update, pause/resume/close endpoints through `ShopService`.

- [ ] **Step 6: Run test**

Run `mvn -f backend/pom.xml -Dtest=ShopMerchantGateIntegrationTest test`.

Expected: PASS.

- [ ] **Step 7: Commit**

Commit as `gate shop creation by merchant approval`.

### Task 3: Implement service item management

**Files:**
- Modify: `backend/src/main/java/com/campushub/shop/ServiceItem.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceItemRepository.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceItemSummary.java`
- Create: `backend/src/main/java/com/campushub/shop/CreateServiceItemRequest.java`
- Create: `backend/src/main/java/com/campushub/shop/UpdateServiceItemRequest.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopService.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceItemController.java`
- Test: `backend/src/test/java/com/campushub/shop/ServiceItemManagementIntegrationTest.java`

- [ ] **Step 1: Write failing service item tests**

Test that only the shop owner can create/edit/publish/pause/off-shelf service items, and published items appear in shop detail.

- [ ] **Step 2: Add item request DTOs and entity methods**

Support category, title, description, min/max price, legacy price, price unit, duration minutes, and cover file id.

- [ ] **Step 3: Add service methods**

Implement create, update, publish, pause, off-shelf, and list by shop. Reject operations when shop is not merchant-owned or blocked.

- [ ] **Step 4: Update item controller**

Expose merchant item management endpoints and public item list/detail as needed.

- [ ] **Step 5: Run tests**

Run `mvn -f backend/pom.xml -Dtest=ServiceItemManagementIntegrationTest test`.

Expected: PASS.

- [ ] **Step 6: Commit**

Commit as `add merchant service item management`.

### Task 4: Implement lightweight service booking workflow

**Files:**
- Modify: `backend/src/main/java/com/campushub/shop/ServiceOrder.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceOrderRepository.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceOrderSummary.java`
- Create: `backend/src/main/java/com/campushub/shop/CreateServiceOrderRequest.java`
- Create: `backend/src/main/java/com/campushub/shop/ServiceOrderActionRequest.java`
- Modify: `backend/src/main/java/com/campushub/shop/ShopService.java`
- Modify: `backend/src/main/java/com/campushub/shop/ServiceOrderController.java`
- Test: `backend/src/test/java/com/campushub/shop/ServiceOrderWorkflowIntegrationTest.java`
- Test: `backend/src/test/java/com/campushub/shop/ShopContactVisibilityIntegrationTest.java`

- [ ] **Step 1: Write failing booking workflow tests**

Cover: user submits booking, merchant receives order, merchant accepts/rejects, completion/cancel transitions, and unauthorized users cannot process orders.

- [ ] **Step 2: Add order request/action DTOs**

`CreateServiceOrderRequest` should include service item id, appointment time, note, and optional estimated amount. Action request should include actor id, note, cancel reason.

- [ ] **Step 3: Extend `ServiceOrder` lifecycle**

Support `REQUESTED`, `ACCEPTED`, `REJECTED`, `IN_SERVICE`, `COMPLETED`, `CANCELED`, `DISPUTE_HANDLING` with timestamps and contact snapshot.

- [ ] **Step 4: Add `ShopService` booking methods**

Implement create booking, list merchant/customer bookings, accept, reject, start, complete, cancel. Create station notifications for merchant/customer on key changes.

- [ ] **Step 5: Add contact visibility logic**

Shop detail should reveal merchant contact snapshot to a customer only after that customer has an order for the shop/service item; merchant always sees own contact.

- [ ] **Step 6: Update order controller**

Expose booking endpoints for customer and merchant workflows.

- [ ] **Step 7: Run tests**

Run `mvn -f backend/pom.xml -Dtest=ServiceOrderWorkflowIntegrationTest,ShopContactVisibilityIntegrationTest test`.

Expected: PASS.

- [ ] **Step 8: Commit**

Commit as `add student shop booking workflow`.

### Task 5: Upgrade frontend shop marketplace and detail flow

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/ShopsView.vue`
- Create: `frontend/src/views/ShopDetailView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add frontend API types/functions**

Add shop detail, service item, booking, and action payload types and API functions.

- [ ] **Step 2: Upgrade shops marketplace**

Replace simple display with cards, filters, merchant identity guidance, and detail links.

- [ ] **Step 3: Add shop detail page**

Show shop header, service items, contact rule, booking dialog, reviews/reports hooks, and responsive layout.

- [ ] **Step 4: Add routes**

Add `/shops/:id` route. Ensure `/shops/merchant` route is ordered before dynamic `:id` if both exist.

- [ ] **Step 5: Build frontend**

Run `npm --prefix frontend run build`.

Expected: PASS with known large chunk warning only.

- [ ] **Step 6: Commit**

Commit as `upgrade student shop marketplace UI`.

### Task 6: Add merchant workspace UI and operations monitor

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Create: `frontend/src/views/ShopMerchantView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`
- Modify: `backend/src/main/java/com/campushub/ops/OperationsController.java`

- [ ] **Step 1: Add merchant workspace APIs**

Expose frontend calls for my shop, create/update shop, create/update service items, list orders, and process bookings.

- [ ] **Step 2: Create merchant workspace page**

Tabs: shop profile, service items, booking orders, basic metrics. Include clear identity guidance if not approved as merchant.

- [ ] **Step 3: Add operations monitor endpoint/UI hook**

Backend ops should expose shop orders and abnormal shop/order status lists. Existing admin ops page can add a tab or table if scope permits.

- [ ] **Step 4: Build frontend and run targeted backend tests**

Run frontend build and targeted backend tests for shop package where available.

- [ ] **Step 5: Commit**

Commit as `add shop merchant workspace`.

### Task 7: End-to-end verification and deployment checkpoint

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README**

Document Phase 3 student shops, merchant deposit boundary, booking workflow, no principal escrow, and production deployment caution.

- [ ] **Step 2: Run frontend build**

Run `npm --prefix frontend run build`.

Expected: PASS with known large chunk warning only.

- [ ] **Step 3: Run backend verification**

If Maven is available, run `mvn -f backend/pom.xml test`. If local Maven is unavailable, use low-impact server-side Docker/backend build only after user authorizes deployment work.

- [ ] **Step 4: Browser verification**

Verify: `/shops`, `/shops/:id`, `/shops/merchant`, booking dialog, merchant order processing, mobile viewport, plus preflight routes `/login` and unknown path.

- [ ] **Step 5: Commit docs**

Commit as `document student shop phase 3 upgrade`.

- [ ] **Step 6: Push and deploy only after verification**

Do not push/deploy until build/test results are clean and user agrees. Deployment must be low-impact: pull, rebuild only necessary services, server-local API smoke, then Playwriter browser verification.

## Self-review

- Spec coverage: This plan covers merchant approval/deposit gating, shop homepage, service item management, lightweight booking workflow, contact snapshots, station notifications, operations visibility, responsive web, and payment boundary.
- Migration safety: Uses V7+ only; explicitly avoids editing V1-V6, important because production V6 had a repaired Flyway incident.
- Payment boundary: No service principal escrow or Alipay key handling is introduced.
- Scope control: Complex scheduling, chat, online principal payment, mini-program, and automatic recommendation are excluded.
