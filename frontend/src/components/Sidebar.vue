<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'
import { navigateTo } from '@/utils/navigate'

interface Props {
  isCollapse: boolean
  categories: any[]
  selectedCategory: number | null
  activeMode: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  toggle: []
  categorySelect: [categoryId: number | null]
  modeChange: [mode: 'discover' | 'history']
}>()

const userStore = useUserStore()
const route = useRoute()

const handleToggle = () => {
  emit('toggle')
}

const handleCategoryClick = (categoryId: number | null) => {
  emit('categorySelect', categoryId)
}

const menuItems = [
  { id: 'discover', name: '发现', icon: 'Compass', desc: '精彩内容' },
  { id: 'history', name: '历史记录', icon: 'Clock', desc: '浏览记录' },
]

const handleNavClick = (path: string) => {
  navigateTo(path)
}

const handleCreateArticle = () => {
  if (!userStore.checkLogin('请先登录以发布文章')) return
  navigateTo('/article/create')
}

const handleMenuClick = (item: any) => {
  if (item.id === 'history') {
    if (!userStore.checkLogin('请先登录')) return
    emit('modeChange', 'history')
  } else {
    emit('categorySelect', null)
    emit('modeChange', 'discover')
  }
}
</script>

<template>
  <aside class="sidebar" :class="{ collapse: isCollapse }">
    <div class="sidebar-inner">
      <div class="logo-area">
        <div class="logo-icon" @click="handleToggle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logo-svg">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="logo-text-group">
            <span class="logo-title">知库</span>
            <span class="logo-badge">BETA</span>
          </div>
        </transition>
      </div>

      <nav class="nav-menu">
        <div class="nav-group-label" v-show="!isCollapse">导航</div>
        <div
          v-for="item in menuItems"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeMode === item.id }"
          @click="handleMenuClick(item)"
        >
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <transition name="fade">
            <div v-show="!isCollapse" class="nav-text-group">
              <span class="nav-text">{{ item.name }}</span>
              <span class="nav-desc">{{ item.desc }}</span>
            </div>
          </transition>
        </div>
      </nav>

      <div class="create-btn-wrap">
        <div class="create-btn" @click="handleCreateArticle">
          <svg class="create-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <transition name="fade">
            <span v-show="!isCollapse">写文章</span>
          </transition>
        </div>
      </div>

      <div class="nav-spacer" />

      <div class="nav-middle">
        <div
          class="nav-item"
          :class="{ active: route.path === '/notices' }"
          @click="handleNavClick('/notices')"
        >
          <el-icon :size="20"><Bell /></el-icon>
          <transition name="fade">
            <div v-show="!isCollapse" class="nav-text-group">
              <span class="nav-text">公告中心</span>
              <span class="nav-desc">系统公告</span>
            </div>
          </transition>
        </div>
      </div>

      <div class="nav-bottom">
        <div
          class="nav-item"
          :class="{ active: route.path === '/about' }"
          @click="handleNavClick('/about')"
        >
          <el-icon :size="20"><InfoFilled /></el-icon>
          <transition name="fade">
            <div v-show="!isCollapse" class="nav-text-group">
              <span class="nav-text">关于我们</span>
              <span class="nav-desc">了解知库</span>
            </div>
          </transition>
        </div>
        <div
          class="nav-item"
          :class="{ active: route.path === '/join' }"
          @click="handleNavClick('/join')"
        >
          <el-icon :size="20"><UserFilled /></el-icon>
          <transition name="fade">
            <div v-show="!isCollapse" class="nav-text-group">
              <span class="nav-text">加入我们</span>
              <span class="nav-desc">成为伙伴</span>
            </div>
          </transition>
        </div>

        <div v-show="!isCollapse" class="sidebar-footer">
          <p>© 2026 知库平台</p>
        </div>
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
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba(226,232,240,0.5);
  display: flex;
  flex-direction: column;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
  overflow: hidden;
  box-shadow: 2px 0 20px rgba(0,0,0,0.04);
}

.sidebar.collapse {
  width: 64px;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 8px;
  position: relative;
  z-index: 1;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 12px;
  flex-shrink: 0;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 8px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.3s ease;
  position: relative;
}

.logo-icon::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 13px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}

.logo-icon:hover::after {
  opacity: 0.5;
  animation: logoPulse 2s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.15); }
}

.logo-icon:hover {
  transform: scale(1.05);
}

.logo-svg {
  width: 20px;
  height: 20px;
  color: #fff;
  position: relative;
  z-index: 1;
}

.logo-text-group {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.logo-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.5px;
}

.logo-badge {
  font-size: 10px;
  font-weight: 600;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  letter-spacing: 0.5px;
  animation: badgeGlow 3s ease-in-out infinite;
}

@keyframes badgeGlow {
  0%, 100% { background: rgba(99, 102, 241, 0.1); }
  50% { background: rgba(99, 102, 241, 0.2); }
}

.nav-menu {
  padding: 4px 0;
}

.nav-group-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 12px 16px 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin: 2px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  color: #64748b;
  text-decoration: none;
  position: relative;
  overflow: hidden;
}

.nav-item:hover {
  background: #f1f5f9;
  color: #334155;
  transform: translateX(2px);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.06));
  color: #6366f1;
  box-shadow: 0 0 0 1px rgba(99,102,241,0.1);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: linear-gradient(180deg, #6366f1, #8b5cf6);
  border-radius: 0 3px 3px 0;
}

.nav-text-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  min-width: 0;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.nav-desc {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.nav-item.active .nav-desc {
  color: #818cf8;
}

/* ── 写文章按钮 ── */
.create-btn-wrap {
  padding: 6px 12px 8px;
}

.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  position: relative;
  overflow: hidden;
  letter-spacing: 0.2px;
  box-shadow: 0 2px 8px rgba(99,102,241,0.2);
}

.create-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.create-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99,102,241,0.3);
}

.create-btn:hover::before {
  transform: translateX(100%);
}

.create-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(99,102,241,0.2);
}

.create-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.create-btn span {
  position: relative;
  z-index: 1;
}

.nav-spacer {
  flex: 1;
}

.nav-middle {
  padding: 4px 0;
  border-top: 1px solid #f1f5f9;
  margin-top: 4px;
}

.nav-bottom {
  border-top: 1px solid #f1f5f9;
  padding: 8px 0;
}

.sidebar-footer {
  padding: 12px 16px;
}

.sidebar-footer p {
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>