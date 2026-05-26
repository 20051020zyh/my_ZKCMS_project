package com.heima.big_event.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    //MP分页插件
    //MyBatis-Plus 分页插件就是帮你自动完成 “分页查询 + 总数统计” 的核心工具
    //自动执行总数统计
    //自动计算分页信息
    //自动拦截分页查询: 自动拼接分页关键字（比如 LIMIT）,自动生成并执行一条统计总数的 COUNT(*) SQL
    //LIMIT的作用是限制返回结果条数的语法，专门做分页


    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 这里根据你的数据库类型选择，比如 MYSQL、POSTGRE_SQL 等
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
