# HỢP ĐỒNG GIAO TIẾP API (API CONTRACT)
## HỆ THỐNG VÍ ĐIỆN TỬ (DIGITAL WALLET) - PHA 1

---

## 1. QUY ƯỚC CHUNG (GENERAL CONVENTIONS)

### 1.1. Giao thức & Định dạng dữ liệu
- **Base URL:** `/api/v1`
- **Giao thức:** HTTP/1.1 hoặc HTTP/2 qua RESTful API.
- **Content-Type:** `application/json; charset=UTF-8` cho toàn bộ Request Body và Response Body.
- **Mã hóa ký tự:** Hỗ trợ UTF-8 (tương thích `utf8mb4` trong CSDL).

### 1.2. Cơ chế Xác thực (Authentication)
- Sử dụng **JSON Web Token (JWT)** theo chuẩn Bearer Token.
- Header bắt buộc đối với các endpoint yêu cầu xác thực:
  ```http
  Authorization: Bearer <access_token>
  ```
- Token được cấp phát qua endpoint `POST /api/v1/auth/login`. Nếu token thiếu, hết hạn hoặc không hợp lệ, hệ thống trả về mã lỗi `401 Unauthorized`.

### 1.3. Chuẩn dữ liệu Tài chính & Thời gian
- **Đơn vị tiền tệ:** Việt Nam Đồng (VND), kiểu số nguyên `BIGINT` (JSON biểu diễn dạng `number`). Tuyệt đối không dùng số thực, số tiền giao dịch luôn $> 0$.
- **Dấu thời gian (Timestamp):** Chuẩn ISO-8601 UTC với độ chính xác mili-giây: `YYYY-MM-DDTHH:mm:ss.sssZ` (tương ứng `DATETIME(3)` trong CSDL MySQL).

### 1.4. Cấu trúc Phản hồi Chuẩn (Standard Response Format)

#### Phản hồi Thành công (Success Response):
```json
{
  "success": true,
  "message": "Thông báo kết quả thao tác",
  "data": { ... }
}
```

#### Phản hồi Thất bại (Error Response):
```json
{
  "success": false,
  "errorCode": "TEN_MA_LOI_CHUAN_HOA",
  "message": "Mô tả chi tiết nguyên nhân lỗi cho client",
  "timestamp": "2026-10-16T00:00:00.000Z"
}
```

---

## 2. CHI TIẾT CÁC ENDPOINT (API SPECIFICATIONS)

---

### NHÓM 1: XÁC THỰC & NGƯỜI DÙNG (AUTHENTICATION)

#### 1.1. Đăng ký tài khoản (`POST /api/v1/auth/register`)
- **Mô tả:** Đăng ký tài khoản người dùng mới và tự động khởi tạo 1 ví điện tử chính với số dư `0 VND`, trạng thái `ACTIVE`.
- **Yêu cầu xác thực:** Không (Public).
- **Ràng buộc CSDL liên quan:** `uq_users_username`, `uq_users_email`, `chk_users_role`.

