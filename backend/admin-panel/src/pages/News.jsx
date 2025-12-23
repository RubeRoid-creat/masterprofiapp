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
  IconButton,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from '@mui/material';
import { Edit as EditIcon, Delete as DeleteIcon, Add as AddIcon } from '@mui/icons-material';
import { newsAPI } from '../api/api';

const CATEGORIES = [
  { value: 'general', label: 'Общее' },
  { value: 'tips', label: 'Советы' },
  { value: 'industry', label: 'Индустрия' },
  { value: 'guides', label: 'Руководства' },
  { value: 'tools', label: 'Инструменты' },
  { value: 'trends', label: 'Тренды' },
];

export default function News() {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dialog, setDialog] = useState({ open: false, mode: 'create', data: null });
  const [deleteDialog, setDeleteDialog] = useState({ open: false, id: null });

  const initialForm = {
    title: '',
    summary: '',
    content: '',
    image_url: '',
    category: 'general',
    is_active: 1,
  };
  const [form, setForm] = useState(initialForm);

  useEffect(() => {
    loadNews();
  }, []);

  const loadNews = async () => {
    try {
      const response = await newsAPI.getAll();
      setNews(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки новостей');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (mode, data = null) => {
    setDialog({ open: true, mode, data });
    if (mode === 'edit' && data) {
      setForm({
        title: data.title,
        summary: data.summary || '',
        content: data.content,
        image_url: data.image_url || '',
        category: data.category || 'general',
        is_active: data.is_active,
      });
    } else {
      setForm(initialForm);
    }
  };

  const handleCloseDialog = () => {
    setDialog({ open: false, mode: 'create', data: null });
    setForm(initialForm);
  };

  const handleSubmit = async () => {
    // Валидация на клиенте
    if (!form.title || form.title.trim().length === 0) {
      setError('Заголовок обязателен');
      return;
    }
    
    if (!form.content || form.content.trim().length === 0) {
      setError('Содержание обязательно');
      return;
    }

    try {
      // Подготовка данных для отправки
      const dataToSend = {
        title: form.title.trim(),
        summary: form.summary?.trim() || null,
        content: form.content.trim(),
        image_url: form.image_url?.trim() || null,
        category: form.category || 'general',
        is_active: form.is_active === 1 || form.is_active === true ? 1 : 0
      };

      if (dialog.mode === 'create') {
        await newsAPI.create(dataToSend);
      } else {
        await newsAPI.update(dialog.data.id, dataToSend);
      }
      handleCloseDialog();
      loadNews();
      setError(''); // Очищаем ошибки при успехе
    } catch (err) {
      console.error('Ошибка при сохранении новости:', err);
      const errorMessage = err.response?.data?.error || err.message || 'Ошибка при сохранении новости';
      setError(errorMessage);
    }
  };

  const handleDelete = async () => {
    try {
      await newsAPI.delete(deleteDialog.id);
      setDeleteDialog({ open: false, id: null });
      loadNews();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка при удалении новости');
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
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Управление новостями</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog('create')}
        >
          Добавить новость
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
              <TableCell>ID</TableCell>
              <TableCell>Заголовок</TableCell>
              <TableCell>Категория</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата публикации</TableCell>
              <TableCell align="right">Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {news.map((item) => (
              <TableRow key={item.id}>
                <TableCell>#{item.id}</TableCell>
                <TableCell>
                  <Typography variant="subtitle2">{item.title}</Typography>
                  <Typography variant="body2" color="textSecondary" noWrap sx={{ maxWidth: 300 }}>
                    {item.summary}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Chip 
                    label={CATEGORIES.find(c => c.value === item.category)?.label || item.category} 
                    size="small" 
                  />
                </TableCell>
                <TableCell>
                  <Chip
                    label={item.is_active ? 'Активна' : 'Черновик'}
                    color={item.is_active ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  {new Date(item.published_at).toLocaleDateString()}
                </TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => handleOpenDialog('edit', item)} color="primary">
                    <EditIcon />
                  </IconButton>
                  <IconButton onClick={() => setDeleteDialog({ open: true, id: item.id })} color="error">
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {news.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  Новостей пока нет
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог создания/редактирования */}
      <Dialog open={dialog.open} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {dialog.mode === 'create' ? 'Добавить новость' : 'Редактировать новость'}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <TextField
              label="Заголовок"
              fullWidth
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
            <TextField
              label="Краткое описание"
              fullWidth
              multiline
              rows={2}
              value={form.summary}
              onChange={(e) => setForm({ ...form, summary: e.target.value })}
            />
            <TextField
              label="Содержание"
              fullWidth
              required
              multiline
              rows={6}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
            />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Категория</InputLabel>
                <Select
                  value={form.category}
                  label="Категория"
                  onChange={(e) => setForm({ ...form, category: e.target.value })}
                >
                  {CATEGORIES.map((cat) => (
                    <MenuItem key={cat.value} value={cat.value}>
                      {cat.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Статус</InputLabel>
                <Select
                  value={form.is_active}
                  label="Статус"
                  onChange={(e) => setForm({ ...form, is_active: e.target.value })}
                >
                  <MenuItem value={1}>Активна</MenuItem>
                  <MenuItem value={0}>Черновик</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <TextField
              label="URL изображения"
              fullWidth
              value={form.image_url}
              onChange={(e) => setForm({ ...form, image_url: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Отмена</Button>
          <Button onClick={handleSubmit} variant="contained">
            {dialog.mode === 'create' ? 'Создать' : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог удаления */}
      <Dialog open={deleteDialog.open} onClose={() => setDeleteDialog({ open: false, id: null })}>
        <DialogTitle>Удалить новость?</DialogTitle>
        <DialogContent>
          Вы уверены, что хотите безвозвратно удалить эту новость?
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog({ open: false, id: null })}>Отмена</Button>
          <Button onClick={handleDelete} color="error" variant="contained">Удалить</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
