import request from '@/utils/request'

// 获取文章举报列表(管理员)
export const getArticleReportList = (params?: any) => {
  return request.get('/article/report/list', { params })
}

// 提交文章举报
export const reportArticle = (data: any) => {
  return request.post('/article/report/add', data)
}

// 审核文章举报(管理员)
export const auditArticleReport = (id: number, status: number) => {
  return request.post('/article/report/audit', null, { params: { id, status } })
}
