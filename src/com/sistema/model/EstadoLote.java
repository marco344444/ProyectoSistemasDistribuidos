package com.sistema.model;

import java.io.Serializable;

public class EstadoLote implements Serializable {
    private String idLote;
    private String estado;
    private int totalImagenes;
    private int procesadas;

    public EstadoLote() {
    }

    public EstadoLote(String idLote, String estado, int totalImagenes, int procesadas) {
        this.idLote = idLote;
        this.estado = estado;
        this.totalImagenes = totalImagenes;
        this.procesadas = procesadas;
    }

    public String getIdLote() {
        return idLote;
    }

    public void setIdLote(String idLote) {
        this.idLote = idLote;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getTotalImagenes() {
        return totalImagenes;
    }

    public void setTotalImagenes(int totalImagenes) {
        this.totalImagenes = totalImagenes;
    }

    public int getProcesadas() {
        return procesadas;
    }

    public void setProcesadas(int procesadas) {
        this.procesadas = procesadas;
    }
}
