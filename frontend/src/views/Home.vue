<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import HeaderBar from '@/components/HeaderBar.vue'
import Sidebar from '@/components/Sidebar.vue'
import ArticleGrid from '@/components/ArticleGrid.vue'
import SideDrawer from '@/components/SideDrawer.vue'
import { getAllCategories } from '@/api/category'
import { getArticleList } from '@/api/article'
import { getBaiduHotSearch, type BaiduHotItem } from '@/api/baidu'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/user'
import { navigateTo } from '@/utils/navigate'

const userStore = useUserStore()
const categories = ref<any[]>([])
const selectedCategory = ref<number | null>(null)
const selectedTag = ref('')
const searchKeyword = ref('')
const articles = ref<any[]>([])
const loading = ref(false)
const pagination = ref({ pageNum: 1, pageSize: 12, total: 0 })
const sidebarCollapse = ref(false)
const panelOpen = ref(false)

// ── 百度热搜 ──
const baiduHotList = ref<BaiduHotItem[]>([])
const baiduLoading = ref(true)

// ── 氛围 Canvas ──
const bgCanvas = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0
let ctx: CanvasRenderingContext2D | null = null
let canvasW = 0, canvasH = 0

const blobs = [
  { h: 210, s: 30, l: 82, r: 0.35, x: 0.10, y: 0.08, vx: 0.04, vy: 0.05, p: 0 },
  { h: 200, s: 25, l: 84, r: 0.30, x: 0.88, y: 0.06, vx: -0.05, vy: 0.04, p: 1.8 },
  { h: 150, s: 28, l: 83, r: 0.32, x: 0.15, y: 0.82, vx: 0.05, vy: -0.04, p: 3.5 },
  { h: 160, s: 22, l: 85, r: 0.28, x: 0.85, y: 0.80, vx: -0.04, vy: -0.05, p: 5.0 },
  { h: 180, s: 20, l: 86, r: 0.36, x: 0.50, y: 0.50, vx: 0.05, vy: 0.04, p: 2.2 },
  { h: 220, s: 22, l: 83, r: 0.30, x: 0.50, y: 0.85, vx: 0.06, vy: -0.04, p: 4.0 },
]

const initBg = () => {
  const c = bgCanvas.value
  if (!c) return
  ctx = c.getContext('2d')
  if (!ctx) return
  canvasW = window.innerWidth; canvasH = window.innerHeight
  c.width = canvasW; c.height = canvasH
}

const drawBg = () => {
  if (!ctx) return
  const c = ctx, t = Date.now() * 0.0002, W = canvasW, H = canvasH
  c.clearRect(0, 0, W, H)
  const bg = c.createLinearGradient(0, 0, W, H)
  bg.addColorStop(0, '#fafcff'); bg.addColorStop(0.5, '#f5fbfa'); bg.addColorStop(1, '#f8fafe')
  c.fillStyle = bg; c.fillRect(0, 0, W, H)

  for (const b of blobs) {
    const bx = W * (b.x + Math.sin(t * b.vx + b.p) * 0.12)
    const by = H * (b.y + Math.cos(t * b.vy + b.p) * 0.10)
    const br = Math.min(W, H) * (b.r + Math.sin(t * 0.3 + b.p) * 0.03)
    const g = c.createRadialGradient(bx, by, 0, bx, by, br)
    g.addColorStop(0, `hsla(${b.h}, ${b.s}%, ${b.l}%, 0.30)`)
    g.addColorStop(0.4, `hsla(${b.h}, ${b.s - 5}%, ${b.l - 4}%, 0.12)`)
    g.addColorStop(1, 'transparent')
    c.fillStyle = g; c.beginPath(); c.arc(bx, by, br, 0, Math.PI * 2); c.fill()
  }

  c.globalAlpha = 0.06
  for (let i = 0; i < 2; i++) {
    const y = H * (0.30 + i * 0.35), wave = Math.sin(t * 0.6 + i * 2) * W * 0.10
    const lg = c.createLinearGradient(0, y - 30, 0, y + 30)
    lg.addColorStop(0, 'transparent'); lg.addColorStop(0.5, `hsla(${i === 0 ? 200 : 150}, 35%, 75%, 0.25)`); lg.addColorStop(1, 'transparent')
    c.fillStyle = lg
    c.beginPath(); c.moveTo(-50, y - 30); c.quadraticCurveTo(W * 0.5 + wave, y - 10, W + 50, y - 30)
    c.lineTo(W + 50, y + 30); c.quadraticCurveTo(W * 0.5 + wave, y + 10, -50, y + 30); c.closePath(); c.fill()
  }
  c.globalAlpha = 1
  animFrameId = requestAnimationFrame(drawBg)
}

