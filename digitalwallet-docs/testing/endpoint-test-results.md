# Kết quả kiểm thử các endpoint bổ sung

File này lưu kết quả test chức năng cho các phần còn thiếu của baseline. Mỗi endpoint được bổ sung sẽ có một mục riêng; bảng dưới chỉ phản ánh tiến độ của đợt bổ sung này.

## 1. Tiến độ

| Endpoint | Trạng thái | Kết quả gần nhất |
| --- | --- | --- |
| `POST /api/v1/linked-banks` | Đã chạy, có lỗi cần sửa | 29 ca: 27 PASS, 2 FAIL |
| `GET /api/v1/linked-banks` | Đã chạy | 8 ca: 8 PASS, 0 FAIL |
| `DELETE /api/v1/linked-banks/{id}` | Đã chạy, có lỗi cần sửa | 10 ca: 9 PASS, 1 FAIL |
| `POST /api/v1/transactions/topup` | Đã chạy, có lỗi cần sửa | 12 ca: 11 PASS, 1 FAIL |
| `POST /api/v1/transactions/withdraw` | Đã chạy, có lỗi cần sửa | 11 ca: 10 PASS, 1 FAIL |
| `POST /api/v1/transactions/transfer` | Đã chạy | 11 ca: 11 PASS, 0 FAIL |
| `GET /api/v1/transactions` | Chưa thực hiện trong đợt bổ sung | — |

## 2. Liên kết ngân hàng — POST /api/v1/linked-banks

### Lần chạy và phạm vi

- Thời điểm bắt đầu suite: **28/10/2026 00:21:19 (Asia/Bangkok)**; JUnit ghi `2026-10-27T17:21:19.703Z`.
- Baseline: commit `b8fa6e2`, nhánh `thang`; chạy cùng file test mới trong workspace.
- Môi trường: Windows 11, Java Temurin 21.0.12.1, Gradle Wrapper 9.8.1, Spring Boot 4.0.8, JUnit Jupiter, MockMvc và Mockito.
- Mã test: [LinkBankEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/LinkBankEndpointTest.java).
- Controller, request validation, handler lỗi, `LinkedBankService` và chuỗi xác thực JWT dùng mã thật. `GetWalletUseCase`, `SaveLinkedBankPort` và `LoadLinkedBankPort` được mock.
- Bộ test kiểm tra hợp đồng HTTP và xử lý liên kết ở tầng ứng dụng; không khởi động MySQL, không kiểm chứng ghi dữ liệu thật hoặc tích hợp ngân hàng bên ngoài.
- Chỉ chạy class `LinkBankEndpointTest`, chưa chạy lại toàn bộ bộ test của dự án.

### Kết quả thực tế

| Chỉ số JUnit | Giá trị |
| --- | --- |
| Tổng số ca thực thi | 29 |
| PASS | 27 |
| FAIL do assertion | 2 |
| Error của test runner | 0 |
| Bỏ qua | 0 |
| Thời gian suite | 15,574 giây |
| Kết thúc lệnh Gradle | Exit code 1 vì có 2 ca FAIL |

### Danh sách trường hợp

