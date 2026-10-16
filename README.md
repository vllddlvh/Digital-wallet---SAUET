# Digital Wallet System

Hệ thống Backend RESTful API cho dịch vụ Ví điện tử. Ứng dụng triển khai theo Hexagonal Architecture, đảm bảo tính độc lập giữa Business và Infrastructure.

---

## 1. Công nghệ sử dụng

- **Ngôn ngữ & Nền tảng:** Java, Spring Boot.
- **Lưu trữ & ORM:** MySQL, Spring Data JPA, Hibernate.
- **Mapper & Utilities:** MapStruct, Lombok.
- **API:** OpenAPI.
- **Kiểm thử:** JUnit, Mockito.
- **Đóng gói:** Docker.

---

## 2. Kiến trúc hệ thống

Dự án áp dụng Hexagonal Architecture: Business core (`Domain` & `Application`) nằm ở trung tâm, không phụ thuộc vào Framework hay Database. Mọi giao tiếp của Business và Infrastructure đều thông qua Ports và Adapters.
- Architecture docs: [digitalwallet-docs/architecture](digitalwallet-docs/architecture.md)
- Đọc thêm về Hexagonal Architecture: https://github.com/thombergs/buckpal

```mermaid
flowchart LR
    subgraph DrivingAdapters["Inbound Adapters (Web / REST)"]
        Client[HTTP Client / Swagger] --> Controller[REST Controllers]
        Controller --> InPort[Inbound Ports / UseCases]
    end

    subgraph ApplicationCore["Core"]
        direction TB
        InPort --> AppService[Application Services]
        AppService --> DomainModel[Domain Models & Rules]
        AppService --> OutPort[Outbound Ports / SPI]
    end

    subgraph DrivenAdapters["Outbound Adapters (Hạ tầng)"]
        OutPort --> PersistenceAdapter[Persistence Adapter / JPA]
        OutPort --> SecurityAdapter[Security Adapter / JWT]
        PersistenceAdapter --> Database[(MySQL Database)]
    end
```

---

## 3. Đặc tả chức năng & Danh mục API

Hệ thống tập trung vào 4 nhóm nghiệp vụ chính:

```mermaid
flowchart LR
    A[Khách / Người dùng] --> B[1. Xác thực<br/>Register / Login]
    B --> C[2. Quản lý Ví<br/>Số dư / Trạng thái]
    C --> D[3. Liên kết Ngân hàng<br/>Link / Unlink]
    C --> E[4. Giao dịch Tài chính<br/>Topup / Withdraw / Transfer]
    E --> F[(Sổ cái Ledger<br/>Transactions)]
```

ERD Schema:

```mermaid
erDiagram
    USERS ||--|| WALLETS : "owns (1:1)"
    WALLETS ||--o{ LINKED_BANK_ACCOUNTS : "links (1:N)"
    WALLETS ||--o{ TRANSACTIONS : "source of (1:N)"
    WALLETS ||--o{ TRANSACTIONS : "target of (1:N)"
    LINKED_BANK_ACCOUNTS ||--o{ TRANSACTIONS : "funds via (1:N)"

    USERS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR(50) username UK "NOT NULL"
        VARCHAR(255) password_hash "BCrypt hash"
        VARCHAR(100) full_name "NOT NULL"
        VARCHAR(100) email UK "NOT NULL"
        VARCHAR(20) role "ROLE_USER, ROLE_ADMIN"
        DATETIME(3) created_at "CURRENT_TIMESTAMP(3)"
    }

    WALLETS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK,UK "NOT NULL"
        VARCHAR(30) wallet_number UK "NOT NULL"
        BIGINT balance ">= 0, default 0"
        VARCHAR(20) status "ACTIVE, FROZEN, CLOSED"
        BIGINT version "Optimistic locking (@Version)"
        DATETIME(3) created_at "CURRENT_TIMESTAMP(3)"
        DATETIME(3) updated_at "ON UPDATE CURRENT_TIMESTAMP(3)"
    }

    LINKED_BANK_ACCOUNTS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT wallet_id FK "NOT NULL"
        VARCHAR(20) bank_code "NOT NULL (VD: VCB, TCB)"
        VARCHAR(100) bank_name "NOT NULL"
        VARCHAR(50) account_number_masked "NOT NULL (VD: ******6789)"
        VARCHAR(100) holder_name "NOT NULL"
        VARCHAR(20) status "ACTIVE, UNLINKED"
        DATETIME(3) linked_at "CURRENT_TIMESTAMP(3)"
        DATETIME(3) unlinked_at "NULL / DATETIME(3)"
    }

    TRANSACTIONS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR(36) reference_code UK "UUID v4 (Idempotency Key)"
        VARCHAR(20) type "TRANSFER, TOPUP, WITHDRAW"
        BIGINT source_wallet_id FK "NULLable"
        BIGINT target_wallet_id FK "NULLable"
        BIGINT linked_bank_account_id FK "NULLable"
        BIGINT amount "> 0"
        VARCHAR(255) description "NULLable"
        DATETIME(3) created_at "CURRENT_TIMESTAMP(3)"
    }
```

