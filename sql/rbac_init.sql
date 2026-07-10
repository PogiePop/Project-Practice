-- =====================================================
-- RBAC权限管理系统 - 数据库初始化脚本
-- 依赖: sql/init.sql 已执行（sys_user表已存在）
-- =====================================================
USE project_practice;

-- 1. 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识',
    status TINYINT DEFAULT 1 COMMENT '状态:0-禁用 1-启用',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 用户-角色中间表 (user_id关联sys_user.id)
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    perm_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID(0=顶级)',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    perms VARCHAR(100) COMMENT '权限标识符(如audit:plan:list)',
    type TINYINT DEFAULT 1 COMMENT '类型:0-目录 1-菜单 2-按钮 3-接口',
    path VARCHAR(200) COMMENT '路由地址',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态:0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 角色-权限中间表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    perm_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, perm_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 种子数据 ====================

-- 角色
INSERT INTO sys_role (role_id, role_name, role_key, status, remark) VALUES
(1, '超级管理员', 'super_admin', 1, '系统最高权限，拥有全部功能'),
(2, '审计处长',   'director',    1, '审批+查看全部数据'),
(3, '审计组长',   'audit_leader', 1, '管理本人负责的审计计划'),
(4, '普通审计员', 'auditor',      1, '查看权限');

-- 权限（树形结构：parent_id=0为目录，type=1为菜单，type=2为按钮/操作）
INSERT INTO sys_permission (perm_id, parent_id, perm_name, perms, type, sort_order) VALUES
-- 计划管理
(1,  0,  '计划管理',     NULL,                 0, 1),
(2,  1,  '计划列表',     'audit:plan:list',    1, 1),
(3,  1,  '新建计划',     'audit:plan:add',     2, 2),
(4,  1,  '编辑计划',     'audit:plan:edit',    2, 3),
(5,  1,  '删除计划',     'audit:plan:delete',  2, 4),
(6,  1,  '审批操作',     'audit:plan:approve', 2, 5),
(7,  1,  '计划变更',     'audit:plan:change',  2, 6),
(8,  1,  '导出计划',     'audit:plan:export',  2, 7),
-- 审计对象
(9,  0,  '审计对象',     NULL,                   0, 2),
(10, 9,  '查看对象',     'audit:object:list',    1, 1),
(11, 9,  '新增对象',     'audit:object:add',     2, 2),
(12, 9,  '编辑对象',     'audit:object:edit',    2, 3),
(13, 9,  '删除对象',     'audit:object:delete',  2, 4),
-- 统计分析
(14, 0,  '统计分析',     NULL,                    0, 3),
(15, 14, '查看统计',     'audit:statistic:view',  1, 1),
-- 系统管理
(16, 0,  '系统管理',     NULL,                    0, 4),
(17, 16, '角色管理',     'audit:sys:role',        1, 1),
(18, 16, '权限分配',     'audit:sys:perm',        1, 2),
(19, 16, '用户管理',     'audit:sys:user',        1, 3),
(20, 16, '系统设置',     'audit:sys:settings',    1, 4),
-- 附件
(21, 0,  '附件管理',     NULL,                   0, 5),
(22, 21, '上传附件',     'audit:attach:upload',  2, 1),
(23, 21, '删除附件',     'audit:attach:delete',  2, 2);

-- ==================== 角色-权限分配 ====================

-- 超级管理员：所有权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, perm_id FROM sys_permission WHERE perms IS NOT NULL;

-- 审计处长：查看+审批+系统设置+附件管理（不含删除计划）
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 2, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:plan:add','audit:plan:edit','audit:plan:approve',
    'audit:plan:change','audit:plan:export',
    'audit:object:list','audit:statistic:view',
    'audit:sys:settings','audit:attach:upload','audit:attach:delete'
);

-- 审计组长：查看+新建编辑+导出+上传
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 3, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:plan:add','audit:plan:edit','audit:plan:export',
    'audit:object:list','audit:statistic:view',
    'audit:attach:upload'
);

-- 普通审计员：只读查看
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 4, perm_id FROM sys_permission WHERE perms IN (
    'audit:plan:list','audit:object:list','audit:statistic:view'
);

-- ==================== 用户-角色分配 ====================
-- sys_user表的种子数据ID: admin=1, zhangsan=2, lisi=3
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin → 超级管理员
(2, 3), -- zhangsan → 审计组长
(3, 4); -- lisi → 普通审计员

-- ==================== 密码说明 ====================
-- 现有密码仍为明文"123456"，首次登录时LoginController会自动识别明文
-- 并升级为BCrypt密文，无需手动转换。
-- 如需手动生成BCrypt密文，可使用以下Java代码：
--   new BCryptPasswordEncoder(10).encode("123456")
-- 然后在MySQL中执行 UPDATE sys_user SET password='{bcrypt_hash}' WHERE username='xxx';

