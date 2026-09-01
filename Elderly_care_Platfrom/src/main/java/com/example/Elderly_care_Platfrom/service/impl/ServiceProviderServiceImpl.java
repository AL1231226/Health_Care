package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.service.IServiceProviderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商家表（服务商账号，归属 service_category 分类） 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Service
public class ServiceProviderServiceImpl extends ServiceImpl<ServiceProviderMapper, ServiceProvider> implements IServiceProviderService {

    @Override
    public Result listProviders(Integer status) {
        QueryWrapper<ServiceProvider> queryWrapper = new QueryWrapper<>();
        //status 为空查全部，按入驻时间倒序（最新的待审核商家在最上面）
        queryWrapper.eq(status != null, "status", status)
                .orderByDesc("create_time");
        return Result.ok(list(queryWrapper));
    }

    @Override
    public Result reviewProvider(Long providerId, Boolean pass) {
        if (providerId == null || pass == null) {
            return Result.fail("参数不完整");
        }
        ServiceProvider provider = getById(providerId);
        if (provider == null) {
            return Result.fail("商家不存在");
        }
        if (provider.getStatus() != 0) {
            return Result.fail("该商家不在待审核状态");
        }
        //通过=1正常可登录；驳回=2停用（无法登录）
        provider.setStatus(pass ? (byte) 1 : (byte) 2);
        if (!updateById(provider)) {
            return Result.fail("操作失败");
        }
        return Result.ok(pass ? "审核通过" : "已驳回");
    }

    @Override
    public Result toggleProviderStatus(Long providerId, Integer status) {
        if (providerId == null || (status == null || (status != 1 && status != 2))) {
            return Result.fail("参数不完整");
        }
        ServiceProvider provider = getById(providerId);
        if (provider == null) {
            return Result.fail("商家不存在");
        }
        if (provider.getStatus() == 0) {
            return Result.fail("待审核商家请先审核");
        }
        provider.setStatus(status.byteValue());
        if (!updateById(provider)) {
            return Result.fail("操作失败");
        }
        return Result.ok(status == 1 ? "已启用" : "已停用");
    }
}
