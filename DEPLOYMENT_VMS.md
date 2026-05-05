# Deployment en Máquinas Virtuales Distribuidas

Guía completa para desplegar el sistema de procesamiento distribuido de imágenes en múltiples máquinas virtuales.

---

## 📊 Arquitectura de Deployment

```
┌─────────────────────────────────────────────────────────────┐
│                   INTERNET / RED LOCAL                      │
└────────────────┬──────────────┬──────────────┬──────────────┘
                 │              │              │
         ┌───────▼────┐  ┌──────▼────┐  ┌─────▼──────┐
         │   VM-DB    │  │ VM-BACKEND │  │ VM-FRONTEND│
         │ PostgreSQL │  │ Java RMI   │  │   Nginx    │
         │  :5433     │  │   :8080    │  │    :80     │
         │  :5432     │  │   :1099    │  │   :443     │
         └────────────┘  └────────────┘  └────────────┘
             10.0.0.20      10.0.0.10       10.0.0.30
```

---

## 🔧 Requisitos Previos

### Hardware Recomendado
- **VM-DB**: 2 CPU, 4GB RAM, 20GB almacenamiento
- **VM-Backend**: 2 CPU, 2GB RAM, 10GB almacenamiento
- **VM-Frontend**: 1 CPU, 1GB RAM, 5GB almacenamiento

### Software Base (todas las VMs)
- Sistema Operativo: **Ubuntu 22.04 LTS** o **Debian 12**
- Acceso SSH habilitado
- Usuario con privilegios `sudo`

---

## �️ Paso 0: Configuración de VMware Workstation

### 0.1 Crear Red Virtual Distribuida

1. **Abrir VMware Workstation**
2. Ir a: **Edit → Virtual Network Editor**
3. Crear nueva red (si no existe):
   - Click en **Add Network**
   - Seleccionar **VMnet5** (o número disponible)
   - Configurar:
     ```
     Network Mode: Custom (you selected this network)
     Use local DHCP service to distribute IP addresses: ✅ Checked
     Subnet IP: 10.0.0.0
     Subnet Mask: 255.255.255.0
     ```
   - Click **OK**

### 0.2 Descargar ISO de Ubuntu

1. Descargar **Ubuntu 22.04 LTS** desde:
   - https://ubuntu.com/download/desktop
   - O desde repositorio de la universidad

2. Guardar en carpeta accesible (ej: `C:\ISOs\ubuntu-22.04-desktop-amd64.iso`)

### 0.3 Crear Primera VM (VM-DB)

1. **New Virtual Machine** → **Custom**
2. Configurar:
   ```
   VM Name: VM-DB
   Location: C:\VMs\VM-DB
   Compatibility: Workstation 17.x (o versión actual)
   Guest OS: Linux → Ubuntu 22.04 64-bit
   CPUs: 2
   RAM: 4096 MB
   Hard Disk: 20 GB (Single file)
   Network Adapter: Custom (VMnet5)
   ```
3. Click **Finish**

### 0.4 Instalación de Ubuntu en VM-DB

1. **Power on VM-DB**
2. Seleccionar ISO de Ubuntu cuando pida
3. Instalar Ubuntu normalmente:
   - Username: `appuser`
   - Hostname: `vm-db`
   - Enable SSH server (durante instalación)
4. Al terminar:
   ```bash
   # Configurar IP estática
   sudo nano /etc/netplan/00-installer-config.yaml
   ```
   Agregar:
   ```yaml
   network:
     version: 2
     ethernets:
       ens33:
         dhcp4: no
         addresses:
           - 10.0.0.20/24
         gateway4: 10.0.0.1
         nameservers:
           addresses: [8.8.8.8, 8.8.4.4]
   ```
   ```bash
   sudo netplan apply
   ```

### 0.5 Crear VM-Backend

Repetir pasos 0.3-0.4 pero:
```
VM Name: VM-Backend
Hostname: vm-backend
CPUs: 2
RAM: 2048 MB
Hard Disk: 10 GB
IP estática: 10.0.0.10
```

### 0.6 Crear VM-Frontend

Repetir pasos 0.3-0.4 pero:
```
VM Name: VM-Frontend
Hostname: vm-frontend
CPUs: 1
RAM: 1024 MB
Hard Disk: 5 GB
IP estática: 10.0.0.30
```

