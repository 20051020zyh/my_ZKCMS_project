<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAdminHomeStats, getUserInfo } from '@/api/user'
import { getMyPermissionPaths } from '@/api/user'
import { ElMessage } from 'element-plus'
import {
  Document,
  Folder,
  ChatDotRound,
  User,
  Bell,
  Avatar,
  Setting,
  SwitchButton,
  Fold,
  Expand,
  DataBoard,
  Monitor,
  Key,
  WarningFilled,
  ChatDotSquare,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const myPermissions = ref<string[]>([])
const noPermissionVisible = ref(false)
const noPermissionMenuTitle = ref('')

const menuItems = [
  { index: '/admin/dashboard', title: '控制台', icon: DataBoard, permKey: '' },
  { index: '/admin/articles', title: '文章管理', icon: Document, permKey: '/article/add' },
  { index: '/admin/categories', title: '分类管理', icon: Folder, permKey: 'category/add' },
  { index: '/admin/comments', title: '评论管理', icon: ChatDotRound, permKey: '/article/comment/audit' },
  { index: '/admin/reports', title: '举报管理', icon: WarningFilled, permKey: '' },
  { index: '/admin/leaveMessages', title: '留言管理', icon: ChatDotSquare, permKey: '' },
  { index: '/admin/users', title: '用户管理', icon: User, permKey: 'user/alluserPage' },
  { index: '/admin/notices', title: '公告管理', icon: Bell, permKey: 'sysNotice/adimin/list' },
  { index: '/admin/roles', title: '角色权限', icon: Avatar, permKey: '/sysRole/list' },
  { index: '/admin/permissions', title: '权限菜单', icon: Key, permKey: '/sysPermission/list' },
  { index: '/admin/settings', title: '系统设置', icon: Setting, permKey: '/sysConfig/get' },
]

const activeMenu = computed(() => route.path)

const handleMenuSelect = async (index: string) => {
  const item = menuItems.find(m => m.index === index)
  if (!item) { 
    router.push(index)
    return 
  }
  if (!item.permKey) { 
    router.push(index)
    return 
  }
  if (myPermissions.value.length === 0) {
    try {
      const res: any = await getMyPermissionPaths()
      myPermissions.value = res.data || []
    } catch {
      noPermissionMenuTitle.value = item.title
      noPermissionVisible.value = true
      return
    }
  }
  const isSuperAdmin = myPermissions.value.includes('*:*:*')
  if (isSuperAdmin || myPermissions.value.includes(item.permKey)) {
    router.push(index)
  } else {
    noPermissionMenuTitle.value = item.title
    noPermissionVisible.value = true
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}

onMounted(async () => {
  try {
    const userRes: any = await getUserInfo()
    if (userRes.data) {
      userStore.setUserInfo(userRes.data)
    }
  } catch {}
  try {
    await getAdminHomeStats()
  } catch {
  }
  try {
    const res: any = await getMyPermissionPaths()
    myPermissions.value = res.data || []
  } catch {
    myPermissions.value = []
  }
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-aside" :class="{ collapsed: isCollapse }">
      <div class="aside-inner">
          <div class="logo-area">
            <div class="logo-mark">
              <svg viewBox="0 0 32 32" fill="none">
                <rect width="32" height="32" rx="8" fill="url(#logo-grad)"/>
                <path d="M9 20l5-12 4 8 3-6 2 10" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                <defs>
                  <linearGradient id="logo-grad" x1="0" y1="0" x2="32" y2="32">
                    <stop offset="0%" stop-color="#6366f1"/>
                    <stop offset="100%" stop-color="#8b5cf6"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <transition name="fade-slide">
              <span v-show="!isCollapse" class="logo-text">知库后台管理系统</span>
            </transition>
          </div>

          <nav class="nav-menu">
            <div
              v-for="item in menuItems"
              :key="item.index"
              class="nav-item"
              :class="{ active: activeMenu === item.index }"
              @click="handleMenuSelect(item.index)"
            >
              <span class="nav-icon">
                <component :is="item.icon" />
              </span>
              <transition name="fade-slide">
                <span v-show="!isCollapse" class="nav-label">{{ item.title }}</span>
              </transition>
              <span v-if="activeMenu === item.index && isCollapse" class="nav-dot"></span>
            </div>
          </nav>
        </div>

        <div class="aside-footer">
          <div class="collapse-trigger" @click="isCollapse = !isCollapse">
            <component :is="isCollapse ? Expand : Fold" />
          </div>
        </div>
      </aside>

      <div class="main-area">
        <header class="admin-header">
          <div class="header-left">
            <div class="breadcrumb">
              <span class="bc-item bc-root" @click="router.push('/admin/dashboard')">
                <Monitor />
              </span>
              <span class="bc-sep">/</span>
              <span class="bc-item bc-current">
                {{ menuItems.find(m => m.index === route.path)?.title || '页面' }}
              </span>
            </div>
          </div>

          <div class="header-right">
            <el-dropdown trigger="hover" popper-class="admin-user-drop">
              <div class="user-info">
                <div class="user-avatar">
                  <img v-if="userStore.userInfo?.userPic" :src="userStore.userInfo.userPic" alt="" />
                  <span v-else>{{ (userStore.userInfo?.nickname || 'A').charAt(0) }}</span>
                </div>
                <div class="user-meta">
                  <span class="user-role">超级管理员</span>
                </div>
                <svg class="user-chevron" viewBox="0 0 16 16" fill="none">
                  <path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleLogout">
                    <SwitchButton style="margin-right:8px;" />退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <main class="admin-main">
          <div class="page-transition-wrapper">
            <router-view v-slot="{ Component }">
              <transition name="page-fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </div>
        </main>
      </div>
    </div>

  <!-- 无权限弹窗 -->
  <Teleport to="body">
    <div v-if="noPermissionVisible" class="modal-overlay" @click.self="noPermissionVisible = false">
      <div class="modal-container">
        <div class="modal-glass">
          <div class="modal-header">
            <div class="modal-hd-icon no-perm-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </div>
            <div class="modal-hd-text">
              <h3 class="modal-title">权限不足</h3>
              <p class="modal-desc">您没有「<strong style="color:#f87171">{{ noPermissionMenuTitle }}</strong>」的访问权限</p>
            </div>
            <button class="modal-close" @click="noPermissionVisible = false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="no-perm-content">
              <svg class="no-perm-lock" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f87171" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/><circle cx="12" cy="16" r="1.5" fill="#f87171"/><line x1="12" y1="16" x2="12" y2="13" stroke="#f87171" stroke-width="1.5" stroke-linecap="round"/></svg>
              <p class="no-perm-text">您当前没有该功能的操作权限</p>
              <p class="no-perm-hint">如需获取权限，请联系管理员</p>
            </div>
          </div>
          <div class="modal-footer">
            <button class="m-btn m-btn-primary" @click="noPermissionVisible = false">我知道了</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #080b14;
  overflow: hidden;
}

/* ====== 无权限页 ====== */
.no-permission-overlay {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #080b14;
}
.no-permission-card {
  text-align: center;
  padding: 56px 48px;
  background: linear-gradient(145deg, #111827, #0f1119);
  border: 1px solid rgba(99,102,241,0.15);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5), inset 0 1px 0 rgba(255,255,255,0.03);
}
.np-icon-ring {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #818cf8;
}
.np-icon-ring svg {
  width: 32px;
  height: 32px;
}
.np-title {
  font-size: 22px;
  font-weight: 700;
  color: #e2e8f0;
  margin: 0 0 8px;
  letter-spacing: -0.3px;
}
.np-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px;
}
.np-progress {
  height: 3px;
  background: rgba(255,255,255,0.06);
  border-radius: 3px;
  overflow: hidden;
  width: 200px;
  margin: 0 auto;
}
.np-bar {
  display: block;
  height: 100%;
  width: 100%;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  border-radius: 3px;
  animation: np-shrink 1.5s ease-in-out forwards;
}
@keyframes np-shrink {
  0% { width: 100%; }
  100% { width: 0%; }
}

/* ====== 侧边栏 ====== */
.admin-aside {
  width: 240px;
  min-width: 240px;
  background: linear-gradient(180deg, #0b0f1a 0%, #0d1120 100%);
  border-right: 1px solid rgba(255,255,255,0.05);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4,0,0.2,1), min-width 0.3s cubic-bezier(0.4,0,0.2,1);
  position: relative;
  z-index: 10;
}
.admin-aside.collapsed {
  width: 72px;
  min-width: 72px;
}
.aside-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px 12px 12px;
  overflow: hidden;
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px 20px;
  margin-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.logo-mark {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
}
.logo-mark svg {
  width: 100%;
  height: 100%;
}
.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: -0.3px;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  color: #94a3b8;
}
.nav-item:hover {
  background: rgba(99,102,241,0.08);
  color: #c7d2fe;
}
.nav-item.active {
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(99,102,241,0.08));
  color: #e2e8f0;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.04);
}
.nav-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}
.nav-label {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}
.nav-dot {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #818cf8;
  box-shadow: 0 0 8px rgba(129,140,248,0.6);
}
.aside-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex;
  justify-content: center;
}
.collapse-trigger {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s ease;
  font-size: 18px;
}
.collapse-trigger:hover {
  background: rgba(99,102,241,0.1);
  color: #818cf8;
}

