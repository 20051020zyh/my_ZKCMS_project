<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RouterView } from 'vue-router'
import { getPopInfo } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const maintenanceMode = ref(false)

const handleMmLgn = () => {
  maintenanceMode.value = false
  router.push('/login')
}

const showNoticeDialog = async (notice: any) => {
  const time = notice.publishTime || notice.createTime || ''
  const formattedTime = time ? new Date(time.replace(/-/g, '/')).toLocaleString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
  const { ElMessageBox } = await import('element-plus')
  ElMessageBox.alert(
    `<div class="np">
      <div class="np-ico">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/>
          <path d="M13.73 21a2 2 0 01-3.46 0"/>
        </svg>
      </div>
      <div class="np-label">公告</div>
      <div class="np-bd">
        <h2 class="np-tt">${notice.title || ''}</h2>
        <div class="np-tm">${formattedTime}</div>
        <div class="np-cnt">${(notice.content || '').replace(/\n/g, '<br>')}</div>
      </div>
    </div>`,
    '',
    {
      confirmButtonText: '我知道了',
      customClass: 'np-box',
      showClose: true,
      closeOnClickModal: false,
      closeOnPressEscape: true,
      icon: undefined,
      dangerouslyUseHTMLString: true
    }
  )
}

const checkMaintenance = async () => {
  if (route.path.startsWith('/admin') || route.path === '/login' || route.path === '/register') {
    maintenanceMode.value = false
    return
  }
  try {
    const res: any = await getPopInfo()
    if (res.data?.type === 'MAINTENANCE') {
      maintenanceMode.value = true
    } else if (res.data?.type === 'NOTICE' && res.data?.notice) {
      maintenanceMode.value = false
      const noticeId = res.data.notice.id
      const shown = localStorage.getItem('notice_shown_' + noticeId)
      if (!shown) {
        localStorage.setItem('notice_shown_' + noticeId, '1')
        showNoticeDialog(res.data.notice)
      }
    } else {
      maintenanceMode.value = false
    }
  } catch (err: any) {
    if (err?.message?.includes('维护')) {
      maintenanceMode.value = true
    } else {
      maintenanceMode.value = false
    }
  }
}

watch(() => route.path, () => {
  checkMaintenance()
}, { immediate: true })
</script>

<template>
  <router-view />

  <!-- 维护模式全页遮罩 -->
  <Transition name="mm">
    <div v-if="maintenanceMode" class="mm-overlay">
      <div class="mm-bg">
        <div class="mm-grid"></div>
        <div class="mm-glow mm-glow--1"></div>
        <div class="mm-glow mm-glow--2"></div>
      </div>
      <div class="mm-body">
        <div class="mm-icon-wrap">
          <div class="mm-icon-ring">
            <svg class="mm-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
            </svg>
          </div>
          <div class="mm-pulse"></div>
        </div>
        <h1 class="mm-title">系统维护中</h1>
        <p class="mm-desc">我们正在进行系统升级维护<br/>暂时无法访问，请稍后再来</p>
        <div class="mm-badge">
          <span class="mm-badge-dot"></span>
          维护进行中
        </div>
        <button class="mm-btn" @click="handleMmLgn">
          确 定
        </button>
      </div>
    </div>
  </Transition>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  background: #080b14;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1e293b;
  line-height: 1.6;
}

#app {
  min-height: 100vh;
  background: #080b14;
}

/* ── Maintenance Overlay ── */
.mm-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.mm-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #0b0d19 0%, #0f1120 30%, #0d0f1e 60%, #0b0d19 100%);
}

.mm-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99,102,241,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99,102,241,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at 50% 45%, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at 50% 45%, black 30%, transparent 70%);
}

.mm-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.mm-glow--1 {
  width: 500px;
  height: 500px;
  top: -100px;
  left: 50%;
  transform: translateX(-50%);
  background: radial-gradient(circle, rgba(99,102,241,0.08), transparent 70%);
  animation: mm-glow-float 8s ease-in-out infinite alternate;
}

.mm-glow--2 {
  width: 400px;
  height: 400px;
  bottom: -80px;
  right: -60px;
  background: radial-gradient(circle, rgba(139,92,246,0.06), transparent 70%);
  animation: mm-glow-float 10s ease-in-out infinite alternate-reverse;
}

@keyframes mm-glow-float {
  from { transform: translateX(-50%) translateY(0); }
  to { transform: translateX(-50%) translateY(-30px); }
}

