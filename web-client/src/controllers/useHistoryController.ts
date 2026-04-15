import { useEffect, useState } from 'react';
import type { HistorialResponse, ReplicaResponse } from '../models/types';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useHistoryController() {
  const usuario = storage.getUser() || 'usuario-demo';

  const [historial, setHistorial] = useState<HistorialResponse | null>(null);
  const [replica, setReplica] = useState<ReplicaResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getHistorial(usuario)
      .then((response) => {
        setHistorial(response);
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
  }, [usuario]);

  return {
    historial,
    replica,
    error,
  };
}
