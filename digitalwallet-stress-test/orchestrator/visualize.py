#!/usr/bin/env python3
"""Vẽ các đồ thị chính từ k6 CSV + sampler CSV.

Output: <out-dir>/plots/*.png

Usage:
  python visualize.py --k6-csv results/k6_raw.csv \
                      --sampler-csv results/sampler.csv \
                      --out-dir results/
"""
import argparse
import os
import sys

try:
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    import pandas as pd
except ImportError:
    print('ERROR: pip install pandas matplotlib', file=sys.stderr)
    sys.exit(1)


def load_k6(path):
    cols = ['metric_name', 'timestamp', 'metric_value',
            'request', 'expected', 'actual', 'outcome', 'account',
            'username', 'wallet_id', 'field', 'client', 'server']
    df = pd.read_csv(path, usecols=lambda c: c in cols)
    df = df[df['metric_name'].isin(['request_outcome', 'unexpected_failure', 'invariant_violation'])].copy()
    df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True, errors='coerce').dt.tz_localize(None)
    return df


def load_sampler(path):
    if not path or not os.path.exists(path):
        return None
    try:
        df = pd.read_csv(path)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True, errors='coerce').dt.tz_localize(None)
        return df
    except Exception:
        return None


def plot_k6(df, plots_dir):
    req  = df[df['metric_name'] == 'request_outcome'].copy()
    fail = df[df['metric_name'] == 'unexpected_failure'].copy()
    viol = df[df['metric_name'] == 'invariant_violation'].copy()
    fail['metric_value'] = pd.to_numeric(fail['metric_value'], errors='coerce').fillna(0)

    # 1. Requests / phút
    if len(req):
        rq = req.set_index('timestamp').resample('1min').size()
        fig, ax = plt.subplots(figsize=(10, 3.5))
        ax.plot(rq.index, rq.values)
        ax.set_title('Requests per minute'); ax.set_ylabel('count'); ax.set_xlabel('time')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'requests_per_min.png')); plt.close(fig)

    # 2. Failures / phút
    if len(fail):
        fpm = fail.set_index('timestamp')['metric_value'].resample('1min').sum()
        fig, ax = plt.subplots(figsize=(10, 3.5))
        ax.plot(fpm.index, fpm.values, color='tab:red')
        ax.set_title('Unexpected failures per minute'); ax.set_ylabel('count'); ax.set_xlabel('time')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'failures_per_min.png')); plt.close(fig)

    # 3. Requests + failures chồng lên nhau
    if len(req):
        rq = req.set_index('timestamp').resample('1min').size()
        fpm = (fail.set_index('timestamp')['metric_value'].resample('1min').sum()
               if len(fail) else pd.Series(0, index=rq.index))
        fig, ax = plt.subplots(figsize=(10, 4))
        ax.bar(rq.index, rq.values, width=pd.Timedelta('45s'), alpha=0.6, label='requests')
        ax2 = ax.twinx()
        ax2.plot(fpm.index, fpm.values, color='tab:red', marker='o', markersize=3, label='failures')
        ax.set_title('Requests and failures per minute')
        ax.set_ylabel('requests'); ax2.set_ylabel('failures'); ax.set_xlabel('time')
        fig.legend(loc='upper right')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'requests_and_failures.png')); plt.close(fig)

    # 4. Outcome breakdown (stacked area)
    if len(req):
        by_out = (req.set_index('timestamp')
                     .groupby([pd.Grouper(freq='1min'), 'outcome']).size().unstack(fill_value=0))
        fig, ax = plt.subplots(figsize=(10, 4))
        by_out.plot(ax=ax, kind='area', stacked=True, alpha=0.8)
        ax.set_title('Requests by outcome per minute'); ax.set_ylabel('count'); ax.set_xlabel('time')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'outcome_breakdown.png')); plt.close(fig)

    # 5. Requests per endpoint
    if len(req):
        cnt = req['request'].value_counts()
        fig, ax = plt.subplots(figsize=(9, max(3, len(cnt)*0.4)))
        cnt.plot(ax=ax, kind='barh')
        ax.invert_yaxis()
        ax.set_title('Requests per endpoint'); ax.set_xlabel('count')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'requests_per_endpoint.png')); plt.close(fig)

    # 6. Invariant violations
    if len(viol):
        fig, ax = plt.subplots(figsize=(8, 4))
        viol['wallet_id'].value_counts().plot(ax=ax, kind='bar')
        ax.set_title(f'Invariant violations by wallet ({len(viol)} events)')
        ax.set_ylabel('count'); ax.set_xlabel('wallet_id')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'invariant_violations.png')); plt.close(fig)


