package com.sistema.rmi;

import com.sistema.distribuido.nodos.INodoTrabajador;
import com.sistema.distribuido.nodos.ResultadoProcesamiento;
import com.sistema.distribuido.nodos.Trabajo;
import com.sistema.model.Archivo;
import com.sistema.model.TipoTransformacion;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NodoTrabajadorImpl extends UnicastRemoteObject implements INodoTrabajador {

    private final String idNodo;
    private final AtomicInteger totalTrabajos = new AtomicInteger(0);
    private final AtomicInteger totalImagenes = new AtomicInteger(0);
    private final AtomicLong tiempoAcumuladoMs = new AtomicLong(0);
    private final AtomicInteger maxHilosParalelos = new AtomicInteger(0);

    public NodoTrabajadorImpl(String idNodo) throws RemoteException {
        super();
        this.idNodo = idNodo;
    }

    @Override
    public ResultadoProcesamiento procesarTrabajo(Trabajo trabajo) throws RemoteException {
        System.out.println("[RMI][" + idNodo + "] Worker procesando trabajo: " + trabajo.getIdTrabajo());

        List<String> rutas = Collections.synchronizedList(new ArrayList<>());
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        Map<String, byte[]> archivosProcesados = Collections.synchronizedMap(new LinkedHashMap<>());

        int cantidad = trabajo.getImagenes() == null ? 0 : trabajo.getImagenes().size();

        long inicio = System.currentTimeMillis();
        int hilos = Math.max(1, Math.min(8, cantidad == 0 ? 1 : cantidad));
        maxHilosParalelos.updateAndGet(actual -> Math.max(actual, hilos));

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < cantidad; i++) {
            final int idx = i;
            tareas.add(pool.submit(() -> {
                String threadName = Thread.currentThread().getName();
                try {
                    int espera = ThreadLocalRandom.current().nextInt(40, 140);
                    Thread.sleep(espera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                Archivo archivo = trabajo.getImagenes().get(idx);
                String formatoSalida = resolverFormatoSalida(archivo);
                String nombreSalida = construirNombreSalida(archivo, idx, formatoSalida);
                rutas.add(nombreSalida);

                byte[] bytesProcesados = procesarImagenReal(archivo, idx, formatoSalida);
                archivosProcesados.put(nombreSalida, bytesProcesados);

                String transformaciones = archivo.getTransformaciones() == null
                        ? "sin transformaciones"
                        : archivo.getTransformaciones().stream().map(Enum::name).collect(Collectors.joining(","));
                String log = LocalDateTime.now() + " - [" + threadName + "] Imagen " + idx + " procesada con: " + transformaciones;
                logs.add(log);
                System.out.println("[RMI][" + idNodo + "] " + log);
            }));
        }

        for (Future<?> tarea : tareas) {
            try {
                tarea.get();
            } catch (Exception e) {
                logs.add(LocalDateTime.now() + " - Error en tarea paralela: " + e.getMessage());
            }
        }
        pool.shutdown();

        long duracion = Math.max(1, System.currentTimeMillis() - inicio);
        totalTrabajos.incrementAndGet();
        totalImagenes.addAndGet(cantidad);
        tiempoAcumuladoMs.addAndGet(duracion);

        String mensaje = "Procesamiento real completado para " + cantidad + " imagen(es)";
        System.out.println("[RMI][" + idNodo + "] Resultado generado: " + mensaje + " en " + duracion + " ms con " + hilos + " hilo(s)");

        return new ResultadoProcesamiento(true, mensaje, rutas, logs, archivosProcesados);
    }

    @Override
    public boolean estaActivo() throws RemoteException {
        return true;
    }

    @Override
    public int obtenerCargaActual() throws RemoteException {
        return ThreadLocalRandom.current().nextInt(5, 50);
    }

    public synchronized Map<String, Object> obtenerMetricasConsumo() {
        Map<String, Object> metricas = new LinkedHashMap<>();
        int trabajos = totalTrabajos.get();
        long promedio = trabajos == 0 ? 0 : (tiempoAcumuladoMs.get() / trabajos);

        metricas.put("idNodo", idNodo);
        metricas.put("trabajosProcesados", trabajos);
        metricas.put("imagenesProcesadas", totalImagenes.get());
        metricas.put("tiempoPromedioMs", promedio);
        metricas.put("maxHilosParalelos", maxHilosParalelos.get());
        metricas.put("cargaActual", obtenerCargaSegura());
        return metricas;
    }

    private int obtenerCargaSegura() {
        try {
            return obtenerCargaActual();
        } catch (Exception e) {
            return 0;
        }
    }

    private byte[] procesarImagenReal(Archivo archivo, int idx, String formatoSalida) {
        try {
            BufferedImage actual = leerImagen(archivo, idx);
            List<TipoTransformacion> transformaciones = archivo.getTransformaciones() == null
                    ? Collections.emptyList()
                    : archivo.getTransformaciones();

            for (TipoTransformacion transformacion : transformaciones) {
                switch (transformacion) {
                    case ESCALA_GRISES:
                        actual = aplicarEscalaGrises(actual);
                        break;
                    case ROTAR:
                        actual = aplicarRotar90(actual);
                        break;
                    case REFLEJAR:
                        actual = aplicarReflejarHorizontal(actual);
                        break;
                    case REDIMENSIONAR:
                        actual = aplicarRedimensionar(actual, 640, 480);
                        break;
                    case RECORTAR:
                        actual = aplicarRecorteCentral(actual, 0.8);
                        break;
                    case DESENFOCAR:
                        actual = aplicarDesenfoque(actual);
                        break;
                    case PERFILAR:
                        actual = aplicarPerfilar(actual);
                        break;
                    case AJUSTE_BRILLO_CONTRASTE:
                        actual = aplicarBrilloContraste(actual, 1.15f, 10f);
                        break;
                    case MARCA_AGUA:
                        actual = aplicarMarcaAgua(actual, "ImageProc");
                        break;
                    case CONVERTIR_FORMATO:
                        // La conversion efectiva se aplica en el encode final segun formatoSalida.
                        break;
                    default:
                        // Otras transformaciones quedan como no-op en esta demo.
                        break;
                }
            }

            return escribirImagen(actual, formatoSalida);
        } catch (Exception e) {
            return escribirImagen(crearImagenBase(idx, "error:" + e.getMessage()), "png");
        }
    }

    private String resolverFormatoSalida(Archivo archivo) {
        if (archivo == null || archivo.getTransformaciones() == null || archivo.getNombre() == null) {
            return "png";
        }

        boolean convertir = archivo.getTransformaciones().contains(TipoTransformacion.CONVERTIR_FORMATO);
        if (!convertir) {
            return "png";
        }

        String nombre = archivo.getNombre().toLowerCase();
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) {
            return "jpg";
        }
        if (nombre.endsWith(".tif") || nombre.endsWith(".tiff")) {
            return "tif";
        }
        if (nombre.endsWith(".png")) {
            return "png";
        }
        return "jpg";
    }

    private String obtenerExtensionSalida(String formato) {
        if ("jpeg".equalsIgnoreCase(formato)) {
            return "jpg";
        }
        if ("tiff".equalsIgnoreCase(formato)) {
            return "tif";
        }
        return formato.toLowerCase();
    }

    private String construirNombreSalida(Archivo archivo, int idx, String formatoSalida) {
        String nombreOriginal = archivo == null ? null : archivo.getNombre();
        String base = extraerNombreBase(nombreOriginal);
        String ext = obtenerExtensionSalida(formatoSalida);
        return base + "_edited_" + idx + "." + ext;
    }

    private String extraerNombreBase(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            return "imagen";
        }

        String limpio = nombreArchivo.trim();
        int slash = Math.max(limpio.lastIndexOf('/'), limpio.lastIndexOf('\\'));
        if (slash >= 0 && slash < limpio.length() - 1) {
            limpio = limpio.substring(slash + 1);
        }

        int dot = limpio.lastIndexOf('.');
        if (dot > 0) {
            limpio = limpio.substring(0, dot);
        }

        limpio = limpio.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (limpio.isEmpty()) {
            return "imagen";
        }
        return limpio;
    }

    private BufferedImage leerImagen(Archivo archivo, int idx) {
        try {
            if (archivo != null && archivo.getContenido() != null && archivo.getContenido().length > 0) {
                BufferedImage leida = ImageIO.read(new ByteArrayInputStream(archivo.getContenido()));
                if (leida != null) {
                    return convertirArgb(leida);
                }
            }
        } catch (Exception ignored) {
            // fallback a imagen base
        }
        String nombre = archivo == null ? "img_" + idx : archivo.getNombre();
        return crearImagenBase(idx, nombre);
    }

    private BufferedImage convertirArgb(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(source, 0, 0, null);
        g2.dispose();
        return out;
    }

    private BufferedImage crearImagenBase(int idx, String label) {
        BufferedImage img = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(13, 13, 20));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(new Color(26, 26, 46));
        g.fillRoundRect(40, 40, 820, 520, 22, 22);
        g.setColor(new Color(129, 140, 248));
        g.fillOval(650, 90, 170, 170);
        g.setColor(new Color(226, 226, 240));
        g.drawString("ImageProc Demo", 70, 100);
        g.drawString("Archivo: " + label, 70, 130);
        g.drawString("Indice: " + idx, 70, 160);
        g.dispose();
        return img;
    }

    private byte[] escribirImagen(BufferedImage image, String formato) {
        try {
            String formatoNormalizado = formato == null ? "png" : formato.toLowerCase();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            BufferedImage paraEscritura = image;

            if ("jpg".equals(formatoNormalizado) || "jpeg".equals(formatoNormalizado)) {
                paraEscritura = convertirRgbSinAlpha(image);
            }

            boolean ok = ImageIO.write(paraEscritura, formatoNormalizado, output);
            if (!ok) {
                output.reset();
                ImageIO.write(convertirArgb(image), "png", output);
            }
            return output.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private BufferedImage convertirRgbSinAlpha(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage aplicarEscalaGrises(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgba = source.getRGB(x, y);
                int a = (rgba >> 24) & 0xff;
                int r = (rgba >> 16) & 0xff;
                int g = (rgba >> 8) & 0xff;
                int b = rgba & 0xff;
                int gray = (r + g + b) / 3;
                int outRgba = (a << 24) | (gray << 16) | (gray << 8) | gray;
                out.setRGB(x, y, outRgba);
            }
        }
        return out;
    }

    private BufferedImage aplicarRotar90(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform transform = new AffineTransform();
        transform.translate(h, 0);
        transform.rotate(Math.toRadians(90));
        g.drawImage(source, transform, null);
        g.dispose();
        return out;
    }

    private BufferedImage aplicarReflejarHorizontal(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        AffineTransform transform = AffineTransform.getScaleInstance(-1, 1);
        transform.translate(-w, 0);
        g.drawImage(source, transform, null);
        g.dispose();
        return out;
    }

    private BufferedImage aplicarRedimensionar(BufferedImage source, int targetW, int targetH) {
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, targetW, targetH, null);
        g.dispose();
        return out;
    }

    private BufferedImage aplicarRecorteCentral(BufferedImage source, double factor) {
        int w = source.getWidth();
        int h = source.getHeight();
        int targetW = Math.max(1, (int) Math.round(w * factor));
        int targetH = Math.max(1, (int) Math.round(h * factor));
        int x = Math.max(0, (w - targetW) / 2);
        int y = Math.max(0, (h - targetH) / 2);

        BufferedImage sub = source.getSubimage(x, y, targetW, targetH);
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(sub, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage aplicarDesenfoque(BufferedImage source) {
        float[] kernelData = {
                1f / 16f, 2f / 16f, 1f / 16f,
                2f / 16f, 4f / 16f, 2f / 16f,
                1f / 16f, 2f / 16f, 1f / 16f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernelData), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(convertirArgb(source), null);
    }

    private BufferedImage aplicarPerfilar(BufferedImage source) {
        float[] kernelData = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernelData), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(convertirArgb(source), null);
    }

    private BufferedImage aplicarBrilloContraste(BufferedImage source, float escala, float desplazamiento) {
        BufferedImage base = convertirArgb(source);
        RescaleOp op = new RescaleOp(
                new float[] { escala, escala, escala, 1f },
                new float[] { desplazamiento, desplazamiento, desplazamiento, 0f },
                null
        );
        return op.filter(base, null);
    }

    private BufferedImage aplicarMarcaAgua(BufferedImage source, String texto) {
        BufferedImage out = convertirArgb(source);
        Graphics2D g = out.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(new Color(129, 140, 248));
        g.drawString(texto, Math.max(16, out.getWidth() - 140), Math.max(22, out.getHeight() - 24));
        g.dispose();
        return out;
    }
}
