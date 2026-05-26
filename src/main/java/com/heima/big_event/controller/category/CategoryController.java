package com.heima.big_event.controller.category;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.validation.AddGroup;
import com.heima.big_event.pojo.validation.UpdateGroup;
import com.heima.big_event.service.category.CategoryService;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

//文章分类控制层
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    //新增文章分类
    @PostMapping("/add")
    @RequirePermission("category/add")
    //上面那个是类别级的,每个方法之前要加方法级别,不然spring扫描会默认忽略
    public Result add(@Validated(AddGroup.class) @RequestBody Category category){
        //因为前端传过来的属性值没有创建者id,所以现在要补充
        System.out.println("【进入add方法】");
        Integer userId = ThreadLocalUtil.getUserId();
        System.out.println("userId = " + userId); // 关键！打印用户ID
        category.setCreateUser(userId);
        System.out.println("category.getCreateUser() = " + category.getCreateUser()); // 确认set进去了
        categoryService.save(category);
        //清理缓存
        redisUtil.delete("category:user:" + userId);
        return Result.success();
    }

    //获取用户创建的分类列表
    @GetMapping("/get/user/list")
    @RequirePermission("category/get/user/list")
    public Result<List<Category>> list(){
        //获取所有分类，不限制用户
        List<Category> categoryList = categoryService.list();
        return Result.success(categoryList);
    }


    //获取分类列表详情
    @GetMapping("/detail")
    @RequirePermission("category/detail")
    public Result<List<Category>> detail(@RequestParam Integer id){
        String key = "category:id:" + id;
        List<Category> categoryList = null;
        //如果redis有数据那么直接返回
        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null){
            //反序列化为Category对象
            try {
                categoryList = objectMapper.readValue(cacheObj.toString(), new TypeReference<List<Category>>() {
                });
                return Result.success(categoryList);
            } catch (JsonProcessingException e) {
                //反序列化失败,打印堆栈信息,继续走数据库查询
                log.warn("Redis 缓存反序列化失败，已跳过缓存直接查询数据库，缓存key：{}", key);
            }
        }
        //缓存击穿:加互斥锁重建缓存
        String lockKey = "lock:category:" + id;
        boolean lockSuccess = false;
        try {
            //尝试加锁
            lockSuccess = redisUtil.lock(lockKey , 30);

            //如果没抢到锁
            if (!lockSuccess) {
                //等200毫秒再试
                Thread.sleep(200);
                return detail(id);
            }
            //高并发下更好的解决办法:加锁之后再去查一次redis
            //避免多个线程去查数据库
            //加入第一个用户查到了,那么这次再查就直接又去redis查了,数据库就不会崩
            Object cacheObjAfterLock = redisUtil.get(key);
            if (cacheObjAfterLock != null){
                try {
                    categoryList = objectMapper.readValue(cacheObjAfterLock.toString(), new TypeReference<List<Category>>() {
                    });
                    return Result.success(categoryList);
                } catch (JsonProcessingException e) {
                    log.warn("Redis 缓存反序列化失败，已跳过缓存直接查询数据库，缓存key：{}", key);
                }
            }

            //只有一个线程(用户)走到这,去查数据库
            //redis那么就去查数据库
            categoryList = categoryService.detail(id);
            //如果数据库也没有,加入缓存穿透
            //MP查询列表不会返回null,只会返回空集合
            if (CollectionUtils.isEmpty(categoryList)){
                try {
                    //Collections.emptyList()是空集合,redis存入的是json字符使用要转换一下
                    String emptyListJson = objectMapper.writeValueAsString(Collections.emptyList());
                    redisUtil.set(key ,emptyListJson , 60);
                } catch (JsonProcessingException e){
                    log.warn("Redis 缓存反序列化失败，已跳过缓存直接查询数据库，缓存key：{}", key, e);
                }
                return Result.success(Collections.emptyList());
            }
            //缓存雪崩
            int expire = 3600 + new Random().nextInt(300);
            //数据库如果有的话,将category对象序列化为JSON字符串存入redis
            try {
                String CategoryJson = objectMapper.writeValueAsString(categoryList);
                redisUtil.set(key , CategoryJson , expire);
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存序列化失败，已跳过缓存直接查询数据库，缓存key：{}", key, e);
            }
        } catch (InterruptedException e) {
            return Result.error("系统繁忙,请稍后再试");
        }
        finally {
            //无论如何都要释放锁
            //只有加锁成功了,才释放锁
            if (lockSuccess){
                redisUtil.unlock(lockKey);
            }
        }
        return Result.success(categoryList);
    }

    //更新文章分类内容
    @PutMapping("/update")
    @RequirePermission("category/update")
    public Result update(@Validated(UpdateGroup.class) @RequestBody Category category){
        //把传入的id进行判断这个id是否存在
        Integer userId = ThreadLocalUtil.getUserId();
        Category oldcategoryid = categoryService.getById(category.getId());

        if (oldcategoryid == null){
            return Result.error("id不存在,更新失败");
        }
        //走到这说明id是存在的
        categoryService.updateById(category);
        //清理列表缓存
        redisUtil.delete("category:user:" + userId);
        //清理详情缓存
        redisUtil.delete("category:id:" + category.getId());
        return Result.success();
    }


    //删除文章分类内容
    @DeleteMapping("/delete")
    @RequirePermission("category/delete")
    public Result delete(@RequestParam Integer id){
        Integer userId = ThreadLocalUtil.getUserId();
        //检查id是否存在
        Category categoryid = categoryService.getById(id);

        if (categoryid == null){
            return Result.error("id不存在,删除失败");
        }
        //到这说明id是存在的
        categoryService.removeById(id);
        //清理列表缓存
        redisUtil.delete("category:user:" + userId);
        //清理详情缓存
        redisUtil.delete("category:id:" + id);
        return Result.success();
    }


    //新增导航栏显示文章分类列表(只显示分类名)
    @GetMapping("all/simple")
    public Result<List<Map<String , Object>>> getAllSimpleCategory(){
        //调用业务层返回所有分类名
        List<Map<String , Object>> categoryNames = categoryService.getAllSimpleCategoryImpl();
        return Result.success(categoryNames);


    }



}
