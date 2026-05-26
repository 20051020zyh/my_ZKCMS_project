package com.heima.big_event.service.article;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.VO.CommentAuditListVO;
import com.heima.big_event.pojo.dto.CommentDTO;
import org.springframework.transaction.annotation.Transactional;


public interface ArticleCommentService extends IService<ArticleComment> {

    //发表文章评论


    //发表文章评论
    String addArticleCommentImpl(
            Integer articleId,
            Integer userId,
            String comment,
            Integer pareintId,
            Integer replyUserId);

    //文章评论分页
    IPage<CommentDTO> getArticleCommentPageImpl(Integer articleId, Integer pageNum, Integer pageSize);

    //管理员审核接口
    @Transactional
    void passComment(Integer commentId);

    @Transactional
    void rejectComment(Integer commentId, String reason);

    //待审核评论列表
    Page<CommentAuditListVO> listPendingCommentsImpl(Integer pageNum, Integer pageSize);

    //按审核状态查询评论列表
    Page<CommentAuditListVO> listCommentsByStatus(Integer pageNum, Integer pageSize, Integer auditStatus);
}
