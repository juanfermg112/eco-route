import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class Hospital {
    private String nombre;
    private int nivelAtencion;
    private boolean tieneCadenaFrio;
    private boolean habilitadoParaControlados;

    // Diccionario: nombre del medicamento → cola FEFO de lotes físicos
    private HashMap<String, PriorityQueue<LoteInventario>> bodegas;

    public Hospital(String nombre, int nivelAtencion, boolean tieneCadenaFrio, boolean habilitadoParaControlados) {
        this.nombre = nombre;
        this.nivelAtencion = nivelAtencion;
        this.tieneCadenaFrio = tieneCadenaFrio;
        this.habilitadoParaControlados = habilitadoParaControlados;
        this.bodegas = new HashMap<>();
    }

    /** Inserta un lote en la cola FEFO del medicamento correspondiente. */
    public void recibirLote(LoteInventario lote) {
        bodegas.putIfAbsent(lote.getMedicamento().getNombre(), new PriorityQueue<>());
        bodegas.get(lote.getMedicamento().getNombre()).add(lote);
    }

    /** Suma las unidades de todos los lotes de un medicamento. O(n) */
    public int getStockTotal(String nombreMed) {
        if (!bodegas.containsKey(nombreMed)) return 0;
        int total = 0;
        for (LoteInventario lote : bodegas.get(nombreMed)) total += lote.getCantidad();
        return total;
    }

    /**
     * FEFO: extrae unidades del lote que caduca primero.
     * Complejidad: O(k * log n) donde k = lotes consumidos.
     */
    public boolean despacharMedicina(String nombreMed, int cantidadNecesaria) {
        if (getStockTotal(nombreMed) < cantidadNecesaria) return false;
        PriorityQueue<LoteInventario> cola = bodegas.get(nombreMed);
        int restante = cantidadNecesaria;
        while (restante > 0 && !cola.isEmpty()) {
            LoteInventario lote = cola.poll();
            if (lote.getCantidad() <= restante) {
                restante -= lote.getCantidad();
            } else {
                lote.reducirCantidad(restante);
                restante = 0;
                cola.add(lote);
            }
        }
        return true;
    }

    /**
     * NUEVO — Retorna todos los lotes de un medicamento sin destruir la cola.
     * Se usa para mostrar lotes disponibles en el combo de traslado.
     * O(n)
     */
    public List<LoteInventario> getLotesDisponibles(String nombreMed) {
        List<LoteInventario> lista = new ArrayList<>();
        if (!bodegas.containsKey(nombreMed)) return lista;
        PriorityQueue<LoteInventario> copia = new PriorityQueue<>(bodegas.get(nombreMed));
        while (!copia.isEmpty()) lista.add(copia.poll());
        return lista;
    }

    /**
     * NUEVO — Extrae un lote completo por su ID para moverlo a otro hospital (traslado).
     * Recorre la cola del medicamento correspondiente, extrae el lote exacto
     * y reconstruye la cola sin él. O(n)
     * @return el LoteInventario extraído, o null si no existe.
     */
    public LoteInventario extraerLotePorId(String idLote, String nombreMed) {
        if (!bodegas.containsKey(nombreMed)) return null;
        PriorityQueue<LoteInventario> colaOriginal = bodegas.get(nombreMed);
        PriorityQueue<LoteInventario> colaTemp     = new PriorityQueue<>();
        LoteInventario encontrado = null;

        while (!colaOriginal.isEmpty()) {
            LoteInventario l = colaOriginal.poll();
            if (l.getIdLote().equals(idLote) && encontrado == null) {
                encontrado = l; // Lo separamos; no lo metemos en colaTemp
            } else {
                colaTemp.add(l);
            }
        }
        // Restaurar la cola sin el lote extraído
        bodegas.put(nombreMed, colaTemp);
        return encontrado;
    }

    /**
     * NUEVO — Recall: elimina un lote de TODOS los medicamentos del hospital.
     * Recorre cada cola de cada medicamento buscando el ID. O(m * n)
     * donde m = tipos de medicamentos, n = lotes por medicamento.
     * @return true si el lote fue encontrado y eliminado.
     */
    public boolean retirarLotePorId(String idLote) {
        for (String nombreMed : bodegas.keySet()) {
            PriorityQueue<LoteInventario> colaOriginal = bodegas.get(nombreMed);
            PriorityQueue<LoteInventario> colaTemp     = new PriorityQueue<>();
            boolean encontrado = false;

            while (!colaOriginal.isEmpty()) {
                LoteInventario l = colaOriginal.poll();
                if (l.getIdLote().equals(idLote) && !encontrado) {
                    encontrado = true; // Lo descartamos (recall)
                } else {
                    colaTemp.add(l);
                }
            }
            bodegas.put(nombreMed, colaTemp);
            if (encontrado) return true;
        }
        return false;
    }

    public HashMap<String, PriorityQueue<LoteInventario>> getBodegas() { return bodegas; }
    public String  getNombre()                  { return nombre; }
    public boolean tieneCadenaFrio()            { return tieneCadenaFrio; }
    public boolean habilitadoParaControlados()  { return habilitadoParaControlados; }

    @Override
    public String toString() { return nombre; }
}