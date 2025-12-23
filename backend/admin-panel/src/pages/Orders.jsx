import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
} from '@mui/material';
import { ordersAPI, statsAPI } from '../api/api';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [masters, setMasters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [assignDialog, setAssignDialog] = useState({ open: false, orderId: null });
  const [selectedMaster, setSelectedMaster] = useState('');
  const [cancelDialog, setCancelDialog] = useState({ open: false, orderId: null, reason: '' });

  useEffect(() => {
    loadOrders();
    loadMasters();
  }, []);

  const loadOrders = async () => {
    try {
      const response = await ordersAPI.getAll();
      setOrders(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки заказов');
    } finally {
      setLoading(false);
    }
  };

  const loadMasters = async () => {
    try {
      const response = await statsAPI.getStats();
      // Здесь нужно будет добавить API для получения списка мастеров
      // Пока используем заглушку
    } catch (err) {
      console.error('Ошибка загрузки мастеров:', err);
    }
  };

  const handleAssign = async () => {
    try {
      await ordersAPI.assign(assignDialog.orderId, selectedMaster);
      setAssignDialog({ open: false, orderId: null });
      setSelectedMaster('');
      loadOrders();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка назначения заказа');
    }
  };

  const handleCancel = async () => {
    try {
      await ordersAPI.cancel(cancelDialog.orderId, cancelDialog.reason);
      setCancelDialog({ open: false, orderId: null, reason: '' });
      loadOrders();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отмены заказа');
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      new: 'warning',
      in_progress: 'info',
      completed: 'success',
      cancelled: 'error',
    };
    return colors[status] || 'default';
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Управление заказами
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper} sx={{ mt: 2 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Клиент</TableCell>
              <TableCell>Устройство</TableCell>
              <TableCell>Проблема</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Стоимость</TableCell>
              <TableCell>Дата</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.id}>
                <TableCell>#{order.id}</TableCell>
                <TableCell>{order.client_name || 'N/A'}</TableCell>
                <TableCell>
                  {order.device_brand} {order.device_model}
                </TableCell>
                <TableCell>{order.problem_description?.substring(0, 50)}...</TableCell>
                <TableCell>
                  <Chip
                    label={order.repair_status}
                    color={getStatusColor(order.repair_status)}
                    size="small"
                  />
                </TableCell>
                <TableCell>{order.estimated_cost ? `${order.estimated_cost} ₽` : 'N/A'}</TableCell>
                <TableCell>{new Date(order.created_at).toLocaleDateString()}</TableCell>
                <TableCell>
                  {order.repair_status === 'new' && (
                    <>
                      <Button
                        size="small"
                        onClick={() => setAssignDialog({ open: true, orderId: order.id })}
                        sx={{ mr: 1 }}
                      >
                        Назначить
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        onClick={() => setCancelDialog({ open: true, orderId: order.id, reason: '' })}
                      >
                        Отменить
                      </Button>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог назначения */}
      <Dialog open={assignDialog.open} onClose={() => setAssignDialog({ open: false, orderId: null })}>
        <DialogTitle>Назначить заказ мастеру</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ mt: 2 }}>
            <InputLabel>Мастер</InputLabel>
            <Select
              value={selectedMaster}
              onChange={(e) => setSelectedMaster(e.target.value)}
              label="Мастер"
            >
              <MenuItem value={1}>Мастер #1</MenuItem>
              {/* Здесь нужно загрузить реальный список мастеров */}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssignDialog({ open: false, orderId: null })}>Отмена</Button>
          <Button onClick={handleAssign} variant="contained" disabled={!selectedMaster}>
            Назначить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог отмены */}
      <Dialog open={cancelDialog.open} onClose={() => setCancelDialog({ open: false, orderId: null, reason: '' })}>
        <DialogTitle>Отменить заказ</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Причина отмены"
            value={cancelDialog.reason}
            onChange={(e) => setCancelDialog({ ...cancelDialog, reason: e.target.value })}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelDialog({ open: false, orderId: null, reason: '' })}>Отмена</Button>
          <Button onClick={handleCancel} variant="contained" color="error">
            Отменить заказ
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

