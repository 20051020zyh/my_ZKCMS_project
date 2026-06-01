<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getUserCenterInfo, updateUserInfo, updateAvatar, updatePassword } from '@/api/user'
import { getUserArticles, getUserCollects, trashArticle, getTrashList, recoverArticle, foreverDeleteArticle, moveCollectFolder } from '@/api/article'
import { getFollowList, getFansList } from '@/api/follow'
import { getFolderList, addFolder, updateFolder, deleteFolder } from '@/api/collectFolder'
import { navigateTo } from '@/utils/navigate'
import {
  EditPen, Lock, ArrowLeft, Camera,
  Postcard, Star, Comment, Document
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'articles' | 'collects' | 'trash' | 'edit'>('articles')
const centerInfo = ref<any>({})
const myArticles = ref<any[]>([])
const loadingArticles = ref(false)
const articlesPage = ref({ pageNum: 1, pageSize: 10, total: 0 })
const loadingInfo = ref(false)

const drawerVisible = ref(false)
const drawerTab = ref<'follow' | 'fans'>('follow')
const drawerList = ref<any[]>([])
const drawerLoading = ref(false)
const drawerPage = ref({ pageNum: 1, pageSize: 10, total: 0 })

const folders = ref<any[]>([])
const selectedFolderId = ref<number | undefined>(undefined)
const showFolderModal = ref(false)
const folderModalMode = ref<'create' | 'rename'>('create')
const folderModalName = ref('')
const editingFolderId = ref<number | null>(null)
const folderSaving = ref(false)
const showFolderDelete = ref(false)
const deletingFolderId = ref<number | null>(null)
const deletingFolderName = ref('')

const showMoveFolder = ref(false)
const moveArticleId = ref<number | null>(null)
const moveFolderId = ref<number | undefined>(undefined)
const movingFolder = ref(false)

const savingProfile = ref(false)
const savingPwd = ref(false)
const uploadingAvatar = ref(false)

const showDeleteModal = ref(false)
const deletingId = ref<number | null>(null)
const deletingTitle = ref('')
const deletingArticle = ref(false)

const showDraftModal = ref(false)
const draftArticleId = ref<number | null>(null)

const articleFilter = ref<'all' | 'published' | 'draft'>('all')

const myCollects = ref<any[]>([])
const loadingCollects = ref(false)
const collectsPage = ref({ pageNum: 1, pageSize: 10, total: 0 })
const folderOverrides = ref<Record<number, number | null>>({})

// 回收站
const trashList = ref<any[]>([])
const loadingTrash = ref(false)
const showTrashConfirm = ref(false)
const trashActionId = ref<number | null>(null)
const trashActionTitle = ref('')
const trashActionType = ref<'recover' | 'forever'>('recover')
const trashActionLoading = ref(false)

const editForm = ref({
  username: '',
  nickname: '',
  email: ''
})

const pwdForm = ref({
  now_pwd: '',
  new_pwd: '',
  re_pwd: ''
})

const fetchCenterInfo = async () => {
  loadingInfo.value = true
  try {
    const res: any = await getUserCenterInfo()
    centerInfo.value = res.data || {}
    editForm.value.username = centerInfo.value.username || ''
    editForm.value.nickname = centerInfo.value.nickname || ''
    editForm.value.email = centerInfo.value.email || ''
  } catch {
    ElMessage.error('获取用户信息失败')
  } finally {
    loadingInfo.value = false
  }
}

const openDrawer = (tab: 'follow' | 'fans') => {
  drawerTab.value = tab
  drawerPage.value = { pageNum: 1, pageSize: 10, total: 0 }
  drawerList.value = []
  drawerVisible.value = true
  if (tab === 'follow') {
    fetchFollowList()
  } else {
    fetchFansList()
  }
}

const switchDrawerTab = (tab: 'follow' | 'fans') => {
  drawerTab.value = tab
  drawerPage.value.pageNum = 1
  drawerList.value = []
  if (tab === 'follow') {
    fetchFollowList()
  } else {
    fetchFansList()
  }
}

const fetchFollowList = async () => {
  drawerLoading.value = true
  try {
    const res: any = await getFollowList(drawerPage.value.pageNum, drawerPage.value.pageSize)
    drawerList.value = res.data?.records || res.data || []
    drawerPage.value.total = res.data?.total || res.data?.length || 0
  } catch {
    ElMessage.error('获取关注列表失败')
  } finally {
    drawerLoading.value = false
  }
}

const fetchFansList = async () => {
  drawerLoading.value = true
  try {
    const res: any = await getFansList(drawerPage.value.pageNum, drawerPage.value.pageSize)
    drawerList.value = res.data?.records || res.data || []
    drawerPage.value.total = res.data?.total || res.data?.length || 0
  } catch {
    ElMessage.error('获取粉丝列表失败')
  } finally {
    drawerLoading.value = false
  }
}

const handleDrawerPageChange = (page: number) => {
  drawerPage.value.pageNum = page
  if (drawerTab.value === 'follow') {
    fetchFollowList()
  } else {
    fetchFansList()
  }
}

const fetchFolders = async () => {
  try {
    const res: any = await getFolderList()
    folders.value = res.data || []
  } catch {
    // ignore
  }
}

const handleSelectFolder = (folderId: number | undefined) => {
  selectedFolderId.value = folderId
  collectsPage.value.pageNum = 1
  fetchCollects()
}

const openCreateFolder = () => {
  folderModalMode.value = 'create'
  folderModalName.value = ''
  editingFolderId.value = null
  showFolderModal.value = true
}

const openRenameFolder = (e: Event, folder: any) => {
  e.stopPropagation()
  folderModalMode.value = 'rename'
  folderModalName.value = folder.name
  editingFolderId.value = folder.id
  showFolderModal.value = true
}

const confirmFolder = async () => {
  if (!folderModalName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  folderSaving.value = true
  try {
    if (folderModalMode.value === 'create') {
      await addFolder(folderModalName.value.trim())
      ElMessage.success('文件夹创建成功')
    } else if (editingFolderId.value) {
      await updateFolder(editingFolderId.value, folderModalName.value.trim())
      ElMessage.success('文件夹已重命名')
    }
    showFolderModal.value = false
    fetchFolders()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    folderSaving.value = false
  }
}

const openDeleteFolder = (e: Event, folder: any) => {
  e.stopPropagation()
  deletingFolderId.value = folder.id
  deletingFolderName.value = folder.name
  showFolderDelete.value = true
}

const confirmDeleteFolder = async () => {
  if (deletingFolderId.value === null) return
  try {
    await deleteFolder(deletingFolderId.value)
    ElMessage.success('文件夹已删除')
    showFolderDelete.value = false
    deletingFolderId.value = null
    if (selectedFolderId.value === deletingFolderId.value) {
      selectedFolderId.value = undefined
    }
    fetchFolders()
    fetchCollects()
  } catch {
    ElMessage.error('删除失败')
  }
}

const openMoveFolder = (articleId: number) => {
  moveArticleId.value = articleId
  moveFolderId.value = selectedFolderId.value
  showMoveFolder.value = true
}

const confirmMoveFolder = async () => {
  if (moveArticleId.value === null) return
  movingFolder.value = true
  try {
    await moveCollectFolder(moveArticleId.value, moveFolderId.value)
    ElMessage.success('移入成功')
    showMoveFolder.value = false
    delete folderOverrides.value[moveArticleId.value]
    fetchCollects()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    movingFolder.value = false
  }
}

const handleRemoveFolder = async (articleId: number) => {
  try {
    await moveCollectFolder(articleId)
    ElMessage.success('已移出分类')
    folderOverrides.value[articleId] = null
    const item = myCollects.value.find((i: any) => i.articleId === articleId)
    if (item) {
      item.folderId = null
    }
    if (selectedFolderId.value !== undefined) {
      myCollects.value = myCollects.value.filter((i: any) => i.articleId !== articleId)
      if (collectsPage.value.total > 0) collectsPage.value.total -= 1
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const fetchMyArticles = async () => {
  loadingArticles.value = true
  try {
    const res: any = await getUserArticles({
      pageNum: articlesPage.value.pageNum,
      pageSize: articlesPage.value.pageSize
    })
    myArticles.value = res.data?.records || []
    articlesPage.value.total = res.data?.total || 0
  } catch {
    ElMessage.error('获取文章列表失败')
  } finally {
    loadingArticles.value = false
  }
}

const handleArticlePageChange = (page: number) => {
  articlesPage.value.pageNum = page
  fetchMyArticles()
}

const filteredArticles = computed(() => {
  const visible = myArticles.value.filter((a: any) => a.isDelete !== 1)
  if (articleFilter.value === 'all') return visible
  const state = articleFilter.value === 'published' ? '已发布' : '草稿'
  return visible.filter((a: any) => a.state === state)
})

const fetchCollects = async () => {
  loadingCollects.value = true
  try {
    const params: any = {
      pageNum: collectsPage.value.pageNum,
      pageSize: collectsPage.value.pageSize,
      _t: Date.now()
    }
    if (selectedFolderId.value) {
      params.folderId = selectedFolderId.value
    }
    const res: any = await getUserCollects(params)
    const records = res.data?.records || []
    const filtered: any[] = []
    for (const record of records) {
      if (record.articleId in folderOverrides.value) {
        record.folderId = folderOverrides.value[record.articleId]
      }
      if (selectedFolderId.value !== undefined && record.folderId !== selectedFolderId.value) {
        continue
      }
      filtered.push(record)
    }
    myCollects.value = filtered
    const removedCount = (res.data?.records?.length || 0) - filtered.length
    collectsPage.value.total = Math.max(0, (res.data?.total || 0) - removedCount)
  } catch {
    ElMessage.error('获取收藏列表失败')
  } finally {
    loadingCollects.value = false
  }
}

const handleCollectPageChange = (page: number) => {
  collectsPage.value.pageNum = page
  fetchCollects()
}

const handleAvatarUpload = async () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: any) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过5MB')
      return
    }
    uploadingAvatar.value = true
    try {
      const res: any = await updateAvatar(file)
      console.log('updateAvatar response:', res)
      
      // 更新当前页面的头像
      const avatarUrl = res.data || ''
      centerInfo.value.user_pic = avatarUrl
      
      // 使用 store 的专门方法更新头像
      userStore.updateUserAvatar(avatarUrl)
      
      ElMessage.success('头像更新成功')
    } catch (err) {
      console.error('头像上传失败:', err)
      ElMessage.error('头像上传失败')
    } finally {
      uploadingAvatar.value = false
    }
  }
  input.click()
}

const handleSaveProfile = async () => {
  savingProfile.value = true
  try {
    const payload: any = {}
    if (editForm.value.username) payload.username = editForm.value.username
    if (editForm.value.nickname) payload.nickname = editForm.value.nickname
    if (editForm.value.email) payload.email = editForm.value.email
    await updateUserInfo(payload)
    centerInfo.value.username = editForm.value.username
    centerInfo.value.nickname = editForm.value.nickname
    centerInfo.value.email = editForm.value.email
    userStore.setUserInfo({
      ...userStore.userInfo,
      username: editForm.value.username,
      nickname: editForm.value.nickname,
      email: editForm.value.email
    })
    ElMessage.success('信息更新成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '信息更新失败')
  } finally {
    savingProfile.value = false
  }
}

const handleChangePwd = async () => {
  if (!pwdForm.value.now_pwd || !pwdForm.value.new_pwd || !pwdForm.value.re_pwd) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (pwdForm.value.new_pwd !== pwdForm.value.re_pwd) {
    ElMessage.warning('两次新密码输入不一致')
    return
  }
  savingPwd.value = true
  try {
    await updatePassword({
      old_pwd: pwdForm.value.now_pwd,
      new_pwd: pwdForm.value.new_pwd,
      re_pwd: pwdForm.value.re_pwd
    })
    ElMessage.success('密码修改成功')
    pwdForm.value = { now_pwd: '', new_pwd: '', re_pwd: '' }
  } catch (error: any) {
    ElMessage.error(error?.message || '密码修改失败')
  } finally {
    savingPwd.value = false
  }
}



const goBack = () => {
  router.push('/')
}

const goToArticle = (item: any) => {
  if (item.state === '草稿') {
    draftArticleId.value = item.id
    showDraftModal.value = true
    return
  }
  navigateTo(`/article/${item.id}`)
}

const goEditDraft = () => {
  if (draftArticleId.value) {
    navigateTo(`/article/create?id=${draftArticleId.value}`)
  }
  showDraftModal.value = false
  draftArticleId.value = null
}

const cancelDraft = () => {
  showDraftModal.value = false
  draftArticleId.value = null
}

const handleEditArticle = (e: Event, id: number) => {
  e.stopPropagation()
  navigateTo(`/article/create?id=${id}`)
}

const handleDeleteArticle = async (e: Event, id: number, title: string) => {
  e.stopPropagation()
  deletingId.value = id
  deletingTitle.value = title
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (deletingId.value === null) return
  deletingArticle.value = true
  try {
    await trashArticle(deletingId.value)
    ElMessage.success('文章已移入回收站')
    showDeleteModal.value = false
    deletingId.value = null
    articlesPage.value.pageNum = 1
    fetchMyArticles()
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    deletingArticle.value = false
  }
}

const cancelDelete = () => {
  showDeleteModal.value = false
  deletingId.value = null
  deletingTitle.value = ''
}

// ==================== 回收站 ====================

const fetchTrashList = async () => {
  loadingTrash.value = true
  try {
    const res: any = await getTrashList()
    trashList.value = res.data || []
  } catch {
    ElMessage.error('获取回收站列表失败')
  } finally {
    loadingTrash.value = false
  }
}

const openTrashConfirm = (type: 'recover' | 'forever', id: number, title: string) => {
  trashActionType.value = type
  trashActionId.value = id
  trashActionTitle.value = title
  showTrashConfirm.value = true
}

const confirmTrashAction = async () => {
  if (trashActionId.value === null) return
  trashActionLoading.value = true
  try {
    if (trashActionType.value === 'recover') {
      await recoverArticle(trashActionId.value)
      ElMessage.success('文章已恢复')
    } else {
      await foreverDeleteArticle(trashActionId.value)
      ElMessage.success('文章已永久删除')
    }
    showTrashConfirm.value = false
    trashActionId.value = null
    fetchTrashList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    trashActionLoading.value = false
  }
}

const cancelTrashAction = () => {
  showTrashConfirm.value = false
  trashActionId.value = null
  trashActionTitle.value = ''
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}

watch(activeTab, (tab) => {
  if (tab === 'articles') {
    articlesPage.value.pageNum = 1
    fetchMyArticles()
  } else if (tab === 'collects') {
    selectedFolderId.value = undefined
    fetchFolders()
    fetchCollects()
  } else if (tab === 'trash') {
    fetchTrashList()
  }
}, { immediate: true })

onMounted(() => {
  if (!userStore.checkLogin('请先登录')) return
  fetchCenterInfo()
})
</script>

<template>
  <div class="profile-page">
    <button class="btn-back" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
    </button>

    <div class="profile-layout">
      <aside class="profile-sidebar">
        <div class="sidebar-user">
          <div class="sidebar-avatar" @click="handleAvatarUpload" v-loading="uploadingAvatar">
            <div class="avatar-ring">
              <div class="avatar-inner">
                <el-avatar :size="100" :src="centerInfo.user_pic">
                  {{ centerInfo.nickname?.charAt(0) || 'U' }}
                </el-avatar>
              </div>
            </div>
            <div class="avatar-camera">
              <el-icon><Camera /></el-icon>
            </div>
          </div>

          <h2 class="sidebar-name">{{ centerInfo.username || '未设置用户名' }}</h2>
          <p class="sidebar-bio">{{ centerInfo.nickname || '这个人很懒，什么都没写' }}</p>
          <p v-if="centerInfo.email" class="sidebar-email">{{ centerInfo.email }}</p>

          <div class="profile-stats">
            <div class="pstat-item" @click="openDrawer('follow')">
              <div class="pstat-icon" style="color: #6366f1; background: rgba(99,102,241,0.1); box-shadow: 0 0 12px rgba(99,102,241,0.15)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
              </div>
              <span class="pstat-val">{{ centerInfo.followCount ?? 0 }}</span>
              <span class="pstat-label">关注</span>
            </div>
            <div class="pstat-item" @click="openDrawer('fans')">
              <div class="pstat-icon" style="color: #c4806a; background: rgba(196,128,106,0.1); box-shadow: 0 0 12px rgba(196,128,106,0.15)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
              </div>
              <span class="pstat-val">{{ centerInfo.fansCount ?? 0 }}</span>
              <span class="pstat-label">粉丝</span>
            </div>
            <div class="pstat-item">
              <div class="pstat-icon" style="color: #6a9b8a; background: rgba(106,155,138,0.1); box-shadow: 0 0 12px rgba(106,155,138,0.15)">
                <el-icon :size="14"><Postcard /></el-icon>
              </div>
              <span class="pstat-val">{{ centerInfo.fabuCount ?? 0 }}</span>
              <span class="pstat-label">已发布</span>
            </div>
            <div class="pstat-item">
              <div class="pstat-icon" style="color: #c8a45c; background: rgba(200,164,92,0.1); box-shadow: 0 0 12px rgba(200,164,92,0.15)">
                <el-icon :size="14"><Document /></el-icon>
              </div>
              <span class="pstat-val">{{ centerInfo.caogaoCount ?? 0 }}</span>
              <span class="pstat-label">草稿</span>
            </div>
            <div class="pstat-item">
              <div class="pstat-icon" style="color: #8b7dd8; background: rgba(139,125,216,0.1); box-shadow: 0 0 12px rgba(139,125,216,0.15)">
                <el-icon :size="14"><Star /></el-icon>
              </div>
              <span class="pstat-val">{{ centerInfo.collectCount ?? 0 }}</span>
              <span class="pstat-label">收藏</span>
            </div>
          </div>

          <div v-if="centerInfo.collectFolders?.length" class="sidebar-section">
            <h4 class="sidebar-section-title">收藏分类</h4>
            <div class="folder-list">
              <div
                v-for="folder in centerInfo.collectFolders"
                :key="folder.id"
                class="folder-item"
                @click="activeTab = 'collects'"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
                <span class="folder-name">{{ folder.name }}</span>
                <span class="folder-count">{{ folder.articleCount }}</span>
              </div>
            </div>
          </div>

            <button class="btn-edit-sidebar" @click="activeTab = 'edit'">
              <el-icon><EditPen /></el-icon>
              <span>编辑资料</span>
            </button>
          </div>
        </aside>

      <main class="profile-content">
        <div class="tab-nav">
          <button :class="['tab-btn', { active: activeTab === 'articles' }]" @click="activeTab = 'articles'">
            <el-icon><Postcard /></el-icon>
            <span>我的文章</span>
          </button>
          <button :class="['tab-btn', { active: activeTab === 'collects' }]" @click="activeTab = 'collects'">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </button>
          <button :class="['tab-btn', { active: activeTab === 'trash' }]" @click="activeTab = 'trash'">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            <span>回收站</span>
          </button>
          <button :class="['tab-btn', { active: activeTab === 'edit' }]" @click="activeTab = 'edit'">
            <el-icon><EditPen /></el-icon>
            <span>编辑资料</span>
          </button>
        </div>

        <div class="tab-content">
          <Transition name="fade-slide">
            <div v-if="activeTab === 'articles'" class="tab-panel">
              <div class="articles-filter">
                <button :class="['filter-chip', { active: articleFilter === 'all' }]" @click="articleFilter = 'all'">全部</button>
                <button :class="['filter-chip', { active: articleFilter === 'published' }]" @click="articleFilter = 'published'">已发布</button>
                <button :class="['filter-chip', { active: articleFilter === 'draft' }]" @click="articleFilter = 'draft'">草稿</button>
              </div>

              <div v-if="loadingArticles" class="empty-state">
                <el-icon class="empty-icon" :size="44"><Postcard /></el-icon>
                <p>加载中...</p>
              </div>

              <div v-else-if="!filteredArticles.length" class="empty-state">
                <el-icon class="empty-icon" :size="44"><Postcard /></el-icon>
                <p>{{ articleFilter === 'all' ? '还没有发布过文章' : articleFilter === 'published' ? '没有已发布的文章' : '没有草稿文章' }}</p>
              </div>

              <div v-else class="articles-list">
                <article
                  v-for="item in filteredArticles"
                  :key="item.id"
                  class="article-item"
                >
                  <div class="ai-left" @click="goToArticle(item)">
                    <h3 class="ai-title">{{ item.title }}</h3>
                    <div class="ai-meta">
                      <span :class="['ai-badge', item.state === '已发布' ? 'published' : 'draft']">{{ item.state }}</span>
                      <span class="ai-date">{{ formatDate(item.updateTime) }}</span>
                    </div>
                  </div>
                  <div class="ai-right" @click="goToArticle(item)">
                    <span v-if="item.viewCount" class="ai-stat">
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                      {{ item.viewCount }}
                    </span>
                    <span v-if="item.likeCount" class="ai-stat">
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
                      {{ item.likeCount }}
                    </span>
                  </div>
                  <div class="ai-actions">
                    <button class="ai-btn edit" @click="handleEditArticle($event, item.id)" title="编辑文章">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                      <span>编辑</span>
                    </button>
                    <button class="ai-btn trash" @click="handleDeleteArticle($event, item.id, item.title)" title="移入回收站">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                      <span>回收站</span>
                    </button>
                  </div>
                </article>
              </div>

              <div v-if="articlesPage.total > articlesPage.pageSize" class="collects-pagination">
                <button class="page-btn" :disabled="articlesPage.pageNum <= 1" @click="handleArticlePageChange(articlesPage.pageNum - 1)">上一页</button>
                <span class="page-info">{{ articlesPage.pageNum }} / {{ Math.ceil(articlesPage.total / articlesPage.pageSize) }}</span>
                <button class="page-btn" :disabled="articlesPage.pageNum >= Math.ceil(articlesPage.total / articlesPage.pageSize)" @click="handleArticlePageChange(articlesPage.pageNum + 1)">下一页</button>
              </div>
            </div>
          </Transition>

          <Transition name="fade-slide">
            <div v-if="activeTab === 'collects'" class="tab-panel">
              <div class="collect-toolbar">
                <div class="collect-folders">
                  <button
                    :class="['folder-chip', { active: selectedFolderId === undefined }]"
                    @click="handleSelectFolder(undefined)"
                  >全部</button>
                  <button
                    v-for="folder in folders"
                    :key="folder.id"
                    :class="['folder-chip', { active: selectedFolderId === folder.id }]"
                    @click="handleSelectFolder(folder.id)"
                  >
                    {{ folder.name }}
                    <div class="folder-chip-actions">
                      <span class="folder-chip-action" @click.stop="openRenameFolder($event, folder)" title="重命名">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                      </span>
                      <span class="folder-chip-action danger" @click.stop="openDeleteFolder($event, folder)" title="删除">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                      </span>
                    </div>
                  </button>
                  <button class="folder-chip folder-add" @click="openCreateFolder">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    新建分类
                  </button>
                </div>
              </div>

              <div v-if="loadingCollects" class="empty-state">
                <el-icon class="empty-icon" :size="44"><Star /></el-icon>
                <p>加载中...</p>
              </div>

              <div v-else-if="!myCollects.length" class="empty-state">
                <el-icon class="empty-icon" :size="44"><Star /></el-icon>
                <p>还没有收藏过文章</p>
              </div>

              <div v-else class="articles-list">
                <article
                  v-for="item in myCollects"
                  :key="item.articleId"
                  class="article-item"
                  @click="goToArticle({ id: item.articleId })"
                >
                  <div class="ai-left">
                    <h3 class="ai-title">{{ item.title }}</h3>
                    <div class="ai-meta">
                      <span class="ai-date">{{ formatDate(item.update_time) }}</span>
                    </div>
                  </div>
                  <div class="ai-actions">
                    <button v-if="selectedFolderId !== undefined || (item.articleId in folderOverrides ? folderOverrides[item.articleId] : item.folderId)" class="ai-btn del" @click.stop="handleRemoveFolder(item.articleId)" title="移出分类">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
                      <span>移出分类</span>
                    </button>
                    <button v-else class="ai-btn edit" @click.stop="openMoveFolder(item.articleId)" title="移入分类">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
                      <span>分类</span>
                    </button>
                  </div>
                </article>
              </div>

              <div v-if="collectsPage.total > collectsPage.pageSize" class="collects-pagination">
                <button class="page-btn" :disabled="collectsPage.pageNum <= 1" @click="handleCollectPageChange(collectsPage.pageNum - 1)">上一页</button>
                <span class="page-info">{{ collectsPage.pageNum }} / {{ Math.ceil(collectsPage.total / collectsPage.pageSize) }}</span>
                <button class="page-btn" :disabled="collectsPage.pageNum >= Math.ceil(collectsPage.total / collectsPage.pageSize)" @click="handleCollectPageChange(collectsPage.pageNum + 1)">下一页</button>
              </div>
            </div>
          </Transition>

          <Transition name="fade-slide">
            <div v-if="activeTab === 'trash'" class="tab-panel">
              <div class="trash-header">
                <div class="trash-header-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                </div>
                <span>回收站 <em class="trash-count">{{ trashList.length }}</em></span>
              </div>

              <div v-if="loadingTrash" class="empty-state">
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.3"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <p>加载中...</p>
              </div>

              <div v-else-if="!trashList.length" class="empty-state">
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.3"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <p>回收站暂无文章</p>
              </div>

              <div v-else class="articles-list">
                <div
                  v-for="item in trashList"
                  :key="item.id"
                  class="article-item trash-item"
                >
                  <div class="ai-left">
                    <h3 class="ai-title">{{ item.title }}</h3>
                    <div class="ai-meta">
                      <span class="ai-date">{{ formatDate(item.updateTime || item.createTime) }}</span>
                    </div>
                  </div>
                  <div class="trash-actions">
                    <button class="trash-btn recover" @click="openTrashConfirm('recover', item.id, item.title)" title="恢复文章">
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                      <span>恢复</span>
                    </button>
                    <button class="trash-btn forever" @click="openTrashConfirm('forever', item.id, item.title)" title="永久删除">
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                      <span>永久删除</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Transition>

          <Transition name="fade-slide">
            <div v-if="activeTab === 'edit'" class="tab-panel">
              <div class="edit-grid">
                <div class="edit-card">
                  <h3 class="edit-title"><el-icon :size="20"><EditPen /></el-icon>基本信息</h3>
                  <div class="edit-form">
                    <div class="field-group">
                      <label>用户名</label>
                      <input v-model="editForm.username" type="text" class="field-input" placeholder="设置用户名" maxlength="20" />
                      <span class="field-hint">修改后下次登录需使用新用户名</span>
                    </div>
                    <div class="field-group">
                      <label>个人简介</label>
                      <textarea v-model="editForm.nickname" class="field-textarea" placeholder="写一段个人介绍，让大家认识你..." rows="3"></textarea>
                      <span class="field-hint">简单介绍一下自己吧</span>
                    </div>
                    <div class="field-group">
                      <label>邮箱</label>
                      <input v-model="editForm.email" type="email" class="field-input" placeholder="输入你的邮箱" />
                    </div>
                    <button class="btn-save" :disabled="savingProfile" @click="handleSaveProfile">
                      {{ savingProfile ? '保存中...' : '保存修改' }}
                    </button>
                  </div>
                </div>

                <div class="edit-card">
                  <h3 class="edit-title"><el-icon :size="20"><Lock /></el-icon>修改密码</h3>
                  <div class="edit-form">
                    <div class="field-group">
                      <label>当前密码</label>
                      <input v-model="pwdForm.now_pwd" type="password" class="field-input" placeholder="输入当前密码" />
                    </div>
                    <div class="field-group">
                      <label>新密码</label>
                      <input v-model="pwdForm.new_pwd" type="password" class="field-input" placeholder="输入新密码" />
                    </div>
                    <div class="field-group">
                      <label>确认密码</label>
                      <input v-model="pwdForm.re_pwd" type="password" class="field-input" placeholder="再次输入新密码" />
                    </div>
                    <button class="btn-save secondary" :disabled="savingPwd" @click="handleChangePwd">
                      {{ savingPwd ? '修改中...' : '更新密码' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </main>
    </div>
  </div>

  <!-- 自定义删除确认弹窗 -->
  <Transition name="modal-fade">
    <div v-if="showDeleteModal" class="delete-overlay" @click.self="cancelDelete">
      <Transition name="modal-scale" appear>
        <div v-if="showDeleteModal" class="delete-modal">
          <div class="delete-modal-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <h3 class="delete-modal-title">确认移入回收站</h3>
          <p class="delete-modal-desc">
            确定要将文章 <strong>{{ deletingTitle }}</strong> 移入回收站吗？
          </p>
          <p class="delete-modal-hint">移入回收站后可以从回收站恢复</p>
          <div class="delete-modal-actions">
            <button class="dbtn dbtn-cancel" @click="cancelDelete" :disabled="deletingArticle">取消</button>
            <button class="dbtn dbtn-confirm" @click="confirmDelete" :disabled="deletingArticle">
              <template v-if="deletingArticle">
                <span class="dbtn-spinner"></span>
                <span>移入中...</span>
              </template>
              <template v-else>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                <span>移入回收站</span>
              </template>
            </button>
          </div>
          <button class="delete-modal-close" @click="cancelDelete">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 自定义草稿提示弹窗 -->
  <Transition name="draft-fade">
    <div v-if="showDraftModal" class="draft-overlay" @click.self="cancelDraft">

      <Transition name="draft-scale" appear>
        <div v-if="showDraftModal" class="draft-modal">
          <div class="draft-modal-glow" />

          <div class="draft-modal-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
            </svg>
          </div>

          <h3 class="draft-modal-title">草稿提示</h3>

          <p class="draft-modal-desc">
            该文章目前为 <span class="draft-badge">草稿</span> 状态
          </p>
          <p class="draft-modal-sub">是否跳转到编辑页面继续完善？</p>

          <div class="draft-modal-actions">
            <button class="dbtn dbtn-ghost" @click="cancelDraft">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              <span>稍后再说</span>
            </button>
            <button class="dbtn dbtn-edit" @click="goEditDraft">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
              <span>去编辑</span>
            </button>
          </div>

          <button class="draft-modal-close" @click="cancelDraft">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 回收站操作确认弹窗 -->
  <Transition name="modal-fade">
    <div v-if="showTrashConfirm" class="delete-overlay" @click.self="cancelTrashAction">
      <Transition name="modal-scale" appear>
        <div v-if="showTrashConfirm" class="delete-modal">
          <div class="delete-modal-icon" :class="trashActionType === 'forever' ? 'icon-danger' : 'icon-recover'">
            <template v-if="trashActionType === 'recover'">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/>
              </svg>
            </template>
            <template v-else>
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </template>
          </div>
          <h3 class="delete-modal-title">{{ trashActionType === 'recover' ? '确认恢复' : '确认永久删除' }}</h3>
          <p class="delete-modal-desc">
            <template v-if="trashActionType === 'recover'">
              确定要恢复文章 <strong>{{ trashActionTitle }}</strong> 吗？
            </template>
            <template v-else>
              确定要永久删除文章 <strong>{{ trashActionTitle }}</strong> 吗？
            </template>
          </p>
          <p v-if="trashActionType === 'forever'" class="delete-modal-hint">永久删除后不可恢复，请谨慎操作</p>
          <div class="delete-modal-actions">
            <button class="dbtn dbtn-cancel" @click="cancelTrashAction" :disabled="trashActionLoading">取消</button>
            <button
              :class="['dbtn', trashActionType === 'recover' ? 'dbtn-edit' : 'dbtn-confirm']"
              @click="confirmTrashAction"
              :disabled="trashActionLoading"
            >
              <template v-if="trashActionLoading">
                <span class="dbtn-spinner"></span>
                <span>{{ trashActionType === 'recover' ? '恢复中...' : '删除中...' }}</span>
              </template>
              <template v-else>
                <template v-if="trashActionType === 'recover'">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                  <span>恢复文章</span>
                </template>
                <template v-else>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                  <span>永久删除</span>
                </template>
              </template>
            </button>
          </div>
          <button class="delete-modal-close" @click="cancelTrashAction">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 关注/粉丝抽屉 -->
  <Transition name="drawer-fade">
    <div v-if="drawerVisible" class="drawer-overlay" @click.self="drawerVisible = false">
      <Transition name="drawer-slide" appear>
        <div v-if="drawerVisible" class="drawer-panel">
          <div class="drawer-header">
            <div class="drawer-tabs">
              <button :class="['drawer-tab', { active: drawerTab === 'follow' }]" @click="switchDrawerTab('follow')">关注</button>
              <button :class="['drawer-tab', { active: drawerTab === 'fans' }]" @click="switchDrawerTab('fans')">粉丝</button>
            </div>
            <button class="drawer-close" @click="drawerVisible = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="drawer-body">
            <div v-if="drawerLoading" class="drawer-loading">
              <span class="drawer-spinner"></span>
              <p>加载中...</p>
            </div>
            <div v-else-if="!drawerList.length" class="drawer-empty">
              <p>{{ drawerTab === 'follow' ? '还没有关注任何人' : '还没有粉丝' }}</p>
            </div>
            <div v-else class="drawer-list">
              <div
                v-for="item in drawerList"
                :key="item.id || item.userId"
                class="drawer-item"
              >
                <img :src="item.userPic || item.avatar || '/default-avatar.png'" class="drawer-item-avatar" />
                <div class="drawer-item-info">
                  <span class="drawer-item-name">{{ item.nickname || item.username || '未知用户' }}</span>
                  <span class="drawer-item-desc">{{ item.bio || item.email || '' }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="drawerPage.total > drawerPage.pageSize" class="drawer-footer">
            <button class="drawer-page-btn" :disabled="drawerPage.pageNum <= 1" @click="handleDrawerPageChange(drawerPage.pageNum - 1)">上一页</button>
            <span class="drawer-page-info">{{ drawerPage.pageNum }} / {{ Math.ceil(drawerPage.total / drawerPage.pageSize) }}</span>
            <button class="drawer-page-btn" :disabled="drawerPage.pageNum * drawerPage.pageSize >= drawerPage.total" @click="handleDrawerPageChange(drawerPage.pageNum + 1)">下一页</button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 新建/重命名文件夹弹窗 -->
  <Transition name="modal-fade">
    <div v-if="showFolderModal" class="delete-overlay" @click.self="showFolderModal = false">
      <Transition name="modal-scale" appear>
        <div v-if="showFolderModal" class="delete-modal" style="width: 340px">
          <div class="delete-modal-icon" style="color: #c8a45c; border-color: rgba(200,164,92,0.15)">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
          </div>
          <h3 class="delete-modal-title">{{ folderModalMode === 'create' ? '新建收藏分类' : '重命名文件夹' }}</h3>
          <div style="padding: 0 24px 20px">
            <input
              v-model="folderModalName"
              type="text"
              class="field-input"
              :placeholder="folderModalMode === 'create' ? '输入文件夹名称' : '输入新名称'"
              maxlength="20"
               @keyup.enter="confirmFolder"
             />
          </div>
          <div class="delete-modal-actions">
            <button class="dbtn dbtn-cancel" @click="showFolderModal = false" :disabled="folderSaving">取消</button>
            <button class="dbtn dbtn-edit" @click="confirmFolder" :disabled="folderSaving">
              {{ folderSaving ? '保存中...' : '确定' }}
            </button>
          </div>
          <button class="delete-modal-close" @click="showFolderModal = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 删除文件夹确认弹窗 -->
  <Transition name="modal-fade">
    <div v-if="showFolderDelete" class="delete-overlay" @click.self="showFolderDelete = false">
      <Transition name="modal-scale" appear>
        <div v-if="showFolderDelete" class="delete-modal">
          <div class="delete-modal-icon icon-danger">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.02" y2="17"/></svg>
          </div>
          <h3 class="delete-modal-title">确认删除文件夹</h3>
          <p class="delete-modal-desc">
            确定要删除 <strong>{{ deletingFolderName }}</strong> 吗？<br/>
            文件夹内的收藏文章将变为未分类
          </p>
          <div class="delete-modal-actions">
            <button class="dbtn dbtn-cancel" @click="showFolderDelete = false">取消</button>
            <button class="dbtn dbtn-confirm" @click="confirmDeleteFolder">删除</button>
          </div>
          <button class="delete-modal-close" @click="showFolderDelete = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- 移入分类弹窗 -->
  <Transition name="modal-fade">
    <div v-if="showMoveFolder" class="delete-overlay" @click.self="showMoveFolder = false">
      <Transition name="modal-scale" appear>
        <div v-if="showMoveFolder" class="delete-modal" style="width: 340px">
          <div class="delete-modal-icon" style="color: #c8a45c; border-color: rgba(200,164,92,0.15)">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
          </div>
          <h3 class="delete-modal-title">移入分类</h3>
          <div class="move-folder-list">
            <button
              :class="['move-folder-item', { active: moveFolderId === undefined }]"
              @click="moveFolderId = undefined"
            >未分类</button>
            <button
              v-for="folder in folders"
              :key="folder.id"
              :class="['move-folder-item', { active: moveFolderId === folder.id }]"
              @click="moveFolderId = folder.id"
            >{{ folder.name }}</button>
          </div>
          <div class="delete-modal-actions">
            <button class="dbtn dbtn-cancel" @click="showMoveFolder = false" :disabled="movingFolder">取消</button>
            <button class="dbtn dbtn-edit" @click="confirmMoveFolder" :disabled="movingFolder">
              {{ movingFolder ? '移入中...' : '确定' }}
            </button>
          </div>
          <button class="delete-modal-close" @click="showMoveFolder = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(175deg, #fdfaf3 0%, #f8f2e7 30%, #faf5ed 60%, #fdf9f2 100%);
  padding-left: 300px;
  position: relative;
}

.btn-back {
  position: fixed;
  top: 20px;
  left: 320px;
  z-index: 20;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(200,180,150,0.12);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(20px);
  color: #8a7d6e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
  box-shadow: 0 2px 12px rgba(160,130,100,0.05);
}

.btn-back:hover {
  background: rgba(255,255,255,0.95);
  color: #c8a45c;
  border-color: rgba(200,164,92,0.3);
  box-shadow: 0 6px 24px rgba(180,140,100,0.14);
  transform: translateX(-3px);
}

/* ========== Layout ========== */
.profile-layout {
  display: flex;
  min-height: 100vh;
}

/* ========== Sidebar ========== */
.profile-sidebar {
  width: 300px;
  flex-shrink: 0;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  background: rgba(255,253,249,0.92);
  border-right: 1px solid rgba(200,180,150,0.1);
  padding: 40px 24px 32px;
  display: flex;
  flex-direction: column;
}

.sidebar-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  gap: 4px;
}

.sidebar-avatar {
  display: inline-block;
  position: relative;
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.4,0,0.2,1);
  margin-bottom: 16px;
  z-index: 1;
}

.sidebar-avatar:hover {
  transform: scale(1.05);
}

.sidebar-avatar .avatar-ring {
  padding: 3px;
  border-radius: 50%;
  background: linear-gradient(135deg, #d4a574, #c8a45c, #b8956a);
  box-shadow: 0 4px 20px rgba(200,164,92,0.22);
}

.sidebar-avatar .avatar-inner {
  border-radius: 50%;
  background: #fdfaf5;
  padding: 2px;
  line-height: 0;
}

.sidebar-avatar .avatar-camera {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  border: 3px solid #fdfaf5;
  transition: all 0.3s;
  box-shadow: 0 3px 10px rgba(180,140,100,0.25);
}

.sidebar-avatar:hover .avatar-camera {
  transform: scale(1.15) rotate(5deg);
  box-shadow: 0 5px 18px rgba(180,140,100,0.4);
}

.sidebar-name {
  font-size: 22px;
  font-weight: 700;
  color: #3d3629;
  margin: 0 0 6px;
  letter-spacing: -0.4px;
  position: relative;
  z-index: 1;
}

.sidebar-bio {
  font-size: 14px;
  color: #8a7d6e;
  margin: 0 0 20px;
  line-height: 1.5;
  position: relative;
  z-index: 1;
}

/* ========== Sidebar Stats ========== */
.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 6px;
  padding: 16px 8px;
  margin: 12px 0;
  background: rgba(200,180,150,0.04);
  border-radius: 16px;
  position: relative;
  z-index: 1;
  width: 100%;
}

.pstat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  animation: statPop 0.5s cubic-bezier(0.34,1.56,0.64,1) both;
}

@keyframes statPop {
  from { opacity: 0; transform: scale(0.6); }
  to { opacity: 1; transform: scale(1); }
}

.pstat-item:hover {
  background: rgba(200,164,92,0.05);
  transform: translateY(-2px);
}

.pstat-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.pstat-item:hover .pstat-icon {
  transform: scale(1.12);
}

.pstat-val {
  font-size: 22px;
  font-weight: 800;
  color: #3d3629;
  line-height: 1;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}

.pstat-label {
  font-size: 11px;
  color: #a0927c;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.sidebar-email {
  font-size: 12px;
  color: #b8a894;
  margin: 0 0 16px;
  word-break: break-all;
  position: relative;
  z-index: 1;
}

.btn-edit-sidebar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 28px;
  margin-top: auto;
  border-radius: 14px;
  border: 1.5px solid rgba(200,164,92,0.22);
  background: rgba(200,164,92,0.06);
  color: #b8956a;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  font-family: inherit;
  position: relative;
  z-index: 1;
}