/* ====== 主区域 ====== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #080b14;
}

/* ====== 顶部栏 ====== */
.admin-header {
  height: 60px;
  min-height: 60px;
  background: linear-gradient(180deg, #0b0f1a, #0a0d18);
  border-bottom: 1px solid rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 5;
}
.header-left {
  display: flex;
  align-items: center;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.bc-item {
  color: #64748b;
}
.bc-root {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;
}
.bc-root:hover {
  color: #818cf8;
}
.bc-sep {
  color: rgba(255,255,255,0.1);
}
.bc-current {
  color: #cbd5e1;
  font-weight: 500;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s ease;
  border: 1px solid rgba(255,255,255,0.06);
}
.header-btn:hover {
  background: rgba(99,102,241,0.1);
  color: #c7d2fe;
  border-color: rgba(99,102,241,0.2);
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 10px;
  transition: background 0.2s;
}
.user-info:hover {
  background: rgba(255,255,255,0.03);
}
.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  flex-shrink: 0;
}
.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.user-role {
  font-size: 13px;
  font-weight: 600;
  color: #c7d2fe;
}
.user-chevron {
  width: 14px;
  height: 14px;
  color: #64748b;
  flex-shrink: 0;
}

/* ====== 主内容区 ====== */
.admin-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ====== 页面过渡 ====== */
.page-transition-wrapper {
  background: #080b14;
  min-height: 100%;
}

.page-fade-leave-active {
  transition: opacity 0.25s cubic-bezier(0.55, 0, 1, 0.45), transform 0.25s cubic-bezier(0.55, 0, 1, 0.45);
}

.page-fade-enter-active {
  transition: opacity 0.4s cubic-bezier(0.22, 1, 0.36, 1), transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}

.page-fade-enter-from {
  opacity: 0;
  transform: scale(0.96) translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: scale(0.97) translateY(-6px);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease, max-width 0.2s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-4px);
}