- SwaggerAPI: https://vllddlvh.github.io/SAUETDigitalWallet/digitalwallet-docs/swaggerAPI/
- API-Contract: [digitalwallet-docs/apicontract](digitalwallet-docs/apicontract.md)
- Business docs: [digitalwallet-docs/business](digitalwallet-docs/business.md)
- Database docs: [digitalwallet-docs/dbanalysis](digitalwallet-docs/dbanalysis.md)

### 3.1. Xác thực & Tài khoản (`/api/v1/auth`)

| Phương thức | Endpoint                | Quyền hạn | Mô tả                                                                                  |
| :---------- | :---------------------- | :-------- | :------------------------------------------------------------------------------------- |
| `POST`      | `/api/v1/auth/register` | Public    | Đăng ký tài khoản mới; tự động khởi tạo ví chính (số dư `0 VND`, trạng thái `ACTIVE`). |
| `POST`      | `/api/v1/auth/login`    | Public    | Đăng nhập bằng `username` / `password`, trả về JWT Access Token (Bearer).              |

### 3.2. Quản lý Ví điện tử (`/api/v1/wallets`)

| Phương thức | Endpoint             | Quyền hạn     | Mô tả                                                                                             |
| :---------- | :------------------- | :------------ | :------------------------------------------------------------------------------------------------ |
| `GET`       | `/api/v1/wallets/me` | Authenticated | Lấy thông tin ví, số ví định danh (`wallet_number`), số dư khả dụng (`balance`) và trạng thái ví. |

### 3.3. Liên kết Ngân hàng (`/api/v1/linked-banks`)

| Phương thức | Endpoint                    | Quyền hạn     | Mô tả                                                                          |
| :---------- | :-------------------------- | :------------ | :----------------------------------------------------------------------------- |
| `POST`      | `/api/v1/linked-banks`      | Authenticated | Liên kết tài khoản ngân hàng (mã hóa/che số tài khoản `account_number_masked`). |
| `GET`       | `/api/v1/linked-banks`      | Authenticated | Lấy danh sách các tài khoản ngân hàng đang kích hoạt (`ACTIVE`) của ví.         |
| `DELETE`    | `/api/v1/linked-banks/{id}` | Authenticated | Hủy liên kết tài khoản ngân hàng (Soft-delete: cập nhật trạng thái `UNLINKED`). |

### 3.4. Giao dịch Tài chính (`/api/v1/transactions`)

| Phương thức | Endpoint                         | Quyền hạn     | Mô tả                                                                      |
| :---------- | :------------------------------- | :------------ | :------------------------------------------------------------------------- |
| `POST`      | `/api/v1/transactions/topup`    | Authenticated | Nạp tiền từ tài khoản ngân hàng liên kết vào ví.                           |
| `POST`      | `/api/v1/transactions/withdraw` | Authenticated | Rút tiền từ ví về tài khoản ngân hàng liên kết (kiểm tra số dư khả dụng). |
| `POST`      | `/api/v1/transactions/transfer` | Authenticated | Chuyển tiền nội bộ sang ví khác theo số ví (`target_wallet_number`).       |
| `GET`       | `/api/v1/transactions`          | Authenticated | Lấy lịch sử giao dịch (nạp, rút, chuyển, nhận tiền) của ví.                |

---

## 4. Gợi ý các cải tiến cho Pha 2

- Phase 2 - suggestion: [digitalwallet-docs/phase2_suggestion](digitalwallet-docs/phase2_suggestion.md)
### 4.1. Về Business

- Thay đổi quan hệ giữa User và Wallet thành 1 - N để một User có thể quản lý nhiều ví (ví USD, VNĐ,...)
### 4.2. Về một số use-cases liên quan tới race condition

- Use-case 1: Khi yêu cầu data từ frontend được gửi nhiều lần, back-end trả về kết quả giống nhau cho những request này. Vậy vấn đề mình gợi ý để các bạn giải quyết là làm sao cho khi thanh toán được khởi tạo, hệ thống payment cần trừ tiền đúng 1 lần để sao cho một message với cùng một nội dung thì được gửi đúng 1 lần tới user.
- Use-case 2: Số dư hiện tại của User là N, User thực hiện cùng lúc hai request chuyển tiền với lần lượt số tiền là A và B (A + B > N). Cả hai request đều đọc số dư N trước khi bất kỳ request nào cập nhật dữ liệu ==> Cả hai đều nghĩ giao dịch hợp lệ. Vậy các bạn phải giải quyết vấn đề nếu một trong hai transaction thành công trước thì transaction còn lại phải bị từ chối.