.btn-edit-sidebar:hover {
  background: rgba(200,164,92,0.12);
  border-color: rgba(200,164,92,0.38);
  color: #a0774a;
  box-shadow: 0 3px 16px rgba(200,164,92,0.16);
  transform: translateY(-1px);
}

/* ========== Sidebar Section ========== */
.sidebar-section {
  text-align: left;
  padding: 16px 0 8px;
  margin-bottom: 8px;
  border-top: 1px solid rgba(200,180,150,0.1);
  position: relative;
  z-index: 1;
}

.sidebar-section-title {
  font-size: 11px;
  font-weight: 700;
  color: #a0927c;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  margin: 0 0 12px;
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  color: #8a7d6e;
}

.folder-item:hover {
  background: rgba(200,164,92,0.06);
  color: #a0774a;
}

.folder-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-count {
  font-size: 12px;
  font-weight: 600;
  color: #c8bda8;
  background: rgba(200,180,150,0.08);
  padding: 1px 8px;
  border-radius: 8px;
}

/* ========== Drawer ========== */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(30,25,20,0.2);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.drawer-panel {
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 380px;
  max-width: 90vw;
  background: linear-gradient(175deg, #fdfaf3, #f8f2e7);
  box-shadow: -8px 0 40px rgba(30,25,20,0.1);
  display: flex;
  flex-direction: column;
  z-index: 10000;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(200,180,150,0.1);
  flex-shrink: 0;
}

.drawer-tabs {
  display: flex;
  gap: 4px;
  background: rgba(200,180,150,0.08);
  border-radius: 10px;
  padding: 3px;
}

.drawer-tab {
  padding: 8px 24px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #a0927c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
}

.drawer-tab.active {
  background: #fff;
  color: #3d3629;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.drawer-tab:hover:not(.active) {
  color: #8a7d6e;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: none;
  background: rgba(200,180,150,0.06);
  color: #a0927c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  flex-shrink: 0;
}

.drawer-close:hover {
  background: rgba(200,180,150,0.12);
  color: #8a7d6e;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.drawer-loading,
.drawer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #a0927c;
  gap: 12px;
}

.drawer-spinner {
  width: 28px;
  height: 28px;
  border: 2.5px solid rgba(200,180,150,0.12);
  border-top-color: #c8a45c;
  border-radius: 50%;
  animation: drSpin .7s linear infinite;
}

@keyframes drSpin { to { transform: rotate(360deg); } }

.drawer-empty p {
  font-size: 14px;
  margin: 0;
}

.drawer-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  transition: background 0.25s;
}

