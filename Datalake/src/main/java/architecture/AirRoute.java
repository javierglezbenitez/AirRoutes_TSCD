
package architecture;

public class AirRoute {

    private final String codigoVuelo;
    private final String escala;
    private final String origen;
    private final String destino;
    private final int duracionMinutos;
    private final double precio;
    private final String aerolinea;
    private final long timestamp;

    public AirRoute(String codigoVuelo, String origen, String destino,
                    int duracionMinutos, double precio, String aerolinea, long timestamp, String escala) {
        this.codigoVuelo = codigoVuelo;
        this.origen = origen;
        this.destino = destino;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
        this.aerolinea = aerolinea;
        this.timestamp = timestamp;
        this.escala = escala;
    }

    public String getCodigoVuelo() { return codigoVuelo; }
    public String getEscala() { return escala; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public double getPrecio() { return precio; }
    public String getAerolinea() { return aerolinea; }
    public long getTimestamp() { return timestamp; }
}
