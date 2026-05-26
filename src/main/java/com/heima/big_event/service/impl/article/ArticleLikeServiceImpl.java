package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.article.ArticleLikeMapper;
import com.heima.big_event.pojo.ArticleLike;
import com.heima.big_event.service.article.ArticleLikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper , ArticleLike> implements ArticleLikeService {
}
