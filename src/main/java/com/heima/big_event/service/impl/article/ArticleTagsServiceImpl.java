package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.article.ArticleTagsMapper;
import com.heima.big_event.pojo.ArticleTag;
import com.heima.big_event.service.article.ArticleTagsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArticleTagsServiceImpl extends ServiceImpl<ArticleTagsMapper , ArticleTag> implements ArticleTagsService {

}
