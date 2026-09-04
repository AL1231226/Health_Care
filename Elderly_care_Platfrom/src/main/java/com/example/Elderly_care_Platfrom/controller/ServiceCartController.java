package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.CheckoutRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCart;
import com.example.Elderly_care_Platfrom.service.IServiceCartService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 购物车表（家属端：加购/列表/改量/删行/勾选结算，结算按商家×项目拆单） 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
@RestController
@RequestMapping("/service-cart")
public class ServiceCartController {

    @Resource
    private IServiceCartService serviceCartService;

    /** 加购：请求 {itemId, quantity=本次新增份数 1~99}，同 (user,item) 数量累加（上限 99 拒） */
    @PostMapping("/add")
    public Result add(@RequestBody ServiceCart cart) {
        return serviceCartService.add(cart);
    }

    /** 本人购物车全部行（归属取 token 家属，加购时间升序，含下架标记） */
    @GetMapping("/list")
    public Result list() {
        return serviceCartService.listCart();
    }

    /** 改数量：quantity 1~99（归属校验） */
    @PutMapping("/quantity/{cartId}")
    public Result quantity(@PathVariable Long cartId, @RequestParam Integer quantity) {
        return serviceCartService.updateQuantity(cartId, quantity);
    }

    /** 删行（归属校验） */
    @DeleteMapping("/remove/{cartId}")
    public Result remove(@PathVariable Long cartId) {
        return serviceCartService.remove(cartId);
    }

    /** 勾选结算：cartIds 逐行各生成一张订单，成功后清除对应购物车行（整体事务，任一失败全回滚） */
    @PostMapping("/checkout")
    public Result checkout(@RequestBody CheckoutRequest req) {
        return serviceCartService.checkout(req);
    }
}
