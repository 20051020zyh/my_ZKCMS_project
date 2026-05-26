<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import Sidebar from '@/components/Sidebar.vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ArticleList from '@/components/ArticleList.vue'
import ArticleRanking from '@/components/ArticleRanking.vue'
import BestArticles from '@/components/BestArticles.vue'
import { getAllCategories } from '@/api/category'
import { getArticleList, getUserCollects } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/user'

const userStore = useUserStore()
const isCollapse = ref(false)
const categories = ref<any[]>([])
const selectedCategory = ref<number | null>(null)
const activeMode = ref<'discover' | 'favorites'>('discover')
const searchKeyword = ref('')
const articles = ref<any[]>([])
const loading = ref(false)
const pagination = ref({
  pageNum: 1,
  pageSize: 15,
  total: 0
})

// ==================== Canvas 炫彩渐变背景 ====================
const bgCanvas = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0
let ctx: CanvasRenderingContext2D | null = null
let canvasW = 0
let canvasH = 0

const blobDefs = [
  { hue: 260, sat: 65, light: 72, r: 0.36, cx: 0.15, cy: 0.18, vx: 0.07, vy: 0.06, phase: 0 },
  { hue: 330, sat: 70, light: 68, r: 0.32, cx: 0.78, cy: 0.14, vx: -0.08, vy: 0.07, phase: 1.3 },
  { hue: 190, sat: 60, light: 70, r: 0.30, cx: 0.82, cy: 0.68, vx: -0.06, vy: -0.07, phase: 2.5 },
  { hue: 340, sat: 55, light: 75, r: 0.28, cx: 0.22, cy: 0.72, vx: 0.08, vy: -0.06, phase: 3.8 },
  { hue: 240, sat: 55, light: 76, r: 0.34, cx: 0.52, cy: 0.38, vx: -0.05, vy: 0.05, phase: 5.0 },
  { hue: 170, sat: 55, light: 72, r: 0.26, cx: 0.56, cy: 0.82, vx: 0.07, vy: -0.05, phase: 1.8 },
  { hue: 300, sat: 60, light: 70, r: 0.30, cx: 0.38, cy: 0.56, vx: 0.06, vy: -0.08, phase: 4.2 },
  { hue: 210, sat: 60, light: 70, r: 0.28, cx: 0.66, cy: 0.34, vx: -0.07, vy: -0.05, phase: 6.0 }
]

const initBackground = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  if (!ctx) return

  canvasW = window.innerWidth
  canvasH = window.innerHeight
  canvas.width = canvasW
  canvas.height = canvasH
}

const drawBackground = () => {
  if (!ctx) return
  const c = ctx
  const t = Date.now() * 0.0003
  const W = canvasW
  const H = canvasH

  c.clearRect(0, 0, W, H)

  const baseGrad = c.createLinearGradient(0, 0, W, H)
  baseGrad.addColorStop(0, '#faf8ff')
  baseGrad.addColorStop(0.35, '#fef7fb')
  baseGrad.addColorStop(0.65, '#f5f7ff')
  baseGrad.addColorStop(1, '#f8faf8')
  c.fillStyle = baseGrad
  c.fillRect(0, 0, W, H)

  for (const b of blobDefs) {
    const bx = W * (b.cx + Math.sin(t * b.vx + b.phase) * 0.16)
    const by = H * (b.cy + Math.cos(t * b.vy + b.phase) * 0.14)
    const br = Math.min(W, H) * (b.r + Math.sin(t * 0.4 + b.phase) * 0.05)

    const gradient = c.createRadialGradient(bx, by, 0, bx, by, br)
    gradient.addColorStop(0, `hsla(${b.hue}, ${b.sat}%, ${b.light}%, 0.45)`)
    gradient.addColorStop(0.2, `hsla(${b.hue}, ${b.sat}%, ${b.light - 6}%, 0.3)`)
    gradient.addColorStop(0.5, `hsla(${b.hue}, ${b.sat - 8}%, ${b.light - 10}%, 0.12)`)
    gradient.addColorStop(0.75, `hsla(${b.hue}, ${b.sat - 15}%, ${b.light - 16}%, 0.03)`)
    gradient.addColorStop(1, 'transparent')

    c.fillStyle = gradient
    c.beginPath()
    c.arc(bx, by, br, 0, Math.PI * 2)
    c.fill()
  }

  // Flowing light ribbons
  c.globalAlpha = 0.12
  for (let i = 0; i < 3; i++) {
    const ribbonY = H * (0.22 + i * 0.28)
    const wave = Math.sin(t * 1.1 + i * 2.1) * W * 0.16

    const rg = c.createLinearGradient(0, ribbonY - 70, 0, ribbonY + 70)
    const ribbonHue = [270, 320, 195][i]
    rg.addColorStop(0, 'transparent')
    rg.addColorStop(0.3, `hsla(${ribbonHue}, 60%, 68%, 0.5)`)
    rg.addColorStop(0.5, `hsla(${ribbonHue + 15}, 65%, 72%, 0.6)`)
    rg.addColorStop(0.7, `hsla(${ribbonHue}, 60%, 68%, 0.5)`)
    rg.addColorStop(1, 'transparent')

    c.fillStyle = rg
    c.beginPath()
    c.moveTo(-50, ribbonY - 70)
    c.quadraticCurveTo(W * 0.5 + wave, ribbonY - 30, W + 50, ribbonY - 70)
    c.lineTo(W + 50, ribbonY + 70)
    c.quadraticCurveTo(W * 0.5 + wave, ribbonY + 30, -50, ribbonY + 70)
    c.closePath()
    c.fill()
  }
  c.globalAlpha = 1

  // Shimmer dots
  const dotCount = 25
  for (let i = 0; i < dotCount; i++) {
    const seed = i * 137.508
    const dx = (Math.sin(t * 0.3 + seed) * 0.5 + 0.5) * W
    const dy = (Math.cos(t * 0.37 + seed * 1.3) * 0.5 + 0.5) * H
    const dr = 2 + Math.sin(t * 2 + i) * 1.2
    const dhue = 250 + Math.sin(t + i * 0.7) * 90

    c.beginPath()
    c.arc(dx, dy, dr, 0, Math.PI * 2)
    c.fillStyle = `hsla(${dhue}, 80%, 68%, ${0.22 + Math.sin(t * 3 + i) * 0.14})`
    c.fill()
  }

  animFrameId = requestAnimationFrame(drawBackground)
}

