# Guia Rapida de Ejecucion

Este documento resume la ejecucion del flujo actual (frontend React + backend visual).

## 1) Levantar backend visual

Desde la raiz del repositorio:

```powershell
.\run-visual-demo.ps1
```

Endpoints principales:

- `http://localhost:8080/api/health`
- `http://localhost:8080/api`

## 2) Levantar frontend

En otra terminal:

```powershell
Set-Location .\web-client
npm install
npm run dev
```

Frontend:

- `http://localhost:5173`

## 3) Flujo funcional

1. Iniciar sesion.
2. Crear lote cargando imagenes.
3. Seleccionar transformaciones del bloque.
4. Enviar lote.
5. Revisar estado en tiempo real, logs y duracion.
6. Descargar imagenes transformadas.

## Nota

Para detalle completo, usar `README.md` de la raiz.
