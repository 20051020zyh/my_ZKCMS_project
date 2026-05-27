package com.heima.big_event.config;

import com.heima.big_event.interceptor.JwtInterceptor;
import com.heima.big_event.interceptor.SensitiveWordInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private SensitiveWordInterceptor sensitiveWordInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //先注册敏感词拦截器（最先执行）
        registry.addInterceptor(sensitiveWordInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/user/login",
                        "/user/register",
                        "/user/testAvatar",
                        "/assets/**",
                        "/image/**",
                        "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.ico",
                        "/", "/index.html", "/login.html", "/register.html",
                        "/druid/**",
                        "/leaveMessage/**"
                );

         //原来的JWT拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs/swagger-config",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/knife4j/**",
                        "/user/login",
                        "/user/register",
                        "/user/testAvatar",
                        "/assets/**",
                        "/image/**",
                        "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.ico",
                        "/", "/index.html", "/login.html", "/register.html",
                        "/druid/**",
                        "/leaveMessage/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}