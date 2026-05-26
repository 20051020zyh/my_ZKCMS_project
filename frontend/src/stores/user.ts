import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import router from '@/router'
import { getAdminHomeStats } from '@/api/user'

let _loginDialogShowing = false

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const isAdmin = ref<boolean>(sessionStorage.getItem('isAdmin') === 'true')

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  const updateUserAvatar = (avatarUrl: string) => {
    if (userInfo.value) {
      userInfo.value = {
        ...userInfo.value,
        userPic: avatarUrl,
        user_pic: avatarUrl
      }
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    isAdmin.value = false
    localStorage.removeItem('token')
    sessionStorage.removeItem('isAdmin')
  }

  const verifyAdmin = async (): Promise<boolean> => {
    if (isAdmin.value) return true
    if (!token.value) return false

    try {
      await getAdminHomeStats()
      isAdmin.value = true
      sessionStorage.setItem('isAdmin', 'true')
      return true
    } catch {
      isAdmin.value = false
      sessionStorage.removeItem('isAdmin')
      return false
    }
  }

  const checkLogin = (message: string = '请先登录') => {
    if (!token.value) {
      if (_loginDialogShowing) return false
      _loginDialogShowing = true
      ElMessageBox.confirm(
        `<div class="prompt-body">
          <div class="prompt-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
          </div>
          <div class="prompt-title">登录以继续</div>
          <p class="prompt-desc">${message}</p>
        </div>`,
        '',
        {
          confirmButtonText: '去登录',
          cancelButtonText: '稍后再说',
          closeOnClickModal: false,
          closeOnPressEscape: true,
          customClass: 'login-prompt-box',
          distinguishCancelAndClose: true,
          icon: undefined,
          dangerouslyUseHTMLString: true
        }
      ).then(() => {
        _loginDialogShowing = false
        router.push('/login')
      }).catch(() => {
        _loginDialogShowing = false
      })
      return false
    }
    return true
  }

  return {
    token,
    userInfo,
    isAdmin,
    setToken,
    setUserInfo,
    updateUserAvatar,
    logout,
    verifyAdmin,
    checkLogin
  }
})
