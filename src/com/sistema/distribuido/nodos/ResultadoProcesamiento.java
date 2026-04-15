package com.sistema.distribuido.nodos;

import java.util.List;
import java.util.Map;

public class ResultadoProcesamiento extends com.sistema.model.ResultadoProcesamiento {
    public ResultadoProcesamiento() {
        super();
    }

    public ResultadoProcesamiento(boolean exito, String mensaje, List<String> rutasArchivosGenerados, List<String> logsGenerados) {
        super(exito, mensaje, rutasArchivosGenerados, logsGenerados);
    }

    public ResultadoProcesamiento(boolean exito, String mensaje, List<String> rutasArchivosGenerados, List<String> logsGenerados, Map<String, byte[]> archivosGenerados) {
        super(exito, mensaje, rutasArchivosGenerados, logsGenerados, archivosGenerados);
    }
}
