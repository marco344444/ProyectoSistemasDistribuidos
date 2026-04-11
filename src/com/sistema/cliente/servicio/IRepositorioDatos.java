package com.sistema.cliente.servicio;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

@Path("/api/v1")
public interface IRepositorioDatos {

    @GET
    @Path("/nodos/activos")
    @Produces("application/json")
    List<InfoNodo> obtenerNodosActivos();

    @POST
    @Path("/nodos/registrar")
    @Consumes("application/json")
    void registrarNodo(InfoNodo nodo);

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

    @POST
    @Path("/logs")
    @Consumes("application/json")
    void guardarLog(RegistroLog log);
}
