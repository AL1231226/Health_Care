package com.example.Elderly_care_Platfrom.dao;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 购物车结算请求体：cartIds 为购物车行 ID 列表（勾选行，逐行各生成一张订单）；
 * 其余整批统一字段与 POST /service-order/create 请求同规
 */
@Data
public class CheckoutRequest {
    /** 购物车行 ID 列表（归属校验：仅可结算本人行） */
    private List<Long> cartIds;
    /** 服务老人（必填，须归属当前家属） */
    private Long elderId;
    /** 服务地址（可空，传了须归属当前用户，否则按未选处理） */
    private Long addressId;
    /** 预约时间（可空，传了须晚于当前时间） */
    private Date serviceTime;
    /** 联系电话（空取登录手机号，否则校验手机号格式） */
    private String contactPhone;
    /** 备注（去空，限 200 字） */
    private String remark;
}
