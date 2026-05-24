# CampusHub Phase 11 Beta Readiness Design

## 1. Goal

Phase 11 moves CampusHub from a deployed feature platform into a controlled Beta-ready state. It does not add a new business line. It closes the Phase 10 verification gap by establishing safe demo credentials, authenticated smoke coverage, browser acceptance coverage, operational runbooks, admin playbooks, user-facing policy/risk guidance, and a final handoff process.

The target outcome is that a maintainer can safely deploy, verify, demonstrate, operate, and roll back CampusHub without reading secrets, resetting unknown production accounts manually, or relying on undocumented tribal knowledge.

## 2. Current baseline

The starting point is branch `phase9-wallet-escrow` at `f97e9bc` with Phase 10 auth/security hardening deployed. JWT request authentication, `/api/auth/me`, `ROLE_ADMIN` checks, same-user private path checks, file binding ownership checks, frontend 401/403 handling, and route guards are already implemented.

Phase 10 verification completed anonymous public API smoke, anonymous protected-route 401 smoke, server Docker builds, and route HTTP checks. Two gaps remain:

1. Authenticated production smoke was not completed because known demo password candidates did not match production data.
2. Playwriter visual verification was not completed because the local Chrome extension was disconnected.

Phase 11 addresses those gaps while preserving the existing payment boundary: CampusHub never reads, prints, copies, stores, or commits real `.env`, SMTP passwords, JWT secrets, payment-center tokens, or Alipay key bodies. Real payment handling remains in API-Transfer-Station.

## 3. Scope

### In scope

- Safe demo student and demo admin credential strategy.
- Authenticated API smoke matrix for anonymous, student, mismatch, non-admin, and admin cases.
- Playwriter desktop and mobile acceptance matrix covering public, user, wallet, role, notification, credit, and admin workspaces.
- Production deployment, rollback, health check, log, API smoke, and browser smoke runbook.
- MySQL backup and restore rehearsal guidance that avoids printing secrets.
- Admin playbooks for governance, credit, restrictions, wallet recharges, withdrawals, payment callback monitoring, and review queues.
- User-facing agreement, privacy, trading-risk, wallet, and beta-scope guidance.
- Demo/seed data boundary between Beta demo accounts and real user data.
- README and CLAUDE handoff updates.

### Non-goals

- New business lines such as maps, mini-program, recommendations, or real-time chat.
- OAuth, SSO, refresh-token redesign, or enterprise RBAC.
- Direct CampusHub Alipay integration or Alipay key handling.
- Transaction principal escrow expansion beyond the existing Phase 9 goods flow.
- Editing already-applied Flyway migrations V1-V12.
- Full monitoring stack, Kubernetes, or multi-region deployment.

## 4. Design approach

Use a "Beta readiness wrapper" approach: add the minimum operational surfaces needed to verify and run the existing platform safely, rather than changing the platform's business model.

The implementation should prefer documentation and deterministic verification scripts over large new application features. Code changes should be limited to:

- a safe demo account bootstrap/reset mechanism;
- optional lightweight policy/risk pages and route links;
- test/smoke scripts that exercise authenticated paths;
- small frontend copy or UX fixes discovered during verification.

Schema changes should be avoided. If a schema change becomes unavoidable, it must be a new V13+ migration only.

## 5. Demo credential strategy

Phase 11 should not read existing password hashes or infer passwords from production data. Instead it should provide an explicit, repeatable, auditable way to create or reset two Beta demo accounts:

- demo student: normal authenticated user for public browse, write operations, wallet, roles, notifications, and credit center;
- demo admin: administrator for `/api/admin/**`, governance, wallet operations, payment monitor, ops analytics, and review queues.

Recommended implementation:

1. Add a backend startup runner or admin-only maintenance component that is disabled by default.
2. Gate it behind environment variables such as a boolean enable flag and explicit demo account email/password values.
3. Require strong non-placeholder passwords when enabled.
4. Create missing demo users or reset only the named demo users, never arbitrary existing users.
5. Ensure required roles and wallet accounts exist.
6. Log only high-level outcomes, never passwords or hashes.
7. Document that the enable flag should be turned off after the reset run.

This gives production maintainers a safe way to establish demo credentials without printing secrets or touching password hashes manually.

## 6. Authenticated API smoke design

The authenticated smoke should be executable from the production server or a trusted maintainer machine without exposing secrets in output. It should accept credentials through environment variables or prompt-free local shell variables and print only endpoint, status, and pass/fail outcome.

The matrix should cover:

- anonymous public reads return 200;
- anonymous writes/private/admin endpoints return 401;
- demo student login returns a token;
- `/api/auth/me` returns the demo student identity;
- demo student can call at least one safe write or user-owned action;
- demo student accessing another user's private path returns 403;
- demo student accessing `/api/admin/**` returns 403;
- demo admin login returns a token;
- demo admin `/api/auth/me` includes admin role information;
- demo admin can access representative admin endpoints under wallet, payment, ops, governance, and review.

The smoke should avoid destructive actions where possible. If a write action is required, use low-impact test data or idempotent actions such as listing owned records, marking own notification read only when safe, or creating clearly labeled Beta smoke data that can be reviewed later.

