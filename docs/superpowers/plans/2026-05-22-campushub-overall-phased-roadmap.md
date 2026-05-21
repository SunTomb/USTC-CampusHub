# CampusHub Overall Phased Roadmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a complete, balanced, Phase-4-sized roadmap for finishing CampusHub as a real campus service platform after the deployed Phase 1-4 business lines.

**Architecture:** Treat each future phase as one independently shippable subsystem with roughly the same size as Phase 4: one focused design/spec document, one implementation plan, one schema migration only when necessary, backend APIs, frontend/admin UI, README/CLAUDE handoff, server-side Docker verification, API smoke, and Playwriter browser verification. Existing bounded contexts remain the source of truth; new work should extend focused packages and reuse shared target models for files, comments, favorites, reports, reviews, notifications, wallet/service-fee records, and audit logs.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, Vue 3, Vite, TypeScript, Pinia, Vue Router, Axios, Element Plus, Docker Compose, Playwriter browser verification.

---

## 0. Current deployed baseline

Production `master` has completed the four business-line phases:

- Phase 1: running tasks, contact registration, role deposits, station notifications, campus zones, task workflow, and operations foundation.
- Phase 2: second-hand marketplace with publisher gate, images, contact intent, comments/favorites/reports/reviews, and service-fee hooks.
- Phase 3: student shops with merchant gate, shop/service item management, lightweight bookings, contact snapshots, merchant workspace, and shop-order operations monitor.
- Phase 4: project ads/campus showcase with publishing, review, expiration, tags, contact visibility, featured slots, detail aggregation, publisher management, and project-ad operations monitor.

Latest verified production implementation commit: `835e850`.
Latest documentation handoff commit: `46a5435`.
Production database has applied Flyway migrations V1-V9. Future schema changes must use V10+.

## 1. Roadmap sizing rule

Each future phase should be close to Phase 4 in scope:

- 1 design spec under `docs/superpowers/specs/`.
- 1 implementation plan under `docs/superpowers/plans/`.
- 6-10 implementation tasks.
- At most one major new backend bounded-context expansion per phase.
- At most one major frontend workspace/page group per phase.
- Server-side Docker build, API smoke, and Playwriter verification before claiming completion.
- No local dependency installation unless the user explicitly changes the current preference.

A phase is too large if it tries to deliver more than one of: governance system, analytics/export system, payment-center integration, mobile UX overhaul, security/auth hardening, or production operations automation. Split those into separate phases.

## 2. Phase 5: Governance, credit, and trust operations

**Objective:** Build the cross-business governance layer needed to safely run real campus beta usage across tasks, goods, shops, and project ads.

**Why now:** Phase 1-4 created four content/transaction surfaces. The next bottleneck is trust: reports, violations, credit adjustment, blacklisting/freezing, and admin auditability currently exist only as scattered prototypes.

**Scope:**

1. Unified report handling queue across `GOODS`, `REWARD_TASK`, `SHOP`, `SERVICE_ORDER`, `PROJECT_AD`, `COMMENT`, and `USER` targets.
2. Report status workflow: `OPEN`, `IN_REVIEW`, `RESOLVED`, `REJECTED`, `ESCALATED`.
3. Violation record creation from handled reports, with severity, penalty, credit delta, and optional deposit impact note.
4. User credit adjustment records and visible credit history.
5. User restriction state: warning, content freeze, posting freeze, service freeze, account disabled; keep actual auth hardening minimal for this phase.
6. Admin action audit log for report/violation/credit/restriction actions.
7. Station notifications for report decisions, violations, and restriction changes.
8. Admin governance workspace in `/admin/ops` or a dedicated `/admin/governance` page.
9. User-facing trust page showing credit score, violation history, and appeal/report status summary.
10. README and `CLAUDE.md` handoff update.

**Suggested files:**

- Create `docs/superpowers/specs/2026-05-22-campushub-phase5-governance-design.md`.
- Create `docs/superpowers/plans/2026-05-22-campushub-phase5-governance-upgrade.md`.
- Create `backend/src/main/resources/db/migration/V10__governance_credit_upgrade.sql`.
- Extend `backend/src/main/java/com/campushub/moderation/*` for report handling and violations.
- Add focused `backend/src/main/java/com/campushub/governance/*` only if moderation becomes too broad.
- Extend `backend/src/main/java/com/campushub/audit/*` for admin action audit if current safety logs are insufficient.
- Add/extend frontend views: `frontend/src/views/AdminGovernanceView.vue`, `frontend/src/views/CreditCenterView.vue`, router, layout navigation, and styles.

**Verification:**

- API smoke: report queue, report handle action, violation creation, user credit history, admin audit list.
- Browser: admin governance queue, violation/credit action, user credit center.
- Regression: existing `/goods`, `/tasks`, `/shops`, `/project-ads`, `/admin/ops` still render.

