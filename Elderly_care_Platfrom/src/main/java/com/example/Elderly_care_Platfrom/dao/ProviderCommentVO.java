package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * <p>
 * 商家端「本店评价」展示 VO:评价本体 + 关联订单号 + 被评服务名 + 家属昵称
 * </p>
 * <p>
 * service_comment 只有 order_id/item_id/user_id 外键,需联 service_order /
 * service_item / sys_user 才能展示订单号与服务名/家属昵称,故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderCommentVO {
    /**
     * 评价ID
     */
    private Long commentId;

    /**
     * 关联订单ID(商家反查该单服务安排用)
     */
    private Long orderId;

    /**
     * 订单号(商家对照订单管理定位用)
     */
    private String orderNo;

    /**
     * 家属用户ID
     */
    private Long userId;

    /**
     * 家属昵称(用户已注销时为「匿名用户」)
     */
    private String userName;

    /**
     * 被评服务项目ID
     */
    private Long itemId;

    /**
     * 被评服务项目名称(项目已删时为 null,前端兜底「服务已删除」)
     */
    private String itemName;

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
