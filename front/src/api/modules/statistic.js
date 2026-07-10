import request from '../request'

/** 获取年度进度总览 */
export function getOverview(params) {
  return request.get('/statistics/overview', { params })
}

/** 获取甘特图数据 */
export function getGanttData(params) {
  return request.get('/statistics/gantt', { params })
}

/** 获取审计人员负载分布 */
export function getWorkload(params) {
  return request.get('/statistics/workload', { params })
}

/** 获取人员负载历史趋势 */
export function getWorkloadTrend(params) {
  return request.get('/statistics/workload-trend', { params })
}

/** 获取进度预警项目列表 */
export function getStatisticAlerts(params) {
  return request.get('/statistics/alerts', { params })
}

/** 手动推送预警消息 */
export function pushAlerts(data) {
  return request.post('/statistics/alerts/push', data)
}
