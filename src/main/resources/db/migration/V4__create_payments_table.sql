CREATE TABLE payments (
    payment_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pay_type VARCHAR(255) NOT NULL,
    pay_amount BIGINT NOT NULL,
    order_name VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    payment_key VARCHAR(255),
    fail_reason VARCHAR(255),
    cancel_reason VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_payment_user ON payments (user_id);
CREATE INDEX idx_payment_orderId ON payments (order_id);
CREATE INDEX idx_payment_paymentKey ON payments (payment_key);
