<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getBrowserDeviceStats } from '@/api/statistics'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(true)
const statDays = ref(7)
const browserChartRef = ref<HTMLDivElement | null>(null)
const deviceChartRef = ref<HTMLDivElement | null>(null)
const osChartRef = ref<HTMLDivElement | null>(null)
const comboChartRef = ref<HTMLDivElement | null>(null)

let browserChart: echarts.ECharts | null = null
let deviceChart: echarts.ECharts | null = null
let osChart: echarts.ECharts | null = null
let comboChart: echarts.ECharts | null = null

const statsData = ref({
  browserStats: [],
  deviceStats: [],
  osStats: [],
  browserOsStats: []
})

const fetchStats = async () => {
  loading.value = true
  try {
    const res: any = await getBrowserDeviceStats(statDays.value)
    if (res.data) {
      statsData.value = res.data
      setTimeout(() => renderCharts(), 80)
    }
  } catch {
    ElMessage.error('获取统计数据失败')
  } finally {
    setTimeout(() => { loading.value = false }, 300)
  }
}

const totalBrowser = computed(() =>
  statsData.value.browserStats.reduce((s: number, i: any) => s + Number(i.total_count || 0), 0)
)
const totalDevice = computed(() =>
  statsData.value.deviceStats.reduce((s: number, i: any) => s + Number(i.total_count || 0), 0)
)
const totalOs = computed(() =>
  statsData.value.osStats.reduce((s: number, i: any) => s + Number(i.total_count || 0), 0)
)

const statCards = computed(() => [
  { label: '浏览器种类', value: statsData.value.browserStats.length, accent: '#6366f1', delay: '0s' },
  { label: '设备类型', value: statsData.value.deviceStats.length, accent: '#10b981', delay: '0.06s' },
  { label: '操作系统', value: statsData.value.osStats.length, accent: '#f59e0b', delay: '0.12s' },
  { label: '总访问样本', value: `${(totalBrowser.value / 10000).toFixed(1)}w`, accent: '#a855f7', delay: '0.18s' },
])

const formatNum = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

const renderCharts = () => {
  renderBrowserChart()
  renderDeviceChart()
  renderOsChart()
  renderComboChart()
}

const disposeCharts = () => {
  browserChart?.dispose(); browserChart = null
  deviceChart?.dispose(); deviceChart = null
  osChart?.dispose(); osChart = null
  comboChart?.dispose(); comboChart = null
}

const renderBrowserChart = () => {
  if (!browserChartRef.value) return
  browserChart?.dispose()
  browserChart = echarts.init(browserChartRef.value)
  const data = statsData.value.browserStats.map((i: any) => ({ name: i.browser, value: i.total_count }))
  browserChart.setOption({
    backgroundColor: 'transparent',
    title: { text: '浏览器分布', left: 'center', top: 6, textStyle: { color: '#0f172a', fontSize: 15, fontWeight: 700 } },
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,19,32,0.92)', borderColor: 'rgba(59,130,246,0.2)', borderWidth: 1, textStyle: { color: '#f1f5f9', fontSize: 12 }, formatter: (p: any) => `${p.name}<br/>访问量: <b>${formatNum(p.value)}</b> (${p.percent}%)` },
    legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { color: '#94a3b8', fontSize: 11 }, itemGap: 8 },
    series: [{
      type: 'pie', radius: ['38%', '68%'], center: ['40%', '52%'],
      avoidLabelOverlap: true,
      padAngle: 1.5,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold', color: '#0f172a' } },
      labelLine: { show: false },
      data,
      color: ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7', '#ec4899', '#f97316', '#8b5cf6', '#14b8a6']
    }]
  })
}

