import request from '@/utils/request'

// 发表评论
export const addComment = (articleId: number, comment: string, parentId?: number, replyUserId?: number) => {
  return request.post('/article/comment/add', null, {
    params: { articleId, comment, parentId: parentId || 0, replyUserId: replyUserId || 0 }
  })
}

// 获取文章评论分页列表
export const getArticleComments = (articleId: number, params?: any) => {
  return request.get('/article/comment/page/list', { params: { articleId, ...params } })
}

// 删除评论
export const deleteComment = (commentId: number) => {
  return request.post('/article/comment/delete', null, { params: { commentId } })
}

// 点赞评论
export const likeComment = (articleId: number, commentId: number) => {
  return request.post('/comment/like/add', null, { params: { articleId, commentId } })
}

// 检查是否已点赞评论
export const checkCommentLike = (commentId: number) => {
  return request.get('/comment/like/check', { params: { commentId } })
}

// 举报评论
export const reportComment = (data: any) => {
  return request.post('/comment/report/add', data)
}

// 评论审核列表(管理员)
export const getPendingCommentList = (params?: any) => {
  return request.get('/article/comment/pending/list', { params })
}

// 评论列表按审核状态(管理员) auditStatus: 1=待审核 2=已通过 3=已驳回
export const getCommentListByStatus = (params?: any) => {
  return request.get('/article/comment/list', { params })
}

// 审核评论(管理员)
export const auditComment = (commentId: number, auditStatus: number, reason?: string) => {
  return request.post('/article/comment/audit', null, {
    params: { commentId, auditStatus, reason: reason || '' }
  })
}

// 批量删除评论(管理员)
export const batchDeleteComments = (commentIds: number[]) => {
  return request.post('/article/comment/batchDelete', commentIds)
}

// 获取评论举报列表(管理员)
export const getCommentReportList = (params?: any) => {
  return request.get('/comment/report/list', { params })
}

// 审核评论举报(管理员)
export const auditCommentReport = (id: number, status: number) => {
  return request.post('/comment/report/audit', null, { params: { id, status } })
}
