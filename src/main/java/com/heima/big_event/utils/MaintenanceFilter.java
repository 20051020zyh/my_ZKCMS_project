package com.heima.big_event.utils;

import com.heima.big_event.service.system.SysConfigService;
import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.RequestWrapper.RequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@Component
public class MaintenanceFilter extends OncePerRequestFilter {

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SysUserRoleService sysUserRoleService;

    private static final String[] EXCLUDE_PATH = {
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs/swagger-config",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/knife4j/**",
            "/login",
            "/logout",
            "/static/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/sysConfig/update",
            "/sysConfig/get",
            "/user/updateAvatar",
            "/user/testAvatar",
            "/user/login",
            "/user/register",
            "/index/popInfo"
    };

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        for (String pattern : EXCLUDE_PATH) {
            if (antPathMatcher.match(pattern, uri)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (!sysConfigService.getStatusImpl()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAdminByJwt(request)) {
            RequestWrapper wrappedRequest = new RequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        returnMaintenanceResult(response);
    }

    private boolean isAdminByJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.trim().isEmpty()) {
            System.out.println("[MaintenanceFilter] 无 Authorization 头");
            return false;
        }

        String token;
        if (authHeader.startsWith("Bearer")) {
            token = authHeader.substring(7).trim();
        } else {
            token = authHeader.trim();
        }

        if (token.isEmpty()) {
            System.out.println("[MaintenanceFilter] token 为空");
            return false;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                System.out.println("[MaintenanceFilter] JWT 验证失败");
                return false;
            }

            Integer userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                System.out.println("[MaintenanceFilter] userId 为 null");
                return false;
            }

            String redisKey = "login:user:" + userId;

            Object redisToken = redisUtil.hget(redisKey, "token");
            if (redisToken == null || !token.equals(redisToken.toString())) {
                System.out.println("[MaintenanceFilter] token 不匹配");
                return false;
            }

            Object statusObj = redisUtil.hget(redisKey, "status");
            if (statusObj == null || Integer.parseInt(statusObj.toString()) == 0) {
                System.out.println("[MaintenanceFilter] 账号被禁用");
                return false;
            }

            // 直接查数据库判断管理员，与 JwtInterceptor 保持一致
            Set<String> permissions = sysUserRoleService.getUserPermissions(userId.longValue());
            System.out.println("[MaintenanceFilter] DB permissions for user " + userId + ": " + permissions);
            boolean isAdmin = permissions.contains("*:*:*");
            System.out.println("[MaintenanceFilter] is admin: " + isAdmin);
            return isAdmin;
        } catch (Exception e) {
            System.out.println("[MaintenanceFilter] 异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void returnMaintenanceResult(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(200);
        String json = "{\"code\":9999,\"msg\":\"系统正在维护中，暂无法操作\"}";
        response.getWriter().write(json);
    }
}
