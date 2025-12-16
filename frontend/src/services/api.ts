import axios from 'axios';

// 创建 axios 实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
});

// 请求拦截器 - 自动携带 JWT 令牌
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一处理响应和错误
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    // 处理不同的错误状态码
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，清除本地存储
          localStorage.removeItem('token');
          break;
        case 403:
          console.error('没有权限访问该资源');
          break;
        case 404:
          console.error('请求的资源不存在');
          break;
        case 500:
          console.error('服务器内部错误');
          break;
        default:
          console.error('请求失败:', error.response.data.message || '未知错误');
      }
    } else if (error.request) {
      console.error('没有收到响应，请检查网络连接');
    } else {
      console.error('请求配置错误:', error.message);
    }
    return Promise.reject(error);
  }
);

// 健康检查 API
export const healthApi = {
  checkHealth: () => {
    return api.get('/health');
  }
};

// 认证相关 API
export const authApi = {
  // 用户注册
  register: (data: { username: string; password: string }) => {
    return api.post('/auth/register', data);
  },
  
  // 用户登录
  login: (data: { username: string; password: string }) => {
    return api.post('/auth/login', data);
  },
};

// 历史人物相关 API
export const historicalPersonApi = {
  // 模糊搜索历史人物
  search: (name: string) => {
    return api.get(`/historical-persons/search?name=${encodeURIComponent(name)}`);
  },
  
  // 获取随机历史人物
  getRandom: () => {
    return api.get('/historical-persons/random');
  },
  
  // 根据 ID 获取历史人物详情
  getById: (id: number) => {
    return api.get(`/historical-persons/${id}`);
  },
  
  // 获取所有历史人物
  getAll: () => {
    return api.get('/historical-persons');
  },
  
  // 添加新历史人物
  add: (person: any) => {
    return api.post('/historical-persons', person);
  },
  
  // 删除历史人物
  delete: (id: number) => {
    return api.delete(`/historical-persons/${id}`);
  },
  
  // 批量添加历史人物
  batchAdd: (persons: any[]) => {
    return api.post('/historical-persons/batch', persons);
  },
  
  // 更新历史人物
  update: (id: number, person: any) => {
    return api.put(`/historical-persons/${id}`, person);
  },
};

// 导出 api 实例，用于其他 API 调用
export default api;