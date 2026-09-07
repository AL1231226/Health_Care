package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商家表（服务商账号，归属 service_category 分类） 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
public interface IServiceProviderService extends IService<ServiceProvider> {

    /** 商家列表（管理员）：status 不传查全部，0待审核 1正常 2停用 */
    Result listProviders(Integer status);

    /** 商家入驻审核（管理员）：通过=1正常，驳回=2停用 */
    Result reviewProvider(Long providerId, Boolean pass);

    /** 商家启用/停用（管理员）：仅 1 与 2 之间切换，待审核的必须走审核 */
    Result toggleProviderStatus(Long providerId, Integer status);

    /** 商家端：查询本人店铺资料（token userId 即 provider_id；密码不返回） */
    Result getSelfProfile();

    /** 商家端：修改本人店铺资料（白名单：名称/负责人/简介/详细地址；主营分类/phone/status/role/providerId 等一律忽略） */
    Result updateSelfProfile(ServiceProvider request);
}