.drawer-item:hover {
  background: rgba(200,164,92,0.04);
}

.drawer-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(200,180,150,0.12);
  flex-shrink: 0;
}

.drawer-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-item-name {
  font-size: 14px;
  font-weight: 600;
  color: #3d3629;
}

.drawer-item-desc {
  font-size: 12px;
  color: #a0927c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid rgba(200,180,150,0.08);
  flex-shrink: 0;
}

.drawer-page-info {
  font-size: 13px;
  color: #a0927c;
  font-weight: 500;
}

.drawer-page-btn {
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid rgba(200,180,150,0.12);
  background: transparent;
  color: #8a7d6e;
  cursor: pointer;
  font-size: 12px;
  font-family: inherit;
  transition: all 0.25s;
}

.drawer-page-btn:hover:not(:disabled) {
  border-color: rgba(200,164,92,0.3);
  color: #a0774a;
}

.drawer-page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* drawer transitions */
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.3s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active {
  transition: transform 0.35s cubic-bezier(0.22,1,0.36,1);
}

.drawer-slide-leave-active {
  transition: transform 0.25s ease-in;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(100%);
}

/* ========== Content ========== */
.profile-content {
  flex: 1;
  min-width: 0;
  padding: 80px 48px 60px;
  max-width: 860px;
  animation: contentSlideIn 0.5s 0.1s cubic-bezier(0.22,1,0.36,1) both;
}

