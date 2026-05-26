<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminHomeStats } from '@/api/user'
import { getArticleTrend } from '@/api/article'
import * as echarts from 'echarts'
import {
  Document,
  User,
  View,
  ChatDotRound,
  StarFilled,
  Star,
  TrendCharts
} from '@element-plus/icons-vue'

const loading = ref(true)
const currentTime = ref('')
const currentDate = ref('')
let timeHandle = 0
const chartRef = ref<HTMLDivElement | null>(null)
const compareChartRef = ref<HTMLDivElement | null>(null)
const growthChartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let compareChartInstance: echarts.ECharts | null = null
let growthChartInstance: echarts.ECharts | null = null
const trendDays = ref(7)
const compareMode = ref('todayVsYesterday')

const stats = ref({
  totalArticles: 0, totalUsers: 0, totalViews: 0, totalComments: 0,
  totalLikes: 0, totalCollections: 0,
  todayArticles: 0, todayUsers: 0, todayViews: 0, todayComments: 0,
  todayLikes: 0, todayCollections: 0,
  yesterdayArticles: 0, yesterdayUsers: 0, yesterdayViews: 0, yesterdayComments: 0,
  yesterdayLikes: 0, yesterdayCollections: 0,
  beforeYesterdayArticles: 0, beforeYesterdayUsers: 0, beforeYesterdayViews: 0, beforeYesterdayComments: 0,
  beforeYesterdayLikes: 0, beforeYesterdayCollections: 0,
})

interface AnimatedNum {
  current: number
  target: number
  display: string
}

const animatedStats = ref<AnimatedNum[]>([])

const trendData = ref<{ dates: string[]; views: number[]; articles: number[]; users: number[] }>({
  dates: [], views: [], articles: [], users: [],
})

const statMeta = [
  { key: 'articles', label: '文章总数', icon: Document, accent: '#6366f1', light: 'rgba(99,102,241,0.15)' },
  { key: 'users', label: '用户总数', icon: User, accent: '#10b981', light: 'rgba(16,185,129,0.15)' },
  { key: 'views', label: '访问总量', icon: View, accent: '#f59e0b', light: 'rgba(245,158,11,0.15)' },
  { key: 'comments', label: '评论总数', icon: ChatDotRound, accent: '#ef4444', light: 'rgba(239,68,68,0.15)' },
  { key: 'likes', label: '点赞总数', icon: StarFilled, accent: '#06b6d4', light: 'rgba(6,182,212,0.15)' },
  { key: 'collections', label: '收藏总数', icon: Star, accent: '#a855f7', light: 'rgba(168,85,247,0.15)' },
]

const statCards = computed(() =>
  statMeta.map((m, i) => ({
    ...m,
    total: (stats.value as any)[`total${m.key.charAt(0).toUpperCase() + m.key.slice(1)}`] || 0,
    anim: animatedStats.value[i] || { display: '0' },
  }))
)

const formatNumber = (num: number): string => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + '百万'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + '千'
  return String(num)
}

const formatCount = (num: number): string => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + '千'
  return String(num)
}

const compareData = computed(() => {
  const labels = ['文章', '用户', '访问', '评论', '点赞', '收藏']
  if (compareMode.value === 'todayVsYesterday') {
    const today = [
      stats.value.todayArticles, stats.value.todayUsers, stats.value.todayViews,
      stats.value.todayComments, stats.value.todayLikes, stats.value.todayCollections,
    ]
    const yesterday = [
      stats.value.yesterdayArticles, stats.value.yesterdayUsers, stats.value.yesterdayViews,
      stats.value.yesterdayComments, stats.value.yesterdayLikes, stats.value.yesterdayCollections,
    ]
    return { labels, base: today, compare: yesterday, baseName: '今日', compareName: '昨日' }
  }
  const yesterday = [
    stats.value.yesterdayArticles, stats.value.yesterdayUsers, stats.value.yesterdayViews,
    stats.value.yesterdayComments, stats.value.yesterdayLikes, stats.value.yesterdayCollections,
  ]
  const beforeYesterday = [
    stats.value.beforeYesterdayArticles, stats.value.beforeYesterdayUsers, stats.value.beforeYesterdayViews,
    stats.value.beforeYesterdayComments, stats.value.beforeYesterdayLikes, stats.value.beforeYesterdayCollections,
  ]
  return { labels, base: yesterday, compare: beforeYesterday, baseName: '昨日', compareName: '前天' }
})

