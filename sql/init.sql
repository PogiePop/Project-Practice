CREATE DATABASE IF NOT EXISTS project_practice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE project_practice;

DROP TABLE IF EXISTS audit_plan_batch;
CREATE TABLE audit_plan_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(32) NOT NULL UNIQUE,
    batch_name VARCHAR(200) NOT NULL,
    plan_type INT DEFAULT 0,
    year INT,
    project_count INT DEFAULT 0,
    start_date DATE,
    end_date DATE,
    approval_status INT DEFAULT 3,
    progress INT DEFAULT 0,
    is_outsource INT DEFAULT 0,
    audit_leader VARCHAR(50),
    audit_leader_id VARCHAR(32),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS audit_object_unit;
CREATE TABLE audit_object_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id VARCHAR(32) NOT NULL UNIQUE,
    unit_code VARCHAR(32),
    unit_name VARCHAR(200) NOT NULL,
    category INT DEFAULT 0,
    category_name VARCHAR(50),
    establishment_count INT DEFAULT 0,
    fund_scale DECIMAL(15,2) DEFAULT 0,
    leader_in_charge VARCHAR(50),
    finance_contact VARCHAR(50),
    finance_contact_phone VARCHAR(20),
    address VARCHAR(200),
    setup_date DATE,
    total_audit_count INT DEFAULT 0,
    latest_audit_date DATE,
    pending_rectify_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS audit_object_leader;
CREATE TABLE audit_object_leader (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leader_id VARCHAR(32) NOT NULL UNIQUE,
    leader_code VARCHAR(32),
    leader_name VARCHAR(50) NOT NULL,
    staff_id VARCHAR(32),
    current_unit_name VARCHAR(200),
    current_position VARCHAR(100),
    is_active INT DEFAULT 1,
    tenure_start_date DATE,
    tenure_years DECIMAL(4,1) DEFAULT 0,
    fund_scope DECIMAL(15,2) DEFAULT 0,
    audit_count INT DEFAULT 0,
    latest_audit_date DATE,
    latest_audit_conclusion VARCHAR(500),
    pending_rectify_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

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
);

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
);

DROP TABLE IF EXISTS plan_change;
CREATE TABLE plan_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_id VARCHAR(32),
    batch_id VARCHAR(32),
    change_type INT,
    change_type_name VARCHAR(50),
    reason VARCHAR(500),
    approval_status INT DEFAULT 1,
    approval_status_name VARCHAR(20),
    apply_time DATETIME
);

DROP TABLE IF EXISTS sys_message;
CREATE TABLE sys_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(32),
    title VARCHAR(200),
    content VARCHAR(1000),
    message_type VARCHAR(20),
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO audit_plan_batch (batch_id, batch_name, plan_type, year, project_count, start_date, end_date, approval_status, progress, audit_leader) VALUES
('JH2026001', '2026年度经济责任审计计划', 0, 2026, 8, '2026-01-01', '2026-06-30', 0, 100, '张三'),
('JH2026002', '集团总部财务收支常规审计', 1, 2026, 12, '2026-02-01', '2026-08-31', 1, 45, '李四'),
('JH2026003', '子公司专项资金专项审计方案', 2, 2026, 5, '2026-03-01', '2026-09-30', 2, 100, '王五'),
('JH2026004', '产业园新建工程项目审计', 3, 2026, 3, '2026-04-01', '2026-12-31', 3, 10, '赵六');

INSERT INTO audit_object_unit (unit_id, unit_code, unit_name, category, category_name, establishment_count, fund_scale, leader_in_charge, finance_contact, finance_contact_phone, total_audit_count, latest_audit_date, pending_rectify_count) VALUES
('UNIT001', 'DW-2020-001', 'XX学院', 1, '二级学院', 85, 5000, '校领导A', '张会计', '13800000001', 2, '2023-05-10', 2),
('UNIT002', 'DW-2020-002', 'YY学院', 1, '二级学院', 62, 3500, '校领导B', '李会计', '13800000002', 1, '2024-02-18', 0),
('UNIT010', 'DW-2018-010', '财务处', 0, '校内职能部门', 15, 8000, '校领导A', '王会计', '13800000003', 3, '2025-06-01', 1),
('UNIT020', 'DW-2019-020', '校办产业集团', 3, '校办企业', 120, 20000, '校领导C', '赵会计', '13800000004', 2, '2024-11-20', 3),
('UNIT025', 'DW-2021-025', '基建项目部A', 4, '基建项目部', 8, 15000, '校领导A', '陈工', '13800000005', 0, NULL, 0);

INSERT INTO audit_object_leader (leader_id, leader_code, leader_name, staff_id, current_unit_name, current_position, is_active, tenure_start_date, tenure_years, fund_scope, audit_count, latest_audit_date, latest_audit_conclusion, pending_rectify_count) VALUES
('LDR001', 'LD-2018-001', '张XX', 'T2020001', 'XX学院', '院长', 1, '2022-03-01', 4.3, 5000, 1, '2024-03-10', '履职情况良好，发现一般性问题5项', 0),
('LDR002', 'LD-2019-002', '李XX', 'T2020002', 'YY学院', '副院长', 1, '2023-06-01', 3.1, 3000, 0, NULL, NULL, 0),
('LDR005', 'LD-2017-005', '王XX', 'T2017010', '财务处', '处长', 1, '2019-01-01', 7.5, 8000, 1, '2023-08-15', '发现重大问题3项，需持续整改', 1),
('LDR008', 'LD-2016-008', '赵XX', 'T2016020', '已退休', '原院长', 0, '2016-01-01', 8.0, 6000, 2, '2022-12-20', '离任审计已完成，归档', 0);

INSERT INTO approval_step (batch_id, step_order, step_name, status, approver_name, comment, operate_time) VALUES
('JH2026001', 1, '提交', 'COMPLETED', '李四', '提交2026年度经责审计计划', '2025-12-15 10:30:00'),
('JH2026001', 2, '组长审核', 'COMPLETED', '张三', '同意，请处长审批', '2025-12-16 09:00:00'),
('JH2026001', 3, '处长审批', 'ACTIVE', NULL, NULL, NULL),
('JH2026001', 4, '校领导审批', 'PENDING', NULL, NULL, NULL),
('JH2026001', 5, '归档', 'PENDING', NULL, NULL, NULL);

INSERT INTO approval_history (approval_id, batch_id, flow_type, status, submit_by, submit_time, result) VALUES
('APR2026001', 'JH2026001', 'NEW_PLAN', '进行中', '李四', '2025-12-15 10:30:00', '审批中'),
('APR2025003', 'JH2026001', 'PLAN_CHANGE', '已完成', '赵六', '2025-09-20 14:00:00', '已通过');

INSERT INTO plan_change (change_id, batch_id, change_type, change_type_name, reason, approval_status, approval_status_name, apply_time) VALUES
('CHG2025001', 'JH2026001', 0, '新增项目', '根据校长办公会决议新增', 0, '已审批', '2025-09-20 10:00:00');

INSERT INTO sys_message (message_id, title, content, message_type, is_read, create_time) VALUES
('MSG001', '项目超期预警', 'XX楼工程审计已超期68天未启动', 'ALERT', 0, '2026-07-08 08:00:00'),
('MSG002', '审批待办提醒', '2026年度经责审计计划变更申请待您审批', 'APPROVAL', 0, '2026-07-07 15:30:00')

