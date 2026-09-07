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

    /** 用户端：单个服务详情（公开接口，服务 + 所属商家名；仅上架且归属商家正常的服务可见） */
    @GetMapping("/item-detail")
    public Result itemDetail(@RequestParam Long itemId) {
        return serviceCategoryService.getItemDetail(itemId);
    }

    /** 用户端：单个服务的评价列表（公开接口，带评价人昵称，最新在前） */
    @GetMapping("/item-comments")
    public Result itemComments(@RequestParam Long itemId) {
        return serviceCategoryService.listItemComments(itemId);
    }

    /** 用户端：全局搜索（公开接口，keyword 匹配分类名/商家名/简介/服务名，返回服务 + 商家两组） */
    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyword) {
        return serviceCategoryService.search(keyword);
    }

    /** 用户端：首页热门推荐（公开接口，销量 Top8 服务 + Top4 商家） */
    @GetMapping("/hot")
    public Result hot() {
        return serviceCategoryService.listHot();
    }
}
