package es.hablabar.location.service;

/**
 * Cálculo de distancias sobre la superficie terrestre con la fórmula del semiverseno
 * (haversine). Para distancias de cientos de metros el error es despreciable frente
 * a la imprecisión propia de la geolocalización.
 */
public final class DistanciaService {

    /** Radio medio de la Tierra en metros. */
    private static final double RADIO_TIERRA_METROS = 6_371_008.8;

    private DistanciaService() {
    }

    /** Distancia en metros entre dos puntos expresados en grados decimales. */
    public static double metros(double latitud1, double longitud1, double latitud2, double longitud2) {
        double lat1 = Math.toRadians(latitud1);
        double lat2 = Math.toRadians(latitud2);
        double deltaLat = Math.toRadians(latitud2 - latitud1);
        double deltaLon = Math.toRadians(longitud2 - longitud1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}