const renderDeviceChart = () => {
  if (!deviceChartRef.value) return
  deviceChart?.dispose()
  deviceChart = echarts.init(deviceChartRef.value)
  const total = totalDevice.value || 1
  const data = statsData.value.deviceStats.map((i: any) => ({ name: i.device_type, value: i.total_count }))
  deviceChart.setOption({
    backgroundColor: 'transparent',
    title: { text: '设备类型分布', left: 'center', top: 6, textStyle: { color: '#0f172a', fontSize: 15, fontWeight: 700 } },
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,19,32,0.92)', borderColor: 'rgba(59,130,246,0.2)', borderWidth: 1, textStyle: { color: '#f1f5f9', fontSize: 12 } },
    graphic: [
      { type: 'text', left: 'center', top: '44%', style: { text: formatNum(total), fill: '#0f172a', fontSize: 24, fontWeight: 800, textAlign: 'center', textVerticalAlign: 'middle' } },
      { type: 'text', left: 'center', top: '55%', style: { text: '总访问', fill: '#64748b', fontSize: 12, fontWeight: 500, textAlign: 'center', textVerticalAlign: 'middle' } }
    ],
    series: [{
      type: 'pie', radius: ['48%', '73%'], center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: { color: '#94a3b8', fontSize: 12, formatter: '{b}\n{d}%' },
      labelLine: { length: 12, length2: 16, lineStyle: { color: 'rgba(203,213,225,0.3)' } },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.4)' } },
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      data: data.length ? data : [{ name: '暂无数据', value: 1 }],
      color: data.length ? ['#10b981', '#f59e0b', '#6366f1'] : ['#334155']
    }]
  })
}

const renderOsChart = () => {
  if (!osChartRef.value) return
  osChart?.dispose()
  osChart = echarts.init(osChartRef.value)
  const total = statsData.value.osStats.reduce((s: number, i: any) => s + Number(i.total_count || 0), 0) || 1
  const data = statsData.value.osStats.map((i: any) => ({ name: i.os, value: i.total_count }))
  osChart.setOption({
    backgroundColor: 'transparent',
    title: { text: '操作系统分布', left: 'center', top: 6, textStyle: { color: '#0f172a', fontSize: 15, fontWeight: 700 } },
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,19,32,0.92)', borderColor: 'rgba(59,130,246,0.2)', borderWidth: 1, textStyle: { color: '#f1f5f9', fontSize: 12 }, formatter: (p: any) => `${p.name}<br/>访问量: <b>${formatNum(p.value)}</b> 次 (${p.percent}%)` },
    series: [{
      type: 'pie', radius: ['30%', '58%'], center: ['50%', '52%'],
      avoidLabelOverlap: true,
      padAngle: 1,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 1.5 },
      label: { color: '#94a3b8', fontSize: 11, formatter: (p: any) => `${p.name}\n${formatNum(p.value)} 次` },
      labelLine: { length: 10, length2: 14, lineStyle: { color: 'rgba(203,213,225,0.3)' } },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.4)' } },
      data,
      color: ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7', '#f97316', '#8b5cf6']
    }]
  })
}

const renderComboChart = () => {
  if (!comboChartRef.value) return
  comboChart?.dispose()
  comboChart = echarts.init(comboChartRef.value)
  const items = statsData.value.browserOsStats
  const labels = items.map((i: any) => i.label).reverse()
  const values = items.map((i: any) => i.total_count).reverse()
  comboChart.setOption({
    backgroundColor: 'transparent',
    title: { text: '浏览器/系统 组合 TOP10', left: 'center', top: 6, textStyle: { color: '#0f172a', fontSize: 15, fontWeight: 700 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(15,19,32,0.92)', borderColor: 'rgba(59,130,246,0.2)', borderWidth: 1, textStyle: { color: '#f1f5f9', fontSize: 12 } },
    grid: { left: 10, right: 50, bottom: 14, top: 44, containLabel: true },
    xAxis: { type: 'value', axisLabel: { color: '#64748b', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(203,213,225,0.2)' } }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'category', data: labels, axisLabel: { color: '#94a3b8', fontSize: 11, fontWeight: 500 }, axisLine: { show: false }, axisTick: { show: false }, splitLine: { show: false } },
    series: [{
      type: 'bar', barWidth: '52%',
      data: values.map((v: number, idx: number) => ({
        value: v,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: `hsl(${250 - idx * 8}, 70%, ${58 + idx * 3}%)` },
            { offset: 1, color: `hsl(${270 - idx * 8}, 80%, ${45 + idx * 3}%)` }
          ]),
          borderRadius: [0, 6, 6, 0]
        }
      })),
      label: { show: true, position: 'right', formatter: (p: any) => formatNum(p.value), color: '#94a3b8', fontSize: 11, fontWeight: 600 },
      emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(59,130,246,0.3)' } },
      animationDuration: 600,
      animationEasing: 'cubicOut'
    }]
  })
}

