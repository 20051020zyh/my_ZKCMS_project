package com.heima.big_event.controller.article;

import com.heima.big_event.mapper.article.TagsMapper;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.Tags;
import com.heima.big_event.service.article.ArticleTagsService;
import com.heima.big_event.service.article.TagsService;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/tags")
public class TagsController {
    @Autowired
    private ArticleTagsService articleTagsService;
    @Autowired
    private TagsService tagsService;
    @Autowired
    private TagsMapper tagsMapper;

    @GetMapping("/list")
    public Result<List<Tags>> getAllTags() {
        List<Tags> tags = tagsService.list();
        return Result.success(tags);
    }

    @PostMapping("/add")
    @RequirePermission("/tags/add")
    public Result addtag(@RequestBody Tags tags){
        if (tags.getSort() == null){
            tags.setSort(0);
        }

        String s = tagsService.addTagImpl(tags);
        if ("新增标签成功".equals(s)) {
            return Result.success("新增标签成功");
        } else {
            return Result.error("添加标签失败");
        }
    }

    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotTags(
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> hotTags = tagsMapper.selectHotTags(limit);
        return Result.success(hotTags);
    }
}
