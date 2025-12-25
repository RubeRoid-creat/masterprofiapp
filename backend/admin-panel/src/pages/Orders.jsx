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
  Autocomplete,
  CircularProgress,
  Alert,
} from '@mui/material';
import { ordersAPI, statsAPI, mastersAPI } from '../api/api';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [masters, setMasters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [assignDialog, setAssignDialog] = useState({ open: false, orderId: null, deviceType: null });
  const [selectedMaster, setSelectedMaster] = useState(null);
  const [masterSearch, setMasterSearch] = useState('');
  const [loadingMasters, setLoadingMasters] = useState(false);
  const [cancelDialog, setCancelDialog] = useState({ open: false, orderId: null, reason: '' });

  useEffect(() => {
    loadOrders();
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

  const loadMasters = async (search = '', deviceType = null) => {
    try {
      setLoadingMasters(true);
      const params = { 
        verified: 'true' // Показываем только верифицированных мастеров
      };
      
      if (search) {
        params.search = search;
      }
      
      if (deviceType) {
        params.device_type = deviceType;
      }
      
      const response = await mastersAPI.getList(params);
      setMasters(response.data);
    } catch (err) {
      console.error('Ошибка загрузки мастеров:', err);
      setError(err.response?.data?.error || 'Ошибка загрузки мастеров');
    } finally {
      setLoadingMasters(false);
    }
  };
  
  useEffect(() => {
    if (assignDialog.open && assignDialog.deviceType) {
      // Загружаем мастеров при открытии диалога с учетом специализации
      loadMasters('', assignDialog.deviceType);
    }
  }, [assignDialog.open, assignDialog.deviceType]);
  
  useEffect(() => {
    if (assignDialog.open && masterSearch) {
      // Debounce для поиска
      const timer = setTimeout(() => {
        loadMasters(masterSearch, assignDialog.deviceType);
      }, 300);
      
      return () => clearTimeout(timer);
    }
  }, [masterSearch, assignDialog.open, assignDialog.deviceType]);

  const handleAssign = async () => {
    if (!selectedMaster) {
      setError('Выберите мастера');
      return;
    }
    
    try {
      await ordersAPI.assign(assignDialog.orderId, selectedMaster.id);
      setAssignDialog({ open: false, orderId: null, deviceType: null });
      setSelectedMaster(null);
      setMasterSearch('');
      loadOrders();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка назначения заказа');
    }
  };
  
  const handleOpenAssignDialog = (orderId) => {
    // Находим заказ для получения device_type
    const order = orders.find(o => o.id === orderId);
    const deviceType = order?.device_type || null;
    
    setAssignDialog({ open: true, orderId, deviceType });
    setSelectedMaster(null);
    setMasterSearch('');
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
                        onClick={() => handleOpenAssignDialog(order.id)}
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
      <Dialog 
        open={assignDialog.open} 
        onClose={() => {
          setAssignDialog({ open: false, orderId: null, deviceType: null });
          setSelectedMaster(null);
          setMasterSearch('');
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Назначить заказ мастеру
          {assignDialog.deviceType && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Требуемая специализация: {assignDialog.deviceType}
            </Typography>
          )}
        </DialogTitle>
        <DialogContent>
          {assignDialog.deviceType && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Показываются только верифицированные мастера со специализацией: <strong>{assignDialog.deviceType}</strong>
            </Alert>
          )}
          <Autocomplete
            sx={{ mt: 2 }}
            options={masters}
            getOptionLabel={(option) => `${option.name || 'Без имени'} (ID: ${option.id})`}
            loading={loadingMasters}
            value={selectedMaster}
            onChange={(event, newValue) => {
              setSelectedMaster(newValue);
            }}
            onInputChange={(event, newInputValue) => {
              setMasterSearch(newInputValue);
              // Поиск будет выполняться через useEffect с debounce
            }}
            inputValue={masterSearch}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Поиск мастера по имени"
                placeholder="Введите имя или фамилию мастера"
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <>
                      {loadingMasters ? <CircularProgress color="inherit" size={20} /> : null}
                      {params.InputProps.endAdornment}
                    </>
                  ),
                }}
              />
            )}
            renderOption={(props, option) => (
              <Box component="li" {...props} key={option.id}>
                <Box>
                  <Typography variant="body1">
                    {option.name || 'Без имени'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Email: {option.email} • Телефон: {option.phone}
                    {option.rating > 0 && ` • Рейтинг: ${option.rating.toFixed(1)}`}
                    {option.completed_orders > 0 && ` • Заказов: ${option.completed_orders}`}
                  </Typography>
                </Box>
              </Box>
            )}
            noOptionsText={loadingMasters ? "Загрузка..." : "Мастера не найдены"}
          />
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => {
              setAssignDialog({ open: false, orderId: null, deviceType: null });
              setSelectedMaster(null);
              setMasterSearch('');
            }}
          >
            Отмена
          </Button>
          <Button 
            onClick={handleAssign} 
            variant="contained" 
            disabled={!selectedMaster}
          >
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

