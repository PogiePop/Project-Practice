-- =====================================================
-- 审计信息管理系统 - 数据库初始化脚本 V2.1
-- 数据库: project_practice (utf8mb4)
-- =====================================================

CREATE DATABASE IF NOT EXISTS project_practice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE project_practice;

-- ==================== 审计计划批次 ====================
DROP TABLE IF EXISTS audit_plan_batch;
CREATE TABLE audit_plan_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL UNIQUE COMMENT '计划编号',
    batch_name VARCHAR(200) NOT NULL COMMENT '计划名称',
    plan_type INT DEFAULT 0 COMMENT '计划类型:0-经责/1-财务收支/2-专项/3-工程',
    year INT COMMENT '年度',
    project_count INT DEFAULT 1 COMMENT '项目数',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    approval_status INT DEFAULT 3 COMMENT '审批状态:0-已审批/1-审批中/2-已归档/3-草稿/4-已驳回',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    is_outsource INT DEFAULT 0 COMMENT '是否委托中介',
    audit_leader VARCHAR(50) COMMENT '审计组长',
    audit_leader_id VARCHAR(32) COMMENT '审计组长ID',
    unit_id VARCHAR(32) COMMENT '关联被审计单位',
    leader_id VARCHAR(32) COMMENT '关联领导干部',
    project_amount DECIMAL(15,2) DEFAULT 0 COMMENT '项目金额(万)',
    actual_amount DECIMAL(15,2) DEFAULT 0 COMMENT '实际使用金额(万)',
    fund_source VARCHAR(100) COMMENT '资金来源',
    audit_conclusion VARCHAR(500) COMMENT '审计结论',
    findings VARCHAR(2000) COMMENT '审计发现',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 被审计单位 ====================
DROP TABLE IF EXISTS audit_object_unit;
CREATE TABLE audit_object_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id VARCHAR(32) NOT NULL UNIQUE COMMENT '单位ID',
    unit_code VARCHAR(32) COMMENT '单位代码',
    unit_name VARCHAR(200) NOT NULL COMMENT '单位名称',
    category INT DEFAULT 0 COMMENT '分类',
    category_name VARCHAR(50) COMMENT '分类名称',
    establishment_count INT DEFAULT 0 COMMENT '编制人数',
    fund_scale DECIMAL(15,2) DEFAULT 0 COMMENT '经费规模(万)',
    leader_in_charge VARCHAR(50) COMMENT '分管校领导',
    leader_in_charge_phone VARCHAR(20) COMMENT '分管领导电话',
    finance_contact VARCHAR(50) COMMENT '财务联系人',
    finance_contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    setup_date DATE COMMENT '设立日期',
    total_audit_count INT DEFAULT 0 COMMENT '审计次数',
    latest_audit_date DATE COMMENT '最近审计日期',
    pending_rectify_count INT DEFAULT 0 COMMENT '待整改数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 经责领导干部 ====================
