package com.heima.big_event.controller.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.big_event.mapper.comment.CommentReportMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.*;
import com.heima.big_event.pojo.VO.CommentReportVO;
import com.heima.big_event.service.article.ArticleCommentService;
import com.heima.big_event.service.comment.CommentLikeService;
import com.heima.big_event.service.comment.CommentReportService;
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
@RequestMapping("/comment/report")
@Validated
public class CommentReportController {
    @Autowired
    private CommentReportMapper commentReportMapper;
    @Autowired
    private CommentReportService commentReportService;
    @Autowired
    private ArticleCommentService articleCommentService;
    @Autowired
    private CommentLikeService commentLikeService;
    @Autowired
    private UserMapper userMapper;

    //评论举报提交
    @PostMapping("/add")
    @RequirePermission("comment/report/add")
    public Result reportComment(@RequestBody CommentReport commentReport) {
        Integer userId = ThreadLocalUtil.getUserId();
        CommentReport report = new CommentReport();
        report.setCommentId(commentReport.getCommentId());
        report.setReportType(commentReport.getReportType());
        report.setContent(commentReport.getContent());
        report.setImages(commentReport.getImages());
        report.setUserId(userId);
        report.setStatus(0);

        commentReportMapper.insert(report);

        return Result.success("举报成功");
    }


    //评论举报审核列表
    @GetMapping("/list")
    @RequirePermission("comment/report/list")
    public Result listCommentReport(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status){
        Page<CommentReport> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<CommentReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommentReport::getStatus , status);
        } else {
            wrapper.eq(CommentReport::getStatus , 0);
        }
        wrapper.orderByDesc(CommentReport::getCreateTime);

        Page<CommentReport> commentReportPage = commentReportMapper.selectPage(page, wrapper);

        Page<CommentReportVO> voPage = new Page<>();
        voPage.setTotal(commentReportPage.getTotal());
        voPage.setPages(commentReportPage.getPages());
        voPage.setCurrent(commentReportPage.getCurrent());
        voPage.setSize(commentReportPage.getSize());

        List<CommentReportVO> voList = new ArrayList<>();
        for (CommentReport r : commentReportPage.getRecords()) {
            CommentReportVO vo = new CommentReportVO();
            vo.setId(r.getId());
            vo.setCommentId(r.getCommentId());
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


    //评论举报审核接口(管理员)
    @PostMapping("/audit")
    @RequirePermission("comment/report/audit")
    public Result auditCommentReport(
            @RequestParam Integer id,
            @RequestParam Integer status
    ){
        //获取举报记录
        CommentReport commentReport = commentReportService.getById(id);
        if (commentReport == null){
            return Result.error("举报记录不存在");
        }

        //更新举报状态
        CommentReport report = new CommentReport();
        report.setId(id);
        report.setStatus(status);
        commentReportService.updateById(report);

        //判断审核的状态
        if (status == 1){
            //获取评论的id
            Integer commentId = commentReport.getCommentId();
            //删除评论表的该id以及以这个id为父级id的评论
            LambdaQueryWrapper<ArticleComment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticleComment::getParentId , commentId);
            articleCommentService.remove(wrapper);
            articleCommentService.removeById(commentId);
            //删除评论点赞表对应的数据
            LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLikeLambdaQueryWrapper.eq(CommentLike::getCommentId , commentId);
            commentLikeService.remove(commentLikeLambdaQueryWrapper);

            return Result.success("审核成功,该评论已删除");
        }
        return Result.success();

    }


}
