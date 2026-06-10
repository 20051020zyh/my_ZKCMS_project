package com.heima.big_event.dataseed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.ArticleLike;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.pojo.User;
import com.heima.big_event.service.article.ArticleCollectService;
import com.heima.big_event.service.article.ArticleLikeService;
import com.heima.big_event.service.article.ArticleService;
import com.heima.big_event.service.category.CategoryService;
import com.heima.big_event.service.user.UserService;
import com.heima.big_event.utils.Others.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 互动数据填充器
 * =====================
 * 为已填充的文章随机添加点赞和收藏记录，使数据更真实。
 *
 * 前置条件：先运行 ArticleDataSeeder 填充文章数据
 * 使用方式：在 IDE 中运行 seedInteractions() 方法
 */
@SpringBootTest
public class InteractionDataSeeder {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleLikeService articleLikeService;

    @Autowired
    private ArticleCollectService articleCollectService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisUtil redisUtil;

    private final Random random = new Random();

    /**
     * 为所有文章随机生成点赞和收藏数据
     */
    @Test
    public void seedInteractions() {
        System.out.println("========================================");
        System.out.println("互动数据填充器 - 开始执行");
        System.out.println("========================================");

        // 1. 动态获取爬虫相关分类ID（按名称查找，兼容不同环境的ID不同）
        List<String> targetCategoryNames = Arrays.asList("前端", "后端", "数据库", "人工智能", "开发工具", "阅读");
        List<Integer> targetCategoryIds = categoryService.lambdaQuery()
                .in(Category::getCategoryName, targetCategoryNames)
                .list()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        System.out.println("目标分类ID: " + targetCategoryIds);

        if (targetCategoryIds.isEmpty()) {
            System.out.println("[错误] 未找到爬虫相关分类，请先创建分类");
            return;
        }

        List<Article> articles = articleService.lambdaQuery()
                .eq(Article::getIsDelete, 0)
                .in(Article::getCategoryId, targetCategoryIds)
                .list();
        List<User> users = userService.list();

        if (articles.isEmpty()) {
            System.out.println("[错误] 没有文章数据，请先运行 ArticleDataSeeder");
            return;
        }
        if (users.isEmpty()) {
            System.out.println("[错误] 没有用户数据，请先创建用户");
            return;
        }

        List<Integer> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        System.out.println("文章总数: " + articles.size());
        System.out.println("用户总数: " + users.size());

        int totalLikes = 0;
        int totalCollects = 0;

        for (Article article : articles) {
            Integer articleId = article.getId();
            // 自己生成随机数，不依赖文章表的字段（因为可能没保存到数据库）
            int targetLikeCount = random.nextInt(100) + 10;  // 10-109个点赞
            int targetCollectCount = random.nextInt(30) + 3; // 3-32个收藏

            // 限制实际插入的记录数（避免数据量过大）
            int actualLikes = Math.min(targetLikeCount, userIds.size());
            int actualCollects = Math.min(targetCollectCount, userIds.size());

            // 随机选取用户进行点赞
            List<Integer> shuffledUserIds = new ArrayList<>(userIds);
            Collections.shuffle(shuffledUserIds, random);

            Set<Integer> likedUserIds = new HashSet<>();
            for (int i = 0; i < actualLikes && i < shuffledUserIds.size(); i++) {
                Integer userId = shuffledUserIds.get(i);
                likedUserIds.add(userId);

                // 检查是否已点赞
                long exists = articleLikeService.lambdaQuery()
                        .eq(ArticleLike::getArticleId, articleId)
                        .eq(ArticleLike::getUserId, userId)
                        .count();
                if (exists == 0) {
                    ArticleLike like = new ArticleLike();
                    like.setArticleId(articleId);
                    like.setUserId(userId);
                    like.setCreateTime(LocalDateTime.now()
                            .minusDays(random.nextInt(60))
                            .minusHours(random.nextInt(24)));
                    articleLikeService.save(like);
                    totalLikes++;
                }
            }

            // 随机选取用户进行收藏（从已点赞的用户中选一部分 + 其他用户）
            Collections.shuffle(shuffledUserIds, random);
            for (int i = 0; i < actualCollects && i < shuffledUserIds.size(); i++) {
                Integer userId = shuffledUserIds.get(i);

                // 检查是否已收藏
                long exists = articleCollectService.lambdaQuery()
                        .eq(ArticleCollect::getArticleId, articleId)
                        .eq(ArticleCollect::getUserId, userId)
                        .count();
                if (exists == 0) {
                    ArticleCollect collect = new ArticleCollect();
                    collect.setArticleId(articleId);
                    collect.setUserId(userId);
                    collect.setFolderId(null); // 未分类收藏
                    collect.setCreateTime(LocalDateTime.now()
                            .minusDays(random.nextInt(60))
                            .minusHours(random.nextInt(24)));
                    articleCollectService.save(collect);
                    totalCollects++;
                }
            }

            // 反写文章表的likeCount和collectCount，确保数据一致
            long actualLikeRecords = articleLikeService.lambdaQuery()
                    .eq(ArticleLike::getArticleId, articleId)
                    .count();
            long actualCollectRecords = articleCollectService.lambdaQuery()
                    .eq(ArticleCollect::getArticleId, articleId)
                    .count();
            article.setLikeCount((int) actualLikeRecords);
            article.setCollectCount((int) actualCollectRecords);
            articleService.updateById(article);

            // 进度显示
            int idx = articles.indexOf(article) + 1;
            if (idx % 20 == 0) {
                System.out.println("  进度: " + idx + "/" + articles.size()
                        + " (点赞: " + totalLikes + ", 收藏: " + totalCollects + ")");
            }
        }

        // 清理Redis缓存
        System.out.println("\n清理Redis缓存...");
        try {
            redisUtil.deleteByPattern("article:*");
            System.out.println("  Redis缓存已清理");
        } catch (Exception e) {
            System.out.println("  [警告] Redis清理失败: " + e.getMessage());
        }

        System.out.println("\n========================================");
        System.out.println("互动数据填充完成！");
        System.out.println("  新增点赞: " + totalLikes + " 条");
        System.out.println("  新增收藏: " + totalCollects + " 条");
        System.out.println("========================================");
    }

