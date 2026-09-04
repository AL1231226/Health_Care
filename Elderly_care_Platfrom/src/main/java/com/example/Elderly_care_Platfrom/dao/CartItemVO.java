package com.example.Elderly_care_Platfrom.dao;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 购物车行展示 VO（item/provider 批量联查拼装；itemStatus=0 下架，null 视为服务已删，前端标灰禁选）
 */
@Data
public class CartItemVO {
    private Long cartId;
    private Long itemId;
    /** 服务名称（item 缺失时为 null，前端兜底文案「服务已删除」） */
    private String itemName;
    /** 单价（元，下单时服务端快照为准，此处仅供展示） */
    private BigDecimal price;
    /** 计价单位（次/份/小时/2小时） */
    private String unit;
    /** 份数 1~99 */
    private Integer quantity;
    /** 项目状态 0下架 1上架；item 缺失为 null */
    private Integer itemStatus;
    private Long providerId;
    /** 商家名称（item 缺失时为 null） */
    private String providerName;
    private Date createTime;
}
