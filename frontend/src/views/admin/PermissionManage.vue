<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getPermissionList,
  getPermissionTree,
  addPermission,
  updatePermission,
  deletePermission,
  getPermissionDetail
} from '@/api/admin'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const treeData = ref<any[]>([])
const flatList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const editingNode = ref<any>(null)
const selectedNode = ref<any>(null)
const deleteDialogVisible = ref(false)
const deletingNode = ref<any>(null)

const form = ref({
  id: null as number | null,
  name: '',
  permission: '',
  path: '',
  parentId: null as number | null,
  type: 1,
})

const menuTypeOptions = [
  { value: 1, label: '菜单', desc: '侧边栏导航菜单' },
  { value: 2, label: '按钮', desc: '页面内的功能按钮' },
]

const fetchData = async () => {
  loading.value = true
  try {
    const [treeRes, listRes] = await Promise.all([
      getPermissionTree(),
      getPermissionList()
    ])
    treeData.value = treeRes.data || []
    flatList.value = listRes.data || []
  } catch {
    ElMessage.error('获取权限列表失败')
  } finally {
    loading.value = false
  }
}

const pendingParentId = ref<number | null>(null)

const handleAdd = (parent?: any) => {
  isEdit.value = false
  editingNode.value = null
  pendingParentId.value = parent?.id || null
  form.value = {
    id: null,
    name: '',
    permission: '',
    path: parent ? `${parent.path}/` : '/',
    parentId: parent?.id || null,
    type: parent ? 2 : 1,
  }
  dialogTitle.value = parent ? `在「${parent.name}」下新增` : '新增根菜单'
  dialogVisible.value = true
}

const handleAddRoot = () => {
  handleAdd(null)
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  editingNode.value = row
  try {
    const res: any = await getPermissionDetail(row.id)
    const data = res.data
    form.value = {
      id: data.id,
      name: data.name,
      permission: data.permission,
      path: data.path,
      parentId: data.parentId,
      type: data.type,
    }
    dialogTitle.value = '编辑权限菜单'
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取权限详情失败')
  }
}

