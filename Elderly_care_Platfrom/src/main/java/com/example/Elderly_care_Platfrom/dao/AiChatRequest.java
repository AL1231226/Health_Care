package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>
 * AI 对话请求体（POST /ai/chat）：{ messages: [{role,content}...] }，末条须为 user
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequest {
    /**
     * 会话历史（前端持有回传；后端忽略 system 条目并按最大条数截留）
     */
    private List<AiChatMessage> messages;
}
