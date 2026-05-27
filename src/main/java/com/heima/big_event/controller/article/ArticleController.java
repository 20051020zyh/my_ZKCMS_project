package com.heima.big_event.controller.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.pojo.Result;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import com.heima.big_event.pojo.VO.ArticleRankVO;
import com.heima.big_event.pojo.VO.ArticleStatsVO;
import com.heima.big_event.pojo.VO.ArticleTrendVO;
import com.heima.big_event.pojo.dto.AddArticleTagDTO;
import com.heima.big_event.pojo.dto.ArticleStatusDTO;
import com.heima.big_event.pojo.dto.BatchUpdateStatusDTO;
import com.heima.big_event.service.dataStatistics.DailyStatService;
import com.heima.big_event.service.dataStatistics.VisitLogService;
import com.heima.big_event.service.article.ArticleLikeService;
import com.heima.big_event.service.article.ArticleService;
import com.heima.big_event.service.category.CategoryService;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/article")
@Validated
public class ArticleController {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ArticleLikeService articleLikeService;
    @Autowired
    private VisitLogService visitLogService;
    @Autowired
    private DailyStatService dailyStatService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;

    //新增文章
    @PostMapping("/add")
    @RequirePermission("/article/add")
    public Result add(@Validated @RequestBody AddArticleTagDTO addArticleTagDTO){
        articleService.addArticle(addArticleTagDTO);
        return Result.success();
    }
    //文章列表(条件分页)
    @GetMapping("/pageList")
    public Result<Page<Article>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            //required = false,因为categoryId不是必须传入的,如果当没有传入的时候也不会阻止程序运行
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String keyword){
        if (categoryName != null && !categoryName.isEmpty()){
            //说明此时用户执行的根据分类名筛选文章
            //用分类名查id
            Category category = categoryService.lambdaQuery()
                    .eq(Category :: getCategoryName , categoryName.trim())
                    .one();

            //然后把查到的id给原来的categoryId
            if (category != null){
                categoryId = category.getId();
            }else {
                categoryId = null;
            }
        }

        //保存原始state传给service（用于判断是否需要过滤status=0）
        String serviceState = state;

        //拼接缓存key(默认值代替null,保证key唯一)
        if (state == null) {
            state = "已发布";
        }
        String cateIdStr = (categoryId == null) ? "-1" : categoryId.toString();
        String stateStr = (serviceState == null) ? "已发布_status0" : state;
        String keywordStr = (keyword == null || keyword.trim().isEmpty()) ? "-" : keyword.trim();
        String key = "article:list:" + pageNum + ":" + pageSize + ":" + cateIdStr + ":" + stateStr + ":" + keywordStr;
        //拆分key:分别存文章列表和总条数
        String keyList = key + ":list";
        String keyTotal = key + "total";

        //先查redis
        //PageImpl不能序列化/反序列化,会报错
        Object cacheListObj = redisUtil.get(keyList);
        Object cacheTotalObj = redisUtil.get(keyTotal);
        //可以加一个安全获取办法,不直接强转
        if (cacheListObj != null && cacheTotalObj != null) {
            return buildPageFromCache(cacheListObj , cacheTotalObj ,pageNum ,pageSize);
        }
        //缓存击穿,加互斥锁
        String LockKey = "Lock:article:list:" + pageNum + ":" + pageSize + ":" + cateIdStr + ":" + stateStr + ":" + keywordStr;
        Page<Article> pd;
        boolean LockSuccess = false;
        try {
            //加锁
            LockSuccess = redisUtil.lock(LockKey , 30);
            if (!LockSuccess) {
                //没抢到锁
                Thread.sleep(200);
                return list(pageNum , pageSize , categoryId , serviceState,categoryName,keyword);
            }
            //加锁之后再去查一次redis
            Object cacheListAfterLock = redisUtil.get(keyList);
            Object cacheTotalAfterLock = redisUtil.get(keyTotal);
            if (cacheListAfterLock != null && cacheTotalAfterLock != null) {
                return buildPageFromCache(cacheListAfterLock , cacheTotalAfterLock ,pageNum ,pageSize);
            }
            //只有一个线程到这查数据库
            pd = articleService.list( pageNum , pageSize , categoryId , serviceState , keyword);
            //缓存穿透
            if (pd.getTotal() == 0) {
                //存-1为空标记
                redisUtil.set(keyList , "[]" , 60);
                redisUtil.set(keyTotal , "-1" , 60);
                return Result.success(new Page<>());
            }

            //缓存雪崩
            int Expire = 7200 + new Random().nextInt(300);
            //存列表和总条数,不是存Page对象
            try {
                //pd.getRecords():取出当前的文章数据列表
                String listJson = objectMapper.writeValueAsString(pd.getRecords());
                redisUtil.set(keyList , listJson , Expire);
                redisUtil.set(keyTotal , String.valueOf(pd.getTotal()) , Expire);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return Result.error("数据处理异常,问题出现在148行附近");
            }
        } catch (InterruptedException e) {
            log.error("异常" , e);
            return Result.error("系统繁忙,请稍后再试");
        } finally {
            //释放锁
            if (LockSuccess){
                redisUtil.unlock(LockKey);
            }
        }
        return Result.success(pd);
    }


    //获取文章详情
    @GetMapping("/detail")
