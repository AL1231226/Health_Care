package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户地址簿表(隶属于家属 sys_user) 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
public interface IUserAddressService extends IService<UserAddress> {

    Result addAddress(UserAddress userAddress);

    Result updateAddress(UserAddress userAddress);

    Result deleteAddress(Long id);

    Result getAddress(Long id);

    Result getAllAddresses();

    Result setDefault( Long addrId);
}
