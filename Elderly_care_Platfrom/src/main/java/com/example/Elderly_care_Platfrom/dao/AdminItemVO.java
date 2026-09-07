package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 管理员端「服务管理」VO（全平台项目监督）：服务基础信息 + 商家名 + 分类名
 * </p>
 * <p>
 * service_item 只有 provider_id/category_id 外键，需联 service_provider / service_category
 * 才能展示归属，故用 VO 承载组合结构返回前端；评分/销量直读 service_item 接活列（
 * score=评价均值、sales=已完成订单数，由写路径回填），不再实时聚合
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminItemVO {
    /**
     * 服务项目ID
     */
    private Long itemId;

    /**
     * 服务名称
     */
    private String itemName;

    /**
     * 所属商家ID
     */
    private Long providerId;

    /**
     * 所属商家名称（商家注销/不存在时为 null，前端兜底）
     */
    private String providerName;

    /**
     * 服务分类ID
     */
    private Long categoryId;

    /**
     * 服务分类名称（分类被删/不存在时为 null，前端兜底）
     */
    private String categoryName;

    /**
     * 单价（下单/展示快照）
     */
    private BigDecimal price;

    /**
     * 计价单位（如 次/天/餐）
     */
    private String unit;

    /**
     * 平均评分（接活列：service_comment 按项目实时平均回填，无评价时为 null）
     */
    private BigDecimal score;

    /**
     * 销量（接活列：已完成订单数回填）
     */
    private Integer sales;

    /**
     * 状态（0下架 1上架）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;
}
