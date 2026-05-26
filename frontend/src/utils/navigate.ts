import router from '@/router'

export const navigateTo = (path: string) => {
  const route = router.resolve(path)
  window.open(route.href, '_blank')
}
