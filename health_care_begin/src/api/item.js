// 服务项目接口（商家端，需登录；归属自动取 token 商家，前端无需传 providerId）
import request from '@/utils/request'

// 当前商家的服务列表：GET /service-item/list，status 可选 0下架/1上架，不传查全部
export const listItems = (status) => {
    return request.get('/service-item/list', { params: status != null ? { status } : {} })
}
// 新增服务：POST /service-item/add（后端强制归属 + 默认上架）
export const addItem = (data) => request.post('/service-item/add', data)
// 修改服务：PUT /service-item/update（评分/销量后端忽略）
export const updateItem = (data) => request.put('/service-item/update', data)
// 删除服务：DELETE /service-item/delete/{id}
export const deleteItem = (id) => request.delete(`/service-item/delete/${id}`)
// 服务详情：GET /service-item/get/{id}
export const getItem = (id) => request.get(`/service-item/get/${id}`)
// 上下架切换：PUT /service-item/status/{id}?status=0 或 1
export const toggleItemStatus = (id, status) => request.put(`/service-item/status/${id}`, null, { params: { status } })
