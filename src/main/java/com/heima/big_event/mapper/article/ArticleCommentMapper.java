package com.heima.big_event.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.dto.CommentDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleCommentMapper  extends BaseMapper<ArticleComment> {
    //分页查询一级评论
    @Select("SELECT " +
            "c.id, " +
            "c.user_id AS userId, " +
            "u.username AS userName, " +
            "u.user_pic AS avatar, " +
            "c.content, " +
            "c.create_time AS createTime, " +
            "c.parent_id AS parentId, " +
            "c.reply_user_id AS replyUserId, " +
            "c.like_count AS likeCount " +
            "FROM article_comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.article_id = #{articleId} " +
            "AND c.is_delete = 0 " +
            "AND c.audit_status = 2 "+
            "AND c.parent_id = 0 " +
            "ORDER BY c.create_time DESC")
    IPage<CommentDTO> selectCommentWithUserPage(Page<CommentDTO> page , @Param("articleId") Integer articleId);



    //批量查询一级评论的回复
    @Select("<script>" +
            "SELECT " +
            "c.id, " +
            "c.user_id AS userId, " +
            "u.username AS userName, " +
            "u.user_pic AS avatar, " +
            "c.content, " +
            "c.create_time AS createTime, " +
            "c.parent_id AS parentId, " +
            "c.reply_user_id AS replyUserId, " +
            "c.like_count AS likeCount, " +
            "ru.username AS replyUserName " +
            "FROM article_comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN user ru ON c.reply_user_id = ru.id AND c.reply_user_id > 0 " +
            "WHERE c.article_id = #{articleId} " +
            "AND c.is_delete = 0 " +
            "AND c.audit_status = 2 "+
            "AND c.parent_id IN " +
            "<foreach collection='parentIds' item='pid' open='(' separator=',' close=')'>" +
            "#{pid}" +
            "</foreach> " +
            "ORDER BY c.create_time ASC" +
            "</script>")
    List<CommentDTO> selectReplyComments(@Param("articleId") Integer articleId, @Param("parentIds") List<Integer> parentIds);
}
