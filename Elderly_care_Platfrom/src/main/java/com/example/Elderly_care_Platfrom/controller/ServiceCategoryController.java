package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.IServiceCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
