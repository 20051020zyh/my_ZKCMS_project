import request from '@/utils/request'

export const addFolder = (name: string) =>
  request.post('/collect/folder/add', null, { params: { name } })

export const updateFolder = (id: number, name: string) =>
  request.put('/collect/folder/update', null, { params: { id, name } })

export const deleteFolder = (id: number) =>
  request.delete('/collect/folder/delete', { params: { id } })

export const getFolderList = () =>
  request.get('/collect/folder/list')
