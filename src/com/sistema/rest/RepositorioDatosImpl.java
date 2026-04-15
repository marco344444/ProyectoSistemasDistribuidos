package com.sistema.rest;

import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositorioDatosImpl implements IRepositorioDatos {

    private final Map<String, InfoNodo> nodos = new LinkedHashMap<>();
    private final Map<String, RegistroTrabajo> trabajos = new LinkedHashMap<>();
    private final List<RegistroLog> logs = new ArrayList<>();

    // Replica simulada en memoria para evidenciar replicacion de BD
    private final Map<String, InfoNodo> nodosReplica = new LinkedHashMap<>();
    private final Map<String, RegistroTrabajo> trabajosReplica = new LinkedHashMap<>();
    private final List<RegistroLog> logsReplica = new ArrayList<>();
    private String ultimoSyncReplica = "N/A";
    private long totalReplicaciones = 0;

    @Override
    public synchronized List<InfoNodo> obtenerNodosActivos() {
        return nodos.values().stream().filter(InfoNodo::isActivo).collect(Collectors.toList());
    }

    @Override
    public synchronized void registrarNodo(InfoNodo nodo) {
        nodos.put(nodo.getIdNodo(), nodo);
        guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Nodo registrado: " + nodo.getIdNodo(), LocalDateTime.now().toString()));
        replicarEstado();
    }

    @Override
    public synchronized String crearTrabajo(RegistroTrabajo trabajo) {
        trabajos.put(trabajo.getIdTrabajo(), trabajo);
        guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Trabajo creado: " + trabajo.getIdTrabajo(), LocalDateTime.now().toString()));
        replicarEstado();
        return trabajo.getIdTrabajo();
    }

    @Override
    public synchronized void actualizarEstadoTrabajo(String idTrabajo, String nuevoEstado) {
        RegistroTrabajo trabajo = trabajos.get(idTrabajo);
        if (trabajo != null) {
            trabajo.setEstado(nuevoEstado);
            guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Estado actualizado: " + idTrabajo + " -> " + nuevoEstado, LocalDateTime.now().toString()));
            replicarEstado();
        }
    }

    @Override
    public synchronized List<RegistroTrabajo> obtenerHistorialUsuario(String idUsuario) {
        return trabajos.values().stream()
                .filter(t -> idUsuario.equals(t.getIdUsuario()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void guardarLog(RegistroLog log) {
        logs.add(log);
        replicarEstado();
    }

    public synchronized List<RegistroLog> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized Map<String, Object> obtenerMetricasReplica() {
        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("nodosPrimario", nodos.size());
        metricas.put("nodosReplica", nodosReplica.size());
        metricas.put("trabajosPrimario", trabajos.size());
        metricas.put("trabajosReplica", trabajosReplica.size());
        metricas.put("logsPrimario", logs.size());
        metricas.put("logsReplica", logsReplica.size());
        metricas.put("ultimoSync", ultimoSyncReplica);
        metricas.put("totalReplicaciones", totalReplicaciones);
        metricas.put("consistente", replicaConsistente());
        return metricas;
    }

    private void replicarEstado() {
        nodosReplica.clear();
        nodosReplica.putAll(nodos);

        trabajosReplica.clear();
        trabajosReplica.putAll(trabajos);

        logsReplica.clear();
        logsReplica.addAll(logs);

        totalReplicaciones++;
        ultimoSyncReplica = LocalDateTime.now().toString();
    }

    private boolean replicaConsistente() {
        if (nodos.size() != nodosReplica.size()) {
            return false;
        }
        if (trabajos.size() != trabajosReplica.size()) {
            return false;
        }
        if (logs.size() != logsReplica.size()) {
            return false;
        }

        if (!new LinkedHashSet<>(nodos.keySet()).equals(new LinkedHashSet<>(nodosReplica.keySet()))) {
            return false;
        }
        return new LinkedHashSet<>(trabajos.keySet()).equals(new LinkedHashSet<>(trabajosReplica.keySet()));
    }
}
