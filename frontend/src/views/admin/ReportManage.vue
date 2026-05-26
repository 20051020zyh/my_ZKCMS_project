<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getArticleReportList, auditArticleReport } from '@/api/articleReport'
import { getCommentReportList, auditCommentReport } from '@/api/comment'
import { exportToCSV, exportToJSON } from '@/utils/export'

type ReportType = 'article' | 'comment'
type ReportStatus = 'pending' | 'confirmed' | 'rejected'

const reportType = ref<ReportType>('article')
const activeTab = ref<ReportStatus>('pending')
const loading = ref(false)
const reportData = ref<any[]>([])
const pagination = ref({ pageNum: 1, pageSize: 10, total: 0 })
const stats = ref({ article: { pending: 0, confirmed: 0, rejected: 0 }, comment: { pending: 0, confirmed: 0, rejected: 0 } })

const typeCards = [
  {
    key: 'article' as ReportType,
    label: '文章举报',
    desc: '对已发布文章的违规举报',
    color: '#4ade80',
    bgGrad: 'linear-gradient(135deg, #4ade8015, #4ade8003)',
    iconGrad: 'linear-gradient(135deg, #4ade80, #22c55e)',
    activeShadow: '0 0 0 1px rgba(74,222,128,0.4), 0 4px 20px rgba(74,222,128,0.08)',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`
  },
  {
    key: 'comment' as ReportType,
    label: '评论举报',
    desc: '对文章评论的违规举报',
    color: '#06b6d4',
    bgGrad: 'linear-gradient(135deg, #06b6d415, #06b6d403)',
    iconGrad: 'linear-gradient(135deg, #06b6d4, #0891b2)',
    activeShadow: '0 0 0 1px rgba(6,182,212,0.4), 0 4px 20px rgba(6,182,212,0.08)',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="12" y1="9" x2="16" y2="9"/><line x1="8" y1="13" x2="16" y2="13"/></svg>`
  }
]

const statusTabs = [
  { key: 'pending' as ReportStatus, label: '待处理', color: '#f59e0b', status: 0 },
  { key: 'confirmed' as ReportStatus, label: '已确认', color: '#ef4444', status: 1 },
  { key: 'rejected' as ReportStatus, label: '已驳回', color: '#64748b', status: 2 },
]

const currentType = computed(() => typeCards.find(t => t.key === reportType.value)!)

const formatDate = (s: string) => {
  if (!s) return ''
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待处理', 1: '已确认', 2: '已驳回' }
  return map[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const tab = statusTabs.find(t => t.key === activeTab.value)!
    const params = { pageNum: pagination.value.pageNum, pageSize: pagination.value.pageSize, status: tab.status }
    let res: any
    if (reportType.value === 'article') {
      res = await getArticleReportList(params)
    } else {
      res = await getCommentReportList(params)
    }
    reportData.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    for (const type of ['article', 'comment'] as ReportType[]) {
      const results = await Promise.all(
        statusTabs.map(tab =>
          type === 'article'
            ? getArticleReportList({ pageNum: 1, pageSize: 1, status: tab.status })
            : getCommentReportList({ pageNum: 1, pageSize: 1, status: tab.status })
        )
      )
      stats.value[type] = {
        pending: results[0].data?.total || 0,
        confirmed: results[1].data?.total || 0,
        rejected: results[2].data?.total || 0,
      }
    }
  } catch { /* ignore */ }
}

const handleTypeChange = (key: ReportType) => {
  reportType.value = key
  activeTab.value = 'pending'
  pagination.value.pageNum = 1
  fetchData()
  fetchStats()
}

const handleStatusChange = (key: ReportStatus) => {
  activeTab.value = key
  pagination.value.pageNum = 1
  fetchData()
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page
  fetchData()
}

const handleAudit = async (id: number, status: number) => {
  const label = status === 1 ? '确认违规' : '驳回'
  const isConfirm = status === 1
  const typeLabel = reportType.value === 'article' ? '文章' : '评论'
  try {
    await ElMessageBox.confirm(
      `<div class="fx-c-header">
        <div class="fx-c-ico ${isConfirm ? 'danger' : 'ghost'}">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">${isConfirm ? '<path d="M12 9v4m0 4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>' : '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>'}</svg>
        </div>
      </div>
      <div class="fx-c-title">${label}${typeLabel}举报</div>
      <div class="fx-c-desc">${isConfirm ? '确认后该' + typeLabel + '将被标记为违规，前台将隐藏展示' : '驳回后该举报将被关闭，' + typeLabel + '保持正常展示'}</div>
      <div class="fx-c-warning ${isConfirm ? 'danger' : 'ghost'}">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${isConfirm ? '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>' : '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>'}</svg>
        <span>${isConfirm ? '确认违规后，该' + typeLabel + '将被隐藏' : '驳回举报后，该举报记录将被关闭'}</span>
      </div>`,
      '', {
        confirmButtonText: `确认${label}`,
        cancelButtonText: '取消',
        center: true,
        icon: undefined,
        dangerouslyUseHTMLString: true,
        customClass: 'fx-confirm-box',
        confirmButtonClass: isConfirm ? 'fx-c-btn-danger' : 'fx-c-btn-ghost'
      }
    )
    if (reportType.value === 'article') {
      await auditArticleReport(id, status)
    } else {
      await auditCommentReport(id, status)
    }
    ElMessage.success({ message: '处理成功', duration: 2000 })
    fetchData()
    fetchStats()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('处理失败') }
}

