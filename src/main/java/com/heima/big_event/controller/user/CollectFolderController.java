package com.heima.big_event.controller.user;

import com.heima.big_event.pojo.CollectFolder;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.service.user.CollectFolderService;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/collect/folder")
public class CollectFolderController {

    @Autowired
    private CollectFolderService collectFolderService;

    //新增收藏分类文件夹
    @PostMapping("/add")
    @RequirePermission(value = "/collect/folder/add", checkPermission = false)
    public Result<CollectFolder> addFolder(@RequestParam String name) {
        Integer userId = ThreadLocalUtil.getUserId();
        CollectFolder folder = collectFolderService.addFolder(name, userId);
        return Result.success(folder);
    }

    //修改收藏分类文件夹名称
    @PutMapping("/update")
    @RequirePermission(value = "/collect/folder/update", checkPermission = false)
    public Result<CollectFolder> updateFolder(@RequestParam Integer id, @RequestParam String name) {
        Integer userId = ThreadLocalUtil.getUserId();
        CollectFolder folder = collectFolderService.updateFolder(id, name, userId);
        return Result.success(folder);
    }

    //删除收藏分类文件夹（文件夹内的收藏文章变为未分类）
    @DeleteMapping("/delete")
    @RequirePermission(value = "/collect/folder/delete", checkPermission = false)
    public Result deleteFolder(@RequestParam Integer id) {
        Integer userId = ThreadLocalUtil.getUserId();
        collectFolderService.deleteFolder(id, userId);
        return Result.success("删除成功");
    }

    //获取当前用户的收藏分类文件夹列表
    @GetMapping("/list")
    @RequirePermission(value = "/collect/folder/list", checkPermission = false)
    public Result<List<CollectFolder>> getFolderList() {
        Integer userId = ThreadLocalUtil.getUserId();
        List<CollectFolder> list = collectFolderService.getFolderList(userId);
        return Result.success(list);
    }
}