const growthData = computed(() => {
  const labels = ['文章', '用户', '访问', '评论', '点赞', '收藏']
  let rates: number[]
  if (compareMode.value === 'todayVsYesterday') {
    const todayVals = [
      stats.value.todayArticles, stats.value.todayUsers, stats.value.todayViews,
      stats.value.todayComments, stats.value.todayLikes, stats.value.todayCollections,
    ]
    const yesterdayVals = [
      stats.value.yesterdayArticles, stats.value.yesterdayUsers, stats.value.yesterdayViews,
      stats.value.yesterdayComments, stats.value.yesterdayLikes, stats.value.yesterdayCollections,
    ]
    rates = labels.map((_, i) => {
      const t = todayVals[i]
      const y = yesterdayVals[i]
      if (y === 0) return t > 0 ? 100 : 0
      return Math.round(((t - y) / y) * 100)
    })
  } else {
    const yesterdayVals = [
      stats.value.yesterdayArticles, stats.value.yesterdayUsers, stats.value.yesterdayViews,
      stats.value.yesterdayComments, stats.value.yesterdayLikes, stats.value.yesterdayCollections,
    ]
    const beforeYesterdayVals = [
      stats.value.beforeYesterdayArticles, stats.value.beforeYesterdayUsers, stats.value.beforeYesterdayViews,
      stats.value.beforeYesterdayComments, stats.value.beforeYesterdayLikes, stats.value.beforeYesterdayCollections,
    ]
    rates = labels.map((_, i) => {
      const t = yesterdayVals[i]
      const y = beforeYesterdayVals[i]
      if (y === 0) return t > 0 ? 100 : 0
      return Math.round(((t - y) / y) * 100)
    })
  }
  return { labels, rates }
})

const buildCompareChartOption = () => {
  const { labels, base, compare, baseName, compareName } = compareData.value
  const maxVal = Math.max(...base, ...compare, 1)
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15,19,32,0.95)',
      borderColor: 'rgba(99,102,241,0.2)',
      borderWidth: 1,
      textStyle: { color: '#e2e8f0', fontSize: 12 },
    },
    legend: {
      data: [baseName, compareName],
      bottom: 8,
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    radar: {
      indicator: labels.map(name => ({ name, max: maxVal })),
      shape: 'polygon',
      splitNumber: 4,
      center: ['50%', '48%'],
      radius: '65%',
      axisName: {
        color: '#94a3b8',
        fontSize: 12,
        fontWeight: 600,
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255,255,255,0.06)',
        },
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(99,102,241,0.02)', 'rgba(99,102,241,0.04)', 'rgba(99,102,241,0.06)', 'rgba(99,102,241,0.08)'],
        },
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255,255,255,0.08)',
        },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: base,
            name: baseName,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { color: '#818cf8', width: 2 },
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(129,140,248,0.35)' },
                  { offset: 1, color: 'rgba(129,140,248,0.05)' },
                ],
              },
            },
            itemStyle: { color: '#818cf8' },
          },
          {
            value: compare,
            name: compareName,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { color: '#f59e0b', width: 2, type: 'dashed' },
            areaStyle: {
              color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(245,158,11,0.2)' },
                  { offset: 1, color: 'rgba(245,158,11,0.02)' },
                ],
              },
            },
            itemStyle: { color: '#f59e0b' },
          },
        ],
      },
    ],
  }
}

const buildGrowthChartOption = () => {
  const { labels, rates } = growthData.value
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,19,32,0.95)',
      borderColor: 'rgba(99,102,241,0.2)',
      borderWidth: 1,
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (params: any) => {
        const item = params[0]
        const rate = item.value
        const color = rate >= 0 ? '#34d399' : '#ef4444'
        return `${item.name}<br/>增长率: <span style="color:${color};font-weight:700">${rate >= 0 ? '+' : ''}${rate}%</span>`
      },
    },
    grid: { left: 0, right: 0, bottom: 40, top: 16, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '增长率(%)',
      nameTextStyle: { color: '#64748b', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.03)' } },
      axisLabel: {
        color: '#64748b',
        fontSize: 11,
        formatter: '{value}%',
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    series: [
      {
        name: '增长率',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: rates.map((rate: number) => ({
          value: rate,
          itemStyle: {
            color: rate >= 0 ? '#34d399' : '#ef4444',
          },
        })),
        lineStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#34d399' },
              { offset: 0.5, color: '#818cf8' },
              { offset: 1, color: '#f59e0b' },
            ],
          },
          width: 3,
        },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(52,211,153,0.25)' },
              { offset: 1, color: 'rgba(52,211,153,0)' },
            ],
          },
        },
        markLine: {
          silent: true,
          data: [{ yAxis: 0 }],
          lineStyle: { color: 'rgba(255,255,255,0.1)', type: 'solid', width: 1 },
          label: { show: false },
        },
      },
    ],
  }
}

