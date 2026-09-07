package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.PasswordChangeRequest;
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

    /**
     * 家属自助修改密码（归属取 token 家属；Controller 层 @RequireRole(USER) 覆盖类级 ADMIN，
     * 管理员/商家打此端点越权拒「无权限」）：原密码核对通过后更新，新密码规则同注册（至少 6 位且含字母和数字）
     *
     * @param req 原密码 + 新密码
     */
    Result changeSelfPassword(PasswordChangeRequest req);
}
