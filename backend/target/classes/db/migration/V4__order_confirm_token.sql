ALTER TABLE trade_order
    ADD COLUMN confirm_token VARCHAR(64) NULL,
    ADD COLUMN confirm_token_expire DATETIME NULL;

CREATE UNIQUE INDEX uk_trade_order_confirm_token ON trade_order (confirm_token);
