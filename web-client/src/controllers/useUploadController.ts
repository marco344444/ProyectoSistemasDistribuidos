import type { ChangeEvent } from 'react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { storage } from '../services/storage';

const AVAILABLE_TRANSFORMATIONS = [
  { key: 'ESCALA_GRISES', label: 'Escala de grises' },
  { key: 'RECORTAR', label: 'Recortar' },
  { key: 'ROTAR', label: 'Rotar 90°' },
  { key: 'REFLEJAR', label: 'Reflejar horizontal' },
  { key: 'REDIMENSIONAR', label: 'Redimensionar' },
  { key: 'DESENFOCAR', label: 'Desenfocar' },
  { key: 'PERFILAR', label: 'Perfilar (nitidez)' },
  { key: 'AJUSTE_BRILLO_CONTRASTE', label: 'Ajustar brillo/contraste' },
  { key: 'MARCA_AGUA', label: 'Marca de agua' },
  { key: 'CONVERTIR_FORMATO', label: 'Convertir formato (JPG/PNG/TIF)' },
] as const;

const MAX_BATCH_BYTES = 1536 * 1024 * 1024;
const MAX_FILES_PER_BATCH = 2000;

function buildEstimatedTimeLabel(totalFiles: number, totalBytes: number): string {
  if (totalFiles <= 0) {
    return 'Sin datos';
  }

  const mb = totalBytes / (1024 * 1024);
  const weighted = totalFiles + Math.ceil(mb / 6);

  if (weighted <= 5) return '5 - 12 seg';
  if (weighted <= 15) return '12 - 25 seg';
  if (weighted <= 35) return '25 - 45 seg';
  if (weighted <= 60) return '45 - 90 seg';
  if (weighted <= 100) return '1.5 - 3 min';
  if (weighted <= 180) return '3 - 6 min';
  if (weighted <= 260) return '6 - 10 min';
  return '10+ min';
}

export function useUploadController() {
  const navigate = useNavigate();
  const [cantidad, setCantidad] = useState(3);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [transformacionesSeleccionadas, setTransformacionesSeleccionadas] = useState<string[]>([]);

  const maxCantidad = useMemo(() => {
    if (files.length === 0) return MAX_FILES_PER_BATCH;
    return Math.max(1, Math.min(MAX_FILES_PER_BATCH, files.length));
  }, [files.length]);

  const estimatedFiles = useMemo(() => {
    if (files.length === 0) {
      return Math.max(1, Math.min(MAX_FILES_PER_BATCH, Math.floor(cantidad || 1)));
    }
    return Math.max(1, Math.min(files.length, Math.floor(cantidad || files.length)));
  }, [cantidad, files]);

  const estimatedBytes = useMemo(() => {
    if (files.length === 0) {
      return 0;
    }
    return files.slice(0, estimatedFiles).reduce((acc, file) => acc + file.size, 0);
  }, [estimatedFiles, files]);

  const estimatedTime = useMemo(
    () => buildEstimatedTimeLabel(estimatedFiles, estimatedBytes),
    [estimatedBytes, estimatedFiles]
  );

  const totalSizeMb = useMemo(
    () => (estimatedBytes / (1024 * 1024)).toFixed(1),
    [estimatedBytes]
  );

  const onFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files || []).slice(0, MAX_FILES_PER_BATCH);
    setFiles(selected);
    if (selected.length > 0) {
      const totalBytes = selected.reduce((acc, file) => acc + file.size, 0);
      if (totalBytes > MAX_BATCH_BYTES) {
        const totalMb = (totalBytes / (1024 * 1024)).toFixed(1);
        setMessage(`El lote seleccionado supera 1.5 GB (actual: ${totalMb} MB).`);
      } else {
        setMessage('');
      }
    }
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
    const usuario = storage.getUser() || 'usuario';

    if (!token) {
      setMessage('No hay sesion activa. Vuelve al login.');
      navigate('/');
      return;
    }

    const clampedCantidad = Math.max(1, Math.min(MAX_FILES_PER_BATCH, Math.floor(cantidad || files.length || 1)));
    const selectedFiles = files.slice(0, clampedCantidad);

    if (selectedFiles.length === 0) {
      setMessage('Debes cargar al menos una imagen real para enviar el lote.');
      return;
    }

    if (selectedFiles.length > 0) {
      const totalBytes = selectedFiles.reduce((acc, file) => acc + file.size, 0);
      if (totalBytes > MAX_BATCH_BYTES) {
        const totalMb = (totalBytes / (1024 * 1024)).toFixed(1);
        setMessage(`El tamaño total del lote excede 1.5 GB (actual: ${totalMb} MB).`);
        return;
      }
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await api.sendBatchWithFiles(token, usuario, selectedFiles, transformacionesSeleccionadas);
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
    estimatedTime,
    totalSizeMb,
    onFileChange,
    onRemoveFile,
    onToggleTransformacion: (key: string) => {
      setTransformacionesSeleccionadas((prev) => {
        if (prev.includes(key)) {
          return prev.filter((value) => value !== key);
        }
        return [...prev, key];
      });
    },
    onSendBatch,
  };
}
