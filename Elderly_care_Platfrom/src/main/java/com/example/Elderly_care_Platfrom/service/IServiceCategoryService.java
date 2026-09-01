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
}
