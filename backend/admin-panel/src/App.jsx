import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import ErrorBoundary from './components/ErrorBoundary';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Orders from './pages/Orders';
import Users from './pages/Users';
import Verification from './pages/Verification';
import Complaints from './pages/Complaints';
import Backups from './pages/Backups';
import News from './pages/News';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

function PrivateRoute({ children }) {
  const location = useLocation();
  const token = localStorage.getItem('admin_token');
  
  if (!token) {
    // Сохраняем текущий путь для редиректа после логина
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  return children;
}

function AppContent() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router basename="/admin">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/*"
            element={
              <PrivateRoute>
                <Layout>
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/orders" element={<Orders />} />
                    <Route path="/users" element={<Users />} />
                    <Route path="/verification" element={<Verification />} />
                    <Route path="/complaints" element={<Complaints />} />
                    <Route path="/backups" element={<Backups />} />
                    <Route path="/news" element={<News />} />
                  </Routes>
                </Layout>
              </PrivateRoute>
            }
          />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <AppContent />
    </ErrorBoundary>
  );
}

export default App;
