package com.sistema.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Archivo implements Serializable {
    private String nombre;
    private byte[] contenido;
    private List<TipoTransformacion> transformaciones = new ArrayList<>();

    public Archivo() {
    }

    public Archivo(String nombre, byte[] contenido, List<TipoTransformacion> transformaciones) {
        this.nombre = nombre;
        this.contenido = contenido;
        if (transformaciones != null) {
            this.transformaciones = new ArrayList<>(transformaciones);
        }
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public List<TipoTransformacion> getTransformaciones() {
        return transformaciones;
    }

    public void setTransformaciones(List<TipoTransformacion> transformaciones) {
        this.transformaciones = transformaciones;
    }
}
