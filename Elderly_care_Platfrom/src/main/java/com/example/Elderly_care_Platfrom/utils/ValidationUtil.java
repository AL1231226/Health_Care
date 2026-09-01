package com.example.Elderly_care_Platfrom.utils;

import java.util.regex.Pattern;

/**
 * 登录/注册参数校验工具类
 */
public final class ValidationUtil {

    /** 手机号：1 开头，第二位 3-9，共 11 位数字 */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** 密码：至少 6 位，且由数字和英文字母组成 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";

    /** 身份证号：18 位数字，末位可为 X */
    public static final String ID_CARD_REGEX = "^\\d{17}[\\dXx]$";


}
