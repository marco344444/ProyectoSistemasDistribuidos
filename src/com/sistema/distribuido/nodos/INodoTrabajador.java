package com.sistema.distribuido.nodos;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INodoTrabajador extends Remote {
    ResultadoProcesamiento procesarTrabajo(Trabajo trabajo) throws RemoteException;

    boolean estaActivo() throws RemoteException;

    int obtenerCargaActual() throws RemoteException;
}
