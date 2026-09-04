package com.example.Elderly_care_Platfrom.dao;

import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户端「商家 + 名下上架服务」展示 VO,商家列表卡片与商家详情页两处共用:
 * GET /service-category/providers(分类下商家列表)与 GET /service-category/provider-detail(商家详情)
 * </p>
 * <p>
 * 两个接口结构同构(商家基础信息 + 上架(1)服务项目 + 评分/评价数聚合),detail 是列表的超集:
 * 列表接口只填卡片所需的公共字段(下方标注「仅详情填充」的字段保持 null),
 * 详情接口全量填充。原 ProviderWithItemsVO 已并入本类(两接口唯一装配点在
 * ServiceCategoryServiceImpl.listCategoryProviders / getProviderDetail)
 * </p>
 * <p>
 * ServiceProvider 实体只对应 service_provider 表的一行,无法表达"商家 + 其名下服务项目"的一对多嵌套,
 * 故用 VO 承载组合结构返回前端(参照 LoginRequest 等 dao 层请求/返回对象风格)
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
     * 商家ID(列表卡片 key,跳商家详情预留)
     */
    private Long providerId;

    /**
     * 商家名称
     */
    private String providerName;

    /**
     * logo 路径(为空时前端用首字头像)
     */
    private String logo;

    /**
     * 商家简介
     */
    private String intro;

    /**
     * 区(列表卡片展示"服务区域";详情页为编码)
     */
    private String district;

    /**
     * 详细经营地址(district 为空时前端兜底展示)
     */
    private String address;

    /**
     * 商家评分:该商家全部评价的评分均分(无评价为 null)
     */
    private BigDecimal score;

    /**
     * 评价条数
     */
    private Integer reviewCount;

    /**
     * 上架(1)服务项目(直接复用实体):列表接口为该分类下的,详情接口为该商家全部
     */
    private List<ServiceItem> items;

    /** 以下字段仅详情接口填充,列表接口保持 null(勿在列表填充,公开接口少下发字段) */

    /**
     * 主营分类名(联 service_category 字典,查不到为 null 前端兜底)
     */
    private String categoryName;

    /**
     * 负责人姓名
     */
    private String legalPerson;

    /**
     * 联系电话(完整展示,用户联系商家用);该接口公开无需登录,严禁在商家列表接口填充
     */
    private String phone;

    /**
     * 省(编码)
     */
    private String province;

    /**
     * 市(编码)
     */
    private String city;
}
