package com.sistema.comun.modelos;

// Enumeración de las transformaciones posibles mencionadas en el proyecto
public enum TipoTransformacion {
    ESCALA_GRISES,
    REDIMENSIONAR,
    RECORTAR,
    ROTAR,
    REFLEJAR,
    DESENFOCAR,
    PERFILAR,
    AJUSTE_BRILLO_CONTRASTE,
    MARCA_AGUA,
    CONVERTIR_FORMATO
}

// Definición de qué hacerle a una imagen específica
public class ConfiguracionImagen {
    private String nombreImagenOriginal;
    private byte[] datosImagen; // La imagen en sí (o su referencia)
    private List<TipoTransformacion> transformaciones;
    private Map<String, String> parametros; // Ej: {"grados": "90", "formato": "JPG"}
    
    // Getters y Setters...
}

// Lo que el Cliente envía al Servidor (Request Lote)
public class RequestLote {
    private String idUsuario;
    private List<ConfiguracionImagen> imagenes;
    
    // Getters y Setters...
}

// Lo que el Servidor envía al Nodo (Trabajo)
public class Trabajo {
    private String idTrabajo;
    private List<ConfiguracionImagen> imagenes; // El nodo descargará o recibirá los datos
    private String rutaEntrada; // Ruta donde el servidor ya guardó las imágenes originales
    private String rutaSalida;  // Ruta donde el nodo debe guardar los resultados
    
    // Getters y Setters...
}

// Lo que el Nodo devuelve al Servidor
public class ResultadoProcesamiento {
    private boolean exito;
    private String mensaje;
    private List<String> rutasArchivosGenerados; // Rutas en el almacenamiento del nodo
    private List<String> logsGenerados;          // Logs de la ejecución
    
    // Getters y Setters...
}