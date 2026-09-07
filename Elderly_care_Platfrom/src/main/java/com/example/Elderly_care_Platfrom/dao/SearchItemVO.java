package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * <p>
 * 用户端公开服务展示 VO（搜索结果 + 首页热门推荐共用）：service_item 上架行 + 所属商家名
 * </p>
 * <p>
 * service_item 只有 provider_id 外键，搜索结果行需联 service_provider 展示「由哪家提供」，
 * 故用 VO 承载组合结构返回前端；评分/销量直读 service_item 接活列（score=评价均值、sales=已完成数）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchItemVO {
    /**
     * 服务项目ID
     */
    private Long itemId;

    /**
     * 服务名称
     */
    private String itemName;

    /**
     * 单价（元）
     */
    private BigDecimal price;

    /**
     * 计价单位（次/份/小时/2小时）
     */
    private String unit;

    /**
     * 服务时长（如 60分钟）
     */
    private String duration;

    /**
     * 服务详情描述（首页热门推荐接口填充,搜索结果行不展示可为 null）
     */
    private String detail;

    /**
     * 平均评分（接活列：无评价时为 null，前端展示「暂无评分」）
     */
    private BigDecimal score;

    /**
     * 销量（接活列：已完成订单数）
     */
    private Integer sales;

    /**
     * 所属商家ID（点击结果跳商家详情用）
     */
    private Long providerId;

    /**
     * 所属商家名称
     */
    private String providerName;
}
