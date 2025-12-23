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
    // Не логируем ошибки 401, если пользователь не залогинен - это нормально
    if (error.response?.status !== 401) {
      console.error('API Error:', error);
    }
    
    if (error.response?.status === 401) {
      const token = localStorage.getItem('admin_token');
      if (token) {
        // Токен был, но стал невалидным - удаляем его
        localStorage.removeItem('admin_token');
      }
      // Редиректим на логин только если мы не на странице логина
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/admin/login';
      }
    } else if (!error.response) {
      // Нет ответа от сервера (сервер не запущен или CORS проблема)
      console.error('Сервер недоступен. Убедитесь, что backend запущен.');
    }
    return Promise.reject(error);
  }
);

// Аутентификация
export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (email, password, name, phone) => 
    api.post('/auth/register', { 
      email, 
      password, 
      name, 
      phone, 
      role: 'admin' 
    }),
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

// Мастера
export const mastersAPI = {
  delete: (masterId) => api.delete(`/admin/masters/${masterId}`),
};

// Верификация
export const verificationAPI = {
  getDocuments: (status, groupByMaster) => api.get('/verification/admin/documents', { 
    params: { status, groupByMaster: groupByMaster ? 'true' : undefined } 
  }),
  getMasters: (status) => api.get('/verification/admin/masters', { params: { status } }),
  getMasterDocuments: (masterId, status) => api.get(`/verification/admin/masters/${masterId}/documents`, { 
    params: { status } 
  }),
  approveDocument: (id) => api.post(`/verification/admin/documents/${id}/approve`),
  rejectDocument: (id, reason) => api.post(`/verification/admin/documents/${id}/reject`, { reason }),
  verifyMaster: (masterId) => api.post(`/verification/admin/masters/${masterId}/verify`),
  rejectMaster: (masterId, reason) => api.post(`/verification/admin/masters/${masterId}/reject`, { reason }),
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

