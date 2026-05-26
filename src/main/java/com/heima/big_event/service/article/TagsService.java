package com.heima.big_event.service.article;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.Tags;

public interface TagsService extends IService<Tags> {

    String addTagImpl(Tags tags);
}
