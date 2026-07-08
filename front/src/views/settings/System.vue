<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Moon, Sunny, DataBoard, ChatLineSquare } from '@element-plus/icons-vue'
import { commonApi } from '@/api'

// 通知设置
const notifySettings = reactive({
    alertPush: true, approvalNotify: true, syncNotify: false, emailNotify: false, soundOn: true
})

// 界面设置
const uiSettings = reactive({
    theme: 'light', tableDensity: 'medium', pageSize: 20, showBreadcrumb: true, autoRefresh: true, refreshInterval: 60
})

// 从后端加载设置
const loadSettings = async () => {
    try {
        const data = await commonApi.getSettings()
        if (data?.notify) {
            try {
                // 后端存储的是 Map.toString() 格式，需要解析
                const n = typeof data.notify === 'string' ? parseMapString(data.notify) : data.notify
                if (n) Object.assign(notifySettings, n)
            } catch {}
        }
        if (data?.ui) {
            try {
                const u = typeof data.ui === 'string' ? parseMapString(data.ui) : data.ui
                if (u) Object.assign(uiSettings, u)
            } catch {}
        }
    } catch {}
}

// 解析后端返回的设置值（支持 JSON 和 Java Map.toString 两种格式）
const parseMapString = (str) => {
    if (!str || typeof str !== 'string') return null
    // 优先尝试 JSON 格式
    if (str.startsWith('{') && str.includes('"')) {
        try { return JSON.parse(str) } catch {}
    }
    // Java Map.toString 格式: {key=value, key=value}
    const obj = {}
    str = str.replace(/[{}]/g, '')
    str.split(',').forEach(pair => {
        const eqIdx = pair.indexOf('=')
        if (eqIdx === -1) return
        const k = pair.substring(0, eqIdx).trim()
        const v = pair.substring(eqIdx + 1).trim()
        if (!k) return
        if (v === 'true') obj[k] = true
        else if (v === 'false') obj[k] = false
        else if (!isNaN(v) && v !== '') obj[k] = Number(v)
        else obj[k] = v
    })
    return obj
}

const saveNotify = async () => {
    try {
        await commonApi.saveNotifySettings({ ...notifySettings })
        ElMessage.success('通知设置已保存')
    } catch {}
}

const saveUI = async () => {
    try {
        await commonApi.saveUISettings({ ...uiSettings })
        ElMessage.success('界面设置已保存')
    } catch {}
}

const saveAll = async () => {
    try {
        await commonApi.saveAllSettings({ notify: { ...notifySettings }, ui: { ...uiSettings } })
        ElMessage.success('全部设置已保存')
    } catch {}
}

const clearCache = () => {
    localStorage.clear()
    ElMessage.success('缓存已清除，请刷新页面')
}

// 系统信息
const systemInfo = {
    version: 'V2.1.0', buildDate: '2026-07-01',
    frontend: 'Vue 3 + Element Plus + Vite',
    backend: 'Spring Boot 3.4 + MyBatis',
    database: 'MySQL 8.0', deployDate: '2026-07-08'
}

onMounted(() => loadSettings())
</script>

