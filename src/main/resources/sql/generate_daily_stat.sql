-- 清理已存在的统计记录
-- DELETE FROM daily_stat;

-- Step 1: 从 visit_log 生成每日 PV/UV/IP 统计
INSERT INTO daily_stat (stat_date, pv, uv, ip_count)
SELECT DATE(create_time), COUNT(*), COUNT(DISTINCT user_id), COUNT(DISTINCT ip)
FROM visit_log
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE pv = VALUES(pv), uv = VALUES(uv), ip_count = VALUES(ip_count);

-- Step 2: 从 article 表补充发文数量（已发布）
INSERT INTO daily_stat (stat_date, publish_count)
SELECT DATE(create_time), COUNT(*)
FROM article
WHERE state = '已发布'
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE publish_count = VALUES(publish_count);

-- Step 3: 所有记录确保非 NULL
UPDATE daily_stat SET pv = COALESCE(pv, 0), uv = COALESCE(uv, 0), ip_count = COALESCE(ip_count, 0), publish_count = COALESCE(publish_count, 0);

-- Step 4: 补齐没有 visit_log 但有文章发布的日期的 pv/uv/ip_count
INSERT IGNORE INTO daily_stat (stat_date, pv, uv, ip_count)
SELECT d.date, 0, 0, 0
FROM (
    SELECT DISTINCT DATE(create_time) AS date FROM article WHERE state = '已发布'
) d
WHERE d.date NOT IN (SELECT stat_date FROM daily_stat);