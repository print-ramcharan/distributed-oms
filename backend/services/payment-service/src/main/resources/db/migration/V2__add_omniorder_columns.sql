ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(36);
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE payments ADD COLUMN payment_method VARCHAR(50);

ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);
