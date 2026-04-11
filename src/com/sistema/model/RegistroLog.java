package com.sistema.model;

import java.io.Serializable;

public class RegistroLog implements Serializable {
    private String id;
    private String nivel;
    private String mensaje;
    private String fecha;

    public RegistroLog() {
    }

    public RegistroLog(String id, String nivel, String mensaje, String fecha) {
        this.id = id;
        this.nivel = nivel;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
