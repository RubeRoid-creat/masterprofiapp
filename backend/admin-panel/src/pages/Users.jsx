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
import { usersAPI } from '../api/api';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [blockDialog, setBlockDialog] = useState({ open: false, user: null, blocked: false, reason: '' });

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
    </Box>
  );
}