### 0.7 Habilitar SSH en Workstation Host

Para conectarte desde Windows a las VMs:

1. **En cada VM**, asegurar SSH está corriendo:
   ```bash
   sudo systemctl status ssh
   sudo systemctl enable ssh
   ```

2. **Desde Windows PowerShell**, probar conexión:
   ```powershell
   ssh appuser@10.0.0.20
   ssh appuser@10.0.0.10
   ssh appuser@10.0.0.30
   ```

### 0.8 Crear Snapshots Iniciales

Antes de empezar la instalación de servicios, guardar estado limpio:

1. **Para cada VM**:
   - Click derecho en VM → **Snapshots → Take Snapshot**
   - Nombre: `clean-ubuntu-setup`
   - Descripción: "Ubuntu 22.04 instalado, SSH habilitado, IPs configuradas"
   - Click **Take Snapshot**

2. Para restaurar en cualquier momento:
   - Click derecho en VM → **Snapshots → Revert to Snapshot → clean-ubuntu-setup**

---

## �📋 Paso 1: Configuración Inicial de cada VM

### En todas las VMs

```bash
# Conectarse por SSH
ssh user@<IP_VM>

# Actualizar paquetes
sudo apt update && sudo apt upgrade -y

# Instalar herramientas base
sudo apt install -y git curl wget net-tools htop
```

### Crear usuario de aplicación
```bash
# Crear usuario dedicado (en todas las VMs)
sudo useradd -m -s /bin/bash appuser
sudo usermod -aG sudo appuser
sudo usermod -aG docker appuser  # Para que pueda usar Docker

# Cambiar a usuario appuser
su - appuser
```

---

## 🗄️ Paso 2: Setup de VM-DB (PostgreSQL)

### 2.1 Instalar Docker y Docker Compose

```bash
# En VM-DB con usuario root o sudo
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Agregar usuario appuser al grupo docker
sudo usermod -aG docker appuser
```

### 2.2 Preparar Repositorio

```bash
cd /home/appuser
git clone https://github.com/marco344444/ProyectoSistemasDistribuidos.git
cd ProyectoSistemasDistribuidos
```

### 2.3 Configurar PostgreSQL

**Editar `db/docker-compose.yml` para exponer en red:**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16
    container_name: imageproc-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: imageproc
      POSTGRES_USER: imageproc
      POSTGRES_PASSWORD: imageproc123
    ports:
      - "0.0.0.0:5433:5432"  # ← CAMBIO: Escuchar en todas las interfaces
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U imageproc -d imageproc"]
      interval: 10s
      timeout: 5s
      retries: 8
    networks:
      - distributed-net

volumes:
  postgres_data:

networks:
  distributed-net:
    driver: bridge
```

### 2.4 Iniciar PostgreSQL

```bash
cd db/
docker-compose up -d

# Verificar que está corriendo
docker ps
docker logs imageproc-postgres

# Probar conexión (desde otro host)
psql -h 10.0.0.20 -p 5433 -U imageproc -d imageproc
```

### 2.5 Configurar Firewall (si aplica)

```bash
# Permitir puerto PostgreSQL desde red interna
sudo ufw allow from 10.0.0.0/24 to any port 5433
sudo ufw reload
```

---

## 🖥️ Paso 3: Setup de VM-Backend (Java RMI + REST)

### 3.1 Instalar JDK 17

```bash
sudo apt install -y openjdk-17-jdk openjdk-17-jre

# Verificar instalación
java -version
javac -version
```

### 3.2 Descargar Driver PostgreSQL

```bash
cd /home/appuser/ProyectoSistemasDistribuidos/lib

# Descargar PostgreSQL JDBC driver
wget https://jdbc.postgresql.org/download/postgresql-42.7.1.jar

# Verificar descarga
ls -lh *.jar
```

### 3.3 Crear Archivo de Configuración

**Crear `/home/appuser/ProyectoSistemasDistribuidos/.env.backend`:**

```properties
# Database Configuration
DB_HOST=10.0.0.20
DB_PORT=5433
DB_NAME=imageproc
DB_USER=imageproc
DB_PASSWORD=imageproc123

# RMI Configuration
RMI_HOST=10.0.0.10
RMI_PORT=1099

