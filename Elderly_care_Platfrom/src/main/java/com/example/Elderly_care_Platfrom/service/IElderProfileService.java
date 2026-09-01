package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ElderProfile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 老人档案表(归属于家属 sys_user) 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
public interface IElderProfileService extends IService<ElderProfile> {
    Result getAllElders();

    Result addElder(ElderProfile elderProfile);

    Result updateElder(ElderProfile elderProfile);

    Result deleteElder(Long elderId);
}
