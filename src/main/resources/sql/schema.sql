CREATE TABLE account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL,
    account_type VARCHAR(31) NOT NULL,
    account_number VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(63) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    amount NUMERIC(12, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NULL
);

CREATE TABLE account_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    amount NUMERIC(12, 4) NOT NULL,
    type ENUM('CREDIT', 'DEBIT') NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT fk_account_entry_account
        FOREIGN KEY (account_id) REFERENCES account(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_account_entry_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE balance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    account_id BIGINT NOT NULL,
    balance NUMERIC(12, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT fk_balance_record_account
        FOREIGN KEY (account_id) REFERENCES account(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);
