package com.example.Elderly_care_Platfrom.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限统一校验注解：标注在 Controller 类或方法上，声明可访问该接口的角色。
 * 由 RoleInterceptor 统一执行（角色不符返 Result 失败形态「无权限」），
 * 替代以往各 service 内手写散落的 role 守卫。
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色列表，取值见 utils.RoleType：
     * 1=家属(sys_user) / 2=管理员(admin) / 3=商家(service_provider)
     */
    String[] value();
}
