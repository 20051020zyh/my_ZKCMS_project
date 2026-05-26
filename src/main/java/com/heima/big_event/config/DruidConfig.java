package com.heima.big_event.config;

// 核心修复：替换为 jakarta 版本的 Servlet/Filter
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 👇 关键1：必须加@Configuration，Spring才能扫描到
@Configuration
public class DruidConfig {

    /**
     * 配置Druid监控后台Servlet（【SpringBoot 3.x + Jakarta版】）
     * 访问地址：http://localhost:8080/druid
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        // 👇 关键2：ServletRegistrationBean 是 jakarta.servlet 的，IDEA会自动导入
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");

        // 后台登录账号密码（自定义）
        bean.addInitParameter("loginUsername", "admin");
        bean.addInitParameter("loginPassword", "123456");

        // 允许所有IP访问（本地开发用，生产环境建议限制IP）
        bean.addInitParameter("allow", "");
        return bean;
    }

    /**
     * 配置Druid监控过滤器（【SpringBoot 3.x + Jakarta版】）
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        // 👇 关键3：FilterRegistrationBean 是 jakarta.servlet 的，IDEA会自动导入
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>(new WebStatFilter());

        // 拦截所有请求
        bean.addUrlPatterns("/*");

        // 排除静态资源、监控页面、Swagger等不走拦截
        bean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*,/swagger-ui/*,/v3/api-docs/*");
        return bean;
    }
}