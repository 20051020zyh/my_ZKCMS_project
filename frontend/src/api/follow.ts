import request from '@/utils/request'

export const toggleFollow = (followedUserId: number) =>
  request.post('/user/follow/toggle', null, { params: { followedUserId } })

export const checkFollow = (followedUserId: number) =>
  request.get('/user/follow/check', { params: { followedUserId } })

export const getFollowList = (pageNum = 1, pageSize = 10) =>
  request.get('/user/follow/list', { params: { pageNum, pageSize } })

export const getFansList = (pageNum = 1, pageSize = 10) =>
  request.get('/user/follow/fans/list', { params: { pageNum, pageSize } })

export const getUserProfile = (userId: number, pageNum = 1, pageSize = 10) =>
  request.get(`/user/profile/${userId}`, { params: { pageNum, pageSize } })