const formatReportType = (type: number) => {
  const map: Record<number, string> = { 1: '虚假信息', 2: '色情低俗', 3: '违法违规', 4: '侵权抄袭', 5: '垃圾广告', 6: '其他' }
  return map[type] || '未知'
}

const handleExportCSV = () => {
  if (!reportData.value.length) { ElMessage.warning('暂无数据'); return }
  const tl = reportType.value === 'article' ? '文章举报' : '评论举报'
  const sl = statusTabs.find(t => t.key === activeTab.value)?.label || ''
  const headers = ['id', 'type', 'content', 'user', 'targetId', 'time', 'status']
  const data = reportData.value.map((i: any) => ({
    id: i.id,
    type: formatReportType(i.reportType),
    content: i.content,
    user: i.userName || '匿名',
    targetId: reportType.value === 'article' ? i.articleId : i.commentId,
    time: formatDate(i.createTime),
    status: getStatusLabel(i.status)
  }))
  exportToCSV(data, headers, `${tl}-${sl}`)
  ElMessage.success({ message: '导出成功', duration: 2000 })
}

const handleExportJSON = () => {
  if (!reportData.value.length) { ElMessage.warning('暂无数据'); return }
  const tl = reportType.value === 'article' ? '文章举报' : '评论举报'
  const sl = statusTabs.find(t => t.key === activeTab.value)?.label || ''
  const data = reportData.value.map((i: any) => ({
    id: i.id,
    reportType: formatReportType(i.reportType),
    content: i.content,
    userName: i.userName || '匿名',
    targetId: reportType.value === 'article' ? i.articleId : i.commentId,
    createTime: i.createTime,
    status: getStatusLabel(i.status)
  }))
  exportToJSON(data, `${tl}-${sl}`)
  ElMessage.success({ message: '导出成功', duration: 2000 })
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="rm" v-loading="loading" element-loading-background="rgba(8,11,20,0.8)">
    <div class="rm-hd">
      <div class="rm-hd-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
      </div>
      <div>
        <h2 class="rm-hd-title">举报管理</h2>
        <p class="rm-hd-sub">统一处理文章和评论的违规举报</p>
      </div>
    </div>

    <!-- 两大类型卡片 -->
    <div class="rm-types">
      <div
        v-for="card in typeCards"
        :key="card.key"
        class="rm-type"
        :class="{ active: reportType === card.key }"
        :style="{ '--c': card.color, '--shadow': card.activeShadow }"
        @click="handleTypeChange(card.key)"
      >
        <div class="rm-type-bg" :style="{ background: card.bgGrad }"></div>
        <div class="rm-type-body">
          <div class="rm-type-top">
            <div class="rm-type-ico" :style="{ background: card.iconGrad }" v-html="card.icon"></div>
            <div class="rm-type-meta">
              <div class="rm-type-label">{{ card.label }}</div>
              <div class="rm-type-desc">{{ card.desc }}</div>
            </div>
            <div class="rm-type-arrow">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
          </div>
          <div class="rm-type-stats">
            <div class="rm-type-stat">
              <span class="rm-type-stat-dot" style="background:#f59e0b"></span>
              <span class="rm-type-stat-num">{{ stats[card.key].pending }}</span>
              <span class="rm-type-stat-lbl">待处理</span>
            </div>
            <div class="rm-type-stat">
              <span class="rm-type-stat-dot" style="background:#ef4444"></span>
              <span class="rm-type-stat-num">{{ stats[card.key].confirmed }}</span>
              <span class="rm-type-stat-lbl">已确认</span>
            </div>
            <div class="rm-type-stat">
              <span class="rm-type-stat-dot" style="background:#64748b"></span>
              <span class="rm-type-stat-num">{{ stats[card.key].rejected }}</span>
              <span class="rm-type-stat-lbl">已驳回</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="rm-tb">
      <div class="rm-tb-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.key"
          class="rm-tab"
          :class="{ active: activeTab === tab.key }"
          :style="{ '--c': tab.color }"
          @click="handleStatusChange(tab.key)"
        >
          <span class="rm-tab-dot" :style="{ background: tab.color }"></span>
          <span>{{ tab.label }}</span>
          <span v-if="stats[reportType][tab.key]" class="rm-tab-badge" :style="{ background: tab.color }">{{ stats[reportType][tab.key] }}</span>
        </button>
      </div>
      <div class="rm-tb-acts">
        <button class="rm-btn" @click="handleExportCSV">
          <Download :size="14" />
          <span>CSV</span>
        </button>
        <button class="rm-btn" @click="handleExportJSON">
          <Download :size="14" />
          <span>JSON</span>
        </button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rm-panel">
      <div class="rm-panel-hd">
        <div class="rm-panel-hd-l">
          <span class="rm-panel-hd-ico" :style="{ color: currentType.color }" v-html="currentType.icon"></span>
          <span>{{ currentType.label }} · {{ statusTabs.find(t => t.key === activeTab)?.label }}</span>
        </div>
        <span class="rm-panel-hd-count">共 {{ pagination.total }} 条</span>
      </div>
      <div class="rm-panel-body">
        <el-table :data="reportData" style="width:100%">
          <el-table-column label="ID" width="70" align="center">
            <template #default="{ row }"><span class="rm-id">{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column label="举报类型" width="110" align="center">
            <template #default="{ row }">
              <span class="rm-tag">{{ formatReportType(row.reportType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="举报内容" min-width="220">
            <template #default="{ row }">
              <span class="rm-cell">{{ row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column label="举报人" width="120" align="center">
            <template #default="{ row }">
              <span class="rm-user">{{ row.userName || '匿名' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="reportType === 'article' ? '被举报文章ID' : '被举报评论ID'" width="130" align="center">
            <template #default="{ row }">
              <span class="rm-id">{{ reportType === 'article' ? row.articleId : row.commentId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="150" align="center">
            <template #default="{ row }">
              <span class="rm-time">{{ formatDate(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <span class="rm-state" :class="'rm-state--' + (row.status === 0 ? 'pending' : row.status === 1 ? 'confirmed' : 'rejected')">
                {{ getStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <div class="rm-ops">
                <template v-if="activeTab === 'pending'">
                  <button class="rm-ops-btn danger" @click="handleAudit(row.id, 1)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4m0 4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>
                    <span>违规</span>
                  </button>
                  <button class="rm-ops-btn ghost" @click="handleAudit(row.id, 2)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    <span>驳回</span>
                  </button>
                </template>
                <span v-else class="rm-ops-done" :class="activeTab">{{ activeTab === 'confirmed' ? '已确认' : '已驳回' }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="rm-panel-ft">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next, ->, total"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.rm {
  padding: 28px 32px;
  min-height: 100%;
}

/* ── Header ── */
.rm-hd {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}
.rm-hd-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05));
  color: #818cf8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rm-hd-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.3px;
}
.rm-hd-sub {
  margin: 3px 0 0;
  font-size: 14px;
  color: #64748b;
}

/* ── Type Cards ── */
.rm-types {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.rm-type {
  position: relative;
  border-radius: 16px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.rm-type:hover {
  transform: translateY(-3px);
  border-color: rgba(255,255,255,0.08);
  box-shadow: 0 16px 40px rgba(0,0,0,0.35);
}
.rm-type.active {
  border-color: var(--c);
  box-shadow: var(--shadow);
}
.rm-type-bg {
  position: absolute;
  inset: 0;
  opacity: 0.6;
  pointer-events: none;
}
.rm-type-body {
  position: relative;
  padding: 20px 22px;
}
.rm-type-top {
  display: flex;
  align-items: center;
  gap: 14px;
}
.rm-type-ico {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  transition: transform 0.35s ease;
}
.rm-type.active .rm-type-ico {
  transform: scale(1.06);
}
.rm-type-meta {
  flex: 1;
  min-width: 0;
}
.rm-type-label {
  font-size: 17px;
  font-weight: 700;
  color: #f1f5f9;
  transition: color 0.3s;
}
.rm-type.active .rm-type-label {
  color: var(--c);
}
.rm-type-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
.rm-type-arrow {
  color: #475569;
  transition: all 0.35s ease;
}
.rm-type.active .rm-type-arrow {
  color: var(--c);
  transform: translateX(4px);
}
.rm-type-stats {
  display: flex;
  gap: 24px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(255,255,255,0.04);
}
.rm-type-stat {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rm-type-stat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.rm-type-stat-num {
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
  font-variant-numeric: tabular-nums;
  min-width: 18px;
}
.rm-type-stat-lbl {
  font-size: 12px;
  color: #64748b;
}
/* ── Toolbar ── */
.rm-tb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.rm-tb-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  background: rgba(255,255,255,0.02);
  border: 1px solid rgba(255,255,255,0.04);
  border-radius: 10px;
}
.rm-tab {
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
.rm-tab:hover { color: #cbd5e1; background: rgba(255,255,255,0.03); }
.rm-tab.active { background: rgba(255,255,255,0.05); color: var(--c); }
.rm-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  transition: transform 0.25s ease;
}
.rm-tab.active .rm-tab-dot { transform: scale(1.3); }
.rm-tab-badge {
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  padding: 1px 6px;
  border-radius: 6px;
  min-width: 16px;
  text-align: center;
  line-height: 1.4;
}
.rm-tb-acts {
  display: flex;
  gap: 8px;
}
.rm-btn {
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
  background: rgba(255,255,255,0.04);
  color: #94a3b8;
  border: 1px solid rgba(255,255,255,0.06);
}
.rm-btn:hover { transform: translateY(-1px); background: rgba(255,255,255,0.08); color: #e2e8f0; }

/* ── Panel ── */
.rm-panel {
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
}
.rm-panel-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.rm-panel-hd-l {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}
.rm-panel-hd-ico {
  display: flex;
  flex-shrink: 0;
}
.rm-panel-hd-count {
  font-size: 12px;
  color: #94a3b8;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
}
.rm-panel-body {
  padding: 0;
}
.rm-panel-ft {
  display: flex;
  justify-content: center;
  padding: 20px;
}

/* ── Cell Styles ── */
.rm-id { color: #64748b; font-family: monospace; font-size: 12px; }
.rm-tag { font-size: 12px; padding: 3px 10px; border-radius: 12px; background: rgba(239,68,68,0.1); color: #fca5a5; font-weight: 500; white-space: nowrap; }
.rm-cell { color: #cbd5e1; font-size: 13px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.rm-cell.sm { -webkit-line-clamp: 1; }
.rm-user { font-size: 13px; padding: 3px 10px; border-radius: 12px; background: rgba(99,102,241,0.1); color: #a5b4fc; font-weight: 500; white-space: nowrap; }
.rm-time { color: #64748b; font-size: 13px; }
.rm-state {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  white-space: nowrap;
}
.rm-state--pending { background: rgba(245,158,11,0.12); color: #fbbf24; }
.rm-state--confirmed { background: rgba(239,68,68,0.12); color: #f87171; }
.rm-state--rejected { background: rgba(100,116,139,0.12); color: #94a3b8; }

/* ── Ops ── */
.rm-ops { display: flex; gap: 4px; justify-content: center; }
.rm-ops-btn {
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
.rm-ops-btn.danger { background: rgba(239,68,68,0.1); color: #fca5a5; border-color: rgba(239,68,68,0.15); }
.rm-ops-btn.danger:hover { background: rgba(239,68,68,0.2); color: #f87171; border-color: rgba(239,68,68,0.3); transform: translateY(-1px); }
.rm-ops-btn.ghost { background: rgba(255,255,255,0.03); color: #94a3b8; border-color: rgba(255,255,255,0.06); }
.rm-ops-btn.ghost:hover { background: rgba(255,255,255,0.06); color: #cbd5e1; border-color: rgba(255,255,255,0.1); transform: translateY(-1px); }
.rm-ops-done { font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 12px; }
.rm-ops-done.confirmed { background: rgba(239,68,68,0.1); color: #f87171; }
.rm-ops-done.rejected { background: rgba(100,116,139,0.1); color: #94a3b8; }

/* ── Table Overrides ── */
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(99,102,241,0.04);
  --el-table-border-color: transparent;
  --el-table-text-color: #cbd5e1;
  --el-table-header-text-color: #94a3b8;
}
:deep(.el-table th.el-table__cell) { background: transparent; border-bottom: 1px solid rgba(255,255,255,0.05); padding: 16px 0; font-weight: 600; }
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(255,255,255,0.03); padding: 14px 0; }
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #94a3b8;
  --el-pagination-button-bg-color: rgba(255,255,255,0.03);
  --el-pagination-hover-color: #818cf8;
  --el-pagination-button-color: #94a3b8;
}
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) { background: rgba(255,255,255,0.03) !important; border-radius: 8px !important; }
:deep(.el-pagination .btn-prev:hover), :deep(.el-pagination .btn-next:hover) { background: rgba(255,255,255,0.08) !important; color: #818cf8; }
:deep(.el-pagination button.is-active) { background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.03); box-shadow: none; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; }
:deep(.el-input__wrapper:hover) { border-color: rgba(99,102,241,0.2); }
:deep(.el-input__wrapper.is-focus) { border-color: #6366f1; }
:deep(.el-input__inner) { color: #e2e8f0; }

@media (max-width: 900px) {
  .rm-types {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .rm { padding: 20px 16px; }
  .rm-type-stats { gap: 16px; }
  .rm-tb { flex-direction: column; align-items: stretch; }
  .rm-tb-tabs { justify-content: center; }
  .rm-tb-acts { justify-content: flex-end; }
}
</style>
