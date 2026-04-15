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

export const api = {
  health: () => getJson<{ ok: boolean; mensaje: string }>('/health'),
  login: (usuario: string, password: string) =>
    getJson<LoginResponse>('/login', { usuario, password }),
  register: (payload: {
    nombres: string;
    apellidos: string;
    cedula: string;
    correo: string;
    password: string;
  }) => getJson<RegisterResponse>('/registro', payload),
  sendBatch: (token: string, usuario: string, cantidad: number) =>
    getJson<SendBatchResponse>('/enviarLote', {
      token,
      usuario,
      cantidad: String(cantidad),
    }),
  getEstado: (token: string, idLote: string) =>
    getJson<EstadoResponse>('/estado', { token, idLote }),
  getHistorial: (usuario: string) => getJson<HistorialResponse>('/historial', { usuario }),
  getDescargas: (idLote: string) => getJson<DescargasResponse>('/descargas', { idLote }),
  getMetricas: (usuario: string) => getJson<MetricasResponse>('/metricas', { usuario }),
  getReplica: () => getJson<ReplicaResponse>('/replica'),
};
