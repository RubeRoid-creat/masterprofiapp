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

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      console.log('Attempting login with:', { email, password: '***' });
      const response = await authAPI.login(email, password);
      console.log('Login response:', response);
      
      const { token, user } = response.data;

      if (!user) {
        setError('Ошибка: пользователь не найден в ответе');
        setLoading(false);
        return;
      }

      if (user.role !== 'admin') {
        setError('Доступ разрешен только администраторам');
        setLoading(false);
        return;
      }

      localStorage.setItem('admin_token', token);
      console.log('Token saved, navigating to /');
      navigate('/');
    } catch (err) {
      console.error('Login error:', err);
      console.error('Error response:', err.response);
      
      let errorMessage = 'Ошибка входа';
      
      if (err.response) {
        // Сервер ответил с ошибкой
        errorMessage = err.response.data?.error || `Ошибка сервера: ${err.response.status}`;
      } else if (err.request) {
        // Запрос отправлен, но ответа нет
        errorMessage = 'Не удалось подключиться к серверу. Убедитесь, что backend запущен на http://localhost:3000';
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
            BestApp Admin
          </Typography>
          <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Вход в панель администратора
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
              id="email"
              label="Email"
              name="email"
              autoComplete="email"
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Пароль"
              type="password"
              id="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? 'Вход...' : 'Войти'}
            </Button>
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="body2">
                Нет аккаунта?{' '}
                <Link to="/register" style={{ textDecoration: 'none' }}>
                  Зарегистрироваться
                </Link>
              </Typography>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
}

