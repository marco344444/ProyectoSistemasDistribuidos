package com.sistema.rest;

import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositorioDatosImpl implements IRepositorioDatos {

    private final Map<String, InfoNodo> nodos = new LinkedHashMap<>();
    private final Map<String, RegistroTrabajo> trabajos = new LinkedHashMap<>();
    private final List<RegistroLog> logs = new ArrayList<>();

    @Override
    public synchronized List<InfoNodo> obtenerNodosActivos() {
        return nodos.values().stream().filter(InfoNodo::isActivo).collect(Collectors.toList());
    }

    @Override
    public synchronized void registrarNodo(InfoNodo nodo) {
        nodos.put(nodo.getIdNodo(), nodo);
        guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Nodo registrado: " + nodo.getIdNodo(), LocalDateTime.now().toString()));
    }

    @Override
    public synchronized String crearTrabajo(RegistroTrabajo trabajo) {
        trabajos.put(trabajo.getIdTrabajo(), trabajo);
        guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Trabajo creado: " + trabajo.getIdTrabajo(), LocalDateTime.now().toString()));
        return trabajo.getIdTrabajo();
    }

    @Override
    public synchronized void actualizarEstadoTrabajo(String idTrabajo, String nuevoEstado) {
        RegistroTrabajo trabajo = trabajos.get(idTrabajo);
        if (trabajo != null) {
            trabajo.setEstado(nuevoEstado);
            guardarLog(new RegistroLog("LOG-" + (logs.size() + 1), "INFO", "Estado actualizado: " + idTrabajo + " -> " + nuevoEstado, LocalDateTime.now().toString()));
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
    }

    public synchronized List<RegistroLog> getLogs() {
        return new ArrayList<>(logs);
    }
}
