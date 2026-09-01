package com.example.Elderly_care_Platfrom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Elderly_care_Platfrom.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户地址簿表(隶属于家属 sys_user) Mapper 接口
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {

}