const renderCompareChart = () => {
  if (!compareChartRef.value) return
  if (!compareChartInstance) {
    compareChartInstance = echarts.init(compareChartRef.value)
  }
  compareChartInstance.setOption(buildCompareChartOption(), true)
}

const renderGrowthChart = () => {
  if (!growthChartRef.value) return
  if (!growthChartInstance) {
    growthChartInstance = echarts.init(growthChartRef.value)
  }
  growthChartInstance.setOption(buildGrowthChartOption(), true)
}

const buildChartOption = () => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15,19,32,0.95)',
    borderColor: 'rgba(99,102,241,0.2)',
    borderWidth: 1,
    textStyle: { color: '#e2e8f0', fontSize: 12 },
    axisPointer: { type: 'cross', crossStyle: { color: '#374151' } },
  },
  legend: {
    data: ['访问量', '文章数', '新增用户'],
    bottom: 0,
    textStyle: { color: '#64748b', fontSize: 12 },
  },
  grid: { left: 0, right: 0, bottom: 40, top: 16, containLabel: true },
  xAxis: {
    type: 'category',
    data: trendData.value.dates,
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
    axisTick: { show: false },
    boundaryGap: false,
  },
  yAxis: [
    {
      type: 'value',
      name: '访问量',
      nameTextStyle: { color: '#64748b', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.03)' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    {
      type: 'value',
      name: '文章数',
      nameTextStyle: { color: '#64748b', fontSize: 11 },
      splitLine: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    {
      type: 'value',
      name: '新增用户',
      nameTextStyle: { color: '#64748b', fontSize: 11 },
      splitLine: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
    },
  ],
  series: [
    {
      name: '访问量',
      type: 'line',
      smooth: true,
      symbol: 'none',
      data: trendData.value.views,
      lineStyle: { color: '#818cf8', width: 3 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(99,102,241,0.35)' },
            { offset: 0.5, color: 'rgba(99,102,241,0.12)' },
            { offset: 1, color: 'rgba(99,102,241,0)' },
          ],
        },
      },
      markLine: {
        silent: true,
        data: [{ type: 'average', name: '平均' }],
        lineStyle: { color: 'rgba(99,102,241,0.25)', type: 'dashed', width: 1 },
        label: { color: '#64748b', fontSize: 10, formatter: 'avg: {c}' },
      },
    },
    {
      name: '文章数',
      type: 'bar',
      yAxisIndex: 1,
      barWidth: 20,
      barGap: '30%',
      data: trendData.value.articles.map((v: number, i: number) => ({
        value: v,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: `rgba(168, 85, 247, ${0.6 + (i / Math.max(trendData.value.articles.length, 1)) * 0.4})` },
              { offset: 1, color: `rgba(124, 58, 237, ${0.3 + (i / Math.max(trendData.value.articles.length, 1)) * 0.3})` },
            ],
          },
        },
      })),
    },
    {
      name: '新增用户',
      type: 'line',
      yAxisIndex: 2,
      smooth: true,
      symbol: 'diamond',
      symbolSize: 8,
      data: trendData.value.users,
      lineStyle: { color: '#f59e0b', width: 2.5 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(245,158,11,0.25)' },
            { offset: 1, color: 'rgba(245,158,11,0)' },
          ],
        },
      },
    },
  ],
})

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption(buildChartOption(), true)
}

const handleResize = () => {
  chartInstance?.resize()
  compareChartInstance?.resize()
  growthChartInstance?.resize()
}

watch([trendData, compareData, growthData], () => {
  nextTick(() => {
    renderChart()
    renderCompareChart()
    renderGrowthChart()
  })
}, { deep: true })

