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
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material';
import { complaintsAPI } from '../api/api';

export default function Complaints() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [resolveDialog, setResolveDialog] = useState({
    open: false,
    complaint: null,
    resolution: '',
    status: 'resolved',
  });

  useEffect(() => {
    loadComplaints();
  }, []);

  const loadComplaints = async () => {
    try {
      const response = await complaintsAPI.getAll();
      setComplaints(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки жалоб');
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async () => {
    try {
      await complaintsAPI.resolve(
        resolveDialog.complaint.id,
        resolveDialog.resolution,
        resolveDialog.status
      );
      setResolveDialog({ open: false, complaint: null, resolution: '', status: 'resolved' });
      loadComplaints();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка обработки жалобы');
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      pending: 'warning',
      reviewing: 'info',
      resolved: 'success',
      rejected: 'error',
      dismissed: 'default',
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
        Управление жалобами
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
              <TableCell>Жалобщик</TableCell>
              <TableCell>Обвиняемый</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Заголовок</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {complaints.map((complaint) => (
              <TableRow key={complaint.id}>
                <TableCell>#{complaint.id}</TableCell>
                <TableCell>{complaint.complainant_name}</TableCell>
                <TableCell>{complaint.accused_name}</TableCell>
                <TableCell>{complaint.complaint_type}</TableCell>
                <TableCell>{complaint.title}</TableCell>
                <TableCell>
                  <Chip label={complaint.status} color={getStatusColor(complaint.status)} size="small" />
                </TableCell>
                <TableCell>{new Date(complaint.created_at).toLocaleDateString()}</TableCell>
                <TableCell>
                  {complaint.status === 'pending' && (
                    <Button
                      size="small"
                      variant="contained"
                      onClick={() =>
                        setResolveDialog({
                          open: true,
                          complaint,
                          resolution: '',
                          status: 'resolved',
                        })
                      }
                    >
                      Обработать
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог обработки */}
      <Dialog
        open={resolveDialog.open}
        onClose={() => setResolveDialog({ open: false, complaint: null, resolution: '', status: 'resolved' })}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Обработка жалобы</DialogTitle>
        <DialogContent>
          {resolveDialog.complaint && (
            <Box>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Жалобщик:</strong> {resolveDialog.complaint.complainant_name}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Обвиняемый:</strong> {resolveDialog.complaint.accused_name}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Описание:</strong> {resolveDialog.complaint.description}
              </Typography>
              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Статус</InputLabel>
                <Select
                  value={resolveDialog.status}
                  onChange={(e) => setResolveDialog({ ...resolveDialog, status: e.target.value })}
                  label="Статус"
                >
                  <MenuItem value="resolved">Разрешено</MenuItem>
                  <MenuItem value="rejected">Отклонено</MenuItem>
                  <MenuItem value="dismissed">Отклонено без рассмотрения</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Решение"
                value={resolveDialog.resolution}
                onChange={(e) => setResolveDialog({ ...resolveDialog, resolution: e.target.value })}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setResolveDialog({ open: false, complaint: null, resolution: '', status: 'resolved' })}
          >
            Отмена
          </Button>
          <Button onClick={handleResolve} variant="contained">
            Сохранить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

