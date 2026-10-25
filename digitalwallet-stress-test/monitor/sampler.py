"""Sample CPU / RAM / disk / network / JVM / MySQL metrics at fixed interval.

Output CSV columns: timestamp, category, metric, value, unit, labels (JSON)

Usage:
  python sampler.py --interval 2 --out results/sampler.csv
  python sampler.py --interval 2 --duration 1900 --out results/sampler.csv
"""
import argparse
import csv
import json
import os
import subprocess
import sys
import time
import urllib.request
from datetime import datetime, timezone

try:
    import psutil
except ImportError:
    print("ERROR: pip install psutil", file=sys.stderr)
    sys.exit(1)


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec='milliseconds')


def write_row(w, ts, category, metric, value, unit, labels=None):
    w.writerow([ts, category, metric, value, unit, json.dumps(labels or {}, separators=(',', ':'))])


# ── System ──────────────────────────────────────────────────────────────────

def sample_system():
    yield ('cpu', 'percent', psutil.cpu_percent(interval=None), '%', None)
    mem = psutil.virtual_memory()
    yield ('ram', 'used_bytes',      mem.used,      'B', None)
    yield ('ram', 'available_bytes', mem.available, 'B', None)
    yield ('ram', 'percent',         mem.percent,   '%', None)
    try:
        io = psutil.disk_io_counters()
        if io:
            yield ('disk', 'read_bytes',  io.read_bytes,  'B', None)
            yield ('disk', 'write_bytes', io.write_bytes, 'B', None)
    except Exception:
        pass
    try:
        net = psutil.net_io_counters()
        if net:
            yield ('net', 'sent_bytes', net.bytes_sent, 'B', None)
            yield ('net', 'recv_bytes', net.bytes_recv, 'B', None)
    except Exception:
        pass


# ── JVM (Actuator) ──────────────────────────────────────────────────────────

def _get_json(url, timeout=2):
    with urllib.request.urlopen(url, timeout=timeout) as r:
        return json.loads(r.read())


def sample_jvm(base_url):
    try:
        names = {m['name'] for m in _get_json(f'{base_url}/metrics')['names']}
    except Exception:
        return

    watch = [
        'jvm.memory.used', 'jvm.memory.max',
        'jvm.gc.pause', 'jvm.threads.live', 'jvm.classes.loaded',
        'hikaricp.connections.active', 'hikaricp.connections.idle',
        'hikaricp.connections.pending',
        'system.cpu.usage', 'process.cpu.usage',
        'process.uptime',
    ]
    for name in watch:
        if name not in names:
            continue
        try:
            data = _get_json(f'{base_url}/metrics/{name}')
        except Exception:
            continue
        # Rate/gauge style: {measurements: [{statistic, value}], availableTags: [...]}
        if 'measurements' in data and isinstance(data['measurements'], list):
            for m in data['measurements']:
                yield ('jvm', name, m.get('value', 0), '', {'stat': m.get('statistic', '')})
        # Aggregated counter: sum over measurements
        elif isinstance(data.get('measurements', None), list):
            total = sum(float(m.get('value', 0) or 0) for m in data['measurements'])
            if total:
                yield ('jvm', name, total, '', None)


# ── MySQL ───────────────────────────────────────────────────────────────────

def mysql_query(sql, root='root', timeout=3):
    try:
        r = subprocess.run(['mysql', f'-u{root}', '-N', '-B', '-e', sql],
                           capture_output=True, text=True, timeout=timeout)
        return r.stdout.strip() if r.returncode == 0 else None
    except Exception:
        return None


def sample_mysql():
    for name in ('Threads_connected', 'Threads_running', 'Queries',
                 'Innodb_row_lock_waits', 'Innodb_row_lock_time'):
        v = mysql_query(f"SHOW GLOBAL STATUS LIKE '{name}'")
        if not v:
            continue
        parts = v.split('\t')
        if len(parts) >= 2 and parts[1].isdigit():
            yield ('mysql', name, int(parts[1]), '', None)


# ── main ────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--interval', type=float, default=2.0)
    ap.add_argument('--duration', type=float, default=None,
                    help='stop after N seconds; omit to run until killed')
    ap.add_argument('--out', required=True)
    ap.add_argument('--actuator-url', default='http://localhost:8080/actuator')
    args = ap.parse_args()

    os.makedirs(os.path.dirname(os.path.abspath(args.out)) or '.', exist_ok=True)

    with open(args.out, 'w', newline='') as f:
        w = csv.writer(f)
        w.writerow(['timestamp', 'category', 'metric', 'value', 'unit', 'labels'])
        f.flush()

        psutil.cpu_percent(interval=None)   # khởi tạo để lần đầu không trả 0
        start = time.time()

        while True:
            if args.duration is not None and (time.time() - start) >= args.duration:
                break

            ts = now_iso()
            for src in (sample_system, lambda: sample_jvm(args.actuator_url), sample_mysql):
                try:
                    for cat, met, val, unit, lab in src():
                        write_row(w, ts, cat, met, val, unit, lab)
                except Exception as e:
                    print(f'sampler: {src.__name__ if hasattr(src, "__name__") else "?"}: {e}',
                          file=sys.stderr)
            f.flush()
            time.sleep(args.interval)


if __name__ == '__main__':
    main()