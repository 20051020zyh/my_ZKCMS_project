package com.heima.big_event.pojo.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    private Integer id;
    private Integer userId;
    private String userName;
    private String avatar;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Integer parentId;
    private Integer replyUserId;
    private String replyUserName;
    private Integer likeCount;
    private Boolean likedByMe;
    private List<CommentDTO> replyList;
}
