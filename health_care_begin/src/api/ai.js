// AI 助手接口（后端 AiChatController，家属端首页「小颐」聊天；token 由 request.js 自动携带）
import request from '@/utils/request'

// AI 对话：POST /ai/chat，{ messages: [{role:'user'|'assistant', content}] } 会话历史由前端持有回传，
// 后端拼「平台服务/商家目录」进 system 后调 DeepSeek，返回 { reply } 助手回复文本
export const chatAi = (messages) => {
  return request.post('/ai/chat', { messages })
}
