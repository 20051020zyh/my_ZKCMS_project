package com.heima.big_event.mapper.article;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.big_event.pojo.Tags;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagsMapper extends BaseMapper<Tags> {

    @Select("SELECT t.id, t.name, COUNT(at.article_id) AS article_count FROM tags t LEFT JOIN article_tag at ON t.id = at.tag_id GROUP BY t.id, t.name ORDER BY article_count DESC, t.id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectHotTags(int limit);
}