@keyframes contentSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ========== Tab Nav ========== */
.tab-nav {
  display: flex;
  gap: 10px;
  padding: 6px;
  background: rgba(255,253,249,0.88);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(200,180,150,0.08);
  border-radius: 18px;
  margin-bottom: 28px;
  box-shadow: 0 1px 2px rgba(180,150,110,0.02);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 13px 20px;
  border-radius: 14px;
  border: none;
  background: transparent;
  color: #a0927c;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  font-family: inherit;
  position: relative;
}

.tab-btn::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 20px;
  height: 2px;
  border-radius: 1px;
  background: #c8a45c;
  transition: transform 0.3s cubic-bezier(0.4,0,0.2,1);
}

.tab-btn:hover {
  color: #8a7d6e;
}

.tab-btn.active {
  background: linear-gradient(135deg, rgba(200,164,92,0.1), rgba(212,165,116,0.06));
  color: #a0774a;
  font-weight: 600;
  box-shadow:
    0 2px 6px rgba(180,140,100,0.06),
    inset 0 1px 0 rgba(200,164,92,0.04);
}

.tab-btn.active::after {
  transform: translateX(-50%) scaleX(1);
}

/* ========== Tab Content ========== */
.tab-content {
  min-height: 320px;
}

.tab-panel {
  width: 100%;
}

