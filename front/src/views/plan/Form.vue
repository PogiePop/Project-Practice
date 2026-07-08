<script setup>
import { formatDateYYYYMMDD } from '@/utils/date';
import { readExcel, exportExcel, downloadImportTemplate } from '@/utils/excel';
import { ElMessage } from 'element-plus';
import { reactive, ref, onMounted } from 'vue'
import { planApi, commonApi, objectApi } from '@/api'

// ============ 筛选 ============
const formInline = reactive({ planName: '', year: '', type: '', state: '' });
const allYear = [];
let now = new Date().getFullYear();
for (let i = 9; i >= 0; i--) allYear.push(now - i);
const allType = ['经济责任审计', '财务收支审计', '专项审计', '工程审计'];
const allState = ['已审批', '审批中', '已归档', '草稿'];

const getProgressStatus = (val) => {
    if (val > 0 && val <= 50) return 'exception'
    if (val > 50 && val <= 70) return ''
    if (val > 70 && val < 100) return 'warning'
    return 'success'
}

// ============ 新建/编辑弹窗 ============
const dialogVisible = ref(false);
const dialogTitle = ref('新建年度审计计划批次');
const isEdit = ref(false);
const newPlanInfo = reactive({ id: '', name: '', type: 0, startDate: '', endDate: '', auditLeader: '', remark: '', unitId: '', leaderId: '' });
const formRef = ref(null);
const clearNewPlanInfo = () => {
    formRef.value?.clearValidate();
    Object.keys(newPlanInfo).forEach(k => newPlanInfo[k] = k === 'type' ? 0 : '');
}
const auditorList = ref([]); const unitList = ref([]); const leaderList = ref([])
const loadAuditors = async () => { try { const data = await commonApi.getAuditors(); auditorList.value = data || [] } catch { } }
const loadUnitsAndLeaders = async () => {
    try { const d = await objectApi.getUnits({ pageSize: 200 }); unitList.value = d?.list || [] } catch { }
    try { const d = await objectApi.getLeaders({ pageSize: 200 }); leaderList.value = d?.list || [] } catch { }
}
const addNewPlan = () => { dialogTitle.value = '新建年度审计计划批次'; isEdit.value = false; clearNewPlanInfo(); loadAuditors(); loadUnitsAndLeaders(); dialogVisible.value = true; };
const editPlan = (row) => {
    dialogTitle.value = '编辑年度审计计划批次'; isEdit.value = true
    newPlanInfo.id = row.date; newPlanInfo.name = row.name; newPlanInfo.type = row.type
    newPlanInfo.auditLeader = row.auditLeader || ''
    newPlanInfo.unitId = row.unitId || ''; newPlanInfo.leaderId = row.leaderId || ''
    newPlanInfo.remark = row.remark || ''
    // 从 during 字段解析日期
    const parts = (row.during || '').split('~').map(s => s.trim())
    newPlanInfo.startDate = parts[0] || ''
    newPlanInfo.endDate = parts[1] || ''
    loadAuditors(); loadUnitsAndLeaders(); dialogVisible.value = true;
};
const formRules = reactive({
    id: [{ required: true, message: '请填写计划编号', trigger: 'blur' }],
    name: [{ required: true, message: '请填写计划名称', trigger: 'blur' }],
    type: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
});

const submitPlan = async () => {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    try {
        const sd = newPlanInfo.startDate ? new Date(newPlanInfo.startDate).toISOString().slice(0, 10) : ''
        const ed = newPlanInfo.endDate ? new Date(newPlanInfo.endDate).toISOString().slice(0, 10) : ''
        const yr = sd ? parseInt(sd.slice(0, 4)) : new Date().getFullYear()
        const payload = {
            batchName: newPlanInfo.name, planType: newPlanInfo.type, year: yr,
            startDate: sd, endDate: ed,
            auditLeader: newPlanInfo.auditLeader || undefined, remark: newPlanInfo.remark || undefined,
            unitId: newPlanInfo.unitId || undefined, leaderId: newPlanInfo.leaderId || undefined
        }
        if (isEdit.value) { await planApi.updatePlanBatch(newPlanInfo.id, payload) }
        else { await planApi.createPlanBatch({ ...payload, batchId: newPlanInfo.id }) }
        ElMessage.success(isEdit.value ? '修改成功' : '提交成功')
        dialogVisible.value = false; fetchTableData()
    } catch { }
};

