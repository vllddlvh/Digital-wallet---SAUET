const PROFILES = {
  smoke:  '../../config/profiles/smoke.json',
  load:   '../../config/profiles/load.json',
  stress: '../../config/profiles/stress.json',
};

const loadJson = (path) => JSON.parse(open(path));
const EPS = 1e-6;
const near = (a, b) => Math.abs(a - b) <= EPS;
const dataKeys = (obj) => Object.keys(obj).filter((k) => !k.startsWith('_'));

export function loadConfig(profileName) {
  const mdpRaw = loadJson('../../config/mdp.json');
  const path = PROFILES[profileName];
  if (!path) throw new Error(`Unknown profile: "${profileName}" (smoke|load|stress)`);
  return new Config(mdpRaw, loadJson(path));
}

// ─────────────────────────────────────────────────────────────────────────────
// MDP config
// ─────────────────────────────────────────────────────────────────────────────

export class MdpConfig {
  constructor(raw) {
    this.zones = raw.zones;
    this.transitions = raw.transitions;
    this.linkedBanks = raw.linkedBanks;
    this.money = raw.money;
    this.tokenTimeoutJitter = raw.tokenTimeoutJitterPercent;
    this.insufficientProb = raw.insufficientBalanceProbability;
    this._validate();
  }

  _validate() {
    for (const z of dataKeys(this.zones)) {
      const actions = this.zones[z].actions ?? {};
      const sum = dataKeys(actions).reduce((s, k) => s + actions[k], 0);
      if (!near(sum, 1)) throw new Error(`[mdp] zone "${z}" weights sum ${sum} != 1`);
    }
    const t = this.transitions;
    if (!near(t.authToConfirmation, 1)) throw new Error('[mdp] authToConfirmation must be 1');
    if (!near(t.confirmationToTransaction, 1)) throw new Error('[mdp] confirmationToTransaction must be 1');
    if (!near(t.transactionToTransaction + t.transactionToConfirmation, 1)) {
      throw new Error('[mdp] transaction transitions must sum to 1');
    }
    if (!(this.linkedBanks.kMin >= 1 && this.linkedBanks.kMin <= this.linkedBanks.kMax)) {
      throw new Error('[mdp] linkedBanks: 1 <= kMin <= kMax required');
    }
    if (!(this.money.minAmount >= 1 && this.money.minAmount <= this.money.maxAmount)) {
      throw new Error('[mdp] money: 1 <= minAmount <= maxAmount required');
    }
    if (!(this.money.overflowExtra >= 1)) throw new Error('[mdp] money.overflowExtra must be >= 1');
    if (!(this.insufficientProb >= 0 && this.insufficientProb <= 1)) {
      throw new Error('[mdp] insufficientBalanceProbability must be in [0,1]');
    }
    if (!(this.tokenTimeoutJitter >= 0 && this.tokenTimeoutJitter <= 1)) {
      throw new Error('[mdp] tokenTimeoutJitterPercent must be in [0,1]');
    }
  }