**Non-goals:**

- Full RBAC enforcement overhaul.
- Legal-grade appeal workflow.
- Automatic ML moderation.
- Real deposit deduction automation.
- Real-name identity verification beyond school email/contact.

## 3. Phase 6: Operations analytics and export

**Objective:** Turn accumulated platform activity into useful operational dashboards for beta decisions.

**Why after governance:** Governance creates trusted statuses and action records. Analytics should read stable operational data rather than invent parallel status logic.

**Scope:**

1. Cross-business dashboard metrics: active users, new users, task/goods/shop/project counts, pending reviews, open reports, completed flows.
2. Business-line tabs: runner task conversion, second-hand intent/contact counts, shop booking funnel, project-ad exposure and interaction.
3. Campus-zone analytics: task origin/destination, goods trade zones, shop service zones, project-ad campus zones.
4. Service-fee and role-deposit summary without changing payment boundary.
5. CSV export endpoints for admin-visible operational tables.
6. Date range filters and simple trend cards.
7. Frontend dashboard refinement in `/admin/ops`.
8. README/CLAUDE handoff update.

**Suggested files:**

- Spec: `docs/superpowers/specs/2026-05-22-campushub-phase6-ops-analytics-design.md`.
- Plan: `docs/superpowers/plans/2026-05-22-campushub-phase6-ops-analytics-upgrade.md`.
- Backend package: `backend/src/main/java/com/campushub/ops/*`.
- Optional migration V11 only if persistent analytics snapshots or export logs are needed.
- Frontend: `frontend/src/views/AdminOperationsView.vue`, optional chart/stat components under `frontend/src/components/ops/`.

**Verification:**

- API smoke for dashboard summary and export endpoints.
- Browser verification of date filters and all admin ops tabs.
- Confirm no secret or private payment data appears in exports.

**Non-goals:**

- Heavy BI system.
- Real-time streaming analytics.
- Complex chart libraries unless already justified.
- User behavior tracking beyond existing business events.

## 4. Phase 7: Responsive Web and user experience polish

**Objective:** Make the deployed web app credible for real mobile browser beta usage.

**Why here:** After the core business and governance flows exist, improving mobile completion rates and error clarity becomes more valuable than adding another domain feature.

**Scope:**

1. Mobile-first pass over `/auth`, `/tasks`, task workspace, `/goods`, goods detail/publish, `/shops`, shop detail/merchant, `/project-ads`, project detail/manage, `/wallet`, `/roles`, `/notifications`, and user credit center.
2. Consistent empty states, login prompts, validation messages, and loading states.
3. Form simplification for publish flows: task, goods, shop service item, project ad.
4. Shared responsive card/list styles to reduce one-off CSS.
5. Vite bundle review and Element Plus import strategy if chunk warnings become painful.
6. Accessibility basics: button labels, contrast checks, keyboard-friendly dialogs.
7. Browser verification matrix: desktop, tablet-ish, 390x844 mobile.

**Suggested files:**

- Spec: `docs/superpowers/specs/2026-05-22-campushub-phase7-responsive-ux-design.md`.
- Plan: `docs/superpowers/plans/2026-05-22-campushub-phase7-responsive-ux-upgrade.md`.
- Frontend: `frontend/src/styles.css`, existing view files, optional focused components under `frontend/src/components/`.
- Backend migration should usually not be needed.

**Verification:**

- `npm --prefix frontend run build` in server/Docker or approved environment.
- Playwriter desktop and mobile checks on the key routes.
- No obvious horizontal overflow on mobile key pages.

**Non-goals:**

- Native mobile app.
- WeChat mini-program.
- Full design-system rewrite.
- Replacing Element Plus.

## 5. Phase 8: Payment-center integration hardening and service-fee operations

**Objective:** Keep CampusHub payment boundaries safe while making service-fee/deposit operations more production-like.

**Why after UX:** Payment integration touches shared infrastructure and should happen once product flows and governance rules are stable.

**Scope:**

1. Define internal payment-center API contract with API-Transfer-Station for role deposits and service fees.
2. Replace mock-only operational flow with environment-driven provider selection while keeping mock mode for local/demo.
3. Payment order creation for role deposits and selected service-fee records.
4. Internal callback handling with idempotency, signature/token verification, and audit logs.
5. Admin payment status monitor: pending, paid, failed, expired.
6. Clear docs for secret names, callback URLs, and operational runbooks.

**Suggested files:**

- Spec: `docs/superpowers/specs/2026-05-22-campushub-phase8-payment-center-design.md`.
- Plan: `docs/superpowers/plans/2026-05-22-campushub-phase8-payment-center-upgrade.md`.
- Backend: `backend/src/main/java/com/campushub/payment/*`, `application-prod.yml`, `.env.prod.example`.
- Migration V11/V12 only if payment callback idempotency or provider order mapping requires new columns.