// ============ 表格数据 ============
const tableData = ref([]);
const loading = ref(false)
const fetchTableData = async () => {
    loading.value = true
    try {
        const data = await planApi.getPlanBatches({
            keyword: formInline.planName || undefined,
            year: formInline.year || undefined, planType: formInline.type !== '' ? formInline.type : undefined,
            approvalStatus: formInline.state !== '' ? formInline.state : undefined
        })
        console.log(data);
        // 将 API 返回字段（batchId/batchName/planType/projectCount/approvalStatus）映射为内部字段
        tableData.value = (data?.list || []).map(row => ({
            date: row.batchId ?? row.date,
            name: row.batchName ?? row.name,
            type: row.planType ?? row.type,
            year: row.year,
            num: row.projectCount ?? row.num ?? 0,
            during: row.startDate ? `${row.startDate} ~ ${row.endDate}` : (row.during || ''),
            state: row.approvalStatus ?? row.state,
            progress: row.progress ?? 0,
            unitId: row.unitId || '',
            unitName: row.unitName || '',
            leaderId: row.leaderId || ''
        }))
    } catch (err) { console.error('获取计划列表失败:', err) } finally { loading.value = false }
};

// ============ 统计卡片 ============
const summaryCards = reactive({ totalPlanCount: 0, approvedCount: 0, approvingCount: 0, alertCount: 0 });
const fetchSummary = async () => {
    try { const data = await planApi.getPlanSummary(); if (data) Object.assign(summaryCards, data) } catch { }
};

const deletePlanBatch = async (batchId) => {
    try { await planApi.deletePlanBatch(batchId); ElMessage.success('已删除'); fetchTableData(); fetchSummary() } catch { }
};

// ============ 导出 ============
const planExportColumns = [
    { prop: 'date', label: '计划编号' }, { prop: 'name', label: '计划名称' }, { prop: 'type', label: '计划类型' },
    { prop: 'year', label: '年度' }, { prop: 'unitName', label: '被审计单位' },
    { prop: 'num', label: '项目数' }, { prop: 'during', label: '实施周期' },
    { prop: 'auditLeader', label: '审计组长' },
    { prop: 'state', label: '审批状态' }, { prop: 'progress', label: '进度' },
];
const handleExportPlanList = () => {
    const exportData = tableData.value.map(row => ({
        date: row.date, name: row.name, type: allType[row.type] || '未知', year: row.year,
        unitName: row.unitName || '', num: row.num, during: row.during,
        auditLeader: row.auditLeader || '', state: allState[row.state] || '未知', progress: row.progress + '%'
    }))
    exportExcel(exportData, `年度审计计划清单_${now}`, planExportColumns)
};

// ============ 导入 ============
const importVisible = ref(false); const currentBatchForImport = ref(null);
const importPreviewData = ref([]); const importPreviewHeader = ref([]);
const importFileName = ref(''); const importMode = ref('plan'); const rawImportFile = ref(null);

const planImportTemplateFields = [
    { label: '计划编号', example: 'JH2026001', required: true }, { label: '计划名称', example: '2026年度经济责任审计计划', required: true },
    { label: '计划类型', example: '0-经济责任/1-财务收支/2-专项/3-工程', required: true }, { label: '年度', example: '2026', required: true },
    { label: '被审计单位编码', example: 'UNIT001', required: false }, { label: '项目数', example: '8', required: false },
    { label: '实施周期', example: '2026-01-01 ~ 2026-06-30', required: false }, { label: '审计组长', example: '张三', required: false },
    { label: '审批状态', example: '3-草稿/1-审批中/0-已审批/2-已归档', required: false }, { label: '进度', example: '0', required: false },
];
const projectImportTemplateFields = [
    { label: '项目名称', example: 'XX学院经济责任审计', required: true }, { label: '审计对象编码', example: 'UNIT001', required: true },
    { label: '覆盖期间起', example: '2024-01-01', required: true }, { label: '覆盖期间止', example: '2025-12-31', required: true },
    { label: '实施开始日期', example: '2026-03-01', required: true }, { label: '实施结束日期', example: '2026-06-30', required: true },
    { label: '审计组长工号', example: 'U001', required: false }, { label: '是否委托中介', example: '0-否/1-是', required: false },
    { label: '备注', example: '', required: false },
];
const openImportDialog = (row) => { currentBatchForImport.value = row; importMode.value = row ? 'project' : 'plan'; importFileName.value = ''; importPreviewData.value = []; importPreviewHeader.value = []; importVisible.value = true; };
const downloadImportFileTemplate = () => { downloadImportTemplate(importMode.value === 'plan' ? '计划批次导入模板' : '审计项目批量导入模板', importMode.value === 'plan' ? planImportTemplateFields : projectImportTemplateFields); };
const handleImportFile = async (file) => { importFileName.value = file.name; rawImportFile.value = file.raw; try { const r = await readExcel(file.raw); importPreviewHeader.value = r.header; importPreviewData.value = r.data; ElMessage.success(`成功读取 ${r.data.length} 条数据`) } catch (err) { ElMessage.error(err.message || '文件读取失败'); importPreviewData.value = []; importPreviewHeader.value = []; } };
const planTypeMap = { '经济责任审计': 0, '财务收支审计': 1, '专项审计': 2, '工程审计': 3 };
const stateMap = { '草稿': 3, '审批中': 1, '已审批': 0, '已归档': 2 };

