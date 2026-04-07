package com.sistema.cliente.servicio;

import javax.ws.rs.*;
import java.util.List;

/**
 * Contrato REST para la API de Base de Datos.
 * Usado internamente por el Servidor de Aplicación.
 */
@Path("/api/v1")
public interface IRepositorioDatos {

    // --- Gestión de Nodos ---
    
    @GET
    @Path("/nodos/activos")
    @Produces("application/json")
    List<InfoNodo> obtenerNodosActivos();

    @POST
    @Path("/nodos/registrar")
    @Consumes("application/json")
    void registrarNodo(InfoNodo nodo);

    // --- Gestión de Trabajos (Lotes) ---

    @POST
    @Path("/trabajos")
    @Consumes("application/json")
    String crearTrabajo(RegistroTrabajo trabajo);

    @PUT
    @Path("/trabajos/{id}/estado")
    @Consumes("application/json")
    void actualizarEstadoTrabajo(@PathParam("id") String idTrabajo, String nuevoEstado);

    @GET
    @Path("/trabajos/usuario/{idUsuario}")
    @Produces("application/json")
    List<RegistroTrabajo> obtenerHistorialUsuario(@PathParam("idUsuario") String idUsuario);

    // --- Logs y Auditoría ---

    @POST
    @Path("/logs")
    @Consumes("application/json")
    void guardarLog(RegistroLog log);
}