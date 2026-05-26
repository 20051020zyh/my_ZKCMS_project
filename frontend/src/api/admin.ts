import request from '@/utils/request'

// ==================== 系统配置管理 ====================

// 获取系统配置
export const getSysConfig = () => {
  return request.get('/sysConfig/get')
}

// 切换维护模式
export const toggleMaintenance = () => {
  return request.put('/sysConfig/update')
}

// ==================== 系统公告管理 ====================

// 获取后台公告列表
export const getNoticeList = () => {
  return request.get('/sysNotice/adimin/list')
}

// 新增公告
export const addNotice = (data: any) => {
  return request.post('/sysNotice/add', data)
}

// 删除公告(支持批量)
export const deleteNotice = (ids: number[]) => {
  return request.delete('/sysNotice/deleteList', { data: ids })
}

// ==================== 角色管理 ====================

// 获取角色列表(分页+条件)
export const getRoleList = (params?: any) => {
  return request.get('/sysRole/list', { params })
}

// 新增角色
export const addRole = (data: any) => {
  return request.post('/sysRole/add', data)
}

// 更新角色
export const updateRole = (data: any) => {
  return request.put('/sysRole/update', data)
}

// 删除角色(支持批量)
export const deleteRole = (ids: number[]) => {
  return request.delete('/sysRole/delete', { params: { ids: ids.join(',') } })
}

// 获取角色详情
export const getRoleDetail = (id: number) => {
  return request.get('/sysRole/select', { params: { id } })
}

// ==================== 权限管理 ====================

// 获取权限列表(平级列表)
export const getPermissionList = () => {
  return request.get('/sysPermission/list')
}

// 获取权限树
export const getPermissionTree = () => {
  return request.get('/sysPermission/tree')
}

// 新增权限
export const addPermission = (data: any) => {
  return request.post('/sysPermission/add', data)
}

// 更新权限
export const updatePermission = (data: any) => {
  return request.put('/sysPermission/update', data)
}

// 删除权限
export const deletePermission = (id: number) => {
  return request.delete('/sysPermission/delete', { params: { id } })
}

// 获取单个权限
export const getPermissionDetail = (id: number) => {
  return request.get('/sysPermission/selectOne', { params: { id } })
}

// ==================== 用户角色关联管理 ====================

// 为用户批量分配角色
export const assignUserRole = (data: any) => {
  return request.post('/sysUserRole/batchAssign', data)
}

// 获取用户当前的角色ID列表
export const getUserRoleIds = (userId: number) => {
  return request.get(`/sysUserRole/getRoleIds/${userId}`)
}

// ==================== 角色权限关联管理 ====================

// 为角色分配权限(批量)
export const assignRolePermission = (data: any) => {
  return request.post('/sysRolePermission/batchAssign', data)
}

// 获取所有角色权限关联
export const getRolePermissionList = () => {
  return request.get('/sysRolePermission/list')
}

// ==================== 用户权限关联管理 ====================

// 为用户直接分配权限(批量)
export const assignUserPermission = (data: any) => {
  return request.post('/sysUserPermission/batchAssign', data)
}

// 获取用户已直接分配的权限ID列表
export const getUserPermissionIds = (userId: number) => {
  return request.get(`/sysUserPermission/getPermissionIds/${userId}`)
}

// 获取用户全部权限ID（角色+直接分配，去重合并）
export const getAllUserPermissionIds = (userId: number) => {
  return request.get(`/sysUserPermission/getAllPermissionIds/${userId}`)
}

// ==================== 首页弹窗信息 ====================

// 获取首页弹窗信息
export const getPopInfo = () => {
  return request.get('/index/popInfo')
}

// ==================== 公告中心（前台用户） ====================

// 获取近3个月公告列表（前台）
export const getUserNoticeList = () => {
  return request.get('/sysNotice/user/list')
}
