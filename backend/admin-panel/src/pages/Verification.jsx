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
  ImageList,
  ImageListItem,
} from '@mui/material';
import { verificationAPI } from '../api/api';

export default function Verification() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [viewDialog, setViewDialog] = useState({ open: false, document: null });
  const [rejectDialog, setRejectDialog] = useState({ open: false, document: null, reason: '' });

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      const response = await verificationAPI.getDocuments();
      setDocuments(response.data);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки документов');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      await verificationAPI.approve(id);
      loadDocuments();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка одобрения документа');
    }
  };

  const handleReject = async () => {
    try {
      await verificationAPI.reject(rejectDialog.document.id, rejectDialog.reason);
      setRejectDialog({ open: false, document: null, reason: '' });
      loadDocuments();
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отклонения документа');
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      pending: 'warning',
      approved: 'success',
      rejected: 'error',
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
        Модерация документов верификации
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
              <TableCell>Мастер</TableCell>
              <TableCell>Тип документа</TableCell>
              <TableCell>Название</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {documents.map((doc) => (
              <TableRow key={doc.id}>
                <TableCell>#{doc.id}</TableCell>
                <TableCell>{doc.master_name}</TableCell>
                <TableCell>{doc.document_type}</TableCell>
                <TableCell>{doc.document_name}</TableCell>
                <TableCell>
                  <Chip label={doc.status} color={getStatusColor(doc.status)} size="small" />
                </TableCell>
                <TableCell>{new Date(doc.created_at).toLocaleDateString()}</TableCell>
                <TableCell>
                  {doc.status === 'pending' && (
                    <>
                      <Button
                        size="small"
                        color="success"
                        onClick={() => handleApprove(doc.id)}
                        sx={{ mr: 1 }}
                      >
                        Одобрить
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        onClick={() => setRejectDialog({ open: true, document: doc, reason: '' })}
                      >
                        Отклонить
                      </Button>
                    </>
                  )}
                  <Button
                    size="small"
                    onClick={() => setViewDialog({ open: true, document: doc })}
                    sx={{ ml: 1 }}
                  >
                    Просмотр
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Диалог просмотра */}
      <Dialog
        open={viewDialog.open}
        onClose={() => setViewDialog({ open: false, document: null })}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Документ: {viewDialog.document?.document_name}</DialogTitle>
        <DialogContent>
          {viewDialog.document && (
            <Box>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Мастер:</strong> {viewDialog.document.master_name}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Тип:</strong> {viewDialog.document.document_type}
              </Typography>
              <img
                src={`http://localhost:3000${viewDialog.document.file_url}`}
                alt={viewDialog.document.document_name}
                style={{ maxWidth: '100%', height: 'auto' }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialog({ open: false, document: null })}>Закрыть</Button>
        </DialogActions>
      </Dialog>

      {/* Диалог отклонения */}
      <Dialog
        open={rejectDialog.open}
        onClose={() => setRejectDialog({ open: false, document: null, reason: '' })}
      >
        <DialogTitle>Отклонить документ</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Причина отклонения"
            value={rejectDialog.reason}
            onChange={(e) => setRejectDialog({ ...rejectDialog, reason: e.target.value })}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialog({ open: false, document: null, reason: '' })}>
            Отмена
          </Button>
          <Button onClick={handleReject} variant="contained" color="error">
            Отклонить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

