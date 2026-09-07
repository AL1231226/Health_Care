package com.example.Elderly_care_Platfrom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;

/**
 * <p>
 * 服务订单表 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
public interface IServiceOrderService extends IService<ServiceOrder> {

    /**
     * 用户端提交订单：归属取 token 家属；商家/价格由服务端从服务项目快照，状态固定待接单
     *
     * @param order 前端传入 elderId/itemId/quantity/serviceTime/addressId/contactPhone/remark
     */
    Result createOrder(ServiceOrder order);

    /**
     * 商家端本店订单列表（归属取 token 商家）：联查服务名/老人信息/地址拼装 VO
     *
     * @param status 可选 0待接单 1服务中 2已完成 3已取消，不传查全部
     */
    Result listMerchantOrders(Integer status);

    /**
     * 商家端订单状态流转：0待接单→1服务中(接单)、1服务中→2已完成(完成服务)
     *
     * @param status 目标状态 1 或 2
     */
    Result updateOrderStatus(Long orderId, Integer status);

    /**
     * 用户端本人订单列表（归属取 token 家属）：联查商家名/服务名/老人姓名/地址拼装 VO
     *
     * @param status 可选 0待接单 1服务中 2已完成 3已取消，不传查全部
     */
    Result listUserOrders(Integer status);

    /**
     * 用户端取消本人待接单订单（仅 0待接单 → 3已取消，商家接单后不可取消）
     *
     * @param orderId 订单ID
     */
    Result cancelOrder(Long orderId);

    /**
     * 管理员端全平台订单列表（只读监督）：仅管理员可达（Controller 层 @RequireRole(ADMIN) 由 RoleInterceptor 统一强制，越权返「无权限」）；
     * 全量订单 create_time 倒序，联查商家名/服务名/下单家属昵称/老人信息/地址拼装 VO，前端做状态筛选与关键词过滤
     */
    Result listAdminOrders();
}