| ID | Trường hợp | Kỳ vọng | Thực tế | Kết quả |
| --- | --- | --- | --- | --- |
| LB-01 | JWT hợp lệ và body đúng | `201`; response đúng hợp đồng; liên kết gắn với ví của user trong JWT; số tài khoản được che trước khi gửi đến port lưu | `201`; id/ví/ngân hàng/trạng thái/thời gian đúng; lưu một lần; số tài khoản `0123456789` được đổi thành `******6789`, không xuất hiện nguyên bản trong response | PASS |
| LB-02 | Bốn trường cùng đạt độ dài tối đa: 20/100/50/100 ký tự | `201` | `201`, dữ liệu và số tài khoản đã che đúng | PASS |
| LB-03 đến LB-07 | `bankCode`: thiếu trường, null, rỗng, toàn dấu cách, dài 21 ký tự | Cả 5 ca trả `400`, có `errors.bankCode`; không gọi nghiệp vụ | Cả 5 ca trả `400`, đúng cấu trúc lỗi, không gọi nghiệp vụ | PASS (5/5) |
| LB-08 đến LB-12 | `bankName`: thiếu trường, null, rỗng, toàn dấu cách, dài 101 ký tự | Cả 5 ca trả `400`, có `errors.bankName`; không gọi nghiệp vụ | Cả 5 ca trả `400`, đúng cấu trúc lỗi, không gọi nghiệp vụ | PASS (5/5) |
| LB-13 đến LB-17 | `accountNumber`: thiếu trường, null, rỗng, toàn dấu cách, dài 51 ký tự | Cả 5 ca trả `400`, có `errors.accountNumber`; không gọi nghiệp vụ | Cả 5 ca trả `400`, đúng cấu trúc lỗi, không gọi nghiệp vụ | PASS (5/5) |
| LB-18 đến LB-22 | `holderName`: thiếu trường, null, rỗng, toàn dấu cách, dài 101 ký tự | Cả 5 ca trả `400`, có `errors.holderName`; không gọi nghiệp vụ | Cả 5 ca trả `400`, đúng cấu trúc lỗi, không gọi nghiệp vụ | PASS (5/5) |
| LB-23 | Không có JWT | `401`, không gọi nghiệp vụ | `401`, không gọi nghiệp vụ | PASS |
| LB-24 | JWT không hợp lệ | `401`, không gọi nghiệp vụ | `401`, không gọi nghiệp vụ | PASS |
| LB-25 | JWT có chữ ký đúng nhưng đã hết hạn | `401`, không gọi nghiệp vụ | `401`, không gọi nghiệp vụ | PASS |
| LB-26 | User đã xác thực nhưng không có ví (giả lập ở use case) | `404`, không lưu liên kết | `404`, không gọi port lưu | PASS |
| LB-27 | Port lưu phát sinh lỗi bất ngờ được chủ động giả lập | `500` với cấu trúc lỗi chuẩn | `500`, có `status`/`error`/`path` đúng | PASS |
| LB-28 | Có JWT hợp lệ nhưng không gửi body | `400`, không gọi nghiệp vụ | `500`; không gọi nghiệp vụ | **FAIL** |
| LB-29 | Có JWT hợp lệ nhưng JSON sai cú pháp: `{"bankCode": ` | `400`, không gọi nghiệp vụ | `500`; không gọi nghiệp vụ | **FAIL** |

HTTP `500` ở LB-27 là lỗi được giả lập có chủ đích và là kết quả đúng của ca đó. Hai ca FAIL thực sự là LB-28 và LB-29.

### Lỗi phát hiện

| Mã lỗi theo dõi trong báo cáo | Ca test | Hiện tượng | Ảnh hưởng |
| --- | --- | --- | --- |
| LB-BUG-01 | `missingBodyReturns400WithoutCallingBusinessLogic` | Request không có body trả `500` thay vì `400` | Dữ liệu đầu vào thiếu bị báo thành lỗi máy chủ |
| LB-BUG-02 | `malformedJsonReturns400WithoutCallingBusinessLogic` | Request JSON hỏng trả `500` thay vì `400` | Client không nhận được phản hồi lỗi đầu vào phù hợp |

JUnit ghi nhận ở cả hai ca:

```text
java.lang.AssertionError: Status expected:<400> but was:<500>
```

[GlobalExceptionHandler](../../digitalwallet-system/src/main/java/sa/uet/digitalwallet_system/adapter/in/web/handler/GlobalExceptionHandler.java) hiện có handler validation cho `MethodArgumentNotValidException`, nhưng chưa có handler riêng cho `HttpMessageNotReadableException`. Lỗi đọc body bị handler `Exception.class` bắt và chuyển thành `500`. Hướng sửa tiếp theo là ánh xạ lỗi đọc body sang `400 Bad Request` rồi chạy lại bộ test này. Đợt này chỉ bổ sung test và báo cáo; hai assertion vẫn giữ kỳ vọng `400` để phát hiện hồi quy sau khi sửa.

