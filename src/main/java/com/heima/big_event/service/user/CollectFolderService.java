package com.heima.big_event.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.CollectFolder;

import java.util.List;

public interface CollectFolderService extends IService<CollectFolder> {
    //新增收藏分类文件夹
    CollectFolder addFolder(String name, Integer userId);
    //修改收藏分类文件夹名称
    CollectFolder updateFolder(Integer id, String name, Integer userId);
    //删除收藏分类文件夹，该文件夹下的收藏文章自动变为未分类
    void deleteFolder(Integer id, Integer userId);
    //获取当前用户的所有收藏分类文件夹列表
    List<CollectFolder> getFolderList(Integer userId);
}
