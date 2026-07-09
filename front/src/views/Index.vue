<script setup>
import router from '@/router/router';
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { UserFilled, Bell, SwitchButton, Setting, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus';
import { commonApi } from '@/api'

const route = useRoute();
const activedMenu = ref(route.path);

// 从 localStorage 读取当前登录用户信息
const userInfo = computed(() => {
  try { const raw = localStorage.getItem('userInfo'); if (raw) return JSON.parse(raw) } catch {}
  return {}
})
const isSuperAdmin = computed(() => userInfo.value.roleLevel == 0 || userInfo.value.role_level == 0)
const userName = computed(() => {
  const u = userInfo.value
  return (u.realName || u.username || '未登录') + '·' + (u.position || u.role || '')
})
const openedMenu = ref(['/plan', '/object']);

const menuRef = ref(null);
const handleClose = (index) => {
  if (openedMenu.value.includes(index)) {
    nextTick(() => { menuRef.value.open(index); })
  }
}

const warnCount = ref(0);
const messageList = ref([])

const fetchMessages = async () => {
  try {
    const data = await commonApi.getMessages({ isRead: 0, pageSize: 50 })
    messageList.value = Array.isArray(data?.list) ? data.list : (Array.isArray(data) ? data : [])
  } catch {}
  warnCount.value = messageList.value.filter(m => !m.isRead).length
}

const unreadCount = computed(() => messageList.value.filter(m => !m.isRead).length)
const messageDrawerVisible = ref(false)

const markAsRead = (msg) => {
  msg.isRead = 1
  warnCount.value = messageList.value.filter(m => !m.isRead).length
  commonApi.markMessageRead(msg.messageId).catch(() => {})
}
const markAllRead = () => {
  messageList.value.forEach(m => m.isRead = 1)
  warnCount.value = 0
  commonApi.markAllMessagesRead().catch(() => {})
  ElMessage.success('已全部标记为已读')
}

const handleLogout = () => {
  commonApi.logout().catch(() => {})
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
  ElMessage.info('已退出登录')
}

const customDraggingVisible = ref(false)
const value = ref(new Date())
const popUpCalendar = () => { customDraggingVisible.value = true; }

// 菜单标题映射
const pageTitle = computed(() => {
  const titles = {
    '/plan/form': '计划清单', '/plan/track': '审批跟踪',
    '/object/unit': '被审计单位库', '/object/lead': '领导干部库',
    '/statistic': '审计进度可视化',
    '/settings/profile': '个人信息', '/settings/system': '系统设置', '/settings/users': '用户管理'
  }
  return titles[route.path] || '信息管理'
})

const userMenuVisible = ref(false)

onMounted(() => {
  fetchMessages()
  const timer = setInterval(fetchMessages, 60000)
  window.addEventListener('msg-refresh', fetchMessages)
  onUnmounted(() => { clearInterval(timer); window.removeEventListener('msg-refresh', fetchMessages) })
})
</script>

<template>
  <div class="common-layout">
    <!-- 顶部导航 -->
    <header class="top-header">
      <div class="header-left">
        <div class="logo-icon">
          <el-icon :size="22"><OfficeBuilding /></el-icon>
        </div>
        <span class="logo-text">审计信息管理系统</span>
        <el-divider direction="vertical" style="height:20px;margin:0 16px;border-color:rgba(255,255,255,0.2)" />
        <span class="logo-sub">基础数据底座</span>
      </div>
      <div class="header-right">
        <!-- 年度选择 -->
        <span class="year-btn" @click="popUpCalendar">
          📅 {{ value.getFullYear() }}年度
        </span>
        <!-- 预警 -->
        <el-badge :value="warnCount" :hidden="warnCount === 0" :max="99">
          <el-button circle class="header-icon-btn" @click="messageDrawerVisible = true" title="消息中心">
            <el-icon :size="18"><Bell /></el-icon>
          </el-button>
        </el-badge>
        <!-- 用户 -->
        <el-dropdown trigger="click" @command="(cmd) => {
            if (cmd === 'logout') handleLogout()
            else if (cmd === 'profile') $router.push('/settings/profile')
            else if (cmd === 'system') $router.push('/settings/system')
        }">
          <div class="user-area">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="user-name">{{ userName }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><UserFilled /></el-icon>个人信息
              </el-dropdown-item>
              <el-dropdown-item command="system">
                <el-icon><Setting /></el-icon>系统设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout-body">
      <!-- 侧边栏 -->
      <aside class="sidebar">
        <el-scrollbar class="sidebar-scroll">
          <el-menu :default-active="activedMenu" :default-openeds="openedMenu"
            class="side-menu" @close="handleClose" router>
            <el-sub-menu index="/plan">
              <template #title>
                <el-icon><Document /></el-icon>
                <span>审计计划管理</span>
              </template>
              <el-menu-item index="/plan/form">
                <el-icon><List /></el-icon>计划清单
              </el-menu-item>
              <el-menu-item index="/plan/track">
                <el-icon><Check /></el-icon>审批跟踪
              </el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="/object">
              <template #title>
                <el-icon><OfficeBuilding /></el-icon>
                <span>审计对象库</span>
              </template>
              <el-menu-item index="/object/unit">
                <el-icon><Folder /></el-icon>被审计单位库
              </el-menu-item>
              <el-menu-item index="/object/lead">
                <el-icon><Avatar /></el-icon>领导干部库
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/statistic">
              <el-icon><DataAnalysis /></el-icon>
              <span>审计进度可视化</span>
            </el-menu-item>
            <el-menu-item v-if="isSuperAdmin" index="/settings/users">
              <el-icon><UserFilled /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </aside>

      <!-- 主内容 -->
      <main class="main-area">
        <!-- 面包屑 -->
        <div class="breadcrumb-bar">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <!-- 子路由 -->
        <div class="page-content">
          <router-view />
        </div>
      </main>
    </div>

    <!-- 日历弹窗 -->
    <el-dialog v-model="customDraggingVisible" title="选择年度" width="500" draggable>
      <el-calendar v-model="value" />
    </el-dialog>

    <!-- 消息抽屉 -->
    <el-drawer v-model="messageDrawerVisible" size="380px" direction="rtl">
      <template #header>
        <div class="drawer-header-row">
          <span style="font-weight:700;font-size:16px">消息中心</span>
          <el-button type="primary" link size="small" @click="markAllRead" :disabled="unreadCount === 0">
            全部已读
          </el-button>
        </div>
      </template>
      <div v-if="messageList.length === 0" class="empty-msg">
        <el-icon :size="56" color="#dcdfe6"><Bell /></el-icon>
        <p>暂无消息</p>
      </div>
      <div v-for="msg in messageList" :key="msg.messageId"
        class="msg-item" :class="{ unread: !msg.isRead }" @click="markAsRead(msg)">
        <div class="msg-title">
          <el-badge is-dot :hidden="msg.isRead" />
          <span>{{ msg.title }}</span>
          <span class="msg-time">{{ msg.createTime }}</span>
        </div>
        <div class="msg-body">{{ msg.content }}</div>
        <el-tag v-if="msg.messageType === 'ALERT'" type="warning" size="small" effect="plain">预警</el-tag>
        <el-tag v-else-if="msg.messageType === 'APPROVAL'" type="primary" size="small" effect="plain">审批</el-tag>
        <el-tag v-else size="small" type="info" effect="plain">系统</el-tag>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* ========== 全局布局 ========== */
.common-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

/* ========== 顶部导航 ========== */
.top-header {
  height: 52px;
  background: linear-gradient(135deg, #1d1e2c 0%, #2a2d3e 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 100;
  box-shadow: 0 1px 6px rgba(0,0,0,0.15);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  width: 32px; height: 32px;
  background: rgba(255,255,255,0.12);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a8c5ff;
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}
.logo-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.55);
  letter-spacing: 2px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.year-btn {
  font-size: 13px;
  color: rgba(255,255,255,0.75);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s;
  user-select: none;
}
.year-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}
.header-icon-btn {
  width: 34px; height: 34px;
  background: rgba(255,255,255,0.08) !important;
  border: none !important;
  color: rgba(255,255,255,0.7) !important;
  transition: all 0.2s;
}
.header-icon-btn:hover {
  background: rgba(255,255,255,0.16) !important;
  color: #fff !important;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
  margin-left: 4px;
}
.user-area:hover {
  background: rgba(255,255,255,0.08);
}
.user-name {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  white-space: nowrap;
}

