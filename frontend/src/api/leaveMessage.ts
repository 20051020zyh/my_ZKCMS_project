import request from '@/utils/request'

export const addLeaveMessage = (data: { name: string; phone: string; email: string; content: string }) => {
  return request.post('/leaveMessage/add', data)
}