### Chạy lại và bằng chứng

Từ thư mục `digitalwallet-system`, chạy bằng PowerShell:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.LinkBankEndpointTest" --no-daemon --console=plain
```

Báo cáo máy đọc được dùng để tổng hợp số liệu: [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.LinkBankEndpointTest.xml). Có thể xem thêm [báo cáo HTML của Gradle](../../digitalwallet-system/build/reports/tests/test/index.html) và [log lần chạy](../../digitalwallet-system/build/link-bank-test.log).

Các file trong `build/` được sinh khi chạy test và không được Git theo dõi; bảng kết quả và lỗi ở trên được lưu trực tiếp trong Markdown để đọc được sau khi chia sẻ repo.

## 3. Liên kết ngân hàng — GET /api/v1/linked-banks

### Lần chạy và phạm vi

- Thời điểm bắt đầu suite: **28/10/2026 00:43:07 (Asia/Bangkok)**; JUnit ghi `2026-10-27T17:43:07.131Z`.
- Baseline: commit `519e086`, nhánh `main`; chạy với file test mới trong workspace.
- Mã test: [GetLinkedBanksEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/GetLinkedBanksEndpointTest.java).
- Chạy `LinkedBankController`, JWT security, `LinkedBankService`, `LinkedBankPersistenceAdapter` và handler lỗi bằng mã thật. `GetWalletUseCase`, JPA repository và mapper được mock.
- Không khởi động MySQL. Test xác nhận adapter gọi repository theo đúng ID ví và trạng thái `ACTIVE`; chưa kiểm chứng truy vấn SQL hoặc dữ liệu lưu thật.

### Kết quả thực tế

JUnit thực thi **8 ca: 8 PASS, 0 FAIL, 0 error, 0 bỏ qua**; suite mất 19,919 giây. Lệnh Gradle kết thúc với **exit code 0**.

| ID | Trường hợp | Kết quả |
| --- | --- | --- |
| GLB-01 | JWT hợp lệ; trả mảng hai liên kết `ACTIVE` của ví đang đăng nhập, đúng trường phản hồi; số tài khoản được che | PASS — `200`; repository được gọi với ID ví và `ACTIVE`, không ghi dữ liệu |
| GLB-02 | Ví không có liên kết đang hoạt động | PASS — `200`, `data: []` |
| GLB-03 | Người dùng thứ hai có ví khác | PASS — chỉ truy vấn ID ví của người dùng thứ hai |
| GLB-04 | Thiếu JWT | PASS — `401`, không gọi nghiệp vụ |
| GLB-05 | JWT không hợp lệ | PASS — `401`, không gọi nghiệp vụ |
| GLB-06 | JWT hết hạn | PASS — `401`, không gọi nghiệp vụ |
| GLB-07 | Người dùng đã xác thực nhưng không có ví | PASS — `404`, không truy vấn liên kết |
| GLB-08 | Repository phát sinh lỗi giả lập | PASS — `500` theo cấu trúc lỗi hiện tại |

`500` ở GLB-08 là kết quả mong đợi của tình huống lỗi lưu trữ được giả lập, không phải ca test thất bại. Chỉ chạy class GET này; các lỗi đã ghi nhận ở bộ POST chưa được sửa hoặc chạy lại trong lần này.

### Chạy lại và bằng chứng

Từ thư mục `digitalwallet-system`, chạy bằng PowerShell:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.GetLinkedBanksEndpointTest" --no-daemon --console=plain
```

Số liệu lấy từ [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.GetLinkedBanksEndpointTest.xml). File trong `build/` không được Git theo dõi; kết quả được ghi tại đây để giữ lại khi chia sẻ repo.

## 4. Hủy liên kết ngân hàng — DELETE /api/v1/linked-banks/{id}

### Lần chạy và phạm vi

