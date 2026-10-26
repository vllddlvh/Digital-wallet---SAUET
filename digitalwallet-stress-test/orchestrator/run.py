import argparse
import os
import signal
import subprocess
import sys
import time
from datetime import datetime

ROOT    = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
K6_DIR  = os.path.join(ROOT, 'k6')
MONITOR = os.path.join(ROOT, 'monitor')


def log(msg): print(f'[orchestrator] {msg}', flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--profile', required=True, choices=['smoke', 'load', 'stress'])
    ap.add_argument('--out', default=None)
    ap.add_argument('--base-url', default='http://localhost:8080')
    ap.add_argument('--no-sampler', action='store_true')
    args = ap.parse_args()

    ts = datetime.now().strftime('%Y-%m-%dT%H-%M-%S')
    out = args.out or os.path.join(ROOT, 'results', f'{args.profile}_{ts}')
    os.makedirs(out, exist_ok=True)
    log(f'out = {out}')

    # ── Sampler (background, log ra file để chẩn đoán) ───────────────────
    sampler = None
    sampler_csv = os.path.join(out, 'sampler.csv')
    sampler_log = os.path.join(out, 'sampler.log')
    sampler_script = os.path.join(MONITOR, 'sampler.py')

    if not args.no_sampler:
        if not os.path.isfile(sampler_script):
            log(f'WARN: không tìm thấy sampler script: {sampler_script}')
        else:
            try:
                log_f = open(sampler_log, 'w')
                sampler = subprocess.Popen(
                    [sys.executable, sampler_script,
                     '--interval', '2', '--out', sampler_csv],
                    stdout=log_f, stderr=log_f,
                )
                log(f'sampler pid={sampler.pid} → {sampler_csv} (log: {sampler_log})')

                # Kiểm tra chết non (import lỗi, sai arg...)
                time.sleep(0.5)
                if sampler.poll() is not None:
                    log(f'WARN: sampler chết ngay (exit={sampler.returncode})')
                    if os.path.exists(sampler_log):
                        with open(sampler_log) as f:
                            err = f.read().strip()
                        if err:
                            log(f'WARN: sampler error:\n{err[:1000]}')
                    sampler = None
            except Exception as e:
                log(f'WARN: sampler không start được ({e}); tiếp tục không sampler')
                sampler = None

    # ── k6 (blocking) ───────────────────────────────────────────────────
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
        log(f'k6 exit = {k6_exit} (0 = ok, 99 = threshold abort)')
    finally:
        if sampler is not None:
            try:
                sampler.send_signal(signal.SIGTERM)
                sampler.wait(timeout=10)
            except subprocess.TimeoutExpired:
                sampler.kill()
            # Báo cáo kết quả sampler
            if os.path.exists(sampler_csv):
                size = os.path.getsize(sampler_csv)
                log(f'sampler.csv = {size} bytes')
                if size < 100:
                    log(f'WARN: sampler.csv quá nhỏ ({size} bytes); xem {sampler_log}')
            else:
                log(f'WARN: sampler.csv KHÔNG được tạo; xem {sampler_log}')

    if k6_exit == 0:
        return 0
    if k6_exit == 99:
        log('k6 aborted via stopCondition threshold (expected ở stress)')
        return 2
    log(f'WARN: k6 exit={k6_exit} (bất thường)')
    return 1


if __name__ == '__main__':
    sys.exit(main())