#!/usr/bin/env bash
set -Eeuo pipefail

log() {
  echo "[INFO] $*"
}

fail() {
  echo "[ERROR] $*" >&2
  exit 1
}

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    fail "missing env: ${name}"
  fi
}

escape_conf() {
  printf "%s" "$1" | sed "s/'/''/g"
}

escape_pgpass() {
  printf "%s" "$1" | sed -e 's/\\/\\\\/g' -e 's/:/\\:/g'
}

ensure_replication_slot() {
  if [ -z "$SLOT_NAME" ]; then
    return 0
  fi
  local escaped_slot
  local slot_state
  escaped_slot="$(escape_conf "$SLOT_NAME")"
  log "ensure replication slot exists: $SLOT_NAME"
  slot_state="$(source_psql -Atqc "WITH existing AS (SELECT slot_name FROM pg_replication_slots WHERE slot_name = '$escaped_slot' AND slot_type = 'physical'), created AS (SELECT slot_name FROM pg_create_physical_replication_slot('$escaped_slot') WHERE NOT EXISTS (SELECT 1 FROM existing)) SELECT CASE WHEN EXISTS (SELECT 1 FROM existing) THEN 'exists' WHEN EXISTS (SELECT 1 FROM created) THEN 'created' ELSE 'missing' END" 2>/dev/null || true)"
  if [ "$slot_state" = "exists" ] || [ "$slot_state" = "created" ]; then
    log "replication slot ready: $SLOT_NAME ($slot_state)"
    return 0
  fi
  fail "failed to ensure replication slot: $SLOT_NAME"
}

require_env DBGUARDIAN_OLD_PGDATA
require_env DBGUARDIAN_NEW_PRIMARY_HOST
require_env DBGUARDIAN_NEW_PRIMARY_PORT
require_env DBGUARDIAN_REPL_USER
require_env DBGUARDIAN_REPL_PASS
require_env DBGUARDIAN_PG_CTL
require_env DBGUARDIAN_PSQL
require_env DBGUARDIAN_PG_REWIND

OLD_PGDATA="${DBGUARDIAN_OLD_PGDATA}"
CONFIG_FILE="${DBGUARDIAN_CONFIG_FILE:-}"
NEW_PRIMARY_HOST="${DBGUARDIAN_NEW_PRIMARY_HOST}"
NEW_PRIMARY_PORT="${DBGUARDIAN_NEW_PRIMARY_PORT}"
REPL_USER="${DBGUARDIAN_REPL_USER}"
REPL_PASS="${DBGUARDIAN_REPL_PASS}"
PG_CTL_CMD="${DBGUARDIAN_PG_CTL}"
PSQL_CMD="${DBGUARDIAN_PSQL}"
PG_REWIND_CMD="${DBGUARDIAN_PG_REWIND}"
LOCAL_HOST="${DBGUARDIAN_LOCAL_HOST:-127.0.0.1}"
LOCAL_PORT="${DBGUARDIAN_LOCAL_PORT:-5432}"
LOCAL_DB="${DBGUARDIAN_LOCAL_DB:-postgres}"
LOCAL_USER="${DBGUARDIAN_LOCAL_USER:-${REPL_USER}}"
LOCAL_PASS="${DBGUARDIAN_LOCAL_PASS:-${REPL_PASS}}"
SLOT_NAME="${DBGUARDIAN_SLOT_NAME:-}"
EXTRA_PRIMARY_CONNINFO="${DBGUARDIAN_EXTRA_PRIMARY_CONNINFO:-}"
STARTUP_WAIT_SECONDS="${DBGUARDIAN_STARTUP_WAIT_SECONDS:-60}"
LOCK_DIR="${TMPDIR:-/tmp}/dbguardian-pg-rewind-$(printf '%s' "$OLD_PGDATA" | cksum | awk '{print $1}')"

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  fail "another recovery process is already operating on pgdata: $OLD_PGDATA"
fi

PRIMARY_CONNINFO="host=$(escape_conf "$NEW_PRIMARY_HOST") port=$(escape_conf "$NEW_PRIMARY_PORT") user=$(escape_conf "$REPL_USER") password=$(escape_conf "$REPL_PASS") application_name=$(escape_conf "$(hostname)")"
if [ -n "$EXTRA_PRIMARY_CONNINFO" ]; then
  PRIMARY_CONNINFO="$PRIMARY_CONNINFO $(escape_conf "$EXTRA_PRIMARY_CONNINFO")"