const handleResize = () => {
  const c = bgCanvas.value
  if (!c) return
  canvasW = window.innerWidth; canvasH = window.innerHeight
  c.width = canvasW; c.height = canvasH
}

onMounted(async () => {
  if (userStore.token) {
    try { const r: any = await getUserInfo(); userStore.setUserInfo(r.data) } catch {}
  }
  await Promise.allSettled([fetchCategories(), fetchArticles(), fetchBaiduHot()])
  await nextTick()
  initBg(); drawBg()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', handleResize)
})

const fetchCategories = async () => {
  try { const r: any = await getAllCategories(); categories.value = r.data || [] } catch {}
}

const fetchBaiduHot = async () => {
  baiduLoading.value = true
  try {
    const res = await getBaiduHotSearch()
    baiduHotList.value = (res.data || []).slice(0, 10)
  } catch {
    // 静默失败，保持空列表
  } finally {
    baiduLoading.value = false
  }
}

/** 格式化热度值：将纯数字转换为“X万”显示 */
const formatHotScore = (score: string) => {
  if (!score) return '-'
  const num = parseInt(score, 10)
  if (isNaN(num)) return score
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

/** 根据标签名返回对应的 CSS class */
const getTagClass = (tag: string) => {
  if (['热', '沸', '爆'].includes(tag)) return 'tag-hot'
  if (['新'].includes(tag)) return 'tag-new'
  if (['辟谣'].includes(tag)) return 'tag-rumor'
  return 'tag-default'
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const p: any = { pageNum: pagination.value.pageNum, pageSize: pagination.value.pageSize }
    if (selectedCategory.value) p.categoryId = selectedCategory.value
    if (selectedTag.value) p.keyword = selectedTag.value
    if (searchKeyword.value) p.keyword = searchKeyword.value
    const res: any = await getArticleList(p)
    articles.value = res.data?.records || []; pagination.value.total = res.data?.total || 0
  } catch { ElMessage.error('获取文章列表失败') }
  finally { loading.value = false }
}

const handleCategorySelect = (id: number | null) => {
  selectedCategory.value = id; selectedTag.value = ''; searchKeyword.value = ''
  pagination.value.pageNum = 1; fetchArticles()
}

const handleTagSelect = (tagName: string) => {
  selectedTag.value = tagName; selectedCategory.value = null; searchKeyword.value = ''
  pagination.value.pageNum = 1; fetchArticles()
}

const handleSearch = (kw: string) => {
  searchKeyword.value = kw; selectedTag.value = ''
  pagination.value.pageNum = 1; fetchArticles()
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page; fetchArticles()
}

const goCreateArticle = () => {
  if (!userStore.checkLogin('请先登录以发布文章')) return
  navigateTo('/article/create')
}
</script>

