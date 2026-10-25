
export class MdpEngine {
  constructor(mdpCfg, rng) {
    this.cfg = mdpCfg;
    this.rng = rng;
    this.transitions = mdpCfg.transitions;
    this.confirmationWeights = mdpCfg.weightsFor('confirmation');
    this.transactionWeights = mdpCfg.weightsFor('transaction');
  }

  /** Zone kế tiếp, có xét token hết hạn (force về Auth). */
  nextZone(currentZone, account) {
    if (account.isTokenExpired() && currentZone !== 'auth') return 'auth';
    if (currentZone === 'auth') return account.hasToken() ? 'confirmation' : 'auth';;
    if (currentZone === 'confirmation') return 'transaction';
    if (currentZone === 'transaction') {
      const stay = this.rng.bernoulli(this.transitions.transactionToTransaction);
      return stay ? 'transaction' : 'confirmation';
    }
    return 'auth';
  }

  /** Danh sách action cần thực thi khi vào `zone`. */
  chooseActions(zone, account) {
    if (zone === 'auth') {
      return account.hasToken() ? ['logout', 'login'] : ['login'];
    }
    if (zone === 'confirmation') {
      return [this._pickFromWeights(this.confirmationWeights)];
    }
    if (zone === 'transaction') {
      return [this._chooseTransactionAction(account)];
    }
    throw new Error(`[mdp] unknown zone: "${zone}"`);
  }

  _chooseTransactionAction(account) {
    // (0) mustTopUp (từ 400 overdraw lần trước) → force topup
    if (account.mustTopUp && account.linkedBanks.length > 0) return 'topUp';

    // (1) Weighted choice có ràng buộc
    const w = { ...this.transactionWeights };
    w.linkBank = (w.linkBank || 0) * (account.banksAvailableCount / account.K);
    if (account.linkedBanks.length === 0) {
      w.topUp = 0;
      w.withdraw = 0;
    }
    return this._pickFromWeights(w);
  }

  _pickFromWeights(weights) {
    const items = [], ws = [];
    for (const k of Object.keys(weights)) {
      if (weights[k] > 0) { items.push(k); ws.push(weights[k]); }
    }
    if (items.length === 0) throw new Error('[mdp] no action available');
    return this.rng.weightedChoice(items, ws);
  }
}