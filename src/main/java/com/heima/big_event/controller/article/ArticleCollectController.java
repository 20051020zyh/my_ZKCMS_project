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

    //收藏/取消收藏文章（folderId可选，传入则指定收藏到哪个文件夹）
    @PostMapping("/toggle")
    @RequirePermission(value = "/article/collect/toggle", checkPermission = false)
    public Result articleCollectToggle(Integer articleId, Integer folderId){
        Integer userId = ThreadLocalUtil.getUserId();
        ArticleCollectVO vo = articleCollectService.toggleCollect(articleId, userId, folderId);
        return Result.success(vo);
    }

    //检查当前用户是否已收藏该文章
    @GetMapping("/check")
    @RequirePermission(value = "/article/collect/check", checkPermission = false)
    public Result checkUserArticleCollect(Integer articleId){
        Integer userId = ThreadLocalUtil.getUserId();
        boolean b = articleCollectService.checkUserArticleCollectImpl(articleId, userId);
        return Result.success(b);
    }

    //获取当前用户的收藏列表（folderId>0查指定文件夹，folderId=-1查未分类，null查全部）
    @GetMapping("/user/list")
    @RequirePermission("/article/collect/user/list")
    public Result getMyCollectArticleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Integer folderId){
        Integer userId = ThreadLocalUtil.getUserId();

        ArticleCollectUserListVO articleCollectUserListVO = articleCollectService.MyCollectArticleListImpl(pageNum, pageSize, userId, folderId);
        return Result.success(articleCollectUserListVO);
    }

    //将已收藏的文章移入/移出指定文件夹
    @PutMapping("/moveFolder")
    @RequirePermission(value = "/article/collect/moveFolder", checkPermission = false)
    public Result moveFolder(@RequestParam Integer articleId, Integer folderId) {
        Integer userId = ThreadLocalUtil.getUserId();
        articleCollectService.moveFolder(articleId, userId, folderId);
        return Result.success();
    }
}
