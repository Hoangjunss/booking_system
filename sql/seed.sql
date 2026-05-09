-- ======================================================
-- SEED DATA FOR CONCERT TICKET BOOKING PLATFORM
-- Only for development/testing
-- ======================================================

-- Clean existing data (optional, comment if needed)
-- TRUNCATE TABLE user_voucher_usage, booking_items, bookings, ticket_categories, vouchers, concerts, users RESTART IDENTITY CASCADE;

-- Insert users (password = "password" encoded with BCrypt: $2a$10$...)
-- For demo: admin@example.com / admin123, customer1@example.com / pass123
INSERT INTO users (email, name, password, role, created_at, updated_at) VALUES
('admin@example.com', 'Admin User', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlE6lUq7X5qJfH5Q5Q5u', 'ADMIN', NOW(), NOW()),
('operator@example.com', 'Operator User', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlE6lUq7X5qJfH5Q5Q5u', 'OPERATOR', NOW(), NOW()),
('customer1@example.com', 'John Doe', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlE6lUq7X5qJfH5Q5Q5u', 'CUSTOMER', NOW(), NOW()),
('customer2@example.com', 'Jane Smith', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EfMlE6lUq7X5qJfH5Q5Q5u', 'CUSTOMER', NOW(), NOW());

-- Insert concerts
INSERT INTO concerts (name, description, venue, event_date, status, created_at, updated_at) VALUES
('Rock Fest 2025', 'Big rock concert with famous bands', 'Central Stadium', '2025-07-15 19:00:00', 'PUBLISHED', NOW(), NOW()),
('Classical Night', 'Orchestra performance', 'City Opera House', '2025-08-20 20:00:00', 'PUBLISHED', NOW(), NOW()),
('Jazz Summer', 'Smooth jazz evening', 'Garden Plaza', '2025-06-10 18:30:00', 'DRAFT', NOW(), NOW());

-- Insert ticket categories
-- Concert 1: Rock Fest
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity, created_at, updated_at) VALUES
(1, 'VIP', 150.00, 100, 100, NOW(), NOW()),
(1, 'Standard', 80.00, 500, 500, NOW(), NOW()),
(1, 'Balcony', 50.00, 200, 200, NOW(), NOW());

-- Concert 2: Classical Night
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity, created_at, updated_at) VALUES
(2, 'VIP', 200.00, 50, 50, NOW(), NOW()),
(2, 'Orchestra', 120.00, 300, 300, NOW(), NOW());

-- Concert 3: Jazz Summer (draft, still add categories anyway)
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity, created_at, updated_at) VALUES
(3, 'General', 45.00, 150, 150, NOW(), NOW());

-- Insert vouchers
INSERT INTO vouchers (code, discount_type, discount_value, usage_limit, used_count, valid_from, valid_to, min_order_value, created_at, updated_at) VALUES
('FLASH10', 'PERCENT', 10, 500, 0, '2025-05-01 00:00:00', '2025-12-31 23:59:59', 0, NOW(), NOW()),
('FIXED20', 'FIXED', 20, 100, 0, '2025-05-01 00:00:00', '2025-08-31 23:59:59', 50, NOW(), NOW()),
('WELCOME5', 'PERCENT', 5, 1000, 0, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 0, NOW(), NOW());

-- Optional: add some sample bookings (with PENDING status) to test admin dashboard
-- But avoid foreign key conflicts, so comment unless needed.
/*
INSERT INTO bookings (user_id, concert_id, idempotency_key, status, total_price, expires_at, created_at, updated_at) VALUES
(3, 1, 'key_001', 'PENDING', 80.00, NOW() + INTERVAL '5 minutes', NOW(), NOW());
INSERT INTO booking_items (booking_id, ticket_category_id, quantity, unit_price) VALUES (1, 2, 1, 80.00);
*/