# Server Configuration
SERVER_PORT=8080
SERVER_HOST=0.0.0.0
```

### 3.4 Actualizar Código Java para Leer Configuración

**Editar o crear `src/com/sistema/config/ConfigLoader.java`:**

```java
package com.sistema.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties;
    
    public static Properties loadConfig() {
        properties = new Properties();
        try {
            FileInputStream fis = new FileInputStream(".env.backend");
            properties.load(fis);
            fis.close();
        } catch (IOException e) {
            System.out.println("⚠️ Archivo .env.backend no encontrado, usando defaults");
            properties.setProperty("DB_HOST", System.getenv().getOrDefault("DB_HOST", "localhost"));
            properties.setProperty("DB_PORT", System.getenv().getOrDefault("DB_PORT", "5433"));
            properties.setProperty("DB_NAME", System.getenv().getOrDefault("DB_NAME", "imageproc"));
            properties.setProperty("DB_USER", System.getenv().getOrDefault("DB_USER", "imageproc"));
            properties.setProperty("DB_PASSWORD", System.getenv().getOrDefault("DB_PASSWORD", "imageproc123"));
            properties.setProperty("SERVER_PORT", System.getenv().getOrDefault("SERVER_PORT", "8080"));
            properties.setProperty("SERVER_HOST", System.getenv().getOrDefault("SERVER_HOST", "0.0.0.0"));
        }
        return properties;
    }
    
    public static String getProperty(String key) {
        if (properties == null) loadConfig();
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        if (properties == null) loadConfig();
        return properties.getProperty(key, defaultValue);
    }
}
```

### 3.5 Compilar Backend

```bash
cd /home/appuser/ProyectoSistemasDistribuidos

# Crear directorio de build
mkdir -p build

# Compilar (incluir PostgreSQL JDBC)
javac -d build -cp "lib/*:." src/com/sistema/**/*.java src/javax/**/*.java

# Verificar compilación
ls -la build/
```

### 3.6 Crear Script de Inicio

**Crear `/home/appuser/ProyectoSistemasDistribuidos/start-backend.sh`:**

```bash
#!/bin/bash

# Cargar configuración
if [ -f .env.backend ]; then
    export $(cat .env.backend | grep -v '^#' | xargs)
fi

echo "=== Iniciando Backend ==="
echo "DB Host: $DB_HOST"
echo "RMI Host: $RMI_HOST"
echo "Server Port: $SERVER_PORT"

# Ejecutar backend
java -cp "build:lib/*" \
    -Ddb.host=$DB_HOST \
    -Ddb.port=$DB_PORT \
    -Ddb.name=$DB_NAME \
    -Ddb.user=$DB_USER \
    -Ddb.password=$DB_PASSWORD \
    -Drmi.host=$RMI_HOST \
    -Drmi.port=$RMI_PORT \
    -Dserver.port=$SERVER_PORT \
    -Dserver.host=$SERVER_HOST \
    com.sistema.main.VisualDemoServerMain
```

### 3.7 Dar Permisos de Ejecución

```bash
chmod +x start-backend.sh

# Ejecutar
./start-backend.sh
```

### 3.8 Crear Servicio Systemd (Producción)

**Crear `/etc/systemd/system/backend-imageproc.service`:**

```ini
[Unit]
Description=Servicio Backend Sistema Distribuido
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/home/appuser/ProyectoSistemasDistribuidos
Environment="PATH=/usr/bin:/usr/local/bin"
ExecStart=/home/appuser/ProyectoSistemasDistribuidos/start-backend.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Habilitar servicio:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable backend-imageproc.service
sudo systemctl start backend-imageproc.service
sudo systemctl status backend-imageproc.service

# Ver logs
sudo journalctl -u backend-imageproc.service -f
```

### 3.9 Configurar Firewall

```bash
sudo ufw allow from 10.0.0.0/24 to any port 8080
sudo ufw allow from 10.0.0.0/24 to any port 1099
sudo ufw reload
```

---

## 🌐 Paso 4: Setup de VM-Frontend (React + Nginx)

### 4.1 Instalar Node.js y Npm

```bash
curl -sL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Verificar
node --version
npm --version
```

### 4.2 Instalar Nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 4.3 Preparar Repositorio

```bash
cd /home/appuser
git clone https://github.com/marco344444/ProyectoSistemasDistribuidos.git
cd ProyectoSistemasDistribuidos/web-client
```

### 4.4 Crear Archivo .env para Frontend

**Crear `web-client/.env.production`:**

```env
VITE_API_URL=http://10.0.0.10:8080/api
VITE_APP_NAME=Sistema Distribuido
```

### 4.5 Build del Frontend

```bash
cd /home/appuser/ProyectoSistemasDistribuidos/web-client