**Request Body:**
```json
{
  "username": "nguyenvana",
  "password": "Password123@",
  "fullName": "Nguyễn Văn A",
  "email": "vana@example.com"
}
```
*Ràng buộc tham số:*
- `username`: String (3 - 50 ký tự), không trùng lặp.
- `password`: String (tối thiểu 6 ký tự).
- `fullName`: String (1 - 100 ký tự).
- `email`: String (hợp lệ theo format email, tối đa 100 ký tự), không trùng lặp.

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "userId": 1,
    "username": "nguyenvana",
    "fullName": "Nguyễn Văn A",
    "email": "vana@example.com",
    "role": "ROLE_USER",
    "createdAt": "2026-10-16T00:00:00.000Z",
    "wallet": {
      "walletId": 1,
      "walletNumber": "WAL1029384756",
      "balance": 0,
      "status": "ACTIVE"
    }
  }
}
```

**Lỗi thường gặp:**
- `400 Bad Request`: Thiếu trường bắt buộc hoặc dữ liệu không đúng định dạng.
- `409 Conflict` (`USER_ALREADY_EXISTS`): `username` hoặc `email` đã tồn tại trên hệ thống.

---

#### 1.2. Đăng nhập (`POST /api/v1/auth/login`)
- **Mô tả:** Xác thực danh tính qua username và mật khẩu, trả về JWT Access Token.
- **Yêu cầu xác thực:** Không (Public).

**Request Body:**
```json
{
  "username": "nguyenvana",
  "password": "Password123@"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": 1,
      "username": "nguyenvana",
      "fullName": "Nguyễn Văn A",
      "role": "ROLE_USER"
    }
  }
}
```

**Lỗi thường gặp:**
- `401 Unauthorized` (`INVALID_CREDENTIALS`): Sai username hoặc password.

---

### NHÓM 2: QUẢN LÝ VÍ ĐIỆN TỬ (WALLET MANAGEMENT)

#### 2.1. Xem thông tin ví & số dư (`GET /api/v1/wallets/me`)
- **Mô tả:** Lấy thông tin chi tiết ví điện tử và số dư tức thời của người dùng đang đăng nhập.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)** *(Thỏa mãn tiêu chí: Endpoint GET cần xác thực)*.
- **Ràng buộc CSDL liên quan:** Bảng `wallets`, `uq_wallets_user`.

**Request Headers:**
```http
Authorization: Bearer <access_token>
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Lấy thông tin ví thành công",
  "data": {
    "walletId": 1,
    "walletNumber": "WAL1029384756",
    "balance": 5000000,
    "status": "ACTIVE",
    "createdAt": "2026-10-16T00:00:00.000Z",
    "updatedAt": "2026-10-16T00:15:30.120Z"
  }
}
```

**Lỗi thường gặp:**
- `401 Unauthorized`: Chưa truyền Token hoặc Token không hợp lệ.
- `404 Not Found` (`WALLET_NOT_FOUND`): Không tìm thấy ví tương ứng với tài khoản.

---

### NHÓM 3: LIÊN KẾT TÀI KHOẢN NGÂN HÀNG (LINKED BANK ACCOUNTS)

#### 3.1. Liên kết tài khoản ngân hàng mới (`POST /api/v1/linked-banks`)
- **Mô tả:** Liên kết tài khoản ngân hàng vào ví phục vụ nạp và rút tiền. Số tài khoản được che mờ tự động trước khi lưu.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)** *(Thỏa mãn tiêu chí: Endpoint POST cần xác thực)*.
- **Ràng buộc CSDL liên quan:** Bảng `linked_bank_accounts`, `fk_linked_wallet`, `chk_linked_status`.

**Request Body:**
```json
{
  "bankCode": "VCB",
  "bankName": "Ngân hàng TMCP Ngoại thương Việt Nam",
  "accountNumber": "0123456789",
  "holderName": "NGUYEN VAN A"
}
```
*Ràng buộc tham số:*
- `bankCode`: String (tối đa 20 ký tự, ví dụ: VCB, TCB, MB, VIB).
- `bankName`: String (tối đa 100 ký tự).
- `accountNumber`: String (chỉ chứa số, độ dài 6 - 20 số).
- `holderName`: String (viết hoa không dấu, tối đa 100 ký tự).

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Liên kết tài khoản ngân hàng thành công",
  "data": {
    "id": 1,
    "bankCode": "VCB",
    "bankName": "Ngân hàng TMCP Ngoại thương Việt Nam",
    "accountNumberMasked": "******6789",
    "holderName": "NGUYEN VAN A",
    "status": "ACTIVE",
    "linkedAt": "2026-10-16T00:10:00.000Z"
  }
}
```

**Lỗi thường gặp:**
- `400 Bad Request`: Thông tin tài khoản ngân hàng không hợp lệ.
- `401 Unauthorized`: Chưa xác thực.

---

