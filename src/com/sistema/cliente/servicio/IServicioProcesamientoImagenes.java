package com.sistema.cliente.servicio;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface IServicioProcesamientoImagenes {

    @WebMethod
    String iniciarSesion(@WebParam(name = "usuario") String usuario,
                         @WebParam(name = "password") String password);

    @WebMethod
    String enviarLoteProcesamiento(@WebParam(name = "tokenSesion") String tokenSesion,
                                   @WebParam(name = "lote") RequestLote lote);

    @WebMethod
    EstadoLote consultarEstadoLote(@WebParam(name = "tokenSesion") String tokenSesion,
                                   @WebParam(name = "idLote") String idLote);

    @WebMethod
    byte[] descargarImagen(@WebParam(name = "tokenSesion") String tokenSesion,
                           @WebParam(name = "idResultado") String idResultado);

    @WebMethod
    byte[] descargarLoteZip(@WebParam(name = "tokenSesion") String tokenSesion,
                            @WebParam(name = "idLote") String idLote);
}
