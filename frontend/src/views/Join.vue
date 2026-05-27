<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { addLeaveMessage } from '@/api/leaveMessage'

const router = useRouter()

const form = ref({ name: '', phone: '', email: '', message: '' })
const submitting = ref(false)

const contactInfo = [
  { icon: 'mail', label: '邮箱', value: '3174471975@qq.com' },
  { icon: 'phone', label: '电话', value: '13319492825' },
  { icon: 'location', label: '地址', value: '江西省赣州市江西环境工程职业学院(章贡校区)' },
]

const positions = [
  {
    title: '前端开发工程师', type: '技术', location: '远程',
    tags: ['Vue 3', 'TypeScript', 'Tailwind'],
    desc: '负责知库前端核心功能开发，参与组件库建设与性能优化。期望你热爱技术、追求极致用户体验。',
  },
  {
    title: '后端开发工程师', type: '技术', location: '远程',
    tags: ['Java', 'Spring Boot', 'MySQL'],
    desc: '负责知库后端服务架构设计与开发，保障系统高可用与数据安全。期待你严谨细致、善于协作。',
  },
  {
    title: '内容运营', type: '运营', location: '远程',
    tags: ['内容策划', '社区运营', '数据分析'],
    desc: '负责平台内容生态建设、优质创作者挖掘与维护。希望你热爱文字、善于沟通。',
  },
  {
    title: 'UI/UX 设计师', type: '设计', location: '远程',
    tags: ['Figma', '交互设计', '视觉设计'],
    desc: '负责知库产品界面与交互体验设计，打造令人愉悦的阅读体验。期待你有独特的审美品味。',
  },
]

const benefits = [
  { icon: 'clock', title: '弹性工作', desc: '远程办公，灵活安排工作时间，兼容生活与工作的平衡。' },
  { icon: 'globe', title: '完全远程', desc: '不限地点，只要有网络就能参与。我们相信分布式协作的力量。' },
  { icon: 'book', title: '学习成长', desc: '定期技术分享、购书报销、参与开源项目的机会。' },
  { icon: 'heart', title: '温暖社区', desc: '扁平化管理，真诚沟通。这里没有办公室政治，只有共同成长。' },
  { icon: 'gift', title: '创作支持', desc: '为创作者提供流量扶持、编辑协助和作品推广资源。' },
  { icon: 'sun', title: '无限可能', desc: '初创平台，每个人都有机会定义产品方向，发挥最大影响力。' },
]

const handleSubmit = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (form.value.name.length > 50) {
    ElMessage.warning('姓名不能超过50个字符')
    return
  }
  if (!form.value.phone || form.value.phone.length !== 11) {
    ElMessage.warning('请填写正确的11位电话号码')
    return
  }
  if (!form.value.email) {
    ElMessage.warning('请填写邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    ElMessage.warning('请填写正确的邮箱格式')
    return
  }
  if (!form.value.message) {
    ElMessage.warning('请填写留言内容')
    return
  }
  submitting.value = true
  try {
    await addLeaveMessage({
      name: form.value.name,
      phone: form.value.phone,
      email: form.value.email,
      content: form.value.message
    })
    ElMessage.success('留言已提交，我们会尽快回复你！')
    form.value = { name: '', phone: '', email: '', message: '' }
  } catch {
    // 错误已在 request.ts 拦截器中处理
  } finally {
    submitting.value = false
  }
}

let sectionObserver: IntersectionObserver | null = null

onMounted(() => {
  sectionObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) e.target.classList.add('visible')
    })
  }, { threshold: 0.12 })
  document.querySelectorAll('.sc-rev').forEach(el => sectionObserver!.observe(el))
})

onUnmounted(() => sectionObserver?.disconnect())
</script>

