<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleList, addRole, updateRole, deleteRole, getRoleDetail, getPermissionTree, assignRolePermission, getRolePermissionList, getPermissionList } from '@/api/admin'

const loading = ref(false)
const roles = ref<any[]>([])
const totalPermissions = ref(0)
const authorizedRoleCount = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const permDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const deletingId = ref<number | null>(null)
const deletingName = ref('')
const currentRole = ref<any>(null)
const permissionTree = ref<any[]>([])
const checkedPermissions = ref<number[]>([])

const form = ref({
  id: null as number | null,
  roleName: '',
  roleCode: '',
  remark: ''
})

const pagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const roleStats = computed(() => ({
  total: pagination.value.total,
  remarked: roles.value.filter(r => r.remark).length,
}))

const fetchRoles = async () => {
  loading.value = true
  try {
    const [roleRes, permListRes, rolePermRes]: any = await Promise.all([
      getRoleList({
        pageNum: pagination.value.pageNum,
        pageSize: pagination.value.pageSize
      }),
      getPermissionList(),
      getRolePermissionList()
    ])
    roles.value = roleRes.data?.records || []
    pagination.value.total = roleRes.data?.total || 0
    totalPermissions.value = (permListRes.data || []).length
    const roleIds = new Set((rolePermRes.data || []).map((rp: any) => rp.roleId))
    authorizedRoleCount.value = roleIds.size
  } catch {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: null, roleName: '', roleCode: '', remark: '' }
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  try {
    const res: any = await getRoleDetail(row.id)
    const d = res.data
    form.value = { id: d.id, roleName: d.roleName, roleCode: d.roleCode, remark: d.remark || '' }
    dialogTitle.value = '编辑角色'
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取角色详情失败')
  }
}

