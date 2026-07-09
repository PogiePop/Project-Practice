<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { planApi } from '@/api'

const currentApproval = reactive({ batchId: 'JH2026001', batchName: '', currentStep: 0 })
const steps = ref([])
const approvalHistory = ref([])
const changeLogs = ref([])
const activeTab = ref('progress')
const approveComment = ref('')
const showApprovePanel = ref(false)
const selectedPlan = ref('JH2026001')
const planOptions = ref([])

const getStepStatus = (s) => ({ COMPLETED: 'success', ACTIVE: 'process', PENDING: 'wait', REJECTED: 'error' }[s] || 'wait')

// 加载可选计划批次列表
const loadPlanOptions = async () => {
    try {
        const data = await planApi.getPlanBatches({ pageSize: 100 })
        planOptions.value = (data?.list || []).map(b => ({
            value: b.batchId,
            label: b.batchId + ' - ' + b.batchName,
            name: b.batchName
        }))
        // 默认选中第一个
        if (planOptions.value.length > 0 && !currentApproval.batchName) {
            const first = planOptions.value[0]
            selectedPlan.value = first.value
            currentApproval.batchId = first.value
            currentApproval.batchName = first.name
        }
    } catch {}
}

const fetchApprovalData = async () => {
    try {
        const p = await planApi.getApprovalProgress(currentApproval.batchId)
        if (p?.steps) steps.value = p.steps
        if (p?.currentStep !== undefined) currentApproval.currentStep = p.currentStep
    } catch {}
    try {
        const h = await planApi.getApprovalHistory(currentApproval.batchId)
        if (h?.list) approvalHistory.value = h.list
    } catch {}
    try {
        const c = await planApi.getPlanChanges(currentApproval.batchId)
        if (c?.list) changeLogs.value = c.list
    } catch {}
}

const onPlanChange = (val) => {
    const opt = planOptions.value.find(o => o.value === val)
    if (opt) {
        currentApproval.batchId = opt.value
        currentApproval.batchName = opt.name
    }
    fetchApprovalData()
}

const handleApprove = async () => {
    if (!approveComment.value) { ElMessage.warning('请填写审批意见'); return }
    try {
        await planApi.approvalAction(currentApproval.batchId, { action: 'APPROVE', comment: approveComment.value })
        ElMessage.success('审批通过')
        approveComment.value = ''; showApprovePanel.value = false
        fetchApprovalData()
    } catch {}
}

const handleReject = async () => {
    if (!approveComment.value) { ElMessage.warning('请填写驳回原因'); return }
    try {
        await planApi.approvalAction(currentApproval.batchId, { action: 'REJECT', comment: approveComment.value })
        ElMessage.warning('已驳回，已生成整改记录')
        approveComment.value = ''; showApprovePanel.value = false
        fetchApprovalData()
    } catch {}
}

const handleApproveChange = async (row) => {
    try {
        await planApi.approveChange(currentApproval.batchId, row.change_id)
        ElMessage.success('变更已确认并生效')
        fetchApprovalData()
    } catch {}
}

// 审计记录表单
const showAuditRecordForm = ref(false)
const batchBudget = ref(null)
const auditRecord = reactive({ actualAmount: null })
const openAuditForm = async () => {
    showAuditRecordForm.value = !showAuditRecordForm.value
    if (showAuditRecordForm.value) {
        try { const data = await planApi.getPlanBatch(currentApproval.batchId); batchBudget.value = data?.projectAmount || 0 } catch { batchBudget.value = 0 }
    }
}
const handleSaveAuditRecord = async () => {
    try {
        await planApi.saveAuditRecord(currentApproval.batchId, { actualAmount: auditRecord.actualAmount })
        ElMessage.success('实际使用金额已保存')
        showAuditRecordForm.value = false
    } catch {}
}

const isRejected = () => steps.value.some(s => s.status === 'REJECTED')

const handleResubmit = async () => {
    try {
        await planApi.resubmitBatch(currentApproval.batchId)
        ElMessage.success('已重新提交，审批流程已重置')
        fetchApprovalData()
    } catch {}
}

onMounted(async () => {
    await loadPlanOptions()
    fetchApprovalData()
})
</script>

