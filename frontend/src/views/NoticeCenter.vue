<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserNoticeList } from '@/api/admin'

const router = useRouter()

const userStore = useUserStore()
const notices = ref<any[]>([])
const loading = ref(true)
const expandedId = ref<number | null>(null)

const fetchNotices = async () => {
  loading.value = true
  try {
    const res: any = await getUserNoticeList()
    notices.value = res.data || []
  } catch {
    notices.value = []
  } finally {
    loading.value = false
  }
}

const toggleExpand = (idx: number) => {
  expandedId.value = expandedId.value === idx ? null : idx
}

const formatPublishTime = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(/-/g, '/'))
  if (isNaN(d.getTime())) return dateStr
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const diffDays = Math.round((today.getTime() - target.getTime()) / 86400000)
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays <= 7) return `${diffDays}天前`
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const formatFullTime = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(/-/g, '/'))
  if (isNaN(d.getTime())) return dateStr
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) + ' ' +
    d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  await fetchNotices()
})
</script>

<template>
  <div class="page">
    <div class="bg-glow bg-glow-1" />
    <div class="bg-glow bg-glow-2" />
    <div class="bg-glow bg-glow-3" />

    <button class="btn-back" @click="router.push('/')">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <line x1="19" y1="12" x2="5" y2="12"/>
        <polyline points="12 19 5 12 12 5"/>
      </svg>
      <span>返回首页</span>
    </button>

    <div class="page-wrap">
      <div class="hero">
        <div class="hero-badge">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 01-3.46 0"/>
          </svg>
          <span>系统公告</span>
        </div>
        <h1 class="hero-title">公告中心</h1>
        <p class="hero-desc">近三个月平台动态</p>
      </div>

      <div class="content">
        <div v-if="loading" class="loading">
          <div class="loader" />
          <p>加载公告中...</p>
        </div>

        <div v-else-if="!notices.length" class="empty">
          <div class="empty-icon">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 01-3.46 0"/>
            </svg>
          </div>
          <h3>暂无公告</h3>
          <p>近三个月内没有发布新的系统公告</p>
        </div>

        <div v-else class="list">
          <div
            v-for="(item, idx) in notices"
            :key="idx"
            class="card"
            :style="{ '--d': idx }"
          >
            <div class="card-header" @click="toggleExpand(idx)">
              <div class="card-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 01-3.46 0"/>
                </svg>
              </div>
              <div class="card-info">
                <h3 class="card-title">{{ item.title }}</h3>
                <div class="card-meta">
                  <span class="card-tag">{{ formatPublishTime(item.publishTime || item.createTime) }}</span>
                  <span class="card-time">发布于 {{ formatFullTime(item.publishTime || item.createTime) }}</span>
                </div>
              </div>
              <div class="card-arrow" :class="{ open: expandedId === idx }">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
            </div>
            <div class="card-body-wrap" :class="{ open: expandedId === idx }">
              <div class="card-body">
                <div class="card-divider" />
                <div class="card-content" v-html="item.content" />
              </div>
            </div>
          </div>
        </div>

        <div class="footer-info">
          <span class="footer-line" />
          <span class="footer-text">共 {{ notices.length }} 条公告</span>
          <span class="footer-line" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow-x: hidden;
  background: linear-gradient(135deg, #faf6f1 0%, #f5ede4 50%, #f0e8de 100%);
  color: #2d2218;
}

/* ── Background Glows ── */
.bg-glow {
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(90px);
  will-change: transform;
  animation: bgFloat 14s ease-in-out infinite alternate;
}

.bg-glow-1 {
  width: 550px;
  height: 550px;
  top: -150px;
  right: -80px;
  background: rgba(251, 191, 36, 0.1);
  animation-delay: 0s;
}

.bg-glow-2 {
  width: 450px;
  height: 450px;
  bottom: -100px;
  left: -100px;
  background: rgba(251, 146, 60, 0.08);
  animation-delay: -5s;
}

.bg-glow-3 {
  width: 380px;
  height: 380px;
  top: 40%;
  left: 60%;
  background: rgba(163, 230, 53, 0.05);
  animation-delay: -10s;
}

@keyframes bgFloat {
  0% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 30px) scale(0.95); }
  100% { transform: translate(15px, -15px) scale(1.02); }
}

