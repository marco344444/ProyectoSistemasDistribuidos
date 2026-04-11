# Entrega 2 - Validacion Tecnica

Este documento valida los dos criterios solicitados:

1. Cliente web envia solicitudes y recibe respuestas de prueba.
2. Servicios/protocolos implementados responden solicitudes de prueba.

## 1) Clientes

- Cliente web: [UI/01_login.html](UI/01_login.html) -> [UI/02_upload.html](UI/02_upload.html) -> [UI/03_status.html](UI/03_status.html)

El cliente web consume endpoints del backend visual:

- GET /api/login
- GET /api/enviarLote
- GET /api/estado

## 2) Servicios y protocolos

Se validan en el smoke test:

- SOAP: ServicioProcesamientoImagenesImpl
- RMI: INodoTrabajador / NodoTrabajadorImpl
- REST simulado: IRepositorioDatos / RepositorioDatosImpl

## Ejecucion de validacion automatica

Desde la raiz del proyecto:

```powershell
.\run-entrega2.ps1
```

Resultado esperado:

- 3 checks en PASS
- Mensaje final: CUMPLE con Entrega 2.

## Ejecucion visual (demo)

1. Levanta backend visual:

```powershell
.\run-visual-demo.ps1
```

2. Abre las interfaces:

- [UI/01_login.html](UI/01_login.html)

3. Realiza solicitudes desde web y verifica respuesta en pantalla y consola.
