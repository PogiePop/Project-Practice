<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { readExcel, exportExcel, downloadImportTemplate } from '@/utils/excel'
import { objectApi } from '@/api'

const searchForm = reactive({ keyword: '', category: '' })
const unitCategories = [{ value: 0, label: '校内职能部门' },{ value: 1, label: '二级学院' },{ value: 2, label: '直属后勤单位' },{ value: 3, label: '校办企业' },{ value: 4, label: '基建项目部' },{ value: 5, label: '附属医院' }]
const tableData = ref([]); const loading = ref(false)

const fetchUnitTable = async () => {
    loading.value = true
    try { const data = await objectApi.getUnits({ keyword: searchForm.keyword || undefined, category: searchForm.category !== '' ? searchForm.category : undefined }); tableData.value = data?.list || [] } catch {} finally { loading.value = false }
}

// 新建/编辑
const dialogVisible = ref(false); const dialogTitle = ref('新建被审计单位'); const isEdit = ref(false)
const unitForm = reactive({ unitName: '', category: '', establishmentCount: null, fundScale: null, leaderInCharge: '', financeContact: '', financeContactPhone: '', address: '', setupDate: '' })
const formRef = ref(null)
const rules = { unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }], category: [{ required: true, message: '请选择分类', trigger: 'change' }] }
const leaderSelectList = ref([])
const loadLeaderSelectList = async () => { try { const d = await objectApi.getLeaders({ pageSize: 200 }); leaderSelectList.value = d?.list || [] } catch {} }
const openAddDialog = () => { dialogTitle.value = '新建被审计单位'; isEdit.value = false; Object.keys(unitForm).forEach(k => unitForm[k] = k === 'category' ? '' : (typeof unitForm[k] === 'number' ? null : '')); loadLeaderSelectList(); dialogVisible.value = true }
const openEditDialog = (row) => { dialogTitle.value = '编辑被审计单位'; isEdit.value = true; Object.assign(unitForm, { unitName: row.unitName, category: row.category, establishmentCount: row.establishmentCount, fundScale: row.fundScale, leaderInCharge: row.leaderInCharge, financeContact: row.financeContact, financeContactPhone: row.financeContactPhone, address: '', setupDate: '' }); unitForm._editId = row.unitId; loadLeaderSelectList(); dialogVisible.value = true }
const submitUnit = async () => { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; try { if (isEdit.value) { await objectApi.updateUnit(unitForm._editId, unitForm) } else { await objectApi.createUnit(unitForm) }; ElMessage.success(isEdit.value ? '修改成功' : '新建成功'); dialogVisible.value = false; fetchUnitTable() } catch {} }

// 详情
const detailDrawerVisible = ref(false); const detailUnit = ref(null)
const openDetail = async (row) => { detailUnit.value = { ...row, auditRecords: [], rectifyLedger: [], specialFunds: [] }; detailDrawerVisible.value = true; try { const [ar, rl, sf] = await Promise.all([objectApi.getUnitAuditRecords(row.unitId), objectApi.getUnitRectifyLedger(row.unitId), objectApi.getUnitSpecialFunds(row.unitId)]); detailUnit.value.auditRecords = ar?.list || []; detailUnit.value.rectifyLedger = rl?.list || []; detailUnit.value.specialFunds = sf?.list || [] } catch {} }

// 整改台账
const rectifyVisible = ref(false); const rectifyData = ref([])
const openRectify = async (row) => { rectifyVisible.value = true; try { const data = await objectApi.getUnitRectifyLedger(row.unitId); rectifyData.value = data?.list || [] } catch {} }

// 删除
const deleteUnit = async (row) => { try { await objectApi.deleteUnit(row.unitId); ElMessage.success('已删除'); fetchUnitTable() } catch {} }

