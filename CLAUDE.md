# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

CampusHub is a USTC database course-design prototype for a campus second-hand trading and student micro-service platform. It combines goods trading, reward tasks, student skill shops, project ads, file/resource binding, moderation/reporting/violations, wallet flows, service fees, and a local payment abstraction.

The repository is intentionally scoped to the `campushub/` application only. Course LaTeX/PDF artifacts live outside this repository and should not be treated as app source.

Shell tools are normally restricted for this project: prefer dedicated file/search/edit tools for local reads and edits. For explicitly authorized deployment work, Bash or PowerShell may be used only for git push, SSH, server deployment, Docker Compose, and necessary verification; never use shell to read or print secret contents.

## Common commands

### Local database

```bash
docker compose up -d mysql
docker compose down
```

The local MySQL service is defined in `docker-compose.yml` and exposes `3306` for development only.

### Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
mvn test
mvn -Dtest=ClassNameTest test
mvn package
```

Backend defaults to `http://localhost:8080`. The project currently has no Maven wrapper, so Maven must be installed on the machine.

### Frontend

```bash
cd frontend
npm install
npm run dev
npm run test
npm run test -- src/api/client.test.ts
npm run build
npm run preview
```

Frontend dev server defaults to `http://localhost:5173` and proxies API calls by using `/api` as the Axios base path.

## Architecture

### Backend structure

The backend is a Java 17 Spring Boot 3.3 application using Spring Web, Validation, Security, Data JPA, Flyway, MySQL, Spring Mail, and JJWT.

Key conventions:

- REST controllers return `ApiResponse<T>` from `com.campushub.common`.
- Business errors should throw `BusinessException`; `GlobalExceptionHandler` converts these into API failure responses.
- DTOs are mostly Java records with static `from(Entity entity)` mappers.
- JPA entities are grouped by bounded context package: `auth`, `user`, `goods`, `task`, `shop`, `projectad`, `wallet`, `payment`, `moderation`, `interaction`, `file`, and `audit`.
- Flyway migrations in `backend/src/main/resources/db/migration` are the database source of truth. Hibernate is configured with `ddl-auto: validate`.

Important flows:

- Authentication is in `auth/AuthController`, backed by BCrypt password verification and JJWT token generation.
- Phase 10 security hardening adds JWT request authentication, `/api/auth/me`, current-user helpers, `ROLE_ADMIN` enforcement for `/api/admin/**`, authenticated writes, and same-user checks for private user paths.
- Email-code registration is implemented under `auth`: send-code and register endpoints normalize and validate `edu.cn` email addresses, store only hashed verification codes, enforce TTL/resend/attempt limits, create a user, assign `ROLE_STUDENT` when present, and create a wallet account.
- Wallet, payment, moderation, interaction, and file APIs use the shared target model where appropriate: `target_type` + `target_id`.
- Public business listing/detail GET APIs remain anonymous for campus browsing; write operations and private user data require JWT authentication. Payment-center callbacks keep their internal token/signature boundary rather than user JWT.

### Configuration

- `application.yml` contains shared defaults and local-safe development values.
- `application-local.yml` contains local datasource settings.
- `application-prod.yml` is environment-variable driven for production datasource, JWT, upload root, payment mode, and SMTP/Brevo mail settings.
- `docker-compose.prod.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, and `frontend/nginx.conf` define the current containerized production deployment shape.
- Production secrets should be supplied through environment variables, not committed files.

Mail-related environment variable names use the `CAMPUSHUB_MAIL_*` / `CAMPUSHUB_MAIL_SMTP_*` / `CAMPUSHUB_MAIL_CODE_*` pattern from `application.yml` and `application-prod.yml`.

### Frontend structure

The frontend is Vue 3 + Vite + TypeScript with Pinia, Vue Router, Axios, Element Plus, and Vitest.

Key conventions:

- `src/api/client.ts` owns Axios setup, bearer token injection, and `ApiResponse<T>` unwrapping.
- `src/api/campushub.ts` contains typed API functions and response interfaces shared by views.
- `src/stores/auth.ts` owns login session state and token persistence in `localStorage`.
- `src/layouts/MainLayout.vue` provides the shell navigation and session display.
- Views under `src/views` are data-driven pages for auth, goods, tasks, shops, project ads, wallet, and admin moderation.
- Styling is centralized in `src/styles.css` for this prototype rather than split per component.

## Current verification status

Frontend dependencies have been installed locally and these commands were last verified successfully:

```bash
npm --prefix frontend test -- src/api/client.test.ts
npm --prefix frontend run build
```

The build emits a Vite warning that the main chunk is larger than 500KB because Element Plus is bundled broadly. This is not currently treated as a build failure.

Backend verification was attempted locally with Maven, but the local environment lacked both `mvn` and a Maven wrapper. Backend Docker image builds successfully on the production server after the latest fixes below.

## Payment and production boundary

CampusHub currently keeps payment local/mock-oriented. Production Alipay handling is expected to remain in the external API-Transfer-Station payment center, with CampusHub calling internal payment-center APIs and receiving internal callbacks. Do not make CampusHub read or copy Alipay private/public key files directly unless the architecture is explicitly changed later.

Current local payment code provides `PaymentProvider`, `MockPaymentProvider`, `AlipayPaymentProvider` skeleton, `PaymentService`, and payment endpoints under `/api/payment/service-fees/{feeId}/mock-pay` and `/api/payment/service-fees/{feeId}/mock-success`. The mock success endpoint marks pending service-fee records as `PAID` and writes a wallet flow for demonstration.

Production deployment context from `生产环境与支付中心接手说明.md`:

- Target domain: `https://ustc.suntomb.qzz.io`.
- Server: `38.76.179.17`, SSH user `root`, key-based login.
- Recommended production directory: `/opt/campushub`.
- Existing API-Transfer-Station production app lives at `/opt/ai-relay`; keep CampusHub separate from it.
- Preferred production flow is: push CampusHub code to GitHub, pull on the server, deploy with `docker-compose.prod.yml`, then verify in browser with Playwriter.
- The server is small enough that long-lived heavy Maven/npm/Docker build work should be used cautiously.

