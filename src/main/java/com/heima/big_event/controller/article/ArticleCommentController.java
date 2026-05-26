package com.heima.big_event.controller.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleCommentMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.CommentAuditListVO;
import com.heima.big_event.pojo.dto.CommentDTO;
import com.heima.big_event.service.article.ArticleCommentService;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/article/comment")
@Validated
public class ArticleCommentController {
    @Autowired
    private ArticleCommentService articleCommentService;
    @Autowired
    private ArticleCommentMapper articleCommentMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;

    //发表文章评论
    @PostMapping("/add")
    @RequirePermission("/article/comment/add")
    public Result addArticleComment(
            @RequestParam Integer articleId,
            @RequestParam String comment,
            @RequestParam(defaultValue = "0") Integer parentId,
            @RequestParam(defaultValue = "0") Integer replyUserId
            ){
        Integer userId = ThreadLocalUtil.getUserId();
        //评论不能是空的
        if (comment == null || comment.trim().isEmpty()){
            return Result.error("评论内容不能为空");
        }
        //评论的长度不能超过500
        if (comment.length() > 500){
            return Result.error("评论内容过长,最多500字噢");
        }
        String s = articleCommentService.addArticleCommentImpl(articleId, userId, comment , parentId , replyUserId);
        if (s.equals("文章不存在或者未发布")){
            return Result.error("文章不存在或者未发布");
        }else if (s.equals("评论内容包含违规内容,请文明发言")){
            return Result.error("评论内容包含违规内容,请文明发言");
        }
        return Result.success(s);
    }


    //文章评论分页
    @GetMapping("/page/list")
    @RequirePermission("/article/comment/page/list")
    public Result<IPage<CommentDTO>> getArticleCommentPage(
            @RequestParam Integer articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize){
        IPage<CommentDTO> articleCommentPageImpl = articleCommentService.getArticleCommentPageImpl(articleId, pageNum, pageSize);
        return Result.success(articleCommentPageImpl);
    }



    //删除文章的评论
    @PostMapping("/delete")
    @RequirePermission("/article/comment/delete")
    @Transactional
    public Result deleteMyComment(@RequestParam("commentId") Integer commentId) {
        return doDeleteComment(commentId , false);
    }


    //管理员审核(通过/驳回共用一个接口)
    @PostMapping("/audit")
    @RequirePermission("/article/comment/audit")
    public Result auitComment(
            Integer commentId,
            @RequestParam(defaultValue = "false") String reason,
            Integer auditStatus){
        //判断评论存不存在
        if (commentId == null){
            return Result.error("评论不存在");
        }
        //判断传入的审核状态是通过(2)还是驳回(3)
        if (auditStatus == 2){
            articleCommentService.passComment(commentId);
        }else {
            articleCommentService.rejectComment(commentId , reason);
        }
        return Result.success();
    }



    //待审核评论的列表
    @GetMapping("/pending/list")
    @RequirePermission("/article/comment/pending/list")
    public Result listPendingComments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ){
        Page<CommentAuditListVO> commentAuditListVOPage = articleCommentService.listPendingCommentsImpl(pageNum, pageSize);
        return Result.success(commentAuditListVOPage);
    }

    //管理员评论列表(按审核状态查询)
    @GetMapping("/list")
    @RequirePermission("/article/comment/list")
    public Result listCommentsByStatus(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "1") Integer auditStatus
    ){
        Page<CommentAuditListVO> page = articleCommentService.listCommentsByStatus(pageNum, pageSize, auditStatus);
        return Result.success(page);
    }

    //管理员批量删除评论
    @PostMapping("/batchDelete")
    @RequirePermission("/article/comment/batchDelete")
    @Transactional
    public Result batchDeleteComments(@RequestBody List<Integer> commentIds) {
        //校验参数
        if (commentIds == null || commentIds.isEmpty()) {
            return Result.error("请先选择要删除的评论");
        }
        for (Integer id : commentIds){


            //只要有一个删除失败,自动抛异常,事务回滚
            doDeleteComment(id, true);
        }
        return Result.success("批量删除评论成功");
    }

    //删除评论的复用代码
    private Result doDeleteComment(Integer commentId , boolean isAdmin){
        ArticleComment articleComment = articleCommentMapper.selectById(commentId);

        // 这里改成抛异常！！！
        if (articleComment == null || articleComment.getIsDelete() == 1) {
            throw new BusinessException("评论不存在或已删除");
        }

        if (!isAdmin){
            Integer userId = ThreadLocalUtil.getUserId();
            if (!userId.equals(articleComment.getUserId())){
                throw new BusinessException("无权限删除该评论");
            }
        }

        Integer articleId = articleComment.getArticleId();
        int deleteCount = 0;

        if (articleComment.getParentId() == 0){
            LambdaQueryWrapper<ArticleComment> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(ArticleComment::getParentId , commentId)
                    .eq(ArticleComment::getIsDelete , 0);
            long selectCount = articleCommentMapper.selectCount(countWrapper);
            deleteCount = 1 + (int)selectCount;

            // 1. 直接更新一级评论本身，不用修改对象
            LambdaUpdateWrapper<ArticleComment> selfWrapper = new LambdaUpdateWrapper<>();
            selfWrapper.eq(ArticleComment::getId, commentId)
                    .eq(ArticleComment::getIsDelete, 0) // 加个乐观锁，防止重复删除
                    .set(ArticleComment::getIsDelete, 1);
            int selfUpdate = articleCommentMapper.update(null, selfWrapper);
            if (selfUpdate <= 0) {
                throw new BusinessException("删除评论失败");
            }

            // 2. 更新所有子评论
            LambdaUpdateWrapper<ArticleComment> childWrapper = new LambdaUpdateWrapper<>();
            childWrapper.eq(ArticleComment::getParentId, commentId)
                    .eq(ArticleComment::getIsDelete, 0)
                    .set(ArticleComment::getIsDelete, 1);
            articleCommentMapper.update(null, childWrapper);
        }else {
            deleteCount = 1;
            LambdaUpdateWrapper<ArticleComment> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ArticleComment::getId, commentId)
                    .set(ArticleComment::getIsDelete, 1);
            int update = articleCommentMapper.update(null, wrapper);

            // 这里也改成抛异常！！！
            if (update <= 0) {
                throw new BusinessException("删除评论失败");
            }
        }

        if (deleteCount > 0){
            LambdaUpdateWrapper<Article> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(Article::getId, articleId)
                    .setSql("comment_count = comment_count - " + deleteCount);
            articleMapper.update(null, wrapper1);
        }

        // 只有成功才会走到这里
        return Result.success("删除成功");
    }

}
