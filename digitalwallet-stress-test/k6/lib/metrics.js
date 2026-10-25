// 3 metric phát ra:
//   - request_outcome       (Counter) : mỗi request 1 sample
//   - invariant_violation   (Counter) : mỗi vi phạm số dư 1 sample
//   - unexpected_failure    (Rate)    : mỗi request 1 sample; threshold abort
//
// Không tag `account` trên request_outcome (cardinality thấp, giữ CSV gọn).

import { Counter, Rate } from 'k6/metrics';

export class Monitor {
  constructor(cfg) {
    this.requestOutcome = new Counter('request_outcome');
    this.invariantViolation = new Counter('invariant_violation');

    // abortOnFail hardcode true — không có trường hợp hợp lý đặt false.
    const sc = cfg.profile.stopConditions;
    const rateOpts = sc ? {
      thresholds: {
        [sc.threshold]: {
          abortOnFail: true,
          delayAbortEval: sc.delayAbortEval,
        },
      },
    } : {};
    this.unexpectedFailure = new Rate('unexpected_failure', rateOpts);
  }

  classify(expected, actual, error) {
    if (error) return 'network_error';
    if (actual === null || actual === undefined) return 'network_error';
    if (expected.indexOf(actual) >= 0) return 'success';
    if (actual >= 500 && actual < 600) return 'unexpected_failure';
    return 'unexpected_status';
  }

  recordRequest({ request, expected, actual, error }) {
    const outcome = this.classify(expected, actual, error);
    this.requestOutcome.add(1, {
      request,
      expected: expected.join('|'),
      actual: (actual === null || actual === undefined) ? 'error' : String(actual),
      outcome,
    });
    this.unexpectedFailure.add(outcome === 'unexpected_failure' ? 1 : 0);
    return outcome;
  }

  recordInvariantViolation({ accountSeq, username, walletId, field, client, server }) {
    this.invariantViolation.add(1, {
      account: String(accountSeq),
      username,
      wallet_id: String(walletId),
      field,
      client: String(client),
      server: String(server),
    });
  }
}