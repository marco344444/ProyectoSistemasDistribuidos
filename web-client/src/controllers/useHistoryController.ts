import { useEffect, useState } from 'react';
import type { HistorialResponse, ReplicaResponse } from '../models/types';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useHistoryController() {
  const token = storage.getToken();

  const [historial, setHistorial] = useState<HistorialResponse | null>(null);
  const [replica, setReplica] = useState<ReplicaResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) {
      setError('No hay sesion activa.');
      return;
    }

    api.getHistorial(token)
      .then((response) => {
        setHistorial(response);
        setError('');
      })
      .catch((e) => {
        setError((e as Error).message);
      });

    api.getReplica()
      .then((response) => {
        setReplica(response);
      })
      .catch(() => {
        // replica is informative only
      });
  }, [token]);

  return {
    historial,
    replica,
    error,
  };
}
