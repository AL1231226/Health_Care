package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * <p>
 * 用户端首页「热门商家」VO（GET /service-category/hot 销量 Top4 商家）：商家基础信息 + 销量/评分聚合
 * </p>
 * <p>
 * 商家销量 = 名下全部上架(1)服务 service_item.sales 列之和（sales 为接活列，
 * 写路径按已完成订单数同事务回填）；评分/评价数仍按 service_comment 实时聚合
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotProviderVO {
    /**
     * 商家ID（点击跳商家详情）
     */
    private Long providerId;

    /**
     * 商家名称
     */
    private String providerName;

    /**
     * logo 路径（为空时前端用首字头像）
     */
    private String logo;

    /**
     * 商家简介
     */
    private String intro;

    /**
     * 主营分类名（联 service_category 字典，查不到为 null 前端兜底）
     */
    private String categoryName;

    /**
     * 区（卡片展示「服务区域」）
     */
    private String district;

    /**
     * 详细经营地址（district 为空时前端兜底展示）
     */
    private String address;

    /**
     * 商家评分：全部评价均分（无评价为 null）
     */
    private BigDecimal score;

    /**
     * 评价条数
     */
    private Integer reviewCount;

    /**
     * 商家销量：名下上架服务销量之和（热门排序依据）
     */
    private Integer totalSales;
}
