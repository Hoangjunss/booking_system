-- ======================================================
-- DATABASE SCHEMA FOR CONCERT TICKET BOOKING PLATFORM
-- PostgreSQL 15+
-- ======================================================

-- Xóa các bảng theo thứ tự phụ thuộc (cascade để tránh lỗi khóa ngoại)
DROP TABLE IF EXISTS user_voucher_usage CASCADE;
DROP TABLE IF EXISTS booking_items CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS ticket_categories CASCADE;
DROP TABLE IF EXISTS vouchers CASCADE;
DROP TABLE IF EXISTS concerts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ======================================================
-- 1. USERS
-- ======================================================
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER' 
                  CHECK (role IN ('CUSTOMER', 'OPERATOR', 'ADMIN')),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_role ON users(role);

-- ======================================================
-- 2. CONCERTS
-- ======================================================
CREATE TABLE concerts (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    venue         VARCHAR(255) NOT NULL,
    event_date    TIMESTAMP NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'PUBLISHED', 'ENDED', 'CANCELLED')),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_concerts_status ON concerts(status);
CREATE INDEX idx_concerts_event_date ON concerts(event_date);

-- ======================================================
-- 3. TICKET CATEGORIES (inventory)
-- ======================================================
CREATE TABLE ticket_categories (
    id                 BIGSERIAL PRIMARY KEY,
    concert_id         BIGINT NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    name               VARCHAR(100) NOT NULL,
    price              DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    total_quantity     INT NOT NULL CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL CHECK (available_quantity >= 0),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_available_le_total CHECK (available_quantity <= total_quantity)
);

CREATE INDEX idx_ticket_categories_concert ON ticket_categories(concert_id);
-- Partial index for fast flash sale query (only categories with available tickets)
CREATE INDEX idx_ticket_categories_available ON ticket_categories(concert_id) WHERE available_quantity > 0;

-- ======================================================
-- 4. VOUCHERS
-- ======================================================
CREATE TABLE vouchers (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(100) NOT NULL UNIQUE,
    discount_type    VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value   DECIMAL(10,2) NOT NULL CHECK (discount_value >= 0),
    usage_limit      INT NOT NULL DEFAULT 1 CHECK (usage_limit >= 0),
    used_count       INT NOT NULL DEFAULT 0 CHECK (used_count >= 0 AND used_count <= usage_limit),
    valid_from       TIMESTAMP NOT NULL,
    valid_to         TIMESTAMP NOT NULL,
    min_order_value  DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (valid_from <= valid_to)
);

CREATE INDEX idx_vouchers_code ON vouchers(code);
-- Partial index bị xóa do CURRENT_TIMESTAMP không immutable, chỉ giữ index thường hoặc bỏ hẳn
-- Nếu muốn tối ưu truy vấn voucher còn hạn có thể tạo index trên (valid_from, valid_to, used_count, code) thông thường
CREATE INDEX idx_vouchers_valid ON vouchers(valid_from, valid_to, used_count, code);

-- ======================================================
-- 5. BOOKINGS (with idempotency key)
-- ======================================================
CREATE TABLE bookings (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    concert_id        BIGINT NOT NULL REFERENCES concerts(id),
    idempotency_key   VARCHAR(255) NOT NULL UNIQUE,
    status            VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'FAILED', 'EXPIRED')),
    total_price       DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_concert ON bookings(concert_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_expires_at ON bookings(expires_at) WHERE status = 'PENDING';

-- ======================================================
-- 6. BOOKING ITEMS (snapshot)
-- ======================================================
CREATE TABLE booking_items (
    id                 BIGSERIAL PRIMARY KEY,
    booking_id         BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    ticket_category_id BIGINT NOT NULL REFERENCES ticket_categories(id),
    quantity           INT NOT NULL CHECK (quantity > 0),
    unit_price         DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);

-- ======================================================
-- 7. USER VOUCHER USAGE (anti-abuse)
-- ======================================================
CREATE TABLE user_voucher_usage (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    voucher_id   BIGINT NOT NULL REFERENCES vouchers(id),
    booking_id   BIGINT NOT NULL REFERENCES bookings(id),
    used_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uniq_user_voucher UNIQUE (user_id, voucher_id)
);

CREATE INDEX idx_user_voucher_user ON user_voucher_usage(user_id);
CREATE INDEX idx_user_voucher_voucher ON user_voucher_usage(voucher_id);

-- ======================================================
-- 8. TRIGGER: release tickets when booking is cancelled/expired
-- ======================================================
CREATE OR REPLACE FUNCTION release_tickets_on_booking_cancel()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.status = 'PENDING' AND NEW.status IN ('CANCELLED', 'EXPIRED', 'FAILED')) THEN
        UPDATE ticket_categories tc
        SET available_quantity = available_quantity + bi.quantity,
            updated_at = CURRENT_TIMESTAMP
        FROM booking_items bi
        WHERE bi.booking_id = NEW.id AND bi.ticket_category_id = tc.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_release_tickets
AFTER UPDATE OF status ON bookings
FOR EACH ROW
EXECUTE FUNCTION release_tickets_on_booking_cancel();

-- ======================================================
-- 9. (Optional) TRIGGER: auto update updated_at column
-- ======================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_concerts_updated_at BEFORE UPDATE ON concerts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ticket_categories_updated_at BEFORE UPDATE ON ticket_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_vouchers_updated_at BEFORE UPDATE ON vouchers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ======================================================
-- 10. COMMENT (optional documentation)
-- ======================================================
COMMENT ON TABLE users IS 'Registered users, including customers and operators';
COMMENT ON TABLE concerts IS 'Concert events, each has multiple ticket categories';
COMMENT ON TABLE ticket_categories IS 'Ticket types with inventory (total/available) under a concert';
COMMENT ON TABLE vouchers IS 'Promotional vouchers with usage limits and validity period';
COMMENT ON TABLE bookings IS 'Booking transactions with unique idempotency key to prevent duplicates';
COMMENT ON TABLE booking_items IS 'Itemized ticket categories within a booking (price snapshot)';
COMMENT ON TABLE user_voucher_usage IS 'Tracks which user used which voucher in which booking, ensures one-time use per user per voucher';
COMMENT ON FUNCTION release_tickets_on_booking_cancel() IS 'Automatically release reserved tickets when a pending booking is cancelled, expired, or failed';