public class NodoLote {
    private String idLote;
    private Medicamento medicamento;
    private Hospital origen;
    private Hospital destino;

    NodoLote izquierdo;
    NodoLote derecho;

    public NodoLote(String idLote, Medicamento medicamento, Hospital origen, Hospital destino) {
        this.idLote = idLote;
        this.medicamento = medicamento;
        this.origen = origen;
        this.destino = destino;
        this.izquierdo = null;
        this.derecho = null;
    }

    public String getIdLote() { return idLote; }
    public Medicamento getMedicamento() { return medicamento; }
    public Hospital getOrigen() { return origen; }
    public Hospital getDestino() { return destino; }

    @Override
    public String toString() {
        return "Lote: " + idLote + " | Fármaco: " + medicamento.getNombre() + " | Ruta: " + origen.getNombre() + " ➔ " + destino.getNombre();
    }
}