<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { readExcel, exportExcel, downloadImportTemplate } from '@/utils/excel'
import { objectApi } from '@/api'

const searchForm = reactive({ keyword: '', isActive: '' })
const tableData = ref([]); const loading = ref(false)

const fetchLeaderTable = async () => {
    loading.value = true
    try { const data = await objectApi.getLeaders({ keyword: searchForm.keyword || undefined, isActive: searchForm.isActive !== '' ? searchForm.isActive : undefined }); tableData.value = data?.list || [] } catch {} finally { loading.value = false }
}

// 新建/编辑
const dialogVisible = ref(false); const dialogTitle = ref('新建领导干部'); const isEdit = ref(false)
const leaderForm = reactive({ leaderName: '', staffId: '', unitName: '', position: '', isActive: 1, tenureStartDate: '', fundScope: null, _editId: '' })
const formRef = ref(null)
const rules = { leaderName: [{ required: true, message: '请输入姓名', trigger: 'blur' }], staffId: [{ required: true, message: '请输入工号', trigger: 'blur' }], position: [{ required: true, message: '请输入职务', trigger: 'blur' }] }
const openAddDialog = () => { dialogTitle.value = '新建领导干部'; isEdit.value = false; Object.keys(leaderForm).forEach(k => leaderForm[k] = k === 'isActive' ? 1 : (typeof leaderForm[k] === 'number' ? null : '')); dialogVisible.value = true }
const openEditDialog = (row) => { dialogTitle.value = '编辑领导干部'; isEdit.value = true; Object.assign(leaderForm, { leaderName: row.leaderName, staffId: row.staffId, unitName: row.currentUnitName, position: row.currentPosition, isActive: row.isActive, tenureStartDate: row.tenureStartDate, fundScope: row.fundScope }); leaderForm._editId = row.leaderId; dialogVisible.value = true }
const submitLeader = async () => { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; try { if (isEdit.value) { await objectApi.updateLeader(leaderForm._editId, leaderForm) } else { await objectApi.createLeader(leaderForm) }; ElMessage.success(isEdit.value ? '修改成功' : '新建成功'); dialogVisible.value = false; fetchLeaderTable() } catch {} }

// 详情
const detailDrawerVisible = ref(false); const detailLeader = ref(null)
const openDetail = async (row) => { detailLeader.value = { ...row, careerHistory: [], auditProjects: [] }; detailDrawerVisible.value = true; try { const [ch, ap] = await Promise.all([objectApi.getLeaderCareerHistory(row.leaderId), objectApi.getLeaderAuditProjects(row.leaderId)]); detailLeader.value.careerHistory = ch?.list || []; detailLeader.value.auditProjects = ap?.list || [] } catch {} }

// 任职履历
const careerVisible = ref(false); const careerData = ref([])
const currentCareerLeaderId = ref('')
const careerFormVisible = ref(false)
const careerForm = reactive({ recordId: '', unitName: '', position: '', startDate: '', endDate: '', dutyDescription: '', fundScope: null })
const isEditCareer = ref(false)

const fetchCareerHistory = async () => {
    try { const data = await objectApi.getLeaderCareerHistory(currentCareerLeaderId.value); careerData.value = data?.list || [] } catch {}
}

const openCareerHistory = async (row) => { currentCareerLeaderId.value = row.leaderId; careerVisible.value = true; fetchCareerHistory() }

const openAddCareer = () => {
    isEditCareer.value = false
    careerForm.recordId = ''; careerForm.unitName = ''; careerForm.position = ''; careerForm.startDate = ''; careerForm.endDate = ''; careerForm.dutyDescription = ''; careerForm.fundScope = null
    careerFormVisible.value = true
}

const openEditCareer = (item) => {
    isEditCareer.value = true
    careerForm.recordId = item.record_id; careerForm.unitName = item.unit_name || ''; careerForm.position = item.position || ''; careerForm.startDate = item.start_date || ''; careerForm.endDate = item.end_date || ''; careerForm.dutyDescription = item.duty_description || ''; careerForm.fundScope = item.fund_scope || null
    careerFormVisible.value = true
}

const submitCareer = async () => {
    try {
        if (isEditCareer.value) {
            await objectApi.updateCareerRecord(currentCareerLeaderId.value, careerForm.recordId, careerForm)
        } else {
            await objectApi.addCareerRecord(currentCareerLeaderId.value, careerForm)
        }
        ElMessage.success(isEditCareer.value ? '履历已更新' : '履历已添加')
        careerFormVisible.value = false; fetchCareerHistory()
    } catch {}
}