- Thời điểm bắt đầu suite cuối: **28/10/2026 01:00:21 (Asia/Bangkok)**; JUnit ghi `2026-10-27T18:00:21.050Z`.
- Baseline: commit `47085ea`, nhánh `main`; chạy với file test mới trong workspace.
- Mã test: [UnlinkBankEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/UnlinkBankEndpointTest.java).
- Chạy controller, JWT security, `LinkedBankService`, `LinkedBankPersistenceAdapter` và handler lỗi bằng mã thật. `GetWalletUseCase`, JPA repository và mapper được mock.
- Không khởi động MySQL. Test xác nhận service tạo bản ghi `UNLINKED` và adapter gọi repository theo cặp `id`/ID ví; chưa xác nhận cập nhật dữ liệu thật trong database.

### Kết quả thực tế

JUnit thực thi **10 ca: 9 PASS, 1 FAIL, 0 error, 0 bỏ qua**; suite mất 15,723 giây. Lệnh Gradle kết thúc với **exit code 1** do ca FAIL.

| ID | Trường hợp | Kết quả |
| --- | --- | --- |
| ULB-01 | Hủy liên kết `ACTIVE` của ví hiện tại | PASS — `200`, message đúng, không có `data`; lưu trạng thái `UNLINKED` và thời điểm hủy, giữ các trường cũ; không xóa vật lý |
| ULB-02 | Người dùng khác yêu cầu hủy liên kết thuộc ví đầu tiên | PASS — `404`, chỉ tìm theo ID ví của người dùng hiện tại; không lưu |
| ULB-03 | ID liên kết không tồn tại | PASS — `404`, không lưu |
| ULB-04 | Liên kết đã ở trạng thái `UNLINKED` | PASS — `404`, không lưu lần hai |
| ULB-05 | Thiếu JWT | PASS — `401`, không gọi nghiệp vụ |
| ULB-06 | JWT không hợp lệ | PASS — `401`, không gọi nghiệp vụ |
| ULB-07 | JWT hết hạn | PASS — `401`, không gọi nghiệp vụ |
| ULB-08 | Người dùng đã xác thực nhưng không có ví | PASS — `404`, không truy vấn liên kết |
| ULB-09 | ID đường dẫn không phải số: `not-a-number` | **FAIL** — kỳ vọng `400` nhưng nhận `500`; không gọi nghiệp vụ |
| ULB-10 | Repository phát sinh lỗi giả lập | PASS — `500` theo cấu trúc lỗi hiện tại |

`500` ở ULB-10 là kết quả mong đợi của ca giả lập lỗi lưu trữ. **ULB-09 là lỗi thực sự**: tham số `id` được khai báo kiểu `Long`, nhưng lỗi chuyển đổi giá trị đường dẫn không được ánh xạ thành `400 Bad Request`. [GlobalExceptionHandler](../../digitalwallet-system/src/main/java/sa/uet/digitalwallet_system/adapter/in/web/handler/GlobalExceptionHandler.java) chưa có handler riêng cho lỗi chuyển kiểu tham số; handler tổng quát trả `500`. Ca test vẫn giữ kỳ vọng `400` để phát hiện khi lỗi được sửa.

### Chạy lại và bằng chứng

