package com.sistema.rmi;

import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;

import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class ClienteRmiMain {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            INodoTrabajador nodo = (INodoTrabajador) registry.lookup(ServidorRmiMain.NOMBRE_BIND);

            System.out.println("[Cliente RMI] Nodo activo: " + nodo.estaActivo());
            System.out.println("[Cliente RMI] Carga actual: " + nodo.obtenerCargaActual() + "%");

            Trabajo trabajo = new Trabajo();
            trabajo.setIdTrabajo("JOB-RMI-001");
            trabajo.setRutaEntrada("/entrada");
            trabajo.setRutaSalida("/salida");
            trabajo.setImagenes(Arrays.asList(
                    new Archivo("a.jpg", "img-a".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ROTAR)),
                    new Archivo("b.jpg", "img-b".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ESCALA_GRISES))
            ));

            ResultadoProcesamiento resultado = nodo.procesarTrabajo(trabajo);
            System.out.println("[Cliente RMI] Exito: " + resultado.isExito());
            System.out.println("[Cliente RMI] Mensaje: " + resultado.getMensaje());
            System.out.println("[Cliente RMI] Archivos generados: " + resultado.getRutasArchivosGenerados());
        } catch (Exception e) {
            System.err.println("[Cliente RMI] Error: " + e.getMessage());
        }
    }
}
