package com.heima.big_event.anno;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//意思是这个注释作用在属性值上
@Target({ElementType.FIELD})
//程序运行时生效,不是在编译时生效
@Retention(RetentionPolicy.RUNTIME)
// 绑定校验器,就是进行判断的那个类,也是自定义的
@Constraint(validatedBy = {StateValidator.class})
public @interface State {
    // 校验失败时的提示信息
    String message() default "发布状态只能是'已发布'或'草稿'";

    // 分组校验（必须保留，框架要求）
    Class<?>[] groups() default {};

    // 负载（必须保留，框架要求）
    Class<? extends Payload>[] payload() default {};
}
