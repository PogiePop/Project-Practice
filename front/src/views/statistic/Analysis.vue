<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { statisticApi } from '@/api'

const selectedYear = ref(new Date().getFullYear())
const yearOptions = []; for (let i = 5; i >= 0; i--) yearOptions.push(selectedYear.value - i)

const overview = reactive({ totalProjects: 0, completedProjects: 0, inProgressProjects: 0, notStartedProjects: 0, overdueProjects: 0 })
const planTypeStats = ref([])
const maxTotal = ref(1)
const ganttGroupBy = ref('planType')
const ganttData = ref([])
const workloadData = ref([])
const alertProjects = ref([])

const ganttMonths = ['01月','02月','03月','04月','05月','06月','07月','08月','09月','10月','11月','12月']
const getBarStyle = (proj) => {
    const sm = parseInt(proj.startDate?.split('-')[1]) - 1 || 0
    const em = parseInt(proj.endDate?.split('-')[1]) - 1 || 11
    const left = (sm / 12) * 100; const width = ((em - sm + 1) / 12) * 100
    let bg = '#409EFF'; if (proj.alertLevel === 'CRITICAL') bg = '#f56c6c'; else if (proj.alertLevel === 'WARNING') bg = '#e6a23c'; else if (proj.progress === 100) bg = '#67c23a'
    return { left: `${left}%`, width: `${width}%`, background: bg }
}

const fetchAll = async () => {
    const year = selectedYear.value
    try { const d = await statisticApi.getOverview({ year }); if (d?.summaryCards) Object.assign(overview, d.summaryCards); if (d?.byPlanType) { planTypeStats.value = d.byPlanType; maxTotal.value = Math.max(...d.byPlanType.map(s => s.total), 1) } } catch {}
    try { const d = await statisticApi.getGanttData({ year }); if (d?.groups) ganttData.value = d.groups } catch {}
    try { const d = await statisticApi.getWorkload({ year }); if (d?.auditLeaders) workloadData.value = d.auditLeaders } catch {}
    try { const d = await statisticApi.getStatisticAlerts({ year }); if (d?.list) alertProjects.value = d.list } catch {}
}

const pushAlert = async (row) => { try { await statisticApi.pushAlerts({ projectIds: [row.projectId] }); ElMessage.success(`已推送预警`) } catch {} }

onMounted(() => fetchAll())
</script>

