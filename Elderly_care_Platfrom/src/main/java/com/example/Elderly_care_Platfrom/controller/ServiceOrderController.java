package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;
import com.example.Elderly_care_Platfrom.service.IServiceOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 服务订单表（用户预约商家服务项目；service_comment.order_id 外键关联本表） 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@RestController
@RequestMapping("/service-order")
public class ServiceOrderController {

    @Resource
    private IServiceOrderService serviceOrderService;

    /** 用户端：提交订单（归属取 token 家属；商家/价格由服务端从服务项目快照，状态固定待接单） */
    @PostMapping("/create")
    public Result create(@RequestBody ServiceOrder serviceOrder) {
        return serviceOrderService.createOrder(serviceOrder);
    }

    /** 商家端：本店订单列表（归属取 token 商家），status 可选 0待接单 1服务中 2已完成 3已取消 */
    @GetMapping("/merchant/list")
    public Result merchantList(@RequestParam(required = false) Integer status) {
        return serviceOrderService.listMerchantOrders(status);
    }

    /** 用户端：本人订单列表（归属取 token 家属），status 可选 0待接单 1服务中 2已完成 3已取消 */
    @GetMapping("/user/list")
    public Result userList(@RequestParam(required = false) Integer status) {
        return serviceOrderService.listUserOrders(status);
    }

    /** 用户端：取消本人待接单订单(0→3，仅限本人订单) */
    @PutMapping("/cancel/{orderId}")
    public Result cancel(@PathVariable Long orderId) {
        return serviceOrderService.cancelOrder(orderId);
    }

    /** 商家端：接单(0→1) / 完成服务(1→2)，仅限本店订单 */
    @PutMapping("/status/{orderId}")
    public Result updateStatus(@PathVariable Long orderId, @RequestParam Integer status) {
        return serviceOrderService.updateOrderStatus(orderId, status);
    }

    /** 管理员端：全平台订单列表（只读监督，仅管理员 role=2 可调，其余角色「无权限」） */
    @GetMapping("/admin/list")
    public Result adminList() {
        return serviceOrderService.listAdminOrders();
    }
}
