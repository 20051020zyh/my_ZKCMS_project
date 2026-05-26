package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleLikeMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.article.ArticleTagsMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.*;
import com.heima.big_event.pojo.VO.ArticleLikeVO;
import com.heima.big_event.pojo.VO.ArticleRankVO;
import com.heima.big_event.pojo.dto.AddArticleTagDTO;
import com.heima.big_event.service.article.*;
import com.heima.big_event.service.category.CategoryService;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper , Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisUtil redisUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TagsService tagsService;
    @Autowired
    private ArticleTagsMapper articleTagsMapper;
    @Autowired
    private ArticleCommentService articleCommentService;
    @Autowired
    private ArticleTagsService articleTagsService;
    @Autowired
    private ArticleLikeService articleLikeService;
    @Autowired
    private ArticleCollectService articleCollectService;

    //新增文章(带标签)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addArticle(AddArticleTagDTO addArticleTagDTO){
        Integer userId = ThreadLocalUtil.getUserId();
        //正常业务:增加文章
        Article article= new Article();
        //把dto里面数据都复制到文章实体
        BeanUtils.copyProperties(addArticleTagDTO , article);
        article.setCreateUser(userId);
        save(article);

        //拿到刚才新增的文章id
        Integer articleId = article.getId();



        //==============  从dto取出标签名列表   =============
        List<String> tagNameList = addArticleTagDTO.getTagNameList();

        //清理缓存(新增后列表缓存要更新)
        //新增文章后   文章列表没有更新新增的文章   要删除
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
        String userArticleCacheKey = "article:user:" + userId;
        redisUtil.delete(userArticleCacheKey);

        if (CollectionUtils.isEmpty(tagNameList)){
            return;
        }

        //存放最终标签的id
        List<Long> finalTagList = new ArrayList<>();


        //===========    第三步    ===========
        for (String tageName : tagNameList){
            //去标签表根据名字查询有没有重复的
            Tags exitsTag = tagsService.lambdaQuery()
                    .eq(Tags::getName , tageName)
                    .one();

            if (exitsTag != null){
                //说明该标签存在
                finalTagList.add(exitsTag.getId());
            } else {
                //标签不存在
                Tags newTag = new Tags();
                newTag.setName(tageName);
                tagsService.save(newTag);
                //拿新增后的标签id
                finalTagList.add(newTag.getId());
            }
        }

        //把文章id和标签id存入关联表
        //先去重复的,防止前端传入了重复的标签
        List<Long> distinctTagIds = finalTagList.stream()
                .distinct()
                        .collect(Collectors.toList());

        //批量插入关联表
        if (!distinctTagIds.isEmpty()){
            articleTagsMapper.batchInsert(articleId , distinctTagIds);
        }
    }

    @Override
    public Page<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state, String keyword) {
        // 参数校验
        if (pageNum == null || pageNum <= 0) {
            throw new IllegalArgumentException("页码参数不能为空且必须大于0");
        }
        if (pageSize == null || pageSize <= 0) {
            throw new IllegalArgumentException("每页大小不能为空且必须大于0");
        }

        //创建分页对象
        Page<Article> page = new Page<>(pageNum , pageSize);

        //构建动态查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(categoryId != null , Article::getCategoryId , categoryId);

        if (state == null) {
            wrapper.eq(Article::getState, "已发布");
        } else if (!state.trim().isEmpty()) {
            wrapper.eq(Article::getState, state);
        }

        wrapper.eq(Article::getIsDelete , 0);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getContent, keyword));
        }

        //执行分页查询
        Page<Article> result = articleMapper.selectPage(page , wrapper);

        enrichArticlesWithUserInfo(result.getRecords());

        return result;
    }

    private void enrichArticlesWithUserInfo(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;

        List<Integer> userIds = articles.stream()
                .map(Article::getCreateUser)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            java.util.Map<Integer, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            for (Article article : articles) {
                if (article.getUsername() != null) continue;
                User user = userMap.get(article.getCreateUser());
                if (user != null) {
                    article.setUsername(user.getUsername());
                    article.setUser_pic(user.getUserPic());
                }
            }
        }

        enrichArticlesWithTags(articles);
    }

    private void enrichArticlesWithTags(List<Article> articles) {
        List<Integer> articleIds = articles.stream()
                .map(Article::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) return;

        List<java.util.Map<String, Object>> tagMaps = articleTagsMapper.selectTagNamesByArticleIds(articleIds);
        java.util.Map<Integer, List<String>> tagMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : tagMaps) {
            Integer articleId = ((Number) row.get("article_id")).intValue();
            String tagName = (String) row.get("name");
            tagMap.computeIfAbsent(articleId, k -> new java.util.ArrayList<>()).add(tagName);
        }

        for (Article article : articles) {
            List<String> tags = tagMap.get(article.getId());
            if (tags != null) {
                article.setTagNames(tags);
            }
        }
    }

    //浏览量自增
    @Override
    public void addView(Integer articleId){
        //生成redis key
        String key = "article:view:" + articleId;

        //10分钟内多次访问只算一次访问
        //加入10分钟内我来过访问一次了,然后又来一次,这是redis里面我的key还没有过期,
        //那么直接返回false,反之则true
        boolean firstVisit = stringRedisTemplate.opsForValue().setIfAbsent(key , "1" , 10 , TimeUnit.MINUTES);


        //redis高性能计数
        String countKey = "view:count:" + articleId;
        stringRedisTemplate.opsForValue().increment(countKey);


        //创建更新的规则
        if (Boolean.TRUE.equals(firstVisit)){
            LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();

            //指定更新哪篇文章的浏览量
            wrapper.eq(Article :: getId , articleId);

            //浏览量加1
            wrapper.setSql("view_count = view_count + 1");

            //执行更新
            //没有实体对象,传null
            articleMapper.update(null , wrapper);
        }

    }


    //同步redis到数据库
    @Override
    public void syncRedisToDB(Integer articleId){
        //从redis拿到这篇文章的总浏览量
        String countKey = "view:count:" + articleId;
        Integer count = Integer.parseInt(stringRedisTemplate.opsForValue().get(countKey));

        //一次性加到数据库
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article :: getId , articleId)
                .setSql("view_count = view_count + " + count);
        articleMapper.update(null , wrapper);

        //同步完清空redis
        stringRedisTemplate.delete(countKey);
    }


    //关键词搜索调用
    @Override
    public List<Article> keywordSearchImpl(String keyword) {
        //拼接key
        String key = "article:search:" + keyword;

        List<Article> article;
        //redis有的话就直接返回
        Object cacheObj = redisUtil.get(key);
        if (cacheObj != null) {
            //如果拿到的是""空字符串,直接说不存在
            if ("".equals(cacheObj.toString().trim())) {
                //直接返回空集合
                return new ArrayList<>();
            }
            //反序列化json字符串为article对象
            try {
                article = objectMapper.readValue(cacheObj.toString(), new TypeReference<List<Article>>() {
                });
                return article;
            } catch (JsonProcessingException e) {
                //反序列化失败,打印堆栈信息,继续走数据库查询
                log.warn("搜索关键词缓存反序列化失败 key:{}, value:{}", key, cacheObj);
                log.error("详细异常信息:" + e);
            }
        }

        //缓存击穿,加互斥锁重建缓存
        String lockKey = "lock:article:search:" + keyword;
        boolean lockSuccess = false;
        List<Article> list;
        try {
            //尝试加锁
            lockSuccess = redisUtil.lock(lockKey, 5);

            if (!lockSuccess) {
                //没抢到锁,等一下再试
                Thread.sleep(200);
                return keywordSearchImpl(keyword);
            }
            //抢到锁了再去查一次redis,避免数据库卡崩
            Object cacheObjAfterLock = redisUtil.get(key);
            if (cacheObjAfterLock != null) {
                if ("".equals(cacheObjAfterLock.toString().trim())) {
                    return new ArrayList<>();
                }
                try {
                    article = objectMapper.readValue(cacheObjAfterLock.toString(), new TypeReference<List<Article>>() {
                    });
                    return article;
                } catch (JsonProcessingException e) {
                    log.warn("反序列化失败 key={}, value={}", key, cacheObjAfterLock, e);
                }
            }

            //只有一个线程走到这里,查数据库
            //没有就去查数据库
            //创建条件构造器
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            //全局搜索(标题,内容)
            //这一段只是在构建查询的sql语句,并没有去查数据,只是写好了sql语句
            wrapper.eq(Article :: getState ,"已发布")
                            .eq(Article::getIsDelete , 0);

            wrapper.and(w ->
                    w.like(Article ::getTitle , keyword)
                            .or()
                            .like(Article ::getContent , keyword));
            //MP自带的list方法,查询所有符合条件的数据
            list = this.list(wrapper);
            //数据库也没有
            //MP永远不会返回null,只会返回空集合
            //CollectionUtils.isEmpty(list)判断list是不是空的
            if (CollectionUtils.isEmpty(list)) {
                //缓存穿透,把空值也存入redis,有效期自定义
                redisUtil.set(key, "", 60);
                throw new BusinessException(1003, "没有找到对应的内容(数据库),请重试");
            }
            //缓存雪崩,再过期时间随便加任意时间,防止同时过期
            int expire = 7200 + new Random().nextInt(300);
            //数据库有的话,将Article对象序列化为json字符串存入redis
            try {
                String articleJson = objectMapper.writeValueAsString(list);
                redisUtil.set(key, articleJson, expire);
            } catch (JsonProcessingException e) {
                log.warn("搜索关键词缓存序列化失败 key={}, article={}", key, list, e);
                throw new BusinessException(1001, "文章搜索缓存解析失败,请重试(1001是自定义错误码)");
            }

        } catch (InterruptedException e) {
            throw new BusinessException(1002, "当前请求人太多,请稍后再试(1002也是自定义错误码)");
        } finally {
            //无论如何都释放锁
            if (lockSuccess) {
                redisUtil.unlock(lockKey);
            }
        }
        return list;
    }

    //批量操作
    @Override
    public void batchUpdateState(List<Integer> articleId, String state){
        //创建构造器
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        //创建sql语句
        wrapper.in(Article :: getId , articleId)
                .set(Article :: getState , state);

        //执行sql语句
        this.update(wrapper);

        //清空缓存
        for (Integer id : articleId){
            redisUtil.delete("article:" + id);
        }

        //清理当前登录用户的文章列表缓存
        Integer userId = ThreadLocalUtil.getUserId();
        String userArticleCacheKey = "article:user:" + userId;
        redisUtil.delete(userArticleCacheKey);
        //清理文章列表的缓存
        redisUtil.deleteByPattern("article:list:*");
        redisUtil.deleteByPattern("article:search:*");
    }

    //点赞/取消点赞接口
    @Transactional
    @Override
    public ArticleLikeVO toggleLike(Integer articleId, Integer userId) {
        //检查文章是否存在,是否已发布
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getState().equals("草稿")) {
            throw new BusinessException("文章不存在或者未发布 , 无法点赞");
        }

        //查询用户是否点赞
        LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId);
        ArticleLike existLike = articleLikeMapper.selectOne(wrapper);

        boolean isLike;
        int newLikeCount;

        //如果点赞表里面有数据
        if (existLike != null) {
            //已点赞:取消点赞
            articleLikeMapper.deleteById(existLike.getId());
            //文章的点赞数-1
            LambdaUpdateWrapper<Article> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(Article::getId, articleId)
                    .setSql("like_count = like_count - 1");

            articleMapper.update(null, wrapper1);
            isLike = false;
        } else {
            //未点赞:新增点赞
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticleId(articleId);
            articleLike.setUserId(userId);
            articleLikeMapper.insert(articleLike);

            //文章表的点赞数+1
            LambdaUpdateWrapper<Article> wrapper1 = new LambdaUpdateWrapper<>();
            wrapper1.eq(Article::getId, articleId)
                    .setSql("like_count = like_count + 1");

            articleMapper.update(null, wrapper1);

            isLike = true;
        }

        //查询最新的点赞数
        ArticleLikeVO vo = new ArticleLikeVO();
