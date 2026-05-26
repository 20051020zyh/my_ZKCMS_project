package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentAuditListVO {
    private Integer id;
    private Integer articleId;//文章id
    private String articleTitle;//文章标题
    private Integer userId;//用户id
    private String userName;//用户名
    private String content;//评论内容
    private LocalDateTime creatime;//评论的时间
    private Integer auditStatus;//审核状态
    private String rejectReason;//驳回原因
}
