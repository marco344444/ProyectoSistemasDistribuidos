import { useCallback, useEffect, useMemo, useState } from 'react';
import type { EstadoResponse, MetricasResponse } from '../models/types';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useStatusController() {
  const token = storage.getToken();
  const idLote = storage.getBatchId();

  const [estado, setEstado] = useState<EstadoResponse | null>(null);
  const [metricas, setMetricas] = useState<MetricasResponse | null>(null);
  const [error, setError] = useState('');

  const refreshEstado = useCallback(async () => {
    if (!token || !idLote) {
      return;
    }

    try {
      const data = await api.getEstado(token, idLote);
      setEstado(data);
      setError('');
    } catch (e) {
      setError((e as Error).message);
    }
  }, [idLote, token]);

  const refreshMetricas = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      const data = await api.getMetricas(token);
      setMetricas(data);
    } catch {
      // Keep previous metrics visible if backend fails temporarily.
    }
  }, [token]);

  useEffect(() => {
    const initialRefresh = window.setTimeout(() => {
      refreshEstado();
      refreshMetricas();
    }, 0);

    const estadoTimer = window.setInterval(refreshEstado, 1000);
    const metricasTimer = window.setInterval(refreshMetricas, 50);

    return () => {
      window.clearTimeout(initialRefresh);
      window.clearInterval(estadoTimer);
      window.clearInterval(metricasTimer);
    };
  }, [refreshEstado, refreshMetricas]);

  const progreso = useMemo(() => {
    if (!estado) return 0;
    if (estado.total <= 0) {
      return estado.estado === 'COMPLETADO' ? 100 : 0;
    }
    return Math.round((estado.procesadas / estado.total) * 100);
  }, [estado]);

  const uiEstado = useMemo(() => {
    const value = estado?.estado || 'RECIBIDO';
    if (value === 'COMPLETADO') return { label: 'Completado', className: 'status-badge chip-green' };
    if (value === 'ERROR') return { label: 'Error', className: 'status-badge chip-red' };
    if (value === 'EN_PROCESO') return { label: 'En progreso', className: 'status-badge badge-amber' };
    return { label: value, className: 'status-badge badge-amber' };
  }, [estado?.estado]);

  return {
    token,
    idLote,
    estado,
    metricas,
    error,
    progreso,
    uiEstado,
  };
}
