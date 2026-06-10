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
  { label: '浏览', value: 'view', countKey: 'viewCount', suffix: '' },
  { label: '收藏', value: 'collect', countKey: 'collectCount', suffix: '' },
  { label: '点赞', value: 'like', countKey: 'likeCount', suffix: '' },
]

const fetchData = async (type: string = 'view') => {
  loading.value = true
  try { const r: any = await getArticleRank({ type, limit: 10 }); items.value = r.data || [] }
  catch { items.value = [] }
  finally { loading.value = false }
}

const handleTabChange = (t: string) => { activeTab.value = t; fetchData(t) }

const goToArticle = (id: number) => {
  if (!userStore.checkLogin('请先登录以查看文章')) return
  navigateTo(`/article/${id}`)
}

const getRankClass = (i: number) => {
  if (i === 0) return 'r1'
  if (i === 1) return 'r2'
  if (i === 2) return 'r3'
  return ''
}

const activeCfg = () => tabs.find(t => t.value === activeTab.value)

const fmt = (n: number) => {
  if (!n) return 0
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}

onMounted(() => fetchData('view'))
</script>

<template>
  <div class="widget">
    <div class="w-head">
      <svg class="w-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 4 18 9"/><polyline points="6 15 12 20 18 15"/></svg>
      <span>排行榜</span>
    </div>

    <div class="w-tabs">
      <button v-for="t in tabs" :key="t.value" class="w-tab" :class="{ active: activeTab === t.value }" @click="handleTabChange(t.value)">{{ t.label }}</button>
    </div>

    <div v-if="loading" class="w-load">
      <div v-for="i in 5" :key="i" class="w-sk" />
    </div>

    <div v-else-if="items.length === 0" class="w-empty">暂无数据</div>

    <div v-else class="w-list">
      <div v-for="(item, i) in items.slice(0, 6)" :key="item.id" class="w-item" :style="{ '--q': i }" @click="goToArticle(item.id)">
        <span class="w-num" :class="getRankClass(i)">
          <template v-if="i === 0">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          </template>
          <template v-else>{{ i + 1 }}</template>
        </span>
        <span class="w-title">{{ item.title }}</span>
        <span class="w-cnt">{{ fmt(activeCfg() ? item[activeCfg()!.countKey] : 0) }}</span>
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
  border-color: rgba(59,130,246,0.1);
}

.w-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.w-icon {
  width: 16px; height: 16px;
  color: #3b82f6;
}

.w-tabs {
  display: flex;
  gap: 4px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(203,213,225,0.2);
}

.w-tab {
  flex: 1;
  padding: 5px 0;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  background: rgba(248,250,252,0.5);
  border: 1px solid rgba(203,213,225,0.25);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.w-tab.active {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  border-color: transparent;
  font-weight: 600;
}

.w-load { padding: 14px 18px; display: flex; flex-direction: column; gap: 10px; }
.w-sk { height: 12px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; animation: sh 1.8s infinite; border-radius: 4px; }
@keyframes sh { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-empty { padding: 24px 18px; text-align: center; color: #94a3b8; font-size: 13px; }

.w-list { padding: 6px; }

.w-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  animation: ri 0.3s ease-out forwards;
  animation-delay: calc(var(--q, 0) * 0.03s);
}

@keyframes ri {
  from { opacity: 0; transform: translateX(-6px); }
  to { opacity: 1; transform: translateX(0); }
}

.w-item:hover { background: rgba(59,130,246,0.04); }

.w-num {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700;
  border-radius: 5px;
  flex-shrink: 0;
  background: rgba(241,245,249,0.5);
  color: #94a3b8;
}

.w-num.r1 { background: linear-gradient(135deg, #f59e0b, #fbbf24); color: #fff; }
.w-num.r2 { background: linear-gradient(135deg, #94a3b8, #cbd5e1); color: #fff; }
.w-num.r3 { background: linear-gradient(135deg, #d4a574, #c4956a); color: #fff; }

.w-title {
  flex: 1;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.w-cnt {
  font-size: 12px;
  font-weight: 600;
  color: #3b82f6;
  flex-shrink: 0;
}


</style>
