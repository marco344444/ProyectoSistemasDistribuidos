import type { ChangeEvent } from 'react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { storage } from '../services/storage';

export function useUploadController() {
  const navigate = useNavigate();
  const [cantidad, setCantidad] = useState(3);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState<File[]>([]);

  const maxCantidad = useMemo(() => {
    if (files.length === 0) return 400;
    return Math.max(1, Math.min(400, files.length));
  }, [files.length]);

  const onFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files || []).slice(0, 400);
    setFiles(selected);
    if (selected.length > 0) {
      setCantidad(selected.length);
    }
  };

  const onSendBatch = async () => {
    const token = storage.getToken();
    const usuario = storage.getUser() || 'usuario-demo';

    if (!token) {
      setMessage('No hay sesion activa. Vuelve al login.');
      navigate('/');
      return;
    }

    const clampedCantidad = Math.max(1, Math.min(400, Math.floor(cantidad || files.length || 1)));

    setLoading(true);
    setMessage('');

    try {
      const response = await api.sendBatch(token, usuario, clampedCantidad);
      storage.setBatchId(response.idLote);
      navigate('/status');
    } catch (error) {
      setMessage((error as Error).message || 'Error enviando lote');
    } finally {
      setLoading(false);
    }
  };

  return {
    cantidad,
    setCantidad,
    files,
    maxCantidad,
    loading,
    message,
    onFileChange,
    onSendBatch,
  };
}