const deleteCareer = async (item) => {
    try { await objectApi.deleteCareerRecord(currentCareerLeaderId.value, item.record_id); ElMessage.success('已删除'); fetchCareerHistory() } catch {}
}

// ============ 履历导出 ============
const careerExportColumns = [
    { prop: 'unit_name', label: '单位名称' },{ prop: 'position', label: '职务' },
    { prop: 'start_date', label: '开始日期' },{ prop: 'end_date', label: '结束日期' },
    { prop: 'duty_description', label: '职责描述' },{ prop: 'fund_scope', label: '分管资金(万)' },
    { prop: 'source', label: '来源' }
]
const exportCareerHistory = () => {
    if (!careerData.value.length) { ElMessage.warning('无履历数据可导出'); return }
    exportExcel(careerData.value.map(r => ({
        ...r, end_date: r.end_date || '至今'
    })), '任职履历', careerExportColumns)
}

// ============ 履历导入 ============
const careerImportVisible = ref(false)
const careerImportPreviewData = ref([])
const careerImportPreviewHeader = ref([])
const careerImportRawFile = ref(null)

const careerImportTemplateFields = [
    { label: '单位名称', example: 'XX学院', required: true },
    { label: '职务', example: '副院长', required: true },
    { label: '开始日期', example: '2020-01-01', required: true },
    { label: '结束日期', example: '2023-12-31', required: false },
    { label: '职责描述', example: '分管教学和科研工作', required: false },
    { label: '分管资金(万)', example: '3000', required: false }
]

const downloadCareerImportTemplate = () => {
    downloadImportTemplate('任职履历导入模板', careerImportTemplateFields)
}

const handleCareerImportFile = async (file) => {
    careerImportRawFile.value = file.raw
    try {
        const r = await readExcel(file.raw)
        careerImportPreviewHeader.value = r.header
        careerImportPreviewData.value = r.data
        ElMessage.success(`成功读取 ${r.data.length} 条`)
    } catch (e) { ElMessage.error(e.message || '读取失败'); careerImportPreviewData.value = [] }
}

const handleCareerImportRemove = () => {
    careerImportPreviewData.value = []; careerImportPreviewHeader.value = []; careerImportRawFile.value = null
}

const confirmCareerImport = async () => {
    if (!careerImportPreviewData.value.length) { ElMessage.warning('无数据'); return }
    try {
        let count = 0
        for (const row of careerImportPreviewData.value) {
            await objectApi.addCareerRecord(currentCareerLeaderId.value, {
                unitName: row['单位名称'] || '', position: row['职务'] || '',
                startDate: row['开始日期'] || '', endDate: row['结束日期'] || '',
                dutyDescription: row['职责描述'] || '',
                fundScope: parseFloat(row['分管资金(万)']) || 0
            })
            count++
        }
        ElMessage.success(`成功导入 ${count} 条履历`)
        careerImportVisible.value = false; fetchCareerHistory()
    } catch {}
}

// 推荐
const recommendVisible = ref(false); const recommendList = ref([])
const openRecommend = async () => { recommendVisible.value = true; try { const data = await objectApi.getLeaderRecommendForAudit({ limit: 20 }); recommendList.value = data?.list || [] } catch {} }
const importToPlan = async (row) => { ElMessage.success(`已将 ${row.leaderName} 加入经责审计推荐清单`) }

// 删除
const deleteLeader = async (row) => { try { await objectApi.deleteLeader(row.leaderId); ElMessage.success('已删除'); fetchLeaderTable() } catch {} }

// 导出
const leaderExportColumns = [{ prop: 'leaderCode', label: '干部编码' },{ prop: 'leaderName', label: '姓名' },{ prop: 'staffId', label: '工号' },{ prop: 'currentUnitName', label: '所属单位' },{ prop: 'currentPosition', label: '现任职务' },{ prop: 'tenureStartDate', label: '任职起始' },{ prop: 'tenureYears', label: '任职年限' },{ prop: 'fundScope', label: '分管资金(万)' },{ prop: 'auditCount', label: '审计次数' },{ prop: 'latestAuditDate', label: '最近审计' },{ prop: 'latestAuditConclusion', label: '审计结论' },{ prop: 'pendingRectifyCount', label: '未整改' }]
const handleExport = () => { const d = tableData.value.map(r => ({ ...r, isActive: r.isActive ? '在职' : '离任' })); exportExcel(d, '经责领导干部库', leaderExportColumns) }

