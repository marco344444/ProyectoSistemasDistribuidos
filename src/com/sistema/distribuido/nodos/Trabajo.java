package com.sistema.distribuido.nodos;

public class Trabajo extends com.sistema.model.Trabajo {
    public Trabajo() {
        super();
    }

    public Trabajo(String idTrabajo, java.util.List<com.sistema.model.Archivo> imagenes, String rutaEntrada, String rutaSalida) {
        super(idTrabajo, imagenes, rutaEntrada, rutaSalida);
    }
}