<template>
  <div class="home" :class="{ 'sidebar-collapsed': sidebarCollapse, 'panel-open': panelOpen }">
    <canvas ref="bgCanvas" class="bg-canvas" />

    <!-- Sidebar (fixed left) -->
    <Sidebar
      :is-collapse="sidebarCollapse"
      :active-mode="'discover'"
      @toggle="sidebarCollapse = !sidebarCollapse"
    />

    <!-- Header (fixed top) - 搜索 + 标签 + 分类 -->
    <HeaderBar
      :categories="categories"
      :selected-category="selectedCategory"
      :selected-tag="selectedTag"
      :sidebar-collapse="sidebarCollapse"
      @search="handleSearch"
      @category-select="handleCategorySelect"
      @tag-select="handleTagSelect"
    />

    <!-- 主内容 -->
    <main class="main-content">
      <div class="container">

        <!-- ⚡ 百度热搜 - 大卡片 -->
        <div class="baidu-hero">
          <div class="baidu-hero-header">
            <div class="baidu-hero-label">
              <svg class="baidu-fire" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
              <span>百度热搜</span>
              <span class="baidu-hero-badge">实时热点</span>
            </div>
            <span class="baidu-hero-update">数据来自百度 · 每10分钟更新</span>
          </div>
          <div class="baidu-hero-grid">
            <template v-if="baiduLoading">
              <div v-for="j in 10" :key="'sk-'+j" class="baidu-hero-item">
                <span class="baidu-hero-num" :class="'n' + j">{{ j }}</span>
                <div class="baidu-hero-content">
                  <span class="baidu-hero-title" style="color:#cbd5e1;">加载中...</span>
                </div>
              </div>
            </template>
            <template v-else-if="baiduHotList.length > 0">
              <a
                v-for="(item, idx) in baiduHotList"
                :key="item.rank"
                class="baidu-hero-item"
                :class="{ 'top3': idx < 3 }"
                :href="item.url || '#'"
                target="_blank"
                rel="noopener noreferrer"
              >
                <span class="baidu-hero-num" :class="'n' + item.rank">{{ item.rank }}</span>
                <div class="baidu-hero-content">
                  <span class="baidu-hero-title">{{ item.word }}</span>
                </div>
                <span v-if="item.desc" class="baidu-hero-tag" :class="getTagClass(item.desc)">{{ item.desc }}</span>
                <span class="baidu-hero-heat">🔥 {{ formatHotScore(item.hotScore) }}</span>
              </a>
            </template>
            <div v-else class="baidu-hero-empty">暂无热搜数据，请稍后刷新页面重试</div>
          </div>
        </div>

        <!-- Loading 骨架 -->
        <div v-if="loading" class="loading-grid">
          <div v-for="i in 6" :key="i" class="sk-card" :style="{ '--s': i }">
            <div class="sk-img" />
            <div class="sk-body">
              <div class="sk-line w-40" />
              <div class="sk-line w-80" />
              <div class="sk-line w-60" />
            </div>
          </div>
        </div>

        <!-- 文章网格 -->
        <div v-else>
          <ArticleGrid
            :articles="articles"
            :pagination="pagination"
            @page-change="handlePageChange"
          />
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && articles.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="empty-icon"><path d="M13 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>
          <p>暂无文章</p>
        </div>
      </div>
    </main>

    <!-- 悬浮写文章 -->
    <button class="fab" @click="goCreateArticle">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
    </button>

    <!-- 右侧抽屉触发按钮（左右箭头） -->
    <button class="drawer-trigger" :class="{ active: panelOpen }" @click="panelOpen = !panelOpen" :title="panelOpen ? '关闭排行榜' : '打开排行榜'">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path :d="panelOpen ? 'M15 18L9 12L15 6' : 'M9 18L15 12L9 6'" />
      </svg>
    </button>

    <!-- 右侧抽屉 -->
    <SideDrawer :visible="panelOpen" @close="panelOpen = false" />
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: #fafcff;
  position: relative;
}

.bg-canvas {
  position: fixed; inset: 0; z-index: 0;
  pointer-events: none;
}

.main-content {
  position: relative;
  z-index: 1;
  padding-top: 104px; /* header(50) + tags(~26) + cats(~28) */
  margin-left: 240px;
  transition: margin-left 0.35s cubic-bezier(0.4, 0, 0.2, 1), margin-right 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.home.sidebar-collapsed .main-content {
  margin-left: 64px;
}

/* 右侧抽屉打开 → 压缩主内容 */
.home.panel-open .main-content {
  margin-right: 340px;
}

@media (max-width: 1300px) {
  .home.panel-open .main-content {
    margin-right: 0;
  }
}

.container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 40px 60px;
}

/* ── 百度热搜大卡片（替换原英雄卡片区域） ── */
.baidu-hero {
  margin: 8px 0 28px;
  padding: 24px 28px;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(203,213,225,0.25);
  border-radius: 20px;
  animation: baiduIn 0.6s ease-out;
}

@keyframes baiduIn {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.baidu-hero-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(203,213,225,0.2);
}