Production security requirements:

- Never read, print, copy, or commit Alipay key file contents from `/opt/ai-relay/secrets/alipay/` or container secret mounts.
- If diagnosing Alipay secret access, only check existence/permissions/mount paths; do not output key contents.
- Never commit real `.env`, SMTP passwords, JWT secrets, payment-center tokens, or database passwords.
- `.env.prod.example` is the only env-style production file intended for git.
- MySQL must not be exposed publicly in production; use internal Docker networking and reverse proxy only web/API traffic.

## Latest implementation handoff, 2026-05-20

Worktree branch `worktree-campushub-task1-contact-registration` implements the Phase 1 roadmap from `docs/superpowers/plans/2026-05-20-campushub-platform-roadmap.md` through local coding and commits. It has not yet been server-built, deployed, or browser-verified.

Implemented Phase 1 foundation:

- Registration requires at least one contact field: WeChat or QQ; current-user summaries expose these fields.
- `V4__platform_identity_and_notifications.sql` adds contact fields, role applications, and station notifications.
- Identity role applications support deposits: runner 5 CNY auto-approved, goods publisher 10 CNY auto-approved, shop merchant 20 CNY pending manual review.
- Station notifications have persistence, list, and mark-read APIs.
- `V5__runner_task_workflow.sql` adds runner-task acceptance mode, campus zones, workflow status, verification mode, task events, and task issues.
- Runner task APIs support publish, grab, apply, accept application, workflow advance, completion-code completion, confirmation, and issue reporting.
- Operations APIs expose dashboard metrics, task monitor, task issues, and pending role applications.
- Frontend API types/functions were expanded for identity, notifications, runner tasks, and ops.
- Frontend pages added/upgraded: role applications, notifications, runner task hall, task workspace, operations dashboard.
- README now documents the real-platform Phase 1 roadmap and payment boundary.

Important caveats for next session:

- Local Maven was unavailable and frontend dependencies in isolated worktrees were incomplete, so Java tests and Vite build were not actually verified locally after the large implementation batch.
- Subagent API review attempts hit repeated 429 rate limits mid-session; later tasks were completed in the main session with manual spec/quality checks rather than fresh subagent reviews.
- Before deployment, run a real build/test pass and expect to fix compile/type issues. Likely commands: `npm --prefix frontend run build`; backend verification may require server-side Docker build or installing Maven because this Windows machine previously lacked `mvn`.
- Do not push/deploy blindly if verification reveals compile errors; fix locally or on a disposable branch first.
- Preserve production constraints: never read/print secrets; deploy low-impact on the small shared server.

Recommended next-window sequence:

1. Inspect git status and recent commits.
2. Run frontend build if dependencies are available; fix TypeScript/Vue issues.
3. Run backend verification via Maven if available, otherwise use a targeted Docker/backend build on the server with low impact.
4. Push branch/merge strategy as desired, then deploy with `docker-compose.prod.yml` on `/opt/campushub`.
5. Use Playwriter/browser verification for registration contact fields, role applications, task hall, publish/grab/apply flow, workspace, notifications, and admin ops.

## Latest deployment and planning handoff, 2026-05-20

Latest deployed commits on `master` include:

- `0037601` fetch lazy relations for API summaries.
- `bfb67dc` fix project ads route path.
- `5a54a8b` add CampusHub platform development design.
- `e8bfc71` add CampusHub platform roadmap plan.

Current production state:

