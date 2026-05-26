package com.heima.big_event.utils.Task;

import com.heima.big_event.pojo.Article;
import com.heima.big_event.service.article.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticlePublishTask {
    //日志记录
    private static final Logger log = LoggerFactory.getLogger(NoticePublishTask.class);

    @Autowired
    private ArticleService articleService;

    //定时发布公告,每分钟执行一次

    @Scheduled(cron = "0 * * * * ?")
    public void publishScheduledArticle(){
        try {
            //查询有没有待发布的文章
            List<Article> articleList = articleService.getScheduledArticleList();
            if (articleList == null || articleList.isEmpty()){
                return;
            }

            //处理发布(修改状态为已发布)
            boolean publishArticle = articleService.publishArticle(articleList);
            if (publishArticle){
                log.info("定时发布成功");
            } else {
                log.error("定时发布失败" );
            }
        } catch (Exception e){
            log.error("定时发布文章执行任务异常" , e);
        }
    }
}
