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
  Button,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
} from '@mui/material';
import { Backup as BackupIcon, Restore as RestoreIcon } from '@mui/icons-material';
import { backupAPI } from '../api/api';

export default function Backups() {
  const [backups, setBackups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [restoreDialog, setRestoreDialog] = useState({ open: false, fileName: null });

  useEffect(() => {
    loadBackups();
  }, []);

  const loadBackups = async () => {
    try {
      const response = await backupAPI.list();
      setBackups(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки бэкапов');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    setCreating(true);
    setError('');
    try {
      await backupAPI.create();
      loadBackups();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка создания бэкапа');
    } finally {
      setCreating(false);
    }
  };

  const handleRestore = async () => {
    try {
      await backupAPI.restore(restoreDialog.fileName);
      setRestoreDialog({ open: false, fileName: null });
      alert('База данных успешно восстановлена!');
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка восстановления бэкапа');
    }
  };

  const formatFileSize = (bytes) => {
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
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
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h4">Управление бэкапами</Typography>
        <Button
          variant="contained"
          startIcon={<BackupIcon />}
          onClick={handleCreate}
          disabled={creating}
        >
          {creating ? 'Создание...' : 'Создать бэкап'}
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Имя файла</TableCell>
              <TableCell>Размер</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {backups.map((backup) => (
              <TableRow key={backup.fileName}>
                <TableCell>{backup.fileName}</TableCell>
                <TableCell>{formatFileSize(backup.fileSize)}</TableCell>
                <TableCell>{new Date(backup.createdAt).toLocaleString()}</TableCell>
                <TableCell>
                  <IconButton
                    color="primary"
                    onClick={() => setRestoreDialog({ open: true, fileName: backup.fileName })}
                  >
                    <RestoreIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог восстановления */}
      <Dialog
        open={restoreDialog.open}
        onClose={() => setRestoreDialog({ open: false, fileName: null })}
      >
        <DialogTitle>Восстановить из бэкапа?</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите восстановить базу данных из бэкапа{' '}
            <strong>{restoreDialog.fileName}</strong>?
          </Typography>
          <Alert severity="warning" sx={{ mt: 2 }}>
            Текущая база данных будет заменена. Перед восстановлением будет создан автоматический бэкап.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRestoreDialog({ open: false, fileName: null })}>Отмена</Button>
          <Button onClick={handleRestore} variant="contained" color="error">
            Восстановить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