.mm-body {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  z-index: 1;
  animation: mm-body-in 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes mm-body-in {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.mm-icon-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mm-icon-ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(139,92,246,0.08));
  border: 1.5px solid rgba(99,102,241,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(165,180,252,0.8);
  z-index: 1;
  animation: mm-ring-in 0.6s 0.15s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.mm-icon {
  width: 40px;
  height: 40px;
  animation: mm-icon-spin 12s linear infinite;
}

@keyframes mm-icon-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes mm-ring-in {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.mm-pulse {
  position: absolute;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 1.5px solid rgba(99,102,241,0.1);
  animation: mm-pulse 2.5s ease-out infinite;
}

@keyframes mm-pulse {
  from { transform: scale(1); opacity: 0.6; }
  to { transform: scale(1.6); opacity: 0; }
}

.mm-title {
  font-size: 32px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.5px;
  margin: 0;
  animation: mm-fade-up 0.6s 0.3s both;
}

.mm-desc {
  font-size: 16px;
  color: rgba(148,163,184,0.7);
  line-height: 1.8;
  text-align: center;
  margin: 0;
  animation: mm-fade-up 0.6s 0.45s both;
}

.mm-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 100px;
  background: rgba(99,102,241,0.08);
  border: 1px solid rgba(99,102,241,0.15);
  color: #a5b4fc;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  animation: mm-fade-up 0.6s 0.6s both;
}

.mm-badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #818cf8;
  animation: mm-dot-blink 1.5s ease-in-out infinite;
}

.mm-btn {
  margin-top: 8px;
  height: 48px;
  padding: 0 48px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
  animation: mm-fade-up 0.6s 0.75s both;
}

.mm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(99, 102, 241, 0.45);
}

.mm-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
}

