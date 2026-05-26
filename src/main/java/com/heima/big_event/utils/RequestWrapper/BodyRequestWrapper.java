package com.heima.big_event.utils.RequestWrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BodyRequestWrapper extends HttpServletRequestWrapper {

    private final String body;

    public BodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 读取原始请求体
        InputStream inputStream = request.getInputStream();
        this.body = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    }

    // 获取请求体JSON字符串
    public String getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bis.read();
            }

            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 空实现即可
            }
        };
    }
}