package com.sistema.distribuido.nodos;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Contrato RMI que los Nodos Trabajadores deben implementar.
 * El Servidor de Aplicación llama a estos métodos remotamente.
 */
public interface INodoTrabajador extends Remote {

    /**
     * Método principal: El servidor envía un trabajo y el nodo lo procesa en paralelo.
     * @param trabajo Objeto con los datos de entrada y transformaciones.
     * @return Objeto con las rutas de los archivos generados y logs.
     * @throws RemoteException Si hay error de comunicación.
     */
    ResultadoProcesamiento procesarTrabajo(Trabajo trabajo) throws RemoteException;

    /**
     * Método de "Heartbeat" o ping para saber si el nodo está vivo.
     * @return true si el nodo está disponible y operativo.
     */
    boolean estaActivo() throws RemoteException;
    
    /**
     * Obtiene la carga actual del nodo (para decidir a cuál enviarle trabajo).
     * @return Porcentaje de uso de CPU o número de hilos ocupados.
     */
    int obtenerCargaActual() throws RemoteException;
}