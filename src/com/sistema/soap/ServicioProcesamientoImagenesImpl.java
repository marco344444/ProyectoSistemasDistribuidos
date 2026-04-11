package com.sistema.soap;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.IServicioProcesamientoImagenes;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServicioProcesamientoImagenesImpl implements IServicioProcesamientoImagenes {

    private final IRepositorioDatos repositorio;
    private final INodoTrabajador nodoTrabajador;
    private final Map<String, String> sesiones = new HashMap<>();

    public ServicioProcesamientoImagenesImpl(IRepositorioDatos repositorio, INodoTrabajador nodoTrabajador) {
        this.repositorio = repositorio;
        this.nodoTrabajador = nodoTrabajador;
    }

    @Override
    public String iniciarSesion(String usuario, String password) {
        String token = "TOKEN-" + UUID.randomUUID();
        sesiones.put(token, usuario);
        repositorio.guardarLog(new RegistroLog("LOGIN-" + System.currentTimeMillis(), "INFO", "Inicio de sesion para: " + usuario, LocalDateTime.now().toString()));
        return token;
    }

    @Override
    public String enviarLoteProcesamiento(String tokenSesion, RequestLote lote) {
        validarSesion(tokenSesion);
        String idTrabajo = "JOB-" + UUID.randomUUID();

        System.out.println("[SOAP] SOAP recibe solicitud para lote del usuario " + lote.getIdUsuario());
        repositorio.crearTrabajo(new RegistroTrabajo(idTrabajo, lote.getIdUsuario(), "RECIBIDO", LocalDateTime.now().toString()));

        Trabajo trabajo = new Trabajo();
        trabajo.setIdTrabajo(idTrabajo);
        trabajo.setImagenes(lote.getImagenes());
        trabajo.setRutaEntrada("/tmp/entrada/" + idTrabajo);
        trabajo.setRutaSalida("/tmp/salida/" + idTrabajo);

        try {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "EN_PROCESO");
            System.out.println("[Backend] Backend envía trabajo a nodo");
            ResultadoProcesamiento resultado = nodoTrabajador.procesarTrabajo(trabajo);

            if (resultado.isExito()) {
                repositorio.actualizarEstadoTrabajo(idTrabajo, "COMPLETADO");
            } else {
                repositorio.actualizarEstadoTrabajo(idTrabajo, "ERROR");
            }

            repositorio.guardarLog(new RegistroLog("R-" + System.currentTimeMillis(), "INFO", "Resultado: " + resultado.getMensaje(), LocalDateTime.now().toString()));
        } catch (Exception e) {
            repositorio.actualizarEstadoTrabajo(idTrabajo, "ERROR");
            repositorio.guardarLog(new RegistroLog("R-" + System.currentTimeMillis(), "ERROR", "Fallo RMI: " + e.getMessage(), LocalDateTime.now().toString()));
        }

        return idTrabajo;
    }

    @Override
    public EstadoLote consultarEstadoLote(String tokenSesion, String idLote) {
        validarSesion(tokenSesion);
        for (RegistroTrabajo t : repositorio.obtenerHistorialUsuario(sesiones.get(tokenSesion))) {
            if (idLote.equals(t.getIdTrabajo())) {
                return new EstadoLote(idLote, t.getEstado(), 0, 0);
            }
        }
        return new EstadoLote(idLote, "NO_ENCONTRADO", 0, 0);
    }

    @Override
    public byte[] descargarImagen(String tokenSesion, String idResultado) {
        validarSesion(tokenSesion);
        return ("Imagen simulada para resultado " + idResultado).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] descargarLoteZip(String tokenSesion, String idLote) {
        validarSesion(tokenSesion);
        return ("ZIP simulado del lote " + idLote).getBytes(StandardCharsets.UTF_8);
    }

    private void validarSesion(String tokenSesion) {
        if (!sesiones.containsKey(tokenSesion)) {
            throw new IllegalArgumentException("Sesion invalida");
        }
    }
}
