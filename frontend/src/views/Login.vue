<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi, register as registerApi } from '@/api/user'
import { getPopInfo } from '@/api/admin'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', rePassword: '' })
const loading = ref(false)
const errorMsg = ref('')
const maintenanceMode = ref(false)

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
      hue: Math.random() < 0.5 ? 250 + Math.random() * 40 : 200 + Math.random() * 40
    })
  }

  const draw = () => {
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (let i = 0; i < particles.length; i++) {
      const p = particles[i]
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `hsla(${p.hue}, 80%, 70%, 0.25)`
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
          ctx.strokeStyle = `hsla(${avgHue}, 70%, 70%, ${0.08 * (1 - dist / 130)})`
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
  checkMaintenance()
})

const checkMaintenance = async () => {
  try {
    const res: any = await getPopInfo()
    maintenanceMode.value = res.data?.type === 'MAINTENANCE'
  } catch {
    maintenanceMode.value = false
  }
}

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
    userStore.setToken(res.data?.token || res.data)
    if (res.data) {
      userStore.setUserInfo(res.data)
    }
    ElMessage.success('登录成功，欢迎回来')
    router.push('/')
  } catch (error: any) {
    errorMsg.value = error?.message || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    errorMsg.value = '请填写完整的注册信息'
    return
  }
  if (registerForm.value.password !== registerForm.value.rePassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await registerApi(registerForm.value)
    ElMessage.success('注册成功，欢迎加入知库')
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
  } catch (error: any) {
    errorMsg.value = error?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const switchTab = (tab: string) => {
  activeTab.value = tab
  errorMsg.value = ''
}

const handleBack = () => {
  router.push('/')
}
</script>

<template>
  <div class="auth-page">
    <canvas ref="bgCanvas" class="bg-canvas" />
    <div class="auth-overlay" />

    <button class="back-link" @click="handleBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      返回首页
    </button>

    <div class="auth-panel">
      <div class="panel-ornament">
        <div class="ornament-ring r1" />
        <div class="ornament-ring r2" />
        <div class="ornament-ring r3" />
      </div>

      <div class="panel-inner">
        <div class="panel-brand">
          <div class="brand-mask">
            <div class="brand-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              </svg>
            </div>
            <div class="brand-shimmer" />
          </div>
          <h1 class="brand-title">{{ activeTab === 'login' ? '欢迎回来' : '加入知库' }}</h1>
          <p class="brand-sub">{{ activeTab === 'login' ? '登录您的账号，发现精彩内容' : '创建账号，开启知识之旅' }}</p>
          <div v-if="maintenanceMode" class="mt-badge">
            <span class="mt-badge-dot" />
            <span>系统维护中</span>
          </div>
        </div>

        <div class="panel-tabs">
          <button class="panel-tab" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</button>
          <button class="panel-tab" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</button>
        </div>

        <div class="panel-form">
          <template v-if="activeTab === 'login'">
            <div class="input-group">
              <div class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <input
                v-model="loginForm.username"
                type="text"
                class="input-field"
                placeholder="用户名"
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
                placeholder="密码"
                @keyup.enter="handleLogin"
                @focus="errorMsg = ''"
              />
            </div>
          </template>

          <template v-if="activeTab === 'register'">
            <div class="input-group">
              <div class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <input
                v-model="registerForm.username"
                type="text"
                class="input-field"
                placeholder="设置用户名"
                autocomplete="off"
                @focus="errorMsg = ''"
              />
            </div>
            <div class="input-group">
              <div class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </div>
              <input
                v-model="registerForm.password"
                type="password"
                class="input-field"
                placeholder="设置密码"
                @focus="errorMsg = ''"
              />
            </div>
            <div class="input-group">
              <div class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </div>
              <input
                v-model="registerForm.rePassword"
                type="password"
                class="input-field"
                placeholder="确认密码"
                @keyup.enter="handleRegister"
                @focus="errorMsg = ''"
              />
            </div>
          </template>

          <div class="error-tip" v-if="errorMsg">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ errorMsg }}
          </div>

          <button
            v-if="activeTab === 'login'"
            class="submit-btn"
            :class="{ loading }"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="!loading" class="btn-text">登 录</span>
            <span v-else class="btn-loader">
              <span class="loader-dot" />
              <span class="loader-dot" />
              <span class="loader-dot" />
            </span>
          </button>

          <button
            v-if="activeTab === 'register'"
            class="submit-btn"
            :class="{ loading }"
            :disabled="loading"
            @click="handleRegister"
          >
            <span v-if="!loading" class="btn-text">注 册</span>
            <span v-else class="btn-loader">
              <span class="loader-dot" />
              <span class="loader-dot" />
              <span class="loader-dot" />
            </span>
          </button>
        </div>

        <div class="panel-foot">
          <span>&copy; 2026 知库平台 &middot; 发现知识的力量</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #fdf2f8 0%, #f5f3ff 30%, #ede9fe 60%, #e0f2fe 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: transparent;
}

