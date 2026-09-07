package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.mapper.ServiceCategoryMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.service.IServiceProviderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.annotation.Resource;
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

    @Resource
    private ServiceCategoryMapper serviceCategoryMapper;

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

    @Override
    public Result getSelfProfile() {
        //商家自助：token userId 即 provider_id（登录后状态必为 1 正常，无需状态闸门）
        ServiceProvider provider = getById(Long.valueOf(UserContext.get().userId()));
        if (provider == null) {
            return Result.fail("商家不存在");
        }
        //密码不返回给前端（响应体里不泄露密码字段）
        provider.setPassword(null);
        return Result.ok(provider);
    }

    @Override
    public Result updateSelfProfile(ServiceProvider request) {
        ServiceProvider provider = getById(Long.valueOf(UserContext.get().userId()));
        if (provider == null) {
            return Result.fail("商家不存在");
        }
        //白名单复制：仅信任名称/主营分类/负责人/简介/详细地址五字段，请求体其他字段(role/status/password/phone/providerId 等)一律忽略
        String providerName = request.getProviderName() == null ? null : request.getProviderName().trim();
        if (providerName == null || providerName.isEmpty()) {
            return Result.fail("商家名称不能为空");
        }
        if (providerName.length() > 30) {
            return Result.fail("商家名称过长（最多30字）");
        }
        if (request.getCategoryId() == null) {
            return Result.fail("请选择主营服务分类");
        }
        //主营分类必须真实存在（同商家入驻校验）
        if (serviceCategoryMapper.selectById(request.getCategoryId()) == null) {
            return Result.fail("主营服务分类不存在");
        }
        //长字段：trim、空串归一 null、限长
        String legalPerson = trimToNull(request.getLegalPerson());
        if (legalPerson != null && legalPerson.length() > 30) {
            return Result.fail("负责人过长（最多30字）");
        }
        String intro = trimToNull(request.getIntro());
        if (intro != null && intro.length() > 200) {
            return Result.fail("简介过长（最多200字）");
        }
        String address = trimToNull(request.getAddress());
        if (address != null && address.length() > 100) {
            return Result.fail("详细地址过长（最多100字）");
        }
        provider.setProviderName(providerName);
        provider.setCategoryId(request.getCategoryId());
        provider.setLegalPerson(legalPerson);
        provider.setIntro(intro);
        provider.setAddress(address);
        if (!updateById(provider)) {
            return Result.fail("操作失败，请稍后重试");
        }
        //回返最新行（置空密码），前端直接整对象同步
        provider = getById(provider.getProviderId());
        provider.setPassword(null);
        return Result.ok(provider);
    }

    /** trim 后为空串归一 null（null 原样返回） */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
