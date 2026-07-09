<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User, View, Hide } from '@element-plus/icons-vue'
import { commonApi } from '@/api'

const router = useRouter()

const loginForm = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const loading = ref(false)

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6-30 个字符', trigger: 'blur' }
  ]
}

const formRef = ref(null)

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await commonApi.login({
      username: loginForm.username,
      password: loginForm.password
    })
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    ElMessage.success('登录成功，欢迎使用审计信息管理系统')
    router.push('/')
  } catch {
    // 请求失败时拦截器已提示，此处忽略
  } finally {
    loading.value = false
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter') handleLogin()
}
</script>

<template>
  <div class="login-container" @keydown="handleKeyDown">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-dots"></div>
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
      <div class="bg-shape shape-4"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="system-icon">
          <el-icon :size="36"><Lock /></el-icon>
        </div>
        <h1>审计信息管理系统</h1>
        <p>基础数据底座 · 审计全覆盖</p>
      </div>
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        size="large"
        autocomplete="off"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名 / 工号"
            :prefix-icon="User"
            name="audit-username"
            autocomplete="off"
            readonly
            @focus="(e) => { e.target.removeAttribute('readonly') }"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            name="audit-password"
            autocomplete="new-password"
            readonly
            @focus="(e) => { e.target.removeAttribute('readonly') }"
            clearable
          >
            <template #suffix>
              <el-icon class="pwd-toggle" @click="showPassword = !showPassword">
                <component :is="showPassword ? Hide : View" />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>学校审计处 · 信息技术中心联合出品</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0e27;
  position: relative;
  overflow: hidden;
}
.login-bg { position: absolute; inset: 0; pointer-events: none; }
.bg-gradient {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse at 20% 50%, #1a237e 0%, transparent 50%),
              radial-gradient(ellipse at 80% 20%, #0d47a1 0%, transparent 50%),
              radial-gradient(ellipse at 50% 80%, #1a237e 0%, transparent 50%),
              linear-gradient(135deg, #0a0e27 0%, #1a1a3e 50%, #0d1b3e 100%);
  animation: bgShift 15s ease-in-out infinite alternate;
  background-size: 200% 200%;
}
@keyframes bgShift {
  0% { background-position: 0% 0%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 50% 100%; }
}
.bg-dots {
  position: absolute; inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}
.bg-shape { position: absolute; border-radius: 50%; opacity: 0.06; background: #fff; filter: blur(80px); }
.shape-1 { width: 600px; height: 600px; top: -300px; right: -200px; animation: float 20s ease-in-out infinite; }
.shape-2 { width: 400px; height: 400px; bottom: -150px; left: -150px; animation: float 25s ease-in-out infinite reverse; }
.shape-3 { width: 250px; height: 250px; top: 40%; left: 5%; animation: float 18s ease-in-out infinite; }
.shape-4 { width: 350px; height: 350px; top: 10%; right: 30%; animation: float 22s ease-in-out infinite; opacity: 0.04; }
@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg) scale(1); }
  33% { transform: translate(40px, -40px) rotate(8deg) scale(1.1); }
  66% { transform: translate(-30px, 30px) rotate(-5deg) scale(0.9); }
}
.login-card {
  width: 420px; background: rgba(255,255,255,0.85); border-radius: 20px;
  padding: 48px 40px 36px; box-shadow: 0 8px 40px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.1);
  position: relative; z-index: 1; backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}
.login-header { text-align: center; margin-bottom: 36px; }
.system-icon {
  width: 64px; height: 64px; background: linear-gradient(135deg, #409EFF, #337ECC);
  border-radius: 16px; display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px; color: #fff; box-shadow: 0 6px 20px rgba(64,158,255,0.35);
}
.login-header h1 { font-size: 22px; font-weight: 700; color: #1f2937; margin: 0 0 6px; letter-spacing: 1px; }
.login-header p { font-size: 13px; color: #909399; margin: 0; letter-spacing: 2px; }
.login-form { margin-top: 8px; }
.login-form :deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #e4e7ed inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #409EFF inset !important; }
.pwd-toggle { cursor: pointer; color: #909399; font-size: 16px; }
.pwd-toggle:hover { color: #409EFF; }
.login-btn { width: 100%; border-radius: 8px; font-size: 16px; letter-spacing: 4px; height: 44px; margin-top: 8px; }
.login-footer { text-align: center; margin-top: 24px; font-size: 12px; color: #c0c4cc; }
@media (max-width: 480px) { .login-card { width: 90%; padding: 36px 24px 28px; } }
</style>