//    @RateLimit(key = "detail" , count = 5 , time = 60)
    public Result detail(@RequestParam Integer id, HttpServletRequest request){
        try {
            articleService.addView(id);
        } catch (Exception e) {
            log.warn("浏览量统计失败 articleId={}, error={}", id, e.getMessage());
        }

        String key = "article:" + id;
        Article article = null;

        try {
            article = loadFromRedisOrDB(id, key);
        } catch (Exception e) {
            log.warn("Redis缓存读取异常,降级为数据库查询 articleId={}, error={}", id, e.getMessage());
            try {
                article = articleService.getById(id);
            } catch (Exception ex) {
                log.error("数据库查询也失败 articleId={}", id, ex);
                return Result.error("系统繁忙,请稍后再试");
            }
        }

        if (article == null) {
            return Result.error("没有找到对应的内容,id可能不存在");
        }

        //回收站的文章不可访问
        if (article.getIsDelete() == 1) {
            return Result.error("文章不存在或已删除");
        }

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        Integer userId = ThreadLocalUtil.getUserId();
        visitLogService.asyncSaveLog(Long.valueOf(id), userId, ip, userAgent);

        if ("草稿".equals(article.getState())) {
            if (userId == null || !userId.equals(article.getCreateUser())) {
                return Result.error("文章不存在或未发布");
            }
        }

        if (article.getCreateUser() != null) {
            User user = userMapper.selectById(article.getCreateUser());
            if (user != null) {
                article.setUsername(user.getUsername());
                article.setUser_pic(user.getUserPic());
            }
        }

        List<String> tagNames = articleService.getTagNamesByArticleId(article.getId());
        article.setTagNames(tagNames);

        return Result.success(article);
    }

    private Article loadFromRedisOrDB(Integer id, String key) {
        Article article = null;

        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null) {
            if ("".equals(cacheObj.toString().trim())) {
                return null;
            }
            try {
                article = objectMapper.readValue(cacheObj.toString(), Article.class);
                if ("草稿".equals(article.getState())) {
                    Integer currentUserId = ThreadLocalUtil.getUserId();
                    if (currentUserId == null || !currentUserId.equals(article.getCreateUser())) {
                        return null;
                    }
                }
                if (article.getIsDelete() == 1) {
                    return null;
                }
                return article;
            } catch (JsonProcessingException e) {
                log.warn("反序列化失败 key={}, value={}", key, cacheObj, e);
            }
        }

        String lockKey = "lock:article:" + id;
        boolean lockSuccess = false;
        try {
            lockSuccess = redisUtil.lock(lockKey, 5);

            if (!lockSuccess) {
                Thread.sleep(200);
                return loadFromRedisOrDB(id, key);
            }

            Object cacheObjAfterLock = redisUtil.get(key);
            if (cacheObjAfterLock != null) {
                if ("".equals(cacheObjAfterLock.toString().trim())) {
                    return null;
                }
                try {
                    article = objectMapper.readValue(cacheObjAfterLock.toString(), Article.class);
                    if ("草稿".equals(article.getState())) {
                        Integer currentUserId = ThreadLocalUtil.getUserId();
                        if (currentUserId == null || !currentUserId.equals(article.getCreateUser())) {
                            return null;
                        }
                    }
                    if (article.getIsDelete() == 1) {
                        return null;
                    }
                    return article;
                } catch (JsonProcessingException e) {
                    log.warn("反序列化失败 key={}, value={}", key, cacheObjAfterLock, e);
                }
            }

            article = articleService.getById(id);
            if (article == null || article.getIsDelete() == 1) {
                redisUtil.set(key, "", 60);
                return null;
            }

            int expire = 7200 + new Random().nextInt(300);
            try {
                String articleJson = objectMapper.writeValueAsString(article);
                redisUtil.set(key, articleJson, expire);
            } catch (JsonProcessingException e) {
                log.warn("序列化失败 key={}, article={}", key, article, e);
            }
            return article;

        } catch (InterruptedException e) {
            throw new RuntimeException("系统繁忙");
        } finally {
            if (lockSuccess) {
                try {
                    redisUtil.unlock(lockKey);
                } catch (Exception e) {
                    log.warn("释放锁失败 lockKey={}", lockKey, e);
                }
            }
        }
    }

    //更新文章
    @PutMapping("/update")
    @RequirePermission("/article/update")
    public Result update(@RequestBody Article article){
        //判断当前id是否存在
        Article byId = articleService.getById(article.getId());
        if (byId == null){
            return Result.error("当前id不存在");
        }
        articleService.updateById(article);
        //清理文章的详情缓存
        redisUtil.delete("article:" + article.getId());
        //清理所有列表缓存
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        //清理当前登录用户的文章列表缓存
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId != null) {
            redisUtil.delete("article:user:" + userId);
        }
        return Result.success();
    }

    //删除文章
    @DeleteMapping("delete")
    @RequirePermission("/article/delete")
    public Result delete(@RequestParam Integer id){
        Integer userId = ThreadLocalUtil.getUserId();
        //校验id本身是否合法
        if (id == null || id <= 0){
            return Result.error("请输入有效的id");
        }
        //加入分布式锁:避免并发删出问题
        String lockKey = "lock:article:delete:" + id;
        boolean lockSuccess = false;
        try {
            lockSuccess = redisUtil.lock(lockKey , 10);
            if (!lockSuccess){
                Thread.sleep(200);
                return delete(id);
            }
            //正常业务
            boolean byId1 = articleService.removeById(id);
            if (!byId1){
                return Result.error("没有找到对应的内容,id也许不存在");
            }
            //清理缓存(新增后列表缓存要更新)
            //新增文章后   文章列表没有更新新增的文章   要删除
            redisUtil.deleteByPattern("article:list:*");
            redisUtil.deleteByPattern("article:search:*");

            String userArticleCacheKey = "article:user:" + userId;
            redisUtil.delete(userArticleCacheKey);

            return Result.success();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lockSuccess){
                redisUtil.unlock(lockKey);
            }
        }
    }
    private Result<Page<Article>> buildPageFromCache(
            Object listObj, Object totalObj,
            Integer pageNum, Integer pageSize) {
        if ("-1".equals(totalObj.toString().trim()))
            return Result.success(new Page<>(pageNum, pageSize, 0));

        try {
            List<Article> records = objectMapper.readValue(listObj.toString(),
                    new TypeReference<>() {});
            Long total = Long.parseLong(totalObj.toString());
            Page<Article> page = new Page<>(pageNum, pageSize, total);
            page.setRecords(records);
            enrichArticlesWithUserInfo(records);
            return Result.success(page);
        } catch (Exception e) {
            log.warn("缓存反序列化失败", e);
            return null; // 触发降级查库
        }
    }

    //新增接口,全文关键词搜索
    @GetMapping("/search")
    @RequirePermission("/article/search")
    public Result<List<Article>> keywordSearch( @RequestParam String keyword){
        //判断传入的参数是不是空的
        //StringUtils.hasText(keyword)可以直接同时判断
        //不为null,不是""不是纯空格/制表符/换行符
        if (!StringUtils.hasText(keyword)){
            return Result.error("输入格式错误,请重新输入");
        }
        List<Article> list = articleService.keywordSearchImpl(keyword);

        //判断是不是非空字符
        if (list.isEmpty()){
            return Result.error("没有找到对应的内容(空字符)");
        }

        return Result.success(list);
    }

    //新增接口,批量操作状态
    @PostMapping("/updateStatus")
    @RequirePermission("/article/updateStatus")
    public Result batchOperation(@RequestBody ArticleStatusDTO dto){
        //基础参数校验
        if (dto.getArticleId() == null || dto.getArticleId().isEmpty()){
            return Result.error("请至少选择一篇文章进行操作");
        }
        if (dto.getState() == null || !("草稿".equals(dto.getState()) || "已发布".equals(dto.getState()))) {
            return Result.error("状态参数不合法");
        }
        //移除集合里的null数据
//        articleId.removeIf(Objects:: isNull);
        //接下来调用业务层
        articleService.batchUpdateState(dto.getArticleId() , dto.getState());
        //操作完成
        return Result.success();

    }

    //新增接口 , 根据分类id获取文章详情
    @GetMapping("/list/by-category")
    public Result<List<Article>> listByCategoryId(@RequestParam(required = false , defaultValue = "0") Integer categoryId){
        //创建构造器
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        //创建sql语句

        wrapper.eq(Article :: getState , "已发布");
        wrapper.eq(Article::getIsDelete, 0);

        if (categoryId != null && categoryId > 0){

            wrapper.eq(Article :: getCategoryId , categoryId);
        }
        //按照发布时间降序排列
        wrapper.orderByDesc(Article::getUpdateTime);

        //执行sql语句,然后返回到一个集合里面丢回给前端
        List<Article> articleList = articleService.list(wrapper);

        return Result.success(articleList);
    }

    //获取当前登录用户创建的所有文章列表（分页）
    @GetMapping("/user/list")
    @RequirePermission("/article/user/list")
    public Result<Page<Article>> userArticleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer userId = ThreadLocalUtil.getUserId();

        // Redis key 按页缓存
        String key = "article:user:" + userId + ":p" + pageNum + ":s" + pageSize;
        Page<Article> pageResult = null;

        // 先查Redis
        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null) {
            if ("".equals(cacheObj.toString().trim())) {
                Page<Article> emptyPage = new Page<>(pageNum, pageSize, 0);
                return Result.success(emptyPage);
            }
            try {
                pageResult = objectMapper.readValue(cacheObj.toString(), new TypeReference<Page<Article>>() {});
                return Result.success(pageResult);
            } catch (JsonProcessingException e) {
                log.warn("用户文章列表分页反序列化失败 key={}", key, e);
            }
        }

        // 缓存击穿：互斥锁重建缓存
        String lockKey = "lock:article:user:" + userId + ":p" + pageNum + ":s" + pageSize;
        boolean lockSuccess = false;
        try {
            lockSuccess = redisUtil.lock(lockKey, 5);
            if (!lockSuccess) {
                Thread.sleep(200);
                return userArticleList(pageNum, pageSize);
            }

            // 双重检查
            Object cacheObjAfterLock = redisUtil.get(key);
            if (cacheObjAfterLock != null) {
                try {
                    pageResult = objectMapper.readValue(cacheObjAfterLock.toString(), new TypeReference<Page<Article>>() {});
                    return Result.success(pageResult);
                } catch (JsonProcessingException e) {
                    log.warn("用户文章列表分页反序列化失败(锁后) key={}", key, e);
                }
            }

            // 查数据库（分页）
            LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Article::getCreateUser, userId);
            queryWrapper.eq(Article::getIsDelete, 0);
            queryWrapper.orderByDesc(Article::getCreateTime);
            pageResult = articleService.page(new Page<>(pageNum, pageSize), queryWrapper);

            // 空值缓存
            if (pageResult.getTotal() == 0) {
                try {
                    redisUtil.set(key, "", 60);
                } catch (Exception ignored) {}
                return Result.success(pageResult);
            }

            // 缓存：随机过期时间 7200~7500秒
            int expire = 7200 + new Random().nextInt(300);
            try {
                String listJson = objectMapper.writeValueAsString(pageResult);
                redisUtil.set(key, listJson, expire);
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存序列化失败 key={}", key, e);
            }

        } catch (InterruptedException e) {
            return Result.error("系统繁忙，请稍后再试");
        } finally {
            if (lockSuccess) {
                redisUtil.unlock(lockKey);
            }
        }
        return Result.success(pageResult);
    }

    //热门文章列表
    @GetMapping("/hot/list")
    public Result<Page<Article>> getHotArticleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize){
        Page<Article> page = articleService.lambdaQuery()
                .eq(Article :: getState , "已发布")
                .eq(Article::getIsHot , 1)
                .orderByDesc(Article :: getLikeCount)//点赞权重
                .orderByDesc(Article :: getViewCount)//浏览权重
                .orderByDesc(Article ::getCreateTime)//创建时间
                .page(new Page<>(pageNum , pageSize));

        enrichArticlesWithUserInfo(page.getRecords());

        return Result.success(page);
    }

    //精选文章接口
    @GetMapping("/best/list")
    public Result<Page<Article>> getBestList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize){
        Page<Article> page = articleService.lambdaQuery()
                .eq(Article :: getState , "已发布")
                .eq(Article :: getIsBest , 1)
                .orderByDesc(Article :: getUpdateTime)
                .page(new Page<>(pageNum , pageSize));

        enrichArticlesWithUserInfo(page.getRecords());

        return Result.success(page);
    }

    //点赞/取消点赞接口
    @PostMapping("/like/toggle")
    @RequirePermission(value = "/article/like/toggle", checkPermission = false)
    public Result articleLikeToggle(Integer articleId){
        Integer userId = ThreadLocalUtil.getUserId();
        ArticleLikeVO articleLikeVO = articleService.toggleLike(articleId, userId);
        return Result.success(articleLikeVO);
    }

    //检查当前用户是否已点赞
    @GetMapping("/like/check")
    @RequirePermission(value = "/article/like/check", checkPermission = false)
    public Result checkUserArticleLike(Integer articleId){
        Integer userId = ThreadLocalUtil.getUserId();
        boolean a = articleService.checkUserArticleLikeImpl(articleId, userId);
        return Result.success(a);
    }


    //定时发布文章
    @PostMapping("/schedule")
    @RequirePermission("/article/schedule")
    public Result schedule(@RequestBody Article article){
        Integer userId = ThreadLocalUtil.getUserId();
        article.setCreateUser(userId);
        boolean b = articleService.schedulePublish(article);
        if (b){
            return Result.success("定时发布设置成功");
        } else {
            return Result.error("定时发布设置失败");
        }
    }


    //移入回收站
    @PostMapping("/trash")
    @RequirePermission("/article/trash")
    public Result getTrash(@RequestParam Integer id){
        //根据文章的id进行更新
        articleService.trashImpl(id);
        //清理缓存
        redisUtil.delete("article:" + id);
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId != null) {
            redisUtil.delete("article:user:" + userId);
        }
        return Result.success();
    }

    //获取回收站的文章列表
    @GetMapping("/trash/look")
    @RequirePermission("/article/trash/look")
    public Result<List<Article>> getTrashLook(){
        //显示当前登录用户的回收站文章(isdelete = 1)
        Integer userId = ThreadLocalUtil.getUserId();
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsDelete , 1)
                .eq(Article::getCreateUser , userId)
                .orderByDesc(Article::getDeleteTime);

        List<Article> list = articleService.list(wrapper);
        return Result.success(list);
    }


    //回收站的恢复
    @PutMapping("/trash/recover")
    @RequirePermission("/article/trash/recover")
    public Result getTrashRecover(@RequestParam Integer id ){
        //根据传入的文章id集合进行更新
        articleService.TrashRecoverImpl(Collections.singletonList(id));
        //清理缓存
        redisUtil.delete("article:" + id);
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId != null) {
            redisUtil.delete("article:user:" + userId);
        }
        return Result.success();
    }


    //永久删除
    @DeleteMapping("/trash/forever")
    @RequirePermission("/article/trash/forever")
    public Result trashForever(@RequestParam Integer id){
        articleService.trashForeverImpl(Collections.singletonList(id));
        //清理缓存
        redisUtil.delete("article:" + id);
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId != null) {
            redisUtil.delete("article:user:" + userId);
        }
        return Result.success("永久删除成功");
    }

    //管理员批量上下架文章
    @PostMapping("/batch/updateStatus")
    @RequirePermission("/article/batch/updateStatus")
    public Result batchUpdateStatus(@RequestBody BatchUpdateStatusDTO dto){

         if (dto.getIds() == null || dto.getIds().isEmpty() || dto.getStatus() == null){
             return Result.error("参数不能为空");
         }
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
         wrapper.in(Article::getId , dto.getIds());
         wrapper.set(Article::getStatus , dto.getStatus());
         articleService.update(wrapper);

        //清除文章列表缓存
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        //清除单篇文章的详情缓存
        for (Integer id : dto.getIds()) {
            redisUtil.delete("article:" + id);
        }

         return Result.success("批量操作成功");
    }

    //管理员批量修改文章分类
    @PostMapping("/batch/updateCategoryId")
    @RequirePermission("/article/batch/updateCategoryId")
    public Result batchUpdateCategoryId(@RequestBody Map<String , Object> params){
        List<Integer> ids = (List<Integer>) params.get("ids");
        Integer categoryId = (Integer) params.get("categoryId");
        if (ids == null || ids.isEmpty() || categoryId == null){
            return Result.error("参数不能为空");
        }

        if (!articleService.categoryExists(categoryId)) {
            return Result.error("分类ID不存在，请检查参数");
        }

        //批量修改分类
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Article::getId , ids);
        wrapper.set(Article::getCategoryId , categoryId);
        articleService.update(wrapper);
        return Result.success("批量操作成功");
    }

    //管理员批量删除文章(逻辑删除)
    @PostMapping("/batch/delete")
    @RequirePermission("/article/batch/delete")
    public Result batchDelete(
            @RequestBody List<Integer> ids){
        if (ids == null || ids.isEmpty()){
            return Result.error("请先选择要删除的文章");
        }
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Article::getId , ids);
        wrapper.set(Article::getIsDelete , 1);
        articleService.update(wrapper);
        return Result.success("批量删除成功");
    }

    //文章排行接口
    //@param type 排行类型:view 阅读量 , like - 点赞数 - collect 收藏数
    //@param limit 取前N条数据,默认10
    @GetMapping("/rank")
    public Result<List<ArticleRankVO>> getArticleRank(
            @RequestParam(defaultValue = "view") String type,
            @RequestParam(defaultValue = "10") Integer limit){
        return Result.success(articleService.getRankList(type , limit));
    }

    //近7天/30天的访问趋势
    @GetMapping("/trend")
    @RequirePermission(value = "/article/trend", checkPermission = false)
    public Result<ArticleTrendVO> getArticleTrend(
            @RequestParam(defaultValue = "7") Integer days){
        if (!days.equals(7) && !days.equals(30)){
            return Result.error("仅支持查询7天或30天的数据");
        }
        return Result.success(dailyStatService.getTrend(days));
    }

    private void enrichArticlesWithUserInfo(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;

        List<Integer> userIds = articles.stream()
                .map(Article::getCreateUser)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (userIds.isEmpty()) return;

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Integer, User> userMap = users.stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (Article article : articles) {
            if (article.getUsername() != null) continue;
            User user = userMap.get(article.getCreateUser());
            if (user != null) {
                article.setUsername(user.getUsername());
                article.setUser_pic(user.getUserPic());
            }
        }
    }

    @GetMapping("/stats")
    @RequirePermission(value = "/article/stats", checkPermission = false)
    public Result<ArticleStatsVO> getArticleStats() {
        ArticleStatsVO vo = new ArticleStatsVO();
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        vo.setTotalArticles(articleMapper.selectCount(wrapper));
        LambdaQueryWrapper<Article> publishedWrapper = new LambdaQueryWrapper<>();
        publishedWrapper.eq(Article::getState, "已发布").eq(Article::getIsDelete, 0);
        vo.setPublishedCount(articleMapper.selectCount(publishedWrapper));
        LambdaQueryWrapper<Article> draftWrapper = new LambdaQueryWrapper<>();
        draftWrapper.eq(Article::getState, "草稿").eq(Article::getIsDelete, 0);
        vo.setDraftCount(articleMapper.selectCount(draftWrapper));
        vo.setTotalViews(articleMapper.sumViewCount());
        vo.setTotalLikes(articleMapper.sumLikeCount());
        vo.setTotalCollects(articleMapper.sumCollectCount());
        return Result.success(vo);
    }
}

