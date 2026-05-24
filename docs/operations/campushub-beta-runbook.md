# CampusHub Beta Runbook

## 1. Safety rules

- Do not print, read, copy, or commit production `.env`, SMTP passwords, JWT secrets, payment-center tokens, database passwords, or Alipay key bodies.
- Do not edit already-applied Flyway migrations V1-V12. Add V13+ only when a schema change is unavoidable.
- Use low-impact checks on the shared small server. Do not run stress tests.
- Keep CampusHub separate from `/opt/ai-relay`; API-Transfer-Station remains responsible for real Alipay integration.

## 2. Demo credential reset

The backend supports a disabled-by-default demo reset path controlled by environment variables:

- `CAMPUSHUB_BETA_DEMO_RESET_ENABLED=true`
- `CAMPUSHUB_BETA_STUDENT_EMAIL`
- `CAMPUSHUB_BETA_STUDENT_USERNAME`
- `CAMPUSHUB_BETA_STUDENT_PASSWORD`
- `CAMPUSHUB_BETA_STUDENT_NO`
- `CAMPUSHUB_BETA_STUDENT_PHONE`
- `CAMPUSHUB_BETA_STUDENT_WECHAT` or `CAMPUSHUB_BETA_STUDENT_QQ`
- `CAMPUSHUB_BETA_ADMIN_EMAIL`
- `CAMPUSHUB_BETA_ADMIN_USERNAME`
- `CAMPUSHUB_BETA_ADMIN_PASSWORD`
- `CAMPUSHUB_BETA_ADMIN_STUDENT_NO`
- `CAMPUSHUB_BETA_ADMIN_PHONE`
- `CAMPUSHUB_BETA_ADMIN_WECHAT` or `CAMPUSHUB_BETA_ADMIN_QQ`

The reset creates or resets only the configured demo student/admin accounts, assigns required roles, ensures wallet accounts, and logs no passwords. Disable `CAMPUSHUB_BETA_DEMO_RESET_ENABLED` after the reset run.

## 3. Authenticated API smoke

Run from a trusted machine or the server with credentials supplied as environment variables. Do not paste passwords into chat or commit them to files.

```bash
CAMPUSHUB_BASE_URL=https://ustc.suntomb.qzz.io \
CAMPUSHUB_SMOKE_STUDENT_EMAIL='demo-student@example.edu.cn' \
CAMPUSHUB_SMOKE_STUDENT_PASSWORD='set-outside-git' \
CAMPUSHUB_SMOKE_ADMIN_EMAIL='demo-admin@example.edu.cn' \
CAMPUSHUB_SMOKE_ADMIN_PASSWORD='set-outside-git' \
./scripts/beta-auth-smoke.sh
```

Expected result: anonymous public reads return 200, anonymous protected routes return 401, student login works, student admin access returns 403, private mismatch returns 403, and admin representative endpoints return 200.

## 4. Pre-deploy checklist

1. Confirm local branch and commit:
   ```bash
   git rev-parse --abbrev-ref HEAD
   git log --oneline -1
   git status --short
   ```
2. Confirm no real secret files are staged:
   ```bash
   git status --short
   git diff --cached --name-only
   ```
3. Review migrations. If a new migration exists, verify it is V13+ and that V1-V12 are unchanged.
4. Push code to GitHub only after tests/builds pass.

## 5. Low-impact production deploy

Run on the server in `/opt/campushub`:

```bash
git fetch origin
git rev-parse --abbrev-ref HEAD
git log --oneline -1
git pull --ff-only
```

For backend changes:

```bash
docker compose -f docker-compose.prod.yml build backend
docker compose -f docker-compose.prod.yml up -d backend
```

For frontend-only changes:

```bash
docker compose -f docker-compose.prod.yml build web
docker compose -f docker-compose.prod.yml up -d web
```

For shared backend/frontend changes, build backend first, then web. Avoid rebuilding MySQL.

## 6. Health checks

```bash
docker compose -f docker-compose.prod.yml ps
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/goods
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/tasks
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/shops
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/project-ads
```

Expected public route/API status: 200.

## 7. Logs

Inspect recent logs only. Do not print environment variables or secret files.

```bash
docker compose -f docker-compose.prod.yml logs --tail=100 backend
docker compose -f docker-compose.prod.yml logs --tail=100 web
```

Look for startup failures, Flyway failures, 401/403 regressions, and repeated 5xx responses.

## 8. Browser acceptance matrix

Desktop viewport:

| Area | Route | Expected |
| --- | --- | --- |
| Login | `/auth` | Demo student/admin can login; session display updates |
| Public browse | `/`, `/tasks`, `/goods`, `/shops`, `/project-ads` | Pages render without white screen |
| User center | `/wallet`, `/roles`, `/notifications`, `/credit` | Student can load owned data |
| Admin wallet | `/admin/wallet` | Admin page renders and student is blocked |
| Admin payment | `/admin/payment` | Orders/callback monitor renders |
| Admin ops | `/admin/ops` | Analytics cards and export controls render |
| Admin governance | `/admin/governance` | Dashboard and report queue render |
| Admin review | `/admin/review` | Review workspace renders |

Mobile viewport 390x844:

| Route | Expected |
| --- | --- |
| `/` | bottom tab visible, no document horizontal overflow |
| `/tasks` | list/filter usable, no document horizontal overflow |
| `/goods` | cards usable, no document horizontal overflow |
| `/shops` | cards usable, no document horizontal overflow |
| `/project-ads` | list usable, no document horizontal overflow |
| `/wallet` | wallet cards/dialog entry usable |
| `/notifications` | list or empty state usable |
| `/credit` | credit history/empty state usable |
| `/admin/wallet` | page renders without white screen |
| `/admin/payment` | page renders without white screen |
| `/admin/ops` | page renders without white screen |
| `/admin/governance` | page renders without white screen |
| `/admin/review` | page renders without white screen |

Record route, viewport, expected result, observed result, and any follow-up fix.

## 9. Rollback

Rollback is a human-approved operation. Use the last known-good commit from deployment notes.

```bash
git log --oneline -5
git checkout <known-good-commit>
docker compose -f docker-compose.prod.yml build backend web
docker compose -f docker-compose.prod.yml up -d backend web
docker compose -f docker-compose.prod.yml ps
```

If the failed deploy applied a database migration, do not manually delete or edit data. Stop and decide whether to forward-fix or restore from backup during a maintenance window.

## 10. Incident notes

For production incidents, record:

- current commit;
- time window;
- affected routes/APIs;
- recent backend/web logs;
- whether migrations ran;
- whether payment-center callbacks were involved.

Do not paste secrets, tokens, `.env`, payment callback tokens, SMTP passwords, database passwords, or Alipay key bodies into incident notes.