#### 3.2. Lấy danh sách ngân hàng liên kết (`GET /api/v1/linked-banks`)
- **Mô tả:** Lấy danh sách tất cả tài khoản ngân hàng đang kích hoạt (`status = 'ACTIVE'`) của người dùng hiện tại.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)**.
- **Ràng buộc CSDL liên quan:** `idx_linked_wallet_status (wallet_id, status)`.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Lấy danh sách tài khoản ngân hàng thành công",
  "data": [
    {
      "id": 1,
      "bankCode": "VCB",
      "bankName": "Ngân hàng TMCP Ngoại thương Việt Nam",
      "accountNumberMasked": "******6789",
      "holderName": "NGUYEN VAN A",
      "status": "ACTIVE",
      "linkedAt": "2026-10-16T00:10:00.000Z"
    }
  ]
}
```

---

#### 3.3. Hủy liên kết tài khoản ngân hàng (`DELETE /api/v1/linked-banks/{id}`)
- **Mô tả:** Hủy liên kết tài khoản ngân hàng bằng cơ chế xóa mềm (Soft Delete). Trạng thái chuyển thành `UNLINKED` và cập nhật `unlinked_at`.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)** *(Thỏa mãn tiêu chí: Endpoint method DELETE)*.
- **Ràng buộc CSDL liên quan:** `chk_linked_unlinked_at`.

**Path Parameters:**
- `id`: `BIGINT` - Định danh bản ghi liên kết ngân hàng cần hủy.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Hủy liên kết tài khoản ngân hàng thành công",
  "data": {
    "id": 1,
    "status": "UNLINKED",
    "unlinkedAt": "2026-10-16T00:20:00.000Z"
  }
}
```

**Lỗi thường gặp:**
- `401 Unauthorized`: Chưa xác thực.
- `403 Forbidden` (`ACCESS_DENIED`): Bản ghi ngân hàng này không thuộc sở hữu của ví người dùng hiện tại.
- `404 Not Found` (`BANK_ACCOUNT_NOT_FOUND`): Không tìm thấy tài khoản ngân hàng hoặc tài khoản đã hủy trước đó.

---

### NHÓM 4: GIAO DỊCH TÀI CHÍNH & SỔ CÁI (TRANSACTIONS LEDGER)

#### 4.1. Nạp tiền vào ví (`POST /api/v1/transactions/topup`)
- **Mô tả:** Nạp tiền từ tài khoản ngân hàng liên kết vào ví. Số dư ví tăng lên, ghi sổ cái giao dịch `TOPUP`.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)**.
- **Ràng buộc CSDL liên quan:** `chk_tx_shape` (yêu cầu `source_wallet_id IS NULL`, `target_wallet_id NOT NULL`, `linked_bank_account_id NOT NULL`), `chk_tx_amount (amount > 0)`.

**Request Body:**
```json
{
  "referenceCode": "550e8400-e29b-41d4-a716-446655440000",
  "linkedBankAccountId": 1,
  "amount": 1000000,
  "description": "Nạp tiền từ VCB vào ví"
}
```
*Ràng buộc tham số:*
- `referenceCode`: UUID v4 (36 ký tự, Idempotency key, duy nhất).
- `linkedBankAccountId`: BIGINT (phải thuộc ví hiện tại và đang `ACTIVE`).
- `amount`: BIGINT (số tiền $> 0$).
- `description`: String (tối đa 255 ký tự, tùy chọn).

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Nạp tiền vào ví thành công",
  "data": {
    "transactionId": 101,
    "referenceCode": "550e8400-e29b-41d4-a716-446655440000",
    "type": "TOPUP",
    "amount": 1000000,
    "newBalance": 6000000,
    "targetWalletNumber": "WAL1029384756",
    "linkedBankAccountId": 1,
    "description": "Nạp tiền từ VCB vào ví",
    "createdAt": "2026-10-16T00:25:00.000Z"
  }
}
```

**Lỗi thường gặp:**
- `400 Bad Request` (`INVALID_AMOUNT`): Số tiền $\le 0$.
- `404 Not Found` (`BANK_ACCOUNT_NOT_FOUND`): Tài khoản ngân hàng không tồn tại hoặc không ở trạng thái `ACTIVE`.
- `409 Conflict` (`DUPLICATE_REFERENCE_CODE`): Trùng lặp `referenceCode` (giao dịch đã được xử lý trước đó).

---

#### 4.2. Rút tiền về ngân hàng (`POST /api/v1/transactions/withdraw`)
- **Mô tả:** Rút tiền từ ví về tài khoản ngân hàng liên kết. Số dư ví giảm, ghi sổ cái giao dịch `WITHDRAW`.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)**.
- **Ràng buộc CSDL liên quan:** `chk_tx_shape` (yêu cầu `source_wallet_id NOT NULL`, `target_wallet_id IS NULL`, `linked_bank_account_id NOT NULL`), `chk_wallets_balance (balance >= 0)`.

**Request Body:**
```json
{
  "referenceCode": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "linkedBankAccountId": 1,
  "amount": 500000,
  "description": "Rút tiền về thẻ VCB"
}
```

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Rút tiền về tài khoản ngân hàng thành công",
  "data": {
    "transactionId": 102,
    "referenceCode": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "type": "WITHDRAW",
    "amount": 500000,
    "newBalance": 5500000,
    "sourceWalletNumber": "WAL1029384756",
    "linkedBankAccountId": 1,
    "description": "Rút tiền về thẻ VCB",
    "createdAt": "2026-10-16T00:30:00.000Z"
  }
}
```

