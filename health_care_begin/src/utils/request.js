//定制请求的实例
//导入axios  npm install axios
import axios from 'axios';
//定义一个变量,记录公共的前缀  ,  baseURL
// 后端无 context-path（接口即 /auth/...），跨域由后端 CORS 配置处理
const baseURL = 'http://localhost:8080';
const instance = axios.create({baseURL})

//添加请求拦截器：登录后所有请求自动带上 token（后端 AuthInterceptor 校验）
instance.interceptors.request.use(
    config=>{
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config;
    },
    err=>Promise.reject(err)
)

//添加响应拦截器
instance.interceptors.response.use(
    result=>{
        return result.data;
    },
    err=>{
        //token 缺失或过期：清掉本地登录态，回登录页重新登录
        if (err.response && err.response.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user_info')
            window.location.href = '/login'
        } else {
            alert('服务异常');
        }
        return Promise.reject(err);//异步的状态转化成失败的状态
    }
)

export default instance;
