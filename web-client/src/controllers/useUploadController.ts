import type { ChangeEvent } from 'react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { storage } from '../services/storage';

const AVAILABLE_TRANSFORMATIONS = [
  { key: 'ESCALA_GRISES', label: 'Escala de grises' },
  { key: 'ROTAR', label: 'Rotar 90°' },
  { key: 'REFLEJAR', label: 'Reflejar horizontal' },
  { key: 'REDIMENSIONAR', label: 'Redimensionar' },
  { key: 'MARCA_AGUA', label: 'Marca de agua' },
] as const;

export function useUploadController() {
  const navigate = useNavigate();
  const [cantidad, setCantidad] = useState(3);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [transformacionesSeleccionadas, setTransformacionesSeleccionadas] = useState<string[]>([
    'ESCALA_GRISES',
    'ROTAR',
  ]);

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

  const onRemoveFile = (index: number) => {
    setFiles((prev) => {
      if (index < 0 || index >= prev.length) {
        return prev;
      }
      const next = prev.filter((_, idx) => idx !== index);
      const nextCantidad = next.length === 0 ? 1 : next.length;
      setCantidad(nextCantidad);
      return next;
    });
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
    const selectedFiles = files.slice(0, clampedCantidad);

    setLoading(true);
    setMessage('');

    try {
      const response = selectedFiles.length > 0
        ? await api.sendBatchWithFiles(token, usuario, selectedFiles, transformacionesSeleccionadas)
        : await api.sendBatch(token, usuario, clampedCantidad);
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
    transformacionesSeleccionadas,
    availableTransformations: AVAILABLE_TRANSFORMATIONS,
    maxCantidad,
    loading,
    message,
    onFileChange,
    onRemoveFile,
    onToggleTransformacion: (key: string) => {
      setTransformacionesSeleccionadas((prev) => {
        if (prev.includes(key)) {
          if (prev.length === 1) {
            return prev;
          }
          return prev.filter((value) => value !== key);
        }
        return [...prev, key];
      });
    },
    onSendBatch,
  };
}
