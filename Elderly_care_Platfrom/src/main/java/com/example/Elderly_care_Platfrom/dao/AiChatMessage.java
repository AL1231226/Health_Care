package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * AI 助手对话消息（POST /ai/chat 请求体）：role = system/user/assistant，content 为消息文本
 * </p>
 * <p>
 * 与 DeepSeek chat/completions 的 messages 元素同构，后端组装系统目录后原样透传给模型；
 * 历史会话由前端持有、随请求回传，后端只保留最近若干条
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessage {
    /**
     * 消息角色：user / assistant（system 由后端按平台服务目录生成，不信任前端传入）
     */
    private String role;

    /**
     * 消息文本
     */
    private String content;
}
