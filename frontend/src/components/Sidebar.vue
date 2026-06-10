<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'
import { navigateTo } from '@/utils/navigate'

interface Props {
  isCollapse: boolean
  activeMode: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  toggle: []
  modeChange: [mode: 'discover' | 'history']
}>()

const userStore = useUserStore()
const route = useRoute()

const handleToggle = () => emit('toggle')

const menuItems = [
  { id: 'discover', name: '发现', icon: 'Compass', desc: '精彩内容' },
  { id: 'history', name: '历史记录', icon: 'Clock', desc: '浏览记录' },
]

const handleMenuClick = (item: any) => {
  if (item.id === 'history') {
    if (!userStore.checkLogin('请先登录')) return
    emit('modeChange', 'history')
  } else {
    emit('modeChange', 'discover')
  }
}
</script>

<template>
  <aside class="sidebar" :class="{ collapse: isCollapse }">
    <div class="sidebar-inner">
      <!-- Logo 区域，点击折叠/展开 -->
      <div class="logo-area">
        <div class="logo-mark" @click="handleToggle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logo-svg">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div class="logo-text-group fade-content">
          <span class="logo-title">知库</span>
          <span class="logo-badge">BETA</span>
        </div>
        <!-- 折叠/展开专用按钮 -->
        <button class="collapse-btn fade-content" @click="handleToggle" title="折叠侧栏">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 4 9 12 15 20"/>
          </svg>
        </button>
      </div>

      <!-- 导航 -->
      <nav class="nav-menu">
        <div class="nav-group-label fade-content">导航</div>
        <div
          v-for="item in menuItems"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeMode === item.id }"
          @click="handleMenuClick(item)"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <div class="nav-text-group fade-content">
            <span class="nav-text">{{ item.name }}</span>
            <span class="nav-desc">{{ item.desc }}</span>
          </div>
        </div>
      </nav>

      <div class="nav-spacer" />

      <!-- 底部 -->
      <div class="nav-bottom">
        <div class="nav-item" :class="{ active: route.path === '/notices' }" @click="navigateTo('/notices')">
          <el-icon :size="20"><Bell /></el-icon>
          <div class="nav-text-group fade-content"><span class="nav-text">公告中心</span><span class="nav-desc">系统公告</span></div>
        </div>
        <div class="nav-item" :class="{ active: route.path === '/about' }" @click="navigateTo('/about')">
          <el-icon :size="20"><InfoFilled /></el-icon>
          <div class="nav-text-group fade-content"><span class="nav-text">关于我们</span><span class="nav-desc">了解知库</span></div>
        </div>
        <div class="nav-item" :class="{ active: route.path === '/join' }" @click="navigateTo('/join')">
          <el-icon :size="20"><UserFilled /></el-icon>
          <div class="nav-text-group fade-content"><span class="nav-text">加入我们</span><span class="nav-desc">成为伙伴</span></div>
        </div>
        <div class="sidebar-footer fade-content"><p>© 2026 知库平台</p></div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 240px;
  background: rgba(255,255,255,0.78);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-right: 1px solid rgba(203,213,225,0.3);
  display: flex;
  flex-direction: column;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 999;
  overflow: hidden;
  will-change: width;
}

.sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.5), transparent);
  pointer-events: none;
}

.sidebar.collapse { width: 64px; }

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 10px;
  position: relative;
  z-index: 1;
}

/* ── 文字淡入淡出 ── */
.fade-content {
  opacity: 1;
  transition: opacity 0.12s ease;
  transition-delay: 0.1s;
}

.sidebar.collapse .fade-content {
  opacity: 0;
  pointer-events: none;
  transition-delay: 0s;
}

/* ── Logo 区域 ── */
.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  gap: 10px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(226,232,240,0.5);
  margin-bottom: 6px;
}

.logo-mark {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #60a5fa, #34d399);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.3s ease;
  position: relative;
}

.logo-mark::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 13px;
  background: linear-gradient(135deg, #60a5fa, #34d399);
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}

.logo-mark:hover::after { opacity: 0.4; animation: logoPulse 2s ease-in-out infinite; }

@keyframes logoPulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.12); }
}

.logo-mark:hover { transform: scale(1.05); }

.logo-svg { width: 20px; height: 20px; color: #fff; position: relative; z-index: 1; }

.logo-text-group {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  flex: 1;
}

.logo-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #0f172a, #334155);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-badge {
  font-size: 10px;
  font-weight: 600;
  color: #3b82f6;
  background: rgba(59,130,246,0.08);
  padding: 2px 6px;
  border-radius: 4px;
}

/* ── 折叠/展开按钮 ── */
.collapse-btn {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: none;
  background: rgba(241,245,249,0.5);
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.collapse-btn svg {
  width: 14px;
  height: 14px;
  transition: transform 0.3s ease;
}

.collapse-btn:hover {
  background: rgba(59,130,246,0.08);
  color: #3b82f6;
}

/* 折叠状态：箭头朝右（展开方向） */
.sidebar.collapse .collapse-btn svg {
  transform: rotate(180deg);
}

/* ── 导航 ── */
.nav-menu { padding: 2px 0; }

.nav-group-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 10px 16px 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 16px;
  margin: 1px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  color: #64748b;
  text-decoration: none;
  position: relative;
  overflow: hidden;
}

.nav-item:hover { background: rgba(59,130,246,0.06); color: #334155; transform: translateX(2px); }

.nav-item.active { background: rgba(59,130,246,0.08); color: #3b82f6; }

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: linear-gradient(180deg, #60a5fa, #34d399);
  border-radius: 0 3px 3px 0;
}

.nav-text-group { display: flex; flex-direction: column; gap: 1px; overflow: hidden; min-width: 0; }

.nav-text { font-size: 14px; font-weight: 500; white-space: nowrap; }

.nav-desc { font-size: 11px; color: #94a3b8; white-space: nowrap; }

.nav-item.active .nav-desc { color: #60a5fa; }

.nav-spacer { flex: 1; }

.nav-bottom { border-top: 1px solid rgba(226,232,240,0.5); padding: 4px 0; flex-shrink: 0; }

.sidebar-footer { padding: 10px 16px; }
.sidebar-footer p { font-size: 11px; color: #94a3b8; text-align: center; }

@media (max-width: 1100px) {
  .sidebar { display: none; }
}
</style>