const handleDelete = (row: any) => {
  deletingNode.value = row
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (!deletingNode.value) return
  try {
    await deletePermission(deletingNode.value.id)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    deletingNode.value = null
    selectedNode.value = null
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleSave = async () => {
  if (!form.value.name) { ElMessage.warning('请输入权限名称'); return }
  if (!form.value.permission) { ElMessage.warning('请输入权限标识'); return }
  if (!form.value.path) { ElMessage.warning('请输入菜单路径'); return }

  try {
    if (isEdit.value) {
      await updatePermission(form.value)
      ElMessage.success('更新成功')
    } else {
      if (pendingParentId.value) {
        form.value.parentId = pendingParentId.value
      }
      await addPermission(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    pendingParentId.value = null
    fetchData()
  } catch {
    ElMessage.error('保存失败')
  }
}

const getTypeTag = (type: number) => {
  return type === 1
    ? { label: '菜单', cls: 'tag-menu' }
    : { label: '按钮', cls: 'tag-btn' }
}

const parentOptions = computed(() => {
  const opts = [{ id: null, name: '（作为根节点）', level: -1 }]
  const flatten = (items: any[], level: number) => {
    items.forEach((item: any) => {
      opts.push({ ...item, level })
      if (item.children?.length) flatten(item.children, level + 1)
    })
  }
  flatten(treeData.value, 0)
  return opts
})

const getParentLabel = (parentId: number | null) => {
  if (!parentId) return '—'
  const find = (items: any[]): any => {
    for (const item of items) {
      if (item.id === parentId) return item
      if (item.children) {
        const found = find(item.children)
        if (found) return found
      }
    }
    return null
  }
  return find(treeData.value)?.name || '—'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="perm-manage" v-loading="loading" element-loading-background="rgba(255,255,255,0.85)">
    <div class="page-hd">
      <div class="hd-left">
        <h2 class="hd-title">权限菜单管理</h2>
        <p class="hd-sub">管理系统所有权限菜单与功能按钮</p>
      </div>
      <button class="btn-primary" @click="handleAddRoot">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        <span>新建根菜单</span>
      </button>
    </div>

    <div class="content-grid">
      <div class="panel tree-panel">
        <div class="panel-hd">
          <div class="panel-hd-left">
            <svg class="hd-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>
            <span>权限树</span>
          </div>
          <span class="panel-count">{{ flatList.length }} 项</span>
        </div>
        <div class="tree-body">
          <div v-if="treeData.length === 0" class="tree-empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.3)" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
            <p>暂无权限数据</p>
            <button class="btn-empty" @click="handleAddRoot">立即创建</button>
          </div>
          <div class="tree-scroll">
            <div v-for="(item, i) in treeData" :key="item.id" class="tree-root">
              <div
                class="tree-node root-node"
                :class="{ selected: selectedNode?.id === item.id }"
                @click="selectedNode = item"
              >
                <div class="node-icon root-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/></svg>
                </div>
                <div class="node-body">
                  <span class="node-name">{{ item.name }}</span>
                  <span class="node-perm">{{ item.permission }}</span>
                </div>
                <span :class="['node-type', getTypeTag(item.type).cls]">{{ getTypeTag(item.type).label }}</span>
                <div class="node-ops">
                  <button class="op-btn add" @click.stop="handleAdd(item)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    <span>新增</span>
                  </button>
                  <button class="op-btn edit" @click.stop="handleEdit(item)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                    <span>编辑</span>
                  </button>
                  <button class="op-btn del" @click.stop="handleDelete(item)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                    <span>删除</span>
                  </button>
                </div>
              </div>
              <template v-if="item.children?.length">
                <div
                  v-for="(child, ci) in item.children"
                  :key="child.id"
                  class="tree-node child-node"
                  :class="{ selected: selectedNode?.id === child.id }"
                  :style="{ '--idx': ci }"
                  @click="selectedNode = child"
                >
                  <div class="node-connector" />
                  <div class="node-icon child-icon">
                    <template v-if="child.type === 1">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
                    </template>
                    <template v-else>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
                    </template>
                  </div>
                  <div class="node-body">
                    <span class="node-name">{{ child.name }}</span>
                    <span class="node-perm">{{ child.permission }}</span>
                  </div>
                  <span :class="['node-type', getTypeTag(child.type).cls]">{{ getTypeTag(child.type).label }}</span>
                  <div class="node-ops">
                    <button class="op-btn add" @click.stop="handleAdd(child)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                      <span>新增</span>
                    </button>
                    <button class="op-btn edit" @click.stop="handleEdit(child)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                      <span>编辑</span>
                    </button>
                    <button class="op-btn del" @click.stop="handleDelete(child)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                      <span>删除</span>
                    </button>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div class="panel info-panel">
        <div class="panel-hd">
          <div class="panel-hd-left">
            <svg class="hd-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            <span>详情</span>
          </div>
        </div>
        <div class="info-body">
          <div v-if="!selectedNode" class="info-empty">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            <p>点击左侧节点查看详情</p>
          </div>
          <div v-else class="info-content">
            <div class="info-avatar" :class="selectedNode.type === 1 ? 'menu' : 'btn'">
              <template v-if="selectedNode.type === 1">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="13" y2="13"/></svg>
              </template>
              <template v-else>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
              </template>
            </div>
            <h3 class="info-name">{{ selectedNode.name }}</h3>
            <span :class="['info-badge', getTypeTag(selectedNode.type).cls]">{{ getTypeTag(selectedNode.type).label }}</span>

            <div class="info-fields">
              <div class="info-row">
                <span class="info-key">权限标识</span>
                <span class="info-val code">{{ selectedNode.permission }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">路由路径</span>
                <span class="info-val"><code>{{ selectedNode.path }}</code></span>
              </div>
              <div class="info-row">
                <span class="info-key">父级菜单</span>
                <span class="info-val">{{ getParentLabel(selectedNode.parentId) }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">ID</span>
                <span class="info-val"><code>#{{ selectedNode.id }}</code></span>
              </div>
            </div>

            <div class="info-actions">
              <button class="info-act edit" @click="handleEdit(selectedNode)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                <span>编辑</span>
              </button>
              <button class="info-act add" @click="handleAdd(selectedNode)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                <span>新增子项</span>
              </button>
              <button class="info-act del" @click="handleDelete(selectedNode)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <span>删除</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteDialogVisible" class="modal-overlay" @click.self="deleteDialogVisible = false">
        <div class="modal-container delete-modal">
          <div class="modal-bg">
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon del">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">确认删除</h3>
                <p class="modal-desc">此操作不可撤销，请谨慎确认</p>
              </div>
              <button class="modal-close" @click="deleteDialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
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
              <p class="delete-text">
                确定要删除「<strong>{{ deletingNode?.name }}</strong>」吗？
              </p>
              <p class="delete-warn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span>如果存在子菜单，将一并删除</span>
              </p>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="deleteDialogVisible = false">取消</button>
              <button class="m-btn m-btn-danger" @click="handleConfirmDelete">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <span>确认删除</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

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
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </template>
                <template v-else>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </template>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">{{ dialogTitle }}</h3>
                <p class="modal-desc">{{ isEdit ? '修改权限菜单的配置信息' : '创建一个新的权限菜单项' }}</p>
              </div>
              <button class="modal-close" @click="dialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="m-field" style="--order: 1">
                <label class="m-label">
                  <span class="m-label-text">权限名称</span>
                  <span class="m-req">*</span>
                </label>
                <div class="m-input-wrap">
                  <input v-model="form.name" class="m-input" placeholder="如：用户管理" maxlength="20" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
              </div>
              <div class="m-field" style="--order: 2">
                <label class="m-label">
                  <span class="m-label-text">权限标识</span>
                  <span class="m-req">*</span>
                </label>
                <div class="m-input-wrap">
                  <input v-model="form.permission" class="m-input" placeholder="如：sys:user:list" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
                <p class="m-hint">系统唯一标识，用于后端 @RequirePermission 注解校验</p>
              </div>
              <div class="m-field" style="--order: 3">
                <label class="m-label">
                  <span class="m-label-text">路由路径</span>
                  <span class="m-req">*</span>
                </label>
                <div class="m-input-wrap">
                  <input v-model="form.path" class="m-input" placeholder="如：/system/users" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
              </div>
              <div class="m-field" style="--order: 4">
                <label class="m-label"><span class="m-label-text">类型</span></label>
                <div class="m-card-group">
                  <div
                    v-for="opt in menuTypeOptions"
                    :key="opt.value"
                    class="m-card"
                    :class="{ active: form.type === opt.value }"
                    @click="form.type = opt.value"
                  >
                    <div class="m-card-indicator">
                      <div class="m-card-dot" />
                    </div>
                    <div class="m-card-info">
                      <span class="m-card-title">{{ opt.label }}</span>
                      <span class="m-card-desc">{{ opt.desc }}</span>
                    </div>
                    <div class="m-card-bg" />
                  </div>
                </div>
              </div>
              <div class="m-field" style="--order: 5">
                <label class="m-label"><span class="m-label-text">父级菜单</span></label>
                <div class="m-select-list">
                  <div
                    v-for="opt in parentOptions"
                    :key="opt.id ?? 'root'"
                    class="m-select-item"
                    :class="{ active: form.parentId === opt.id }"
                    @click="form.parentId = opt.id"
                  >
                    <div class="m-select-check">
                      <svg v-if="form.parentId === opt.id" class="check-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                    </div>
                    <span class="m-select-indent" :style="{ paddingLeft: (opt.level + 1) * 16 + 'px' }">
                      <svg v-if="opt.level >= 0" class="m-select-arrow" width="12" height="12" viewBox="0 0 14 14" fill="none"><path d="M4 3l4 4-4 4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
                      <span class="m-select-name">{{ opt.name }}</span>
                      <span v-if="opt.level >= 0" class="m-select-level">{{ opt.level === 0 ? '根菜单' : `第 ${opt.level + 1} 级` }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="dialogVisible = false">
                <span>取消</span>
              </button>
              <button class="m-btn m-btn-primary" @click="handleSave">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline v-if="isEdit" points="20 6 9 17 4 12" />
                  <template v-else>
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </template>
                </svg>
                <span>{{ isEdit ? '保存修改' : '确认创建' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.perm-manage {
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
  color: #0f172a;
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
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 14px rgba(59,130,246,0.3);
  transition: all 0.25s ease;
}
.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(59,130,246,0.45);
}

/* Grid Layout */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 20px;
  align-items: start;
}

/* Info panel sticky */
.info-panel {
  position: sticky;
  top: 20px;
}

/* Panels */
.panel {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
}
.panel-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(203,213,225,0.2);
}
.panel-hd-left {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
.hd-icon { color: #60a5fa; flex-shrink: 0; }
.panel-count {
  font-size: 12px;
  color: #94a3b8;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(203,213,225,0.2);
}

/* Tree Panel */
.tree-body {
  padding: 12px;
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  min-height: 200px;
}
.tree-body::-webkit-scrollbar { width: 4px; }
.tree-body::-webkit-scrollbar-thumb { background: rgba(203,213,225,0.3); border-radius: 2px; }
.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #64748b;
  font-size: 14px;
}
.tree-empty p { margin: 0; }
.btn-empty {
  padding: 6px 18px;
  border-radius: 8px;
  border: 1px solid rgba(59,130,246,0.3);
  background: rgba(59,130,246,0.08);
  color: #60a5fa;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-empty:hover {
  background: rgba(59,130,246,0.15);
}
.tree-scroll {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}
.tree-node:hover {
  background: rgba(59,130,246,0.06);
}
.tree-node.selected {
  background: rgba(59,130,246,0.1);
  box-shadow: inset 0 0 0 1px rgba(59,130,246,0.15);
}
.root-node { margin-bottom: 2px; }

.child-node {
  margin-left: 24px;
  padding-left: 12px;
  animation: nodeIn 0.25s ease backwards;
  animation-delay: calc(var(--idx) * 0.04s);
}
@keyframes nodeIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}
.node-connector {
  position: absolute;
  left: -12px;
  top: 0;
  width: 12px;
  height: 50%;
  border-left: 1px solid rgba(203,213,225,0.3);
  border-bottom: 1px solid rgba(203,213,225,0.3);
  border-radius: 0 0 0 8px;
}

.node-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
}
.root-icon {
  background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(59,130,246,0.05));
  color: #60a5fa;
}
.child-icon {
  background: rgba(203,213,225,0.3);
  color: #64748b;
}
.child-node:hover .child-icon {
  color: #94a3b8;
  background: rgba(203,213,225,0.3);
}

.node-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  line-height: 1.2;
}
.node-perm {
  font-size: 11px;
  color: #64748b;
  font-family: 'SF Mono', 'Fira Code', monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-type {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}
.tag-menu {
  background: rgba(59,130,246,0.12);
  color: #60a5fa;
}
.tag-btn {
  background: rgba(16,185,129,0.12);
  color: #34d399;
}

.node-ops {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.tree-node:hover .node-ops {
  opacity: 1;
}
.op-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
  transition: all 0.2s;
}
.op-btn svg { flex-shrink: 0; }
.op-btn.add:hover { background: rgba(59,130,246,0.12); color: #60a5fa; }
.op-btn.edit:hover { background: rgba(16,185,129,0.12); color: #34d399; }
.op-btn.del:hover { background: rgba(239,68,68,0.12); color: #f87171; }

/* Info Panel */
.info-body {
  padding: 20px;
  min-height: 300px;
}
.info-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 20px;
  color: #64748b;
  font-size: 13px;
}
.info-empty p { margin: 0; }

.info-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.info-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.info-avatar.menu {
  background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(59,130,246,0.05));
  color: #60a5fa;
}
.info-avatar.btn {
  background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05));
  color: #34d399;
}
.info-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.info-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 8px;
}

.info-fields {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
  padding-top: 14px;
  border-top: 1px solid rgba(203,213,225,0.3);
}
.info-row {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.info-key {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.info-val {
  font-size: 14px;
  color: #475569;
}
.info-val.code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: #60a5fa;
}
.info-val code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: #94a3b8;
  background: rgba(203,213,225,0.2);
  padding: 2px 8px;
  border-radius: 4px;
}

.info-actions {
  display: flex;
  gap: 8px;
  width: 100%;
  margin-top: 8px;
  padding-top: 14px;
  border-top: 1px solid rgba(203,213,225,0.3);
}
.info-act {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(203,213,225,0.2);
  color: #94a3b8;
}
.info-act:hover { background: rgba(203,213,225,0.3); color: #475569; }
.info-act.edit:hover { background: rgba(59,130,246,0.12); color: #60a5fa; }
.info-act.add:hover { background: rgba(16,185,129,0.12); color: #34d399; }
.info-act.del:hover { background: rgba(239,68,68,0.12); color: #f87171; }

/* ========== 高级自定义弹窗 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.4);
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
  width: 520px;
  max-height: 90vh;
  animation: modalFloat 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes modalFloat {
  from { opacity: 0; transform: scale(0.92) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-bg {
  position: absolute;
  inset: -60px;
  pointer-events: none;
  overflow: hidden;
  border-radius: 24px;
}

.modal-glass {
  position: relative;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.4), inset 0 1px 0 rgba(203,213,225,0.3);
  overflow: hidden;
}

/* Header */
.modal-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(203,213,225,0.3);
}
.modal-hd-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.modal-hd-icon.add {
  background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(59,130,246,0.05));
  color: #60a5fa;
}
.modal-hd-icon.edit {
  background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05));
  color: #34d399;
}
.modal-hd-icon.del {
  background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05));
  color: #f87171;
}
.modal-hd-text {
  flex: 1;
  min-width: 0;
}
.modal-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}
.modal-desc {
  margin: 3px 0 0;
  font-size: 13px;
  color: #64748b;
}
.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(203,213,225,0.2);
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.modal-close:hover {
  background: rgba(239,68,68,0.12);
  color: #f87171;
}