fi

PGPASSFILE="$(mktemp "${TMPDIR:-/tmp}/dbguardian-pgpass.XXXXXX")"
cleanup_runtime_files() {
  rm -f "$PGPASSFILE"
  rmdir "$LOCK_DIR" 2>/dev/null || true
}
trap cleanup_runtime_files EXIT
chmod 600 "$PGPASSFILE"
printf '%s:%s:%s:%s:%s\n' \
  "$(escape_pgpass "$NEW_PRIMARY_HOST")" \
  "$(escape_pgpass "$NEW_PRIMARY_PORT")" \
  "$(escape_pgpass "postgres")" \
  "$(escape_pgpass "$REPL_USER")" \
  "$(escape_pgpass "$REPL_PASS")" > "$PGPASSFILE"

source_psql() {
  PGPASSFILE="$PGPASSFILE" "$PSQL_CMD" \
    -h "$NEW_PRIMARY_HOST" \
    -p "$NEW_PRIMARY_PORT" \
    -U "$REPL_USER" \
    -d postgres "$@"
}

source_rewind() {
  PGPASSFILE="$PGPASSFILE" "$PG_REWIND_CMD" \
    -D "$OLD_PGDATA" \
    --source-server="host=$NEW_PRIMARY_HOST port=$NEW_PRIMARY_PORT user=$REPL_USER dbname=postgres" "$@"
}

ensure_old_primary_execution_host() {
  local local_ips
  local primary_ips
  local ip

  local_ips="$(hostname -I 2>/dev/null || true)"
  primary_ips="$(getent ahostsv4 "$NEW_PRIMARY_HOST" 2>/dev/null | awk '{print $1}' | sort -u || true)"
  [ -n "$primary_ips" ] || primary_ips="$NEW_PRIMARY_HOST"

  for ip in $primary_ips; do
    case " $local_ips " in
      *" $ip "*)
        fail "refusing to run on current primary host: localIp=$ip, newPrimary=$NEW_PRIMARY_HOST:$NEW_PRIMARY_PORT; remote executor must target the old primary"
        ;;
    esac
  done
}

PGREWIND_MISSING_GRANTS=""

probe_source_function() {
  local test_sql="$1"
  local grant_sql="$2"
  if ! source_psql -v ON_ERROR_STOP=1 -Atqc "$test_sql" >/dev/null 2>&1; then
    PGREWIND_MISSING_GRANTS="${PGREWIND_MISSING_GRANTS}${grant_sql}"$'\n'
  fi
}

check_source_privileges() {
  PGREWIND_MISSING_GRANTS=""
  probe_source_function \
    "select pg_read_binary_file('global/pg_control') is not null" \
    "GRANT EXECUTE ON FUNCTION pg_read_binary_file(text) TO \"$REPL_USER\";"
  probe_source_function \
    "select pg_read_binary_file('global/pg_control', 0, 1, true) is not null" \
    "GRANT EXECUTE ON FUNCTION pg_read_binary_file(text, bigint, bigint, boolean) TO \"$REPL_USER\";"
  probe_source_function \
    "select pg_stat_file('global/pg_control', true) is not null" \
    "GRANT EXECUTE ON FUNCTION pg_stat_file(text, boolean) TO \"$REPL_USER\";"
  probe_source_function \
    "select pg_ls_dir('.', true, false) is not null" \
    "GRANT EXECUTE ON FUNCTION pg_ls_dir(text, boolean, boolean) TO \"$REPL_USER\";"
  if [ -n "$PGREWIND_MISSING_GRANTS" ]; then
    echo "[ERROR] pg_rewind source privileges missing for user \"$REPL_USER\" on $NEW_PRIMARY_HOST:$NEW_PRIMARY_PORT (database postgres)" >&2
    echo "[ERROR] execute the following on the new primary as a superuser (e.g. postgres):" >&2
    printf '%s' "$PGREWIND_MISSING_GRANTS" >&2
    fail "source privilege check failed before stopping old primary"
  fi
}