**Lỗi thường gặp:**
- `400 Bad Request` (`INSUFFICIENT_BALANCE`): Số dư ví không đủ để rút tiền (`balance < amount`).
- `400 Bad Request` (`WALLET_FROZEN`): Ví đang bị đóng băng hoặc không hoạt động.

---

#### 4.3. Chuyển tiền giữa hai ví (`POST /api/v1/transactions/transfer`)
- **Mô tả:** Chuyển tiền từ ví người gửi sang ví người nhận bằng số ví (`targetWalletNumber`). Thực thi atomic transaction: trừ ví gửi, cộng ví nhận, ghi sổ cái `TRANSFER`.
- **Yêu cầu xác thực:** **Có (`Bearer Token`)**.
- **Ràng buộc CSDL liên quan:** `chk_tx_shape` (yêu cầu `source_wallet_id <> target_wallet_id`, `linked_bank_account_id IS NULL`), `chk_wallets_balance`.

**Request Body:**
```json
{
  "referenceCode": "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd",
  "targetWalletNumber": "WAL9876543210",
  "amount": 200000,
  "description": "Chuyển tiền trả tiền ăn trưa"
}
```
*Ràng buộc tham số:*
- `referenceCode`: UUID v4 (36 ký tự).
- `targetWalletNumber`: String (số ví người nhận, khác số ví người gửi).
- `amount`: BIGINT (số tiền $> 0$).
- `description`: String (tối đa 255 ký tự).

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Chuyển tiền thành công",
  "data": {
    "transactionId": 103,
    "referenceCode": "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd",
    "type": "TRANSFER",
    "amount": 200000,
    "newBalance": 5300000,
    "sourceWalletNumber": "WAL1029384756",
    "targetWalletNumber": "WAL9876543210",
    "description": "Chuyển tiền trả tiền ăn trưa",
    "createdAt": "2026-10-16T00:35:00.000Z"
  }
}
```

**Lỗi thường gặp:**
- `400 Bad Request` (`SAME_WALLET_TRANSFER`): Ví nguồn và ví đích trùng nhau.
- `400 Bad Request` (`INSUFFICIENT_BALANCE`): Số dư ví người gửi không đủ.
- `404 Not Found` (`TARGET_WALLET_NOT_FOUND`): Không tìm thấy ví người nhận.
- `400 Bad Request` (`WALLET_INACTIVE`): Ví gửi hoặc ví nhận đang bị khóa/đóng băng (`FROZEN`/`CLOSED`).

---

#### 4.4. Tra cứu lịch sử giao dịch (`GET /api/v1/transactions`)
- **Mô tả:** Lấy danh sách giao dịch tài chính (Nạp, Rút, Chuyển) có liên quan đến ví của người dùng hiện tại (làm ví nguồn hoặc ví đích), sắp xếp thời gian giảm dần (`created_at DESC`).
- **Yêu cầu xác thực:** **Có (`Bearer Token`)**.
- **Ràng buộc CSDL liên quan:** `idx_tx_source_created`, `idx_tx_target_created`.

**Query Parameters:**
- `page`: Integer (tùy chọn, mặc định `0`).
- `size`: Integer (tùy chọn, mặc định `20`, tối đa `100`).

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Lấy lịch sử giao dịch thành công",
  "data": {
    "content": [
      {
        "id": 103,
        "referenceCode": "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd",
        "type": "TRANSFER",
        "direction": "OUT",
        "amount": 200000,
        "sourceWalletId": 1,
        "targetWalletId": 2,
        "linkedBankAccountId": null,
        "description": "Chuyển tiền trả tiền ăn trưa",
        "createdAt": "2026-10-16T00:35:00.000Z"
      },
      {
        "id": 101,
        "referenceCode": "550e8400-e29b-41d4-a716-446655440000",
        "type": "TOPUP",
        "direction": "IN",
        "amount": 1000000,
        "sourceWalletId": null,
        "targetWalletId": 1,
        "linkedBankAccountId": 1,
        "description": "Nạp tiền từ VCB vào ví",
        "createdAt": "2026-10-16T00:25:00.000Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

---

### NHÓM 5: TÀI LIỆU API (API DOCUMENTATION)

| Endpoint | Method | Quyền hạn | Mô tả |
| :--- | :---: | :---: | :--- |
| `/api-docs` | `GET` | Public | Cung cấp tài liệu OpenAPI dạng JSON Schema |
| `/swagger-ui.html` | `GET` | Public | Giao diện đồ họa tương tác và kiểm thử API trực quan (Swagger UI) |

---

## 3. BẢNG MÃ LỖI NGHIỆP VỤ (ERROR CODES REFERENCE)

| Mã lỗi (`errorCode`) | HTTP Status | Mô tả chi tiết nguyên nhân |
| :--- | :---: | :--- |
| `INVALID_INPUT` | `400 Bad Request` | Dữ liệu đầu vào không hợp lệ hoặc thiếu trường bắt buộc |
| `INVALID_AMOUNT` | `400 Bad Request` | Số tiền giao dịch phải là số nguyên dương $> 0$ |
| `INSUFFICIENT_BALANCE` | `400 Bad Request` | Số dư trong ví không đủ để rút hoặc chuyển tiền |
| `SAME_WALLET_TRANSFER` | `400 Bad Request` | Không thể chuyển tiền cho chính ví của mình |
| `WALLET_FROZEN` | `400 Bad Request` | Ví đang ở trạng thái FROZEN hoặc CLOSED, không thể giao dịch |
| `BANK_ACCOUNT_UNLINKED` | `400 Bad Request` | Tài khoản ngân hàng đã bị hủy liên kết trước đó |
| `INVALID_CREDENTIALS` | `401 Unauthorized` | Tên đăng nhập hoặc mật khẩu không chính xác |
| `UNAUTHORIZED` | `401 Unauthorized` | Token không hợp lệ, thiếu token hoặc token đã hết hạn |
| `ACCESS_DENIED` | `403 Forbidden` | Không có quyền truy cập hoặc thao tác trên tài nguyên của người khác |
| `WALLET_NOT_FOUND` | `404 Not Found` | Không tìm thấy ví tương ứng |
| `TARGET_WALLET_NOT_FOUND` | `404 Not Found` | Số ví người nhận không tồn tại trên hệ thống |
| `BANK_ACCOUNT_NOT_FOUND` | `404 Not Found` | Không tìm thấy tài khoản ngân hàng liên kết |
| `USER_ALREADY_EXISTS` | `409 Conflict` | Username hoặc email đã được đăng ký |
| `DUPLICATE_REFERENCE_CODE` | `409 Conflict` | Mã tham chiếu giao dịch (`referenceCode`) đã tồn tại |
| `CONCURRENT_UPDATE_CONFLICT` | `409 Conflict` | Xung đột cập nhật đồng thời (Optimistic Lock Version) |
| `INTERNAL_SERVER_ERROR` | `500 Server Error` | Lỗi máy chủ ngoài ý muốn |
