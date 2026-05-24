# CampusHub Backup and Restore Guide

## 1. Rules

- Do not print `.env`, database passwords, JWT secrets, payment-center tokens, SMTP passwords, or Alipay key bodies.
- Do not paste SQL dumps into chat.
- Do not restore over production without explicit approval and a maintenance window.
- Prefer restore rehearsal into a temporary database or disposable environment.

## 2. Backup

Run on the production server in `/opt/campushub`.

Create a restricted backup directory:

```bash
mkdir -p /opt/campushub/backups
chmod 700 /opt/campushub/backups
```

Create a timestamped dump from inside the MySQL container. This uses container environment variables and does not print the password:

```bash
backup_file="/opt/campushub/backups/campushub-$(date +%Y%m%d-%H%M%S).sql"
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > "$backup_file"
chmod 600 "$backup_file"
ls -lh "$backup_file"
```

Expected: backup file exists and has non-zero size.

## 3. Restore rehearsal into temporary database

Create a temporary database in the MySQL container:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS campushub_restore_check"'
```

Restore the dump into the temporary database:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" campushub_restore_check' < "$backup_file"
```

Verify table count without printing table contents:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '\''campushub_restore_check'\''"'
```

Expected: a positive table count.

Drop the temporary database after the rehearsal:

```bash
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE campushub_restore_check"'
```

## 4. Production restore

Production restore is destructive. Before restoring:

1. Confirm the exact backup file.
2. Stop backend/web to prevent writes.
3. Take one more fresh backup.
4. Get explicit approval for the maintenance window.

Command shape:

```bash
docker compose -f docker-compose.prod.yml stop backend web
docker compose -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < "$backup_file"
docker compose -f docker-compose.prod.yml up -d backend web
```

After restore, run container health checks, anonymous API smoke, authenticated API smoke, and browser smoke.
