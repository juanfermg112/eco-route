import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class Hospital {
    private String nombre;
    private int nivelAtencion;
    private boolean tieneCadenaFrio;
    private boolean habilitadoParaControlados;
    private HashMap<String, PriorityQueue<LoteInventario>> bodegas;

    public Hospital(String nombre, int nivelAtencion, boolean tieneCadenaFrio, boolean habilitadoParaControlados) {
        this.nombre = nombre;
        this.nivelAtencion = nivelAtencion;
        this.tieneCadenaFrio = tieneCadenaFrio;
        this.habilitadoParaControlados = habilitadoParaControlados;
        this.bodegas = new HashMap<>();
    }

    public void recibirLote(LoteInventario lote) {
        if (lote == null) return;
        bodegas.putIfAbsent(lote.getMedicamento().getNombre(), new PriorityQueue<>());
        PriorityQueue<LoteInventario> cola = bodegas.get(lote.getMedicamento().getNombre());

        // VALIDACIÓN DE DUPLICADOS INTERNOS: Si el lote ya existe en esta bodega, se incrementa la cantidad
        for (LoteInventario l : cola) {
            if (l.getIdLote().equalsIgnoreCase(lote.getIdLote())) {
                l.reducirCantidad(-lote.getCantidad()); // Un valor negativo suma las unidades
                return;
            }
        }
        cola.add(lote);
    }

    public int getStockTotal(String nombreMed) {
        if (!bodegas.containsKey(nombreMed)) return 0;
        int total = 0;
        for (LoteInventario lote : bodegas.get(nombreMed)) total += lote.getCantidad();
        return total;
    }

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

    public List<LoteInventario> getLotesDisponibles(String nombreMed) {
        List<LoteInventario> lista = new ArrayList<>();
        if (!bodegas.containsKey(nombreMed)) return lista;
        PriorityQueue<LoteInventario> copia = new PriorityQueue<>(bodegas.get(nombreMed));
        while (!copia.isEmpty()) lista.add(copia.poll());
        return lista;
    }

    public LoteInventario extraerLotePorId(String idLote, String nombreMed) {
        if (!bodegas.containsKey(nombreMed)) return null;
        PriorityQueue<LoteInventario> colaOriginal = bodegas.get(nombreMed);
        PriorityQueue<LoteInventario> colaTemp     = new PriorityQueue<>();
        LoteInventario encontrado = null;

        while (!colaOriginal.isEmpty()) {
            LoteInventario l = colaOriginal.poll();
            if (l.getIdLote().equalsIgnoreCase(idLote) && encontrado == null) {
                encontrado = l;
            } else {
                colaTemp.add(l);
            }
        }
        bodegas.put(nombreMed, colaTemp);
        return encontrado;
    }

    public boolean retirarLotePorId(String idLote) {
        boolean globalEncontrado = false;
        for (String nombreMed : bodegas.keySet()) {
            PriorityQueue<LoteInventario> colaOriginal = bodegas.get(nombreMed);
            PriorityQueue<LoteInventario> colaTemp     = new PriorityQueue<>();
            boolean encontradoEnMed = false;

            while (!colaOriginal.isEmpty()) {
                LoteInventario l = colaOriginal.poll();
                if (l.getIdLote().equalsIgnoreCase(idLote) && !encontradoEnMed) {
                    encontradoEnMed = true;
                    globalEncontrado = true;
                } else {
                    colaTemp.add(l);
                }
            }
            bodegas.put(nombreMed, colaTemp);
        }
        return globalEncontrado;
    }

    public HashMap<String, PriorityQueue<LoteInventario>> getBodegas() { return bodegas; }
    public String getNombre() { return nombre; }
    public boolean tieneCadenaFrio() { return tieneCadenaFrio; }
    public boolean habilitadoParaControlados() { return habilitadoParaControlados; }

    @Override
    public String toString() { return nombre; }
}