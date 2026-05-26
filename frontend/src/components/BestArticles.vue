<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBestArticles } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { navigateTo } from '@/utils/navigate'

const userStore = useUserStore()
const items = ref<any[]>([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getBestArticles({ pageNum: 1, pageSize: 5 })
    items.value = res.data?.records || []
  } catch (error) {
    items.value = []
  } finally {
    loading.value = false
  }
}

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

onMounted(() => fetchData())
</script>

<template>
  <div class="widget">
    <div class="widget-header">
      <h3 class="widget-title">
        <el-icon><Medal /></el-icon>
        精选
      </h3>
    </div>

    <div v-if="loading" class="widget-loading">
      <div v-for="i in 4" :key="i" class="best-skeleton" />
    </div>

    <div v-else-if="items.length === 0" class="widget-empty">暂无数据</div>

    <div v-else class="widget-list">
      <div
        v-for="(item, index) in items"
        :key="item.id"
        class="best-item"
        :style="{ '--k': index }"
        @click="goToArticle(item.id)"
      >
        <div class="best-badge" :class="'badge-' + (index + 1)">{{ index + 1 }}</div>
        <div class="best-info">
          <h4 class="best-title">{{ item.title }}</h4>
          <span class="best-meta">{{ new Date(item.createTime).toLocaleDateString('zh-CN') }} · {{ item.viewCount || 0 }} 阅读</span>
        </div>
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
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f1f5f9;
  background: linear-gradient(180deg, #fafbfd, #fff);
}

.widget-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.widget-title :deep(.el-icon) {
  color: #f59e0b;
}

.widget-loading {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.best-skeleton {
  height: 40px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s infinite;
  border-radius: 6px;
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

.best-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: bestSlideIn 0.35s ease-out forwards;
  animation-delay: calc(var(--k, 0) * 0.06s);
}

@keyframes bestSlideIn {
  from {
    opacity: 0;
    transform: translateX(-6px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.best-item:hover {
  background: #fffbeb;
  transform: translateX(3px);
}

.best-badge {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f1f5f9;
  color: #64748b;
  margin-top: 2px;
  transition: all 0.25s ease;
}

.best-item:hover .best-badge {
  box-shadow: 0 2px 8px rgba(245,158,11,0.2);
}

.best-badge.badge-1 {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245,158,11,0.3);
}

.best-badge.badge-2 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}

.best-badge.badge-3 {
  background: linear-gradient(135deg, #fde68a, #fbbf24);
  color: #92400e;
}

.best-info {
  flex: 1;
  min-width: 0;
}

.best-title {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  margin: 0 0 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.25s ease;
}

.best-item:hover .best-title {
  color: #d97706;
}

.best-meta {
  font-size: 12px;
  color: #94a3b8;
}
</style>
