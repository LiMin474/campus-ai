-- 系统助手账号（id=9999）
-- 用于匹配 Agent 阶段二：商品发布时自动给求购者发聊天通知
-- 不可登录（无有效密码/手机号），仅作为系统消息发送者

INSERT INTO `user` (`id`, `student_no`, `phone`, `nickname`, `password`, `role`, `credit_score`, `carbon_points`, `appeal_fail_count`, `banned`, `avatar_url`, `created_at`, `updated_at`)
VALUES (
    9999,
    'SYSTEM',
    NULL,
    '系统助手',
    '',                          -- 空密码，无法登录
    'SYSTEM',                    -- 新角色类型
    100,                         -- 满信用
    0,
    0,
    FALSE,
    NULL,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE `nickname` = '系统助手', `role` = 'SYSTEM';
