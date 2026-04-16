import { useEffect, useMemo, useState } from 'react';
import type { DescargasResponse } from '../models/types';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useDownloadController() {
  const idLote = storage.getBatchId();
  const token = storage.getToken();
  const [data, setData] = useState<DescargasResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!idLote) {
      return;
    }

    if (!token) {
      setError('No hay sesion activa para consultar descargas');
      return;
    }

    api.getDescargas(token, idLote)
      .then((response) => {
        setData(response);
        setError('');
      })
      .catch((e) => {
        setError((e as Error).message);
      });
  }, [idLote, token]);

  const summary = useMemo(() => {
    const archivos = data?.archivos || [];
    const ready = archivos.filter((item) => item.listo).length;
    const pending = archivos.length - ready;
    const totalKb = archivos.reduce((acc, item) => acc + Number(item.tamKb || 0), 0);

    return { ready, pending, totalKb, total: archivos.length };
  }, [data]);

  const downloadFile = async (fileName: string) => {
    const safeName = fileName && fileName.trim() ? fileName.trim() : 'resultado.png';
    if (!token) {
      throw new Error('No hay sesion activa para descargar');
    }

    const blob = await api.descargarImagen(token, safeName);
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = safeName;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  };

  const downloadZip = async () => {
    if (!token) {
      throw new Error('No hay sesion activa para descargar');
    }
    if (!idLote) {
      throw new Error('No hay lote seleccionado para descargar');
    }

    const blob = await api.descargarLoteZip(token, idLote);
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${idLote}.zip`;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  };

  return {
    idLote,
    data,
    error,
    summary,
    downloadFile,
    downloadZip,
  };
}
