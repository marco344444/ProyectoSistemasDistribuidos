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
import com.sistema.model.Archivo;
import com.sistema.rest.RepositorioDatosJdbcImpl;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServicioProcesamientoImagenesImpl implements IServicioProcesamientoImagenes {

    private final IRepositorioDatos repositorio;
    private final List<INodoTrabajador> nodosTrabajadores;
    private final ExecutorService procesadorLotes;
    private final AtomicInteger siguienteNodoIdx = new AtomicInteger(0);
    private final Map<String, String> sesiones = new HashMap<>();
    private final Map<String, byte[]> resultadosPorId = new LinkedHashMap<>();
    private final Map<String, List<String>> resultadosPorLote = new LinkedHashMap<>();
    private final Map<String, List<String>> logsPorLote = new LinkedHashMap<>();
    private final Map<String, Long> duracionPorLoteMs = new LinkedHashMap<>();
    private final Map<String, Integer> totalPorLote = new LinkedHashMap<>();
    private final Map<String, Integer> procesadasPorLote = new LinkedHashMap<>();
    private final Map<String, LocalDateTime> fechaRecepcionPorLote = new LinkedHashMap<>();

    public ServicioProcesamientoImagenesImpl(IRepositorioDatos repositorio, INodoTrabajador nodoTrabajador) {
        this(repositorio, Collections.singletonList(nodoTrabajador));
    }

    public ServicioProcesamientoImagenesImpl(IRepositorioDatos repositorio, List<INodoTrabajador> nodosTrabajadores) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio no puede ser null");
        if (nodosTrabajadores == null || nodosTrabajadores.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un nodo trabajador");
        }
        this.nodosTrabajadores = new ArrayList<>(nodosTrabajadores);
        this.procesadorLotes = Executors.newFixedThreadPool(Math.max(4, this.nodosTrabajadores.size()));
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
        LocalDateTime fechaRecepcion = LocalDateTime.now();
        int totalImagenes = (lote.getImagenes() == null) ? 0 : lote.getImagenes().size();

        System.out.println("[SOAP] SOAP recibe solicitud para lote del usuario " + lote.getIdUsuario());
        repositorio.crearTrabajo(new RegistroTrabajo(idTrabajo, lote.getIdUsuario(), "RECIBIDO", fechaRecepcion.toString()));

        synchronized (resultadosPorId) {
            totalPorLote.put(idTrabajo, totalImagenes);
            procesadasPorLote.put(idTrabajo, 0);
            fechaRecepcionPorLote.put(idTrabajo, fechaRecepcion);
            resultadosPorLote.put(idTrabajo, new ArrayList<>());
            logsPorLote.put(idTrabajo, new ArrayList<>());
            duracionPorLoteMs.put(idTrabajo, 0L);
        }

        try {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "EN_PROCESO");
            iniciarProcesamientoAsincrono(idTrabajo, lote, fechaRecepcion);
        } catch (Exception e) {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "ERROR");
            repositorio.guardarLog(new RegistroLog("R-" + System.currentTimeMillis(), "ERROR", "Fallo RMI: " + e.getMessage(), LocalDateTime.now().toString()));
        }

        return idTrabajo;
    }

    @Override
    public EstadoLote consultarEstadoLote(String tokenSesion, String idLote) {
        validarSesion(tokenSesion);
        for (RegistroTrabajo t : repositorio.obtenerHistorialUsuario(sesiones.get(tokenSesion))) {
            if (idLote.equals(t.getIdTrabajo())) {
                int total;
                int procesadas;
                synchronized (resultadosPorId) {
                    total = totalPorLote.getOrDefault(idLote, 0);
                    procesadas = procesadasPorLote.getOrDefault(idLote, 0);

                    if (total <= 0) {
                        List<String> resultados = resultadosPorLote.get(idLote);
                        if (resultados != null && !resultados.isEmpty()) {
                            total = resultados.size();
                            procesadas = resultados.size();
                        }
                    }
                }
                return new EstadoLote(idLote, t.getEstado(), total, procesadas);
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

    private INodoTrabajador seleccionarNodo() {
        int idx = Math.floorMod(siguienteNodoIdx.getAndIncrement(), nodosTrabajadores.size());
        return nodosTrabajadores.get(idx);
    }

    private void iniciarProcesamientoAsincrono(String idTrabajo, RequestLote lote, LocalDateTime fechaRecepcion) {
        CompletableFuture.runAsync(() -> procesarLoteEnSegundoPlano(idTrabajo, lote, fechaRecepcion), procesadorLotes);
    }

    private void procesarLoteEnSegundoPlano(String idTrabajo, RequestLote lote, LocalDateTime fechaRecepcion) {
        long inicio = System.currentTimeMillis();
        LocalDateTime fechaConversion = fechaRecepcion;
        List<Archivo> imagenes = lote.getImagenes() == null
                ? Collections.emptyList()
                : lote.getImagenes();

        if (imagenes.isEmpty()) {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "COMPLETADO");
            synchronized (resultadosPorId) {
                duracionPorLoteMs.put(idTrabajo, 0L);
            }
            return;
        }

        int totalImagenes = imagenes.size();
        int tamanoBloque = calcularTamanoBloque(totalImagenes);
        List<List<Archivo>> bloques = particionarImagenes(imagenes, tamanoBloque);
        AtomicInteger procesadas = new AtomicInteger(0);
        AtomicInteger bloquesFallidos = new AtomicInteger(0);
        List<CompletableFuture<ResultadoBloque>> tareas = new ArrayList<>();

        for (int i = 0; i < bloques.size(); i++) {
            final int bloqueIdx = i + 1;
            final List<Archivo> bloque = bloques.get(i);
            CompletableFuture<ResultadoBloque> tarea = CompletableFuture
                    .supplyAsync(() -> procesarBloque(idTrabajo, bloque, bloqueIdx), procesadorLotes)
                    .thenApply(resultado -> {
                        if (!resultado.exito) {
                            bloquesFallidos.incrementAndGet();
                        }
                        int procesadasActuales = Math.min(totalImagenes, procesadas.addAndGet(resultado.procesadas));
                        sincronizarResultadoParcial(idTrabajo, resultado, procesadasActuales, inicio);
                        return resultado;
                    });
            tareas.add(tarea);
        }

        CompletableFuture.allOf(tareas.toArray(CompletableFuture[]::new)).join();

        List<String> rutasFinales = new ArrayList<>();
        List<String> logsFinales = new ArrayList<>();
        Map<String, byte[]> archivosFinales = new LinkedHashMap<>();
        for (CompletableFuture<ResultadoBloque> tarea : tareas) {
            ResultadoBloque resultado = tarea.join();
            rutasFinales.addAll(resultado.rutas);
            logsFinales.addAll(resultado.logs);
            archivosFinales.putAll(resultado.archivos);
            if (resultado.fechaConversion != null && resultado.fechaConversion.isAfter(fechaConversion)) {
                fechaConversion = resultado.fechaConversion;
            }
        }

        boolean exitoProcesamiento = bloquesFallidos.get() == 0;
        boolean persistenciaExitosa = true;

        try {
            if (repositorio instanceof RepositorioDatosJdbcImpl) {
                ((RepositorioDatosJdbcImpl) repositorio).registrarResultadosImagenes(
                        idTrabajo,
                        lote.getIdUsuario(),
                        imagenes,
                        rutasFinales,
                        archivosFinales,
                        "nodo-rmi-cluster",
                        fechaRecepcion,
                        fechaConversion
                );
            }
        } catch (Exception e) {
            persistenciaExitosa = false;
            logsFinales.add("Error persistiendo resultados del lote: " + e.getMessage());
        } finally {
            synchronized (resultadosPorId) {
                resultadosPorLote.put(idTrabajo, new ArrayList<>(rutasFinales));
                logsPorLote.put(idTrabajo, new ArrayList<>(logsFinales));
                resultadosPorId.putAll(archivosFinales);
                procesadasPorLote.put(idTrabajo, Math.min(totalImagenes, procesadas.get()));
                duracionPorLoteMs.put(idTrabajo, Math.max(0L, System.currentTimeMillis() - inicio));
            }

            try {
                repositorio.actualizarEstadoTrabajo(idTrabajo, exitoProcesamiento ? "COMPLETADO" : "ERROR");
            } catch (Exception e) {
                synchronized (resultadosPorId) {
                    logsPorLote.computeIfAbsent(idTrabajo, key -> new ArrayList<>())
                            .add("No se pudo actualizar el estado final: " + e.getMessage());
                }
            }

            try {
                repositorio.guardarLog(new RegistroLog(
                        "R-" + System.currentTimeMillis(),
                        exitoProcesamiento ? "INFO" : "ERROR",
                        "Resultado lote " + idTrabajo + ": " + (exitoProcesamiento ? "COMPLETADO" : "ERROR")
                                + (persistenciaExitosa ? "" : " (persistencia parcial fallida)"),
                        LocalDateTime.now().toString()
                ));
            } catch (Exception e) {
                synchronized (resultadosPorId) {
                    logsPorLote.computeIfAbsent(idTrabajo, key -> new ArrayList<>())
                            .add("No se pudo guardar log final: " + e.getMessage());
                }
            }
        }
    }

    private int calcularTamanoBloque(int totalImagenes) {
        int base = (int) Math.ceil((double) totalImagenes / nodosTrabajadores.size());
        return Math.max(2, base);
    }

    private List<List<Archivo>> particionarImagenes(List<Archivo> imagenes, int tamanoBloque) {
        List<List<Archivo>> bloques = new ArrayList<>();
        for (int i = 0; i < imagenes.size(); i += tamanoBloque) {
            int fin = Math.min(imagenes.size(), i + tamanoBloque);
            bloques.add(new ArrayList<>(imagenes.subList(i, fin)));
        }
        return bloques;
    }

    private ResultadoBloque procesarBloque(String idTrabajo, List<Archivo> bloque, int bloqueIdx) {
        Exception ultimoError = null;
        int intentosMaximos = Math.max(2, nodosTrabajadores.size());

        for (int intento = 1; intento <= intentosMaximos; intento++) {
            Trabajo trabajo = new Trabajo();
            trabajo.setIdTrabajo(idTrabajo + "-BLOQUE-" + bloqueIdx + "-INT-" + intento);
            trabajo.setImagenes(bloque);
            trabajo.setRutaEntrada("/tmp/entrada/" + idTrabajo);
            trabajo.setRutaSalida("/tmp/salida/" + idTrabajo);

            try {
                INodoTrabajador nodoSeleccionado = seleccionarNodo();
                ResultadoProcesamiento resultado = nodoSeleccionado.procesarTrabajo(trabajo);
                if (resultado == null || !resultado.isExito()) {
                    ultimoError = new IllegalStateException("El nodo devolvio un resultado fallido para el bloque " + bloqueIdx);
                    continue;
                }

                int procesadasBloque = resultado.getRutasArchivosGenerados() == null
                        ? bloque.size()
                        : Math.min(bloque.size(), resultado.getRutasArchivosGenerados().size());
                return ResultadoBloque.ok(
                        resultado.getRutasArchivosGenerados(),
                        resultado.getLogsGenerados(),
                        resultado.getArchivosGenerados(),
                        procesadasBloque,
                        LocalDateTime.now()
                );
            } catch (Exception e) {
                ultimoError = e;
            }
        }

        String mensaje = ultimoError == null
                ? "Error RMI procesando bloque " + bloqueIdx
                : "Error RMI procesando bloque " + bloqueIdx + ": " + ultimoError.getMessage();
        return ResultadoBloque.error(mensaje, 0);
    }

    private void sincronizarResultadoParcial(
            String idTrabajo,
            ResultadoBloque parcial,
            int procesadasActuales,
            long inicio
    ) {
        synchronized (resultadosPorId) {
            List<String> rutas = resultadosPorLote.get(idTrabajo);
            if (rutas == null) {
                rutas = new ArrayList<>();
            }
            rutas.addAll(parcial.rutas);
            resultadosPorLote.put(idTrabajo, rutas);

            List<String> logs = logsPorLote.get(idTrabajo);
            if (logs == null) {
                logs = new ArrayList<>();
            }
            logs.addAll(parcial.logs);
            logsPorLote.put(idTrabajo, logs);

            resultadosPorId.putAll(parcial.archivos);
            procesadasPorLote.put(idTrabajo, procesadasActuales);
            duracionPorLoteMs.put(idTrabajo, Math.max(0L, System.currentTimeMillis() - inicio));
        }
    }

    private static class ResultadoBloque {
        private final boolean exito;
        private final List<String> rutas;
        private final List<String> logs;
        private final Map<String, byte[]> archivos;
        private final int procesadas;
        private final LocalDateTime fechaConversion;

        private ResultadoBloque(
                boolean exito,
                List<String> rutas,
                List<String> logs,
                Map<String, byte[]> archivos,
                int procesadas,
                LocalDateTime fechaConversion
        ) {
            this.exito = exito;
            this.rutas = rutas;
            this.logs = logs;
            this.archivos = archivos;
            this.procesadas = procesadas;
            this.fechaConversion = fechaConversion;
        }

        private static ResultadoBloque ok(
                List<String> rutas,
                List<String> logs,
                Map<String, byte[]> archivos,
                int procesadas,
                LocalDateTime fechaConversion
        ) {
            return new ResultadoBloque(
                    true,
                    rutas == null ? new ArrayList<>() : new ArrayList<>(rutas),
                    logs == null ? new ArrayList<>() : new ArrayList<>(logs),
                    archivos == null ? new LinkedHashMap<>() : new LinkedHashMap<>(archivos),
                    Math.max(0, procesadas),
                    fechaConversion
            );
        }

        private static ResultadoBloque error(String mensaje, int procesadas) {
            List<String> logs = new ArrayList<>();
            logs.add(mensaje);
            return new ResultadoBloque(false, new ArrayList<>(), logs, new LinkedHashMap<>(), Math.max(0, procesadas), LocalDateTime.now());
        }
    }
}
