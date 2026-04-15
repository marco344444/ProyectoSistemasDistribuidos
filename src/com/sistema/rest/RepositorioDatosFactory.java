package com.sistema.rest;

import com.sistema.cliente.servicio.IRepositorioDatos;

public final class RepositorioDatosFactory {

    private RepositorioDatosFactory() {
    }

    public static IRepositorioDatos crearRepositorio() {
        String url = leerConfiguracion("DB_URL", "db.url", "");
        if (url.isEmpty()) {
            return new RepositorioDatosImpl();
        }

        String user = leerConfiguracion("DB_USER", "db.user", "imageproc");
        String password = leerConfiguracion("DB_PASSWORD", "db.password", "imageproc123");

        try {
            IRepositorioDatos jdbc = new RepositorioDatosJdbcImpl(url, user, password);
            System.out.println("[DB] Repositorio JDBC habilitado en: " + url);
            return jdbc;
        } catch (Exception e) {
            System.err.println("[DB] No se pudo iniciar JDBC, usando repositorio en memoria. Motivo: " + e.getMessage());
            return new RepositorioDatosImpl();
        }
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
}