// 导入
const importVisible = ref(false); const rawImportFile = ref(null); const importPreviewData = ref([]); const importPreviewHeader = ref([]); const importFileName = ref('')
const leaderImportTemplateFields = [{ label: '姓名', example: '张XX', required: true },{ label: '工号', example: 'T2020001', required: true },{ label: '所属单位', example: 'XX学院', required: true },{ label: '职务', example: '院长', required: true },{ label: '是否在职', example: '1-在职/0-离任', required: true },{ label: '任职起始日期', example: '2022-03-01', required: false },{ label: '任职年限', example: '4.3', required: false },{ label: '分管资金(万)', example: '5000', required: false },{ label: '审计结论', example: '履职情况良好', required: false }]
const downloadLeaderTemplate = () => downloadImportTemplate('领导干部导入模板', leaderImportTemplateFields)
const handleImportFile = async (file) => { importFileName.value = file.name; rawImportFile.value = file.raw; try { const r = await readExcel(file.raw); importPreviewHeader.value = r.header; importPreviewData.value = r.data; ElMessage.success(`成功读取 ${r.data.length} 条`) } catch (e) { ElMessage.error(e.message || '读取失败'); importPreviewData.value = [] } }
const confirmImport = async () => { if (!importPreviewData.value.length) { ElMessage.warning('无数据'); return }; try { for (const row of importPreviewData.value) { await objectApi.createLeader({ leaderName: row['姓名'] || '未命名', staffId: row['工号'] || '', currentUnitName: row['所属单位'] || row['单位'] || '', currentPosition: row['职务'] || row['现任职务'] || '', isActive: row['是否在职'] === '0' || row['是否在职'] === '离任' ? 0 : 1, tenureStartDate: row['任职起始日期'] || undefined, fundScope: parseFloat(row['分管资金(万)']) || 0 }) }; ElMessage.success(`成功导入 ${importPreviewData.value.length} 条`); importVisible.value = false; fetchLeaderTable() } catch {} }
const handleRemoveImportFile = () => { importFileName.value = ''; importPreviewData.value = []; importPreviewHeader.value = []; rawImportFile.value = null }

onMounted(() => fetchLeaderTable())
</script>

