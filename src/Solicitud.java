public class Solicitud implements Comparable<Solicitud> {
    private String idSolicitud;
    private String nombrePaciente;
    private int edad;
    private String sintomas;
    private Hospital hospitalSolicitante; // Enlazado directamente al catálogo maestro
    private Medicamento medicamento;
    private int nivelUrgencia; // 1 (Rojo) a 5 (Azul)
    private long timestampLlegada;

    public Solicitud(String idSolicitud, String nombrePaciente, int edad, String sintomas, Hospital hospitalSolicitante, Medicamento medicamento, int nivelUrgencia) {
        this.idSolicitud = idSolicitud;
        this.nombrePaciente = nombrePaciente;
        this.edad = edad;
        this.sintomas = sintomas;
        this.hospitalSolicitante = hospitalSolicitante;
        this.medicamento = medicamento;
        this.nivelUrgencia = nivelUrgencia;
        this.timestampLlegada = System.nanoTime();
    }

    @Override
    public int compareTo(Solicitud otra) {
        int comparacionUrgencia = Integer.compare(this.nivelUrgencia, otra.nivelUrgencia);
        if (comparacionUrgencia != 0) {
            return comparacionUrgencia;
        }
        return Long.compare(this.timestampLlegada, otra.timestampLlegada);
    }

    // Getters
    public String getIdSolicitud() { return idSolicitud; }
    public String getNombrePaciente() { return nombrePaciente; }
    public Hospital getHospitalSolicitante() { return hospitalSolicitante; }
    public Medicamento getMedicamento() { return medicamento; }
    public int getNivelUrgencia() { return nivelUrgencia; }
}