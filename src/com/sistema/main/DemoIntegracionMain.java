package com.sistema.main;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;
import com.sistema.rest.RepositorioDatosFactory;
import com.sistema.rmi.NodoTrabajadorImpl;
import com.sistema.rmi.ServidorRmiMain;
import com.sistema.soap.ServicioProcesamientoImagenesImpl;

import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class DemoIntegracionMain {

    public static void main(String[] args) {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(1099);
            }

            registry.rebind(ServidorRmiMain.NOMBRE_BIND, new NodoTrabajadorImpl("worker-main"));

            IRepositorioDatos repositorio = RepositorioDatosFactory.crearRepositorio();
            repositorio.registrarNodo(new InfoNodo("worker-main", "localhost", 1099, true));

            ServicioProcesamientoImagenesImpl soap = new ServicioProcesamientoImagenesImpl(
                    repositorio,
                    (com.sistema.distribuido.nodos.INodoTrabajador) registry.lookup(ServidorRmiMain.NOMBRE_BIND)
            );

            System.out.println("Cliente envía login");
            String token = soap.iniciarSesion("usuario-demo", "1234");

            RequestLote lote = new RequestLote();
            lote.setIdUsuario("usuario-demo");
            lote.setImagenes(Arrays.asList(
                    new Archivo("foto1.jpg", "f1".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ESCALA_GRISES)),
                    new Archivo("foto2.jpg", "f2".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ROTAR, TipoTransformacion.DESENFOCAR))
            ));

            System.out.println("Cliente envía lote");
            String idLote = soap.enviarLoteProcesamiento(token, lote);

            EstadoLote estado = soap.consultarEstadoLote(token, idLote);
            System.out.println("Estado lote: " + estado.getEstado());
            System.out.println("Descarga ZIP simulado (bytes): " + soap.descargarLoteZip(token, idLote).length);

            System.out.println("Flujo completo demo: Cliente -> SOAP -> Backend -> RMI -> Worker -> Resultado");
        } catch (Exception e) {
            System.err.println("Error en la demo de integracion: " + e.getMessage());
        }
    }
}