const handleSave = async () => {
  if (!form.value.roleName) { ElMessage.warning('请输入角色名称'); return }
  if (!form.value.roleCode) { ElMessage.warning('请输入角色编码'); return }
  try {
    if (isEdit.value) {
      await updateRole(form.value)
      ElMessage.success('更新成功')
    } else {
      await addRole(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchRoles()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDeleteClick = (row: any) => {
  deletingId.value = row.id
  deletingName.value = row.roleName
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (!deletingId.value) return
  try {
    await deleteRole([deletingId.value])
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    deletingId.value = null
    fetchRoles()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleAssignPermission = async (row: any) => {
  currentRole.value = row
  checkedPermissions.value = []
  try {
    const [treeRes, permListRes]: any = await Promise.all([
      getPermissionTree(),
      getRolePermissionList()
    ])
    permissionTree.value = treeRes.data || []
    // 筛选当前角色已有的权限ID
    const rolePerms = (permListRes.data || []).filter(
      (rp: any) => rp.roleId === row.id
    )
    checkedPermissions.value = rolePerms.map((rp: any) => rp.permissionId)
    permDialogVisible.value = true
  } catch {
    ElMessage.error('获取权限数据失败')
  }
}

const handleSavePermission = async () => {
  if (!currentRole.value) return
  try {
    await assignRolePermission({
      roleId: currentRole.value.id,
      permissionIds: checkedPermissions.value
    })
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
    fetchRoles()
  } catch {
    ElMessage.error('权限分配失败')
  }
}

const togglePerm = (id: number) => {
  const idx = checkedPermissions.value.indexOf(id)
  if (idx > -1) {
    checkedPermissions.value.splice(idx, 1)
  } else {
    checkedPermissions.value.push(id)
  }
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page
  fetchRoles()
}

const getCodeBadge = (code: string) => {
  if (code === 'ADMIN' || code === 'super_admin') return { label: '管理员', cls: 'badge-admin' }
  if (code === 'USER') return { label: '用户', cls: 'badge-user' }
  return { label: code, cls: 'badge-other' }
}

onMounted(() => { fetchRoles() })
</script>

<template>
  <div class="role-manage" v-loading="loading" element-loading-background="rgba(8,11,20,0.85)">
    <div class="page-hd">
      <div class="hd-left">
        <h2 class="hd-title">角色权限管理</h2>
        <p class="hd-sub">管理系统角色及其权限分配</p>
      </div>
      <button class="btn-primary" @click="handleAdd">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        <span>新增角色</span>
      </button>
    </div>

    <div class="stats-row">
      <div class="stat-card" style="--accent:#6366f1;--delay:0s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
        <div class="stat-info">
          <span class="stat-num">{{ roleStats.total }}</span>
          <span class="stat-label">角色总数</span>
        </div>
      </div>
      <div class="stat-card" style="--accent:#10b981;--delay:0.06s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        <div class="stat-info">
          <span class="stat-num">{{ authorizedRoleCount }}</span>
          <span class="stat-label">已授权角色</span>
        </div>
      </div>
      <div class="stat-card" style="--accent:#a855f7;--delay:0.12s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
        <div class="stat-info">
          <span class="stat-num">{{ totalPermissions }}</span>
          <span class="stat-label">权限总数</span>
        </div>
      </div>
      <div class="stat-card" style="--accent:#f59e0b;--delay:0.18s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        <div class="stat-info">
          <span class="stat-num">{{ roles.length ? Math.round(roleStats.remarked / roles.length * 100) : 0 }}%</span>
          <span class="stat-label">备注率</span>
        </div>
      </div>
    </div>

    <div class="table-panel">
      <div class="panel-hd">
        <div class="panel-hd-left">
          <svg class="hd-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
          <span>角色列表</span>
        </div>
        <div class="panel-hd-right">
          <span class="panel-count">共 {{ pagination.total }} 项</span>
        </div>
      </div>
      <div class="table-scroll">
        <table class="role-table">
          <thead>
            <tr>
              <th class="col-id">ID</th>
              <th class="col-name">角色名称</th>
              <th class="col-code">角色编码</th>
              <th class="col-desc">备注</th>
              <th class="col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in roles" :key="row.id" class="table-row" :style="{'--idx':i}">
              <td class="col-id"><span class="id-text">#{{ row.id }}</span></td>
              <td class="col-name">
                <div class="name-cell">
                  <div class="name-avatar" :class="getCodeBadge(row.roleCode).cls">
                    {{ row.roleName.charAt(0) }}
                  </div>
                  <div class="name-meta">
                    <span class="name-text">{{ row.roleName }}</span>
                  </div>
                </div>
              </td>
              <td class="col-code">
                <div class="code-cell">
                  <span class="code-value">{{ row.roleCode }}</span>
                  <span :class="['code-tag', getCodeBadge(row.roleCode).cls]">{{ getCodeBadge(row.roleCode).label }}</span>
                </div>
              </td>
              <td class="col-desc"><span class="desc-text">{{ row.remark || '—' }}</span></td>
              <td class="col-ops">
                <div class="ops-group">
                  <button class="ops-btn edit" @click="handleEdit(row)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                    <span>编辑</span>
                  </button>
                  <button class="ops-btn perm" @click="handleAssignPermission(row)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                    <span>权限</span>
                  </button>
                  <button class="ops-btn del" @click="handleDeleteClick(row)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                    <span>删除</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && roles.length === 0">
              <td colspan="5" class="empty-row">
                <div class="empty-cell">
                  <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
                  <p>暂无角色数据</p>
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

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal-container">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon" :class="isEdit ? 'edit' : 'add'">
                <template v-if="isEdit">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </template>
                <template v-else>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </template>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">{{ dialogTitle }}</h3>
                <p class="modal-desc">{{ isEdit ? '修改角色名称、编码与备注信息' : '创建一个新的系统角色' }}</p>
              </div>
              <button class="modal-close" @click="dialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="m-field" style="--order:1">
                <label class="m-label"><span class="m-label-text">角色名称</span><span class="m-req">*</span></label>
                <div class="m-input-wrap">
                  <input v-model="form.roleName" class="m-input" placeholder="如：超级管理员" maxlength="20" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
              </div>
              <div class="m-field" style="--order:2">
                <label class="m-label"><span class="m-label-text">角色编码</span><span class="m-req">*</span></label>
                <div class="m-input-wrap">
                  <input v-model="form.roleCode" class="m-input" placeholder="如：ADMIN / USER" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
                <p class="m-hint">系统唯一标识，用于权限校验</p>
              </div>
              <div class="m-field" style="--order:3">
                <label class="m-label"><span class="m-label-text">备注</span></label>
                <div class="m-input-wrap">
                  <textarea v-model="form.remark" class="m-input m-textarea" placeholder="可选：角色的功能描述" rows="3" />
                  <div class="m-input-border" />
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="dialogVisible = false">取消</button>
              <button class="m-btn m-btn-primary" @click="handleSave">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline v-if="isEdit" points="20 6 9 17 4 12" />
                  <template v-else>
                    <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                  </template>
                </svg>
                <span>{{ isEdit ? '保存修改' : '确认创建' }}</span>
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
                <p class="modal-desc">为「<strong style="color:#e2e8f0">{{ currentRole?.roleName }}</strong>」选择权限</p>
              </div>
              <button class="modal-close" @click="permDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body perm-body">
              <div v-if="permissionTree.length === 0" class="tree-empty">
                <p>暂无权限数据，请先创建权限菜单</p>
              </div>
              <div v-else class="perm-tree-wrap">
                <div v-for="(node, ni) in permissionTree" :key="node.id" class="perm-node" :style="{'--ni':ni}">
                  <div class="perm-node-hd">
                    <label class="perm-check">
                      <input type="checkbox" :checked="checkedPermissions.includes(node.id)" @change="togglePerm(node.id)" />
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
                          <input type="checkbox" :checked="checkedPermissions.includes(child.id)" @change="togglePerm(child.id)" />
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
                          <input type="checkbox" :checked="checkedPermissions.includes(grand.id)" @change="togglePerm(grand.id)" />
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
              <button class="m-btn m-btn-primary" @click="handleSavePermission">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <span>保存权限</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteDialogVisible" class="modal-overlay" @click.self="deleteDialogVisible = false">
        <div class="modal-container delete-modal">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon del">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">确认删除</h3>
                <p class="modal-desc">此操作不可撤销</p>
              </div>
              <button class="modal-close" @click="deleteDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="delete-body">
              <div class="delete-icon-wrap">
                <svg class="delete-icon-pulse" viewBox="0 0 80 80" fill="none">
                  <circle cx="40" cy="40" r="36" stroke="rgba(239,68,68,0.15)" stroke-width="2" stroke-dasharray="4 4"/>
                  <circle cx="40" cy="40" r="24" fill="rgba(239,68,68,0.08)"/>
                  <path d="M33 33l14 14M33 47l14-14" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
              </div>
              <p class="delete-text">确定要删除「<strong>{{ deletingName }}</strong>」吗？</p>
              <p class="delete-warn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span>如果角色已被用户关联，将无法删除</span>
              </p>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="deleteDialogVisible = false">取消</button>
              <button class="m-btn m-btn-danger" @click="handleConfirmDelete">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <span>确认删除</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.role-manage {
  padding: 28px 32px;
  min-height: 100%;
}

.page-hd {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.hd-left { display: flex; flex-direction: column; gap: 6px; }
.hd-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.5px;
}
.hd-sub {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #fff;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
  transition: all 0.25s ease;
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(99,102,241,0.45);
}

/* Stats */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  animation: statIn 0.5s ease backwards;
  animation-delay: var(--delay);
}
@keyframes statIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.stat-card:hover {
  border-color: rgba(99,102,241,0.12);
}
.stat-glow {
  position: absolute;
  right: -30px;
  top: -30px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent), transparent);
  opacity: 0.06;
  pointer-events: none;
  transition: all 0.4s;
}
.stat-card:hover .stat-glow {
  transform: scale(1.5);
  opacity: 0.12;
}
.stat-icon {
  width: 36px;
  height: 36px;
  padding: 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent) 20%, transparent), transparent);
  color: var(--accent);
  flex-shrink: 0;
}
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-num { font-size: 24px; font-weight: 800; color: #f1f5f9; line-height: 1; }
.stat-label { font-size: 12px; color: #64748b; font-weight: 500; }

/* Table Panel */
.table-panel {
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
}
.panel-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}
.panel-hd-left {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}
.hd-icon { color: #818cf8; flex-shrink: 0; }
.panel-count {
  font-size: 12px;
  color: #94a3b8;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
}
.table-scroll { overflow-x: auto; }

.role-table {
  width: 100%;
  border-collapse: collapse;
}
.role-table th {
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  white-space: nowrap;
}
.role-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
  font-size: 14px;
  color: #cbd5e1;
}
.table-row {
  animation: rowIn 0.35s ease backwards;
  animation-delay: calc(var(--idx) * 0.04s);
  transition: background 0.2s;
}
.table-row:hover { background: rgba(99,102,241,0.03); }
.table-row:last-child td { border-bottom: none; }
@keyframes rowIn {
  from { opacity: 0; transform: translateX(-6px); }
  to { opacity: 1; transform: translateX(0); }
}

.col-id { width: 70px; }
.col-name { min-width: 160px; }
.col-code { width: 130px; }
.col-desc { min-width: 180px; }
.col-ops { width: 130px; text-align: center; }

.id-text { color: #64748b; font-family: 'SF Mono', monospace; font-size: 13px; }

.name-cell { display: flex; align-items: center; gap: 10px; }
.name-avatar {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700;
  flex-shrink: 0;
}
.name-avatar.badge-admin { background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05)); color: #818cf8; }
.name-avatar.badge-user { background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05)); color: #34d399; }
.name-avatar.badge-other { background: linear-gradient(135deg, rgba(245,158,11,0.2), rgba(245,158,11,0.05)); color: #fbbf24; }
.name-meta { display: flex; flex-direction: column; min-width: 0; }
.name-text { font-weight: 600; color: #e2e8f0; }

.code-cell { display: flex; align-items: center; gap: 8px; }
.code-value { font-size: 13px; font-weight: 500; color: #cbd5e1; font-family: 'SF Mono', monospace; letter-spacing: 0.3px; }
.code-tag { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 5px; letter-spacing: 0.2px; }
.code-tag.badge-admin { background: rgba(99,102,241,0.1); color: #818cf8; }
.code-tag.badge-user { background: rgba(16,185,129,0.1); color: #34d399; }
.code-tag.badge-other { background: rgba(148,163,184,0.1); color: #94a3b8; }

.desc-text { color: #94a3b8; font-size: 13px; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }

.empty-row td { padding: 60px 16px; }
.empty-cell { display: flex; flex-direction: column; align-items: center; gap: 10px; color: #64748b; }
.empty-cell p { margin: 0; }

.ops-group { display: flex; justify-content: center; gap: 2px; }
.ops-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
  transition: all 0.2s;
}
.ops-btn svg { flex-shrink: 0; }
.ops-btn.edit:hover { background: rgba(99,102,241,0.12); color: #818cf8; }
.ops-btn.perm:hover { background: rgba(16,185,129,0.12); color: #34d399; }
.ops-btn.del:hover { background: rgba(239,68,68,0.12); color: #f87171; }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.03);
}

/* ====== Modal Shared ====== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: overlayIn 0.3s ease;
}
@keyframes overlayIn {
  from { opacity: 0; backdrop-filter: blur(0); -webkit-backdrop-filter: blur(0); }
  to { opacity: 1; backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
}
.modal-container {
  position: relative;
  width: 480px;
  max-height: 90vh;
  animation: modalFloat 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes modalFloat {
  from { opacity: 0; transform: scale(0.92) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.modal-bg {
  position: absolute; inset: -60px; pointer-events: none; overflow: hidden; border-radius: 24px;
}

.modal-glass {
  position: relative;
  background: linear-gradient(170deg, rgba(17,24,39,0.97), rgba(15,18,30,0.98));
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.6), inset 0 1px 0 rgba(255,255,255,0.04);
  overflow: hidden;
}
.modal-header {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.modal-hd-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.modal-hd-icon.add { background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05)); color: #818cf8; }
.modal-hd-icon.edit { background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05)); color: #34d399; }
.modal-hd-icon.del { background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05)); color: #f87171; }
.modal-hd-icon.perm { background: linear-gradient(135deg, rgba(168,85,247,0.2), rgba(168,85,247,0.05)); color: #c084fc; }
.modal-hd-text { flex: 1; min-width: 0; }
.modal-title { margin: 0; font-size: 17px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.3px; }
.modal-desc { margin: 3px 0 0; font-size: 13px; color: #64748b; }
.modal-close {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: rgba(255,255,255,0.03); color: #64748b;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all 0.2s;
}
.modal-close:hover { background: rgba(239,68,68,0.12); color: #f87171; }

.modal-body {
  padding: 20px 28px 12px;
  display: flex; flex-direction: column; gap: 16px;
  max-height: 55vh; overflow-y: auto;
}
.modal-body::-webkit-scrollbar { width: 4px; }
.modal-body::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.06); border-radius: 2px; }

.m-field { display: flex; flex-direction: column; gap: 6px; animation: fieldIn 0.4s ease backwards; animation-delay: calc(var(--order) * 0.07s); }
@keyframes fieldIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.m-label { display: flex; align-items: center; gap: 4px; }
.m-label-text { font-size: 13px; font-weight: 600; color: #94a3b8; }
.m-req { font-size: 14px; color: #f87171; font-weight: 700; }
.m-input-wrap { position: relative; }
.m-input {
  width: 100%; padding: 11px 16px; border-radius: 10px; border: none;
  background: rgba(255,255,255,0.03); color: #e2e8f0; font-size: 14px;
  outline: none; position: relative; z-index: 2; box-sizing: border-box;
  transition: background 0.25s; font-family: inherit;
}
.m-input:focus { background: rgba(255,255,255,0.05); }
.m-input::placeholder { color: #475569; }
.m-textarea { resize: vertical; min-height: 80px; line-height: 1.5; }
.m-input-border {
  position: absolute; inset: 0; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.08);
  pointer-events: none; z-index: 1;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.m-input:focus ~ .m-input-border { border-color: rgba(99,102,241,0.4); box-shadow: 0 0 0 3px rgba(99,102,241,0.08); }
.m-input-glow {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  width: 6px; height: 6px; border-radius: 50%;
  background: rgba(99,102,241,0.15); z-index: 2; pointer-events: none;
  transition: all 0.3s;
}
.m-input:focus ~ .m-input-glow { background: #818cf8; box-shadow: 0 0 10px rgba(99,102,241,0.4); }
.m-hint { margin: 0; font-size: 12px; color: #475569; line-height: 1.4; }

.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 16px 28px 24px;
  border-top: 1px solid rgba(255,255,255,0.04);
}
.m-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 22px; border-radius: 10px;
  font-size: 14px; font-weight: 600; border: none;
  cursor: pointer; transition: all 0.25s ease;
  position: relative; overflow: hidden;
}
.m-btn-ghost { background: rgba(255,255,255,0.03); color: #94a3b8; border: 1px solid rgba(255,255,255,0.06); }
.m-btn-ghost:hover { background: rgba(255,255,255,0.06); color: #cbd5e1; border-color: rgba(255,255,255,0.1); }
.m-btn-primary { background: linear-gradient(135deg, #6366f1, #7c3aed); color: #fff; box-shadow: 0 4px 16px rgba(99,102,241,0.3); }
.m-btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99,102,241,0.45); }
.m-btn-primary:active { transform: translateY(0) scale(0.98); }
.m-btn-shimmer {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
  transform: translateX(-100%); transition: transform 0.6s;
}
.m-btn-primary:hover .m-btn-shimmer { transform: translateX(100%); }

/* Delete Modal */
.delete-modal { width: 420px; }
.delete-body { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 12px 28px 20px; }
.delete-icon-wrap { animation: delPulse 2s ease-in-out infinite; }
@keyframes delPulse { 0%,100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.06); opacity: 0.85; } }
.delete-icon-pulse { width: 64px; height: 64px; }
.delete-text { margin: 0; font-size: 15px; color: #e2e8f0; text-align: center; }
.delete-text strong { color: #f87171; font-weight: 700; }
.delete-warn { display: flex; align-items: center; gap: 6px; margin: 0; font-size: 13px; color: #f59e0b; background: rgba(245,158,11,0.08); padding: 8px 14px; border-radius: 8px; border: 1px solid rgba(245,158,11,0.12); }
.delete-warn svg { flex-shrink: 0; color: #f59e0b; }
.m-btn-danger { background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; box-shadow: 0 4px 16px rgba(239,68,68,0.3); }
.m-btn-danger:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(239,68,68,0.45); }
.m-btn-danger:active { transform: translateY(0) scale(0.98); }

/* Perm Tree Modal */
.perm-modal { width: 1200px !important; }
.perm-body { max-height: 68vh; padding: 20px 28px 12px; }
.perm-tree-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.tree-empty { padding: 40px 0; text-align: center; color: #64748b; }
.tree-empty p { margin: 0; }
.perm-node {
  background: rgba(255,255,255,0.025);
  border: 1px solid rgba(255,255,255,0.06);
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
.perm-check { display: flex; align-items: center; cursor: pointer; position: relative; flex-shrink:0; }
.perm-check input { position: absolute; opacity: 0; width: 0; height: 0; }
.perm-check-mark {
  width: 16px; height: 16px; border-radius: 4px;
  border: 2px solid rgba(255,255,255,0.12);
  background: rgba(255,255,255,0.03);
  transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.perm-check input:checked + .perm-check-mark {
  border-color: #818cf8;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.perm-check input:checked + .perm-check-mark::after {
  content: '';
  width: 5px; height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) translateY(-1px);
}
.perm-node-icon { color: #818cf8; flex-shrink: 0; width: 14px; height: 14px; }
.perm-node-name { font-size: 14px; font-weight: 600; color: #e2e8f0; letter-spacing: 0.3px; }
.perm-node-perm { display: none; }

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
  border: 1px solid rgba(255,255,255,0.04);
  cursor: pointer;
}
.perm-child:hover {
  background: rgba(255,255,255,0.06);
  border-color: rgba(99,102,241,0.2);
}
.perm-child-icon { color: #64748b; flex-shrink: 0; width: 12px; height: 12px; }
.perm-child-name { font-size: 13px; font-weight: 500; color: #cbd5e1; }
.perm-child-perm { display: none; }
.perm-child-type { font-size: 10px; font-weight: 600; padding: 1px 7px; border-radius: 4px; letter-spacing: 0.2px; margin-left: 2px; }
.perm-child-type.menu { background: rgba(99,102,241,0.1); color: #818cf8; }
.perm-child-type.btn { background: rgba(16,185,129,0.1); color: #34d399; }

/* Element Plus Overrides */
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #94a3b8;
  --el-pagination-button-bg-color: rgba(255,255,255,0.03);
  --el-pagination-hover-color: #818cf8;
  --el-pagination-button-color: #94a3b8;
}
:deep(.el-pagination button.is-active) {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
}
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) {
  background: transparent;
}
</style>