DROP TABLE IF EXISTS audit_object_leader;
CREATE TABLE audit_object_leader (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leader_id VARCHAR(32) NOT NULL UNIQUE COMMENT '干部ID',
    leader_code VARCHAR(32) COMMENT '干部代码',
    leader_name VARCHAR(50) NOT NULL COMMENT '姓名',
    staff_id VARCHAR(32) COMMENT '工号',
    current_unit_name VARCHAR(200) COMMENT '所属单位',
    current_position VARCHAR(100) COMMENT '现任职务',
    phone VARCHAR(20) COMMENT '联系电话',
    is_active INT DEFAULT 1 COMMENT '是否在职',
    tenure_start_date DATE COMMENT '任职起始',
    tenure_years DECIMAL(4,1) DEFAULT 0 COMMENT '任职年限',
    fund_scope DECIMAL(15,2) DEFAULT 0 COMMENT '分管资金(万)',
    audit_count INT DEFAULT 0 COMMENT '审计次数',
    latest_audit_date DATE COMMENT '最近审计日期',
    latest_audit_conclusion VARCHAR(500) COMMENT '最近审计结论',
    pending_rectify_count INT DEFAULT 0 COMMENT '未整改数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 任职履历 ====================
DROP TABLE IF EXISTS leader_career_history;
CREATE TABLE leader_career_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(32) NOT NULL UNIQUE,
    leader_id VARCHAR(32) NOT NULL,
    unit_name VARCHAR(200),
    position VARCHAR(100),
    start_date DATE,
    end_date DATE,
    duty_description VARCHAR(500),
    fund_scope DECIMAL(15,2) DEFAULT 0,
    source VARCHAR(50) DEFAULT '手动录入',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 附件 ====================
DROP TABLE IF EXISTS audit_attachment;
CREATE TABLE audit_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attach_id VARCHAR(32) NOT NULL UNIQUE,
    batch_id VARCHAR(32) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT DEFAULT 0,
    file_type VARCHAR(100),
    attach_type VARCHAR(50),
    attach_type_name VARCHAR(50),
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    upload_by VARCHAR(50),
    file_path VARCHAR(500)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 方案模板 ====================
DROP TABLE IF EXISTS audit_template;
CREATE TABLE audit_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id VARCHAR(32) NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    plan_type INT DEFAULT 0,
    version VARCHAR(20) DEFAULT 'V1.0',
    description VARCHAR(500),
    content TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS batch_template;
CREATE TABLE batch_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL,
    template_id VARCHAR(32) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_template (batch_id, template_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 审批流 ====================
DROP TABLE IF EXISTS approval_step;
CREATE TABLE approval_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL,
    step_order INT,
    step_name VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    approver_name VARCHAR(50),
    comment VARCHAR(500),
    operate_time DATETIME
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS approval_history;
CREATE TABLE approval_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    approval_id VARCHAR(32),
    batch_id VARCHAR(32),
    flow_type VARCHAR(50),
    status VARCHAR(20),
    submit_by VARCHAR(50),
    submit_time DATETIME,
    result VARCHAR(20)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS plan_change;
CREATE TABLE plan_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_id VARCHAR(32),
    batch_id VARCHAR(32),
    change_type INT,
    change_type_name VARCHAR(50),
    reason VARCHAR(500),
    change_data TEXT COMMENT '变更数据JSON快照',
    approval_status INT DEFAULT 1,
    approval_status_name VARCHAR(20),
    apply_time DATETIME
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 整改台账 ====================
DROP TABLE IF EXISTS audit_rectify_ledger;
CREATE TABLE audit_rectify_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rectify_id VARCHAR(32) NOT NULL UNIQUE,
    batch_id VARCHAR(32) NOT NULL,
    unit_id VARCHAR(32),
    leader_id VARCHAR(32),
    issue_description VARCHAR(500) NOT NULL,
    issue_category VARCHAR(50) DEFAULT '财务管理',
    rectify_status INT DEFAULT 0 COMMENT '0未整改/1整改中/2已整改',
    responsible_person VARCHAR(50),
    deadline DATE,
    rectify_progress VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 系统用户 ====================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    staff_id VARCHAR(32),
    department VARCHAR(100),
    position VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'AUDITOR',
    role_level INT DEFAULT 1 COMMENT '0=超级管理员 1=审计员',
    avatar VARCHAR(200),
    last_login DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 系统设置 ====================
DROP TABLE IF EXISTS sys_settings;
CREATE TABLE sys_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_key (user_id, setting_key)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 系统消息 ====================
DROP TABLE IF EXISTS sys_message;
CREATE TABLE sys_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(32),
    title VARCHAR(200),
    content VARCHAR(1000),
    message_type VARCHAR(20),
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 种子数据
-- =====================================================

INSERT INTO sys_user (username, password, real_name, staff_id, department, position, phone, email, role, role_level) VALUES
('admin', '123456', '陈处长', 'T2021001', '审计处', '处长', '13800000000', 'chen@school.edu.cn', 'DIRECTOR', 0),
('zhangsan', '123456', '张三', 'T2019001', '审计处', '审计组长', '13800000001', 'zhang@school.edu.cn', 'AUDIT_LEADER', 1),
('lisi', '123456', '李四', 'T2019002', '审计处', '审计组长', '13800000002', 'li@school.edu.cn', 'AUDIT_LEADER', 1);

INSERT INTO audit_object_unit (unit_id, unit_code, unit_name, category, category_name, establishment_count, fund_scale, leader_in_charge, finance_contact, finance_contact_phone) VALUES
('UNIT001', 'DW-2020-001', 'XX学院', 1, '二级学院', 85, 5000, '张XX', '李XX', '13800000002'),
('UNIT002', 'DW-2020-002', 'YY学院', 1, '二级学院', 62, 3500, '王XX', '赵XX', '13800000004'),
('UNIT010', 'DW-2018-010', '财务处', 0, '校内职能部门', 15, 8000, '张XX', '李XX', '13800000002'),
('UNIT020', 'DW-2019-020', '校办产业集团', 3, '校办企业', 120, 20000, '陈处长', '王XX', '13800000003');

INSERT INTO audit_object_leader (leader_id, leader_code, leader_name, staff_id, current_unit_name, current_position, phone, is_active, tenure_start_date, tenure_years, fund_scope) VALUES
('LDR001', 'LD-2018-001', '张XX', 'T2020001', 'XX学院', '院长', '13800000001', 1, '2022-03-01', 4.3, 5000),
('LDR002', 'LD-2019-002', '李XX', 'T2020002', 'XX学院', '副院长', '13800000002', 1, '2023-06-01', 3.1, 3000),
('LDR005', 'LD-2017-005', '王XX', 'T2017010', '财务处', '处长', '13800000003', 1, '2019-01-01', 7.5, 8000),
('LDR008', 'LD-2016-008', '赵XX', 'T2016020', '已退休', '原院长', '13800000004', 0, '2016-01-01', 8.0, 6000);

INSERT INTO audit_plan_batch (batch_id, batch_name, plan_type, year, project_count, start_date, end_date, approval_status, progress, unit_id, leader_id, project_amount) VALUES
('JH2026001', '2026年度XX学院经济责任审计', 0, 2026, 8, '2026-01-01', '2026-06-30', 0, 100, 'UNIT001', 'LDR001', 500),
('JH2026002', '集团总部财务收支常规审计', 1, 2026, 12, '2026-02-01', '2026-08-31', 1, 45, 'UNIT002', NULL, 300),
('JH2026003', '子公司专项资金专项审计', 2, 2026, 5, '2026-03-01', '2026-09-30', 2, 100, 'UNIT010', 'LDR005', 200),
('JH2026004', '产业园新建工程项目审计', 3, 2026, 3, '2026-04-01', '2026-12-31', 3, 10, 'UNIT020', NULL, 800);

INSERT INTO approval_step (batch_id, step_order, step_name, status, approver_name, comment, operate_time) VALUES
('JH2026001', 1, '提交', 'COMPLETED', '李四', '提交2026年度经责审计计划', NOW()),
('JH2026001', 2, '组长审核', 'COMPLETED', '张三', '同意，请处长审批', NOW()),
('JH2026001', 3, '处长审批', 'ACTIVE', NULL, NULL, NULL),
('JH2026001', 4, '校领导审批', 'PENDING', NULL, NULL, NULL),
('JH2026001', 5, '归档', 'PENDING', NULL, NULL, NULL);

INSERT INTO audit_template (template_id, template_name, plan_type, version, description, content) VALUES
('TPL001', '经济责任审计标准化方案', 0, 'V2.1', '适用于校内中层领导干部经济责任审计', '一、审计目标\n二、审计范围\n三、审计程序'),
('TPL002', '财务收支审计标准化方案', 1, 'V1.5', '适用于校内各单位财务收支常规审计', '一、审计目标\n二、审计范围\n三、审计程序'),
('TPL003', '专项审计标准化方案', 2, 'V1.0', '适用于各类专项资金审计', '一、审计目标\n二、审计范围\n三、审计程序'),
('TPL004', '工程审计标准化方案', 3, 'V2.0', '适用于基建工程项目审计', '一、审计目标\n二、审计范围\n三、审计程序');

INSERT INTO batch_template (batch_id, template_id) VALUES ('JH2026001','TPL001'),('JH2026002','TPL002'),('JH2026003','TPL003'),('JH2026004','TPL004');

INSERT INTO sys_settings (user_id, setting_key, setting_value) VALUES
('admin', 'notify', '{"alertPush":true,"approvalNotify":true,"syncNotify":false,"emailNotify":false,"soundOn":true}'),
('admin', 'ui', '{"theme":"light","tableDensity":"medium","pageSize":20,"showBreadcrumb":true,"autoRefresh":true,"refreshInterval":60}');
