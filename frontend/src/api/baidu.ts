import request from '@/utils/request'

/** 百度热搜条目 */
export interface BaiduHotItem {
  rank: number
  word: string
  desc: string
  hotScore: string
  url: string
}

/** 获取百度实时热搜 Top 10 */
export const getBaiduHotSearch = () => {
  return request.get<any, { code: number; data: BaiduHotItem[] }>('/baidu/hot')
}
