package com.heima.big_event.service.impl.article;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.mapper.article.ArticleCommentMapper;
import com.heima.big_event.mapper.article.ArticleMapper;
import com.heima.big_event.mapper.user.UserMapper;
import com.heima.big_event.pojo.Article;
import com.heima.big_event.pojo.ArticleComment;
import com.heima.big_event.pojo.User;
import com.heima.big_event.pojo.VO.CommentAuditListVO;
import com.heima.big_event.pojo.dto.CommentDTO;
import com.heima.big_event.service.article.ArticleCommentService;
import com.heima.big_event.utils.SensitiveWordUtil;
import com.heima.big_event.service.comment.CommentLikeService;
import com.heima.big_event.utils.Others.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArticleCommentServiceImpl extends ServiceImpl<ArticleCommentMapper, ArticleComment> implements ArticleCommentService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleCommentMapper articleCommentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommentLikeService commentLikeService;


    //发表文章评论(带二级回复)
    @Override
    public String addArticleCommentImpl(
            Integer articleId,
            Integer userId,
            String comment,
            Integer pareintId,
            Integer replyUserId){
        //检查文章是否存在,是否是已发布状态
        Article article = articleMapper.selectById(articleId);
        if (article ==null || article.getState().equals("草稿")){
            return "文章不存在或者未发布";
        }
        if (SensitiveWordUtil.containsSensitive(comment)){
            return "评论内容包含违规内容,请文明发言";
        }

        //把过滤的内容
        ArticleComment articleComment = new ArticleComment();
        articleComment.setArticleId(articleId);
        articleComment.setUserId(userId);
        articleComment.setContent(comment);

        articleComment.setParentId(pareintId);
        articleComment.setReplyUserId(replyUserId);
        articleComment.setAuditStatus(1);

        //插入数据
        articleCommentMapper.insert(articleComment);

        //更新评论数
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getId , articleId)
                .setSql("comment_count = comment_count + 1");
        articleMapper.update(null , wrapper);

        return "评论成功";
    }

    //文章评论分页(带二级回复)
    @Override
    public IPage<CommentDTO> getArticleCommentPageImpl(Integer articleId, Integer pageNum, Integer pageSize){
        Page<CommentDTO> page = new Page<>(pageNum , pageSize);

        IPage<CommentDTO> commentDTOIPage = articleCommentMapper.selectCommentWithUserPage(page , articleId);


        List<CommentDTO> parentComments  = commentDTOIPage.getRecords();

        //如果没有一级评论,直接返回
        if (parentComments.isEmpty()){
            return commentDTOIPage;
        }

        //提取所有一级评论的id
        List<Integer> parentIds = parentComments.stream()
                .map(CommentDTO::getId)
                .collect(Collectors.toList());

        //一次性批量查询所有二级回复
        List<CommentDTO> replyComments = articleCommentMapper.selectReplyComments(articleId , parentIds);

        //把回复按照parentId分组,按照一级评论分组
        Map<Integer , List<CommentDTO>> replyMap = replyComments.stream()
                .collect(Collectors.groupingBy(CommentDTO::getParentId));

        //把所有的一级评论map列表打包成list返回replyList
        for (CommentDTO parent : parentComments){
            List<CommentDTO> replies = replyMap.getOrDefault(parent.getId() , new ArrayList<>());
            parent.setReplyList(replies);
        }

        //填充当前登录用户对每一条评论的点赞状态
        try {
            Integer currentUserId = ThreadLocalUtil.getUserId();
            if (currentUserId != null) {
                fillLikedByMe(parentComments, currentUserId);
                fillLikedByMe(replyComments, currentUserId);
            }
        } catch (Exception ignored) {
        }

        return commentDTOIPage;

    }

    private void fillLikedByMe(List<CommentDTO> comments, Integer userId) {
        for (CommentDTO c : comments) {
            try {
                boolean liked = commentLikeService.checkCommentLikeImpl(c.getId(), userId);
                c.setLikedByMe(liked);
            } catch (Exception ignored) {
                c.setLikedByMe(false);
            }
        }
    }


    //管理员审核接口
    @Transactional
    @Override
    public void passComment(Integer commentId){
        //权限校验:必须是拥有审核评论权限的管理员
        checkAdmin();

        LambdaUpdateWrapper<ArticleComment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ArticleComment::getId , commentId)
                .set(ArticleComment::getAuditStatus , 2);//通过了

        articleCommentMapper.update(null , wrapper);
    }

    @Transactional
    @Override
    public void rejectComment(Integer commentId, String reason){
        //权限校验:必须是拥有审核评论权限的管理员
        checkAdmin();

        LambdaUpdateWrapper<ArticleComment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ArticleComment::getId , commentId)
                .set(ArticleComment::getAuditStatus , 3)//驳回了
                .set(ArticleComment::getRejectReason , reason);

        articleCommentMapper.update(null , wrapper);
    }

    //待审核评论列表
    @Override
    public Page<CommentAuditListVO> listPendingCommentsImpl(Integer pageNum, Integer pageSize){
        //管理员权限
        checkAdmin();

        //构造查询条件
        Page<ArticleComment> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<ArticleComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleComment::getAuditStatus , 1)
                .orderByDesc(ArticleComment::getCreateTime);

        //执行分页查询
        Page<ArticleComment> commentPage = articleCommentMapper.selectPage(page, wrapper);

        //转化成vo类返回,补充需要返回的信息
        Page<CommentAuditListVO> commentAuditListVOPage = new Page<>();
        commentAuditListVOPage.setTotal(commentPage.getTotal());
        commentAuditListVOPage.setPages(commentPage.getPages());
        commentAuditListVOPage.setCurrent(commentPage.getCurrent());
        commentAuditListVOPage.setSize(commentPage.getSize());

        List<CommentAuditListVO> commentAuditListVOList = new ArrayList<>();
        for (ArticleComment comment : commentPage.getRecords()){
            CommentAuditListVO vo = new CommentAuditListVO();
            vo.setId(comment.getId());
            vo.setArticleId(comment.getArticleId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setCreatime(comment.getCreateTime());
            vo.setAuditStatus(comment.getAuditStatus());


            //补充文章的标题
            Article article = articleMapper.selectById(comment.getArticleId());
            if (article != null){
                vo.setArticleTitle(article.getTitle());
            }

            //补充发布用户名称
            User user = userMapper.selectById(comment.getUserId());
            if (user != null){
                vo.setUserName(user.getUsername());
            }

            commentAuditListVOList.add(vo);
        }
        commentAuditListVOPage.setRecords(commentAuditListVOList);
        return commentAuditListVOPage;
    }


    //按审核状态查询评论列表
    @Override
    public Page<CommentAuditListVO> listCommentsByStatus(Integer pageNum, Integer pageSize, Integer auditStatus){
        checkAdmin();

        Page<ArticleComment> page = new Page<>(pageNum , pageSize);
        LambdaQueryWrapper<ArticleComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleComment::getAuditStatus , auditStatus)
                .orderByDesc(ArticleComment::getCreateTime);

        Page<ArticleComment> commentPage = articleCommentMapper.selectPage(page, wrapper);

        Page<CommentAuditListVO> voPage = new Page<>();
        voPage.setTotal(commentPage.getTotal());
        voPage.setPages(commentPage.getPages());
        voPage.setCurrent(commentPage.getCurrent());
        voPage.setSize(commentPage.getSize());

        List<CommentAuditListVO> voList = new ArrayList<>();
        for (ArticleComment comment : commentPage.getRecords()){
            CommentAuditListVO vo = new CommentAuditListVO();
            vo.setId(comment.getId());
            vo.setArticleId(comment.getArticleId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setCreatime(comment.getCreateTime());
            vo.setAuditStatus(comment.getAuditStatus());
            vo.setRejectReason(comment.getRejectReason());

            Article article = articleMapper.selectById(comment.getArticleId());
            if (article != null){
                vo.setArticleTitle(article.getTitle());
            }

            User user = userMapper.selectById(comment.getUserId());
            if (user != null){
                vo.setUserName(user.getUsername());
            }

            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    //权限校验方法
    //等待后续修复
    public void checkAdmin(){
        Integer userId = ThreadLocalUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null){
            throw new BusinessException("用户不存在");
        }




    }
}
