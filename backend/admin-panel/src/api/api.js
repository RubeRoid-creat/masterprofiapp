import axios from 'axios';

// Получаем URL из переменных окружения или используем дефолтный
// По умолчанию указываем боевой backend на сервере
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://212.74.227.208:3000/api';

console.log('API Base URL:', API_BASE_URL);

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Добавляем токен к каждому запросу
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Обработка ошибок
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    } else if (!error.response) {
      // Нет ответа от сервера (сервер не запущен или CORS проблема)
      console.error('Сервер недоступен. Убедитесь, что backend запущен на http://localhost:3000');
    }
    return Promise.reject(error);
  }
);

// Аутентификация
export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
};

// Статистика
export const statsAPI = {
  getStats: () => api.get('/admin/stats'),
  getOrdersStats: (period = 'all') => api.get(`/admin/stats/orders?period=${period}`),
};

// Заказы
export const ordersAPI = {
  getAll: (params) => api.get('/orders', { params }),
  getById: (id) => api.get(`/orders/${id}`),
  assign: (orderId, masterId) => api.post(`/admin/orders/${orderId}/assign`, { masterId }),
  cancel: (orderId, reason) => api.post(`/admin/orders/${orderId}/cancel`, { reason }),
};

// Пользователи
export const usersAPI = {
  getAll: (params) => api.get('/admin/users', { params }),
  block: (userId, blocked, reason) => api.post(`/admin/users/${userId}/block`, { blocked, reason }),
};

// Верификация
export const verificationAPI = {
  getDocuments: (status) => api.get('/verification/admin/documents', { params: { status } }),
  approve: (id) => api.post(`/verification/admin/documents/${id}/approve`),
  reject: (id, reason) => api.post(`/verification/admin/documents/${id}/reject`, { reason }),
};

// Жалобы
export const complaintsAPI = {
  getAll: (status) => api.get('/complaints/admin/all', { params: { status } }),
  getById: (id) => api.get(`/complaints/admin/${id}`),
  resolve: (id, resolution, status) => api.post(`/complaints/admin/${id}/resolve`, { resolution, status }),
};

// Бэкапы
export const backupAPI = {
  create: () => api.post('/admin/backup/create'),
  list: () => api.get('/admin/backup/list'),
  restore: (fileName) => api.post('/admin/backup/restore', { fileName }),
};

export default api;