## 7. Browser acceptance matrix

Playwriter verification should cover both desktop and mobile viewports.

Desktop matrix:

- login and session restore;
- public browse: home, tasks, goods, shops, project ads;
- user flows: wallet, role applications, notifications, credit center;
- admin flows: wallet operations, payment monitor, ops analytics, governance, review;
- auth UX: anonymous user redirected or prompted on protected routes; non-admin blocked from admin routes.

Mobile matrix:

- home, tasks, goods, shops, project ads;
- wallet, roles, notifications, credit;
- admin wallet/payment/ops/governance/review pages render without white screen;
- bottom tab and directory drawer remain usable;
- tested pages have no document-level horizontal overflow.

Browser verification should record route, viewport, expected result, observed result, and any follow-up fix. It should not require screenshots unless a visual regression is found.

## 8. Operations runbook design

The production runbook should be written as a maintainer checklist, not a narrative. It should include:

- pre-deploy checks: branch, commit, working tree, secret hygiene, migration review;
- low-impact deployment sequence: pull, targeted backend/web build, restart only needed services;
- health checks: container status, backend health/API endpoints, frontend route HTTP checks;
- logs: how to inspect recent backend/web logs without printing secrets;
- API smoke: anonymous and authenticated commands or script usage;
- browser smoke: Playwriter desktop/mobile checklist;
- rollback: git rollback to previous known-good commit, rebuild/restart affected service, verify;
- migration caution: do not edit applied migrations; backup before risky migrations;
- incident notes: preserve logs, avoid destructive DB actions, document exact commit and time.

## 9. Backup and restore rehearsal design

The MySQL backup/restore guidance should be safe and secret-minimizing:

- use Docker Compose service names and environment variable references rather than printing credentials;
- write backups to a timestamped server-local path with restrictive permissions;
- verify backup file existence and non-zero size;
- rehearse restore into a temporary database or disposable local environment where feasible;
- never paste `.env` contents, database passwords, or full dumps into chat;
- document that production destructive restore requires explicit human approval and a maintenance window.

## 10. Admin playbook design

Admin playbooks should map real Beta operations to existing admin pages and APIs:

- report handling: triage, review, resolve/reject/escalate;
- violation records: severity, penalty, credit delta, notification expectations;
- credit adjustment: when to add/subtract credit and how to write concise reasons;
- restrictions: warning, posting freeze, service freeze, disabled state, review cadence;
- wallet recharges: Alipay callback path monitoring and WeChat manual review;
- withdrawals: freeze, approve, complete, reject, and audit notes;
- payment monitor: pending/paid/failed callback events and idempotency checks;
- review queues: role applications, project ads, shop merchant review if applicable;
- support hygiene: never ask users for passwords, payment secrets, or private keys.

## 11. User-facing policy and risk guidance

Phase 11 should expose or document user-facing Beta guidance:

- service agreement: CampusHub is a campus information and wallet/service-fee platform;
- privacy notice: contact information is collected for campus transactions and exposed only according to business rules;
- trading risk notice: offline WeChat/QQ dealings require caution; platform does not guarantee offline transaction principal;
- wallet notice: balance, frozen balance, recharge, withdrawal, service-fee, and escrow-style goods flow boundaries;
- beta notice: features may change, users should report issues, and administrators may moderate unsafe content.

A lightweight frontend policy page group is preferred if implementation time allows. Static in-repository Markdown documentation is acceptable as the minimum Beta readiness artifact.

## 12. Demo data and real data boundary

Demo accounts and smoke data must be visibly labeled as Beta/demo. They must not overwrite real users, real orders, real wallet records, or real moderation history except when explicitly targeting the configured demo account emails.

The runbook should state:

- demo credentials are for controlled Beta demonstration only;
- demo reset affects only configured demo accounts;
- seeded records should use names and descriptions that identify them as Beta smoke/demo data;
- cleanup should use targeted IDs or labels, not broad deletes;
- production data repair or deletion requires separate approval.

## 13. Verification and completion criteria

Phase 11 is complete when:

1. Design spec and implementation plan are written.
2. Demo credential strategy is implemented or documented with an executable safe path.
3. Authenticated API smoke has been run or is ready with clear commands and any blockers documented.
4. Playwriter desktop/mobile acceptance matrix has been run or any tooling blocker is documented with exact next steps.
5. Production runbook, backup/restore guidance, admin playbooks, and user-facing policy/risk guidance exist.
6. README and CLAUDE handoff describe the Phase 11 state and Phase 12+ non-goals.
7. Build and smoke verification are run with low impact before any completion claim.

## 14. Risks and mitigations

- Risk: demo reset accidentally modifies real accounts. Mitigation: require exact configured demo emails and never update arbitrary users.
- Risk: smoke scripts print tokens or secrets. Mitigation: only print pass/fail and HTTP status, never token bodies.
- Risk: backup commands expose passwords. Mitigation: use container environment references and document no secret printing.
- Risk: Phase 11 expands into new product work. Mitigation: defer maps, mini-program, recommendations, chat, and new payment flows to Phase 12+.
- Risk: server load during verification. Mitigation: targeted builds, low-frequency checks, no stress tests.