/* Body */
.modal-body {
  padding: 20px 28px 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.modal-body::-webkit-scrollbar { width: 4px; }
.modal-body::-webkit-scrollbar-thumb { background: rgba(203,213,225,0.3); border-radius: 2px; }

.m-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: fieldIn 0.4s ease backwards;
  animation-delay: calc(var(--order) * 0.07s);
}
@keyframes fieldIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.m-label {
  display: flex;
  align-items: center;
  gap: 4px;
}
.m-label-text {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}
.m-req {
  font-size: 14px;
  color: #f87171;
  font-weight: 700;
}

.m-input-wrap {
  position: relative;
}
.m-input {
  width: 100%;
  padding: 11px 16px;
  border-radius: 10px;
  border: none;
  background: rgba(203,213,225,0.2);
  color: #334155;
  font-size: 14px;
  outline: none;
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  transition: background 0.25s;
}
.m-input:focus {
  background: rgba(203,213,225,0.3);
}
.m-input::placeholder { color: #475569; }
.m-input-border {
  position: absolute;
  inset: 0;
  border-radius: 10px;
  border: 1px solid rgba(203,213,225,0.4);
  pointer-events: none;
  z-index: 1;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.m-input:focus ~ .m-input-border {
  border-color: rgba(59,130,246,0.4);
  box-shadow: 0 0 0 3px rgba(59,130,246,0.08);
}
.m-input-glow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(59,130,246,0.15);
  z-index: 2;
  pointer-events: none;
  transition: all 0.3s;
}
.m-input:focus ~ .m-input-glow {
  background: #60a5fa;
  box-shadow: 0 0 10px rgba(59,130,246,0.4);
}

.m-hint {
  margin: 0;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
}

/* Card Group (Type Selector) */
.m-card-group {
  display: flex;
  gap: 10px;
}
.m-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid rgba(203,213,225,0.3);
  background: rgba(203,213,225,0.15);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}
