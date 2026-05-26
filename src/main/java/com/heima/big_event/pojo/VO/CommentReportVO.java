package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentReportVO {
    private Integer id;
    private Integer commentId;
    private Integer reportType;
    private String content;
    private List<String> images;
    private Integer userId;
    private String userName;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
