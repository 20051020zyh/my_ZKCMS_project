package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleCollectMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.VO.ArticleCollectUserListVO;
import com.heima.big_event.pojo.VO.ArticleCollectVO;
import com.heima.big_event.service.article.ArticleCollectService;
import com.heima.big_event.utils.Others.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleCollectServiceImpl extends ServiceImpl<ArticleCollectMapper , ArticleCollect> implements ArticleCollectService {
    @Autowired
    private ArticleCollectMapper articleCollectMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private RedisUtil redisUtil;


    //收藏/取消收藏
    @Override
    public ArticleCollectVO toggleCollect(Integer articleId, Integer userId, Integer folderId) {
        //检查文章是否存在,是否"已发布"
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getState().equals("草稿")) {
            throw new BusinessException("文章不存在或者未发布");
        }

        //查询用户有没有收藏
        LambdaQueryWrapper<ArticleCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleCollect::getArticleId, articleId)
                .eq(ArticleCollect::getUserId, userId);

        ArticleCollect articleCollect = articleCollectMapper.selectOne(wrapper);

        boolean isCollect;

        //如果收藏表里面有数据
        if (articleCollect != null) {
            //那么就取消收藏
            articleCollectMapper.deleteById(articleCollect.getId());

            LambdaUpdateWrapper<Article> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(Article::getId, articleId)
                    .setSql("collect_count = collect_count - 1");

            articleMapper.update(null, wrapper1);
            isCollect = false;
        } else {
            //那么就新增收藏
            ArticleCollect exisCollect = new ArticleCollect();
            exisCollect.setArticleId(articleId);
            exisCollect.setUserId(userId);
            if (folderId != null && folderId > 0) {
                exisCollect.setFolderId(folderId);
            }
            articleCollectMapper.insert(exisCollect);

            //文章表的收藏数+1
            LambdaUpdateWrapper<Article> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(Article::getId, articleId)
                    .setSql("collect_count = collect_count + 1");

            articleMapper.update(null, wrapper1);
            isCollect = true;
        }

        //查询最新的收藏数
        ArticleCollectVO vo = new ArticleCollectVO();
        Article article1 = articleMapper.selectById(articleId);
        vo.setCollectCount(article1.getCollectCount());
        vo.setIsCollect(isCollect);

        redisUtil.deleteByPattern("article:list:*");
        redisUtil.delete("article:" + articleId);

        return vo;
    }

    //检查当前用户是否收藏
    @Override
    public boolean checkUserArticleCollectImpl(Integer article, Integer userId) {
        LambdaQueryWrapper<ArticleCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleCollect::getUserId, userId)
                .eq(ArticleCollect::getArticleId, article);
        ArticleCollect articleCollect = articleCollectMapper.selectOne(wrapper);
        boolean is_Collect;
        if (articleCollect == null) {
            is_Collect = false;
        } else {
            is_Collect = true;
        }
        return is_Collect;
    }

    //我的收藏列表
    @Override
    public ArticleCollectUserListVO MyCollectArticleListImpl(Integer pageNum, Integer pageSize, Integer userId, Integer folderId) {
        Page<ArticleCollect> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ArticleCollect> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(ArticleCollect::getUserId, userId);

        if (folderId != null && folderId > 0) {
            wrapper1.eq(ArticleCollect::getFolderId, folderId);
        } else if (folderId != null && folderId == -1) {
            wrapper1.isNull(ArticleCollect::getFolderId);
        }

        wrapper1.orderByDesc(ArticleCollect::getCreateTime);

        //分页查询用户的收藏记录
        Page<ArticleCollect> collectResult = articleCollectMapper.selectPage(page, wrapper1);

        //从分页结果中提取文章id列表
        List<Integer> articleIdList = collectResult.getRecords().stream()
                .map(ArticleCollect::getArticleId)
                .collect(Collectors.toList());
        //如果集合是空的
        if (articleIdList.isEmpty()) {
            //没有收藏的文章
            return new ArticleCollectUserListVO();
        }

        //去文章表查询这些id的文章
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.in(Article::getId, articleIdList)
                .eq(Article::getState, "已发布");

        List<Article> articleList = articleMapper.selectList(articleWrapper);

        //组装Vo
        List<ArticleCollectUserListVO> vos = new ArrayList<>();
        for (ArticleCollect collect : collectResult.getRecords()) {
            Article article = articleList.stream()
                    .filter(a -> a.getId().equals(collect.getArticleId()))
                    .findFirst()
                    .orElse(null);
            if (article != null) {
                ArticleCollectUserListVO vo = new ArticleCollectUserListVO();
                vo.setTitle(article.getTitle());
                vo.setCover_img(article.getCoverImg());
                vo.setUpdate_time(collect.getCreateTime());
                vo.setArticleId(article.getId());
                vo.setFolderId(collect.getFolderId());
                vos.add(vo);
            }
        }


        //封装分页的结果
        ArticleCollectUserListVO result = new ArticleCollectUserListVO();
        //返回收藏的总条数
        result.setTotal((int) collectResult.getTotal());
        //返回当前页的数据
        result.setRecords(vos);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);

        return result;
    }

    //将收藏的文章移入/移出指定文件夹
    @Override
    public void moveFolder(Integer articleId, Integer userId, Integer folderId) {
        LambdaQueryWrapper<ArticleCollect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleCollect::getArticleId, articleId)
                .eq(ArticleCollect::getUserId, userId);
        ArticleCollect collect = articleCollectMapper.selectOne(wrapper);
        if (collect == null) {
            throw new BusinessException("收藏记录不存在");
        }
        if (folderId != null && folderId > 0) {
            collect.setFolderId(folderId);
        } else {
            collect.setFolderId(null);
        }
        articleCollectMapper.updateById(collect);
    }
}
