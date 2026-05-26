package com.heima.big_event.interceptor;

import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.utils.JwtUtil;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    //JWT工具类,用于生成和解析token
    @Autowired
    private JwtUtil jwtUtil;

    //Redis工具类,管理黑名单
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //放行OPTIONS预检请求(解决跨域+空Token报错)
        //浏览器在发送跨域请求前会先发送OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 如果不是映射到方法，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // ========== 1. 获取方法上的 @RequirePermission 注解 ==========
        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            trySilentAuthenticate(request);
            return true;
        }
        //放行登录/注册接口
        //无需token验证
        //放行登录/注册接口 + Knife4j 全部路径
        if (requirePermission.requiredLogin()) {
            String uri = request.getRequestURI();
            if (uri.equals("/user/login")
                    || uri.equals("/user/register")
                    || uri.equals("/category/detail")
                    || uri.endsWith(".html")
                    || uri.startsWith("/assets/")
                    || uri.startsWith("/image/")
                    || uri.startsWith("/doc.html")
                    || uri.startsWith("/webjars/")
                    || uri.startsWith("/swagger-resources/")
                    || uri.startsWith("/v3/api-docs/")
                    || uri.startsWith("/knife4j/")
                    || uri.startsWith("/swagger-ui/")
                    || uri.startsWith("/swagger-ui.html")
                || uri.equals("/sysConfig/update")
                || uri.equals("/sysConfig/get"))
            {
                return true;
            }

            //完善Token获取逻辑,避免空指针
            //1.获取请求头中的Authorization
            //request.getHeader(String name)方法来自HttpServletRequest request,用来获取指定请求头的值
            String authHeader = request.getHeader("Authorization");//从HTTP请求头中获取Authorizationd的值
            //trim()去除字符串两端的空白字符
            //isEmpty()检查字符串长度是否为0
            if (authHeader == null || authHeader.trim().isEmpty()) {
                //设置HTTP响应的内容类型为JSON
                //application/json:表明响应体式JSON格式
                //charset=UTF-8:指定字符编码为UTF-8
                //确保前端能正确解析响应内容,中文不会乱码
                response.setContentType("application/json;charset=UTF-8");
                //设置HTTP响应状态码为401(未授权)
                //HttpServletResponse.SC_UNAUTHORIZED= 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                //向响应体写入JSON格式的错误信息
                //response.getWriter():获取响应输出流
                //write(string str):写入字符串内容
                response.getWriter().write("{\"code\":401,\"msg\":\"未提供认证令牌\"}");
                return false;
            }

            //2.提取token(兼容Bearer前缀)
            String token;
            //authHeader.startsWith("Bearer"):判断请求头是否以Bearer开头
            if (authHeader.startsWith("Bearer")) {
                //提取"Bearer"前缀后面的令牌部分
                token = authHeader.substring(7).trim();
                //防止去掉前缀后为空
                if (token.isEmpty()) {
                    //既然是空的为什么还要写这串代码呢
                    //因为要返回给前端看,让前端看的懂,JSON格式,返回401(未认证) , 失败原因
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"code\":401,\"msg\":\"认证令牌格式错误\"}");
                    return false;
                }
            } else {
                //直接使用并去掉空格
                token = authHeader.trim();
            }

            //打印日志方便排查
            System.out.println("请求的Token:" + token);
            //"blacklist:"：标识这是黑名单数据
            //redisUtil.get(String key)获取键对应的值
            //如果返回值不是null,说明该token在黑名单中
            boolean isBlack = redisUtil.get("blacklist : " + token) != null;
            System.out.println("黑名单校验结果:" + isBlack);
            //isBlack == true 说明在黑名单中

            //校验黑名单
            if (isBlack) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(401);
                response.getWriter().write("{\"code\":401,\"msg\":\"令牌过期\"}");
                return false;
            }


            //验证token(捕获解析异常,避免空指针/格式错误导致崩溃)
            try {
                //!jwtUtil.validateToken(token):只有当jwtUtil.validateToken(token)返回的是false时才会执行下面的代码
                //那么也就是说当token有异常的时候才会执行下面这段代码
                if (!jwtUtil.validateToken(token)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"code\":401,\"msg\":\"令牌无效或已过期\"}");
                    return false;
                }
                //IllegalArgumentException e触发的场景:
                //传入的token为null
                //传入的token为空字符串
                //token格式明显错误,不是三段式
            } catch (IllegalArgumentException e) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"msg\":\"令牌格式错误或为空\"}");
                return false;
                //Exception e:捕获所有其他类型的异常
            } catch (Exception e) {
                //捕获其他JWT解析异常
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"msg\":\"令牌验证失败：" + e.getMessage() + "\"}");
                return false;
            }

            //验证通过,存入ThreadLocal
            String username = jwtUtil.getUsernameFromToken(token);
            Integer userId = jwtUtil.getUserIdFromToken(token);
            //redis中没有token或者token不一致(目的是保证一个用户同一时间只有一个有效的token)
            String redisKey = "login:user:" + userId;
            Object redisToken = redisUtil.hget(redisKey, "token");

            if (redisToken == null || !token.equals(redisToken.toString())) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"msg\":\"登录状态已失效\"}");
                return false;
            }

            //从 Redis 获取用户状态 status  1=启用 0=禁用
            Object statusObj = redisUtil.hget(redisKey, "status");
            if (statusObj == null || Integer.parseInt(statusObj.toString()) == 0) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"msg\":\"账号已被禁用，请联系管理员\"}");
                return false;
            }


            ThreadLocalUtil.set("userId", userId);
            ThreadLocalUtil.set("username", username);

            //放入request属性
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            //拦截器日志
            System.out.println("Token验证通过 - Token" + token);
            System.out.println("Token验证通过 - Valid: true");

            // ========== 3. 权限校验 ==========
            if (requirePermission.checkPermission()) {
                String requiredPerm = requirePermission.value();
                System.out.println("=== 权限校验开始 ===");
                System.out.println("需要的权限: " + requiredPerm);
                
                Set<String> userPermissions = sysUserRoleService.getUserPermissions(userId.longValue());
                System.out.println("用户拥有的权限: " + userPermissions);

                if (userPermissions.contains("*:*:*") || userPermissions.contains(requiredPerm)) {
                    System.out.println("权限校验通过");
                    return true;
                } else {
                    System.out.println("权限校验失败");
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，需要权限：" + requiredPerm + "\"}");
                    return false;
                }
            }
            return true;
        }
            //放行
            return true;
    }

    private void trySilentAuthenticate(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return;
            }

            String token;
            if (authHeader.startsWith("Bearer")) {
                token = authHeader.substring(7).trim();
                if (token.isEmpty()) {
                    return;
                }
            } else {
                token = authHeader.trim();
            }

            boolean isBlack = redisUtil.get("blacklist : " + token) != null;
            if (isBlack) {
                return;
            }

            if (!jwtUtil.validateToken(token)) {
                return;
            }

            String username = jwtUtil.getUsernameFromToken(token);
            Integer userId = jwtUtil.getUserIdFromToken(token);

            String redisKey = "login:user:" + userId;
            Object redisToken = redisUtil.hget(redisKey, "token");
            if (redisToken == null || !token.equals(redisToken.toString())) {
                return;
            }

            Object statusObj = redisUtil.hget(redisKey, "status");
            if (statusObj == null || Integer.parseInt(statusObj.toString()) == 0) {
                return;
            }

            ThreadLocalUtil.set("userId", userId);
            ThreadLocalUtil.set("username", username);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
        } catch (Exception e) {
            // 静默认证失败不阻塞请求
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清理ThreadLocal,防止内存泄露
        ThreadLocalUtil.clear();
    }
}

