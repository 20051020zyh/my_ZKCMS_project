import request from '@/utils/request'

// 提交留言
export const addLeaveMessage = (data: any) => {
  return request.post('/leaveMessage/add', data)
}