const handleDaysChange = (d: number) => {
  statDays.value = d
  disposeCharts()
  loading.value = true
  fetchStats()
}

const handleResize = () => {
  browserChart?.resize()
  deviceChart?.resize()
  osChart?.resize()
  comboChart?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<template>
  <div class="bd-stats">
    <!-- Hero -->
    <div class="bd-hero">
      <div class="bd-hero-bg">
        <div class="bd-orb o1" />
        <div class="bd-orb o2" />
        <div class="bd-orb o3" />
      </div>
      <div class="bd-hero-body">
        <div class="bd-hero-left">
          <div class="bd-bread">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
            <span>数据统计</span>
            <span class="bd-bread-arrow">/</span>
            <span class="bd-bread-current">浏览器与设备</span>
          </div>
          <h1 class="bd-hero-title">浏览器 &amp; 设备分析</h1>
          <p class="bd-hero-desc">了解访客的设备偏好，为兼容性优化和用户体验决策提供数据支撑</p>
        </div>
        <div class="bd-hero-right">
          <div class="bd-hero-tabs">
            <button :class="['bd-tab', { active: statDays === 7 }]" @click="handleDaysChange(7)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M21 12H3"/><path d="M12 3v18"/></svg>
              近7天
            </button>
            <button :class="['bd-tab', { active: statDays === 30 }]" @click="handleDaysChange(30)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M21 12H3"/><path d="M12 3v18"/></svg>
              近30天
            </button>
            <button :class="['bd-tab', { active: statDays === 90 }]" @click="handleDaysChange(90)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M21 12H3"/><path d="M12 3v18"/></svg>
              近90天
            </button>
          </div>
          <button class="bd-refresh-btn" @click="handleDaysChange(statDays)">
            <Refresh style="width:15px;height:15px" />
            <span>刷新数据</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="bd-stats-row">
      <div
        v-for="(c, i) in statCards"
        :key="c.label"
        class="bd-stat-card"
        :style="{ '--accent': c.accent, '--delay': c.delay }"
      >
        <div class="bd-stat-glow" />
        <div class="bd-stat-icon" :style="{ background: `linear-gradient(135deg, ${c.accent}22, ${c.accent}08)`, color: c.accent }">
          <template v-if="i === 0">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          </template>
          <template v-else-if="i === 1">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M9 9h6"/><path d="M15 9v6"/></svg>
          </template>
          <template v-else-if="i === 2">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/><line x1="21.17" y1="8" x2="12" y2="8"/><line x1="3.95" y1="6.06" x2="8.54" y2="14"/><line x1="10.88" y1="21.94" x2="15.46" y2="14"/></svg>
          </template>
          <template v-else>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          </template>
        </div>
        <div class="bd-stat-info">
          <span class="bd-stat-num">{{ c.value }}</span>
          <span class="bd-stat-label">{{ c.label }}</span>
        </div>
      </div>
    </div>

    <!-- Charts -->
    <div class="bd-charts" v-loading="loading" element-loading-background="rgba(255,255,255,0.85)">
      <div class="bd-chart-grid">
        <div class="bd-panel">
          <div ref="browserChartRef" class="bd-chart-box" />
        </div>
        <div class="bd-panel">
          <div ref="deviceChartRef" class="bd-chart-box" />
        </div>
      </div>
      <div class="bd-chart-grid">
        <div class="bd-panel">
          <div ref="osChartRef" class="bd-chart-box" />
        </div>
        <div class="bd-panel">
          <div ref="comboChartRef" class="bd-chart-box" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bd-stats {
  padding: 24px 28px;
  min-height: 100%;
}

/* ── Hero ── */
.bd-hero {
  position: relative;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
  margin-bottom: 22px;
}
.bd-hero-bg { display: none !important; }
.bd-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}
.o1 {
  width: 380px; height: 380px;
  background: radial-gradient(circle, rgba(59,130,246,0.3), transparent);
  top: -120px; right: -60px;
  animation: bdFloat 12s ease-in-out infinite alternate;
}
.o2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(168,85,247,0.2), transparent);
  bottom: -100px; left: 8%;
  animation: bdFloat 16s ease-in-out infinite alternate-reverse;
}
.o3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(6,182,212,0.18), transparent);
  top: 20%; right: 25%;
  animation: bdFloat 10s ease-in-out infinite alternate;
}
@keyframes bdFloat {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(30px, -20px) scale(1.1); }
}

