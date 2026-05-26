import request from '@/utils/request'

// 获取所有标签
export const getAllTags = () => {
  return request.get('/tags/list')
}

// 新增标签
export const addTag = (data: any) => {
  return request.post('/tags/add', data)
}
