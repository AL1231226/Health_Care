package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.AiChatRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.IAiChatService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * AI 助手 前端控制器（家属端首页小颐聊天窗口）
 * </p>
 * <p>
 * 类级 @RequireRole(USER)：仅家属可聊；管理员/商家打此接口同样「无权限」
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@RestController
@RequestMapping("/ai")
@RequireRole(RoleType.USER)
public class AiChatController {

    @Resource
    private IAiChatService aiChatService;

    /**
     * AI 对话：{messages:[{role,content}...]}，末条须为 user；后端拼平台服务目录进 system 后调 DeepSeek
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody(required = false) AiChatRequest request) {
        return aiChatService.chat(request == null ? null : request.getMessages());
    }
}
