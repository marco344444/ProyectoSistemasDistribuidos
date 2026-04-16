package com.sistema.soap;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.IServicioProcesamientoImagenes;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServicioProcesamientoImagenesImpl implements IServicioProcesamientoImagenes {

    private final IRepositorioDatos repositorio;
    private final INodoTrabajador nodoTrabajador;
    private final Map<String, String> sesiones = new HashMap<>();
    private final Map<String, byte[]> resultadosPorId = new LinkedHashMap<>();
    private final Map<String, List<String>> resultadosPorLote = new LinkedHashMap<>();
    private final Map<String, List<String>> logsPorLote = new LinkedHashMap<>();
    private final Map<String, Long> duracionPorLoteMs = new LinkedHashMap<>();

    public ServicioProcesamientoImagenesImpl(IRepositorioDatos repositorio, INodoTrabajador nodoTrabajador) {
        this.repositorio = repositorio;
        this.nodoTrabajador = nodoTrabajador;
    }

    @Override
    public String iniciarSesion(String usuario, String password) {
        String token = "TOKEN-" + UUID.randomUUID();
        sesiones.put(token, usuario);
        repositorio.guardarLog(new RegistroLog("LOGIN-" + System.currentTimeMillis(), "INFO", "Inicio de sesion para: " + usuario, LocalDateTime.now().toString()));
        return token;
    }

    @Override
    public String enviarLoteProcesamiento(String tokenSesion, RequestLote lote) {
        validarSesion(tokenSesion);
        String idTrabajo = "JOB-" + UUID.randomUUID();
        long inicio = System.currentTimeMillis();

        System.out.println("[SOAP] SOAP recibe solicitud para lote del usuario " + lote.getIdUsuario());
        repositorio.crearTrabajo(new RegistroTrabajo(idTrabajo, lote.getIdUsuario(), "RECIBIDO", LocalDateTime.now().toString()));

        Trabajo trabajo = new Trabajo();
        trabajo.setIdTrabajo(idTrabajo);
        trabajo.setImagenes(lote.getImagenes());
        trabajo.setRutaEntrada("/tmp/entrada/" + idTrabajo);
        trabajo.setRutaSalida("/tmp/salida/" + idTrabajo);

        try {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "EN_PROCESO");
            System.out.println("[Backend] Backend envía trabajo a nodo");
            ResultadoProcesamiento resultado = nodoTrabajador.procesarTrabajo(trabajo);

            if (resultado.isExito()) {
                repositorio.actualizarEstadoTrabajo(idTrabajo, "COMPLETADO");
                synchronized (resultadosPorId) {
                    resultadosPorLote.put(idTrabajo, new ArrayList<>(resultado.getRutasArchivosGenerados()));
                    logsPorLote.put(idTrabajo, new ArrayList<>(resultado.getLogsGenerados()));
                    if (resultado.getArchivosGenerados() != null) {
                        resultadosPorId.putAll(resultado.getArchivosGenerados());
                    }
                }
            } else {
                repositorio.actualizarEstadoTrabajo(idTrabajo, "ERROR");
            }

            repositorio.guardarLog(new RegistroLog("R-" + System.currentTimeMillis(), "INFO", "Resultado: " + resultado.getMensaje(), LocalDateTime.now().toString()));
        } catch (Exception e) {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "ERROR");
            repositorio.guardarLog(new RegistroLog("R-" + System.currentTimeMillis(), "ERROR", "Fallo RMI: " + e.getMessage(), LocalDateTime.now().toString()));
        } finally {
            long duracion = Math.max(0L, System.currentTimeMillis() - inicio);
            synchronized (resultadosPorId) {
                duracionPorLoteMs.put(idTrabajo, duracion);
                if (!logsPorLote.containsKey(idTrabajo)) {
                    logsPorLote.put(idTrabajo, new ArrayList<>());
                }
            }
        }

        return idTrabajo;
    }

    @Override
    public EstadoLote consultarEstadoLote(String tokenSesion, String idLote) {
        validarSesion(tokenSesion);
        for (RegistroTrabajo t : repositorio.obtenerHistorialUsuario(sesiones.get(tokenSesion))) {
            if (idLote.equals(t.getIdTrabajo())) {
                return new EstadoLote(idLote, t.getEstado(), 0, 0);
            }
        }
        return new EstadoLote(idLote, "NO_ENCONTRADO", 0, 0);
    }

    @Override
    public byte[] descargarImagen(String tokenSesion, String idResultado) {
        validarSesion(tokenSesion);
        synchronized (resultadosPorId) {
            byte[] data = resultadosPorId.get(idResultado);
            if (data != null) {
                return data;
            }
        }
        return ("No encontrado: " + idResultado).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] descargarLoteZip(String tokenSesion, String idLote) {
        validarSesion(tokenSesion);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(baos, StandardCharsets.UTF_8);

            List<String> resultados;
            synchronized (resultadosPorId) {
                resultados = resultadosPorLote.get(idLote) == null
                        ? new ArrayList<>()
                        : new ArrayList<>(resultadosPorLote.get(idLote));
            }

            for (String nombre : resultados) {
                byte[] contenido;
                synchronized (resultadosPorId) {
                    contenido = resultadosPorId.get(nombre);
                }
                if (contenido == null) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(nombre);
                zip.putNextEntry(entry);
                zip.write(contenido);
                zip.closeEntry();
            }

            if (resultados.isEmpty()) {
                ZipEntry readme = new ZipEntry("README.txt");
                zip.putNextEntry(readme);
                zip.write(("El lote " + idLote + " no tiene archivos disponibles").getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            zip.finish();
            zip.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return ("No se pudo generar ZIP del lote " + idLote).getBytes(StandardCharsets.UTF_8);
        }
    }

    public List<String> obtenerResultadosLote(String idLote) {
        synchronized (resultadosPorId) {
            List<String> resultados = resultadosPorLote.get(idLote);
            return resultados == null ? new ArrayList<>() : new ArrayList<>(resultados);
        }
    }

    public List<String> obtenerLogsLote(String idLote) {
        synchronized (resultadosPorId) {
            List<String> logs = logsPorLote.get(idLote);
            return logs == null ? new ArrayList<>() : new ArrayList<>(logs);
        }
    }

    public long obtenerDuracionLoteMs(String idLote) {
        synchronized (resultadosPorId) {
            Long duracion = duracionPorLoteMs.get(idLote);
            return duracion == null ? 0L : duracion;
        }
    }

    public String obtenerUsuarioSesion(String tokenSesion) {
        validarSesion(tokenSesion);
        return sesiones.get(tokenSesion);
    }

    public void cerrarSesion(String tokenSesion) {
        sesiones.remove(tokenSesion);
    }

    private void validarSesion(String tokenSesion) {
        if (!sesiones.containsKey(tokenSesion)) {
            throw new IllegalArgumentException("Sesion invalida");
        }
    }
}
