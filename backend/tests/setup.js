// Настройка окружения для тестов
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET = 'test-secret-key';
process.env.PORT = 3001;
process.env.DATABASE_PATH = ':memory:';

// Mock для Firebase Admin
jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  credential: {
    cert: jest.fn()
  },
  messaging: jest.fn(() => ({
    send: jest.fn().mockResolvedValue('success'),
    sendMulticast: jest.fn().mockResolvedValue({ successCount: 1, failureCount: 0 })
  }))
}));

// Mock для WebSocket
jest.mock('../websocket.js', () => ({
  initWebSocket: jest.fn(),
  broadcastToMaster: jest.fn(),
  broadcastToClient: jest.fn()
}));

// Глобальные утилиты для тестов
global.delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));
