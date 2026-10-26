# Digital Wallet - Stress test

## 1. [Quy trình thực hiện](../digitalwallet-docs/stress-test/Planning.md)

## 2. Hướng dẫn sử dụng

Configuration: [config](./config)

Việc chạy test có thể được thực hiện với [notebook này](./notebooks/DigitalWallet_StressTest.ipynb). Trong đó sẽ hỗ trợ clone repo từ Github hoặc lấy mã nguồn trực tiếp từ Dataset.

**Lưu ý:** Notebook trên được thiết kế chỉ để chạy trên Kaggle (Ubuntu). Việc thực hiện trên máy chính chủ có thể sinh file rác và tiến trình chạy ngầm không đảm bảo.

Tiến trình cơ bản sẽ như sau:
    1. Clone mã nguồn về. Yêu cầu có đủ 2 folder [SUT](../digitalwallet-system) và [Test](../digitalwallet-stress-test/).
    2. Thực hiện chạy các `bash` tại thư mục [scripts](./scripts) lần lượt: tải dependencies (JDK, mysql,...), build SUT và khởi tạo DB.
    3. Cuối cùng, khởi chạy SUT và thực hiện stress test.

Version note:
    1. Phiên bản hiện tại đang chạy SUT và Test trên cùng một instance (cùng 1 máy), nên có thể xảy ra hiện tượng phần mềm Test chặn tài nguyên của SUT. Việc này thường không dẫn tới kịch bản DDOS, mà sẽ dẫn tới cả 2 hệ thống hoạt động cầm chừng, không theo kịch bản.

## 3. Folder structure

Hệ thống mock test được tổ chức như sau:

```plain
SAUETDigitalWallet/
├── digitalwallet-docs/                   # API specs
│
├── digitalwallet-system/                 # SUT, giữ nguyên
│
└── digitalwallet-stress-test/
    ├── README.md             
    ├── config/
    │   ├── mdp.json                      # mô hình MDP mô phỏng hành vi người dùng
    │   └── profiles/                     # số account, μ, think time, VU, thời lượng (mục 4)
    │       ├── smoke.json                
    │       ├── load.json
    │       └── stress.json
    ├── k6/
    │   ├── main.js                       # scenario, setup(), vòng lặp session
    │   └── lib/
    │       ├── config.js                 # nạp và kiểm tra profile
    │       ├── api.js                    # tạo request tới SUT
    │       ├── account.js                # quản lý trạng thái user phía client
    │       ├── mdp.js                    # mô phỏng hành vi người dùng (MDP)
    │       ├── data.js                   # sinh request body
    │       ├── random.js                 # utils
    │       └── metrics.js                # định nghĩa monitor phía client (response, timeout,...)
    │
    ├── scripts/                          # các bash để deploy application
    │   ├── 00_install.sh                 # JDK 21, MySQL, k6 
    │   ├── 10_mysql.sh
    │   ├── 20_build_sut.sh
    │   ├── 30_start_app.sh
    │   └── 90_reset_db.sh
    │
    ├── monitor/
    │   ├── sampler.py                    # định nghĩa monitor phía server (cpu, ram,...)
    │   └── requirements.txt
    │
    ├── orchestrator/
    │   ├── run.py                        # reset DB → app → sampler → k6 → đối soát → gom kết quả
    │   ├── reconcile.py
    │   └── visualize.py
    │
    ├── notebooks/
    │
    └── results/                          # thư mục chứa kết quả (log trước khi visualize)
```
