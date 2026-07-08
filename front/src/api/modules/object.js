import request from '../request'

// ==================== 被审计单位库 ====================

/** 查询被审计单位列表（分页） */
export function getUnits(params) {
  return request.get('/objects/units', { params })
}

/** 获取单位分类树 */
export function getUnitCategoryTree() {
  return request.get('/objects/units/category-tree')
}

/** 查询单位详情 */
export function getUnitDetail(unitId) {
  return request.get(`/objects/units/${unitId}`)
}

/** 新建被审计单位 */
export function createUnit(data) {
  return request.post('/objects/units', data)
}

/** 编辑被审计单位 */
export function updateUnit(unitId, data) {
  return request.put(`/objects/units/${unitId}`, data)
}

/** 删除被审计单位 */
export function deleteUnit(unitId) {
  return request.delete(`/objects/units/${unitId}`)
}

/** 查询单位历年干部任免台账 */
export function getUnitCadreChanges(unitId) {
  return request.get(`/objects/units/${unitId}/cadre-changes`)
}

/** 查询单位历年审计记录 */
export function getUnitAuditRecords(unitId) {
  return request.get(`/objects/units/${unitId}/audit-records`)
}

/** 查询单位整改台账 */
export function getUnitRectifyLedger(unitId) {
  return request.get(`/objects/units/${unitId}/rectify-ledger`)
}

/** 查询单位专项资金项目清单 */
export function getUnitSpecialFunds(unitId) {
  return request.get(`/objects/units/${unitId}/special-funds`)
}

/** 全局搜索审计对象 */
export function searchObjects(params) {
  return request.get('/objects/search', { params })
}

// ==================== 经责领导干部库 ====================

/** 查询经责领导干部列表（分页） */
export function getLeaders(params) {
  return request.get('/objects/leaders', { params })
}

/** 查询领导干部详情 */
export function getLeaderDetail(leaderId) {
  return request.get(`/objects/leaders/${leaderId}`)
}

/** 新建经责领导干部 */
export function createLeader(data) {
  return request.post('/objects/leaders', data)
}

/** 编辑经责领导干部 */
export function updateLeader(leaderId, data) {
  return request.put(`/objects/leaders/${leaderId}`, data)
}

/** 删除经责领导干部 */
export function deleteLeader(leaderId) {
  return request.delete(`/objects/leaders/${leaderId}`)
}

/** 查询干部任职履历全记录 */
export function getLeaderCareerHistory(leaderId) {
  return request.get(`/objects/leaders/${leaderId}/career-history`)
}

/** 手动添加任职记录 */
export function addCareerRecord(leaderId, data) {
  return request.post(`/objects/leaders/${leaderId}/career-history`, data)
}

/** 编辑任职记录 */
export function updateCareerRecord(leaderId, recordId, data) {
  return request.put(`/objects/leaders/${leaderId}/career-history/${recordId}`, data)
}

/** 删除任职记录 */
export function deleteCareerRecord(leaderId, recordId) {
  return request.delete(`/objects/leaders/${leaderId}/career-history/${recordId}`)
}

/** 查询干部关联的所有经责审计项目 */
export function getLeaderAuditProjects(leaderId) {
  return request.get(`/objects/leaders/${leaderId}/audit-projects`)
}

/** 生成滚动经责审计推荐清单 */
export function getLeaderRecommendForAudit(params) {
  return request.get('/objects/leaders/recommend-for-audit', { params })
}

// ==================== 数据同步 ====================

/** 手动触发同步 */
export function triggerSync(data) {
  return request.post('/sync/trigger', data)
}

/** 查询同步日志 */
export function getSyncLogs(params) {
  return request.get('/sync/logs', { params })
}
