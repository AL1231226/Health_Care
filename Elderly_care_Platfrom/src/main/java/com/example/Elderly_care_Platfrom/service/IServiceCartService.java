package com.example.Elderly_care_Platfrom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Elderly_care_Platfrom.dao.CheckoutRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCart;

/**
 * <p>
 * 购物车表 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
public interface IServiceCartService extends IService<ServiceCart> {

    /**
     * 加购：item 须存在且上架；同 (user,item) 无行插新、有行数量累加（上限 99 拒）
     *
     * @param cart 前端传入 itemId/quantity（本次新增份数 1~99），归属取 token 家属
     */
    Result add(ServiceCart cart);

    /**
     * 本人购物车全部行（归属取 token 家属，加购时间升序）：item/provider 批量联查拼装 VO
     */
    Result listCart();

    /**
     * 改数量（1~99，归属校验）
     *
     * @param cartId   购物车行ID
     * @param quantity 新份数
     */
    Result updateQuantity(Long cartId, Integer quantity);

    /**
     * 删行（归属校验）
     */
    Result remove(Long cartId);

    /**
     * 结算：勾选行逐行校验/生成订单（复用下单 createOrder 同源校验与快照），
     * 事务内全部落库成功后按 cartIds 清除购物车行；任一失效服务或下单失败整批拒绝并整体回滚
     *
     * @param req cartIds 勾选行 + 整批统一字段（老人/地址/电话/预约时间/备注）
     */
    Result checkout(CheckoutRequest req);
}
