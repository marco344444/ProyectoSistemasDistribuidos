package com.sistema.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Trabajo implements Serializable {
    private String idTrabajo;
    private List<Archivo> imagenes = new ArrayList<>();
    private String rutaEntrada;
    private String rutaSalida;

    public Trabajo() {
    }

    public Trabajo(String idTrabajo, List<Archivo> imagenes, String rutaEntrada, String rutaSalida) {
        this.idTrabajo = idTrabajo;
        if (imagenes != null) {
            this.imagenes = new ArrayList<>(imagenes);
        }
        this.rutaEntrada = rutaEntrada;
        this.rutaSalida = rutaSalida;
    }

    public String getIdTrabajo() {
        return idTrabajo;
    }

    public void setIdTrabajo(String idTrabajo) {
        this.idTrabajo = idTrabajo;
    }

    public List<Archivo> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<Archivo> imagenes) {
        this.imagenes = imagenes;
    }

    public String getRutaEntrada() {
        return rutaEntrada;
    }

    public void setRutaEntrada(String rutaEntrada) {
        this.rutaEntrada = rutaEntrada;
    }

    public String getRutaSalida() {
        return rutaSalida;
    }

    public void setRutaSalida(String rutaSalida) {
        this.rutaSalida = rutaSalida;
    }
}