const confirmImport = async () => {
    if (importPreviewData.value.length === 0) { ElMessage.warning('没有可导入的数据'); return; }
    try {
        if (importMode.value === 'plan') {
            const batches = importPreviewData.value.map(row => {
                const typeRaw = row['计划类型'] ?? row['类型'] ?? '0'
                return { batchName: row['计划名称'] || row['名称'] || '未命名计划', planType: planTypeMap[typeRaw] ?? parseInt(typeRaw) ?? 0, year: parseInt(row['年度']) || now, startDate: (row['实施周期'] || '').split('~')[0]?.trim() || `${now}-01-01`, endDate: (row['实施周期'] || '').split('~')[1]?.trim() || `${now}-12-31`, unitId: row['被审计单位编码'] || undefined, auditLeader: row['审计组长'] || undefined }
            })
            await planApi.batchCreatePlanBatches({ batches })
            ElMessage.success(`成功导入 ${batches.length} 个计划批次`)
        } else {
            if (rawImportFile.value) { const fd = new FormData(); fd.append('file', rawImportFile.value); await planApi.importProjects(currentBatchForImport.value.date, fd) }
            currentBatchForImport.value.num = (currentBatchForImport.value.num || 0) + importPreviewData.value.length
            ElMessage.success(`成功导入 ${importPreviewData.value.length} 条项目`)
        }
        importVisible.value = false; fetchTableData()
    } catch { }
};
const handleRemoveImportFile = () => { importFileName.value = ''; importPreviewData.value = []; importPreviewHeader.value = []; rawImportFile.value = null };

// ============ 推荐 ============
const recommendVisible = ref(false); const recommendList = ref([]); const selectedRecommendRows = ref([]);
const openRecommendDialog = async () => { recommendVisible.value = true; selectedRecommendRows.value = []; try { const data = await planApi.getRecommendObjects(); recommendList.value = data?.list || [] } catch { } };
const handleRecommendSelectionChange = (rows) => { selectedRecommendRows.value = rows; };
const importRecommend = async () => { if (selectedRecommendRows.value.length === 0) { ElMessage.warning('请至少勾选一个推荐对象'); return; } try { await planApi.importRecommendToBatch(currentBatchForImport?.value?.date || tableData.value[0]?.date, { objectIds: selectedRecommendRows.value.map(r => r.objectId), defaultAuditLeaderId: newPlanInfo.auditLeaderId || undefined }); ElMessage.success(`已将 ${selectedRecommendRows.value.length} 个推荐对象加入年度计划`); recommendVisible.value = false; fetchTableData() } catch { } };

// ============ 附件 ============
const attachVisible = ref(false); const currentBatchForAttach = ref(null); const uploadFiles = ref([]); const attachType = ref('');
const attachTypes = [{ value: 'DELEGATION', label: '审计委托书' }, { value: 'RESOLUTION', label: '审批决议' }, { value: 'TASK', label: '上级任务文件' }, { value: 'RESEARCH', label: '调研材料' }, { value: 'OTHER', label: '其他' }];
const attachmentList = ref([]);
const openAttachDialog = async (row) => { currentBatchForAttach.value = row; attachVisible.value = true; try { const data = await planApi.getAttachments(row.date); attachmentList.value = data?.list || data || [] } catch { } };
const submitUpload = async () => { if (uploadFiles.value.length === 0) { ElMessage.warning('请选择文件'); return; } try { let count = 0; for (const f of uploadFiles.value) { const fd = new FormData(); fd.append('file', f.raw); if (attachType.value) fd.append('attachType', attachType.value); await planApi.uploadAttachment(currentBatchForAttach.value.date, fd); count++ } ElMessage.success(`成功上传 ${count} 个附件`); uploadFiles.value = []; openAttachDialog(currentBatchForAttach.value) } catch { } };
const deleteAttach = async (attach) => { try { await planApi.deleteAttachment(currentBatchForAttach.value.date, attach.attachId); ElMessage.success('已删除'); openAttachDialog(currentBatchForAttach.value) } catch { } };
const previewAttachment = (attachId) => {
    // 附件预览 — PDF/图片直接在新窗口打开，Office 文件触发下载
    window.open(planApi.getPreviewUrl(attachId), '_blank')
};

// ============ 方案模板 ============
const templateVisible = ref(false); const currentBatchForTemplate = ref(null); const templateList = ref([]); const selectedTemplateRows = ref([]);
const openTemplateDialog = async (row) => { currentBatchForTemplate.value = row; selectedTemplateRows.value = []; try { const data = await planApi.getTemplates(); templateList.value = data?.list || [] } catch { } finally { templateVisible.value = true } };
const handleTemplateSelectionChange = (rows) => { selectedTemplateRows.value = rows; };
const bindTemplates = async () => { if (selectedTemplateRows.value.length === 0) { ElMessage.warning('请至少勾选一个方案模板'); return; } try { await planApi.bindTemplates(currentBatchForTemplate.value.date, { templateIds: selectedTemplateRows.value.map(t => t.templateId) }); ElMessage.success(`已绑定 ${selectedTemplateRows.value.length} 个方案模板`); templateVisible.value = false } catch { } };

