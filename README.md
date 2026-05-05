# Proyecto Sistemas Distribuidos

Aplicacion demo de procesamiento distribuido de imagenes con:

- Backend Java (RMI + SOAP + REST mock + servidor HTTP visual)
- Cliente web React + TypeScript (MVC)
- Base de datos PostgreSQL
- Deployment en máquinas virtuales distribuidas

## 🚀 Arranque Rápido

### 1. Validar Entorno

Primero, valida que tengas todas las dependencias instaladas:

```powershell
.\setup-env.ps1
```

Este script verifica:
- ✅ Java JDK 17+
- ✅ Node.js 18+ y npm
- ✅ Docker (opcional)
- ✅ Directorios necesarios
- ✅ Crea archivos de configuración

### 2. Iniciar Sistema

Desde la raíz del repositorio, usa el script unificado:

```powershell
# Inicia BD + Backend + Frontend
.\start.ps1

# O especifica qué iniciar
.\start.ps1 -Mode start -Service backend   # Solo Backend
.\start.ps1 -Mode start -Service db        # Solo BD
.\start.ps1 -Mode start -Service frontend  # Solo Frontend
```

### 3. Verificar Estado

```powershell
.\start.ps1 -Mode status     # Ver estado de servicios
.\start.ps1 -Mode check      # Validar salud del sistema
```

### 4. Detener Sistema

```powershell
.\start.ps1 -Mode stop

# Incluir -RemoveVolumes para borrar datos de BD
.\start.ps1 -Mode stop -RemoveVolumes
```

---

## 📋 Requisitos

- **Java JDK 17+** (con `java` y `javac` en PATH)
- **Node.js 18+** y npm
- **Docker Desktop** (opcional pero recomendado para PostgreSQL)
- **Windows PowerShell 5+** o PowerShell Core

### Instalación de Dependencias

**Java:**
- Descarga desde: https://www.oracle.com/java/technologies/downloads/

**Node.js:**
- Descarga desde: https://nodejs.org/ (versión LTS)

**Docker:**
- Descarga desde: https://www.docker.com/products/docker-desktop

---

## 📊 Estructura del Proyecto

```
ProyectoSistemasDistribuidos/
├── src/com/sistema/              # Backend Java
│   ├── main/                      # Entry points
│   ├── rmi/                       # RMI para nodos distribuidos
│   ├── rest/                      # API REST
│   ├── soap/                      # SOAP Service
│   ├── model/                     # Modelos de datos
│   └── ...
├── web-client/                    # Frontend React + TypeScript
│   ├── src/
│   │   ├── components/            # Componentes UI
│   │   ├── views/                 # Vistas/Páginas
│   │   ├── controllers/           # Lógica (MVC)
│   │   ├── services/              # Servicios de API
│   │   └── models/                # Tipos y interfaces
│   └── package.json
├── db/                            # Base de datos
│   ├── docker-compose.yml         # PostgreSQL en Docker
│   └── init/001_schema.sql        # Esquema de BD
├── lib/                           # Librerías externas
├── start.ps1                      # ⭐ Script principal (nuevo)
├── setup-env.ps1                  # Validación del entorno
├── README.md                      # Este archivo
├── DEPLOYMENT_VMS.md              # Guía de deployment en VMs
├── AUDITORIA.md                   # Análisis del sistema
└── ...
```

---

## 🎯 Flujo de Uso

1. **Login**: Inicia sesión en http://localhost:5173
2. **Nueva Solicitud**: Crea un nuevo lote de procesamiento
3. **Cargar Imágenes**: Selecciona imágenes y transformaciones
4. **Enviar**: Procesa el lote
5. **Monitoreo**: Observa progreso en tiempo real
6. **Resultados**: Descarga imágenes transformadas

---

## 🔌 Endpoints Principales

| Endpoint | Descripción |
|----------|-------------|
| `GET /api/health` | Estado del backend |
| `POST /api/login` | Autenticación |
| `POST /api/registro` | Registro de usuario |
| `POST /api/enviarLoteArchivos` | Enviar lote de imágenes |
| `GET /api/estado/{idLote}` | Estado del procesamiento |
| `GET /api/descargar/{idResultado}` | Descargar resultado |

---

## 🗄️ Base de Datos

PostgreSQL se inicia automáticamente con docker-compose.

**Credenciales por defecto:**
```
Host: localhost
Puerto: 5433
BD: imageproc
Usuario: imageproc
Contraseña: imageproc123
```

**Tablas principales:**
- `auth.usuarios` - Usuarios del sistema
- `auth.sesiones` - Sesiones activas
- `procesamiento.trabajos` - Trabajos de procesamiento
- `procesamiento.logs` - Logs de ejecución

