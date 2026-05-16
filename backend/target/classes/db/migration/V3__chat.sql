CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    context_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    context_id BIGINT NOT NULL DEFAULT 0,
    last_message_preview VARCHAR(500) NULL,
    last_message_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_conversation (user_a_id, user_b_id, context_type, context_id),
    KEY idx_user_a_time (user_a_id, last_message_at),
    KEY idx_user_b_time (user_b_id, last_message_at)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    KEY idx_conv_created (conversation_id, created_at)
);