/* ====== 全局滚动条 ====== */
.admin-main::-webkit-scrollbar {
  width: 6px;
}
.admin-main::-webkit-scrollbar-track {
  background: transparent;
}
.admin-main::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.06);
  border-radius: 3px;
}
.admin-main::-webkit-scrollbar-thumb:hover {
  background: rgba(255,255,255,0.1);
}

/* ====== 无权限弹窗 ====== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: overlayIn 0.3s ease;
}
@keyframes overlayIn {
  from { opacity: 0; backdrop-filter: blur(0); -webkit-backdrop-filter: blur(0); }
  to { opacity: 1; backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
}
.modal-container {
  position: relative;
  width: 440px;
  max-height: 90vh;
  animation: modalFloat 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes modalFloat {
  from { opacity: 0; transform: scale(0.92) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.modal-glass {
  position: relative;
  background: linear-gradient(170deg, rgba(17,24,39,0.97), rgba(15,18,30,0.98));
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.6), inset 0 1px 0 rgba(255,255,255,0.04);
  overflow: hidden;
}
.modal-header {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.modal-hd-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.modal-hd-icon.no-perm-icon {
  background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05));
  color: #f87171;
}
.modal-hd-text { flex: 1; min-width: 0; }
.modal-title { margin: 0; font-size: 17px; font-weight: 700; color: #f1f5f9; letter-spacing: -0.3px; }
.modal-desc { margin: 3px 0 0; font-size: 13px; color: #64748b; line-height: 1.5; }
.modal-close {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: rgba(255,255,255,0.03); color: #64748b;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all 0.2s;
}
.modal-close:hover { background: rgba(239,68,68,0.12); color: #f87171; }
.modal-body {
  padding: 24px 28px 12px;
  display: flex; flex-direction: column; gap: 16px;
}
.no-perm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}
.no-perm-lock {
  animation: lockShake 2s ease-in-out infinite;
}
@keyframes lockShake {
  0%, 100% { transform: translateY(0); }
  5% { transform: translateY(-4px); }
  10% { transform: translateY(0); }
}
.no-perm-text {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  text-align: center;
}
.no-perm-hint {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  text-align: center;
}
.modal-footer {
  display: flex; justify-content: center;
  padding: 16px 28px 24px;
  border-top: 1px solid rgba(255,255,255,0.04);
}
.m-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 28px; border-radius: 10px;
  font-size: 14px; font-weight: 600; border: none;
  cursor: pointer; transition: all 0.25s ease;
  position: relative; overflow: hidden;
}
.m-btn-primary { background: linear-gradient(135deg, #6366f1, #7c3aed); color: #fff; box-shadow: 0 4px 16px rgba(99,102,241,0.3); }
.m-btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(99,102,241,0.45); }
.m-btn-primary:active { transform: translateY(0) scale(0.98); }
</style>

<style>
/* ── 下拉菜单暗色主题 ── */
.admin-user-drop.el-popper,
.admin-user-drop.el-popper[data-popper-placement^=bottom],
.admin-user-drop.el-popper[data-popper-placement],
.admin-user-drop.el-popper.el-dropdown__popper {
  background: #1a1d2e !important;
  background-color: #1a1d2e !important;
  border: 1px solid rgba(255,255,255,0.06) !important;
  border-radius: 10px !important;
  padding: 0 !important;
  box-shadow: 0 12px 40px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.03) !important;
}

.admin-user-drop .el-dropdown-menu,
.admin-user-drop .el-dropdown-menu--default,
.admin-user-drop .el-dropdown-menu.el-dropdown-menu--default {
  background: transparent !important;
  background-color: transparent !important;
  border: none !important;
  padding: 4px !important;
  margin: 0 !important;
}

.admin-user-drop .el-dropdown-menu__item {
  background: transparent !important;
  background-color: transparent !important;
  font-size: 13px !important;
  padding: 10px 16px !important;
  border-radius: 8px !important;
  color: #c7d2fe !important;
  transition: all 0.2s ease !important;
}

.admin-user-drop .el-dropdown-menu__item:hover {
  background: rgba(99,102,241,0.15) !important;
  background-color: rgba(99,102,241,0.15) !important;
  color: #e0e7ff !important;
}

.admin-user-drop .el-dropdown-menu__item.is-selected {
  background: rgba(99,102,241,0.2) !important;
  background-color: rgba(99,102,241,0.2) !important;
  color: #e0e7ff !important;
}

.admin-user-drop .el-popper__arrow,
.admin-user-drop .popper__arrow {
  display: none !important;
}
</style>