<template>
    <div class="analysis-page">
        <!-- 顶栏 -->
        <div class="top-row">
            <span class="page-title">📊 审计进度可视化</span>
            <el-select v-model="selectedYear" @change="fetchAll" style="width:120px" size="small"><el-option v-for="y in yearOptions" :key="y" :label="`${y}年度`" :value="y" /></el-select>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-row">
            <div class="stat-card"><div class="stat-num">{{ overview.totalProjects }}</div><div class="stat-label">项目总数</div></div>
            <div class="stat-card done"><div class="stat-num" style="color:#67c23a">{{ overview.completedProjects }}</div><div class="stat-label">已完成</div></div>
            <div class="stat-card ing"><div class="stat-num" style="color:#409EFF">{{ overview.inProgressProjects }}</div><div class="stat-label">进行中</div></div>
            <div class="stat-card pending"><div class="stat-num" style="color:#909399">{{ overview.notStartedProjects }}</div><div class="stat-label">未启动</div></div>
            <div class="stat-card alert"><div class="stat-num" style="color:#f56c6c">{{ overview.overdueProjects }}</div><div class="stat-label">超期预警</div></div>
        </div>

        <!-- 柱状图 -->
        <el-card shadow="never" class="chart-card">
            <template #header><span class="chart-title">📊 按计划类型分布</span></template>
            <div class="bar-chart">
                <div v-for="stat in planTypeStats" :key="stat.planType" class="bar-row">
                    <div class="bar-label">{{ stat.planTypeName }}</div>
                    <div class="bar-track">
                        <div class="bar-seg done" :style="{ width: (stat.completed / maxTotal * 100) + '%' }">{{ stat.completed > 2 ? stat.completed : '' }}</div>
                        <div class="bar-seg ing" :style="{ width: (stat.inProgress / maxTotal * 100) + '%', left: (stat.completed / maxTotal * 100) + '%' }">{{ stat.inProgress > 2 ? stat.inProgress : '' }}</div>
                        <div class="bar-seg overdue" :style="{ width: (stat.overdue / maxTotal * 100) + '%', left: ((stat.completed + stat.inProgress) / maxTotal * 100) + '%' }">{{ stat.overdue > 1 ? stat.overdue : '' }}</div>
                    </div>
                    <div class="bar-total">{{ stat.total }}</div>
                </div>
            </div>
            <div class="bar-legend"><span><i style="background:#67c23a"></i>已完成</span><span><i style="background:#409EFF"></i>进行中</span><span><i style="background:#f56c6c"></i>超期</span></div>
        </el-card>

        <!-- 甘特图 -->
        <el-card shadow="never" class="chart-card" style="flex:1">
            <template #header><div class="chart-header-row"><span class="chart-title">📅 项目进度甘特图</span><el-radio-group v-model="ganttGroupBy" size="small"><el-radio-button value="planType">按类型</el-radio-button><el-radio-button value="status">按阶段</el-radio-button></el-radio-group></div></template>
            <div class="gantt-wrap">
                <div class="gantt-header"><div class="gantt-label-col"></div><div class="gantt-months"><div v-for="m in ganttMonths" :key="m" class="gantt-month">{{ m }}</div></div></div>
                <div v-for="group in ganttData" :key="group.groupName" class="gantt-group">
                    <div class="gantt-group-title">{{ group.groupName }}</div>
                    <div v-for="proj in group.projects" :key="proj.projectName" class="gantt-row">
                        <div class="gantt-label"><span class="proj-name" :title="proj.projectName">{{ proj.projectName }}</span><span style="font-size:11px;color:#909399">{{ proj.auditLeader }}</span></div>
                        <div class="gantt-track"><div class="gantt-bar" :style="getBarStyle(proj)"><span class="gantt-bar-text">{{ proj.progress }}%</span></div></div>
                    </div>
                </div>
            </div>
            <div class="gantt-legend"><span><i style="background:#67c23a"></i>已完成</span><span><i style="background:#409EFF"></i>正常</span><span><i style="background:#e6a23c"></i>滞后</span><span><i style="background:#f56c6c"></i>严重超期</span></div>
        </el-card>

        <!-- 负载 + 预警 -->
        <div class="bottom-row">
            <el-card shadow="never" class="half-card">
                <template #header><span class="chart-title">👥 审计组长负载</span></template>
                <div v-for="wl in workloadData" :key="wl.userId" class="wl-row">
                    <div class="wl-info"><span class="wl-name">{{ wl.userName }}</span><span class="wl-role">{{ wl.role }}</span></div>
                    <div class="wl-bar"><el-progress :percentage="Math.min(wl.workloadPercent, 100)" :color="wl.overload ? '#f56c6c' : wl.workloadPercent > 80 ? '#e6a23c' : '#409EFF'" :stroke-width="16" :text-inside="true"><span style="font-size:11px">{{ wl.currentProjects }}/{{ wl.maxCapacity }}</span></el-progress></div>
                    <el-tag v-if="wl.overload" type="danger" size="small" effect="dark">超负荷</el-tag>
                </div>
            </el-card>
            <el-card shadow="never" class="half-card alert-panel">
                <template #header><span class="chart-title" style="color:#f56c6c">⚠️ 进度预警</span></template>
                <div v-for="a in alertProjects" :key="a.projectName" class="alert-row">
                    <div class="alert-left">
                        <div class="alert-name"><el-tag :type="a.alertType === 0 ? 'danger' : 'warning'" size="small" effect="dark">{{ a.alertTypeName }}</el-tag><span>{{ a.projectName }}</span></div>
                        <div class="alert-detail">超期 <b>{{ a.daysOverdue }}</b> 天 · {{ a.auditLeader }} <el-tag v-if="a.notified" type="success" size="small" effect="plain">已通知</el-tag><el-tag v-else type="info" size="small" effect="plain">未通知</el-tag></div>
                    </div>
                    <el-button size="small" type="primary" @click="pushAlert(a)" :disabled="a.notified">{{ a.notified ? '已推送' : '推送提醒' }}</el-button>
                </div>
                <div v-if="!alertProjects.length" class="empty-hint">暂无预警项目</div>
            </el-card>
        </div>
    </div>
