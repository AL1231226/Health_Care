package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.AiChatMessage;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCategory;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.service.IAiChatService;
import com.example.Elderly_care_Platfrom.service.IServiceCategoryService;
import com.example.Elderly_care_Platfrom.service.IServiceCommentService;
import com.example.Elderly_care_Platfrom.service.IServiceItemService;
import com.example.Elderly_care_Platfrom.service.IServiceProviderService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * AI 助手服务实现：家属端对话 = 「平台服务/商家目录实时快照」拼 system 提示词 + DeepSeek 补全
 * </p>
 * <p>
 * 目录每次请求现查库（上架服务 + 正常商家），销量/评分列即最新值，模型据此回答
 * 「平台有哪些服务/哪家卖得好/哪个便宜/帮我推荐」，无需 RAG/工具调用编排；
 * 无状态：会话历史由前端持有回传，本服务不存会话
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    @Resource
    private IServiceCategoryService serviceCategoryService;

    @Resource
    private IServiceProviderService serviceProviderService;

    @Resource
    private IServiceItemService serviceItemService;

    @Resource
    private IServiceCommentService serviceCommentService;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    /** hutool HttpRequest：每请求独立、线程安全；连接/读取总超时 60s */
    private static final int HTTP_TIMEOUT_MS = 60_000;

    /** 携带给模型的最大历史消息条数（防无限回传撑爆上下文） */
    private static final int MAX_HISTORY = 20;

    /** 用户侧消息总字符上限（防灌超长文本） */
    private static final int MAX_INPUT_CHARS = 8000;

    @Override
    public Result chat(List<AiChatMessage> history) {
        // ===== 入参校验：末条须为 user，角色合法，总量受限 =====
        if (history == null || history.isEmpty() || history.get(history.size() - 1) == null) {
            return Result.fail("消息不能为空");
        }
        List<AiChatMessage> messages = history.stream()
                .filter(Objects::nonNull)
                .filter(m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole()))
                .filter(m -> m.getContent() != null && !m.getContent().trim().isEmpty())
                .collect(Collectors.toList());
        if (messages.isEmpty() || !"user".equals(messages.get(messages.size() - 1).getRole())) {
            return Result.fail("消息内容不能为空");
        }
        int totalChars = messages.stream().mapToInt(m -> m.getContent().length()).sum();
        if (totalChars > MAX_INPUT_CHARS) {
            return Result.fail("消息过长，请精简后再发送");
        }
        if (messages.size() > MAX_HISTORY) {
            messages = messages.subList(messages.size() - MAX_HISTORY, messages.size());
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Result.fail("AI 助手暂未配置，请联系管理员");
        }

        // ===== 组装请求：system(平台目录实时快照) + 历史会话，结构同 DeepSeek chat/completions =====
        JSONArray payloadMessages = new JSONArray();
        JSONObject system = new JSONObject();
        system.set("role", "system");
        system.set("content", buildCatalogPrompt());
        payloadMessages.add(system);
        for (AiChatMessage m : messages) {
            JSONObject msg = new JSONObject();
            msg.set("role", m.getRole());
            msg.set("content", m.getContent());
            payloadMessages.add(msg);
        }
        JSONObject payload = new JSONObject();
        payload.set("model", model);
        payload.set("messages", payloadMessages);
        payload.set("temperature", 0.6);
        payload.set("max_tokens", 800);
        try {
            HttpResponse response = HttpRequest.post(baseUrl + "/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(HTTP_TIMEOUT_MS)
                    .body(payload.toString())
                    .execute();
            if (response.getStatus() != 200) {
                // 400/401/429/5xx：统一友好文案，细节留服务端日志便于排查 key/额度问题
                log.error("DeepSeek 返回异常 code={} body={}", response.getStatus(),
                        truncate(response.body(), 500));
                return Result.fail("AI 服务暂时不可用，请稍后再试");
            }
            JSONObject node = JSONUtil.parseObj(response.body());
            JSONObject firstChoice = node.getJSONArray("choices").getJSONObject(0);
            String reply = firstChoice.getJSONObject("message").getStr("content");
            if (reply == null || reply.trim().isEmpty()) {
                reply = "收到啦～小颐刚才走神了，你再说一遍好吗？";
            }
            return Result.ok(Collections.singletonMap("reply", reply.trim()));
        } catch (Exception e) {
            log.error("AI 对话调用失败", e);
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }

    /** 现查库拼系统提示词：角色设定 + 服务/商家目录（真实数据，按已售数降序），让模型能答平台选品问题 */
    private String buildCatalogPrompt() {
        // 正常(1)商家 + 名下上架(1)服务一次批量取出（items 已按销量降序、同销量 id 升序）
        QueryWrapper<ServiceProvider> providerWrapper = new QueryWrapper<>();
        providerWrapper.eq("status", 1);
        List<ServiceProvider> providers = serviceProviderService.list(providerWrapper);
        List<Long> providerIds = providers.stream()
                .map(ServiceProvider::getProviderId).collect(Collectors.toList());
        List<ServiceItem> allItems = Collections.emptyList();
        Map<Long, Integer> providerSales = new HashMap<>();
        if (!providerIds.isEmpty()) {
            QueryWrapper<ServiceItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.in("provider_id", providerIds).eq("status", 1)
                    .orderByDesc("sales").orderByAsc("item_id");
            allItems = serviceItemService.list(itemWrapper);
            for (ServiceItem item : allItems) {
                providerSales.merge(item.getProviderId(),
                        item.getSales() == null ? 0 : item.getSales(), Integer::sum);
            }
        }
        // 商家评分/评价数实时聚合（与全站展示口径一致：service_comment 均分）
        Map<Long, List<ServiceComment>> commentsByProvider = new HashMap<>();
        if (!providerIds.isEmpty()) {
            QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
            commentWrapper.in("provider_id", providerIds);
            commentsByProvider = serviceCommentService.list(commentWrapper).stream()
                    .collect(Collectors.groupingBy(ServiceComment::getProviderId));
        }
        // 分类名字典
        List<Long> categoryIds = providers.stream().map(ServiceProvider::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : serviceCategoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ServiceCategory::getCategoryId, ServiceCategory::getCategoryName));
        Map<Long, String> ownerNameMap = providers.stream()
                .collect(Collectors.toMap(ServiceProvider::getProviderId, ServiceProvider::getProviderName));

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是颐养平台的 AI 助手「小颐」，面向为家里老人预约居家服务（助餐/助洁/助浴/助医/康复护理）的家属用户。")
                .append("语气亲切、简洁、中文回答，可用少量 emoji 但不要刷屏。回答只基于下方平台实时目录与通用生活常识，")
                .append("目录里没有的服务不要编造，可说“平台上暂时没有这项服务”或推荐相近服务。\n");
        prompt.append("回答选品类问题（有哪些服务/哪个卖得好/哪个价格低/帮我推荐）时：先看目录再答，给出服务名、商家、价格、已售数方便对比；")
                .append("“已售”即平台该服务已完成订单数，越高越受欢迎；“价格低”看单价且注意单位不同（次/份/小时）不要直接比大小；")
                .append("同一服务有不同商家提供时也按数据对比。涉及疾病诊断治疗请提醒用户咨询专业医生，你只做生活服务推荐。")
                .append("用户闲聊家常时正常回应，并可以自然地把话题带回平台服务。\n");

        prompt.append("【平台服务目录】(格式：服务名｜分类｜商家｜价格/单位｜时长｜已售｜评分｜简介，按已售数从高到低)\n");
        for (ServiceItem item : allItems) {
            prompt.append("- ").append(item.getItemName()).append("｜")
                    .append(categoryNameMap.getOrDefault(item.getCategoryId(), "未分类")).append("｜")
                    .append(ownerNameMap.getOrDefault(item.getProviderId(), "未知商家")).append("｜")
                    .append("¥").append(item.getPrice()).append("/").append(item.getUnit() == null ? "" : item.getUnit()).append("｜")
                    .append(item.getDuration() == null || "无".equals(item.getDuration()) ? "时长面议" : item.getDuration()).append("｜")
                    .append("已售").append(item.getSales() == null ? 0 : item.getSales()).append("｜")
                    .append(item.getScore() == null ? "暂无评分" : item.getScore()).append("｜")
                    .append(truncate(item.getDetail(), 60)).append("\n");
        }
        prompt.append("【平台商家目录】(格式：商家名｜主营分类｜区域｜评分·评价数｜名下服务已售合计｜简介)\n");
        for (ServiceProvider provider : providers) {
            List<ServiceComment> comments = commentsByProvider.getOrDefault(provider.getProviderId(),
                    Collections.emptyList());
            String scoreText = "暂无评分";
            if (!comments.isEmpty()) {
                double avg = comments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
                scoreText = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).toPlainString()
                        + "·" + comments.size() + "条评价";
            }
            prompt.append("- ").append(provider.getProviderName()).append("｜")
                    .append(categoryNameMap.getOrDefault(provider.getCategoryId(), "未分类")).append("｜")
                    .append(provider.getDistrict() == null ? "区域详询" : provider.getDistrict()).append("｜")
                    .append(scoreText).append("｜")
                    .append("合计已售").append(providerSales.getOrDefault(provider.getProviderId(), 0)).append("｜")
                    .append(truncate(provider.getIntro(), 50)).append("\n");
        }
        return prompt.toString();
    }

    /** 目录长文本截断（换行符归一空格），控制 system 提示词体积 */
    private String truncate(String text, int max) {
        if (text == null) {
            return "暂无简介";
        }
        String flat = text.replaceAll("\\s+", " ");
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }
}
