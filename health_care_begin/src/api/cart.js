// 购物车相关接口（后端 ServiceCartController；token 由 request.js 自动携带，归属走后端 UserContext）
import request from '@/utils/request'

// 加购：POST /service-cart/add（data: { itemId, quantity=本次新增份数 1~99 }；同项目无行插新、有行数量累加，上限 99 拒）
export const addToCart = (itemId, quantity) => request.post('/service-cart/add', { itemId, quantity })

// 本人购物车全部行：GET /service-cart/list（含 itemStatus 下架标记，商家分组由前端做）
export const listCart = () => request.get('/service-cart/list')

// 改数量：PUT /service-cart/quantity/{cartId}?quantity=（1~99，归属校验）
export const updateCartQuantity = (cartId, quantity) =>
  request.put(`/service-cart/quantity/${cartId}`, null, { params: { quantity } })

// 删行：DELETE /service-cart/remove/{cartId}（归属校验）
export const removeCartItem = (cartId) => request.delete(`/service-cart/remove/${cartId}`)

// 勾选结算：POST /service-cart/checkout
// data: { cartIds: [勾选行ID], elderId, addressId, serviceTime, contactPhone, remark }（整批统一字段，与下单接口同规）
// 逐行各生成一张订单（商家×项目拆单），成功返回订单展示 VO 列表 [{ orderNo, itemName, quantity, totalPrice }]
export const checkoutCart = (data) => request.post('/service-cart/checkout', data)
