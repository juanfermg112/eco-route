// NUEVA CLASE: Representa una caja física de medicamentos en bodega
public class LoteInventario implements Comparable<LoteInventario> {
    private String idLote;
    private Medicamento medicamento;
    private int cantidad;
    private int diasParaCaducar;

    public LoteInventario(String idLote, Medicamento medicamento, int cantidad, int diasParaCaducar) {
        this.idLote = idLote;
        this.medicamento = medicamento;
        this.cantidad = cantidad;
        this.diasParaCaducar = diasParaCaducar;
    }

    // EL ORDENAMIENTO DE LA COLA: Siempre pone arriba el que caduca primero (FEFO)
    @Override
    public int compareTo(LoteInventario otro) {
        return Integer.compare(this.diasParaCaducar, otro.diasParaCaducar);
    }

    public void reducirCantidad(int cant) {
        this.cantidad -= cant;
    }

    public String getIdLote() { return idLote; }
    public Medicamento getMedicamento() { return medicamento; }
    public int getCantidad() { return cantidad; }
    public int getDiasParaCaducar() { return diasParaCaducar; }
}