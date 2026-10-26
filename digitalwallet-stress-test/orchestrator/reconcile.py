#!/usr/bin/env python3
"""Parse k6_raw.csv → request_log.csv (cho vẽ) + summary.json (tổng hợp).

request_log.csv cột:
  time_sec, state, request, expected, actual, label, detail
  - time_sec = giây kể từ request đầu tiên
  - label    = success | failure | violation
  - detail   = '' (success) | body/error (failure) | 'client=X server=Y' (violation)
"""
import argparse
import csv
import json
import os
import sys
import urllib.parse
from collections import Counter

RELEVANT_METRICS = {'request_outcome', 'invariant_violation'}
LOG_COLUMNS = ['time_sec', 'state', 'request', 'expected', 'actual', 'label', 'detail']


def parse_extra_tags(s):
    if not s:
        return {}
    out = {}
    for pair in s.split('&'):
        if '=' not in pair:
            continue
        k, v = pair.split('=', 1)
        out[k] = v
    return out


def load_k6_raw(path):
    """Trả về list record đã parse; time_sec gán sau khi biết t0."""
    rows = []
    with open(path, newline='') as f:
        for row in csv.DictReader(f):
            if row.get('metric_name') not in RELEVANT_METRICS:
                continue
            extra = parse_extra_tags(row.get('extra_tags', ''))
            try:
                ts = int(float(row['timestamp']))
            except (KeyError, ValueError):
                continue

            detail_enc = extra.get('detail', '')
            detail = urllib.parse.unquote(detail_enc) if detail_enc else ''

            rows.append({
                'ts':       ts,
                'state':    extra.get('state', ''),
                'request':  extra.get('request', ''),
                'expected': extra.get('expected', ''),
                'actual':   extra.get('actual', ''),
                'label':    extra.get('label', ''),
                'detail':   detail,
            })
    return rows


def write_request_log(rows, out_path):
    if not rows:
        with open(out_path, 'w', newline='') as f:
            csv.writer(f).writerow(LOG_COLUMNS)
        return
    t0 = min(r['ts'] for r in rows)
    with open(out_path, 'w', newline='') as f:
        w = csv.DictWriter(f, fieldnames=LOG_COLUMNS)
        w.writeheader()
        for r in rows:
            w.writerow({
                'time_sec': f"{r['ts'] - t0:.3f}",
                'state':    r['state'],
                'request':  r['request'],
                'expected': r['expected'],
                'actual':   r['actual'],
                'label':    r['label'],
                'detail':   r['detail'],
            })


def summarize(rows):
    labels = Counter(r['label'] for r in rows)
    by_request = {}
    for r in rows:
        req = r['request'] or '(unknown)'
        d = by_request.setdefault(req, Counter())
        d[r['label']] += 1
    total = len(rows)
    return {
        'total_requests': total,
        'success':        labels.get('success', 0),
        'failure':        labels.get('failure', 0),
        'violation':      labels.get('violation', 0),
        'success_rate':   (labels.get('success', 0) / total) if total else 0.0,
        'failure_rate':   (labels.get('failure', 0) / total) if total else 0.0,
        'violation_rate': (labels.get('violation', 0) / total) if total else 0.0,
        'by_request':     {k: dict(v) for k, v in by_request.items()},
        'failures':       [r for r in rows if r['label'] == 'failure'][:50],   # mẫu 50 đầu
        'violations':     [r for r in rows if r['label'] == 'violation'],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--k6-csv', required=True)
    ap.add_argument('--out-dir', required=True)
    args = ap.parse_args()

    if not os.path.exists(args.k6_csv):
        print(f'ERROR: {args.k6_csv} not found', file=sys.stderr)
        sys.exit(1)

    os.makedirs(args.out_dir, exist_ok=True)
    rows = load_k6_raw(args.k6_csv)

    write_request_log(rows, os.path.join(args.out_dir, 'request_log.csv'))
    summary = summarize(rows)

    # Gộp dump summary nếu có (script dump ghi ra dump_summary.json).
    dump_path = os.path.join(args.out_dir, 'dump_summary.json')
    if os.path.exists(dump_path):
        try:
            with open(dump_path) as f:
                summary['db_dump'] = json.load(f)
        except Exception:
            pass

    with open(os.path.join(args.out_dir, 'summary.json'), 'w') as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)

    print(f"[reconcile] {summary['total_requests']} log entries → request_log.csv")
    print(f"[reconcile] success={summary['success']}  failure={summary['failure']}  violation={summary['violation']}")


if __name__ == '__main__':
    main()