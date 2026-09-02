package com.example.Elderly_care_Platfrom.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 服务评价表（用户对商家/服务项目的评价，商家评分由本表 score 聚合而来）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Getter
@Setter
@ToString
@TableName("service_comment")
public class ServiceComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价ID
     */
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    /**
     * 被评服务项目 (FK -> service_item.item_id)
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 被评商家（冗余，FK -> service_provider.provider_id）
     */
    @TableField("provider_id")
    private Long providerId;

    /**
     * 关联订单 (FK -> 订单表，订单表未建故无外键，可空)
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 评价用户 (FK -> sys_user.user_id)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 评分 1-5
     */
    @TableField("score")
    private Integer score;

    /**
     * 评价内容
     */
    @TableField("content")
    private String content;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
