<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile, toggleFollow } from '@/api/follow'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref<any>(null)
const loading = ref(true)
const followLoading = ref(false)

const pagination = ref({
  pageNum: 1,
  pageSize: 15,
  total: 0
})

const base64UrlDecode = (str: string) => {
  str = str.replace(/-/g, '+').replace(/_/g, '/')
  while (str.length % 4) str += '='
  return atob(str)
}

const getCurrentUserId = () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return null
    const payload = JSON.parse(base64UrlDecode(token.split('.')[1]))
    return payload.userId
  } catch {
    return null
  }
}

const isSelf = computed(() => {
  const uid = getCurrentUserId()
  return uid && profile.value?.id && uid === profile.value.id
})

const fetchProfile = async () => {
  loading.value = true
  try {
    const userId = Number(route.params.id)
    const res = await getUserProfile(userId, pagination.value.pageNum, pagination.value.pageSize)
    profile.value = res.data
    pagination.value.total = res.data.total || 0
  } catch {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const handleToggleFollow = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  if (isSelf.value) {
    ElMessage.info('无法关注自己喔，小傻瓜')
    return
  }
  followLoading.value = true
  try {
    const res = await toggleFollow(profile.value.id)
    profile.value.isFollowed = res.data.isFollowed
    profile.value.followCount = res.data.followCount
    profile.value.fansCount = res.data.fansCount
  } catch {
    // 错误信息已在请求拦截器中统一提示
  } finally {
    followLoading.value = false
  }
}

const goToArticle = (id: number) => {
  router.push(`/article/${id}`)
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page
  fetchProfile()
}

onMounted(fetchProfile)
</script>

<template>
  <div class="user-profile-page">
    <div v-if="loading" class="up-loading">
      <div class="up-loading-spinner"></div>
    </div>

    <template v-else-if="profile">
      <div class="up-layout">
        <aside class="up-sidebar">
          <button class="up-back" @click="router.back()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            <span>返回</span>
          </button>

          <div class="up-profile-card">
            <img :src="profile.userPic || '/default-avatar.png'" class="up-avatar" />
            <h1 class="up-nickname">{{ profile.nickname || profile.username }}</h1>
            <p class="up-username">@{{ profile.username }}</p>
            <p class="up-bio">{{ profile.email || '这个人很懒，什么都没留下' }}</p>

            <div class="up-stats">
              <div class="up-stat-item">
                <span class="up-stat-num">{{ profile.articleCount || 0 }}</span>
                <span class="up-stat-label">文章</span>
              </div>
              <div class="up-stat-item">
                <span class="up-stat-num">{{ profile.followCount || 0 }}</span>
                <span class="up-stat-label">关注</span>
              </div>
              <div class="up-stat-item">
                <span class="up-stat-num">{{ profile.fansCount || 0 }}</span>
                <span class="up-stat-label">粉丝</span>
              </div>
            </div>

            <button
              :class="['up-follow-btn', { followed: profile.isFollowed, 'up-self-btn': isSelf }]"
              :disabled="followLoading"
              @click="handleToggleFollow"
            >
              <template v-if="isSelf">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                关注自己
              </template>
              <template v-else-if="profile.isFollowed">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                已关注
              </template>
              <template v-else>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                关注
              </template>
            </button>
          </div>
        </aside>

        <main class="up-main">
          <div class="up-main-header">
            <h2 class="up-section-title">
              文章
              <span class="up-count-badge">{{ pagination.total }}</span>
            </h2>
          </div>

          <div v-if="profile.articles?.length" class="up-article-list">
            <div
              v-for="article in profile.articles"
              :key="article.id"
              class="up-article-card"
              @click="goToArticle(article.id)"
            >
              <div v-if="article.coverImg" class="up-ac-cover">
                <img :src="article.coverImg" :alt="article.title" />
              </div>
              <div class="up-ac-content">
                <h3 class="up-ac-title">{{ article.title }}</h3>
                <p class="up-ac-summary">{{ article.summary || article.content?.replace(/<[^>]*>/g, '').substring(0, 120) }}</p>
                <div class="up-ac-meta">
                  <span class="up-ac-date">{{ new Date(article.createTime).toLocaleDateString() }}</span>
                  <span class="up-meta-dot">·</span>
                  <span class="up-ac-reads">{{ article.viewCount || 0 }} 阅读</span>
                  <span class="up-meta-dot">·</span>
                  <span class="up-ac-likes">{{ article.likeCount || 0 }} 点赞</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="up-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <p>还没有发布文章</p>
          </div>

          <div v-if="pagination.total > pagination.pageSize" class="up-pagination">
            <button :disabled="pagination.pageNum <= 1" @click="handlePageChange(pagination.pageNum - 1)">上一页</button>
            <span class="up-page-info">{{ pagination.pageNum }} / {{ Math.ceil(pagination.total / pagination.pageSize) }}</span>
            <button :disabled="pagination.pageNum * pagination.pageSize >= pagination.total" @click="handlePageChange(pagination.pageNum + 1)">下一页</button>
          </div>
        </main>
      </div>
    </template>

    <div v-else class="up-empty up-empty-full">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <p>用户不存在</p>
    </div>
  </div>
</template>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  color: #e0e0e0;
}