<template>
    <div class="track-page">
        <!-- 选择器 -->
        <div class="selector-bar">
            <span class="selector-label">计划批次：</span>
            <el-select v-model="selectedPlan" @change="onPlanChange" style="width:480px" placeholder="请选择计划批次" filterable>
                <el-option v-for="opt in planOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
        </div>

        <el-card class="approval-card" shadow="never">
            <template #header>
                <div class="approval-header-row">
                    <span class="approval-title"><el-icon><Check /></el-icon> 审批流跟踪 — {{ currentApproval.batchName || currentApproval.batchId }}</span>
                    <el-radio-group v-model="activeTab" size="small">
                        <el-radio-button value="progress">审批进度</el-radio-button>
                        <el-radio-button value="history">历史记录</el-radio-button>
                        <el-radio-button value="changes">变更记录</el-radio-button>
                    </el-radio-group>
                </div>
            </template>

            <!-- 审批进度 -->
            <div v-show="activeTab === 'progress'">
                <el-steps :active="currentApproval.currentStep" finish-status="success" align-center style="margin:16px 0 24px">
                    <el-step v-for="step in steps" :key="step.stepOrder" :title="step.stepName" :status="getStepStatus(step.status)">
                        <template #description>
                            <div v-if="step.approverName" style="font-size:12px;margin-top:4px;color:#909399">{{ step.approverName }}</div>
                        </template>
                    </el-step>
                </el-steps>

                <el-timeline v-if="steps.filter(s => s.comment).length > 0">
                    <el-timeline-item v-for="step in steps.filter(s => s.comment)" :key="step.stepOrder" :timestamp="step.operateTime" placement="top" :type="step.status === 'COMPLETED' ? 'success' : 'danger'">
                        <el-card shadow="hover" size="small" style="margin-top:-6px"><strong>{{ step.stepName }} — {{ step.approverName }}</strong><div style="margin-top:4px;color:#606266">{{ step.comment }}</div></el-card>
                    </el-timeline-item>
                </el-timeline>

                <div v-if="!isRejected() && currentApproval.currentStep >= 0 && currentApproval.currentStep < steps.length" style="margin-top:20px">
                    <el-button type="primary" @click="showApprovePanel = !showApprovePanel" size="small">{{ showApprovePanel ? '收起' : '填写审批意见' }}</el-button>
                </div>
                <div v-if="!isRejected() && showApprovePanel" class="approve-panel">
                    <el-input v-model="approveComment" type="textarea" :rows="3" placeholder="请输入审批意见..." style="margin-bottom:12px" />
                    <div style="display:flex;gap:10px"><el-button type="success" @click="handleApprove">审批通过</el-button><el-button type="danger" @click="handleReject">驳回</el-button></div>
                </div>
                <div v-if="isRejected()" style="margin-top:12px;padding:16px;background:#fef0f0;border-radius:8px;border:1px solid #fde2e2">
                    <p style="color:#f56c6c;margin:0 0 10px">⚠️ 该计划已被驳回，已自动生成整改记录。修改后请重新提交。</p>
                    <el-button type="primary" @click="handleResubmit">🔄 重新提交审批</el-button>
                </div>
                <!-- 审计记录表单 -->
                <div style="margin-top:16px">
                    <el-button @click="openAuditForm" size="small" type="warning" plain>
                        📝 {{ showAuditRecordForm ? '收起审计记录' : '填写审计记录' }}
                    </el-button>
                </div>
                <div v-if="showAuditRecordForm" style="margin-top:12px;padding:16px;background:#fafbfc;border-radius:8px;border:1px solid #ebeef5">
                    <el-form :model="auditRecord" label-width="120px" size="small">
                        <el-form-item label="实际使用金额(万)">
                            <el-input-number v-model="auditRecord.actualAmount" :min="0" :precision="2" style="width:100%" />
                            <div v-if="batchBudget != null" style="margin-top:4px;font-size:12px;color:#909399">
                                预算金额：<b style="color:#409EFF">{{ batchBudget }}</b> 万元
                                <span v-if="auditRecord.actualAmount > batchBudget" style="color:#f56c6c"> ⚠️ 超出预算！</span>
                            </div>
                        </el-form-item>
                        <el-form-item><el-button type="primary" @click="handleSaveAuditRecord">确认实际使用金额</el-button></el-form-item>
                    </el-form>
                </div>
            </div>

            <!-- 审批历史 -->
            <div v-show="activeTab === 'history'">
                <el-table :data="approvalHistory" size="small" empty-text="暂无审批历史">
                    <el-table-column prop="approval_id" label="审批编号" width="170" />
                    <el-table-column label="类型" width="100">
                        <template #default="{ row }">
                            <el-tag size="small" :type="row.flow_type === 'NEW_PLAN' ? 'primary' : 'warning'">
                                {{ row.flow_type === 'NEW_PLAN' ? '新增计划' : row.flow_type === 'PLAN_CHANGE' ? '计划变更' : row.flow_type }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="submit_by" label="提交人" width="90" />
                    <el-table-column prop="submit_time" label="提交时间" width="170" />
                    <el-table-column prop="result" label="结果" width="90">
                        <template #default="{ row }">
                            <el-tag size="small" :type="row.result === '已通过' ? 'success' : row.result === '审批中' ? 'warning' : 'danger'">{{ row.result }}</el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </div>

            <!-- 变更记录 -->
            <div v-show="activeTab === 'changes'">
                <el-table :data="changeLogs" size="small" empty-text="暂无变更记录">
                    <el-table-column prop="change_id" label="变更编号" width="170" />
                    <el-table-column prop="change_type_name" label="变更类型" width="110">
                        <template #default="{ row }">
                            <el-tag size="small">{{ row.change_type_name }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="reason" label="变更原因" min-width="220" show-overflow-tooltip />
                    <el-table-column prop="apply_time" label="申请时间" width="160" />
                    <el-table-column prop="approval_status_name" label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag size="small" :type="row.approval_status === 0 ? 'success' : 'warning'">{{ row.approval_status_name }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80">
                        <template #default="{ row }">
                            <el-button v-if="row.approval_status != 0 || row.approval_status_name === '待确认'" size="small" type="primary" link @click="handleApproveChange(row)">确认</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </el-card>
    </div>
</template>

<style scoped>
.track-page { padding: 16px; height: 100%; display: flex; flex-direction: column; gap: 12px; }
.selector-bar { background: #fff; border-radius: 10px; padding: 12px 18px; display: flex; align-items: center; gap: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.03); }
.selector-label { font-weight: 600; font-size: 14px; color: #303133; white-space: nowrap; }
.approval-card { flex: 1; border-radius: 10px; }
.approval-card :deep(.el-card__header) { padding: 10px 18px; background: #fafbfc; border-bottom: 1px solid #ebeef5; }
.approval-header-row { display: flex; justify-content: space-between; align-items: center; }
.approval-title { font-weight: 700; font-size: 15px; }
.approve-panel { margin-top: 12px; padding: 16px; background: #fafbfc; border-radius: 8px; border: 1px solid #ebeef5; }
:deep(.el-steps) { flex-wrap: wrap; }
</style>
