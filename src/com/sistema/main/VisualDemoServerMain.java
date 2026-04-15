package com.sistema.main;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.IRepositorioDatos;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;
import com.sistema.rest.RepositorioDatosFactory;
import com.sistema.rest.RepositorioDatosImpl;
import com.sistema.rest.RepositorioDatosJdbcImpl;
import com.sistema.rmi.NodoTrabajadorImpl;
import com.sistema.rmi.ServidorRmiMain;
import com.sistema.soap.ServicioProcesamientoImagenesImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class VisualDemoServerMain {

    private static final List<LoteDemo> LOTES = new CopyOnWriteArrayList<>();
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void main(String[] args) {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(1099);
            }

            NodoTrabajadorImpl nodoVisual = new NodoTrabajadorImpl("worker-visual");
            registry.rebind(ServidorRmiMain.NOMBRE_BIND, nodoVisual);

            IRepositorioDatos repositorio = RepositorioDatosFactory.crearRepositorio();
            if (!(repositorio instanceof RepositorioDatosJdbcImpl)) {
                throw new IllegalStateException("Este servidor visual requiere PostgreSQL/JDBC activo (DB_URL, DB_USER, DB_PASSWORD)");
            }

            dbUrl = leerConfiguracion("DB_URL", "db.url", "");
            dbUser = leerConfiguracion("DB_USER", "db.user", "imageproc");
            dbPassword = leerConfiguracion("DB_PASSWORD", "db.password", "imageproc123");
            validarConexionBaseDatos();

            repositorio.registrarNodo(new InfoNodo("worker-visual", "localhost", 1099, true));

            ServicioProcesamientoImagenesImpl soap = new ServicioProcesamientoImagenesImpl(
                    repositorio,
                    (com.sistema.distribuido.nodos.INodoTrabajador) registry.lookup(ServidorRmiMain.NOMBRE_BIND)
            );

            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);

            httpServer.createContext("/api/health", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                sendJson(exchange, 200, "{\"ok\":true,\"mensaje\":\"Servidor visual activo\"}");
            });

            httpServer.createContext("/api/login", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String usuario = params.getOrDefault("usuario", "").trim();
                String password = params.getOrDefault("password", "").trim();

                if (usuario.isEmpty() || password.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"usuario y password son obligatorios\"}");
                    return;
                }

                UsuarioDb usuarioEncontrado;
                try {
                    usuarioEncontrado = buscarUsuarioValido(usuario, password);
                } catch (SQLException e) {
                    sendJson(exchange, 500, "{\"error\":\"No se pudo validar credenciales en base de datos\"}");
                    return;
                }

                if (usuarioEncontrado == null) {
                    sendJson(exchange, 401, "{\"error\":\"Credenciales invalidas\"}");
                    return;
                }

                String token = soap.iniciarSesion(usuarioEncontrado.idUsuario, password);
                try {
                    guardarSesionActiva(token, usuarioEncontrado.idUsuario);
                } catch (SQLException e) {
                    System.err.println("No se pudo persistir sesion en BD: " + e.getMessage());
                }

                String json = "{\"token\":\"" + jsonEscape(token) + "\",\"usuario\":\"" + jsonEscape(usuarioEncontrado.correo) + "\",\"nombre\":\""
                        + jsonEscape(usuarioEncontrado.nombres + " " + usuarioEncontrado.apellidos) + "\"}";
                sendJson(exchange, 200, json);
            });

            httpServer.createContext("/api/registro", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String nombres = params.getOrDefault("nombres", "").trim();
                String apellidos = params.getOrDefault("apellidos", "").trim();
                String cedula = params.getOrDefault("cedula", "").trim();
                String correo = params.getOrDefault("correo", "").trim().toLowerCase();
                String password = params.getOrDefault("password", "").trim();

                if (nombres.isEmpty() || apellidos.isEmpty() || cedula.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"Todos los campos son obligatorios\"}");
                    return;
                }

                try {
                    registrarUsuario(nombres, apellidos, cedula, correo, password);
                } catch (SQLException e) {
                    if (esConflictoUnicidad(e)) {
                        sendJson(exchange, 409, "{\"error\":\"El correo o la cedula ya estan registrados\"}");
                    } else {
                        sendJson(exchange, 500, "{\"error\":\"No se pudo guardar el registro en base de datos\"}");
                    }
                    return;
                }

                String json = "{\"ok\":true,\"correo\":\"" + jsonEscape(correo) + "\",\"mensaje\":\"Registro exitoso\"}";
                sendJson(exchange, 200, json);
            });

            httpServer.createContext("/api/enviarLote", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String token = params.getOrDefault("token", "");
                String usuario = params.getOrDefault("usuario", "usuario-demo");
                int cantidad = parseInt(params.get("cantidad"), 3);
                cantidad = Math.max(1, Math.min(400, cantidad));

                RequestLote lote = construirLote(usuario, cantidad);
                String idLote = soap.enviarLoteProcesamiento(token, lote);

                LoteDemo loteDemo = new LoteDemo();
                loteDemo.idLote = idLote;
                loteDemo.usuario = usuario;
                loteDemo.cantidad = cantidad;
                loteDemo.estado = "COMPLETADO";
                loteDemo.fecha = String.valueOf(System.currentTimeMillis());
                loteDemo.nodos = "worker-visual";
                loteDemo.duracionMs = soap.obtenerDuracionLoteMs(idLote);
                loteDemo.archivos = soap.obtenerResultadosLote(idLote);
                loteDemo.logs = soap.obtenerLogsLote(idLote);
                loteDemo.transformaciones = Arrays.asList("ESCALA_GRISES", "ROTAR");
                if (loteDemo.archivos.isEmpty()) {
                    loteDemo.archivos = new ArrayList<>();
                    for (int i = 0; i < cantidad; i++) {
                        loteDemo.archivos.add(idLote + "_img_" + i + "_procesada.png");
                    }
                }
                LOTES.add(loteDemo);

                String json = "{\"idLote\":\"" + jsonEscape(idLote) + "\",\"cantidad\":" + cantidad + "}";
                sendJson(exchange, 200, json);
            });

            httpServer.createContext("/api/enviarLoteArchivos", exchange -> {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendOptions(exchange, "POST, OPTIONS");
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }
                try {
                    String body = leerBody(exchange);
                    String token = extraerCampoJson(body, "token");
                    String usuario = extraerCampoJson(body, "usuario");
                    List<Archivo> archivos = extraerArchivosDesdeJson(body);

                    if (token.isEmpty() || usuario.isEmpty() || archivos.isEmpty()) {
                        sendJson(exchange, 400, "{\"error\":\"token, usuario y archivos son obligatorios\"}");
                        return;
                    }

                    RequestLote lote = new RequestLote(usuario, archivos);
                    String idLote = soap.enviarLoteProcesamiento(token, lote);

                    LoteDemo loteDemo = new LoteDemo();
                    loteDemo.idLote = idLote;
                    loteDemo.usuario = usuario;
                    loteDemo.cantidad = archivos.size();
                    loteDemo.estado = "COMPLETADO";
                    loteDemo.fecha = String.valueOf(System.currentTimeMillis());
                    loteDemo.nodos = "worker-visual";
                    loteDemo.duracionMs = soap.obtenerDuracionLoteMs(idLote);
                    loteDemo.archivos = soap.obtenerResultadosLote(idLote);
                    loteDemo.logs = soap.obtenerLogsLote(idLote);
                    loteDemo.transformaciones = obtenerTransformacionesLote(archivos);
                    LOTES.add(loteDemo);

                    String json = "{\"idLote\":\"" + jsonEscape(idLote) + "\",\"cantidad\":" + archivos.size() + "}";
                    sendJson(exchange, 200, json);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJson(exchange, 500, "{\"error\":\"Fallo procesando lote con archivos: " + jsonEscape(e.getMessage()) + "\"}");
                }
            });

            httpServer.createContext("/api/estado", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String token = params.getOrDefault("token", "");
                String idLote = params.getOrDefault("idLote", "");

                EstadoLote estado = soap.consultarEstadoLote(token, idLote);
                LoteDemo lote = buscarLote(idLote);
                String json = "{\"idLote\":\"" + jsonEscape(estado.getIdLote()) + "\",\"estado\":\"" + jsonEscape(estado.getEstado())
                    + "\",\"total\":" + estado.getTotalImagenes() + ",\"procesadas\":" + estado.getProcesadas()
                    + ",\"duracionMs\":" + (lote == null ? 0 : lote.duracionMs)
                    + ",\"logs\":" + toJsonArray(lote == null ? Collections.emptyList() : lote.logs)
                    + ",\"transformaciones\":" + toJsonArray(lote == null ? Collections.emptyList() : lote.transformaciones)
                    + "}";
                sendJson(exchange, 200, json);
            });

            httpServer.createContext("/api/historial", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String usuario = params.getOrDefault("usuario", "usuario-demo");
                List<LoteDemo> filtrados = new ArrayList<>();
                for (LoteDemo lote : LOTES) {
                    if (usuario.equals(lote.usuario)) {
                        filtrados.add(lote);
                    }
                }
                filtrados.sort(Comparator.comparingLong(a -> -Long.parseLong(a.fecha)));

                int totalImagenes = 0;
                int completados = 0;
                for (LoteDemo lote : filtrados) {
                    totalImagenes += lote.cantidad;
                    if ("COMPLETADO".equals(lote.estado)) {
                        completados++;
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"totalLotes\":").append(filtrados.size())
                        .append(",\"completados\":").append(completados)
                        .append(",\"enProgreso\":").append(Math.max(0, filtrados.size() - completados))
                        .append(",\"totalImagenes\":").append(totalImagenes)
                        .append(",\"lotes\":[");

                for (int i = 0; i < filtrados.size(); i++) {
                    LoteDemo lote = filtrados.get(i);
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append("{\"idLote\":\"").append(jsonEscape(lote.idLote)).append("\"")
                            .append(",\"fecha\":\"").append(jsonEscape(lote.fecha)).append("\"")
                            .append(",\"imagenes\":").append(lote.cantidad)
                            .append(",\"estado\":\"").append(jsonEscape(lote.estado)).append("\"")
                            .append(",\"duracionMs\":").append(lote.duracionMs)
                            .append(",\"nodos\":\"").append(jsonEscape(lote.nodos)).append("\"}");
                }
                sb.append("]}");
                sendJson(exchange, 200, sb.toString());
            });

            httpServer.createContext("/api/descargas", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String idLote = params.getOrDefault("idLote", "");
                LoteDemo lote = buscarLote(idLote);
                if (lote == null) {
                    sendJson(exchange, 404, "{\"error\":\"Lote no encontrado\"}");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"idLote\":\"").append(jsonEscape(lote.idLote)).append("\"")
                        .append(",\"estado\":\"").append(jsonEscape(lote.estado)).append("\"")
                        .append(",\"cantidad\":").append(lote.cantidad)
                        .append(",\"archivos\":[");
                for (int i = 0; i < lote.archivos.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    String nombre = lote.archivos.get(i);
                    sb.append("{\"nombre\":\"").append(jsonEscape(nombre)).append("\",\"tamKb\":")
                            .append(80 + (i * 12)).append(",\"listo\":true}");
                }
                sb.append("]}");
                sendJson(exchange, 200, sb.toString());
            });

            httpServer.createContext("/api/descargarImagen", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String token = params.getOrDefault("token", "");
                String idResultado = params.getOrDefault("idResultado", "");

                if (token.isEmpty() || idResultado.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"token e idResultado son obligatorios\"}");
                    return;
                }

                byte[] contenido = soap.descargarImagen(token, idResultado);
                sendBinary(exchange, 200, "image/png", idResultado, contenido);
            });

            httpServer.createContext("/api/metricas", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> params = parseQuery(exchange.getRequestURI());
                String usuario = params.getOrDefault("usuario", "usuario-demo");

                int lotesUsuario = 0;
                int imagenesUsuario = 0;
                for (LoteDemo lote : LOTES) {
                    if (usuario.equals(lote.usuario)) {
                        lotesUsuario++;
                        imagenesUsuario += lote.cantidad;
                    }
                }

                Runtime runtime = Runtime.getRuntime();
                long memoriaUsadaMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

                Map<String, Object> metricasNodo = nodoVisual.obtenerMetricasConsumo();
                Map<String, Object> replica = obtenerMetricasReplica(repositorio);

                String json = "{"
                        + "\"usuario\":\"" + jsonEscape(usuario) + "\","
                        + "\"lotesUsuario\":" + lotesUsuario + ","
                        + "\"imagenesUsuario\":" + imagenesUsuario + ","
                        + "\"cpuNuclei\":" + runtime.availableProcessors() + ","
                        + "\"memoriaUsadaMb\":" + memoriaUsadaMb + ","
                        + "\"nodo\":{"
                        + "\"id\":\"" + jsonEscape(String.valueOf(metricasNodo.get("idNodo"))) + "\","
                        + "\"trabajosProcesados\":" + metricasNodo.get("trabajosProcesados") + ","
                        + "\"imagenesProcesadas\":" + metricasNodo.get("imagenesProcesadas") + ","
                        + "\"tiempoPromedioMs\":" + metricasNodo.get("tiempoPromedioMs") + ","
                        + "\"maxHilosParalelos\":" + metricasNodo.get("maxHilosParalelos") + ","
                        + "\"cargaActual\":" + metricasNodo.get("cargaActual")
                        + "},"
                        + "\"replica\":{\"nodosPrimario\":" + replica.get("nodosPrimario")
                        + ",\"nodosReplica\":" + replica.get("nodosReplica")
                        + ",\"trabajosPrimario\":" + replica.get("trabajosPrimario")
                        + ",\"trabajosReplica\":" + replica.get("trabajosReplica")
                        + ",\"logsPrimario\":" + replica.get("logsPrimario")
                        + ",\"logsReplica\":" + replica.get("logsReplica")
                        + ",\"totalReplicaciones\":" + replica.get("totalReplicaciones")
                        + ",\"consistente\":" + replica.get("consistente")
                        + ",\"ultimoSync\":\"" + jsonEscape(String.valueOf(replica.get("ultimoSync"))) + "\"}"
                        + "}";
                sendJson(exchange, 200, json);
            });

            httpServer.createContext("/api/replica", exchange -> {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                    return;
                }

                Map<String, Object> replica = obtenerMetricasReplica(repositorio);
                String json = "{"
                        + "\"nodosPrimario\":" + replica.get("nodosPrimario")
                        + ",\"nodosReplica\":" + replica.get("nodosReplica")
                        + ",\"trabajosPrimario\":" + replica.get("trabajosPrimario")
                        + ",\"trabajosReplica\":" + replica.get("trabajosReplica")
                        + ",\"logsPrimario\":" + replica.get("logsPrimario")
                        + ",\"logsReplica\":" + replica.get("logsReplica")
                        + ",\"totalReplicaciones\":" + replica.get("totalReplicaciones")
                        + ",\"consistente\":" + replica.get("consistente")
                        + ",\"ultimoSync\":\"" + jsonEscape(String.valueOf(replica.get("ultimoSync"))) + "\""
                        + "}";
                sendJson(exchange, 200, json);
            });

            httpServer.setExecutor(null);
            httpServer.start();

            System.out.println("Servidor visual iniciado en http://localhost:8080");
            System.out.println("Health: http://localhost:8080/api/health");
            System.out.println("Abre http://localhost:5173 para iniciar el flujo visual.");
        } catch (Exception e) {
            System.err.println("Error iniciando servidor visual: " + e.getMessage());
        }
    }

    private static RequestLote construirLote(String usuario, int cantidad) {
        List<Archivo> imagenes = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            imagenes.add(new Archivo(
                    "img_" + i + ".jpg",
                    ("contenido_" + i).getBytes(StandardCharsets.UTF_8),
                    Arrays.asList(TipoTransformacion.ESCALA_GRISES, TipoTransformacion.ROTAR)
            ));
        }
        return new RequestLote(usuario, imagenes);
    }

    private static LoteDemo buscarLote(String idLote) {
        for (LoteDemo lote : LOTES) {
            if (lote.idLote.equals(idLote)) {
                return lote;
            }
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        try {
            if (value == null) {
                return fallback;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String leerBody(HttpExchange exchange) {
        try {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String extraerCampoJson(String json, String campo) {
        if (json == null || json.isEmpty() || campo == null || campo.isEmpty()) {
            return "";
        }
        String marker = "\"" + campo + "\"";
        int markerIdx = json.indexOf(marker);
        if (markerIdx < 0) {
            return "";
        }
        int colonIdx = json.indexOf(':', markerIdx + marker.length());
        if (colonIdx < 0) {
            return "";
        }
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) {
            return "";
        }
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            return "";
        }
        return jsonUnescape(json.substring(quoteStart + 1, quoteEnd));
    }

    private static List<Archivo> extraerArchivosDesdeJson(String json) {
        List<Archivo> archivos = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return archivos;
        }

        int cursor = 0;
        while (cursor >= 0 && cursor < json.length()) {
            int nombreKey = json.indexOf("\"nombre\"", cursor);
            if (nombreKey < 0) {
                break;
            }

            int nombreColon = json.indexOf(':', nombreKey);
            int nombreStart = json.indexOf('"', nombreColon + 1);
            int nombreEnd = nombreStart < 0 ? -1 : json.indexOf('"', nombreStart + 1);
            if (nombreStart < 0 || nombreEnd < 0) {
                break;
            }

            int contenidoKey = json.indexOf("\"contenidoBase64\"", nombreEnd);
            if (contenidoKey < 0) {
                break;
            }

            int objectStart = json.lastIndexOf('{', nombreKey);
            int objectEnd = json.indexOf('}', contenidoKey);
            String bloqueArchivo = (objectStart >= 0 && objectEnd > objectStart)
                    ? json.substring(objectStart, objectEnd + 1)
                    : "";

            int contenidoColon = json.indexOf(':', contenidoKey);
            int contenidoStart = json.indexOf('"', contenidoColon + 1);
            int contenidoEnd = contenidoStart < 0 ? -1 : json.indexOf('"', contenidoStart + 1);
            if (contenidoStart < 0 || contenidoEnd < 0) {
                break;
            }

            String nombre = jsonUnescape(json.substring(nombreStart + 1, nombreEnd));
            String base64 = jsonUnescape(json.substring(contenidoStart + 1, contenidoEnd));
            List<TipoTransformacion> transformaciones = extraerTransformacionesDesdeBloque(bloqueArchivo);
            if (transformaciones.isEmpty()) {
                transformaciones = Arrays.asList(TipoTransformacion.ESCALA_GRISES, TipoTransformacion.ROTAR, TipoTransformacion.MARCA_AGUA);
            }
            try {
                byte[] contenido = Base64.getDecoder().decode(base64);
                archivos.add(new Archivo(
                        nombre,
                        contenido,
                        transformaciones
                ));
            } catch (IllegalArgumentException ignored) {
                // ignorar archivo malformado
            }

            cursor = contenidoEnd + 1;
        }

        return archivos;
    }

    private static List<TipoTransformacion> extraerTransformacionesDesdeBloque(String bloqueArchivo) {
        if (bloqueArchivo == null || bloqueArchivo.isEmpty()) {
            return Collections.emptyList();
        }

        int key = bloqueArchivo.indexOf("\"transformaciones\"");
        if (key < 0) {
            return Collections.emptyList();
        }
        int arrayStart = bloqueArchivo.indexOf('[', key);
        int arrayEnd = arrayStart < 0 ? -1 : bloqueArchivo.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0 || arrayEnd <= arrayStart) {
            return Collections.emptyList();
        }

        String contenido = bloqueArchivo.substring(arrayStart + 1, arrayEnd).trim();
        if (contenido.isEmpty()) {
            return Collections.emptyList();
        }

        List<TipoTransformacion> transformaciones = new ArrayList<>();
        String[] tokens = contenido.split(",");
        for (String token : tokens) {
            String raw = token.trim();
            if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                raw = raw.substring(1, raw.length() - 1);
            }
            String nombre = jsonUnescape(raw).trim().toUpperCase();
            try {
                transformaciones.add(TipoTransformacion.valueOf(nombre));
            } catch (IllegalArgumentException ignored) {
                // ignorar transformacion no valida
            }
        }
        return transformaciones;
    }

    private static List<String> obtenerTransformacionesLote(List<Archivo> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return Collections.emptyList();
        }
        Archivo primero = archivos.get(0);
        if (primero.getTransformaciones() == null || primero.getTransformaciones().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> nombres = new ArrayList<>();
        for (TipoTransformacion transformacion : primero.getTransformaciones()) {
            nombres.add(transformacion.name());
        }
        return nombres;
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(jsonEscape(values.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static Map<String, Object> obtenerMetricasReplica(IRepositorioDatos repositorio) {
        if (repositorio instanceof RepositorioDatosImpl) {
            return ((RepositorioDatosImpl) repositorio).obtenerMetricasReplica();
        }
        if (repositorio instanceof RepositorioDatosJdbcImpl) {
            return ((RepositorioDatosJdbcImpl) repositorio).obtenerMetricasReplica();
        }

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("nodosPrimario", 0);
        metricas.put("nodosReplica", 0);
        metricas.put("trabajosPrimario", 0);
        metricas.put("trabajosReplica", 0);
        metricas.put("logsPrimario", 0);
        metricas.put("logsReplica", 0);
        metricas.put("ultimoSync", "N/A");
        metricas.put("totalReplicaciones", 0);
        metricas.put("consistente", false);
        return metricas;
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(exchange, "GET, POST, OPTIONS");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendBinary(HttpExchange exchange, int statusCode, String contentType, String fileName, byte[] bytes) throws IOException {
        byte[] payload = bytes == null ? new byte[0] : bytes;
        exchange.getResponseHeaders().add("Content-Type", contentType);
        addCorsHeaders(exchange, "GET, OPTIONS");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + jsonEscape(fileName) + "\"");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(payload);
        }
    }

    private static void sendOptions(HttpExchange exchange, String allowMethods) throws IOException {
        addCorsHeaders(exchange, allowMethods);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static void addCorsHeaders(HttpExchange exchange, String allowMethods) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", allowMethods);
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonUnescape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static void validarConexionBaseDatos() throws SQLException {
        try (Connection ignored = abrirConexionDb()) {
            // Conexion valida.
        }
    }

    private static Connection abrirConexionDb() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static UsuarioDb buscarUsuarioValido(String usuario, String password) throws SQLException {
        String sql = "SELECT id_usuario, correo, nombres, apellidos FROM auth.usuarios "
                + "WHERE activo = TRUE AND password = ? AND (LOWER(correo) = LOWER(?) OR LOWER(id_usuario) = LOWER(?)) LIMIT 1";

        try (Connection con = abrirConexionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, password);
            ps.setString(2, usuario);
            ps.setString(3, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UsuarioDb(
                        rs.getString("id_usuario"),
                        rs.getString("correo"),
                        rs.getString("nombres"),
                        rs.getString("apellidos")
                );
            }
        }
    }

    private static void registrarUsuario(String nombres, String apellidos, String cedula, String correo, String password) throws SQLException {
        String sql = "INSERT INTO auth.usuarios(id_usuario, nombres, apellidos, cedula, correo, password, activo) VALUES(?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection con = abrirConexionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, nombres);
            ps.setString(3, apellidos);
            ps.setString(4, cedula);
            ps.setString(5, correo);
            ps.setString(6, password);
            ps.executeUpdate();
        }
    }

    private static void guardarSesionActiva(String token, String idUsuario) throws SQLException {
        String sql = "INSERT INTO auth.sesiones(token_sesion, id_usuario, activa) VALUES(?, ?, TRUE) "
                + "ON CONFLICT (token_sesion) DO UPDATE SET id_usuario = EXCLUDED.id_usuario, activa = TRUE, creado_en = NOW()";
        try (Connection con = abrirConexionDb();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, idUsuario);
            ps.executeUpdate();
        }
    }

    private static boolean esConflictoUnicidad(SQLException e) {
        return "23505".equals(e.getSQLState());
    }

    private static String leerConfiguracion(String envKey, String sysProp, String fallback) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        String propValue = System.getProperty(sysProp);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }

        return fallback;
    }

    private static class UsuarioDb {
        String idUsuario;
        String correo;
        String nombres;
        String apellidos;

        UsuarioDb(String idUsuario, String correo, String nombres, String apellidos) {
            this.idUsuario = idUsuario;
            this.correo = correo;
            this.nombres = nombres;
            this.apellidos = apellidos;
        }
    }

    private static class LoteDemo {
        String idLote;
        String usuario;
        int cantidad;
        String estado;
        String fecha;
        String nodos;
        long duracionMs;
        List<String> archivos = Collections.emptyList();
        List<String> logs = Collections.emptyList();
        List<String> transformaciones = Collections.emptyList();
    }
}