  weightsFor(zoneName) {
    const actions = this.zones[zoneName]?.actions ?? {};
    const out = {};
    for (const k of dataKeys(actions)) out[k] = actions[k];
    return out;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile config
//
// kind = "flat"   → smoke / load  (accounts, targetRps cố định)
// kind = "staged" → stress        (stages[] tích luỹ)
// ─────────────────────────────────────────────────────────────────────────────

export class ProfileConfig {
  constructor(raw) {
    this.name = raw.name;
    this.receivingWallets = raw.receivingWallets;
    this.tokenTimeout = raw.tokenTimeout;
    this.stopConditions = raw.stopConditions ?? null;

    if (Array.isArray(raw.stages)) {
      this.kind = 'staged';
      this.stages = raw.stages.map((s) => ({
        accounts: s.accounts,
        targetRps: s.targetRps,
        activationDurationSec: s.activationDurationSec,
        holdDurationSec: s.holdDurationSec,
      }));
      this.peakAccounts = Math.max(...this.stages.map((s) => s.accounts));
      this.totalDurationSec = this.stages.reduce(
        (s, st) => s + st.activationDurationSec + st.holdDurationSec, 0);
    } else {
      this.kind = 'flat';
      this.accounts = raw.accounts;
      this.targetRps = raw.targetRps;
      this.activationDurationSec = raw.activationDurationSec;
      this.durationSec = raw.durationSec;
      this.peakAccounts = raw.accounts;
      this.totalDurationSec = raw.durationSec;
    }
    this._validate();
  }

  _validate() {
    if (!this.name) throw new Error('[profile] missing name');
    if (!(this.receivingWallets >= 1)) throw new Error('[profile] receivingWallets must be >= 1');
    if (!(this.tokenTimeout?.muSec > 0)) throw new Error('[profile] tokenTimeout.muSec must be > 0');

    if (this.kind === 'flat') {
      if (!(this.accounts >= 1)) throw new Error('[profile] accounts must be >= 1');
      if (!(this.targetRps >= 1)) throw new Error('[profile] targetRps must be >= 1');
      if (!(this.activationDurationSec > 0)) throw new Error('[profile] activationDurationSec must be > 0');
      if (!(this.durationSec > this.activationDurationSec)) {
        throw new Error('[profile] durationSec must be > activationDurationSec');
      }
    } else {
      if (this.stages.length === 0) throw new Error('[profile] stages must not be empty');
      let prevAcc = 0, prevRps = 0;
      for (const [i, st] of this.stages.entries()) {
        if (!(st.accounts > prevAcc)) throw new Error(`[profile] stages[${i}].accounts must strictly increase`);
        if (!(st.targetRps > prevRps)) throw new Error(`[profile] stages[${i}].targetRps must strictly increase`);
        if (!(st.activationDurationSec > 0)) throw new Error(`[profile] stages[${i}].activationDurationSec must be > 0`);
        if (!(st.holdDurationSec >= 0)) throw new Error(`[profile] stages[${i}].holdDurationSec must be >= 0`);
        prevAcc = st.accounts;
        prevRps = st.targetRps;
      }
    }

    if (this.stopConditions) {
      if (typeof this.stopConditions.threshold !== 'string') {
        throw new Error('[profile] stopConditions.threshold must be a string');
      }
      if (typeof this.stopConditions.delayAbortEval !== 'string') {
        throw new Error('[profile] stopConditions.delayAbortEval must be a duration string');
      }
    }
  }

  /** Stages array cho k6 executor `ramping-vus` (giá trị tích luỹ). */
  k6Stages() {
    if (this.kind === 'flat') {
      return [
        { target: this.accounts, duration: `${this.activationDurationSec}s` },
        { target: this.accounts, duration: `${this.durationSec - this.activationDurationSec}s` },
      ];
    }
    const out = [];
    for (const st of this.stages) {
      out.push({ target: st.accounts, duration: `${st.activationDurationSec}s` });
      out.push({ target: st.accounts, duration: `${st.holdDurationSec}s` });
    }
    return out;
  }

  /** Trạng thái tải tại `elapsedSec` kể từ đầu test. */
  stageAt(elapsedSec) {
    if (this.kind === 'flat') {
      return { accounts: this.accounts, targetRps: this.targetRps };
    }
    let t = 0;
    for (const st of this.stages) {
      t += st.activationDurationSec + st.holdDurationSec;
      if (elapsedSec < t) return { accounts: st.accounts, targetRps: st.targetRps };
    }
    const last = this.stages[this.stages.length - 1];
    return { accounts: last.accounts, targetRps: last.targetRps };
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

export class Config {
  constructor(mdpRaw, profileRaw) {
    this.mdp = new MdpConfig(mdpRaw);
    this.profile = new ProfileConfig(profileRaw);
  }
}