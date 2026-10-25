"""Phân tích k6 CSV → summary JSON + Markdown.

Báo cáo:
  - Tổng request, tổng failure (Rate), failure rate.
  - Số request / endpoint + breakdown theo outcome.
  - Danh sách invariant_violation (wallet_id, client vs server).
  - Time-series 1s (request + failure) → dùng cho visualize.

Usage:
  python reconcile.py --k6-csv results/k6_raw.csv --out-dir results/
"""
import argparse
import csv
import json
import os
import sys
from collections import Counter, defaultdict


RELEVANT = {'request_outcome', 'unexpected_failure', 'invariant_violation'}


def parse_csv(path):
    with open(path, newline='') as f:
        for row in csv.DictReader(f):
            if row.get('metric_name') in RELEVANT:
                yield row


def summarize(rows):
    req_by_name         = Counter()
    outcome_by_name     = defaultdict(Counter)
    per_sec_req         = Counter()
    per_sec_fail        = Counter()
    violations          = []
    fail_samples        = 0
    fail_positive       = 0

    for row in rows:
        name = row.get('metric_name')
        ts   = row.get('timestamp', '')
        sec  = ts[:19]                      # YYYY-MM-DDTHH:MM:SS

        if name == 'request_outcome':
            req     = row.get('request', '')
            outcome = row.get('outcome', '')
            req_by_name[req] += 1
            outcome_by_name[req][outcome] += 1
            per_sec_req[sec] += 1

        elif name == 'unexpected_failure':
            try: val = float(row.get('metric_value', '0') or 0)
            except ValueError: val = 0
            fail_samples += 1
            if val > 0:
                fail_positive += 1
                per_sec_fail[sec] += 1

        elif name == 'invariant_violation':
            violations.append({
                'account':   row.get('account', ''),
                'username':  row.get('username', ''),
                'wallet_id': row.get('wallet_id', ''),
                'field':     row.get('field', ''),
                'client':    row.get('client', ''),
                'server':    row.get('server', ''),
                'timestamp': ts,
            })

    return {
        'total_requests':            sum(req_by_name.values()),
        'requests_per_endpoint':     dict(req_by_name),
        'outcomes_per_endpoint':     {k: dict(v) for k, v in outcome_by_name.items()},
        'total_failures':            fail_positive,
        'failure_rate':              (fail_positive / fail_samples) if fail_samples else 0.0,
        'invariant_violation_count': len(violations),
        'invariant_violations':      violations,
        'per_second_requests':       dict(per_sec_req),
        'per_second_failures':       dict(per_sec_fail),
    }


def write_report(s, out_dir):
    with open(os.path.join(out_dir, 'reconcile_summary.json'), 'w') as f:
        json.dump(s, f, indent=2, ensure_ascii=False)

    lines = ['# Test summary', '']
    lines.append(f"- Total requests: **{s['total_requests']}**")
    lines.append(f"- Failures (Rate unexpected_failure): **{s['total_failures']}**")
    lines.append(f"- Failure rate: **{s['failure_rate']*100:.2f}%**")
    lines.append(f"- Invariant violations: **{s['invariant_violation_count']}**")
    lines.append('')
    lines.append('## Requests per endpoint')
    lines.append('')
    lines.append('| Endpoint | Requests | Success | Failure | Other |')
    lines.append('|---|---:|---:|---:|---:|')
    for req, cnt in sorted(s['requests_per_endpoint'].items(), key=lambda x: -x[1]):
        oc = s['outcomes_per_endpoint'].get(req, {})
        other = sum(v for k, v in oc.items() if k not in ('success', 'unexpected_failure'))
        lines.append(f"| {req} | {cnt} | {oc.get('success', 0)} | {oc.get('unexpected_failure', 0)} | {other} |")
    lines.append('')

    if s['invariant_violation_count'] > 0:
        lines.append('## Invariant violations')
        lines.append('')
        lines.append('| Account | Username | Wallet ID | Client | Server |')
        lines.append('|---|---|---|---:|---:|')
        for v in s['invariant_violations'][:100]:
            lines.append(f"| {v['account']} | {v['username']} | {v['wallet_id']} | "
                         f"{v['client']} | {v['server']} |")
        if s['invariant_violation_count'] > 100:
            lines.append(f"| ... | ({s['invariant_violation_count']-100} more) | | | |")

    with open(os.path.join(out_dir, 'reconcile_summary.md'), 'w') as f:
        f.write('\n'.join(lines))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--k6-csv', required=True)
    ap.add_argument('--out-dir', required=True)
    args = ap.parse_args()

    if not os.path.exists(args.k6_csv):
        print(f'ERROR: k6 CSV not found: {args.k6_csv}', file=sys.stderr)
        sys.exit(1)

    os.makedirs(args.out_dir, exist_ok=True)
    summary = summarize(parse_csv(args.k6_csv))
    write_report(summary, args.out_dir)
    print(f"[reconcile] {summary['total_requests']} req, "
          f"{summary['total_failures']} fail, "
          f"{summary['invariant_violation_count']} violations "
          f"→ {args.out_dir}/reconcile_summary.{{json,md}}")


if __name__ == '__main__':
    main()