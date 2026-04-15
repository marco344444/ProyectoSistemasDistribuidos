CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS procesamiento;

CREATE TABLE IF NOT EXISTS auth.usuarios (
    id_usuario      VARCHAR(120) PRIMARY KEY,
    nombres         VARCHAR(120) NOT NULL,
    apellidos       VARCHAR(120) NOT NULL,
    cedula          VARCHAR(40) UNIQUE NOT NULL,
    correo          VARCHAR(120) UNIQUE NOT NULL,
    password        VARCHAR(160) NOT NULL,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS auth.sesiones (
    token_sesion    VARCHAR(120) PRIMARY KEY,
    id_usuario      VARCHAR(120) NOT NULL REFERENCES auth.usuarios(id_usuario),
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    expira_en       TIMESTAMP,
    activa          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS procesamiento.nodos (
    id_nodo         VARCHAR(80) PRIMARY KEY,
    host            VARCHAR(120) NOT NULL,
    puerto          INTEGER NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS procesamiento.trabajos (
    id_trabajo      VARCHAR(120) PRIMARY KEY,
    id_usuario      VARCHAR(120) NOT NULL,
    estado          VARCHAR(40) NOT NULL,
    fecha_creacion  VARCHAR(60) NOT NULL,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS procesamiento.logs (
    id              BIGSERIAL PRIMARY KEY,
    id_log          VARCHAR(120),
    nivel           VARCHAR(20) NOT NULL,
    mensaje         TEXT NOT NULL,
    fecha           VARCHAR(60) NOT NULL,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trabajos_usuario ON procesamiento.trabajos(id_usuario);
CREATE INDEX IF NOT EXISTS idx_trabajos_estado ON procesamiento.trabajos(estado);
CREATE INDEX IF NOT EXISTS idx_logs_fecha ON procesamiento.logs(fecha);

INSERT INTO auth.usuarios (id_usuario, nombres, apellidos, cedula, correo, password)
VALUES
    ('admin@imageproc.com', 'Admin', 'Sistema', '1000000001', 'admin@imageproc.com', 'admin123'),
    ('user@imageproc.com', 'Usuario', 'Demo', '1000000002', 'user@imageproc.com', 'user123')
ON CONFLICT (id_usuario) DO NOTHING;
