.PHONY: up down build logs restart clean check

# Khởi động toàn bộ stack (build nếu cần)
up:
	docker compose up -d --build

# Dừng toàn bộ stack
down:
	docker compose down

# Build lại image (không cache)
build:
	docker compose build --no-cache

# Xem logs realtime
logs:
	docker compose logs -f

# Restart app container (giữ MySQL)
restart:
	docker compose restart app

# Xóa toàn bộ (bao gồm volume MySQL data)
clean:
	docker compose down -v --rmi local

# Kiểm tra trạng thái
check:
	docker compose ps

