<script setup lang="ts">
import { ref, shallowRef, computed, watch, onMounted, onUnmounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAllCategories } from '@/api/category'
import { getAllTags, addTag } from '@/api/tags'
import { addArticle, scheduleArticle, updateArticle, getArticleDetail } from '@/api/article'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEditMode = computed(() => !!route.query.id)
const articleId = computed(() => Number(route.query.id) || 0)

const categories = ref<any[]>([])
const allTags = ref<any[]>([])
const selectedTagNames = ref<string[]>([])
const newTagInput = ref('')
const showTagInput = ref(false)
const tagInputRef = ref<HTMLInputElement | null>(null)
const MAX_TAGS = 7

const remainingTags = computed(() => MAX_TAGS - selectedTagNames.value.length)

const submitting = ref(false)
const imageUploading = ref(false)
const editorRef = shallowRef<any>(null)

const toolbarConfig = {
  excludeKeys: []
}

const editorConfig = {
  placeholder: '开始写作...',
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 10 * 1024 * 1024,
      customUpload: async (file: File, insertFn: any) => {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await request.post('/upload', formData)
          if (res.data) {
            insertFn(res.data)
          }
        } catch (e) {
          ElMessage.error('图片上传失败')
        }
      }
    },
    uploadVideo: {
      maxFileSize: 200 * 1024 * 1024,
      customUpload: async (file: File, insertFn: any) => {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await request.post('/upload', formData)
          if (res.data) {
            insertFn(res.data)
          }
        } catch (e) {
          ElMessage.error('视频上传失败')
        }
      }
    }
  }
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
  }
})

const form = ref({
  title: '',
  categoryId: null as number | null,
  coverImg: '',
  content: '',
  state: '已发布',
  scheduleTime: '',
  seoTitle: '',
  seoDescription: '',
  seoKeywords: ''
})

const coverPreview = ref('')

const wordCount = computed(() => {
  const t = form.value.content.trim()
  if (!t) return 0
  const text = t.replace(/<[^>]*>/g, '')
  return text.length
})

const canSubmit = computed(() => {
  const text = form.value.content.replace(/<[^>]*>/g, '').trim()
  return form.value.title.trim().length > 0 && text.length > 10 && form.value.categoryId !== null
})

const seoOpen = ref(false)

const seoFilledCount = computed(() => {
  let count = 0
  if (form.value.seoTitle.trim()) count++
  if (form.value.seoDescription.trim()) count++
  if (form.value.seoKeywords.trim()) count++
  return count
})

const isScheduled = computed(() => form.value.state === '定时发布')

// ── Schedule Presets & Dropdowns ──
const scheduleMode = ref<'preset' | 'custom'>('preset')
const selectedPreset = ref(30)

const schedulePresets = [
  { label: '10 分钟后', value: 10 },
  { label: '15 分钟后', value: 15 },
  { label: '30 分钟后', value: 30 },
  { label: '1 小时后', value: 60 },
  { label: '2 小时后', value: 120 },
  { label: '4 小时后', value: 240 },
  { label: '明天此时', value: 1440 },
]

const schMonth = ref(new Date().getMonth() + 1)
const schDay = ref(new Date().getDate())
const schHour = ref(new Date().getHours())
const schMinute = ref(0)

const daysInSchMonth = computed(() => {
  const now = new Date()
  return new Date(now.getFullYear(), schMonth.value, 0).getDate()
})

const dayOptions = computed(() => {
  const total = daysInSchMonth.value
  return Array.from({ length: total }, (_, i) => ({ value: i + 1, label: `${i + 1} 日` }))
})

const minuteOptions = computed(() => {
  return Array.from({ length: 60 }, (_, i) => ({ value: i, label: `${String(i).padStart(2, '0')} 分` }))
})

const hourOptions = computed(() => {
  return Array.from({ length: 24 }, (_, i) => ({ value: i, label: `${String(i).padStart(2, '0')} 时` }))
})