Từ thư mục `digitalwallet-system`, chạy bằng PowerShell:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.UnlinkBankEndpointTest" --no-daemon --console=plain
```

Số liệu lấy từ [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.UnlinkBankEndpointTest.xml). File trong `build/` không được Git theo dõi; kết quả được ghi tại đây để giữ lại khi chia sẻ repo. Chỉ chạy class DELETE này; các ca POST và GET chưa được chạy lại trong lần này.

## 5. Nạp tiền — POST /api/v1/transactions/topup

### Phạm vi và kết quả

- Lần chạy: **28/10/2026 20:05:45 (Asia/Bangkok)**; JUnit ghi `2026-10-28T13:05:45.651Z`. Baseline: commit `b5e9eb5` trên `main`, cùng file test mới trong workspace.
- Mã test: [TopupEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/TopupEndpointTest.java). Controller, JWT security, validation, `TransactionService` và handler lỗi dùng mã thật; các port ví, ngân hàng và giao dịch được mock.
- **12 ca thực thi: 11 PASS, 1 FAIL, 0 error, 0 bỏ qua**; suite mất 5,257 giây. Gradle kết thúc với exit code 1 do ca FAIL.
- Không chạy MySQL hoặc tích hợp ngân hàng thật; test xác nhận lệnh ghi ví và giao dịch được gửi tới port, chưa xác nhận dữ liệu lưu bền vững hay tiền thực được chuyển.

| ID | Trường hợp | Kết quả |
| --- | --- | --- |
| TU-01 | JWT hợp lệ, nạp 250 vào ví có số dư 1000 | PASS — `200`; ví mới có số dư 1250; tạo giao dịch `TOPUP` gắn đúng ví và liên kết ngân hàng |
| TU-02–TU-06 | Thiếu ID ngân hàng, thiếu số tiền, số tiền bằng 0, số tiền âm, mô tả dài 256 ký tự | PASS (5/5) — `400`, không gọi nghiệp vụ |
| TU-07–TU-08 | Thiếu JWT, JWT không hợp lệ | PASS (2/2) — `401`, không gọi nghiệp vụ |
| TU-09 | Người dùng không có ví | PASS — `404`, không ghi ví/giao dịch |
| TU-10 | Ví không hoạt động | PASS — `400`, không ghi ví/giao dịch |
| TU-11 | Liên kết ngân hàng không thuộc ví hiện tại hoặc không tồn tại | PASS — `404`, không ghi ví/giao dịch |
| TU-12 | Liên kết ngân hàng đã `UNLINKED` | **FAIL** — kỳ vọng từ chối `400`, thực tế trả `200` |

**TU-12 là lỗi nghiệp vụ:** `TransactionService.topup` chỉ kiểm tra liên kết ngân hàng có tồn tại theo ID và ví, chưa kiểm tra trạng thái `ACTIVE`. Vì thế liên kết đã hủy vẫn đi qua luồng cộng số dư và ghi giao dịch. Hướng sửa là yêu cầu liên kết ở trạng thái `ACTIVE` trước khi cập nhật ví; giữ assertion của test để xác nhận sau khi sửa. Đây là kiểm tra logic hiện tại, không xác nhận giao dịch với ngân hàng ngoài hệ thống.

### Chạy lại

Từ thư mục `digitalwallet-system`:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.TopupEndpointTest" --no-daemon --console=plain
```

Số liệu lấy từ [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.TopupEndpointTest.xml). File trong `build/` không được Git theo dõi. Chỉ chạy class nạp tiền trong lần này.

## 6. Rút tiền — POST /api/v1/transactions/withdraw

- Lần chạy: **28/10/2026 20:07:12 (Asia/Bangkok)**; JUnit ghi `2026-10-28T13:07:12.604Z`. Baseline: commit `9e4c1d9` trên `main`, cùng file test mới trong workspace.
- Mã test: [WithdrawEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/WithdrawEndpointTest.java). Controller, JWT, validation và `TransactionService` dùng mã thật; các port lưu trữ được mock. Không chạy MySQL hay ngân hàng thật.
- **11 ca: 10 PASS, 1 FAIL, 0 error, 0 bỏ qua**; suite mất 4,174 giây. Gradle exit code 1 do ca FAIL.

