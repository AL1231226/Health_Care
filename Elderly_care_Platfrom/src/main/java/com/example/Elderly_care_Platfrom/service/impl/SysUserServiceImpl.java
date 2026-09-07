package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.PasswordChangeRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.example.Elderly_care_Platfrom.mapper.SysUserMapper;
import com.example.Elderly_care_Platfrom.service.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PASSWORD_REGEX;

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

    @Override
    public Result changeSelfPassword(PasswordChangeRequest req) {
        //归属取 token 家属（UserId 即 sys_user.id，同 familyLogin 签发口径）
        UserContext.UserInfo login = UserContext.get();
        if (req == null || StrUtil.isBlank(req.getOldPassword()) || StrUtil.isBlank(req.getNewPassword())) {
            return Result.fail("参数不完整");
        }
        Long userId = login == null || login.userId() == null ? null : Long.parseLong(login.userId());
        SysUser user = userId == null ? null : getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!user.getPassword().equals(req.getOldPassword())) {
            return Result.fail("原密码不正确");
        }
        //新密码规则与登录/注册一致（至少 6 位且包含字母和数字），提前拦不匹配的格式
        if (!Pattern.matches(PASSWORD_REGEX, req.getNewPassword())) {
            return Result.fail("新密码至少 6 位，且需包含字母和数字");
        }
        if (req.getNewPassword().equals(req.getOldPassword())) {
            return Result.fail("新密码不能与原密码相同");
        }
        user.setPassword(req.getNewPassword());
        if (!updateById(user)) {
            return Result.fail("操作失败，请重试");
        }
        return Result.ok("修改成功");
    }
}
