package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.article.ArticleReportMapper;
import com.heima.big_event.pojo.ArticleReport;
import com.heima.big_event.service.article.ArticleReportService;
import org.springframework.stereotype.Service;

@Service
public class ArticleReportServiceImpl extends ServiceImpl<ArticleReportMapper , ArticleReport> implements ArticleReportService {
}
