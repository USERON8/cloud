import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const request = axios.create({
    baseURL: '', // 使用相对路径，让vite代理处理
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    config => {
        // 获取token并添加到请求头
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        console.log('发送请求:', config)
        return config
    },
    error => {
        console.error('请求错误:', error)
        ElMessage.error('请求发送失败')
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        console.log('响应数据:', response.data)
        
        // 根据业务状态码显示不同的提示信息
        const { code, message } = response.data
        
        // 成功提示（只对修改操作显示成功提示）
        if (code === 200) {
            // 对于非查询类的操作显示成功提示
            if (response.config.method !== 'get' && message) {
                ElMessage.success(message)
            }
        } 
        // 业务错误提示
        else {
            ElMessage.error(message || '操作失败')
            return Promise.reject(new Error(message || '操作失败'))
        }
        
        return response.data
    },
    error => {
        console.error('响应错误:', error)
        
        // HTTP错误提示
        if (error.response) {
            const { status, data } = error.response
            let message = '请求失败'
            
            // 根据HTTP状态码显示不同的错误提示
            switch (status) {
                case 400:
                    message = data.message || '请求参数错误'
                    break
                case 401:
                    message = '未授权，请重新登录'
                    // 清除本地存储的认证信息
                    localStorage.removeItem('token')
                    localStorage.removeItem('userType')
                    localStorage.removeItem('nickname')
                    localStorage.removeItem('userId')
                    // 重定向到登录页面
                    window.location.href = '/login'
                    break
                case 403:
                    message = '禁止访问'
                    break
                case 404:
                    message = '请求的资源不存在'
                    break
                case 500:
                    message = '服务器内部错误'
                    break
                default:
                    message = data.message || `请求失败 (${status})`
            }
            
            ElMessage.error(message)
        } else if (error.request) {
            ElMessage.error('网络连接失败，请检查网络')
        } else {
            ElMessage.error('请求配置错误')
        }

        return Promise.reject(error)
    }
)

export default request