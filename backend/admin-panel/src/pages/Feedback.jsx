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
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Card,
  CardContent,
} from '@mui/material';
import { Visibility as VisibilityIcon, CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { feedbackAPI } from '../api/api';

const FEEDBACK_TYPES = {
  suggestion: 'Предложение',
  bug_report: 'Сообщение об ошибке',
  complaint: 'Жалоба',
  praise: 'Благодарность',
  other: 'Другое',
};

const STATUS_COLORS = {
  new: 'default',
  in_progress: 'warning',
  resolved: 'success',
  closed: 'default',
};

export default function Feedback() {
  const [feedback, setFeedback] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [viewDialog, setViewDialog] = useState({ open: false, data: null });
  const [respondDialog, setRespondDialog] = useState({ open: false, data: null, response: '' });
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterType, setFilterType] = useState('all');

  useEffect(() => {
    loadFeedback();
  }, [filterStatus, filterType]);

  const loadFeedback = async () => {
    try {
      setLoading(true);
      const params = {};
      if (filterStatus !== 'all') params.status = filterStatus;
      if (filterType !== 'all') params.feedback_type = filterType;
      
      const response = await feedbackAPI.getAll(params);
      setFeedback(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки обратной связи');
    } finally {
      setLoading(false);
    }
  };

  const handleView = (item) => {
    setViewDialog({ open: true, data: item });
  };

  const handleRespond = (item) => {
    setRespondDialog({ open: true, data: item, response: item.admin_response || '' });
  };

  const handleSubmitResponse = async () => {
    try {
      await feedbackAPI.respond(
        respondDialog.data.id,
        respondDialog.response,
        'resolved'
      );
      setRespondDialog({ open: false, data: null, response: '' });
      loadFeedback();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отправки ответа');
    }
  };

  const handleUpdateStatus = async (id, status) => {
    try {
      await feedbackAPI.updateStatus(id, status);
      loadFeedback();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка обновления статуса');
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
      <Box sx={{ mb: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
        <Typography variant="h4">Обратная связь</Typography>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Статус</InputLabel>
          <Select
            value={filterStatus}
            label="Статус"
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <MenuItem value="all">Все</MenuItem>
            <MenuItem value="new">Новые</MenuItem>
            <MenuItem value="in_progress">В работе</MenuItem>
            <MenuItem value="resolved">Решено</MenuItem>
            <MenuItem value="closed">Закрыто</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Тип</InputLabel>
          <Select
            value={filterType}
            label="Тип"
            onChange={(e) => setFilterType(e.target.value)}
          >
            <MenuItem value="all">Все</MenuItem>
            <MenuItem value="suggestion">Предложение</MenuItem>
            <MenuItem value="bug_report">Ошибка</MenuItem>
            <MenuItem value="complaint">Жалоба</MenuItem>
            <MenuItem value="praise">Благодарность</MenuItem>
            <MenuItem value="other">Другое</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Пользователь</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Тема</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {feedback.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.id}</TableCell>
                <TableCell>
                  <Box>
                    <Typography variant="body2">{item.user_name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.user_email}
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>{FEEDBACK_TYPES[item.feedback_type] || item.feedback_type}</TableCell>
                <TableCell>{item.subject}</TableCell>
                <TableCell>
                  <Chip
                    label={item.status}
                    color={STATUS_COLORS[item.status] || 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {new Date(item.created_at).toLocaleDateString('ru-RU')}
                </TableCell>
                <TableCell>
                  <Button
                    size="small"
                    startIcon={<VisibilityIcon />}
                    onClick={() => handleView(item)}
                  >
                    Просмотр
                  </Button>
                  {item.status !== 'resolved' && (
                    <Button
                      size="small"
                      color="primary"
                      startIcon={<CheckCircleIcon />}
                      onClick={() => handleRespond(item)}
                      sx={{ ml: 1 }}
                    >
                      Ответить
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог просмотра */}
      <Dialog open={viewDialog.open} onClose={() => setViewDialog({ open: false, data: null })} maxWidth="md" fullWidth>
        <DialogTitle>Обратная связь #{viewDialog.data?.id}</DialogTitle>
        <DialogContent>
          {viewDialog.data && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Пользователь: {viewDialog.data.user_name} ({viewDialog.data.user_email})
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Тип: {FEEDBACK_TYPES[viewDialog.data.feedback_type]}
              </Typography>
              <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>
                {viewDialog.data.subject}
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                {viewDialog.data.message}
              </Typography>
              {viewDialog.data.admin_response && (
                <Card sx={{ mt: 2, bgcolor: 'primary.light' }}>
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>
                      Ответ администрации:
                    </Typography>
                    <Typography variant="body2">{viewDialog.data.admin_response}</Typography>
                  </CardContent>
                </Card>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialog({ open: false, data: null })}>Закрыть</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог ответа */}
      <Dialog open={respondDialog.open} onClose={() => setRespondDialog({ open: false, data: null, response: '' })} maxWidth="sm" fullWidth>
        <DialogTitle>Ответить на обратную связь</DialogTitle>
        <DialogContent>
          {respondDialog.data && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                От: {respondDialog.data.user_name}
              </Typography>
              <Typography variant="h6" sx={{ mt: 1, mb: 2 }}>
                {respondDialog.data.subject}
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Ответ администрации"
                value={respondDialog.response}
                onChange={(e) => setRespondDialog({ ...respondDialog, response: e.target.value })}
                sx={{ mt: 2 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRespondDialog({ open: false, data: null, response: '' })}>
            Отмена
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmitResponse}
            disabled={!respondDialog.response.trim()}
          >
            Отправить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
