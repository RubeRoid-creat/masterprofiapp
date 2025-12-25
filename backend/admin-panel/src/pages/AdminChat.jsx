import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Badge,
  Paper,
  TextField,
  IconButton,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import { Send as SendIcon, Person as PersonIcon } from '@mui/icons-material';
import { adminChatAPI } from '../api/api';

export default function AdminChat() {
  const [users, setUsers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [messageText, setMessageText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    if (selectedUserId) {
      loadMessages(selectedUserId);
      // Обновляем сообщения каждые 3 секунды
      const interval = setInterval(() => {
        loadMessages(selectedUserId);
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [selectedUserId]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await adminChatAPI.getUsers();
      setUsers(response.data);
      if (response.data.length > 0 && !selectedUserId) {
        setSelectedUserId(response.data[0].id);
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка загрузки пользователей');
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async (userId) => {
    try {
      const response = await adminChatAPI.getMessages(userId);
      setMessages(response.data);
    } catch (err) {
      console.error('Ошибка загрузки сообщений:', err);
    }
  };

  const handleSendMessage = async () => {
    if (!messageText.trim() || !selectedUserId) return;

    try {
      setSending(true);
      await adminChatAPI.sendMessage(selectedUserId, messageText);
      setMessageText('');
      loadMessages(selectedUserId);
    } catch (err) {
      setError(err.response?.data?.error || 'Ошибка отправки сообщения');
    } finally {
      setSending(false);
    }
  };

  const selectedUser = users.find(u => u.id === selectedUserId);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', height: 'calc(100vh - 64px)' }}>
      {/* Список пользователей */}
      <Paper sx={{ width: 300, borderRight: 1, borderColor: 'divider' }}>
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h6">Пользователи</Typography>
        </Box>
        <List sx={{ overflow: 'auto', height: 'calc(100vh - 120px)' }}>
          {users.map((user) => (
            <ListItem key={user.id} disablePadding>
              <ListItemButton
                selected={selectedUserId === user.id}
                onClick={() => setSelectedUserId(user.id)}
              >
                <ListItemAvatar>
                  <Badge badgeContent={user.unread_count || 0} color="error">
                    <Avatar>
                      <PersonIcon />
                    </Avatar>
                  </Badge>
                </ListItemAvatar>
                <ListItemText
                  primary={user.name}
                  secondary={user.email}
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Paper>

      {/* Чат */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {selectedUser ? (
          <>
            <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
              <Typography variant="h6">{selectedUser.name}</Typography>
              <Typography variant="body2" color="text.secondary">
                {selectedUser.email} • {selectedUser.phone}
              </Typography>
            </Box>

            <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
              {messages.map((msg) => (
                <Box
                  key={msg.id}
                  sx={{
                    display: 'flex',
                    justifyContent: msg.sender_role === 'admin' ? 'flex-end' : 'flex-start',
                    mb: 2,
                  }}
                >
                  <Paper
                    sx={{
                      p: 1.5,
                      maxWidth: '70%',
                      bgcolor: msg.sender_role === 'admin' ? 'primary.main' : 'grey.200',
                      color: msg.sender_role === 'admin' ? 'white' : 'text.primary',
                    }}
                  >
                    <Typography variant="body2">{msg.message_text}</Typography>
                    {msg.image_url && (
                      <Box sx={{ mt: 1 }}>
                        <img
                          src={`${import.meta.env.VITE_API_URL?.replace('/api', '') || 'http://212.74.227.208:3000'}${msg.image_url}`}
                          alt="Attachment"
                          style={{ maxWidth: '100%', borderRadius: 4 }}
                        />
                      </Box>
                    )}
                    <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7 }}>
                      {new Date(msg.created_at).toLocaleString('ru-RU')}
                    </Typography>
                  </Paper>
                </Box>
              ))}
            </Box>

            <Divider />

            <Box sx={{ p: 2, display: 'flex', gap: 1 }}>
              <TextField
                fullWidth
                size="small"
                placeholder="Введите сообщение..."
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
              />
              <IconButton
                color="primary"
                onClick={handleSendMessage}
                disabled={!messageText.trim() || sending}
              >
                {sending ? <CircularProgress size={24} /> : <SendIcon />}
              </IconButton>
            </Box>
          </>
        ) : (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
            <Typography color="text.secondary">Выберите пользователя для начала чата</Typography>
          </Box>
        )}

        {error && (
          <Alert severity="error" onClose={() => setError('')} sx={{ m: 2 }}>
            {error}
          </Alert>
        )}
      </Box>
    </Box>
  );
}
