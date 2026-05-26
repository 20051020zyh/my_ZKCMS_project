<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = [
  { label: '核心接口', value: 84, target: 84, suffix: '' },
  { label: '技术栈组件', value: 20, target: 20, suffix: '+' },
  { label: '迭代版本', value: 4, target: 4, suffix: '' },
  { label: '代码行数', value: 17000, target: 17000, suffix: '+' },
]

const milestones = [
  {
    period: '3月9日 — 3月23日',
    title: 'Java 基础夯实',
    desc: '集中补充 Java 基础知识，从面向对象核心概念系统学习至 Map 双列集合，为后续框架学习打下坚实基础。',
    icon: 'book'
  },
  {
    period: '3月24日 — 3月30日',
    title: 'SpringBoot 入门与项目搭建',
    desc: '自学 SpringBoot 框架核心知识点，快速完成 CMS 项目基本骨架搭建，确定核心模块技术方案。',
    icon: 'rocket'
  },
  {
    period: '4月15日',
    title: '核心功能升级',
    desc: '完成接口限流功能开发，基于 AOP 实现系统追踪日志功能，提升系统稳定性和可维护性。',
    icon: 'shield'
  },
  {
    period: '4月28日',
    title: '接口功能拓展',
    desc: '新增 5 个核心接口：显示所有分类名、浏览量自增、全文关键词搜索、批量操作状态、按分类获取文章。',
    icon: 'code'
  },
  {
    period: '4月29日 — 4月30日',
    title: '前后端交互实现',
    desc: '完成前后端数据交互开发，打通前端页面与后端接口，实现核心功能的可视化操作。',
    icon: 'link'
  },
  {
    period: '5月1日 — 5月2日',
    title: '模块功能迭代优化',
    desc: '完成文章发布、标签、回收站、SEO 相关接口，同步实现评论交互、审核风控与举报体系开发',
    icon: 'layers'
  },
  {
    period: '5月6日 — 5月17日',
    title: '权限体系搭建与后台运维能力升级',
    desc: '完成角色、权限菜单、用户管理全套接口，实现精细化权限校验，新增批量运维、数据统计排行、公告及站点维护相关功能',
    icon: 'workspace'
  },
  {
    period: '5月18日 — 5月24日',
    title: '前后端交互架构迭代',
    desc: '依托后端项目完成配套前端页面开发，打通前后端与数据库数据链路，完成第二代前后端交互能力落地',
    icon: 'refresh'
  },
]

const values = [
  { title: '独立开发', desc: '前后端均由一人独立完成开发与迭代，具备全栈能力与系统性思维。', color: '#b8956a' },
  { title: '持续迭代', desc: '从零基础起步，分阶段逐步完善功能，每个版本都在上一次基础上精进。', color: '#c4806a' },
  { title: '实用至上', desc: '注重实用性和可扩展性，每个功能模块都经过精心设计，贴合并发场景。', color: '#7a9c8a' },
  { title: '追求极致', desc: '持续优化用户体验，新增更多实用功能，让系统更加完善、体验更流畅。', color: '#6a8f9c' },
]

let counting = false
let observer: IntersectionObserver | null = null

const startCount = () => {
  if (counting) return
  counting = true
  const steps = 60
  const interval = 2000 / steps
  stats.forEach((stat) => {
    const inc = stat.target / steps
    let s = 0
    const t = setInterval(() => {
      s++
      stat.value = Math.min(Math.round(inc * s), stat.target)
      if (s >= steps) clearInterval(t)
    }, interval)
  })
}

let sectionObserver: IntersectionObserver | null = null

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) startCount()
  }, { threshold: 0.3 })
  const statsEl = document.querySelector('.stats-section')
  if (statsEl) observer.observe(statsEl)

  sectionObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) e.target.classList.add('visible')
    })
  }, { threshold: 0.15 })
  document.querySelectorAll('.scroll-reveal').forEach(el => sectionObserver!.observe(el))
})

onUnmounted(() => {
  observer?.disconnect()
  sectionObserver?.disconnect()
})
</script>

