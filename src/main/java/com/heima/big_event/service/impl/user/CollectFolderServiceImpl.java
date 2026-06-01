package com.heima.big_event.service.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleCollectMapper;
import com.heima.big_event.mapper.category.CollectFolderMapper;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.CollectFolder;
import com.heima.big_event.service.user.CollectFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectFolderServiceImpl extends ServiceImpl<CollectFolderMapper, CollectFolder> implements CollectFolderService {

    @Autowired
    private CollectFolderMapper collectFolderMapper;

    @Autowired
    private ArticleCollectMapper articleCollectMapper;

    //新增收藏分类文件夹
    @Override
    public CollectFolder addFolder(String name, Integer userId) {
        CollectFolder folder = new CollectFolder();
        folder.setUserId(userId);
        folder.setName(name);
        collectFolderMapper.insert(folder);
        return folder;
    }

    //修改收藏分类文件夹名称，校验归属权
    @Override
    public CollectFolder updateFolder(Integer id, String name, Integer userId) {
        CollectFolder folder = collectFolderMapper.selectById(id);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException("文件夹不存在或无权操作");
        }
        folder.setName(name);
        collectFolderMapper.updateById(folder);
        return folder;
    }

    //删除收藏分类文件夹，将该文件夹下的收藏记录的folder_id置为NULL（变为未分类）
    @Transactional
    @Override
    public void deleteFolder(Integer id, Integer userId) {
        CollectFolder folder = collectFolderMapper.selectById(id);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException("文件夹不存在或无权操作");
        }
        LambdaUpdateWrapper<ArticleCollect> uw = new LambdaUpdateWrapper<>();
        uw.eq(ArticleCollect::getFolderId, id)
                .eq(ArticleCollect::getUserId, userId)
                .set(ArticleCollect::getFolderId, null);
        articleCollectMapper.update(null, uw);
        collectFolderMapper.deleteById(id);
    }

    //获取当前用户的所有收藏分类文件夹，按创建时间升序排列
    @Override
    public List<CollectFolder> getFolderList(Integer userId) {
        LambdaQueryWrapper<CollectFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectFolder::getUserId, userId).orderByAsc(CollectFolder::getCreateTime);
        return collectFolderMapper.selectList(wrapper);
    }
}
