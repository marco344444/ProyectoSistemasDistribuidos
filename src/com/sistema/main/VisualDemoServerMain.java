package com.sistema.main;

import com.sistema.cliente.servicio.EstadoLote;
import com.sistema.cliente.servicio.InfoNodo;
import com.sistema.cliente.servicio.RequestLote;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;
import com.sistema.rest.RepositorioDatosImpl;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class VisualDemoServerMain {

    private static final List<LoteDemo> LOTES = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(1099);
            }

            registry.rebind(ServidorRmiMain.NOMBRE_BIND, new NodoTrabajadorImpl("worker-visual"));

            RepositorioDatosImpl repositorio = new RepositorioDatosImpl();
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
                String usuario = params.getOrDefault("usuario", "usuario-demo");
                String password = params.getOrDefault("password", "1234");

                String token = soap.iniciarSesion(usuario, password);
                String json = "{\"token\":\"" + jsonEscape(token) + "\",\"usuario\":\"" + jsonEscape(usuario) + "\"}";
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
                loteDemo.duracionMs = 1200L + (long) cantidad * 150L;
                loteDemo.archivos = new ArrayList<>();
                for (int i = 1; i <= cantidad; i++) {
                    loteDemo.archivos.add("img_" + i + "_procesada.jpg");
                }
                LOTES.add(loteDemo);

                String json = "{\"idLote\":\"" + jsonEscape(idLote) + "\",\"cantidad\":" + cantidad + "}";
                sendJson(exchange, 200, json);
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
                String json = "{\"idLote\":\"" + jsonEscape(estado.getIdLote()) + "\",\"estado\":\"" + jsonEscape(estado.getEstado())
                        + "\",\"total\":" + estado.getTotalImagenes() + ",\"procesadas\":" + estado.getProcesadas() + "}";
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

            httpServer.setExecutor(null);
            httpServer.start();

            System.out.println("Servidor visual iniciado en http://localhost:8080");
            System.out.println("Health: http://localhost:8080/api/health");
            System.out.println("Abre UI/01_login.html para iniciar el flujo visual.");
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

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
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
    }
}
