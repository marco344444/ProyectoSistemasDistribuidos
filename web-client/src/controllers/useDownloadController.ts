import { useEffect, useMemo, useState } from 'react';
import type { DescargasResponse } from '../models/types';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useDownloadController() {
  const idLote = storage.getBatchId();
  const [data, setData] = useState<DescargasResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!idLote) {
      return;
    }

    api.getDescargas(idLote)
      .then((response) => {
        setData(response);
        setError('');
      })
      .catch((e) => {
        setError((e as Error).message);
      });
  }, [idLote]);

  const summary = useMemo(() => {
    const archivos = data?.archivos || [];
    const ready = archivos.filter((item) => item.listo).length;
    const pending = archivos.length - ready;
    const totalKb = archivos.reduce((acc, item) => acc + Number(item.tamKb || 0), 0);

    return { ready, pending, totalKb, total: archivos.length };
  }, [data]);

  return {
    idLote,
    data,
    error,
    summary,
  };
}
