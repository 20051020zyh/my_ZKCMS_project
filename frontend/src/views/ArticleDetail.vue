<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleDetail, toggleLike, checkLike, toggleCollect, checkCollect, reportArticle } from '@/api/article'
import { getArticleComments, addComment, likeComment, reportComment } from '@/api/comment'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<any>({})
const comments = ref<any[]>([])
const loading = ref(true)
const commentText = ref('')
const commentLoading = ref(false)
const liked = ref(false)
const collected = ref(false)
const readProgress = ref(0)
const showBackTop = ref(false)
const reportVisible = ref(false)
const reportReason = ref('')
const reportType = ref(1)
const reportSubmitting = ref(false)
const commentSuccessVisible = ref(false)
const reportSuccessVisible = ref(false)

// 二级回复状态
const replyToId = ref<number | null>(null)
const replyToUser = ref('')
const replyToUserId = ref(0)
const replyText = ref('')
const replyLoading = ref(false)

// 右侧抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'new' | 'reply'>('new')

// 评论举报状态
const commentReportVisible = ref(false)
const commentReportTarget = ref<any>(null)
const commentReportReason = ref('')
const commentReportType = ref(1)
const commentReportSubmitting = ref(false)

const bgCanvas = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0

const reportReasons = [
  { label: '虚假信息', value: 1 },
  { label: '色情低俗', value: 2 },
  { label: '违法违规', value: 3 },
  { label: '侵权抄袭', value: 4 },
  { label: '垃圾广告', value: 5 },
  { label: '其他原因', value: 6 },
]

interface Orb {
  x: number; y: number; r: number; dx: number; dy: number
  hue: number; alpha: number; pulse: number; pulseSpeed: number
}

const startBackground = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const orbs: Orb[] = []
  const count = 6

  for (let i = 0; i < count; i++) {
    orbs.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: 100 + Math.random() * 200,
      dx: (Math.random() - 0.5) * 0.3,
      dy: (Math.random() - 0.5) * 0.25,
      hue: [25, 35, 42, 340, 355, 15][i],
      alpha: 0.06 + Math.random() * 0.07,
      pulse: Math.random() * Math.PI * 2,
      pulseSpeed: 0.003 + Math.random() * 0.006
    })
  }

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (const orb of orbs) {
      orb.x += orb.dx
      orb.y += orb.dy
      orb.pulse += orb.pulseSpeed

      const pulseScale = 1 + Math.sin(orb.pulse) * 0.15
      const currentR = orb.r * pulseScale

      if (orb.x - currentR > canvas.width) orb.x = -currentR
      if (orb.x + currentR < 0) orb.x = canvas.width + currentR
      if (orb.y - currentR > canvas.height) orb.y = -currentR
      if (orb.y + currentR < 0) orb.y = canvas.height + currentR

      const gradient = ctx.createRadialGradient(orb.x, orb.y, 0, orb.x, orb.y, currentR)
      gradient.addColorStop(0, `hsla(${orb.hue}, 70%, 65%, ${orb.alpha * 1.5})`)
      gradient.addColorStop(0.5, `hsla(${orb.hue}, 60%, 70%, ${orb.alpha})`)
      gradient.addColorStop(1, `hsla(${orb.hue}, 50%, 80%, 0)`)

      ctx.beginPath()
      ctx.arc(orb.x, orb.y, currentR, 0, Math.PI * 2)
      ctx.fillStyle = gradient
      ctx.fill()
    }

    animFrameId = requestAnimationFrame(draw)
  }
  draw()
}

