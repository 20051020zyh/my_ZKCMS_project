-- =====================================================
-- 0. 数据库表结构变更（新增字段和新表）
-- =====================================================

-- user表新增关注数和粉丝数字段
ALTER TABLE user ADD COLUMN follow_count INT DEFAULT 0 COMMENT '关注数';
ALTER TABLE user ADD COLUMN fans_count INT DEFAULT 0 COMMENT '粉丝数';

-- article_collect表新增收藏文件夹ID字段
ALTER TABLE article_collect ADD COLUMN folder_id INT DEFAULT NULL COMMENT '收藏文件夹ID';

-- 用户关注关系表
CREATE TABLE IF NOT EXISTS user_follow (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关注者ID',
    followed_user_id INT NOT NULL COMMENT '被关注者ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    INDEX idx_user_id (user_id),
    INDEX idx_followed_user_id (followed_user_id),
    UNIQUE KEY uk_user_follow (user_id, followed_user_id)
) COMMENT '用户关注关系表';

-- 收藏分类文件夹表
CREATE TABLE IF NOT EXISTS collect_folder (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(50) NOT NULL COMMENT '文件夹名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) COMMENT '收藏分类文件夹表';


-- =====================================================
-- 1. 新增权限记录到 sys_permission 表
-- =====================================================
-- 用户管理 - 新增用户相关权限按钮
INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '用户主页', '/user/profile', '/user/profile', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'user/alluserPage' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/user/profile');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '关注/取关', '/user/follow/toggle', '/user/follow/toggle', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'user/alluserPage' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/user/follow/toggle');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '检查关注', '/user/follow/check', '/user/follow/check', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'user/alluserPage' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/user/follow/check');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '关注列表', '/user/follow/list', '/user/follow/list', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'user/alluserPage' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/user/follow/list');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '粉丝列表', '/user/follow/fans/list', '/user/follow/fans/list', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'user/alluserPage' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/user/follow/fans/list');

-- 文章管理 - 新增收藏相关权限按钮
INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '收藏/取消收藏', '/article/collect/toggle', '/article/collect/toggle', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = '/article/add' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/article/collect/toggle');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '收藏列表', '/article/collect/user/list', '/article/collect/user/list', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = '/article/add' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/article/collect/user/list');

-- 收藏管理 - 新建父菜单（如果不存在）
INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '收藏管理', 'collect:menu', '/collect', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'collect:menu');

-- 收藏管理 - 新增文件夹相关权限按钮
INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '新增文件夹', '/collect/folder/add', '/collect/folder/add', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'collect:menu' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/collect/folder/add');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '修改文件夹', '/collect/folder/update', '/collect/folder/update', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'collect:menu' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/collect/folder/update');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '删除文件夹', '/collect/folder/delete', '/collect/folder/delete', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'collect:menu' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/collect/folder/delete');

INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '文件夹列表', '/collect/folder/list', '/collect/folder/list', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = 'collect:menu' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/collect/folder/list');

-- 移入文件夹权限
INSERT INTO sys_permission (name, permission, path, parent_id, type)
SELECT '移入文件夹', '/article/collect/moveFolder', '/article/collect/moveFolder', t.id, 2
FROM (SELECT id FROM sys_permission WHERE permission = '/article/add' LIMIT 1) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = '/article/collect/moveFolder');


-- =====================================================
-- 将新权限赋值给超级管理员角色(role_code = 'super_admin')
-- =====================================================
-- 先查询管理员角色ID
SET @adminRoleId = (SELECT id FROM sys_role WHERE role_code = 'super_admin' LIMIT 1);

-- 批量插入角色-权限关联（只插入不存在的）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT @adminRoleId, id FROM sys_permission WHERE permission IN (
    '/user/profile',
    '/user/follow/toggle',
    '/user/follow/check',
    '/user/follow/list',
    '/user/follow/fans/list',
    '/article/collect/toggle',
    '/article/collect/user/list',
    '/collect/folder/add',
    '/collect/folder/update',
    '/collect/folder/delete',
    '/collect/folder/list',
    '/article/collect/moveFolder'
) AND id NOT IN (
    SELECT permission_id FROM sys_role_permission WHERE role_id = @adminRoleId
);
