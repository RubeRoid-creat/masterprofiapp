import { useState, useEffect } from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import {
  Assignment as AssignmentIcon,
  People as PeopleIcon,
  VerifiedUser as VerifiedUserIcon,
  AttachMoney as AttachMoneyIcon,
  ReportProblem as ReportProblemIcon,
} from '@mui/icons-material';
import { statsAPI } from '../api/api';
import StatCard from '../components/StatCard';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('admin_token');
    if (token) {
      loadStats();
    } else {
      setError('Требуется авторизация');
      setLoading(false);
    }
  }, []);

  const loadStats = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await statsAPI.getStats();
      setStats(response.data);
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Сессия истекла. Пожалуйста, войдите снова.');
        // Редирект произойдет через interceptor
      } else {
        setError(err.response?.data?.error || 'Ошибка загрузки статистики');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
        <Button variant="contained" onClick={loadStats}>
          Попробовать снова
        </Button>
      </Box>
    );
  }

  if (!stats) {
    return (
      <Box>
        <Alert severity="info" sx={{ mb: 2 }}>
          Нет данных. Убедитесь, что backend сервер запущен.
        </Alert>
        <Button variant="contained" onClick={loadStats}>
          Загрузить данные
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Дашборд
      </Typography>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        {/* Заказы */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Всего заказов"
            value={stats.orders.total}
            icon={<AssignmentIcon />}
            color="#1976d2"
            subtitle={`Сегодня: ${stats.orders.today}`}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Новые"
            value={stats.orders.new}
            icon={<AssignmentIcon />}
            color="#ed6c02"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="В работе"
            value={stats.orders.inProgress}
            icon={<AssignmentIcon />}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Завершено"
            value={stats.orders.completed}
            icon={<AssignmentIcon />}
            color="#0288d1"
          />
        </Grid>

        {/* Мастера */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Всего мастеров"
            value={stats.masters.total}
            icon={<PeopleIcon />}
            color="#9c27b0"
            subtitle={`Верифицировано: ${stats.masters.verified}`}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="На смене"
            value={stats.masters.onShift}
            icon={<PeopleIcon />}
            color="#f57c00"
          />
        </Grid>

        {/* Клиенты */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Всего клиентов"
            value={stats.clients.total}
            icon={<PeopleIcon />}
            color="#1976d2"
            subtitle={`Активных: ${stats.clients.active}`}
          />
        </Grid>

        {/* Доходы */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Доходы платформы"
            value={`${stats.revenue.total.toLocaleString()} ₽`}
            icon={<AttachMoneyIcon />}
            color="#2e7d32"
            subtitle={`За месяц: ${stats.revenue.thisMonth.toLocaleString()} ₽`}
          />
        </Grid>

        {/* Жалобы */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Жалобы"
            value={stats.complaints.total}
            icon={<ReportProblemIcon />}
            color="#d32f2f"
            subtitle={`Ожидают: ${stats.complaints.pending}`}
          />
        </Grid>

        {/* Верификация */}
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Документы на проверке"
            value={stats.verification.pending}
            icon={<VerifiedUserIcon />}
            color="#ed6c02"
          />
        </Grid>
      </Grid>
    </Box>
  );
}

