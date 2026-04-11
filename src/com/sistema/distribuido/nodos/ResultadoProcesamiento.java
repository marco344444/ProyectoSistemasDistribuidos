package com.sistema.distribuido.nodos;

import java.util.List;

public class ResultadoProcesamiento extends com.sistema.model.ResultadoProcesamiento {
    public ResultadoProcesamiento() {
        super();
    }

    public ResultadoProcesamiento(boolean exito, String mensaje, List<String> rutasArchivosGenerados, List<String> logsGenerados) {
        super(exito, mensaje, rutasArchivosGenerados, logsGenerados);
    }
}
