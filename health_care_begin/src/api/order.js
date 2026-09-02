// 服务订单相关接口（后端 ServiceOrderController；token 由 request.js 自动携带，归属走后端 UserContext）
import request from '@/utils/request'

// 提交订单：POST /service-order/create
// data: { elderId, itemId, quantity, serviceTime, addressId, contactPhone, remark }
// user_id / provider_id / 单价总价 / 状态由后端从 token 与服务项目快照定（前端传值不生效）
export const createOrder = (data) => request.post('/service-order/create', data)

// 商家端：本店订单列表 GET /service-order/merchant/list，status 可选 0待接单 1服务中 2已完成 3已取消，不传查全部
export const listMerchantOrders = (status) => {
  return request.get('/service-order/merchant/list', { params: status != null ? { status } : {} })
}

// 商家端：接单(0→1) / 完成服务(1→2)：PUT /service-order/status/{orderId}?status=
export const updateOrderStatus = (orderId, status) =>
  request.put(`/service-order/status/${orderId}`, null, { params: { status } })

// 家属端：我的订单列表 GET /service-order/user/list，status 可选 0待接单 1服务中 2已完成 3已取消，不传查全部
export const listUserOrders = (status) => {
  return request.get('/service-order/user/list', { params: status != null ? { status } : {} })
}

// 家属端：取消待接单订单(0→3)：PUT /service-order/cancel/{orderId}
export const cancelOrder = (orderId) => request.put(`/service-order/cancel/${orderId}`)
