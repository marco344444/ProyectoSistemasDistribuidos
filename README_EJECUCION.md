# Prototipo Distribuido - Guia de ejecucion

Este prototipo implementa un flujo simulado de procesamiento masivo de imagenes:

Cliente -> SOAP -> Backend -> RMI -> Worker -> Resultado

## Estructura principal

- src/com/sistema/rmi: Nodo, servidor y cliente RMI.
- src/com/sistema/soap: Implementacion del servicio SOAP.
- src/com/sistema/rest: Repositorio REST simulado en memoria.
- src/com/sistema/model: Modelos simples de dominio.
- src/com/sistema/main: Main de integracion completa.

## Requisitos

- JDK 8+ instalado (debe existir el comando javac y java en PATH).

## Compilar

Desde la carpeta ProyectoSistemasDistribuidos:

PowerShell:

javac -encoding UTF-8 -d out (Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)

## Ejecutar por etapas (orden recomendado)

1) RMI servidor:

java -cp out com.sistema.rmi.ServidorRmiMain

2) RMI cliente (en otra terminal):

java -cp out com.sistema.rmi.ClienteRmiMain

3) Flujo integrado completo (opcional, en una sola ejecucion):

java -cp out com.sistema.main.DemoIntegracionMain

## Resultado esperado en consola

Mensajes similares a:

- Cliente envia lote
- SOAP recibe solicitud
- Backend envia trabajo a nodo
- Worker procesando trabajo
- Resultado generado

## Notas

- No hay base de datos real: todo se guarda en memoria.
- El procesamiento de imagenes es mock (simulado).
- Se preservaron los contratos existentes y no se modificaron sus firmas.
