package com.sistema.model;

import java.io.Serializable;

public class InfoNodo implements Serializable {
    private String idNodo;
    private String host;
    private int puerto;
    private boolean activo;

    public InfoNodo() {
    }

    public InfoNodo(String idNodo, String host, int puerto, boolean activo) {
        this.idNodo = idNodo;
        this.host = host;
        this.puerto = puerto;
        this.activo = activo;
    }

    public String getIdNodo() {
        return idNodo;
    }

    public void setIdNodo(String idNodo) {
        this.idNodo = idNodo;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
