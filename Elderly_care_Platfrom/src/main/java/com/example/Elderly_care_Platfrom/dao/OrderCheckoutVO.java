package com.example.Elderly_care_Platfrom.dao;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车结算结果 VO：勾选行逐张生成的订单（成功页逐单展示）
 */
@Data
public class OrderCheckoutVO {
    /** 订单号（yyyyMMddHHmmss+4位随机） */
    private String orderNo;
    /** 服务名称 */
    private String itemName;
    /** 份数 */
    private Integer quantity;
    /** 实付总价（单价×数量快照） */
    private BigDecimal totalPrice;
}
