<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/category'
import { exportToCSV, exportToJSON } from '@/utils/export'

const loading = ref(false)
const categories = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

const form = ref({
  id: null as number | null,
  categoryName: '',
  categoryAlias: ''
})

const categoryStats = computed(() => ({
  total: categories.value.length,
}))

const fetchCategories = async () => {
  loading.value = true
  try {
    const res: any = await getUserCategoryList()
    categories.value = res.data || []
  } catch {
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  isEdit.value = false
  form.value = { id: null, categoryName: '', categoryAlias: '' }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑分类'
  isEdit.value = true
  form.value = { id: row.id, categoryName: row.categoryName, categoryAlias: row.categoryAlias }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.categoryName) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (isEdit.value) {
      await updateCategory(form.value)
      ElMessage.success('更新成功')
    } else {
      await addCategory(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      `<div class="fx-c-header">
        <div class="fx-c-ico danger">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
        </div>
      </div>
      <div class="fx-c-title">删除分类</div>
      <div class="fx-c-desc">删除后数据将无法恢复，请谨慎操作</div>
      <div class="fx-c-warning danger">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>即将删除该分类，此操作不可撤销</span>
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
    await deleteCategory(id)
    ElMessage.success('删除成功')
    fetchCategories()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const handleExportCSV = () => {
  if (!categories.value.length) { ElMessage.warning('暂无数据可导出'); return }
  const headers = ['id', 'categoryName', 'categoryAlias', 'createTime']
  const data = categories.value.map(i => ({ id: i.id, categoryName: i.categoryName, categoryAlias: i.categoryAlias, createTime: formatDate(i.createTime) }))
  exportToCSV(data, headers, '分类列表')
  ElMessage.success('导出成功')
}

const handleExportJSON = () => {
  if (!categories.value.length) { ElMessage.warning('暂无数据可导出'); return }
  const data = categories.value.map(i => ({ id: i.id, categoryName: i.categoryName, categoryAlias: i.categoryAlias, createTime: i.createTime }))
  exportToJSON(data, '分类列表')
  ElMessage.success('导出成功')
}

onMounted(() => { fetchCategories() })
</script>

<template>
  <div class="cat-page" v-loading="loading" element-loading-background="rgba(8,11,20,0.8)">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">分类管理</h2>
        <p class="page-subtitle">管理文章分类，方便用户按分类浏览内容</p>
      </div>
      <div class="header-actions">
        <div class="dropdown-wrap">
          <button class="action-btn ghost-btn" @click="handleExportCSV">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span>导出</span>
          </button>
        </div>
        <button class="action-btn add-btn" @click="handleAdd">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>新增分类</span>
        </button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card stat-card-sm">
        <div class="stat-card-inner-sm">
          <div class="stat-icon-wrap-sm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
          </div>
          <div class="stat-info-sm">
            <span class="stat-label-sm">分类总数</span>
            <span class="stat-num-sm">{{ categoryStats.total }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <div class="panel-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:#60a5fa"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
          <span>分类列表</span>
        </div>
        <span class="panel-badge">共 {{ categoryStats.total }} 个分类</span>
      </div>
      <div class="table-wrapper">
        <table class="cm-table">
          <thead>
            <tr>
              <th>分类ID</th>
              <th>分类名称</th>
              <th>分类别名</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && categories.length === 0">
              <td colspan="5" class="empty-row">
                <div class="empty-cell">
                  <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1.2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
                  <p>暂无分类数据</p>
                </div>
              </td>
            </tr>
            <tr v-for="cat in categories" :key="cat.id">
              <td><span class="cell-id">#{{ cat.id }}</span></td>
              <td>
                <div class="name-cell">
                  <span class="name-text">{{ cat.categoryName }}</span>
                </div>
              </td>
              <td><span class="alias-tag">{{ cat.categoryAlias || '-' }}</span></td>
              <td><span class="time-text">{{ formatDate(cat.createTime) }}</span></td>
              <td>
                <div class="ops-group">
                  <button class="ops-btn edit" @click="handleEdit(cat)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                    <span>编辑</span>
                  </button>
                  <button class="ops-btn del" @click="handleDelete(cat.id)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                    <span>删除</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false" custom-class="cat-dialog">
      <div class="dialog-body">
        <div class="form-group">
          <label class="form-label">分类名称</label>
          <div class="form-input-wrap">
            <input v-model="form.categoryName" placeholder="请输入分类名称" class="form-input" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">分类别名</label>
          <div class="form-input-wrap">
            <input v-model="form.categoryAlias" placeholder="请输入分类别名（如：tech）" class="form-input" />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="d-btn d-btn-ghost" @click="dialogVisible = false">取消</button>
          <button class="d-btn d-btn-primary" @click="handleSave">保存</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cat-page {
  padding: 24px 32px;
  min-height: 100%;
}

/* ── Header ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.header-left { display: flex; flex-direction: column; gap: 6px; }
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}
.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* ── Buttons ── */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.ghost-btn {
  background: rgba(203,213,225,0.3);
  color: #94a3b8;
  border: 1px solid rgba(203,213,225,0.3);
}
.ghost-btn:hover {
  background: rgba(203,213,225,0.4);
  color: #334155;
}
.add-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 4px 14px rgba(59,130,246,0.3);
}
.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59,130,246,0.45);
}

/* ── Stats ── */
.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; margin-bottom: 20px; }
.stat-card {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
}
.stat-card-sm { max-width: 220px; }
.stat-card-inner-sm {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
}
.stat-icon-wrap-sm {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  box-shadow: 0 4px 12px rgba(59,130,246,0.3);
}
.stat-info-sm { display: flex; flex-direction: column; gap: 1px; }
.stat-label-sm { font-size: 11px; color: #64748b; font-weight: 500; letter-spacing: 0.3px; }
.stat-num-sm { font-size: 18px; font-weight: 700; color: #0f172a; letter-spacing: -0.3px; line-height: 1.2; }

/* ── Panel ── */
.panel {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(203,213,225,0.3);
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
.panel-badge {
  font-size: 12px;
  color: #64748b;
}

/* ── Table ── */
.table-wrapper { overflow-x: auto; }
.cm-table {
  width: 100%;
  border-collapse: collapse;
}
.cm-table thead th {
  padding: 14px 20px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(203,213,225,0.3);
  background: rgba(203,213,225,0.15);
}
.cm-table tbody td {
  padding: 14px 20px;
  font-size: 13px;
  color: #475569;
  border-bottom: 1px solid rgba(203,213,225,0.2);
}
.cm-table tbody tr:hover td { background: rgba(59,130,246,0.03); }
.cm-table tbody tr:last-child td { border-bottom: none; }
.cell-id { color: #64748b; font-family: monospace; font-size: 12px; }
.name-text { color: #334155; font-weight: 500; }
.alias-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: rgba(59,130,246,0.08);
  color: #a5b4fc;
}
.time-text { font-size: 12px; color: #64748b; font-family: monospace; }

/* ── Ops ── */
.ops-group { display: flex; gap: 6px; }
.ops-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #94a3b8;
  background: transparent;
}
.ops-btn.edit:hover { background: rgba(59,130,246,0.1); color: #60a5fa; }
.ops-btn.del:hover { background: rgba(239,68,68,0.1); color: #f87171; }

/* ── Empty ── */
.empty-row td { padding: 0 !important; }
.empty-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 12px;
}
.empty-cell p { margin: 0; font-size: 13px; color: #475569; }

/* ── Dialog ── */
.dialog-body { padding: 4px 0; }
.form-group { margin-bottom: 18px; }
.form-group:last-child { margin-bottom: 0; }
.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}
.form-input-wrap { width: 100%; }
.form-input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(203,213,225,0.4);
  border-radius: 10px;
  background: rgba(203,213,225,0.2);
  color: #334155;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}
.form-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.15); }
.form-input::placeholder { color: #475569; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
.d-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}
.d-btn-ghost { background: rgba(203,213,225,0.3); color: #94a3b8; border: 1px solid rgba(203,213,225,0.3); }
.d-btn-ghost:hover { background: rgba(203,213,225,0.4); color: #334155; }
.d-btn-primary { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; box-shadow: 0 4px 14px rgba(59,130,246,0.3); }
.d-btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(59,130,246,0.45); }

/* ── Element Plus overrides ── */
:deep(.el-dialog) {
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}
:deep(.el-dialog__header) { padding: 20px 24px 0; }
:deep(.el-dialog__title) { font-size: 17px; font-weight: 600; color: #0f172a; }
:deep(.el-dialog__body) { padding: 20px 24px; }
:deep(.el-dialog__footer) { padding: 0 24px 20px; }
:deep(.el-dialog__close) { color: #475569; font-size: 16px; }
:deep(.el-dialog__close:hover) { color: #94a3b8; }
:deep(.el-loading-mask) { border-radius: 14px; }
</style>

<style>
.fx-confirm-box {
  padding: 0 !important;
  border: none !important;
  border-radius: 20px !important;
  width: 400px !important;
  background: #ffffff !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4), 0 0 0 1px rgba(203,213,225,0.3) !important;
  animation: fxCIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  overflow: hidden !important;
}
@keyframes fxCIn {
  from { opacity: 0; transform: scale(0.85) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.fx-confirm-box .el-message-box__header { display: none !important; }
.fx-confirm-box .el-message-box__content { padding: 0 !important; }
.fx-confirm-box .el-message-box__container { padding: 0 !important; display: flex !important; flex-direction: column !important; position: static !important; }
.fx-confirm-box .el-message-box__message { margin: 0 !important; padding: 0 !important; flex: none !important; }
.fx-confirm-box .el-message-box__btns {
  padding: 0 28px 28px !important;
  display: flex !important;
  gap: 12px !important;
  justify-content: center !important;
  border-top: 1px solid rgba(203,213,225,0.3) !important;
  margin-top: 0 !important;
}
.fx-confirm-box .el-message-box__btns .el-button {
  height: 44px !important;
  padding: 0 28px !important;
  border-radius: 12px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  font-family: inherit !important;
  margin: 0 !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  flex: 1 !important;
  border: none !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  letter-spacing: 0.3px !important;
}
.fx-confirm-box .el-message-box__btns .el-button--default { 
  background: rgba(203,213,225,0.3) !important; 
  color: #94a3b8 !important; 
  border: 1px solid rgba(203,213,225,0.5) !important; 
}
.fx-confirm-box .el-message-box__btns .el-button--default:hover { 
  background: rgba(203,213,225,0.5) !important; 
  color: #334155 !important; 
  transform: translateY(-1px) !important;
}
.fx-c-btn-pass { 
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important; 
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(16,185,129,0.35) !important;
}
.fx-c-btn-pass:hover { 
  transform: translateY(-2px) !important; 
  box-shadow: 0 8px 25px rgba(16,185,129,0.45) !important; 
}
.fx-c-btn-reject { 
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important; 
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(245,158,11,0.35) !important;
}
.fx-c-btn-reject:hover { 
  transform: translateY(-2px) !important; 
  box-shadow: 0 8px 25px rgba(245,158,11,0.45) !important; 
}
.fx-c-btn-danger { 
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important; 
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(239,68,68,0.35) !important;
}
.fx-c-btn-danger:hover { 
  transform: translateY(-2px) !important; 
  box-shadow: 0 8px 25px rgba(239,68,68,0.45) !important; 
}
.fx-c-btn-ghost { 
  background: rgba(148,163,184,0.1) !important; 
  color: #475569 !important; 
  border: 1px solid rgba(148,163,184,0.2) !important; 
}
.fx-c-btn-ghost:hover { 
  background: rgba(148,163,184,0.18) !important; 
  color: #334155 !important; 
  transform: translateY(-1px) !important;
}
.fx-c-header {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 36px 28px 20px;
}
.fx-c-ico {
  width: 64px; 
  height: 64px;
  border-radius: 18px;
  display: flex; 
  align-items: center; 
  justify-content: center;
  color: #94a3b8;
  flex-shrink: 0;
  position: relative;
}
.fx-c-ico.pass { 
  color: #34d399; 
  background: linear-gradient(135deg, rgba(52,211,153,0.15), rgba(16,185,129,0.08));
  box-shadow: 0 8px 24px rgba(52,211,153,0.2);
}
.fx-c-ico.reject { 
  color: #fbbf24; 
  background: linear-gradient(135deg, rgba(251,191,36,0.15), rgba(245,158,11,0.08));
  box-shadow: 0 8px 24px rgba(251,191,36,0.2);
}
.fx-c-ico.danger { 
  color: #f87171; 
  background: linear-gradient(135deg, rgba(248,113,113,0.15), rgba(239,68,68,0.08));
  box-shadow: 0 8px 24px rgba(248,113,113,0.2);
}
.fx-c-ico.ghost { 
  color: #94a3b8; 
  background: linear-gradient(135deg, rgba(148,163,184,0.12), rgba(100,116,139,0.06));
  box-shadow: 0 8px 24px rgba(148,163,184,0.15);
}
.fx-c-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  padding: 0 28px;
  margin-bottom: 8px;
  letter-spacing: -0.3px;
  text-align: center;
}
.fx-c-desc {
  font-size: 14px;
  color: #64748b;
  padding: 0 28px;
  margin-bottom: 20px;
  line-height: 1.6;
  text-align: center;
}
.fx-c-warning {
  margin: 0 28px 24px;
  padding: 16px 18px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}
.fx-c-warning.pass {
  background: linear-gradient(135deg, rgba(52,211,153,0.1), rgba(16,185,129,0.05));
  border: 1px solid rgba(52,211,153,0.2);
  color: #34d399;
}
.fx-c-warning.reject {
  background: linear-gradient(135deg, rgba(251,191,36,0.1), rgba(245,158,11,0.05));
  border: 1px solid rgba(251,191,36,0.2);
  color: #fbbf24;
}
.fx-c-warning.danger {
  background: linear-gradient(135deg, rgba(248,113,113,0.1), rgba(239,68,68,0.05));
  border: 1px solid rgba(248,113,113,0.2);
  color: #f87171;
}
.fx-c-warning.ghost {
  background: linear-gradient(135deg, rgba(148,163,184,0.08), rgba(100,116,139,0.04));
  border: 1px solid rgba(148,163,184,0.15);
  color: #94a3b8;
}
.fx-c-warning svg {
  flex-shrink: 0;
  margin-top: 2px;
}
.fx-c-hl { color: #f87171; font-weight: 700; }
</style>