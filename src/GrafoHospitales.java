import java.util.*;

public class GrafoHospitales {
    private List<Hospital> nodos;
    private int[][] matrizAdyacencia;
    private int numHospitales;
    private HashMap<String, Integer> indicePorNombre;

    public GrafoHospitales(int capacidad) {
        nodos = new ArrayList<>();
        matrizAdyacencia = new int[capacidad][capacidad];
        indicePorNombre = new HashMap<>();
        numHospitales = 0;
        for (int[] fila : matrizAdyacencia) Arrays.fill(fila, Integer.MAX_VALUE);
    }

    public void agregarHospital(Hospital h) {
        nodos.add(h);
        indicePorNombre.put(h.getNombre(), numHospitales);
        numHospitales++;
    }

    public void addRuta(String origen, String destino, int tiempoMinutos) {
        Integer i = indicePorNombre.get(origen);
        Integer j = indicePorNombre.get(destino);
        if (i != null && j != null) {
            matrizAdyacencia[i][j] = tiempoMinutos;
            matrizAdyacencia[j][i] = tiempoMinutos; // Grafo no dirigido (bidireccional)
        }
    }

    public String buscarRutaMasRapida(String nombreOrigen, String nombreDestino) {
        Integer origenIdx = indicePorNombre.get(nombreOrigen);
        Integer destinoIdx = indicePorNombre.get(nombreDestino);

        if (origenIdx == null || destinoIdx == null) return "Ruta no mapeada";
        if (origenIdx.equals(destinoIdx)) return "Mismo lugar (0 min)";

        int[] distancias = new int[numHospitales];
        Arrays.fill(distancias, Integer.MAX_VALUE);
        distancias[origenIdx] = 0;

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(i -> distancias[i]));
        pq.add(origenIdx);

        while (!pq.isEmpty()) {
            int u = pq.poll();
            for (int v = 0; v < numHospitales; v++) {
                if (matrizAdyacencia[u][v] != Integer.MAX_VALUE) {
                    if (distancias[u] + matrizAdyacencia[u][v] < distancias[v]) {
                        distancias[v] = distancias[u] + matrizAdyacencia[u][v];
                        pq.add(v);
                    }
                }
            }
        }
        return (distancias[destinoIdx] == Integer.MAX_VALUE) ? "No existe ruta posible" : distancias[destinoIdx] + " min";
    }
}