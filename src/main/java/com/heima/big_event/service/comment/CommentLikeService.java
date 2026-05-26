package com.heima.big_event.service.comment;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.CommentLike;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import org.springframework.transaction.annotation.Transactional;

public interface CommentLikeService extends IService<CommentLike> {
    //评论点赞
    //点赞/取消点赞接口
    @Transactional
    ArticleLikeVO commentLike(Integer articleId, Integer userId, Integer commentId);

    //检查该用户是否给评论点了赞
    boolean checkCommentLikeImpl(Integer commentId, Integer userId);
}