---

## 📈 Build Frontend

Para compilar el frontend para producción:

```powershell
cd web-client
npm run build
```

Los archivos compilados quedarán en `web-client/dist/`

---

## 🐛 Troubleshooting

### "Puerto X ya está en uso"

```powershell
# Detener servicios anteriores
.\start.ps1 -Mode stop

# O matar procesos específicos
# Backend: taskkill /F /IM java.exe
# Frontend: taskkill /F /IM node.exe
```

### "Java no encontrado"

Asegúrate de que Java esté en PATH:
```powershell
java -version
javac -version

# Si no funciona, agrega Java al PATH manualmente
$env:PATH += ";C:\Program Files\Java\jdk-17\bin"
```

### "npm: command not found"

Reinstala Node.js y npm desde https://nodejs.org/

### "Docker no disponible"

El sistema funcionará sin Docker pero usará BD en memoria. Para persistencia, instala Docker Desktop.

### "BD no se conecta"

```powershell
# Verificar que PostgreSQL está corriendo
docker ps

# Ver logs del contenedor
docker logs imageproc-postgres

# Reconectar
.\start.ps1 -Mode restart -Service db
```

---

## 📚 Documentación Adicional

| Documento | Propósito |
|-----------|-----------|
| [DEPLOYMENT_VMS.md](DEPLOYMENT_VMS.md) | Deployment en VMs distribuidas con VMware Workstation |
| [AUDITORIA.md](AUDITORIA.md) | Análisis del sistema, gaps y recomendaciones |
| [ENTREGA_2.md](ENTREGA_2.md) | Especificación de la entrega 2 |
| [PROCESO.md](PROCESO.md) | Proceso de desarrollo |

---

## 🎮 Scripts Disponibles

### Script Principal (NUEVO - Usar este)
```powershell
.\start.ps1                              # Inicia todo
.\start.ps1 -Mode stop                   # Detiene todo
.\start.ps1 -Mode restart                # Reinicia todo
.\start.ps1 -Mode status                 # Ver estado
.\start.ps1 -Mode check                  # Validar salud
```

### Setup y Validación
```powershell
.\setup-env.ps1                          # Validar entorno
.\setup-env.ps1 -Force                   # Regenerar .env
```

### Scripts Legados (Deprecated)
```powershell
# Estos scripts ya no son necesarios - usar start.ps1
# .\run-all.ps1
# .\run-visual-demo.ps1
# .\run-db.ps1
# .\stop-all.ps1
```

---

## ⚙️ Configuración Avanzada

### Variables de Entorno

Edita `.env.local` para personalizar:

```
DB_HOST=localhost
DB_PORT=5433
BACKEND_PORT=8080
FRONTEND_PORT=5173
```

### Perfiles

El sistema soporta diferentes perfiles:

```powershell
.\start.ps1 -Profile dev       # Desarrollo (logs verbosos)
.\start.ps1 -Profile prod      # Producción (logs normales)
.\start.ps1 -Profile test      # Testing (sin UI)
```

---

## 🚀 Deployment en VMs

Para desplegar en máquinas virtuales distribuidas, consulta [DEPLOYMENT_VMS.md](DEPLOYMENT_VMS.md)

Incluye:
- Setup de VMware Workstation
- Configuración de 3 VMs (DB, Backend, Frontend)
- Network setup
- Health checks

---

## 📊 Arquitectura

```
┌─────────────────────────────────────────────────┐
│              Frontend React (5173)              │
│   - Login, Upload, Monitoreo, Descargas        │
└────────────┬────────────────────────────────────┘
             │ HTTP/REST
             ▼
┌─────────────────────────────────────────────────┐
│         Backend Java (8080)                     │
│   - REST API, RMI Registry, SOAP Service       │
└────────────┬────────────────────────────────────┘
             │ JDBC
             ▼
┌─────────────────────────────────────────────────┐
│    PostgreSQL (5433) en Docker                 │
│   - Usuarios, Sesiones, Trabajos, Logs        │
└─────────────────────────────────────────────────┘
```

---

## 🤝 Contribuir

1. Haz cambios en una rama nueva
2. Ejecuta `.\start.ps1 -Mode check` para validar
3. Abre un Pull Request
4. Espera revisión

---

## 📝 Licencia

Proyecto académico - Universidad

---

## 📞 Soporte

- Documentación: Ver archivos .md en raíz
- Issues: Abrir en GitHub
- Auditoría: Consultar AUDITORIA.md

---

**Última actualización:** Mayo 5, 2026  
**Versión:** 2.0 (Scripts Unificados)