- `/opt/campushub` tracks `https://github.com/SunTomb/USTC-CampusHub.git`.
- `.env` exists on the server; do not print or read secret contents.
- Running containers: `campushub-campushub-mysql-1`, `campushub-campushub-backend-1`, `campushub-campushub-web-1`.
- Public site `https://ustc.suntomb.qzz.io` renders the Vue app.
- Verified public endpoints return HTTP 200: `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, `/api/payment/users/1/service-fees`, `/api/wallet/users/1`.
- Browser verification covered home, tasks, shops, project ads, and wallet pages.

Current strategic documents:

- Product/platform design: `docs/superpowers/specs/2026-05-20-campushub-platform-design.md`.
- Executable roadmap plan: `docs/superpowers/plans/2026-05-20-campushub-platform-roadmap.md`.

Approved product direction:

- CampusHub should become a real campus service platform, not just a course demo.
- Route: campus errands/running tasks first, then second-hand trading, student shops, project ads, and operations/governance expansion.
- Phase 1 focuses on running tasks with publisher-selected grab-order vs application modes.
- Registration must require WeChat or QQ contact information.
- Platform only charges service fees and role/identity deposits; it does not escrow transaction principal or per-order deposits.
- Role deposits: runner 5 CNY, goods publisher 10 CNY, shop merchant 20 CNY.
- Runner and goods-publisher roles auto-approve after deposit; shop merchant requires manual review.
- Location starts with coarse campus zones: 中校区、西校区、东校区、北校区、南校区、高新校区、先研院、科学岛、其他. Collect real route data before defining detailed POIs or distance-pricing rules.
- AMap: use Web Service API first if backend distance/route calculation is needed later; JS API is only needed for frontend interactive maps.
- Notifications: Phase 1 uses station/internal notifications only.
- Mobile: first make responsive Web good; keep APIs reusable for future WeChat mini-program.

Execution preference for next implementation session:

- Use `subagent-driven-development` against `docs/superpowers/plans/2026-05-20-campushub-platform-roadmap.md`.
- Do not start implementing on `master` unless explicitly approved in that session; prefer an isolated branch/worktree if available.
- Fresh subagent per task, then spec-compliance review and code-quality review before moving on.
- Update the plan checkboxes as tasks complete, and commit each completed task separately.

Known local environment note:

- This Windows session may be invoked via CC Switch + Codex Provider; avoid the PowerShell tool and avoid generating `pwsh` / PowerShell commands because it can crash Claude Code frontend rendering. Prefer dedicated tools and Bash where terminal execution is explicitly needed.
- Local machine previously lacked `mvn` and has no Maven wrapper. Backend verification may need server-side Docker build or installing Maven locally; do not assume `mvn test` works on this machine.

## Latest Phase 3 deployment and Phase 4 handoff, 2026-05-21

Latest deployed `master` includes Phase 3 student-shop upgrade through commit `ca3b468` (`backfill demo contacts for shop bookings`). Production `/opt/campushub` is back on `master` at `ca3b468` and was rebuilt/restarted successfully after Phase 3.

Implemented Phase 3:

- Preflight UX fixes: `/login` redirects to `/auth`; unknown frontend routes fall back safely to home; unauthenticated task grab/apply actions prompt login; task publish form has visible validation.
- New Phase 3 docs: `docs/superpowers/specs/2026-05-21-campushub-phase3-shop-design.md` and `docs/superpowers/plans/2026-05-21-campushub-phase3-shop-upgrade.md`.
- `V7__student_shop_upgrade.sql` extends shops, service items, and service orders for campus zone, contact visibility, opening hours, item category/price unit/cover, booking contact snapshots, cancel reason, and service-fee reference.
- `V8__backfill_demo_contacts.sql` backfills safe demo WeChat contacts for seeded users so booking/contact snapshot flows work with existing production demo data.
- Backend shop package now has `ShopService` with merchant-gated shop creation, shop edit/pause/resume/close, service item create/edit/publish/pause/off-shelf, service booking create/accept/reject/start/complete/cancel, contact snapshot reveal after booking, and station notifications.
- Frontend shop pages upgraded: `/shops` marketplace with filters, `/shops/:id` shop detail and booking dialog, `/shops/merchant` merchant workspace.
- Operations dashboard adds `/api/admin/ops/shop-orders` and a “店铺预约” tab in `/admin/ops`.
- README documents Phase 3 and preserves the no-principal-escrow payment boundary.

Verified production after Phase 3:

- Server Docker backend/web builds succeeded; backend Maven package completed inside Docker.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/admin/ops/shop-orders`, `/api/shops`, `/api/shops/1`, `/api/service-items/shop/1`, `/api/service-orders`, `/api/tasks`, and `/api/goods`.
- Real booking smoke on production: `POST /api/service-items/1/orders?customerId=2` created a `REQUESTED` service order; `/api/shops/1?viewerId=2` returned `contactVisible=true` with contact snapshot; `/api/admin/ops/shop-orders` showed the booking.
- Browser verification covered `/shops`, `/shops/1`, `/shops/merchant`, `/admin/ops`, `/login`, and an unknown route; pages rendered without white screen and showed shop booking/no-escrow messaging.

