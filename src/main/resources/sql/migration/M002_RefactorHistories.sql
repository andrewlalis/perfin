/*
Migration to clean up history entities so that they are easier to work with, and
less prone to errors.

- Removes existing account history items.
- Adds a generic history table and history items that are linked to a history.
- Adds history links to accounts and transactions.
*/

CREATE TABLE history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
);

CREATE TABLE history_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    history_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    type VARCHAR(63) NOT NULL,
    CONSTRAINT fk_history_item_history
        FOREIGN KEY (history_id) REFERENCES history(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE history_item_text (
    id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT fk_history_item_text_pk
        FOREIGN KEY (id) REFERENCES history_item(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE history_account (
    account_id BIGINT NOT NULL,
    history_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, history_id),
    CONSTRAINT fk_history_account_account
        FOREIGN KEY (account_id) REFERENCES account(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_history_account_history
        FOREIGN KEY (history_id) REFERENCES history(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE history_transaction (
    transaction_id BIGINT NOT NULL,
    history_id BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, history_id),
    CONSTRAINT fk_history_transaction_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_history_transaction_history
        FOREIGN KEY (history_id) REFERENCES history(id)
            ON UPDATE CASCADE ON DELETE CASCADE
);

DROP TABLE IF EXISTS account_history_item_text;
DROP TABLE IF EXISTS account_history_item_account_entry;
DROP TABLE IF EXISTS account_history_item_balance_record;
DROP TABLE IF EXISTS account_history_item;


