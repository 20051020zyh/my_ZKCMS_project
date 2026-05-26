package com.heima.big_event.interceptor;

import com.heima.big_event.utils.SensitiveWordUtil;
import com.heima.big_event.utils.RequestWrapper.BodyRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class SensitiveWordInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String[] values = entry.getValue();
                for (String value : values) {
                    if (SensitiveWordUtil.containsSensitive(value)) {
                        out(response, "内容包含敏感词，请重新输入");
                        return false;
                    }
                }
            }
            return true;
        }

        String body;
        if (request instanceof BodyRequestWrapper wrapper) {
            body = wrapper.getBody();
        } else {
            body = "";
        }

        if (body != null && !body.isEmpty()) {
            if (SensitiveWordUtil.containsSensitive(body)) {
                out(response, "内容包含敏感词，请重新输入");
                return false;
            }
        }

        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            for (String value : values) {
                if (SensitiveWordUtil.containsSensitive(value)) {
                    out(response, "内容包含敏感词，请重新输入");
                    return false;
                }
            }
        }

        return true;
    }

    private void out(HttpServletResponse response, String msg) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        String json = String.format("{\"code\":400,\"msg\":\"%s\"}", msg);
        response.getWriter().write(json);
    }
}