<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const ws = ref(null)
const messageCount = ref(0)

// WebSocket 消息推送连接（联调时取消注释）
// const connectWebSocket = () => {
//   const token = localStorage.getItem('token')
//   if (!token) return
//   const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
//   ws.value = new WebSocket(`${protocol}//${location.host}/api/audit/v1/ws?token=${token}`)
//   ws.value.onmessage = (event) => {
//     const msg = JSON.parse(event.data)
//     messageCount.value++
//     ElMessage.warning({ message: msg.title || '收到新消息', duration: 4000 })
//   }
//   ws.value.onclose = () => { setTimeout(connectWebSocket, 5000) }
// }

onMounted(() => {
  // connectWebSocket()
})

onUnmounted(() => {
  if (ws.value) ws.value.close()
})
</script>

<template>
  <router-view />
</template>

<style scoped></style>
