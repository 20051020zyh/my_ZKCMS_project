package com.heima.big_event.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 百度热搜条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaiduHotItem implements Serializable {

    /** 排名（1~10） */
    private Integer rank;

    /** 热搜标题 */
    private String word;

    /** 热搜描述（标签名，如"热议"、"新"、"热"） */
    private String desc;

    /** 热度值（百度 wise 接口无此字段，用排名指数代替） */
    private String hotScore;

    /** 百度原始链接 */
    private String url;
}
