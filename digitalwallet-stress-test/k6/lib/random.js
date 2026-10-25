export class Random {
  constructor(seed) {
    // Nếu không truyền seed → sinh từ Date.now(); đảm bảo khác 0.
    this.state = (seed >>> 0) || ((Date.now() & 0xffffffff) >>> 0) || 1;
  }

  next() {
    this.state = (this.state + 0x6d2b79f5) >>> 0;
    let t = this.state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return (t ^ (t >>> 14)) >>> 0;
  }

  float() { return this.next() / 4294967296; }

  /** Số nguyên đều, inclusive. */
  int(min, max) { return Math.floor(this.float() * (max - min + 1)) + min; }

  floatRange(min, max) { return this.float() * (max - min) + min; }

  bernoulli(p) { return this.float() < p; }

  pickOne(arr) {
    if (!arr || arr.length === 0) throw new Error('pickOne: empty array');
    return arr[this.int(0, arr.length - 1)];
  }

  /** Chọn theo trọng số; chuẩn hoá nội bộ. */
  weightedChoice(items, weights) {
    const total = weights.reduce((s, w) => s + w, 0);
    if (!(total > 0)) throw new Error('weightedChoice: total weight must be > 0');
    let r = this.float() * total;
    for (let i = 0; i < items.length; i++) {
      r -= weights[i];
      if (r < 0) return items[i];
    }
    return items[items.length - 1];
  }

  /** Chuẩn cắt: N(mean, stddev), clip về [lo, hi] (Box–Muller). */
  normalClipped(mean, stddev, lo, hi) {
    let u1 = this.float(); if (u1 === 0) u1 = 1e-12;
    let u2 = this.float();
    const z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    return Math.max(lo, Math.min(hi, mean + stddev * z));
  }

  /** Exponential(mean) — dùng để sinh gap giữa các action. */
  exponential(mean) {
    let u = this.float(); if (u === 0) u = 1e-12;
    return -mean * Math.log(u);
  }
}