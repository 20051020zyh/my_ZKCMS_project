package com.heima.big_event.filter;

import com.heima.big_event.utils.RequestWrapper.BodyRequestWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Integer.MIN_VALUE)
public class RequestBodyCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            chain.doFilter(request, servletResponse);
            return;
        }

        BodyRequestWrapper wrapper = new BodyRequestWrapper(request);
        chain.doFilter(wrapper, servletResponse);
    }
}