export type LoginResponse = {
  token: string;
  usuario: string;
  nombre?: string;
};

export type RegisterResponse = {
  ok: boolean;
  correo: string;
  mensaje: string;
};

export type SendBatchResponse = {
  idLote: string;
  cantidad: number;
};

export type EstadoResponse = {
  idLote: string;
  estado: string;
  total: number;
  procesadas: number;
  duracionMs: number;
  logs: string[];
  transformaciones: string[];
};

export type HistorialLote = {
  idLote: string;
  fecha: string;
  imagenes: number;
  estado: string;
  duracionMs: number;
  nodos: string;
};

export type HistorialResponse = {
  totalLotes: number;
  completados: number;
  enProgreso: number;
  totalImagenes: number;
  lotes: HistorialLote[];
};

export type DescargaArchivo = {
  nombre: string;
  tamKb: number;
  listo: boolean;
};

export type DescargasResponse = {
  idLote: string;
  estado: string;
  cantidad: number;
  archivos: DescargaArchivo[];
};

export type MetricasResponse = {
  usuario: string;
  lotesUsuario: number;
  imagenesUsuario: number;
  cpuNuclei: number;
  memoriaUsadaMb: number;
  nodo: {
    id: string;
    trabajosProcesados: number;
    imagenesProcesadas: number;
    tiempoPromedioMs: number;
    maxHilosParalelos: number;
    cargaActual: number;
  };
  replica: ReplicaResponse;
};

export type ReplicaResponse = {
  nodosPrimario: number;
  nodosReplica: number;
  trabajosPrimario: number;
  trabajosReplica: number;
  logsPrimario: number;
  logsReplica: number;
  totalReplicaciones: number;
  consistente: boolean;
  ultimoSync: string;
};
