<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Download, Delete, SuccessFilled, Clock } from '@element-plus/icons-vue'
import { getCommentListByStatus, auditComment, batchDeleteComments } from '@/api/comment'
import { exportToCSV, exportToJSON } from '@/utils/export'

type TabKey = 'pending' | 'approved' | 'rejected'

interface TabItem { key: TabKey; label: string; color: string; status: number }

const tabs: TabItem[] = [
  { key: 'pending', label: '待审核', color: '#6366f1', status: 1 },
  { key: 'approved', label: '已通过', color: '#34d399', status: 2 },
  { key: 'rejected', label: '已驳回', color: '#f87171', status: 3 },
]

const activeTab = ref<TabKey>('pending')
const loading = ref(false)

const commentData = ref<Record<TabKey, any[]>>({ pending: [], approved: [], rejected: [] })
const pagination = ref<Record<TabKey, { pageNum: number; pageSize: number; total: number }>>({
  pending: { pageNum: 1, pageSize: 10, total: 0 },
  approved: { pageNum: 1, pageSize: 10, total: 0 },
  rejected: { pageNum: 1, pageSize: 10, total: 0 },
})

const selectedComments = ref<number[]>([])

const currentData = computed(() => commentData.value[activeTab.value])
const currentPagination = computed(() => pagination.value[activeTab.value])

const stats = computed(() => ({
  pending: pagination.value.pending.total,
  approved: pagination.value.approved.total,
  rejected: pagination.value.rejected.total,
}))

