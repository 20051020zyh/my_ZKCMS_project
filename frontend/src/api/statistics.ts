import request from '@/utils/request'

export const getBrowserDeviceStats = (days = 7) => {
  return request.get('/statistics/browser-device', { params: { days } })
}

export const aggregateStats = () => {
  return request.post('/statistics/aggregate')
}