.m-card:hover {
  border-color: rgba(59,130,246,0.2);
  background: rgba(59,130,246,0.04);
  transform: translateY(-1px);
}
.m-card.active {
  border-color: rgba(59,130,246,0.5);
  background: rgba(59,130,246,0.1);
  box-shadow: 0 0 0 1px rgba(59,130,246,0.25), 0 4px 16px rgba(59,130,246,0.1);
}
.m-card-indicator {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(203,213,225,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}
.m-card.active .m-card-indicator {
  border-color: #60a5fa;
  background: rgba(59,130,246,0.2);
}
.m-card-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
  transition: all 0.3s;
  transform: scale(0);
}
.m-card.active .m-card-dot {
  background: #60a5fa;
  transform: scale(1);
  box-shadow: 0 0 8px rgba(59,130,246,0.5);
}
.m-card-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.m-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  transition: color 0.3s;
}
.m-card.active .m-card-title {
  color: #334155;
}
.m-card-desc {
  font-size: 11px;
  color: #64748b;
}
.m-card-bg {
  position: absolute;
  right: -20px;
  top: -20px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59,130,246,0.06), transparent);
  pointer-events: none;
  transition: all 0.4s;
}
.m-card.active .m-card-bg {
  transform: scale(1.5);
  opacity: 1;
}

