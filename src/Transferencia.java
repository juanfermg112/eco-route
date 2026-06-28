public class Transferencia implements Comparable<Transferencia> {
    private String idLote;
    private String origen;
    private String destino;

    public Transferencia(String idLote, String origen, String destino) {
        this.idLote = idLote;
        this.origen = origen;
        this.destino = destino;
    }

    public String getIdLote() { return idLote; }
    public String getHospitalDestino() { return destino; }

    @Override
    public int compareTo(Transferencia otra) {
        return this.idLote.compareTo(otra.idLote);
    }

    @Override
    public String toString() {
        return "Lote Técnico: " + idLote + " [Origen: " + origen + " ➔ Destino: " + destino + "]";
    }
}