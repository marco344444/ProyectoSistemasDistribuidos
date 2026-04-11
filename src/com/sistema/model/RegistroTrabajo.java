package com.sistema.model;

import java.io.Serializable;

public class RegistroTrabajo implements Serializable {
    private String idTrabajo;
    private String idUsuario;
    private String estado;
    private String fechaCreacion;

    public RegistroTrabajo() {
    }

    public RegistroTrabajo(String idTrabajo, String idUsuario, String estado, String fechaCreacion) {
        this.idTrabajo = idTrabajo;
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public String getIdTrabajo() {
        return idTrabajo;
    }

    public void setIdTrabajo(String idTrabajo) {
        this.idTrabajo = idTrabajo;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
