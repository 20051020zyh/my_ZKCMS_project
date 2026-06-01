import request from '@/utils/request'

// 获取文章列表
export const getArticleList = (params: any) => {
  return request.get('/article/pageList', { params })
}

// 获取文章详情
export const getArticleDetail = (id: number) => {
  return request.get('/article/detail', { params: { id } })
}

// 获取热门文章
export const getHotArticles = (params?: any) => {
  return request.get('/article/hot/list', { params })
}

// 获取精选文章
export const getBestArticles = (params?: any) => {
  return request.get('/article/best/list', { params })
}

// 点赞/取消点赞
export const toggleLike = (articleId: number) => {
  return request.post('/article/like/toggle', null, { params: { articleId } })
}

// 检查是否已点赞
export const checkLike = (articleId: number) => {
  return request.get('/article/like/check', { params: { articleId } })
}

// 收藏/取消收藏
export const toggleCollect = (articleId: number, folderId?: number) => {
  const params: any = { articleId }
  if (folderId) params.folderId = folderId
  return request.post('/article/collect/toggle', null, { params })
}

// 检查是否已收藏
export const checkCollect = (articleId: number) => {
  return request.get('/article/collect/check', { params: { articleId } })
}

// 获取用户收藏列表
export const getUserCollects = (params?: any) => {
  return request.get('/article/collect/user/list', { params })
}

// 移入/移出收藏文件夹
export const moveCollectFolder = (articleId: number, folderId?: number) => {
  const params: any = { articleId }
  if (folderId !== undefined) params.folderId = folderId
  return request({
    method: 'put',
    url: '/article/collect/moveFolder',
    params
  })
}

// 新增文章
export const addArticle = (data: any) => {
  return request.post('/article/add', data)
}

// 更新文章
export const updateArticle = (data: any) => {
  return request.put('/article/update', data)
}

// 删除文章
export const deleteArticle = (id: number) => {
  return request.delete('/article/delete', { params: { id } })
}

// 获取用户文章列表
export const getUserArticles = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get('/article/user/list', { params })
}

// 搜索文章
export const searchArticles = (keyword: string) => {
  return request.get('/article/search', { params: { keyword } })
}

// 批量更新文章状态
export const batchUpdateStatus = (data: any) => {
  return request.post('/article/updateStatus', data)
}

// 按分类获取文章
export const getArticlesByCategory = (categoryId: number) => {
  return request.get('/article/list/by-category', { params: { categoryId } })
}

// 定时发布文章
export const scheduleArticle = (data: any) => {
  return request.post('/article/schedule', data)
}

// 举报文章
export const reportArticle = (data: { articleId: number; reportType: number; content: string; images?: string[] }) => {
  return request.post('/article/report/add', data)
}

// 文章排行榜
export const getArticleRank = (params: { type: string; limit?: number }) => {
  return request.get('/article/rank', { params })
}

// ==================== 回收站 ====================

// 移入回收站
export const trashArticle = (id: number) => {
  return request.post('/article/trash', null, { params: { id } })
}

// 查看回收站列表
export const getTrashList = (params?: any) => {
  return request.get('/article/trash/look', { params })
}

// 从回收站恢复
export const recoverArticle = (id: number) => {
  return request.put('/article/trash/recover', null, { params: { id } })
}

// 永久删除
export const foreverDeleteArticle = (id: number) => {
  return request.delete('/article/trash/forever', { params: { id } })
}

// 获取近7天/30天访问趋势
export const getArticleTrend = (days: number) => {
  return request.get('/article/trend', { params: { days } })
}

// 文章管理统计接口
export const getArticleStats = () => {
  return request.get('/article/stats')
}

// ==================== 标签 ====================

// 获取热门标签
export const getHotTags = (limit?: number) => {
  return request.get('/tags/hot', { params: { limit } })
}
