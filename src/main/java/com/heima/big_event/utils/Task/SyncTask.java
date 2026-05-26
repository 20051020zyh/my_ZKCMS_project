package com.heima.big_event.utils.Task;

import com.heima.big_event.service.article.ArticleService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SyncTask {
    @Resource
    ArticleService articleService;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    //每五分钟执行一次
    @Scheduled(fixedRate = 300000)
    //@Scheduled是spring的定时任务注解,自动周期性的执行这个方法
    public void sync(){
        //view:count:*的意思就是代表匹配所有前缀为view:count:的 Redis Key
        Set<String> keys = stringRedisTemplate.keys("view:count:*");

        for (String key: keys){
            Integer id = Integer.parseInt(key.replace("view:count:",""));
            articleService.syncRedisToDB(id);

            }
        }
    }
