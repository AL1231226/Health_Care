package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.IServiceCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 服务分类字典表 前端控制器
 * </p>
 * <p>
 * 公开接口（字典数据，未加入 JWT 拦截路径），首页分类从该接口拉取
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@RestController
@RequestMapping("/service-category")
public class ServiceCategoryController {

    @Resource
    private IServiceCategoryService serviceCategoryService;

    @GetMapping("/list")
    public Result list() {
        return serviceCategoryService.listEnabledCategories();
    }

    /** 用户端：分类下的商家列表（公开接口，含各商家该分类下的服务项目） */
    @GetMapping("/providers")
    public Result listProviders(@RequestParam Long categoryId) {
        return serviceCategoryService.listCategoryProviders(categoryId);
    }

    /** 用户端：商家详情（公开接口，基础信息 + 名下全部上架服务，仅正常商家可见） */
    @GetMapping("/provider-detail")
    public Result providerDetail(@RequestParam Long providerId) {
        return serviceCategoryService.getProviderDetail(providerId);
    }

    /** 用户端：商家评价列表（公开接口，带评价人昵称/被评服务名，最新在前） */
    @GetMapping("/provider-comments")
    public Result providerComments(@RequestParam Long providerId) {
        return serviceCategoryService.listProviderComments(providerId);
    }
}
