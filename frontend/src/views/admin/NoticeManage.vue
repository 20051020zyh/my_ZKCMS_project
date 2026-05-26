<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoticeList, addNotice, deleteNotice } from '@/api/admin'
import { exportToCSV, exportToJSON } from '@/utils/export'

const loading = ref(false)
const notices = ref<any[]>([])
const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const batchDeleteVisible = ref(false)
const deletingId = ref<number | null>(null)
const deletingName = ref('')
const selectedIds = ref<number[]>([])

const form = ref({
  title: '',
  content: '',
  status: 1,
})

const stats = computed(() => ({
  total: notices.value.length,
  published: notices.value.filter(n => n.status === 1).length,
  draft: notices.value.filter(n => n.status === 0).length,
}))

const allChecked = ref(false)

const fetchNotices = async () => {
  loading.value = true
  try {
    const res: any = await getNoticeList()
    notices.value = res.data || []
  } catch {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.value = { title: '', content: '', status: 1 }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title) { ElMessage.warning('请输入公告标题'); return }
  if (!form.value.content) { ElMessage.warning('请输入公告内容'); return }
  try {
    await addNotice(form.value)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    fetchNotices()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDeleteClick = (row: any) => {
  deletingId.value = row.id
  deletingName.value = row.title
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (!deletingId.value) return
  try {
    await deleteNotice([deletingId.value])
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    deletingId.value = null
    fetchNotices()
  } catch {
    ElMessage.error('删除失败')
  }
}

const toggleSelect = (id: number) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

const toggleAll = () => {
  allChecked.value = !allChecked.value
  selectedIds.value = allChecked.value ? notices.value.map(n => n.id) : []
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的公告')
    return
  }
  batchDeleteVisible.value = true
}

const handleConfirmBatchDelete = async () => {
  try {
    await deleteNotice(selectedIds.value)
    ElMessage.success(`成功删除 ${selectedIds.value.length} 条公告`)
    batchDeleteVisible.value = false
    selectedIds.value = []
    allChecked.value = false
    fetchNotices()
  } catch {
    ElMessage.error('批量删除失败')
  }
}

const handleExportCSV = () => {
  if (notices.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  const headers = ['id', 'title', 'content', 'status', 'createTime']
  const exportData = notices.value.map(item => ({
    id: item.id,
    title: item.title,
    content: item.content || '',
    status: item.status === 1 ? '已发布' : '草稿',
    createTime: new Date(item.createTime).toLocaleString('zh-CN')
  }))
  exportToCSV(exportData, headers, '公告列表')
  ElMessage.success('导出成功')
}

const handleExportJSON = () => {
  if (notices.value.length === 0) { ElMessage.warning('暂无数据可导出'); return }
  const exportData = notices.value.map(item => ({
    id: item.id,
    title: item.title,
    content: item.content || '',
    status: item.status === 1 ? '已发布' : '草稿',
    createTime: item.createTime
  }))
  exportToJSON(exportData, '公告列表')
  ElMessage.success('导出成功')
}

const formatDate = (s: string) => {
  if (!s) return '-'
  return new Date(s).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const statusText = (s: number) => s === 1 ? '已发布' : '草稿'
const truncate = (t: string, n: number) => t?.length > n ? t.slice(0, n) + '…' : t

onMounted(() => { fetchNotices() })
</script>

<template>
  <div class="notice-manage" v-loading="loading" element-loading-background="rgba(8,11,20,0.85)">
    <div class="page-hd">
      <div class="hd-left">
        <h2 class="hd-title">公告管理</h2>
        <p class="hd-sub">管理和发布系统公告通知</p>
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
        <button class="btn-primary" @click="handleAdd">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>新增公告</span>
        </button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card" style="--accent:#6366f1;--delay:0s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
        <div class="stat-info"><span class="stat-num">{{ stats.total }}</span><span class="stat-label">公告总数</span></div>
      </div>
      <div class="stat-card" style="--accent:#10b981;--delay:0.06s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        <div class="stat-info"><span class="stat-num">{{ stats.published }}</span><span class="stat-label">已发布</span></div>
      </div>
      <div class="stat-card" style="--accent:#f59e0b;--delay:0.12s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        <div class="stat-info"><span class="stat-num">{{ stats.draft }}</span><span class="stat-label">草稿</span></div>
      </div>
      <div class="stat-card" style="--accent:#a855f7;--delay:0.18s">
        <div class="stat-glow" />
        <svg class="stat-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        <div class="stat-info">
          <span class="stat-num">{{ stats.total ? Math.round(stats.published / stats.total * 100) : 0 }}%</span>
          <span class="stat-label">发布率</span>
        </div>
      </div>
    </div>

    <div class="table-panel">
      <div class="panel-hd">
        <div class="panel-hd-left">
          <svg class="hd-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
          <span>公告列表</span>
        </div>
        <div class="panel-hd-right">
          <span v-if="selectedIds.length" class="batch-info">已选 {{ selectedIds.length }} 项</span>
          <button v-if="selectedIds.length" class="btn-batch-del" @click="handleBatchDelete">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            <span>批量删除</span>
          </button>
        </div>
      </div>
      <div class="table-scroll">
        <table class="notice-table">
          <thead>
            <tr>
              <th class="col-chk">
                <label class="chk-wrap">
                  <input type="checkbox" :checked="allChecked && notices.length > 0" @change="toggleAll" />
                  <span class="chk-mark" />
                </label>
              </th>
              <th class="col-id">ID</th>
              <th class="col-title">标题</th>
              <th class="col-content">内容</th>
              <th class="col-status">状态</th>
              <th class="col-time">创建时间</th>
              <th class="col-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in notices" :key="row.id" class="table-row" :class="{selected: selectedIds.includes(row.id)}" :style="{'--idx':i}">
              <td class="col-chk">
                <label class="chk-wrap">
                  <input type="checkbox" :checked="selectedIds.includes(row.id)" @change="toggleSelect(row.id)" />
                  <span class="chk-mark" />
                </label>
              </td>
              <td class="col-id"><span class="id-text">#{{ row.id }}</span></td>
              <td class="col-title">
                <div class="title-cell">
                  <svg class="title-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                  <span class="title-text">{{ row.title }}</span>
                </div>
              </td>
              <td class="col-content"><span class="content-text">{{ truncate(row.content, 60) }}</span></td>
              <td class="col-status">
                <span :class="['status-badge', row.status === 1 ? 'pub' : 'draft']">{{ statusText(row.status) }}</span>
              </td>
              <td class="col-time"><span class="time-text">{{ formatDate(row.createTime) }}</span></td>
              <td class="col-ops">
                <div class="ops-group">
                  <button class="ops-btn del" @click="handleDeleteClick(row)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                    <span>删除</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && notices.length === 0">
              <td colspan="7" class="empty-row">
                <div class="empty-cell">
                  <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1.2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
                  <p>暂无公告数据</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增弹窗 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal-container">
          <div class="modal-bg">
            <div class="modal-orb orb-1" />
            <div class="modal-orb orb-2" />
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon add">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">新增公告</h3>
                <p class="modal-desc">发布一条新的系统公告</p>
              </div>
              <button class="modal-close" @click="dialogVisible = false">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="m-field" style="--order:1">
                <label class="m-label"><span class="m-label-text">公告标题</span><span class="m-req">*</span></label>
                <div class="m-input-wrap">
                  <input v-model="form.title" class="m-input" placeholder="请输入公告标题" maxlength="50" />
                  <div class="m-input-border" />
                  <div class="m-input-glow" />
                </div>
              </div>
              <div class="m-field" style="--order:2">
                <label class="m-label"><span class="m-label-text">公告内容</span><span class="m-req">*</span></label>
                <div class="m-input-wrap">
                  <textarea v-model="form.content" class="m-input m-textarea" placeholder="请输入公告内容" rows="6" />
                  <div class="m-input-border" />
                </div>
              </div>
              <div class="m-field" style="--order:3">
                <label class="m-label"><span class="m-label-text">发布状态</span></label>
                <div class="m-radio-group">
                  <label :class="['m-radio', form.status === 1 && 'active']">
                    <input type="radio" v-model="form.status" :value="1" />
                    <span class="m-radio-dot" />
                    <span>已发布</span>
                  </label>
                  <label :class="['m-radio', form.status === 0 && 'active']">
                    <input type="radio" v-model="form.status" :value="0" />
                    <span class="m-radio-dot" />
                    <span>草稿</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="dialogVisible = false">取消</button>
              <button class="m-btn m-btn-primary" @click="handleSave">
                <span class="m-btn-shimmer" />
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                <span>确认发布</span>
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
            <div class="modal-orb orb-1 del-orb" />
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

    <!-- 批量删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="batchDeleteVisible" class="modal-overlay" @click.self="batchDeleteVisible = false">
        <div class="modal-container delete-modal">
          <div class="modal-bg">
            <div class="modal-orb orb-1 del-orb" />
          </div>
          <div class="modal-glass">
            <div class="modal-header">
              <div class="modal-hd-icon del">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </div>
              <div class="modal-hd-text">
                <h3 class="modal-title">批量删除</h3>
                <p class="modal-desc">此操作不可撤销</p>
              </div>
              <button class="modal-close" @click="batchDeleteVisible = false">
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
              <p class="delete-text">确定要删除已选的 <strong>{{ selectedIds.length }}</strong> 条公告吗？</p>
              <p class="delete-warn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <span>删除后无法恢复</span>
              </p>
            </div>
            <div class="modal-footer">
              <button class="m-btn m-btn-ghost" @click="batchDeleteVisible = false">取消</button>
              <button class="m-btn m-btn-danger" @click="handleConfirmBatchDelete">
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
.notice-manage {
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
.hd-title { margin: 0; font-size: 26px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.5px; }
.hd-sub { margin: 0; font-size: 14px; color: #64748b; }
.hd-actions { display: flex; gap: 10px; align-items: center; }
.export-group { display: flex; gap: 6px; }

.btn-ghost {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px; border-radius: 10px;
  font-size: 13px; font-weight: 500;
  border: 1px solid rgba(255,255,255,0.06); cursor: pointer;
  background: rgba(255,255,255,0.02); color: #94a3b8;
  transition: all 0.2s;
}
.btn-ghost:hover { background: rgba(255,255,255,0.05); color: #cbd5e1; }

.btn-primary {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 22px; border-radius: 10px;
  font-size: 14px; font-weight: 600; border: none; cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #fff;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
  transition: all 0.25s ease;
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 22px rgba(99,102,241,0.45); }

/* Stats */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  position: relative;
  display: flex; align-items: center; gap: 16px;
  padding: 18px 20px; border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  animation: statIn 0.5s ease backwards;
  animation-delay: var(--delay);
}
@keyframes statIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.stat-card:hover { border-color: rgba(99,102,241,0.12); }
.stat-glow {
  position: absolute; right: -30px; top: -30px;
  width: 100px; height: 100px; border-radius: 50%;
  background: radial-gradient(circle, var(--accent), transparent);
  opacity: 0.06; pointer-events: none; transition: all 0.4s;
}
.stat-card:hover .stat-glow { transform: scale(1.5); opacity: 0.12; }
.stat-icon {
  width: 36px; height: 36px; padding: 8px; border-radius: 10px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent) 20%, transparent), transparent);
  color: var(--accent); flex-shrink: 0;
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
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}
.panel-hd-left { display: flex; align-items: center; gap: 9px; font-size: 14px; font-weight: 600; color: #e2e8f0; }
.hd-icon { color: #818cf8; flex-shrink: 0; }
.panel-hd-right { display: flex; align-items: center; gap: 8px; }
.batch-info { font-size: 12px; color: #f59e0b; padding: 3px 10px; border-radius: 10px; background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.1); }
.btn-batch-del {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 500;
  padding: 5px 12px; border-radius: 8px;
  border: none; cursor: pointer;
  background: rgba(239,68,68,0.1); color: #f87171;
  transition: all 0.2s;
}
.btn-batch-del:hover { background: rgba(239,68,68,0.2); }

.table-scroll { overflow-x: auto; }
.notice-table { width: 100%; border-collapse: collapse; }
.notice-table th {
  text-align: left; font-size: 12px; font-weight: 600;
  color: #64748b; text-transform: uppercase; letter-spacing: 0.4px;
  padding: 14px 16px; border-bottom: 1px solid rgba(255,255,255,0.04); white-space: nowrap;
}
.notice-table td {
  padding: 14px 16px; border-bottom: 1px solid rgba(255,255,255,0.03);
  font-size: 14px; color: #cbd5e1;
}
.table-row { animation: rowIn 0.35s ease backwards; animation-delay: calc(var(--idx) * 0.04s); transition: background 0.2s; }
.table-row:hover { background: rgba(99,102,241,0.03); }
.table-row.selected { background: rgba(99,102,241,0.05); }
.table-row:last-child td { border-bottom: none; }
@keyframes rowIn { from { opacity: 0; transform: translateX(-6px); } to { opacity: 1; transform: translateX(0); } }

.col-chk { width: 48px; }
.col-id { width: 70px; }
.col-title { min-width: 180px; }
.col-content { min-width: 260px; }
.col-status { width: 90px; }
.col-time { width: 160px; }
.col-ops { width: 100px; text-align: center; }

.id-text { color: #64748b; font-family: 'SF Mono', monospace; font-size: 13px; }

.title-cell { display: flex; align-items: center; gap: 8px; }
.title-icon { color: #818cf8; flex-shrink: 0; }
.title-text { font-weight: 600; color: #e2e8f0; }

.content-text { color: #94a3b8; font-size: 13px; }

.status-badge { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px; letter-spacing: 0.3px; }
.status-badge.pub { background: rgba(16,185,129,0.12); color: #34d399; }
.status-badge.draft { background: rgba(245,158,11,0.12); color: #fbbf24; }

.time-text { color: #64748b; font-size: 13px; }

.empty-row td { padding: 60px 16px; }
.empty-cell { display: flex; flex-direction: column; align-items: center; gap: 10px; color: #64748b; }
.empty-cell p { margin: 0; }

.ops-group { display: flex; justify-content: center; gap: 2px; }
.ops-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px; border-radius: 8px; border: none; cursor: pointer;
  background: transparent; font-size: 12px; font-weight: 500;
  color: #64748b; white-space: nowrap; transition: all 0.2s;
}
.ops-btn svg { flex-shrink: 0; }
.ops-btn.del:hover { background: rgba(239,68,68,0.12); color: #f87171; }

/* Checkbox */
.chk-wrap { display: flex; align-items: center; cursor: pointer; position: relative; }
.chk-wrap input { position: absolute; opacity: 0; width: 0; height: 0; }
.chk-mark {
  width: 16px; height: 16px; border-radius: 4px;
  border: 2px solid rgba(255,255,255,0.1);
  background: transparent; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.chk-wrap input:checked + .chk-mark {
  border-color: #818cf8;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  box-shadow: 0 2px 6px rgba(99,102,241,0.3);
}
.chk-wrap input:checked + .chk-mark::after {
  content: ''; width: 4px; height: 8px;
  border: solid #fff; border-width: 0 2px 2px 0;
  transform: rotate(45deg); margin-top: -1px;
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
@keyframes overlayIn { from { opacity:0; backdrop-filter:blur(0); -webkit-backdrop-filter:blur(0); } to { opacity:1; backdrop-filter:blur(12px); -webkit-backdrop-filter:blur(12px); } }
.modal-container { position: relative; width: 500px; max-height: 90vh; animation: modalFloat 0.4s cubic-bezier(0.22,1,0.36,1); }
@keyframes modalFloat { from { opacity:0; transform: scale(0.92) translateY(20px); } to { opacity:1; transform: scale(1) translateY(0); } }
.modal-bg { position: absolute; inset: -60px; pointer-events: none; overflow: hidden; border-radius: 24px; }
.modal-orb { position: absolute; border-radius: 50%; filter: blur(60px); opacity: 0.3; }
.modal-orb.orb-1 { width: 240px; height: 240px; background: radial-gradient(circle, #6366f1, transparent); top: -80px; right: -60px; animation: orbFloat1 6s ease-in-out infinite; }
.modal-orb.orb-2 { width: 200px; height: 200px; background: radial-gradient(circle, #a855f7, transparent); bottom: -60px; left: -60px; animation: orbFloat2 8s ease-in-out infinite; }
@keyframes orbFloat1 { 0%,100% { transform:translate(0,0) scale(1); } 50% { transform:translate(-20px,10px) scale(1.1); } }
@keyframes orbFloat2 { 0%,100% { transform:translate(0,0) scale(1); } 50% { transform:translate(20px,-10px) scale(1.08); } }
.modal-glass {
  position: relative;
  background: linear-gradient(170deg, rgba(17,24,39,0.97), rgba(15,18,30,0.98));
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.6), inset 0 1px 0 rgba(255,255,255,0.04);
  overflow: hidden;
}
.modal-header { display: flex; align-items: flex-start; gap: 14px; padding: 24px 28px 16px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.modal-hd-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.modal-hd-icon.add { background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05)); color: #818cf8; }
.modal-hd-icon.del { background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05)); color: #f87171; }
.modal-hd-text { flex: 1; min-width: 0; }
.modal-title { margin: 0; font-size: 17px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.3px; }
.modal-desc { margin: 3px 0 0; font-size: 13px; color: #64748b; }
.modal-close { width: 32px; height: 32px; border-radius: 8px; border: none; background: rgba(255,255,255,0.03); color: #64748b; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.2s; }
.modal-close:hover { background: rgba(239,68,68,0.12); color: #f87171; }
.modal-body { padding: 20px 28px 12px; display: flex; flex-direction: column; gap: 16px; max-height: 60vh; overflow-y: auto; }
.modal-body::-webkit-scrollbar { width: 4px; }
.modal-body::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.06); border-radius: 2px; }
.m-field { display: flex; flex-direction: column; gap: 6px; animation: fieldIn 0.4s ease backwards; animation-delay: calc(var(--order) * 0.07s); }
@keyframes fieldIn { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:translateY(0); } }
.m-label { display: flex; align-items: center; gap: 4px; }
.m-label-text { font-size: 13px; font-weight: 600; color: #94a3b8; }
.m-req { font-size: 14px; color: #f87171; font-weight: 700; }
.m-input-wrap { position: relative; }
.m-input { width: 100%; padding: 11px 16px; border-radius: 10px; border: none; background: rgba(255,255,255,0.03); color: #e2e8f0; font-size: 14px; outline: none; position: relative; z-index: 2; box-sizing: border-box; transition: background 0.25s; font-family: inherit; }
.m-input:focus { background: rgba(255,255,255,0.05); }
.m-input::placeholder { color: #475569; }
.m-textarea { resize: vertical; min-height: 120px; line-height: 1.6; }
.m-input-border { position: absolute; inset: 0; border-radius: 10px; border: 1px solid rgba(255,255,255,0.08); pointer-events: none; z-index: 1; transition: border-color 0.3s, box-shadow 0.3s; }
.m-input:focus ~ .m-input-border { border-color: rgba(99,102,241,0.4); box-shadow: 0 0 0 3px rgba(99,102,241,0.08); }
.m-input-glow { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); width: 6px; height: 6px; border-radius: 50%; background: rgba(99,102,241,0.15); z-index: 2; pointer-events: none; transition: all 0.3s; }
.m-input:focus ~ .m-input-glow { background: #818cf8; box-shadow: 0 0 10px rgba(99,102,241,0.4); }

/* Radio Group */
.m-radio-group { display: flex; gap: 12px; }
.m-radio {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 18px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.06);
  background: rgba(255,255,255,0.02);
  cursor: pointer; transition: all 0.25s;
  font-size: 14px; color: #94a3b8; font-weight: 500;
}
.m-radio input { position: absolute; opacity: 0; width: 0; height: 0; }
.m-radio.active { border-color: rgba(99,102,241,0.3); background: rgba(99,102,241,0.06); color: #818cf8; }
.m-radio-dot {
  width: 14px; height: 14px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.12);
  background: transparent; transition: all 0.25s;
  display: flex; align-items: center; justify-content: center;
}
.m-radio.active .m-radio-dot { border-color: #818cf8; background: #818cf8; box-shadow: 0 0 8px rgba(99,102,241,0.4); }
.m-radio.active .m-radio-dot::after { content: ''; width: 4px; height: 4px; border-radius: 50%; background: #fff; }

.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 28px 24px; border-top: 1px solid rgba(255,255,255,0.04); }
.m-btn { display: flex; align-items: center; gap: 7px; padding: 10px 22px; border-radius: 10px; font-size: 14px; font-weight: 600; border: none; cursor: pointer; transition: all 0.25s ease; position: relative; overflow: hidden; }
.m-btn-ghost { background: rgba(255,255,255,0.03); color: #94a3b8; border: 1px solid rgba(255,255,255,0.06); }
.m-btn-ghost:hover { background: rgba(255,255,255,0.06); color: #cbd5e1; border-color: rgba(255,255,255,0.1); }
.m-btn-primary { background: linear-gradient(135deg, #6366f1, #7c3aed); color: #fff; box-shadow: 0 4px 16px rgba(99,102,241,0.3); }
.m-btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99,102,241,0.45); }
.m-btn-primary:active { transform: translateY(0) scale(0.98); }
.m-btn-shimmer { position: absolute; inset: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent); transform: translateX(-100%); transition: transform 0.6s; }
.m-btn-primary:hover .m-btn-shimmer { transform: translateX(100%); }

/* Delete Modal */
.delete-modal { width: 420px; }
.del-orb { width: 200px !important; height: 200px !important; background: radial-gradient(circle, #ef4444, transparent) !important; top: -60px !important; right: -60px !important; }
.delete-body { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 12px 28px 20px; }
.delete-icon-wrap { animation: delPulse 2s ease-in-out infinite; }
@keyframes delPulse { 0%,100% { transform:scale(1); opacity:1; } 50% { transform:scale(1.06); opacity:0.85; } }
.delete-icon-pulse { width: 64px; height: 64px; }
.delete-text { margin: 0; font-size: 15px; color: #e2e8f0; text-align: center; }
.delete-text strong { color: #f87171; font-weight: 700; }
.delete-warn { display: flex; align-items: center; gap: 6px; margin: 0; font-size: 13px; color: #f59e0b; background: rgba(245,158,11,0.08); padding: 8px 14px; border-radius: 8px; border: 1px solid rgba(245,158,11,0.12); }
.delete-warn svg { flex-shrink: 0; color: #f59e0b; }
.m-btn-danger { background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; box-shadow: 0 4px 16px rgba(239,68,68,0.3); }
.m-btn-danger:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(239,68,68,0.45); }
.m-btn-danger:active { transform: translateY(0) scale(0.98); }
</style>