.page-wrap {
  position: relative;
  z-index: 1;
  max-width: 680px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

/* ── Header ── */
.btn-back {
  position: fixed;
  left: 24px;
  bottom: 32px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255,255,255,0.7);
  border: 1px solid rgba(0,0,0,0.04);
  border-radius: 14px;
  color: rgba(0,0,0,0.45);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.btn-back:hover {
  background: rgba(255,255,255,0.9);
  border-color: rgba(0,0,0,0.08);
  color: rgba(0,0,0,0.7);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

/* ── Hero ── */
.hero {
  text-align: center;
  padding: 52px 0 48px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: rgba(251,191,36,0.06);
  border: 1px solid rgba(251,191,36,0.1);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(180, 130, 50, 0.7);
  margin-bottom: 24px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.hero-title {
  margin: 0;
  font-size: clamp(36px, 5vw, 48px);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
  color: #1d1d1f;
}

.hero-desc {
  font-size: 15px;
  color: rgba(120, 90, 50, 0.5);
  margin: 10px 0 0;
  font-weight: 400;
}

/* ── Loading ── */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  color: rgba(0,0,0,0.3);
  font-size: 14px;
  gap: 20px;
}

.loader {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(0,0,0,0.04);
  border-top-color: rgba(0,0,0,0.25);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Empty ── */
.empty {
  text-align: center;
  padding: 80px 0;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(251,191,36,0.04);
  border: 1px solid rgba(251,191,36,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: rgba(180, 130, 50, 0.2);
}

.empty h3 {
  font-size: 17px;
  font-weight: 600;
  color: rgba(0,0,0,0.5);
  margin: 0 0 6px;
}

.empty p {
  font-size: 14px;
  color: rgba(0,0,0,0.25);
  margin: 0;
}

/* ── List ── */
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  background: rgba(255,250,240,0.6);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1px solid rgba(255,250,240,0.3);
  border-radius: 22px;
  overflow: hidden;
  box-shadow:
    0 1px 3px rgba(0,0,0,0.02),
    0 8px 32px rgba(0,0,0,0.02);
  animation: cardIn 0.5s ease-out calc(var(--d) * 0.08s + 0.1s) both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  user-select: none;
}

.card-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: rgba(0,0,0,0.02);
  border: 1px solid rgba(0,0,0,0.03);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(0,0,0,0.18);
  transition: all 0.3s ease;
}

.card:hover .card-icon {
  background: rgba(251,191,36,0.06);
  border-color: rgba(251,191,36,0.1);
  color: rgba(251,191,36,0.5);
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 6px;
  background: rgba(251,191,36,0.05);
  color: rgba(180, 130, 50, 0.65);
}

.card-time {
  font-size: 12px;
  color: rgba(0,0,0,0.25);
}

.card-arrow {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(0,0,0,0.15);
  transition: all 0.35s cubic-bezier(0.34,1.56,0.64,1);
  flex-shrink: 0;
}

.card-arrow.open {
  transform: rotate(180deg);
  color: rgba(251,191,36,0.5);
}

/* ── Expand ── */
.card-body-wrap {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.4s cubic-bezier(0.22,1,0.36,1);
  opacity: 0;
}

.card-body-wrap.open {
  grid-template-rows: 1fr;
  opacity: 1;
}

.card-body {
  overflow: hidden;
  padding: 0 20px 20px;
}

.card-divider {
  height: 1px;
  background: linear-gradient(90deg, rgba(251,191,36,0.08), rgba(251,146,60,0.04), transparent);
  margin-bottom: 16px;
}

.card-content {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(0,0,0,0.55);
  white-space: pre-wrap;
}

.card-content :deep(p) {
  margin: 0 0 12px;
}

.card-content :deep(a) {
  color: rgba(180, 130, 50, 0.6);
  text-decoration: none;
  border-bottom: 1px solid rgba(180, 130, 50, 0.15);
  transition: all 0.2s;
}

.card-content :deep(a:hover) {
  color: rgba(180, 130, 50, 0.8);
  border-bottom-color: rgba(180, 130, 50, 0.25);
}

/* ── Footer ── */
.footer-info {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
  margin-top: 40px;
}

.footer-line {
  width: 48px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(251,191,36,0.08));
}

.footer-text {
  font-size: 12px;
  color: rgba(180, 130, 50, 0.3);
  white-space: nowrap;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .page-wrap {
    padding: 0 16px 60px;
  }

  .hero {
    padding: 36px 0 32px;
  }

  .card-header {
    padding: 14px 16px;
  }

  .card-title {
    font-size: 14px;
  }

  .card-body {
    padding: 0 16px 16px;
  }
}
</style>