.baidu-hero-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.baidu-hero-label .baidu-fire { width: 20px; height: 20px; color: #ef4444; }

.baidu-hero-badge {
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #f97316);
  padding: 2px 9px;
  border-radius: 100px;
}

.baidu-hero-update {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.baidu-hero-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.baidu-hero-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: 10px;
  transition: all 0.25s ease;
  cursor: default;
  text-decoration: none;
  color: inherit;
}

a.baidu-hero-item { cursor: pointer; }

.baidu-hero-empty {
  padding: 24px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.baidu-hero-item:hover {
  background: rgba(241,245,249,0.6);
  transform: translateX(4px);
}

.baidu-hero-item.top3 {
  background: rgba(254,242,242,0.4);
}

.baidu-hero-item.top3:hover {
  background: rgba(254,242,242,0.7);
}

.baidu-hero-num {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
  background: rgba(241,245,249,0.6);
  color: #94a3b8;
}

.baidu-hero-num.n1 { background: rgba(239,68,68,0.12); color: #dc2626; }
.baidu-hero-num.n2 { background: rgba(249,115,22,0.12); color: #ea580c; }
.baidu-hero-num.n3 { background: rgba(245,158,11,0.12); color: #d97706; }
.baidu-hero-num.n4 { background: rgba(59,130,246,0.08); color: #3b82f6; }
.baidu-hero-num.n5 { background: rgba(16,185,129,0.08); color: #10b981; }
.baidu-hero-num.n6,
.baidu-hero-num.n7,
.baidu-hero-num.n8,
.baidu-hero-num.n9,
.baidu-hero-num.n10 { background: rgba(99,102,241,0.06); color: #6366f1; }

.baidu-hero-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.baidu-hero-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.baidu-hero-item.top3 .baidu-hero-title {
  font-size: 15px;
}

.baidu-hero-heat {
  font-size: 12px;
  font-weight: 600;
  color: #f97316;
  flex-shrink: 0;
  white-space: nowrap;
}

.baidu-hero-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 100px;
  flex-shrink: 0;
  white-space: nowrap;
}
.tag-hot { color: #ef4444; background: rgba(239,68,68,0.10); }
.tag-new { color: #3b82f6; background: rgba(59,130,246,0.10); }
.tag-rumor { color: #6366f1; background: rgba(99,102,241,0.10); }
.tag-default { color: #64748b; background: rgba(100,116,139,0.08); }

/* ── Loading ── */
.loading-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }

.sk-card {
  background: #fff; border-radius: 14px; overflow: hidden;
  border: 1px solid rgba(203,213,225,0.3);
  opacity: 0; animation: skFadeIn 0.4s ease-out forwards;
  animation-delay: calc(var(--s, 0) * 0.06s);
}

@keyframes skFadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

.sk-img {
  height: 180px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%; animation: shimmer 1.8s infinite;
}

.sk-body { padding: 16px 18px; display: flex; flex-direction: column; gap: 10px; }

.sk-line {
  height: 14px; border-radius: 4px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%; animation: shimmer 1.8s infinite;
}

.sk-line.w-40 { width: 40%; } .sk-line.w-60 { width: 60%; } .sk-line.w-80 { width: 80%; }

@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* ── Empty ── */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 80px 0; }
.empty-icon { width: 64px; height: 64px; color: #cbd5e1; }
.empty-state p { font-size: 16px; color: #94a3b8; }

/* ── FAB ── */
.fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 52px; height: 52px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 20px rgba(59,130,246,0.3);
  transition: all 0.3s ease;
  z-index: 50;
}

.fab svg { width: 22px; height: 22px; }

.fab:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 28px rgba(59,130,246,0.4);
}

.fab:active { transform: scale(0.95); }

/* ── 抽屉触发按钮（页面右边界中间） ── */
.drawer-trigger {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 60px;
  border: 1px solid rgba(203,213,225,0.3);
  border-right: none;
  border-radius: 6px 0 0 6px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(10px);
  color: #3b82f6;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: -2px 0 8px rgba(0,0,0,0.03);
  transition: all 0.3s ease;
  z-index: 50;
  padding: 0;
}

.drawer-trigger svg {
  width: 16px;
  height: 16px;
  transition: transform 0.3s ease;
}

.drawer-trigger:hover {
  background: #fff;
  box-shadow: -2px 0 12px rgba(59,130,246,0.1);
  color: #2563eb;
  width: 26px;
}

.drawer-trigger.active {
  background: rgba(59,130,246,0.06);
  border-color: rgba(59,130,246,0.2);
  color: #3b82f6;
  z-index: 1000;
}

@media (max-width: 1100px) {
  .main-content { margin-left: 0; padding-top: 104px; }
  .home.panel-open .main-content { margin-left: 0; margin-right: 0; }
  .home.sidebar-collapsed .main-content { margin-left: 0; }
  .container { padding: 0 24px 40px; }
  .baidu-hero { padding: 20px 18px; }
  .fab { right: 16px; bottom: 28px; }
}
</style>
