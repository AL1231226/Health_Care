package com.example.Elderly_care_Platfrom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    //拦截器
    public void addInterceptors(InterceptorRegistry registry) {
        //添加拦截器：需要登录的业务接口（auth 登录/注册放行）
        registry.addInterceptor(authInterceptor)
                //拦截的请求
                .addPathPatterns("/address/**", "/elder-profile/**", "/service-provider/**", "/sys-user/**", "/service-item/**", "/service-order/**");
    }
}
