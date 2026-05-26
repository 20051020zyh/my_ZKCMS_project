SET SESSION cte_max_recursion_depth = 1000000;
SET FOREIGN_KEY_CHECKS = 0;

-- 数字辅助表
DROP TEMPORARY TABLE IF EXISTS num_20000;
CREATE TEMPORARY TABLE num_20000 (n INT PRIMARY KEY);
INSERT INTO num_20000
WITH RECURSIVE cte AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 20000
)
SELECT n FROM cte;

-- ==========================================
-- Step 3: 插入 20000 篇文章
-- ==========================================
INSERT INTO article (
    title, content, cover_img, state, view_count,
    category_id, create_user, create_time, update_time,
    like_count, collect_count, comment_count,
    is_hot, is_best, is_featured, is_delete, status,
    publish_time, seo_title, seo_description, seo_keywords
)
SELECT
    CONCAT('Article_', n),
    CONCAT('Content of article ', n, '. This is a sample article with some interesting content for testing purposes.'),
    '',
    IF(RAND() < 0.9, '已发布', '草稿'),
    FLOOR(1 + RAND() * 10000),
    ELT(1 + FLOOR(RAND() * 4), 2, 3, 4, 8),
    ELT(1 + FLOOR(RAND() * 3), 1, 4, 5 + FLOOR(RAND() * 500)),
    NOW() - INTERVAL FLOOR(1 + RAND() * 60) DAY,
    NOW() - INTERVAL FLOOR(1 + RAND() * 14) DAY,
    0, 0, 0,
    IF(RAND() < 0.15, 1, 0),
    IF(RAND() < 0.1, 1, 0),
    IF(RAND() < 0.05, 1, 0),
    0, 0,
    IF(RAND() < 0.9, NOW() - INTERVAL FLOOR(1 + RAND() * 30) DAY, NULL),
    '', '', ''
FROM num_20000;

-- ==========================================
-- Step 4: 生成文章点赞数据
-- CROSS JOIN + RAND() 随机抽样，~6% → 约 60 万条
-- ==========================================
INSERT INTO article_like (article_id, user_id, create_time)
SELECT a.id, u.id, NOW() - INTERVAL FLOOR(1 + RAND() * 30) DAY
FROM article a
CROSS JOIN user u
WHERE RAND() < 0.06;

-- ==========================================
-- Step 5: 生成文章收藏数据
-- ~2.5% → 约 25 万条
-- ==========================================
INSERT INTO article_collect (article_id, user_id, create_time)
SELECT a.id, u.id, NOW() - INTERVAL FLOOR(1 + RAND() * 30) DAY
FROM article a
CROSS JOIN user u
WHERE RAND() < 0.025;

-- ==========================================
-- Step 6: 生成 50000 条评论
-- ==========================================
DROP TEMPORARY TABLE IF EXISTS num_50000;
CREATE TEMPORARY TABLE num_50000 (n INT PRIMARY KEY);
INSERT INTO num_50000
WITH RECURSIVE cte AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 50000
)
SELECT n FROM cte;

INSERT INTO article_comment (article_id, user_id, content, create_time, is_delete, like_count, audit_status)
SELECT
    1 + FLOOR(RAND() * 20000),
    ELT(1 + FLOOR(RAND() * 3), 1, 4, 5 + FLOOR(RAND() * 500)),
    ELT(1 + FLOOR(RAND() * 10),
        '好文章，学习了！',
        '写的很棒，支持！',
        '非常有帮助，感谢分享',
        '收藏了，慢慢看',
        '这个观点很新颖',
        '分析得很透彻',
        '涨知识了，赞一个',
        '写得不错，期待更多',
        '路过，留个脚印',
        '分享得真好，已推荐'
    ),
    NOW() - INTERVAL FLOOR(1 + RAND() * 20) DAY,
    0,
    FLOOR(RAND() * 10),
    2
FROM num_50000;

-- ==========================================
-- Step 7: 更新文章表的计数（从真实关联表统计）
-- ==========================================
UPDATE article a
LEFT JOIN (
    SELECT article_id, COUNT(*) AS cnt
    FROM article_like
    GROUP BY article_id
) l ON a.id = l.article_id
LEFT JOIN (
    SELECT article_id, COUNT(*) AS cnt
    FROM article_collect
    GROUP BY article_id
) c ON a.id = c.article_id
LEFT JOIN (
    SELECT article_id, COUNT(*) AS cnt
    FROM article_comment
    WHERE is_delete = 0
    GROUP BY article_id
) cm ON a.id = cm.article_id
SET
    a.like_count = COALESCE(l.cnt, 0),
    a.collect_count = COALESCE(c.cnt, 0),
    a.comment_count = COALESCE(cm.cnt, 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 验证
-- ==========================================
SELECT '===== 验证结果 =====' AS '';

SELECT 'user' AS table_name, COUNT(*) AS record_count FROM user
UNION ALL
SELECT CONCAT('article (likes=', SUM(like_count), ', collects=', SUM(collect_count), ', comments=', SUM(comment_count), ')'),
       COUNT(*)
FROM article
UNION ALL
SELECT 'article_like', COUNT(*) FROM article_like
UNION ALL
SELECT 'article_collect', COUNT(*) FROM article_collect
UNION ALL
SELECT 'article_comment', COUNT(*) FROM article_comment;