// 导出
// 导出 — 包含所有基础信息 + 审计信息
const unitExportColumns = [
    { prop: 'unitCode', label: '单位编码' },{ prop: 'unitName', label: '单位名称' },
    { prop: 'categoryName', label: '分类' },{ prop: 'establishmentCount', label: '编制人数' },
    { prop: 'fundScale', label: '经费规模(万)' },{ prop: 'leaderInCharge', label: '分管校领导' },
    { prop: 'financeContact', label: '财务联系人' },{ prop: 'financeContactPhone', label: '联系电话' },
    { prop: 'address', label: '地址' },{ prop: 'setupDate', label: '设立日期' },
    { prop: 'totalAuditCount', label: '审计次数' },{ prop: 'latestAuditDate', label: '最近审计日期' },
    { prop: 'pendingRectifyCount', label: '待整改数' }
]
const handleExport = () => exportExcel(tableData.value, '被审计单位库', unitExportColumns)

// 导入 — 包含所有基础信息 + 审计信息
const importVisible = ref(false); const rawImportFile = ref(null); const importPreviewData = ref([]); const importPreviewHeader = ref([]); const importFileName = ref('')
const unitImportTemplateFields = [
    { label: '单位名称', example: 'XX学院', required: true },
    { label: '分类', example: '1-二级学院', required: true },
    { label: '编制人数', example: '85', required: false },
    { label: '经费规模(万)', example: '5000', required: false },
    { label: '分管校领导', example: '校领导A', required: false },
    { label: '财务联系人', example: '张会计', required: false },
    { label: '联系电话', example: '13800000001', required: false },
    { label: '地址', example: 'XX校区XX楼', required: false },
    { label: '设立日期', example: '2000-09-01', required: false },
    { label: '审计次数', example: '2', required: false },
    { label: '最近审计日期', example: '2023-05-10', required: false },
    { label: '待整改数', example: '0', required: false }
]
const downloadUnitTemplate = () => downloadImportTemplate('被审计单位导入模板', unitImportTemplateFields)
const handleImportFile = async (file) => { importFileName.value = file.name; rawImportFile.value = file.raw; try { const r = await readExcel(file.raw); importPreviewHeader.value = r.header; importPreviewData.value = r.data; ElMessage.success(`成功读取 ${r.data.length} 条`) } catch (e) { ElMessage.error(e.message || '读取失败'); importPreviewData.value = [] } }
const confirmImport = async () => { if (!importPreviewData.value.length) { ElMessage.warning('无数据'); return }; try { const categoryMap = { '校内职能部门': 0, '二级学院': 1, '直属后勤单位': 2, '校办企业': 3, '基建项目部': 4, '附属医院': 5 }; for (const row of importPreviewData.value) { await objectApi.createUnit({ unitName: row['单位名称'] || row['名称'] || '未命名', category: parseInt(row['分类']) || categoryMap[row['分类']] || 0, establishmentCount: parseInt(row['编制人数']) || 0, fundScale: parseFloat(row['经费规模(万)']) || 0, leaderInCharge: row['分管校领导'] || '', financeContact: row['财务联系人'] || '', financeContactPhone: row['联系电话'] || '', address: row['地址'] || '', setupDate: row['设立日期'] || undefined, totalAuditCount: parseInt(row['审计次数']) || 0, latestAuditDate: row['最近审计日期'] || null, pendingRectifyCount: parseInt(row['待整改数']) || 0 }) }; ElMessage.success(`成功导入 ${importPreviewData.value.length} 条`); importVisible.value = false; fetchUnitTable() } catch {} }
const handleRemoveImportFile = () => { importFileName.value = ''; importPreviewData.value = []; importPreviewHeader.value = []; rawImportFile.value = null }

onMounted(() => fetchUnitTable())
</script>

