-- ============================================================
-- 校园二手交易 测试数据种子脚本
-- 用法:
--   mysql -u root -p campus_trade < seed_products.sql
--   或在 mysql 客户端: source D:/study/project/campus-trade/seed_products.sql
-- 说明: 补 5 个测试同学 + 30 件商品 + 30 张图片
--       所有账号密码均为 111111
-- ============================================================

-- ============================================================
-- 第一部分: 补充 5 个测试同学(密码 hash 同小李同学, 即密码 111111)
-- ============================================================
INSERT INTO user (student_no, phone, nickname, password, role, credit_score, carbon_points, appeal_fail_count, banned, avatar_url, last_signin_date, created_at, updated_at) VALUES
('2024001', '13800000001', '考研张',   '$2a$10$G0mfr4KBR2PGCjGM3c2iyemHKxo6419OpNbYYQRrHrtkJwe3uynD2', 'STUDENT', 100, 0,  0, 0, NULL, NULL, NOW(), NOW()),
('2024002', '13800000002', '运动王',   '$2a$10$G0mfr4KBR2PGCjGM3c2iyemHKxo6419OpNbYYQRrHrtkJwe3uynD2', 'STUDENT', 95,  12, 0, 0, NULL, NULL, NOW(), NOW()),
('2024003', '13800000003', '生活赵',   '$2a$10$G0mfr4KBR2PGCjGM3c2iyemHKxo6419OpNbYYQRrHrtkJwe3uynD2', 'STUDENT', 100, 5,  0, 0, NULL, NULL, NOW(), NOW()),
('2024004', '13800000004', '毕业陈',   '$2a$10$G0mfr4KBR2PGCjGM3c2iyemHKxo6419OpNbYYQRrHrtkJwe3uynD2', 'STUDENT', 90,  20, 0, 0, NULL, NULL, NOW(), NOW()),
('2024005', '13800000005', '书虫孙',   '$2a$10$G0mfr4KBR2PGCjGM3c2iyemHKxo6419OpNbYYQRrHrtkJwe3uynD2', 'STUDENT', 100, 8,  0, 0, NULL, NULL, NOW(), NOW());

-- ============================================================
-- 第二部分: 30 件商品
-- seller_id 用子查询通过 student_no 关联, 不依赖自增 id 连续
-- status 统一 ON_SHELF (在售)
-- 分类: 1书本 2数码 3衣物 4生活用品 5学习资料 6其他
-- ============================================================
INSERT INTO product (seller_id, title, description, price, category_id, status, view_count, like_count, condition_label, created_at, updated_at) VALUES
-- ---------- 小李同学(111111) - 数码控 - 数码(2) ----------
((SELECT id FROM user WHERE student_no='111111'), 'iPad Air 2022 九成新 考研网课配套 平板电脑 32G 白色', 'iPad Air 第五代 2022款 32G WiFi版 白色 九成新 配原装充电器 适合考研看网课 记笔记 屏幕无划痕 电池健康', 2800.00, 2, 'ON_SHELF', 156, 23, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='111111'), 'AirPods Pro 2 降噪蓝牙耳机 二代 九成新', 'AirPods Pro 第二代 主动降噪 通透模式 九成新 续航正常 配原装充电盒 适合自习室降噪学习', 900.00, 2, 'ON_SHELF', 89, 15, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='111111'), '罗技机械键盘 茶轴 87键 八成新 打字舒适', '罗技机械键盘 茶轴 87键紧凑布局 八成新 键帽轻微使用痕迹 手感舒适 适合宿舍打字写论文', 220.00, 2, 'ON_SHELF', 45, 8, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='111111'), '戴尔24寸显示器 1080P IPS 七成新 适合宿舍外接', '戴尔24寸 IPS 1080P 七成新 边框轻微磕碰 画质清晰 可作笔记本外接屏 双屏写代码', 380.00, 2, 'ON_SHELF', 67, 5, '七成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='111111'), 'Kindle Paperwhite 4 代电子书阅读器 八成新 32G', 'Kindle Paperwhite 第四代 32G 八成新 背光正常 适合看电子书小说考研资料 护眼不伤眼', 450.00, 2, 'ON_SHELF', 78, 12, '八成新', NOW(), NOW()),

