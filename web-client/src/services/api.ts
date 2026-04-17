import type {
  DescargasResponse,
  EstadoResponse,
  HistorialResponse,
  LoginResponse,
  MetricasResponse,
  RegisterResponse,
  ReplicaResponse,
  SendBatchResponse,
} from '../models/types';

const BASE_URL = 'http://localhost:8080/api';

async function fileToBase64(file: File): Promise<string> {
  const arrayBuffer = await file.arrayBuffer();
  let binary = '';
  const bytes = new Uint8Array(arrayBuffer);
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    const sub = bytes.subarray(i, i + chunk);
    binary += String.fromCharCode(...sub);
  }
  return window.btoa(binary);
}

async function getJson<T>(path: string, params?: Record<string, string>): Promise<T> {
  const search = params ? '?' + new URLSearchParams(params).toString() : '';
  const response = await fetch(`${BASE_URL}${path}${search}`);
  if (!response.ok) {
    let message = 'Error de servidor';
    try {
      const body = (await response.json()) as { error?: string };
      message = body.error || message;
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }
  return (await response.json()) as T;
}

async function postJson<T>(path: string, body: Record<string, string>): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    let message = 'Error de servidor';
    try {
      const payload = (await response.json()) as { error?: string };
      message = payload.error || message;
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }

  return (await response.json()) as T;
}

export const api = {
  health: () => getJson<{ ok: boolean; mensaje: string }>('/health'),
  login: (usuario: string, password: string) =>
    postJson<LoginResponse>('/login', { usuario, password }),
  register: (payload: {
    nombres: string;
    apellidos: string;
    cedula: string;
    correo: string;
    password: string;
  }) => postJson<RegisterResponse>('/registro', payload),
  logout: (token: string) => getJson<{ ok: boolean; mensaje: string }>('/logout', { token }),
  sendBatch: (token: string, usuario: string, cantidad: number) =>
    getJson<SendBatchResponse>('/enviarLote', {
      token,
      usuario,
      cantidad: String(cantidad),
    }),
  sendBatchWithFiles: async (token: string, usuario: string, files: File[], transformaciones: string[]) => {
    const archivos = await Promise.all(
      files.map(async (file) => ({
        nombre: file.name,
        contenidoBase64: await fileToBase64(file),
        transformaciones,
      }))
    );

    const response = await fetch(`${BASE_URL}/enviarLoteArchivos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, usuario, archivos }),
    });

    if (!response.ok) {
      let message = 'No se pudo enviar el lote con archivos reales';
      try {
        const body = (await response.json()) as { error?: string };
        message = body.error || message;
      } catch {
        // ignore parse errors
      }
      throw new Error(message);
    }

    return (await response.json()) as SendBatchResponse;
  },
  getEstado: (token: string, idLote: string) =>
    getJson<EstadoResponse>('/estado', { token, idLote }),
  getHistorial: (token: string) => getJson<HistorialResponse>('/historial', { token }),
  getDescargas: (token: string, idLote: string) => getJson<DescargasResponse>('/descargas', { token, idLote }),
  descargarImagen: async (token: string, idResultado: string): Promise<Blob> => {
    const search = new URLSearchParams({ token, idResultado }).toString();
    const response = await fetch(`${BASE_URL}/descargarImagen?${search}`);
    if (!response.ok) {
      throw new Error('No se pudo descargar la imagen transformada');
    }
    return response.blob();
  },
  descargarLoteZip: async (token: string, idLote: string): Promise<Blob> => {
    const search = new URLSearchParams({ token, idLote }).toString();
    const response = await fetch(`${BASE_URL}/descargarLoteZip?${search}`);
    if (!response.ok) {
      throw new Error('No se pudo descargar el lote en ZIP');
    }
    return response.blob();
  },
  getMetricas: (token: string) => getJson<MetricasResponse>('/metricas', { token }),
  getReplica: () => getJson<ReplicaResponse>('/replica'),
};
