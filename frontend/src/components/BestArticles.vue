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
  try { const r: any = await getBestArticles({ pageNum: 1, pageSize: 5 }); items.value = r.data?.records || [] }
  catch { items.value = [] }
  finally { loading.value = false }
}

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

onMounted(() => fetchData())
</script>

<template>
  <div class="widget">
    <div class="w-head">
      <svg class="w-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="6"/><path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11"/></svg>
      <span>精选</span>
    </div>

    <div v-if="loading" class="w-load">
      <div v-for="i in 4" :key="i" class="w-sk" />
    </div>

    <div v-else-if="items.length === 0" class="w-empty">暂无</div>

    <div v-else class="w-list">
      <div v-for="(item, i) in items" :key="item.id" class="b-item" :style="{ '--p': i }" @click="goToArticle(item.id)">
        <span class="b-badge" :class="'b' + (i + 1)">{{ i + 1 }}</span>
        <div class="b-info">
          <span class="b-title">{{ item.title }}</span>
          <span class="b-meta">{{ new Date(item.createTime).toLocaleDateString('zh-CN') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.widget {
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(203,213,225,0.25);
  overflow: hidden;
  transition: all 0.3s ease;
}

.widget:hover {
  background: rgba(255,255,255,0.88);
  border-color: rgba(16,185,129,0.1);
}

.w-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px 12px;
  border-bottom: 1px solid rgba(203,213,225,0.2);
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.w-icon {
  width: 15px; height: 15px;
  color: #10b981;
}

.w-load { padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
.w-sk { height: 32px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; animation: sh 1.8s infinite; border-radius: 6px; }
@keyframes sh { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-empty { padding: 24px 18px; text-align: center; color: #94a3b8; font-size: 13px; }

.w-list { padding: 6px; }

.b-item {
  display: flex;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  animation: bi 0.3s ease-out forwards;
  animation-delay: calc(var(--p, 0) * 0.04s);
}

@keyframes bi {
  from { opacity: 0; transform: translateX(-6px); }
  to { opacity: 1; transform: translateX(0); }
}

.b-item:hover { background: rgba(16,185,129,0.04); transform: translateX(2px); }

.b-badge {
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
  border-radius: 6px;
  flex-shrink: 0;
  margin-top: 1px;
  background: rgba(241,245,249,0.5);
  color: #94a3b8;
}

.b-badge.b1 { background: linear-gradient(135deg, #10b981, #059669); color: #fff; }
.b-badge.b2 { background: linear-gradient(135deg, #34d399, #10b981); color: #fff; }
.b-badge.b3 { background: linear-gradient(135deg, #6ee7b7, #34d399); color: #065f46; }

.b-info { flex: 1; min-width: 0; }

.b-title {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
}

.b-item:hover .b-title { color: #059669; }

.b-meta {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}
</style>