const animateCounter = () => {
  const targets = statMeta.map(m => {
    const totalKey = `total${m.key.charAt(0).toUpperCase() + m.key.slice(1)}` as keyof typeof stats.value
    return (stats.value as any)[totalKey] || 0
  })
  const duration = 500
  const startTime = performance.now()
  const startValues = targets.map(() => 0)

  const tick = (now: number) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const ease = 1 - Math.pow(1 - progress, 3)

    animatedStats.value = targets.map((target, i) => ({
      current: startValues[i],
      target,
      display: formatNumber(Math.round(target * ease)),
    }))

    if (progress < 1) {
      requestAnimationFrame(tick)
    }
  }
  requestAnimationFrame(tick)
}

const updateClock = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${h}:${m}:${s}`
  const days = ['日', '一', '二', '三', '四', '五', '六']
  currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${days[now.getDay()]}`
}

const fetchStats = async () => {
  try {
    const res: any = await getAdminHomeStats()
    if (res.data) {
      const d = res.data
      stats.value = {
        totalArticles: d.totalArticles || 0,
        totalUsers: d.totalUsers || 0,
        totalViews: d.totalViews || 0,
        totalComments: d.totalComments || 0,
        totalLikes: d.totalLikes || 0,
        totalCollections: d.totalCollections || 0,
        todayArticles: d.todayArticles || 0,
        todayUsers: d.todayUsers || 0,
        todayViews: d.todayViews || 0,
        todayComments: d.todayComments || 0,
        todayLikes: d.todayLikes || 0,
        todayCollections: d.todayCollections || 0,
        yesterdayArticles: d.yesterdayArticles || 0,
        yesterdayUsers: d.yesterdayUsers || 0,
        yesterdayViews: d.yesterdayViews || 0,
        yesterdayComments: d.yesterdayComments || 0,
        yesterdayLikes: d.yesterdayLikes || 0,
        yesterdayCollections: d.yesterdayCollections || 0,
        beforeYesterdayArticles: d.beforeYesterdayArticles || 0,
        beforeYesterdayUsers: d.beforeYesterdayUsers || 0,
        beforeYesterdayViews: d.beforeYesterdayViews || 0,
        beforeYesterdayComments: d.beforeYesterdayComments || 0,
        beforeYesterdayLikes: d.beforeYesterdayLikes || 0,
        beforeYesterdayCollections: d.beforeYesterdayCollections || 0,
      }
    }
  } catch {
    ElMessage.error('获取统计数据失败')
  }
}

const fetchTrend = async () => {
  try {
    const res: any = await getArticleTrend(trendDays.value)
    if (res.data) {
      trendData.value = {
        dates: res.data.dateList || [],
        views: res.data.viewList || [],
        articles: res.data.publishList || [],
        users: res.data.userList || [],
      }
    }
  } catch {
    ElMessage.error('获取趋势数据失败')
  }
}

const switchTrend = (days: number) => {
  trendDays.value = days
  fetchTrend()
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  updateClock()
  timeHandle = window.setInterval(updateClock, 1000)
  await Promise.all([fetchStats(), fetchTrend()])
  window.addEventListener('resize', handleResize)
  setTimeout(() => {
    loading.value = false
    animateCounter()
    nextTick(() => {
      renderChart()
      renderCompareChart()
      renderGrowthChart()
    })
  }, 400)
})

onUnmounted(() => {
  clearInterval(timeHandle)
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  compareChartInstance?.dispose()
  growthChartInstance?.dispose()
})
</script>

