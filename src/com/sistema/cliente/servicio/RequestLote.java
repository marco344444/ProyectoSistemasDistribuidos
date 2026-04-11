package com.sistema.cliente.servicio;

public class RequestLote extends com.sistema.model.RequestLote {
    public RequestLote() {
        super();
    }

    public RequestLote(String idUsuario, java.util.List<com.sistema.model.Archivo> imagenes) {
        super(idUsuario, imagenes);
    }
}
