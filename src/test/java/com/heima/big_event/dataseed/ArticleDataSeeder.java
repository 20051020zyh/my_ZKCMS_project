package com.heima.big_event.dataseed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.pojo.Tags;
import com.heima.big_event.pojo.User;
import com.heima.big_event.mapper.article.ArticleTagsMapper;
import com.heima.big_event.service.article.ArticleService;
import com.heima.big_event.service.article.TagsService;
import com.heima.big_event.service.category.CategoryService;
import com.heima.big_event.service.user.UserService;
import com.heima.big_event.utils.Others.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章数据填充器
 * =====================
 * 从 scripts/data/ 目录读取爬虫生成的JSON文件，
 * 通过Service层写入数据库，自动处理分类映射、用户分配、标签创建。
 *
 * 使用方式：
 *   1. 先运行 Python 爬虫脚本生成 JSON 文件
 *   2. 在 IDE 中运行本测试类的 seedArticles() 方法
 *   3. 完成后清理 Redis 缓存
 */
@SpringBootTest
public class ArticleDataSeeder {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private ArticleTagsMapper articleTagsMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // JSON文件路径（项目根目录/scripts/data/）
    private static final String DATA_DIR = "scripts/data";

    // JSON文件名 → 分类名 映射
    private static final Map<String, String> FILE_CATEGORY_MAP = new LinkedHashMap<>();
    static {
        FILE_CATEGORY_MAP.put("articles_frontend.json", "前端");
        FILE_CATEGORY_MAP.put("articles_backend.json", "后端");
        FILE_CATEGORY_MAP.put("articles_database.json", "数据库");
        FILE_CATEGORY_MAP.put("articles_ai.json", "人工智能");
        FILE_CATEGORY_MAP.put("articles_devtools.json", "开发工具");
        FILE_CATEGORY_MAP.put("articles_reading.json", "阅读");
    }

    // 虚拟作者用户名列表（对应分类）
    private static final Map<String, String> CATEGORY_AUTHOR_MAP = new LinkedHashMap<>();
    static {
        CATEGORY_AUTHOR_MAP.put("前端", "author_fe");
        CATEGORY_AUTHOR_MAP.put("后端", "author_be");
        CATEGORY_AUTHOR_MAP.put("数据库", "author_db");
        CATEGORY_AUTHOR_MAP.put("人工智能", "author_ai");
        CATEGORY_AUTHOR_MAP.put("开发工具", "author_tool");
        CATEGORY_AUTHOR_MAP.put("阅读", "author_read");
    }

    private final Random random = new Random();