const toLocalDatetime = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:00`
}

const updateScheduleTime = () => {
  if (scheduleMode.value === 'preset') {
    const now = new Date()
    const future = new Date(now.getTime() + selectedPreset.value * 60 * 1000)
    form.value.scheduleTime = toLocalDatetime(future)
  } else {
    const now = new Date()
    const d = new Date(now.getFullYear(), schMonth.value - 1, schDay.value, schHour.value, schMinute.value)
    if (d > now) {
      form.value.scheduleTime = toLocalDatetime(d)
    } else {
      const nextDay = new Date(d.getTime() + 86400000)
      form.value.scheduleTime = toLocalDatetime(nextDay)
    }
  }
}

const initCustom = () => {
  const now = new Date()
  schMonth.value = now.getMonth() + 1
  schDay.value = now.getDate()
  schHour.value = now.getHours()
  schMinute.value = Math.ceil(now.getMinutes() / 5) * 5
  if (schMinute.value >= 60) schMinute.value = 55
}

watch(selectedPreset, () => {
  if (scheduleMode.value === 'preset') updateScheduleTime()
})

watch(scheduleMode, (mode) => {
  if (mode === 'custom') initCustom()
  updateScheduleTime()
})

watch([schMonth, schDay, schHour, schMinute], () => {
  if (scheduleMode.value === 'custom') {
    if (schDay.value > daysInSchMonth.value) schDay.value = daysInSchMonth.value
    updateScheduleTime()
  }
})

watch(() => form.value.state, (newState) => {
  if (newState === '定时发布') {
    updateScheduleTime()
  }
})

const fetchCategories = async () => {
  try {
    console.log('[CreateArticle] 正在获取分类列表...')
    const res: any = await getAllCategories()
    console.log('[CreateArticle] 分类接口返回:', res)
    console.log('[CreateArticle] 分类数据:', res.data)
    categories.value = (res.data || []).filter(Boolean) as any[]
    console.log('[CreateArticle] categories 已设置, 数量:', categories.value.length)
  } catch (e: any) {
    console.error('[CreateArticle] 获取分类失败:', e?.message || e)
    ElMessage.error('获取分类失败')
  }
}

const fetchAllTags = async () => {
  try {
    const res: any = await getAllTags()
    allTags.value = (res.data || []).filter(Boolean) as any[]
  } catch (e: any) {
    console.error('[CreateArticle] 获取标签失败:', e?.message || e)
  }
}

const toggleTag = (tagName: string) => {
  const idx = selectedTagNames.value.indexOf(tagName)
  if (idx > -1) {
    selectedTagNames.value.splice(idx, 1)
  } else {
    if (selectedTagNames.value.length >= MAX_TAGS) {
      ElMessage.warning(`最多选择 ${MAX_TAGS} 个标签`)
      return
    }
    selectedTagNames.value.push(tagName)
  }
}

const removeTag = (tagName: string) => {
  const idx = selectedTagNames.value.indexOf(tagName)
  if (idx > -1) {
    selectedTagNames.value.splice(idx, 1)
  }
}

const addNewTag = async () => {
  const name = newTagInput.value.trim()
  if (!name) return
  if (selectedTagNames.value.length >= MAX_TAGS) {
    ElMessage.warning(`最多选择 ${MAX_TAGS} 个标签`)
    return
  }
  if (selectedTagNames.value.includes(name)) {
    ElMessage.warning('该标签已选择')
    newTagInput.value = ''
    return
  }
  const exists = allTags.value.find((t: any) => t.name === name)
  if (!exists) {
    try {
      await addTag({ name })
      await fetchAllTags()
    } catch {
      // 即使后端添加失败，也允许前端使用
    }
  }
  selectedTagNames.value.push(name)
  newTagInput.value = ''
  showTagInput.value = false
}

const loadArticleForEdit = async (id?: number) => {
  const targetId = id ?? articleId.value
  if (!targetId) return
  console.log('[CreateArticle] 开始加载文章, id:', targetId)
  try {
    const res: any = await getArticleDetail(targetId)
    console.log('[CreateArticle] getArticleDetail 返回:', res)
    const article = res.data
    console.log('[CreateArticle] 文章数据:', article)
    if (article) {
      form.value.title = article.title || ''
      form.value.categoryId = article.categoryId ?? null
      form.value.coverImg = article.coverImg || ''
      form.value.content = article.content || ''
      form.value.state = article.state || '已发布'
      form.value.seoTitle = article.seoTitle || ''
      form.value.seoDescription = article.seoDescription || ''
      form.value.seoKeywords = article.seoKeywords || ''
      selectedTagNames.value = article.tagNames ? [...article.tagNames] : []
      if (article.coverImg) {
        coverPreview.value = article.coverImg
      }
      console.log('[CreateArticle] 表单数据已填充')
    }
  } catch (e: any) {
    console.error('[CreateArticle] 获取文章信息失败:', e)
    ElMessage.error('获取文章信息失败')
  }
}

const resetForm = () => {
  form.value = {
    title: '',
    categoryId: null as number | null,
    coverImg: '',
    content: '',
    state: '已发布',
    scheduleTime: '',
    seoTitle: '',
    seoDescription: '',
    seoKeywords: ''
  }
  coverPreview.value = ''
  selectedTagNames.value = []
}

const handleCoverUpload = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: any) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过 5MB')
      return
    }

    imageUploading.value = true
    const formData = new FormData()
    formData.append('file', file)

    try {
      const res: any = await request.post('/upload', formData)
      const url = res.data || res
      form.value.coverImg = url
      coverPreview.value = url
    } catch (e: any) {
      console.error('封面上传失败:', e)
      ElMessage.error(e?.response?.data?.message || '图片上传失败')
    } finally {
      imageUploading.value = false
    }

    input.remove()
  }
  input.click()
}

const removeCover = () => {
  form.value.coverImg = ''
  coverPreview.value = ''
}

const handleSubmit = async () => {
  if (!userStore.checkLogin('请先登录')) return
  if (!canSubmit.value) {
    ElMessage.warning('请完善文章信息（标题、分类、内容至少10个字）')
    return
  }
  if (isScheduled.value && !form.value.scheduleTime) {
    ElMessage.warning('请选择定时发布时间')
    return
  }

  submitting.value = true
  try {
    const payload: any = {
      title: form.value.title.trim(),
      categoryId: form.value.categoryId,
      coverImg: form.value.coverImg || '',
      content: form.value.content.trim(),
      state: form.value.state === '定时发布' ? '草稿' : form.value.state,
      tagNameList: selectedTagNames.value,
      seoTitle: form.value.seoTitle.trim(),
      seoDescription: form.value.seoDescription.trim(),
      seoKeywords: form.value.seoKeywords.trim()
    }

    if (isEditMode.value) {
      payload.id = articleId.value
      await updateArticle(payload)
      ElMessage.success('文章更新成功！')
      router.push('/')
      return
    }

    if (isScheduled.value) {
      payload.scheduleTime = form.value.scheduleTime
      await scheduleArticle({
        ...payload,
        scheduleTime: form.value.scheduleTime
      })
      const d = new Date(form.value.scheduleTime)
      ElMessage.success(`定时发布已设置，将在 ${d.toLocaleString('zh-CN')} 自动发布`)
      router.push('/')
      return
    }

    if (form.value.state === '草稿') {
      await addArticle(payload)
      ElMessage.success('草稿保存成功！')
      router.push('/')
      return
    }

    await addArticle(payload)
    ElMessage.success('文章发布成功！')
    router.push('/')
  } catch (error: any) {
    const msg = error?.response?.data?.msg || error?.message || ''
    if (form.value.state === '草稿') {
      ElMessage.error(msg || '草稿保存失败，请稍后重试')
    } else if (isScheduled.value) {
      ElMessage.error(msg || '定时发布设置失败，请稍后重试')
    } else {
      ElMessage.error(msg || '文章发布失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/')
}

const selectedCategoryName = computed(() => {
  if (!form.value.categoryId) return ''
  const cat = categories.value.find((c: any) => (c?.categoryId || c?.id) === form.value.categoryId)
  return cat?.categoryName || cat?.name || ''
})

const catDropdownOpen = ref(false)
const catDropdownRef = ref<HTMLElement | null>(null)

const toggleCatDropdown = () => {
  catDropdownOpen.value = !catDropdownOpen.value
}

const selectCategory = (cat: any) => {
  form.value.categoryId = (cat.categoryId || cat.id) ?? null
  catDropdownOpen.value = false
}

const closeCatDropdown = () => {
  catDropdownOpen.value = false
}

const handleDocClick = (e: MouseEvent) => {
  if (!catDropdownOpen.value) return
  if (catDropdownRef.value && !catDropdownRef.value.contains(e.target as Node)) {
    catDropdownOpen.value = false
  }
}

watch(catDropdownOpen, (val) => {
  if (val) {
    setTimeout(() => document.addEventListener('click', handleDocClick), 0)
  } else {
    document.removeEventListener('click', handleDocClick)
  }
})

// ==================== Canvas Cool Blob Background ====================
const bgCanvas = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0
let ctx: CanvasRenderingContext2D | null = null
let canvasW = 0
let canvasH = 0

const blobDefs = [
  { hue: 195, sat: 55, light: 72, r: 0.34, cx: 0.12, cy: 0.15, vx: 0.06, vy: 0.05, phase: 0 },
  { hue: 220, sat: 50, light: 70, r: 0.30, cx: 0.80, cy: 0.12, vx: -0.07, vy: 0.06, phase: 1.5 },
  { hue: 180, sat: 50, light: 68, r: 0.28, cx: 0.85, cy: 0.72, vx: -0.05, vy: -0.06, phase: 2.8 },
  { hue: 210, sat: 45, light: 74, r: 0.26, cx: 0.18, cy: 0.75, vx: 0.07, vy: -0.05, phase: 4.0 },
  { hue: 200, sat: 45, light: 76, r: 0.32, cx: 0.48, cy: 0.35, vx: -0.04, vy: 0.04, phase: 5.2 },
  { hue: 170, sat: 45, light: 70, r: 0.24, cx: 0.55, cy: 0.85, vx: 0.06, vy: -0.04, phase: 1.9 },
  { hue: 230, sat: 50, light: 72, r: 0.28, cx: 0.35, cy: 0.60, vx: 0.05, vy: -0.07, phase: 4.5 },
  { hue: 190, sat: 50, light: 68, r: 0.26, cx: 0.68, cy: 0.30, vx: -0.06, vy: -0.04, phase: 6.2 }
]

const initBackground = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  if (!ctx) return

  canvasW = window.innerWidth
  canvasH = window.innerHeight
  canvas.width = canvasW
  canvas.height = canvasH
}

const drawBackground = () => {
  if (!ctx) return
  const c = ctx
  const t = Date.now() * 0.0003
  const W = canvasW
  const H = canvasH

  c.clearRect(0, 0, W, H)

  const baseGrad = c.createLinearGradient(0, 0, W, H)
  baseGrad.addColorStop(0, '#f0f7ff')
  baseGrad.addColorStop(0.3, '#f5faff')
  baseGrad.addColorStop(0.6, '#f0f9f8')
  baseGrad.addColorStop(1, '#f8faff')
  c.fillStyle = baseGrad
  c.fillRect(0, 0, W, H)

  for (const b of blobDefs) {
    const bx = W * (b.cx + Math.sin(t * b.vx + b.phase) * 0.16)
    const by = H * (b.cy + Math.cos(t * b.vy + b.phase) * 0.14)
    const br = Math.min(W, H) * (b.r + Math.sin(t * 0.4 + b.phase) * 0.05)

    const gradient = c.createRadialGradient(bx, by, 0, bx, by, br)
    gradient.addColorStop(0, `hsla(${b.hue}, ${b.sat}%, ${b.light}%, 0.40)`)
    gradient.addColorStop(0.2, `hsla(${b.hue}, ${b.sat}%, ${b.light - 6}%, 0.25)`)
    gradient.addColorStop(0.5, `hsla(${b.hue}, ${b.sat - 8}%, ${b.light - 10}%, 0.10)`)
    gradient.addColorStop(0.75, `hsla(${b.hue}, ${b.sat - 15}%, ${b.light - 16}%, 0.02)`)
    gradient.addColorStop(1, 'transparent')

    c.fillStyle = gradient
    c.beginPath()
    c.arc(bx, by, br, 0, Math.PI * 2)
    c.fill()
  }

  c.globalAlpha = 0.08
  for (let i = 0; i < 3; i++) {
    const ribbonY = H * (0.20 + i * 0.30)
    const wave = Math.sin(t * 1.0 + i * 2.0) * W * 0.15

    const rg = c.createLinearGradient(0, ribbonY - 60, 0, ribbonY + 60)
    const ribbonHue = [200, 180, 220][i]
    rg.addColorStop(0, 'transparent')
    rg.addColorStop(0.3, `hsla(${ribbonHue}, 55%, 65%, 0.4)`)
    rg.addColorStop(0.5, `hsla(${ribbonHue + 10}, 60%, 70%, 0.5)`)
    rg.addColorStop(0.7, `hsla(${ribbonHue}, 55%, 65%, 0.4)`)
    rg.addColorStop(1, 'transparent')

    c.fillStyle = rg
    c.beginPath()
    c.moveTo(-50, ribbonY - 60)
    c.quadraticCurveTo(W * 0.5 + wave, ribbonY - 20, W + 50, ribbonY - 60)
    c.lineTo(W + 50, ribbonY + 60)
    c.quadraticCurveTo(W * 0.5 + wave, ribbonY + 20, -50, ribbonY + 60)
    c.closePath()
    c.fill()
  }
  c.globalAlpha = 1

  const dotCount = 30
  for (let i = 0; i < dotCount; i++) {
    const seed = i * 137.508
    const dx = (Math.sin(t * 0.3 + seed) * 0.5 + 0.5) * W
    const dy = (Math.cos(t * 0.37 + seed * 1.3) * 0.5 + 0.5) * H
    const dr = 1.5 + Math.sin(t * 2 + i) * 1.0
    const dhue = 190 + Math.sin(t + i * 0.7) * 60

    c.beginPath()
    c.arc(dx, dy, dr, 0, Math.PI * 2)
    c.fillStyle = `hsla(${dhue}, 70%, 68%, ${0.18 + Math.sin(t * 3 + i) * 0.10})`
    c.fill()
  }

  animFrameId = requestAnimationFrame(drawBackground)
}

const handleBgResize = () => {
  const canvas = bgCanvas.value
  if (!canvas) return
  canvasW = window.innerWidth
  canvasH = window.innerHeight
  canvas.width = canvasW
  canvas.height = canvasH
}

onMounted(() => {
  if (!userStore.token) {
    userStore.checkLogin('请先登录以发布文章')
    router.push('/')
    return
  }
  fetchCategories()
  fetchAllTags()
  initBackground()
  drawBackground()
  window.addEventListener('resize', handleBgResize)
})

// 当路由参数变化时自动加载/重置文章数据
watch(() => route.query.id, (newQueryId) => {
  const id = Number(newQueryId) || 0
  console.log('[CreateArticle] route.query.id 变化:', newQueryId, '→ articleId:', id)
  if (id) {
    loadArticleForEdit(id)
  } else {
    resetForm()
  }
}, { immediate: true })

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', handleBgResize)
})
</script>

<template>
  <div class="create-page">
    <canvas ref="bgCanvas" class="bg-canvas" />

    <div class="page-inner">
      <div class="form-panel">
        <div class="form-header">
          <button class="btn-back" @click="goBack">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </button>
          <div class="header-text">
            <h1 class="page-title">{{ isEditMode ? '编辑文章' : '创作新文章' }}</h1>
            <p class="page-subtitle">{{ isEditMode ? '修改文章内容并保存' : '分享你的知识与见解' }}</p>
          </div>
        </div>

        <div class="form-body">
          <div class="form-section">
            <div class="field-group">
              <label class="field-label">标题 <span class="required">*</span></label>
              <div class="title-wrap">
                <input
                  v-model="form.title"
                  type="text"
                  class="input-title"
                  placeholder="输入文章标题..."
                  maxlength="100"
                />
                <span class="title-count">{{ form.title.length }}/100</span>
              </div>
            </div>

            <div class="field-row">
              <div class="field-group flex-1">
                <label class="field-label">分类 <span class="required">*</span></label>
                <div class="select-wrap" ref="catDropdownRef">
                  <button class="cat-trigger" @click="toggleCatDropdown" type="button">
                    <span class="cat-trigger-left">
                      <svg class="cat-trigger-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/>
                      </svg>
                      <span :class="{ placeholder: !selectedCategoryName }">{{ selectedCategoryName || '选择分类' }}</span>
                    </span>
                    <svg class="cat-trigger-chevron" :class="{ open: catDropdownOpen }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </button>

                  <Transition name="drop-slide">
                    <div v-if="catDropdownOpen" class="cat-dropdown">
                      <div class="cat-dropdown-inner">
                        <button
                          v-for="cat in categories"
                          :key="cat?.categoryId || cat?.id"
                          class="cat-option"
                          :class="{ selected: form.categoryId === (cat?.categoryId || cat?.id) }"
                          @click="selectCategory(cat)"
                          type="button"
                        >
                          <span class="cat-opt-name">{{ cat?.categoryName || cat?.name }}</span>
                          <svg v-if="form.categoryId === (cat?.categoryId || cat?.id)" class="cat-opt-check" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"/>
                          </svg>
                        </button>
                        <div v-if="!categories.length" class="cat-empty">暂无分类</div>
                      </div>
                    </div>
                  </Transition>
                </div>
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">
                标签
                <span class="label-hint">（点击选择已有标签，也可新建）</span>
                <span class="tag-count-hint">{{ selectedTagNames.length }}/{{ MAX_TAGS }}{{ remainingTags > 0 ? `，还可选 ${remainingTags} 个` : '，已达上限' }}</span>
              </label>
              <div class="tag-select-area">
                <div class="tag-chips">
                  <span
                    v-for="tag in allTags"
                    :key="tag.id"
                    class="tag-chip"
                    :class="{ active: selectedTagNames.includes(tag.name), disabled: !selectedTagNames.includes(tag.name) && remainingTags <= 0 }"
                    @click="toggleTag(tag.name)"
                  >{{ tag.name }}</span>
                  <button class="tag-add-btn" @click="showTagInput = true" type="button" v-if="!showTagInput" :disabled="remainingTags <= 0">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                    </svg>
                    <span>新建</span>
                  </button>
                </div>
                <div v-if="showTagInput" class="tag-input-row">
                  <input
                    v-model="newTagInput"
                    type="text"
                    class="tag-input"
                    placeholder="输入标签名称，回车确认"
                    maxlength="20"
                    @keyup.enter="addNewTag"
                    @keyup.escape="showTagInput = false"
                    ref="tagInputRef"
                  />
                  <button class="tag-confirm-btn" @click="addNewTag" type="button">确认</button>
                  <button class="tag-cancel-btn" @click="showTagInput = false; newTagInput = ''" type="button">取消</button>
                </div>
                <div v-if="selectedTagNames.length" class="selected-tags">
                  <span class="selected-tag-label">已选：</span>
                  <span
                    v-for="tag in selectedTagNames"
                    :key="tag"
                    class="selected-tag"
                  >
                    {{ tag }}
                    <button class="tag-remove" @click="removeTag(tag)" type="button">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                      </svg>
                    </button>
                  </span>
                </div>
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">发布方式</label>
              <div class="publish-options">
                <label class="option-chip" :class="{ active: form.state === '已发布' }">
                  <input type="radio" v-model="form.state" value="已发布" />
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                  </svg>
                  <span>发布</span>
                </label>
                <label class="option-chip" :class="{ active: form.state === '草稿' }">
                  <input type="radio" v-model="form.state" value="草稿" />
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
                  </svg>
                  <span>存草稿</span>
                </label>
                <label class="option-chip" :class="{ active: form.state === '定时发布' }" v-if="!isEditMode">
                  <input type="radio" v-model="form.state" value="定时发布" />
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                  </svg>
                  <span>定时发布</span>
                </label>
              </div>

              <transition name="schedule-slide">
                <div v-if="isScheduled" class="schedule-wrap">

                  <div class="schedule-tabs">
                    <button
                      class="tab-btn"
                      :class="{ active: scheduleMode === 'preset' }"
                      @click="scheduleMode = 'preset'"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="10 13 2 13"/><polyline points="20 13 14 13"/><polyline points="20 5 14 5"/><polyline points="10 5 2 5"/><polyline points="20 21 14 21"/><polyline points="10 21 2 21"/>
                      </svg>
                      快速
                    </button>
                    <button
                      class="tab-btn"
                      :class="{ active: scheduleMode === 'custom' }"
                      @click="scheduleMode = 'custom'"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                      </svg>
                      自定义
                    </button>
                  </div>

                  <transition name="mode-fade" mode="out-in">
                    <!-- Preset Mode -->
                    <div v-if="scheduleMode === 'preset'" class="preset-grid" key="preset">
                      <button
                        v-for="p in schedulePresets"
                        :key="p.value"
                        class="preset-btn"
                        :class="{ active: selectedPreset === p.value }"
                        @click="selectedPreset = p.value"
                      >
                        <span class="preset-dot"></span>
                        {{ p.label }}
                      </button>
                    </div>

                    <!-- Custom Dropdown Mode -->
                    <div v-else class="custom-dropdown-panel" key="custom">
                      <div class="custom-grid">
                        <div class="drop-box">
                          <span class="drop-label">月</span>
                          <div class="drop-wrap">
                            <select v-model="schMonth" class="drop-select">
                              <option v-for="m in 12" :key="m" :value="m">{{ m }} 月</option>
                            </select>
                            <svg class="drop-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                          </div>
                        </div>
                        <div class="drop-box">
                          <span class="drop-label">日</span>
                          <div class="drop-wrap">
                            <select v-model="schDay" class="drop-select">
                              <option v-for="opt in dayOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                            </select>
                            <svg class="drop-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                          </div>
                        </div>
                        <div class="drop-box">
                          <span class="drop-label">时</span>
                          <div class="drop-wrap">
                            <select v-model="schHour" class="drop-select">
                              <option v-for="opt in hourOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                            </select>
                            <svg class="drop-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                          </div>
                        </div>
                        <div class="drop-box">
                          <span class="drop-label">分</span>
                          <div class="drop-wrap">
                            <select v-model="schMinute" class="drop-select">
                              <option v-for="opt in minuteOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                            </select>
                            <svg class="drop-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                          </div>
                        </div>
                      </div>

                      <div class="custom-summary" v-if="form.scheduleTime">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                        </svg>
                        <span>{{ new Date(form.scheduleTime).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }}</span>
                      </div>
                    </div>
                  </transition>
                </div>
              </transition>
            </div>

            <div class="seo-section">
              <button class="seo-toggle" type="button" @click="seoOpen = !seoOpen">
                <div class="seo-toggle-left">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                  </svg>
                  <span>SEO(用于搜索引擎)</span>
                  <span class="seo-badge" v-if="seoFilledCount > 0">{{ seoFilledCount }}/3 已填</span>
                  <span class="seo-badge empty" v-else>可选</span>
                </div>
                <svg
                  class="seo-chevron"
                  :class="{ open: seoOpen }"
                  width="16" height="16"
                  viewBox="0 0 24 24"
                  fill="none" stroke="currentColor"
                  stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
                >
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </button>
              <transition name="seo-expand">
                <div v-show="seoOpen" class="seo-body">
                  <div class="seo-grid">
                    <div class="seo-field">
                      <label class="seo-label">SEO 标题</label>
                      <input
                        v-model="form.seoTitle"
                        type="text"
                        class="input-seo"
                        placeholder="搜索引擎展示的标题"
                        maxlength="100"
                      />
                      <span class="seo-count">{{ form.seoTitle.length }}/100</span>
                    </div>
                    <div class="seo-field">
                      <label class="seo-label">SEO 描述</label>
                      <textarea
                        v-model="form.seoDescription"
                        class="input-seo-textarea"
                        placeholder="搜索引擎展示的描述"
                        maxlength="200"
                        rows="3"
                      ></textarea>
                      <span class="seo-count">{{ form.seoDescription.length }}/200</span>
                    </div>
                    <div class="seo-field">
                      <label class="seo-label">SEO 关键词</label>
                      <input
                        v-model="form.seoKeywords"
                        type="text"
                        class="input-seo"
                        placeholder="多个关键词用逗号分隔"
                        maxlength="100"
                      />
                      <span class="seo-count">{{ form.seoKeywords.length }}/100</span>
                    </div>
                  </div>
                </div>
              </transition>
            </div>

            <div class="field-group">
              <label class="field-label">封面图片</label>
              <div class="cover-upload" @click="handleCoverUpload">
                <template v-if="coverPreview">
                  <img :src="coverPreview" class="cover-preview" />
                  <div class="cover-overlay">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/>
                      <circle cx="12" cy="13" r="4"/>
                    </svg>
                    <span>点击更换</span>
                  </div>
                  <button class="cover-remove" @click.stop="removeCover">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </template>
                <template v-else>
                  <div class="upload-placeholder" :class="{ uploading: imageUploading }">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
                    </svg>
                    <span>{{ imageUploading ? '上传中...' : '上传封面图片' }}</span>
                    <small>支持 JPG / PNG / WebP，不超过 5MB</small>
                  </div>
                </template>
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">内容 <span class="required">*</span></label>
              <div class="editor-wrap">
                <div class="editor-toolbar">
                  <Toolbar
                    :editor="editorRef"
                    :defaultConfig="toolbarConfig"
                    mode="default"
                  />
                </div>
                <Editor
                  v-model="form.content"
                  :defaultConfig="editorConfig"
                  mode="default"
                  @onCreated="handleCreated"
                />
                <div class="editor-footer">
                  <span class="char-count">{{ wordCount }} 字</span>
                  <span class="char-hint">至少 10 个字</span>
                </div>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button class="btn-cancel" @click="goBack">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
              <span>取消</span>
            </button>
            <button class="btn-primary" @click="handleSubmit" :disabled="submitting || !canSubmit">
              <template v-if="submitting">
                <span class="spinner"></span>
                <span>{{ isScheduled ? '排期中...' : '提交中...' }}</span>
              </template>
              <template v-else>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                </svg>
                <span>{{ isEditMode ? '保存修改' : isScheduled ? '确认定时' : form.state === '草稿' ? '存入草稿' : '确认发布' }}</span>
              </template>
            </button>
          </div>
        </div>
      </div>

      <div class="info-panel">
        <div class="info-sticky">
          <div class="info-card preview-card">
            <div class="card-label">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
              </svg>
              文章预览
            </div>
            <div class="preview-body">
              <div v-if="coverPreview" class="preview-cover">
                <img :src="coverPreview" />
              </div>
              <div class="preview-meta">
                <span class="preview-category" v-if="selectedCategoryName">{{ selectedCategoryName }}</span>
                <span class="preview-badge" :class="form.state === '定时发布' ? 'schedule' : form.state === '草稿' ? 'draft' : 'publish'">
                  <template v-if="form.state === '定时发布'">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    {{ form.scheduleTime ? new Date(form.scheduleTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '待设定' }}
                  </template>
                  <template v-else-if="form.state === '草稿'">草稿</template>
                  <template v-else>发布</template>
                </span>
              </div>
              <h3 class="preview-title">{{ form.title || '文章标题' }}</h3>
              <div class="preview-content" v-if="form.content">
                <div v-html="form.content"></div>
              </div>
              <p class="preview-text" v-else>开始写作，预览将实时更新...</p>
            </div>
          </div>

          <div class="info-card schedule-card" v-if="isScheduled">
            <div class="card-label">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
              排期信息
            </div>
            <div class="schedule-display">
              <div class="schedule-ring">
                <svg viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(14,165,233,0.08)" stroke-width="6"/>
                  <circle cx="50" cy="50" r="42" fill="none" stroke="#0ea5e9" stroke-width="6" stroke-dasharray="264" stroke-dashoffset="66" stroke-linecap="round" transform="rotate(-90 50 50)"/>
                </svg>
                <div class="ring-center">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#0ea5e9" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                </div>
              </div>
              <div class="schedule-text" v-if="form.scheduleTime">
                <span class="schedule-label">发布时间</span>
                <span class="schedule-date">{{ new Date(form.scheduleTime).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }}</span>
                <span class="schedule-time">{{ new Date(form.scheduleTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</span>
              </div>
              <div class="schedule-text empty" v-else>
                <span class="schedule-label">请选择发布时间</span>
              </div>
            </div>
          </div>

          <div class="info-card tips-card">
            <div class="card-label">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
              </svg>
              写作建议
            </div>
            <ul class="tips-list">
              <li>
                <span class="tip-num">01</span>
                <span>标题简洁有力，吸引读者点击</span>
              </li>
              <li>
                <span class="tip-num">02</span>
                <span>合理分段，每段不超过 200 字</span>
              </li>
              <li>
                <span class="tip-num">03</span>
                <span>搭配封面图片，提升视觉效果</span>
              </li>
              <li>
                <span class="tip-num">04</span>
                <span>添加合适标签，让更多读者发现</span>
              </li>
            </ul>
          </div>

          <div class="info-card stats-card">
            <div class="stat-row">
              <span class="stat-label">标题字数</span>
              <span class="stat-value">{{ form.title.length }} / 100</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">内容篇幅</span>
              <span class="stat-value">{{ wordCount }} 字</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">发布方式</span>
              <span class="stat-value" :class="{ scheduled: isScheduled }">{{ form.state }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.create-page {
  min-height: 100vh;
  position: relative;
  display: flex;
}

.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.page-inner {
  display: flex;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px;
  gap: 32px;
  position: relative;
  z-index: 1;
}

/* ── Left Panel ── */
.form-panel {
  flex: 1;
  min-width: 0;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(148,163,184,0.12);
  box-shadow: 0 1px 4px rgba(0,0,0,0.02), 0 8px 32px rgba(0,0,0,0.03);
  overflow: hidden;
}

.form-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 28px 32px 0;
}

.btn-back {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-back:hover {
  border-color: #0ea5e9;
  color: #0ea5e9;
  background: rgba(14,165,233,0.04);
}

.header-text {
  flex: 1;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.4px;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 4px 0 0;
}

/* ── Form Body ── */
.form-body {
  padding: 24px 32px 32px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  letter-spacing: 0.2px;
}

.required {
  color: #f87171;
}

.field-row {
  display: flex;
  gap: 16px;
}

.flex-1 {
  flex: 1;
}

.label-hint {
  font-weight: 400;
  font-size: 12px;
  color: #94a3b8;
}

.tag-count-hint {
  font-weight: 500;
  font-size: 12px;
  color: #0ea5e9;
  margin-left: 8px;
}

/* ── Tag Select Area ── */
.tag-select-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-chip {
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  border: 1.5px solid #e2e8f0;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.tag-chip:hover {
  border-color: #7dd3fc;
  color: #0284c7;
  background: rgba(14,165,233,0.04);
}

.tag-chip.active {
  background: rgba(14,165,233,0.08);
  border-color: #38bdf8;
  color: #0284c7;
  font-weight: 600;
}

.tag-chip.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tag-chip.disabled:hover {
  border-color: #e2e8f0;
  color: #64748b;
  background: #f1f5f9;
}

.tag-add-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  background: transparent;
  border: 1.5px dashed #d1d5db;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.tag-add-btn:hover:not(:disabled) {
  border-color: #38bdf8;
  color: #0284c7;
  background: rgba(14,165,233,0.04);
}

.tag-add-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.tag-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tag-input {
  flex: 1;
  padding: 8px 14px;
  font-size: 13px;
  color: #0f172a;
  background: #fff;
  border: 1.5px solid #38bdf8;
  border-radius: 10px;
  outline: none;
  font-family: inherit;
  transition: all 0.2s ease;
  max-width: 240px;
}

.tag-input::placeholder {
  color: #94a3b8;
}

.tag-confirm-btn {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: #0ea5e9;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.tag-confirm-btn:hover {
  background: #0284c7;
}

.tag-cancel-btn {
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.tag-cancel-btn:hover {
  background: #e2e8f0;
  color: #334155;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.selected-tag-label {
  font-size: 12px;
  color: #94a3b8;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #0284c7;
  background: rgba(14,165,233,0.06);
  border-radius: 14px;
}

.tag-remove {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #7dd3fc;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.tag-remove:hover {
  background: rgba(239,68,68,0.1);
  color: #ef4444;
}

/* ── Title ── */
.title-wrap {
  position: relative;
}

.input-title {
  width: 100%;
  padding: 14px 80px 14px 18px;
  font-size: 16px;
  font-weight: 500;
  color: #0f172a;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;
}

.input-title::placeholder {
  color: #94a3b8;
  font-weight: 400;
}

.input-title:focus {
  border-color: #0ea5e9;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(14,165,233,0.06);
}

.title-count {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #94a3b8;
  pointer-events: none;
}

/* ── Select / Category Dropdown ── */
.select-wrap {
  position: relative;
}

.cat-trigger {
  width: 100%;
  padding: 12px 14px 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 14px;
  color: #0f172a;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  outline: none;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
}

.cat-trigger:hover {
  border-color: #cbd5e1;
  background: #fff;
}

.cat-trigger:focus-visible {
  border-color: #0ea5e9;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.08);
}

.cat-trigger-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.cat-trigger-left span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-trigger-left span.placeholder {
  color: #94a3b8;
}

.cat-trigger-icon {
  flex-shrink: 0;
  color: #94a3b8;
  transition: color 0.2s;
}

.cat-trigger:hover .cat-trigger-icon {
  color: #0ea5e9;
}

.cat-trigger-chevron {
  flex-shrink: 0;
  color: #94a3b8;
  transition: transform 0.25s ease;
}

.cat-trigger-chevron.open {
  transform: rotate(180deg);
}

.cat-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 50;
  background: #fff;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(15,23,42,0.1), 0 0 0 1px rgba(15,23,42,0.04);
  overflow: hidden;
}

.cat-dropdown-inner {
  max-height: 220px;
  overflow-y: auto;
  padding: 6px;
}

.cat-option {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  text-align: left;
}

.cat-option:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.cat-option.selected {
  background: rgba(14,165,233,0.06);
  color: #0ea5e9;
  font-weight: 600;
}

.cat-opt-check {
  color: #0ea5e9;
  flex-shrink: 0;
}

.cat-empty {
  padding: 20px 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

/* ── Dropdown Transition ── */
.drop-slide-enter-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.drop-slide-leave-active {
  transition: all 0.15s ease-in;
}

.drop-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px) scaleY(0.94);
}

.drop-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px) scaleY(0.96);
}

/* ── Publish Options ── */
.publish-options {
  display: flex;
  gap: 8px;
}

.option-chip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.option-chip input {
  display: none;
}

.option-chip:hover {
  border-color: #94a3b8;
  background: #f1f5f9;
  color: #334155;
}

.option-chip.active {
  border-color: #0ea5e9;
  background: rgba(14,165,233,0.04);
  color: #0284c7;
}

/* ── Schedule ── */
.schedule-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(14,165,233,0.04);
  border: 1px solid rgba(14,165,233,0.12);
}

.schedule-icon {
  color: #0ea5e9;
  flex-shrink: 0;
}

.input-datetime {
  flex: 1;
  padding: 8px 12px;
  font-size: 13px;
  color: #0f172a;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  outline: none;
  font-family: inherit;
  transition: all 0.25s ease;
}

.input-datetime:focus {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.06);
}

.schedule-slide-enter-active,
.schedule-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.schedule-slide-enter-from,
.schedule-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ── Schedule Presets & Rings ── */
.schedule-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 8px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(14,165,233,0.03);
  border: 1px solid rgba(14,165,233,0.1);
}

.schedule-tabs {
  display: flex;
  gap: 6px;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s ease;
}

.tab-btn.active {
  background: #fff;
  color: #0284c7;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.tab-btn:hover:not(.active) {
  color: #334155;
}

/* Preset Grid */
.preset-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.preset-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.22s ease;
  overflow: hidden;
}

.preset-btn:hover {
  border-color: #94a3b8;
  background: #f8fafc;
  color: #1e293b;
}

.preset-btn.active {
  border-color: #0ea5e9;
  background: rgba(14,165,233,0.06);
  color: #0284c7;
}

.preset-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.preset-btn.active .preset-dot {
  background: #0ea5e9;
  box-shadow: 0 0 6px rgba(14,165,233,0.4);
}

/* Mode Fade Transition */
.mode-fade-enter-active,
.mode-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.mode-fade-enter-from,
.mode-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* Custom Dropdown Panel */
.custom-dropdown-panel {
  padding: 4px 0;
}

.custom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}

.drop-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.drop-label {
  font-size: 10px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.drop-wrap {
  position: relative;
  width: 100%;
}

.drop-select {
  width: 100%;
  padding: 10px 30px 10px 12px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  outline: none;
  font-family: inherit;
  appearance: none;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: center;
  text-align-last: center;
}

.drop-select:focus {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 4px rgba(14,165,233,0.06);
}

.drop-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
}

.custom-summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(14,165,233,0.06);
  color: #0284c7;
  font-size: 13px;
  font-weight: 600;
  width: 100%;
}

/* ── Cover Upload ── */
.cover-upload {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1.5px dashed #e2e8f0;
  background: #fafbfc;
  transition: all 0.25s ease;
  min-height: 120px;
}

.cover-upload:hover {
  border-color: #0ea5e9;
  background: rgba(14,165,233,0.02);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px;
  color: #94a3b8;
  transition: all 0.25s ease;
}

.upload-placeholder svg {
  color: #94a3b8;
  transition: color 0.25s ease;
}

.cover-upload:hover .upload-placeholder svg {
  color: #0ea5e9;
}

.upload-placeholder span {
  font-size: 14px;
  font-weight: 500;
}

.upload-placeholder small {
  font-size: 12px;
  color: #cbd5e1;
}

.upload-placeholder.uploading {
  opacity: 0.6;
  pointer-events: none;
}

.cover-preview {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  display: block;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15,23,42,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.cover-upload:hover .cover-overlay {
  opacity: 1;
}

.cover-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
}

.cover-upload:hover .cover-remove {
  opacity: 1;
}

.cover-remove:hover {
  background: rgba(239,68,68,0.8);
}

/* ── SEO Section ── */
.seo-section {
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.seo-section:focus-within {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.08);
}

.seo-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: none;
  border: none;
  cursor: pointer;
  color: #334155;
  font-size: 14px;
  font-family: inherit;
  transition: background 0.2s ease;
  -webkit-user-select: none;
  user-select: none;
}

.seo-toggle:hover {
  background: #f8fafc;
}

.seo-toggle-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #1e293b;
}

.seo-toggle-left svg {
  color: #0ea5e9;
  flex-shrink: 0;
}

.seo-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  background: #e0f2fe;
  color: #0284c7;
  line-height: 1.6;
}

.seo-badge.empty {
  background: #f1f5f9;
  color: #94a3b8;
  font-weight: 500;
}

.seo-chevron {
  flex-shrink: 0;
  color: #94a3b8;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.seo-chevron.open {
  transform: rotate(-180deg);
}

.seo-body {
  border-top: 1px solid #f1f5f9;
  padding: 18px;
  background: #fafbfc;
}

.seo-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.seo-field {
  position: relative;
}

.seo-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
  letter-spacing: 0.2px;
}

.input-seo {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  color: #0f172a;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;
}

.input-seo:focus {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.1);
}

.input-seo::placeholder {
  color: #94a3b8;
}

.input-seo-textarea {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #0f172a;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  outline: none;
  resize: vertical;
  min-height: 72px;
  transition: all 0.25s ease;
  font-family: inherit;
}

.input-seo-textarea:focus {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.1);
}

.input-seo-textarea::placeholder {
  color: #94a3b8;
}

.seo-count {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 11px;
  color: #94a3b8;
  pointer-events: none;
}

/* ── SEO Expand Transition ── */
.seo-expand-enter-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.seo-expand-leave-active {
  transition: all 0.18s ease-in;
}

.seo-expand-enter-from,
.seo-expand-leave-to {
  opacity: 0;
}

/* ── Editor ── */
.editor-wrap {
  position: relative;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.25s ease;
  background: #fff;
}

.editor-wrap:focus-within {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 4px rgba(14,165,233,0.06);
}

.editor-toolbar {
  border-bottom: 1px solid #f1f5f9;
}

.editor-toolbar :deep(.w-e-toolbar) {
  background: #fafbfc;
  border: none;
  padding: 4px 8px;
}

.editor-toolbar :deep(.w-e-bar-item button) {
  color: #475569;
  border-radius: 6px;
  padding: 4px 6px;
}

.editor-toolbar :deep(.w-e-bar-item button:hover) {
  background: #e2e8f0;
  color: #0f172a;
}

.editor-toolbar :deep(.w-e-bar-item svg) {
  width: 16px;
  height: 16px;
}

.editor-wrap :deep(.w-e-text-container) {
  min-height: 380px;
  background: #fff;
  color: #0f172a;
}

.editor-wrap :deep(.w-e-text-container .w-e-scroll) {
  padding: 0 18px;
}

.editor-wrap :deep(.w-e-text-container [data-slate-editor]) {
  padding: 16px 0;
  font-size: 15px;
  line-height: 1.8;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 18px;
  font-size: 12px;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
}

.char-count {
  color: #64748b;
  font-weight: 500;
}

.char-hint {
  color: #94a3b8;
}

/* ── Actions ── */
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.btn-primary,
.btn-cancel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.2px;
}

.btn-primary {
  flex: 1;
  border: none;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  box-shadow: 0 4px 14px rgba(14,165,233,0.25);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(14,165,233,0.3);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-cancel {
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #64748b;
}

.btn-cancel:hover:not(:disabled) {
  border-color: #ef4444;
  color: #ef4444;
  background: rgba(239,68,68,0.04);
}

.btn-cancel:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Right Panel ── */
.info-panel {
  width: 320px;
  flex-shrink: 0;
}

.info-sticky {
  position: sticky;
  top: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 16px;
  border: 1px solid rgba(148,163,184,0.12);
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
}

.card-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}

/* Preview Card */
.preview-cover {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.preview-cover img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.preview-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.preview-category {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(14,165,233,0.08);
  color: #0284c7;
}

.preview-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
}

.preview-badge.publish {
  background: rgba(34,197,94,0.08);
  color: #16a34a;
}

.preview-badge.draft {
  background: #f1f5f9;
  color: #64748b;
}

.preview-badge.schedule {
  background: rgba(14,165,233,0.08);
  color: #0284c7;
}

.preview-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-text {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
}

.preview-content {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.preview-content :deep(p) {
  margin: 0 0 6px;
}

.preview-content :deep(img) {
  display: none;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4) {
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 4px;
}

/* Schedule Card */
.schedule-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.schedule-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.schedule-ring svg {
  width: 100%;
  height: 100%;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.schedule-text {
  text-align: center;
}

.schedule-text.empty .schedule-label {
  color: #94a3b8;
}

.schedule-label {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.schedule-date {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.schedule-time {
  display: block;
  font-size: 13px;
  color: #0284c7;
  margin-top: 2px;
  font-weight: 500;
}

/* Tips Card */
.tips-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.tip-num {
  font-size: 12px;
  font-weight: 700;
  color: #0ea5e9;
  background: rgba(14,165,233,0.06);
  padding: 0 5px;
  border-radius: 4px;
  flex-shrink: 0;
  line-height: 1.6;
  min-width: 24px;
  text-align: center;
}

/* Stats Card */
.stats-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.stat-value.scheduled {
  color: #0284c7;
}

/* ── New Category Modal ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 32px rgba(0,0,0,0.1), 0 0 0 1px rgba(0,0,0,0.04);
  width: 400px;
  max-width: calc(100vw - 40px);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-field label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.modal-input {
  padding: 12px 14px;
  font-size: 14px;
  color: #0f172a;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  outline: none;
  transition: all 0.25s;
  font-family: inherit;
}

.modal-input::placeholder {
  color: #94a3b8;
}

.modal-input:focus {
  border-color: #0ea5e9;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(14,165,233,0.06);
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 0 24px 20px;
}

.modal-btn {
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 1.5px solid transparent;
  font-family: inherit;
}

.modal-btn.cancel {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

.modal-btn.cancel:hover {
  background: #e2e8f0;
}

.modal-btn.confirm {
  background: #0ea5e9;
  color: #fff;
}

.modal-btn.confirm:hover:not(:disabled) {
  background: #0284c7;
}

.modal-btn.confirm:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-card,
.modal-fade-leave-to .modal-card {
  transform: translateY(12px) scale(0.96);
}

/* ── Responsive ── */
@media (max-width: 960px) {
  .page-inner {
    flex-direction: column;
    padding: 16px;
    gap: 16px;
  }

  .info-panel {
    width: 100%;
  }

  .field-row {
    flex-direction: column;
  }

  .form-header {
    padding: 20px 20px 0;
  }

  .form-body {
    padding: 20px;
  }

  .publish-options {
    flex-wrap: wrap;
  }

  .option-chip {
    min-width: calc(50% - 4px);
  }
}
</style>