<template>
  <div class="about-page">
    <div class="page-wrap">
      <button class="back-btn" @click="router.push('/')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回首页
      </button>

      <section class="hero">
        <div class="hero-badge reveal">✦ 关于我们</div>
        <h1 class="hero-title reveal" style="--d:0.1s">知库 CMS 系统<br/>开发历程</h1>
        <p class="hero-sub reveal" style="--d:0.2s">
          从 Java 基础到 SpringBoot 实战，独立完成前后端全栈开发<br/>
          逐步迭代完善功能，持续优化用户体验，致力于打造企业级 CMS 平台
        </p>
      </section>

      <section class="stats-section">
        <div class="stats-row">
          <div class="stat-card scroll-reveal" v-for="(s, i) in stats" :key="i" :style="{ '--d': (0.06 * i) + 's' }">
            <div class="stat-num">
              <span class="num-val">{{ s.value.toLocaleString() }}</span>
              <span class="num-suf">{{ s.suffix }}</span>
            </div>
            <span class="stat-lbl">{{ s.label }}</span>
          </div>
        </div>
      </section>

      <section class="intro-section">
        <div class="section-label scroll-reveal">项目简介</div>
        <div class="intro-grid">
          <div class="intro-visual scroll-reveal" style="--d:0.08s">
            <div class="code-frame">
              <div class="cf-dots">
                <span class="cf-dot red"></span>
                <span class="cf-dot yellow"></span>
                <span class="cf-dot green"></span>
              </div>
              <div class="cf-body">
                <div class="cf-line"><span class="cf-kw">import</span> { Knowledge } <span class="cf-kw">from</span> <span class="cf-str">'@zhiku/core'</span></div>
                <div class="cf-line"><span class="cf-kw">const</span> <span class="cf-fn">cms</span> = <span class="cf-kw">new</span> <span class="cf-cls">CMS</span>()</div>
                <div class="cf-line"><span class="cf-cm">// 全栈自研 · 持续迭代</span></div>
                <div class="cf-line cf-indent"><span class="cf-kw">await</span> cms.<span class="cf-fn">bootstrap</span>({</div>
                <div class="cf-line cf-dbl"><span class="cf-prop">modules</span>: [</div>
                <div class="cf-line cf-tri"><span class="cf-str">'user'</span>, <span class="cf-str">'article'</span>, <span class="cf-str">'category'</span></div>
                <div class="cf-line cf-dbl">],</div>
                <div class="cf-line cf-indent">})</div>
                <div class="cf-line cf-cursor"><span class="cf-kw">export</span> <span class="cf-kw">default</span> cms<span class="cf-blink">|</span></div>
              </div>
            </div>
          </div>
          <div class="intro-text scroll-reveal" style="--d:0.12s">
            <h2>一套全栈自研的内容管理平台</h2>
            <p>知库是一套由开发者独立开发的 CMS 内容管理系统，涵盖用户管理模块、文章分类模块、文章管理模块等核心功能，前后端均由一人完成开发与迭代。</p>
            <p>项目从零基础起步，先夯实 Java 基础，再快速上手 SpringBoot 框架搭建项目骨架，通过分阶段迭代的方式，逐步实现接口限流、AOP 追踪日志、全文检索等核心功能。</p>
            <p>整个开发过程注重实用性和可扩展性，每一个功能模块都经过精心设计，后续将持续优化用户体验，新增更多实用功能，让系统更加完善。</p>
          </div>
        </div>
      </section>

      <section class="timeline-section">
        <div class="section-label scroll-reveal">开发历程</div>
        <h2 class="section-title scroll-reveal" style="--d:0.04s">每一步都算数</h2>
        <div class="timeline">
          <div class="tl-track"></div>
          <div class="tl-item scroll-reveal" :class="'tl-item--' + (i % 2 === 0 ? 'right' : 'left')" v-for="(m, i) in milestones" :key="i" :style="{ '--d': (0.06 * i + 0.06) + 's' }">
            <div class="tl-card">
              <div class="tl-card-top">
                <div class="tl-icon-box">
                  <svg v-if="m.icon==='book'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
                  <svg v-else-if="m.icon==='rocket'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 00-2.91-.09z"/><path d="M12 15l-3-3a22 22 0 012.22-3.92M8.5 7A15.5 15.5 0 0115 3c3.31 0 6 2.69 6 6a15.5 15.5 0 01-4 6.5"/></svg>
                  <svg v-else-if="m.icon==='shield'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                  <svg v-else-if="m.icon==='code'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
                  <svg v-else-if="m.icon==='link'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
                  <svg v-else-if="m.icon==='layers'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
                  <svg v-else-if="m.icon==='workspace'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>
                </div>
                <div>
                  <span class="tl-period">{{ m.period }}</span>
                  <h4 class="tl-card-title">{{ m.title }}</h4>
                </div>
              </div>
              <p class="tl-card-desc">{{ m.desc }}</p>
            </div>
            <div class="tl-marker">
              <div class="tl-dot"></div>
              <div class="tl-branch"></div>
            </div>
          </div>
        </div>
      </section>

      <section class="values-section">
        <div class="section-label scroll-reveal">核心价值观</div>
        <h2 class="section-title scroll-reveal" style="--d:0.04s">驱动我们前行的信念</h2>
        <div class="values-grid">
          <div class="value-card scroll-reveal" v-for="(v, i) in values" :key="i" :style="{ '--d': (0.06 * i) + 's' }">
            <div class="vc-stripe" :style="{ background: v.color }"></div>
            <h3>{{ v.title }}</h3>
            <p>{{ v.desc }}</p>
          </div>
        </div>
      </section>

      <section class="cta-end scroll-reveal">
        <div class="cta-box">
          <div class="cta-orb"></div>
          <div class="cta-orb o2"></div>
          <h2>和我们一起构建<br/>更好的内容生态</h2>
          <p>知库持续进化中，欢迎反馈与建议</p>
          <div class="cta-actions">
            <button class="cta-btn primary" @click="router.push('/')">探索首页</button>
            <button class="cta-btn outline" @click="router.push('/join')">联系我们</button>
          </div>
        </div>
      </section>

      <footer class="about-footer">
        <p>© 2026 知库 CMS 平台 · 保持好奇，永远年轻</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@300;400;500;600;700&display=swap');

