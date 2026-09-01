package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.example.Elderly_care_Platfrom.mapper.SysUserMapper;
import com.example.Elderly_care_Platfrom.service.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 家属表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-28
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public Result listUsers(Integer status) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        //status 为空查全部，按注册时间倒序
        queryWrapper.eq(status != null, "status", status)
                .orderByDesc("create_time");
        java.util.List<SysUser> users = list(queryWrapper);
        //密码不返回给前端（响应体里不泄露密码字段）
        users.forEach(user -> user.setPassword(null));
        return Result.ok(users);
    }

    @Override
    public Result toggleUserStatus(Long id, Integer status) {
        if (id == null || (status == null || (status != 0 && status != 1))) {
            return Result.fail("参数不完整");
        }
        SysUser user = getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setStatus(status.byteValue());
        if (!updateById(user)) {
            return Result.fail("操作失败");
        }
        return Result.ok(status == 1 ? "已启用" : "已禁用");
    }
}
