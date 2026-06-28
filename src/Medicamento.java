public class Medicamento {
    private String nombre;
    private boolean requiereCadenaFrio;
    private boolean esControlado;

    public Medicamento(String nombre, boolean requiereCadenaFrio, boolean esControlado) {
        this.nombre = nombre;
        this.requiereCadenaFrio = requiereCadenaFrio;
        this.esControlado = esControlado;
    }

    public String getNombre() { return nombre; }
    public boolean requiereCadenaFrio() { return requiereCadenaFrio; }
    public boolean esControlado() { return esControlado; }

    @Override
    public String toString() {
        return nombre + (requiereCadenaFrio ? " ❄️" : "") + (esControlado ? " ⚠️" : "");
    }
}