<template>
  <div class="dashboard" v-loading="loading" element-loading-background="rgba(8,11,20,0.85)">
    <!-- Hero Section -->
    <div class="hero">
      <div class="hero-bg">
        <div class="hero-orb orb-1" />
        <div class="hero-orb orb-2" />
        <div class="hero-orb orb-3" />
      </div>
      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-greeting">
            <span class="hero-emoji">👋</span>
            <span class="hero-greeting-text">{{ greeting }}，管理员</span>
          </div>
          <h1 class="hero-title">数据仪表盘</h1>
          <p class="hero-desc">这是你的平台实时数据概览，所有指标自动更新</p>
        </div>
        <div class="hero-right">
          <div class="hero-clock">
            <div class="clock-display">{{ currentTime }}</div>
            <div class="clock-date">{{ currentDate }}</div>
          </div>
          <div class="hero-badge">
            <span class="badge-dot" />
            系统运行中
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Cards Grid -->
    <div class="stats-grid">
      <div
        v-for="(card, idx) in statCards"
        :key="card.key"
        class="stat-card"
        :style="{
          '--accent': card.accent,
          '--light': card.light,
          '--delay': `${idx * 0.06}s`,
        }"
      >
        <div class="card-glow" />
        <div class="card-body">
          <div class="card-icon" :style="{ background: `linear-gradient(135deg, ${card.accent}, ${card.accent}dd)`, boxShadow: `0 8px 24px ${card.accent}44` }">
            <component :is="card.icon" />
          </div>
          <div class="card-info">
            <span class="card-num">{{ card.anim.display }}</span>
            <span class="card-label">{{ card.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Chart Section -->
    <div class="chart-panel">
      <div class="panel-hd">
        <div class="panel-hd-left">
          <TrendCharts class="hd-icon" />
          <span class="hd-title">趋势分析</span>
          <div class="trend-tabs">
            <button
              class="trend-tab"
              :class="{ active: trendDays === 7 }"
              @click="switchTrend(7)"
            >近7天</button>
            <button
              class="trend-tab"
              :class="{ active: trendDays === 30 }"
              @click="switchTrend(30)"
            >近30天</button>
          </div>
        </div>
      </div>
      <div class="chart-body">
        <div ref="chartRef" style="height: 300px; width: 100%;" />
      </div>
    </div>

    <!-- Bottom Charts Row -->
    <div class="bottom-charts-row">
      <!-- Radar Chart -->
      <div class="compare-panel">
        <div class="panel-hd">
          <div class="panel-hd-left">
            <svg class="hd-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
            <span class="hd-title">数据对比</span>
          </div>
          <div class="panel-hd-right">
            <div class="toggle-group">
              <button :class="['toggle-btn', { active: compareMode === 'todayVsYesterday' }]" @click="compareMode = 'todayVsYesterday'">今日 vs 昨日</button>
              <button :class="['toggle-btn', { active: compareMode === 'yesterdayVsBeforeYesterday' }]" @click="compareMode = 'yesterdayVsBeforeYesterday'">昨日 vs 前天</button>
            </div>
            <span class="hd-badge">雷达图对比</span>
          </div>
        </div>
        <div class="chart-body">
          <div ref="compareChartRef" style="height: 300px; width: 100%;" />
        </div>
      </div>

      <!-- Growth Rate Line Chart -->
      <div class="growth-panel">
        <div class="panel-hd">
          <div class="panel-hd-left">
            <TrendCharts class="hd-icon" />
            <span class="hd-title">增长对比</span>
          </div>
          <div class="panel-hd-right">
            <div class="toggle-group">
              <button :class="['toggle-btn', { active: compareMode === 'todayVsYesterday' }]" @click="compareMode = 'todayVsYesterday'">今日/昨日</button>
              <button :class="['toggle-btn', { active: compareMode === 'yesterdayVsBeforeYesterday' }]" @click="compareMode = 'yesterdayVsBeforeYesterday'">昨日/前天</button>
            </div>
            <span class="hd-badge">增长率</span>
          </div>
        </div>
        <div class="chart-body">
          <div ref="growthChartRef" style="height: 300px; width: 100%;" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px 28px;
  min-height: 100%;
}

/* ── Hero ── */
.hero {
  position: relative;
  border-radius: 20px;
  background: linear-gradient(145deg, #111827, #0a0e17);
  border: 1px solid rgba(255,255,255,0.05);
  overflow: hidden;
  margin-bottom: 24px;
}
.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(99,102,241,0.3), transparent);
  top: -120px; right: -80px;
  animation: orbFloat 12s ease-in-out infinite alternate;
}
.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(16,185,129,0.2), transparent);
  bottom: -100px; left: 10%;
  animation: orbFloat 16s ease-in-out infinite alternate-reverse;
}
.orb-3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(245,158,11,0.15), transparent);
  top: 30%; right: 30%;
  animation: orbFloat 10s ease-in-out infinite alternate;
}
@keyframes orbFloat {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(30px, -20px) scale(1.1); }
}