const fetchArticle = async () => {
  try {
    const id = Number(route.params.id)
    const res: any = await getArticleDetail(id)
    article.value = res.data || {}

    if (userStore.token) {
      checkLikeStatus()
      checkCollectStatus()
    }
  } catch {
    ElMessage.error('文章不存在')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const checkLikeStatus = async () => {
  try {
    const res: any = await checkLike(article.value.id)
    liked.value = res.data === true
  } catch { /* ignore */ }
}

const checkCollectStatus = async () => {
  try {
    const res: any = await checkCollect(article.value.id)
    collected.value = res.data === true
  } catch { /* ignore */ }
}

const fetchComments = async () => {
  try {
    const id = Number(route.params.id)
    console.log('请求评论，文章ID:', id)
    const res: any = await getArticleComments(id, { pageNum: 1, pageSize: 50 })
    console.log('评论接口返回:', res)
    comments.value = res.data?.records || []
    console.log('评论数据:', comments.value)
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const handleLike = async () => {
  if (!userStore.checkLogin('请先登录')) return
  try {
    const res: any = await toggleLike(article.value.id)
    const { likeCount, isLike } = res.data || res
    article.value.likeCount = likeCount
    liked.value = isLike
  } catch { /* ignore */ }
}

const handleCollect = async () => {
  if (!userStore.checkLogin('请先登录')) return
  try {
    const res: any = await toggleCollect(article.value.id)
    const { collectCount, isCollect } = res.data || res
    article.value.collectCount = collectCount
    collected.value = isCollect
    ElMessage.success(isCollect ? '已收藏' : '已取消收藏')
  } catch { /* ignore */ }
}

const handleShare = async () => {
  const url = window.location.href
  if (navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(url)
      ElMessage.success('链接已复制到剪贴板')
    } catch {
      ElMessage.info('分享链接: ' + url)
    }
  } else {
    ElMessage.info('分享链接: ' + url)
  }
}

const openReportDialog = () => {
  if (!userStore.checkLogin('请先登录')) return
  reportReason.value = ''
  reportType.value = 1
  reportVisible.value = true
}

const submitReport = async () => {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请填写举报说明')
    return
  }
  reportSubmitting.value = true
  try {
    await reportArticle({
      articleId: article.value.id,
      reportType: reportType.value,
      content: reportReason.value.trim()
    })
    reportVisible.value = false
    reportSuccessVisible.value = true
  } catch {
    ElMessage.error('举报提交失败')
  } finally {
    reportSubmitting.value = false
  }
}

const handleComment = async () => {
  if (!commentText.value.trim()) return
  if (!userStore.checkLogin('请先登录')) return
  commentLoading.value = true
  try {
    await addComment(article.value.id, commentText.value.trim())
    commentText.value = ''
    drawerVisible.value = false
    commentSuccessVisible.value = true
    await fetchComments()
  } catch {
    ElMessage.error('评论失败')
  } finally {
    commentLoading.value = false
  }
}

const openNewComment = () => {
  if (!userStore.checkLogin('请先登录')) return
  drawerMode.value = 'new'
  drawerVisible.value = true
}

const openReply = (comment: any) => {
  if (!userStore.checkLogin('请先登录')) return
  replyToId.value = comment.parentId && comment.parentId > 0 ? comment.parentId : comment.id
  replyToUser.value = comment.userName || ''
  replyToUserId.value = comment.userId || 0
  replyText.value = ''
  drawerMode.value = 'reply'
  drawerVisible.value = true
}

const cancelReply = () => {
  replyToId.value = null
  replyToUser.value = ''
  replyToUserId.value = 0
  replyText.value = ''
  drawerVisible.value = false
}

const handleReplySubmit = async () => {
  if (!replyText.value.trim() || !replyToId.value) return
  replyLoading.value = true
  try {
    await addComment(article.value.id, replyText.value.trim(), replyToId.value, replyToUserId.value)
    replyText.value = ''
    replyToId.value = null
    replyToUser.value = ''
    replyToUserId.value = 0
    drawerVisible.value = false
    commentSuccessVisible.value = true
    await fetchComments()
  } catch {
    ElMessage.error('回复失败')
  } finally {
    replyLoading.value = false
  }
}

const handleCommentLike = async (comment: any) => {
  if (!userStore.checkLogin('请先登录')) return
  try {
    const res: any = await likeComment(article.value.id, comment.id)
    if (res.code === 0) {
      const { likeCount, isLike } = res.data
      comment.likeCount = likeCount
      comment.likedByMe = isLike
    } else {
      comment.likedByMe = !comment.likedByMe
      comment.likeCount = (comment.likeCount || 0) + (comment.likedByMe ? 1 : -1)
    }
  } catch {
    comment.likedByMe = !comment.likedByMe
    comment.likeCount = (comment.likeCount || 0) + (comment.likedByMe ? 1 : -1)
  }
}

const openCommentReport = (comment: any) => {
  if (!userStore.checkLogin('请先登录')) return
  commentReportTarget.value = comment
  commentReportReason.value = ''
  commentReportType.value = 1
  commentReportVisible.value = true
}

const submitCommentReport = async () => {
  if (!commentReportReason.value.trim()) {
    ElMessage.warning('请填写举报说明')
    return
  }
  commentReportSubmitting.value = true
  try {
    await reportComment({
      commentId: commentReportTarget.value.id,
      reportType: commentReportType.value,
      content: commentReportReason.value.trim()
    })
    commentReportVisible.value = false
    ElMessage.success('举报已提交')
  } catch {
    ElMessage.error('举报提交失败')
  } finally {
    commentReportSubmitting.value = false
  }
}

const handleScroll = () => {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readProgress.value = docHeight > 0 ? Math.min((scrollTop / docHeight) * 100, 100) : 0
  showBackTop.value = scrollTop > 400
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatFullDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const formatShortDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  await nextTick()
  startBackground()
  fetchArticle()
  fetchComments()
})

watch(() => route.params.id, (newId) => {
  if (newId) {
    loading.value = true
    fetchArticle()
    fetchComments()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (animFrameId) cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <div class="detail-page">
    <canvas ref="bgCanvas" class="bg-canvas" />

    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: readProgress + '%' }" />
    </div>

    <button v-show="showBackTop" class="btn-back-top" @click="scrollToTop">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 15l-6-6-6 6"/></svg>
    </button>

    <header class="detail-nav">
      <div class="nav-inner">
        <button class="nav-back" @click="router.push('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          <span>返回发现</span>
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="loader-ring"><div /><div /><div /></div>
      <p>正在加载文章...</p>
    </div>

    <template v-else>
      <div class="content-layout">
        <!-- ==================== 左侧：文章正文 ==================== -->
        <main class="layout-left">
          <article class="article-main">
            <div class="article-header">
              <h1 class="article-title">{{ article.title }}</h1>
              <div class="article-meta">
                <span v-if="article.categoryName" class="meta-tag">{{ article.categoryName }}</span>
                <span v-if="article.categoryName" class="meta-divider"></span>
                <span class="meta-item">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  浏览量 {{ article.viewCount || 0 }}
                </span>
                <span class="meta-divider"></span>
                <span class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  于 {{ formatFullDate(article.createTime) }} 发布
                </span>
              </div>
            </div>

            <div v-if="article.tagNames && article.tagNames.length" class="article-tags">
              <span v-for="tag in article.tagNames" :key="tag" class="article-tag">{{ tag }}</span>
            </div>

            <div class="article-body-wrap">
              <div class="article-body" v-html="article.content" />
            </div>
          </article>
        </main>

        <!-- ==================== 右侧：作者·图片·互动·评论 ==================== -->
        <aside class="layout-right">
          <div class="sidebar-sticky">
            <!-- 作者卡片 -->
            <div class="profile-card">
              <div class="profile-glow"></div>
              <div class="profile-avatar">
                <el-avatar :size="60" :src="article.user_pic || ''">
                  <template #default>
                    <svg viewBox="0 0 24 24" fill="currentColor" style="width:30px;height:30px;opacity:0.5"><path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v1.2c0 .66.54 1.2 1.2 1.2h16.8c.66 0 1.2-.54 1.2-1.2v-1.2c0-3.2-6.4-4.8-9.6-4.8z"/></svg>
                  </template>
                </el-avatar>
                <div class="profile-status"></div>
              </div>
              <div class="profile-info">
                <span class="profile-name">{{ article.username || '匿名用户' }}</span>
                <span class="profile-bio">{{ article.nickname || '这个人很懒，什么都没留下' }}</span>
              </div>
            </div>

            <!-- 封面预览 -->
            <div v-if="article.coverImg" class="cover-thumb">
              <img :src="article.coverImg" :alt="article.title" />
            </div>

            <!-- 互动栏 -->
            <div class="interaction-bar">
              <button :class="['ib-btn', { active: liked }]" @click="handleLike">
                <span class="ib-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
                </span>
                <span class="ib-label">{{ liked ? '已赞' : '点赞' }}</span>
                <span class="ib-count">{{ article.likeCount || 0 }}</span>
              </button>
              <button :class="['ib-btn', { active: collected }]" @click="handleCollect">
                <span class="ib-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" :fill="collected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                </span>
                <span class="ib-label">{{ collected ? '已藏' : '收藏' }}</span>
                <span class="ib-count">{{ article.collectCount || 0 }}</span>
              </button>
              <button class="ib-btn" @click="handleShare">
                <span class="ib-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 3.98"/></svg>
                </span>
                <span class="ib-label">分享</span>
              </button>
              <button class="ib-btn ib-btn-warn" @click="openReportDialog">
                <span class="ib-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
                </span>
                <span class="ib-label">举报</span>
              </button>
            </div>

            <!-- 评论区 -->
            <div class="comment-panel">
              <div class="cp-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                <span>评论</span>
                <span class="cp-count">{{ comments.length }}</span>
              </div>

                <div class="cp-input-area" @click="openNewComment">
                <textarea
                  class="cp-textarea"
                  placeholder="分享你的想法..."
                  rows="2"
                  readonly
                />
                <div class="cp-hint">点击输入评论</div>
              </div>

              <div v-if="!comments.length" class="cp-empty">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.3"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                <p>暂无评论，来聊聊你的想法吧</p>
              </div>

              <div class="cp-list">
                <div v-for="comment in comments" :key="comment.id" class="cp-item">
                  <div class="cpi-avatar">
                    <img v-if="comment.avatar" :src="comment.avatar" class="cpi-avatar-img" />
                    <span v-else>{{ (comment.userName || '匿').charAt(0) }}</span>
                  </div>
                  <div class="cpi-body">
                    <div class="cpi-top">
                      <span class="cpi-author">{{ comment.userName || '匿名用户' }}</span>
                      <span class="cpi-time">{{ formatFullDate(comment.createTime) }}</span>
                    </div>
                    <p class="cpi-text">{{ comment.content }}</p>
                    <div class="cpi-actions">
                      <button
                        :class="['cpi-action-btn', { liked: comment.likedByMe }]"
                        @click="handleCommentLike(comment)"
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" :fill="comment.likedByMe ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z"/></svg>
                        <span>{{ comment.likeCount || 0 }}</span>
                      </button>
                      <button class="cpi-action-btn" @click="openReply(comment)">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                        <span>回复</span>
                      </button>
                      <button class="cpi-action-btn cpi-report" @click="openCommentReport(comment)">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
                        <span>举报</span>
                      </button>
                    </div>

                    <!-- 二级回复列表 -->
                    <div v-if="comment.replyList && comment.replyList.length" class="cpi-replies">
                      <div v-for="reply in comment.replyList" :key="reply.id" class="cpi-reply-item">
                        <div class="cpi-reply-avatar">
                          <img v-if="reply.avatar" :src="reply.avatar" class="cpi-avatar-img" />
                          <span v-else>{{ (reply.userName || '匿').charAt(0) }}</span>
                        </div>
                        <div class="cpi-reply-body">
                          <div class="cpi-reply-top">
                            <span class="cpi-author">{{ reply.userName || '匿名用户' }}</span>
                            <span v-if="reply.replyUserName" class="cpi-reply-at">回复 @{{ reply.replyUserName }}</span>
                            <span class="cpi-time">{{ formatShortDate(reply.createTime) }}</span>
                          </div>
                          <p class="cpi-text">{{ reply.content }}</p>
                          <div class="cpi-actions">
                            <button
                              :class="['cpi-action-btn', { liked: reply.likedByMe }]"
                              @click="handleCommentLike(reply)"
                            >
                              <svg width="13" height="13" viewBox="0 0 24 24" :fill="reply.likedByMe ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z"/></svg>
                              <span>{{ reply.likeCount || 0 }}</span>
                            </button>
                            <button class="cpi-action-btn" @click="openReply(reply)">
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                              <span>回复</span>
                            </button>
                            <button class="cpi-action-btn cpi-report" @click="openCommentReport(reply)">
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
                              <span>举报</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </aside>
      </div>
    </template>

    <!-- 举报弹窗 -->
    <Teleport to="body">
      <div v-if="reportVisible" class="report-overlay" @click.self="reportVisible = false">
        <div class="report-dialog">
          <div class="rd-header">
            <h3 class="rd-title">举报文章</h3>
            <button class="rd-close" @click="reportVisible = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="rd-body">
            <div class="rd-types">
              <button
                v-for="r in reportReasons"
                :key="r.value"
                :class="['rd-type-btn', { active: reportType === r.value }]"
                @click="reportType = r.value"
              >{{ r.label }}</button>
            </div>
            <textarea
              v-model="reportReason"
              class="rd-textarea"
              placeholder="请详细描述举报原因..."
              rows="4"
            />
          </div>
          <div class="rd-footer">
            <button class="rd-cancel" @click="reportVisible = false">取消</button>
            <button class="rd-submit" :disabled="reportSubmitting" @click="submitReport">
              {{ reportSubmitting ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 评论成功弹窗 -->
    <Teleport to="body">
      <Transition name="cs-modal">
        <div v-if="commentSuccessVisible" class="cs-overlay" @click.self="commentSuccessVisible = false">
          <div class="cs-dialog">
            <div class="cs-glow"></div>
            <div class="cs-icon-wrap">
              <div class="cs-icon-ring">
                <svg class="cs-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
            </div>
            <h3 class="cs-title">评论发表成功</h3>
            <p class="cs-desc">感谢您的参与！评论已提交<br/>等待管理员审核通过后即可展示</p>
            <div class="cs-badge-row">
              <span class="cs-badge">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                内容审核中
              </span>
            </div>
            <button class="cs-btn" @click="commentSuccessVisible = false">
              我知道了
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 评论举报弹窗 -->
    <Teleport to="body">
      <div v-if="commentReportVisible" class="report-overlay" @click.self="commentReportVisible = false">
        <div class="report-dialog">
          <div class="rd-header">
            <h3 class="rd-title">举报评论</h3>
            <button class="rd-close" @click="commentReportVisible = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="rd-body">
            <div class="rd-types">
              <button
                v-for="r in reportReasons"
                :key="r.value"
                :class="['rd-type-btn', { active: commentReportType === r.value }]"
                @click="commentReportType = r.value"
              >{{ r.label }}</button>
            </div>
            <textarea
              v-model="commentReportReason"
              class="rd-textarea"
              placeholder="请详细描述举报原因..."
              rows="4"
            />
          </div>
          <div class="rd-footer">
            <button class="rd-cancel" @click="commentReportVisible = false">取消</button>
            <button class="rd-submit" :disabled="commentReportSubmitting" @click="submitCommentReport">
              {{ commentReportSubmitting ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 举报成功弹窗 -->
    <Teleport to="body">
      <Transition name="cs-modal">
        <div v-if="reportSuccessVisible" class="cs-overlay" @click.self="reportSuccessVisible = false">
          <div class="cs-dialog">
            <div class="cs-glow" style="background: radial-gradient(circle at 50% 0%, rgba(239,68,68,0.25), transparent 70%);"></div>
            <div class="cs-icon-wrap">
              <div class="cs-icon-ring" style="border-color: rgba(239,68,68,0.2); background: linear-gradient(135deg, #ef4444, #dc2626);">
                <svg class="cs-check" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
            </div>
            <h3 class="cs-title">举报提交成功</h3>
            <p class="cs-desc">感谢您的反馈！举报信息已提交<br/>等待管理员审核处理</p>
            <div class="cs-badge-row">
              <span class="cs-badge" style="background: rgba(239,68,68,0.1); color: #ef4444; border-color: rgba(239,68,68,0.15);">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                审核处理中
              </span>
            </div>
            <button class="cs-btn" @click="reportSuccessVisible = false" style="--btn-color: #ef4444;">
              我知道了
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 右侧回复抽屉 -->
    <Transition name="reply-drawer">
      <div v-if="drawerVisible" class="reply-drawer-overlay" @click.self="cancelReply">
        <div class="reply-drawer">
          <div class="reply-drawer-header">
            <div class="reply-drawer-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
              <span>{{ drawerMode === 'reply' ? '回复 @' + replyToUser : '发表评论' }}</span>
            </div>
            <button class="reply-drawer-close" @click="cancelReply">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div class="reply-drawer-input-row">
            <input
              v-if="drawerMode === 'reply'"
              v-model="replyText"
              class="reply-drawer-input"
              type="text"
              placeholder="输入回复内容..."
              @keyup.enter="handleReplySubmit"
            />
            <input
              v-else
              v-model="commentText"
              class="reply-drawer-input"
              type="text"
              placeholder="分享你的想法..."
              @keyup.enter="handleComment"
            />
            <button
              v-if="drawerMode === 'reply'"
              class="reply-drawer-send"
              :disabled="!replyText.trim() || replyLoading"
              @click="handleReplySubmit"
            >{{ replyLoading ? '发送中...' : '发送' }}</button>
            <button
              v-else
              class="reply-drawer-send"
              :disabled="!commentText.trim() || commentLoading"
              @click="handleComment"
            >{{ commentLoading ? '发送中...' : '发送' }}</button>
          </div>

          <div class="reply-drawer-divider"></div>

          <div class="reply-drawer-comments">
            <div class="reply-drawer-comments-header">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
              <span>全部评论</span>
              <span class="rdc-count">{{ comments.length }}</span>
            </div>
            <div v-if="!comments.length" class="rdc-empty">
              <p>暂无评论</p>
            </div>
            <div v-else class="rdc-list">
              <div v-for="comment in comments" :key="comment.id" class="rdc-item">
                <div class="rdc-item-avatar">
                  <img v-if="comment.avatar" :src="comment.avatar" class="rdc-avatar-img" />
                  <span v-else>{{ (comment.userName || '匿').charAt(0) }}</span>
                </div>
                <div class="rdc-item-body">
                  <div class="rdc-item-top">
                    <span class="rdc-item-author">{{ comment.userName || '匿名用户' }}</span>
                    <span class="rdc-item-time">{{ formatShortDate(comment.createTime) }}</span>
                  </div>
                  <p class="rdc-item-text">{{ comment.content }}</p>
                  <div class="rdc-item-actions">
                    <button
                      :class="['rdc-action-btn', { liked: comment.likedByMe }]"
                      @click="handleCommentLike(comment)"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" :fill="comment.likedByMe ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z"/></svg>
                      <span>{{ comment.likeCount || 0 }}</span>
                    </button>
                    <button class="rdc-action-btn" @click="openReply(comment)">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                      <span>回复</span>
                    </button>
                  </div>
                  <!-- 二级回复 -->
                  <div v-if="comment.replyList && comment.replyList.length" class="rdc-replies">
                    <div v-for="reply in comment.replyList" :key="reply.id" class="rdc-reply-item">
                      <div class="rdc-reply-avatar">
                        <img v-if="reply.avatar" :src="reply.avatar" class="rdc-avatar-img" />
                        <span v-else>{{ (reply.userName || '匿').charAt(0) }}</span>
                      </div>
                      <div class="rdc-reply-body">
                        <div class="rdc-reply-top">
                          <span class="rdc-reply-author">{{ reply.userName || '匿名用户' }}</span>
                          <span v-if="reply.replyUserName" class="rdc-reply-at">@{{ reply.replyUserName }}</span>
                          <span class="rdc-reply-time">{{ formatShortDate(reply.createTime) }}</span>
                        </div>
                        <p class="rdc-reply-text">{{ reply.content }}</p>
                        <div class="rdc-item-actions">
                          <button
                            :class="['rdc-action-btn', { liked: reply.likedByMe }]"
                            @click="handleCommentLike(reply)"
                          >
                            <svg width="12" height="12" viewBox="0 0 24 24" :fill="reply.likedByMe ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z"/></svg>
                            <span>{{ reply.likeCount || 0 }}</span>
                          </button>
                          <button class="rdc-action-btn" @click="openReply(reply)">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                            <span>回复</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:opsz@9..40&family=Lora:wght@400;500;600&family=Noto+Serif+SC:wght@300;400;500;600;700&family=Playfair+Display:wght@500;600;700&display=swap');

.detail-page {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(175deg, #fdfaf5 0%, #f9f3ea 25%, #faf6ef 50%, #fdf9f3 75%, #fefcf8 100%);
  overflow-x: hidden;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ========== Progress Bar ========== */
.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  z-index: 1000;
  background: transparent;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #d4a574, #c4806a, #b8956a);
  transition: width 0.15s ease-out;
  border-radius: 0 2px 2px 0;
}

/* ========== Back to Top ========== */
.btn-back-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  z-index: 999;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1.5px solid rgba(200, 164, 92, 0.2);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  color: #b8956a;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(180, 140, 100, 0.1);
  font-family: inherit;
}

.btn-back-top:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(180, 140, 100, 0.18);
  border-color: rgba(200, 164, 92, 0.4);
  color: #a0774a;
}

/* ========== Nav ========== */
.detail-nav {
  position: sticky;
  top: 3px;
  z-index: 100;
  padding: 12px 0;
}

.nav-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 28px;
}

.nav-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 12px;
  border: 1.5px solid rgba(200, 164, 92, 0.15);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px);
  color: #8a7d6e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  box-shadow: 0 1px 4px rgba(180, 140, 100, 0.04);
}

.nav-back:hover {
  background: rgba(255, 255, 255, 0.9);
  color: #c8a45c;
  border-color: rgba(200, 164, 92, 0.3);
  box-shadow: 0 2px 10px rgba(200, 164, 92, 0.1);
}

/* ========== Loading ========== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 140px 20px;
  position: relative;
  z-index: 1;
}

.loader-ring {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
}

.loader-ring div {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #c8a45c;
  animation: loaderBounce 0.6s ease-in-out infinite alternate;
}

.loader-ring div:nth-child(2) { animation-delay: 0.15s; }
.loader-ring div:nth-child(3) { animation-delay: 0.3s; }

@keyframes loaderBounce {
  to { transform: translateY(-10px); opacity: 0.4; }
}

.loading-state p {
  font-size: 14px;
  color: #b8a894;
  font-weight: 500;
}

/* ========== Two-Column Layout ========== */
.content-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 28px 80px;
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 48px;
  align-items: start;
  position: relative;
  z-index: 1;
}

/* ========== Left: Article Content ========== */
.layout-left {
  min-width: 0;
}

.article-main {
  animation: contentFadeUp 0.6s ease-out;
}

@keyframes contentFadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-header {
  margin-bottom: 40px;
  padding-bottom: 32px;
  border-bottom: 1px solid rgba(200, 180, 150, 0.2);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 28px;
}

.article-tag {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  color: #818cf8;
  background: rgba(99,102,241,0.06);
  border-radius: 10px;
  transition: all 0.2s ease;
}

.article-tag:hover {
  background: rgba(99,102,241,0.12);
  color: #6366f1;
}

.meta-tag {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  padding: 3px 10px;
  border-radius: 5px;
  background: rgba(200, 164, 92, 0.08);
  color: #b8956a;
  border: 1px solid rgba(200, 164, 92, 0.12);
}

.article-title {
  font-size: 36px;
  font-weight: 400;
  color: #1f1a12;
  line-height: 1.3;
  margin: 0 0 20px;
  letter-spacing: -0.3px;
  font-family: 'DM Serif Display', 'Noto Serif SC', Georgia, serif;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #9a8e7c;
  font-weight: 500;
}

.meta-item svg {
  opacity: 0.5;
  flex-shrink: 0;
}

.meta-divider {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #d4c8b8;
}

/* ========== Article Body ========== */
.article-body-wrap {
  margin-bottom: 32px;
}

.article-body {
  font-size: 17px;
  line-height: 2;
  color: #3a3228;
  font-family: 'Lora', 'Noto Serif SC', Georgia, serif;
}

.article-body :deep(p) {
  margin-bottom: 24px;
  letter-spacing: 0.3px;
}

.article-body :deep(img) {
  max-width: 100%;
  border-radius: 14px;
  margin: 28px 0;
  box-shadow: 0 4px 20px rgba(140, 110, 80, 0.1);
}

.article-body :deep(h2) {
  font-size: 26px;
  font-weight: 700;
  color: #2d2418;
  margin: 44px 0 18px;
  letter-spacing: -0.3px;
  padding-bottom: 10px;
  border-bottom: 2px solid rgba(200, 164, 92, 0.12);
}

.article-body :deep(h3) {
  font-size: 20px;
  font-weight: 700;
  color: #3d3629;
  margin: 36px 0 14px;
}

.article-body :deep(blockquote) {
  border-left: 3px solid #c9a96e;
  padding: 14px 24px;
  margin: 24px 0;
  background: rgba(200, 164, 92, 0.04);
  border-radius: 0 12px 12px 0;
  color: #6b5f4e;
  font-style: italic;
  font-size: 16px;
}

.article-body :deep(code) {
  background: rgba(200, 164, 92, 0.08);
  padding: 2px 8px;
  border-radius: 5px;
  font-size: 14px;
  color: #a0774a;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.article-body :deep(pre) {
  background: #1f1a12;
  color: #e8dcc8;
  padding: 28px;
  border-radius: 14px;
  overflow-x: auto;
  margin: 28px 0;
  font-size: 14px;
  line-height: 1.7;
}

.article-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.article-body :deep(a) {
  color: #b8956a;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.2s;
}

.article-body :deep(a:hover) {
  color: #c9a96e;
}

/* ========== Tags ========== */
.article-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 40px;
  padding-top: 28px;
  border-top: 1px solid rgba(200, 180, 150, 0.15);
}

.tag-item {
  padding: 6px 18px;
  background: rgba(200, 164, 92, 0.06);
  color: #8a7d6b;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(200, 164, 92, 0.12);
  transition: all 0.25s;
  cursor: default;
}

.tag-item:hover {
  background: rgba(200, 164, 92, 0.12);
  color: #b8956a;
  border-color: rgba(200, 164, 92, 0.25);
}

/* ========== Right Sidebar ========== */
.layout-right {
  position: relative;
}

.sidebar-sticky {
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== Profile Card ========== */
.profile-card {
  position: relative;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(200, 180, 150, 0.12);
  border-radius: 18px;
  padding: 28px 24px 24px;
  text-align: center;
  overflow: hidden;
  box-shadow:
    0 1px 3px rgba(180, 150, 110, 0.03),
    0 8px 24px rgba(180, 150, 110, 0.06);
}

.profile-glow {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 164, 92, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.profile-avatar {
  position: relative;
  display: inline-block;
  margin-bottom: 14px;
}

.profile-avatar .el-avatar {
  border: 3px solid rgba(200, 164, 92, 0.15);
  box-shadow: 0 4px 16px rgba(180, 140, 100, 0.1);
  transition: transform 0.3s ease;
}

.profile-card:hover .profile-avatar .el-avatar {
  transform: scale(1.04);
}

.profile-status {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #7bc47f;
  border: 2.5px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-name {
  font-size: 16px;
  font-weight: 700;
  color: #2d2418;
}

.profile-bio {
  font-size: 12px;
  color: #9a8e7c;
  font-weight: 500;
  line-height: 1.4;
}

/* ========== Cover Thumb ========== */
.cover-thumb {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(180, 150, 110, 0.08);
}

.cover-thumb img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.cover-thumb:hover img {
  transform: scale(1.03);
}

/* ========== Interaction Bar ========== */
.interaction-bar {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.ib-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 10px 14px;
  border-radius: 14px;
  border: 1px solid rgba(200, 180, 150, 0.1);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  color: #5c5143;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  box-shadow:
    0 1px 3px rgba(180, 150, 110, 0.03),
    0 4px 12px rgba(180, 150, 110, 0.04);
}

.ib-btn:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(200, 164, 92, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(180, 150, 110, 0.08);
  color: #b8956a;
}

.ib-btn:active {
  transform: translateY(0) scale(0.97);
}

.ib-btn.active {
  background: rgba(200, 164, 92, 0.08);
  border-color: rgba(200, 164, 92, 0.2);
  color: #a0774a;
}

.ib-btn.ib-btn-warn:hover {
  color: #c4806a;
  border-color: rgba(200, 100, 80, 0.2);
}

.ib-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(200, 164, 92, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.ib-btn:hover .ib-icon {
  background: rgba(200, 164, 92, 0.1);
}

.ib-btn.active .ib-icon {
  background: rgba(200, 164, 92, 0.14);
}

.ib-btn.ib-btn-warn:hover .ib-icon {
  background: rgba(200, 100, 80, 0.08);
}

.ib-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.ib-count {
  font-size: 11px;
  font-weight: 500;
  color: #b8a894;
}

.ib-btn.active .ib-count {
  color: #a0774a;
}

.ib-btn.active svg {
  animation: pulseHeart 0.5s ease;
}

@keyframes pulseHeart {
  0%, 100% { transform: scale(1); }
  25% { transform: scale(1.2); }
  50% { transform: scale(1); }
  75% { transform: scale(1.1); }
}

/* ========== Comment Panel ========== */
.comment-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(200, 180, 150, 0.1);
  border-radius: 18px;
  padding: 22px 20px;
  box-shadow:
    0 1px 3px rgba(180, 150, 110, 0.03),
    0 6px 20px rgba(180, 150, 110, 0.05);
}

.cp-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #3d3629;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(200, 180, 150, 0.1);
}

.cp-header svg {
  opacity: 0.4;
}

.cp-count {
  font-size: 12px;
  font-weight: 500;
  color: #b8956a;
  background: rgba(200, 164, 92, 0.1);
  padding: 1px 9px;
  border-radius: 9px;
  margin-left: auto;
}

.cp-input-area {
  margin-bottom: 16px;
}

.cp-textarea {
  width: 100%;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1.5px solid rgba(200, 180, 150, 0.18);
  background: rgba(255, 253, 249, 0.8);
  color: #3d3629;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.6;
  outline: none;
  resize: vertical;
  transition: all 0.25s;
  box-sizing: border-box;
}

.cp-textarea:focus {
  border-color: rgba(200, 164, 92, 0.35);
  background: #fffdf9;
  box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.06);
}

.cp-textarea::placeholder {
  color: #c8bda8;
  font-size: 12px;
}

.cp-hint {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  font-size: 11px;
  color: #c8bda8;
  cursor: text;
}

.cp-submit {
  padding: 8px 20px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #c9a96e, #b8956a);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  letter-spacing: 0.3px;
}

.cp-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(200, 164, 92, 0.35);
}

.cp-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========== Comment List ========== */
.cp-empty {
  text-align: center;
  padding: 28px 0 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.cp-empty p {
  font-size: 12px;
  color: #c8bda8;
  margin: 0;
}

.cp-list {
  display: flex;
  flex-direction: column;
  max-height: 420px;
  overflow-y: auto;
}

.cp-list::-webkit-scrollbar {
  width: 3px;
}

.cp-list::-webkit-scrollbar-track {
  background: transparent;
}

.cp-list::-webkit-scrollbar-thumb {
  background: rgba(200, 180, 150, 0.3);
  border-radius: 3px;
}

.cp-item {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(200, 180, 150, 0.06);
}

.cp-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.cpi-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #a0927c, #8a7d6e);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

.cpi-body {
  flex: 1;
  min-width: 0;
}

.cpi-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.cpi-author {
  font-size: 13px;
  font-weight: 600;
  color: #3d3629;
}

.cpi-time {
  font-size: 11px;
  color: #c8bda8;
}

.cpi-text {
  font-size: 13px;
  color: #5c5143;
  line-height: 1.65;
  margin: 0 0 8px;
}

.cpi-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 6px;
}

.cpi-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #b8a894;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.cpi-action-btn:hover {
  color: #c4806a;
  background: rgba(200, 164, 92, 0.06);
}
.cpi-action-btn.liked {
  color: #c4806a;
}
.cpi-action-btn.cpi-report:hover {
  color: #ef4444;
  background: rgba(239,68,68,0.06);
}

