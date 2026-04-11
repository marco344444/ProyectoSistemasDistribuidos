package com.sistema.cliente.servicio;

public class EstadoLote extends com.sistema.model.EstadoLote {
    public EstadoLote() {
        super();
    }

    public EstadoLote(String idLote, String estado, int totalImagenes, int procesadas) {
        super(idLote, estado, totalImagenes, procesadas);
    }
}