.fade-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.22,1,0.36,1);
}

.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(18px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ========== Empty State ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 90px 20px;
  color: #b8a894;
}

.empty-icon {
  margin-bottom: 18px;
  color: #d4c8b0;
  opacity: 0.6;
}

.empty-state p {
  font-size: 15px;
  font-weight: 500;
}

/* ========== Filter Chips ========== */
.articles-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.filter-chip {
  padding: 9px 20px;
  border-radius: 12px;
  border: 1.5px solid rgba(200,180,150,0.12);
  background: rgba(255,253,249,0.85);
  backdrop-filter: blur(8px);
  color: #a0927c;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  font-family: inherit;
}

.filter-chip:hover {
  border-color: rgba(200,164,92,0.25);
  color: #8a7d6e;
  background: rgba(200,164,92,0.04);
}

.filter-chip.active {
  background: linear-gradient(135deg, rgba(200,164,92,0.1), rgba(212,165,116,0.05));
  border-color: rgba(200,164,92,0.3);
  color: #a0774a;
  font-weight: 600;
  box-shadow: 0 1px 6px rgba(180,140,100,0.08);
}

/* ========== Articles List ========== */
.articles-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.article-item {
  background: rgba(255,253,249,0.88);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(200,180,150,0.08);
  border-radius: 16px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  box-shadow: 0 1px 2px rgba(180,150,110,0.02);
  animation: articleIn 0.45s cubic-bezier(0.22,1,0.36,1) both;
}

