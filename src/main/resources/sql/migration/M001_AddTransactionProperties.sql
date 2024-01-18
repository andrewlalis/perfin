/*
Migration to add additional properties to transactions as per this GitHub issue:
https://github.com/andrewlalis/perfin/issues/10

Adds the following:
- An optional "vendor" field and associated vendor entity.
- An optional "category" field and associated category entity.
- An optional set of "tags" that are user-defined strings.
- An optional set of "line items" that comprise some subtotal of the transaction
  and can be used to specify that X amount of the total was spent on some
  specific item.
- An optional address of the purchase.
*/

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
    description VARCHAR(255) NOT NULL,
    CONSTRAINT fk_transaction_line_item_transaction
        FOREIGN KEY (transaction_id) REFERENCES transaction(id)
            ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_transaction_line_item_quantity_positive
        CHECK quantity > 0
);

ALTER TABLE transaction
    ADD COLUMN vendor_id BIGINT DEFAULT NULL AFTER description;
ALTER TABLE transaction
    ADD COLUMN category_id BIGINT DEFAULT NULL AFTER vendor_id;
ALTER TABLE transaction
    ADD CONSTRAINT fk_transaction_vendor
        FOREIGN KEY (vendor_id) REFERENCES transaction_vendor(id)
            ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE transaction
    ADD CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id) REFERENCES transaction_category(id)
            ON UPDATE CASCADE ON DELETE SET NULL;

