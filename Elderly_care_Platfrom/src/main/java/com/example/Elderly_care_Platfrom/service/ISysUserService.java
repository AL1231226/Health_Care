package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 家属表 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-28
 */
public interface ISysUserService extends IService<SysUser> {

    /** 家属列表：status 不传查全部，0禁用 1正常（返回前剔除密码） */
    Result listUsers(Integer status);

    /** 家属启用/禁用：0禁用 1启用 */
    Result toggleUserStatus(Long id, Integer status);
}