const formatDate = (s: string) => {
  if (!s) return ''
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const fetchData = async (tab: TabKey) => {
  try {
    const t = tabs.find(item => item.key === tab)!
    const pag = pagination.value[tab]
    const res: any = await getCommentListByStatus({ pageNum: pag.pageNum, pageSize: pag.pageSize, auditStatus: t.status })
    commentData.value[tab] = res.data?.records || []
    pag.total = res.data?.total || 0
  } catch { ElMessage.error('获取数据失败') }
}

const handleTabChange = async (key: TabKey) => {
  activeTab.value = key
  selectedComments.value = []
  loading.value = true
  await fetchData(key)
  loading.value = false
}

const handleAudit = async (id: number, status: number) => {
  const t = status === 2 ? '通过' : '驳回'
  const isPass = status === 2
  try {
    await ElMessageBox.confirm(
      `<div class="fx-c-header">
        <div class="fx-c-ico ${isPass ? 'pass' : 'reject'}">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">${isPass ? '<path d="M20 6L9 17l-5-5"/>' : '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>'}</svg>
        </div>
      </div>
      <div class="fx-c-title">确认${t}评论</div>
      <div class="fx-c-desc">${isPass ? '通过后该评论将在文章中公开显示' : '驳回后该评论将被隐藏，用户将无法看到'}</div>
      <div class="fx-c-warning ${isPass ? 'pass' : 'reject'}">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${isPass ? '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>' : '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>'}</svg>
        <span>${isPass ? '该评论将通过审核，对所有用户可见' : '该评论将被拒绝，不会显示在文章中'}</span>
      </div>`,
      '', {
        confirmButtonText: `确认${t}`,
        cancelButtonText: '取消',
        center: true,
        icon: undefined,
        dangerouslyUseHTMLString: true,
        customClass: 'fx-confirm-box',
        confirmButtonClass: isPass ? 'fx-c-btn-pass' : 'fx-c-btn-reject'
      }
    )
    await auditComment(id, status)
    ElMessage.success({ message: `${t}成功`, duration: 2000 })
    fetchData(activeTab.value)
    fetchData('pending')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(`${t}失败`) }
}

const handleBatchDelete = async () => {
  if (!selectedComments.value.length) { ElMessage.warning('请先选择'); return }
  try {
    await ElMessageBox.confirm(
      `<div class="fx-c-header">
        <div class="fx-c-ico danger">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
        </div>
      </div>
      <div class="fx-c-title">批量删除评论</div>
      <div class="fx-c-desc">删除后数据将无法恢复，请谨慎操作</div>
      <div class="fx-c-warning danger">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>即将删除选中的 <strong class="fx-c-hl">${selectedComments.value.length}</strong> 条评论，此操作不可撤销</span>
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
    const ids = selectedComments.value.map((i: any) => i.commentId || i.id)
    await batchDeleteComments(ids)
    ElMessage.success({ message: '批量删除成功', duration: 2000 })
    selectedComments.value = []
    fetchData(activeTab.value)
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('批量删除失败') }
}

const handleSelectionChange = (sel: any[]) => { selectedComments.value = sel.map((i: any) => i.commentId || i.id) }

const handlePageChange = (p: number) => {
  pagination.value[activeTab.value].pageNum = p
  fetchData(activeTab.value)
}

const handleExportCSV = () => {
  if (!currentData.value.length) { ElMessage.warning('暂无数据'); return }
  const data = currentData.value.map((i: any) => ({
    id: i.id, content: i.content, user: i.userName, article: i.articleTitle, time: formatDate(i.creatime)
  }))
  const label = tabs.find(t => t.key === activeTab.value)?.label || ''
  exportToCSV(data, ['id', 'content', 'user', 'article', 'time'], label)
  ElMessage.success({ message: '导出成功', duration: 2000 })
}

const handleExportJSON = () => {
  if (!currentData.value.length) { ElMessage.warning('暂无数据'); return }
  const data = currentData.value.map((i: any) => ({ id: i.id, content: i.content, user: i.userName, article: i.articleTitle, time: i.creatime }))
  const label = tabs.find(t => t.key === activeTab.value)?.label || ''
  exportToJSON(data, label)
  ElMessage.success({ message: '导出成功', duration: 2000 })
}

onMounted(async () => {
  await Promise.all([
    fetchData('pending'),
    fetchData('approved'),
    fetchData('rejected'),
  ])
})
</script>

<template>
  <div class="comment-manage" v-loading="loading" element-loading-background="rgba(8,11,20,0.8)">
    <div class="page-header">
      <div class="header-left">
        <div class="header-title-row">
          <div class="header-icon-wrap">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          </div>
          <div>
            <h2 class="page-title">评论管理</h2>
            <p class="page-subtitle">审核和管理社区评论内容</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div
        v-for="t in tabs"
        :key="t.key"
        class="stat-card"
        :class="{ active: activeTab === t.key }"
        :style="{ '--accent': t.color }"
        @click="handleTabChange(t.key)"
      >
        <div class="stat-bg" :style="{ background: `linear-gradient(135deg, ${t.color}15, ${t.color}03)` }"></div>
        <div class="stat-inner">
          <div class="stat-icon-wrap" :style="{ background: `linear-gradient(135deg, ${t.color}, ${t.color}dd)`, boxShadow: `0 8px 24px ${t.color}40` }">
            <component :is="t.key === 'pending' ? Clock : t.key === 'approved' ? SuccessFilled : Close" />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats[t.key] }}</div>
            <div class="stat-label">{{ t.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="status-tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="status-tab"
            :class="{ active: activeTab === t.key }"
            :style="{ '--accent': t.color }"
            @click="handleTabChange(t.key)"
          >
            <span class="status-dot" :style="{ background: t.color, boxShadow: `0 0 8px ${t.color}80` }"></span>
            <span>{{ t.label }}</span>
            <span v-if="stats[t.key]" class="status-count" :style="{ background: t.color }">{{ stats[t.key] }}</span>
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <button v-if="selectedComments.length" class="action-btn delete-btn" @click="handleBatchDelete">
          <Delete :size="14" />
          <span>删除（{{ selectedComments.length }}）</span>
        </button>
        <button class="action-btn export-btn" @click="handleExportCSV">
          <Download :size="14" />
          <span>CSV</span>
        </button>
        <button class="action-btn export-btn" @click="handleExportJSON">
          <Download :size="14" />
          <span>JSON</span>
        </button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="panel">
      <div class="panel-header">
        <div class="panel-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:#60a5fa"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <span>{{ tabs.find(t => t.key === activeTab)?.label }}列表</span>
        </div>
        <span class="panel-badge">共 {{ currentPagination.total }} 条</span>
      </div>

      <div class="table-wrapper">
        <el-table
          :data="currentData"
          @selection-change="handleSelectionChange"
          style="width: 100%"
          :key="activeTab"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="评论ID" width="70" align="center">
            <template #default="{ row }"><span class="cell-id">{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column label="用户" width="130" align="center">
            <template #default="{ row }">
              <span class="user-tag">{{ row.userName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="文章" min-width="180">
            <template #default="{ row }">
              <span class="article-title-text">{{ row.articleTitle }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评论内容" min-width="220">
            <template #default="{ row }">
              <span class="content-text">{{ row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="150" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatDate(row.creatime) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="activeTab === 'rejected'" label="驳回原因" min-width="150">
            <template #default="{ row }">
              <span class="reject-reason">{{ row.rejectReason || '无' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <div class="ops-group">
                <template v-if="activeTab === 'pending'">
                  <button class="ops-btn ops-pass" @click="handleAudit(row.id, 2)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
                    <span>通过</span>
                  </button>
                  <button class="ops-btn ops-reject" @click="handleAudit(row.id, 3)">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    <span>驳回</span>
                  </button>
                </template>
                <template v-else>
                  <span class="ops-done-text" :class="activeTab">{{ activeTab === 'approved' ? '已通过' : '已驳回' }}</span>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination[activeTab].pageNum"
          :page-size="pagination[activeTab].pageSize"
          :total="pagination[activeTab].total"
          layout="prev, pager, next, jumper, ->, total"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.comment-manage {
  padding: 28px 32px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}
.header-left { display: flex; flex-direction: column; gap: 6px; }
.header-title-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(59,130,246,0.2), rgba(59,130,246,0.05));
  color: #60a5fa;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(59,130,246,0.15);
  animation: hdr-icon-in 0.5s cubic-bezier(0.34,1.56,0.64,1);
}
@keyframes hdr-icon-in {
  from { transform: scale(0) rotate(-20deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}
.page-title { margin: 0; font-size: 26px; font-weight: 700; color: #0f172a; letter-spacing: -0.5px; }
.page-subtitle { margin: 4px 0 0; font-size: 14px; color: #64748b; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  position: relative;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(203,213,225,0.4);
  box-shadow: 0 8px 25px rgba(0,0,0,0.06);
}
.stat-card.active {
  border-color: var(--accent);
  box-shadow: 0 4px 20px color-mix(in srgb, var(--accent) 8%, transparent);
}
.stat-bg { position: absolute; inset: 0; opacity: 0.4; pointer-events: none; }
.stat-inner { position: relative; display: flex; align-items: center; gap: 10px; padding: 14px; }
.stat-icon-wrap {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; color: #fff; flex-shrink: 0;
  transition: transform 0.3s ease;
}
.stat-card.active .stat-icon-wrap {
  transform: scale(1.05);
}
.stat-info { display: flex; flex-direction: column; min-width: 0; }
.stat-num { font-size: 20px; font-weight: 800; color: #0f172a; line-height: 1.1; letter-spacing: -0.5px; transition: color 0.3s; }
.stat-card.active .stat-num { color: var(--accent); }
.stat-label { font-size: 11px; color: #94a3b8; font-weight: 500; margin-top: 1px; white-space: nowrap; }
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.status-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  background: rgba(203,213,225,0.15);
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 10px;
}
.status-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
  white-space: nowrap;
}
.status-tab:hover { color: #475569; background: rgba(203,213,225,0.2); }
.status-tab.active { background: rgba(203,213,225,0.3); color: var(--accent); }
.status-dot {
  width: 6px; height: 6px; border-radius: 50%;
  transition: transform 0.25s ease;
}
.status-tab.active .status-dot { transform: scale(1.3); }
.status-count {
  font-size: 10px; font-weight: 700; color: #fff;
  padding: 1px 6px; border-radius: 6px;
  min-width: 16px; text-align: center; line-height: 1.4;
}
.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
  white-space: nowrap;
}
.action-btn:hover { transform: translateY(-1px); }
.export-btn { background: rgba(203,213,225,0.3); color: #94a3b8; border: 1px solid rgba(203,213,225,0.3); }
.export-btn:hover { background: rgba(203,213,225,0.4); color: #334155; }
.delete-btn { background: rgba(239,68,68,0.1); color: #fca5a5; border: 1px solid rgba(239,68,68,0.15); animation: del-btn-in 0.3s ease; }
@keyframes del-btn-in { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }
.delete-btn:hover { background: rgba(239,68,68,0.18); color: #fecaca; }

.panel {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
  animation: panel-in 0.4s cubic-bezier(0.22,1,0.36,1);
}
@keyframes panel-in { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid rgba(203,213,225,0.3); }
.panel-title { display: flex; align-items: center; gap: 9px; font-size: 14px; font-weight: 600; color: #334155; }
.panel-badge { font-size: 12px; color: #94a3b8; padding: 4px 12px; border-radius: 12px; background: rgba(203,213,225,0.2); }
.table-wrapper { padding: 0; }

.cell-id { color: #64748b; font-family: monospace; font-size: 12px; }
.user-tag { font-size: 13px; padding: 3px 10px; border-radius: 12px; background: rgba(59,130,246,0.1); color: #a5b4fc; font-weight: 500; }
.article-title-text { color: #475569; font-size: 13px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.content-text { color: #475569; font-size: 13px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.reject-reason { color: #fca5a5; font-size: 12px; display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.time-text { color: #64748b; font-size: 13px; }

.ops-group { display: flex; gap: 4px; justify-content: center; }
.ops-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
  white-space: nowrap;
}
.ops-pass { background: rgba(52,211,153,0.1); color: #34d399; border-color: rgba(52,211,153,0.15); }
.ops-pass:hover { background: rgba(52,211,153,0.2); color: #6ee7b7; transform: translateY(-1px); }
.ops-reject { background: rgba(251,191,36,0.1); color: #fcd34d; border-color: rgba(251,191,36,0.15); }
.ops-reject:hover { background: rgba(251,191,36,0.2); color: #fde68a; transform: translateY(-1px); }
.ops-done-text { font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 12px; }
.ops-done-text.approved { background: rgba(52,211,153,0.1); color: #34d399; }
.ops-done-text.rejected { background: rgba(248,113,113,0.1); color: #f87171; }

.pagination-wrapper { display: flex; justify-content: center; padding: 20px; }

:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-row-hover-bg-color: rgba(59,130,246,0.04); --el-table-border-color: transparent; --el-table-text-color: #475569; --el-table-header-text-color: #94a3b8; }
:deep(.el-table th.el-table__cell) { background: transparent; border-bottom: 1px solid rgba(203,213,225,0.3); padding: 16px 0; font-weight: 600; }
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(203,213,225,0.2); padding: 14px 0; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: #94a3b8; --el-pagination-button-bg-color: rgba(203,213,225,0.2); --el-pagination-hover-color: #60a5fa; --el-pagination-button-color: #94a3b8; }
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) { background: rgba(203,213,225,0.2) !important; border-radius: 8px !important; }
:deep(.el-pagination .btn-prev:hover), :deep(.el-pagination .btn-next:hover) { background: rgba(203,213,225,0.4) !important; color: #60a5fa; }
:deep(.el-pagination button.is-active) { background: linear-gradient(135deg, #3b82f6, #60a5fa); color: #fff; }
:deep(.el-input__wrapper) { background: rgba(203,213,225,0.2); box-shadow: none; border: 1px solid rgba(203,213,225,0.4); border-radius: 8px; }
:deep(.el-input__wrapper:hover) { border-color: rgba(59,130,246,0.2); }
:deep(.el-input__wrapper.is-focus) { border-color: #3b82f6; }
:deep(.el-input__inner) { color: #334155; }
:deep(.el-checkbox__input .el-checkbox__inner) { border-color: rgba(203,213,225,0.5); }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background: #60a5fa; border-color: #60a5fa; }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .comment-manage { padding: 20px 16px; } .stats-row { grid-template-columns: repeat(2, 1fr); } .toolbar { flex-direction: column; align-items: stretch; } }
</style>