**Verification:**

- Mock provider regression stays working.
- Server-side API smoke in mock or staging token mode.
- Never print/read Alipay key contents from CampusHub or API-Transfer-Station.

**Non-goals:**

- CampusHub directly reading Alipay private/public key bodies.
- Transaction principal escrow.
- Per-order deposit freeze.
- Replacing API-Transfer-Station payment center.

## 6. Phase 9: Authentication, authorization, and production security hardening

**Objective:** Move from course-prototype open APIs toward safer beta production controls.

**Why after payment planning:** Auth hardening can break demos and operations if done too early. By Phase 9, business flows and admin needs are clearer.

**Scope:**

1. Enforce JWT authentication on write operations while preserving public read endpoints.
2. Replace query-param user IDs with authenticated user context where practical.
3. Admin endpoint role checks using `ROLE_ADMIN`.
4. Rate limiting or resend protections for sensitive endpoints already not covered.
5. Upload validation tightening: content type, size, path safety, target ownership.
6. Security audit log expansion for admin and sensitive user actions.
7. Regression pass for all frontend flows after auth changes.

**Suggested files:**

- Spec: `docs/superpowers/specs/2026-05-22-campushub-phase9-auth-security-design.md`.
- Plan: `docs/superpowers/plans/2026-05-22-campushub-phase9-auth-security-upgrade.md`.
- Backend: `backend/src/main/java/com/campushub/config/SecurityConfig.java`, `auth`, controllers using user IDs, `audit`.
- Frontend: `src/api/client.ts`, auth store, route guards, login prompts.

**Verification:**

- Authenticated happy paths for all write operations.
- Anonymous public reads still work.
- Admin operations blocked for non-admin users.
- Playwriter login and key write-flow checks.

**Non-goals:**

- OAuth/SAML.
- Multi-school enterprise SSO.
- Full penetration test remediation beyond this app's current scope.

## 7. Phase 10: Beta launch readiness and operational runbook

**Objective:** Prepare CampusHub for controlled Alpha/Beta usage with clear deployment, rollback, moderation, and support processes.

**Scope:**

1. Production runbook: deploy, rollback, DB migration caution, container health, logs, API smoke, browser smoke.
2. Admin playbooks: report handling, violation severity, credit adjustment, content takedown, user restriction.
3. Beta seed data cleanup and demo data separation.
4. User-facing policy pages: service agreement, privacy/risk notice, trading safety tips.
5. Monitoring checklist: uptime, container health, disk usage, MySQL backup, API errors.
6. Backup/restore dry-run documentation without exposing secrets.
7. Final end-to-end verification matrix across all major flows.

**Suggested files:**

- Spec: `docs/superpowers/specs/2026-05-22-campushub-phase10-beta-readiness-design.md`.
- Plan: `docs/superpowers/plans/2026-05-22-campushub-phase10-beta-readiness-upgrade.md`.
- Docs: `docs/operations/` if explicit docs are desired.
- Frontend: policy/risk pages if implemented in-app.
- Backend migration only if policy acceptance records are required.

**Verification:**

- Full production smoke checklist.
- Documented rollback command validated in a non-destructive way.
- Playwriter checks for user-facing policy links and major routes.

**Non-goals:**

- Multi-region deployment.
- Kubernetes migration.
- Enterprise observability stack.

## 8. Recommended immediate next step

Start Phase 5 with governance and credit. It is the highest leverage next phase because it strengthens every existing business line without changing the payment boundary or adding a fifth content marketplace.

### Phase 5 first task outline

1. Write `docs/superpowers/specs/2026-05-22-campushub-phase5-governance-design.md`.
2. Write `docs/superpowers/plans/2026-05-22-campushub-phase5-governance-upgrade.md`.
3. Implement V10 schema for report workflow, violation/credit history, restriction records, and admin audit if needed.
4. Implement backend services/controllers.
5. Implement admin governance UI and user credit center.
6. Verify via server Docker build, API smoke, and Playwriter.
7. Update README and `CLAUDE.md` handoff.

## 9. Self-review

- Coverage: The plan incorporates the original roadmap's business order and adds balanced future phases for governance, analytics, UX, payment integration, security, and beta readiness.
- Scope: Each future phase is sized to be similar to Phase 4 and independently shippable.
- Boundaries: The plan preserves no principal escrow, no per-order deposit freezes, no CampusHub Alipay key handling, no edits to applied migrations, and low-impact production verification.
- Ambiguity: Phase 5 is explicitly recommended as governance/credit; alternative directions are documented but not mixed into the same implementation phase.