@keyframes mm-dot-blink {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

@keyframes mm-fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Transition ── */
.mm-enter-active { transition: opacity 0.4s ease; }
.mm-leave-active { transition: opacity 0.3s ease; }
.mm-enter-from,
.mm-leave-to { opacity: 0; }

/* ── ElMessageBox 自定义弹窗 ── */
.login-prompt-box {
  border-radius: 20px !important;
  padding: 0 !important;
  border: 1px solid rgba(255,255,255,0.25) !important;
  box-shadow:
    0 0 0 1px rgba(99, 102, 241, 0.08),
    0 4px 16px rgba(0,0,0,0.04),
    0 24px 48px rgba(0,0,0,0.08),
    0 48px 80px rgba(99, 102, 241, 0.08) !important;
  width: 380px !important;
  overflow: visible !important;
  animation: promptFloatIn 0.35s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

@keyframes promptFloatIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(16px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.login-prompt-box .el-message-box__header {
  display: none;
}

.login-prompt-box .el-message-box__content {
  padding: 0 !important;
}

.login-prompt-box .el-message-box__container {
  position: static !important;
  padding: 0 !important;
  display: flex !important;
  justify-content: center !important;
}

.login-prompt-box .el-message-box__message {
  margin: 0 !important;
  padding: 0 !important;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  flex: none !important;
}

.login-prompt-box .prompt-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #eef2ff 0%, #ede9fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.login-prompt-box .prompt-icon::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.04));
  z-index: -1;
}

.login-prompt-box .prompt-icon svg {
  width: 36px;
  height: 36px;
  color: #6366f1;
}

.login-prompt-box .prompt-title {
  font-size: 19px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.3px;
}

.login-prompt-box .prompt-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

.login-prompt-box .prompt-body {
  padding: 40px 36px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.login-prompt-box .el-message-box__btns {
  padding: 0 36px 36px !important;
  display: flex !important;
  gap: 10px !important;
  justify-content: stretch !important;
  flex-wrap: nowrap !important;
}

.login-prompt-box .el-message-box__btns .el-button {
  flex: 1;
  height: 44px !important;
  border-radius: 12px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  font-family: inherit !important;
  margin: 0 !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.login-prompt-box .el-message-box__btns .el-button--primary {
  border: none !important;
  background: linear-gradient(135deg, #6366f1, #4f46e5) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3) !important;
}

.login-prompt-box .el-message-box__btns .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35) !important;
}

.login-prompt-box .el-message-box__btns .el-button--primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25) !important;
}

.login-prompt-box .el-message-box__btns .el-button--default {
  border: 1.5px solid #e2e8f0 !important;
  background: transparent !important;
  color: #64748b !important;
}

.login-prompt-box .el-message-box__btns .el-button--default:hover {
  border-color: #cbd5e1 !important;
  background: #f8fafc !important;
  color: #334155 !important;
}

.el-message-box__wrapper {
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  animation: overlayFadeIn 0.2s ease !important;
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

::selection {
  background: #6366f1;
  color: #fff;
}

.el-pagination {
  --el-pagination-hover-color: #6366f1;
  --el-pagination-button-color: #64748b;
  --el-pagination-button-bg-color: transparent;
}

.el-pagination button.is-active {
  --el-pagination-button-color: #6366f1 !important;
  font-weight: 600;
}

.el-skeleton__item {
  --el-skeleton-color: #e2e8f0;
  --el-skeleton-to-color: #f1f5f9;
}

/* ── 公告弹窗 ── */
.np-box {
  padding: 0 !important;
  border: none !important;
  border-radius: 20px !important;
  width: 460px !important;
  max-height: 80vh !important;
  overflow: hidden !important;
  background: #fff !important;
  box-shadow:
    0 4px 20px rgba(59, 130, 246, 0.06),
    0 12px 40px rgba(59, 130, 246, 0.08) !important;
  animation: npFadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

@keyframes npFadeIn {
  from { opacity: 0; transform: scale(0.95) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.np-box .el-message-box__header {
  display: none !important;
}

.np-box .el-message-box__content {
  padding: 0 !important;
}

.np-box .el-message-box__container {
  padding: 0 !important;
  display: flex !important;
}

.np-box .el-message-box__message {
  margin: 0 !important;
  padding: 0 !important;
  width: 100%;
}

.np-box .el-message-box__btns {
  padding: 0 28px 24px !important;
  display: flex !important;
  justify-content: center !important;
}

.np-box .el-message-box__btns .el-button {
  height: 40px !important;
  padding: 0 28px !important;
  border-radius: 10px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  font-family: inherit !important;
  border: none !important;
  background: #eff6ff !important;
  color: #3b82f6 !important;
  margin: 0 !important;
  transition: all 0.2s ease !important;
}

.np-box .el-message-box__btns .el-button:hover {
  background: #dbeafe !important;
  color: #2563eb !important;
}

.np-box .el-message-box__close {
  position: absolute !important;
  top: 14px !important;
  right: 14px !important;
  width: 26px !important;
  height: 26px !important;
  border-radius: 8px !important;
  color: #94a3b8 !important;
  font-size: 14px !important;
  transition: all 0.2s ease !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.np-box .el-message-box__close:hover {
  background: #f1f5f9 !important;
  color: #64748b !important;
}

.np {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 28px 20px;
}

.np-ico {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  margin-bottom: 12px;
}

.np-label {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 4px;
  margin-bottom: 20px;
}

.np-bd {
  width: 100%;
  text-align: left;
  border-top: 1px solid #f1f5f9;
  padding-top: 16px;
}

.np-tt {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: -0.3px;
  line-height: 1.35;
  margin: 0 0 6px;
}

.np-tm {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 14px;
}

.np-cnt {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
  max-height: 280px;
  overflow-y: auto;
  word-break: break-word;
}

.np-cnt::-webkit-scrollbar {
  width: 4px;
}

.np-cnt::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}

/* ── 下拉菜单暗色主题（全局强制） ── */
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

/* ── Global Confirm Modal Styles (used by CommentManage & ReportManage) ── */
.fx-confirm-box {
  padding: 0 !important;
  border: none !important;
  border-radius: 20px !important;
  width: 400px !important;
  background: linear-gradient(160deg, #1a1f2e 0%, #0d1117 100%) !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.05) !important;
  animation: fxCIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  overflow: hidden !important;
}
@keyframes fxCIn {
  from { opacity: 0; transform: scale(0.85) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.fx-confirm-box .el-message-box__header { display: none !important; }
.fx-confirm-box .el-message-box__content { padding: 0 !important; }
.fx-confirm-box .el-message-box__container { padding: 0 !important; display: flex !important; flex-direction: column !important; position: static !important; }
.fx-confirm-box .el-message-box__message { margin: 0 !important; padding: 0 !important; flex: none !important; }
.fx-confirm-box .el-message-box__btns {
  padding: 0 28px 28px !important;
  display: flex !important;
  gap: 12px !important;
  justify-content: center !important;
  border-top: 1px solid rgba(255,255,255,0.06) !important;
  margin-top: 0 !important;
}
.fx-confirm-box .el-message-box__btns .el-button {
  height: 44px !important;
  padding: 0 28px !important;
  border-radius: 12px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  font-family: inherit !important;
  margin: 0 !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
  flex: 1 !important;
  border: none !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  letter-spacing: 0.3px !important;
}
.fx-confirm-box .el-message-box__btns .el-button--default { background: rgba(255,255,255,0.06) !important; color: #94a3b8 !important; border: 1px solid rgba(255,255,255,0.1) !important; }
.fx-confirm-box .el-message-box__btns .el-button--default:hover { background: rgba(255,255,255,0.1) !important; color: #e2e8f0 !important; transform: translateY(-1px) !important; }
.fx-c-btn-pass { background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important; color: #fff !important; box-shadow: 0 4px 15px rgba(16,185,129,0.35) !important; }
.fx-c-btn-pass:hover { transform: translateY(-2px) !important; box-shadow: 0 8px 25px rgba(16,185,129,0.45) !important; }
.fx-c-btn-reject { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%) !important; color: #fff !important; box-shadow: 0 4px 15px rgba(245,158,11,0.35) !important; }
.fx-c-btn-reject:hover { transform: translateY(-2px) !important; box-shadow: 0 8px 25px rgba(245,158,11,0.45) !important; }
.fx-c-btn-danger { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important; color: #fff !important; box-shadow: 0 4px 15px rgba(239,68,68,0.35) !important; }
.fx-c-btn-danger:hover { transform: translateY(-2px) !important; box-shadow: 0 8px 25px rgba(239,68,68,0.45) !important; }
.fx-c-btn-ghost { background: rgba(148,163,184,0.1) !important; color: #cbd5e1 !important; border: 1px solid rgba(148,163,184,0.2) !important; }
.fx-c-btn-ghost:hover { background: rgba(148,163,184,0.18) !important; color: #e2e8f0 !important; transform: translateY(-1px) !important; }
.fx-c-header { display: flex; justify-content: center; align-items: center; padding: 36px 28px 20px; }
.fx-c-ico { width: 64px; height: 64px; border-radius: 18px; display: flex; align-items: center; justify-content: center; color: #94a3b8; flex-shrink: 0; position: relative; }
.fx-c-ico.pass { color: #34d399; background: linear-gradient(135deg, rgba(52,211,153,0.15), rgba(16,185,129,0.08)); box-shadow: 0 8px 24px rgba(52,211,153,0.2); }
.fx-c-ico.reject { color: #fbbf24; background: linear-gradient(135deg, rgba(251,191,36,0.15), rgba(245,158,11,0.08)); box-shadow: 0 8px 24px rgba(251,191,36,0.2); }
.fx-c-ico.danger { color: #f87171; background: linear-gradient(135deg, rgba(248,113,113,0.15), rgba(239,68,68,0.08)); box-shadow: 0 8px 24px rgba(248,113,113,0.2); }
.fx-c-ico.ghost { color: #94a3b8; background: linear-gradient(135deg, rgba(148,163,184,0.12), rgba(100,116,139,0.06)); box-shadow: 0 8px 24px rgba(148,163,184,0.15); }
.fx-c-title { font-size: 20px; font-weight: 700; color: #f1f5f9; padding: 0 28px; margin-bottom: 8px; letter-spacing: -0.3px; text-align: center; }
.fx-c-desc { font-size: 14px; color: #64748b; padding: 0 28px; margin-bottom: 20px; line-height: 1.6; text-align: center; }
.fx-c-warning { margin: 0 28px 24px; padding: 16px 18px; border-radius: 14px; display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 13px; line-height: 1.6; text-align: center; }
.fx-c-warning.pass { background: linear-gradient(135deg, rgba(52,211,153,0.1), rgba(16,185,129,0.05)); border: 1px solid rgba(52,211,153,0.2); color: #34d399; }
.fx-c-warning.reject { background: linear-gradient(135deg, rgba(251,191,36,0.1), rgba(245,158,11,0.05)); border: 1px solid rgba(251,191,36,0.2); color: #fbbf24; }
.fx-c-warning.danger { background: linear-gradient(135deg, rgba(248,113,113,0.1), rgba(239,68,68,0.05)); border: 1px solid rgba(248,113,113,0.2); color: #f87171; }
.fx-c-warning.ghost { background: linear-gradient(135deg, rgba(148,163,184,0.08), rgba(100,116,139,0.04)); border: 1px solid rgba(148,163,184,0.15); color: #94a3b8; }
.fx-c-warning svg { flex-shrink: 0; margin-top: 2px; }
.fx-c-hl { color: #f87171; font-weight: 700; }
</style>