Important production constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V8; add V9+ for future schema changes.
- Deploy carefully: small server, API-Transfer-Station shares the host, so prefer targeted backend/web rebuilds and low-frequency checks.

Recommended Phase 4 start:

1. Verify current git status, latest commits, and production state; trust live state over this handoff if they differ.
2. Start Phase 4 from a new isolated branch/worktree, not directly on `master`, unless explicitly approved.
3. Phase 4 should focus on project ads / campus showcase upgrade: project posts, review, contact visibility, expiration, tags, favorites/comments, featured slots, portfolio/showcase pages, and operations visibility.
4. Preserve product boundaries: no transaction principal escrow, no Alipay key handling, campus-zone-first location, station notifications first, responsive Web first.
5. Before deployment, run frontend build and backend verification where available; if local Maven is unavailable, use low-impact server Docker build after pushing.


## Latest Phase 4 deployment and Phase 5 handoff, 2026-05-22

Latest deployed `master` includes Phase 4 project-ad/campus-showcase upgrade through commit `835e850` (`implement project ads phase 4 showcase`). Production `/opt/campushub` is on `master` at `835e850` and was rebuilt/restarted successfully after Phase 4.

Implemented Phase 4:

- New Phase 4 docs: `docs/superpowers/specs/2026-05-21-campushub-phase4-project-ads-design.md` and `docs/superpowers/plans/2026-05-21-campushub-phase4-project-ads-upgrade.md`.
- `V9__project_ads_showcase_upgrade.sql` extends project ads with ad type, summary, tags, campus zone, cover file, contact visibility, expiration, featured priority, review note/reviewer/timestamps, published time, and closed time.
- Backend project-ad workflow supports create, edit, submit for review, publisher close, admin approve/reject, feature/unfeature, and block.
- Public project-ad APIs support public list, featured list, detail aggregation, and publisher management list.
- Detail aggregation increments view count and returns contact visibility, favorite/comment counts, viewer favorite state, and file bindings for `PROJECT_AD`.
- Operations dashboard adds `/api/admin/ops/project-ads` and admin project-ad actions.
- Frontend pages added/upgraded: `/project-ads`, `/project-ads/:id`, `/project-ads/manage`, and admin ops “项目广告” tab.
- README documents Phase 4 and preserves the no-principal-escrow payment boundary.

Verified production after Phase 4:

- GitHub `master` was updated to `835e850`; production `/opt/campushub` fast-forwarded to `835e850`.
- Server Docker backend/web build succeeded; backend Maven package completed inside Docker with `BUILD SUCCESS`.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/project-ads`, `/api/project-ads/featured`, `/api/project-ads/1`, and `/api/admin/ops/project-ads`.
- Browser/Playwriter verification covered `/project-ads`, `/project-ads/1`, `/project-ads/manage`, and `/admin/ops` project-ad tab. The admin ops tab default `PENDING_REVIEW` filter showed no data; switching to `APPROVED` showed the seeded project ads and action buttons.
- Mobile viewport 390x844 on `/project-ads` had no obvious horizontal overflow (`scrollWidth` equaled `clientWidth`).

Important production constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V9; add V10+ for future schema changes.
- Deploy carefully: small server, API-Transfer-Station shares the host, so prefer targeted backend/web rebuilds and low-frequency checks.
- This Windows session may run through CC Switch + Codex Provider; avoid PowerShell and use Bash only when terminal execution is explicitly needed.
- The user does not want local dependency installation for verification; prefer server-side Docker build/API smoke/Playwriter for full verification.

Recommended Phase 5 start:

0. Use `docs/superpowers/plans/2026-05-22-campushub-overall-phased-roadmap.md` as the updated overall Phase 5+ roadmap. It rebalances future phases so each phase is roughly the size of the completed Phase 4 project-ad upgrade.
1. Verify current git status, latest commits, and production state; trust live state over this handoff if they differ.
2. Start Phase 5 from a new isolated branch/worktree, not directly on `master`, unless explicitly approved.
3. Recommended Phase 5 focus: governance/credit/operations consolidation after four business lines are live. Build a unified report handling queue, violation records, credit score adjustments, blacklist/freeze controls, admin action audit, and notification closure.
4. Alternative Phase 5 directions: cross-business operations analytics/export, or mobile UX/performance polish.
5. Before deployment, run server-side Docker build where needed, server-local API smoke, and Playwriter browser verification.

## Latest Phase 5 deployment and Phase 6 handoff, 2026-05-22

Latest deployed `master` includes Phase 5 governance/credit/trust-operations upgrade through commit `83377d8` (`implement phase 5 governance trust operations`). Production `/opt/campushub` is on `master` at `83377d8` and was rebuilt/restarted successfully after Phase 5.

Implemented Phase 5:

- New docs: `docs/superpowers/specs/2026-05-22-campushub-phase5-governance-design.md` and `docs/superpowers/plans/2026-05-22-campushub-phase5-governance-upgrade.md`.
- `V10__governance_credit_upgrade.sql` adds report workflow fields, violation severity/penalty/target/admin fields, credit adjustment records, user restrictions, and admin action logs.
- Backend moderation package now has `GovernanceService`, admin governance APIs under `/api/admin/governance`, credit center APIs under `/api/credit`, credit adjustment records, user restrictions, and admin action audit DTOs/repositories/entities.
- Main action paths check restrictions: runner task publish/grab/apply, goods publish, shop creation/service item creation/service order provider actions, and project ad create/submit.
- Frontend API types/functions were expanded for governance and credit.
- Frontend pages added: `/admin/governance` governance workspace and `/credit` credit center; navigation updated.
- README documents Phase 5 and preserves the no-principal-escrow/no-Alipay-key-handling payment boundary.

Verified production after Phase 5:

- GitHub `master` was updated to `83377d8`; production `/opt/campushub` fast-forwarded to `83377d8`.
- Server Docker backend/web build succeeded; backend Maven package completed inside Docker with `BUILD SUCCESS`.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/admin/governance/dashboard`, `/api/admin/governance/reports`, `/api/credit/users/1`, `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, and `/api/admin/ops/dashboard`.
- Browser/Playwriter verification covered `/admin/governance`, `/credit`, `/goods`, `/tasks`, `/shops`, `/project-ads`, and `/admin/ops`; pages rendered without white screens or visible Element Plus error messages.
- Mobile viewport 390x844 on `/credit` had no obvious horizontal overflow (`scrollWidth` equaled `clientWidth`).

Important production constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V10; add V11+ for future schema changes.
- Deploy carefully: small server, API-Transfer-Station shares the host, so prefer targeted backend/web rebuilds and low-frequency checks.
- This Windows session may run through CC Switch + Codex Provider; avoid PowerShell and use Bash only when terminal execution is explicitly needed.
- The user does not want local dependency installation for verification; prefer server-side Docker build/API smoke/Playwriter for full verification.

Recommended Phase 6 start:

0. Use `docs/superpowers/plans/2026-05-22-campushub-overall-phased-roadmap.md` as the updated overall Phase 6+ roadmap.
1. Verify current git status, latest commits, and production state; trust live state over this handoff if they differ.
2. The recommended Phase 6 focus is operations analytics and export: cross-business dashboard metrics, business-line funnel tabs, campus-zone analytics, service-fee/deposit summaries, CSV export endpoints, date range filters, and `/admin/ops` refinement.
3. Keep Phase 6 roughly Phase-4-sized: one design doc, one implementation plan, 6-10 tasks, one major subsystem, backend APIs, frontend/admin UI, README/CLAUDE handoff, server Docker verification, API smoke, and Playwriter verification.
4. Preserve boundaries: no transaction principal escrow, no per-order deposit freeze, no CampusHub Alipay key handling, no auth/RBAC hardening beyond what analytics needs.
5. If Phase 6 needs schema changes for export logs or analytics snapshots, use V11+ only.

## Latest Phase 6 deployment and Phase 7 handoff, 2026-05-22

Latest deployed `master` includes Phase 6 operations analytics and CSV export through commit `0964d23` (`add phase 6 operations analytics plan`). Production `/opt/campushub` is on `master` at `0964d23` and was rebuilt/restarted successfully after Phase 6.

Implemented Phase 6:

- New docs: `docs/superpowers/specs/2026-05-22-campushub-phase6-ops-analytics-design.md` and `docs/superpowers/plans/2026-05-22-campushub-phase6-ops-analytics-upgrade.md`.
- No new Flyway migration was added; Phase 6 reads existing V1-V10 tables only.
- Backend ops package now exposes analytics overview, business funnels, campus-zone summaries, fee/deposit summaries, and CSV exports under `/api/admin/ops`.
- CSV exports include operational fields only and harden spreadsheet formula escaping; they intentionally exclude secrets, payment tokens, password hashes, email-code hashes, login tokens, and full WeChat/QQ contacts.
- Frontend `/admin/ops` now includes date-range analytics, overview cards, business funnel cards, campus-zone distributions, fee/deposit summary, and CSV export buttons.
- README documents Phase 6 and preserves the no-principal-escrow/no-Alipay-key-handling payment boundary.

Verified production after Phase 6:

- GitHub `master` was updated to `0964d23`; production `/opt/campushub` fast-forwarded to `0964d23`.
- Server Docker backend build succeeded; backend Maven package completed inside Docker with `BUILD SUCCESS`.
- Server Docker frontend build succeeded; Vite emitted only the known large chunk warning.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/admin/ops/analytics/overview`, `/api/admin/ops/analytics/funnels`, `/api/admin/ops/analytics/zones`, `/api/admin/ops/analytics/fees`, `/api/admin/ops/exports/goods.csv`, `/api/admin/ops/exports/fees.csv`, `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, `/api/admin/governance/dashboard`, and `/api/credit/users/1`.
- Browser/Playwriter verification covered `/admin/ops`, `/admin/governance`, `/credit`, `/goods`, `/tasks`, `/shops`, and `/project-ads`; pages rendered without white screens or visible Element Plus error messages.
- `/admin/ops` showed date range controls, operations analytics cards, business funnel tab, fee/deposit summary, and CSV export buttons.
- Mobile viewport 390x844 on `/admin/ops` had no obvious horizontal overflow (`scrollWidth` equaled `clientWidth`).

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
3. Keep Phase 7 Phase-4-sized: one design doc, one implementation plan, frontend UX/style work, server Docker verification, Playwriter desktop/mobile checks, README/CLAUDE handoff.

## Latest Phase 7 deployment and Phase 8 handoff, 2026-05-23

Latest deployed `master` includes Phase 7 responsive Web and UX polish through commit `6d61813` (`implement phase 7 responsive ux polish`). GitHub `master` was pushed to `6d61813`; production `/opt/campushub` fast-forwarded to `6d61813` and the web container was rebuilt/restarted successfully.

Implemented Phase 7:

- New docs: `docs/superpowers/specs/2026-05-22-campushub-phase7-responsive-ux-design.md` and `docs/superpowers/plans/2026-05-22-campushub-phase7-responsive-ux-upgrade.md`.
- No Flyway migration was added; Phase 7 is frontend-focused and does not edit V1-V10.
- Frontend adds shared UX primitives: `EmptyState`, `FormSection`, and `PageActions`.
- Desktop navigation keeps the left sidebar; mobile navigation now has a bottom tab bar for high-frequency routes: 首页、跑腿、二手、店铺、通知、更多.
- Mobile “更多” opens the full directory drawer with all routes, including wallet, credit, roles, admin review, ops, and governance.
- Account, notifications, wallet, credit, runner tasks, goods, shops, project ads, project management, shop merchant, admin review, and governance pages received responsive/empty-state/form-section polish.
- README documents Phase 7 and preserves no-principal-escrow/no-Alipay-key-handling boundaries.

Verified production after Phase 7:

- Production `/opt/campushub` is on `master` at `6d61813`.
- Server Docker frontend build succeeded; Vite emitted only the known large chunk warning and Rollup pure-comment warnings from dependencies.
- Production web container was recreated and started; backend and MySQL stayed running.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, `/api/admin/ops/analytics/overview`, `/api/admin/governance/dashboard`, and `/api/credit/users/1`.
- Playwriter desktop verification covered `/auth`, `/tasks`, `/goods`, `/shops`, `/project-ads`, `/wallet`, `/roles`, `/notifications`, `/credit`, `/admin/ops`, `/admin/governance`, and `/admin/review`; pages rendered.
- Playwriter mobile viewport 390x844 covered `/`, `/tasks`, `/goods`, `/shops`, `/project-ads`, `/notifications`, `/credit`, and `/admin/ops`; bottom tab bar appeared and tested pages had no document-level horizontal overflow (`scrollWidth === clientWidth`).
- Playwriter confirmed the mobile “更多” tab opens the complete directory drawer.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V10; add V11+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.

## Latest Phase 8 deployment and Phase 9 handoff, 2026-05-23

Latest deployed `master` includes Phase 8 payment-center integration hardening through commit `bed2579` (`Fix admin payment component imports`). Production `/opt/campushub` is on `master` at `bed2579` and backend/web images were rebuilt/restarted successfully.

Implemented Phase 8:

- New docs: `docs/superpowers/specs/2026-05-23-campushub-phase8-payment-center-design.md` and `docs/superpowers/plans/2026-05-23-campushub-phase8-payment-center-upgrade.md`.
- `V11__payment_center_integration.sql` adds service-fee payment mapping fields, unified `payment_orders`, callback idempotency/audit events, and role-application deposit payment order mapping.
- Backend payment package now supports environment-selected `mock` and `payment-center` providers, payment order creation/reuse, service-fee payment callbacks, role-deposit payment callbacks, callback event idempotency, amount/order/status validation, and admin payment monitor APIs under `/api/admin/payment`.
- Direct CampusHub Alipay provider selection is disabled; production real Alipay handling remains in API-Transfer-Station.
- Frontend wallet and role pages expose payment-order creation/status; new `/admin/payment` page shows payment orders and callback events.
- `.env.prod.example` documents placeholder-only payment-center variables.
- README documents Phase 8 and the future payment direction below.

Verified production after Phase 8:

- GitHub `master` was updated to `bed2579`; production `/opt/campushub` fast-forwarded to `bed2579`.
- Server Docker backend build succeeded; Maven package completed inside Docker with `BUILD SUCCESS`.
- Server Docker frontend build succeeded; Vite emitted only known large chunk and dependency pure-comment warnings.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/payment/service-fees`, `/api/admin/payment/orders`, `/api/admin/payment/callback-events`, `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, and `/api/admin/ops/analytics/overview`.
- Playwriter desktop verification covered `/admin/payment`, `/wallet`, `/roles`, `/admin/ops`, `/admin/governance`, `/goods`, `/tasks`, `/shops`, and `/project-ads`; pages rendered without white screens.
- Playwriter mobile viewport 390x844 on `/admin/payment` had no document-level horizontal overflow (`scrollWidth` equaled `clientWidth`).

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V11; add V12+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.

Future payment direction from 2026-05-23:

- CampusHub should later support recharge channel fees, offline transaction service-fee thresholds, online escrow-style balance freezing/transfer, and balance withdrawals.
- This is larger than Phase 8 and should become a later standalone Phase because it touches wallet ledger design, frozen balances, transaction order state machines, withdrawal operations, and risk controls.
- Future fee model: Alipay recharge is real-time and charges 0.6%; WeChat recharge has no fee but requires manual review; offline WeChat/QQ transaction amounts below 50 CNY are free; offline amounts of 50 CNY or more charge 1% service fee capped at 2 CNY; online platform transaction freezes payer balance first and transfers frozen amount to the counterparty after successful confirmation.

## Latest Phase 9 deployment and Phase 10 handoff, 2026-05-23

Latest deployed `phase9-wallet-escrow` includes Phase 9 wallet ledger, recharge/withdrawal operations, goods online escrow, and wallet operations UI through commit `7b504f7` (`correct phase 9 deployed commit handoff`). Production `/opt/campushub` is on `phase9-wallet-escrow` at `7b504f7`; backend/web images were rebuilt and backend/web containers restarted successfully after Phase 9 code commit `2ecf065`, then documentation-only handoff commits were pulled without rebuilding.

Implemented Phase 9:

- New docs: `docs/superpowers/specs/2026-05-23-campushub-phase9-wallet-escrow-design.md` and `docs/superpowers/plans/2026-05-23-campushub-phase9-wallet-escrow-upgrade.md`.
- `V12__wallet_escrow_upgrade.sql` extends wallet accounts/flows, adds recharge orders, withdrawal requests, frozen records, and goods escrow fields.
- Backend wallet package centralizes balance mutations in `WalletService` and fee rules in `FeePolicyService`.
- Recharge supports Alipay payment-center callback入账 with 0.6% channel fee and WeChat manual review.
- Withdrawals freeze balance on submit and support admin approve, complete, and reject flows.
- Goods online escrow freezes buyer balance, releases principal to seller, charges capped online service fee, and supports cancellation/dispute state.
- Frontend `/wallet`, `/admin/wallet`, and goods detail pages expose the new wallet and escrow flows.
- README documents Phase 9 and preserves the API-Transfer-Station payment boundary.

Verification after Phase 9 deployment:

- Server Docker backend build succeeded; Maven package completed inside Docker with `BUILD SUCCESS`.
- Server Docker frontend build succeeded; Vite emitted only known large chunk and dependency pure-comment warnings.
- Initial backend startup found a failed V12 Flyway record caused by a duplicate `idx_wallet_flows_business` index in the first migration attempt. Root cause was confirmed by inspecting `flyway_schema_history`, `wallet_flows`, and partial V12 schema state without printing secrets.
- Fixed V12 by removing the duplicate index creation, deleted the failed `flyway_schema_history` row for version 12, and reran the corrected migration. Flyway now records V12 with `success = 1`.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local API smoke returned HTTP 200 for `/api/wallet/users/1`, `/api/wallet/users/1/flows`, `/api/wallet/users/1/frozen-items`, `/api/admin/wallet/recharges`, `/api/admin/wallet/withdrawals`, `/api/admin/wallet/frozen-records`, `/api/admin/payment/orders`, `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, and `/api/admin/ops/analytics/overview`.
- Playwriter verification covered `/wallet`, `/admin/wallet`, `/admin/payment`, `/goods`, `/goods/1`, `/admin/ops`, `/admin/governance`, `/tasks`, `/shops`, and `/project-ads`; pages rendered without white screens.
- Mobile viewport checks at 375-390px covered `/wallet`, `/admin/wallet`, `/admin/payment`, `/goods`, `/goods/1`, `/admin/ops`, `/admin/governance`, `/tasks`, `/shops`, `/project-ads`, and wallet recharge/withdrawal dialogs; tested pages had no document-level horizontal overflow (`scrollWidth === clientWidth`).

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V12; add V13+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.