-- ---------- 考研张(2024001) - 考研党 - 学习资料(5) ----------
((SELECT id FROM user WHERE student_no='2024001'), '考研数学李永乐复习全书 2025版 几乎全新', '李永乐考研数学复习全书 2025版 数学一 几乎全新 无笔记 覆盖高数线代概率 考研必备', 35.00, 5, 'ON_SHELF', 234, 41, '几乎全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024001'), '考研英语历年真题详解 2015-2024 十年真题 几乎全新', '考研英语一历年真题详解 2015-2024十年合集 几乎全新 解析详细 阅读完形翻译全覆盖', 30.00, 5, 'ON_SHELF', 189, 33, '几乎全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024001'), '肖秀荣政治考点背诵手册 2025版 九成新 重点标注清晰', '肖秀荣2025考点背诵手册 九成新 重点已用荧光笔标注 马原毛中特史纲思修全覆盖 政治背诵利器', 20.00, 5, 'ON_SHELF', 145, 28, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024001'), '数据结构王道考研笔记 手写整理 重点突出', '408数据结构王道考研笔记 手写整理 重点难点标注清晰 真题分类整理 适合跨考复习', 15.00, 5, 'ON_SHELF', 56, 9, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024001'), '张宇考研数学1000题 八成新 部分习题已做', '张宇考研数学1000题 数学一 八成新 前半部分习题已做有笔记 后半部分空白 适合二刷', 25.00, 5, 'ON_SHELF', 98, 14, '八成新', NOW(), NOW()),

-- ---------- 运动王(2024002) - 运动达人 - 衣物(3)+其他(6) ----------
((SELECT id FROM user WHERE student_no='2024002'), '捷安特山地自行车 26寸 21速 八成新 校区代步', '捷安特ATX 26寸山地车 21速变速 八成新 刹车灵敏 校区内代步神器 可骑行测试', 350.00, 6, 'ON_SHELF', 112, 19, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024002'), '专业滑板 双翘板 八成新 刷街利器', '专业双翘滑板 八成新 轴承顺滑 板面防滑 适合校园刷街代步 新手进阶适用', 120.00, 6, 'ON_SHELF', 34, 6, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024002'), '耐克篮球鞋 42码 九成新 实战鞋 抓地好', 'Nike篮球鞋 42码 九成新 实战鞋 抓地力强 鞋底磨损轻微 适合外场实战', 280.00, 3, 'ON_SHELF', 67, 11, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024002'), '阿迪达斯运动短裤 M码 全新带吊牌 夏季训练', 'Adidas运动短裤 M码 全新带吊牌 速干面料 夏季跑步训练健身适用', 60.00, 3, 'ON_SHELF', 23, 4, '全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024002'), 'TPE瑜伽垫 6mm 加厚 紫色 八成新 防滑', 'TPE瑜伽垫 6mm加厚 紫色 八成新 双面防滑 回弹性好 适合瑜伽拉伸宿舍健身', 45.00, 3, 'ON_SHELF', 29, 5, '八成新', NOW(), NOW()),

-- ---------- 生活赵(2024003) - 生活家 - 生活用品(4) ----------
((SELECT id FROM user WHERE student_no='2024003'), 'LED护眼台灯 可调光 三档 三级能效 九成新', 'LED护眼台灯 三档调光 三级能效 九成新 无频闪 适合宿舍学习看书护眼', 55.00, 4, 'ON_SHELF', 78, 13, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024003'), '小熊电热水壶 1.5L 自动断电 八成新 宿舍用', '小熊电热水壶 1.5L容量 自动断电防干烧 八成新 烧水快 宿舍泡面喝茶必备', 40.00, 4, 'ON_SHELF', 56, 8, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024003'), '桌面收纳盒三层 抽屉式 透明 九成新 整理桌面', '桌面收纳盒三层抽屉式 透明 九成新 收纳文具化妆品杂物 整理宿舍桌面', 25.00, 4, 'ON_SHELF', 34, 6, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024003'), 'USB小风扇 静音三档 夹扇 七成新 夏季降温', 'USB夹扇小风扇 静音三档调速 七成新 可夹床头桌边 宿舍夏季降温必备', 20.00, 4, 'ON_SHELF', 45, 7, '七成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024003'), '折叠床上书桌 竹制 可调节 八成新 躺着看书', '折叠床上书桌 竹制可调节高度 八成新 稳固承重好 躺床上看书写字用电脑', 35.00, 4, 'ON_SHELF', 38, 5, '八成新', NOW(), NOW()),