// ============ 穿透 ============
const penetrateVisible = ref(false); const penetrateData = ref(null);
const openPenetrate = async (row) => { penetrateData.value = { batchId: row.date, batchName: row.name, planType: allType[row.type] || '未知', progress: { progressPercent: row.progress, currentPhase: row.progress < 30 ? '准备阶段' : row.progress < 70 ? '实施阶段' : '报告阶段' }, workingPapers: { totalCount: 0, completedCount: 0 }, rectifyLedger: { totalIssues: 0, rectifiedCount: 0, rectifyingCount: 0, pendingCount: 0 }, reports: { draftSubmitted: false, finalSubmitted: false } }; penetrateVisible.value = true; try { const data = await planApi.penetrateProject(row.date); if (data) Object.assign(penetrateData.value, data) } catch { } };

// ============ 变更 ============
const changeVisible = ref(false); const currentBatchForChange = ref(null);
const changeForm = reactive({ changeType: 0, reason: '', batchName: '', planType: 0, startDate: '', endDate: '', auditLeader: '', unitId: '', leaderId: '', remark: '' });
const changeTypes = [{ value: 0, label: '修改计划信息' }, { value: 1, label: '调减项目' }, { value: 2, label: '修改周期' }, { value: 3, label: '其他变更' }];
const openChangeDialog = (row) => { loadAuditors(); loadUnitsAndLeaders(); currentBatchForChange.value = row; changeForm.changeType = 0; changeForm.reason = ''; changeForm.batchName = row.name; changeForm.planType = row.type; const parts = (row.during || '').split('~').map(s => s.trim()); changeForm.startDate = parts[0] || ''; changeForm.endDate = parts[1] || ''; changeForm.auditLeader = row.auditLeader || ''; changeForm.unitId = row.unitId || ''; changeForm.leaderId = row.leaderId || ''; changeForm.remark = row.remark || ''; changeVisible.value = true; };
const submitChange = async () => { if (!changeForm.reason) { ElMessage.warning('请填写变更原因'); return; } try { const payload = { changeType: changeForm.changeType, reason: changeForm.reason, batchName: changeForm.batchName, planType: changeForm.planType, startDate: changeForm.startDate ? new Date(changeForm.startDate).toISOString().slice(0,10) : '', endDate: changeForm.endDate ? new Date(changeForm.endDate).toISOString().slice(0,10) : '', auditLeader: changeForm.auditLeader, unitId: changeForm.unitId, leaderId: changeForm.leaderId, remark: changeForm.remark }; await planApi.createPlanChange(currentBatchForChange.value.date, payload); ElMessage.success('变更已提交，请在审批跟踪中确认'); changeVisible.value = false; } catch { } };

onMounted(() => { fetchTableData(); fetchSummary() })
</script>

