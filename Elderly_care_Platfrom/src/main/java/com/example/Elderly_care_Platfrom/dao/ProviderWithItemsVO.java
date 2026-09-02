package com.example.Elderly_care_Platfrom.dao;

import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户端「分类下的商家」展示 VO：商家信息 + 该分类下上架的服务项目
 * </p>
 * <p>
 * ServiceProvider 实体只对应 service_provider 表的一行，无法表达"商家 + 其名下服务项目"的一对多嵌套，
 * 故用 VO 承载组合结构返回前端（参照 LoginRequest 等 dao 层请求/返回对象风格）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderWithItemsVO {
    /**
     * 商家ID（卡片 key，后续跳商家详情预留）
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
     * 区（展示"服务区域"）
     */
    private String district;

    /**
     * 详细经营地址（district 为空时前端兜底展示）
     */
    private String address;

    /**
     * 商家评分：该商家所有显示(1)评价的评分均分（无评价为 null）
     */
    private BigDecimal score;

    /**
     * 评价条数（status=1 的评价数）
     */
    private Integer reviewCount;

    /**
     * 该分类下 status=1 的服务项目（直接复用实体）
     */
    private List<ServiceItem> items;
}
