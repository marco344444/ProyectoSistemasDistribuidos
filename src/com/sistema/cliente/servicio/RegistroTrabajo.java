package com.sistema.cliente.servicio;

public class RegistroTrabajo extends com.sistema.model.RegistroTrabajo {
    public RegistroTrabajo() {
        super();
    }

    public RegistroTrabajo(String idTrabajo, String idUsuario, String estado, String fechaCreacion) {
        super(idTrabajo, idUsuario, estado, fechaCreacion);
    }
}
