<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, logout as logoutApi } from '@/api/user'
import { getHotTags } from '@/api/article'
import { ElMessage } from 'element-plus'
import { navigateTo } from '@/utils/navigate'

const router = useRouter()

interface Props {
  categories: any[]
  selectedCategory: number | null
  selectedTag: string
  sidebarCollapse?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  search: [keyword: string]
  categorySelect: [categoryId: number | null]
  tagSelect: [tagName: string]
}>()

const userStore = useUserStore()
const searchKeyword = ref('')
const hotTags = ref<any[]>([])

const handleSearch = () => {
  const kw = searchKeyword.value.trim()
  if (kw) emit('search', kw)
}

const handleEnter = () => handleSearch()

const handleTagClick = (tagName: string) => {
  if (props.selectedTag === tagName) {
    emit('tagSelect', '')
  } else {
    emit('tagSelect', tagName)
  }
}

const handleLogout = async () => {
  try { await logoutApi() } catch {}
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}

const goToProfile = () => {
  if (!userStore.checkLogin('请先登录')) return
  navigateTo('/profile')
}

const handleCommand = (cmd: string) => {
  if (cmd === 'profile') goToProfile()
  else if (cmd === 'logout') handleLogout()
}

const selectCategory = (id: number | null) => {
  emit('categorySelect', id)
}

const fetchHotTags = async () => {
  try { const r: any = await getHotTags(12); hotTags.value = r.data || [] } catch {}
}

onMounted(async () => {
  if (userStore.token) {
    try { const r: any = await getUserInfo(); userStore.setUserInfo(r.data) } catch {}
  }
  fetchHotTags()
})
</script>

<template>
  <header class="header" :class="{ collapsed: props.sidebarCollapse }">
    <!-- 第一行：搜索 + 用户 -->
    <div class="header-main">
      <div class="header-inner">
        <div class="header-left">
          <!-- 留空，只有右侧内容 -->
        </div>
        <div class="header-right">
          <div class="search-box">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <input v-model="searchKeyword" placeholder="搜索文章..." @keyup.enter="handleEnter" />
          </div>

          <el-dropdown v-if="userStore.token" trigger="hover" @command="handleCommand">
            <div class="user-avatar">
              <el-avatar :size="30" :src="userStore.userInfo?.userPic || ''">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <div v-else class="auth-btns">
            <button class="btn-login" @click="navigateTo('/login')">登录</button>
            <button class="btn-register" @click="navigateTo('/login')">注册</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：热门标签 -->
    <div v-if="hotTags.length" class="tag-strip">
      <div class="strip-inner">
        <span class="strip-label">热门</span>
        <span
          v-for="tag in hotTags"
          :key="tag.id"
          class="tag-chip"
          :class="{ active: props.selectedTag === tag.name }"
          @click="handleTagClick(tag.name)"
        >{{ tag.name }}</span>
      </div>
    </div>

    <!-- 第三行：分类 -->
    <div class="cat-strip">
      <div class="strip-inner">
        <span
          class="cat-chip"
          :class="{ active: props.selectedCategory === null }"
          @click="selectCategory(null)"
        >全部</span>
        <span
          v-for="cat in categories"
          :key="cat.categoryId || cat.id"
          class="cat-chip"
          :class="{ active: props.selectedCategory === (cat.categoryId || cat.id) }"
          @click="selectCategory(cat.categoryId || cat.id)"
        >{{ cat.categoryName || cat.name }}</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: fixed;
  top: 0; left: 240px; right: 0;
  z-index: 100;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(20px) saturate(1.3);
  border-bottom: 1px solid rgba(203,213,225,0.2);
  transition: left 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.header.collapsed {
  left: 64px;
}

.header-main {
  border-bottom: 1px solid rgba(203,213,225,0.12);
}

.header-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 40px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left { flex: 1; }

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border-radius: 100px;
  padding: 0 14px;
  height: 32px;
  border: 1px solid transparent;
  transition: all 0.3s ease;
  width: 200px;
}

.search-box:focus-within {
  background: #fff;
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.06);
  width: 260px;
}

.search-icon { width: 15px; height: 15px; color: #94a3b8; flex-shrink: 0; }

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: #0f172a;
  width: 100%;
  font-family: inherit;
}

.search-box input::placeholder { color: #94a3b8; }

.user-avatar {
  cursor: pointer;
  border-radius: 50%;
  padding: 2px;
  border: 2px solid transparent;
  transition: border-color 0.2s ease;
}

.user-avatar:hover { border-color: rgba(59,130,246,0.3); }

.auth-btns { display: flex; gap: 6px; }

.btn-login {
  height: 30px; padding: 0 14px;
  border-radius: 100px;
  border: 1px solid #e2e8f0;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-login:hover { border-color: #60a5fa; color: #3b82f6; }

.btn-register {
  height: 30px; padding: 0 14px;
  border-radius: 100px;
  border: none;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(59,130,246,0.2);
}

.btn-register:hover {
  box-shadow: 0 4px 12px rgba(59,130,246,0.3);
  transform: translateY(-1px);
}

/* ── 标签条 ── */
.tag-strip {
  border-bottom: 1px solid rgba(203,213,225,0.1);
}

.strip-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 5px 40px;
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}

.strip-inner::-webkit-scrollbar { display: none; }

.strip-label {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  white-space: nowrap;
  padding: 3px 4px;
}

.tag-chip {
  padding: 3px 10px;
  font-size: 12px;
  color: #3b82f6;
  background: rgba(59,130,246,0.06);
  border-radius: 100px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  user-select: none;
}

.tag-chip:hover { background: rgba(59,130,246,0.1); }

.tag-chip.active { background: #3b82f6; color: #fff; }

/* ── 分类条 ── */
.cat-strip {
  border-bottom: 1px solid rgba(203,213,225,0.1);
}

.cat-chip {
  padding: 4px 12px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: rgba(241,245,249,0.5);
  border-radius: 100px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
  user-select: none;
}

.cat-chip:hover { background: #f1f5f9; color: #334155; }

.cat-chip.active {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  box-shadow: 0 2px 8px rgba(59,130,246,0.2);
}

@media (max-width: 1100px) {
  .header { left: 0; }
}

@media (max-width: 900px) {
  .header-inner { padding: 0 20px; }
  .search-box { width: 140px; }
  .search-box:focus-within { width: 180px; }
  .strip-inner { padding: 5px 20px; }
}
</style>