.about-page {
  min-height: 100vh;
  background: #faf8f5;
  font-family: 'Noto Serif SC', 'STSong', 'Songti SC', 'SimSun', 'Georgia', serif;
}

.page-wrap {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 32px 80px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: rgba(255,255,255,0.85);
  border: 1px solid rgba(180,160,130,0.25);
  border-radius: 24px;
  color: #8b7355;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  backdrop-filter: blur(8px);
  transition: all 0.25s;
}
.back-btn:hover { background: #fff; border-color: #c4a97d; color: #5c4a32; transform: translateX(-4px); }

/* ── Hero ── */
.hero { padding: 72px 0 48px; text-align: center; }
.hero-badge {
  display: inline-flex; gap: 6px; padding: 6px 18px;
  background: rgba(196,169,125,0.08); border: 1px solid rgba(196,169,125,0.2);
  border-radius: 20px; font-size: 13px; color: #8b7355;
  margin-bottom: 24px;
}
.hero-title {
  font-size: clamp(34px, 5vw, 54px); font-weight: 700;
  color: #2d2318; line-height: 1.15; margin: 0 0 20px; letter-spacing: -1px;
}
.hero-sub {
  font-size: 15px; color: #8b7355; line-height: 1.8;
  max-width: 560px; margin: 0 auto;
}

/* ── Stats ── */
.stats-section { padding: 16px 0 48px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card {
  background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 18px; padding: 28px 20px; text-align: center;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 10px 32px rgba(180,140,100,0.07); border-color: rgba(196,169,125,0.2); }
.stat-num { font-size: 38px; font-weight: 700; color: #2d2318; line-height: 1; margin-bottom: 6px; letter-spacing: -1px; }
.num-suf { font-size: 22px; color: #c4a97d; font-weight: 500; }
.stat-lbl { font-size: 13px; color: #8b7355; }

.section-label {
  font-size: 12px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 3px; color: #c4a97d; margin-bottom: 14px;
}
.section-title {
  font-size: clamp(26px, 3.5vw, 36px); font-weight: 700;
  color: #2d2318; margin: 0 0 48px; letter-spacing: -0.5px;
}

/* ── Intro ── */
.intro-section { padding: 40px 0 72px; }
.intro-grid { display: grid; grid-template-columns: 1fr 1.2fr; gap: 56px; align-items: center; }
.intro-visual { display: flex; justify-content: center; align-items: center; }

.code-frame {
  width: 100%;
  max-width: 340px;
  background: linear-gradient(145deg, #1a1b2e, #12132a);
  border-radius: 16px;
  border: 1px solid rgba(196,169,125,0.12);
  box-shadow: 0 12px 40px rgba(0,0,0,0.08), 0 0 0 1px rgba(196,169,125,0.06);
  overflow: hidden;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'Consolas', monospace;
}
.cf-dots {
  display: flex;
  gap: 7px;
  padding: 14px 16px 10px;
  background: rgba(255,255,255,0.02);
  border-bottom: 1px solid rgba(196,169,125,0.06);
}
.cf-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.cf-dot.red { background: #ff5f56; }
.cf-dot.yellow { background: #ffbd2e; }
.cf-dot.green { background: #27c93f; }
.cf-body {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.cf-line {
  font-size: 12.5px;
  line-height: 1.7;
  color: #c7d2e0;
  white-space: nowrap;
}
.cf-indent { padding-left: 14px; }
.cf-dbl { padding-left: 28px; }
.cf-tri { padding-left: 42px; }
.cf-kw { color: #c792ea; }
.cf-fn { color: #82aaff; }
.cf-cls { color: #ffcb6b; }
.cf-str { color: #c3e88d; }
.cf-cm { color: #546e7a; font-style: italic; }
.cf-prop { color: #f07178; }
.cf-cursor { margin-top: 4px; }
.cf-blink {
  animation: blinker 1s step-end infinite;
  color: #c7d2e0;
  font-weight: 300;
}
@keyframes blinker {
  50% { opacity: 0; }
}

.intro-text h2 { font-size: 24px; font-weight: 700; color: #2d2318; margin: 0 0 18px; }
.intro-text p { font-size: 14px; color: #7a6a5a; line-height: 1.8; margin: 0 0 12px; }

/* ── Timeline ── */
.timeline-section { padding: 32px 0 72px; }
.timeline { position: relative; padding: 12px 0; }
.tl-track {
  position: absolute; left: 50%; top: 0; bottom: 0;
  transform: translateX(-50%);
  width: 1px; background: linear-gradient(180deg, transparent, rgba(196,169,125,0.25) 10%, rgba(196,169,125,0.25) 90%, transparent);
}
.tl-item { display: flex; align-items: flex-start; margin-bottom: 28px; }
.tl-item--right { flex-direction: row-reverse; }
.tl-item--left { flex-direction: row; }

.tl-card {
  width: calc(50% - 28px); background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 16px; padding: 22px 26px; transition: all 0.3s;
}
.tl-item--left .tl-card { text-align: right; }
.tl-item--left .tl-card-top { flex-direction: row-reverse; }
.tl-item--left .tl-card-desc { text-align: right; }
.tl-card:hover { box-shadow: 0 6px 24px rgba(180,140,100,0.06); }

.tl-marker {
  width: 56px; flex-shrink: 0;
  display: flex; flex-direction: column; align-items: center;
  position: relative;
}
.tl-dot {
  width: 14px; height: 14px; border-radius: 50%;
  background: #faf8f5; border: 2.5px solid #c4a97d;
  margin-top: 16px; z-index: 2;
}
.tl-branch {
  width: 24px; height: 1px; background: rgba(196,169,125,0.3);
  position: absolute; top: 22px;
}
.tl-item--right .tl-branch { right: 0; }
.tl-item--left .tl-branch { left: 0; }
.tl-card-top { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; }
.tl-icon-box {
  width: 38px; height: 38px; border-radius: 10px;
  background: rgba(196,169,125,0.08); display: flex;
  align-items: center; justify-content: center; color: #8b7355; flex-shrink: 0;
}
.tl-icon-box svg { width: 18px; height: 18px; }
.tl-period {
  font-size: 11px; font-weight: 600; color: #c4a97d;
  display: inline-block; padding: 2px 10px;
  background: rgba(196,169,125,0.06); border-radius: 6px;
  margin-bottom: 4px;
}
.tl-card-title { font-size: 16px; font-weight: 600; color: #2d2318; margin: 0; }
.tl-card-desc { font-size: 13px; color: #8b7355; line-height: 1.7; margin: 0; }

/* ── Values ── */
.values-section { padding: 32px 0 72px; }
.values-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.value-card {
  background: #fff; border: 1px solid rgba(180,160,130,0.1);
  border-radius: 18px; padding: 32px 28px; overflow: hidden;
  transition: all 0.3s; position: relative;
}
.value-card:hover { transform: translateY(-4px); box-shadow: 0 10px 32px rgba(180,140,100,0.07); }
.vc-stripe {
  position: absolute; top: 0; left: 0; width: 4px; height: 100%;
  border-radius: 2px 0 0 2px;
}
.value-card h3 { font-size: 17px; font-weight: 600; color: #2d2318; margin: 0 0 10px; }
.value-card p { font-size: 13px; color: #8b7355; line-height: 1.65; margin: 0; }

/* ── CTA ── */
.cta-end { padding: 24px 0 20px; }
.cta-box {
  background: linear-gradient(135deg, #fff, #faf8f5);
  border: 1px solid rgba(196,169,125,0.18);
  border-radius: 26px; padding: 54px 48px;
  text-align: center; position: relative; overflow: hidden;
}
.cta-orb {
  position: absolute; border-radius: 50%;
  background: radial-gradient(circle, rgba(196,169,125,0.06), transparent);
  pointer-events: none;
  width: 340px; height: 340px; top: -120px; left: -80px;
}
.cta-orb.o2 { width: 260px; height: 260px; bottom: -100px; right: -60px; top: auto; left: auto; }
.cta-box h2 { font-size: 28px; font-weight: 700; color: #2d2318; line-height: 1.4; margin: 0 0 14px; }
.cta-box p { font-size: 14px; color: #8b7355; margin: 0 0 28px; }
.cta-actions { display: flex; justify-content: center; gap: 12px; }
.cta-btn {
  padding: 12px 30px; border-radius: 12px; font-size: 14px; font-weight: 600;
  cursor: pointer; font-family: inherit; transition: all 0.25s; border: none;
}
.cta-btn.primary {
  background: linear-gradient(135deg, #c4a97d, #b8956a); color: #fff;
  box-shadow: 0 4px 14px rgba(180,140,100,0.2);
}
.cta-btn.primary:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(180,140,100,0.3); }
.cta-btn.outline {
  background: transparent; border: 1px solid rgba(180,160,130,0.3); color: #8b7355;
}
.cta-btn.outline:hover { border-color: #c4a97d; color: #5c4a32; background: rgba(196,169,125,0.04); }

/* ── Footer ── */
.about-footer { text-align: center; padding: 36px 0 0; }
.about-footer p { font-size: 12px; color: #b8a090; margin: 0; }

/* ── Animations ── */
.reveal {
  opacity: 0; transform: translateY(28px);
  animation: revealUp 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
  animation-delay: var(--d, 0s);
}
.scroll-reveal {
  opacity: 0; transform: translateY(36px);
  transition: all 0.7s cubic-bezier(0.22, 0.61, 0.36, 1);
  transition-delay: var(--d, 0s);
}
.scroll-reveal.visible { opacity: 1; transform: translateY(0); }
@keyframes revealUp { to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .page-wrap { padding: 24px 20px 60px; }
  .hero-title { font-size: 28px; }
  .hero { padding: 48px 0 32px; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .intro-grid { grid-template-columns: 1fr; gap: 32px; text-align: center; }
  .intro-visual { order: -1; }
  .code-frame { max-width: 280px; }
  .values-grid { grid-template-columns: 1fr; }
  .cta-box { padding: 36px 24px; }
  .cta-actions { flex-direction: column; align-items: center; }
}
</style>