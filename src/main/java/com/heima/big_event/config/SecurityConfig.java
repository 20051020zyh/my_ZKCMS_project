package com.heima.big_event.config;

import com.heima.big_event.utils.MaintenanceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    //注入过滤器（Spring 管理的实例，不会 null）
    private final MaintenanceFilter maintenanceFilter;

    //构造方法注入
    public SecurityConfig(MaintenanceFilter maintenanceFilter) {
        this.maintenanceFilter = maintenanceFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .sessionManagement().disable()

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 👇 用注入的 filter，不要用 new 的！
        .addFilterBefore(maintenanceFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
