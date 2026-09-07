package com.example.Elderly_care_Platfrom.config;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 角色统一校验拦截器（注册在 AuthInterceptor 之后：先登录后角色，未登录到不了本层）。
 * 读取目标方法/Controller 类上的 @RequireRole，当前登录用户 role 不在声明列表内即拒绝，
 * 替代各 service 内手写散落的「无权限」守卫。
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 非控制器方法（静态资源/预检等）直接放行
            return true;
        }
        // 方法注解优先，无则回退 Controller 类级注解（单角色 Controller 标一次，新方法自动继承）
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            // 未声明角色要求（公开或登录即可用）放行
            return true;
        }
        // 已登录用户角色命中即放行
        UserContext.UserInfo user = UserContext.get();
        if (user != null && Arrays.asList(requireRole.value()).contains(user.role())) {
            return true;
        }
        // 越权拒绝：与全站「HTTP 恒 200 + Result」惯例一致，success=false 直走前端既有错误处理路径
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"success\":false,\"errorMsg\":\"无权限\"}");
        return false;
    }
}