-- ---------- 毕业陈(2024004) - 毕业清仓 - 混合品类 ----------
((SELECT id FROM user WHERE student_no='2024004'), '双肩书包 电脑包 15寸 九成新 防水大容量', '双肩电脑书包 15寸笔记本仓 九成新 防水面料 大容量多隔层 适合通勤上课', 50.00, 6, 'ON_SHELF', 89, 14, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024004'), '膳魔师保温杯 350ml 八成新 不锈钢', '膳魔师保温杯 350ml 不锈钢 八成新 保温6小时 适合带水上课泡枸杞', 45.00, 4, 'ON_SHELF', 34, 5, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024004'), '自动折叠雨伞 防风 全新带套 晴雨两用', '自动折叠雨伞 防风骨架 全新带套 晴雨两用 一键开合 校园通勤必备', 25.00, 4, 'ON_SHELF', 23, 4, '全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024004'), '卡西欧计算器 fx-991CN 几乎全新 考试允许', '卡西欧fx-991CN计算器 几乎全新 考试允许型号 科学计算 函数运算 考研专业课必备', 60.00, 2, 'ON_SHELF', 67, 10, '几乎全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024004'), '闪迪移动硬盘 1TB USB3.0 八成新 存资料备份', '闪迪移动硬盘 1TB USB3.0 八成新 读写正常 存考研资料课程视频备份', 180.00, 2, 'ON_SHELF', 56, 9, '八成新', NOW(), NOW()),

-- ---------- 书虫孙(2024005) - 读书人 - 书本(1) ----------
((SELECT id FROM user WHERE student_no='2024005'), '同济高等数学第七版 上册 几乎全新 教材', '同济大学高等数学第七版上册 几乎全新 无笔记 微积分基础教材 高数必学', 20.00, 1, 'ON_SHELF', 145, 22, '几乎全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024005'), '牛津高阶英汉双解词典 第10版 九成新 厚重', '牛津高阶英汉双解词典第10版 九成新 释义详尽 例句丰富 英语学习查词必备', 80.00, 1, 'ON_SHELF', 89, 15, '九成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024005'), '百年孤独 马尔克斯 全新未拆封 长篇小说', '马尔克斯百年孤独 全新未拆封 范晔译本 魔幻现实主义经典名著 课外阅读', 30.00, 1, 'ON_SHELF', 67, 11, '全新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024005'), '考研数学习题集 张宇1000题 八成新 有笔记', '张宇考研数学1000题 八成新 有手写笔记 习题分类详细 适合考研数学刷题', 25.00, 1, 'ON_SHELF', 98, 16, '八成新', NOW(), NOW()),
((SELECT id FROM user WHERE student_no='2024005'), '计算机网络自顶向下 第7版 九成新 课本教材', '计算机网络自顶向下方法第7版 九成新 无笔记 经典教材 适合计网课程学习', 45.00, 1, 'ON_SHELF', 78, 12, '九成新', NOW(), NOW());

