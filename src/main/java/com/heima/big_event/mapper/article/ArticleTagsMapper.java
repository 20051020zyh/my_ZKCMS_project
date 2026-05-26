package com.heima.big_event.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.ArticleTag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleTagsMapper extends BaseMapper<ArticleTag> {

    @Insert("<script>" +
            "insert into article_tag(article_id,tag_id) values " +
            "<foreach collection='tagIds' item='tid' separator=','>" +
            "(#{articleId},#{tid})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("articleId") Integer articleId,
                     @Param("tagIds") List<Long> tagIds);

    @Select("<script>SELECT at.article_id, t.name FROM article_tag at INNER JOIN tags t ON at.tag_id = t.id WHERE at.article_id IN " +
            "<foreach collection='articleIds' item='aid' open='(' separator=',' close=')'>#{aid}</foreach></script>")
    List<Map<String, Object>> selectTagNamesByArticleIds(@Param("articleIds") List<Integer> articleIds);
}
