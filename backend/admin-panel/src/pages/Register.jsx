import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Box,
  Alert,
} from '@mui/material';
import { authAPI } from '../api/api';

export default function Register() {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    name: '',
    phone: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Валидация
    if (!formData.email || !formData.password || !formData.name || !formData.phone) {
      setError('Все поля обязательны для заполнения');
      setLoading(false);
      return;
    }

    if (formData.password.length < 6) {
      setError('Пароль должен содержать минимум 6 символов');
      setLoading(false);
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Пароли не совпадают');
      setLoading(false);
      return;
    }

    // Валидация email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setError('Введите корректный email адрес');
      setLoading(false);
      return;
    }

    // Валидация телефона (российский формат)
    const phoneRegex = /^(\+7|7|8)?[\s\-]?\(?[489][0-9]{2}\)?[\s\-]?[0-9]{3}[\s\-]?[0-9]{2}[\s\-]?[0-9]{2}$/;
    if (!phoneRegex.test(formData.phone.replace(/\s/g, ''))) {
      setError('Введите корректный номер телефона');
      setLoading(false);
      return;
    }

    try {
      console.log('Attempting registration with:', { email: formData.email, name: formData.name, phone: formData.phone });
      const response = await authAPI.register(
        formData.email,
        formData.password,
        formData.name,
        formData.phone
      );
      console.log('Registration response:', response);
      
      const { token, user } = response.data;

      if (!user) {
        setError('Ошибка: пользователь не найден в ответе');
        setLoading(false);
        return;
      }

      if (user.role !== 'admin') {
        setError('Ошибка регистрации: роль не установлена как администратор');
        setLoading(false);
        return;
      }

      localStorage.setItem('admin_token', token);
      console.log('Token saved, navigating to /');
      navigate('/');
    } catch (err) {
      console.error('Registration error:', err);
      console.error('Error response:', err.response);
      
      let errorMessage = 'Ошибка регистрации';
      
      if (err.response) {
        // Сервер ответил с ошибкой
        errorMessage = err.response.data?.error || `Ошибка сервера: ${err.response.status}`;
      } else if (err.request) {
        // Запрос отправлен, но ответа нет
        errorMessage = 'Не удалось подключиться к серверу. Убедитесь, что backend запущен.';
      } else {
        // Ошибка при настройке запроса
        errorMessage = err.message || 'Неизвестная ошибка';
      }
      
      setError(errorMessage);
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ padding: 4, width: '100%' }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom>
            Регистрация администратора
          </Typography>
          <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Создайте учетную запись администратора
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="name"
              label="Имя"
              name="name"
              autoComplete="name"
              autoFocus
              value={formData.name}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label="Email"
              name="email"
              type="email"
              autoComplete="email"
              value={formData.email}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              id="phone"
              label="Телефон"
              name="phone"
              type="tel"
              autoComplete="tel"
              placeholder="+7 (999) 123-45-67"
              value={formData.phone}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Пароль"
              type="password"
              id="password"
              autoComplete="new-password"
              value={formData.password}
              onChange={handleChange}
              helperText="Минимум 6 символов"
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="confirmPassword"
              label="Подтвердите пароль"
              type="password"
              id="confirmPassword"
              autoComplete="new-password"
              value={formData.confirmPassword}
              onChange={handleChange}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? 'Регистрация...' : 'Зарегистрироваться'}
            </Button>
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="body2">
                Уже есть аккаунт?{' '}
                <Link to="/login" style={{ textDecoration: 'none' }}>
                  Войти
                </Link>
              </Typography>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
}






