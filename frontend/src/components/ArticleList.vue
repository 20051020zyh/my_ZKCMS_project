<script setup lang="ts">
import { checkLike, checkCollect } from '@/api/article'
import { ref, watch } from 'vue'
import { View, Collection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { navigateTo } from '@/utils/navigate'

interface Props {
  articles: any[]
  loading: boolean
  pagination: {
    pageNum: number
    pageSize: number
    total: number
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  pageChange: [page: number]
}>()

const userStore = useUserStore()
const likedArticles = ref<Set<number>>(new Set())
const collectedArticles = ref<Set<number>>(new Set())
const coverOrientations = ref<Record<number, 'portrait' | 'landscape'>>({})

const onImgLoad = (e: Event, id: number) => {
  const img = e.target as HTMLImageElement
  if (img.naturalWidth > img.naturalHeight) {
    coverOrientations.value[id] = 'landscape'
  } else {
    coverOrientations.value[id] = 'portrait'
  }
}

const formatViewCount = (count: number) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  return count.toString()
}

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

watch(
  () => props.articles,
  async (articles) => {
    if (!articles || articles.length === 0) return
    const ids = articles.map((a: any) => a.id)
    try {
      const [likeRes, collectRes] = await Promise.all([
        Promise.allSettled(ids.map((id: number) => checkLike(id))),
        Promise.allSettled(ids.map((id: number) => checkCollect(id)))
      ])
      const newLiked = new Set<number>()
      const newCollected = new Set<number>()
      likeRes.forEach((result: any, idx: number) => {
        if (result.status === 'fulfilled') {
          const data = result.value?.data ?? result.value
          if (data === true || data?.data === true) newLiked.add(ids[idx])
        }
      })
      collectRes.forEach((result: any, idx: number) => {
        if (result.status === 'fulfilled') {
          const data = result.value?.data ?? result.value
          if (data === true || data?.data === true) newCollected.add(ids[idx])
        }
      })
      likedArticles.value = newLiked
      collectedArticles.value = newCollected
    } catch (error) {
      console.error('加载点赞/收藏状态失败:', error)
    }
  },
  { immediate: true }
)

const handlePageChange = (page: number) => {
  emit('pageChange', page)
}
</script>

<template>
  <div class="article-feed">
    <div v-if="loading" class="feed-loading">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <div class="sk-left">
          <div class="sk-line sk-title" />
          <div class="sk-line sk-text" />
          <div class="sk-line sk-meta" />
        </div>
        <div class="sk-right" />
      </div>
    </div>

    <div v-else-if="articles.length === 0" class="feed-empty">
      <el-empty description="暂无文章" :image-size="80" />
    </div>

    <template v-else>
      <div
        v-for="(article, index) in articles"
        :key="article.id"
        class="feed-card"
        :style="{ '--i': index }"
        @click="goToArticle(article.id)"
      >
        <div class="card-body">
          <div class="card-meta-top">
            <span class="card-author">
              <el-avatar :size="24" :src="article.user_pic || ''">
                <template #default>
                  <svg viewBox="0 0 24 24" fill="currentColor" style="width:14px;height:14px;opacity:0.6"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v1.2c0 .66.54 1.2 1.2 1.2h16.8c.66 0 1.2-.54 1.2-1.2v-1.2c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                </template>
              </el-avatar>
              <span>{{ article.username || '匿名' }}</span>
            </span>
            <span v-if="article.categoryName" class="card-category">{{ article.categoryName }}</span>
          </div>

          <h3 class="card-title">{{ article.title }}</h3>
          <p class="card-desc">{{ article.content }}</p>

          <div class="card-footer">
            <div class="card-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ formatViewCount(article.viewCount || 0) }}
              </span>
              <span class="stat-item" :class="{ liked: likedArticles.has(article.id) }">
                <svg class="heart-icon" viewBox="0 0 24 24" :fill="likedArticles.has(article.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
                {{ article.likeCount || 0 }}
              </span>
              <span class="stat-item" :class="{ collected: collectedArticles.has(article.id) }">
                <el-icon><Collection /></el-icon>
                {{ article.collectCount || 0 }}
              </span>
            </div>
          </div>

          <div v-if="article.tagNames && article.tagNames.length" class="card-tags">
            <span v-for="tag in article.tagNames" :key="tag" class="card-tag">{{ tag }}</span>
          </div>
        </div>

        <div v-if="article.coverImg" class="card-thumb" :class="coverOrientations[article.id] || ''">
          <div class="thumb-inner">
            <img :src="article.coverImg" :alt="article.title" @load="onImgLoad($event, article.id)" />
          </div>
        </div>
      </div>

      <div v-if="pagination.total > pagination.pageSize" class="feed-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          background
          small
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.article-feed {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feed-card {
  display: flex;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e2e8f0;
  opacity: 0;
  animation: cardSlideUp 0.45s ease-out forwards;
  animation-delay: calc(var(--i, 0) * 0.06s);
}

@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.feed-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.07), 0 0 0 1px rgba(99,102,241,0.1);
  border-color: rgba(99,102,241,0.2);
}

.card-body {
  flex: 1;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.card-meta-top {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.card-author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-weight: 500;
}

.card-category {
  padding: 3px 10px;
  background: linear-gradient(135deg, #eef2ff, #ede9fe);
  color: #6366f1;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.feed-card:hover .card-category {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  transition: color 0.25s ease;
}

.feed-card:hover .card-title {
  color: #6366f1;
}

.card-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.card-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #94a3b8;
  transition: all 0.25s ease;
}

.heart-icon {
  width: 15px;
  height: 15px;
  display: block;
  transition: color 0.3s ease;
}

.liked {
  color: #6366f1;
}

.collected {
  color: #f59e0b;
}

.card-thumb {
  width: 240px;
  flex-shrink: 0;
  padding: 10px;
  box-sizing: border-box;
}

.card-thumb.landscape {
  height: 160px;
}

.card-thumb.portrait {
  height: 210px;
}

.card-thumb:not(.landscape):not(.portrait) {
  height: 185px;
}

.thumb-inner {
  width: 100%;
  height: 100%;
  border-radius: 24px;
  overflow: hidden;
  background: #fff;
}

.thumb-inner img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.4, 0, 0.2, 1);
}

.feed-card:hover .thumb-inner img {
  transform: scale(1.05);
}

.feed-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* Skeleton */
.feed-loading {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  display: flex;
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  gap: 24px;
  border: 1px solid #e2e8f0;
}

.sk-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-right {
  width: 240px;
  height: 185px;
  border-radius: 24px;
}

.sk-line {
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s infinite;
  border-radius: 4px;
}

.sk-title {
  height: 22px;
  width: 60%;
}

.sk-text {
  height: 16px;
  width: 90%;
}

.sk-meta {
  height: 14px;
  width: 40%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.feed-empty {
  background: #fff;
  border-radius: 14px;
  padding: 60px;
  border: 1px solid #e2e8f0;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.card-tag {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #818cf8;
  background: rgba(99,102,241,0.06);
  border-radius: 8px;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.feed-card:hover .card-tag {
  background: rgba(99,102,241,0.12);
  color: #6366f1;
}
</style>