-- ============================================================
-- 第三部分: 商品图片(每件 1 张, picsum 占位图, seed 固定图片不变)
-- product_id 用子查询通过 title 关联
-- ============================================================
INSERT INTO product_image (product_id, image_url, sort_order) VALUES
((SELECT id FROM product WHERE title='iPad Air 2022 九成新 考研网课配套 平板电脑 32G 白色' LIMIT 1),          'https://picsum.photos/seed/ipad001/600/400', 0),
((SELECT id FROM product WHERE title='AirPods Pro 2 降噪蓝牙耳机 二代 九成新' LIMIT 1),                       'https://picsum.photos/seed/airpods01/600/400', 0),
((SELECT id FROM product WHERE title='罗技机械键盘 茶轴 87键 八成新 打字舒适' LIMIT 1),                       'https://picsum.photos/seed/keyboard01/600/400', 0),
((SELECT id FROM product WHERE title='戴尔24寸显示器 1080P IPS 七成新 适合宿舍外接' LIMIT 1),                  'https://picsum.photos/seed/monitor01/600/400', 0),
((SELECT id FROM product WHERE title='Kindle Paperwhite 4 代电子书阅读器 八成新 32G' LIMIT 1),                 'https://picsum.photos/seed/kindle01/600/400', 0),
((SELECT id FROM product WHERE title='考研数学李永乐复习全书 2025版 几乎全新' LIMIT 1),                        'https://picsum.photos/seed/math201/600/400', 0),
((SELECT id FROM product WHERE title='考研英语历年真题详解 2015-2024 十年真题 几乎全新' LIMIT 1),                'https://picsum.photos/seed/english201/600/400', 0),
((SELECT id FROM product WHERE title='肖秀荣政治考点背诵手册 2025版 九成新 重点标注清晰' LIMIT 1),              'https://picsum.photos/seed/politics201/600/400', 0),
((SELECT id FROM product WHERE title='数据结构王道考研笔记 手写整理 重点突出' LIMIT 1),                         'https://picsum.photos/seed/datastr201/600/400', 0),
((SELECT id FROM product WHERE title='张宇考研数学1000题 八成新 部分习题已做' LIMIT 1),                        'https://picsum.photos/seed/zhangyu201/600/400', 0),
((SELECT id FROM product WHERE title='捷安特山地自行车 26寸 21速 八成新 校区代步' LIMIT 1),                     'https://picsum.photos/seed/bike202/600/400', 0),
((SELECT id FROM product WHERE title='专业滑板 双翘板 八成新 刷街利器' LIMIT 1),                                'https://picsum.photos/seed/skate202/600/400', 0),
((SELECT id FROM product WHERE title='耐克篮球鞋 42码 九成新 实战鞋 抓地好' LIMIT 1),                            'https://picsum.photos/seed/nike202/600/400', 0),
((SELECT id FROM product WHERE title='阿迪达斯运动短裤 M码 全新带吊牌 夏季训练' LIMIT 1),                       'https://picsum.photos/seed/adidas202/600/400', 0),
((SELECT id FROM product WHERE title='TPE瑜伽垫 6mm 加厚 紫色 八成新 防滑' LIMIT 1),                            'https://picsum.photos/seed/yoga202/600/400', 0),
((SELECT id FROM product WHERE title='LED护眼台灯 可调光 三档 三级能效 九成新' LIMIT 1),                        'https://picsum.photos/seed/lamp203/600/400', 0),
((SELECT id FROM product WHERE title='小熊电热水壶 1.5L 自动断电 八成新 宿舍用' LIMIT 1),                       'https://picsum.photos/seed/kettle203/600/400', 0),
((SELECT id FROM product WHERE title='桌面收纳盒三层 抽屉式 透明 九成新 整理桌面' LIMIT 1),                     'https://picsum.photos/seed/box203/600/400', 0),
((SELECT id FROM product WHERE title='USB小风扇 静音三档 夹扇 七成新 夏季降温' LIMIT 1),                        'https://picsum.photos/seed/fan203/600/400', 0),
((SELECT id FROM product WHERE title='折叠床上书桌 竹制 可调节 八成新 躺着看书' LIMIT 1),                      'https://picsum.photos/seed/desk203/600/400', 0),
((SELECT id FROM product WHERE title='双肩书包 电脑包 15寸 九成新 防水大容量' LIMIT 1),                         'https://picsum.photos/seed/bag204/600/400', 0),
((SELECT id FROM product WHERE title='膳魔师保温杯 350ml 八成新 不锈钢' LIMIT 1),                              'https://picsum.photos/seed/cup204/600/400', 0),
((SELECT id FROM product WHERE title='自动折叠雨伞 防风 全新带套 晴雨两用' LIMIT 1),                            'https://picsum.photos/seed/umbrella204/600/400', 0),
((SELECT id FROM product WHERE title='卡西欧计算器 fx-991CN 几乎全新 考试允许' LIMIT 1),                        'https://picsum.photos/seed/calc204/600/400', 0),
((SELECT id FROM product WHERE title='闪迪移动硬盘 1TB USB3.0 八成新 存资料备份' LIMIT 1),                     'https://picsum.photos/seed/hdd204/600/400', 0),
((SELECT id FROM product WHERE title='同济高等数学第七版 上册 几乎全新 教材' LIMIT 1),                         'https://picsum.photos/seed/math205/600/400', 0),
((SELECT id FROM product WHERE title='牛津高阶英汉双解词典 第10版 九成新 厚重' LIMIT 1),                        'https://picsum.photos/seed/dict205/600/400', 0),
((SELECT id FROM product WHERE title='百年孤独 马尔克斯 全新未拆封 长篇小说' LIMIT 1),                         'https://picsum.photos/seed/novel205/600/400', 0),
((SELECT id FROM product WHERE title='考研数学习题集 张宇1000题 八成新 有笔记' LIMIT 1),                        'https://picsum.photos/seed/mathex205/600/400', 0),
((SELECT id FROM product WHERE title='计算机网络自顶向下 第7版 九成新 课本教材' LIMIT 1),                       'https://picsum.photos/seed/network205/600/400', 0);

-- ============================================================
-- 完成: 5 用户 + 30 商品 + 30 图片
-- 验证: SELECT COUNT(*) FROM user;    -- 应 +5
--       SELECT COUNT(*) FROM product; -- 应 +30
--       SELECT COUNT(*) FROM product_image; -- 应 +30
-- ============================================================
