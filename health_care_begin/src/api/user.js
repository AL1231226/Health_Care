import request from '@/utils/request.js'
//登录
export const loginService = (data) => {
    return request.post('/auth/family/login', data)
}
//注册
export const registerService = (data) => {
    return request.post('/auth/family/register', data)
}
//商家登录
export const providerLoginService = (data) => {
    return request.post('/auth/provider/login', data)
}
//商家入驻注册
export const providerRegisterService = (data) => {
    return request.post('/auth/provider/register', data)
}
//管理员登录
export const adminLoginService = (data) => {
    return request.post('/auth/admin/login', data)
}
//家属列表（管理端）：GET /sys-user/list，status 可选 0禁用/1正常，不传查全部
export const listSysUsers = (status) => {
    return request.get('/sys-user/list', { params: status != null ? { status } : {} })
}
//家属启用/禁用（管理端）：PUT /sys-user/status/{id}?status=0 或 1
export const updateUserStatus = (id, status) => {
    return request.put(`/sys-user/status/${id}`, null, { params: { status } })
}
//家属端自助修改密码（本人）：PUT /sys-user/self/password，data: { oldPassword, newPassword }
export const changeUserPassword = (data) => {
    return request.put('/sys-user/self/password', data)
}