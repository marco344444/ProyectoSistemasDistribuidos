package com.sistema.rmi;

import com.sistema.distribuido.nodos.INodoTrabajador;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorRmiMain {

    public static final String NOMBRE_BIND = "NodoTrabajadorPrincipal";

    public static void main(String[] args) {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("[RMI] Registry creado en puerto 1099");
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(1099);
                System.out.println("[RMI] Registry existente reutilizado en puerto 1099");
            }

            INodoTrabajador nodo = new NodoTrabajadorImpl("worker-01");
            registry.rebind(NOMBRE_BIND, nodo);

            System.out.println("[RMI] Nodo registrado con nombre: " + NOMBRE_BIND);
            System.out.println("[RMI] Servidor listo para recibir trabajos.");
        } catch (Exception e) {
            System.err.println("[RMI] Error iniciando servidor: " + e.getMessage());
        }
    }
}