</template>

<style scoped>
.analysis-page { padding: 16px; height: 100%; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; }
.top-row { display: flex; align-items: center; gap: 16px; }
.page-title { font-size: 16px; font-weight: 700; color: #303133; }

.stats-row { display: flex; gap: 12px; }
.stat-card { flex: 1; background: #fff; border-radius: 10px; padding: 18px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.stat-card.done { border-top: 3px solid #67c23a; } .stat-card.ing { border-top: 3px solid #409EFF; } .stat-card.pending { border-top: 3px solid #909399; } .stat-card.alert { border-top: 3px solid #f56c6c; }
.stat-num { font-size: 28px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }

.chart-card { border-radius: 10px; }
.chart-title { font-weight: 700; font-size: 14px; }
.chart-header-row { display: flex; justify-content: space-between; align-items: center; }

/* 柱状图 */
.bar-chart { padding: 0 10px; }
.bar-row { display: flex; align-items: center; margin-bottom: 10px; }
.bar-label { width: 110px; font-size: 13px; color: #606266; flex-shrink: 0; }
.bar-track { flex: 1; height: 26px; background: #f0f2f5; border-radius: 4px; position: relative; overflow: hidden; }
.bar-seg { position: absolute; top: 0; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #fff; font-weight: 600; }
.bar-seg.done { background: #67c23a; left: 0; } .bar-seg.ing { background: #409EFF; } .bar-seg.overdue { background: #f56c6c; }
.bar-total { width: 32px; text-align: center; font-weight: 700; font-size: 14px; flex-shrink: 0; }
.bar-legend { display: flex; gap: 18px; margin-top: 6px; margin-left: 110px; font-size: 12px; color: #909399; }
.bar-legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }

/* 甘特图 */
.gantt-wrap { overflow-x: auto; }
.gantt-header { display: flex; margin-bottom: 4px; }
.gantt-label-col { width: 200px; flex-shrink: 0; }
.gantt-months { flex: 1; display: flex; }
.gantt-month { flex: 1; text-align: center; font-size: 11px; color: #c0c4cc; border-left: 1px solid #f5f5f5; }
.gantt-group { margin-bottom: 10px; }
.gantt-group-title { font-size: 13px; font-weight: 700; color: #303133; padding: 3px 0; border-bottom: 1px solid #ebeef5; margin-bottom: 3px; }
.gantt-row { display: flex; align-items: center; margin-bottom: 5px; }
.gantt-label { width: 200px; flex-shrink: 0; display: flex; flex-direction: column; padding-right: 8px; }
.proj-name { font-size: 12px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gantt-track { flex: 1; height: 24px; background: #f5f7fa; border-radius: 3px; position: relative; }
.gantt-bar { position: absolute; top: 2px; height: 20px; border-radius: 3px; display: flex; align-items: center; justify-content: center; min-width: 30px; }
.gantt-bar-text { font-size: 10px; color: #fff; font-weight: 600; }
.gantt-legend { display: flex; gap: 16px; margin-top: 8px; font-size: 12px; color: #909399; }
.gantt-legend i { display: inline-block; width: 12px; height: 12px; border-radius: 2px; vertical-align: middle; margin-right: 4px; }

/* 负载 + 预警 */
.bottom-row { display: flex; gap: 12px; }
.half-card { flex: 1; border-radius: 10px; }
.wl-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.wl-info { width: 75px; flex-shrink: 0; } .wl-name { display: block; font-size: 14px; font-weight: 600; } .wl-role { font-size: 11px; color: #909399; }
.wl-bar { flex: 1; }

.alert-panel :deep(.el-card__header) { background: #fef0f0; border-bottom-color: #fde2e2; }
.alert-row { display: flex; align-items: center; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; gap: 8px; }
.alert-row:last-child { border-bottom: none; }
.alert-left { flex: 1; min-width: 0; }
.alert-name { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; }
.alert-detail { margin-top: 4px; font-size: 12px; color: #909399; }
.empty-hint { text-align: center; padding: 30px; color: #c0c4cc; font-size: 14px; }
</style>