    /**
     * 清理所有互动数据（用于回滚）
     */
    @Test
    public void cleanupInteractions() {
        System.out.println("准备清理爬虫导入文章的互动数据...");

        // 动态获取爬虫相关分类ID
        List<String> targetCategoryNames = Arrays.asList("前端", "后端", "数据库", "人工智能", "开发工具", "阅读");
        List<Integer> targetCategoryIds = categoryService.lambdaQuery()
                .in(Category::getCategoryName, targetCategoryNames)
                .list()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        if (targetCategoryIds.isEmpty()) {
            System.out.println("未找到爬虫相关分类，无需清理");
            return;
        }

        // 只获取爬虫导入的文章
        List<Article> seededArticles = articleService.lambdaQuery()
                .eq(Article::getIsDelete, 0)
                .in(Article::getCategoryId, targetCategoryIds)
                .list();

        if (seededArticles.isEmpty()) {
            System.out.println("没有爬虫导入的文章，无需清理");
            return;
        }

        List<Integer> seededArticleIds = seededArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        // 删除这些文章的点赞记录
        long likeCount = articleLikeService.lambdaQuery()
                .in(ArticleLike::getArticleId, seededArticleIds)
                .count();
        articleLikeService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArticleLike>()
                .in(ArticleLike::getArticleId, seededArticleIds));

        // 删除这些文章的收藏记录
        long collectCount = articleCollectService.lambdaQuery()
                .in(ArticleCollect::getArticleId, seededArticleIds)
                .count();
        articleCollectService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArticleCollect>()
                .in(ArticleCollect::getArticleId, seededArticleIds));

        // 重置这些文章的likeCount和collectCount为0
        articleService.lambdaUpdate()
                .in(Article::getId, seededArticleIds)
                .set(Article::getLikeCount, 0)
                .set(Article::getCollectCount, 0)
                .update();

        System.out.println("已清理 " + likeCount + " 条点赞, " + collectCount + " 条收藏");
        System.out.println("已重置 " + seededArticleIds.size() + " 篇爬虫文章的likeCount和collectCount");

        // 清理Redis
        try {
            redisUtil.deleteByPattern("article:*");
            System.out.println("Redis缓存已清理");
        } catch (Exception e) {
            System.out.println("Redis清理失败: " + e.getMessage());
        }
    }
}
