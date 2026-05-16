-- 求购图片表
CREATE TABLE IF NOT EXISTS `wanted_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wanted_id` BIGINT NOT NULL COMMENT '求购ID',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    PRIMARY KEY (`id`),
    INDEX `idx_wanted_id` (`wanted_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求购图片表';

-- wanted表添加浏览量字段（如果已存在则忽略）
SET @dbname = DATABASE();
SET @tablename = 'wanted';
SET @columnname = 'view_count';
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
    'SELECT 1',
    'ALTER TABLE wanted ADD COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT "浏览量"'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
