package com.heima.big_event.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM article WHERE is_delete = 0")
    Long sumViewCount();

    @Select("SELECT COALESCE(SUM(like_count), 0) FROM article WHERE is_delete = 0")
    Long sumLikeCount();

    @Select("SELECT COALESCE(SUM(collect_count), 0) FROM article WHERE is_delete = 0")
    Long sumCollectCount();

    @Select("SELECT COALESCE(SUM(like_count), 0) FROM article WHERE create_time BETWEEN #{start} AND #{end} AND is_delete = 0")
    Long sumLikeCountByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(collect_count), 0) FROM article WHERE create_time BETWEEN #{start} AND #{end} AND is_delete = 0")
    Long sumCollectCountByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM article WHERE create_time BETWEEN #{start} AND #{end} AND is_delete = 0")
    Long sumViewCountByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
