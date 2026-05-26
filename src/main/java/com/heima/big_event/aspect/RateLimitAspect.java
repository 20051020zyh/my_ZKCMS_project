package com.heima.big_event.aspect;

import com.heima.big_event.anno.RateLimit;
import com.heima.big_event.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

//说明这是个切面类.用来给方法添加额外功能
@Aspect
@Component//交给spring管理
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    // 构造器注入RedissonClient:操作redis的工具
    public RateLimitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 1. 修复空指针：先判空，再获取request
        //获取当前请求的上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            //不是浏览器请求，直接放行(避免空指针)
            return point.proceed();
        }
        //拿到请求对象,里面有IP ,请求头之类的信息
        HttpServletRequest request = attributes.getRequest();
        //获取真实IP
        String ip = getClientIp(request);

        // 2. 获取当前被访问的方法名，构造限流key
        //格式:   IP地址:方法名:自定义key(让redis知道是谁在请求哪个接口,精准限流)
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodName = signature.getMethod().getName();
        String key = ip + ":" + methodName + ":" + rateLimit.key();

        // 3. 获取Redisson限流器，设置限流规则
        //用刚才的key创建一个专属限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        //RateType.OVERALL所以请求统一限制(不区分客户端)
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.count(), rateLimit.time(), RateIntervalUnit.SECONDS);

        // 4. 尝试获取令牌，超过阈值抛异常
        //rateLimiter.tryAcquire():尝试获取令牌
        if (!rateLimiter.tryAcquire()) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }

        // 5. 放行请求，执行目标方法
        return point.proceed();
    }

    /**
     * 获取客户端真实IP（兼容代理/网关场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理，取第一个IP
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}