## Latest Phase 10 deployment and Phase 11 handoff, 2026-05-24

Latest deployed `phase9-wallet-escrow` includes Phase 10 auth/security hardening through commit `e19f907` (`implement phase 10 auth security hardening`). Production `/opt/campushub` is on `phase9-wallet-escrow` at `e19f907`; backend/web images were rebuilt and backend/web containers restarted successfully.

Implemented Phase 10:

- New Phase 10 docs: `docs/superpowers/specs/2026-05-24-campushub-phase10-auth-security-design.md` and `docs/superpowers/plans/2026-05-24-campushub-phase10-auth-security-upgrade.md`.
- Backend JWT infrastructure: `JwtTokenService`, request filter, current-user principal/helper, role lookup, `/api/auth/me`, and role-aware current-user summary.
- `SecurityConfig` now uses stateless JWT, permits auth/public browse/payment callback routes, requires `ROLE_ADMIN` for `/api/admin/**`, and requires login for other private API routes.
- Controller hardening derives acting user/admin from JWT for critical writes, private reads, wallet/payment operations, governance/ops/admin actions, notifications, credit, shop/task/project/goods flows, and file binding.
- File binding now checks uploader ownership and target ownership for goods, project ads, reward tasks, shops, and service items.
- Frontend auth state restores via `/auth/me`, has 401/403 handling and route guards, and removes production-sensitive demo user/admin fallbacks from protected flows.
- Additional safety fixes: payment order detail requires payer/admin access, `/api/goods/orders/**` is no longer covered by anonymous goods-detail GET matching, and `ResponseStatusException` maps to `ApiResponse` with proper status.

