import request from '@/utils/request'

// 用户登录
export const login = (data: { username: string; password: string }) => {
  return request.post('/user/login', data)
}

// 用户注册
export const register = (data: { username: string; password: string }) => {
  return request.post('/user/register', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/user/userInfo')
}

// 更新用户信息
export const updateUserInfo = (data: any) => {
  return request({
    method: 'PUT',
    url: '/user/update',
    data: data
  })
}

// 更新头像
export const updateAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/updateAvatar', formData)
}

// 退出登录
export const logout = () => {
  return request.get('/user/logout')
}

// 更新密码
export const updatePassword = (data: any) => {
  return request.patch('/user/updatePwd', data)
}

// 获取个人中心信息
export const getUserCenterInfo = () => {
  return request.get('/user/center/info')
}

// ==================== 管理员用户管理 ====================

// 获取后台首页统计
export const getAdminHomeStats = () => {
  return request.get('/user/admin/home/stats')
}

// 获取用户分页列表(管理员)
export const getAllUserPage = (params?: any) => {
  return request.get('/user/allUserPage', { params })
}

// 获取用户状态统计（正常/禁用数）
export const getUserStatusStats = () => {
  return request.get('/user/admin/userStatusStats')
}

// 启用/禁用用户账号
export const updateUserStatus = (userId: number, status: number) => {
  return request.post('/user/updateStatus', null, { params: { userId, status } })
}

// 注销用户（级联删除该用户所有相关数据）
export const deleteUser = (userId: number) => {
  return request.delete('/user/deleteUser', { params: { userId } })
}

// 获取当前用户的权限路径列表（用于前端菜单级权限校验）
export const getMyPermissionPaths = () => {
  return request.get('/user/admin/permissionPaths')
}

// 文件上传
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData)
}
