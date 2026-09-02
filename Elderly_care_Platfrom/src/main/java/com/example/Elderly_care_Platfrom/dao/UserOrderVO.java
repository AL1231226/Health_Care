package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 用户端「我的订单」VO：订单基础信息 + 商家名 + 服务项目名 + 服务老人姓名 + 服务地址文本
 * </p>
 * <p>
 * service_order 只有 item_id/elder_id/address_id/provider_id 外键，需联 service_item / service_provider /
 * elder_profile / user_address 才能展示商家名、服务名、服务老人与服务地址，故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderVO {
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 状态（0待接单 1服务中 2已完成 3已取消）
     */
    private Integer orderStatus;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单价快照
     */
    private BigDecimal unitPrice;

    /**
     * 总价
     */
    private BigDecimal totalPrice;

    /**
     * 预约服务时间（可空）
     */
    private Date serviceTime;

    /**
     * 下单时间
     */
    private Date createTime;

    /**
     * 下单预留联系电话
     */
    private String contactPhone;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 服务项目名称（项目已删时为 null，前端兜底）
     */
    private String itemName;

    /**
     * 服务商家名称（商家停用/不存在时为 null，前端兜底）
     */
    private String providerName;

    /**
     * 服务老人ID
     */
    private Long elderId;

    /**
     * 老人姓名（档案已删时为 null，前端兜底）
     */
    private String elderName;

    /**
     * 服务地址全文（省市区+详细；未选择时为 null）
     */
    private String addressText;
}
