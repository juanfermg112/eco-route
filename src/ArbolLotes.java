public class ArbolLotes {
    private NodoLote raiz;

    public ArbolLotes() {
        this.raiz = null;
    }

    public String registrarLote(String idLote, Medicamento med, Hospital origen, Hospital destino) {
        if (med.requiereCadenaFrio() && !destino.tieneCadenaFrio()) return "ERROR_FRIO";
        if (med.esControlado() && !destino.habilitadoParaControlados()) return "ERROR_CONTROLADO";

        if (raiz == null) {
            raiz = new NodoLote(idLote, med, origen, destino);
            return "EXITO";
        }
        return insertarRecursivo(raiz, idLote, med, origen, destino);
    }

    private String insertarRecursivo(NodoLote actual, String idLote, Medicamento med, Hospital origen, Hospital destino) {
        int comparacion = idLote.compareTo(actual.getIdLote());
        if (comparacion == 0) return "DUPLICADO";
        else if (comparacion < 0) {
            if (actual.izquierdo == null) {
                actual.izquierdo = new NodoLote(idLote, med, origen, destino);
                return "EXITO";
            } else return insertarRecursivo(actual.izquierdo, idLote, med, origen, destino);
        } else {
            if (actual.derecho == null) {
                actual.derecho = new NodoLote(idLote, med, origen, destino);
                return "EXITO";
            } else return insertarRecursivo(actual.derecho, idLote, med, origen, destino);
        }
    }

    public NodoLote buscarLote(String idLoteBuscado) {
        return buscarRecursivo(raiz, idLoteBuscado);
    }

    private NodoLote buscarRecursivo(NodoLote actual, String idLoteBuscado) {
        if (actual == null) return null;
        int comparacion = idLoteBuscado.compareTo(actual.getIdLote());
        if (comparacion == 0) return actual;
        else if (comparacion < 0) return buscarRecursivo(actual.izquierdo, idLoteBuscado);
        else return buscarRecursivo(actual.derecho, idLoteBuscado);
    }

    public void obtenerHistorialInorden(NodoLote actual, java.util.List<NodoLote> lista) {
        if (actual != null) {
            obtenerHistorialInorden(actual.izquierdo, lista);
            lista.add(actual);
            obtenerHistorialInorden(actual.derecho, lista);
        }
    }

    public NodoLote getRaiz() { return raiz; }
}