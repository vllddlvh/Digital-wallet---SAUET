#!/usr/bin/env bash
# Dump DB sau bài test.
#
# Modes:
#   --mode=all        Full dump (schema + data).
#   --mode=violated   Chỉ dump các account có wallet_id xuất hiện trong
#                     CSV k6 ở metric "invariant_violation". "Lịch sử
#                     account" = MỌI giao dịch mà ví đó là source hoặc
#                     target, không giới hạn ở giao dịch gây vi phạm.
#
# Inputs:
#   --from-csv=<path>      CSV k6 (bắt buộc nếu --mode=violated và không có --wallet-ids)
#   --wallet-ids=1,2,3     Override thủ công
#   --out=<dir>            Thư mục đầu ra (default: $RESULTS_DIR)
#   --grace=<sec>          Chờ trước khi dump, để in-flight request kết thúc
#
# Outputs:
#   all:       <out>/full_dump.sql  + dump_summary.json
#   violated:  <out>/violated_accounts.csv
#              <out>/violated_transactions.csv
#              <out>/violated_linked_banks.csv  (nếu bảng tồn tại)
#              <out>/dump_summary.json
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

MODE=""
FROM_CSV=""
WALLET_IDS_OVERRIDE=""
GRACE=0
OUT=""

while [ $# -gt 0 ]; do
  case "$1" in
    --mode=*)         MODE="${1#*=}" ;;
    --from-csv=*)     FROM_CSV="${1#*=}" ;;
    --wallet-ids=*)   WALLET_IDS_OVERRIDE="${1#*=}" ;;
    --out=*)          OUT="${1#*=}" ;;
    --grace=*)        GRACE="${1#*=}" ;;
    -h|--help)        sed -n '2,30p' "$0"; exit 0 ;;
    *) die "unknown arg: $1" ;;
  esac; shift
done

[ -n "$MODE" ] || die "--mode is required (all|violated)"
OUT="${OUT:-$RESULTS_DIR}"
mkdir -p "$OUT"
[ "$GRACE" -gt 0 ] && { log "grace ${GRACE}s"; sleep "$GRACE"; }

# ── --mode=all ───────────────────────────────────────────────────────────────
dump_all() {
  local out_sql="$OUT/full_dump.sql"
  log "mysqldump → $out_sql"
  mysqldump -u"$MYSQL_ROOT_USER" \
    --single-transaction --no-tablespaces \
    --skip-comments --routines --triggers \
    "$DB_NAME" > "$out_sql"
  ls -lh "$out_sql"

  local bytes; bytes=$(stat -c%s "$out_sql" 2>/dev/null || stat -f%z "$out_sql")
  cat > "$OUT/dump_summary.json" <<JSON
{
  "mode": "all",
  "database": "${DB_NAME}",
  "full_dump_bytes": ${bytes},
  "outputs": ["full_dump.sql"]
}
JSON
}

# ── --mode=violated ──────────────────────────────────────────────────────────
extract_wallet_ids_from_csv() {
  python3 - "$1" <<'PY'
import csv, sys
path = sys.argv[1]
seen = set()
with open(path, newline='') as f:
    reader = csv.DictReader(f)
    if not reader.fieldnames or 'wallet_id' not in reader.fieldnames:
        print(''); raise SystemExit(0)
    for row in reader:
        if row.get('metric_name') != 'invariant_violation':
            continue
        w = (row.get('wallet_id') or '').strip()
        if w:
            seen.add(w)
print(','.join(sorted(seen, key=lambda x: int(x) if x.isdigit() else x)))
PY
}

# TSV → CSV chuẩn (quote field, đổi \N → rỗng).
tsv_to_csv() {
  awk -F'\t' 'BEGIN{OFS=","} {
    for(i=1;i<=NF;i++){
      if ($i=="\\N") $i=""
      gsub(/"/, "\"\"", $i)
      $i = "\"" $i "\""
    }
    print
  }'
}

dump_violated() {
  local wallet_ids="$WALLET_IDS_OVERRIDE"
  if [ -z "$wallet_ids" ]; then
    [ -n "$FROM_CSV" ] || die "--mode=violated needs --from-csv or --wallet-ids"
    [ -f "$FROM_CSV" ] || die "CSV not found: $FROM_CSV"
    wallet_ids=$(extract_wallet_ids_from_csv "$FROM_CSV")
  fi

  if [ -z "$wallet_ids" ]; then
    log "No violated accounts found; writing empty summary."
    cat > "$OUT/dump_summary.json" <<JSON
{
  "mode": "violated",
  "wallet_ids": [],
  "violated_account_count": 0,
  "transaction_count": 0,
  "outputs": []
}
JSON
    return
  fi

  local n; n=$(echo "$wallet_ids" | tr ',' '\n' | wc -l | tr -d ' ')
  log "violated wallets: $n → $wallet_ids"

  # MySQL IN (...) với chuỗi (MySQL tự coerce int).
  local in_clause
  in_clause="'${wallet_ids//,/\',\'}'"

  # 1. Metadata account (user + wallet + đếm bank).
  mysql -u"$MYSQL_ROOT_USER" -B -e "
    SELECT u.id AS user_id, u.username, u.email, u.full_name,
           w.id AS wallet_id, w.wallet_number, w.balance, w.status
    FROM wallets w JOIN users u ON u.id = w.user_id
    WHERE w.id IN ($in_clause)
    ORDER BY w.id;
  " "$DB_NAME" | tsv_to_csv > "$OUT/violated_accounts.csv"
  log "→ $OUT/violated_accounts.csv"

  # 2. Toàn bộ giao dịch liên quan (đến VÀ đi) — chính là "lịch sử account".
  mysql -u"$MYSQL_ROOT_USER" -B -e "
    SELECT * FROM transactions
    WHERE source_wallet_id IN ($in_clause)
       OR target_wallet_id IN ($in_clause)
    ORDER BY created_at, id;
  " "$DB_NAME" | tsv_to_csv > "$OUT/violated_transactions.csv"
  log "→ $OUT/violated_transactions.csv"

  # 3. Linked banks của các account đó (nếu bảng tồn tại).
  local bank_file=""
  if table_exists linked_bank_accounts; then
    mysql -u"$MYSQL_ROOT_USER" -B -e "
      SELECT * FROM linked_bank_accounts
      WHERE wallet_id IN ($in_clause)
      ORDER BY wallet_id, id;
    " "$DB_NAME" | tsv_to_csv > "$OUT/violated_linked_banks.csv"
    bank_file="violated_linked_banks.csv"
    log "→ $OUT/$bank_file"
  else
    log "warn: table linked_bank_accounts not found; skipping banks dump"
  fi

  local tx_count; tx_count=$(( $(wc -l < "$OUT/violated_transactions.csv") - 1 ))
  [ "$tx_count" -lt 0 ] && tx_count=0

  # Summary JSON.
  local ids_json; ids_json=$(printf '%s' "$wallet_ids" | sed 's/,/, /g')
  cat > "$OUT/dump_summary.json" <<JSON
{
  "mode": "violated",
  "wallet_ids": [${ids_json}],
  "violated_account_count": ${n},
  "transaction_count": ${tx_count},
  "outputs": ["violated_accounts.csv", "violated_transactions.csv"${bank_file:+", \"$bank_file\""}]
}
JSON
}

case "$MODE" in
  all)      dump_all ;;
  violated) dump_violated ;;
  *)        die "unknown mode: $MODE" ;;
esac

log "done: $OUT/dump_summary.json"