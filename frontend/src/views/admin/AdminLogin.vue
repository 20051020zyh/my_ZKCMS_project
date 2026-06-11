<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi } from '@/api/user'
import { checkAdmin } from '@/api/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loginForm = ref({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const noPermDialogVisible = ref(false)
const bgCanvas = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0

const startParticles = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles: any[] = []
  const count = 55

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 2 + 0.8,
      vx: (Math.random() - 0.5) * 0.35,
      vy: (Math.random() - 0.5) * 0.35,
      hue: Math.random() < 0.5 ? 210 + Math.random() * 30 : 160 + Math.random() * 30
    })
  }

  const draw = () => {
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (let i = 0; i < particles.length; i++) {
      const p = particles[i]
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `hsla(${p.hue}, 60%, 70%, 0.25)`
      ctx.fill()

      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1
    }

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 130) {
          const avgHue = (particles[i].hue + particles[j].hue) / 2
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `hsla(${avgHue}, 55%, 70%, ${0.08 * (1 - dist / 130)})`
          ctx.stroke()
        }
      }
    }

    animFrameId = requestAnimationFrame(draw)
  }

  draw()
}

const handleResize = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

onMounted(() => {
  startParticles()
  window.addEventListener('resize', handleResize)
  if (route.query.noPermission === '1') {
    noPermDialogVisible.value = true
  }
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', handleResize)
})

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请填写完整的登录信息'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await loginApi(loginForm.value)
    const token = res.data?.token || res.data
    userStore.setToken(token)

    const adminRes: any = await checkAdmin()
    if (!adminRes.data) {
      userStore.logout()
      noPermDialogVisible.value = true
      loading.value = false
      return
    }

    sessionStorage.setItem('isAdmin', 'true')
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (error: any) {
    errorMsg.value = error?.message || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/')
}
</script>

<template>
  <div class="admin-auth">
    <canvas ref="bgCanvas" class="bg-canvas" />
    <div class="auth-overlay" />

    <button class="back-link" @click="handleBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      返回首页
    </button>

    <div class="auth-panel">
      <div class="panel-decoration">
        <div class="decoration-circle c1" />
        <div class="decoration-circle c2" />
        <div class="decoration-circle c3" />
      </div>

      <div class="panel-content">
        <div class="panel-brand">
          <div class="brand-mask">
            <div class="brand-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <rect x="3" y="3" width="7" height="7" rx="1.5" />
                <rect x="14" y="3" width="7" height="7" rx="1.5" />
                <rect x="3" y="14" width="7" height="7" rx="1.5" />
                <rect x="14" y="14" width="7" height="7" rx="1.5" />
              </svg>
            </div>
            <div class="brand-shimmer" />
          </div>
          <h1 class="brand-title">管理控制台</h1>
          <p class="brand-sub">请使用管理员账号登录系统</p>
        </div>

        <div class="panel-form">
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <input
              v-model="loginForm.username"
              type="text"
              class="input-field"
              placeholder="管理员账号"
              autocomplete="off"
              @keyup.enter="handleLogin"
              @focus="errorMsg = ''"
            />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </div>
            <input
              v-model="loginForm.password"
              type="password"
              class="input-field"
              placeholder="管理员密码"
              @keyup.enter="handleLogin"
              @focus="errorMsg = ''"
            />
          </div>

          <div class="error-tip" v-if="errorMsg">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ errorMsg }}
          </div>

          <button class="submit-btn" :class="{ loading }" :disabled="loading" @click="handleLogin">
            <span v-if="!loading" class="btn-text">登 录</span>
            <span v-else class="btn-loader">
              <span class="loader-dot" />
              <span class="loader-dot" />
              <span class="loader-dot" />
            </span>
          </button>
        </div>

        <div class="panel-foot">
          <span>&copy; 2026 知库后台管理系统 &middot; 仅限授权管理员访问</span>
        </div>
      </div>
    </div>
  </div>

  <!-- 无权限弹窗 -->
  <Teleport to="body">
    <div v-if="noPermDialogVisible" class="modal-overlay" @click.self="noPermDialogVisible = false">
      <div class="modal-container">
        <div class="modal-glass">
          <div class="modal-header">
            <div class="modal-hd-icon no-perm-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </div>
            <div class="modal-hd-text">
              <h3 class="modal-title">权限不足</h3>
              <p class="modal-desc">您无该权限，如果需要请联系管理员</p>
            </div>
            <button class="modal-close" @click="noPermDialogVisible = false">
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
            <button class="m-btn m-btn-primary" @click="noPermDialogVisible = false">我知道了</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.admin-auth {
  min-height: 100vh;
  background: linear-gradient(160deg, #f0f7ff 0%, #f5fbfa 30%, #f8fafe 60%, #f0f5ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.auth-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse 70% 50% at 30% 20%, rgba(59,130,246,0.08), transparent 50%),
              radial-gradient(ellipse 60% 60% at 75% 40%, rgba(16,185,129,0.06), transparent 50%),
              radial-gradient(ellipse 50% 50% at 50% 80%, rgba(59,130,246,0.04), transparent 50%);
  z-index: 1;
  pointer-events: none;
}

.back-link {
  position: fixed;
  top: 32px;
  left: 32px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 10px;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.back-link:hover {
  color: #1e293b;
  background: rgba(255,255,255,0.9);
  border-color: #60a5fa;
  box-shadow: 0 2px 12px rgba(59,130,246,0.1);
}

.auth-panel {
  position: relative;
  z-index: 2;
  width: 420px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 50px rgba(0,0,0,0.06), 0 0 0 1px rgba(255,255,255,0.5) inset;
  animation: panelIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes panelIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.panel-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
}

.c1 {
  width: 300px;
  height: 300px;
  top: -180px;
  right: -120px;
  border: 1px solid rgba(59,130,246,0.12);
  background: transparent;
}

.c2 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: -80px;
  border: 1px solid rgba(16,185,129,0.1);
  background: transparent;
}

.c3 {
  width: 140px;
  height: 140px;
  top: 40%;
  right: -50px;
  border: 1px solid rgba(59,130,246,0.08);
  background: transparent;
  opacity: 1;
}

.panel-content {
  position: relative;
  z-index: 1;
  padding: 44px 36px 28px;
}

.panel-brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand-mask {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
}

.brand-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 50%, #34d399 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
  z-index: 1;
}

