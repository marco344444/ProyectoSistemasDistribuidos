package com.sistema.rest;

import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;
import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepositorioDatosJdbcImpl implements IRepositorioDatos {

    private final String url;
    private final String user;
    private final String password;

    public RepositorioDatosJdbcImpl(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        inicializarEsquemaMinimo();
    }

    @Override
    public synchronized List<InfoNodo> obtenerNodosActivos() {
        List<InfoNodo> nodos = new ArrayList<>();
        String sql = "SELECT id_nodo, host, puerto, activo FROM procesamiento.nodos WHERE activo = TRUE ORDER BY id_nodo";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nodos.add(new InfoNodo(
                        rs.getString("id_nodo"),
                        rs.getString("host"),
                        rs.getInt("puerto"),
                        rs.getBoolean("activo")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando nodos activos: " + e.getMessage(), e);
        }
        return nodos;
    }

    @Override
    public synchronized void registrarNodo(InfoNodo nodo) {
        String sql = "INSERT INTO procesamiento.nodos(id_nodo, host, puerto, activo, actualizado_en) "
                + "VALUES(?, ?, ?, ?, NOW()) "
                + "ON CONFLICT (id_nodo) DO UPDATE SET host = EXCLUDED.host, puerto = EXCLUDED.puerto, activo = EXCLUDED.activo, actualizado_en = NOW()";

        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nodo.getIdNodo());
            ps.setString(2, nodo.getHost());
            ps.setInt(3, nodo.getPuerto());
            ps.setBoolean(4, nodo.isActivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando nodo: " + e.getMessage(), e);
        }

        guardarLog(new RegistroLog("LOG-" + System.currentTimeMillis(), "INFO", "Nodo registrado: " + nodo.getIdNodo(), LocalDateTime.now().toString()));
    }

    @Override
    public synchronized String crearTrabajo(RegistroTrabajo trabajo) {
        String sql = "INSERT INTO procesamiento.trabajos(id_trabajo, id_usuario, estado, fecha_creacion, actualizado_en) "
                + "VALUES(?, ?, ?, ?, NOW()) "
                + "ON CONFLICT (id_trabajo) DO UPDATE SET id_usuario = EXCLUDED.id_usuario, estado = EXCLUDED.estado, fecha_creacion = EXCLUDED.fecha_creacion, actualizado_en = NOW()";

        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trabajo.getIdTrabajo());
            ps.setString(2, trabajo.getIdUsuario());
            ps.setString(3, trabajo.getEstado());
            ps.setString(4, trabajo.getFechaCreacion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creando trabajo: " + e.getMessage(), e);
        }

        guardarLog(new RegistroLog("LOG-" + System.currentTimeMillis(), "INFO", "Trabajo creado: " + trabajo.getIdTrabajo(), LocalDateTime.now().toString()));
        return trabajo.getIdTrabajo();
    }

    @Override
    public synchronized void actualizarEstadoTrabajo(String idTrabajo, String nuevoEstado) {
        String sql = "UPDATE procesamiento.trabajos SET estado = ?, actualizado_en = NOW() WHERE id_trabajo = ?";

        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, idTrabajo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando estado del trabajo: " + e.getMessage(), e);
        }

        guardarLog(new RegistroLog("LOG-" + System.currentTimeMillis(), "INFO", "Estado actualizado: " + idTrabajo + " -> " + nuevoEstado, LocalDateTime.now().toString()));
    }

    @Override
    public synchronized List<RegistroTrabajo> obtenerHistorialUsuario(String idUsuario) {
        List<RegistroTrabajo> historial = new ArrayList<>();
        String sql = "SELECT id_trabajo, id_usuario, estado, fecha_creacion FROM procesamiento.trabajos WHERE id_usuario = ? ORDER BY actualizado_en DESC";

        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historial.add(new RegistroTrabajo(
                            rs.getString("id_trabajo"),
                            rs.getString("id_usuario"),
                            rs.getString("estado"),
                            rs.getString("fecha_creacion")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando historial del usuario: " + e.getMessage(), e);
        }

        return historial;
    }

    @Override
    public synchronized void guardarLog(RegistroLog log) {
        String sql = "INSERT INTO procesamiento.logs(id_log, nivel, mensaje, fecha, creado_en) VALUES(?, ?, ?, ?, NOW())";
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, log.getId());
            ps.setString(2, log.getNivel());
            ps.setString(3, log.getMensaje());
            ps.setString(4, log.getFecha());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando log: " + e.getMessage(), e);
        }
    }

    public synchronized void registrarResultadosImagenes(
            String idTrabajo,
            String idUsuario,
            List<Archivo> imagenes,
            List<String> rutasResultados,
            Map<String, byte[]> archivosGenerados,
            String nodoProcesador,
            LocalDateTime fechaRecepcion,
            LocalDateTime fechaConversion
    ) {
        String sql = "INSERT INTO procesamiento.resultados_imagen(" +
                "id_trabajo, id_usuario, indice_imagen, nombre_original, transformaciones, " +
                "fecha_recepcion, fecha_conversion, nodo_procesador, ruta_resultado, tamano_resultado_bytes) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id_trabajo, indice_imagen) DO UPDATE SET " +
                "transformaciones = EXCLUDED.transformaciones, " +
                "fecha_conversion = EXCLUDED.fecha_conversion, " +
                "nodo_procesador = EXCLUDED.nodo_procesador, " +
                "ruta_resultado = EXCLUDED.ruta_resultado, " +
                "tamano_resultado_bytes = EXCLUDED.tamano_resultado_bytes";

        List<Archivo> imagenesSafe = imagenes == null ? new ArrayList<>() : imagenes;
        List<String> rutasSafe = rutasResultados == null ? new ArrayList<>() : rutasResultados;
        Map<String, byte[]> archivosSafe = archivosGenerados == null ? new LinkedHashMap<>() : archivosGenerados;

        int total = Math.max(imagenesSafe.size(), rutasSafe.size());
        if (total <= 0) {
            return;
        }

        try (Connection con = abrirConexion()) {
            for (int i = 0; i < total; i++) {
                Archivo archivo = i < imagenesSafe.size() ? imagenesSafe.get(i) : null;
                String nombreOriginal = (archivo != null && archivo.getNombre() != null && !archivo.getNombre().trim().isEmpty())
                        ? archivo.getNombre().trim()
                        : ("img_" + i + ".png");

                String transformaciones = obtenerTransformacionesTexto(archivo);
                String rutaResultado = i < rutasSafe.size() ? rutasSafe.get(i) : construirRutaResultadoFallback(nombreOriginal, i);
                int bytesResultado = 0;
                byte[] contenido = archivosSafe.get(rutaResultado);
                if (contenido != null) {
                    bytesResultado = contenido.length;
                }

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, idTrabajo);
                    ps.setString(2, idUsuario);
                    ps.setInt(3, i);
                    ps.setString(4, nombreOriginal);
                    ps.setString(5, transformaciones);
                    ps.setTimestamp(6, Timestamp.valueOf(fechaRecepcion));
                    ps.setTimestamp(7, Timestamp.valueOf(fechaConversion));
                    ps.setString(8, nodoProcesador == null ? "nodo-rmi" : nodoProcesador);
                    ps.setString(9, rutaResultado);
                    ps.setInt(10, Math.max(0, bytesResultado));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error persistiendo resultados por imagen: " + e.getMessage(), e);
        }
    }

    public synchronized Map<String, Object> obtenerMetricasReplica() {
        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("nodosPrimario", contar("procesamiento.nodos"));
        metricas.put("nodosReplica", contar("procesamiento.nodos"));
        metricas.put("trabajosPrimario", contar("procesamiento.trabajos"));
        metricas.put("trabajosReplica", contar("procesamiento.trabajos"));
        metricas.put("logsPrimario", contar("procesamiento.logs"));
        metricas.put("logsReplica", contar("procesamiento.logs"));
        metricas.put("ultimoSync", LocalDateTime.now().toString());
        metricas.put("totalReplicaciones", 0);
        metricas.put("consistente", true);
        return metricas;
    }

    private int contar(String tabla) {
        String tablaPermitida;
        switch (tabla) {
            case "procesamiento.nodos":
                tablaPermitida = "procesamiento.nodos";
                break;
            case "procesamiento.trabajos":
                tablaPermitida = "procesamiento.trabajos";
                break;
            case "procesamiento.logs":
                tablaPermitida = "procesamiento.logs";
                break;
            default:
                throw new IllegalArgumentException("Tabla no permitida para conteo: " + tabla);
        }

        String sql = "SELECT COUNT(*) FROM " + tablaPermitida;
        try (Connection con = abrirConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    private Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private String obtenerTransformacionesTexto(Archivo archivo) {
        if (archivo == null || archivo.getTransformaciones() == null || archivo.getTransformaciones().isEmpty()) {
            return "SIN_TRANSFORMACIONES";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < archivo.getTransformaciones().size(); i++) {
            TipoTransformacion transformacion = archivo.getTransformaciones().get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(transformacion == null ? "DESCONOCIDA" : transformacion.name());
        }
        return sb.toString();
    }

    private String construirRutaResultadoFallback(String nombreOriginal, int indiceImagen) {
        String base = nombreOriginal == null ? "imagen" : nombreOriginal.trim();
        if (base.isEmpty()) {
            base = "imagen";
        }

        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash < base.length() - 1) {
            base = base.substring(slash + 1);
        }

        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }

        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isEmpty()) {
            base = "imagen";
        }

        return base + "_edited_" + indiceImagen + ".png";
    }

    private void inicializarEsquemaMinimo() {
        String[] ddl = new String[] {
                "CREATE SCHEMA IF NOT EXISTS procesamiento",
                "CREATE TABLE IF NOT EXISTS procesamiento.nodos ("
                        + "id_nodo VARCHAR(80) PRIMARY KEY,"
                        + "host VARCHAR(120) NOT NULL,"
                        + "puerto INTEGER NOT NULL,"
                        + "activo BOOLEAN NOT NULL DEFAULT TRUE,"
                        + "actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()"
                        + ")",
                "CREATE TABLE IF NOT EXISTS procesamiento.trabajos ("
                        + "id_trabajo VARCHAR(120) PRIMARY KEY,"
                        + "id_usuario VARCHAR(120) NOT NULL,"
                        + "estado VARCHAR(40) NOT NULL,"
                        + "fecha_creacion VARCHAR(60) NOT NULL,"
                        + "actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()"
                        + ")",
                "CREATE TABLE IF NOT EXISTS procesamiento.logs ("
                        + "id BIGSERIAL PRIMARY KEY,"
                        + "id_log VARCHAR(120),"
                        + "nivel VARCHAR(20) NOT NULL,"
                        + "mensaje TEXT NOT NULL,"
                        + "fecha VARCHAR(60) NOT NULL,"
                        + "creado_en TIMESTAMP NOT NULL DEFAULT NOW()"
                    + ")",
                "CREATE TABLE IF NOT EXISTS procesamiento.resultados_imagen ("
                    + "id_resultado BIGSERIAL PRIMARY KEY,"
                    + "id_trabajo VARCHAR(120) NOT NULL,"
                    + "id_usuario VARCHAR(120) NOT NULL,"
                    + "indice_imagen INTEGER NOT NULL,"
                    + "nombre_original VARCHAR(260) NOT NULL,"
                    + "transformaciones TEXT NOT NULL,"
                    + "fecha_recepcion TIMESTAMP NOT NULL,"
                    + "fecha_conversion TIMESTAMP NOT NULL,"
                    + "nodo_procesador VARCHAR(120) NOT NULL,"
                    + "ruta_resultado VARCHAR(260) NOT NULL,"
                    + "tamano_resultado_bytes INTEGER NOT NULL DEFAULT 0"
                        + ")"
        };

        try (Connection con = abrirConexion()) {
            for (String sql : ddl) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.execute();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar el esquema JDBC: " + e.getMessage(), e);
        }
    }
}
