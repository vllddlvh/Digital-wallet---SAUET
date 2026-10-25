#!/usr/bin/env python3
"""Vẽ 8 biểu đồ kết quả từ request_log.csv (+ sampler.csv nếu có).

X-axis của mọi biểu đồ kết quả bắt đầu từ 0 (giây kể từ request đầu tiên).
Series rỗng / toàn 0 → vẫn vẽ đường y=0.
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

LABELS = ['success', 'failure', 'violation']
COLORS = {'success': 'tab:green', 'failure': 'tab:red', 'violation': 'tab:orange'}
WINDOW = 30   # giây


def load_log(path):
    df = pd.read_csv(path)
    if df.empty:
        return df
    df['time_sec'] = df['time_sec'].astype(float)
    df['sec_bin']  = df['time_sec'].astype(int)
    return df


def rolling_counts(df, max_sec):
    """Trả về DataFrame index=bins(0..max), cột=label, giá trị=số request trong [t-30, t]."""
    bins = list(range(0, max_sec + 1))
    if df.empty:
        return pd.DataFrame(0, index=bins, columns=LABELS)

    pivot = (df.groupby(['sec_bin', 'label']).size()
               .unstack(fill_value=0)
               .reindex(bins, fill_value=0)
               .reindex(columns=LABELS, fill_value=0))

    # Rolling trailing 30s: mỗi điểm t chứa [t-29, t] (30 giây liên tiếp)
    return pivot.rolling(window=WINDOW, min_periods=1).sum()


def plot_counts(rolling, plots_dir):
    for label in LABELS:
        fig, ax = plt.subplots(figsize=(10, 3))
        ax.plot(rolling.index, rolling[label], color=COLORS[label])
        ax.set_ylim(bottom=0)
        ax.set_xlabel('Time since first request (s)')
        ax.set_ylabel(f'{label} count (30s window)')
        ax.set_title(f'{label.capitalize()} count (rolling 30s)')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, f'count_{label}.png')); plt.close(fig)


def plot_ratios(rolling, plots_dir):
    total = rolling.sum(axis=1)
    for label in LABELS:
        ratio = rolling[label].divide(total.where(total > 0, 1)).fillna(0)
        fig, ax = plt.subplots(figsize=(10, 3))
        ax.plot(ratio.index, ratio.values, color=COLORS[label])
        ax.set_ylim(0, 1)
        ax.set_xlabel('Time since first request (s)')
        ax.set_ylabel(f'{label} ratio')
        ax.set_title(f'{label.capitalize()} ratio (rolling 30s)')
        fig.tight_layout(); fig.savefig(os.path.join(plots_dir, f'ratio_{label}.png')); plt.close(fig)


def plot_combined(rolling, plots_dir):
    fig, ax = plt.subplots(figsize=(10, 4))
    for label in LABELS:
        ax.plot(rolling.index, rolling[label], label=label, color=COLORS[label])
    ax.set_ylim(bottom=0)
    ax.set_xlabel('Time since first request (s)')
    ax.set_ylabel('count (30s window)')
    ax.set_title('Success / Failure / Violation per 30s')
    ax.legend()
    fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'combined_count.png')); plt.close(fig)


def plot_pie(df, plots_dir):
    counts = df['label'].value_counts().reindex(LABELS, fill_value=0)
    fig, ax = plt.subplots(figsize=(6, 6))
    if counts.sum() == 0:
        ax.text(0.5, 0.5, 'No requests', ha='center', va='center')
        ax.axis('off')
    else:
        ax.pie(counts.values, labels=[f'{l}: {c}' for l, c in zip(LABELS, counts.values)],
               colors=[COLORS[l] for l in LABELS], autopct='%1.1f%%', startangle=90)
        ax.set_title('Overall result distribution')
    fig.tight_layout(); fig.savefig(os.path.join(plots_dir, 'pie_overall.png')); plt.close(fig)


def plot_sampler(path, plots_dir):
    if not path or not os.path.exists(path):
        print(f'[visualize] WARNING: sampler CSV not found: {path}')
        return
    try:
        df = pd.read_csv(path)
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True, errors='coerce').dt.tz_localize(None)
    except Exception as e:
        print(f'[visualize] ERROR reading sampler CSV: {e}')
        return

    if df.empty:
        print(f'[visualize] WARNING: sampler CSV is empty: {path}')
        return

    t_min = df['timestamp'].min()
    t_max = df['timestamp'].max()

    def line(category, metric, title, ylabel, fname, color=None):
        sub = df[(df['category'] == category) & (df['metric'] == metric)]
        fig, ax = plt.subplots(figsize=(10, 3))
        
        if len(sub):
            ax.plot(sub['timestamp'], sub['value'], color=color or 'tab:blue')
        else:
            # Nếu không có dữ liệu, vẫn vẽ đường y=0 để biểu đồ không bị trống
            ax.plot([t_min, t_max], [0, 0], color=color or 'tab:blue', linestyle='--')
            ax.text(0.5, 0.5, 'No data', transform=ax.transAxes, ha='center', va='center')
            
        ax.set_ylim(bottom=0)
        ax.set_title(title)
        ax.set_ylabel(ylabel)
        ax.set_xlabel('time')
        fig.tight_layout()
        fig.savefig(os.path.join(plots_dir, fname))
        plt.close(fig)

    # Vẽ các metric quan trọng
    line('cpu', 'percent', 'CPU Usage %', '%', 'system_cpu.png', 'tab:orange')
    line('ram', 'percent', 'RAM Usage %', '%', 'system_ram.png', 'tab:blue')
    line('jvm', 'jvm.memory.used', 'JVM Memory Used', 'bytes', 'system_jvm_memory.png')
    line('mysql', 'Threads_running', 'MySQL Threads Running', 'threads', 'system_mysql_threads.png')
    
    print(f'[visualize] System charts → {plots_dir}')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--log-csv', required=True, help='request_log.csv')
    ap.add_argument('--sampler-csv', default=None)
    ap.add_argument('--out-dir', required=True)
    args = ap.parse_args()

    if not os.path.exists(args.log_csv):
        print(f'ERROR: {args.log_csv} not found', file=sys.stderr)
        sys.exit(1)

    plots_dir = os.path.join(args.out_dir, 'plots')
    os.makedirs(plots_dir, exist_ok=True)

    df = load_log(args.log_csv)
    max_sec = int(df['time_sec'].max()) if not df.empty else 0
    rolling = rolling_counts(df, max_sec)

    plot_counts(rolling, plots_dir)
    plot_ratios(rolling, plots_dir)
    plot_combined(rolling, plots_dir)
    plot_pie(df, plots_dir)
    plot_sampler(args.sampler_csv, plots_dir)

    print(f'[visualize] charts → {plots_dir}')


if __name__ == '__main__':
    main()