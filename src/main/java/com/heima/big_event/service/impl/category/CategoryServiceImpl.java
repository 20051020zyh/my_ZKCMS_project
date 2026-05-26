package com.heima.big_event.service.impl.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.category.CategoryMapper;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.service.category.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper , Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;


    //分类列表
    @Override
    public List<Category> list(Integer userId) {
        //使用LambdaQueryWrapper构建查询条件
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category :: getCreateUser , userId);

        //把查询的到的结果返回
        //selectList,这个方法就是用来按条件查询列表的
        return categoryMapper.selectList(wrapper);
    }


    //获取分类列表详情
    @Override
    public List<Category> detail(Integer id) {
        //根据传入的id进行查询
        LambdaQueryWrapper<Category> get = new LambdaQueryWrapper<>();
        get.eq(Category :: getId , id);
        return categoryMapper.selectList(get);
    }

    //获取所有分类名
    @Override
    public List<Map<String , Object>> getAllSimpleCategoryImpl(){
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Category::getId , Category::getCategoryName);

        List<Category> categoryList = this.list(wrapper);
        //categoryList的类型是category的,要转换成string类型
        return categoryList.stream()
                .map(c ->{
                    //直接用Map双列集合
                    Map<String , Object> map = new HashMap<>();
                    map.put("id" , c.getId());
                    map.put("name" , c.getCategoryName());
                    return map;
                })
                .toList();


    }
}
