<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllUserPage, updateUserStatus, deleteUser, getUserStatusStats } from '@/api/user'
import { getRoleList, assignUserRole, getUserRoleIds, getPermissionTree, assignUserPermission, getAllUserPermissionIds } from '@/api/admin'
import { exportToCSV, exportToJSON } from '@/utils/export'

const loading = ref(false)
const users = ref<any[]>([])
const roles = ref<any[]>([])
const roleDialogVisible = ref(false)
const roleDialogLoading = ref(false)
const currentUser = ref<any>(null)
const selectedRoleIds = ref<number[]>([])
const deleteDialogVisible = ref(false)
const deletingUserId = ref<number | null>(null)
const deletingUserName = ref('')

const statusDialogVisible = ref(false)
const statusTargetUser = ref<any>(null)
const statusNewValue = ref(0)

const permDialogVisible = ref(false)
const permDialogLoading = ref(false)
const permissionTree = ref<any[]>([])
const checkedPermissionIds = ref<number[]>([])

const pagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const searchKeyword = ref('')

const userStats = ref({ total: 0, active: 0, disabled: 0 })

const fetchUserStats = async () => {
  try {
    const res: any = await getUserStatusStats()
    if (res.code === 0) {
      userStats.value = res.data
    }
  } catch {}
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res: any = await getAllUserPage(params)
    users.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page
  fetchUsers()
}

const handleAssignRole = async (row: any) => {
  currentUser.value = row
  selectedRoleIds.value = []
  roleDialogLoading.value = true
  roleDialogVisible.value = true
  try {
    const [roleRes, userRoleRes]: any = await Promise.all([
      getRoleList({ pageNum: 1, pageSize: 999 }),
      getUserRoleIds(row.id)
    ])
    roles.value = roleRes.data?.records || []
    selectedRoleIds.value = userRoleRes.data || []
  } catch {
    ElMessage.error('获取数据失败')
  } finally {
    roleDialogLoading.value = false
  }
}

