package com.heima.big_event.pojo;


import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("article_comment")
public class ArticleComment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer articleId;
    private Integer userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @NotBlank(message = "评论内容不能为空")
    private String content;

    // 逻辑删除注解
    @TableLogic
    private Integer isDelete;
    private Integer parentId;//父级id
    private Integer replyUserId;//回复的用户id
    @TableField(exist = false)
    private List<ArticleComment> replyList;//存回复列表的字段
    private Integer likeCount;//评论点赞数
    private Integer auditStatus;//审核状态
    private String rejectReason;//驳回原因
}