@keyframes articleIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-item:nth-child(1) { animation-delay: 0s; }
.article-item:nth-child(2) { animation-delay: 0.05s; }
.article-item:nth-child(3) { animation-delay: 0.1s; }
.article-item:nth-child(4) { animation-delay: 0.15s; }
.article-item:nth-child(5) { animation-delay: 0.2s; }
.article-item:nth-child(n+6) { animation-delay: 0.25s; }

.article-item:hover {
  border-color: rgba(200,164,92,0.2);
  box-shadow: 0 6px 24px rgba(180,150,110,0.1);
  transform: translateY(-2px);
}

.ai-left {
  min-width: 0;
  flex: 1;
}

.ai-title {
  font-size: 16px;
  font-weight: 600;
  color: #3d3629;
  margin: 0 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.2px;
  transition: color 0.25s;
}

.article-item:hover .ai-title {
  color: #a0774a;
}

.ai-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 6px;
  letter-spacing: 0.3px;
}

.ai-badge.published {
  background: rgba(106,155,138,0.1);
  color: #5a8a7a;
}

.ai-badge.draft {
  background: rgba(200,164,92,0.1);
  color: #b8956a;
}

.ai-date {
  font-size: 13px;
  color: #b8a894;
}

.ai-right {
  display: flex;
  gap: 20px;
  flex-shrink: 0;
  margin-left: 24px;
}

