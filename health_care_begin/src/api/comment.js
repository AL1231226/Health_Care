// 服务评价相关接口（后端 ServiceCommentController；token 由 request.js 自动携带，归属走后端 UserContext）
import request from '@/utils/request'

// 发表评价：POST /service-comment/create
// data: { orderId, score(1~5), content(选填) }
// itemId / providerId / userId 由后端从订单与 token 定（前端传值不生效）
export const createComment = (data) => request.post('/service-comment/create', data)

// 我的全部评价：GET /service-comment/my（已评价列表，最新在前，含订单号/服务名/商家名）
export const listMyComments = () => request.get('/service-comment/my')
