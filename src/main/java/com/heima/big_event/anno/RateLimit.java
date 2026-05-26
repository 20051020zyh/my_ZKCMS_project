package com.heima.big_event.anno;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    //限流key前缀
    String key() default "";
    //时间窗口(秒)
    int time() default 60;
    //最大请求数
    int count() default 10;
    
}
