package com.sistema.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RequestLote implements Serializable {
    private String idUsuario;
    private List<Archivo> imagenes = new ArrayList<>();

    public RequestLote() {
    }

    public RequestLote(String idUsuario, List<Archivo> imagenes) {
        this.idUsuario = idUsuario;
        if (imagenes != null) {
            this.imagenes = new ArrayList<>(imagenes);
        }
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public List<Archivo> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<Archivo> imagenes) {
        this.imagenes = imagenes;
    }
}