<template>
  <div class="join-page">
    <div class="page-wrap">
      <button class="back-btn" @click="router.push('/')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回首页
      </button>

      <section class="hero">
        <div class="hero-badge reveal">✦ 加入我们 / 联系我们</div>
        <h1 class="hero-title reveal" style="--d:0.1s">每一个认真的人<br/>都值得被认真对待</h1>
        <p class="hero-sub reveal" style="--d:0.2s">
          有任何问题或合作需求，欢迎随时联系或留言
        </p>
      </section>

      <section class="contact-section sc-rev" style="--d:0s">
        <div class="section-label">联系方式</div>
        <div class="contact-row">
          <div class="contact-item sc-rev" v-for="(c, i) in contactInfo" :key="i" :style="{ '--d': (0.06 * i) + 's' }">
            <div class="ci-icon">
              <svg v-if="c.icon==='mail'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              <svg v-else-if="c.icon==='phone'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
            </div>
            <div class="ci-text">
              <span class="ci-label">{{ c.label }}</span>
              <span class="ci-value">{{ c.value }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="form-section">
        <div class="section-label sc-rev">留言给我们</div>
        <div class="message-grid">
          <div class="message-form sc-rev" style="--d:0.06s">
            <div class="form-body">
              <div class="ff-row">
                <div class="ff-group">
                  <label>姓名</label>
                  <input v-model="form.name" placeholder="请输入您的姓名" />
                </div>
                <div class="ff-group">
                  <label>联系电话</label>
                  <input v-model="form.phone" placeholder="请输入联系电话" />
                </div>
              </div>
              <div class="ff-group">
                <label>邮箱</label>
                <input v-model="form.email" type="email" placeholder="请输入邮箱地址" />
              </div>
              <div class="ff-group full">
                <label>留言内容</label>
                <textarea v-model="form.message" placeholder="请输入留言内容..." rows="4"></textarea>
              </div>
              <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
                <span v-if="submitting" class="spinner"></span>
                {{ submitting ? '提交中...' : '提交留言' }}
              </button>
            </div>
          </div>
          <div class="message-illust sc-rev" style="--d:0.1s">
            <div class="ill-box">
              <div class="ill-ring r1"></div>
              <div class="ill-ring r2"></div>
              <div class="ill-ring r3"></div>
              <div class="ill-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1"><path d="M21 11.5a8.38 8.38 0 01-.9 3.8A8.5 8.5 0 017.5 20.1 8.38 8.38 0 013.7 19.2L1 21l1.6-4.2A8.38 8.38 0 013.5 13a8.5 8.5 0 0110.6-7.3 8.38 8.38 0 016.4 6.6"/></svg>
              </div>
            </div>
            <p class="ill-text">我们期待听到你的声音<br/>每一条留言都会认真阅读</p>
          </div>
        </div>
      </section>

      <section class="positions-section">
        <div class="section-label sc-rev">招聘岗位</div>
        <h2 class="section-title sc-rev" style="--d:0.04s">加入我们，一起做点有意思的事</h2>
        <div class="pos-list">
          <div class="pos-card sc-rev" v-for="(p, i) in positions" :key="i" :style="{ '--d': (0.05 * i) + 's' }">
            <div class="pos-top">
              <div class="pos-head">
                <h3>{{ p.title }}</h3>
                <span class="pos-type">{{ p.type }}</span>
                <span class="pos-loc">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
                  {{ p.location }}
                </span>
              </div>
              <p class="pos-desc">{{ p.desc }}</p>
            </div>
            <div class="pos-tags">
              <span class="pos-tag" v-for="t in p.tags" :key="t">{{ t }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="benefits-section">
        <div class="section-label sc-rev">工作方式</div>
        <h2 class="section-title sc-rev" style="--d:0.04s">在这里，你值得更好的</h2>
        <div class="benefits-grid">
          <div class="benefit-card sc-rev" v-for="(b, i) in benefits" :key="i" :style="{ '--d': (0.05 * i) + 's' }">
            <div class="bc-icon">
              <svg v-if="b.icon==='clock'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <svg v-else-if="b.icon==='globe'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z"/></svg>
              <svg v-else-if="b.icon==='book'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
              <svg v-else-if="b.icon==='heart'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
              <svg v-else-if="b.icon==='gift'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 010-5C11 2 12 7 12 7z"/><path d="M12 7h4.5a2.5 2.5 0 000-5C13 2 12 7 12 7z"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            </div>
            <div class="bc-text">
              <h4>{{ b.title }}</h4>
              <p>{{ b.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <footer class="join-footer">
        <p>© 2026 知库平台 · 每一个认真的人，都值得被认真对待</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@300;400;500;600;700&display=swap');

.join-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #faf8f5 0%, #f5f0ea 40%, #faf8f5 100%);
  font-family: 'Noto Serif SC', 'STSong', 'Songti SC', 'SimSun', 'Georgia', serif;
}

.page-wrap {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 32px 80px;
}

.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 18px; background: rgba(255,255,255,0.85);
  border: 1px solid rgba(180,160,130,0.25); border-radius: 24px;
  color: #8b7355; font-size: 13px; cursor: pointer;
  font-family: inherit; backdrop-filter: blur(8px); transition: all 0.25s;
}
.back-btn:hover { background: #fff; border-color: #c4a97d; color: #5c4a32; transform: translateX(-4px); }

/* ── Hero ── */
.hero { padding: 60px 0 48px; text-align: center; }
.hero-badge {
  display: inline-flex; gap: 6px; padding: 6px 18px;
  background: rgba(196,169,125,0.08); border: 1px solid rgba(196,169,125,0.2);
  border-radius: 20px; font-size: 13px; color: #8b7355; margin-bottom: 24px;
}
.hero-title {
  font-size: clamp(32px, 5vw, 50px); font-weight: 700;
  color: #2d2318; line-height: 1.18; margin: 0 0 18px; letter-spacing: -1px;
}
.hero-sub {
  font-size: 15px; color: #8b7355; line-height: 1.7;
  max-width: 460px; margin: 0 auto;
}

.section-label {
  font-size: 12px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 3px; color: #c4a97d; margin-bottom: 24px;
}
.section-title {
  font-size: clamp(24px, 3.5vw, 34px); font-weight: 700;
  color: #2d2318; margin: 0 0 40px; letter-spacing: -0.5px;
}

/* ── Contact ── */
.contact-section { padding: 24px 0 56px; }
.contact-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.contact-item {
  background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 18px; padding: 28px 24px; display: flex;
  align-items: center; gap: 16px; transition: all 0.3s;
}
.contact-item:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(180,140,100,0.06); border-color: rgba(196,169,125,0.2); }
.ci-icon {
  width: 46px; height: 46px; border-radius: 13px;
  background: rgba(196,169,125,0.08); display: flex;
  align-items: center; justify-content: center; color: #8b7355; flex-shrink: 0;
}
.ci-icon svg { width: 22px; height: 22px; }
.ci-text { display: flex; flex-direction: column; gap: 3px; }
.ci-label { font-size: 11px; font-weight: 600; color: #b8a090; text-transform: uppercase; letter-spacing: 1px; }
.ci-value { font-size: 14px; color: #5c4a32; word-break: break-all; }

/* ── Form ── */
.form-section { padding: 16px 0 72px; }
.message-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; align-items: start; }
.message-form { background: #fff; border: 1px solid rgba(180,160,130,0.12); border-radius: 22px; padding: 36px; }
.form-body { display: flex; flex-direction: column; gap: 18px; }
.ff-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.ff-group { display: flex; flex-direction: column; gap: 5px; }
.ff-group.full {}
.ff-group label { font-size: 12px; font-weight: 600; color: #5c4a32; letter-spacing: 0.5px; }
.ff-group input, .ff-group textarea {
  padding: 11px 14px; border: 1px solid rgba(180,160,130,0.2);
  border-radius: 10px; font-size: 14px; color: #2d2318;
  background: #faf8f5; font-family: inherit; outline: none;
  transition: all 0.25s; resize: vertical;
}
.ff-group input:focus, .ff-group textarea:focus {
  border-color: #c4a97d; box-shadow: 0 0 0 3px rgba(196,169,125,0.06);
}
.submit-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; padding: 13px; border: none; border-radius: 11px;
  font-size: 14px; font-weight: 600; color: #fff; cursor: pointer;
  font-family: inherit; transition: all 0.25s;
  background: linear-gradient(135deg, #c4a97d, #b8956a);
  box-shadow: 0 3px 12px rgba(180,140,100,0.18);
}
.submit-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(180,140,100,0.25); }
.submit-btn:disabled { opacity: 0.65; cursor: not-allowed; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.message-illust { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; }
.ill-box {
  position: relative; width: 180px; height: 180px;
  display: flex; align-items: center; justify-content: center; margin-bottom: 24px;
}
.ill-ring {
  position: absolute; border-radius: 50%; border: 1px solid rgba(180,160,130,0.15);
}
.r1 { width: 100%; height: 100%; animation: ringPulse 5s ease-in-out infinite; }
.r2 { width: 78%; height: 78%; animation: ringPulse 5s ease-in-out infinite 0.8s; }
.r3 { width: 56%; height: 56%; animation: ringPulse 5s ease-in-out infinite 1.6s; }
.ill-icon {
  position: relative; z-index: 2; width: 64px; height: 64px;
  border-radius: 50%; background: rgba(196,169,125,0.1);
  display: flex; align-items: center; justify-content: center; color: #8b7355;
}
.ill-icon svg { width: 32px; height: 32px; }
@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.08); opacity: 1; }
}
.ill-text { text-align: center; font-size: 14px; color: #8b7355; line-height: 1.8; margin: 0; }

/* ── Positions ── */
.positions-section { padding: 8px 0 72px; }
.pos-list { display: flex; flex-direction: column; gap: 10px; }
.pos-card {
  background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 18px; padding: 26px 30px; display: flex;
  align-items: center; gap: 28px; transition: all 0.3s;
}
.pos-card:hover { border-color: rgba(196,169,125,0.2); box-shadow: 0 4px 22px rgba(180,140,100,0.04); transform: translateX(4px); }
.pos-top { flex: 1; min-width: 0; }
.pos-head { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.pos-head h3 { font-size: 16px; font-weight: 600; color: #2d2318; margin: 0; }
.pos-type {
  font-size: 11px; font-weight: 600; padding: 3px 10px;
  border-radius: 6px; background: rgba(196,169,125,0.1); color: #8b7355;
}
.pos-loc { font-size: 12px; color: #a09080; display: flex; align-items: center; gap: 4px; }
.pos-desc { font-size: 13px; color: #8b7355; line-height: 1.6; margin: 0; }
.pos-tags { display: flex; gap: 6px; flex-wrap: wrap; flex-shrink: 0; }
.pos-tag {
  font-size: 11px; padding: 3px 10px; border-radius: 6px;
  background: rgba(180,160,130,0.05); color: #8b7355;
  border: 1px solid rgba(180,160,130,0.12);
}

/* ── Benefits ── */
.benefits-section { padding: 8px 0 56px; }
.benefits-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.benefit-card {
  background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 18px; padding: 28px 24px; transition: all 0.3s;
}
.benefit-card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(180,140,100,0.06); }
.bc-icon {
  width: 42px; height: 42px; border-radius: 12px;
  background: rgba(196,169,125,0.08); display: flex;
  align-items: center; justify-content: center; color: #8b7355; margin-bottom: 16px;
}
.bc-icon svg { width: 19px; height: 19px; }
.bc-text h4 { font-size: 15px; font-weight: 600; color: #2d2318; margin: 0 0 6px; }
.bc-text p { font-size: 12px; color: #8b7355; line-height: 1.6; margin: 0; }

/* ── Footer ── */
.join-footer { text-align: center; padding: 24px 0 0; }
.join-footer p { font-size: 12px; color: #b8a090; margin: 0; }

/* ── Animations ── */
.reveal {
  opacity: 0; transform: translateY(28px);
  animation: revUp 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
  animation-delay: var(--d, 0s);
}
.sc-rev {
  opacity: 0; transform: translateY(36px);
  transition: all 0.7s cubic-bezier(0.22, 0.61, 0.36, 1);
  transition-delay: var(--d, 0s);
}
.sc-rev.visible { opacity: 1; transform: translateY(0); }
@keyframes revUp { to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .page-wrap { padding: 24px 20px 60px; }
  .hero { padding: 40px 0 32px; }
  .hero-title { font-size: 28px; }
  .contact-row { grid-template-columns: 1fr; }
  .message-grid { grid-template-columns: 1fr; }
  .message-illust { padding: 24px 0 0; }
  .ff-row { grid-template-columns: 1fr; }
  .pos-card { flex-direction: column; align-items: flex-start; gap: 14px; }
  .pos-tags { justify-content: flex-start; }
  .benefits-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .benefits-grid { grid-template-columns: 1fr; }
}
</style>