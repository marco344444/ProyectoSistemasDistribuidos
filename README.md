# Proyecto Sistemas Distribuidos

Aplicacion demo de procesamiento distribuido de imagenes con:

- Backend Java (RMI + SOAP + REST mock + servidor HTTP visual)
- Cliente web React + TypeScript (MVC)

## Requisitos

- Java JDK 17+ (con `java` y `javac` en PATH)
- Node.js 18+ y npm
- Windows PowerShell

## Estructura

- `src/com/sistema`: backend Java
- `web-client`: cliente React + Vite
- `run-visual-demo.ps1`: compila y levanta backend visual en `http://localhost:8080`

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
