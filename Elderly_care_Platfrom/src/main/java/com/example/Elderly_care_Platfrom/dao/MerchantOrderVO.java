package com.example.Elderly_care_Platfrom.dao;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 商家端「订单管理」VO：订单基础信息 + 服务名 + 服务老人信息（健康备注等，接单/服务前必需）+ 服务地址文本
 * </p>
 * <p>
 * service_order 只有 item_id/elder_id/address_id 外键，需联 service_item / elder_profile / user_address
 * 才能展示服务名、老人健康信息与服务地址，故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantOrderVO {
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
     * 服务老人ID
     */
    private Long elderId;

    /**
     * 老人姓名（档案已删时为 null）
     */
    private String elderName;

    /**
     * 老人性别（1男 0女）
     */
    private Integer gender;

    /**
     * 老人生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthDate;

    /**
     * 老人联系电话（可空）
     */
    private String elderPhone;

    /**
     * 健康备注/护理注意事项（商家接单/服务前必看）
     */
    private String healthNote;

    /**
     * 服务地址全文（省市区+详细；未选择时为 null）
     */
    private String addressText;
}
