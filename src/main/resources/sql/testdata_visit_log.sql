-- ============================================================
-- visit_log 测试数据
-- 包含真实 User-Agent 字符串，可直接测试聚合流程
-- 执行后调用 POST /statistics/aggregate 触发聚合
-- ============================================================

-- Chrome / Windows
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 1, 1, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 08:30:00'),
('article', 1, 2, '192.168.1.101', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 08:32:00'),
('article', 2, 3, '192.168.1.102', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36', '2026-06-01 08:35:00'),
('article', 1, NULL, '10.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 09:00:00'),
('article', 3, 4, '192.168.1.103', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 09:15:00'),
('article', 2, NULL, '10.0.0.2', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 09:30:00'),
('article', 1, 5, '192.168.1.104', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36', '2026-06-01 10:00:00'),
('article', 4, 6, '192.168.1.105', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 10:30:00'),
('article', 1, NULL, '10.0.0.3', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36', '2026-06-01 11:00:00'),
('article', 3, 7, '192.168.1.106', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 11:30:00'),
('article', 2, NULL, '10.0.0.4', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 12:00:00'),
('article', 5, 8, '192.168.1.107', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 14:00:00'),
('article', 1, NULL, '10.0.0.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 15:00:00'),
('article', 4, 9, '192.168.1.108', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 16:00:00'),
('article', 1, 10, '192.168.1.109', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36', '2026-06-01 18:00:00');

-- Chrome / macOS
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 2, 11, '172.16.0.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 08:45:00'),
('article', 3, 12, '172.16.0.2', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 09:20:00'),
('article', 1, 13, '172.16.0.3', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36', '2026-06-01 10:15:00'),
('article', 5, NULL, '172.16.0.4', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 11:45:00'),
('article', 2, 14, '172.16.0.5', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 13:30:00'),
('article', 4, NULL, '172.16.0.6', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36', '2026-06-01 15:20:00');

-- Safari / iOS
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 1, 15, '192.168.2.100', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1', '2026-06-01 09:00:00'),
('article', 3, 16, '192.168.2.101', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1', '2026-06-01 09:30:00'),
('article', 2, NULL, '192.168.2.102', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1', '2026-06-01 10:00:00'),
('article', 5, 17, '192.168.2.103', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', '2026-06-01 11:00:00'),
('article', 1, 18, '192.168.2.104', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1', '2026-06-01 12:30:00'),
('article', 4, NULL, '192.168.2.105', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1', '2026-06-01 14:00:00'),
('article', 3, 19, '192.168.2.106', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1', '2026-06-01 15:30:00'),
('article', 2, NULL, '192.168.2.107', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1', '2026-06-01 17:00:00');

-- Safari / macOS
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 1, 20, '172.16.1.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15', '2026-06-01 08:50:00'),
('article', 3, NULL, '172.16.1.2', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15', '2026-06-01 10:30:00'),
('article', 5, 21, '172.16.1.3', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15', '2026-06-01 12:00:00'),
('article', 2, 22, '172.16.1.4', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15', '2026-06-01 14:30:00');

-- Edge / Windows
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 4, 23, '10.10.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0', '2026-06-01 09:15:00'),
('article', 1, 24, '10.10.0.2', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/129.0.0.0', '2026-06-01 10:45:00'),
('article', 3, NULL, '10.10.0.3', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0', '2026-06-01 12:15:00'),
('article', 2, 25, '10.10.0.4', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0', '2026-06-01 14:00:00'),
('article', 5, NULL, '10.10.0.5', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0', '2026-06-01 16:30:00');

-- Firefox / Windows
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 2, 26, '192.168.3.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0', '2026-06-01 09:30:00'),
('article', 4, NULL, '192.168.3.2', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0', '2026-06-01 11:00:00'),
('article', 1, 27, '192.168.3.3', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0', '2026-06-01 13:00:00'),
('article', 3, 28, '192.168.3.4', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0', '2026-06-01 15:30:00');

-- 微信内置 / iOS
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 1, 29, '192.168.4.1', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/9.0.0', '2026-06-01 10:00:00'),
('article', 5, 30, '192.168.4.2', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.9.0', '2026-06-01 11:30:00'),
('article', 3, NULL, '192.168.4.3', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/9.0.1', '2026-06-01 14:00:00'),
('article', 2, 31, '192.168.4.4', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/9.0.0', '2026-06-01 16:00:00');

-- 微信内置 / Android
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 4, 32, '192.168.4.5', 'Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36 MicroMessenger/9.0.0', '2026-06-01 10:20:00'),
('article', 1, NULL, '192.168.4.6', 'Mozilla/5.0 (Linux; Android 14; Xiaomi 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/129.0.0.0 Mobile Safari/537.36 MicroMessenger/8.9.0', '2026-06-01 12:45:00'),
('article', 5, 33, '192.168.4.7', 'Mozilla/5.0 (Linux; Android 15; Samsung Galaxy S25) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36 MicroMessenger/9.0.0', '2026-06-01 15:00:00');

-- Chrome / Android 移动端
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 2, 34, '192.168.5.1', 'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6728.58 Mobile Safari/537.36', '2026-06-01 08:30:00'),
('article', 3, 35, '192.168.5.2', 'Mozilla/5.0 (Linux; Android 14; OnePlus 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6728.59 Mobile Safari/537.36', '2026-06-01 09:45:00'),
('article', 1, NULL, '192.168.5.3', 'Mozilla/5.0 (Linux; Android 14; Samsung Galaxy S24) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.100 Mobile Safari/537.36', '2026-06-01 11:15:00'),
('article', 4, 36, '192.168.5.4', 'Mozilla/5.0 (Linux; Android 15; Xiaomi 15 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6728.58 Mobile Safari/537.36', '2026-06-01 13:30:00'),
('article', 5, NULL, '192.168.5.5', 'Mozilla/5.0 (Linux; Android 13; Huawei P60) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.88 Mobile Safari/537.36', '2026-06-01 15:45:00'),
('article', 2, 37, '192.168.5.6', 'Mozilla/5.0 (Linux; Android 15; OPPO Find X8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6728.58 Mobile Safari/537.36', '2026-06-01 17:30:00');

-- Chrome / Linux
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 3, 38, '172.20.0.1', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36', '2026-06-01 10:30:00'),
('article', 1, 39, '172.20.0.2', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36', '2026-06-01 14:15:00');

-- Opera / Windows
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 5, 40, '10.20.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0', '2026-06-01 11:30:00'),
('article', 4, NULL, '10.20.0.2', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 OPR/114.0.0.0', '2026-06-01 16:00:00');

-- QQ浏览器 / Android
INSERT INTO visit_log (biz_type, biz_id, user_id, ip, user_agent, create_time) VALUES
('article', 2, 41, '192.168.6.1', 'Mozilla/5.0 (Linux; Android 15; Xiaomi 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36 V1_AND_SQ_9.0.0_0_YYB_A QQ/9.0.0', '2026-06-01 12:00:00'),
('article', 1, NULL, '192.168.6.2', 'Mozilla/5.0 (Linux; Android 14; vivo X200) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36 V1_AND_SQ_8.9.0_0_YYB_A QQ/8.9.0', '2026-06-01 15:00:00');
