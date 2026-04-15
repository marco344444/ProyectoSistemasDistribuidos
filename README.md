# Proyecto Sistemas Distribuidos

Aplicacion demo de procesamiento distribuido de imagenes con:

- Backend Java (RMI + SOAP + REST mock + servidor HTTP visual)
- Cliente web React + TypeScript (MVC)

## Requisitos

- Java JDK 17+ (con `java` y `javac` en PATH)
- Node.js 18+ y npm
- Windows PowerShell
- Docker Desktop (opcional, recomendado para PostgreSQL)

## Estructura

- `src/com/sistema`: backend Java
- `web-client`: cliente React + Vite
- `db`: infraestructura de base de datos PostgreSQL (docker + schema)
- `run-visual-demo.ps1`: compila y levanta backend visual en `http://localhost:8080`
- `run-all.ps1`: levanta DB + backend + frontend en un comando
- `stop-all.ps1`: detiene DB + backend + frontend

## Arranque y apagado rapido (todo en uno)

Desde la raiz del repo:

```powershell
.\run-all.ps1
```

Para detener todo:

```powershell
.\stop-all.ps1
```

Si quieres detener y ademas borrar volumenes de PostgreSQL:

```powershell
.\stop-all.ps1 -RemoveVolumes
```

## Base de datos relacional (PostgreSQL)

La base de datos se define siguiendo la infraestructura de capas (autenticacion + aplicacion + BD relacional).

### 1. Levantar PostgreSQL

Desde la raiz del repo:

```powershell
.\run-db.ps1
```

Credenciales por defecto:

- Host: `localhost`
- Puerto: `5433`
- DB: `imageproc`
- User: `imageproc`
- Password: `imageproc123`

El esquema inicial se crea desde `db/init/001_schema.sql` con tablas:

- `auth.usuarios`
- `auth.sesiones`
- `procesamiento.nodos`
- `procesamiento.trabajos`
- `procesamiento.logs`

`/api/login` y `/api/registro` ya usan PostgreSQL exclusivamente.
El archivo `data/usuarios.json` queda solo como referencia historica y ya no se escribe desde el backend.

### 2. Habilitar JDBC en backend

1. Descarga el driver PostgreSQL JDBC (`postgresql-*.jar`)
2. Colocalo en la carpeta `lib/`
3. Exporta variables antes de ejecutar backend:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/imageproc"
$env:DB_USER = "imageproc"
$env:DB_PASSWORD = "imageproc123"
```

Con esas variables, el backend usa repositorio JDBC. Si no estan definidas, sigue usando repositorio en memoria.

## Como correr el proyecto

### 1. Levantar backend

Desde la raiz del repo (`ProyectoSistemasDistribuidos`):

```powershell
.\run-visual-demo.ps1
```

El backend queda en:

- Health: `http://localhost:8080/api/health`
- API base: `http://localhost:8080/api`

### 2. Levantar frontend React

En otra terminal:

```powershell
Set-Location .\web-client
npm install
npm run dev
```

Frontend en:

- `http://localhost:5173`

## Flujo de uso

1. Iniciar sesion en el frontend.
2. Ir a "Nueva solicitud".
3. Cargar imagenes y seleccionar transformaciones del bloque.
4. Enviar lote.
5. Revisar estado en tiempo real (polling 1s), metricas automáticas y logs de ejecucion.
6. Descargar resultados desde "Descargas".

## Build frontend

```powershell
Set-Location .\web-client
npm run build
```

## Notas

- El backend usa almacenamiento en memoria para la demo.
- Existe CORS habilitado para consumo desde el frontend local.
- Si el puerto `8080` esta ocupado, detén el proceso y vuelve a ejecutar el script.
