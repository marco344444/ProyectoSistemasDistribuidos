package com.sistema.cliente.servicio;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.util.List;

/**
 * Contrato SOAP expuesto al cliente.
 * Define las operaciones disponibles para el usuario final.
 */
@WebService
public interface IServicioProcesamientoImagenes {

    /**
     * Permite al usuario acceder al sistema.
     * @return Token de sesión o ID de usuario si es exitoso.
     */
    @WebMethod
    String iniciarSesion(@WebParam(name = "usuario") String usuario, 
                         @WebParam(name = "password") String password);

    /**
     * Envía un lote de imágenes para ser procesadas.
     * @param tokenSesion Token de autenticación.
     * @param lote Contiene las imágenes (en base64) y sus transformaciones.
     * @return ID único del trabajo (Job ID) para seguimiento.
     */
    @WebMethod
    String enviarLoteProcesamiento(@WebParam(name = "tokenSesion") String tokenSesion, 
                                   @WebParam(name = "lote") RequestLote lote);

    /**
     * Consulta el estado actual de un trabajo enviado.
     */
    @WebMethod
    EstadoLote consultarEstadoLote(@WebParam(name = "tokenSesion") String tokenSesion, 
                                   @WebParam(name = "idLote") String idLote);

    /**
     * Descarga una imagen procesada individual.
     * @return Arreglo de bytes de la imagen.
     */
    @WebMethod
    byte[] descargarImagen(@WebParam(name = "tokenSesion") String tokenSesion, 
                           @WebParam(name = "idResultado") String idResultado);

    /**
     * Descarga todas las imágenes de un lote en un ZIP.
     * @return Arreglo de bytes del archivo ZIP.
     */
    @WebMethod
    byte[] descargarLoteZip(@WebParam(name = "tokenSesion") String tokenSesion, 
                            @WebParam(name = "idLote") String idLote);
}