<template>
    <div class="unit-page">
        <el-card class="table-card" shadow="never">
            <div class="card-header">
                <span class="card-title">🏢 被审计单位库</span>
                <div class="card-actions">
                    <el-button @click="importVisible = true"><el-icon><Upload /></el-icon>导入Excel</el-button>
                    <el-button @click="handleExport">导出Excel</el-button>
                    <el-button type="primary" @click="openAddDialog"><el-icon><Plus /></el-icon>新建单位</el-button>
                </div>
            </div>
            <div class="search-bar">
                <el-form :inline="true" :model="searchForm">
                    <el-form-item><el-input v-model="searchForm.keyword" placeholder="🔍 搜索单位名称/编码..." clearable style="width:240px" /></el-form-item>
                    <el-form-item><el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width:170px"><el-option v-for="c in unitCategories" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item>
                    <el-form-item><el-button type="primary" @click="fetchUnitTable">查询</el-button><el-button @click="() => { searchForm.keyword = ''; searchForm.category = ''; fetchUnitTable() }">重置</el-button></el-form-item>
                </el-form>
            </div>
            <el-table :data="tableData" v-loading="loading" stripe style="width:100%" height="calc(100vh - 260px)" row-key="unitId">
                <el-table-column prop="unitCode" label="单位编码" width="150" />
                <el-table-column prop="unitName" label="单位名称" width="170" fixed show-overflow-tooltip />
                <el-table-column prop="categoryName" label="分类" width="140"><template #default="{ row }"><el-tag size="small">{{ row.categoryName }}</el-tag></template></el-table-column>
                <el-table-column prop="establishmentCount" label="编制" width="70" align="center" />
                <el-table-column prop="fundScale" label="经费(万)" width="110" sortable />
                <el-table-column prop="leaderInCharge" label="分管校领导" width="110" />
                <el-table-column prop="financeContact" label="财务联系人" width="110" />
                <el-table-column prop="totalAuditCount" label="审计次数" width="85" align="center" />
                <el-table-column prop="latestAuditDate" label="最近审计" width="110"><template #default="{ row }">{{ row.latestAuditDate || '-' }}</template></el-table-column>
                <el-table-column prop="pendingRectifyCount" label="待整改" width="75" align="center"><template #default="{ row }"><el-tag v-if="row.pendingRectifyCount > 0" type="danger" size="small">{{ row.pendingRectifyCount }}</el-tag><span v-else style="color:#67c23a">0</span></template></el-table-column>
                <el-table-column label="操作" width="250" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
                        <el-button size="small" link @click="openEditDialog(row)">编辑</el-button>
                        <el-button size="small" link @click="openRectify(row)">整改</el-button>
                        <el-popconfirm title="确定删除？" @confirm="deleteUnit(row)"><template #reference><el-button size="small" link type="danger">删除</el-button></template></el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新建/编辑 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" draggable :close-on-click-modal="false">
            <el-form ref="formRef" :model="unitForm" :rules="rules" label-width="110px">
                <el-form-item label="单位名称" prop="unitName"><el-input v-model="unitForm.unitName" /></el-form-item>
                <el-form-item label="分类" prop="category"><el-select v-model="unitForm.category" style="width:100%"><el-option v-for="c in unitCategories" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="编制人数"><el-input-number v-model="unitForm.establishmentCount" :min="0" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="经费规模(万)"><el-input-number v-model="unitForm.fundScale" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
                <el-form-item label="分管校领导"><el-select v-model="unitForm.leaderInCharge" placeholder="请选择分管校领导" clearable filterable style="width:100%"><el-option v-for="l in leaderSelectList" :key="l.leaderId" :label="l.leaderName" :value="l.leaderName" /></el-select></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="财务联系人"><el-select v-model="unitForm.financeContact" placeholder="请选择财务联系人" clearable filterable style="width:100%"><el-option v-for="l in leaderSelectList" :key="l.leaderId" :label="l.leaderName" :value="l.leaderName" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="联系电话"><el-input v-model="unitForm.financeContactPhone" /></el-form-item></el-col></el-row>
                <el-form-item label="地址"><el-input v-model="unitForm.address" /></el-form-item>
                <el-form-item label="设立日期"><el-date-picker v-model="unitForm.setupDate" type="date" style="width:100%" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitUnit">确认</el-button></template>
        </el-dialog>

        <!-- 详情抽屉 -->
        <el-drawer v-model="detailDrawerVisible" title="单位详情" size="580px">
            <template v-if="detailUnit">
                <el-descriptions :column="2" border size="small"><el-descriptions-item label="编码">{{ detailUnit.unitCode }}</el-descriptions-item><el-descriptions-item label="名称">{{ detailUnit.unitName }}</el-descriptions-item><el-descriptions-item label="分类">{{ detailUnit.categoryName }}</el-descriptions-item><el-descriptions-item label="编制">{{ detailUnit.establishmentCount }}</el-descriptions-item><el-descriptions-item label="经费">{{ detailUnit.fundScale }} 万</el-descriptions-item><el-descriptions-item label="分管领导">{{ detailUnit.leaderInCharge }}</el-descriptions-item><el-descriptions-item label="联系人">{{ detailUnit.financeContact }}</el-descriptions-item><el-descriptions-item label="电话">{{ detailUnit.financeContactPhone }}</el-descriptions-item></el-descriptions>
                <el-divider content-position="left">审计记录</el-divider>
                <el-table :data="detailUnit.auditRecords" size="small"><el-table-column prop="projectName" label="项目" min-width="180" /><el-table-column prop="date" label="日期" width="100" /><el-table-column prop="type" label="类型" width="110" /><el-table-column prop="issueCount" label="问题数" width="70" /></el-table>
                <el-divider content-position="left">整改台账</el-divider>
                <el-table :data="detailUnit.rectifyLedger" size="small"><el-table-column prop="issue_description" label="问题" min-width="180" show-overflow-tooltip /><el-table-column label="状态" width="80"><template #default="{ row }"><el-tag size="small" :type="row.rectify_status === 2 ? 'success' : row.rectify_status === 1 ? 'warning' : 'danger'">{{ row.rectify_status === 2 ? '已整改' : row.rectify_status === 1 ? '整改中' : '未整改' }}</el-tag></template></el-table-column></el-table>
                <el-divider content-position="left">专项资金</el-divider>
                <el-table :data="detailUnit.specialFunds" size="small"><el-table-column prop="fundName" label="项目" min-width="180" /><el-table-column prop="source" label="来源" width="90" /><el-table-column prop="amount" label="金额" width="80" /></el-table>
            </template>
        </el-drawer>

        <!-- 整改台账 -->
        <el-dialog v-model="rectifyVisible" title="📋 整改台账" width="65%" draggable>
            <el-table :data="rectifyData" size="small"><el-table-column prop="batch_id" label="关联项目" width="150" /><el-table-column prop="issue_description" label="问题描述" min-width="200" show-overflow-tooltip /><el-table-column prop="issue_category" label="类别" width="100" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag size="small" :type="row.rectify_status === 2 ? 'success' : row.rectify_status === 1 ? 'warning' : 'danger'">{{ row.rectify_status === 2 ? '已整改' : row.rectify_status === 1 ? '整改中' : '未整改' }}</el-tag></template></el-table-column><el-table-column prop="responsible_person" label="责任人" width="80" /><el-table-column prop="deadline" label="截止" width="110" /><el-table-column prop="rectify_progress" label="进展" min-width="160" show-overflow-tooltip /></el-table>
        </el-dialog>

        <!-- 导入 -->
        <el-dialog v-model="importVisible" title="📥 批量导入被审计单位" width="55%" draggable>
            <el-alert type="info" :closable="false" style="margin-bottom:14px" description="下载模板 → 填写 → 上传 → 预览 → 确认导入" />
            <el-button @click="downloadUnitTemplate" style="margin-bottom:12px"><el-icon><Download /></el-icon>下载导入模板</el-button>
            <el-upload drag :auto-upload="false" :on-change="handleImportFile" :on-remove="handleRemoveImportFile" accept=".xlsx,.xls" :limit="1" class="upload-block"><el-icon :size="36" color="#c0c4cc"><Upload /></el-icon><div class="el-upload__text">拖拽或<em>点击上传</em></div><template #tip><div class="el-upload__tip">仅支持 .xlsx / .xls</div></template></el-upload>
            <div v-if="importPreviewData.length > 0" style="margin-top:14px"><el-alert type="success" :closable="false" :title="`已解析 ${importPreviewData.length} 条`" style="margin-bottom:8px" /><el-table :data="importPreviewData.slice(0,5)" size="small" border max-height="200"><el-table-column v-for="h in importPreviewHeader" :key="h" :prop="h" :label="h" min-width="100" show-overflow-tooltip /></el-table></div>
            <template #footer><el-button @click="importVisible = false">取消</el-button><el-button type="primary" @click="confirmImport" :disabled="!importPreviewData.length">确认导入（{{ importPreviewData.length }} 条）</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
.unit-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
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
