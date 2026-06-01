package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ArticleCenterInfoVO {
    private String username;
    private String nickname;
    private String email;
    private String user_pic;
    private Long fabuCount;
    private Long caogaoCount;
    private Long commentCount;
    private Long collectCount;
    private Integer followCount;
    private Integer fansCount;
    private List<Map<String, Object>> collectFolders;
}
