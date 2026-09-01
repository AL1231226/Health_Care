package com.example.Elderly_care_Platfrom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 服务项目表（商家提供的具体服务，购物车加购单位） Mapper 接口
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Mapper
public interface ServiceItemMapper extends BaseMapper<ServiceItem> {

}
