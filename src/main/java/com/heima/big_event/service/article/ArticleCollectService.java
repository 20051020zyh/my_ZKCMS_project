package com.heima.big_event.service.article;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.VO.ArticleCollectUserListVO;
import com.heima.big_event.pojo.VO.ArticleCollectVO;

public interface ArticleCollectService extends IService<ArticleCollect> {
    //收藏/取消收藏
    ArticleCollectVO toggleCollect(Integer articleId, Integer userId);

    //检查当前用户是否收藏
    boolean checkUserArticleCollectImpl(Integer article, Integer userId);

    //我的收藏列表
    ArticleCollectUserListVO MyCollectArticleListImpl(Integer pageNum, Integer pageSize, Integer userId);
}