def plot_sampler(df, plots_dir):
    # CPU
    cpu = df[(df['category'] == 'cpu') & (df['metric'] == 'percent')]
    if len(cpu):
        fig, ax = plt.subplots(figsize=(10, 2.8))
        ax.plot(cpu['timestamp'], cpu['value'], color='tab:orange')
        ax.set_title('CPU %'); ax.set_ylabel('%'); ax.set_xlabel('time')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'cpu.png')); plt.close(fig)

    # RAM %
    ram = df[(df['category'] == 'ram') & (df['metric'] == 'percent')]
    if len(ram):
        fig, ax = plt.subplots(figsize=(10, 2.8))
        ax.plot(ram['timestamp'], ram['value'], color='tab:blue')
        ax.set_title('RAM %'); ax.set_ylabel('%'); ax.set_xlabel('time')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'ram.png')); plt.close(fig)

    # JVM heap (jvm.memory.used)
    heap = df[(df['category'] == 'jvm') & (df['metric'] == 'jvm.memory.used')]
    if len(heap):
        fig, ax = plt.subplots(figsize=(10, 3.2))
        for label, g in heap.groupby(heap['labels'].fillna('')):
            ax.plot(g['timestamp'], g['value'], label=label[:40])
        ax.set_title('JVM memory used'); ax.set_ylabel('bytes'); ax.set_xlabel('time')
        ax.legend(fontsize=6, loc='upper left')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'jvm_memory.png')); plt.close(fig)

    # Hikari pool
    hk = df[df['metric'].isin(['hikaricp.connections.active',
                               'hikaricp.connections.idle',
                               'hikaricp.connections.pending'])]
    if len(hk):
        fig, ax = plt.subplots(figsize=(10, 3))
        for m, g in hk.groupby('metric'):
            ax.plot(g['timestamp'], g['value'], label=m)
        ax.set_title('HikariCP pool'); ax.set_ylabel('connections'); ax.set_xlabel('time')
        ax.legend(fontsize=7)
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'hikari.png')); plt.close(fig)

    # MySQL
    my = df[df['category'] == 'mysql']
    if len(my):
        fig, ax = plt.subplots(figsize=(10, 3))
        for m, g in my.groupby('metric'):
            ax.plot(g['timestamp'], g['value'], label=m)
        ax.set_title('MySQL status'); ax.set_ylabel('value'); ax.set_xlabel('time')
        ax.legend(fontsize=7)
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'mysql.png')); plt.close(fig)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--k6-csv', required=True)
    ap.add_argument('--sampler-csv', default=None)
    ap.add_argument('--out-dir', required=True)
    args = ap.parse_args()

    if not os.path.exists(args.k6_csv):
        print(f'ERROR: {args.k6_csv} not found', file=sys.stderr)
        sys.exit(1)

    plots_dir = os.path.join(args.out_dir, 'plots')
    os.makedirs(plots_dir, exist_ok=True)

    k6_df = load_k6(args.k6_csv)
    plot_k6(k6_df, plots_dir)
    print(f'[visualize] k6 plots → {plots_dir}')

    s_df = load_sampler(args.sampler_csv)
    if s_df is not None and len(s_df):
        plot_sampler(s_df, plots_dir)
        print(f'[visualize] sampler plots → {plots_dir}')
    else:
        print(f'[visualize] no sampler CSV; skip system plots')


if __name__ == '__main__':
    main()