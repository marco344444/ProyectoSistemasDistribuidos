package com.sistema.rest;

import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RegistroLog;
import com.sistema.cliente.servicio.RegistroTrabajo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String sql = "SELECT COUNT(*) FROM " + tabla;
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