<template>
    <div class="lead-page">
        <el-card class="table-card" shadow="never">
            <div class="card-header">
                <span class="card-title">👤 经济责任领导干部库</span>
                <div class="card-actions">
                    <el-button @click="importVisible = true"><el-icon><Upload /></el-icon>导入Excel</el-button>
                    <el-button @click="handleExport">导出Excel</el-button>
                    <el-button type="warning" @click="openRecommend"><el-icon><List /></el-icon>滚动推荐</el-button>
                    <el-button type="primary" @click="openAddDialog"><el-icon><Plus /></el-icon>新建干部</el-button>
                </div>
            </div>
            <div class="search-bar">
                <el-form :inline="true" :model="searchForm">
                    <el-form-item><el-input v-model="searchForm.keyword" placeholder="🔍 搜索姓名/工号..." clearable style="width:220px" /></el-form-item>
                    <el-form-item><el-select v-model="searchForm.isActive" placeholder="在职/离任" clearable style="width:140px"><el-option label="在职" :value="1" /><el-option label="离任" :value="0" /></el-select></el-form-item>
                    <el-form-item><el-button type="primary" @click="fetchLeaderTable">查询</el-button><el-button @click="() => { searchForm.keyword = ''; searchForm.isActive = ''; fetchLeaderTable() }">重置</el-button></el-form-item>
                </el-form>
            </div>
            <el-table :data="tableData" v-loading="loading" stripe style="width:100%" height="calc(100vh - 260px)" row-key="leaderId">
                <el-table-column prop="leaderCode" label="编码" width="140" />
                <el-table-column prop="leaderName" label="姓名" width="80" fixed />
                <el-table-column prop="staffId" label="工号" width="110" />
                <el-table-column prop="currentUnitName" label="所属单位" width="150" show-overflow-tooltip />
                <el-table-column prop="currentPosition" label="现任职务" width="110" />
                <el-table-column prop="isActive" label="状态" width="75"><template #default="{ row }"><el-tag size="small" :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '在职' : '离任' }}</el-tag></template></el-table-column>
                <el-table-column prop="tenureYears" label="任职年限" width="85" sortable />
                <el-table-column prop="fundScope" label="分管资金(万)" width="130" sortable />
                <el-table-column prop="auditCount" label="审计次数" width="85" align="center" />
                <el-table-column prop="latestAuditDate" label="最近审计" width="105"><template #default="{ row }">{{ row.latestAuditDate || '-' }}</template></el-table-column>
                <el-table-column prop="latestAuditConclusion" label="最近审计结论" min-width="180" show-overflow-tooltip />
                <el-table-column prop="pendingRectifyCount" label="未整改" width="75" align="center"><template #default="{ row }"><el-tag v-if="row.pendingRectifyCount" type="danger" size="small">{{ row.pendingRectifyCount }}</el-tag><span v-else style="color:#67c23a">0</span></template></el-table-column>
                <el-table-column label="操作" width="250" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button size="small" link @click="openEditDialog(row)">编辑</el-button>
                        <el-button size="small" link @click="openCareerHistory(row)">履历</el-button>
                        <el-popconfirm title="确定删除？" @confirm="deleteLeader(row)"><template #reference><el-button size="small" link type="danger">删除</el-button></template></el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新建/编辑 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" draggable :close-on-click-modal="false">
            <el-form ref="formRef" :model="leaderForm" :rules="rules" label-width="110px">
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="姓名" prop="leaderName"><el-input v-model="leaderForm.leaderName" /></el-form-item></el-col><el-col :span="12"><el-form-item label="工号" prop="staffId"><el-input v-model="leaderForm.staffId" /></el-form-item></el-col></el-row>
                <el-form-item label="所属单位"><el-input v-model="leaderForm.unitName" placeholder="请输入/选择所属单位" /></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="职务" prop="position"><el-input v-model="leaderForm.position" /></el-form-item></el-col><el-col :span="12"><el-form-item label="是否在职"><el-radio-group v-model="leaderForm.isActive"><el-radio :value="1">在职</el-radio><el-radio :value="0">离任</el-radio></el-radio-group></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="任职起始"><el-date-picker v-model="leaderForm.tenureStartDate" type="date" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="分管资金(万)"><el-input-number v-model="leaderForm.fundScope" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
            </el-form>
            <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitLeader">确认</el-button></template>
        </el-dialog>

        <!-- 详情抽屉 -->
        <el-drawer v-model="detailDrawerVisible" title="领导干部详情" size="560px">
            <template v-if="detailLeader">
                <el-descriptions :column="2" border size="small"><el-descriptions-item label="编码">{{ detailLeader.leaderCode }}</el-descriptions-item><el-descriptions-item label="姓名">{{ detailLeader.leaderName }}</el-descriptions-item><el-descriptions-item label="工号">{{ detailLeader.staffId }}</el-descriptions-item><el-descriptions-item label="单位">{{ detailLeader.currentUnitName }}</el-descriptions-item><el-descriptions-item label="职务">{{ detailLeader.currentPosition }}</el-descriptions-item><el-descriptions-item label="状态"><el-tag size="small" :type="detailLeader.isActive ? 'success' : 'info'">{{ detailLeader.isActive ? '在职' : '离任' }}</el-tag></el-descriptions-item><el-descriptions-item label="任职起始">{{ detailLeader.tenureStartDate }}</el-descriptions-item><el-descriptions-item label="年限">{{ detailLeader.tenureYears }} 年</el-descriptions-item><el-descriptions-item label="分管资金">{{ detailLeader.fundScope }} 万</el-descriptions-item><el-descriptions-item label="最近审计">{{ detailLeader.latestAuditDate || '-' }}</el-descriptions-item></el-descriptions>
                <el-divider content-position="left">任职履历</el-divider>
                <el-timeline><el-timeline-item v-for="(item, i) in detailLeader.careerHistory" :key="i" :timestamp="`${item.start_date || item.startDate || ''} ~ ${item.end_date || item.endDate || '至今'}`" placement="top" :type="i === 0 ? 'primary' : 'info'"><el-card shadow="hover" size="small"><strong>{{ item.unit_name || item.unitName }} — {{ item.position }}</strong><div style="color:#606266;margin-top:4px">{{ item.duty_description || item.duty || '' }}</div><div style="font-size:12px;color:#909399">来源：{{ item.source || '手动录入' }}</div></el-card></el-timeline-item></el-timeline>
                <el-divider content-position="left">关联审计项目</el-divider>
                <el-table :data="detailLeader.auditProjects" size="small"><el-table-column prop="projectName" label="项目" min-width="180" /><el-table-column prop="period" label="期间" width="160" /><el-table-column prop="status" label="状态" width="80" /><el-table-column prop="conclusion" label="结论" min-width="160" show-overflow-tooltip /><el-table-column label="问题" width="80"><template #default="{ row }">{{ row.issueCount || 0 }}项，已整改{{ row.rectifiedCount || 0 }}</template></el-table-column></el-table>
            </template>
        </el-drawer>

        <!-- 推荐弹窗 -->
        <el-dialog v-model="recommendVisible" title="📊 滚动经责审计推荐清单" width="65%" draggable>
            <el-alert type="info" :closable="false" style="margin-bottom:14px" description="根据干部任职周期自动匹配，筛选应纳入年度经责审计计划的领导干部。" />
            <el-table :data="recommendList" size="small"><el-table-column prop="leaderName" label="姓名" width="80" /><el-table-column prop="unitName" label="单位" width="150" /><el-table-column prop="position" label="职务" width="90" /><el-table-column prop="tenureYears" label="任职年限" width="90" sortable /><el-table-column prop="lastAuditDate" label="上轮审计" width="110"><template #default="{ r }">{{ r.lastAuditDate || '无' }}</template></el-table-column><el-table-column prop="reason" label="推荐理由" min-width="220" show-overflow-tooltip /><el-table-column prop="priority" label="优先级" width="80"><template #default="{ r }"><el-tag size="small" :type="r.priority === 'HIGH' ? 'danger' : r.priority === 'MEDIUM' ? 'warning' : 'info'">{{ r.priority === 'HIGH' ? '高' : r.priority === 'MEDIUM' ? '中' : '低' }}</el-tag></template></el-table-column><el-table-column label="操作" width="130"><template #default="{ r }"><el-button size="small" type="primary" link @click="importToPlan(r)">导入年度计划</el-button></template></el-table-column></el-table>
        </el-dialog>

        <!-- 履历弹窗 -->
        <el-dialog v-model="careerVisible" title="📜 任职履历" width="60%" draggable>
            <div style="margin-bottom:12px;display:flex;gap:8px">
                <el-button type="primary" size="small" @click="openAddCareer"><el-icon><Plus /></el-icon>添加履历</el-button>
                <el-button size="small" @click="exportCareerHistory"><el-icon><Download /></el-icon>导出</el-button>
                <el-button size="small" @click="careerImportVisible = true"><el-icon><Upload /></el-icon>导入</el-button>
            </div>
            <el-timeline v-if="careerData.length > 0">
                <el-timeline-item v-for="(item, i) in careerData" :key="i" :timestamp="`${item.start_date || item.startDate || ''} ~ ${item.end_date || item.endDate || '至今'}`" placement="top" :type="i === 0 ? 'primary' : 'info'">
                    <el-card shadow="hover" size="small">
                        <div style="display:flex;justify-content:space-between;align-items:center">
                            <strong>{{ item.unit_name || item.unitName }} — {{ item.position }}</strong>
                            <div>
                                <el-button size="small" link @click="openEditCareer(item)">编辑</el-button>
                                <el-popconfirm title="确定删除？" @confirm="deleteCareer(item)"><template #reference><el-button size="small" link type="danger">删除</el-button></template></el-popconfirm>
                            </div>
                        </div>
                        <div style="color:#606266;margin-top:4px">{{ item.duty_description || item.duty || item.dutyDescription || '' }}</div>
                        <div style="font-size:12px;color:#909399">来源：{{ item.source || '手动录入' }}</div>
                    </el-card>
                </el-timeline-item>
            </el-timeline>
            <div v-else style="text-align:center;color:#909399;padding:40px">暂无任职履历记录</div>
        </el-dialog>

        <!-- 履历添加/编辑弹窗 -->
        <el-dialog v-model="careerFormVisible" :title="isEditCareer ? '编辑任职履历' : '添加任职履历'" width="500px" draggable :close-on-click-modal="false">
            <el-form :model="careerForm" label-width="100px">
                <el-form-item label="单位名称"><el-input v-model="careerForm.unitName" placeholder="请输入单位名称" /></el-form-item>
                <el-form-item label="职务"><el-input v-model="careerForm.position" placeholder="请输入职务" /></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="careerForm.startDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col><el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="careerForm.endDate" type="date" placeholder="留空表示至今" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col></el-row>
                <el-form-item label="职责描述"><el-input v-model="careerForm.dutyDescription" type="textarea" :rows="2" placeholder="分管工作描述" /></el-form-item>
                <el-form-item label="分管资金(万)"><el-input-number v-model="careerForm.fundScope" :min="0" :precision="2" style="width:100%" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="careerFormVisible = false">取消</el-button><el-button type="primary" @click="submitCareer">确认</el-button></template>
        </el-dialog>

        <!-- 履历导入弹窗 -->
        <el-dialog v-model="careerImportVisible" title="📥 批量导入任职履历" width="55%" draggable>
            <el-alert type="info" :closable="false" style="margin-bottom:14px" description="下载模板 → 填写 → 上传 → 预览 → 确认导入。导入的履历将关联到当前干部。" />
            <el-button @click="downloadCareerImportTemplate" style="margin-bottom:12px"><el-icon><Download /></el-icon>下载履历导入模板</el-button>
            <el-upload drag :auto-upload="false" :on-change="handleCareerImportFile" :on-remove="handleCareerImportRemove" accept=".xlsx,.xls" :limit="1" class="upload-block"><el-icon :size="36" color="#c0c4cc"><Upload /></el-icon><div class="el-upload__text">拖拽或<em>点击上传</em></div><template #tip><div class="el-upload__tip">仅支持 .xlsx / .xls</div></template></el-upload>
            <div v-if="careerImportPreviewData.length > 0" style="margin-top:14px"><el-alert type="success" :closable="false" :title="`已解析 ${careerImportPreviewData.length} 条`" style="margin-bottom:8px" /><el-table :data="careerImportPreviewData.slice(0,5)" size="small" border max-height="200"><el-table-column v-for="h in careerImportPreviewHeader" :key="h" :prop="h" :label="h" min-width="100" show-overflow-tooltip /></el-table></div>
            <template #footer><el-button @click="careerImportVisible = false">取消</el-button><el-button type="primary" @click="confirmCareerImport" :disabled="!careerImportPreviewData.length">确认导入（{{ careerImportPreviewData.length }} 条）</el-button></template>
        </el-dialog>

        <!-- 导入 -->
        <el-dialog v-model="importVisible" title="📥 批量导入领导干部" width="55%" draggable>
            <el-alert type="info" :closable="false" style="margin-bottom:14px" description="下载模板 → 填写 → 上传 → 预览 → 确认导入" />
            <el-button @click="downloadLeaderTemplate" style="margin-bottom:12px"><el-icon><Download /></el-icon>下载导入模板</el-button>
            <el-upload drag :auto-upload="false" :on-change="handleImportFile" :on-remove="handleRemoveImportFile" accept=".xlsx,.xls" :limit="1" class="upload-block"><el-icon :size="36" color="#c0c4cc"><Upload /></el-icon><div class="el-upload__text">拖拽或<em>点击上传</em></div><template #tip><div class="el-upload__tip">仅支持 .xlsx / .xls</div></template></el-upload>
            <div v-if="importPreviewData.length > 0" style="margin-top:14px"><el-alert type="success" :closable="false" :title="`已解析 ${importPreviewData.length} 条`" style="margin-bottom:8px" /><el-table :data="importPreviewData.slice(0,5)" size="small" border max-height="200"><el-table-column v-for="h in importPreviewHeader" :key="h" :prop="h" :label="h" min-width="100" show-overflow-tooltip /></el-table></div>
            <template #footer><el-button @click="importVisible = false">取消</el-button><el-button type="primary" @click="confirmImport" :disabled="!importPreviewData.length">确认导入（{{ importPreviewData.length }} 条）</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
.lead-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.table-card { flex: 1; display: flex; flex-direction: column; border-radius: 10px; overflow: hidden; }
.table-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 18px; background: #fafbfc; border-bottom: 1px solid #ebeef5; }
.card-title { font-size: 15px; font-weight: 700; color: #303133; }
.card-actions { display: flex; gap: 8px; }
.search-bar { padding: 10px 18px; border-bottom: 1px solid #ebeef5; }
.search-bar :deep(.el-form-item) { margin-bottom: 0; margin-right: 10px; }
:deep(.el-table) { font-size: 13px; }
:deep(.el-table th) { background: #fafbfc; color: #606266; font-weight: 600; }
.upload-block :deep(.el-upload) { width: 100%; }
.upload-block :deep(.el-upload-dragger) { width: 100%; border-radius: 10px; padding: 28px; }
</style>
