<script setup lang="ts">
import { ref, watch } from 'vue'
import { checkLike, checkCollect } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { navigateTo } from '@/utils/navigate'

interface Props {
  articles: any[]
  pagination: { pageNum: number; pageSize: number; total: number }
}

const props = defineProps<Props>()
const emit = defineEmits<{ pageChange: [page: number] }>()

const userStore = useUserStore()
const likedArticles = ref<Set<number>>(new Set())
const collectedArticles = ref<Set<number>>(new Set())

const formatCount = (n: number) => {
  if (!n) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toString()
}

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

watch(() => props.articles, async (articles) => {
  if (!articles?.length) return
  const ids = articles.map((a: any) => a.id)
  try {
    const [lr, cr] = await Promise.all([
      Promise.allSettled(ids.map((id: number) => checkLike(id))),
      Promise.allSettled(ids.map((id: number) => checkCollect(id)))
    ])
    const likes = new Set<number>()
    const collects = new Set<number>()
    lr.forEach((r: any, i: number) => {
      if (r.status === 'fulfilled' && (r.value?.data === true || r.value?.data?.data === true)) likes.add(ids[i])
    })
    cr.forEach((r: any, i: number) => {
      if (r.status === 'fulfilled' && (r.value?.data === true || r.value?.data?.data === true)) collects.add(ids[i])
    })
    likedArticles.value = likes
    collectedArticles.value = collects
  } catch {}
}, { immediate: true })

const handlePageChange = (page: number) => emit('pageChange', page)
</script>

<template>
  <div class="grid-wrap">
    <div class="article-grid">
      <div
        v-for="(article, index) in articles"
        :key="article.id"
        class="grid-card"
        :style="{ '--d': index }"
        @click="goToArticle(article.id)"
      >
        <!-- Image -->
        <div class="card-img" v-if="article.coverImg">
          <img :src="article.coverImg" :alt="article.title" loading="lazy" />
        </div>
        <div class="card-img card-img-placeholder" v-else>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
            <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/>
          </svg>
        </div>

        <!-- Body -->
        <div class="card-body">
          <div class="card-top">
            <span v-if="article.categoryName" class="card-cat">{{ article.categoryName }}</span>
            <span class="card-author">{{ article.username || '匿名' }}</span>
          </div>
          <h3 class="card-title">{{ article.title }}</h3>
          <p class="card-desc">{{ article.content }}</p>

          <div class="card-tags" v-if="article.tagNames?.length">
            <span v-for="tag in article.tagNames" :key="tag" class="card-tag">{{ tag }}</span>
          </div>

          <div class="card-stats">
            <span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="stat-icon"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              {{ formatCount(article.viewCount) }}
            </span>
            <span :class="{ liked: likedArticles.has(article.id) }">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="stat-icon"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
              {{ article.likeCount || 0 }}
            </span>
            <span :class="{ collected: collectedArticles.has(article.id) }">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="stat-icon"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              {{ article.collectCount || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="pagination.total > pagination.pageSize" class="grid-pagination">
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
  </div>
</template>

<style scoped>
.grid-wrap {
  animation: gridFadeIn 0.4s ease-out;
}

@keyframes gridFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.grid-card {
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(203,213,225,0.25);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: cardUp 0.45s ease-out forwards;
  animation-delay: calc(var(--d, 0) * 0.04s);
  display: flex;
  flex-direction: column;
}

@keyframes cardUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.grid-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 36px rgba(59,130,246,0.06), 0 2px 8px rgba(0,0,0,0.02);
  border-color: rgba(59,130,246,0.15);
  background: rgba(255,255,255,0.92);
}

.card-img {
  height: 180px;
  overflow: hidden;
  background: #f8fafc;
  flex-shrink: 0;
}

.card-img img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.grid-card:hover .card-img img {
  transform: scale(1.06);
}

.card-img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #cbd5e1;
}

.card-img-placeholder svg {
  width: 40px; height: 40px;
}

.card-body {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.card-cat {
  padding: 2px 10px;
  background: rgba(59,130,246,0.06);
  color: #3b82f6;
  border-radius: 100px;
  font-weight: 600;
}

.card-author {
  color: #94a3b8;
  font-weight: 500;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.25s ease;
}

.grid-card:hover .card-title {
  color: #3b82f6;
}

.card-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  flex: 1;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}

.card-tag {
  padding: 2px 7px;
  font-size: 11px;
  color: #60a5fa;
  background: rgba(59,130,246,0.04);
  border-radius: 4px;
}

.card-stats {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid rgba(226,232,240,0.4);
}

.card-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
  transition: color 0.25s ease;
}

.card-stats span.liked { color: #3b82f6; }
.card-stats span.collected { color: #10b981; }

.stat-icon {
  width: 14px; height: 14px;
}

.grid-pagination {
  display: flex;
  justify-content: center;
  padding: 32px 0 8px;
}

@media (max-width: 1200px) {
  .article-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .article-grid { grid-template-columns: 1fr; }
}
</style>
