<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getLeaveMessages, batchDeleteLeaveMessages } from '@/api/leaveMessage'

const loading = ref(false)
const messages = ref<any[]>([])
const pagination = ref({ pageNum: 1, pageSize: 10, total: 0 })
const selectedIds = ref<number[]>([])

const formatDate = (s: string) => {
  if (!s) return ''
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getLeaveMessages({ pageNum: pagination.value.pageNum, pageSize: pagination.value.pageSize })
    messages.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch {
    ElMessage.error('获取留言列表失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (sel: any[]) => {
  selectedIds.value = sel.map((i: any) => i.id)
}

const handlePageChange = (p: number) => {
  pagination.value.pageNum = p
  fetchData()
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先选择要删除的留言'); return }
  try {
    await ElMessageBox.confirm(
      `<div class="fx-c-header">
        <div class="fx-c-ico danger">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
        </div>
      </div>
      <div class="fx-c-title">批量删除留言</div>
      <div class="fx-c-desc">删除后数据将无法恢复，请谨慎操作</div>
      <div class="fx-c-warning danger">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>即将删除选中的 <strong class="fx-c-hl">${selectedIds.value.length}</strong> 条留言，此操作不可撤销</span>
      </div>`,
      '', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        center: true,
        icon: undefined,
        dangerouslyUseHTMLString: true,
        customClass: 'fx-confirm-box',
        confirmButtonClass: 'fx-c-btn-danger'
      }
    )
    await batchDeleteLeaveMessages(selectedIds.value)
    ElMessage.success({ message: '删除成功', duration: 2000 })
    selectedIds.value = []
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleReply = () => {
  ElMessage.info('回复功能暂未开放')
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="message-manage" v-loading="loading" element-loading-background="rgba(8,11,20,0.8)">
    <div class="page-header">
      <div class="header-left">
        <div class="header-title-row">
          <div class="header-icon-wrap">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
          </div>
          <div>
            <h2 class="page-title">留言管理</h2>
            <p class="page-subtitle">查看和管理访客留言信息</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="toolbar-info">共 {{ pagination.total }} 条留言</span>
      </div>
      <div class="toolbar-right">
        <button v-if="selectedIds.length" class="action-btn delete-btn" @click="handleBatchDelete">
          <Delete :size="14" />
          <span>删除（{{ selectedIds.length }}）</span>
        </button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="panel">
      <div class="panel-header">
        <div class="panel-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:#818cf8"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
          <span>留言列表</span>
        </div>
        <span class="panel-badge">共 {{ pagination.total }} 条</span>
      </div>

      <div class="table-wrapper">
        <el-table :data="messages" @selection-change="handleSelectionChange" style="width: 100%">
          <el-table-column type="selection" width="55" />
          <el-table-column label="ID" width="80" align="center">
            <template #default="{ row }"><span class="cell-id">{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column label="留言人" min-width="120" align="center">
            <template #default="{ row }"><span class="user-tag">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column label="手机号码" min-width="140" align="center">
            <template #default="{ row }"><span class="phone-text">{{ row.phone }}</span></template>
          </el-table-column>
          <el-table-column label="邮箱地址" min-width="200">
            <template #default="{ row }"><span class="email-text">{{ row.email }}</span></template>
          </el-table-column>
          <el-table-column label="留言时间" min-width="160" align="center">
            <template #default="{ row }"><span class="time-text">{{ formatDate(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default>
              <button class="ops-btn ops-reply" @click="handleReply">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
                <span>回复</span>
              </button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next, jumper, ->, total"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-manage {
  padding: 28px 32px;
  min-height: 100%;
}

.page-header { margin-bottom: 24px; }
.header-left { display: flex; flex-direction: column; gap: 6px; }
.header-title-row { display: flex; align-items: center; gap: 16px; }
.header-icon-wrap {
  width: 48px; height: 48px; border-radius: 14px;
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05));
  color: #818cf8; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(99,102,241,0.15);
  animation: hdr-icon-in 0.5s cubic-bezier(0.34,1.56,0.64,1);
}
@keyframes hdr-icon-in {
  from { transform: scale(0) rotate(-20deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}
.page-title { margin: 0; font-size: 26px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.5px; }
.page-subtitle { margin: 4px 0 0; font-size: 14px; color: #64748b; }

.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px; margin-bottom: 16px;
}
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.toolbar-info { font-size: 13px; color: #94a3b8; }
.toolbar-right { display: flex; gap: 8px; align-items: center; }
.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  height: 36px; padding: 0 16px; border: none; border-radius: 100px;
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all 0.25s ease; font-family: inherit; white-space: nowrap;
}
.action-btn:hover { transform: translateY(-1px); }
.delete-btn {
  background: rgba(239,68,68,0.1); color: #fca5a5;
  border: 1px solid rgba(239,68,68,0.15);
  animation: del-btn-in 0.3s ease;
}
@keyframes del-btn-in { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }
.delete-btn:hover { background: rgba(239,68,68,0.18); color: #fecaca; }

.panel {
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  animation: panel-in 0.4s cubic-bezier(0.22,1,0.36,1);
}
@keyframes panel-in { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.panel-title { display: flex; align-items: center; gap: 9px; font-size: 14px; font-weight: 600; color: #e2e8f0; }
.panel-badge { font-size: 12px; color: #94a3b8; padding: 4px 12px; border-radius: 12px; background: rgba(255,255,255,0.03); }
.table-wrapper { padding: 0; }

.cell-id { color: #64748b; font-family: monospace; font-size: 12px; }
.user-tag { font-size: 13px; padding: 3px 10px; border-radius: 12px; background: rgba(99,102,241,0.1); color: #a5b4fc; font-weight: 500; }
.phone-text { color: #cbd5e1; font-size: 13px; font-family: monospace; }
.email-text { color: #cbd5e1; font-size: 13px; }
.time-text { color: #64748b; font-size: 13px; }

.ops-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 12px; border-radius: 8px;
  border: 1px solid transparent; font-size: 12px; font-weight: 500;
  cursor: pointer; transition: all 0.25s ease; font-family: inherit; white-space: nowrap;
}
.ops-reply { background: rgba(99,102,241,0.1); color: #818cf8; border-color: rgba(99,102,241,0.15); }
.ops-reply:hover { background: rgba(99,102,241,0.2); color: #a5b4fc; transform: translateY(-1px); }

.pagination-wrapper { display: flex; justify-content: center; padding: 20px; }

:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-row-hover-bg-color: rgba(99,102,241,0.04); --el-table-border-color: transparent; --el-table-text-color: #cbd5e1; --el-table-header-text-color: #94a3b8; }
:deep(.el-table th.el-table__cell) { background: transparent; border-bottom: 1px solid rgba(255,255,255,0.05); padding: 16px 0; font-weight: 600; }
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.03); padding: 14px 0; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: #94a3b8; --el-pagination-button-bg-color: rgba(255,255,255,0.03); --el-pagination-hover-color: #818cf8; --el-pagination-button-color: #94a3b8; }
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) { background: rgba(255,255,255,0.03) !important; border-radius: 8px !important; }
:deep(.el-pagination .btn-prev:hover), :deep(.el-pagination .btn-next:hover) { background: rgba(255,255,255,0.08) !important; color: #818cf8; }
:deep(.el-pagination button.is-active) { background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.03); box-shadow: none; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; }
:deep(.el-input__wrapper:hover) { border-color: rgba(99,102,241,0.2); }
:deep(.el-input__wrapper.is-focus) { border-color: #6366f1; }
:deep(.el-input__inner) { color: #e2e8f0; }
:deep(.el-checkbox__input .el-checkbox__inner) { border-color: rgba(255,255,255,0.12); }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background: #818cf8; border-color: #818cf8; }

@media (max-width: 768px) { .message-manage { padding: 20px 16px; } .toolbar { flex-direction: column; align-items: stretch; } }
</style>
