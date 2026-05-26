package com.heima.big_event.pojo.VO;

import lombok.Data;

import java.util.List;

@Data
public class ArticleTrendVO {
    private List<String> dateList;
    private List<Long> publishList;
    private List<Integer> viewList;
    private List<Integer> userList;
}
