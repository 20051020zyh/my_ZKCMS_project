package com.heima.big_event.pojo.VO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleCollectUserListVO {
    private String title;
    private String cover_img;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime update_time;
    private Integer articleId;
    private Integer Total;
    private List<ArticleCollectUserListVO> records;
    private Integer pageNum;
    private Integer pageSize;
}