.up-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
}

.up-loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255,255,255,.1);
  border-top-color: #a78bfa;
  border-radius: 50%;
  animation: spin .8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.up-layout {
  display: flex;
  min-height: 100vh;
}

.up-sidebar {
  width: 340px;
  flex-shrink: 0;
  padding: 32px 24px;
  border-right: 1px solid rgba(255,255,255,.06);
  background: rgba(255,255,255,.02);
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.up-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,.1);
  background: rgba(255,255,255,.05);
  color: rgba(255,255,255,.7);
  cursor: pointer;
  font-size: 13px;
  transition: all .3s;
  margin-bottom: 32px;
}

.up-back:hover {
  background: rgba(255,255,255,.1);
  color: #fff;
}

.up-profile-card {
  text-align: center;
}

.up-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(167,139,250,.4);
  margin-bottom: 20px;
  transition: transform .3s;
}

.up-avatar:hover {
  transform: scale(1.05);
}

.up-nickname {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px;
}

.up-username {
  font-size: 14px;
  color: rgba(167,139,250,.6);
  margin: 0 0 16px;
}

.up-bio {
  font-size: 14px;
  color: rgba(255,255,255,.5);
  margin: 0 0 28px;
  line-height: 1.6;
}

.up-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 28px;
  padding: 16px 0;
  border-top: 1px solid rgba(255,255,255,.06);
  border-bottom: 1px solid rgba(255,255,255,.06);
}

.up-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 8px;
  transition: background .3s;
}

.up-stat-item:hover {
  background: rgba(255,255,255,.04);
}

.up-stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.up-stat-label {
  font-size: 12px;
  color: rgba(255,255,255,.4);
}

.up-follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 32px;
  border-radius: 24px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all .3s;
  background: linear-gradient(135deg, #a78bfa, #7c3aed);
  color: #fff;
}

.up-follow-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(124,58,237,.4);
}

.up-follow-btn.followed {
  background: rgba(255,255,255,.08);
  border: 1px solid rgba(255,255,255,.15);
  color: #a78bfa;
}

.up-follow-btn.followed:hover:not(:disabled) {
  background: rgba(255,255,255,.12);
}

.up-follow-btn:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.up-self-btn {
  background: rgba(255,255,255,.05);
  border: 1px solid rgba(255,255,255,.1);
  color: rgba(255,255,255,.3);
  cursor: default;
}

.up-main {
  flex: 1;
  padding: 40px 48px 60px;
  max-width: 860px;
}

.up-main-header {
  margin-bottom: 24px;
}

.up-section-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.up-count-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 12px;
  background: rgba(167,139,250,.15);
  color: #a78bfa;
}

.up-article-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.up-article-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all .25s;
}

.up-article-card:hover {
  background: rgba(255,255,255,.04);
}

.up-article-card + .up-article-card {
  border-top: 1px solid rgba(255,255,255,.04);
}

.up-ac-cover {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  order: 1;
}

.up-ac-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.up-ac-content {
  flex: 1;
  min-width: 0;
}

.up-ac-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color .3s;
}

.up-article-card:hover .up-ac-title {
  color: #a78bfa;
}

.up-ac-summary {
  font-size: 13px;
  color: rgba(255,255,255,.45);
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

.up-ac-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255,255,255,.35);
}

.up-meta-dot {
  color: rgba(255,255,255,.15);
}

.up-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: rgba(255,255,255,.25);
  gap: 16px;
}

.up-empty-full {
  min-height: 80vh;
}

.up-empty p {
  font-size: 14px;
  margin: 0;
}

.up-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(255,255,255,.06);
}

.up-pagination button {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,.1);
  background: rgba(255,255,255,.05);
  color: #e0e0e0;
  cursor: pointer;
  font-size: 13px;
  transition: all .3s;
}

.up-pagination button:hover:not(:disabled) {
  background: rgba(255,255,255,.1);
  border-color: rgba(167,139,250,.3);
  color: #fff;
}

.up-pagination button:disabled {
  opacity: .3;
  cursor: not-allowed;
}

.up-page-info {
  font-size: 13px;
  color: rgba(255,255,255,.4);
}
</style>
