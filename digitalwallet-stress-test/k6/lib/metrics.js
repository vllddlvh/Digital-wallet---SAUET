// 2 metric phát ra:
//   - request_outcome       (Counter) : mỗi request 1 sample; tags gọn, có label + state + detail
//   - invariant_violation   (Counter) : mỗi vi phạm số dư 1 sample
//   - unexpected_failure    (Rate)    : dùng cho threshold abort
//
// `label` ∈ {success, failure} ở request_outcome; ở invariant_violation label='violation'.
// `detail` URL-encoded để an toàn với '&' khi reconcile parse extra_tags.

import { Counter, Rate } from 'k6/metrics';

export class Monitor {
  constructor(cfg) {
    this.requestOutcome     = new Counter('request_outcome');
    this.invariantViolation = new Counter('invariant_violation');

    const sc = cfg.profile.stopConditions;
    const rateOpts = sc ? {
      thresholds: {
        [sc.threshold]: { abortOnFail: true, delayAbortEval: sc.delayAbortEval },
      },
    } : {};
    this.unexpectedFailure = new Rate('unexpected_failure', rateOpts);
  }

  classify(expected, actual, error) {
    if (error) return 'failure';
    if (actual === null || actual === undefined) return 'failure';
    return expected.indexOf(actual) >= 0 ? 'success' : 'failure';
  }

  recordRequest({ request, state, expected, actual, error, detail }) {
    const label = this.classify(expected, actual, error);
    const encDetail = detail ? encodeURIComponent(detail.slice(0, 300)) : '';
    this.requestOutcome.add(1, {
      request,
      state:    state || '',
      expected: expected.join('|'),
      actual:   (actual === null || actual === undefined) ? 'error' : String(actual),
      label,
      detail:   encDetail,
    });
    this.unexpectedFailure.add(label === 'failure' ? 1 : 0);
    return label;
  }

  recordInvariantViolation({ accountSeq, username, walletId, field, client, server }) {
    const detail = encodeURIComponent(`client=${client} server=${server}`);
    this.invariantViolation.add(1, {
      account:   String(accountSeq),
      username,
      wallet_id: String(walletId),
      field,
      state:     'confirmation',
      request:   'balance_check',
      expected:  'match',
      actual:    'mismatch',
      label:     'violation',
      detail,
    });
  }
}