package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCategory;
import com.example.Elderly_care_Platfrom.mapper.ServiceCategoryMapper;
import com.example.Elderly_care_Platfrom.service.IServiceCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 服务分类字典表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Service
public class ServiceCategoryServiceImpl extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> implements IServiceCategoryService {

    @Override
    public Result listEnabledCategories() {
        // 只返回启用中的分类，按 sort 升序（前端首页按此顺序展示）
        QueryWrapper<ServiceCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1).orderByAsc("sort");
        List<ServiceCategory> categories = list(queryWrapper);
        return Result.ok(categories, (long) categories.size());
    }
}
