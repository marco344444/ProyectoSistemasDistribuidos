package com.sistema.rmi;

import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NodoTrabajadorImpl extends UnicastRemoteObject implements INodoTrabajador {

    private final String idNodo;

    public NodoTrabajadorImpl(String idNodo) throws RemoteException {
        super();
        this.idNodo = idNodo;
    }

    @Override
    public ResultadoProcesamiento procesarTrabajo(Trabajo trabajo) throws RemoteException {
        System.out.println("[RMI][" + idNodo + "] Worker procesando trabajo: " + trabajo.getIdTrabajo());

        List<String> rutas = new ArrayList<>();
        List<String> logs = new ArrayList<>();

        int cantidad = trabajo.getImagenes() == null ? 0 : trabajo.getImagenes().size();
        for (int i = 0; i < cantidad; i++) {
            String nombreSalida = trabajo.getRutaSalida() + "/img_" + i + "_procesada.jpg";
            rutas.add(nombreSalida);
            logs.add(LocalDateTime.now() + " - Imagen " + i + " procesada de forma simulada");
        }

        String mensaje = "Procesamiento simulado completado para " + cantidad + " imagen(es)";
        System.out.println("[RMI][" + idNodo + "] Resultado generado: " + mensaje);

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
}
