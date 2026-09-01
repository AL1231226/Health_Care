package com.example.Elderly_care_Platfrom.config;


import com.example.Elderly_care_Platfrom.utils.JwtUtil;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    //拦截器(处理请求）
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        //看请求头是否匹配
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\":\"未登录或token为空\"}");
            return false;
        }
        //取出并解析token
        try {
            String token = authHeader.substring(7);
            Claims claims = JwtUtil.parseToken(token);
            //后面执行的 Controller 就能通过 UserContext.get() 拿到当前登录用户是谁
            UserContext.set(new UserContext.UserInfo(
                    claims.get("userId", String.class),
                    claims.get("role", String.class),
                    claims.get("name", String.class)
            ));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\":\"token无效或已过期\"}");
            return false;
        }
    }

    @Override
    //清空
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