/* ===== 二级回复 ===== */
.cpi-replies {
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid rgba(200, 180, 150, 0.15);
}

.cpi-reply-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(200, 180, 150, 0.06);
}
.cpi-reply-item:last-child { border-bottom: none; }

.cpi-reply-avatar {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8dcc8, #d4c5a8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #8a7a64;
  overflow: hidden;
}

.cpi-reply-body {
  flex: 1;
  min-width: 0;
}

.cpi-reply-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 2px;
}

.cpi-reply-at {
  font-size: 11px;
  color: #c4806a;
  font-weight: 500;
}

.cpi-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

/* ===== 右侧回复抽屉 ===== */
.reply-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  justify-content: flex-end;
  background: rgba(20, 15, 10, 0.55);
}

.reply-drawer-overlay.reply-drawer-enter-active {
  animation: drawerOverlayIn 0.3s ease;
}
.reply-drawer-overlay.reply-drawer-leave-active {
  animation: drawerOverlayIn 0.3s ease reverse;
}

@keyframes drawerOverlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes drawerIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.reply-drawer {
  width: 440px;
  max-width: 92vw;
  height: 100%;
  background: linear-gradient(180deg, #fefcf8 0%, #fdfaf5 100%);
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 40px rgba(60, 40, 20, 0.15);
}

.reply-drawer-overlay.reply-drawer-enter-active .reply-drawer {
  animation: drawerIn 0.35s cubic-bezier(0.2, 0, 0, 1);
}
.reply-drawer-overlay.reply-drawer-leave-active .reply-drawer {
  animation: drawerIn 0.3s cubic-bezier(0.2, 0, 0, 1) reverse;
}

.reply-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(200, 180, 150, 0.1);
  flex-shrink: 0;
}

