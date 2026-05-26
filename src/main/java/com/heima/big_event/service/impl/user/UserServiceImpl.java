package com.heima.big_event.service.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.mapper.article.ArticleCollectMapper;
import com.heima.big_event.mapper.article.ArticleCommentMapper;
import com.heima.big_event.mapper.article.ArticleLikeMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.article.ArticleReportMapper;
import com.heima.big_event.mapper.article.ArticleTagsMapper;
import com.heima.big_event.mapper.category.CategoryMapper;
import com.heima.big_event.mapper.comment.CommentLikeMapper;
import com.heima.big_event.mapper.comment.CommentReportMapper;
import com.heima.big_event.mapper.dataStatistics.VisitLogMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleCollect;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.ArticleLike;
import com.heima.big_event.pojo.ArticleReport;
import com.heima.big_event.pojo.ArticleTag;
import com.heima.big_event.pojo.Category;
import com.heima.big_event.pojo.CommentLike;
import com.heima.big_event.pojo.CommentReport;
import com.heima.big_event.pojo.SysRole;
import com.heima.big_event.pojo.SysUserRole;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.VO.AdminHomeStatsVO;
import com.heima.big_event.pojo.VO.ArticleCenterInfoVO;
import com.heima.big_event.pojo.VO.UserWithRolesVO;
import com.heima.big_event.pojo.VisitLog;
import com.heima.big_event.mapper.system.SysRoleMapper;
import com.heima.big_event.mapper.system.SysUserRoleMapper;
import com.heima.big_event.service.system.SysRoleService;
import com.heima.big_event.service.system.SysUserRoleService;
import com.heima.big_event.service.user.UserService;
import com.heima.big_event.utils.Others.Md5Util;
import com.heima.big_event.utils.Others.RedisUtil;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleCommentMapper articleCommentMapper;
    @Autowired
    private ArticleCollectMapper articleCollectMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private VisitLogMapper visitLogMapper;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private ArticleTagsMapper articleTagsMapper;

    @Autowired
    private ArticleReportMapper articleReportMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private CommentReportMapper commentReportMapper;

    @Autowired
    private CategoryMapper categoryMapper;
    //查询用户
    @Override
    public User findByUserName(String username) {
        return lambdaQuery().eq(User::getUsername,username).one();
    }

    //注册
    @Override
    public void register(String username, String password) {
        String md5String = Md5Util.getMD5String(password);

        User user = new User();
        user.setUsername(username);
        user.setPassword(md5String);
        save(user);

        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getRoleCode, "USER");
        SysRole userRole = sysRoleService.getOne(roleWrapper);
        if (userRole == null) {
            userRole = new SysRole();
            userRole.setRoleName("普通用户");
            userRole.setRoleCode("USER");
            userRole.setRemark("系统默认角色");
            sysRoleService.save(userRole);
        }

        SysUserRole sysUserRole = new SysUserRole();
        sysUserRole.setUserId(user.getId().longValue());
        sysUserRole.setRoleId(userRole.getId());
        sysUserRoleService.save(sysUserRole);
    }

    //更新用户头像
    @Override
    public void updateAvatar(Integer userId , String avatarUrl) {
        User user = new User();
        user.setId(userId);
        user.setUserPic(avatarUrl);

        //调用MyBatis-Plus的更新方法
        userMapper.updateById(user);
    }

    //更新用户密码
    @Override
    public void updatePwd(Integer userId, String newPwd) {
        //对密码进行加密
        String md5String = Md5Util.getMD5String(newPwd);

        //创建user对象
        User user = new User();
        user.setId(userId);
        user.setPassword(md5String);

        //把user对象传进去之后,方法会根据id来更新密码,而且因为还配置了自动更新时间,
        //所以不用再手动更新时间
        userMapper.updateById(user);
    }

    //退出登录
    @Override
    public void logout(String authorization){
        //移除当前登录的用户
        ThreadLocalUtil.clear();

        //把token加入redis黑名单
        if (authorization != null && !authorization.isEmpty()){
            redisUtil.set("blacklist : " + authorization , "invaild" , 86400);
        }
    }

    //个人中心接口
    @Override
    public ArticleCenterInfoVO UserCenterInfoImpl(Integer userId){
        //查对应用户基础数据
        User user = userMapper.selectById(userId);

        //查询已发布文章数
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getCreateUser , userId)
                .eq(Article::getState , "已发布");
        Long fabuCount = articleMapper.selectCount(wrapper);

        //查询草稿文章数
        LambdaQueryWrapper<Article> wrapper1 = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getCreateUser , userId)
                .eq(Article::getState , "草稿");
        Long caogaoCount = articleMapper.selectCount(wrapper);

        //查询我的评论总数
        LambdaQueryWrapper<ArticleComment> articleCommentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleCommentLambdaQueryWrapper.eq(ArticleComment::getUserId , userId)
                .eq(ArticleComment::getIsDelete , 0);
        Long commentCount = articleCommentMapper.selectCount(articleCommentLambdaQueryWrapper);


        //查询我的收藏总数
        LambdaQueryWrapper<ArticleCollect> articleCollectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleCollectLambdaQueryWrapper.eq(ArticleCollect::getUserId , userId);
        Long collectCount = articleCollectMapper.selectCount(articleCollectLambdaQueryWrapper);


        //打包返回咯
        ArticleCenterInfoVO vo = new ArticleCenterInfoVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setUser_pic(user.getUserPic());
        vo.setEmail(user.getEmail());
        vo.setFabuCount(fabuCount);
        vo.setCaogaoCount(caogaoCount);
        vo.setCommentCount(commentCount);
        vo.setCollectCount(collectCount);

        return vo;
    }

    //后台首页统计接口
    @Override
    public AdminHomeStatsVO AdminHomeStatsImpl(){
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getIsDelete , 0);
        Long totalArticles = articleMapper.selectCount(articleWrapper);
        Long totalUsers = userMapper.selectCount(null);
        Long totalViews = articleMapper.sumViewCount();

        LambdaQueryWrapper<ArticleComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(ArticleComment::getIsDelete , 0);
        Long totalComments = articleCommentMapper.selectCount(commentWrapper);

        Long totalLikes = articleMapper.sumLikeCount();
        Long totalCollections = articleMapper.sumCollectCount();

        LocalDateTime startOfday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfday = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);

        LambdaQueryWrapper<Article> todayArticleWrapper = new LambdaQueryWrapper<>();
        todayArticleWrapper.between(Article::getCreateTime , startOfday , endOfday)
                .eq(Article::getIsDelete , 0);
        Long todayArticles = articleMapper.selectCount(todayArticleWrapper);

        LambdaQueryWrapper<User> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.between(User::getCreateTime , startOfday , endOfday);
        Long todayUsers = userMapper.selectCount(todayUserWrapper);

        LambdaQueryWrapper<ArticleComment> todayCommentWrapper = new LambdaQueryWrapper<>();
        todayCommentWrapper.between(ArticleComment::getCreateTime , startOfday , endOfday)
                .eq(ArticleComment::getIsDelete , 0);
        Long todayComments = articleCommentMapper.selectCount(todayCommentWrapper);

        // 从点赞表和收藏表统计今日新增点赞和收藏记录数
        LambdaQueryWrapper<ArticleLike> todayLikeWrapper = new LambdaQueryWrapper<>();
        todayLikeWrapper.between(ArticleLike::getCreateTime , startOfday , endOfday);
        Long todayLikes = articleLikeMapper.selectCount(todayLikeWrapper);

        LambdaQueryWrapper<ArticleCollect> todayCollectWrapper = new LambdaQueryWrapper<>();
        todayCollectWrapper.between(ArticleCollect::getCreateTime , startOfday , endOfday);
        Long todayCollections = articleCollectMapper.selectCount(todayCollectWrapper);

        // 从访问日志表统计今日访问总量
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Map<String, Object> visitStat = visitLogMapper.statVisit(today);
        Long todayViews = visitStat != null ? ((Number) visitStat.getOrDefault("pv", 0)).longValue() : 0L;

        java.time.LocalDate yesterday = today.minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.atTime(23, 59, 59, 999_999_999);

        LambdaQueryWrapper<Article> yesterdayArticleWrapper = new LambdaQueryWrapper<>();
        yesterdayArticleWrapper.between(Article::getCreateTime, startOfYesterday, endOfYesterday)
                .eq(Article::getIsDelete , 0);
        Long yesterdayArticles = articleMapper.selectCount(yesterdayArticleWrapper);

        LambdaQueryWrapper<User> yesterdayUserWrapper = new LambdaQueryWrapper<>();
        yesterdayUserWrapper.between(User::getCreateTime, startOfYesterday, endOfYesterday);
        Long yesterdayUsers = userMapper.selectCount(yesterdayUserWrapper);

        LambdaQueryWrapper<ArticleComment> yesterdayCommentWrapper = new LambdaQueryWrapper<>();
        yesterdayCommentWrapper.between(ArticleComment::getCreateTime, startOfYesterday, endOfYesterday)
                .eq(ArticleComment::getIsDelete , 0);
        Long yesterdayComments = articleCommentMapper.selectCount(yesterdayCommentWrapper);

        // 从点赞表和收藏表统计昨日新增点赞和收藏记录数
        LambdaQueryWrapper<ArticleLike> yesterdayLikeWrapper = new LambdaQueryWrapper<>();
        yesterdayLikeWrapper.between(ArticleLike::getCreateTime, startOfYesterday, endOfYesterday);
        Long yesterdayLikes = articleLikeMapper.selectCount(yesterdayLikeWrapper);

        LambdaQueryWrapper<ArticleCollect> yesterdayCollectWrapper = new LambdaQueryWrapper<>();
        yesterdayCollectWrapper.between(ArticleCollect::getCreateTime, startOfYesterday, endOfYesterday);
        Long yesterdayCollections = articleCollectMapper.selectCount(yesterdayCollectWrapper);

        // 从访问日志表统计昨日访问总量
        java.util.Map<String, Object> yesterdayVisitStat = visitLogMapper.statVisit(yesterday);
        Long yesterdayViews = yesterdayVisitStat != null ? ((Number) yesterdayVisitStat.getOrDefault("pv", 0)).longValue() : 0L;

        java.time.LocalDate beforeYesterday = today.minusDays(2);
        LocalDateTime startOfBeforeYesterday = beforeYesterday.atStartOfDay();
        LocalDateTime endOfBeforeYesterday = beforeYesterday.atTime(23, 59, 59, 999_999_999);

        LambdaQueryWrapper<Article> beforeYesterdayArticleWrapper = new LambdaQueryWrapper<>();
        beforeYesterdayArticleWrapper.between(Article::getCreateTime, startOfBeforeYesterday, endOfBeforeYesterday)
                .eq(Article::getIsDelete, 0);
        Long beforeYesterdayArticles = articleMapper.selectCount(beforeYesterdayArticleWrapper);

        LambdaQueryWrapper<User> beforeYesterdayUserWrapper = new LambdaQueryWrapper<>();
        beforeYesterdayUserWrapper.between(User::getCreateTime, startOfBeforeYesterday, endOfBeforeYesterday);
        Long beforeYesterdayUsers = userMapper.selectCount(beforeYesterdayUserWrapper);

        LambdaQueryWrapper<ArticleComment> beforeYesterdayCommentWrapper = new LambdaQueryWrapper<>();
        beforeYesterdayCommentWrapper.between(ArticleComment::getCreateTime, startOfBeforeYesterday, endOfBeforeYesterday)
                .eq(ArticleComment::getIsDelete, 0);
        Long beforeYesterdayComments = articleCommentMapper.selectCount(beforeYesterdayCommentWrapper);

        LambdaQueryWrapper<ArticleLike> beforeYesterdayLikeWrapper = new LambdaQueryWrapper<>();
        beforeYesterdayLikeWrapper.between(ArticleLike::getCreateTime, startOfBeforeYesterday, endOfBeforeYesterday);
        Long beforeYesterdayLikes = articleLikeMapper.selectCount(beforeYesterdayLikeWrapper);

        LambdaQueryWrapper<ArticleCollect> beforeYesterdayCollectWrapper = new LambdaQueryWrapper<>();
        beforeYesterdayCollectWrapper.between(ArticleCollect::getCreateTime, startOfBeforeYesterday, endOfBeforeYesterday);
        Long beforeYesterdayCollections = articleCollectMapper.selectCount(beforeYesterdayCollectWrapper);

        java.util.Map<String, Object> beforeYesterdayVisitStat = visitLogMapper.statVisit(beforeYesterday);
        Long beforeYesterdayViews = beforeYesterdayVisitStat != null ? ((Number) beforeYesterdayVisitStat.getOrDefault("pv", 0)).longValue() : 0L;

        AdminHomeStatsVO vo = new AdminHomeStatsVO();
        vo.setTotalArticles(totalArticles);
        vo.setTotalUsers(totalUsers);
        vo.setTotalViews(totalViews);
        vo.setTotalComments(totalComments);
        vo.setTotalLikes(totalLikes);
        vo.setTotalCollections(totalCollections);
        vo.setTodayArticles(todayArticles);
        vo.setTodayUsers(todayUsers);
        vo.setTodayViews(todayViews);
        vo.setTodayComments(todayComments);
        vo.setTodayLikes(todayLikes);
        vo.setTodayCollections(todayCollections);
        vo.setYesterdayArticles(yesterdayArticles);
        vo.setYesterdayUsers(yesterdayUsers);
        vo.setYesterdayViews(yesterdayViews);
        vo.setYesterdayComments(yesterdayComments);
        vo.setYesterdayLikes(yesterdayLikes);
        vo.setYesterdayCollections(yesterdayCollections);
        vo.setBeforeYesterdayArticles(beforeYesterdayArticles);
        vo.setBeforeYesterdayUsers(beforeYesterdayUsers);
        vo.setBeforeYesterdayViews(beforeYesterdayViews);
        vo.setBeforeYesterdayComments(beforeYesterdayComments);
        vo.setBeforeYesterdayLikes(beforeYesterdayLikes);
        vo.setBeforeYesterdayCollections(beforeYesterdayCollections);

        return vo;
    }

    //获取用户状态统计
    @Override
    public java.util.Map<String, Long> getUserStatusStats() {
        Long total = userMapper.selectCount(null);
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(User::getStatus, 1);
        Long active = userMapper.selectCount(activeWrapper);
        LambdaQueryWrapper<User> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(User::getStatus, 0);
        Long disabled = userMapper.selectCount(disabledWrapper);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        map.put("total", total);
        map.put("active", active);
        map.put("disabled", disabled);
        return map;
    }

    //全用户分页管理
    @Override
    public IPage<UserWithRolesVO> getUserPageList(Integer pageNum, Integer pageSize, String keyword){
        Page<User> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            userWrapper.and(w -> w.like(User::getUsername, like)
                    .or().like(User::getNickname, like)
                    .or().like(User::getEmail, like));
        }
        userWrapper.orderByDesc(User::getCreateTime);
        IPage<User> userPage = this.page(page, userWrapper);

        Page<UserWithRolesVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (userPage.getRecords().isEmpty()) {
            return voPage;
        }

        List<Integer> userIds = userPage.getRecords().stream().map(User::getId).collect(Collectors.toList());

        Map<Integer, List<String>> userRolesMap = new HashMap<>();
        for (Integer uid : userIds) {
            userRolesMap.put(uid, new ArrayList<>());
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds)
        );
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
            List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
                );
            Map<Long, String> roleNameMap = roles.stream().collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));

            for (SysUserRole ur : userRoles) {
                String roleName = roleNameMap.get(ur.getRoleId());
                if (roleName != null) {
                    userRolesMap.computeIfAbsent(ur.getUserId().intValue(), k -> new ArrayList<>()).add(roleName);
                }
            }
        }

        List<UserWithRolesVO> voList = userPage.getRecords().stream().map(u -> {
            UserWithRolesVO vo = new UserWithRolesVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setEmail(u.getEmail());
            vo.setUserPic(u.getUserPic());
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());
            vo.setStatus(u.getStatus());
            vo.setRoles(userRolesMap.getOrDefault(u.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    //注销用户（级联删除该用户所有相关数据）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserWithAllData(Integer userId) {
        // 1. 查出该用户的所有文章ID
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getCreateUser, userId);
        List<Article> articles = articleMapper.selectList(articleWrapper);
        List<Integer> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());

        // 2. 删除文章标签关联 (article_tag)
        if (!articleIds.isEmpty()) {
            LambdaQueryWrapper<ArticleTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(ArticleTag::getArticleId, articleIds);
            articleTagsMapper.delete(tagWrapper);
        }

        // 3. 删除文章收藏 (article_collect)
        LambdaQueryWrapper<ArticleCollect> collectWrapper = new LambdaQueryWrapper<>();
        collectWrapper.eq(ArticleCollect::getUserId, userId);
        articleCollectMapper.delete(collectWrapper);

        // 4. 删除文章点赞 (article_like)
        LambdaQueryWrapper<ArticleLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(ArticleLike::getUserId, userId);
        articleLikeMapper.delete(likeWrapper);

        // 5. 删除文章本身 (article)
        if (!articleIds.isEmpty()) {
            articleMapper.deleteBatchIds(articleIds);
        }

        // 6. 删除文章举报 (article_report)
        LambdaQueryWrapper<ArticleReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(ArticleReport::getUserId, userId);
        articleReportMapper.delete(reportWrapper);

        // 7. 删除评论点赞 (comment_like)
        LambdaQueryWrapper<CommentLike> commentLikeWrapper = new LambdaQueryWrapper<>();
        commentLikeWrapper.eq(CommentLike::getUserId, userId);
        commentLikeMapper.delete(commentLikeWrapper);

        // 8. 删除评论举报 (comment_report)
        LambdaQueryWrapper<CommentReport> commentReportWrapper = new LambdaQueryWrapper<>();
        commentReportWrapper.eq(CommentReport::getUserId, userId);
        commentReportMapper.delete(commentReportWrapper);

        // 9. 删除文章评论 (article_comment) — 先查出该用户的所有评论ID，删除子评论
        LambdaQueryWrapper<ArticleComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(ArticleComment::getUserId, userId);
        List<ArticleComment> comments = articleCommentMapper.selectList(commentWrapper);
        List<Integer> commentIds = comments.stream().map(ArticleComment::getId).collect(Collectors.toList());
        if (!commentIds.isEmpty()) {
            LambdaQueryWrapper<ArticleComment> childCommentWrapper = new LambdaQueryWrapper<>();
            childCommentWrapper.in(ArticleComment::getParentId, commentIds);
            articleCommentMapper.delete(childCommentWrapper);
        }
        articleCommentMapper.delete(commentWrapper);

        // 10. 删除访问日志 (visit_log)
        LambdaQueryWrapper<VisitLog> visitLogWrapper = new LambdaQueryWrapper<>();
        visitLogWrapper.eq(VisitLog::getUserId, userId);
        visitLogMapper.delete(visitLogWrapper);

        // 11. 删除用户角色关联 (sys_user_role)
        LambdaQueryWrapper<SysUserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysUserRole::getUserId, userId.longValue());
        sysUserRoleService.remove(roleWrapper);

        // 12. 删除该用户创建的分类 (category)
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(Category::getCreateUser, userId);
        categoryMapper.delete(categoryWrapper);

        // 13. 清除Redis缓存
        redisUtil.delete("login:user:" + userId);

        // 14. 最后删除用户本身 (user)
        userMapper.deleteById(userId);
    }

}