<template>
    <div class="form-page">
        <!-- 统计卡片 -->
        <div class="stats-row">
            <div class="stat-card">
                <div class="stat-icon" style="background:#ecf5ff;color:#409EFF"><el-icon :size="20">
                        <DocumentCopy />
                    </el-icon></div>
                <div class="stat-info"><span class="stat-num">{{ summaryCards.totalPlanCount }}</span><span
                        class="stat-label">计划总数</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon" style="background:#f0fff4;color:#67c23a"><el-icon :size="20">
                        <Check />
                    </el-icon></div>
                <div class="stat-info"><span class="stat-num">{{ summaryCards.approvedCount }}</span><span
                        class="stat-label">已审批通过</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon" style="background:#fdf6ec;color:#e6a23c"><el-icon :size="20">
                        <Timer />
                    </el-icon></div>
                <div class="stat-info"><span class="stat-num">{{ summaryCards.approvingCount }}</span><span
                        class="stat-label">审批中</span></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon" style="background:#fef0f0;color:#f56c6c"><el-icon :size="20">
                        <Warning />
                    </el-icon></div>
                <div class="stat-info"><span class="stat-num">{{ summaryCards.alertCount }}</span><span
                        class="stat-label">超出预警</span></div>
            </div>
        </div>

        <!-- 计划清单表格 -->
        <el-card class="table-card" shadow="never">
            <div class="card-header">
                <span class="card-title">📝 年度审计计划清单</span>
                <div class="card-actions">
                    <el-button @click="openImportDialog(null)"><el-icon>
                            <Upload />
                        </el-icon>批量导入Excel</el-button>
                    <el-button type="primary" @click="addNewPlan"><el-icon>
                            <Plus />
                        </el-icon>新建计划批次</el-button>
                </div>
            </div>
            <div class="search-bar">
                <el-form :inline="true" :model="formInline">
                    <el-form-item><el-input v-model="formInline.planName" placeholder="🔍 搜索计划名称/编号..." clearable
                            style="width:220px" /></el-form-item>
                    <el-form-item><el-select v-model="formInline.year" placeholder="全部年度" clearable
                            style="width:130px"><el-option v-for="v in allYear" :key="v" :label="v"
                                :value="v" /></el-select></el-form-item>
                    <el-form-item><el-select v-model="formInline.type" placeholder="全部类型" clearable
                            style="width:170px"><el-option v-for="(v, i) in allType" :key="i" :label="v"
                                :value="i" /></el-select></el-form-item>
                    <el-form-item><el-select v-model="formInline.state" placeholder="全部状态" clearable
                            style="width:130px"><el-option v-for="(v, i) in allState" :key="i" :label="v"
                                :value="i" /></el-select></el-form-item>
                    <el-form-item><el-button type="primary" @click="fetchTableData">查询</el-button><el-button
                            @click="handleExportPlanList">导出Excel</el-button></el-form-item>
                </el-form>
            </div>
            <el-table :data="tableData" v-loading="loading" style="width:100%" height="calc(100vh - 330px)"
                row-key="date" stripe>
                <el-table-column prop="date" label="计划编号" width="170" />
                <el-table-column prop="name" label="计划名称" min-width="240" show-overflow-tooltip />
                <el-table-column prop="type" label="计划类型" width="130">
                    <template #default="{ row }"><el-tag size="small"
                            :type="row.type === 0 ? 'primary' : row.type === 1 ? 'success' : row.type === 2 ? 'warning' : row.type === 3 ? 'danger' : 'info'">{{
                            allType[row.type] || '未知' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="unitName" label="被审计单位" width="150" show-overflow-tooltip />
                <el-table-column prop="year" label="年度" width="80" />
                <el-table-column prop="num" label="项目数" width="80" align="center" />
                <el-table-column prop="during" label="实施周期" width="200"><template #default="{ row }">{{
                        formatDateYYYYMMDD(row.during) }}</template></el-table-column>
                <el-table-column prop="state" label="审批状态" width="90">
                    <template #default="{ row }"><el-tag size="small"
                            :type="row.state === 0 ? 'success' : row.state === 1 ? 'warning' : row.state === 2 ? 'info' : ''">{{
                            allState[row.state] || '未知' }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="progress" label="进度" width="140">
                    <template #default="{ row }"><el-progress :text-inside="true" :stroke-width="20"
                            :percentage="row.progress" :status="getProgressStatus(row.progress)" /></template>
                </el-table-column>
                <el-table-column label="操作" width="480" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" link type="primary" @click="openPenetrate(row)">穿透</el-button>
                        <el-button size="small" link @click="editPlan(row)">编辑</el-button>
                        <el-button size="small" link @click="openAttachDialog(row)">附件</el-button>
                        <el-button size="small" link @click="openTemplateDialog(row)">方案</el-button>
                        <el-button size="small" link @click="openImportDialog(row)">导入</el-button>
                        <el-button size="small" link @click="openRecommendDialog">推荐</el-button>
                        <el-button size="small" link type="warning" @click="openChangeDialog(row)">变更</el-button>
                        <el-popconfirm title="确定删除？" @confirm="deletePlanBatch(row.date)"><template
                                #reference><el-button size="small" link
                                    type="danger">删除</el-button></template></el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新建/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" draggable :close-on-click-modal="false">
            <el-form ref="formRef" :model="newPlanInfo" :rules="formRules" label-width="100px">
                <el-form-item label="计划编号" prop="id"><el-input v-model="newPlanInfo.id" placeholder="请输入计划编号"
                        :disabled="isEdit" /></el-form-item>
                <el-form-item label="计划名称" prop="name"><el-input v-model="newPlanInfo.name"
                        placeholder="请输入计划名称" /></el-form-item>
                <el-form-item label="计划类型" prop="type"><el-select v-model="newPlanInfo.type" placeholder="请选择类型"
                        style="width:100%"><el-option v-for="(v, i) in allType" :key="i" :label="v"
                            :value="i" /></el-select></el-form-item>
                <el-form-item label="计划周期"><el-date-picker v-model="newPlanInfo.startDate" type="date"
                        placeholder="开始日期" style="width:48%" /> <span style="margin:0 8px;color:#909399">—</span>
                    <el-date-picker v-model="newPlanInfo.endDate" type="date" placeholder="结束日期" style="width:48%"
                        :disabled-date="(t) => newPlanInfo.startDate && t.getTime() < new Date(newPlanInfo.startDate).getTime()" /></el-form-item>
                <el-form-item label="审计组长"><el-select v-model="newPlanInfo.auditLeader" placeholder="请选择审计组长" clearable
                        filterable style="width:100%"><el-option v-for="a in auditorList" :key="a.userId"
                            :label="a.userName" :value="a.userId"><span>{{ a.userName }}</span><span
                                style="float:right;color:#909399;font-size:12px">可接 {{ a.availableCapacity }}
                                项</span></el-option></el-select></el-form-item>
                <el-form-item label="被审计单位"><el-select v-model="newPlanInfo.unitId" placeholder="请选择被审计单位" clearable filterable style="width:100%"><el-option v-for="u in unitList" :key="u.unitId" :label="u.unitName" :value="u.unitId" /></el-select></el-form-item>
                <el-form-item label="经责干部"><el-select v-model="newPlanInfo.leaderId" placeholder="请选择经责干部（可选）" clearable filterable style="width:100%"><el-option v-for="l in leaderList" :key="l.leaderId" :label="l.leaderName + ' - ' + l.currentPosition" :value="l.leaderId" /></el-select></el-form-item>
                <el-form-item label="备注"><el-input v-model="newPlanInfo.remark" type="textarea" :rows="3"
                        placeholder="备注信息" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary"
                    @click="submitPlan">确认提交</el-button></template>
        </el-dialog>

        <!-- 智能推荐弹窗 -->
        <el-dialog v-model="recommendVisible" title="🤖 智能推荐待审对象" width="70%" draggable>
            <el-alert type="info" :closable="false" style="margin-bottom:16px"
                description="系统综合多维度规则自动筛选近3年需启动审计的单位/领导干部，勾选后一键导入年度计划。" />
            <el-table :data="recommendList" row-key="objectId" @selection-change="handleRecommendSelectionChange"
                max-height="400">
                <el-table-column type="selection" width="45" />
                <el-table-column prop="objectName" label="对象名称" width="180" />
                <el-table-column prop="categoryName" label="分类" width="130" />
                <el-table-column prop="recommendReason" label="推荐理由" min-width="220" show-overflow-tooltip />
                <el-table-column prop="lastAuditDate" label="上轮审计" width="120"><template #default="{ row }">{{
                    row.lastAuditDate || '无' }}</template></el-table-column>
                <el-table-column prop="riskLevel" label="风险" width="80"><template #default="{ row }"><el-tag
                            :type="row.riskLevel === 'HIGH' ? 'danger' : row.riskLevel === 'MEDIUM' ? 'warning' : 'info'"
                            size="small">{{ row.riskLevel === 'HIGH' ? '高' : row.riskLevel === 'MEDIUM' ? '中' : '低'
                            }}</el-tag></template></el-table-column>
                <el-table-column prop="recommendScore" label="推荐分" width="80" sortable />
            </el-table>
            <template #footer><el-button @click="recommendVisible = false">取消</el-button><el-button type="primary"
                    @click="importRecommend">一键加入年度计划</el-button></template>
        </el-dialog>

        <!-- 附件弹窗 -->
        <el-dialog v-model="attachVisible" title="📎 附件管理" width="60%" draggable>
            <div v-if="currentBatchForAttach" class="dialog-subtitle">计划批次：{{ currentBatchForAttach.name }}（{{
                currentBatchForAttach.date }}）</div>
            <el-upload drag multiple :auto-upload="false" :on-change="(f) => uploadFiles.push(f)"
                accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png,.bmp" class="upload-block">
                <el-icon :size="36" color="#c0c4cc">
                    <Upload />
                </el-icon>
                <div class="el-upload__text">拖拽或<em>点击上传</em></div>
                <template #tip>
                    <div class="el-upload__tip">PDF / Word / Excel / 图片，单文件 ≤ 50MB</div>
                </template>
            </el-upload>
            <div style="margin-top:12px;display:flex;gap:12px;align-items:center"><span>附件类型：</span><el-select
                    v-model="attachType" placeholder="选择类型" clearable style="width:180px"><el-option
                        v-for="at in attachTypes" :key="at.value" :label="at.label"
                        :value="at.value" /></el-select><el-button type="primary" @click="submitUpload">确认上传</el-button>
            </div>
            <el-divider />
            <el-table :data="attachmentList" max-height="200" size="small">
                <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
                <el-table-column prop="attachTypeName" label="类型" width="120" />
                <el-table-column label="大小" width="90"><template #default="{ row }">{{ row.fileSize ? (row.fileSize /
                    1024).toFixed(1) + ' KB' : '-' }}</template></el-table-column>
                <el-table-column prop="uploadTime" label="上传时间" width="130" />
                <el-table-column label="操作" width="140"><template #default="{ row }"><el-button size="small" link
                            @click="previewAttachment(row.attachId)">预览</el-button><el-popconfirm
                            title="确定删除？" @confirm="deleteAttach(row)"><template #reference><el-button size="small"
                                    type="danger"
                                    link>删除</el-button></template></el-popconfirm></template></el-table-column>
            </el-table>
        </el-dialog>

        <!-- 方案模板弹窗 -->
        <el-dialog v-model="templateVisible" title="📋 审计方案模板绑定" width="60%" draggable>
            <div v-if="currentBatchForTemplate" class="dialog-subtitle">计划批次：{{ currentBatchForTemplate.name }}</div>
            <el-table :data="templateList" row-key="template_id" @selection-change="handleTemplateSelectionChange">
                <el-table-column type="selection" width="45" />
                <el-table-column prop="templateName" label="模板名称" min-width="200" />
                <el-table-column prop="planType" label="适用类型" width="140"><template #default="{ row }">{{
                    allType[row.planType] || '通用' }}</template></el-table-column>
                <el-table-column prop="version" label="版本" width="80" />
            </el-table>
            <template #footer><el-button @click="templateVisible = false">取消</el-button><el-button type="primary"
                    @click="bindTemplates">批量关联</el-button></template>
        </el-dialog>

        <!-- 穿透弹窗 -->
        <el-dialog v-model="penetrateVisible" title="🔍 计划穿透查询" width="65%" draggable>
            <template v-if="penetrateData">
                <el-descriptions :column="2" border size="small"><el-descriptions-item label="计划编号">{{
                        penetrateData.batchId }}</el-descriptions-item><el-descriptions-item label="计划名称">{{
                        penetrateData.batchName }}</el-descriptions-item><el-descriptions-item label="计划类型">{{
                        penetrateData.planType }}</el-descriptions-item><el-descriptions-item label="当前阶段">{{
                            penetrateData.progress.currentPhase }}</el-descriptions-item><el-descriptions-item
                        label="实施进度"><el-progress :percentage="penetrateData.progress.progressPercent"
                            :stroke-width="16" /></el-descriptions-item><el-descriptions-item label="配套文件"><el-tag
                            v-for="f in (penetrateData.guidingFiles || [])" :key="f.fileName" size="small"
                            style="margin-right:4px">{{ f.fileName }}</el-tag></el-descriptions-item></el-descriptions>
                <el-row :gutter="16" style="margin-top:16px">
                    <el-col :span="6"><el-statistic title="底稿总数"
                            :value="penetrateData.workingPapers?.totalCount || 0" /></el-col>
                    <el-col :span="6"><el-statistic title="已完成底稿"
                            :value="penetrateData.workingPapers?.completedCount || 0" /></el-col>
                    <el-col :span="6"><el-statistic title="整改问题数"
                            :value="penetrateData.rectifyLedger?.totalIssues || 0" /></el-col>
                    <el-col :span="6"><el-statistic title="整改完成"
                            :value="penetrateData.rectifyLedger?.rectifiedCount || 0" /></el-col>
                </el-row>
                <el-divider v-if="penetrateData.rectifyLedger?.list?.length > 0" />
                <el-table v-if="penetrateData.rectifyLedger?.list?.length > 0" :data="penetrateData.rectifyLedger.list" size="small" style="margin-top:8px">
                    <el-table-column prop="issue_description" label="问题描述" min-width="200" show-overflow-tooltip />
                    <el-table-column prop="issue_category" label="类别" width="100" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag size="small" :type="row.rectify_status === 2 ? 'success' : row.rectify_status === 1 ? 'warning' : 'danger'">{{ row.rectify_status === 2 ? '已整改' : row.rectify_status === 1 ? '整改中' : '未整改' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="responsible_person" label="责任人" width="80" />
                    <el-table-column prop="deadline" label="截止日期" width="110" />
                    <el-table-column prop="rectify_progress" label="进展" min-width="150" show-overflow-tooltip />
                </el-table>
            </template>
        </el-dialog>

        <!-- 变更弹窗 -->
        <el-dialog v-model="changeVisible" title="📝 计划变更申请" width="60%" draggable>
            <div v-if="currentBatchForChange" class="dialog-subtitle">计划批次：{{ currentBatchForChange.name }}（{{ currentBatchForChange.date }}）</div>
            <el-form label-width="100px">
                <el-form-item label="变更类型" required><el-select v-model="changeForm.changeType" style="width:100%"><el-option v-for="ct in changeTypes" :key="ct.value" :label="ct.label" :value="ct.value" /></el-select></el-form-item>
                <el-form-item label="变更原因" required><el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="请描述变更原因..." /></el-form-item>
                <el-divider>修改以下字段（留空则不变）</el-divider>
                <el-form-item label="计划名称"><el-input v-model="changeForm.batchName" /></el-form-item>
                <el-form-item label="计划类型"><el-select v-model="changeForm.planType" style="width:100%"><el-option v-for="(v,i) in allType" :key="i" :label="v" :value="i" /></el-select></el-form-item>
                <el-form-item label="计划周期"><el-date-picker v-model="changeForm.startDate" type="date" placeholder="开始" style="width:45%" /> <span style="margin:0 8px">—</span> <el-date-picker v-model="changeForm.endDate" type="date" placeholder="结束" style="width:45%" /></el-form-item>
                <el-form-item label="审计组长"><el-select v-model="changeForm.auditLeader" clearable filterable style="width:100%"><el-option v-for="a in auditorList" :key="a.userId" :label="a.userName" :value="a.userName" /></el-select></el-form-item>
                <el-form-item label="被审计单位"><el-select v-model="changeForm.unitId" clearable filterable style="width:100%"><el-option v-for="u in unitList" :key="u.unitId" :label="u.unitName" :value="u.unitId" /></el-select></el-form-item>
                <el-form-item label="经责干部"><el-select v-model="changeForm.leaderId" clearable filterable style="width:100%"><el-option v-for="l in leaderList" :key="l.leaderId" :label="l.leaderName + ' - ' + l.currentPosition" :value="l.leaderId" /></el-select></el-form-item>
                <el-form-item label="备注"><el-input v-model="changeForm.remark" type="textarea" :rows="2" /></el-form-item>
            </el-form>
            <el-alert type="warning" :closable="false" show-icon description="变更提交后需在审批跟踪中确认才会生效。" style="margin-top:8px" />
            <template #footer><el-button @click="changeVisible = false">取消</el-button><el-button type="primary" @click="submitChange">提交变更并生效</el-button></template>
        </el-dialog>

        <!-- 导入弹窗 -->
        <el-dialog v-model="importVisible" :title="importMode === 'plan' ? '📥 批量导入计划批次' : '📥 批量导入项目'" width="55%"
            draggable>
            <div v-if="currentBatchForImport" class="dialog-subtitle">目标批次：<el-tag size="small" type="primary">{{
                    currentBatchForImport.name }}</el-tag>（{{ currentBatchForImport.date }}）</div>
            <el-alert type="info" :closable="false" style="margin-bottom:16px"
                :description="importMode === 'plan' ? '下载模板 → 填写 → 上传 → 预览 → 确认导入' : '下载模板 → 填写 → 上传 → 预览 → 确认导入'" />
            <el-button @click="downloadImportFileTemplate" style="margin-bottom:12px"><el-icon>
                    <Download />
                </el-icon>下载导入模板</el-button>
            <el-upload drag :auto-upload="false" :on-change="handleImportFile" :on-remove="handleRemoveImportFile"
                accept=".xlsx,.xls" :limit="1" class="upload-block">
                <el-icon :size="36" color="#c0c4cc">
                    <Upload />
                </el-icon>
                <div class="el-upload__text">拖拽或<em>点击上传</em></div>
                <template #tip>
                    <div class="el-upload__tip">仅支持 .xlsx / .xls，请按模板填写</div>
                </template>
            </el-upload>
            <div v-if="importPreviewData.length > 0" style="margin-top:16px">
                <el-alert type="success" :closable="false" show-icon
                    :title="`已解析 ${importPreviewData.length} 条数据（${importFileName}）`" style="margin-bottom:8px" />
                <el-table :data="importPreviewData.slice(0, 5)" size="small" border max-height="200"><el-table-column
                        v-for="h in importPreviewHeader" :key="h" :prop="h" :label="h" min-width="100"
                        show-overflow-tooltip /></el-table>
                <div v-if="importPreviewData.length > 5"
                    style="text-align:center;color:#909399;margin-top:4px;font-size:12px">
                    仅显示前5条预览，共 {{ importPreviewData.length }} 条</div>
            </div>
            <template #footer><el-button @click="importVisible = false">取消</el-button><el-button type="primary"
                    @click="confirmImport" :disabled="importPreviewData.length === 0">确认导入（{{ importPreviewData.length
                    }} 条）</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
.form-page {
    padding: 16px;
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.stats-row {
    display: flex;
    gap: 12px;
    flex-shrink: 0;
}

.stat-card {
    flex: 1;
    background: #fff;
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    transition: box-shadow 0.2s;
    cursor: default;
}

.stat-card:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-info {
    display: flex;
    flex-direction: column;
}

.stat-num {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
}

.stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 2px;
}

.table-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    border-radius: 10px;
    overflow: hidden;
}

.table-card :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 0;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 18px;
    background: #fafbfc;
    border-bottom: 1px solid #ebeef5;
}

.card-title {
    font-size: 15px;
    font-weight: 700;
    color: #303133;
}

.card-actions {
    display: flex;
    gap: 8px;
}

.search-bar {
    padding: 10px 18px;
    border-bottom: 1px solid #ebeef5;
}

.search-bar :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 12px;
}

.search-bar :deep(.el-form-item:last-child) {
    margin-right: 0;
}

.dialog-subtitle {
    margin-bottom: 12px;
    color: #909399;
    font-size: 13px;
}

.upload-block :deep(.el-upload) {
    width: 100%;
}

.upload-block :deep(.el-upload-dragger) {
    width: 100%;
    border-radius: 10px;
    padding: 30px;
}

:deep(.el-table) {
    font-size: 13px;
}

:deep(.el-table th) {
    background: #fafbfc;
    color: #606266;
    font-weight: 600;
}

:deep(.el-table .el-table__row:hover) {
    background: #f5f7fa;
}
</style>
