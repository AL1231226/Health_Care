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
}