| ID | Trường hợp | Kết quả |
| --- | --- | --- |
| WD-01 | Rút 250 từ số dư 1000 | PASS — `200`; số dư gửi tới port lưu là 750, ghi giao dịch `WITHDRAW` |
| WD-02 | Rút đúng toàn bộ số dư | PASS — `200`; số dư mới bằng 0 |
| WD-03 | Rút vượt số dư | PASS — `400`, không ghi ví/giao dịch |
| WD-04–WD-06 | Thiếu ID ngân hàng, số tiền bằng 0, thiếu số tiền | PASS (3/3) — `400`, không gọi nghiệp vụ |
| WD-07–WD-08 | Thiếu JWT, JWT không hợp lệ | PASS (2/2) — `401`, không gọi nghiệp vụ |
| WD-09 | Ví không hoạt động | PASS — `400`, không ghi ví/giao dịch |
| WD-10 | Liên kết không thuộc ví hoặc không tồn tại | PASS — `404`, không ghi ví/giao dịch |
| WD-11 | Liên kết đã `UNLINKED` | **FAIL** — kỳ vọng từ chối `400`, thực tế `200` |

**WD-11 là lỗi nghiệp vụ:** `TransactionService.withdraw` kiểm tra liên kết tồn tại theo ID và ví nhưng không kiểm tra trạng thái `ACTIVE`. Liên kết đã hủy vẫn đi qua luồng trừ số dư và ghi giao dịch. Cần kiểm tra trạng thái trước khi cập nhật ví, rồi chạy lại test giữ kỳ vọng `400`.

Chạy lại từ `digitalwallet-system`:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.WithdrawEndpointTest" --no-daemon --console=plain
```

Số liệu lấy từ [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.WithdrawEndpointTest.xml). File trong `build/` không được Git theo dõi. Chỉ chạy class rút tiền trong lần này.

## 7. Chuyển tiền — POST /api/v1/transactions/transfer

- Lần chạy: **28/10/2026 20:08:33 (Asia/Bangkok)**; JUnit ghi `2026-10-28T13:08:33.845Z`. Baseline: commit `caf45c8` trên `main`, cùng file test mới trong workspace.
- Mã test: [TransferEndpointTest.java](../../digitalwallet-system/src/test/java/sa/uet/digitalwallet_system/adapter/in/web/TransferEndpointTest.java). Controller, JWT, validation và `TransactionService` dùng mã thật; các port lưu trữ được mock.
- **11 ca: 11 PASS, 0 FAIL, 0 error, 0 bỏ qua**; suite mất 4,241 giây; Gradle exit code 0.

| ID | Trường hợp | Kết quả |
| --- | --- | --- |
| TF-01 | Chuyển 250 từ ví có 1000 sang ví có 500 | PASS — `200`; gửi số dư mới 750 cho cả hai ví và ghi giao dịch `TRANSFER` đúng ví nguồn/đích |
| TF-02 | Chuyển cho chính ví nguồn | PASS — `400`, không ghi dữ liệu |
| TF-03 | Không đủ số dư | PASS — `400`, không ghi dữ liệu |
| TF-04–TF-05 | Ví nguồn hoặc ví đích không hoạt động | PASS (2/2) — `400`, không ghi dữ liệu |
| TF-06 | Không tìm thấy ví đích | PASS — `404`, không ghi dữ liệu |
| TF-07–TF-09 | Số ví đích trống, số tiền bằng 0, thiếu số tiền | PASS (3/3) — `400`, không gọi nghiệp vụ |
| TF-10–TF-11 | Thiếu JWT, JWT không hợp lệ | PASS (2/2) — `401`, không gọi nghiệp vụ |

Test không chạy MySQL, nên chưa xác nhận transaction database thực sự rollback đồng thời hai cập nhật ví và ghi giao dịch. Chưa kiểm thử cạnh tranh nhiều request trong đợt bổ sung này.

Chạy lại từ `digitalwallet-system`:

```powershell
.\gradlew.bat --gradle-user-home "$env:TEMP\sauetdigitalwallet-gradle" test --tests "sa.uet.digitalwallet_system.adapter.in.web.TransferEndpointTest" --no-daemon --console=plain
```

Số liệu lấy từ [JUnit XML](../../digitalwallet-system/build/test-results/test/TEST-sa.uet.digitalwallet_system.adapter.in.web.TransferEndpointTest.xml). File trong `build/` không được Git theo dõi.