.reply-drawer-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #3d3629;
}

.reply-drawer-title svg {
  color: #c4806a;
}

.reply-drawer-close {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: rgba(200, 180, 150, 0.1);
  color: #a0927c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.reply-drawer-close:hover { background: rgba(200, 180, 150, 0.2); color: #8a7a64; }

.reply-drawer-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  flex-shrink: 0;
}

.reply-drawer-input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border: 1.5px solid rgba(200, 180, 150, 0.2);
  border-radius: 10px;
  background: rgba(255, 253, 249, 0.8);
  color: #3d3629;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: all 0.25s;
}
.reply-drawer-input::placeholder { color: #c8bda8; }
.reply-drawer-input:focus { border-color: rgba(200, 164, 92, 0.35); background: #fffdf9; }

.reply-drawer-send {
  height: 40px;
  padding: 0 22px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  white-space: nowrap;
}
.reply-drawer-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(200,164,92,0.35);
}
.reply-drawer-send:disabled { opacity: 0.4; cursor: not-allowed; }

.reply-drawer-divider {
  height: 1px;
  background: rgba(200, 180, 150, 0.12);
  margin: 0 20px;
  flex-shrink: 0;
}

.reply-drawer-comments {
  flex: 1;
  overflow-y: auto;
  padding: 14px 20px 20px;
}

.reply-drawer-comments::-webkit-scrollbar { width: 4px; }
.reply-drawer-comments::-webkit-scrollbar-track { background: transparent; }
.reply-drawer-comments::-webkit-scrollbar-thumb { background: rgba(200,180,150,0.2); border-radius: 2px; }

.reply-drawer-comments-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #5c5143;
  margin-bottom: 14px;
}
.reply-drawer-comments-header svg { color: #c4806a; }

.rdc-count {
  font-size: 11px;
  color: #b8956a;
  background: rgba(200,164,92,0.1);
  padding: 1px 8px;
  border-radius: 8px;
  margin-left: auto;
}

.rdc-empty {
  text-align: center;
  padding: 40px 0;
  color: #c8bda8;
  font-size: 13px;
}

.rdc-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rdc-item {
  display: flex;
  gap: 10px;
}

.rdc-item-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8dcc8, #d4c5a8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #8a7a64;
  overflow: hidden;
}

