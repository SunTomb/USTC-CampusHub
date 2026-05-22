# CampusHub Phase 7 Responsive UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CampusHub credible and usable on mobile browsers by improving responsive layout, navigation, shared states, publish forms, and key page workflows without adding new business domains.

**Architecture:** Phase 7 is a frontend-focused UX pass. It keeps the existing Vue 3 + Element Plus structure, adds only a few focused shared components, consolidates responsive CSS in `frontend/src/styles.css`, and updates existing views in place. Backend and Flyway migrations are out of scope unless a small API-display bug is discovered; production database migrations V1-V10 must not be edited.

**Tech Stack:** Vue 3, Vite, TypeScript, Pinia, Vue Router, Axios, Element Plus, CSS media queries, Docker/production build verification, Playwriter browser checks.

---

## Scope and constraints

- Do not use PowerShell or PowerShell commands.
- Do not install local dependencies unless the user explicitly changes the preference.
- Prefer dedicated Read/Edit/Grep/Glob/Git MCP tools for local work.
- Use Bash only after explicit authorization for shell-only verification, git push, SSH, server Docker build, or deployment.
- Do not read, print, copy, or commit real `.env`, SMTP passwords, JWT secrets, payment tokens, or Alipay key contents.
- Do not edit already-applied Flyway migrations V1-V10. Phase 7 should not add a migration.
- Do not expand Phase 7 into payment-center hardening, auth/RBAC hardening, a native app, WeChat mini-program, or full design-system rewrite.
- Keep changes small and reversible.

## Task summary

1. Add shared UX primitives and responsive shell.
2. Polish account, wallet, roles, notifications, and credit pages.
3. Polish runner task hall and workspace.
4. Polish second-hand goods list, detail, and publish flows.
5. Polish shop marketplace, detail, and merchant workspace.
6. Polish project ads showcase, detail, and management flows.
7. Polish admin pages for mobile safety without reducing desktop efficiency.
8. Update documentation and verify with build/API/browser checks.

## Implementation details

### Task 1: Shared UX primitives and responsive shell

Create:
- `frontend/src/components/common/EmptyState.vue`
- `frontend/src/components/common/FormSection.vue`
- `frontend/src/components/common/PageActions.vue`

Modify:
- `frontend/src/layouts/MainLayout.vue`
- `frontend/src/styles.css`

Implement a mobile drawer navigation while preserving desktop sidebar. Add shared CSS classes: `.state-card`, `.form-section`, `.page-actions`, `.mobile-table-wrapper`, mobile header/content spacing, mobile dialog width, and touch-friendly action wrapping.

### Task 2: Account pages

Modify:
- `frontend/src/views/AuthView.vue`
- `frontend/src/views/NotificationsView.vue`
- `frontend/src/views/RoleApplicationsView.vue`
- `frontend/src/views/WalletView.vue`
- `frontend/src/views/CreditCenterView.vue`
- `frontend/src/styles.css`

Add shared empty states, clearer contact/privacy guidance, and responsive account/wallet/credit layouts.

### Task 3: Runner tasks

Modify:
- `frontend/src/views/TasksView.vue`
- `frontend/src/views/TaskWorkspaceView.vue`
- `frontend/src/styles.css`

Add task empty/error states, improve login prompts, and make publish/workspace sections easier to use on mobile. Do not change API payload fields.

### Task 4: Goods

Modify:
- `frontend/src/views/GoodsView.vue`
- `frontend/src/views/GoodsDetailView.vue`
- `frontend/src/views/GoodsPublishView.vue`
- `frontend/src/styles.css`

Add goods empty/error states, contact-boundary copy, and responsive publish form sections.

### Task 5: Shops

Modify:
- `frontend/src/views/ShopsView.vue`
- `frontend/src/views/ShopDetailView.vue`
- `frontend/src/views/ShopMerchantView.vue`
- `frontend/src/styles.css`

Add shop empty/error states, booking boundary copy, and responsive merchant form sections.

### Task 6: Project ads

Modify:
- `frontend/src/views/ProjectAdsView.vue`
- `frontend/src/views/ProjectAdDetailView.vue`
- `frontend/src/views/ProjectAdManageView.vue`
- `frontend/src/styles.css`

Add project empty/error states and responsive project form sections.

### Task 7: Admin mobile safety

Modify:
- `frontend/src/views/AdminOperationsView.vue`
- `frontend/src/views/AdminGovernanceView.vue`
- `frontend/src/views/AdminReviewView.vue`
- `frontend/src/styles.css`

Wrap tables in `.mobile-table-wrapper`, add compact empty states, and keep desktop table efficiency.

### Task 8: Documentation and verification

Modify:
- `README.md`
- `CLAUDE.md`

Add Phase 7 summary and handoff only after implementation/verification. Preferred verification is server-side Docker build, safe API smoke, and Playwriter desktop/tablet/mobile checks. Known Vite large chunk warning is not a failure.

## Verification matrix

Build:
- `docker compose -f docker-compose.prod.yml build campushub-web` in server/approved environment.

Safe API smoke:
- `/api/goods`
- `/api/tasks`
- `/api/shops`
- `/api/project-ads`
- `/api/admin/ops/analytics/overview`
- `/api/admin/governance/dashboard`
- `/api/credit/users/1`

Browser viewports:
- Desktop 1365x768.
- Tablet-ish 820x1180.
- Mobile 390x844.

Key pages:
- `/auth`, `/tasks`, `/goods`, `/goods/publish`, `/shops`, `/shops/merchant`, `/project-ads`, `/project-ads/manage`, `/wallet`, `/roles`, `/notifications`, `/credit`, `/admin/ops`, `/admin/governance`, `/admin/review`.

Pass condition: no white screen, no obvious Element Plus runtime error, no uncontrolled document-level horizontal overflow on user-facing mobile pages, main actions visible, forms/dialogs usable.

## Self-review

This plan covers the Phase 7 design scope, avoids payment/auth/schema expansion, and keeps verification aligned with the user's no-local-install and low-impact server constraints.
