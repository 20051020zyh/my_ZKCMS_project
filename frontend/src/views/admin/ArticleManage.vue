<script setup lang="ts">
import { ref, shallowRef, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox, ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus';
import {
  Plus,
  CircleCheck,
  Delete,
  Edit,
  Document,
  View,
  Star
} from '@element-plus/icons-vue';
import { getArticleList, deleteArticle, batchUpdateStatus, addArticle, getArticleDetail, updateArticle, getArticleStats } from '@/api/article';
import { getAllCategories } from '@/api/category';
import { exportToCSV, exportToJSON } from '@/utils/export';
import request from '@/utils/request';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const loading = ref(false);
const articles = ref<any[]>([]);
const categories = ref<any[]>([]);
const selectedArticles = ref<number[]>([]);
const editorRef = shallowRef<any>(null);
const statsLoading = ref(false);

// 统计数据 - 从接口获取真实数据
const articleStats = ref({
  published: 0,
  draft: 0,
  totalViews: 0,
  totalLikes: 0
});

// 辅助函数
const formatNumber = (num: number): string => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
  return String(num);
};

const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
};

const toolbarConfig = {
  excludeKeys: []
};

const editorConfig = {
  placeholder: '请输入文章内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/upload',
      fieldName: 'file',
      maxFileSize: 10 * 1024 * 1024,
      customInsert(res: any, insertFn: any) {
        if (res && res.data) {
          insertFn(res.data);
        }
      }
    }
  }
};

const handleCreated = (editor: any) => {
  editorRef.value = editor;
};

const pagination = ref({
 pageNum: 1,
 pageSize: 10,
 total: 0
});
const searchForm = ref({
 categoryId: null as number | null,
 state: '',
 keyword: ''
});
const dialogVisible = ref(false);
const dialogTitle = ref('新增文章');
const isEdit = ref(false);
const coverImage = ref<any[]>([]);

