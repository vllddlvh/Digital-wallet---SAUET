"""End-to-end test orchestrator.

Steps:
  1. reset_db
  2. build_sut (optional, --skip-build để bỏ qua)
  3. start_app
  4. start_sampler (nền)
  5. run k6
  6. dump DB (all + violated, từ k6 CSV)
  7. reconcile (phân tích CSV)

Usage:
  python -m orchestrator.run --profile=smoke
  python -m orchestrator.run --profile=stress --out=results/stress_2026-10-25
"""
import argparse
import os
import signal
import subprocess
import sys
import time
from datetime import datetime

ROOT      = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRIPTS   = os.path.join(ROOT, 'scripts')
ORCH      = os.path.dirname(os.path.abspath(__file__))
K6_DIR    = os.path.join(ROOT, 'k6')
MONITOR   = os.path.join(ROOT, 'monitor')


def log(msg):
    print(f'[orchestrator] {msg}', flush=True)


def run(cmd, **kw):
    log('$ ' + ' '.join(cmd))
    return subprocess.run(cmd, check=True, **kw)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--profile', required=True, choices=['smoke', 'load', 'stress'])
    ap.add_argument('--out', default=None)
    ap.add_argument('--skip-build', action='store_true')
    ap.add_argument('--skip-dump-all', action='store_true')
    ap.add_argument('--base-url', default='http://localhost:8080')
    args = ap.parse_args()

    ts = datetime.now().strftime('%Y-%m-%dT%H-%M-%S')
    out = args.out or os.path.join(ROOT, 'results', f'{args.profile}_{ts}')
    os.makedirs(out, exist_ok=True)
    log(f'out = {out}')

    # 1. Reset DB (kill app cũ + drop/tạo lại schema)
    run(['bash', os.path.join(SCRIPTS, '90_reset_db.sh')])

    # 2. Build (idempotent, có thể bỏ qua)
    if not args.skip_build:
        run(['bash', os.path.join(SCRIPTS, '20_build_sut.sh')])

    # 3. Start app (Hibernate DDL_AUTO=update tạo schema mới)
    run(['bash', os.path.join(SCRIPTS, '30_start_app.sh')])

    # 4. Sampler (nền)
    sampler_csv = os.path.join(out, 'sampler.csv')
    sampler = subprocess.Popen(
        [sys.executable, os.path.join(MONITOR, 'sampler.py'),
         '--interval', '2', '--out', sampler_csv],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    log(f'sampler pid={sampler.pid} → {sampler_csv}')

    # 5. k6
    k6_csv     = os.path.join(out, 'k6_raw.csv')
    k6_summary = os.path.join(out, 'k6_summary.json')
    env = os.environ.copy()
    env['PROFILE']  = args.profile
    env['BASE_URL'] = args.base_url

    k6_exit = -1
    try:
        proc = subprocess.run(
            ['k6', 'run',
             '--out', f'csv={k6_csv}',
             '--summary-export', k6_summary,
             '--quiet',
             os.path.join(K6_DIR, 'main.js')],
            cwd=K6_DIR, env=env, check=False,
        )
        k6_exit = proc.returncode
        log(f'k6 exit = {k6_exit} (99 = threshold abort)')
    finally:
        sampler.send_signal(signal.SIGTERM)
        try: sampler.wait(timeout=10)
        except subprocess.TimeoutExpired: sampler.kill()

    # 6. Grace + dump DB
    log('grace 5s trước khi dump (in-flight requests)')
    time.sleep(5)

    if not args.skip_dump_all:
        run(['bash', os.path.join(SCRIPTS, '80_dump_db.sh'),
             '--mode=all', f'--out={out}', '--grace=0'])

    run(['bash', os.path.join(SCRIPTS, '80_dump_db.sh'),
         '--mode=violated', f'--from-csv={k6_csv}', f'--out={out}', '--grace=0'])

    # 7. Reconcile (textual + JSON summary)
    run([sys.executable, os.path.join(ORCH, 'reconcile.py'),
         '--k6-csv', k6_csv, '--out-dir', out])

    log(f'done. results in {out}')
    if k6_exit == 99:
        log('WARNING: k6 aborted via threshold (stopCondition triggered)')
        sys.exit(2)


if __name__ == '__main__':
    main()