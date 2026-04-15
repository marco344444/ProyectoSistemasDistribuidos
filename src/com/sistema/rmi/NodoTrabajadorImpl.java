package com.sistema.rmi;

import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NodoTrabajadorImpl extends UnicastRemoteObject implements INodoTrabajador {

    private final String idNodo;
    private final AtomicInteger totalTrabajos = new AtomicInteger(0);
    private final AtomicInteger totalImagenes = new AtomicInteger(0);
    private final AtomicLong tiempoAcumuladoMs = new AtomicLong(0);
    private final AtomicInteger maxHilosParalelos = new AtomicInteger(0);

    public NodoTrabajadorImpl(String idNodo) throws RemoteException {
        super();
        this.idNodo = idNodo;
    }

    @Override
    public ResultadoProcesamiento procesarTrabajo(Trabajo trabajo) throws RemoteException {
        System.out.println("[RMI][" + idNodo + "] Worker procesando trabajo: " + trabajo.getIdTrabajo());

        List<String> rutas = Collections.synchronizedList(new ArrayList<>());
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        int cantidad = trabajo.getImagenes() == null ? 0 : trabajo.getImagenes().size();

        long inicio = System.currentTimeMillis();
        int hilos = Math.max(1, Math.min(8, cantidad == 0 ? 1 : cantidad));
        maxHilosParalelos.updateAndGet(actual -> Math.max(actual, hilos));

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < cantidad; i++) {
            final int idx = i;
            tareas.add(pool.submit(() -> {
                String threadName = Thread.currentThread().getName();
                try {
                    int espera = ThreadLocalRandom.current().nextInt(40, 140);
                    Thread.sleep(espera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String nombreSalida = trabajo.getRutaSalida() + "/img_" + idx + "_procesada.jpg";
                rutas.add(nombreSalida);
                String log = LocalDateTime.now() + " - [" + threadName + "] Imagen " + idx + " procesada de forma simulada";
                logs.add(log);
                System.out.println("[RMI][" + idNodo + "] " + log);
            }));
        }

        for (Future<?> tarea : tareas) {
            try {
                tarea.get();
            } catch (Exception e) {
                logs.add(LocalDateTime.now() + " - Error en tarea paralela: " + e.getMessage());
            }
        }
        pool.shutdown();

        long duracion = Math.max(1, System.currentTimeMillis() - inicio);
        totalTrabajos.incrementAndGet();
        totalImagenes.addAndGet(cantidad);
        tiempoAcumuladoMs.addAndGet(duracion);

        String mensaje = "Procesamiento simulado completado para " + cantidad + " imagen(es)";
        System.out.println("[RMI][" + idNodo + "] Resultado generado: " + mensaje + " en " + duracion + " ms con " + hilos + " hilo(s)");

        return new ResultadoProcesamiento(true, mensaje, rutas, logs);
    }

    @Override
    public boolean estaActivo() throws RemoteException {
        return true;
    }

    @Override
    public int obtenerCargaActual() throws RemoteException {
        return ThreadLocalRandom.current().nextInt(5, 50);
    }

    public synchronized Map<String, Object> obtenerMetricasConsumo() {
        Map<String, Object> metricas = new LinkedHashMap<>();
        int trabajos = totalTrabajos.get();
        long promedio = trabajos == 0 ? 0 : (tiempoAcumuladoMs.get() / trabajos);

        metricas.put("idNodo", idNodo);
        metricas.put("trabajosProcesados", trabajos);
        metricas.put("imagenesProcesadas", totalImagenes.get());
        metricas.put("tiempoPromedioMs", promedio);
        metricas.put("maxHilosParalelos", maxHilosParalelos.get());
        metricas.put("cargaActual", obtenerCargaSegura());
        return metricas;
    }

    private int obtenerCargaSegura() {
        try {
            return obtenerCargaActual();
        } catch (Exception e) {
            return 0;
        }
    }
}