Verified after Phase 10:

- Local frontend `npm --prefix frontend run test -- src/api/client.test.ts` passed.
- Local frontend `npm --prefix frontend run build` passed with only known Vite large chunk and dependency pure-comment warnings.
- Local `git diff --check` and `git diff --cached --check` had no whitespace errors before commit.
- Server Docker backend build succeeded; Maven package completed inside Docker with `BUILD SUCCESS`.
- Server Docker frontend build succeeded; Vite emitted only known large chunk and dependency pure-comment warnings.
- Production containers running: MySQL healthy, backend running, web running.
- Server-local anonymous API smoke returned HTTP 200 for `/api/goods`, `/api/tasks`, `/api/shops`, `/api/project-ads`, and `/api/goods/1`.
- Server-local anonymous protected-route smoke returned HTTP 401 for `POST /api/goods`, `POST /api/wallet/users/1/recharges`, `GET /api/admin/wallet/recharges`, `GET /api/goods/orders/1`, `GET /api/goods/orders/buyer/1`, and `GET /api/payment/orders/nonexistent`.
- Server-local frontend route smoke returned HTTP 200 for `/`, `/auth`, `/goods`, `/tasks`, `/shops`, `/project-ads`, `/wallet`, and `/admin/wallet`.

Verification caveats:

