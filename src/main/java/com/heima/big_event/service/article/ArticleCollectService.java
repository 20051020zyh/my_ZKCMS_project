package com.heima.big_event.service.article;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.VO.ArticleCollectUserListVO;
import com.heima.big_event.pojo.VO.ArticleCollectVO;

public interface ArticleCollectService extends IService<ArticleCollect> {
    //收藏/取消收藏文章（folderId可选，不为null时将该收藏归入指定文件夹）
    ArticleCollectVO toggleCollect(Integer articleId, Integer userId, Integer folderId);

    //检查当前用户是否已收藏该文章
    boolean checkUserArticleCollectImpl(Integer article, Integer userId);

    //获取当前用户的收藏列表（支持按folderId筛选：>0查指定文件夹，=-1查未分类，null查全部）
    ArticleCollectUserListVO MyCollectArticleListImpl(Integer pageNum, Integer pageSize, Integer userId, Integer folderId);

    //将收藏的文章移入/移出指定文件夹
    void moveFolder(Integer articleId, Integer userId, Integer folderId);
}
