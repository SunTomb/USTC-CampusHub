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
- Email-code registration is implemented under `auth`: send-code and register endpoints normalize and validate `edu.cn` email addresses, store only hashed verification codes, enforce TTL/resend/attempt limits, create a user, assign `ROLE_STUDENT` when present, and create a wallet account.
- Wallet, payment, moderation, interaction, and file APIs use the shared target model where appropriate: `target_type` + `target_id`.
- Current `SecurityConfig` permits `/api/**` for the course prototype; token-aware frontend state exists but most backend APIs are still open for local demonstration.

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