/* Select List (Parent) */
.m-select-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 160px;
  overflow-y: auto;
  padding: 3px;
  border-radius: 10px;
  background: rgba(255,255,255,0.015);
  border: 1px solid rgba(203,213,225,0.3);
}
.m-select-list::-webkit-scrollbar { width: 4px; }
.m-select-list::-webkit-scrollbar-thumb { background: rgba(203,213,225,0.3); border-radius: 2px; }

.m-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.m-select-item:hover {
  background: rgba(59,130,246,0.05);
}
.m-select-item.active {
  background: rgba(59,130,246,0.1);
}
.m-select-check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid rgba(203,213,225,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}
.m-select-item.active .m-select-check {
  border-color: #60a5fa;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}
.check-icon {
  color: #fff;
}
.m-select-indent {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}
.m-select-arrow {
  flex-shrink: 0;
  color: #475569;
}
.m-select-name {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  transition: color 0.2s;
}
.m-select-item.active .m-select-name {
  color: #334155;
}
.m-select-level {
  font-size: 10px;
  color: #475569;
  margin-left: auto;
  font-weight: 500;
}

/* Delete Modal */
.delete-modal {
  width: 440px;
}
.delete-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 12px 28px 20px;
}
.delete-icon-wrap {
  animation: delPulse 2s ease-in-out infinite;
}
@keyframes delPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.06); opacity: 0.85; }
}
.delete-icon-pulse {
  width: 72px;
  height: 72px;
}
.delete-text {
  margin: 0;
  font-size: 16px;
  color: #334155;
  text-align: center;
  line-height: 1.5;
}
.delete-text strong {
  color: #f87171;
  font-weight: 700;
}
.delete-warn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 13px;
  color: #f59e0b;
  background: rgba(245,158,11,0.08);
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid rgba(245,158,11,0.12);
}
.delete-warn svg {
  flex-shrink: 0;
  color: #f59e0b;
}
.m-btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 4px 16px rgba(239,68,68,0.3);
}
.m-btn-danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(239,68,68,0.45);
}
.m-btn-danger:active {
  transform: translateY(0) scale(0.98);
}

/* Footer */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 28px 24px;
  border-top: 1px solid rgba(203,213,225,0.3);
}
.m-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}
.m-btn-ghost {
  background: rgba(203,213,225,0.2);
  color: #94a3b8;
  border: 1px solid rgba(203,213,225,0.3);
}
.m-btn-ghost:hover {
  background: rgba(203,213,225,0.3);
  color: #475569;
  border-color: rgba(203,213,225,0.5);
}
.m-btn-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 16px rgba(59,130,246,0.3);
}
.m-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(59,130,246,0.45);
}
.m-btn-primary:active {
  transform: translateY(0) scale(0.98);
}
.m-btn-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(203,213,225,0.4), transparent);
  transform: translateX(-100%);
  transition: transform 0.6s;
}
.m-btn-primary:hover .m-btn-shimmer {
  transform: translateX(100%);
}
</style>
