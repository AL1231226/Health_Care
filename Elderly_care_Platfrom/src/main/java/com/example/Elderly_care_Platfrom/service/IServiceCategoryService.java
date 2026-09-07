package com.example.Elderly_care_Platfrom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCategory;

/**
 * <p>
 * 服务分类字典表 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
public interface IServiceCategoryService extends IService<ServiceCategory> {
    /**
     * 启用中的分类列表（公开接口，无需登录）：按 sort 升序
     */
    Result listEnabledCategories();

    /**
     * 用户端：某分类下正常(1)的商家，附带其该分类下上架(1)的服务项目
     */
    Result listCategoryProviders(Long categoryId);

    /**
     * 用户端：商家详情（基础信息 + 名下全部上架服务），仅正常(1)商家可见
     */
    Result getProviderDetail(Long providerId);

    /**
     * 用户端：商家评价列表（带评价人昵称/被评服务名，最新在前）
     */
    Result listProviderComments(Long providerId);

    /**
     * 用户端：单个服务详情（服务 + 所属商家名），仅上架(1)且归属商家正常(1)的服务可见
     */
    Result getItemDetail(Long itemId);

    /**
     * 用户端：单个服务的评价列表（带评价人昵称，最新在前）
     */
    Result listItemComments(Long itemId);

    /**
     * 用户端：全局搜索（公开接口，无需登录）——keyword 命中分类名/商家名/商家简介/服务名，
     * 返回两组：命中服务（上架且归属商家正常）+ 命中商家（正常商家，带名下全部上架服务与评分聚合）
     */
    Result search(String keyword);

    /**
     * 用户端：首页热门推荐（公开接口，无需登录）——销量 Top8 上架服务（含商家名/详情）
     * + 销量 Top4 正常商家（按名下上架服务销量总和，含评分/评价数聚合与主营分类名）
     */
    Result listHot();
}
