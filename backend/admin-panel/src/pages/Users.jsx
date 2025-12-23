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
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Switch,
  FormControlLabel,
} from '@mui/material';
import { usersAPI, mastersAPI } from '../api/api';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [blockDialog, setBlockDialog] = useState({ open: false, user: null, blocked: false, reason: '' });
  const [deleteDialog, setDeleteDialog] = useState({ open: false, user: null });

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      const response = await usersAPI.getAll();
      setUsers(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки пользователей');
    } finally {
      setLoading(false);
    }
  };

  const handleBlock = async () => {
    try {
      await usersAPI.block(blockDialog.user.id, blockDialog.blocked, blockDialog.reason);
      setBlockDialog({ open: false, user: null, blocked: false, reason: '' });
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка блокировки пользователя');
    }
  };

  const handleDelete = async () => {
    try {
      if (!deleteDialog.user?.master_id) {
        setError('Этот пользователь не является мастером');
        return;
      }
      
      await mastersAPI.delete(deleteDialog.user.master_id);
      setDeleteDialog({ open: false, user: null });
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка удаления мастера');
    }
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
        Управление пользователями
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
              <TableCell>Имя</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Телефон</TableCell>
              <TableCell>Роль</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Верификация</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell>#{user.id}</TableCell>
                <TableCell>{user.name}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.phone}</TableCell>
                <TableCell>
                  <Chip label={user.role} size="small" />
                </TableCell>
                <TableCell>
                  <Chip
                    label={user.is_blocked ? 'Заблокирован' : 'Активен'}
                    color={user.is_blocked ? 'error' : 'success'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {user.role === 'master' && user.verification_status && (
                    <Chip
                      label={user.verification_status === 'verified' ? 'Верифицирован' : 
                             user.verification_status === 'pending' ? 'На проверке' : 
                             user.verification_status === 'rejected' ? 'Отклонен' : 'Не верифицирован'}
                      color={user.verification_status === 'verified' ? 'success' : 
                             user.verification_status === 'pending' ? 'warning' : 'default'}
                      size="small"
                    />
                  )}
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                      size="small"
                      color={user.is_blocked ? 'success' : 'error'}
                      onClick={() =>
                        setBlockDialog({
                          open: true,
                          user,
                          blocked: !user.is_blocked,
                          reason: '',
                        })
                      }
                    >
                      {user.is_blocked ? 'Разблокировать' : 'Заблокировать'}
                    </Button>
                    {user.role === 'master' && user.master_id && (
                      <Button
                        size="small"
                        color="error"
                        variant="outlined"
                        onClick={() => setDeleteDialog({ open: true, user })}
                      >
                        Удалить
                      </Button>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог блокировки */}
      <Dialog
        open={blockDialog.open}
        onClose={() => setBlockDialog({ open: false, user: null, blocked: false, reason: '' })}
      >
        <DialogTitle>
          {blockDialog.blocked ? 'Заблокировать пользователя' : 'Разблокировать пользователя'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Пользователь: {blockDialog.user?.name} ({blockDialog.user?.email})
          </Typography>
          {blockDialog.blocked && (
            <TextField
              fullWidth
              multiline
              rows={3}
              label="Причина блокировки"
              value={blockDialog.reason}
              onChange={(e) => setBlockDialog({ ...blockDialog, reason: e.target.value })}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBlockDialog({ open: false, user: null, blocked: false, reason: '' })}>
            Отмена
          </Button>
          <Button
            onClick={handleBlock}
            variant="contained"
            color={blockDialog.blocked ? 'error' : 'success'}
          >
            {blockDialog.blocked ? 'Заблокировать' : 'Разблокировать'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог удаления мастера */}
      <Dialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, user: null })}
      >
        <DialogTitle>Удалить аккаунт мастера</DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Вы уверены, что хотите удалить аккаунт мастера?
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>Имя:</strong> {deleteDialog.user?.name}
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            <strong>Email:</strong> {deleteDialog.user?.email}
          </Typography>
          <Typography variant="body2" sx={{ mb: 2 }}>
            <strong>Телефон:</strong> {deleteDialog.user?.phone}
          </Typography>
          <Alert severity="warning" sx={{ mt: 2 }}>
            Это действие необратимо! Будет удален аккаунт мастера и связанный пользователь.
            Все связанные данные (заказы, назначения, транзакции) также будут удалены.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog({ open: false, user: null })}>
            Отмена
          </Button>
          <Button
            onClick={handleDelete}
            variant="contained"
            color="error"
          >
            Удалить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

