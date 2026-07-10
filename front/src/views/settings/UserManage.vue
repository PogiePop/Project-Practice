<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { commonApi } from '@/api'

const tableData = ref([])
const fetchUsers = async () => {
    try { const d = await commonApi.getUserList(); tableData.value = d?.list || [] } catch {}
}

// 新建/编辑
const dialogVisible = ref(false); const isEdit = ref(false)
const userForm = reactive({ username: '', password: '', realName: '', staffId: '', department: '', position: '', phone: '', email: '', roleLevel: 1 })
const formRef = ref(null)
const rules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }

const openAdd = () => { isEdit.value = false; Object.keys(userForm).forEach(k => userForm[k] = k === 'roleLevel' ? 1 : ''); dialogVisible.value = true }
const openEdit = (row) => { isEdit.value = true; Object.assign(userForm, row); userForm.password = ''; dialogVisible.value = true }
const submitUser = async () => {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
    try {
        if (isEdit.value) { await commonApi.updateUser(userForm.username, userForm) }
        else { await commonApi.createUser(userForm) }
        ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
        dialogVisible.value = false; fetchUsers()
    } catch {}
}

const deleteUser = async (row) => {
    if (row.username === 'admin') { ElMessage.warning('不能删除超级管理员'); return }
    try { await commonApi.deleteUser(row.username); ElMessage.success('已删除'); fetchUsers() } catch {}
}

const resetPwd = async (row) => {
    try { await commonApi.resetUserPassword(row.username, { password: '123456' }); ElMessage.success(`已重置 ${row.username} 的密码为 123456`) } catch {}
}

onMounted(() => fetchUsers())
</script>

<template>
    <div class="usermg-page">
        <el-card class="table-card" shadow="never">
            <div class="card-header">
                <span class="card-title">👥 用户管理</span>
                <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>新建用户</el-button>
            </div>
            <el-table :data="tableData" stripe style="width:100%" height="calc(100vh - 200px)">
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="realName" label="姓名" width="100" />
                <el-table-column prop="staffId" label="工号" width="110" />
                <el-table-column prop="department" label="部门" width="120" />
                <el-table-column prop="position" label="职务" width="100" />
                <el-table-column prop="phone" label="手机" width="130" />
                <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
                <el-table-column label="角色" width="120">
                    <template #default="{ row }">
                        <el-tag :type="(row.roleLevel == 0) ? 'danger' : ''" size="small">{{ row.roleLevel == 0 ? '处长（超级管理员）' : '审计组员' }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" link @click="openEdit(row)">编辑</el-button>
                        <el-button size="small" link @click="resetPwd(row)">重置密码</el-button>
                        <el-popconfirm title="确定删除？" @confirm="deleteUser(row)"><template #reference><el-button size="small" link type="danger" :disabled="row.username === 'admin'">删除</el-button></template></el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="500px" draggable :close-on-click-modal="false">
            <el-form ref="formRef" :model="userForm" :rules="rules" label-width="80px" autocomplete="off">
                <el-form-item label="用户名" prop="username"><el-input v-model="userForm.username" :disabled="isEdit" /></el-form-item>
                <el-form-item v-if="!isEdit" label="密码"><el-input v-model="userForm.password" type="password" placeholder="默认123456" autocomplete="new-password" name="new-user-pwd" readonly @focus="(e) => e.target.removeAttribute('readonly')" /></el-form-item>
                <el-form-item label="姓名" prop="realName"><el-input v-model="userForm.realName" /></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="工号"><el-input v-model="userForm.staffId" /></el-form-item></el-col><el-col :span="12"><el-form-item label="角色"><el-select v-model="userForm.roleLevel"><el-option :value="0" label="处长（超级管理员）" /><el-option :value="1" label="审计组员" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="部门"><el-input v-model="userForm.department" /></el-form-item></el-col><el-col :span="12"><el-form-item label="职务"><el-input v-model="userForm.position" /></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="手机"><el-input v-model="userForm.phone" /></el-form-item></el-col><el-col :span="12"><el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item></el-col></el-row>
            </el-form>
            <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitUser">确认</el-button></template>
        </el-dialog>
    </div>
</template>

<style scoped>
.usermg-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.table-card { flex: 1; border-radius: 10px; }
.table-card :deep(.el-card__body) { padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 18px; background: #fafbfc; border-bottom: 1px solid #ebeef5; }
.card-title { font-size: 15px; font-weight: 700; }
</style>
