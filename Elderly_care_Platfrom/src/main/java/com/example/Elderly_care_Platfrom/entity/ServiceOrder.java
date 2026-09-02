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
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 服务订单表（用户预约商家服务项目；service_comment.order_id 外键关联本表）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Getter
@Setter
@ToString
@TableName("service_order")
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单号（业务编号，生成规则后续定）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 下单用户 (FK -> sys_user.id)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 服务老人 (FK -> elder_profile.elder_id，下单为哪位老人预约)
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 服务商家 (FK -> service_provider.provider_id)
     */
    @TableField("provider_id")
    private Long providerId;

    /**
     * 服务项目 (FK -> service_item.item_id)
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 单价快照（下单时项目价格）
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 总价 = 单价 * 数量
     */
    @TableField("total_price")
    private BigDecimal totalPrice;

    /**
     * 状态（0待接单 1服务中 2已完成 3已取消）
     */
    @TableField("order_status")
    private Byte orderStatus;

    /**
     * 预约服务时间（可空）
     */
    @TableField("service_time")
    private Date serviceTime;

    /**
     * 服务地址 (FK -> user_address.addr_id，可空)
     */
    @TableField("address_id")
    private Long addressId;

    /**
     * 下单预留联系电话（可空，默认取用户手机号）
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 订单备注
     */
    @TableField("remark")
    private String remark;

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
