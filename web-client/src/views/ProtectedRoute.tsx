import type { ReactElement } from 'react';
import { Navigate } from 'react-router-dom';
import { storage } from '../services/storage';

type Props = {
  children: ReactElement;
};

export function ProtectedRoute({ children }: Props) {
  const token = storage.getToken();
  if (!token) {
    return <Navigate to="/" replace />;
  }
  return children;
}
