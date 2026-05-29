import request from '@/utils/request'

export const addLeaveMessage = (data: { name: string; phone: string; email: string; content: string }) => {
  return request.post('/leaveMessage/add', data)
}

export const getLeaveMessages = (params: { pageNum: number; pageSize: number }) => {
  return request.get('/leaveMessage/list', { params })
}

export const batchDeleteLeaveMessages = (ids: number[]) => {
  return request.post('/leaveMessage/batchDelete', ids)
}
