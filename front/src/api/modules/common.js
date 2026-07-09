import request from '../request'

// ==================== 认证 ====================

/** 登录 */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get('/auth/userinfo')
}

/** 退出登录 */
export function logout() {
  return request.post('/auth/logout')
}

// ==================== 字典 ====================

/** 获取全部枚举字典 */
export function getAllDict() {
  return request.get('/dict/all')
}

/** 获取指定字典 */
export function getDict(dictCode) {
  return request.get(`/dict/${dictCode}`)
}

// ==================== 用户 ====================

/** 获取可选的审计人员列表 */
export function getAuditors() {
  return request.get('/users/auditors')
}

/** 获取可选的中介机构列表 */
export function getOutsourceOrgs() {
  return request.get('/users/outsource-orgs')
}

// ==================== 消息 ====================

/** 查询我的消息列表 */
export function getMessages(params) {
  return request.get('/messages', { params })
}

/** 标记消息已读 */
export function markMessageRead(messageId) {
  return request.put(`/messages/${messageId}/read`)
}

/** 全部标记已读 */
export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}

/** 获取未读消息数 */
export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

// ==================== 用户信息 ====================

/** 更新用户信息 */
export function updateUserInfo(data) {
  return request.put('/auth/userinfo', data)
}

/** 修改密码 */
export function changePassword(data) {
  return request.put('/auth/password', data)
}

// ==================== 系统设置 ====================

/** 获取所有设置 */
export function getSettings() {
  return request.get('/settings')
}

/** 保存通知设置 */
export function saveNotifySettings(data) {
  return request.post('/settings/notify', data)
}

/** 保存界面设置 */
export function saveUISettings(data) {
  return request.post('/settings/ui', data)
}

/** 保存全部设置 */
export function saveAllSettings(data) {
  return request.post('/settings', data)
}

// ==================== 用户管理（超级管理员） ====================

export function getUserList(params) { return request.get('/admin/users', { params }) }
export function createUser(data) { return request.post('/admin/users', data) }
export function updateUser(username, data) { return request.put(`/admin/users/${username}`, data) }
export function deleteUser(username) { return request.delete(`/admin/users/${username}`) }
/** 导入消息通知 */
export function sendImportMsg(module, added, updated) {
  const parts = []
  if (added) parts.push(`新增${added}条`)
  if (updated) parts.push(`更新${updated}条`)
  return request.post('/messages/import-notify', {
    title: `${module}导入完成`,
    content: `批量导入完成：${parts.join('，')}`,
    messageType: 'SYSTEM'
  }).catch(() => {})
}

export function resetUserPassword(username, data) { return request.put(`/admin/users/${username}/password`, data) }
