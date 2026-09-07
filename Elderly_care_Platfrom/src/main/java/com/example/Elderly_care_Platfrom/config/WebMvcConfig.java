package com.example.Elderly_care_Platfrom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, RoleInterceptor roleInterceptor) {
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    /** 需登录的业务接口前缀（auth 登录/注册与 service-category 公开浏览除外），登录/角色两拦截器共用 */
    private static final String[] PROTECTED_PATTERNS = {"/address/**", "/elder-profile/**", "/service-provider/**",
            "/sys-user/**", "/service-item/**", "/service-order/**", "/service-comment/**", "/service-cart/**"};

    @Override
    //拦截器
    public void addInterceptors(InterceptorRegistry registry) {
        //第一层：登录拦截——需登录的业务接口（auth 登录/注册放行），未登录 401
        registry.addInterceptor(authInterceptor)
                //拦截的请求
                .addPathPatterns(PROTECTED_PATTERNS);
        //第二层：角色统一校验拦截——按 @RequireRole 声明验角色，先登录后角色（注册顺序即执行顺序）
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns(PROTECTED_PATTERNS);
    }
}
