# 审计信息管理系统 — 前端接口文档（V1.0）

> **项目技术栈：** Vue 3 + Element Plus + Vite + Vue Router
>
> **适用对象：** 后端开发人员
>
> **文档用途：** 定义前端所需全部 RESTful API 接口契约，作为前后端联调依据。

---

## 目录

- [一、通用约定](#一通用约定)
- [二、审计计划管理模块](#二审计计划管理模块)
  - [2.1 计划批次管理](#21-计划批次管理)
  - [2.2 项目编排管理](#22-项目编排管理)
  - [2.3 智能推荐](#23-智能推荐)
  - [2.4 附件管理](#24-附件管理)
  - [2.5 审计方案绑定](#25-审计方案绑定)
  - [2.6 审批流管理](#26-审批流管理)
  - [2.7 计划预警](#27-计划预警)
  - [2.8 计划变更管理](#28-计划变更管理)
  - [2.9 穿透查询](#29-穿透查询)
- [三、审计对象管理模块](#三审计对象管理模块)
  - [3.1 被审计单位库](#31-被审计单位库)
  - [3.2 经责领导干部库](#32-经责领导干部库)
  - [3.3 干部任职履历](#33-干部任职履历)
  - [3.4 数据同步](#34-数据同步)
- [四、审计进度可视化模块](#四审计进度可视化模块)
  - [4.1 进度总览](#41-进度总览)
  - [4.2 甘特图数据](#42-甘特图数据)
  - [4.3 资源负载](#43-资源负载)
  - [4.4 进度预警](#44-进度预警)
- [五、通用字典接口](#五通用字典接口)
- [六、文件上传/下载规范](#六文件上传下载规范)
- [七、WebSocket 推送规范](#七websocket-推送规范)

---

## 一、通用约定

### 1.1 基础信息

| 项目           | 说明                             |
| -------------- | -------------------------------- |
| 协议           | HTTPS                            |
| 接口前缀       | `/api/audit/v1`                  |
| 字符编码       | UTF-8                            |
| 请求体格式     | `application/json`               |
| 文件上传格式   | `multipart/form-data`            |
| 鉴权方式       | Bearer Token（Header: `Authorization: Bearer <token>`） |

### 1.2 统一响应结构

所有接口返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1751425600000,
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 1.3 业务状态码

| code  | 说明               |
| ----- | ------------------ |
| 200   | 成功               |
| 400   | 参数校验失败       |
| 401   | 未登录/Token 过期  |
| 403   | 无权限             |
| 404   | 资源不存在         |
| 409   | 业务冲突（如重复） |
| 500   | 服务器内部错误     |

### 1.4 分页请求/响应规范

**请求参数：**

| 字段     | 类型    | 必填 | 说明             |
| -------- | ------- | ---- | ---------------- |
| pageNum  | integer | 否   | 页码，默认 1     |
| pageSize | integer | 否   | 每页条数，默认 20 |

**响应 data 分页结构：**

```json
{
  "total": 100,
  "pageNum": 1,
  "pageSize": 20,
  "pages": 5,
  "list": []
}
```

### 1.5 枚举值字典

#### 计划类型（planType）

| 值  | 说明           |
| --- | -------------- |
| 0   | 经济责任审计   |
| 1   | 财务收支审计   |
| 2   | 专项审计       |
| 3   | 工程审计       |

#### 审批状态（approvalStatus）

| 值  | 说明   |
| --- | ------ |
| 0   | 已审批 |
| 1   | 审批中 |
| 2   | 已归档 |
| 3   | 草稿   |
| 4   | 已驳回 |

#### 项目实施状态（projectStatus）

| 值  | 说明     |
| --- | -------- |
| 0   | 未启动   |
| 1   | 进行中   |
| 2   | 已完结   |
| 3   | 已归档   |
| 4   | 超期     |
| 5   | 暂停     |

#### 审计对象分类（unitCategory）

| 值  | 说明         |
| --- | ------------ |
| 0   | 校内职能部门 |
| 1   | 二级学院     |
| 2   | 直属后勤单位 |
| 3   | 校办企业     |
| 4   | 基建项目部   |
| 5   | 附属医院     |

#### 整改状态（rectifyStatus）

| 值  | 说明     |
| --- | -------- |
| 0   | 未整改   |
| 1   | 整改中   |
| 2   | 已整改   |
| 3   | 无需整改 |

#### 是否委托中介（isOutsource）

| 值  | 说明             |
| --- | ---------------- |
| 0   | 校内自审         |
| 1   | 委托中介         |

#### 变更类型（changeType）

| 值  | 说明     |
| --- | -------- |
| 0   | 新增项目 |
| 1   | 调减项目 |
| 2   | 修改周期 |
| 3   | 其他变更 |

#### 预警类型（alertType）

| 值  | 说明             |
| --- | ---------------- |
| 0   | 超期未启动       |
| 1   | 实施滞后         |
| 2   | 报告逾期未提交   |
| 3   | 超计划预设期限   |

---

## 二、审计计划管理模块

> 对应前端页面：`src/views/plan/Form.vue`（计划列表）、`src/views/plan/Track.vue`（审批跟踪）

### 2.1 计划批次管理

#### 2.1.1 查询计划批次列表（分页）

> 对应前端：计划清单页 — 多条件组合筛选 + 分页列表

```
GET /api/audit/v1/plan/batches
```

**请求参数：**

| 字段           | 类型    | 必填 | 说明                                        |
| -------------- | ------- | ---- | ------------------------------------------- |
| keyword        | string  | 否   | 计划编号 / 名称模糊搜索                     |
| planType       | integer | 否   | 计划类型（见枚举），不传=全部               |
| year           | integer | 否   | 计划年度，如 2026                           |
| approvalStatus | integer | 否   | 审批状态（见枚举），不传=全部               |
| isOutsource    | integer | 否   | 是否委托中介，不传=全部                     |
| startDate      | string  | 否   | 实施周期起（YYYY-MM-DD）                    |
| endDate        | string  | 否   | 实施周期止（YYYY-MM-DD）                    |
| pageNum        | integer | 否   | 页码，默认 1                                |
| pageSize       | integer | 否   | 每页条数，默认 20                           |

**响应 data.list 元素：**

```json
{
  "batchId": "JH2026001",
  "batchName": "2026年度经济责任审计计划",
  "planType": 0,
  "planTypeName": "经济责任审计",
  "year": 2026,
  "projectCount": 8,
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "approvalStatus": 0,
  "approvalStatusName": "已审批",
  "isOutsource": 0,
  "auditLeader": "张三",
  "createTime": "2025-12-15 10:30:00",
  "createBy": "李四",
  "progress": 75,
  "progressStatus": "warning"
}
```

> **⚠️ 前端展示说明：** `progress` 0 或 100 显示 `success`（绿），0 < x ≤ 50 显示 `exception`（红），50 < x ≤ 70 无特殊样式，70 < x < 100 显示 `warning`（橙）。`progressStatus` 字段由后端计算后直接返回前端使用。

---

#### 2.1.2 新建计划批次

```
POST /api/audit/v1/plan/batches
```

**请求体：**

```json
{
  "batchName": "2026年度经济责任审计计划",
  "planType": 0,
  "year": 2026,
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "isOutsource": 0,
  "auditLeader": "张三",
  "auditLeaderId": "U001",
  "remark": "备注信息"
}
```

**响应 data：**

```json
{
  "batchId": "JH2026001",
  "batchName": "2026年度经济责任审计计划",
  "planType": 0,
  "year": 2026,
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "approvalStatus": 3,
  "createTime": "2025-12-15 10:30:00",
  "createBy": "李四"
}
```

---

#### 2.1.3 批量新建计划批次

```
POST /api/audit/v1/plan/batches/batch
```

**请求体：**

```json
{
  "batches": [
    {
      "batchName": "2026年度经济责任审计计划",
      "planType": 0,
      "year": 2026,
      "startDate": "2026-01-01",
      "endDate": "2026-06-30"
    },
    {
      "batchName": "2026年度财务收支审计计划",
      "planType": 1,
      "year": 2026,
      "startDate": "2026-03-01",
      "endDate": "2026-09-30"
    }
  ]
}
```

**响应 data：** 返回批量创建结果（含成功数、失败数及每条详情）

```json
{
  "successCount": 2,
  "failCount": 0,
  "details": [
    { "batchName": "2026年度经济责任审计计划", "success": true, "batchId": "JH2026001" },
    { "batchName": "2026年度财务收支审计计划", "success": true, "batchId": "JH2026002" }
  ]
}
```

---

#### 2.1.4 查询计划批次详情

```
GET /api/audit/v1/plan/batches/{batchId}
```

**响应 data：**

```json
{
  "batchId": "JH2026001",
  "batchName": "2026年度经济责任审计计划",
  "planType": 0,
  "planTypeName": "经济责任审计",
  "year": 2026,
  "projectCount": 8,
  "startDate": "2026-01-01",
  "endDate": "2026-06-30",
  "approvalStatus": 0,
  "approvalStatusName": "已审批",
  "isOutsource": 0,
  "auditLeader": "张三",
  "auditLeaderId": "U001",
  "remark": "备注信息",
  "createTime": "2025-12-15 10:30:00",
  "createBy": "李四",
  "updateTime": "2025-12-20 14:00:00",
  "updateBy": "王五",
  "attachments": [],
  "templates": [],
  "changeLogs": []
}
```

---

#### 2.1.5 编辑计划批次

```
PUT /api/audit/v1/plan/batches/{batchId}
```

**请求体：**（仅传需要修改的字段）

```json
{
  "batchName": "2026年度经济责任审计计划（修订）",
  "startDate": "2026-02-01",
  "endDate": "2026-08-31",
  "auditLeader": "赵六",
  "auditLeaderId": "U002",
  "remark": "调整后的备注"
}
```

---

#### 2.1.6 删除计划批次

> 仅草稿状态（approvalStatus=3）可删除

```
DELETE /api/audit/v1/plan/batches/{batchId}
```

---

#### 2.1.7 导出计划列表 Excel

```
GET /api/audit/v1/plan/batches/export
```

**请求参数：** 同 2.1.1 筛选条件（无需 pageNum/pageSize，导出全部命中结果）

**响应：** 文件流（`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）

---

### 2.2 项目编排管理

#### 2.2.1 查询计划批次下的项目列表（分页）

```
GET /api/audit/v1/plan/batches/{batchId}/projects
```

**请求参数：**

| 字段           | 类型    | 必填 | 说明                  |
| -------------- | ------- | ---- | --------------------- |
| keyword        | string  | 否   | 项目名称 / 编号模糊搜索 |
| projectType    | integer | 否   | 项目类型              |
| projectStatus  | integer | 否   | 实施状态              |
| isOutsource    | integer | 否   | 是否委托中介          |
| auditObjectId  | string  | 否   | 审计对象 ID           |
| pageNum        | integer | 否   | 页码                  |
| pageSize       | integer | 否   | 每页条数              |

**响应 data.list 元素：**

```json
{
  "projectId": "PJ202600101",
  "projectCode": "PJ2026-001-01",
  "projectName": "XX学院院长经济责任审计",
  "planType": 0,
  "auditObjectId": "OBJ001",
  "auditObjectName": "XX学院",
  "auditObjectType": 1,
  "coverPeriodStart": "2023-01-01",
  "coverPeriodEnd": "2025-12-31",
  "projectStartDate": "2026-02-01",
  "projectEndDate": "2026-04-30",
  "auditLeader": "张三",
  "auditLeaderId": "U001",
  "isOutsource": 0,
  "outsourceOrgName": null,
  "projectStatus": 1,
  "projectStatusName": "进行中",
  "fundAmount": 5000000.00,
  "sortOrder": 1
}
```

---

#### 2.2.2 手动新增项目

```
POST /api/audit/v1/plan/batches/{batchId}/projects
```

**请求体：**

```json
{
  "projectName": "XX学院院长经济责任审计",
  "auditObjectId": "OBJ001",
  "coverPeriodStart": "2023-01-01",
  "coverPeriodEnd": "2025-12-31",
  "projectStartDate": "2026-02-01",
  "projectEndDate": "2026-04-30",
  "auditLeaderId": "U001",
  "isOutsource": 0,
  "outsourceOrgName": null,
  "teamMemberIds": ["U003", "U004"]
}
```

---

#### 2.2.3 Excel 批量导入项目

```
POST /api/audit/v1/plan/batches/{batchId}/projects/import
```

**请求体：** `multipart/form-data`

| 字段 | 类型 | 必填 | 说明               |
| ---- | ---- | ---- | ------------------ |
| file | File | 是   | Excel 文件（.xlsx） |

**响应 data：**

```json
{
  "totalRows": 50,
  "successCount": 48,
  "failCount": 2,
  "failDetails": [
    { "row": 5, "reason": "审计对象编码不存在" },
    { "row": 23, "reason": "覆盖期间格式错误" }
  ]
}
```

> **前端交互说明：** 提供 Excel 模板下载接口，含必填字段列及示例数据行。

---

#### 2.2.4 下载项目导入模板

```
GET /api/audit/v1/plan/projects/template/download
```

**响应：** 文件流

---

#### 2.2.5 批量修改项目字段

```
PUT /api/audit/v1/plan/batches/{batchId}/projects/batch-update
```

**请求体：**

```json
{
  "projectIds": ["PJ202600101", "PJ202600102", "PJ202600103"],
  "updates": {
    "planType": 0,
    "projectStartDate": "2026-03-01",
    "projectEndDate": "2026-06-30",
    "auditLeaderId": "U005"
  }
}
```

---

#### 2.2.6 编辑单个项目

```
PUT /api/audit/v1/plan/projects/{projectId}
```

**请求体：** 同 2.2.2，传需要修改的字段。

---

#### 2.2.7 删除项目

```
DELETE /api/audit/v1/plan/projects/{projectId}
```

---

#### 2.2.8 导出项目列表 Excel

```
GET /api/audit/v1/plan/batches/{batchId}/projects/export
```

**请求参数：** 同 2.2.1 筛选条件（无分页）

---

### 2.3 智能推荐

#### 2.3.1 获取推荐待审对象清单

```
GET /api/audit/v1/plan/recommend/objects
```

**请求参数：**

| 字段            | 类型    | 必填 | 说明                             |
| --------------- | ------- | ---- | -------------------------------- |
| planType        | integer | 否   | 计划类型筛选                     |
| riskLevel       | string  | 否   | 风险等级：HIGH / MEDIUM / LOW    |
| pageNum         | integer | 否   | 页码                             |
| pageSize        | integer | 否   | 每页条数                         |

**响应 data.list 元素：**

```json
{
  "objectId": "OBJ001",
  "objectName": "XX学院",
  "objectType": "UNIT",
  "objectTypeName": "被审计单位",
  "category": 1,
  "categoryName": "二级学院",
  "recommendReason": "距上轮经责审计已超3年",
  "lastAuditDate": "2022-05-20",
  "lastAuditResult": "发现问题12项，已整改10项",
  "riskLevel": "HIGH",
  "fundScale": 50000000.00,
  "leaderTenureYears": 4,
  "recommendScore": 92
}
```

> **推荐规则说明：** 系统综合经责审计滚动周期（超 3 年未审优先）、上一轮审计完成时间、干部任职年限（≥3 年）、资金规模（额度越大优先级越高）、基建项目竣工年限（竣工满 2 年）多维度加权评分，按 `recommendScore` 降序排列。

---

#### 2.3.2 一键将推荐对象加入计划批次

```
POST /api/audit/v1/plan/batches/{batchId}/projects/recommend-import
```

**请求体：**

```json
{
  "objectIds": ["OBJ001", "OBJ003", "OBJ007"],
  "defaultProjectStartDate": "2026-03-01",
  "defaultProjectEndDate": "2026-08-31",
  "defaultAuditLeaderId": "U001"
}
```

**响应 data：**

```json
{
  "successCount": 3,
  "failCount": 0,
  "createdProjects": [
    { "projectId": "PJ202600111", "projectName": "XX学院经济责任审计" },
    { "projectId": "PJ202600112", "projectName": "YY处财务收支审计" },
    { "projectId": "PJ202600113", "projectName": "ZZ公司专项审计" }
  ]
}
```

---

### 2.4 附件管理

#### 2.4.1 上传附件

```
POST /api/audit/v1/plan/batches/{batchId}/attachments
```

**请求体：** `multipart/form-data`

| 字段         | 类型   | 必填 | 说明                                                |
| ------------ | ------ | ---- | --------------------------------------------------- |
| file         | File   | 是   | 文件（支持 PDF/Word/Excel/图片，单文件 ≤ 50MB）     |
| attachType   | string | 是   | DELEGATION（委托书）/ RESOLUTION（审批决议）/ TASK（上级任务文件）/ RESEARCH（调研材料）/ OTHER（其他） |
| description  | string | 否   | 附件说明                                            |

**响应 data：**

```json
{
  "attachId": "ATT2026001",
  "fileName": "审计委托书.pdf",
  "fileSize": 204800,
  "fileType": "application/pdf",
  "attachType": "DELEGATION",
  "attachTypeName": "审计委托书",
  "uploadTime": "2025-12-15 11:00:00",
  "uploadBy": "李四",
  "previewUrl": "/api/audit/v1/files/ATT2026001/preview",
  "downloadUrl": "/api/audit/v1/files/ATT2026001/download"
}
```

---

#### 2.4.2 查询附件列表

```
GET /api/audit/v1/plan/batches/{batchId}/attachments
```

**响应 data：** 附件对象数组

---

#### 2.4.3 删除附件

```
DELETE /api/audit/v1/plan/batches/{batchId}/attachments/{attachId}
```

---

#### 2.4.4 文件在线预览

```
GET /api/audit/v1/files/{attachId}/preview
```

**响应：** 根据文件类型返回可预览格式流（PDF→PDF流，Office→转 PDF 流，图片→原图流），`Content-Disposition: inline`。

---

#### 2.4.5 文件下载

```
GET /api/audit/v1/files/{attachId}/download
```

**响应：** 文件流，`Content-Disposition: attachment; filename="xxx.pdf"`。

---

### 2.5 审计方案绑定

#### 2.5.1 查询标准化审计方案模板库

```
GET /api/audit/v1/templates
```

**请求参数：**

| 字段     | 类型    | 必填 | 说明     |
| -------- | ------- | ---- | -------- |
| planType | integer | 否   | 计划类型 |
| keyword  | string  | 否   | 模板名称模糊搜索 |
| pageNum  | integer | 否   | 页码     |
| pageSize | integer | 否   | 每页条数 |

**响应 data.list 元素：**

```json
{
  "templateId": "TPL001",
  "templateName": "经济责任审计标准化方案",
  "planType": 0,
  "planTypeName": "经济责任审计",
  "version": "V2.1",
  "description": "适用于校内中层领导干部经济责任审计",
  "createTime": "2025-06-01 09:00:00",
  "updateTime": "2025-11-10 15:30:00"
}
```

---

#### 2.5.2 为计划批次批量关联方案模板

```
POST /api/audit/v1/plan/batches/{batchId}/templates
```

**请求体：**

```json
{
  "templateIds": ["TPL001", "TPL002"]
}
```

---

#### 2.5.3 为单个项目定制专属方案

```
PUT /api/audit/v1/plan/projects/{projectId}/custom-template
```

**请求体：**

```json
{
  "content": "针对本项目定制的审计实施方案完整内容（富文本 / JSON）",
  "basedOnTemplateId": "TPL001"
}
```

---

#### 2.5.4 查看计划批次关联的方案

```
GET /api/audit/v1/plan/batches/{batchId}/templates
```

**响应 data：** 已关联方案模板列表 + 各项目定制方案标记

```json
{
  "batchTemplates": [
    {
      "templateId": "TPL001",
      "templateName": "经济责任审计标准化方案",
      "isDefault": true
    }
  ],
  "projectCustomTemplates": [
    {
      "projectId": "PJ202600101",
      "projectName": "XX学院院长经济责任审计",
      "hasCustom": true
    }
  ]
}
```

---

#### 2.5.5 穿透查看方案完整内容

```
GET /api/audit/v1/templates/{templateId}/content
```

**响应 data：**

```json
{
  "templateId": "TPL001",
  "templateName": "经济责任审计标准化方案",
  "version": "V2.1",
  "content": "（富文本 HTML 或结构化 JSON）",
  "attachments": []
}
```

```
GET /api/audit/v1/plan/projects/{projectId}/custom-template/content
```

---

### 2.6 审批流管理

> 对应前端页面：`src/views/plan/Track.vue`

#### 2.6.1 提交审批

```
POST /api/audit/v1/plan/batches/{batchId}/approval/submit
```

**请求体：**

```json
{
  "approvalType": "NEW_PLAN",
  "comment": "请审批2026年度经济责任审计计划"
}
```

**approvalType 取值：** `NEW_PLAN`（新增计划）、`PLAN_CHANGE`（计划变更）、`PLAN_ADD`（调增项目）、`PLAN_REDUCE`（调减项目）

---

#### 2.6.2 查询审批流进度（步骤条）

```
GET /api/audit/v1/plan/batches/{batchId}/approval/progress
```

**响应 data：**

```json
{
  "approvalId": "APR2026001",
  "approvalType": "NEW_PLAN",
  "currentStep": 2,
  "steps": [
    {
      "stepOrder": 1,
      "stepName": "提交",
      "status": "COMPLETED",
      "approverId": "U010",
      "approverName": "李四",
      "comment": "提交2026年度经责审计计划",
      "operateTime": "2025-12-15 10:30:00"
    },
    {
      "stepOrder": 2,
      "stepName": "组长审核",
      "status": "COMPLETED",
      "approverId": "U020",
      "approverName": "张三",
      "comment": "同意，请处长审批",
      "operateTime": "2025-12-16 09:00:00"
    },
    {
      "stepOrder": 3,
      "stepName": "处长审批",
      "status": "ACTIVE",
      "approverId": "U030",
      "approverName": "王五",
      "comment": null,
      "operateTime": null
    },
    {
      "stepOrder": 4,
      "stepName": "校领导审批",
      "status": "PENDING",
      "approverId": null,
      "approverName": null,
      "comment": null,
      "operateTime": null
    },
    {
      "stepOrder": 5,
      "stepName": "归档",
      "status": "PENDING",
      "approverId": null,
      "approverName": null,
      "comment": null,
      "operateTime": null
    }
  ]
}
```

> **步骤状态枚举：** `PENDING`（待处理）、`ACTIVE`（当前处理中）、`COMPLETED`（已完成）、`REJECTED`（已驳回）、`SKIPPED`（已跳过）

---

#### 2.6.3 审批操作（通过/驳回）

```
POST /api/audit/v1/plan/batches/{batchId}/approval/action
```

**请求体：**

```json
{
  "action": "APPROVE",
  "comment": "同意通过"
}
```

```json
{
  "action": "REJECT",
  "comment": "计划周期需调整，请修改后重新提交"
}
```

**action 取值：** `APPROVE` / `REJECT`

---

#### 2.6.4 查询审批历史记录（某计划下所有审批流程）

```
GET /api/audit/v1/plan/batches/{batchId}/approval/history
```

**响应 data：** 审批流对象数组（含已完结、进行中、已驳回的全部审批记录）

---

#### 2.6.5 自主配置审批流程模板

```
GET /api/audit/v1/approval/flow-templates
```

```
POST /api/audit/v1/approval/flow-templates
```

**请求体：**

```json
{
  "templateName": "年度计划新增审批流",
  "applyTo": "NEW_PLAN",
  "steps": [
    { "stepOrder": 1, "stepName": "提交", "approverRole": "SUBMITTER" },
    { "stepOrder": 2, "stepName": "组长审核", "approverRole": "AUDIT_LEADER" },
    { "stepOrder": 3, "stepName": "处长审批", "approverRole": "DIRECTOR" },
    { "stepOrder": 4, "stepName": "校领导审批", "approverRole": "VP" },
    { "stepOrder": 5, "stepName": "归档", "approverRole": "ARCHIVIST" }
  ]
}
```

---

### 2.7 计划预警

#### 2.7.1 查询预警记录列表（分页）

```
GET /api/audit/v1/plan/alerts
```

**请求参数：**

| 字段        | 类型    | 必填 | 说明               |
| ----------- | ------- | ---- | ------------------ |
| alertType   | integer | 否   | 预警类型           |
| isRead      | integer | 否   | 是否已读（0-未读 / 1-已读） |
| batchId     | string  | 否   | 计划批次 ID        |
| pageNum     | integer | 否   |                    |
| pageSize    | integer | 否   |                    |

**响应 data.list 元素：**

```json
{
  "alertId": "ALT2026001",
  "alertType": 3,
  "alertTypeName": "超计划预设期限",
  "batchId": "JH2026004",
  "batchName": "产业园新建工程项目审计",
  "projectId": "PJ202600405",
  "projectName": "XX楼工程审计",
  "alertContent": "项目实施已超计划预设期限15天",
  "severity": "HIGH",
  "isRead": 0,
  "notifyTargets": [
    { "userId": "U001", "userName": "张三", "role": "审计组长" },
    { "userId": "U030", "userName": "王五", "role": "审计处负责人" }
  ],
  "createTime": "2026-07-01 08:00:00"
}
```

---

#### 2.7.2 标记预警已读

```
PUT /api/audit/v1/plan/alerts/{alertId}/read
```

#### 2.7.3 批量标记预警已读

```
PUT /api/audit/v1/plan/alerts/batch-read
```

**请求体：**

```json
{
  "alertIds": ["ALT2026001", "ALT2026002", "ALT2026003"]
}
```

---

#### 2.7.4 查询首页预警统计卡片数据

```
GET /api/audit/v1/plan/statistics/summary
```

**响应 data：**

```json
{
  "totalPlanCount": 12,
  "approvedCount": 8,
  "approvingCount": 3,
  "alertCount": 4
}
```

> 对应前端 `Form.vue` 顶部 4 个统计卡片。

---

### 2.8 计划变更管理

#### 2.8.1 发起计划变更申请

```
POST /api/audit/v1/plan/batches/{batchId}/changes
```

**请求体：**

```json
{
  "changeType": 0,
  "reason": "根据校党委会最新决议，新增XX专项审计任务",
  "details": {
    "addedProjects": [
      {
        "projectName": "XX专项审计",
        "auditObjectId": "OBJ020",
        "coverPeriodStart": "2025-01-01",
        "coverPeriodEnd": "2025-12-31",
        "projectStartDate": "2026-08-01",
        "projectEndDate": "2026-10-31",
        "auditLeaderId": "U002"
      }
    ]
  }
}
```

---

#### 2.8.2 查询变更记录列表

```
GET /api/audit/v1/plan/batches/{batchId}/changes
```

**响应 data.list 元素：**

```json
{
  "changeId": "CHG2026001",
  "changeType": 0,
  "changeTypeName": "新增项目",
  "reason": "根据校党委会最新决议，新增XX专项审计任务",
  "changeBefore": "（变更前快照 JSON）",
  "changeAfter": "（变更后快照 JSON）",
  "approvalStatus": 1,
  "approvalStatusName": "审批中",
  "applicantName": "李四",
  "applyTime": "2026-03-15 10:00:00",
  "approvalSteps": []
}
```

---

#### 2.8.3 查询变更前后对比详情

```
GET /api/audit/v1/plan/changes/{changeId}/diff
```

**响应 data：**

```json
{
  "changeId": "CHG2026001",
  "changeType": 0,
  "batchId": "JH2026001",
  "batchName": "2026年度经济责任审计计划",
  "diffFields": [
    {
      "field": "projectCount",
      "fieldName": "项目数",
      "before": 8,
      "after": 9
    }
  ],
  "changeBefore": { "projectCount": 8, "projects": [...] },
  "changeAfter": { "projectCount": 9, "projects": [...] }
}
```

---

### 2.9 穿透查询

#### 2.9.1 计划穿透 — 获取完整关联信息

> 从计划汇总页穿透查看单项目完整链路：计划 → 项目 → 底稿 → 报告 → 整改

```
GET /api/audit/v1/plan/projects/{projectId}/penetrate
```

**响应 data：**

```json
{
  "projectId": "PJ202600101",
  "projectName": "XX学院院长经济责任审计",
  "batchInfo": {
    "batchId": "JH2026001",
    "batchName": "2026年度经济责任审计计划",
    "planType": 0
  },
  "guidingFiles": [
    { "attachId": "ATT001", "fileName": "审计委托书.pdf", "previewUrl": "..." }
  ],
  "progress": {
    "projectStatus": 1,
    "progressPercent": 60,
    "currentPhase": "底稿编制"
  },
  "workingPapers": {
    "totalCount": 25,
    "completedCount": 18,
    "list": []
  },
  "reports": {
    "draftSubmitted": true,
    "finalSubmitted": false,
    "list": []
  },
  "rectifyLedger": {
    "totalIssues": 8,
    "rectifiedCount": 5,
    "rectifyingCount": 2,
    "pendingCount": 1,
    "list": []
  }
}
```

---

## 三、审计对象管理模块

> 对应前端页面：`src/views/object/Unit.vue`（被审计单位库）、`src/views/object/Lead.vue`（经责领导干部库）

### 3.1 被审计单位库

#### 3.1.1 查询被审计单位列表（分页）

```
GET /api/audit/v1/objects/units
```

**请求参数：**

| 字段       | 类型    | 必填 | 说明                         |
| ---------- | ------- | ---- | ---------------------------- |
| keyword    | string  | 否   | 单位名称 / 编码模糊搜索      |
| category   | integer | 否   | 分类（见枚举），不传=全部    |
| parentId   | string  | 否   | 上级单位 ID，用于树形筛选    |
| pageNum    | integer | 否   |                              |
| pageSize   | integer | 否   |                              |

**响应 data.list 元素：**

```json
{
  "unitId": "UNIT001",
  "unitCode": "DW-2020-001",
  "unitName": "XX学院",
  "category": 1,
  "categoryName": "二级学院",
  "parentUnitId": null,
  "parentUnitName": null,
  "establishmentCount": 85,
  "fundScale": 50000000.00,
  "leaderInCharge": "校领导A",
  "financeContact": "财务联系人姓名",
  "financeContactPhone": "13800000000",
  "currentCadreCount": 3,
  "totalAuditCount": 2,
  "latestAuditDate": "2023-05-10",
  "pendingRectifyCount": 2,
  "specialFundProjects": [],
  "children": []
}
```

---

#### 3.1.2 获取单位分类树（用于级联筛选）

```
GET /api/audit/v1/objects/units/category-tree
```

**响应 data：**

```json
[
  {
    "category": 0,
    "categoryName": "校内职能部门",
    "count": 25,
    "children": []
  },
  {
    "category": 1,
    "categoryName": "二级学院",
    "count": 18,
    "children": [
      { "unitId": "UNIT001", "unitName": "XX学院", "count": 0 },
      { "unitId": "UNIT002", "unitName": "YY学院", "count": 0 }
    ]
  }
]
```

---

#### 3.1.3 查询单位详情（含完整档案信息）

```
GET /api/audit/v1/objects/units/{unitId}
```

**响应 data：**

```json
{
  "unitId": "UNIT001",
  "unitCode": "DW-2020-001",
  "unitName": "XX学院",
  "category": 1,
  "categoryName": "二级学院",
  "basicInfo": {
    "establishmentCount": 85,
    "fundScale": 50000000.00,
    "leaderInCharge": "校领导A",
    "leaderInChargeId": "U100",
    "financeContact": "财务联系人姓名",
    "financeContactPhone": "13800000000",
    "address": "XX校区XX楼",
    "setupDate": "2000-09-01"
  },
  "cadreChangeLogs": [],
  "auditRecords": [],
  "rectifyLedger": [],
  "specialFundProjects": [],
  "updateLogs": []
}
```

---

#### 3.1.4 新建被审计单位

```
POST /api/audit/v1/objects/units
```

**请求体：**

```json
{
  "unitName": "新设学院",
  "category": 1,
  "parentUnitId": null,
  "establishmentCount": 50,
  "fundScale": 20000000.00,
  "leaderInCharge": "校领导B",
  "leaderInChargeId": "U101",
  "financeContact": "联系人姓名",
  "financeContactPhone": "13900000000",
  "address": "XX校区YY楼",
  "setupDate": "2025-09-01"
}
```

---

#### 3.1.5 编辑被审计单位

```
PUT /api/audit/v1/objects/units/{unitId}
```

---

#### 3.1.6 删除被审计单位

```
DELETE /api/audit/v1/objects/units/{unitId}
```

---

#### 3.1.7 查询单位历年干部任免台账

```
GET /api/audit/v1/objects/units/{unitId}/cadre-changes
```

**响应 data.list：**

```json
{
  "list": [
    {
      "changeId": "CC001",
      "cadreName": "张XX",
      "position": "院长",
      "changeType": "APPOINTMENT",
      "changeTypeName": "任命",
      "effectiveDate": "2022-03-01",
      "source": "MANUAL",
      "sourceName": "手动录入"
    }
  ]
}
```

---

#### 3.1.8 查询单位历年审计记录

```
GET /api/audit/v1/objects/units/{unitId}/audit-records
```

---

#### 3.1.9 查询单位历次审计问题整改台账

```
GET /api/audit/v1/objects/units/{unitId}/rectify-ledger
```

**响应 data.list 元素：**

```json
{
  "rectifyId": "RCT001",
  "projectId": "PJ202400301",
  "projectName": "2024年XX学院财务收支审计",
  "issueDescription": "差旅费报销缺少审批单",
  "issueCategory": "财务管理",
  "rectifyStatus": 1,
  "rectifyStatusName": "整改中",
  "responsiblePerson": "李XX",
  "deadline": "2025-12-31",
  "rectifyProgress": "已补充80%缺漏审批单",
  "createTime": "2024-08-15"
}
```

---

#### 3.1.10 查询单位专项资金项目清单

```
GET /api/audit/v1/objects/units/{unitId}/special-funds
```

**响应 data.list 元素：**

```json
{
  "fundId": "FUND001",
  "fundName": "2025年度实验室建设专项资金",
  "fundSource": "中央财政",
  "totalAmount": 5000000.00,
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "status": "IN_PROGRESS"
}
```

---

#### 3.1.11 全局搜索审计对象（用于项目创建等场景自动回填）

```
GET /api/audit/v1/objects/search
```

**请求参数：**

| 字段     | 类型   | 必填 | 说明                               |
| -------- | ------ | ---- | ---------------------------------- |
| keyword  | string | 是   | 单位名称 / 编码模糊搜索（≥2 字符） |
| category | integer | 否  | 限定分类                           |
| limit    | integer | 否  | 返回条数，默认 10                  |

**响应 data：**

```json
[
  {
    "objectId": "UNIT001",
    "objectCode": "DW-2020-001",
    "objectName": "XX学院",
    "objectType": "UNIT",
    "category": 1,
    "financeContact": "联系人",
    "financeContactPhone": "13800000000"
  }
]
```

> **前端交互说明：** 项目创建 / 通知书生成 / 底稿编制 / 整改下发等场景，选择审计对象后，自动回填单位名称、覆盖期间、联系人等基础信息，由后端根据 objectId 返回完整关联信息。

---

### 3.2 经责领导干部库

#### 3.2.1 查询经责领导干部列表（分页）

```
GET /api/audit/v1/objects/leaders
```

**请求参数：**

| 字段       | 类型    | 必填 | 说明                    |
| ---------- | ------- | ---- | ----------------------- |
| keyword    | string  | 否   | 姓名 / 工号模糊搜索     |
| unitId     | string  | 否   | 所属单位 ID             |
| isActive   | integer | 否   | 是否在职（0-离任 / 1-在职） |
| pageNum    | integer | 否   |                         |
| pageSize   | integer | 否   |                         |

**响应 data.list 元素：**

```json
{
  "leaderId": "LDR001",
  "leaderCode": "LD-2018-001",
  "leaderName": "张XX",
  "staffId": "T2020001",
  "currentUnitId": "UNIT001",
  "currentUnitName": "XX学院",
  "currentPosition": "院长",
  "isActive": 1,
  "tenureStartDate": "2022-03-01",
  "tenureYears": 4.3,
  "fundScope": 50000000.00,
  "auditCount": 1,
  "latestAuditDate": "2024-03-10",
  "latestAuditConclusion": "履职情况良好，发现一般性问题5项",
  "pendingRectifyCount": 0
}
```

---

#### 3.2.2 查询领导干部详情

```
GET /api/audit/v1/objects/leaders/{leaderId}
```

**响应 data：** 含任职履历完整记录、关联审计项目、历次审计评价、整改台账。

```json
{
  "leaderId": "LDR001",
  "leaderName": "张XX",
  "careerHistory": [],
  "auditProjects": [],
  "auditConclusions": [],
  "rectifyLedger": []
}
```

---

#### 3.2.3 新建经责领导干部

```
POST /api/audit/v1/objects/leaders
```

---

#### 3.2.4 编辑经责领导干部

```
PUT /api/audit/v1/objects/leaders/{leaderId}
```

#### 3.2.5 删除经责领导干部

```
DELETE /api/audit/v1/objects/leaders/{leaderId}
```

---

#### 3.2.6 查询干部任职履历全记录

```
GET /api/audit/v1/objects/leaders/{leaderId}/career-history
```

**响应 data.list 元素：**

```json
{
  "recordId": "CH001",
  "unitId": "UNIT001",
  "unitName": "XX学院",
  "position": "副院长",
  "startDate": "2018-09-01",
  "endDate": "2022-02-28",
  "fundScope": 30000000.00,
  "dutyDescription": "分管教学、科研工作",
  "source": "SYNC",
  "sourceName": "人事系统同步"
}
```

---

#### 3.2.7 查询干部关联的所有经责审计项目

```
GET /api/audit/v1/objects/leaders/{leaderId}/audit-projects
```

**响应 data.list 元素：**

```json
{
  "projectId": "PJ202400101",
  "projectName": "XX学院院长经济责任审计",
  "planType": 0,
  "auditPeriodStart": "2022-03-01",
  "auditPeriodEnd": "2024-02-28",
  "projectStatus": 3,
  "projectStatusName": "已归档",
  "conclusion": "履职情况良好",
  "issueCount": 5,
  "rectifiedCount": 5
}
```

---

#### 3.2.8 生成滚动经责审计推荐清单

```
GET /api/audit/v1/objects/leaders/recommend-for-audit
```

**请求参数：**

| 字段          | 类型    | 必填 | 说明                          |
| ------------- | ------- | ---- | ----------------------------- |
| minTenureYear | integer | 否   | 最低任职年限筛选，默认 3      |
| limit         | integer | 否   | 返回条数，默认 20             |

**响应 data：**

```json
{
  "list": [
    {
      "leaderId": "LDR005",
      "leaderName": "赵XX",
      "unitName": "YY学院",
      "position": "院长",
      "tenureYears": 4.5,
      "lastAuditDate": null,
      "reason": "任职已超4年，尚未开展经责审计",
      "priority": "HIGH"
    }
  ],
  "total": 15
}
```

---

### 3.3 干部任职履历

#### 3.3.1 手动添加任职记录

```
POST /api/audit/v1/objects/leaders/{leaderId}/career-history
```

**请求体：**

```json
{
  "unitId": "UNIT001",
  "position": "副院长",
  "startDate": "2018-09-01",
  "endDate": "2022-02-28",
  "fundScope": 30000000.00,
  "dutyDescription": "分管教学、科研工作"
}
```

---

#### 3.3.2 编辑任职记录

```
PUT /api/audit/v1/objects/leaders/{leaderId}/career-history/{recordId}
```

---

#### 3.3.3 删除任职记录

```
DELETE /api/audit/v1/objects/leaders/{leaderId}/career-history/{recordId}
```

---

### 3.4 数据同步

#### 3.4.1 手动触发同步（对接学校人事数据中台）

```
POST /api/audit/v1/sync/trigger
```

**请求体：**

```json
{
  "syncType": "PERSONNEL",
  "targetIds": ["UNIT001", "UNIT002"]
}
```

**syncType 取值：** `PERSONNEL`（人事数据）、`FUND`（经费数据）、`ALL`（全量同步）

---

#### 3.4.2 查询同步日志

```
GET /api/audit/v1/sync/logs
```

**请求参数：**

| 字段     | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| syncType | string | 否   | 同步类型 |
| pageNum  | integer | 否   |          |
| pageSize | integer | 否   |          |

**响应 data.list 元素：**

```json
{
  "logId": "SYNC2026001",
  "syncType": "PERSONNEL",
  "syncTypeName": "人事数据同步",
  "status": "SUCCESS",
  "totalCount": 156,
  "updateCount": 12,
  "failCount": 0,
  "detail": "更新干部任免信息12条",
  "triggerBy": "系统自动",
  "startTime": "2026-07-08 02:00:00",
  "endTime": "2026-07-08 02:00:15"
}
```

---

## 四、审计进度可视化模块

> 对应前端页面：`src/views/statistic/Analysis.vue`

### 4.1 进度总览

#### 4.1.1 获取年度进度总览（统计卡片 + 图表数据）

```
GET /api/audit/v1/statistics/overview
```

**请求参数：**

| 字段 | 类型    | 必填 | 说明                       |
| ---- | ------- | ---- | -------------------------- |
| year | integer | 否   | 年度，默认当前年           |

**响应 data：**

```json
{
  "summaryCards": {
    "totalPlans": 15,
    "totalProjects": 86,
    "completedProjects": 42,
    "inProgressProjects": 28,
    "notStartedProjects": 10,
    "overdueProjects": 6,
    "onsiteAuditProjects": 18,
    "reportPendingProjects": 8
  },
  "byPlanType": [
    { "planType": 0, "planTypeName": "经济责任审计", "total": 35, "completed": 20, "inProgress": 10, "overdue": 5 },
    { "planType": 1, "planTypeName": "财务收支审计", "total": 25, "completed": 12, "inProgress": 10, "overdue": 3 },
    { "planType": 2, "planTypeName": "专项审计", "total": 16, "completed": 8, "inProgress": 6, "overdue": 2 },
    { "planType": 3, "planTypeName": "工程审计", "total": 10, "completed": 2, "inProgress": 6, "overdue": 2 }
  ],
  "byMonth": [
    { "month": "2026-01", "started": 5, "completed": 3 },
    { "month": "2026-02", "started": 8, "completed": 4 }
  ]
}
```

---

### 4.2 甘特图数据

#### 4.2.1 获取年度审计项目甘特图数据

```
GET /api/audit/v1/statistics/gantt
```

**请求参数：**

| 字段          | 类型    | 必填 | 说明     |
| ------------- | ------- | ---- | -------- |
| year          | integer | 否   | 年度     |
| planType      | integer | 否   | 计划类型 |
| projectStatus | integer | 否   | 实施状态 |
| groupBy       | string  | 否   | 分组维度：`planType`（按项目类型，默认）/ `status`（按实施阶段） |

**响应 data：**

```json
{
  "groups": [
    {
      "groupKey": "0",
      "groupName": "经济责任审计",
      "projects": [
        {
          "projectId": "PJ202600101",
          "projectName": "XX学院院长经济责任审计",
          "planType": 0,
          "projectStatus": 1,
          "projectStatusName": "进行中",
          "startDate": "2026-02-01",
          "endDate": "2026-05-31",
          "progress": 60,
          "auditLeader": "张三",
          "isOutsource": 0,
          "alertLevel": "NORMAL"
        }
      ]
    }
  ]
}
```

**alertLevel 取值：** `NORMAL`（正常）、`WARNING`（滞后）、`CRITICAL`（严重超期）

---

### 4.3 资源负载

#### 4.3.1 获取审计人员负载分布

```
GET /api/audit/v1/statistics/workload
```

**请求参数：**

| 字段 | 类型    | 必填 | 说明 |
| ---- | ------- | ---- | ---- |
| year | integer | 否   | 年度 |

**响应 data：**

```json
{
  "auditLeaders": [
    {
      "userId": "U001",
      "userName": "张三",
      "role": "AUDIT_LEADER",
      "currentProjects": 5,
      "maxCapacity": 6,
      "workloadPercent": 83,
      "projects": [
        { "projectId": "PJ202600101", "projectName": "XX学院院长经责审计", "progress": 60 },
        { "projectId": "PJ202600301", "projectName": "ZZ公司专项审计", "progress": 30 }
      ]
    },
    {
      "userId": "U002",
      "userName": "赵六",
      "role": "AUDIT_LEADER",
      "currentProjects": 7,
      "maxCapacity": 6,
      "workloadPercent": 117,
      "overload": true,
      "projects": []
    }
  ],
  "outsourceOrgs": [
    {
      "orgId": "ORG001",
      "orgName": "XX会计师事务所",
      "currentProjects": 3,
      "maxCapacity": 5,
      "workloadPercent": 60,
      "projects": []
    }
  ]
}
```

> **前端交互说明：** `workloadPercent` ≥ 100 标红高亮，提示管理层该人员/机构超负荷。

---

#### 4.3.2 获取人员负载历史趋势

```
GET /api/audit/v1/statistics/workload-trend
```

**请求参数：**

| 字段   | 类型   | 必填 | 说明               |
| ------ | ------ | ---- | ------------------ |
| userId | string | 是   | 审计人员 ID        |
| months | integer | 否   | 近几个月，默认 12  |

**响应 data：**

```json
{
  "userId": "U001",
  "userName": "张三",
  "trend": [
    { "month": "2025-08", "projectCount": 4, "workloadPercent": 67 },
    { "month": "2025-09", "projectCount": 5, "workloadPercent": 83 }
  ]
}
```

---

### 4.4 进度预警

#### 4.4.1 获取进度预警项目列表（供可视化页标红）

```
GET /api/audit/v1/statistics/alerts
```

**请求参数：**

| 字段      | 类型    | 必填 | 说明                                   |
| --------- | ------- | ---- | -------------------------------------- |
| alertType | integer | 否   | 预警类型（0-超期未启动 / 1-实施滞后 / 2-报告逾期） |
| year      | integer | 否   | 年度                                   |

**响应 data.list 元素：**

```json
{
  "projectId": "PJ202600405",
  "projectName": "XX楼工程审计",
  "batchName": "2026年度工程审计计划",
  "alertType": 0,
  "alertTypeName": "超期未启动",
  "planStartDate": "2026-05-01",
  "daysOverdue": 68,
  "auditLeader": "赵六",
  "auditLeaderId": "U002",
  "notified": true
}
```

---

#### 4.4.2 手动推送预警消息

```
POST /api/audit/v1/statistics/alerts/push
```

**请求体：**

```json
{
  "projectIds": ["PJ202600405"],
  "notifyTargets": ["AUDIT_LEADER", "DIRECTOR"],
  "customMessage": "请尽快启动审计工作"
}
```

---

#### 4.4.3 查询我的消息（当前登录用户的消息列表）

```
GET /api/audit/v1/messages
```

**请求参数：**

| 字段    | 类型    | 必填 | 说明                   |
| ------- | ------- | ---- | ---------------------- |
| isRead  | integer | 否   | 是否已读               |
| pageNum | integer | 否   |                        |
| pageSize | integer | 否   |                        |

**响应 data.list 元素：**

```json
{
  "messageId": "MSG2026001",
  "title": "审计项目超期预警",
  "content": "您负责的「XX楼工程审计」项目已超期68天未启动，请尽快处理。",
  "messageType": "ALERT",
  "relatedProjectId": "PJ202600405",
  "isRead": 0,
  "createTime": "2026-07-08 08:00:00"
}
```

#### 4.4.4 标记单条消息已读

```
PUT /api/audit/v1/messages/{messageId}/read
```

#### 4.4.5 全部标记已读

```
PUT /api/audit/v1/messages/read-all
```

#### 4.4.6 获取未读消息数

```
GET /api/audit/v1/messages/unread-count
```

**响应 data：**

```json
{ "count": 5 }
```

---

## 五、通用字典接口

### 5.1 获取全部枚举字典（一次性加载）

```
GET /api/audit/v1/dict/all
```

**响应 data：**

```json
{
  "planType": [
    { "value": 0, "label": "经济责任审计" },
    { "value": 1, "label": "财务收支审计" },
    { "value": 2, "label": "专项审计" },
    { "value": 3, "label": "工程审计" }
  ],
  "approvalStatus": [
    { "value": 0, "label": "已审批" },
    { "value": 1, "label": "审批中" },
    { "value": 2, "label": "已归档" },
    { "value": 3, "label": "草稿" },
    { "value": 4, "label": "已驳回" }
  ],
  "projectStatus": [
    { "value": 0, "label": "未启动" },
    { "value": 1, "label": "进行中" },
    { "value": 2, "label": "已完结" },
    { "value": 3, "label": "已归档" },
    { "value": 4, "label": "超期" },
    { "value": 5, "label": "暂停" }
  ],
  "unitCategory": [
    { "value": 0, "label": "校内职能部门" },
    { "value": 1, "label": "二级学院" },
    { "value": 2, "label": "直属后勤单位" },
    { "value": 3, "label": "校办企业" },
    { "value": 4, "label": "基建项目部" },
    { "value": 5, "label": "附属医院" }
  ],
  "rectifyStatus": [
    { "value": 0, "label": "未整改" },
    { "value": 1, "label": "整改中" },
    { "value": 2, "label": "已整改" },
    { "value": 3, "label": "无需整改" }
  ],
  "isOutsource": [
    { "value": 0, "label": "校内自审" },
    { "value": 1, "label": "委托中介" }
  ],
  "changeType": [
    { "value": 0, "label": "新增项目" },
    { "value": 1, "label": "调减项目" },
    { "value": 2, "label": "修改周期" },
    { "value": 3, "label": "其他变更" }
  ],
  "alertType": [
    { "value": 0, "label": "超期未启动" },
    { "value": 1, "label": "实施滞后" },
    { "value": 2, "label": "报告逾期未提交" },
    { "value": 3, "label": "超计划预设期限" }
  ],
  "attachType": [
    { "value": "DELEGATION", "label": "审计委托书" },
    { "value": "RESOLUTION", "label": "审批决议" },
    { "value": "TASK", "label": "上级任务文件" },
    { "value": "RESEARCH", "label": "调研材料" },
    { "value": "OTHER", "label": "其他" }
  ]
}
```

### 5.2 获取指定字典

```
GET /api/audit/v1/dict/{dictCode}
```

**dictCode 取值：** `planType` / `approvalStatus` / `projectStatus` / `unitCategory` / `rectifyStatus` / `isOutsource` / `changeType` / `alertType` / `attachType`

---

### 5.3 获取可选的审计组长/审计人员列表

```
GET /api/audit/v1/users/auditors
```

**响应 data：**

```json
[
  { "userId": "U001", "userName": "张三", "role": "AUDIT_LEADER", "availableCapacity": 2 },
  { "userId": "U003", "userName": "李四", "role": "AUDITOR", "availableCapacity": 5 }
]
```

---

### 5.4 获取可选的中介机构列表

```
GET /api/audit/v1/users/outsource-orgs
```

---

## 六、文件上传/下载规范

### 6.1 上传规范

| 项目         | 规格                                          |
| ------------ | --------------------------------------------- |
| 请求方式     | `POST`，`multipart/form-data`                  |
| 字段名       | `file`                                        |
| 单文件上限   | 50 MB                                         |
| 支持格式     | PDF、DOC、DOCX、XLS、XLSX、JPG、PNG、BMP       |
| 批量上传     | 使用 `files` 字段，数组形式                     |
| 预览支持     | PDF 原生预览；Office 转 PDF 预览；图片原图预览   |
| 返回         | `attachId` + `previewUrl` + `downloadUrl`      |

### 6.2 附件业务关联

附件通过 `attachType` 分类，与计划批次的关联关系由上传接口自动绑定（参数中含 batchId）。

### 6.3 Excel 导入模板

所有 Excel 导入功能，前端通过调用模板下载接口获取标准模板，用户填写后上传。后端需校验必填字段、数据格式，并返回逐行错误明细。

---

## 七、WebSocket 推送规范

### 7.1 连接

```
wss://<host>/api/audit/v1/ws?token=<jwt_token>
```

### 7.2 推送消息格式

```json
{
  "type": "ALERT",
  "subType": "PROJECT_OVERDUE",
  "title": "审计项目超期预警",
  "content": "您负责的「XX楼工程审计」项目已超期68天未启动",
  "data": {
    "projectId": "PJ202600405",
    "alertId": "ALT2026005"
  },
  "timestamp": 1751425600000
}
```

### 7.3 推送消息类型

| type              | subType               | 说明                       | 推送对象               |
| ----------------- | --------------------- | -------------------------- | ---------------------- |
| ALERT             | PROJECT_OVERDUE       | 项目超期预警               | 审计组长、审计处负责人 |
| ALERT             | PROJECT_LAGGING       | 项目实施滞后               | 审计组长               |
| ALERT             | REPORT_OVERDUE        | 报告逾期未提交             | 审计组长、处长         |
| APPROVAL          | STEP_REACHED          | 审批流程到达新节点         | 下一节点审批人         |
| APPROVAL          | APPROVAL_REJECTED     | 审批被驳回                 | 提交人                 |
| APPROVAL          | APPROVAL_COMPLETED    | 审批完成                   | 提交人                 |
| CHANGE            | CHANGE_APPROVED       | 计划变更审批通过           | 变更申请人             |
| SYSTEM            | SYNC_COMPLETED        | 数据同步完成               | 系统管理员             |

### 7.4 前端处理方式

- 消息到达后，前端在页面右上角弹出 `ElNotification` 通知
- 同时更新顶部消息未读角标数量
- 预警类消息携带 `projectId`，点击通知可直接跳转至对应项目详情

---

## 附录 A：前端页面 — 接口对照表

| 前端页面                      | 路由                 | 主要使用接口                                                                 |
| ----------------------------- | -------------------- | ---------------------------------------------------------------------------- |
| 年度审计计划清单              | `/plan/form`         | 2.1.1 / 2.1.2 / 2.1.4 / 2.1.5 / 2.1.7 / 2.7.4                              |
| 项目编排（计划内项目列表）     | `/plan/form` (弹窗)  | 2.2.1 / 2.2.2 / 2.2.3 / 2.2.5 / 2.2.8                                       |
| 智能推荐待审对象              | `/plan/form` (弹窗)  | 2.3.1 / 2.3.2                                                                |
| 附件上传 & 预览               | `/plan/form` (弹窗)  | 2.4.1 / 2.4.2 / 2.4.3 / 2.4.4 / 2.4.5                                       |
| 审计方案绑定                  | `/plan/form` (弹窗)  | 2.5.1 / 2.5.2 / 2.5.3 / 2.5.4 / 2.5.5                                       |
| 计划审批流跟踪                | `/plan/track`        | 2.6.2 / 2.6.3 / 2.6.4                                                       |
| 计划变更管理                  | `/plan/form` (弹窗)  | 2.8.1 / 2.8.2 / 2.8.3                                                       |
| 穿透查询                      | `/plan/form` (跳转)  | 2.9.1                                                                        |
| 被审计单位库                  | `/object/unit`       | 3.1.1 / 3.1.2 / 3.1.3 / 3.1.4 / 3.1.5 / 3.1.7 / 3.1.8 / 3.1.9 / 3.1.10    |
| 经责领导干部库                | `/object/lead`       | 3.2.1 / 3.2.2 / 3.2.3 / 3.2.5 / 3.2.6 / 3.2.7                              |
| 进度可视化大屏                | `/statistic`         | 4.1.1 / 4.2.1 / 4.3.1 / 4.4.1                                               |
| 全局字典                      | （应用初始化加载）   | 5.1 / 5.3                                                                     |
| WebSocket 消息推送            | （全局连接）         | 7.1 / 7.2 / 7.3                                                              |

---

## 附录 B：数据库核心表建议

| 表名                     | 说明                 | 核心字段摘要                                         |
| ------------------------ | -------------------- | ---------------------------------------------------- |
| audit_plan_batch         | 审计计划批次         | batchId, batchName, planType, year, approvalStatus   |
| audit_project            | 审计项目             | projectId, batchId, auditObjectId, projectStatus     |
| audit_attachment         | 附件                 | attachId, batchId, fileName, attachType, previewUrl  |
| audit_template           | 审计方案模板         | templateId, templateName, planType, version, content |
| approval_flow            | 审批流               | approvalId, batchId, flowType, currentStep           |
| approval_step            | 审批步骤             | stepId, approvalId, stepOrder, approverId, status    |
| approval_history         | 审批历史             | historyId, batchId, flowType, approvalId, result     |
| plan_change              | 计划变更记录         | changeId, batchId, changeType, changeBefore, changeAfter |
| plan_alert               | 计划预警             | alertId, batchId, projectId, alertType, isRead       |
| audit_object_unit        | 被审计单位           | unitId, unitName, category, fundScale                |
| unit_cadre_change        | 单位干部任免台账     | changeId, unitId, cadreName, position, effectiveDate |
| audit_object_leader      | 经责领导干部         | leaderId, leaderName, unitId, position, isActive     |
| leader_career_history    | 干部任职履历         | recordId, leaderId, unitId, position, startDate       |
| leader_audit_record      | 干部审计记录关联     | recordId, leaderId, projectId, conclusion            |
| audit_rectify_ledger     | 整改台账             | rectifyId, projectId, unitId, issueDescription, rectifyStatus |
| sync_log                 | 数据同步日志         | logId, syncType, status, updateCount                 |
| sys_message              | 系统消息             | messageId, userId, messageType, isRead               |
| sys_user                 | 系统用户             | userId, userName, role                               |
| sys_dict                 | 数据字典             | dictCode, dictValue, dictLabel                       |

---

> **文档版本：** V1.0
>
> **最后更新：** 2026-07-08
>
> **维护人：** 前端开发团队
>
> **变更说明：** 初始版本，覆盖审计信息管理三大模块全部接口定义。