const handleSaveRole = async () => {
  if (!currentUser.value) return
  try {
    await assignUserRole({
      userId: currentUser.value.id,
      roleIds: selectedRoleIds.value
    })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch {
    ElMessage.error('角色分配失败')
  }
}

const toggleRole = (roleId: number) => {
  const idx = selectedRoleIds.value.indexOf(roleId)
  if (idx > -1) selectedRoleIds.value.splice(idx, 1)
  else selectedRoleIds.value.push(roleId)
}

const handleToggleStatus = async (userId: number, currentStatus: number) => {
  const newStatus = currentStatus === 1 ? 0 : 1
  const actionText = newStatus === 0 ? '禁用' : '启用'
  try {
    await updateUserStatus(userId, newStatus)
    ElMessage.success(`${actionText}成功`)
    fetchUsers()
    fetchUserStats()
  } catch {
    ElMessage.error(`${actionText}失败`)
  }
}

const handleShowStatusConfirm = (row: any) => {
  statusTargetUser.value = row
  statusNewValue.value = row.status === 1 ? 0 : 1
  statusDialogVisible.value = true
}

const handleConfirmStatusToggle = async () => {
  if (!statusTargetUser.value) return
  const userId = statusTargetUser.value.id
  const newStatus = statusNewValue.value
  const actionText = newStatus === 0 ? '禁用' : '启用'
  try {
    await updateUserStatus(userId, newStatus)
    ElMessage.success(`${actionText}成功`)
    statusDialogVisible.value = false
    fetchUsers()
    fetchUserStats()
  } catch {
    ElMessage.error(`${actionText}失败`)
  }
}

const handleShowDelete = (row: any) => {
  deletingUserId.value = row.id
  deletingUserName.value = row.nickname || row.username
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (deletingUserId.value === null) return
  try {
    await deleteUser(deletingUserId.value)
    ElMessage.success(`用户「${deletingUserName.value}」已注销，所有相关数据已清除`)
    deleteDialogVisible.value = false
    fetchUsers()
    fetchUserStats()
  } catch {
    ElMessage.error('注销用户失败')
  }
}

// 过滤只保留后台管理系统的权限
const filterAdminPermissions = (permissions: any[]): any[] => {
  // 定义保留的后台管理权限关键词（白名单）
  const includeKeywords = ['sys', '系统', 'admin', '管理', 'role', '角色', 
                           'permission', '权限', 'notice', '公告', 'config', '配置',
                           'dashboard', '仪表盘', 'statistics', '统计', 'user-manage',
                           '用户管理', '用户']
  
  const filter = (nodes: any[]): any[] => {
    return nodes
      .filter(node => {
        // 先检查是否是要直接删除的分类
        const nodeName = node.name.toLowerCase()
        if (nodeName.includes('文章管理') || nodeName.includes('分类管理') || nodeName.includes('评论管理')) {
          return false
        }
        
        // 检查是否在白名单中
        const name = (node.name + node.permission + node.path).toLowerCase()
        const isIncluded = includeKeywords.some(key => name.includes(key.toLowerCase()))
        
        // 如果节点有子节点，先处理子节点
        if (node.children && node.children.length > 0) {
          node.children = filter(node.children)
          // 有符合条件的子节点，或者本身在白名单中，就保留
          return isIncluded || node.children.length > 0
        }
        
        // 叶子节点看是否在白名单
        return isIncluded
      })
  }
  
  return filter(permissions)
}

const handleAssignPermission = async (row: any) => {
  currentUser.value = row
  checkedPermissionIds.value = []
  permDialogLoading.value = true
  permDialogVisible.value = true
  try {
    const [treeRes, userPermRes]: any = await Promise.all([
      getPermissionTree(),
      getAllUserPermissionIds(row.id)
    ])
    // 过滤只保留后台管理权限
    permissionTree.value = filterAdminPermissions(treeRes.data || [])
    checkedPermissionIds.value = userPermRes.data || []
  } catch {
    ElMessage.error('获取权限数据失败')
  } finally {
    permDialogLoading.value = false
  }
}

const toggleUserPerm = (id: number) => {
  const idx = checkedPermissionIds.value.indexOf(id)
  if (idx > -1) {
    checkedPermissionIds.value.splice(idx, 1)
  } else {
    checkedPermissionIds.value.push(id)
  }
}

const handleSaveUserPermission = async () => {
  if (!currentUser.value) return
  try {
    await assignUserPermission({
      userId: currentUser.value.id,
      permissionIds: checkedPermissionIds.value
    })
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch {
    ElMessage.error('权限分配失败')
  }
}

const formatDate = (s: string) => {
  if (!s) return '-'
  return new Date(s).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const getStatusBadge = (s: number) => s === 1 ? { label: '正常', cls: 'active' } : { label: '禁用', cls: 'disabled' }

const getRoleBadge = (name: string) => {
  if (name === '超级管理员' || name === '系统管理员') return 'admin'
  if (name === '普通用户' || name === '访客') return 'user'
  return 'other'
}

const adminUserCount = computed(() => {
  return users.value.filter((u: any) => u.roles?.some((r: string) => r === '超级管理员' || r === '系统管理员')).length
})

const handleExportCSV = () => {
  if (users.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  const headers = ['id', 'username', 'nickname', 'email', 'status', 'createTime']
  const exportData = users.value.map(item => ({
    id: item.id, username: item.username, nickname: item.nickname,
    email: item.email, status: item.status === 1 ? '正常' : '禁用',
    createTime: new Date(item.createTime).toLocaleString('zh-CN')
  }))
  exportToCSV(exportData, headers, '用户列表')
  ElMessage.success('导出成功')
}

const handleExportJSON = () => {
  if (users.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  const exportData = users.value.map(item => ({
    id: item.id, username: item.username, nickname: item.nickname,
    email: item.email, status: item.status === 1 ? '正常' : '禁用',
    createTime: item.createTime
  }))
  exportToJSON(exportData, '用户列表')
  ElMessage.success('导出成功')
}

onMounted(() => { fetchUsers(); fetchUserStats() })
</script>

<template>
  <div class="user-manage" v-loading="loading" element-loading-background="rgba(255,255,255,0.85)">
    <div class="page-hd">
      <div class="hd-left">
        <h2 class="hd-title">用户管理</h2>
        <p class="hd-sub">管理系统注册用户及其角色分配</p>
      </div>
      <div class="hd-actions">
        <div class="export-group">
          <button class="btn-ghost" @click="handleExportCSV">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span>CSV</span>
          </button>
          <button class="btn-ghost" @click="handleExportJSON">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span>JSON</span>
          </button>
        </div>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card" style="--accent:#3b82f6;--delay:0s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
        <div class="stat-info"><span class="stat-num">{{ userStats.total }}</span><span class="stat-label">用户总数</span></div>
      </div>
      <div class="stat-card" style="--accent:#10b981;--delay:0.06s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        <div class="stat-info"><span class="stat-num">{{ userStats.active }}</span><span class="stat-label">正常</span></div>
      </div>
      <div class="stat-card" style="--accent:#ef4444;--delay:0.12s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <div class="stat-info"><span class="stat-num">{{ userStats.disabled }}</span><span class="stat-label">已禁用</span></div>
      </div>
      <div class="stat-card" style="--accent:#a855f7;--delay:0.18s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        <div class="stat-info"><span class="stat-num">{{ adminUserCount }}</span><span class="stat-label">管理角色数</span></div>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-input-wrap">
        <svg class="search-input-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model="searchKeyword"
          class="search-input"
          type="text"
          placeholder="搜索用户名、昵称、邮箱..."
          @keyup.enter="fetchUsers"
        />
        <button v-if="searchKeyword" class="search-input-clear" @click="searchKeyword = ''; fetchUsers()">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </div>

    <div class="table-panel">
      <div class="panel-hd">
        <div class="panel-hd-left">
          <svg class="hd-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
          <span>用户列表</span>
        </div>
        <div class="panel-hd-right">
          <span class="panel-count">共 {{ pagination.total }} 项</span>
        </div>
      </div>
      <div class="table-scroll">
        <table class="user-table">
          <thead>
            <tr>
              <th class="col-id">用户ID</th>
              <th class="col-user">用户名</th>
              <th class="col-email">邮箱</th>
              <th class="col-role1">角色</th>
              <th class="col-status">状态</th>
              <th class="col-time">注册时间</th>
              <th class="col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in users" :key="row.id" class="table-row" :style="{'--idx':i}">
              <td class="col-id"><span class="id-text">{{ row.id }}</span></td>
              <td class="col-user">
                <div class="user-cell">
                  <div class="user-avatar">{{ row.username.charAt(0) }}</div>
                  <div class="user-meta">
                    <span class="user-name">{{ row.username }}</span>
                  </div>
                </div>
              </td>
              <td class="col-email"><span class="email-text">{{ row.email || '—' }}</span></td>
              <td class="col-role1">
                <div class="role-tags">
                  <span v-if="row.roles?.length" v-for="r in row.roles" :key="r" :class="['role-tag', getRoleBadge(r)]">{{ r }}</span>
                  <span v-else class="no-role">—</span>
                </div>
              </td>
              <td class="col-status">
                <span :class="['status-badge', getStatusBadge(row.status).cls]">{{ getStatusBadge(row.status).label }}</span>
              </td>
              <td class="col-time"><span class="time-text">{{ formatDate(row.createTime) }}</span></td>
              <td class="col-ops">
                <div class="ops-group">
                  <button class="ops-btn role" title="分配角色" @click="handleAssignRole(row)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                    <span>角色</span>
                  </button>
                  <button class="ops-btn perm" title="分配权限" @click="handleAssignPermission(row)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                    <span>权限</span>
                  </button>
                  <button :class="['ops-btn', row.status === 1 ? 'warn' : 'succ']" @click="handleShowStatusConfirm(row)">
                    <template v-if="row.status === 1">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                      <span>禁用</span>
                    </template>
                    <template v-else>
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                      <span>启用</span>
                    </template>
                  </button>
                  <button class="ops-btn danger" title="注销用户" @click="handleShowDelete(row)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                    <span>注销</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && users.length === 0">
              <td colspan="6" class="empty-row">
                <div class="empty-cell">
                  <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
                  <p>暂无用户数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next, ->, total"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 分配角色弹窗 -->
    <Teleport to="body">
      <div v-if="roleDialogVisible" class="modal-overlay" @click.self="roleDialogVisible = false">
        <div class="modal-container">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon perm">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">分配角色</h3>
                <p class="modal-desc">为「<strong style="color:#334155">{{ currentUser?.nickname || currentUser?.username }}</strong>」选择角色</p>
              </div>
              <button class="modal-close" @click="roleDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body role-body">
              <div v-if="roleDialogLoading" class="role-loading">
                <div class="loading-spinner" />
                <p>正在加载角色列表...</p>
              </div>
              <div v-else-if="roles.length === 0" class="role-empty">
                <p>暂无可用角色，请先创建角色</p>
              </div>
              <div v-else class="role-list">
                <div v-for="(role, ri) in roles" :key="role.id" :class="['role-item', { selected: selectedRoleIds.includes(role.id) }]" :style="{'--ri':ri}" @click="toggleRole(role.id)">
                  <div class="role-left">
                    <div class="role-avatar" :class="getRoleBadge(role.roleCode)">
                      {{ role.roleName.charAt(0) }}
                    </div>
                    <div class="role-info">
                      <span class="role-name">{{ role.roleName }}</span>
                      <span class="role-code">{{ role.roleCode }}</span>
                    </div>
                  </div>
                  <div class="role-right">
                    <span class="role-desc">{{ role.description || '—' }}</span>
                    <span :class="['role-check', { checked: selectedRoleIds.includes(role.id) }]">
                      <svg v-if="selectedRoleIds.includes(role.id)" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="roleDialogVisible = false">取消</button>
              <button class="m-btn m-btn-primary" @click="handleSaveRole">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                <span>保存分配</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 注销确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteDialogVisible" class="modal-overlay" @click.self="deleteDialogVisible = false">
        <div class="modal-container" style="width:420px">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon" style="background:linear-gradient(135deg,rgba(239,68,68,0.2),rgba(239,68,68,0.05));color:#f87171">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">确认注销用户</h3>
                <p class="modal-desc">此操作不可恢复，所有相关数据将被永久删除</p>
              </div>
              <button class="modal-close" @click="deleteDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body" style="padding:16px 28px">
              <div style="display:flex;align-items:center;gap:12px;padding:14px 16px;border-radius:10px;background:rgba(239,68,68,0.06);border:1px solid rgba(239,68,68,0.12)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f87171" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span style="font-size:14px;color:#fca5a5;line-height:1.5">
                  即将注销用户 <strong style="color:#0f172a">「{{ deletingUserName }}」</strong>，该用户的所有文章、评论、收藏、点赞、分类等数据将被永久删除。
                </span>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="deleteDialogVisible = false">取消</button>
              <button class="m-btn" style="background:linear-gradient(135deg,#ef4444,#dc2626);color:#fff;box-shadow:0 4px 16px rgba(239,68,68,0.3)" @click="handleConfirmDelete">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <span>确认注销</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 分配权限弹窗 -->
    <Teleport to="body">
      <div v-if="permDialogVisible" class="modal-overlay" @click.self="permDialogVisible = false">
        <div class="modal-container perm-modal">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon perm">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">分配权限</h3>
                <p class="modal-desc">为「<strong style="color:#334155">{{ currentUser?.nickname || currentUser?.username }}</strong>」<span style="color:#a78bfa">直接分配权限</span>（独立于角色权限）</p>
              </div>
              <button class="modal-close" @click="permDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body perm-body">
              <div v-if="permDialogLoading" class="role-loading">
                <div class="loading-spinner" />
                <p>正在加载权限列表...</p>
              </div>
              <div v-else-if="permissionTree.length === 0" class="tree-empty" style="padding:40px 0;text-align:center;color:#64748b">
                <p>暂无权限数据，请先创建权限菜单</p>
              </div>
              <div v-else class="perm-tree-wrap">
                <div v-for="(node, ni) in permissionTree" :key="node.id" class="perm-node" :style="{'--ni':ni}">
                  <div class="perm-node-hd">
                    <label class="perm-check">
                      <input type="checkbox" :checked="checkedPermissionIds.includes(node.id)" @change="toggleUserPerm(node.id)" />
                      <span class="perm-check-mark" />
                    </label>
                    <svg class="perm-node-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/></svg>
                    <span class="perm-node-name">{{ node.name }}</span>
                    <span class="perm-node-perm">{{ node.permission }}</span>
                  </div>
                  <div v-if="node.children?.length" class="perm-children">
                    <template v-for="child in node.children" :key="child.id">
                      <div class="perm-child">
                        <label class="perm-check">
                          <input type="checkbox" :checked="checkedPermissionIds.includes(child.id)" @change="toggleUserPerm(child.id)" />
                          <span class="perm-check-mark" />
                        </label>
                        <svg class="perm-child-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <template v-if="child.type === 1"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></template>
                          <template v-else><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></template>
                        </svg>
                        <span class="perm-child-name">{{ child.name }}</span>
                        <span class="perm-child-perm">{{ child.permission }}</span>
                        <span :class="['perm-child-type', child.type === 1 ? 'menu' : 'btn']">{{ child.type === 1 ? '菜单' : '按钮' }}</span>
                      </div>
                      <div v-for="grand in (child.children || [])" :key="grand.id" class="perm-child">
                        <label class="perm-check">
                          <input type="checkbox" :checked="checkedPermissionIds.includes(grand.id)" @change="toggleUserPerm(grand.id)" />
                          <span class="perm-check-mark" />
                        </label>
                        <svg class="perm-child-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
                        <span class="perm-child-name">{{ grand.name }}</span>
                        <span class="perm-child-perm">{{ grand.permission }}</span>
                        <span class="perm-child-type btn">按钮</span>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="permDialogVisible = false">取消</button>
              <button class="m-btn m-btn-primary" @click="handleSaveUserPermission">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                <span>保存权限</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 启用/禁用确认弹窗 -->
    <Teleport to="body">
      <div v-if="statusDialogVisible" class="modal-overlay" @click.self="statusDialogVisible = false">
        <div class="modal-container" style="width:420px">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon" :style="{background: statusNewValue === 0 ? 'linear-gradient(135deg,rgba(239,68,68,0.2),rgba(239,68,68,0.05))' : 'linear-gradient(135deg,rgba(16,185,129,0.2),rgba(16,185,129,0.05))', color: statusNewValue === 0 ? '#f87171' : '#34d399'}">
                <template v-if="statusNewValue === 0">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                </template>
                <template v-else>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                </template>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">{{ statusNewValue === 0 ? '确认禁用用户' : '确认启用用户' }}</h3>
                <p class="modal-desc">{{ statusNewValue === 0 ? '禁用后该用户将无法登录系统' : '启用后该用户可正常登录系统' }}</p>
              </div>
              <button class="modal-close" @click="statusDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body" style="padding:16px 28px">
              <div style="display:flex;align-items:center;gap:12px;padding:14px 16px;border-radius:10px;background:rgba(245,158,11,0.06);border:1px solid rgba(245,158,11,0.12)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span style="font-size:14px;color:#fcd34d;line-height:1.5">
                  {{ statusNewValue === 0 ? '即将' : '即将' }}<strong style="color:#0f172a">{{ statusNewValue === 0 ? '禁用' : '启用' }}</strong>用户 <strong style="color:#0f172a">「{{ statusTargetUser?.nickname || statusTargetUser?.username }}」</strong>，{{ statusNewValue === 0 ? '该用户将无法登录系统。' : '该用户可恢复正常使用。' }}
                </span>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="statusDialogVisible = false">取消</button>
              <button :class="['m-btn']" :style="statusNewValue === 0 ? 'background:linear-gradient(135deg,#ef4444,#dc2626);color:#fff;box-shadow:0 4px 16px rgba(239,68,68,0.3)' : 'background:linear-gradient(135deg,#10b981,#059669);color:#fff;box-shadow:0 4px 16px rgba(16,185,129,0.3)'" @click="handleConfirmStatusToggle">
                <template v-if="statusNewValue === 0">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                  <span>确认禁用</span>
                </template>
                <template v-else>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                  <span>确认启用</span>
                </template>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.user-manage { padding: 28px 32px; min-height: 100%; }

.page-hd { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.hd-left { display: flex; flex-direction: column; gap: 6px; }
.hd-title { margin: 0; font-size: 26px; font-weight: 700; color: #0f172a; letter-spacing: -0.5px; }
.hd-sub { margin: 0; font-size: 14px; color: #64748b; }
.hd-actions { display: flex; gap: 10px; align-items: center; }
.export-group { display: flex; gap: 6px; }

.btn-ghost {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px; border-radius: 10px;
  font-size: 13px; font-weight: 500;
  border: 1px solid rgba(203,213,225,0.3); cursor: pointer;
  background: rgba(203,213,225,0.15); color: #94a3b8;
  transition: all 0.2s;
}
.btn-ghost:hover { background: rgba(203,213,225,0.3); color: #475569; }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card {
  position: relative; display: flex; align-items: center; gap: 16px;
  padding: 18px 20px; border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden; animation: statIn 0.5s ease backwards; animation-delay: var(--delay);
}
@keyframes statIn { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }
.stat-card:hover { border-color: rgba(59,130,246,0.12); }
.stat-glow { position: absolute; right:-30px; top:-30px; width:100px; height:100px; border-radius:50%; background:radial-gradient(circle, var(--accent), transparent); opacity:0.06; pointer-events:none; transition:all 0.4s; }
.stat-card:hover .stat-glow { transform:scale(1.5); opacity:0.12; }
.stat-icon { width:36px; height:36px; padding:8px; border-radius:10px; background:linear-gradient(135deg, color-mix(in srgb, var(--accent)20%, transparent), transparent); color:var(--accent); flex-shrink:0; }
.stat-info { display:flex; flex-direction:column; gap:2px; }
.stat-num { font-size:24px; font-weight:800; color:#0f172a; line-height:1; }
.stat-label { font-size:12px; color:#64748b; font-weight:500; }

/* Table Panel */
.table-panel { border-radius:14px; background:#ffffff; border:1px solid rgba(203,213,225,0.3); overflow:hidden; }
.search-bar {
  padding: 0 0 12px;
}
.search-bar .search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 320px;
}
.search-bar .search-input-icon {
  position: absolute;
  left: 12px;
  color: #64748b;
  pointer-events: none;
}
.search-bar .search-input {
  width: 100%;
  height: 36px;
  padding: 0 32px 0 36px;
  border: 1px solid rgba(203,213,225,0.4);
  border-radius: 10px;
  background: rgba(203,213,225,0.2);
  color: #334155;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}
.search-bar .search-input::placeholder { color: #64748b; }
.search-bar .search-input:focus {
  border-color: rgba(59,130,246,0.3);
  background: rgba(203,213,225,0.3);
}
.search-bar .search-input-clear {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}
.search-bar .search-input-clear:hover { color: #334155; background: rgba(203,213,225,0.4); }
.panel-hd { display:flex; align-items:center; justify-content:space-between; padding:16px 20px; border-bottom:1px solid rgba(203,213,225,0.2); }
.panel-hd-left { display:flex; align-items:center; gap:9px; font-size:14px; font-weight:600; color:#334155; }
.hd-icon { color:#60a5fa; flex-shrink:0; }
.panel-count { font-size:12px; color:#94a3b8; padding:4px 12px; border-radius:12px; background:rgba(203,213,225,0.2); }
.table-scroll { overflow-x:auto; }

.user-table { width:100%; border-collapse:collapse; }
.user-table th { text-align:left; font-size:12px; font-weight:600; color:#64748b; text-transform:uppercase; letter-spacing:0.4px; padding:14px 16px; border-bottom:1px solid rgba(203,213,225,0.3); white-space:nowrap; }
.user-table td { padding:14px 16px; border-bottom:1px solid rgba(203,213,225,0.2); font-size:14px; color:#475569; }
.table-row { animation:rowIn 0.35s ease backwards; animation-delay:calc(var(--idx) * 0.04s); transition:background 0.2s; }
.table-row:hover { background:rgba(59,130,246,0.03); }
.table-row:last-child td { border-bottom:none; }
@keyframes rowIn { from { opacity:0; transform:translateX(-6px); } to { opacity:1; transform:translateX(0); } }

.col-id { width:70px; }
.col-user { min-width:200px; }
.col-email { min-width:180px; }
.col-role1 { min-width:140px; }
.col-status { width:90px; }
.col-time { width:160px; }
.col-ops { width:150px; text-align:center; }

.id-text { color:#64748b; font-family:'SF Mono',monospace; font-size:13px; }

.user-cell { display:flex; align-items:center; gap:10px; }
.user-avatar { width:34px; height:34px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:14px; font-weight:700; background:linear-gradient(135deg,rgba(59,130,246,0.2),rgba(59,130,246,0.05)); color:#60a5fa; flex-shrink:0; }
.user-meta { display:flex; flex-direction:column; gap:1px; min-width:0; }
.user-name { font-weight:600; color:#334155; font-size:14px; }
.user-uname { font-size:12px; color:#64748b; }
.email-text { color:#94a3b8; font-size:13px; }

.status-badge { font-size:11px; font-weight:600; padding:3px 10px; border-radius:6px; letter-spacing:0.3px; }
.status-badge.active { background:rgba(16,185,129,0.12); color:#34d399; }
.status-badge.disabled { background:rgba(239,68,68,0.12); color:#f87171; }

.time-text { color:#64748b; font-size:13px; }

.role-tags { display:flex; flex-wrap:wrap; gap:4px; }
.role-tag { font-size:11px; font-weight:600; padding:2px 8px; border-radius:5px; letter-spacing:0.2px; white-space:nowrap; }
.role-tag.admin { background:rgba(59,130,246,0.12); color:#60a5fa; }
.role-tag.user { background:rgba(16,185,129,0.12); color:#34d399; }
.role-tag.other { background:rgba(245,158,11,0.12); color:#fbbf24; }
.no-role { color:#475569; font-size:13px; }
.empty-row td { padding:60px 16px; }
.empty-cell { display:flex; flex-direction:column; align-items:center; gap:10px; color:#64748b; }
.empty-cell p { margin:0; }

.ops-group { display:flex; justify-content:center; gap:2px; }
.ops-btn {
  display:inline-flex; align-items:center; gap:4px;
  padding:5px 10px; border-radius:8px; border:none; cursor:pointer;
  background:transparent; font-size:12px; font-weight:500;
  color:#64748b; white-space:nowrap; transition:all 0.2s;
}
.ops-btn svg { flex-shrink:0; }
.ops-btn.role:hover { background:rgba(59,130,246,0.12); color:#60a5fa; }
.ops-btn.perm:hover { background:rgba(168,85,247,0.12); color:#c084fc; }
.ops-btn.warn:hover { background:rgba(245,158,11,0.12); color:#fbbf24; }
.ops-btn.succ:hover { background:rgba(16,185,129,0.12); color:#34d399; }
.ops-btn.danger:hover { background:rgba(239,68,68,0.12); color:#f87171; }

.pagination-wrap { display:flex; justify-content:flex-end; padding:16px 20px; border-top:1px solid rgba(203,213,225,0.2); }

/* Modal Shared */
.modal-overlay { position:fixed; inset:0; z-index:9999; display:flex; align-items:center; justify-content:center; background:rgba(0,0,0,0.4); backdrop-filter:blur(12px); -webkit-backdrop-filter:blur(12px); animation:overlayIn 0.3s ease; }
@keyframes overlayIn { from { opacity:0; backdrop-filter:blur(0); -webkit-backdrop-filter:blur(0); } to { opacity:1; backdrop-filter:blur(12px); -webkit-backdrop-filter:blur(12px); } }
.modal-container { position:relative; width:520px; max-height:90vh; animation:modalFloat 0.4s cubic-bezier(0.22,1,0.36,1); }
@keyframes modalFloat { from { opacity:0; transform:scale(0.92) translateY(20px); } to { opacity:1; transform:scale(1) translateY(0); } }
.modal-bg { position:absolute; inset:-60px; pointer-events:none; overflow:hidden; border-radius:24px; }
.modal-glass { position:relative; background:#ffffff; border:1px solid rgba(203,213,225,0.3); border-radius:20px; box-shadow:0 24px 80px rgba(0,0,0,0.4), inset 0 1px 0 rgba(203,213,225,0.3); overflow:hidden; }
.modal-header { display:flex; align-items:flex-start; gap:14px; padding:24px 28px 16px; border-bottom:1px solid rgba(203,213,225,0.3); }
.modal-hd-icon { width:44px; height:44px; border-radius:12px; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.modal-hd-icon.perm { background:linear-gradient(135deg,rgba(168,85,247,0.2),rgba(168,85,247,0.05)); color:#c084fc; }
.modal-hd-text { flex:1; min-width:0; }
.modal-title { margin:0; font-size:17px; font-weight:700; color:#0f172a; letter-spacing:-0.3px; }
.modal-desc { margin:3px 0 0; font-size:13px; color:#64748b; }
.modal-close { width:32px; height:32px; border-radius:8px; border:none; background:rgba(203,213,225,0.2); color:#64748b; cursor:pointer; display:flex; align-items:center; justify-content:center; flex-shrink:0; transition:all 0.2s; }
.modal-close:hover { background:rgba(239,68,68,0.12); color:#f87171; }
.modal-body { padding:20px 28px 12px; display:flex; flex-direction:column; gap:16px; max-height:60vh; overflow-y:auto; }
.modal-body::-webkit-scrollbar { width:4px; }
.modal-body::-webkit-scrollbar-thumb { background:rgba(203,213,225,0.3); border-radius:2px; }

.role-body { max-height:65vh; }
.role-loading { display:flex; flex-direction:column; align-items:center; gap:12px; padding:40px 0; color:#94a3b8; }
.role-loading p { margin:0; }
.loading-spinner { width:32px; height:32px; border:3px solid rgba(203,213,225,0.3); border-top-color:#60a5fa; border-radius:50%; animation:spin 0.8s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.role-empty { padding:40px 0; text-align:center; color:#64748b; }
.role-empty p { margin:0; }

.role-list { display:flex; flex-direction:column; gap:6px; }
.role-item {
  display:flex; align-items:center; justify-content:space-between;
  padding:12px 14px; border-radius:10px;
  border:1px solid rgba(203,213,225,0.3);
  background:rgba(203,213,225,0.15);
  cursor:pointer; transition:all 0.25s;
  animation:roleIn 0.35s ease backwards;
  animation-delay:calc(var(--ri) * 0.05s);
}
@keyframes roleIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:translateY(0); } }
.role-item:hover { border-color:rgba(59,130,246,0.15); background:rgba(59,130,246,0.04); }
.role-item.selected { border-color:rgba(59,130,246,0.3); background:rgba(59,130,246,0.08); }
.role-left { display:flex; align-items:center; gap:10px; }
.role-avatar { width:32px; height:32px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:13px; font-weight:700; flex-shrink:0; }
.role-avatar.admin { background:linear-gradient(135deg,rgba(59,130,246,0.2),rgba(59,130,246,0.05)); color:#60a5fa; }
.role-avatar.user { background:linear-gradient(135deg,rgba(16,185,129,0.2),rgba(16,185,129,0.05)); color:#34d399; }
.role-avatar.other { background:linear-gradient(135deg,rgba(245,158,11,0.2),rgba(245,158,11,0.05)); color:#fbbf24; }
.role-info { display:flex; flex-direction:column; gap:2px; }
.role-name { font-size:14px; font-weight:600; color:#334155; }
.role-code { font-size:11px; color:#64748b; font-family:'SF Mono',monospace; }
.role-right { display:flex; align-items:center; gap:12px; }
.role-desc { font-size:12px; color:#64748b; max-width:120px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.role-check {
  width:22px; height:22px; border-radius:6px;
  border:2px solid rgba(203,213,225,0.4);
  display:flex; align-items:center; justify-content:center;
  transition:all 0.25s; flex-shrink:0;
}
.role-check.checked { border-color:#60a5fa; background:linear-gradient(135deg,#3b82f6,#2563eb); box-shadow:0 2px 8px rgba(59,130,246,0.3); }

.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 28px 24px; border-top:1px solid rgba(203,213,225,0.3); }
.m-btn { display:flex; align-items:center; gap:7px; padding:10px 22px; border-radius:10px; font-size:14px; font-weight:600; border:none; cursor:pointer; transition:all 0.25s ease; position:relative; overflow:hidden; }
.m-btn-ghost { background:rgba(203,213,225,0.2); color:#94a3b8; border:1px solid rgba(203,213,225,0.3); }
.m-btn-ghost:hover { background:rgba(203,213,225,0.3); color:#475569; border-color:rgba(203,213,225,0.5); }
.m-btn-primary { background:linear-gradient(135deg,#3b82f6,#2563eb); color:#fff; box-shadow:0 4px 16px rgba(59,130,246,0.3); }
.m-btn-primary:hover { transform:translateY(-1px); box-shadow:0 6px 24px rgba(59,130,246,0.45); }
.m-btn-primary:active { transform:translateY(0) scale(0.98); }
.m-btn-shimmer { position:absolute; inset:0; background:linear-gradient(90deg,transparent,rgba(203,213,225,0.4),transparent); transform:translateX(-100%); transition:transform 0.6s; }
.m-btn-primary:hover .m-btn-shimmer { transform:translateX(100%); }

/* Permission Tree */
.perm-modal { width: 1200px !important; }
.perm-body { max-height: 68vh; padding: 20px 28px 12px; }
.perm-tree-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.perm-node {
  background: rgba(255,255,255,0.025);
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 12px;
  padding: 16px 20px;
  animation: permIn 0.35s ease backwards;
  animation-delay: calc(var(--ni) * 0.05s);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.25s;
}
.perm-node:hover {
  border-color: rgba(129,140,248,0.15);
}
@keyframes permIn { from { opacity:0; transform:translateY(8px); } to { opacity:1; transform:translateY(0); } }
.perm-node-hd {
  display: flex;
  align-items: center;
  gap: 10px;
}
.perm-check { position:relative; display:flex; align-items:center; cursor:pointer; flex-shrink:0; }
.perm-check input { position:absolute; opacity:0; width:0; height:0; }
.perm-check-mark { width:16px; height:16px; border-radius:4px; border:2px solid rgba(203,213,225,0.5); background:rgba(203,213,225,0.2); transition:all 0.2s; display:flex; align-items:center; justify-content:center; }
.perm-check input:checked + .perm-check-mark { border-color:#60a5fa; background:linear-gradient(135deg,#3b82f6,#2563eb); box-shadow:0 2px 8px rgba(59,130,246,0.3); }
.perm-check input:checked + .perm-check-mark::after { content:''; width:5px; height:8px; border:solid #fff; border-width:0 2px 2px 0; transform:rotate(45deg) translateY(-1px); }
.perm-node-icon { color:#60a5fa; flex-shrink:0; width:14px; height:14px; }
.perm-node-name { font-size:14px; font-weight:600; color:#334155; letter-spacing:0.3px; }
.perm-node-perm { display:none; }
.perm-children {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.perm-child {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 8px;
  border-radius: 6px;
  background: rgba(255,255,255,0.025);
  transition: all 0.18s;
  border: 1px solid rgba(203,213,225,0.3);
  cursor: pointer;
}
.perm-child:hover {
  background: rgba(203,213,225,0.3);
  border-color: rgba(59,130,246,0.2);
}
.perm-child-icon { color:#64748b; flex-shrink:0; width:12px; height:12px; }
.perm-child-name { font-size:13px; font-weight:500; color:#475569; }
.perm-child-perm { display:none; }
.perm-child-type { font-size:10px; font-weight:600; padding:1px 7px; border-radius:4px; letter-spacing:0.2px; margin-left:2px; }
.perm-child-type.menu { background:rgba(59,130,246,0.1); color:#60a5fa; }
.perm-child-type.btn { background:rgba(16,185,129,0.1); color:#34d399; }

/* Element Plus Overrides */
:deep(.el-pagination) { --el-pagination-bg-color:transparent; --el-pagination-text-color:#94a3b8; --el-pagination-button-bg-color:rgba(203,213,225,0.2); --el-pagination-hover-color:#60a5fa; --el-pagination-button-color:#94a3b8; }
:deep(.el-pagination button.is-active) { background:linear-gradient(135deg,#3b82f6,#60a5fa); color:#fff; }
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) { background:transparent; }
</style>
