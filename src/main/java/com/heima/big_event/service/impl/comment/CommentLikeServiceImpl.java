package com.heima.big_event.service.impl.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleCommentMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.comment.CommentLikeMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.CommentLike;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import com.heima.big_event.service.comment.CommentLikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper , CommentLike> implements CommentLikeService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    @Autowired
    private ArticleCommentMapper articleCommentMapper;



    //评论点赞
    //点赞/取消点赞接口
    @Transactional
    @Override
    public ArticleLikeVO commentLike(Integer articleId, Integer userId, Integer commentId){
        //检查文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getState().equals("草稿")){
            throw  new BusinessException("文章不存在或者未发布");
        }

        //检查评论是否存在
        ArticleComment articleComment = articleCommentMapper.selectById(commentId);
        if (articleComment == null){
            throw new BusinessException("评论不存在");
        }


        //查询用户是否点赞了
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId , commentId)
                .eq(CommentLike::getUserId , userId);
        CommentLike existLike = commentLikeMapper.selectOne(wrapper);

        boolean isLike;
        int newLikeCount;

        //如果评论点赞表里面有数据的话
        if (existLike != null){
            commentLikeMapper.deleteById(existLike.getId());
            //评论表的点赞数-1
            LambdaUpdateWrapper<ArticleComment> commentLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            commentLambdaUpdateWrapper.eq(ArticleComment::getId , commentId)
                    .setSql("like_count = like_count - 1")
                            .gt(ArticleComment::getLikeCount , 0);

            articleCommentMapper.update(null , commentLambdaUpdateWrapper);
            isLike = false;
        } else {
            //说明之前没有数据
            //新增点赞
            CommentLike commentLike = new CommentLike();
            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);
            commentLikeMapper.insert(commentLike);

            //评论表的该评论点赞数+1
            LambdaUpdateWrapper<ArticleComment> articleCommentLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            articleCommentLambdaUpdateWrapper.eq(ArticleComment::getId , commentId)
                    .setSql("like_count = like_count + 1");
            articleCommentMapper.update(null , articleCommentLambdaUpdateWrapper);
            isLike = true;
        }

        //查询最新的评论点赞数
        ArticleLikeVO vo = new ArticleLikeVO();
        ArticleComment articleComment1 = articleCommentMapper.selectById(commentId);
        vo.setLikeCount(articleComment1.getLikeCount());
        vo.setIsLike(isLike);
        return vo;


    }


    //检查该用户是否给评论点了赞
    @Override
    public boolean checkCommentLikeImpl(Integer commentId, Integer userId){
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId , commentId)
                .eq(CommentLike::getUserId , userId);
        return commentLikeMapper.exists(wrapper);
        //exists比selectOne查的更快,开销更小
    }
}
