# PostgreSQL PITR (Point-in-Time Recovery) Guide
#
# This document describes how to enable WAL archiving and pgBackRest
# for point-in-time recovery on PostgreSQL 17 in production.

## Why PITR?

Flyway migrations handle schema evolution, but they don't protect against:
- Accidental tenant data deletion
- Corrupt data writes
- Rogue migrations that need rollback
- Disaster recovery (hardware failure, natural disaster)

PITR allows you to restore your database to **any second in the past**.

## Architecture

```
┌─────────────┐      WAL Segments      ┌─────────────────┐
│ PostgreSQL  │ ──────────────────────► │  WAL Archive    │
│   (Primary) │   (pgBackRest push)     │  (MinIO Bucket) │
└──────┬──────┘                         └─────────────────┘
       │
       │ pgBackRest restore
       ▼
┌─────────────┐      WAL Replay        ┌─────────────────┐
│ PostgreSQL  │ ◄───────────────────── │  WAL Archive    │
│  (Standby)  │   (pgBackRest pull)     │  (MinIO Bucket) │
└─────────────┘                         └─────────────────┘
```

## Step 1: Enable WAL Archiving in PostgreSQL

Add to your PostgreSQL configuration (`postgresql.conf` or via Helm values):

```ini
# WAL Level
wal_level = logical
archive_mode = on
archive_command = 'pgbackrest --stanza=aurora archive-push %p'
archive_timeout = 60  # Force WAL switch every 60s

# Replication
max_wal_senders = 10
wal_keep_size = 1GB
```

## Step 2: Install pgBackRest

pgBackRest is included in the PostgreSQL Docker image or installed separately:

```bash
# Debian/Ubuntu
apt-get install -y pgbackrest

# Or use the official Docker image
docker pull pgbackrest/pgbackrest:latest
```

## Step 3: Configure pgBackRest

Create `/etc/pgbackrest/pgbackrest.conf`:

```ini
[aurora]
pg1-path = /var/lib/postgresql/data
pg1-port = 5432
pg1-user = aurora

# MinIO as backup storage (S3-compatible)
repo1-type = s3
repo1-path = /aurora-backups
repo1-region = us-east-1
repo1-endpoint = aurora-minio:9000
repo1-s3-uri-style = path
repo1-s3-key = minioadmin
repo1-s3-key-secret = your_minio_secret
repo1-s3-tls = false

# Retention
repo1-retention-full = 30
repo1-retention-diff = 7

# Backup types
repo1-bundle = y
repo1-block = y
repo1-cipher-type = aes-256-cbc
repo1-cipher-pass = your_encryption_key
```

## Step 4: Initialize pgBackRest Stanza

```bash
# Create stanza (initializes the backup catalog)
pgbackrest --stanza=aurora stanza-create

# Verify configuration
pgbackrest --stanza=aurora check
```

## Step 5: Run Backups

```bash
# Full backup (weekly)
pgbackrest --stanza=aurora --type=full backup

# Differential backup (daily)
pgbackrest --stanza=aurora --type=diff backup

# Incremental backup (hourly, default)
pgbackrest --stanza=aurora --type=incr backup

# List all backups
pgbackrest --stanza=aurora info
```

## Step 6: Point-in-Time Recovery

### Restore to a specific timestamp

```bash
# Stop PostgreSQL
systemctl stop postgresql

# Restore to a specific point in time
pgbackrest --stanza=aurora \
  --type=time \
  --target="2026-05-04 14:30:00" \
  --target-action=promote \
  restore

# Start PostgreSQL
systemctl start postgresql
```

### Restore to before a specific transaction

```bash
pgbackrest --stanza=aurora \
  --type=xid \
  --target="12345" \
  --target-action=promote \
  restore
```

### Restore to a named restore point

```bash
# Create a named restore point
SELECT pg_create_restore_point('before_migration_v4');

# Restore to that point
pgbackrest --stanza=aurora \
  --type=name \
  --target="before_migration_v4" \
  --target-action=promote \
  restore
```

## Kubernetes Integration

### pgBackRest Sidecar Container

Add to your Helm values:

```yaml
backup:
  pitr:
    enabled: true
    image:
      repository: pgbackrest/pgbackrest
      tag: latest
    schedule:
      full: "0 2 * * 0"    # Weekly full backup (Sunday 2 AM)
      diff: "0 2 * * 1-6"  # Daily diff backup (Mon-Sat 2 AM)
    retention:
      full: 30
      diff: 7
    encryption:
      enabled: true
      cipherPass: ${PGBACKREST_CIPHER_PASS}
    minio:
      endpoint: "http://aurora-minio:9000"
      bucket: "aurora-backups"
      accessKey: ${MINIO_ACCESS_KEY}
      secretKey: ${MINIO_SECRET_KEY}
```

## Monitoring

### pgBackRest Info

```bash
# Show backup status
pgbackrest --stanza=aurora info

# Output:
# stanza: aurora
#     status: ok
#     cipher: aes-256-cbc
#
#     db (current)
#         wal archive min/max (17): 000000010000000100000001 / 00000001000000270000008F
#
#     full backup: 20260504-020000F
#         timestamp start/stop: 2026-05-04 02:00:00 / 2026-05-04 02:15:30
#         wal start/stop: 000000010000002700000080 / 00000001000000270000008F
#         database size: 12.3GB
#         backup size: 4.1GB
#         repository size: 2.8GB
```

### Grafana Dashboard

pgBackRest exports metrics via its monitoring endpoint. Add to your Grafana:

```yaml
# Prometheus scrape config
- job_name: pgbackrest
  static_configs:
    - targets: ['pgbackrest:9090']
```

## Recovery Time Objectives

| Metric | Target | Achieved With |
|--------|--------|---------------|
| RPO (Recovery Point) | < 1 minute | WAL archiving (archive_timeout=60s) |
| RTO (Recovery Time) | < 30 minutes | pgBackRest parallel restore |
| Retention | 30 days | MinIO + repo1-retention-full=30 |

## Disaster Recovery Checklist

1. [ ] Verify pgBackRest stanza is healthy: `pgbackrest --stanza=aurora check`
2. [ ] Confirm latest backup exists: `pgbackrest --stanza=aurora info`
3. [ ] Test restore on a separate PostgreSQL instance (monthly drill)
4. [ ] Verify MinIO bucket accessibility
5. [ ] Confirm encryption keys are backed up securely
6. [ ] Document recovery procedure for your team
