CREATE TABLE payments (id UUID PRIMARY KEY, order_id UUID NOT NULL UNIQUE, amount NUMERIC(10,2) NOT NULL, currency VARCHAR(10) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL);

CREATE TABLE payment_transactions (id UUID PRIMARY KEY, payment_id UUID NOT NULL, gateway_ref VARCHAR(100), status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_payment FOREIGN KEY (payment_id) REFERENCES payments(id));