.rdc-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.rdc-item-body { flex: 1; min-width: 0; }

.rdc-item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.rdc-item-author {
  font-size: 13px;
  font-weight: 600;
  color: #3d3629;
}

.rdc-item-time {
  font-size: 10px;
  color: #c8bda8;
}

.rdc-item-text {
  font-size: 13px;
  color: #5c5143;
  line-height: 1.6;
  margin: 0 0 6px;
}

.rdc-item-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.rdc-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 7px;
  border-radius: 5px;
  border: none;
  background: transparent;
  color: #b8a894;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.rdc-action-btn:hover { color: #c4806a; background: rgba(200,164,92,0.06); }
.rdc-action-btn.liked { color: #c4806a; }

/* 抽屉内二级回复 */
.rdc-replies {
  margin-top: 8px;
  padding-left: 10px;
  border-left: 2px solid rgba(200,180,150,0.12);
}

.rdc-reply-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
}

.rdc-reply-avatar {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8dcc8, #d4c5a8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
  color: #8a7a64;
  overflow: hidden;
}

.rdc-reply-body { flex: 1; min-width: 0; }

.rdc-reply-top {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 1px;
}

.rdc-reply-author {
  font-size: 12px;
  font-weight: 600;
  color: #3d3629;
}

.rdc-reply-at {
  font-size: 11px;
  color: #c4806a;
  font-weight: 500;
}

