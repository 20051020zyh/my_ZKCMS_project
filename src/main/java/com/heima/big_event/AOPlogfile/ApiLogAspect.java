package com.heima.big_event.AOPlogfile;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 接口日志追踪 AOP 切面
 * 拦截指定包下的 Controller 接口，记录请求/响应全链路信息
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    /**
     * 定义切点：拦截项目中所有 Controller 包下的方法（可根据实际包名调整）
     * 示例：com.xxx.xxx.controller 替换为你的项目控制器包路径
     */
    @Pointcut("execution(* com.heima.big_event.controller..*(..))")
    public void apiLogPointcut() {}

    /**
     * 环绕通知：核心逻辑，拦截方法执行前后，记录耗时、请求参数、返回结果
     */
    @Around("apiLogPointcut()")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取请求上下文（请求人、IP、接口路径等）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 2. 记录请求开始时间（用于计算执行耗时）
        long startTime = System.currentTimeMillis();

        // 3. 获取接口方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName(); // 方法名
        String className = joinPoint.getTarget().getClass().getName(); // 类全路径

        try {
            // 4. 打印请求日志
            log.info("===== 接口请求开始 =====");
            log.info("请求人IP: {}", getRequestIp(request)); // 请求IP（可关联到具体用户）
            log.info("请求接口: {}#{}", className, methodName); // 类名+方法名
            log.info("请求路径: {}", request.getRequestURI()); // 接口URL
            log.info("请求参数: {}", Arrays.toString(joinPoint.getArgs())); // 请求参数

            // 5. 执行目标方法（获取接口返回结果）
            Object result = joinPoint.proceed();

            // 6. 计算执行耗时
            long costTime = System.currentTimeMillis() - startTime;

            // 7. 打印响应日志
            log.info("返回结果: {}", result);
            log.info("执行耗时: {}ms", costTime);
            log.info("===== 接口请求结束 =====\n");

            return result;
        } catch (Exception e) {
            // 异常日志记录
            long costTime = System.currentTimeMillis() - startTime;
            log.error("接口异常！耗时: {}ms | 异常信息: ", costTime, e);
            throw e; // 抛出异常，不影响业务流程
        }
    }

    /**
     * 辅助方法：获取请求IP（区分本地/远程请求）
     */
    private String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