.hero-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 28px 32px 20px;
}
.hero-left { display: flex; flex-direction: column; gap: 8px; }
.hero-greeting {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hero-emoji { font-size: 20px; }
.hero-greeting-text {
  font-size: 15px;
  font-weight: 500;
  color: #94a3b8;
}
.hero-title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #f1f5f9;
  letter-spacing: -0.5px;
  line-height: 1.2;
}
.hero-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}
.hero-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}
.hero-clock { text-align: right; }
.clock-display {
  font-size: 32px;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 2px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.clock-date {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.hero-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #34d399;
  padding: 5px 14px;
  border-radius: 20px;
  background: rgba(16,185,129,0.1);
  border: 1px solid rgba(16,185,129,0.15);
}
.badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #34d399;
  animation: badgePulse 2s ease-in-out infinite;
}
@keyframes badgePulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(52,211,153,0.6); }
  50% { opacity: 0.6; box-shadow: 0 0 0 8px rgba(52,211,153,0); }
}

/* ── Stats Grid ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
.stat-card {
  position: relative;
  border-radius: 16px;
  background: linear-gradient(145deg, #111827, #0a0e17);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  animation: cardIn 0.6s ease-out both;
  animation-delay: var(--delay);
}
.stat-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent);
  box-shadow: 0 16px 40px rgba(0,0,0,0.5);
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 100% at 50% 0%, var(--light), transparent 70%);
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}
.stat-card:hover .card-glow { opacity: 1; }
.card-body {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 18px 14px;
}
.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
  transition: transform 0.3s;
}
.stat-card:hover .card-icon { transform: scale(1.05); }
.card-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.card-num {
  font-size: 24px;
  font-weight: 800;
  color: #f1f5f9;
  line-height: 1.1;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}
.card-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-top: 3px;
}

/* ── Chart ── */
.chart-panel {
  border-radius: 16px;
  background: linear-gradient(145deg, #111827, #0a0e17);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  margin-bottom: 24px;
  animation: panelFade 0.6s ease-out 0.3s both;
}
@keyframes panelFade {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.panel-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 0;
}
.panel-hd-left {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  flex-basis: auto;
}
.hd-icon {
  font-size: 18px;
  color: #818cf8;
}
.hd-title {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
}
.hd-right { display: flex; align-items: center; gap: 10px; }
.trend-tabs { display: flex; gap: 4px; flex-wrap: nowrap; }
.trend-tab {
  height: 28px;
  padding: 0 14px;
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
  background: rgba(255,255,255,0.03);
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.trend-tab:hover { background: rgba(255,255,255,0.06); color: #94a3b8; }
.trend-tab.active {
  background: rgba(99,102,241,0.15);
  color: #818cf8;
  border-color: rgba(99,102,241,0.25);
}
.hd-badge {
  font-size: 11px;
  color: #64748b;
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(255,255,255,0.03);
}
.panel-hd-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.toggle-group {
  display: flex;
  gap: 4px;
  background: rgba(255,255,255,0.03);
  padding: 3px;
  border-radius: 10px;
}
.toggle-btn {
  height: 26px;
  padding: 0 12px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.toggle-btn:hover { color: #94a3b8; }
.toggle-btn.active {
  background: rgba(99,102,241,0.15);
  color: #818cf8;
}
.chart-body { padding: 12px 20px 16px; }

/* ── Compare Panel (Radar Chart) ── */
.compare-panel {
  border-radius: 16px;
  background: linear-gradient(145deg, #111827, #0a0e17);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  animation: panelFade 0.6s ease-out 0.45s both;
}

/* ── Growth Panel (Line Chart) ── */
.growth-panel {
  border-radius: 16px;
  background: linear-gradient(145deg, #111827, #0a0e17);
  border: 1px solid rgba(255,255,255,0.04);
  overflow: hidden;
  animation: panelFade 0.6s ease-out 0.6s both;
}

/* ── Bottom Charts Row ── */
.bottom-charts-row {
  display: grid;
  grid-template-columns: 2fr 3fr;
  gap: 20px;
  margin-top: 24px;
}

/* ─ Responsive ── */
@media (max-width: 1400px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .dashboard { padding: 16px; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-content { flex-direction: column; gap: 16px; }
  .hero-right { align-items: flex-start; }
  .hero-title { font-size: 22px; }
}
</style>
