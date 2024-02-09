/*
This migration adds a few things:
- A `description` column to the account table, so people can add extra notes and
content to their accounts that isn't otherwise captured by the other fields.
- A `category_id` is added to transaction line items, so that each line item can
individually be marked with a category, so that you can further differentiate
large purchases consisting of smaller items.
*/
ALTER TABLE account
    ADD COLUMN description VARCHAR(255) DEFAULT NULL AFTER currency;

ALTER TABLE transaction_line_item
    ADD COLUMN category_id BIGINT DEFAULT NULL AFTER description;

ALTER TABLE transaction_line_item
    ADD CONSTRAINT fk_transaction_line_item_category
        FOREIGN KEY (category_id) REFERENCES transaction_category(id)
            ON UPDATE CASCADE ON DELETE CASCADE;
