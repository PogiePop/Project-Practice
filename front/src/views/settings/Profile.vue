<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Edit, Lock, Phone, OfficeBuilding, Tickets, ArrowRight } from '@element-plus/icons-vue'
import { commonApi } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const userInfo = reactive({
    username: '', realName: '', staffId: '', department: '', position: '',
    phone: '', email: '', avatar: '', role: '', lastLogin: ''
})

const fetchUserInfo = async () => {
    try {
        const data = await commonApi.getUserInfo()
        if (data) Object.assign(userInfo, data)
    } catch {}
}

// 编辑资料
const editVisible = ref(false)
const editForm = reactive({ realName: '', phone: '', email: '', department: '', position: '' })
const editFormRef = ref(null)
const editRules = {
    realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确邮箱', trigger: 'blur' }]
}

const openEdit = () => {
    editForm.realName = userInfo.realName || ''
    editForm.phone = userInfo.phone || ''
    editForm.email = userInfo.email || ''
    editForm.department = userInfo.department || ''
    editForm.position = userInfo.position || ''
    editVisible.value = true
}

const submitEdit = async () => {
    const valid = await editFormRef.value?.validate().catch(() => false)
    if (!valid) return
    try {
        await commonApi.updateUserInfo(editForm)
        Object.assign(userInfo, editForm)
        ElMessage.success('个人信息已更新')
        editVisible.value = false
    } catch {}
}

// 修改密码
const passwordVisible = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordFormRef = ref(null)
const passwordRules = {
    oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
    newPassword: [{ required: true, min: 6, max: 30, message: '密码长度 6-30 位', trigger: 'blur' }],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: (_rule, value, callback) => { if (value !== passwordForm.newPassword) callback(new Error('两次输入不一致')); else callback() }, trigger: 'blur' }
    ]
}

const submitPassword = async () => {
    const valid = await passwordFormRef.value?.validate().catch(() => false)
    if (!valid) return
    try {
        await commonApi.changePassword({
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功')
        passwordVisible.value = false
        passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''
    } catch {}
}

const roleName = (r) => ({ DIRECTOR: '处长', AUDIT_LEADER: '审计组长' }[r] || '审计人员')

onMounted(() => fetchUserInfo())
</script>

<template>
    <div class="profile-page">
        <el-card shadow="never" class="info-card">
            <div class="avatar-section">
                <el-avatar :size="80" :icon="UserFilled" :src="userInfo.avatar" />
                <div class="avatar-text">
                    <h3>{{ userInfo.realName || userInfo.username || '未设置' }}</h3>
                    <p>{{ roleName(userInfo.role) }}</p>
                </div>
                <el-button type="primary" plain size="small" @click="openEdit"><el-icon><Edit /></el-icon>编辑资料</el-button>
            </div>
            <el-divider />
            <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="用户名"><el-icon style="margin-right:4px"><Tickets /></el-icon>{{ userInfo.username || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工号">{{ userInfo.staffId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="部门"><el-icon style="margin-right:4px"><OfficeBuilding /></el-icon>{{ userInfo.department || '-' }}</el-descriptions-item>
                <el-descriptions-item label="职务">{{ userInfo.position || '-' }}</el-descriptions-item>
                <el-descriptions-item label="手机"><el-icon style="margin-right:4px"><Phone /></el-icon>{{ userInfo.phone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
                <el-descriptions-item label="最近登录">{{ userInfo.lastLogin || '-' }}</el-descriptions-item>
                <el-descriptions-item label="角色">
                    <el-tag size="small" :type="userInfo.role === 'DIRECTOR' ? 'danger' : 'primary'">{{ roleName(userInfo.role) }}</el-tag>
                </el-descriptions-item>
            </el-descriptions>
        </el-card>

        <el-row :gutter="12" style="margin-top:12px">
            <el-col :span="12">
                <el-card shadow="never" class="action-card" @click="passwordVisible = true">
                    <div class="action-content">
                        <el-icon :size="28" color="#409EFF"><Lock /></el-icon>
                        <div><strong>修改密码</strong><p>定期更新密码保障账户安全</p></div>
                        <el-icon color="#c0c4cc"><ArrowRight /></el-icon>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="12">
                <el-card shadow="never" class="action-card" @click="router.push('/settings/system')">
                    <div class="action-content">
                        <el-icon :size="28" color="#e6a23c"><Setting /></el-icon>
                        <div><strong>系统设置</strong><p>通知偏好、界面显示等</p></div>
                        <el-icon color="#c0c4cc"><ArrowRight /></el-icon>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 编辑资料弹窗 -->
        <el-dialog v-model="editVisible" title="编辑个人信息" width="500px" draggable :close-on-click-modal="false">
            <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
                <el-form-item label="姓名" prop="realName"><el-input v-model="editForm.realName" /></el-form-item>
                <el-form-item label="手机号" prop="phone"><el-input v-model="editForm.phone" /></el-form-item>
                <el-form-item label="邮箱" prop="email"><el-input v-model="editForm.email" /></el-form-item>
                <el-form-item label="部门"><el-input v-model="editForm.department" /></el-form-item>
                <el-form-item label="职务"><el-input v-model="editForm.position" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="editVisible = false">取消</el-button><el-button type="primary" @click="submitEdit">保存</el-button></template>
        </el-dialog>

        <!-- 修改密码弹窗 -->
        <el-dialog v-model="passwordVisible" title="修改密码" width="480px" draggable :close-on-click-modal="false">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
                <el-form-item label="当前密码" prop="oldPassword"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
                <el-form-item label="新密码" prop="newPassword"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
            </el-form>
            <template #footer><el-button @click="passwordVisible = false">取消</el-button><el-button type="primary" @click="submitPassword">确认修改</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
.profile-page { padding: 16px; height: 100%; overflow-y: auto; }
.info-card { border-radius: 10px; }
.avatar-section { display: flex; align-items: center; gap: 16px; }
.avatar-text h3 { margin: 0; font-size: 20px; color: #303133; }
.avatar-text p { margin: 4px 0 0; font-size: 13px; color: #909399; }
.action-card { border-radius: 10px; cursor: pointer; transition: all 0.2s; }
.action-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); transform: translateY(-1px); }
.action-content { display: flex; align-items: center; gap: 14px; }
.action-content div { flex: 1; }
.action-content strong { font-size: 14px; color: #303133; }
.action-content p { margin: 3px 0 0; font-size: 12px; color: #909399; }
</style>