.rdc-reply-time {
  font-size: 10px;
  color: #c8bda8;
  margin-left: auto;
}

.rdc-reply-text {
  font-size: 12px;
  color: #5c5143;
  line-height: 1.5;
  margin: 0 0 4px;
}

/* ========== Report Dialog ========== */
.report-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 20, 10, 0.35);
  backdrop-filter: blur(4px);
  animation: overlayIn 0.2s ease;
}

@keyframes overlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.report-dialog {
  width: 420px;
  max-width: 92vw;
  background: #fffdf8;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(140, 100, 60, 0.18);
  overflow: hidden;
  animation: dialogIn 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes dialogIn {
  from { opacity: 0; transform: translateY(16px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.rd-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.rd-title {
  font-size: 17px;
  font-weight: 700;
  color: #3d3629;
  margin: 0;
}

.rd-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: rgba(200, 180, 150, 0.1);
  color: #a0927c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.rd-close:hover {
  background: rgba(200, 180, 150, 0.2);
  color: #5c5143;
}

.rd-body {
  padding: 16px 24px 20px;
}

.rd-types {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.rd-type-btn {
  padding: 6px 16px;
  border-radius: 8px;
  border: 1.5px solid rgba(200, 180, 150, 0.2);
  background: transparent;
  color: #8a7d6e;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.rd-type-btn:hover {
  border-color: rgba(200, 164, 92, 0.35);
  color: #b8956a;
}

.rd-type-btn.active {
  background: rgba(200, 164, 92, 0.1);
  border-color: #c8a45c;
  color: #a0774a;
  font-weight: 600;
}

.rd-textarea {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid rgba(200, 180, 150, 0.2);
  background: rgba(254, 252, 248, 0.8);
  color: #3d3629;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.6;
  outline: none;
  resize: vertical;
  transition: all 0.25s;
}

.rd-textarea:focus {
  border-color: rgba(200, 164, 92, 0.4);
  background: #fffdf9;
  box-shadow: 0 0 0 3px rgba(200, 164, 92, 0.06);
}

.rd-textarea::placeholder {
  color: #c8bda8;
}

.rd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}

.rd-cancel {
  padding: 8px 20px;
  border-radius: 10px;
  border: 1.5px solid rgba(200, 180, 150, 0.2);
  background: transparent;
  color: #8a7d6e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.rd-cancel:hover {
  border-color: rgba(200, 180, 150, 0.4);
  color: #5c5143;
}

.rd-submit {
  padding: 8px 20px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s;
  font-family: inherit;
  letter-spacing: 0.3px;
}

.rd-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(200, 164, 92, 0.3);
}

.rd-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========== Responsive ========== */
@media (max-width: 900px) {
  .content-layout {
    grid-template-columns: 1fr;
    padding: 16px 16px 60px;
    gap: 24px;
  }

  .layout-right {
    order: -1;
  }

  .sidebar-sticky {
    position: static;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .actions-card {
    padding-bottom: 16px;
  }

  .comment-card {
    grid-column: 1 / -1;
  }

  .detail-nav {
    padding: 8px 0;
  }

  .nav-inner {
    padding: 0 16px;
  }

  .content-glass {
    padding: 28px 20px;
    border-radius: 16px;
  }

  .hero-title {
    font-size: 26px;
  }

  .hero-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .sidebar-sticky {
    grid-template-columns: 1fr;
  }

  .report-dialog {
    width: 95vw;
  }

  .rd-types {
    gap: 6px;
  }

  .rd-type-btn {
    padding: 5px 12px;
    font-size: 12px;
  }
}

/* ── 评论成功弹窗 ── */
.cs-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 25, 20, 0.5);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.cs-dialog {
  position: relative;
  width: 380px;
  background: linear-gradient(170deg, #fefcf8, #faf5ed);
  border-radius: 24px;
  padding: 48px 36px 36px;
  text-align: center;
  box-shadow:
    0 0 0 1px rgba(200, 164, 92, 0.08),
    0 8px 40px rgba(120, 100, 70, 0.12),
    0 32px 80px rgba(120, 100, 70, 0.08);
  overflow: hidden;
  animation: cs-float-in 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cs-float-in {
  from { opacity: 0; transform: scale(0.92) translateY(24px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.cs-glow {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(106, 155, 138, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.cs-icon-wrap {
  position: relative;
  margin-bottom: 22px;
  z-index: 1;
}

.cs-icon-ring {
  width: 72px;
  height: 72px;
  margin: 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #6a9b8a, #5a8a7a);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 0 4px rgba(106, 155, 138, 0.15),
    0 8px 24px rgba(106, 155, 138, 0.25);
  animation: cs-icon-pop 0.6s 0.1s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes cs-icon-pop {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.cs-check {
  width: 32px;
  height: 32px;
  color: #fff;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.1));
  animation: cs-check-draw 0.6s 0.3s ease both;
  stroke-dasharray: 50;
  stroke-dashoffset: 50;
}

@keyframes cs-check-draw {
  to { stroke-dashoffset: 0; }
}

.cs-title {
  font-size: 20px;
  font-weight: 700;
  color: #2d281f;
  margin: 0 0 8px;
  letter-spacing: -0.3px;
  position: relative;
  z-index: 1;
}

.cs-desc {
  font-size: 14px;
  color: #7a6a5a;
  margin: 0 0 20px;
  line-height: 1.7;
  position: relative;
  z-index: 1;
}

.cs-badge-row {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.cs-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(200, 164, 92, 0.08);
  border: 1px solid rgba(200, 164, 92, 0.15);
  color: #b8956a;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.cs-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 36px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #c8a45c, #b8956a);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
  box-shadow: 0 4px 16px rgba(200, 164, 92, 0.25);
  position: relative;
  z-index: 1;
}

.cs-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(200, 164, 92, 0.35);
  background: linear-gradient(135deg, #d4a574, #c8a45c);
}

.cs-btn:active {
  transform: translateY(0) scale(0.98);
}

.cs-btn svg {
  transition: transform 0.3s ease;
}

.cs-btn:hover svg {
  transform: translateX(3px);
}

.cs-modal-enter-active {
  transition: opacity 0.3s ease;
}
.cs-modal-leave-active {
  transition: opacity 0.25s ease;
}
.cs-modal-enter-from,
.cs-modal-leave-to {
  opacity: 0;
}
.cs-modal-enter-from .cs-dialog {
  animation: none;
  opacity: 0;
  transform: scale(0.92) translateY(24px);
}
</style>