.brand-shimmer {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%);
  animation: shimmer 3s ease-in-out infinite;
  z-index: 2;
}

@keyframes shimmer {
  0%, 100% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  50% { transform: translateX(100%) translateY(100%) rotate(45deg); }
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
  letter-spacing: -0.3px;
}

.brand-sub {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
  font-weight: 400;
}

.panel-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-group {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.25s ease;
}

.input-group:focus-within {
  border-color: #60a5fa;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(59,130,246,0.08);
}

.input-icon {
  padding-left: 14px;
  color: #cbd5e1;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: color 0.25s ease;
}

.input-group:focus-within .input-icon {
  color: #3b82f6;
}

.input-field {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #1e293b;
  font-size: 14px;
  padding: 14px 14px;
  font-family: inherit;
  letter-spacing: 0.2px;
}

.input-field::placeholder {
  color: #cbd5e1;
}

.input-field:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 30px #f8fafc inset;
  -webkit-text-fill-color: #1e293b;
}

.error-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f87171;
  font-size: 13px;
  padding: 0 2px;
  line-height: 1.4;
  animation: shake 0.4s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.submit-btn {
  width: 100%;
  height: 46px;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  font-family: inherit;
  letter-spacing: 4px;
  margin-top: 8px;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 14px;
  background: linear-gradient(135deg, #60a5fa, #3b82f6, #34d399, #3b82f6, #60a5fa);
  background-size: 300% 300%;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}

.submit-btn:hover:not(:disabled)::before {
  opacity: 0.6;
  animation: gradientShift 3s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 4px 24px rgba(59,130,246,0.35);
  transform: translateY(-2px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.submit-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.submit-btn:hover::after {
  transform: translateX(100%);
}

.btn-text {
  position: relative;
  z-index: 1;
}

.btn-loader {
  display: flex;
  gap: 6px;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.loader-dot {
  width: 7px;
  height: 7px;
  background: #fff;
  border-radius: 50%;
  animation: dotBounce 1.2s ease-in-out infinite;
}

.loader-dot:nth-child(2) { animation-delay: 0.15s; }
.loader-dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.panel-foot {
  text-align: center;
  margin-top: 28px;
  font-size: 12px;
  color: #cbd5e1;
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
  background: #fff;
  border: 1px solid rgba(203,213,225,0.3);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.5) inset;
  overflow: hidden;
}
.modal-header {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(203,213,225,0.2);
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
.modal-title { margin: 0; font-size: 17px; font-weight: 700; color: #0f172a; letter-spacing: -0.3px; }
.modal-desc { margin: 3px 0 0; font-size: 13px; color: #64748b; line-height: 1.5; }
.modal-close {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: rgba(241,245,249,0.5); color: #94a3b8;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all 0.2s;
}
.modal-close:hover { background: rgba(239,68,68,0.1); color: #ef4444; }
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
  color: #1e293b;
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
  border-top: 1px solid rgba(203,213,225,0.2);
}
.m-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 28px; border-radius: 10px;
  font-size: 14px; font-weight: 600; border: none;
  cursor: pointer; transition: all 0.25s ease;
  position: relative; overflow: hidden;
}
.m-btn-primary { background: linear-gradient(135deg, #60a5fa, #3b82f6); color: #fff; box-shadow: 0 4px 16px rgba(59,130,246,0.3); }
.m-btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 24px rgba(59,130,246,0.45); }
.m-btn-primary:active { transform: translateY(0) scale(0.98); }
</style>