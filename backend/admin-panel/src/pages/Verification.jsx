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
  Tabs,
  Tab,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Grid,
  Card,
  CardContent,
  Divider,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { verificationAPI } from '../api/api';

export default function Verification() {
  const [masters, setMasters] = useState([]);
  const [allMastersStats, setAllMastersStats] = useState({ pending: 0, verified: 0, rejected: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tabValue, setTabValue] = useState(0); // 0 - pending, 1 - verified, 2 - rejected
  const [expandedMaster, setExpandedMaster] = useState(null);
  const [viewDialog, setViewDialog] = useState({ open: false, document: null, master: null });
  const [rejectDialog, setRejectDialog] = useState({ open: false, master: null, reason: '' });
  const [rejectDocDialog, setRejectDocDialog] = useState({ open: false, document: null, reason: '' });
  const [masterDocuments, setMasterDocuments] = useState({});

  useEffect(() => {
    loadMasters();
    loadAllMastersStats();
  }, [tabValue]);

  const loadAllMastersStats = async () => {
    try {
      // Загружаем статистику по всем статусам для табов
      const [pendingRes, verifiedRes, rejectedRes] = await Promise.all([
        verificationAPI.getMasters('pending'),
        verificationAPI.getMasters('verified'),
        verificationAPI.getMasters('rejected'),
      ]);
      setAllMastersStats({
        pending: pendingRes.data.length,
        verified: verifiedRes.data.length,
        rejected: rejectedRes.data.length,
      });
    } catch (err) {
      console.error('Ошибка загрузки статистики:', err);
    }
  };

  const loadMasters = async () => {
    try {
      setLoading(true);
      setError('');
      const status = tabValue === 0 ? 'pending' : tabValue === 1 ? 'verified' : 'rejected';
      const response = await verificationAPI.getMasters(status);
      setMasters(response.data);
      
      // Загружаем документы для каждого мастера
      const docsPromises = response.data.map(async (master) => {
        try {
          const docsResponse = await verificationAPI.getMasterDocuments(master.id);
          return { masterId: master.id, documents: docsResponse.data };
        } catch (err) {
          console.error(`Ошибка загрузки документов мастера ${master.id}:`, err);
          return { masterId: master.id, documents: [] };
        }
      });
      
      const docsResults = await Promise.all(docsPromises);
      const docsMap = {};
      docsResults.forEach(result => {
        docsMap[result.masterId] = result.documents;
      });
      setMasterDocuments(docsMap);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки мастеров');
    } finally {
      setLoading(false);
    }
  };

  const loadMasterDocuments = async (masterId) => {
    try {
      const response = await verificationAPI.getMasterDocuments(masterId);
      setMasterDocuments(prev => ({
        ...prev,
        [masterId]: response.data
      }));
    } catch (err) {
      console.error('Ошибка загрузки документов мастера:', err);
    }
  };

  const handleApproveDocument = async (documentId) => {
    try {
      await verificationAPI.approveDocument(documentId);
      // Находим мастера по documentId и перезагружаем его документы
      const masterId = Object.keys(masterDocuments).find(id => 
        masterDocuments[id].some(doc => doc.id === documentId)
      );
      if (masterId) {
        await loadMasterDocuments(masterId);
        await loadMasters(); // Перезагружаем список мастеров для обновления статистики
        await loadAllMastersStats(); // Обновляем статистику в табах
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка одобрения документа');
    }
  };

  const handleRejectDocument = async () => {
    try {
      await verificationAPI.rejectDocument(rejectDocDialog.document.id, rejectDocDialog.reason);
      setRejectDocDialog({ open: false, document: null, reason: '' });
      
      const masterId = Object.keys(masterDocuments).find(id => 
        masterDocuments[id].some(doc => doc.id === rejectDocDialog.document.id)
      );
      if (masterId) {
        await loadMasterDocuments(masterId);
        await loadMasters();
        await loadAllMastersStats(); // Обновляем статистику в табах
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отклонения документа');
    }
  };

  const handleVerifyMaster = async (masterId) => {
    try {
      await verificationAPI.verifyMaster(masterId);
      await loadMasters();
      await loadAllMastersStats();
      setError(''); // Очищаем ошибки при успехе
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка верификации мастера');
    }
  };

  const handleRejectMaster = async () => {
    try {
      await verificationAPI.rejectMaster(rejectDialog.master.id, rejectDialog.reason);
      setRejectDialog({ open: false, master: null, reason: '' });
      await loadMasters();
      await loadAllMastersStats();
      setError('');
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отклонения мастера');
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      pending: 'warning',
      approved: 'success',
      verified: 'success',
      not_verified: 'default',
      rejected: 'error',
    };
    return colors[status] || 'default';
  };

  const getVerificationStatusLabel = (status) => {
    const labels = {
      not_verified: 'Не верифицирован',
      pending: 'На проверке',
      verified: 'Верифицирован',
      rejected: 'Отклонен',
    };
    return labels[status] || status;
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
        Верификация мастеров
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 2 }}>
        <Tab label={`На проверке (${allMastersStats.pending})`} />
        <Tab label={`Верифицированные (${allMastersStats.verified})`} />
        <Tab label={`Отклоненные (${allMastersStats.rejected})`} />
      </Tabs>

      <Box sx={{ mt: 2 }}>
        {masters.length === 0 ? (
          <Alert severity="info">Нет мастеров для отображения</Alert>
        ) : (
          masters.map((master) => {
            const documents = masterDocuments[master.id] || [];
            const pendingDocs = documents.filter(doc => doc.status === 'pending');
            const approvedDocs = documents.filter(doc => doc.status === 'approved');
            const rejectedDocs = documents.filter(doc => doc.status === 'rejected');

            return (
              <Accordion
                key={master.id}
                expanded={expandedMaster === master.id}
                onChange={(e, isExpanded) => setExpandedMaster(isExpanded ? master.id : null)}
                sx={{ mb: 2 }}
              >
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} md={4}>
                      <Typography variant="h6">{master.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {master.email} • {master.phone}
                      </Typography>
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <Typography variant="body2">
                        <strong>ИНН:</strong> {master.inn || 'Не указан'}
                      </Typography>
                    </Grid>
                    <Grid item xs={12} md={2}>
                      <Chip
                        label={getVerificationStatusLabel(master.verification_status)}
                        color={getStatusColor(master.verification_status)}
                        size="small"
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Typography variant="body2">
                        Документы: {approvedDocs.length} одобрено, {pendingDocs.length} на проверке, {rejectedDocs.length} отклонено
                      </Typography>
                    </Grid>
                  </Grid>
                </AccordionSummary>
                <AccordionDetails>
                  <Divider sx={{ mb: 2 }} />
                  
                  {/* Действия с мастером */}
                  {tabValue === 0 && pendingDocs.length === 0 && approvedDocs.length > 0 && (
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle1" gutterBottom>
                        Все документы одобрены. Можно верифицировать мастера.
                      </Typography>
                      <Box sx={{ mt: 1 }}>
                        <Button
                          variant="contained"
                          color="success"
                          onClick={() => handleVerifyMaster(master.id)}
                          sx={{ mr: 1 }}
                        >
                          Верифицировать мастера
                        </Button>
                        <Button
                          variant="outlined"
                          color="error"
                          onClick={() => setRejectDialog({ open: true, master, reason: '' })}
                        >
                          Отклонить верификацию
                        </Button>
                      </Box>
                    </Box>
                  )}

                  {/* Список документов */}
                  {documents.length === 0 ? (
                    <Alert severity="warning">У мастера нет загруженных документов</Alert>
                  ) : (
                    <TableContainer component={Paper} variant="outlined">
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Тип</TableCell>
                            <TableCell>Название</TableCell>
                            <TableCell>Статус</TableCell>
                            <TableCell>Дата загрузки</TableCell>
                            <TableCell>Действия</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {documents.map((doc) => (
                            <TableRow key={doc.id}>
                              <TableCell>{doc.document_type}</TableCell>
                              <TableCell>{doc.document_name}</TableCell>
                              <TableCell>
                                <Chip
                                  label={doc.status}
                                  color={getStatusColor(doc.status)}
                                  size="small"
                                />
                              </TableCell>
                              <TableCell>
                                {new Date(doc.created_at).toLocaleDateString('ru-RU')}
                              </TableCell>
                              <TableCell>
                                {doc.status === 'pending' && (
                                  <>
                                    <Button
                                      size="small"
                                      color="success"
                                      onClick={() => handleApproveDocument(doc.id)}
                                      sx={{ mr: 1 }}
                                    >
                                      Одобрить
                                    </Button>
                                    <Button
                                      size="small"
                                      color="error"
                                      onClick={() => setRejectDocDialog({ open: true, document: doc, reason: '' })}
                                    >
                                      Отклонить
                                    </Button>
                                  </>
                                )}
                                <Button
                                  size="small"
                                  onClick={() => setViewDialog({ open: true, document: doc, master })}
                                  sx={{ ml: doc.status !== 'pending' ? 0 : 1 }}
                                >
                                  Просмотр
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}

                  {/* Предупреждение об отсутствии ИНН */}
                  {!master.inn && (
                    <Alert severity="warning" sx={{ mt: 2 }}>
                      У мастера не указан ИНН. Это обязательное поле для верификации.
                    </Alert>
                  )}
                </AccordionDetails>
              </Accordion>
            );
          })
        )}
      </Box>

      {/* Диалог просмотра документа */}
      <Dialog
        open={viewDialog.open}
        onClose={() => setViewDialog({ open: false, document: null, master: null })}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Документ: {viewDialog.document?.document_name}</DialogTitle>
        <DialogContent>
          {viewDialog.document && viewDialog.master && (
            <Box>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Мастер:</strong> {viewDialog.master.name}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>ИНН:</strong> {viewDialog.master.inn || 'Не указан'}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Email:</strong> {viewDialog.master.email}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Телефон:</strong> {viewDialog.master.phone}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                <strong>Тип:</strong> {viewDialog.document.document_type}
              </Typography>
              {viewDialog.document.rejection_reason && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  <strong>Причина отклонения:</strong> {viewDialog.document.rejection_reason}
                </Alert>
              )}
              <img
                src={`http://212.74.227.208:3000${viewDialog.document.file_url}`}
                alt={viewDialog.document.document_name}
                style={{ maxWidth: '100%', height: 'auto', marginTop: '16px' }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialog({ open: false, document: null, master: null })}>
            Закрыть
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог отклонения документа */}
      <Dialog
        open={rejectDocDialog.open}
        onClose={() => setRejectDocDialog({ open: false, document: null, reason: '' })}
      >
        <DialogTitle>Отклонить документ</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Причина отклонения"
            value={rejectDocDialog.reason}
            onChange={(e) => setRejectDocDialog({ ...rejectDocDialog, reason: e.target.value })}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDocDialog({ open: false, document: null, reason: '' })}>
            Отмена
          </Button>
          <Button onClick={handleRejectDocument} variant="contained" color="error">
            Отклонить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог отклонения мастера */}
      <Dialog
        open={rejectDialog.open}
        onClose={() => setRejectDialog({ open: false, master: null, reason: '' })}
      >
        <DialogTitle>Отклонить верификацию мастера</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Это действие отклонит верификацию мастера и все его pending документы.
          </Alert>
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
          <Button onClick={() => setRejectDialog({ open: false, master: null, reason: '' })}>
            Отмена
          </Button>
          <Button onClick={handleRejectMaster} variant="contained" color="error">
            Отклонить
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
