import request from '../request'

// ==================== 计划批次管理 ====================

/** 查询计划批次列表（分页） */
export function getPlanBatches(params) {
  return request.get('/plan/batches', { params })
}

/** 查询计划批次详情 */
export function getPlanBatch(batchId) {
  return request.get(`/plan/batches/${batchId}`)
}

/** 新建计划批次 */
export function createPlanBatch(data) {
  return request.post('/plan/batches', data)
}

/** 批量新建计划批次 */
export function batchCreatePlanBatches(data) {
  return request.post('/plan/batches/batch', data)
}

/** 编辑计划批次 */
export function updatePlanBatch(batchId, data) {
  return request.put(`/plan/batches/${batchId}`, data)
}

/** 删除计划批次 */
export function deletePlanBatch(batchId) {
  return request.delete(`/plan/batches/${batchId}`)
}

/** 导出计划列表 Excel */
export function exportPlanBatches(params) {
  return request.get('/plan/batches/export', { params, responseType: 'blob' })
}

// ==================== 项目编排管理 ====================

/** 查询计划批次下的项目列表 */
export function getProjects(batchId, params) {
  return request.get(`/plan/batches/${batchId}/projects`, { params })
}

/** 手动新增项目 */
export function createProject(batchId, data) {
  return request.post(`/plan/batches/${batchId}/projects`, data)
}

/** Excel 批量导入项目 - 上传文件 */
export function importProjects(batchId, formData) {
  return request.post(`/plan/batches/${batchId}/projects/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 下载项目导入模板 */
export function downloadProjectTemplate() {
  return request.get('/plan/projects/template/download', { responseType: 'blob' })
}

/** 批量修改项目字段 */
export function batchUpdateProjects(batchId, data) {
  return request.put(`/plan/batches/${batchId}/projects/batch-update`, data)
}

/** 编辑单个项目 */
export function updateProject(projectId, data) {
  return request.put(`/plan/projects/${projectId}`, data)
}

/** 删除项目 */
export function deleteProject(projectId) {
  return request.delete(`/plan/projects/${projectId}`)
}

/** 导出项目列表 */
export function exportProjects(batchId, params) {
  return request.get(`/plan/batches/${batchId}/projects/export`, { params, responseType: 'blob' })
}

// ==================== 智能推荐 ====================

/** 获取推荐待审对象清单 */
export function getRecommendObjects(params) {
  return request.get('/plan/recommend/objects', { params })
}

/** 一键将推荐对象加入计划批次 */
export function importRecommendToBatch(batchId, data) {
  return request.post(`/plan/batches/${batchId}/projects/recommend-import`, data)
}

// ==================== 附件管理 ====================

/** 上传附件 */
export function uploadAttachment(batchId, formData) {
  return request.post(`/plan/batches/${batchId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 查询附件列表 */
export function getAttachments(batchId) {
  return request.get(`/plan/batches/${batchId}/attachments`)
}

/** 删除附件 */
export function deleteAttachment(batchId, attachId) {
  return request.delete(`/plan/batches/${batchId}/attachments/${attachId}`)
}

/** 文件预览 URL（绝对路径，供 window.open 使用） */
export function getPreviewUrl(attachId) {
  return `/api/audit/v1/files/${attachId}/preview`
}

/** 文件下载 URL（绝对路径，供下载链接使用） */
export function getDownloadUrl(attachId) {
  return `/api/audit/v1/files/${attachId}/download`
}

// ==================== 审计方案绑定 ====================

/** 查询标准化审计方案模板库 */
export function getTemplates(params) {
  return request.get('/templates', { params })
}

/** 为计划批次批量关联方案模板 */
export function bindTemplates(batchId, data) {
  return request.post(`/plan/batches/${batchId}/templates`, data)
}

/** 为单个项目定制专属方案 */
export function setCustomTemplate(projectId, data) {
  return request.put(`/plan/projects/${projectId}/custom-template`, data)
}

/** 查看计划批次关联的方案 */
export function getBatchTemplates(batchId) {
  return request.get(`/plan/batches/${batchId}/templates`)
}

/** 穿透查看方案完整内容 */
export function getTemplateContent(templateId) {
  return request.get(`/templates/${templateId}/content`)
}

/** 查看项目定制方案内容 */
export function getProjectCustomTemplate(projectId) {
  return request.get(`/plan/projects/${projectId}/custom-template/content`)
}

// ==================== 审批流管理 ====================

/** 提交审批 */
export function submitApproval(batchId, data) {
  return request.post(`/plan/batches/${batchId}/approval/submit`, data)
}

/** 查询审批流进度 */
export function getApprovalProgress(batchId) {
  return request.get(`/plan/batches/${batchId}/approval/progress`)
}

/** 审批操作（通过/驳回） */
export function approvalAction(batchId, data) {
  return request.post(`/plan/batches/${batchId}/approval/action`, data)
}

/** 重新提交被驳回的计划 */
export function resubmitBatch(batchId) {
  return request.post(`/plan/batches/${batchId}/approval/resubmit`)
}

/** 查询审批历史记录 */
export function getApprovalHistory(batchId) {
  return request.get(`/plan/batches/${batchId}/approval/history`)
}

/** 获取审批流程模板列表 */
export function getApprovalFlowTemplates() {
  return request.get('/approval/flow-templates')
}

/** 创建审批流程模板 */
export function createApprovalFlowTemplate(data) {
  return request.post('/approval/flow-templates', data)
}

// ==================== 计划预警 ====================

/** 查询预警记录列表 */
export function getPlanAlerts(params) {
  return request.get('/plan/alerts', { params })
}

/** 标记预警已读 */
export function markAlertRead(alertId) {
  return request.put(`/plan/alerts/${alertId}/read`)
}

/** 批量标记预警已读 */
export function batchMarkAlertsRead(data) {
  return request.put('/plan/alerts/batch-read', data)
}

/** 查询首页统计卡片数据 */
export function getPlanSummary() {
  return request.get('/plan/statistics/summary')
}

// ==================== 计划变更管理 ====================

/** 发起计划变更申请 */
export function createPlanChange(batchId, data) {
  return request.post(`/plan/batches/${batchId}/changes`, data)
}

/** 查询变更记录列表 */
export function getPlanChanges(batchId) {
  return request.get(`/plan/batches/${batchId}/changes`)
}

/** 查询变更前后对比详情 */
export function getChangeDiff(changeId) {
  return request.get(`/plan/changes/${changeId}/diff`)
}

/** 确认变更（应用变更数据到计划） */
export function approveChange(batchId, changeId) {
  return request.post(`/plan/batches/${batchId}/changes/${changeId}/approve`)
}

// ==================== 穿透查询 ====================

/** 计划穿透查询 */
export function penetrateProject(projectId) {
  return request.get(`/plan/projects/${projectId}/penetrate`)
}

/** 导出后归档（仅100%进度的计划） */
export function archiveBatch(batchId) {
  return request.post(`/plan/batches/${batchId}/archive`)
}
export function archiveBatchList(batchIds) {
  return request.post('/plan/batches/archive-batch', { batchIds })
}

/** 保存审计记录（金额、发现、结论） */
export function saveAuditRecord(batchId, data) {
  return request.put(`/plan/batches/${batchId}/audit-record`, data)
}
