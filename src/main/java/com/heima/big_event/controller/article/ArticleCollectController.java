package com.heima.big_event.controller.article;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.VO.ArticleCollectUserListVO;
import com.heima.big_event.pojo.VO.ArticleCollectVO;
import com.heima.big_event.service.article.ArticleCollectService;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequestMapping("/article/collect")
public class ArticleCollectController {
    @Autowired
    private ArticleCollectService articleCollectService;

    //收藏/取消收藏接口
    @PostMapping("/toggle")
    @RequirePermission(value = "/article/collect/toggle", checkPermission = false)
    public Result articleCollectToggle(Integer articleId){
        Integer userId = ThreadLocalUtil.getUserId();
        ArticleCollectVO vo = articleCollectService.toggleCollect(articleId, userId);
        return Result.success(vo);
    }

    //检查当前用户是否收藏
    @GetMapping("/check")
    @RequirePermission(value = "/article/collect/check", checkPermission = false)
    public Result checkUserArticleCollect(Integer articleId){
        Integer userId = ThreadLocalUtil.getUserId();
        boolean b = articleCollectService.checkUserArticleCollectImpl(articleId, userId);
        return Result.success(b);
    }

    //我的收藏列表
    @GetMapping("/user/list")
    @RequirePermission("/article/collect/user/list")
    public Result getMyCollectArticleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize){
        Integer userId = ThreadLocalUtil.getUserId();

        ArticleCollectUserListVO articleCollectUserListVO = articleCollectService.MyCollectArticleListImpl(pageNum, pageSize, userId);
        return Result.success(articleCollectUserListVO);
    }
}