const handleBgResize = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  canvasW = window.innerWidth
  canvasH = window.innerHeight
  canvas.width = canvasW
  canvas.height = canvasH
}

onMounted(async () => {
  if (userStore.token) {
    try {
      const res: any = await getUserInfo()
      userStore.setUserInfo(res.data)
    } catch {}
  }
  await Promise.allSettled([fetchCategories(), fetchArticles()])
  await nextTick()
  initBackground()
  drawBackground()
  window.addEventListener('resize', handleBgResize)
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', handleBgResize)
})
// ==================== 背景动画 END ====================

const fetchCategories = async () => {
  try {
    const res: any = await getAllCategories()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    if (activeMode.value === 'favorites') {
      const res: any = await getUserCollects({
        pageNum: pagination.value.pageNum,
        pageSize: pagination.value.pageSize
      })
      articles.value = res.data?.records || []
      pagination.value.total = res.data?.total || 0
    } else {
      const params: any = {
        pageNum: pagination.value.pageNum,
        pageSize: pagination.value.pageSize
      }
      if (selectedCategory.value) params.categoryId = selectedCategory.value
      if (searchKeyword.value) params.keyword = searchKeyword.value
      const res: any = await getArticleList(params)
      articles.value = res.data?.records || []
      pagination.value.total = res.data?.total || 0
    }
  } catch (error) {
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

const handleCategorySelect = (categoryId: number | null) => {
  selectedCategory.value = categoryId
  activeMode.value = 'discover'
  pagination.value.pageNum = 1
  fetchArticles()
}

const handleModeChange = (mode: 'discover' | 'favorites' | 'history') => {
  if (mode === 'history') {
    ElMessage.info('浏览历史功能开发中，敬请期待')
    return
  }
  activeMode.value = mode as 'discover' | 'favorites'
  pagination.value.pageNum = 1
  fetchArticles()
}

const handleSearch = (keyword: string) => {
  searchKeyword.value = keyword
  pagination.value.pageNum = 1
  fetchArticles()
}

const handlePageChange = (page: number) => {
  pagination.value.pageNum = page
  fetchArticles()
}

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<template>
  <div class="home-layout">
    <canvas ref="bgCanvas" class="bg-canvas" />

    <Sidebar
      :is-collapse="isCollapse"
      @toggle="toggleSidebar"
      @category-select="handleCategorySelect"
      @mode-change="handleModeChange"
      :categories="categories"
      :selected-category="selectedCategory"
      :active-mode="activeMode"
    />

    <div class="main-area" :class="{ expanded: isCollapse }">
      <HeaderBar
        :selected-category="selectedCategory"
        @search="handleSearch"
        @category-select="handleCategorySelect"
      />

      <div class="content-area">
        <div class="content-inner">
          <div class="feed-section">
            <ArticleList
              :articles="articles"
              :loading="loading"
              :pagination="pagination"
              @page-change="handlePageChange"
            />
          </div>

          <aside class="side-section">
            <ArticleRanking />
            <BestArticles />
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-layout {
  display: flex;
  min-height: 100vh;
  background: #faf8ff;
  position: relative;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.main-area {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 0;
  position: relative;
  z-index: 1;
  animation: mainFadeIn 0.5s ease-out;
}

@keyframes mainFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.main-area.expanded {
  margin-left: 64px;
}

.content-area {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 32px;
}

.content-inner {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.feed-section {
  flex: 1;
  min-width: 0;
}

.side-section {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 140px;
  height: fit-content;
  animation: sideSlideIn 0.5s ease-out 0.2s both;
}

@keyframes sideSlideIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1100px) {
  .side-section {
    display: none;
  }
}
</style>