.bd-hero-body {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 22px 28px 20px;
}
.bd-hero-left { display: flex; flex-direction: column; gap: 6px; }
.bd-bread {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}
.bd-bread svg { opacity: 0.5; }
.bd-bread-arrow { color: #475569; }
.bd-bread-current { color: #94a3b8; }
.bd-hero-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.4px;
  line-height: 1.2;
}
.bd-hero-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  max-width: 460px;
}
.bd-hero-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}
.bd-hero-tabs {
  display: flex;
  gap: 6px;
  background: rgba(203,213,225,0.3);
  padding: 4px;
  border-radius: 12px;
}
.bd-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: none;
  background: transparent;
  color: #64748b;
  border-radius: 9px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.bd-tab:hover { color: #94a3b8; background: rgba(203,213,225,0.3); }
.bd-tab.active {
  color: #0f172a;
  background: linear-gradient(135deg, #3b82f6, #3b82f6);
  box-shadow: 0 4px 16px rgba(59,130,246,0.35);
}
.bd-refresh-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: 1px solid rgba(203,213,225,0.4);
  background: rgba(203,213,225,0.2);
  color: #94a3b8;
  border-radius: 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.25s;
}
.bd-refresh-btn:hover {
  border-color: rgba(59,130,246,0.3);
  color: #334155;
  background: rgba(59,130,246,0.08);
}
.bd-refresh-btn svg { transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.bd-refresh-btn:hover svg { transform: rotate(180deg); }

/* ── Stats Row ── */
.bd-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 22px;
}
.bd-stat-card {
  position: relative;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  animation: bdCardIn 0.5s ease-out both;
  animation-delay: var(--delay);
}
.bd-stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
  box-shadow: 0 8px 25px rgba(0,0,0,0.06);
}
@keyframes bdCardIn {
  from { opacity: 0; transform: translateY(16px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.bd-stat-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 100% at 50% 0%, color-mix(in srgb, var(--accent) 12%, transparent), transparent 70%);
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}
.bd-stat-card:hover .bd-stat-glow { opacity: 1; }
.bd-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.bd-stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
  z-index: 1;
}
.bd-stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  letter-spacing: -0.3px;
}
.bd-stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

/* ── Charts ── */
.bd-charts {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.bd-chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.bd-panel {
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
  overflow: hidden;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.bd-panel:hover {
  border-color: rgba(59,130,246,0.12);
  box-shadow: 0 8px 25px rgba(0,0,0,0.06);
}
.bd-chart-box {
  width: 100%;
  height: 370px;
}

/* ── Responsive ── */
@media (max-width: 1200px) {
  .bd-chart-grid { grid-template-columns: 1fr; }
  .bd-stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .bd-hero-body { flex-direction: column; align-items: flex-start; gap: 14px; }
  .bd-hero-right { align-items: flex-start; width: 100%; }
  .bd-stats-row { grid-template-columns: 1fr; }
  .bd-stats { padding: 16px; }
}
</style>
