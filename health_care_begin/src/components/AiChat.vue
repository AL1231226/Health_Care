<script setup>
// ============ 家属端 AI 助手聊天浮窗（小颐）：首页小精灵点击展开 ============
// 会话历史由本组件内存持有（关闭即清），发送时整段回传后端（最近 20 条由后端截留）；
// 后端每次请求现拼平台服务/商家目录进 system 提示词，模型知道平台服务与销量/价格
import { nextTick, ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { chatAi } from '@/api/ai.js'

const emit = defineEmits(['close'])

// 窗口尺寸：可拖右下角手柄实时缩放（窗口锚定右下，变大向左上扩展；受视口与最小尺寸约束）
const size = ref({ w: 480, h: 660 })
let resizeState = null
const onResizeDown = (e) => {
  e.preventDefault()
  resizeState = { startX: e.clientX, startY: e.clientY, startW: size.value.w, startH: size.value.h }
  window.addEventListener('pointermove', onResizeMove)
  window.addEventListener('pointerup', onResizeUp)
}
const onResizeMove = (e) => {
  if (!resizeState) return
  const maxW = window.innerWidth - 40 // 右侧固定 16 + 视口左边留 24
  const maxH = window.innerHeight - 178 // 底缘距视口底 150 + 留 28
  size.value.w = Math.min(Math.max(resizeState.startW + e.clientX - resizeState.startX, 340), maxW)
  size.value.h = Math.min(Math.max(resizeState.startH + e.clientY - resizeState.startY, 420), maxH)
}
const onResizeUp = () => {
  resizeState = null
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', onResizeUp)
}

// 开场白纯前端展示，不占一次真实请求
const welcome = '你好呀，我是小颐 👋 想知道平台有哪些服务、哪家卖得好、哪个价格低，或者想让我帮你挑服务，直接问我就行～'
const messages = ref([{ role: 'assistant', content: welcome }])
const draft = ref('')
const loading = ref(false)
const listRef = ref(null)

// 新消息（含「正在思考」占位）追加后滚到底
watch([messages, loading], () => {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
})

const send = async () => {
  const text = draft.value.trim()
  if (!text || loading.value) return
  draft.value = ''
  messages.value.push({ role: 'user', content: text })
  loading.value = true
  try {
    const result = await chatAi(messages.value)
    if (result.success) {
      messages.value.push({ role: 'assistant', content: result.data?.reply || '收到啦～' })
    } else {
      // 后端未配置/上游不可用时也以气泡回显，不弹全局打扰
      messages.value.push({ role: 'assistant', content: `小颐暂时开不了小差：${result.errorMsg || '服务暂不可用'}，稍后再试试吧～` })
    }
  } catch (err) {
    messages.value.push({ role: 'assistant', content: '连接出了点问题，刷新后再和我聊聊吧～' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ai-chat" :style="{ width: size.w + 'px', height: size.h + 'px' }">
    <div class="chat-head">
      <span class="chat-logo">颐</span>
      <div class="chat-title">
        <b>小颐 · AI 助手</b>
        <i>知服务 · 比价格 · 看销量</i>
      </div>
      <el-icon class="close-btn" :size="16" @click="emit('close')"><Close /></el-icon>
    </div>

    <div ref="listRef" class="chat-list">
      <div v-for="(m, idx) in messages" :key="idx" class="msg-row" :class="m.role">
        <span v-if="m.role === 'assistant'" class="msg-logo">颐</span>
        <div class="bubble">{{ m.content }}</div>
      </div>
      <div v-if="loading" class="msg-row assistant">
        <span class="msg-logo">颐</span>
        <div class="bubble typing">
          <i></i><i></i><i></i>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        resize="none"
        maxlength="500"
        placeholder="问问小颐：平台有哪些服务？哪个卖得好？"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" round class="send-btn" :loading="loading" @click="send">发送</el-button>
    </div>

    <!-- 右下角缩放手柄：按住拖动放大/缩小聊天窗 -->
    <div class="resize-handle" title="拖动调整窗口大小" @pointerdown="onResizeDown"></div>
  </div>
</template>

<style scoped>
.ai-chat {
  position: fixed;
  right: 16px;
  bottom: 150px;
  z-index: 300;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(45, 42, 38, 0.22);
  overflow: hidden;
  animation: chat-in 0.25s ease;
}
/* 右下角缩放手柄（双线角标，拖动时整体 resize 由 JS 接管） */
.resize-handle {
  position: absolute;
  right: 3px;
  bottom: 3px;
  z-index: 5;
  width: 22px;
  height: 22px;
  cursor: nwse-resize;
  touch-action: none;
  user-select: none;
}
.resize-handle::before {
  content: '';
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 11px;
  height: 11px;
  border-right: 2px solid #c9c0b4;
  border-bottom: 2px solid #c9c0b4;
  border-radius: 0 0 3px 0;
}
.resize-handle::after {
  content: '';
  position: absolute;
  right: 9px;
  bottom: 9px;
  width: 6px;
  height: 6px;
  border-right: 2px solid #ece4d8;
  border-bottom: 2px solid #ece4d8;
  border-radius: 0 0 2px 0;
}
@keyframes chat-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============ 头部 ============ */
.chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
}
.chat-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 18px;
  font-weight: 600;
}
.chat-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.chat-title b {
  font-size: 16px;
}
.chat-title i {
  font-size: 12px;
  font-style: normal;
  opacity: 0.88;
}
.close-btn {
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: background 0.2s;
}
.close-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* ============ 消息区 ============ */
.chat-list {
  flex: 1;
  padding: 18px;
  overflow-y: auto;
  background: #faf7f4;
}
.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
}
.bubble {
  max-width: 78%;
  padding: 11px 15px;
  border-radius: 14px;
  border-top-left-radius: 4px;
  background: #fff;
  color: #3a362f;
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 2px 6px rgba(45, 42, 38, 0.06);
}
.msg-row.user .bubble {
  border-radius: 14px;
  border-top-right-radius: 4px;
  background: linear-gradient(135deg, #ff9c57, #ff7a45);
  color: #fff;
}
.bubble.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px 16px;
}
.bubble.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff9c57;
  animation: typing 1.2s infinite;
}
.bubble.typing i:nth-child(2) {
  animation-delay: 0.2s;
}
.bubble.typing i:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* ============ 输入区 ============ */
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid #f2ede6;
  background: #fff;
}
.chat-input :deep(.el-textarea__inner) {
  font-size: 15px;
  box-shadow: none;
  background: #f6f2ec;
  border: 1px solid #f6f2ec;
  padding: 9px 13px;
}
.send-btn {
  flex-shrink: 0;
  border: none;
}
</style>
