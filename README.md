# 审计信息管理系统 — 前后端接口文档（V2.0）

> **项目技术栈：** Vue 3 + Element Plus + Vite（前端） / Spring Boot 3.4 + MyBatis + MySQL（后端）
>
> **数据库：** project_practice（MySQL 8.0，utf8mb4）
>
> **前端端口：** 5173（Vite 代理 `/api` → `http://localhost:8080`）
>
> **后端端口：** 8080（context-path: `/`，接口前缀 `/audit/v1`）

---

## 目录

- [一、通用约定](#一通用约定)
- [二、认证与用户](#二认证与用户)
- [三、审计计划管理](#三审计计划管理)
- [四、审计对象管理](#四审计对象管理)
- [五、审计进度可视化](#五审计进度可视化)
- [六、通用字典与设置](#六通用字典与设置)
- [七、附件管理](#七附件管理)
- [八、消息中心](#八消息中心)
- [附录 A：前端页面路由表](#附录-a前端页面路由表)
- [附录 B：数据库表结构](#附录-b数据库表结构)

---

## 一、通用约定

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTP（开发）/ HTTPS（生产） |
| 接口前缀 | `/api/audit/v1`（前端代理到后端 `/audit/v1`） |
| 字符编码 | UTF-8 |
| 请求体格式 | `application/json` |
| 文件上传 | `multipart/form-data` |
| 鉴权 | Bearer Token（`Authorization: Bearer jwt-token-{username}`） |

### 1.2 统一响应结构

```json
{ "code": 200, "message": "操作成功", "data": {}, "timestamp": 1751425600000 }
```

### 1.3 业务状态码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录/Token过期 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 1.4 分页规范

请求：`pageNum`（默认1）、`pageSize`（默认20）
响应：`{ "total": N, "pageNum": 1, "pageSize": 20, "pages": M, "list": [...] }`

### 1.5 核心枚举

| 枚举 | 值 |
|------|-----|
| planType | 0-经济责任审计, 1-财务收支审计, 2-专项审计, 3-工程审计 |
| approvalStatus | 0-已审批, 1-审批中, 2-已归档, 3-草稿, 4-已驳回 |
| unitCategory | 0-职能部门, 1-二级学院, 2-后勤单位, 3-校办企业, 4-基建项目部, 5-附属医院 |
| rectifyStatus | 0-未整改, 1-整改中, 2-已整改 |
| changeType | 0-修改计划信息, 1-调减项目, 2-修改周期, 3-其他 |

---

## 二、认证与用户

### 2.1 登录

```
POST /api/audit/v1/auth/login
Body: { "username": "admin", "password": "123456" }
Response: { "token": "jwt-token-admin", "userInfo": { "realName": "陈处长", "role": "DIRECTOR", ... } }
```

### 2.2 获取当前用户信息

```
GET /api/audit/v1/auth/userinfo
```

### 2.3 更新个人信息

```
PUT /api/audit/v1/auth/userinfo
Body: { "realName": "", "phone": "", "email": "", "department": "", "position": "" }
```

### 2.4 修改密码

```
PUT /api/audit/v1/auth/password
Body: { "oldPassword": "", "newPassword": "" }
```

### 2.5 退出登录

```
POST /api/audit/v1/auth/logout
```

---

## 三、审计计划管理

> 前端页面：`src/views/plan/Form.vue`、`src/views/plan/Track.vue`

### 3.1 计划批次 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/plan/batches` | 列表（keyword/planType/year/approvalStatus） |
| GET | `/plan/batches/{batchId}` | 详情 |
| POST | `/plan/batches` | 新建 |
| POST | `/plan/batches/batch` | 批量新建 |
| PUT | `/plan/batches/{batchId}` | 编辑（全字段：batchName/planType/year/startDate/endDate/auditLeader/remark/unitId/leaderId） |
| DELETE | `/plan/batches/{batchId}` | 删除 |
| GET | `/plan/statistics/summary` | 首页统计卡片 |

**响应字段：** batchId, batchName, planType, year, projectCount, startDate, endDate, approvalStatus, progress, auditLeader, unitId, unitName（JOIN）, leaderId, remark

### 3.2 审批流

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/plan/batches/{batchId}/approval/progress` | 审批进度（steps + currentStep，自动初始化） |
| POST | `/plan/batches/{batchId}/approval/action` | 审批操作（action: APPROVE/REJECT + comment） |
| POST | `/plan/batches/{batchId}/approval/resubmit` | 重新提交被驳回的计划 |
| GET | `/plan/batches/{batchId}/approval/history` | 审批历史 |

**审批-进度联动：**
- 步骤1完成→10%, 步骤2→30%, 步骤3→60%, 步骤4→85%, 步骤5→100%+已审批
- 驳回→0%+已驳回，自动生成整改记录
- 重新提交→重置为步骤1ACTIVE

### 3.3 计划变更

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/plan/batches/{batchId}/changes` | 变更记录列表 |
| POST | `/plan/batches/{batchId}/changes` | 提交变更（含完整计划字段快照） |
| POST | `/plan/batches/{batchId}/changes/{changeId}/approve` | 确认变更→应用数据到计划 |

**变更流程：** 提交（status=1待确认）→ 在Track.vue确认 → 解析change_data JSON → 更新plan_batch → status=0已确认

### 3.4 穿透查询

```
GET /api/audit/v1/plan/projects/{projectId}/penetrate
```

返回：batchInfo + guidingFiles（从附件表查）+ progress + workingPapers + reports + rectifyLedger（从整改台账表查，含list明细）

### 3.5 智能推荐

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/plan/recommend/objects` | 推荐待审对象（单位+干部混合，按推荐分降序） |
| POST | `/plan/batches/{batchId}/projects/recommend-import` | 一键导入推荐对象 |

### 3.6 审计方案模板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/templates` | 模板列表 |
| GET | `/templates/{templateId}/content` | 模板内容 |
| GET | `/plan/batches/{batchId}/templates` | 批次绑定的模板 |
| POST | `/plan/batches/{batchId}/templates` | 批量绑定模板 |

---

## 四、审计对象管理

> 前端页面：`src/views/object/Unit.vue`、`src/views/object/Lead.vue`

### 4.1 被审计单位

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/objects/units` | 列表（keyword/category） |
| GET | `/objects/units/{unitId}` | 详情 |
| POST | `/objects/units` | 新建 |
| PUT | `/objects/units/{unitId}` | 编辑 |
| DELETE | `/objects/units/{unitId}` | 删除 |
| GET | `/objects/units/{unitId}/audit-records` | 审计记录 |
| GET | `/objects/units/{unitId}/rectify-ledger` | 整改台账（从audit_rectify_ledger查） |

**响应字段：** unitId, unitCode, unitName, category, categoryName, establishmentCount, fundScale, leaderInCharge, financeContact, financeContactPhone, address, setupDate, totalAuditCount, latestAuditDate, pendingRectifyCount（动态计算）, leaderNames

### 4.2 经责领导干部

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/objects/leaders` | 列表（keyword/isActive） |
| GET | `/objects/leaders/{leaderId}` | 详情 |
| POST | `/objects/leaders` | 新建（自动计算tenureYears） |
| PUT | `/objects/leaders/{leaderId}` | 编辑（全字段，含tenureStartDate/tenureYears/fundScope） |
| DELETE | `/objects/leaders/{leaderId}` | 删除 |
| GET | `/objects/leaders/{leaderId}/career-history` | 任职履历列表 |
| POST | `/objects/leaders/{leaderId}/career-history` | 添加履历 |
| PUT | `/objects/leaders/{leaderId}/career-history/{recordId}` | 编辑履历 |
| DELETE | `/objects/leaders/{leaderId}/career-history/{recordId}` | 删除履历 |
| GET | `/objects/leaders/{leaderId}/audit-projects` | 关联审计项目（查plan_batch + rectify统计） |
| GET | `/objects/leaders/recommend-for-audit` | 经责审计推荐清单 |

**任职年限：** 在职干部动态计算 `(today - tenureStartDate) / 365`，精确到1位小数。

### 4.3 三向关联传导

```
计划审批完成
  → 被审计单位: totalAuditCount+1, latestAuditDate更新, pendingRectifyCount实时统计
    → 级联该单位所有在职领导: pendingRectifyCount同步
  → 直接关联领导: auditCount+1, latestAuditDate更新
计划被驳回
  → 自动创建整改记录（类别=审批驳回, 状态=未整改, 截止=30天后）
  → 重新提交审批通过后自动清除驳回整改记录
```

---

## 五、审计进度可视化

> 前端页面：`src/views/statistic/Analysis.vue`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/statistics/overview` | 总览（summaryCards + byPlanType） |
| GET | `/statistics/gantt` | 甘特图数据（按planType分组） |
| GET | `/statistics/workload` | 审计组长负载分布 |
| GET | `/statistics/alerts` | 进度预警项目 |
| POST | `/statistics/alerts/push` | 手动推送预警 |

---

## 六、通用字典与设置

### 6.1 字典

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dict/all` | 全部枚举字典 |
| GET | `/users/auditors` | 审计人员列表 |

### 6.2 系统设置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/settings` | 获取设置（notify + ui） |
| POST | `/settings/notify` | 保存通知设置（JSON） |
| POST | `/settings/ui` | 保存界面设置（JSON） |
| POST | `/settings` | 保存全部设置 |

---

## 七、附件管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/plan/batches/{batchId}/attachments` | 附件列表 |
| POST | `/plan/batches/{batchId}/attachments` | 上传（multipart: file + attachType） |
| DELETE | `/plan/batches/{batchId}/attachments/{attachId}` | 删除 |
| GET | `/files/{attachId}/preview` | 预览（PDF/图片inline，Office自动转下载） |
| GET | `/files/{attachId}/download` | 下载 |

**存储：** 文件存 `{workdir}/uploads/`，元数据存 `audit_attachment` 表。

---

## 八、消息中心

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/messages` | 消息列表 |
| PUT | `/messages/{messageId}/read` | 标记已读 |
| PUT | `/messages/read-all` | 全部已读 |
| GET | `/messages/unread-count` | 未读数 |

---

## 附录 A：前端页面路由表

| 路由 | 页面 | 主要API |
|------|------|---------|
| `/login` | 登录 | auth/login |
| `/plan/form` | 计划清单 | plan/batches CRUD, statistics/summary, recommend, attachments, templates, changes, penetrate |
| `/plan/track` | 审批跟踪 | approval/progress, approval/action, approval/resubmit, approval/history, changes |
| `/object/unit` | 被审计单位库 | objects/units CRUD, rectify-ledger, audit-records |
| `/object/lead` | 领导干部库 | objects/leaders CRUD, career-history CRUD, audit-projects, recommend |
| `/statistic` | 进度可视化 | statistics/overview, gantt, workload, alerts |
| `/settings/profile` | 个人信息 | auth/userinfo, auth/password |
| `/settings/system` | 系统设置 | settings CRUD |

---

## 附录 B：数据库表结构

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| audit_plan_batch | 审计计划批次 | batch_id, batch_name, plan_type, year, project_count, start_date, end_date, approval_status, progress, audit_leader, unit_id, leader_id, audit_conclusion |
| audit_object_unit | 被审计单位 | unit_id, unit_code, unit_name, category, category_name, establishment_count, fund_scale, leader_in_charge, finance_contact, finance_contact_phone, address, setup_date, total_audit_count, latest_audit_date, pending_rectify_count |
| audit_object_leader | 领导干部 | leader_id, leader_code, leader_name, staff_id, current_unit_name, current_position, is_active, tenure_start_date, tenure_years, fund_scope, audit_count, latest_audit_date, latest_audit_conclusion, pending_rectify_count |
| leader_career_history | 任职履历 | record_id, leader_id, unit_name, position, start_date, end_date, duty_description, fund_scope, source |
| audit_attachment | 附件 | attach_id, batch_id, file_name, file_size, file_type, attach_type, file_path |
| audit_template | 方案模板 | template_id, template_name, plan_type, version, content |
| batch_template | 批次-模板关联 | batch_id, template_id |
| approval_step | 审批步骤 | batch_id, step_order, step_name, status, approver_name, comment, operate_time |
| approval_history | 审批历史 | approval_id, batch_id, flow_type, submit_by, submit_time, result |
| plan_change | 计划变更 | change_id, batch_id, change_type, change_type_name, reason, change_data, approval_status, approval_status_name, apply_time |
| audit_rectify_ledger | 整改台账 | rectify_id, batch_id, unit_id, issue_description, issue_category, rectify_status, responsible_person, deadline, rectify_progress |
| sys_user | 系统用户 | username, password, real_name, staff_id, department, position, phone, email, role |
| sys_settings | 系统设置 | user_id, setting_key, setting_value |
| sys_message | 系统消息 | message_id, title, content, message_type, is_read, create_time |

---

> **文档版本：** V2.0
>
> **最后更新：** 2026-07-08
>
> **变更说明：** V2.0 新增认证模块、系统设置、任职履历CRUD、整改台账联动、审批-进度联动、变更确认流程、三向关联传导、完整导入导出。