.ai-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #a0927c;
  font-weight: 500;
}

.ai-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 12px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.article-item:hover .ai-actions {
  opacity: 1;
}

.ai-btn {
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  white-space: nowrap;
}

.ai-btn.edit {
  color: #8a7a6a;
}

.ai-btn.edit:hover {
  background: rgba(200,164,92,0.12);
  color: #a0774a;
}

.ai-btn.del {
  color: #b8948a;
}

.ai-btn.del:hover {
  background: rgba(196,128,106,0.12);
  color: #c4705a;
}

.ai-btn.trash {
  color: #8a7d6b;
}

.ai-btn.trash:hover {
  background: rgba(200,164,92,0.12);
  color: #a0774a;
}

/* ========== Pagination ========== */
.collects-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 28px;
  padding: 16px;
}

/* ========== Collect Folders ========== */
.collect-toolbar {
  margin-bottom: 20px;
}

.collect-folders {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.folder-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 10px;
  border: 1.5px solid rgba(200,180,150,0.12);
  background: rgba(255,253,249,0.88);
  color: #8a7d6e;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  white-space: nowrap;
}

.folder-chip:hover {
  border-color: rgba(200,164,92,0.25);
  color: #a0774a;
}

.folder-chip.active {
  background: rgba(200,164,92,0.1);
  border-color: rgba(200,164,92,0.3);
  color: #a0774a;
  font-weight: 600;
}

.folder-chip.folder-add {
  border-style: dashed;
  color: #b8a894;
  font-weight: 400;
}

.folder-chip.folder-add:hover {
  border-color: rgba(200,164,92,0.3);
  color: #a0774a;
}

.folder-chip-actions {
  display: inline-flex;
  gap: 2px;
  margin-left: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.folder-chip:hover .folder-chip-actions {
  opacity: 1;
}

.folder-chip-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  color: #a0927c;
  transition: all 0.2s;
}

.folder-chip-action:hover {
  background: rgba(200,180,150,0.12);
  color: #8a7d6e;
}

.folder-chip-action.danger:hover {
  background: rgba(196,80,60,0.1);
  color: #c4503c;
}

/* ========== Move Folder Modal ========== */
.move-folder-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 24px 20px;
}

.move-folder-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1.5px solid rgba(200,180,150,0.1);
  background: #fefcf8;
  color: #8a7d6e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  text-align: left;
  width: 100%;
}

.move-folder-item:hover {
  border-color: rgba(200,164,92,0.25);
  background: rgba(200,164,92,0.04);
  color: #a0774a;
}

.move-folder-item.active {
  border-color: rgba(200,164,92,0.3);
  background: rgba(200,164,92,0.1);
  color: #a0774a;
  font-weight: 600;
}

.page-btn {
  padding: 10px 24px;
  border-radius: 12px;
  border: 1.5px solid rgba(200,180,150,0.12);
  background: rgba(255,253,249,0.88);
  backdrop-filter: blur(8px);
  color: #8a7d6e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.page-btn:hover:not(:disabled) {
  border-color: rgba(200,164,92,0.25);
  color: #a0774a;
  background: rgba(200,164,92,0.06);
  box-shadow: 0 3px 12px rgba(180,150,110,0.08);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #a0927c;
  font-weight: 500;
}

/* ========== Edit Forms ========== */
.edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.edit-card {
  background: rgba(255,253,249,0.88);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(200,180,150,0.08);
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 1px 2px rgba(180,150,110,0.02);
  animation: cardEntrance 0.5s 0.15s cubic-bezier(0.22,1,0.36,1) both;
}

.edit-card:last-child {
  animation-delay: 0.25s;
}

.edit-title {
  font-size: 17px;
  font-weight: 700;
  color: #3d3629;
  margin: 0 0 26px;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.2px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group label {
  font-size: 12px;
  font-weight: 600;
  color: #a0927c;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.field-input,
.field-textarea {
  width: 100%;
  padding: 13px 18px;
  border-radius: 14px;
  border: 1.5px solid rgba(200,180,150,0.15);
  background: #fefcf8;
  color: #3d3629;
  font-size: 15px;
  outline: none;
  transition: all 0.3s;
  font-family: inherit;
}

.field-textarea {
  resize: vertical;
  min-height: 70px;
}

.field-input:focus,
.field-textarea:focus {
  border-color: rgba(200,164,92,0.4);
  background: #fffdf9;
  box-shadow: 0 0 0 4px rgba(200,164,92,0.06);
}

.field-input.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #faf7f0;
}

.field-input::placeholder,
.field-textarea::placeholder {
  color: #c8bda8;
}

.field-hint {
  font-size: 12px;
  color: #c8bda8;
  font-weight: 500;
}

.btn-save {
  align-self: flex-start;
  padding: 12px 34px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4,0,0.2,1);
  font-family: inherit;
  letter-spacing: 0.3px;
  box-shadow: 0 3px 14px rgba(200,164,92,0.22);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(200,164,92,0.32);
  background: linear-gradient(135deg, #d4a574, #c8a45c);
}

.btn-save:active:not(:disabled) {
  transform: translateY(0);
}

.btn-save:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-save.secondary {
  background: #fefcf6;
  border: 1.5px solid rgba(200,164,92,0.18);
  color: #a0774a;
  box-shadow: 0 1px 3px rgba(180,150,110,0.04);
}

.btn-save.secondary:hover:not(:disabled) {
  background: #fffdf9;
  border-color: rgba(200,164,92,0.3);
  box-shadow: 0 5px 16px rgba(180,150,110,0.1);
  color: #8a6340;
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
  .profile-page { padding-left: 0; padding: 20px 16px 60px; }

  .profile-layout {
    flex-direction: column;
  }

  .profile-sidebar {
    width: 100%;
    position: static;
    height: auto;
    border-right: none;
    padding: 24px 16px;
  }

  .sidebar-user {
    gap: 0;
  }

  .edit-grid {
    grid-template-columns: 1fr;
  }

  .btn-back {
    top: 16px;
    left: 16px;
    width: 40px;
    height: 40px;
    border-radius: 12px;
  }
}

@media (max-width: 560px) {
  .profile-page {
    padding-left: 0;
    padding: 16px 12px 60px;
  }

  .btn-back {
    top: 16px;
    left: 14px;
    width: 38px;
    height: 38px;
    border-radius: 12px;
  }

  .profile-layout {
    flex-direction: column;
  }

  .profile-sidebar {
    width: 100%;
    position: static;
    height: auto;
    border-right: none;
    padding: 20px 12px;
  }

  .edit-grid {
    grid-template-columns: 1fr;
  }

  .article-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    padding: 18px 20px;
  }

  .ai-right {
    margin-left: 0;
  }
}

