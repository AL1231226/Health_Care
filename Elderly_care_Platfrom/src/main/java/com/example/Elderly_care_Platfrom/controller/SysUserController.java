package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.PasswordChangeRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.ISysUserService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 家属表 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-28
 */
@RestController
@RequestMapping("/sys-user")
@RequireRole(RoleType.ADMIN)
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;

    /** 家属列表：status 可选 0禁用/1正常 */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer status) {
        return sysUserService.listUsers(status);
    }

    /** 启用/禁用：status=0 或 1 */
    @PutMapping("/status/{id}")
    public Result toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        return sysUserService.toggleUserStatus(id, status);
    }

    /** 家属端自助修改密码（方法级 USER 覆盖类级 ADMIN，归属取 token；管理员/商家越权拒「无权限」） */
    @RequireRole(RoleType.USER)
    @PutMapping("/self/password")
    public Result changePassword(@RequestBody PasswordChangeRequest req) {
        return sysUserService.changeSelfPassword(req);
    }
}
