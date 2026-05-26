package com.heima.big_event.controller.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.mapper.article.ArticleReportMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleReport;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.VO.ArticleReportVO;
import com.heima.big_event.service.article.ArticleReportService;
import com.heima.big_event.service.article.ArticleService;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/article/report")
@Validated
public class ArticleReportController {
    @Autowired
    private ArticleReportMapper articleReportMapper;
    @Autowired
    private ArticleReportService articleReportService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private UserMapper userMapper;

    //文章举报提交
    @PostMapping("/add")
    @RequirePermission("/article/report/add")
    public Result reportArticle(@RequestBody ArticleReport articleReport){
        ArticleReport report = new ArticleReport();
        report.setArticleId(articleReport.getArticleId());  // 文章ID
        report.setReportType(articleReport.getReportType());// 类型
        report.setContent(articleReport.getContent());      // 描述
        report.setImages(articleReport.getImages());        // 图片
        report.setUserId(ThreadLocalUtil.getUserId());        // 举报人
        report.setStatus(0);                     // 待审核

        articleReportMapper.insert(report);

        return  Result.success("举报提交成功");
    }


    //文章举报审核列表
    @GetMapping("/list")
    @RequirePermission("/article/report/list")
    public Result listArticleReport(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status){
        Page<ArticleReport> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<ArticleReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ArticleReport::getStatus , status);
        } else {
            wrapper.eq(ArticleReport::getStatus , 0);
        }
        wrapper.orderByDesc(ArticleReport::getCreateTime);

        Page<ArticleReport> articleReportPage = articleReportMapper.selectPage(page, wrapper);

        Page<ArticleReportVO> voPage = new Page<>();
        voPage.setTotal(articleReportPage.getTotal());
        voPage.setPages(articleReportPage.getPages());
        voPage.setCurrent(articleReportPage.getCurrent());
        voPage.setSize(articleReportPage.getSize());

        List<ArticleReportVO> voList = new ArrayList<>();
        for (ArticleReport r : articleReportPage.getRecords()) {
            ArticleReportVO vo = new ArticleReportVO();
            vo.setId(r.getId());
            vo.setArticleId(r.getArticleId());
            vo.setReportType(r.getReportType());
            vo.setContent(r.getContent());
            vo.setImages(r.getImages());
            vo.setUserId(r.getUserId());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setUpdateTime(r.getUpdateTime());

            User user = userMapper.selectById(r.getUserId());
            vo.setUserName(user != null ? user.getUsername() : null);

            voList.add(vo);
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    //文章举报审核接口(管理员)
    @PostMapping("/audit")
    @RequirePermission("/article/report/audit")
    public Result auditArticleReport(
            @RequestParam Integer id,
            @RequestParam Integer status
    ){
        //获取这条举报记录
        ArticleReport articleReport = articleReportMapper.selectById(id);
        if (articleReport == null){
            return Result.error("举报记录不存在");
        }
        //更新举报的状态
        ArticleReport report = new ArticleReport();
        report.setId(id);
        report.setStatus(status);

        articleReportMapper.updateById(report);

        //如果审核通过(违规) --- 下架文章(文章status == 1)
        if (status == 1){
            Integer articleId = articleReport.getArticleId();
            LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Article::getId , articleId)
                    .set(Article::getStatus , 1);

            articleService.update(null , wrapper);
            return Result.success("审核成功,已下架该文章");
        }
        return Result.success();
    }
}