//        LambdaQueryWrapper<Article> wrapper1 = new LambdaQueryWrapper<>();
//        wrapper1.eq(Article::getId, articleId)
//                //提取该文章的点赞数
//                .select(Article::getLikeCount);
//
//        Integer likeCount = articleMapper.selectObjs(wrapper1)
//                .stream()
//                .findFirst()
//                .map(obj -> (Integer) obj)
//                .orElse(0);//如果没有,默认返回0
        Article newArticle = articleMapper.selectById(articleId);
        vo.setLikeCount(newArticle.getLikeCount());
        vo.setIsLike(isLike);

        redisUtil.deleteByPattern("article:list:*");
        redisUtil.delete("article:" + articleId);

        return vo;
    }


    //检查当前用户是否点赞
    @Override
    public boolean checkUserArticleLikeImpl(Integer articleId, Integer userId){
         LambdaQueryWrapper<ArticleLike> wrapper = new LambdaQueryWrapper<>();
         wrapper.eq(ArticleLike :: getArticleId , articleId)
                 .eq(ArticleLike :: getUserId , userId);
         ArticleLike articleLike = articleLikeMapper.selectOne(wrapper);
         boolean is_like;
         //如果没有记录,说明没有点赞
         if (articleLike == null){
             is_like = false;
         }else {
             is_like = true;
         }
         return is_like;
     }


     //定时发布文章(保存为"草稿"状态)
     @Override
     public boolean schedulePublish(Article article){
        //不管状态是什么,有没有定时,都先存数据库
         boolean save = this.save(article);

         //清理缓存：让个人中心能看到刚新增的草稿
         if (save) {
             Integer userId = article.getCreateUser();
             if (userId != null) {
                 redisUtil.delete("article:user:" + userId);
             }
             redisUtil.deleteByPattern("article:list:*");
             redisUtil.deleteByPattern("article:search:*");
         }
         return save;
     }


    /**
     * 定时任务专用：查询多条条待发布的公告(状态为"草稿"+发布时间<=现在)
     * @return 多条待发布公告
     */
    @Override
    public List<Article> getScheduledArticleList(){
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getState,"草稿")
                .le(Article::getPublishTime , LocalDateTime.now());
        List<Article> articleList = this.list(wrapper);
        return articleList;
    }

    /**
     * 更新文章状态为已发布
     * @return 是否更新成功
     */
    @Override
    public boolean publishArticle(List<Article> articleList){
        //先把集合里面的每个对象的ID都放进一个新的集合里去
        List<Integer> AricleIdList = new ArrayList<>();
        for (Article article : articleList){
            Integer articleId = article.getId();
            AricleIdList.add(articleId);
        }

        //然后批量修改
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Article::getId , AricleIdList)
                .set(Article::getState , "已发布");
        boolean update = this.update(wrapper);

        //清理缓存：让主页能显示刚发布的文章
        if (update) {
            redisUtil.deleteByPattern("article:list:*");
            redisUtil.deleteByPattern("article:search:*");
            for (Article article : articleList) {
                redisUtil.delete("article:" + article.getId());
                Integer userId = article.getCreateUser();
                if (userId != null) {
                    redisUtil.delete("article:user:" + userId);
                }
            }
        }
        return update;
    }

    //移入回收站
    @Override
    public void trashImpl(Integer articleId){
        //根据传入的id进行isdelete字段更新
        if (articleId == null){
            throw new BusinessException("文章不存在");
        }
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getId , articleId)
                .set(Article::getIsDelete , 1)
                        .set(Article::getDeleteTime , LocalDateTime.now());

        articleMapper.update(null, wrapper);
    }


    //回收站恢复
    @Override
    public void TrashRecoverImpl(List<Integer> articleIdList){
        //根据传入的id集合进行更新
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Article::getId , articleIdList)
                .set(Article::getIsDelete , 0)
                .set(Article::getDeleteTime , null);

        this.update(null , wrapper);
    }


    //回收站永久删除
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trashForeverImpl(List<Integer> articleIdList){
        //先删除标签表对应的数据
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ArticleTag::getArticleId , articleIdList);
        articleTagsService.remove(wrapper);
        //删除articlecomment表
        LambdaQueryWrapper<ArticleComment> articleCommentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleCommentLambdaQueryWrapper.in(ArticleComment::getArticleId , articleIdList);
        articleCommentService.remove(articleCommentLambdaQueryWrapper);
        //删除articlelike表对应的数据
        LambdaQueryWrapper<ArticleLike> articleLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleLikeLambdaQueryWrapper.in(ArticleLike::getArticleId , articleIdList);
        articleLikeService.remove(articleLikeLambdaQueryWrapper);
        //删除articleCollect表对应的数据
        LambdaQueryWrapper<ArticleCollect> articleCollectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleCollectLambdaQueryWrapper.in(ArticleCollect::getArticleId , articleIdList);
        articleCollectService.remove(articleCollectLambdaQueryWrapper);

        //最后删除文章表的数据
        this.removeByIds(articleIdList);
    }


    //文章排行接口
    @Override
    public List<ArticleRankVO> getRankList(String type, Integer limit){
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getState, "已发布")
                .eq(Article::getStatus , 0)
                .eq(Article::getIsDelete , 0);

        //使用穿透
        switch (type){
            case "like":
                wrapper.orderByDesc(Article::getLikeCount);
                break;
            case "collect":
                wrapper.orderByDesc(Article::getCollectCount);
                break;
            default:
                wrapper.orderByDesc(Article::getViewCount);
        }

        // 用Page对象实现分页查询，避免直接拼接LIMIT
        Page<Article> page = new Page<>(1, limit);
        articleMapper.selectPage(page, wrapper);

        // 转换为VO
        return page.getRecords().stream()
                .map(article -> {
                    ArticleRankVO vo = new ArticleRankVO();
                    BeanUtils.copyProperties(article, vo);
                    return vo;
                }).collect(Collectors.toList());
    }


    @Override
    public boolean categoryExists(Integer categoryId) {
        return categoryService.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, categoryId)) > 0;
    }

    @Override
    public List<String> getTagNamesByArticleId(Integer articleId) {
        List<Integer> ids = java.util.Collections.singletonList(articleId);
        List<java.util.Map<String, Object>> tagMaps = articleTagsMapper.selectTagNamesByArticleIds(ids);
        return tagMaps.stream()
                .map(row -> (String) row.get("name"))
                .collect(Collectors.toList());
    }


}
