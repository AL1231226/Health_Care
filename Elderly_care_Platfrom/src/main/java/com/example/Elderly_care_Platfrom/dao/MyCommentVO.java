package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * <p>
 * 家属端「我的评价」展示 VO:评价本体 + 关联订单号 + 服务项目名 + 商家名
 * </p>
 * <p>
 * service_comment 只有 order_id/item_id/provider_id 外键,需联 service_order /
 * service_item / service_provider 才能展示订单号与服务名/商家名,故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyCommentVO {
    /**
     * 评价ID
     */
    private Long commentId;

    /**
     * 关联订单ID(前端跳订单列表/定位用)
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 被评服务项目名称(项目已删时为 null,前端兜底)
     */
    private String itemName;

    /**
     * 被评商家名称(商家不存在时为 null,前端兜底)
     */
    private String providerName;

    /**
     * 评分 1-5
     */
    private Integer score;

    /**
     * 评价内容(选填,空为 null)
     */
    private String content;

    /**
     * 评价时间
     */
    private Date createTime;
}
