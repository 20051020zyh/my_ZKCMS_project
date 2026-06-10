package com.heima.big_event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时 5 秒
        factory.setConnectTimeout(5000);
        // 读取超时 10 秒
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