- Authenticated user/admin smoke was not completed because known demo password candidates did not match production data; do not reset production accounts or read password hashes just for smoke verification.
- Playwriter browser verification was not completed because the local Playwriter Chrome extension was not connected. Use Playwriter after reconnecting the extension for visual desktop/mobile checks.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V12; add V13+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Phase 11 should focus on beta readiness after auth/security: create/verify usable demo credentials, complete authenticated API smoke, reconnect Playwriter for desktop/mobile verification, and polish any auth UX issues found.

## Latest Phase 11 beta readiness handoff, 2026-05-24

Phase 11 focuses on Beta readiness rather than new business functionality.

Implemented/expected Phase 11 artifacts:

- Design spec: `docs/superpowers/specs/2026-05-24-campushub-phase11-beta-readiness-design.md`.
- Implementation plan: `docs/superpowers/plans/2026-05-24-campushub-phase11-beta-readiness-upgrade.md`.
- Disabled-by-default Beta demo account reset controlled by `CAMPUSHUB_BETA_DEMO_RESET_ENABLED` and explicit demo student/admin environment variables.
- Authenticated smoke script: `scripts/beta-auth-smoke.sh`.
- Operations docs under `docs/operations/`: beta runbook, backup/restore guide, and admin playbook.
- User-facing policy/risk page: `/policy`.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment-center token, database password, or Alipay key contents.
- Production real payment remains in API-Transfer-Station; CampusHub must not handle Alipay key bodies directly.
- Do not edit already-applied migrations V1-V12; add V13+ only if a future schema change is unavoidable.
- Demo reset must only target configured Beta demo accounts and should be disabled after the reset run.
- Use low-impact production verification on the small shared server: targeted backend/web builds, API smoke, and Playwriter checks.

Recommended Phase 12+ directions:

- Beta feedback fixes from the Phase 11 acceptance matrix;
- focused monitoring/alerting improvements;
- map/location enhancement;
- WeChat mini-program exploration;
- recommendation/search improvements;
- real-time chat only after governance and privacy requirements are revisited.

