import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'
import { getMyPermissionPaths, checkAdmin } from '@/api/user'
import request from '@/utils/request'

const _maintenanceCache = { active: false, checking: false, callbacks: [] as ((active: boolean) => void)[] }

const checkMaintenance = (): Promise<boolean> => {
  if (_maintenanceCache.checking) {
    return new Promise(resolve => {
      _maintenanceCache.callbacks.push(resolve)
    })
  }
  _maintenanceCache.checking = true
  return request.get('/index/popInfo').then((res: any) => {
    const active = res.data?.type === 'MAINTENANCE'
    _maintenanceCache.active = active
    _maintenanceCache.checking = false
    _maintenanceCache.callbacks.forEach(cb => cb(active))
    _maintenanceCache.callbacks = []
    return active
  }).catch(() => {
    _maintenanceCache.active = false
    _maintenanceCache.checking = false
    _maintenanceCache.callbacks.forEach(cb => cb(false))
    _maintenanceCache.callbacks = []
    return false
  })
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    meta: { title: '知库 - 首页' },
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    meta: { title: '知库 - 登录' },
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    meta: { title: '知库 - 文章详情' },
    component: () => import('@/views/ArticleDetail.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    meta: { title: '知库 - 个人中心' },
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/about',
    name: 'About',
    meta: { title: '知库 - 关于' },
    component: () => import('@/views/About.vue')
  },
  {
    path: '/join',
    name: 'Join',
    meta: { title: '知库 - 加入我们' },
    component: () => import('@/views/Join.vue')
  },
  {
    path: '/notices',
    name: 'Notices',
    meta: { title: '知库 - 公告中心' },
    component: () => import('@/views/NoticeCenter.vue')
  },
  {
    path: '/article/create',
    name: 'CreateArticle',
    meta: { title: '知库 - 写文章' },
    component: () => import('@/views/CreateArticle.vue')
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    meta: { title: '管理后台 - 登录' },
    component: () => import('@/views/admin/AdminLogin.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        meta: { title: '管理后台 - 控制台' },
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'articles',
        name: 'AdminArticles',
        meta: { title: '管理后台 - 文章管理' },
        component: () => import('@/views/admin/ArticleManage.vue')
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        meta: { title: '管理后台 - 分类管理' },
        component: () => import('@/views/admin/CategoryManage.vue')
      },
      {
        path: 'comments',
        name: 'AdminComments',
        meta: { title: '管理后台 - 评论管理' },
        component: () => import('@/views/admin/CommentManage.vue')
      },
      {
        path: 'reports',
        name: 'AdminReports',
        meta: { title: '管理后台 - 举报管理' },
        component: () => import('@/views/admin/ReportManage.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        meta: { title: '管理后台 - 用户管理' },
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        meta: { title: '管理后台 - 公告管理' },
        component: () => import('@/views/admin/NoticeManage.vue')
      },
      {
        path: 'roles',
        name: 'AdminRoles',
        meta: { title: '管理后台 - 角色权限' },
        component: () => import('@/views/admin/RoleManage.vue')
      },
      {
        path: 'permissions',
        name: 'AdminPermissions',
        meta: { title: '管理后台 - 权限菜单' },
        component: () => import('@/views/admin/PermissionManage.vue')
      },
      {
        path: 'leaveMessages',
        name: 'AdminLeaveMessages',
        meta: { title: '管理后台 - 留言管理' },
        component: () => import('@/views/admin/LeaveMessageManage.vue')
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        meta: { title: '管理后台 - 系统设置' },
        component: () => import('@/views/admin/Settings.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from) => {
  // 维护模式检查：对普通用户页面在导航前拦截
  if (!to.path.startsWith('/admin') && to.path !== '/login' && to.path !== '/register') {
    const isMaintenance = await checkMaintenance()
    if (isMaintenance) {
      return '/'
    }
  }

  if (to.path === '/admin/login') {
    return true
  }

  if (to.path.startsWith('/admin')) {
    const token = localStorage.getItem('token')
    if (!token) {
      return '/admin/login'
    }

    try {
      const adminRes: any = await checkAdmin()
      if (!adminRes.data) {
        localStorage.removeItem('token')
        sessionStorage.removeItem('isAdmin')
        return '/admin/login?noPermission=1'
      }

      const res: any = await getMyPermissionPaths()
      const perms: string[] = res.data || []
      if (perms.length === 0) {
        localStorage.removeItem('token')
        sessionStorage.removeItem('isAdmin')
        return '/admin/login?noPermission=1'
      }
      return true
    } catch {
      localStorage.removeItem('token')
      sessionStorage.removeItem('isAdmin')
      return '/admin/login?noPermission=1'
    }
  } else {
    return true
  }
})

router.afterEach((to) => {
  const title = to.meta?.title
  if (title) {
    document.title = title as string
  }
})

export default router
