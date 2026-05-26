package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.article.ArticleTagsMapper;
import com.heima.big_event.mapper.article.TagsMapper;
import com.heima.big_event.pojo.Tags;
import com.heima.big_event.service.article.TagsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TagsServiceImpl extends ServiceImpl<TagsMapper , Tags> implements TagsService {
    @Autowired
    private ArticleTagsMapper articleTagsMapper;
    @Autowired
    private TagsMapper tagsMapper;

    @Override
    public String addTagImpl(Tags tags){
        try {
            // 这里如果配置了自动填充，可以删掉手动setCreateTime这行
            tags.setCreateTime(LocalDateTime.now());
            int insert = tagsMapper.insert(tags);
            if (insert > 0){
                return "新增标签成功";
            } else {
                return "添加标签失败";
            }
        } catch (Exception e) {
            // 打印完整的异常栈，看看到底是什么错
            e.printStackTrace();
            log.error("新增标签失败", e);
            return "添加标签失败：" + e.getMessage();
        }
    }
}
