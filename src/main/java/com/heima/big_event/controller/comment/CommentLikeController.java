package com.heima.big_event.controller.comment;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import com.heima.big_event.service.comment.CommentLikeService;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/comment/like")
public class CommentLikeController {
    //评论点赞表
    @Autowired
    private CommentLikeService commentLikeService;

    //点赞/取消点赞接口
    @PostMapping("/add")
    @RequirePermission("comment/like/add")
    public Result addCommentLike(Integer articleId , Integer commentId){
        Integer userId = ThreadLocalUtil.getUserId();
        ArticleLikeVO articleLikeVO = commentLikeService.commentLike(articleId, userId, commentId);
        return Result.success(articleLikeVO);
    }


    //检查该用户是否给评论点了赞
    @GetMapping("/check")
    @RequirePermission("comment/like/check")
    public Result checkCommentLike(Integer commentId){
        Integer userId = ThreadLocalUtil.getUserId();
        boolean b = commentLikeService.checkCommentLikeImpl(commentId, userId);
        return Result.success(b);
    }
}
