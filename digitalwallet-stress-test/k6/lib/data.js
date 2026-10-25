export const BANKS = [
  { code: 'VCB',  name: 'Vietcombank' },
  { code: 'TCB',  name: 'Techcombank' },
  { code: 'ACB',  name: 'Asia Commercial Bank' },
  { code: 'BIDV', name: 'Bank for Investment and Development' },
  { code: 'VTB',  name: 'VietinBank' },
  { code: 'MB',   name: 'Military Bank' },
  { code: 'VPB',  name: 'VPBank' },
  { code: 'STB',  name: 'Sacombank' },
];

export function generateRunId() {
  // Base36 timestamp + 4 ký tự ngẫu nhiên để tránh trùng khi hai test chạy nhanh.
  const t = Date.now().toString(36);
  const r = Math.floor(Math.random() * 0x10000).toString(36).padStart(4, '0');
  return `${t}${r}`;
}

export class DataFactory {
  constructor(runId, rng) {
    this.runId = runId;
    this.rng = rng;
    this._seq = 0;
  }

  nextSeq() { return ++this._seq; }
  username(seq) { return `user_${this.runId}_${seq}`; }
  email(seq) { return `user_${this.runId}_${seq}@test.local`; }
  password() { return 'Passw0rd!'; }
  fullName(seq) { return `User ${this.runId} ${seq}`; }

  /** K ~ uniform[kMin, kMax] cho một account (cấp 1 lần, không reset). */
  drawK(kMin, kMax) { return this.rng.int(kMin, kMax); }

  /** Info bank cho lần LinkBank thứ `kIndex` của account `seq`. */
  bankInfo(seq, kIndex) {
    const bank = this.rng.pickOne(BANKS);
    return {
      bankCode: bank.code,
      bankName: bank.name,
      accountNumber: `acc_${this.runId}_${seq}_${kIndex}`,
      holderName: `Holder ${seq}`,
    };
  }
}