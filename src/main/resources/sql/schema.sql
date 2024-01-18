CREATE TABLE account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    account_type VARCHAR(31) NOT NULL,
    account_number VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(63) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploaded_at TIMESTAMP NOT NULL,
    identifier VARCHAR(63) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL
);

/* TRANSACTION ENTITIES */

CREATE TABLE transaction_vendor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE transaction_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT NULL,
    name VARCHAR(63) NOT NULL UNIQUE,
    color VARCHAR(6) NOT NULL DEFAULT 'FFFFFF',
    CONSTRAINT fk_transaction_category_parent
        FOREIGN KEY (parent_id) REFERENCES transaction_category(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE transaction_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(63) NOT NULL UNIQUE
);

CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    amount NUMERIC(12, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NULL,
    vendor_id BIGINT DEFAULT NULL,
    category_id BIGINT DEFAULT NULL,
    CONSTRAINT fk_transaction_vendor
        FOREIGN KEY (vendor_id) REFERENCES transaction_vendor(id)
            ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id) REFERENCES transaction_category(id)
            ON UPDATE CASCADE ON DELETE SET NULL
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

CREATE TABLE transaction_attachment (
    transaction_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, attachment_id),
    CONSTRAINT fk_transaction_attachment_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_transaction_attachment_attachment
        FOREIGN KEY (attachment_id) REFERENCES attachment(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE transaction_tag_join (
    transaction_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_transaction_tag_join_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_transaction_tag_join_tag
        FOREIGN KEY (tag_id) REFERENCES transaction_tag(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE transaction_line_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    value_per_item NUMERIC(12, 4) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    idx INT NOT NULL DEFAULT 0,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT fk_transaction_line_item_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_transaction_line_item_quantity_positive
        CHECK quantity > 0
);

/* BALANCE RECORD ENTITIES */

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

CREATE TABLE balance_record_attachment (
    balance_record_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    PRIMARY KEY (balance_record_id, attachment_id),
    CONSTRAINT fk_balance_record_attachment_balance_record
        FOREIGN KEY (balance_record_id) REFERENCES balance_record(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_balance_record_attachment_attachment
        FOREIGN KEY (attachment_id) REFERENCES attachment(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE account_history_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp TIMESTAMP NOT NULL,
    account_id BIGINT NOT NULL,
    type VARCHAR(63) NOT NULL,
    CONSTRAINT fk_account_history_item_account
        FOREIGN KEY (account_id) REFERENCES account(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE account_history_item_text (
    item_id BIGINT NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT fk_account_history_item_text_pk
        FOREIGN KEY (item_id) REFERENCES account_history_item(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE account_history_item_account_entry (
    item_id BIGINT NOT NULL PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    CONSTRAINT fk_account_history_item_account_entry_pk
        FOREIGN KEY (item_id) REFERENCES account_history_item(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_account_history_item_account_entry
        FOREIGN KEY (entry_id) REFERENCES account_entry(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE account_history_item_balance_record (
    item_id BIGINT NOT NULL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    CONSTRAINT fk_account_history_item_balance_record_pk
        FOREIGN KEY (item_id) REFERENCES account_history_item(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_account_history_item_balance_record
        FOREIGN KEY (record_id) REFERENCES balance_record(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);
