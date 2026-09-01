package com.example.Elderly_care_Platfrom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 商家表（服务商账号，归属 service_category 分类） Mapper 接口
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Mapper
public interface ServiceProviderMapper extends BaseMapper<ServiceProvider> {

}
