import java.util.HashMap;

public class Hospital {
    private String nombre;
    private int nivelAtencion;
    private boolean tieneCadenaFrio;
    private boolean habilitadoParaControlados;

    // --- NUEVO: Inventario interno del Hospital ---
    private HashMap<String, Integer> inventarioMedicamentos;

    public Hospital(String nombre, int nivelAtencion, boolean tieneCadenaFrio, boolean habilitadoParaControlados) {
        this.nombre = nombre;
        this.nivelAtencion = nivelAtencion;
        this.tieneCadenaFrio = tieneCadenaFrio;
        this.habilitadoParaControlados = habilitadoParaControlados;
        this.inventarioMedicamentos = new HashMap<>(); // Inicializamos el inventario vacío
    }

    // --- MÉTODOS PARA GESTIONAR EL INVENTARIO ---

    // Permite establecer una cantidad inicial (stock base)
    public void setStockInicial(String nombreMedicamento, int cantidad) {
        inventarioMedicamentos.put(nombreMedicamento, cantidad);
    }

    // Obtiene la cantidad actual de un medicamento (devuelve 0 si no lo tiene)
    public int getStock(String nombreMedicamento) {
        return inventarioMedicamentos.getOrDefault(nombreMedicamento, 0);
    }

    // Resta stock cuando se envía a otro lado
    public boolean reducirStock(String nombreMedicamento, int cantidadRestar) {
        int stockActual = getStock(nombreMedicamento);
        if (stockActual >= cantidadRestar) {
            inventarioMedicamentos.put(nombreMedicamento, stockActual - cantidadRestar);
            return true; // Éxito al restar
        }
        return false; // No hay suficiente stock
    }

    // Suma stock cuando recibe un envío
    public void aumentarStock(String nombreMedicamento, int cantidadSumar) {
        int stockActual = getStock(nombreMedicamento);
        inventarioMedicamentos.put(nombreMedicamento, stockActual + cantidadSumar);
    }

    public String getNombre() { return nombre; }
    public boolean tieneCadenaFrio() { return tieneCadenaFrio; }
    public boolean habilitadoParaControlados() { return habilitadoParaControlados; }

    @Override
    public String toString() {
        return nombre;
    }
}