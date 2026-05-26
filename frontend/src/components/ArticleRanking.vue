<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getArticleRank } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { navigateTo } from '@/utils/navigate'

const userStore = useUserStore()
const items = ref<any[]>([])
const loading = ref(false)
const activeTab = ref('view')

const tabs = [
  { label: '浏览量', value: 'view', icon: 'View', countKey: 'viewCount', suffix: '阅读' },
  { label: '收藏榜', value: 'collect', icon: 'Star', countKey: 'collectCount', suffix: '收藏' },
  { label: '点赞榜', value: 'like', icon: 'TrendCharts', countKey: 'likeCount', suffix: '赞' },
]

const fetchData = async (type: string = 'view') => {
  loading.value = true
  try {
    const res: any = await getArticleRank({ type, limit: 10 })
    items.value = res.data || []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

const handleTabChange = (type: string) => {
  activeTab.value = type
  fetchData(type)
}

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

const getRankClass = (index: number) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const activeTabConfig = () => tabs.find(t => t.value === activeTab.value)

const formatCount = (num: number) => {
  if (!num) return 0
  if (num >= 10000) return (num / 10000).toFixed(3).replace(/\.?0+$/, '') + 'w'
  if (num >= 1000) return (num / 1000).toFixed(3).replace(/\.?0+$/, '') + 'k'
  return num
}

onMounted(() => fetchData('view'))
</script>

<template>
  <div class="widget">
    <div class="widget-header">
      <h3 class="widget-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="title-icon">
          <polyline points="6 9 12 4 18 9" />
          <polyline points="6 15 12 20 18 15" />
        </svg>
        排行榜
      </h3>
    </div>

    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: activeTab === tab.value }"
        @click="handleTabChange(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="widget-loading">
      <div v-for="i in 5" :key="i" class="rank-skeleton" />
    </div>

    <div v-else-if="items.length === 0" class="widget-empty">暂无排行数据</div>

    <div v-else class="widget-list">
      <div
        v-for="(item, index) in items.slice(0, 8)"
        :key="item.id"
        class="rank-item"
        :style="{ '--j': index }"
        @click="goToArticle(item.id)"
      >
        <span class="rank-num" :class="getRankClass(index)">
          <template v-if="index === 0">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          </template>
          <template v-else>{{ index + 1 }}</template>
        </span>
        <span class="rank-title">{{ item.title }}</span>
        <span class="rank-count">
          {{ formatCount(activeTabConfig() ? item[activeTabConfig()!.countKey] : 0) }}
          <small>{{ activeTabConfig()?.suffix }}</small>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.widget {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.widget-header {
  padding: 18px 20px 0;
}

.widget-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: #6366f1;
}

.tab-bar {
  display: flex;
  gap: 4px;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.tab-btn {
  flex: 1;
  padding: 7px 0;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.tab-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.tab-btn.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(99,102,241,0.25);
  font-weight: 600;
}

.widget-loading {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-skeleton {
  height: 16px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.widget-empty {
  padding: 32px 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.widget-list {
  padding: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: rankSlideIn 0.35s ease-out forwards;
  animation-delay: calc(var(--j, 0) * 0.05s);
}

@keyframes rankSlideIn {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.rank-item:hover {
  background: #f8fafc;
  transform: translateX(3px);
}

.rank-num {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 8px;
  flex-shrink: 0;
  transition: all 0.25s ease;
}

.rank-num.rank-1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245,158,11,0.35);
}

.rank-num.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #b0bec5);
  color: #fff;
  box-shadow: 0 2px 6px rgba(148,163,184,0.25);
}

.rank-num.rank-3 {
  background: linear-gradient(135deg, #d4a574, #c4956a);
  color: #fff;
  box-shadow: 0 2px 6px rgba(180,140,100,0.25);
}

.rank-title {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.rank-count {
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
  white-space: nowrap;
  flex-shrink: 0;
}

.rank-count small {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
  margin-left: 2px;
}
</style>