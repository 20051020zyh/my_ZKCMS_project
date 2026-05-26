-- 生成更多访问日志记录，让趋势图有数据展示
-- 从已发布文章中随机选取 ~2% 生成页面访问记录
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, create_time)
SELECT
    'article',
    a.id,
    CASE WHEN RAND() < 0.7 THEN (SELECT id FROM user ORDER BY RAND() LIMIT 1) ELSE NULL END,
    CONCAT(
        FLOOR(1 + RAND() * 223),
        '.', FLOOR(1 + RAND() * 255),
        '.', FLOOR(1 + RAND() * 255),
        '.', FLOOR(1 + RAND() * 255)
    ),
    DATE_ADD(
        DATE(a.create_time),
        INTERVAL FLOOR(RAND() * 86400) SECOND
    )
FROM article a
WHERE a.state = '已发布'
  AND RAND() < 0.02
LIMIT 50000;
