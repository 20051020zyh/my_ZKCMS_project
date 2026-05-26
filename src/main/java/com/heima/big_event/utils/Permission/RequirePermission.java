package com.heima.big_event.utils.Permission;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /**
     * 需要的权限标识，对应 sys_permission.permission 字段的值
     * 例如： "/article/report/list" 或 "category/add"
     */
    String value();

    /**
     * 是否需要登录（默认 true）
     * 如果是 false，则只检查登录状态，不检查具体权限
     */
    boolean requiredLogin() default true;

    /**
     * 是否检查具体权限（默认 true）
     * 如果为 false，则只验证登录状态，不校验具体权限标识
     * 适用于 admin 验证类接口，避免循环依赖
     */
    boolean checkPermission() default true;
}
