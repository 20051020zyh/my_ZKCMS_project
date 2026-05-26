package com.heima.big_event.service.article;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import com.heima.big_event.pojo.VO.ArticleRankVO;
import com.heima.big_event.pojo.dto.AddArticleTagDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ArticleService extends IService<Article> {
    //新增文章(带标签)
    @Transactional(rollbackFor = Exception.class)
    void addArticle(AddArticleTagDTO addArticleTagDTO);

    Page<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state, String keyword);


    //浏览量自增
    void addView(Integer articleId);

    //同步redis到数据库
    void syncRedisToDB(Integer articleId);

    //关键词搜索调用
    List<Article> keywordSearchImpl(String keyword);

    //批量操作
    void batchUpdateState(List<Integer> articleId, String state);

    //点赞/取消点赞接口
    @Transactional
//加上事务
    ArticleLikeVO toggleLike(Integer articleId, Integer userId);

    //检查当前用户是否点赞
    boolean checkUserArticleLikeImpl(Integer articleId, Integer userId);

    //定时发布文章
    boolean schedulePublish(Article article);

    List<Article> getScheduledArticleList();

    boolean publishArticle(List<Article> articleList);

    //移入回收站
    void trashImpl(Integer articleId);

    //回收站恢复
    void TrashRecoverImpl(List<Integer> articleIdList);

    //回收站永久删除
    void trashForeverImpl(List<Integer> articleIdList);

    //文章排行接口
    List<ArticleRankVO> getRankList(String type, Integer limit);

    boolean categoryExists(Integer categoryId);

    List<String> getTagNamesByArticleId(Integer articleId);
}