.auth-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse 70% 50% at 30% 20%, rgba(236,72,153,0.1), transparent 50%),
              radial-gradient(ellipse 60% 60% at 75% 40%, rgba(139,92,246,0.08), transparent 50%),
              radial-gradient(ellipse 50% 50% at 50% 80%, rgba(59,130,246,0.06), transparent 50%);
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
  color: #6b7280;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 10px;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.back-link:hover {
  color: #374151;
  background: rgba(255,255,255,0.9);
  border-color: #cbd5e1;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.auth-panel {
  position: relative;
  z-index: 2;
  width: 420px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.6);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 50px rgba(0,0,0,0.08), 0 0 0 1px rgba(255,255,255,0.5) inset;
  animation: panelIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes panelIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.panel-ornament {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.ornament-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(139,92,246,0.12);
}

.r1 {
  width: 300px;
  height: 300px;
  top: -180px;
  right: -120px;
}

.r2 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: -80px;
  border-color: rgba(236,72,153,0.1);
}

.r3 {
  width: 140px;
  height: 140px;
  top: 40%;
  right: -50px;
  border-color: rgba(59,130,246,0.08);
}

.panel-inner {
  position: relative;
  z-index: 1;
  padding: 44px 40px 32px;
}

.panel-brand {
  text-align: center;
  margin-bottom: 32px;
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
  background: linear-gradient(135deg, #ec4899 0%, #8b5cf6 50%, #6366f1 100%);
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
  background: linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.15) 50%, transparent 60%);
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

.panel-tabs {
  display: flex;
  background: #f1f5f9;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 28px;
}

.panel-tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
  border-radius: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.panel-tab.active {
  background: #fff;
  color: #6366f1;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.03);
}

.panel-tab:hover:not(.active) {
  color: #64748b;
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
  border-color: #8b5cf6;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(139,92,246,0.08);
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
  color: #8b5cf6;
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
  color: #ef4444;
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
  background: linear-gradient(135deg, #ec4899, #8b5cf6, #6366f1);
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
  background: linear-gradient(135deg, #ec4899, #8b5cf6, #6366f1, #8b5cf6, #ec4899);
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
  box-shadow: 0 4px 24px rgba(139,92,246,0.35);
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
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
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

.mt-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(239,68,68,0.08), rgba(239,68,68,0.04));
  border: 1px solid rgba(239,68,68,0.12);
  font-size: 12px;
  font-weight: 500;
  color: #ef4444;
  animation: mtBadgeIn 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes mtBadgeIn {
  from { opacity: 0; transform: scale(0.9) translateY(-4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.mt-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
  animation: mtBadgePulse 1.6s ease-in-out infinite;
}

@keyframes mtBadgePulse {
  0%, 100% { opacity: 0.3; transform: scale(0.7); box-shadow: 0 0 0 0 rgba(239,68,68,0.3); }
  50% { opacity: 1; transform: scale(1.1); box-shadow: 0 0 0 6px rgba(239,68,68,0); }
}
</style>