check_local_recovery_mode() {
  PGPASSWORD="$LOCAL_PASS" "$PSQL_CMD" -h "$LOCAL_HOST" -p "$LOCAL_PORT" -U "$LOCAL_USER" -d "$LOCAL_DB" -Atqc "select case when pg_is_in_recovery() then 't' else 'f' end" 2>/dev/null || return 1
}

precheck_target_rewind_capability() {
  local caps
  caps="$(PGPASSWORD="$LOCAL_PASS" "$PSQL_CMD" -h "$LOCAL_HOST" -p "$LOCAL_PORT" -U "$LOCAL_USER" -d "$LOCAL_DB" -Atqc "select current_setting('wal_log_hints') || ':' || current_setting('data_checksums')" 2>/dev/null || true)"
  if [ -z "$caps" ]; then
    log "skip target rewind capability precheck: old primary not reachable for query"
    return 0
  fi
  if [ "${caps%%:*}" = "on" ] || [ "${caps##*:}" = "on" ]; then
    log "target rewind capability ok (wal_log_hints:data_checksums = $caps)"
    return 0
  fi
  echo "[ERROR] old primary lacks pg_rewind prerequisite (wal_log_hints:data_checksums = $caps)" >&2
  echo "[ERROR] pg_rewind cannot run against this target and would be unsafe" >&2
  echo "[ERROR] fix path: enable 'wal_log_hints = on' on both nodes (ALTER SYSTEM SET wal_log_hints=on; then restart) or init the cluster with data checksums; the already-diverged old primary must then be rebuilt with pg_basebackup" >&2
  fail "target rewind capability check failed before stopping old primary"
}

log "verify recovery execution host"
ensure_old_primary_execution_host

log "check new primary status"
source_psql -Atqc "select case when pg_is_in_recovery() then 'standby' else 'primary' end" | grep -qx "primary" || fail "new primary is not writable"
log "verify pg_rewind source privileges"
check_source_privileges
log "precheck target rewind capability"
precheck_target_rewind_capability
ensure_replication_slot

[ -d "$OLD_PGDATA" ] || fail "pgdata not found: $OLD_PGDATA"
if [ -n "$CONFIG_FILE" ] && [ ! -f "$CONFIG_FILE" ]; then
  fail "configured PostgreSQL config file not found: $CONFIG_FILE"
fi

log "stop old primary"
"$PG_CTL_CMD" -D "$OLD_PGDATA" -m fast stop >/dev/null 2>&1 || true
sleep 2

log "dry-run pg_rewind"
source_rewind --dry-run >/dev/null || fail "pg_rewind dry-run failed"

log "run pg_rewind"
source_rewind >/dev/null || fail "pg_rewind failed"

AUTO_CONF="$OLD_PGDATA/postgresql.auto.conf"
mkdir -p "$OLD_PGDATA"
{
  echo "primary_conninfo = '$PRIMARY_CONNINFO'"
  if [ -n "$SLOT_NAME" ]; then
    echo "primary_slot_name = '$(escape_conf "$SLOT_NAME")'"
  fi
} >> "$AUTO_CONF"

touch "$OLD_PGDATA/standby.signal"

log "start old primary as standby"
PG_CTL_START_LOG="$(mktemp "${TMPDIR:-/tmp}/dbguardian-pg-ctl-start.XXXXXX")"
PG_CTL_START_COMMAND=("$PG_CTL_CMD" -D "$OLD_PGDATA")
if [ -n "$CONFIG_FILE" ]; then
  PG_CTL_START_COMMAND+=( -o "-c config_file=$CONFIG_FILE" )
fi
PG_CTL_START_COMMAND+=( -l "$PG_CTL_START_LOG" -w -t "$STARTUP_WAIT_SECONDS" start )
if ! "${PG_CTL_START_COMMAND[@]}"; then
  echo "[ERROR] failed to start old primary; pg_ctl output follows:" >&2
  [ -s "$PG_CTL_START_LOG" ] && tail -n 80 "$PG_CTL_START_LOG" >&2
  rm -f "$PG_CTL_START_LOG"
  fail "failed to start old primary"
fi
rm -f "$PG_CTL_START_LOG"

log "wait recovery mode"
for _ in $(seq 1 "$STARTUP_WAIT_SECONDS"); do
  if [ "$(check_local_recovery_mode || true)" = "t" ]; then
    log "old node recovered as standby"
    exit 0
  fi
  sleep 1
done

fail "old node did not enter recovery mode"