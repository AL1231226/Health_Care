package com.example.Elderly_care_Platfrom.utils;

/**
 * 角色常量（与登录 JWT 的 role claim、各表 role 列取值一致，统一消灭魔法串）。
 * 角色模型：1=家属(sys_user，token userId 即 sys_user.id) / 2=管理员(admin，token userId 即 admin.id)
 * / 3=商家(service_provider，token userId 即 provider_id)。
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
public final class RoleType {

    private RoleType() {
    }

    /** 家属 */
    public static final String USER = "1";

    /** 管理员 */
    public static final String ADMIN = "2";

    /** 商家 */
    public static final String PROVIDER = "3";
}