    /**
     * 主方法：填充所有分类的文章
     */
    @Test
    public void seedArticles() {
        System.out.println("========================================");
        System.out.println("文章数据填充器 - 开始执行");
        System.out.println("========================================");

        // 1. 加载数据库中的分类和用户
        List<Category> categories = categoryService.list();
        Map<String, Integer> categoryNameToId = categories.stream()
                .collect(Collectors.toMap(Category::getCategoryName, Category::getId, (a, b) -> a));
        System.out.println("已有分类: " + categoryNameToId);

        List<User> users = userService.list();
        Map<String, Integer> usernameToId = users.stream()
                .collect(Collectors.toMap(User::getUsername, User::getId, (a, b) -> a));
        System.out.println("已有用户: " + usernameToId.keySet());

        // 如果虚拟作者不存在，使用第一个用户作为默认
        Integer defaultUserId = users.isEmpty() ? 1 : users.get(0).getId();
        System.out.println("默认用户ID: " + defaultUserId);

        int totalInserted = 0;
        int totalSkipped = 0;

        // 2. 遍历每个JSON文件
        for (Map.Entry<String, String> entry : FILE_CATEGORY_MAP.entrySet()) {
            String filename = entry.getKey();
            String categoryName = entry.getValue();
            Integer categoryId = categoryNameToId.get(categoryName);

            if (categoryId == null) {
                System.out.println("\n[跳过] 分类 '" + categoryName + "' 不存在，请先创建");
                continue;
            }

            // 确定该分类的作者ID
            String authorUsername = CATEGORY_AUTHOR_MAP.get(categoryName);
            Integer authorId = usernameToId.getOrDefault(authorUsername, defaultUserId);

            File jsonFile = new File(DATA_DIR, filename);
            if (!jsonFile.exists()) {
                System.out.println("\n[跳过] 文件不存在: " + jsonFile.getAbsolutePath());
                continue;
            }

            System.out.println("\n-------- 处理: " + categoryName + " (" + filename + ") --------");

            List<Map<String, Object>> articles;
            try {
                String json = Files.readString(jsonFile.toPath());
                articles = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            } catch (IOException e) {
                System.out.println("[错误] 读取JSON失败: " + e.getMessage());
                continue;
            }

            System.out.println("  JSON文章数: " + articles.size());

            for (Map<String, Object> articleData : articles) {
                String title = (String) articleData.get("title");
                String content = (String) articleData.get("content");

                if (title == null || title.isBlank() || content == null || content.isBlank()) {
                    totalSkipped++;
                    continue;
                }

                // 截断过长的标题（数据库字段限制）
                if (title.length() > 100) {
                    title = title.substring(0, 100);
                }

                // 检查标题是否已存在（去重）
                long existCount = articleService.lambdaQuery()
                        .eq(Article::getTitle, title)
                        .count();
                if (existCount > 0) {
                    totalSkipped++;
                    continue;
                }

                // 创建文章对象
                Article article = new Article();
                article.setTitle(title);
                article.setContent(content);
                article.setCoverImg((String) articleData.get("coverImg"));
                article.setState("已发布");
                article.setCategoryId(categoryId);
                article.setCreateUser(authorId);

                // 随机统计数据
                int viewCount = random.nextInt(5000) + 50;
                article.setViewCount(viewCount);
                article.setLikeCount(random.nextInt(Math.max(1, viewCount / 5)) + 5);
                article.setCollectCount(random.nextInt(Math.max(1, viewCount / 10)) + 1);
                article.setCommentCount(random.nextInt(Math.max(1, viewCount / 20)));
                article.setIsHot(viewCount > 3000 ? 1 : 0);
                article.setIsBest(random.nextDouble() < 0.15 ? 1 : 0);
                article.setIsDelete(0);
                article.setStatus(1);

                // 随机时间（过去90天内）
                LocalDateTime randomTime = LocalDateTime.now()
                        .minusDays(random.nextInt(90))
                        .minusHours(random.nextInt(24))
                        .minusMinutes(random.nextInt(60));
                article.setCreateTime(randomTime);
                article.setUpdateTime(randomTime);
                article.setPublishTime(randomTime);

                // SEO字段
                article.setSeoTitle(title);
                String summary = (String) articleData.get("summary");
                article.setSeoDescription(summary != null ? summary.substring(0, Math.min(summary.length(), 150)) : title);
                article.setSeoKeywords(title);

                // 保存文章
                try {
                    articleService.save(article);
                    totalInserted++;

                    // 处理标签
                    processArticleTags(article, articleData);

                    if (totalInserted % 10 == 0) {
                        System.out.println("  已插入: " + totalInserted + " 篇");
                    }
                } catch (Exception e) {
                    System.out.println("  [错误] 插入失败: " + title + " - " + e.getMessage());
                    totalSkipped++;
                }
            }
        }

        // 3. 清理Redis缓存
        System.out.println("\n清理Redis缓存...");
        try {
            redisUtil.deleteByPattern("article:list:*");
            redisUtil.deleteByPattern("article:search:*");
            redisUtil.deleteByPattern("article:user:*");
            redisUtil.deleteByPattern("article:*");
            System.out.println("  Redis缓存已清理");
        } catch (Exception e) {
            System.out.println("  [警告] Redis清理失败: " + e.getMessage());
        }

        System.out.println("\n========================================");
        System.out.println("填充完成！插入: " + totalInserted + " 篇, 跳过: " + totalSkipped + " 篇");
        System.out.println("========================================");
    }

    /**
     * 处理文章标签
     */
    private void processArticleTags(Article article, Map<String, Object> articleData) {
        Object tagsObj = articleData.get("tags");
        if (tagsObj == null) return;

        List<String> tagNames;
        try {
            tagNames = objectMapper.convertValue(tagsObj, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return;
        }

        if (tagNames == null || tagNames.isEmpty()) return;

        List<Long> tagIds = new ArrayList<>();

        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) continue;

            // 查找或创建标签
            Tags existingTag = tagsService.lambdaQuery()
                    .eq(Tags::getName, tagName)
                    .one();

            if (existingTag != null) {
                tagIds.add(existingTag.getId());
            } else {
                Tags newTag = new Tags();
                newTag.setName(tagName);
                tagsService.save(newTag);
                tagIds.add(newTag.getId());
            }
        }

        // 批量插入文章-标签关联
        if (!tagIds.isEmpty()) {
            try {
                articleTagsMapper.batchInsert(article.getId(), tagIds);
            } catch (Exception e) {
                System.out.println("    [警告] 标签关联失败 articleId=" + article.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 清理所有填充的文章（用于回滚）
     * 注意：此方法会删除所有 state="已发布" 且 isDelete=0 的文章
     * 请谨慎使用！
     */
    @Test
    public void cleanupSeededArticles() {
        System.out.println("准备清理填充的文章...");
        long count = articleService.lambdaQuery()
                .eq(Article::getIsDelete, 0)
                .count();
        System.out.println("当前文章总数: " + count);

        // 逻辑删除（移入回收站）
        articleService.lambdaUpdate()
                .eq(Article::getIsDelete, 0)
                .set(Article::getIsDelete, 1)
                .update();

        System.out.println("已将 " + count + " 篇文章移入回收站");

        // 清理Redis
        try {
            redisUtil.deleteByPattern("article:*");
            System.out.println("Redis缓存已清理");
        } catch (Exception e) {
            System.out.println("Redis清理失败: " + e.getMessage());
        }
    }
}
