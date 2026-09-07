package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.IServiceProviderService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商家表（服务商账号，归属 service_category 分类） 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@RestController
@RequestMapping("/service-provider")
@RequireRole(RoleType.ADMIN)
public class ServiceProviderController {

    @Resource
    private IServiceProviderService serviceProviderService;

    /** 商家列表（管理员）：status 可选 0待审核/1正常/2停用 */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer status) {
        return serviceProviderService.listProviders(status);
    }

    /** 入驻审核（管理员）：pass=true 通过(1)，false 驳回(2) */
    @PutMapping("/review/{providerId}")
    public Result review(@PathVariable Long providerId, @RequestParam Boolean pass) {
        return serviceProviderService.reviewProvider(providerId, pass);
    }

    /** 启用/停用（管理员）：status=1 或 2 */
    @PutMapping("/status/{providerId}")
    public Result toggleProviderStatus(@PathVariable Long providerId, @RequestParam Integer status) {
        return serviceProviderService.toggleProviderStatus(providerId, status);
    }
}
