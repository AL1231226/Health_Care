package com.example.Elderly_care_Platfrom.dao;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 管理员端「订单管理」VO（全平台只读监督）：订单基础信息 + 商家名 + 服务名 + 下单家属昵称 +
 * 服务老人信息（含健康备注，管理员协调投诉需见老人健康与联系信息）+ 服务地址文本
 * </p>
 * <p>
 * service_order 只有 user_id/provider_id/item_id/elder_id/address_id 外键，需联 sys_user /
 * service_provider / service_item / elder_profile / user_address 才能展示归属与内容，故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminOrderVO {
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
     * 服务商家ID
     */
    private Long providerId;

    /**
     * 服务商家名称（商家注销/不存在时为 null，前端兜底）
     */
    private String providerName;

    /**
     * 服务项目ID
     */
    private Long itemId;

    /**
     * 服务项目名称（项目已删时为 null，前端兜底）
     */
    private String itemName;

    /**
     * 下单家属昵称（家属账号已删时为 null，前端兜底）
     */
    private String familyName;

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
     * 健康备注/护理注意事项（管理员监督协调参考）
     */
    private String healthNote;

    /**
     * 服务地址全文（省市区+详细；未选择时为 null）
     */
    private String addressText;
}
