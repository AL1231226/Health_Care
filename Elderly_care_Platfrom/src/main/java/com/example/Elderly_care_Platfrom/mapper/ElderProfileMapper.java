package com.example.Elderly_care_Platfrom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.Elderly_care_Platfrom.entity.ElderProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 老人档案表(归属于家属 sys_user) Mapper 接口
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Mapper
public interface ElderProfileMapper extends BaseMapper<ElderProfile> {

}