/* ── Draft Modal ── */
.draft-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30,25,20,0.3);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.draft-modal {
  position: relative;
  width: 380px;
  max-width: 90vw;
  background: linear-gradient(145deg, #fffcfa, #fefaf4);
  border-radius: 24px;
  padding: 44px 36px 28px;
  text-align: center;
  box-shadow:
    0 24px 80px rgba(30,25,20,0.14),
    0 0 0 1px rgba(200,164,92,0.08);
  overflow: hidden;
}

.draft-modal-glow {
  position: absolute;
  top: -60px;
  right: -60px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200,164,92,0.12), transparent 70%);
  pointer-events: none;
}

.draft-modal-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(200,164,92,0.12), rgba(212,165,116,0.06));
  color: #c8a45c;
  animation: draftIconIn 0.6s cubic-bezier(0.34,1.56,0.64,1) both;
  position: relative;
  box-shadow: 0 0 0 1px rgba(200,164,92,0.06);
}

.draft-modal-icon::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 30%, rgba(200,164,92,0.04), transparent 70%);
  pointer-events: none;
}

@keyframes draftIconIn {
  from { transform: scale(0) rotate(-15deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}

.draft-modal-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: #3d3629;
  letter-spacing: -0.4px;
  position: relative;
}

.draft-modal-desc {
  margin: 0 0 4px;
  font-size: 14px;
  color: #5a5040;
  line-height: 1.6;
  position: relative;
}

.draft-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 12px;
  border-radius: 6px;
  background: rgba(200,164,92,0.1);
  color: #b8956a;
  letter-spacing: 0.3px;
}

.draft-modal-sub {
  margin: 0 0 28px;
  font-size: 14px;
  color: #a0927c;
  line-height: 1.5;
  position: relative;
}

.draft-modal-actions {
  display: flex;
  gap: 10px;
  position: relative;
}

.dbtn-ghost {
  flex: 1;
  height: 44px;
  border-radius: 12px;
  border: 1.5px solid rgba(200,180,150,0.12);
  background: transparent;
  color: #8a7d6e;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
}

.dbtn-ghost:hover {
  background: rgba(200,180,150,0.06);
  border-color: rgba(200,180,150,0.25);
  color: #5a5040;
}

.dbtn-edit {
  flex: 1;
  height: 44px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.25s cubic-bezier(0.4,0,0.2,1);
  box-shadow: 0 3px 14px rgba(200,164,92,0.22);
  letter-spacing: 0.3px;
}

.dbtn-edit:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 28px rgba(200,164,92,0.32);
  background: linear-gradient(135deg, #d4a574, #c8a45c);
}

.dbtn-edit:active {
  transform: translateY(0);
}

.draft-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #c8bda8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.draft-modal-close:hover {
  background: rgba(200,180,150,0.08);
  color: #8a7d6e;
}

/* ── Draft Modal Transitions ── */
.draft-fade-enter-active { transition: all 0.3s ease; }
.draft-fade-leave-active { transition: all 0.2s ease; }
.draft-fade-enter-from { opacity: 0; }
.draft-fade-leave-to { opacity: 0; }

.draft-scale-enter-active { transition: all 0.35s cubic-bezier(0.34,1.56,0.64,1); }
.draft-scale-leave-active { transition: all 0.2s ease; }
.draft-scale-enter-from { opacity: 0; transform: scale(0.85) translateY(16px); }
.draft-scale-leave-to { opacity: 0; transform: scale(0.94) translateY(6px); }

/* ── Delete Modal ── */
.delete-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30,25,20,0.35);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.delete-modal {
  position: relative;
  width: 380px;
  max-width: 90vw;
  background: #fff;
  border-radius: 20px;
  padding: 40px 36px 28px;
  text-align: center;
  box-shadow: 0 24px 80px rgba(30,25,20,0.18), 0 0 0 1px rgba(255,255,255,0.06);
}

.delete-modal-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(220,80,60,0.08);
  color: #dc4a3a;
  animation: modalIconPulse 0.6s cubic-bezier(0.34,1.56,0.64,1) both;
}

@keyframes modalIconPulse {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.delete-modal-title {
  margin: 0 0 10px;
  font-size: 19px;
  font-weight: 700;
  color: #2d281f;
  letter-spacing: -0.3px;
}

.delete-modal-desc {
  margin: 0 0 4px;
  font-size: 14px;
  color: #5a5040;
  line-height: 1.6;
}

.delete-modal-desc strong {
  color: #3d3629;
  font-weight: 600;
}

.delete-modal-hint {
  margin: 0 0 28px;
  font-size: 13px;
  color: #b8948a;
}

.delete-modal-actions {
  display: flex;
  gap: 10px;
}

.dbtn {
  flex: 1;
  height: 44px;
  border-radius: 12px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.dbtn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dbtn-cancel {
  background: #f3f0ea;
  color: #5a5040;
}

.dbtn-cancel:hover:not(:disabled) {
  background: #eae4da;
}

.dbtn-confirm {
  background: #dc4a3a;
  color: #fff;
}

.dbtn-confirm:hover:not(:disabled) {
  background: #c83d2e;
}

.dbtn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: dbtnSpin 0.6s linear infinite;
}

@keyframes dbtnSpin {
  to { transform: rotate(360deg); }
}

.delete-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #b8a894;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.delete-modal-close:hover {
  background: #f3f0ea;
  color: #5a5040;
}

/* ── Modal Transitions ── */
.modal-fade-enter-active { transition: all 0.3s ease; }
.modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from { opacity: 0; }
.modal-fade-leave-to { opacity: 0; }

.modal-scale-enter-active { transition: all 0.3s cubic-bezier(0.34,1.56,0.64,1); }
.modal-scale-leave-active { transition: all 0.2s ease; }
.modal-scale-enter-from { opacity: 0; transform: scale(0.88) translateY(12px); }
.modal-scale-leave-to { opacity: 0; transform: scale(0.94) translateY(6px); }

/* ── Trash Tab ── */
.trash-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  font-size: 15px;
  font-weight: 700;
  color: #3d3629;
}

.trash-header-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(180,150,110,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a7d6b;
}

.trash-count {
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
  color: #b8956a;
  background: rgba(200,164,92,0.1);
  padding: 1px 9px;
  border-radius: 9px;
  margin-left: 4px;
}

.trash-item .ai-left {
  flex: 1;
}

.trash-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.trash-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border-radius: 10px;
  border: 1.5px solid rgba(200,180,150,0.12);
  background: rgba(255,255,255,0.7);
  color: #7a6e5e;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
}

.trash-btn.recover:hover {
  background: rgba(106,155,138,0.08);
  border-color: rgba(106,155,138,0.2);
  color: #5a8a7a;
}

.trash-btn.forever:hover {
  background: rgba(196,128,106,0.08);
  border-color: rgba(196,128,106,0.2);
  color: #c4705a;
}

.trash-btn svg {
  flex-shrink: 0;
}

.delete-modal-icon.icon-recover {
  color: #6a9b8a;
}

.delete-modal-icon.icon-danger {
  color: #c4806a;
}
</style>