// 自定义上下架确认弹窗
const statusDialogVisible = ref(false);
const statusDialogTarget = ref({ id: 0, status: 0, actionText: '' });
const statusDialogLoading = ref(false);
const form = ref({
 id: null as number | null,
 title: '',
 content: '',
 categoryId: null as number | null,
 state: '已发布',
 coverImg: '',
 tags: ''
});
const fetchArticles = async () => {
 loading.value = true;
 try {
 const params: any = {
 pageNum: pagination.value.pageNum,
 pageSize: pagination.value.pageSize,
 state: searchForm.value.state
 };
 if (searchForm.value.categoryId) {
 params.categoryId = searchForm.value.categoryId;
 }
 if (searchForm.value.keyword) {
 params.keyword = searchForm.value.keyword;
 }
 const res: any = await getArticleList(params);
 articles.value = res.data?.records || [];
 pagination.value.total = res.data?.total || 0;
 }
 catch (error) {
 ElMessage.error('获取文章列表失败');
 }
 finally {
 loading.value = false;
 }
};
const fetchCategories = async () => {
 try {
 const res: any = await getAllCategories();
 categories.value = res.data || [];
 }
 catch (error) {
 console.error('获取分类失败:', error);
 }
};
const handleSearch = () => {
 pagination.value.pageNum = 1;
 fetchArticles();
};
const handleReset = () => {
 searchForm.value = {
 categoryId: null,
 state: '',
 keyword: ''
 };
 handleSearch();
};
const handlePageChange = (page: number) => {
 pagination.value.pageNum = page;
 fetchArticles();
};
const handleSelectionChange = (selection: any[]) => {
 selectedArticles.value = selection.map(item => item.id);
};
const handleDelete = async (id: number) => {
 try {
 await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 });
 await deleteArticle(id);
 ElMessage.success('删除成功');
 fetchArticles();
 fetchStats();
 }
 catch (error: any) {
 if (error !== 'cancel') {
 ElMessage.error('删除失败');
 }
 }
};
const handleToggleStatus = async (id: number, status: number) => {
  const actionText = status === 0 ? '上架' : '下架';
  statusDialogTarget.value = { id, status, actionText };
  statusDialogVisible.value = true;
};
const confirmToggleStatus = async () => {
  const { id, status, actionText } = statusDialogTarget.value;
  statusDialogLoading.value = true;
  try {
    await request.post('/article/batch/updateStatus', { ids: [id], status });
    ElMessage.success({ message: `${actionText}成功`, duration: 2000 });
    statusDialogVisible.value = false;
    fetchArticles();
    fetchStats();
  } catch (error: any) {
    ElMessage.error(`${actionText}失败`);
  } finally {
    statusDialogLoading.value = false;
  }
};
const handleBatchOperation = async (state: string) => {
 if (selectedArticles.value.length === 0) {
 ElMessage.warning('请先选择文章');
 return;
 }
 try {
 await ElMessageBox.confirm(`确定要将选中的文章设为"${state}"吗？`, '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 });
 await batchUpdateStatus({
 articleId: selectedArticles.value,
 state
 });
 ElMessage.success('操作成功');
 selectedArticles.value = [];
 fetchArticles();
 fetchStats();
 }
 catch (error: any) {
 if (error !== 'cancel') {
 ElMessage.error('操作失败');
 }
 }
};
const handleAdd = () => {
 dialogTitle.value = '新增文章';
 isEdit.value = false;
 form.value = {
 id: null,
 title: '',
 content: '',
 categoryId: null,
 state: '已发布',
 coverImg: '',
 tags: ''
 };
 coverImage.value = [];
 dialogVisible.value = true;
};
const handleEdit = async (row: any) => {
 dialogTitle.value = '编辑文章';
 isEdit.value = true;
 try {
 const res: any = await getArticleDetail(row.id);
 form.value = {
 id: res.data?.id,
 title: res.data?.title || '',
 content: res.data?.content || '',
 categoryId: res.data?.categoryId || null,
 state: res.data?.state || '已发布',
 coverImg: res.data?.coverImg || '',
 tags: res.data?.tags ? res.data.tags.join(',') : ''
 };
 coverImage.value = res.data?.coverImg ? [{ url: res.data.coverImg }] : [];
 dialogVisible.value = true;
 }
 catch (error) {
 ElMessage.error('获取文章详情失败');
 }
};
const handleUploadSuccess = (response: any) => {
 if (response && response.data) {
 form.value.coverImg = response.data;
 coverImage.value = [{ url: response.data }];
 ElMessage.success('图片上传成功');
 }
};

const handleUploadError = () => {
 ElMessage.error('图片上传失败，请重试');
};

const handleBeforeUpload = (file: File) => {
 const isImage = file.type.startsWith('image/');
 if (!isImage) {
 ElMessage.error('只能上传图片文件！');
 return false;
 }
 const isLt5M = file.size / 1024 / 1024 < 5;
 if (!isLt5M) {
 ElMessage.error('图片大小不能超过 5MB！');
 return false;
 }
 return true;
};

const getCategoryName = (categoryId: number | null) => {
  if (!categoryId) return '未分类';
  const cat = categories.value.find(c => c.id === categoryId)
  return cat ? cat.name : '未分类'
};

const uploadHeaders = computed(() => {
 const token = localStorage.getItem('token');
 return {
 Authorization: token ? `Bearer ${token}` : ''
 };
});
const handleRemoveCover = () => {
 form.value.coverImg = '';
 coverImage.value = [];
};
const handleSave = async () => {
 if (!form.value.title.trim()) {
 ElMessage.warning('请输入文章标题');
 return;
 }
 if (!form.value.content.trim()) {
 ElMessage.warning('请输入文章内容');
 return;
 }
 if (!form.value.categoryId) {
 ElMessage.warning('请选择文章分类');
 return;
 }
 try {
 const data = {
 ...form.value,
 tags: form.value.tags ? form.value.tags.split(',').map((t: string) => t.trim()) : []
 };
 if (isEdit.value) {
 await updateArticle(data);
 ElMessage.success('更新成功');
 }
 else {
 await addArticle(data);
 ElMessage.success('新增成功');
 }
 dialogVisible.value = false;
 fetchArticles();
 fetchStats();
 }
 catch (error) {
 ElMessage.error('保存失败');
 }
};
const handleBatchDelete = async () => {
 if (selectedArticles.value.length === 0) {
 ElMessage.warning('请先选择要删除的文章');
 return;
 }
 try {
 await ElMessageBox.confirm('确定要批量删除选中的文章吗？', '提示', {
 confirmButtonText: '确定',
 cancelButtonText: '取消',
 type: 'warning'
 });
 for (const id of selectedArticles.value) {
 await deleteArticle(id);
 }
 ElMessage.success('批量删除成功');
 selectedArticles.value = [];
 fetchArticles();
 fetchStats();
 }
 catch (error: any) {
 if (error !== 'cancel') {
 ElMessage.error('批量删除失败');
 }
 }
};
const handleExportCSV = () => {
 if (articles.value.length === 0) {
 ElMessage.warning('暂无数据可导出');
 return;
 }
 const headers = ['id', 'title', 'categoryName', 'state', 'viewCount', 'likeCount', 'createTime'];
 const exportData = articles.value.map(item => ({
 id: item.id,
 title: item.title,
 categoryName: getCategoryName(item.categoryId),
 state: item.state,
 viewCount: item.viewCount || 0,
 likeCount: item.likeCount || 0,
 createTime: new Date(item.createTime).toLocaleString('zh-CN')
 }));
 exportToCSV(exportData, headers, '文章列表');
 ElMessage.success('导出成功');
};
const handleExportJSON = () => {
  if (articles.value.length === 0) {
    ElMessage.warning('暂无数据可导出');
    return;
  }
  const exportData = articles.value.map(item => ({
    id: item.id,
    title: item.title,
    categoryName: getCategoryName(item.categoryId),
    state: item.state,
    viewCount: item.viewCount || 0,
    likeCount: item.likeCount || 0,
    createTime: item.createTime
  }));
  exportToJSON(exportData, '文章列表');
  ElMessage.success('导出成功');
};

const fetchStats = async () => {
  try {
    statsLoading.value = true;
    const res: any = await getArticleStats();
    if (res.code === 0 && res.data) {
      articleStats.value = {
        published: res.data.publishedCount || 0,
        draft: res.data.draftCount || 0,
        totalViews: res.data.totalViews || 0,
        totalLikes: res.data.totalLikes || 0
      };
    }
  } catch (error) {
    console.error('获取文章统计失败:', error);
  } finally {
    statsLoading.value = false;
  }
};

onMounted(() => {
 fetchCategories();
 fetchArticles();
 fetchStats();
});
</script>

<template>
  <div class="article-manage-container" v-loading="loading" element-loading-background="rgba(8,11,20,0.8)">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">文章管理</h2>
        <p class="page-subtitle">管理和发布您的所有文章</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-card-bg" style="background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(99,102,241,0.03));"></div>
        <div class="stat-card-inner">
          <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #6366f1, #818cf8); box-shadow: 0 8px 24px rgba(99,102,241,0.3);">
            <Document />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ pagination.total }}</div>
            <div class="stat-label">文章总数</div>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-bg" style="background: linear-gradient(135deg, rgba(16,185,129,0.15), rgba(16,185,129,0.03));"></div>
        <div class="stat-card-inner">
          <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #10b981, #34d399); box-shadow: 0 8px 24px rgba(16,185,129,0.3);">
            <CircleCheck />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ articleStats.published }}</div>
            <div class="stat-label">已发布</div>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-bg" style="background: linear-gradient(135deg, rgba(245,158,11,0.15), rgba(245,158,11,0.03));"></div>
        <div class="stat-card-inner">
          <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #f59e0b, #fbbf24); box-shadow: 0 8px 24px rgba(245,158,11,0.3);">
            <View />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ formatNumber(articleStats.totalViews) }}</div>
            <div class="stat-label">总阅读</div>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-bg" style="background: linear-gradient(135deg, rgba(168,85,247,0.15), rgba(168,85,247,0.03));"></div>
        <div class="stat-card-inner">
          <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #a855f7, #c084fc); box-shadow: 0 8px 24px rgba(168,85,247,0.3);">
            <Star />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ formatNumber(articleStats.totalLikes) }}</div>
            <div class="stat-label">总点赞</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选与操作栏 -->
    <div class="toolbar">
      <div class="toolbar-filters">
        <div class="search-input-wrap">
          <svg class="search-input-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model="searchForm.keyword"
            class="search-input"
            type="text"
            placeholder="搜索文章标题..."
            @keyup.enter="handleSearch"
          />
          <button v-if="searchForm.keyword" class="search-input-clear" @click="searchForm.keyword = ''; handleSearch()">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-label">分类</span>
          <div class="filter-chips">
            <span
              class="chip"
              :class="{ active: searchForm.categoryId === null }"
              @click="searchForm.categoryId = null; handleSearch()"
            >全部</span>
            <span
              v-for="cat in categories"
              :key="cat.id"
              class="chip"
              :class="{ active: searchForm.categoryId === cat.id }"
              @click="searchForm.categoryId = cat.id; handleSearch()"
            >{{ cat.name }}</span>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">状态</span>
          <div class="filter-chips">
            <span
              class="chip"
              :class="{ active: searchForm.state === '' }"
              @click="searchForm.state = ''; handleSearch()"
            >全部</span>
            <span
              class="chip"
              :class="{ active: searchForm.state === '已发布' }"
              @click="searchForm.state = '已发布'; handleSearch()"
            >已发布</span>
            <span
              class="chip"
              :class="{ active: searchForm.state === '草稿' }"
              @click="searchForm.state = '草稿'; handleSearch()"
            >草稿</span>
          </div>
        </div>
        <button class="toolbar-reset" @click="handleReset" title="重置筛选">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
          <span>重置</span>
        </button>
      </div>
      <div class="toolbar-actions">
        <div v-if="selectedArticles.length > 0" class="batch-bar">
          <span class="batch-count">已选 <em>{{ selectedArticles.length }}</em> 篇</span>
          <div class="batch-divider" />
          <button class="batch-btn publish" @click="handleBatchOperation('已发布')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <span>发布</span>
          </button>
          <button class="batch-btn draft" @click="handleBatchOperation('草稿')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            <span>草稿</span>
          </button>
          <button class="batch-btn delete" @click="handleBatchDelete">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            <span>删除</span>
          </button>
        </div>
        <div class="actions-group">
          <el-dropdown trigger="click">
            <button class="action-btn export-btn">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              <span>导出</span>
              <svg class="chevron" width="10" height="10" viewBox="0 0 16 16" fill="none"><path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleExportCSV">导出 CSV</el-dropdown-item>
                <el-dropdown-item @click="handleExportJSON">导出 JSON</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="panel table-panel">
      <div class="panel-header">
        <div class="panel-title">
          <Document class="panel-title-icon" />
          <span>文章列表</span>
        </div>
        <span class="panel-badge">共 {{ pagination.total }} 篇</span>
      </div>
      <div class="table-wrapper">
        <el-table
          :data="articles"
          @selection-change="handleSelectionChange"
          style="width: 100%"
          :stripe="false"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="文章ID" width="80" align="center" />
          <el-table-column prop="title" label="文章标题" min-width="260">
            <template #default="{ row }">
              <div class="title-cell">
                <span class="article-title">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="110" align="center">
            <template #default="{ row }">
              <span class="category-tag">{{ getCategoryName(row.categoryId) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="state" label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="state-badge" :class="row.state === '已发布' ? 'published' : 'draft'">
                {{ row.state }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="阅读" width="80" align="center">
            <template #default="{ row }">
              <span class="meta-text">{{ formatNumber(row.viewCount || 0) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="80" align="center">
            <template #default="{ row }">
              <span class="meta-text">{{ formatNumber(row.likeCount || 0) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatDate(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <button
                  v-if="row.status === 1"
                  class="act-btn act-online"
                  @click="handleToggleStatus(row.id, 0)"
                  title="上架"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
                  <span>上架</span>
                </button>
                <button
                  v-if="row.status === 0"
                  class="act-btn act-offline"
                  @click="handleToggleStatus(row.id, 1)"
                  title="下架"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                  <span>下架</span>
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
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
    
    <!-- 文章编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px" class="article-form">
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" class="title-input" />
        </el-form-item>
        
        <el-form-item label="文章分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择文章分类" class="category-select">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="封面图片">
          <el-upload
            class="cover-upload"
            :action="'/api/upload'"
            :headers="uploadHeaders"
            list-type="picture-card"
            :file-list="coverImage"
            :limit="1"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="handleBeforeUpload"
            :on-remove="handleRemoveCover"
            accept="image/*"
          >
            <el-icon v-if="!coverImage.length" size="32" class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        
        <el-form-item label="文章标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        
        <el-form-item label="文章状态">
          <el-radio-group v-model="form.state">
            <el-radio label="已发布" />
            <el-radio label="草稿" />
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="文章内容" prop="content" class="editor-form-item">
          <div class="editor-wrapper">
            <Toolbar
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              v-model="form.content"
              :defaultConfig="editorConfig"
              mode="default"
              @onCreated="handleCreated"
            />
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上下架确认弹窗 -->
    <Teleport to="body">
      <Transition name="st-modal">
        <div v-if="statusDialogVisible" class="st-overlay" @click.self="statusDialogVisible = false">
          <div class="st-dialog" :class="statusDialogTarget.actionText === '上架' ? 'online' : 'offline'">
            <div class="st-glow"></div>
            <div class="st-icon-wrap">
              <div class="st-icon-ring">
                <svg
                  v-if="statusDialogTarget.actionText === '上架'"
                  width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                >
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="16 12 12 8 8 12"/>
                  <line x1="12" y1="16" x2="12" y2="8"/>
                </svg>
                <svg
                  v-else
                  width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                >
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="8 12 12 16 16 12"/>
                  <line x1="12" y1="8" x2="12" y2="16"/>
                </svg>
              </div>
            </div>
            <div class="st-content">
              <h3 class="st-title">确认{{ statusDialogTarget.actionText }}</h3>
              <p class="st-desc">
                <template v-if="statusDialogTarget.actionText === '上架'">
                  上架后文章将恢复公开显示，所有用户都可以正常浏览
                </template>
                <template v-else>
                  下架后文章将不对用户可见，但不会删除文章内容
                </template>
              </p>
              <div class="st-info-bar">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="12" x2="12" y2="16"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                <span>此操作可以随时撤销</span>
              </div>
            </div>
            <div class="st-actions">
              <button class="st-btn st-btn-cancel" @click="statusDialogVisible = false" :disabled="statusDialogLoading">取消</button>
              <button
                class="st-btn st-btn-confirm"
                :class="statusDialogTarget.actionText === '上架' ? 'online' : 'offline'"
                :disabled="statusDialogLoading"
                @click="confirmToggleStatus"
              >
                <span v-if="!statusDialogLoading">确认{{ statusDialogTarget.actionText }}</span>
                <span v-else class="st-loading">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="st-spin"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                  处理中...
                </span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.article-manage-container {
  padding: 28px 32px;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.header-left { display: flex; flex-direction: column; gap: 6px; }
.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.5px;
}
.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  position: relative;
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  transition: all 0.3s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(99,102,241,0.15);
  box-shadow: 0 12px 32px rgba(0,0,0,0.4);
}
.stat-card-bg {
  position: absolute;
  inset: 0;
  opacity: 0.4;
  pointer-events: none;
}
.stat-card-inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 18px;
}
.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
  color: #f1f5f9;
  line-height: 1.1;
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-top: 2px;
}

/* 筛选工具栏 */
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 18px 22px;
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
}
.toolbar-filters {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  flex: 1;
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.search-input-icon {
  position: absolute;
  left: 12px;
  color: #64748b;
  pointer-events: none;
}
.search-input {
  width: 200px;
  height: 34px;
  padding: 0 32px 0 36px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  background: rgba(255,255,255,0.03);
  color: #e2e8f0;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}
.search-input::placeholder { color: #64748b; }
.search-input:focus {
  border-color: rgba(99,102,241,0.3);
  background: rgba(255,255,255,0.05);
}
.search-input-clear {
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
.search-input-clear:hover { color: #e2e8f0; background: rgba(255,255,255,0.08); }
.filter-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.filter-chips {
  display: flex;
  align-items: center;
  gap: 4px;
}
.chip {
  padding: 5px 13px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  background: rgba(255,255,255,0.03);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  user-select: none;
}
.chip:hover {
  color: #cbd5e1;
  background: rgba(255,255,255,0.06);
}
.chip.active {
  color: #fff;
  background: rgba(99,102,241,0.18);
  border-color: rgba(99,102,241,0.25);
}
.toolbar-reset {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 11px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}
.toolbar-reset:hover {
  color: #94a3b8;
  border-color: rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.03);
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px;
  border-radius: 10px;
  background: rgba(99,102,241,0.08);
  border: 1px solid rgba(99,102,241,0.12);
  animation: batchIn 0.25s ease;
}
@keyframes batchIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
.batch-count {
  font-size: 13px;
  color: #94a3b8;
  white-space: nowrap;
}
.batch-count em {
  font-style: normal;
  color: #818cf8;
  font-weight: 700;
}
.batch-divider {
  width: 1px;
  height: 18px;
  background: rgba(99,102,241,0.15);
}
.batch-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
  color: #94a3b8;
}
.batch-btn:hover { color: #e2e8f0; background: rgba(255,255,255,0.05); }
.batch-btn.publish:hover { color: #34d399; background: rgba(16,185,129,0.1); }
.batch-btn.draft:hover { color: #fbbf24; background: rgba(245,158,11,0.1); }
.batch-btn.delete:hover { color: #f87171; background: rgba(239,68,68,0.1); }

/* 操作按钮组 */
.actions-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
}
.export-btn {
  background: rgba(255,255,255,0.04);
  color: #94a3b8;
  border: 1px solid rgba(255,255,255,0.06);
}
.export-btn:hover {
  background: rgba(255,255,255,0.07);
  color: #cbd5e1;
  border-color: rgba(255,255,255,0.1);
}
.export-btn .chevron {
  margin-left: -1px;
  transition: transform 0.2s;
}
.export-btn:hover .chevron {
  transform: translateY(1px);
}
.add-btn-primary {
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #fff;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
}
.add-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99,102,241,0.45);
}

/* 通用面板 */
.panel {
  border-radius: 14px;
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  margin-bottom: 16px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
}
.panel-title-icon { font-size: 17px; color: #818cf8; }
.panel-badge {
  font-size: 12px;
  color: #94a3b8;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(255,255,255,0.03);
}

/* 表格面板 */
.table-wrapper { padding: 0 20px; }
.article-title {
  font-weight: 500;
  color: #cbd5e1;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.category-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(99,102,241,0.1);
  color: #818cf8;
}
.state-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.state-badge.published {
  background: rgba(16,185,129,0.12);
  color: #34d399;
}
.state-badge.draft {
  background: rgba(148,163,184,0.12);
  color: #94a3b8;
}
.meta-text {
  color: #94a3b8;
  font-weight: 500;
}
.time-text {
  color: #64748b;
  font-size: 13px;
}
.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

/* 自定义操作按钮 */
.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 11px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
  position: relative;
  overflow: hidden;
}
.act-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.act-btn:hover::before {
  opacity: 1;
}
.act-btn:active {
  transform: scale(0.95);
}
.act-online {
  background: linear-gradient(135deg, rgba(16,185,129,0.12), rgba(16,185,129,0.06));
  color: #34d399;
  border: 1px solid rgba(16,185,129,0.18);
}
.act-online:hover {
  background: linear-gradient(135deg, rgba(16,185,129,0.22), rgba(16,185,129,0.1));
  border-color: rgba(16,185,129,0.35);
  box-shadow: 0 2px 12px rgba(16,185,129,0.15);
  color: #6ee7b7;
}
.act-offline {
  background: linear-gradient(135deg, rgba(245,158,11,0.12), rgba(245,158,11,0.06));
  color: #fbbf24;
  border: 1px solid rgba(245,158,11,0.18);
}
.act-offline:hover {
  background: linear-gradient(135deg, rgba(245,158,11,0.22), rgba(245,158,11,0.1));
  border-color: rgba(245,158,11,0.35);
  box-shadow: 0 2px 12px rgba(245,158,11,0.15);
  color: #fcd34d;
}
.act-edit {
  background: rgba(99,102,241,0.08);
  color: #818cf8;
  border: 1px solid rgba(99,102,241,0.12);
  padding: 5px 10px;
}
.act-edit:hover {
  background: rgba(99,102,241,0.18);
  border-color: rgba(99,102,241,0.25);
  color: #a5b4fc;
}
.act-delete {
  background: rgba(239,68,68,0.08);
  color: #f87171;
  border: 1px solid rgba(239,68,68,0.12);
  padding: 5px 10px;
}
.act-delete:hover {
  background: rgba(239,68,68,0.18);
  border-color: rgba(239,68,68,0.25);
  color: #fca5a5;
}

/* 上下架确认弹窗 */
.st-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 4, 12, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.st-dialog {
  position: relative;
  width: 400px;
  background: linear-gradient(160deg, #111827, #0c0f1a);
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.06);
  padding: 0;
  overflow: hidden;
  box-shadow: 0 32px 64px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.03) inset;
  animation: st-enter 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes st-enter {
  from { opacity: 0; transform: scale(0.92) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.st-glow {
  position: absolute;
  top: -80px;
  left: 50%;
  transform: translateX(-50%);
  width: 240px;
  height: 240px;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0.08;
  transition: background 0.4s ease;
}
.st-dialog.online .st-glow {
  background: radial-gradient(circle, #10b981 0%, transparent 70%);
}
.st-dialog.offline .st-glow {
  background: radial-gradient(circle, #f59e0b 0%, transparent 70%);
}
.st-icon-wrap {
  display: flex;
  justify-content: center;
  padding: 32px 0 16px;
  position: relative;
  z-index: 1;
}
.st-icon-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.4s ease;
}
.st-dialog.online .st-icon-ring {
  background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05));
  border: 1.5px solid rgba(16,185,129,0.25);
  color: #34d399;
  box-shadow: 0 0 30px rgba(16,185,129,0.1);
}
.st-dialog.offline .st-icon-ring {
  background: linear-gradient(135deg, rgba(245,158,11,0.2), rgba(245,158,11,0.05));
  border: 1.5px solid rgba(245,158,11,0.25);
  color: #fbbf24;
  box-shadow: 0 0 30px rgba(245,158,11,0.1);
}
.st-content {
  padding: 0 32px 20px;
  text-align: center;
  position: relative;
  z-index: 1;
}
.st-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.3px;
}
.st-dialog.online .st-title { color: #d1fae5; }
.st-dialog.offline .st-title { color: #fef3c7; }
.st-desc {
  margin: 0 0 16px;
  font-size: 13.5px;
  line-height: 1.6;
  color: #94a3b8;
}
.st-info-bar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  color: #64748b;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.04);
}
.st-actions {
  display: flex;
  gap: 10px;
  padding: 16px 32px 28px;
  position: relative;
  z-index: 1;
}
.st-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
  font-family: inherit;
}
.st-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.st-btn-cancel {
  background: rgba(255,255,255,0.04);
  color: #94a3b8;
  border: 1px solid rgba(255,255,255,0.06);
}
.st-btn-cancel:hover:not(:disabled) {
  background: rgba(255,255,255,0.08);
  color: #cbd5e1;
}
.st-btn-confirm.online {
  background: linear-gradient(135deg, #059669, #10b981);
  color: #fff;
  box-shadow: 0 4px 16px rgba(16,185,129,0.3);
}
.st-btn-confirm.online:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(16,185,129,0.45);
  transform: translateY(-1px);
}
.st-btn-confirm.offline {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
  box-shadow: 0 4px 16px rgba(245,158,11,0.3);
}
.st-btn-confirm.offline:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(245,158,11,0.45);
  transform: translateY(-1px);
}
.st-loading {
  display: flex;
  align-items: center;
  gap: 6px;
}
.st-spin {
  animation: st-spin 1s linear infinite;
}
@keyframes st-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 弹窗过渡动画 */
.st-modal-enter-active { animation: st-enter 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.st-modal-leave-active { animation: st-enter 0.25s ease reverse; }
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
}

/* 表单样式 */
.article-form { padding: 8px; }
.title-input { width: 100%; }
.category-select { width: 200px; }
.cover-upload { width: 200px; }
.upload-icon { color: #64748b; }

.editor-wrapper {
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
  overflow: hidden;
  z-index: 1;
  width: 100%;
}
.editor-wrapper :deep(.w-e-toolbar) {
  border-bottom: 1px solid rgba(255,255,255,0.06);
  background: #111827;
}
.editor-wrapper :deep(.w-e-toolbar .w-e-bar-item button) {
  color: #94a3b8;
}
.editor-wrapper :deep(.w-e-toolbar .w-e-bar-item button:hover) {
  background: rgba(99,102,241,0.1);
  color: #e2e8f0;
}
.editor-wrapper :deep(.w-e-text-container) {
  min-height: 350px;
  background: #0d1117;
  color: #e2e8f0;
}
.editor-form-item { width: 100%; }
.editor-form-item :deep(.el-form-item__content) { width: 100%; }

/* Element Plus 覆盖样式 */
:deep(.el-form-item__label) { color: #94a3b8; font-weight: 500; }
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(99,102,241,0.04);
  --el-table-border-color: transparent;
  --el-table-text-color: #cbd5e1;
  --el-table-header-text-color: #94a3b8;
}
:deep(.el-table th.el-table__cell) {
  background: transparent;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  padding: 16px 0;
  font-weight: 600;
}
:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255,255,255,0.03);
  padding: 14px 0;
}
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #94a3b8;
  --el-pagination-button-bg-color: rgba(255,255,255,0.03);
  --el-pagination-hover-color: #818cf8;
  --el-pagination-button-color: #94a3b8;
}
:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: rgba(255,255,255,0.03) !important;
  border-radius: 8px;
}
:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: rgba(255,255,255,0.08) !important;
  color: #818cf8;
}
:deep(.el-pagination .btn-prev .el-icon),
:deep(.el-pagination .btn-next .el-icon) {
  color: inherit;
}
:deep(.el-pagination button.is-active) {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
}
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.03); box-shadow: none; border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; }
:deep(.el-input__wrapper:hover) { border-color: rgba(99,102,241,0.2); }
:deep(.el-input__wrapper.is-focus) { border-color: #6366f1; box-shadow: 0 0 0 1px rgba(99,102,241,0.2); }
:deep(.el-input__inner) { color: #e2e8f0; }
:deep(.el-select .el-input__wrapper) { background: rgba(255,255,255,0.03); }
:deep(.el-radio__label) { color: #cbd5e1; }
:deep(.el-dialog) {
  background: linear-gradient(145deg, #111827, #0f1320);
  border: 1px solid rgba(255,255,255,0.06);
}
:deep(.el-dialog__header) { border-bottom: 1px solid rgba(255,255,255,0.04); }
:deep(.el-dialog__title) { color: #e2e8f0; }
:deep(.el-dialog__footer) { border-top: 1px solid rgba(255,255,255,0.04); }

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-filters { flex-direction: column; align-items: flex-start; gap: 12px; }
  .toolbar-actions { justify-content: flex-end; }
  .batch-bar { flex-wrap: wrap; }
}
@media (max-width: 768px) {
  .article-manage-container { padding: 20px 16px; }
  .stats-row { grid-template-columns: 1fr; }
  .filter-group { flex-direction: column; align-items: flex-start; }
}
</style>
