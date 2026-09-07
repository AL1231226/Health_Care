package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.AiChatMessage;
import com.example.Elderly_care_Platfrom.dao.Result;

import java.util.List;

/**
 * <p>
 * AI 助手服务（家属端首页「小颐」聊天，/ai/chat）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
public interface IAiChatService {
    /**
     * 家属端 AI 对话（Controller 层 @RequireRole(USER) 统一强制）：接收前端传来的会话历史，
     * 后端现查库拼「平台服务/商家目录」进 system 提示词，再调 DeepSeek 补全回复。
     * 返回 Result.ok({reply: 助手回复文本}) 或 fail（未配置/参数非法/上游错误统一文案）
     */
    Result chat(List<AiChatMessage> history);
}