# Instalar dependencias
npm install

# Build de producción
npm run build

# El resultado estará en dist/
ls -la dist/
```

### 4.6 Configurar Nginx como Servidor Web

**Crear `/etc/nginx/sites-available/imageproc`:**

```nginx
upstream backend {
    server 10.0.0.10:8080;
}

server {
    listen 80;
    listen [::]:80;
    server_name _;

    root /var/www/imageproc;
    index index.html;

    # Servir archivos estáticos
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy a Backend
    location /api {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Comprimir respuestas
    gzip on;
    gzip_types text/plain text/css text/javascript application/javascript application/json;
}
```

### 4.7 Habilitar Configuración

```bash
# Crear directorio para la aplicación
sudo mkdir -p /var/www/imageproc

# Copiar archivos compilados
sudo cp -r dist/* /var/www/imageproc/

# Cambiar propietario
sudo chown -R www-data:www-data /var/www/imageproc

# Habilitar sitio
sudo ln -s /etc/nginx/sites-available/imageproc /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Verificar configuración de Nginx
sudo nginx -t

# Reiniciar Nginx
sudo systemctl restart nginx
```

### 4.8 Configurar Firewall

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw reload
```

---

## 🔗 Paso 5: Configuración de Red

### 5.1 Resolver Nombres de Host (Opcional pero Recomendado)

**En cada VM, editar `/etc/hosts`:**

```bash
sudo nano /etc/hosts
```

**Agregar:**

```
10.0.0.20   vm-db
10.0.0.10   vm-backend
10.0.0.30   vm-frontend
```

### 5.2 Verificar Conectividad

```bash
# Desde VM-Frontend
ping vm-backend
curl http://vm-backend:8080/api/health

# Desde VM-Backend
psql -h vm-db -p 5433 -U imageproc -d imageproc -c "SELECT version();"

# Desde VM-DB
nc -zv vm-backend 8080
```

---

## ✅ Paso 6: Verificación de Deployment

### 6.1 Checklist de Base de Datos

```bash
# SSH a VM-DB
ssh user@10.0.0.20

# Verificar que PostgreSQL está corriendo
docker ps | grep postgres

# Verificar tablas
psql -h localhost -p 5433 -U imageproc -d imageproc -c "\dt"

# Resultado esperado:
# schema_name │          table_name          │
# ─────────────┼──────────────────────────────┤
# auth        │ usuarios                     │
# auth        │ sesiones                     │
# procesamiento│ nodos                       │
# procesamiento│ trabajos                    │
# procesamiento│ logs                        │
```

### 6.2 Checklist de Backend

```bash
# SSH a VM-Backend
ssh user@10.0.0.10

# Ver estado del servicio
sudo systemctl status backend-imageproc.service

# Probar endpoint de salud
curl http://localhost:8080/api/health

# Respuesta esperada:
# {"status":"OK","timestamp":"..."}

# Ver logs
sudo journalctl -u backend-imageproc.service -n 20
```

### 6.3 Checklist de Frontend

```bash
# SSH a VM-Frontend
ssh user@10.0.0.30

# Verificar que Nginx está activo
sudo systemctl status nginx

# Verificar que archivos estáticos existen
ls -la /var/www/imageproc/

# Acceder desde navegador
# http://10.0.0.30
# o desde máquina local: http://vm-frontend
```

### 6.4 Test de Flujo Completo

1. **Desde navegador**: `http://10.0.0.30`
2. **Registrarse**: Usuario nuevo
3. **Cargar imagen**: Verificar que sube
4. **Enviar lote**: Ver que se procesa
5. **Descargar resultado**: Verificar que funciona

---

## 🐛 Troubleshooting

### Problema: VMs no pueden comunicarse entre sí

```bash
# Verificar en Workstation que todas las VMs están en VMnet5
# (Ver configuración de cada VM: VM → Settings → Network Adapter)

# En cada VM, verificar IP y red
ip addr show

# Probar ping entre VMs
ping 10.0.0.20  # Desde otra VM
ping 10.0.0.10
ping 10.0.0.30

# Si no funciona, reiniciar adaptador de red
sudo systemctl restart networking
```

### Problema: No tengo conexión a Internet desde VMs

```bash
# Verificar gateway en Workstation
# Edit → Virtual Network Editor → VMnet5 → NAT Settings
# Gateway debe estar configurado (ej: 10.0.0.1)

# En cada VM, verificar /etc/netplan/
sudo cat /etc/netplan/00-installer-config.yaml

# Agregar DNS si falta
sudo nano /etc/netplan/00-installer-config.yaml
```

Agregar línea:
```yaml
nameservers:
  addresses: [8.8.8.8, 8.8.4.4]
```

```bash
sudo netplan apply
ping 8.8.8.8
```

### Problema: Rendimiento lento en VMs

```bash
# En Workstation:
# 1. Aumentar CPUs y RAM a cada VM
#    Edit → Memory: aumentar a 4GB mínimo
#    Edit → Processors: aumentar a 2 CPUs

# 2. Desactivar 3D Graphics si no se necesita
#    VM → Settings → Display → deseleccionar "Accelerate 3D graphics"

# 3. Usar discos rápidos SSD en el host
```

### Problema: No puedo SSH desde Windows a las VMs

```powershell
# En PowerShell, verificar que la VM está corriendo
# Probar conexión directa
ping 10.0.0.20

# Si funciona ping pero no SSH:
# 1. Verificar SSH está habilitado en VM
ssh appuser@10.0.0.20

# 2. Si pide contraseña, ingresar la contraseña de Ubuntu

# 3. Para evitar ingresar contraseña cada vez, agregar clave SSH
ssh-keygen -t rsa -N ""  # En Windows
# Copiar ~/.ssh/id_rsa.pub a VM en ~/.ssh/authorized_keys
```

### Problema: Necesito rollback rápido a estado limpio

```
1. Abrir VMware Workstation
2. Click derecho en VM
3. Snapshots → Revert to Snapshot → clean-ubuntu-setup
4. VM volverá al estado guardado en segundos
5. Poder on VM y continuar
```

### Problema: VM no inicia después de cambios

```
1. Revertir a snapshot limpio:
   Snapshots → Revert to Snapshot → clean-ubuntu-setup

2. O restaurar manual desde backup:
   - En host, copiar archivo de backup de VM
   - Reemplazar archivo de disco de VM
   - Power on
```

### Problema: Backend crashea en VM

```bash
# SSH a VM-Backend
ssh appuser@10.0.0.10

# Ver logs en tiempo real
sudo journalctl -u backend-imageproc.service -f

# O ejecutar backend manualmente para ver errores
cd ProyectoSistemasDistribuidos
./start-backend.sh 2>&1 | head -50

# Común: PostgreSQL no está accesible
# Verificar conexión a VM-DB
psql -h 10.0.0.20 -p 5433 -U imageproc -d imageproc -c "SELECT 1"
```

### Problema: Frontend muestra error de conexión a Backend

```bash
# SSH a VM-Frontend
ssh appuser@10.0.0.30

# Ver logs de Nginx
sudo tail -f /var/log/nginx/error.log

# Probar que puede conectar a Backend
curl http://10.0.0.10:8080/api/health

# Verificar configuración de Nginx
sudo nano /etc/nginx/sites-available/imageproc
# Verificar que "upstream backend" apunta a 10.0.0.10:8080

sudo systemctl restart nginx
```

### Problema: Base de datos no se inicializa correctamente

```bash
# SSH a VM-DB
ssh appuser@10.0.0.20

# Ver logs del contenedor PostgreSQL
docker logs imageproc-postgres -f

# Conectarse a PostgreSQL manualmente
psql -h localhost -p 5433 -U imageproc -d imageproc

# Ver tablas (debería mostrar auth.usuarios, auth.sesiones, etc)
\dt

# Si no hay tablas, ejecutar script SQL manualmente
psql -h localhost -p 5433 -U imageproc -d imageproc < init/001_schema.sql
```

### Problema: Necesito acceder a archivos de VM desde Windows

**Opción 1: SMB (Samba)**
```bash
# En VM, instalar Samba
sudo apt install -y samba

# Compartir carpeta en /etc/samba/smb.conf
sudo nano /etc/samba/smb.conf

# Agregar al final:
[shared]
  path = /home/appuser/ProyectoSistemasDistribuidos
  browseable = yes
  writable = yes
  guest ok = yes

sudo systemctl restart samba
```

Luego desde Windows:
```powershell
# Abrir File Explorer
# Escribir: \\10.0.0.10\shared
```

**Opción 2: SCP (desde PowerShell)**
```powershell
# Descargar archivo de VM
scp appuser@10.0.0.10:/home/appuser/file.txt C:\Users\prada\Desktop\

# Subir archivo a VM
scp C:\Users\prada\Desktop\file.txt appuser@10.0.0.10:/home/appuser/
```

### Problema: Backend no puede conectar a PostgreSQL

```bash
# En VM-Backend, verificar conexión
psql -h 10.0.0.20 -p 5433 -U imageproc -d imageproc -c "SELECT 1"

# Si falla, verificar logs de PostgreSQL en VM-DB
docker logs imageproc-postgres

# Verificar que PostgreSQL escucha en 0.0.0.0
docker exec imageproc-postgres psql -U imageproc -d imageproc -c "SHOW listen_addresses;"
```

### Problema: Backend crashea al iniciar

```bash
# Ver logs detallados
cd /home/appuser/ProyectoSistemasDistribuidos
java -cp "build:lib/*" com.sistema.main.VisualDemoServerMain 2>&1 | head -50

# Verificar que el .jar de PostgreSQL está presente
ls -la lib/postgresql-*.jar

# Verificar que la compilación fue exitosa
javac -d build -cp "lib/*:." src/com/sistema/**/*.java 2>&1
```

### Problema: Base de datos no se inicializa

```bash
# Conectarse al contenedor
docker exec -it imageproc-postgres bash

# Ejecutar script SQL manualmente
psql -U imageproc -d imageproc < /docker-entrypoint-initdb.d/001_schema.sql

# Ver logs del contenedor
docker logs imageproc-postgres --tail 50
```

---

## 📈 Escalado (Agregar más Nodos Trabajadores)

Para agregar máquinas adicionales que actúen como **nodos trabajadores**:

### VM-Nodo-N (N = 1, 2, 3...)

```bash
# Instalar Java
sudo apt install -y openjdk-17-jdk

# Clonar repositorio
git clone https://github.com/marco344444/ProyectoSistemasDistribuidos.git

# Descargar JDBC driver
cd ProyectoSistemasDistribuidos/lib
wget https://jdbc.postgresql.org/download/postgresql-42.7.1.jar

# Crear .env.worker
cat > .env.worker << EOF
DB_HOST=10.0.0.20
DB_PORT=5433
DB_NAME=imageproc
DB_USER=imageproc
DB_PASSWORD=imageproc123
RMI_REGISTRY_HOST=10.0.0.10
RMI_REGISTRY_PORT=1099
WORKER_PORT=1099
WORKER_NAME=nodo-trabajador-N
EOF

# Compilar
mkdir -p build
javac -d build -cp "lib/*:." src/com/sistema/**/*.java

# Ejecutar worker (ver código de NodoTrabajadorImpl)
java -cp "build:lib/*" com.sistema.rmi.NodoTrabajadorImpl
```

---

## 🎯 Tips y Trucos de VMware Workstation

### 1. Acelerar Deployment con Templates

Una vez que tengas una VM-DB configurada completamente:

```
1. Power off VM-DB
2. Click derecho → Snapshots → Take Snapshot → "production-ready-db"
3. Click derecho → Manage → Clone
4. Elegir "Full Clone"
5. Nombre: VM-DB-Backup
6. Cambiar IP en la clonada a 10.0.0.21 para tener backup
```

### 2. Ejecutar Comandos Simultáneamente en Múltiples VMs

**Script PowerShell para Workstation:**

```powershell
$vms = @("10.0.0.20", "10.0.0.10", "10.0.0.30")

foreach ($vm in $vms) {
    Write-Host "Conectando a $vm..."
    ssh appuser@$vm "sudo apt update && sudo apt upgrade -y" &
}
Wait-Job
```

### 3. Monitoreo en Tiempo Real

En PowerShell, monitorear todas las VMs:

```powershell
while ($true) {
    Clear-Host
    Write-Host "=== Monitor de VMs ===" -ForegroundColor Green
    
    $vms = @{
        "vm-db" = "10.0.0.20"
        "vm-backend" = "10.0.0.10"
        "vm-frontend" = "10.0.0.30"
    }
    
    foreach ($name in $vms.Keys) {
        $ip = $vms[$name]
        $status = if (Test-Connection -ComputerName $ip -Count 1 -Quiet) {
            "✅ ONLINE"
        } else {
            "❌ OFFLINE"
        }
        Write-Host "$name ($ip): $status"
    }
    
    Start-Sleep -Seconds 5
}
```

### 4. Backup Automático de VMs

Desde PowerShell (como Administrador):

```powershell
# Backup completo de VM-DB
$sourceVM = "C:\VMs\VM-DB"
$backupPath = "C:\Backups\VM-DB-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

Copy-Item -Path $sourceVM -Destination $backupPath -Recurse

Write-Host "Backup creado en: $backupPath"
```

### 5. Compartir Carpetas Entre Host y VM

**En Workstation:**

1. Power off la VM
2. Click derecho → Settings
3. Options → Shared Folders
4. Add Shared Folder:
   ```
   Host path: C:\Users\prada\Desktop\Sistemas Distribuidos
   Name: shared-project
   ```
5. Power on VM

**En VM:**
```bash
# Ver carpeta compartida
ls -la /mnt/hgfs/shared-project

# Montar permanentemente
sudo nano /etc/fstab
# Agregar línea:
# .host:/shared-project /mnt/shared nfs defaults 0 0

# O crear enlace simbólico
ln -s /mnt/hgfs/shared-project ~/proyecto
```

### 6. Acelerar Copias entre Host y VM

**Usar rsync (más rápido que SCP):**

```bash
# Desde Host (PowerShell con WSL2 o Git Bash):
rsync -avz --progress appuser@10.0.0.10:/home/appuser/ProyectoSistemasDistribuidos ./

# O inverso (desde VM a Host):
rsync -avz --progress ./ProyectoSistemasDistribuidos appuser@<HOST_IP>:/home/appuser/
```

### 7. Uso de Consola Serial en Workstation

Para debugging de boot issues:

```
1. Click derecho en VM → Settings
2. Serial Ports
3. Add → Output file: /tmp/vm-db-serial.log
4. Power on VM
5. Ver logs: cat /tmp/vm-db-serial.log
```

### 8. Limpieza de Espacio en Disco

Las VMs en Workstation ocupan mucho espacio. Para limpiar:

```powershell
# Ver tamaño de VMs
Get-ChildItem -Path "C:\VMs" -Recurse -File | Measure-Object -Property Length -Sum | ForEach-Object { 
    [math]::Round($_.Sum / 1GB, 2) 
}

# Compactar disco de VM (libera espacio)
# En Workstation: VM → Settings → Hard Disk → Compact
```

### 9. Red Avanzada: Conectar Workstation a Red Real

Si necesitas que las VMs accedan a dispositivos en la red física:

```
1. En Workstation: Edit → Virtual Network Editor
2. Seleccionar VMnet5
3. Network Mode: Bridged
4. Seleccionar interfaz física: Ethernet (tu interfaz real)
5. OK

Las VMs ahora obtendrán IP del DHCP de tu red real
```

### 10. Atajos de Teclado Útiles

| Atajo | Acción |
|-------|--------|
| `Ctrl+Alt+Enter` | Pantalla completa |
| `Ctrl+Alt+T` | Escapar del modo full-screen |
| `Ctrl+Shift+F` | Fit window to VM |
| `Ctrl+Alt+I` | Instalar VMware Tools |

---

## 📝 Checklist Rápido - Primer Setup en Workstation

- [ ] Crear red VMnet5 en Workstation (Edit → Virtual Network Editor)
- [ ] Crear 3 VMs (VM-DB, VM-Backend, VM-Frontend)
- [ ] Instalar Ubuntu 22.04 en cada una
- [ ] Configurar IPs estáticas (10.0.0.20, 10.0.0.10, 10.0.0.30)
- [ ] Habilitar SSH en cada VM (`sudo systemctl enable ssh`)
- [ ] Crear snapshots limpios (`clean-ubuntu-setup`)
- [ ] Probar SSH desde Windows PowerShell
- [ ] Probar ping entre VMs
- [ ] Clonar repositorio en cada VM
- [ ] Comenzar deployment (Paso 2 en adelante)

### Dashboards Recomendados

1. **Prometheus + Grafana** para métricas
2. **ELK Stack** (Elasticsearch + Logstash + Kibana) para logs
3. **Datadog** o **New Relic** para APM

### Comando de Monitoreo Manual

```bash
# Monitor de recursos en cada VM
watch -n 5 'free -h && echo "---" && df -h && echo "---" && ps aux | grep java'

# Ver conexiones activas a BD
ssh user@10.0.0.20 'psql -U imageproc -d imageproc -c "SELECT count(*) FROM pg_stat_activity;"'

# Ver conexiones al backend
ssh user@10.0.0.10 'netstat -an | grep 8080 | wc -l'
```

---

## 🔒 Seguridad en Producción

### Recomendaciones

1. **SSL/TLS**: Generar certificados para Nginx
   ```bash
   sudo apt install -y certbot python3-certbot-nginx
   sudo certbot certonly --nginx -d tu-dominio.com
   ```

2. **Cambiar Contraseñas Default**:
   ```sql
   ALTER USER imageproc WITH PASSWORD 'nueva-contraseña-segura';
   ```

3. **Firewall**: Restringir acceso a solo IPs necesarias
   ```bash
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow from 10.0.0.0/24
   ```

4. **Backups Automáticos**:
   ```bash
   # Script backup en VM-DB
   0 2 * * * docker exec imageproc-postgres pg_dump -U imageproc imageproc > /backups/imageproc-$(date +\%Y\%m\%d).sql
   ```

---

## 📋 Resumen de Puertos

| VM | Servicio | Puerto | Protocolo |
|----|----------|--------|-----------|
| VM-DB | PostgreSQL | 5433 | TCP |
| VM-Backend | HTTP/REST | 8080 | TCP |
| VM-Backend | RMI Registry | 1099 | TCP |
| VM-Frontend | HTTP | 80 | TCP |
| VM-Frontend | HTTPS | 443 | TCP |

---

## 🚀 Deployment Automático (Script Completo)

**Crear `deploy-all-vms.sh`:**

```bash
#!/bin/bash

set -e

DB_HOST="10.0.0.20"
BACKEND_HOST="10.0.0.10"
FRONTEND_HOST="10.0.0.30"

echo "🚀 Iniciando deployment distribuido..."

# Deploy DB
echo "📦 Configurando VM-DB..."
ssh appuser@$DB_HOST << 'EOF'
cd ProyectoSistemasDistribuidos/db
docker-compose up -d
EOF

# Deploy Backend
echo "🖥️  Configurando VM-Backend..."
ssh appuser@$BACKEND_HOST << 'EOF'
cd ProyectoSistemasDistribuidos
./start-backend.sh &
EOF

# Deploy Frontend
echo "🌐 Configurando VM-Frontend..."
ssh appuser@$FRONTEND_HOST << 'EOF'
cd ProyectoSistemasDistribuidos/web-client
npm install
npm run build
sudo cp -r dist/* /var/www/imageproc/
sudo systemctl restart nginx
EOF

echo "✅ ¡Deployment completo!"
echo "Frontend: http://$FRONTEND_HOST"
echo "Backend: http://$BACKEND_HOST:8080"
echo "Database: $DB_HOST:5433"
```

---

## ❓ Preguntas Frecuentes

**P: ¿Puedo usar Windows Server en lugar de Linux?**  
R: Sí, pero recomendamos Linux por simplicidad. En Windows necesitarías WSL2 o Docker Desktop.

**P: ¿Cómo escalo a 100+ usuarios?**  
R: Agrega load balancing (HAProxy/Nginx) frente a múltiples instancias de Backend.

**P: ¿Qué pasa si VM-DB se cae?**  
R: Implementa replicación PostgreSQL o usa Aurora (AWS).

**P: ¿Puedo usar Kubernetes en lugar de VMs?**  
R: Sí, la arquitectura es compatible. Necesitarías Dockerfiles para cada servicio.

---

**Última actualización**: Mayo 2026  
**Versión**: 1.0
