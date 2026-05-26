package com.heima.big_event.service.category;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.big_event.pojo.Category;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {
    //文章分类列表查询(根据登录用户)
    List<Category> list(Integer userId);

    //获取文章详情(根据id)
    List<Category> detail(Integer id);

    //获取所有分类名
    List<Map<String , Object>> getAllSimpleCategoryImpl();
}
