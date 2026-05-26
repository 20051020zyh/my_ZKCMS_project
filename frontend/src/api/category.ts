import request from '@/utils/request'

// 获取所有分类
export const getAllCategories = () => {
  return request.get('/category/all/simple')
}

// 获取用户分类列表
export const getUserCategoryList = () => {
  return request.get('/category/get/user/list')
}

// 新增分类
export const addCategory = (data: any) => {
  return request.post('/category/add', data)
}

// 更新分类
export const updateCategory = (data: any) => {
  return request.put('/category/update', data)
}

// 删除分类
export const deleteCategory = (id: number) => {
  return request.delete('/category/delete', { params: { id } })
}

// 获取分类详情
export const getCategoryDetail = (id: number) => {
  return request.get('/category/detail', { params: { id } })
}