<template>
    <div class="system-page">
        <!-- 通知设置 -->
        <el-card shadow="never" class="setting-card">
            <template #header>
                <div class="card-header-row"><el-icon color="#409EFF"><Bell /></el-icon><span>通知偏好设置</span></div>
            </template>
            <el-form label-width="130px" label-position="left">
                <el-form-item label="预警消息推送">
                    <el-switch v-model="notifySettings.alertPush" />
                    <span class="form-hint">项目超期、实施滞后的预警实时推送</span>
                </el-form-item>
                <el-form-item label="审批流程通知">
                    <el-switch v-model="notifySettings.approvalNotify" />
                    <span class="form-hint">审批节点到达、审批结果通知</span>
                </el-form-item>
                <el-form-item label="同步完成通知">
                    <el-switch v-model="notifySettings.syncNotify" />
                    <span class="form-hint">数据中台同步完成时通知</span>
                </el-form-item>
                <el-form-item label="邮件通知">
                    <el-switch v-model="notifySettings.emailNotify" />
                    <span class="form-hint">重要事项同时发送邮件</span>
                </el-form-item>
                <el-form-item label="声音提醒">
                    <el-switch v-model="notifySettings.soundOn" />
                </el-form-item>
                <el-form-item><el-button type="primary" @click="saveNotify">保存通知设置</el-button></el-form-item>
            </el-form>
        </el-card>

        <!-- 界面设置 -->
        <el-card shadow="never" class="setting-card">
            <template #header>
                <div class="card-header-row"><el-icon color="#e6a23c"><DataBoard /></el-icon><span>界面显示设置</span></div>
            </template>
            <el-form label-width="130px" label-position="left">
                <el-form-item label="主题模式">
                    <el-radio-group v-model="uiSettings.theme">
                        <el-radio-button value="light"><el-icon><Sunny /></el-icon> 浅色</el-radio-button>
                        <el-radio-button value="dark"><el-icon><Moon /></el-icon> 深色</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="表格密度">
                    <el-radio-group v-model="uiSettings.tableDensity">
                        <el-radio-button value="small">紧凑</el-radio-button>
                        <el-radio-button value="medium">适中</el-radio-button>
                        <el-radio-button value="large">宽松</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="默认每页条数">
                    <el-select v-model="uiSettings.pageSize" style="width:140px">
                        <el-option :value="10" label="10 条" />
                        <el-option :value="20" label="20 条" />
                        <el-option :value="50" label="50 条" />
                        <el-option :value="100" label="100 条" />
                    </el-select>
                </el-form-item>
                <el-form-item label="显示面包屑">
                    <el-switch v-model="uiSettings.showBreadcrumb" />
                </el-form-item>
                <el-form-item label="自动刷新数据">
                    <el-switch v-model="uiSettings.autoRefresh" />
                </el-form-item>
                <el-form-item label="刷新间隔(秒)" v-if="uiSettings.autoRefresh">
                    <el-input-number v-model="uiSettings.refreshInterval" :min="10" :max="600" :step="10" />
                </el-form-item>
                <el-form-item><el-button type="primary" @click="saveUI">保存界面设置</el-button></el-form-item>
            </el-form>
        </el-card>

        <!-- 关于 + 操作 -->
        <el-card shadow="never" class="setting-card">
            <template #header>
                <div class="card-header-row"><el-icon color="#67c23a"><ChatLineSquare /></el-icon><span>关于系统</span></div>
            </template>
            <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="系统版本">{{ systemInfo.version }}</el-descriptions-item>
                <el-descriptions-item label="构建日期">{{ systemInfo.buildDate }}</el-descriptions-item>
                <el-descriptions-item label="前端技术栈">{{ systemInfo.frontend }}</el-descriptions-item>
                <el-descriptions-item label="后端技术栈">{{ systemInfo.backend }}</el-descriptions-item>
                <el-descriptions-item label="数据库">{{ systemInfo.database }}</el-descriptions-item>
                <el-descriptions-item label="部署日期">{{ systemInfo.deployDate }}</el-descriptions-item>
            </el-descriptions>
            <div style="margin-top:16px;display:flex;gap:12px">
                <el-button type="primary" @click="saveAll">保存全部设置</el-button>
                <el-button @click="clearCache" type="danger" plain>清除本地缓存</el-button>
            </div>
        </el-card>
    </div>
</template>

<style scoped>
.system-page { padding: 16px; height: 100%; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.setting-card { border-radius: 10px; }
.card-header-row { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 15px; color: #303133; }
.form-hint { margin-left: 12px; font-size: 12px; color: #909399; }
:deep(.el-form-item) { margin-bottom: 18px; }
:deep(.el-form-item:last-child) { margin-bottom: 0; }
</style>
