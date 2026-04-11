package com.sistema.main;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;
import com.sistema.rest.RepositorioDatosImpl;
import com.sistema.rmi.NodoTrabajadorImpl;
import com.sistema.rmi.ServidorRmiMain;
import com.sistema.soap.ServicioProcesamientoImagenesImpl;

import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

public class Entrega2SmokeTestMain {

    public static void main(String[] args) {
        int ok = 0;
        int total = 0;

        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(1099);
            }

            registry.rebind(ServidorRmiMain.NOMBRE_BIND, new NodoTrabajadorImpl("worker-entrega2"));
            INodoTrabajador nodo = (INodoTrabajador) registry.lookup(ServidorRmiMain.NOMBRE_BIND);

            RepositorioDatosImpl repositorio = new RepositorioDatosImpl();
            repositorio.registrarNodo(new InfoNodo("worker-entrega2", "localhost", 1099, true));

            ServicioProcesamientoImagenesImpl soap = new ServicioProcesamientoImagenesImpl(repositorio, nodo);

            total++;
            if (checkRmi(nodo)) {
                ok++;
                logPass("RMI: nodo responde pruebas");
            } else {
                logFail("RMI: nodo no respondio correctamente");
            }

            total++;
            if (checkWebClientFlow(soap)) {
                ok++;
                logPass("Cliente web: envia solicitud y recibe respuesta");
            } else {
                logFail("Cliente web: fallo flujo de prueba");
            }

            total++;
            if (checkRestRepo(repositorio)) {
                ok++;
                logPass("REST repositorio: guarda y consulta datos de prueba");
            } else {
                logFail("REST repositorio: fallo operacion de prueba");
            }

            System.out.println();
            System.out.println("================= RESULTADO ENTREGA 2 =================");
            System.out.println("Checks aprobados: " + ok + " / " + total);
            if (ok == total) {
                System.out.println("Estado: CUMPLE con los puntos tecnicos de Entrega 2.");
            } else {
                System.out.println("Estado: NO CUMPLE completamente. Revisar logs anteriores.");
            }
        } catch (Exception e) {
            System.err.println("Error ejecutando smoke test Entrega 2: " + e.getMessage());
        }
    }

    private static boolean checkRmi(INodoTrabajador nodo) {
        try {
            if (!nodo.estaActivo()) {
                return false;
            }

            Trabajo trabajo = new Trabajo();
            trabajo.setIdTrabajo("CHECK-RMI-001");
            trabajo.setRutaEntrada("/entrada");
            trabajo.setRutaSalida("/salida");
            trabajo.setImagenes(Arrays.asList(
                    new Archivo("rmi_1.jpg", "rmi1".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ROTAR)),
                    new Archivo("rmi_2.jpg", "rmi2".getBytes(StandardCharsets.UTF_8), Arrays.asList(TipoTransformacion.ESCALA_GRISES))
            ));

            ResultadoProcesamiento resultado = nodo.procesarTrabajo(trabajo);
            return resultado != null && resultado.isExito() && resultado.getRutasArchivosGenerados() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkWebClientFlow(ServicioProcesamientoImagenesImpl soap) {
        try {
            String token = soap.iniciarSesion("web@cliente.com", "1234");
            RequestLote lote = crearLote("web@cliente.com", 3);
            String idLote = soap.enviarLoteProcesamiento(token, lote);
            EstadoLote estado = soap.consultarEstadoLote(token, idLote);
            return idLote != null && !idLote.isEmpty() && estado != null && estado.getEstado() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkRestRepo(RepositorioDatosImpl repo) {
        try {
            List<InfoNodo> activos = repo.obtenerNodosActivos();
            return activos != null && !activos.isEmpty() && repo.getLogs() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static RequestLote crearLote(String usuario, int cantidad) {
        RequestLote lote = new RequestLote();
        lote.setIdUsuario(usuario);

        java.util.ArrayList<Archivo> archivos = new java.util.ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            archivos.add(new Archivo(
                    "img_" + i + ".jpg",
                    ("contenido_" + i).getBytes(StandardCharsets.UTF_8),
                    Arrays.asList(TipoTransformacion.ESCALA_GRISES, TipoTransformacion.ROTAR)
            ));
        }
        lote.setImagenes(archivos);
        return lote;
    }

    private static void logPass(String mensaje) {
        System.out.println("[PASS] " + mensaje);
    }

    private static void logFail(String mensaje) {
        System.out.println("[FAIL] " + mensaje);
    }
}
