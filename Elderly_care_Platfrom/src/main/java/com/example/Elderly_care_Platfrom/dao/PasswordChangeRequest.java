package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码请求体：家属端自助改密（原密码校验通过后更新为新密码）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeRequest {
    /** 原密码（与库中现存密码比对，防止他人代改） */
    private String oldPassword;
    /** 新密码（规则与登录/注册一致：至少 6 位且包含字母和数字） */
    private String newPassword;
}