/* ========== 侧边栏 + 主内容 ========== */
.layout-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: #fff;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0,0,0,0.04);
  z-index: 10;
}
.sidebar-scroll {
  flex: 1;
  height: 0;
}
.side-menu {
  border-right: none !important;
  padding: 8px 0;
}
.side-menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}
.side-menu :deep(.el-sub-menu__title:hover) {
  background: #f5f7fa;
  color: #409EFF;
}
.side-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  font-size: 13px;
  margin: 2px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}
.side-menu :deep(.el-menu-item:hover) {
  background: #ecf5ff;
  color: #409EFF;
}
.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  color: #409EFF;
  font-weight: 600;
}

/* ========== 主内容区 ========== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.breadcrumb-bar {
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}
.page-content {
  flex: 1;
  overflow: auto;
  background: #f0f2f5;
}

/* ========== 消息抽屉 ========== */
.drawer-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.empty-msg {
  text-align: center;
  padding-top: 80px;
  color: #c0c4cc;
}
.empty-msg p {
  margin-top: 12px;
  font-size: 14px;
}
.msg-item {
  padding: 14px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 2px;
  transition: background 0.15s;
}
.msg-item:hover {
  background: #fafbfc;
}
.msg-item.unread {
  background: #ecf5ff;
}
.msg-item .msg-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.msg-item .msg-time {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
  font-weight: 400;
}
.msg-item .msg-body {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.msg-item .el-tag {
  margin-top: 8px;
}
</style>
