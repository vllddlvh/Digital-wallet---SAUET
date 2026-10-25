# Digital Wallet - Stress test

## 1. [Quy trình thực hiện](../digitalwallet-docs/testplan.md)

## 2. Folder structure

```plain
SAUETDigitalWallet/
├── digitalwallet-docs/                   # API specs
│
├── digitalwallet-system/                 # SUT, giữ nguyên
│
└── digitalwallet-stress-test/
    ├── README.md                         # cách chạy local và trên Kaggle
    ├── config/
    │   ├── mdp.json                      # tỉ lệ hành động, chuyển khu vực, số tiền, K (mục 2.2)
    │   └── profiles/
    │       ├── smoke.json                # số account, μ, think time, VU, thời lượng (mục 4)
    │       ├── load.json
    │       └── stress.json
    ├── k6/
    │   ├── main.js                       # scenario, setup(), vòng lặp session
    │   └── lib/
    │       ├── config.js                 # nạp và kiểm tra profile
    │       ├── api.js                    # một hàm cho mỗi endpoint (tag name, Bearer, check)
    │       ├── account.js                # trạng thái account: token, T, knownBalance, bank, cờ mustTopUp
    │       ├── mdp.js                    # 3 khu vực, chọn hành động theo tỉ lệ, chuyển khu vực
    │       ├── data.js                   # sinh username, email, danh sách bank
    │       ├── random.js                 # chuẩn cắt ±30%, uniform, weighted choice
    │       └── metrics.js                # unexpected_failure, counter TopUp/Withdraw
    │
    ├── scripts/                          # các bash để deploy application
    │   ├── 00_install.sh                 # JDK 21, MySQL, k6 (ghim phiên bản)
    │   ├── 10_mysql.sh
    │   ├── 20_build_sut.sh
    │   ├── 30_start_app.sh
    │   └── 90_reset_db.sh
    │
    ├── monitor/
    │   ├── sampler.py                    # psutil + metric JVM + MySQL
    │   └── requirements.txt
    │
    ├── orchestrator/
    │   ├── run.py                        # reset DB → app → sampler → k6 → đối soát → gom kết quả
    │   ├── reconcile.py                  # so SUM(balance) với counter của k6
    │   └── visualize.py
    │
    ├── notebooks/
    │
    └── results/                          # thư mục chứa kết quả (log trước khi visualize)
```
