import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { DownloadView } from './views/DownloadView';
import { HistoryView } from './views/HistoryView';
import { LoginView } from './views/LoginView';
import { ProtectedRoute } from './views/ProtectedRoute';
import { StatusView } from './views/StatusView';
import { UploadView } from './views/UploadView';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginView />} />
        <Route
          path="/upload"
          element={
            <ProtectedRoute>
              <UploadView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/status"
          element={
            <ProtectedRoute>
              <StatusView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/download"
          element={
            <ProtectedRoute>
              <DownloadView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/history"
          element={
            <ProtectedRoute>
              <HistoryView />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
