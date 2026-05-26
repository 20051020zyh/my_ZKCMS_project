<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, logout as logoutApi } from '@/api/user'
import { getAllCategories } from '@/api/category'
import { getHotTags } from '@/api/article'
import { ElMessage } from 'element-plus'
import { navigateTo } from '@/utils/navigate'

const router = useRouter()

interface Props {
  selectedCategory: number | null
}

const props = defineProps<Props>()

const userStore = useUserStore()
const searchKeyword = ref('')
const categories = ref<any[]>([])
const hotTags = ref<any[]>([])
const selectedTag = ref('')

const emit = defineEmits<{
  search: [keyword: string]
  categorySelect: [categoryId: number | null]
}>()

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  selectedTag.value = kw
  emit('search', kw)
}

const handleEnter = () => handleSearch()

const handleClear = () => {
  selectedTag.value = ''
  emit('search', '')
}

const handleTagClick = (tagName: string) => {
  if (selectedTag.value === tagName) {
    selectedTag.value = ''
    searchKeyword.value = ''
    emit('search', '')
  } else {
    selectedTag.value = tagName
    searchKeyword.value = tagName
    emit('search', tagName)
  }
}

const fetchCategories = async () => {
  try {
    const res: any = await getAllCategories()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchHotTags = async () => {
  try {
    const res: any = await getHotTags(12)
    hotTags.value = res.data || []
  } catch (error) {
    console.error('获取热门标签失败:', error)
  }
}

const selectCategory = (categoryId: number | null) => {
  selectedTag.value = ''
  searchKeyword.value = ''
  emit('categorySelect', categoryId)
}

const fetchUserInfo = async () => {
  if (!userStore.token) return
  try {
    const res: any = await getUserInfo()
    userStore.setUserInfo(res.data)
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const handleLogout = async () => {
  try {
    await logoutApi()
    userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  } catch (error) {
    userStore.logout()
    router.push('/login')
  }
}

const goToProfile = () => {
  if (!userStore.checkLogin('请先登录')) return
  navigateTo('/profile')
}

const handleCommand = (command: string) => {
  if (command === 'profile') goToProfile()
  else if (command === 'logout') handleLogout()
}

onMounted(() => {
  fetchUserInfo()
  fetchCategories()
  fetchHotTags()
})
</script>

<template>
  <header class="header-bar">
    <div class="header-inner">
      <div class="search-section">
        <div class="search-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章..."
            clearable
            @clear="handleClear"
            @keyup.enter="handleEnter"
          />
          <el-button @click="handleSearch" class="search-btn">搜索</el-button>
        </div>
      </div>

      <div class="user-section">
        <el-dropdown v-if="userStore.token" trigger="hover" @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="32" :src="userStore.userInfo?.userPic || ''">
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <div v-else class="auth-buttons">
          <el-button class="btn-register" @click="navigateTo('/login')">注册</el-button>
          <el-button type="primary" class="btn-login" @click="navigateTo('/login')">登录</el-button>
        </div>
      </div>
    </div>

    <div v-if="hotTags.length" class="tag-bar">
      <span class="tag-bar-label">热门标签</span>
      <span
        v-for="tag in hotTags"
        :key="tag.id"
        class="tag-item"
        :class="{ active: selectedTag === tag.name }"
        @click="handleTagClick(tag.name)"
      >{{ tag.name }}</span>
    </div>

    <div class="category-bar">
      <span
        class="cat-item"
        :class="{ active: props.selectedCategory === null }"
        @click="selectCategory(null)"
      >全部</span>
      <span
        v-for="cat in categories"
        :key="cat.categoryId || cat.id"
        class="cat-item"
        :class="{ active: props.selectedCategory === (cat.categoryId || cat.id) }"
        @click="selectCategory(cat.categoryId || cat.id)"
      >{{ cat.categoryName || cat.name }}</span>
    </div>
  </header>
</template>

<style scoped>
.header-bar {
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226,232,240,0.35);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: none;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.search-section {
  flex: 1;
  max-width: 520px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0;
  background: #f1f5f9;
  border-radius: 30px;
  padding: 0 4px 0 20px;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.search-box:focus-within {
  background: #fff;
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
}

.search-icon {
  color: #94a3b8;
  font-size: 16px;
  flex-shrink: 0;
}

.search-box :deep(.el-input) {
  flex: 1;
}

.search-box :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding: 8px 10px;
}

.search-box :deep(.el-input__inner) {
  font-size: 14px;
  color: #1e293b;
}

.search-btn {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  color: #fff;
  border-radius: 30px;
  padding: 0 24px;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.search-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.search-btn:hover {
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
  transform: translateY(-1px);
}

.search-btn:hover::after {
  transform: translateX(100%);
}

.search-btn:active {
  transform: translateY(0);
}

.user-section {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 8px;
  transition: background 0.2s ease;
}

.user-info:hover {
  background: #f1f5f9;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.auth-buttons {
  display: flex;
  gap: 8px;
}

.btn-register {
  background: transparent;
  border: 1px solid #e2e8f0;
  color: #64748b;
  border-radius: 8px;
  padding: 0 20px;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
}

.btn-register:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.btn-login {
  background: #6366f1;
  border: none;
  border-radius: 8px;
  padding: 0 24px;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
}

.btn-login:hover {
  background: #4f46e5;
}

.category-bar {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px 12px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.category-bar::-webkit-scrollbar {
  display: none;
}

.cat-item {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  user-select: none;
}

.cat-item:hover {
  background: #e2e8f0;
  color: #1e293b;
  transform: translateY(-1px);
}

.cat-item.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  box-shadow: 0 2px 8px rgba(99,102,241,0.25);
}

.tag-bar {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}

.tag-bar::-webkit-scrollbar {
  display: none;
}

.tag-bar-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  letter-spacing: 0.5px;
  white-space: nowrap;
  flex-shrink: 0;
}

.tag-item {
  padding: 3px 10px;
  font-size: 12px;
  color: #818cf8;
  background: rgba(99,102,241,0.06);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  user-select: none;
}

.tag-item:hover {
  background: rgba(99,102,241,0.12);
  color: #6366f1;
  transform: translateY(-1px);
}

.tag-item.active {
  background: #6366f1;
  color: #fff;
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}

.tag-item.active:hover {
  background: #4f46e5;
  color: #fff;
  transform: translateY(-1px);
}
</style>
