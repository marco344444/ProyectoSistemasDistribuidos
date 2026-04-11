package com.sistema.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResultadoProcesamiento implements Serializable {
    private boolean exito;
    private String mensaje;
    private List<String> rutasArchivosGenerados = new ArrayList<>();
    private List<String> logsGenerados = new ArrayList<>();

    public ResultadoProcesamiento() {
    }

    public ResultadoProcesamiento(boolean exito, String mensaje, List<String> rutasArchivosGenerados, List<String> logsGenerados) {
        this.exito = exito;
        this.mensaje = mensaje;
        if (rutasArchivosGenerados != null) {
            this.rutasArchivosGenerados = new ArrayList<>(rutasArchivosGenerados);
        }
        if (logsGenerados != null) {
            this.logsGenerados = new ArrayList<>(logsGenerados);
        }
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<String> getRutasArchivosGenerados() {
        return rutasArchivosGenerados;
    }

    public void setRutasArchivosGenerados(List<String> rutasArchivosGenerados) {
        this.rutasArchivosGenerados = rutasArchivosGenerados;
    }

    public List<String> getLogsGenerados() {
        return logsGenerados;
    }

    public void setLogsGenerados(List<String> logsGenerados) {
        this.logsGenerados = logsGenerados;
    }
}
