package com.example.Elderly_care_Platfrom.dao;

import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户端「商家详情」展示 VO：商家基础信息 + 其名下全部上架的服务项目
 * </p>
 * <p>
 * 与 ProviderWithItemsVO 相比多出主营分类名/负责人/电话等完整信息，
 * 供商家详情页「上方基础信息 + 下方服务」两块展示
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderDetailVO {
    /**
     * 商家ID
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
     * 主营分类名（联 service_category 字典）
     */
    private String categoryName;

    /**
     * 负责人姓名
     */
    private String legalPerson;

    /**
     * 联系电话（完整展示，用户联系商家用）
     */
    private String phone;

    /**
     * 省（编码）
     */
    private String province;

    /**
     * 市（编码）
     */
    private String city;

    /**
     * 区（编码）
     */
    private String district;

    /**
     * 详细经营地址
     */
    private String address;

    /**
     * 商家评分：该商家全部评价的评分均分（无评价为 null）
     */
    private BigDecimal score;

    /**
     * 评价条数
     */
    private Integer reviewCount;

    /**
     * 该商家全部 status=1 的服务项目（直接复用实体）
     */
    private List<ServiceItem> items;
}
