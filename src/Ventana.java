import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Ventana {
    private JPanel principal;
    private JTabbedPane tabTriage;
    private JTextField txtId;
    private JTextField txtNombrePaciente;
    private JSpinner spnEdad;
    private JTextField txtSintomas;
    private JComboBox<String> cmbHospitalTriage; // ¡Nuevo! Conectado a la red de hospitales
    private JComboBox<String> cmbMedicina;
    private JComboBox<String> cmbUrgencia;
    private JButton btnIngresar;
    private JButton btnSiguiente;
    private JButton btnDeshacer; // ¡Nuevo! Botón para aplicar la Pila (Undo)
    private JTable tablaTriage;

    private JTextField txtLote;
    private JComboBox<String> cmbMedicinaLote;
    private JComboBox<String> cmbOrigen;
    private JComboBox<String> cmbDestino;
    private JButton btnTraslado;
    private JButton btnRecall;
    private JTable tablaLogistica;
    private JButton btnVerInventario;
    private JLabel lblMetricas;

    // --- ESTRUCTURAS DE DATOS ---
    private PriorityQueue<Solicitud> colaTriage = new PriorityQueue<>();
    private ArbolLotes motorLogistico = new ArbolLotes();
    private GrafoHospitales redHospitales;

    // 🧠 LA PILA DE DESHACER (Estructura LIFO exigida en el Syllabus)
    private Stack<Solicitud> pilaDeshacer = new Stack<>();

    // --- MODELOS Y DICCIONARIOS ---
    private DefaultTableModel modeloTriage;
    private DefaultTableModel modeloLogistica;
    private HashMap<String, Medicamento> bdMedicamentos = new HashMap<>();
    private HashMap<String, Hospital> bdHospitales = new HashMap<>();

    public Ventana() {
        inicializarBasesDeDatos();
        configurarTablas();
        cargarComboBoxes();

        // ==========================================
        // BOTÓN: INGRESAR PACIENTE
        // ==========================================
        btnIngresar.addActionListener(e -> {
            try {
                String id = txtId.getText().trim();
                String nombre = txtNombrePaciente.getText().trim();
                int edad = Integer.parseInt(spnEdad.getValue().toString());
                String sintomas = txtSintomas.getText().trim();

                Hospital hospSolicitante = bdHospitales.get(cmbHospitalTriage.getSelectedItem().toString());
                Medicamento med = bdMedicamentos.get(cmbMedicina.getSelectedItem().toString());
                int urgencia = cmbUrgencia.getSelectedIndex() + 1;

                if (id.isEmpty() || nombre.isEmpty() || sintomas.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Por favor, complete todos los campos clínicos de Triage.");
                    return;
                }

                Solicitud nuevaSolicitud = new Solicitud(id, nombre, edad, sintomas, hospSolicitante, med, urgencia);

                // 1. Agregar a la Cola de Prioridad
                colaTriage.add(nuevaSolicitud);

                // 2. Apilar en la Pila de Deshacer (LIFO)
                pilaDeshacer.push(nuevaSolicitud);

                actualizarTablaTriage();
                limpiarCamposTriage();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error en el formato de los datos clínicos.");
            }
        });

        // ==========================================
        // BOTÓN: DESHACER ÚLTIMO INGRESO (Uso de la Pila)
        // ==========================================
        btnDeshacer.addActionListener(e -> {
            // Evaluamos si la estructura LIFO tiene elementos
            if (!pilaDeshacer.isEmpty()) {
                // Sacamos el último elemento ingresado de la Pila
                Solicitud ultimaDeshecha = pilaDeshacer.pop();

                // Lo removemos de la Cola de Prioridad activa
                colaTriage.remove(ultimaDeshecha);

                JOptionPane.showMessageDialog(null,
                        "🔄 ACCIÓN DESHECHA\nSe eliminó del Triage al paciente: " + ultimaDeshecha.getNombrePaciente(),
                        "Control de Historial (Stack LIFO)", JOptionPane.INFORMATION_MESSAGE);

                actualizarTablaTriage();
            } else {
                JOptionPane.showMessageDialog(null, "No hay acciones recientes para deshacer en esta sesión.", "Pila Vacía", JOptionPane.WARNING_MESSAGE);
            }
        });

        // ==========================================
        // BOTÓN: ATENDER SIGUIENTE
        // ==========================================b
        btnSiguiente.addActionListener(e -> {
            if (!colaTriage.isEmpty()) {
                Solicitud atendida = colaTriage.poll();
                // Al ser atendido de forma definitiva, limpiamos la posibilidad de deshacerlo
                pilaDeshacer.remove(atendida);

                JOptionPane.showMessageDialog(null,
                        "🚨 ATENDIENDO PACIENTE CRÍTICO 🚨\n\n" +
                                "Paciente: " + atendida.getNombrePaciente() + "\n" +
                                "Centro Destino: " + atendida.getHospitalSolicitante().getNombre() + "\n" +
                                "Fármaco Requerido: " + atendida.getMedicamento().getNombre(),
                        "Triage Médico - SafeDose", JOptionPane.INFORMATION_MESSAGE);
                actualizarTablaTriage();
            } else {
                JOptionPane.showMessageDialog(null, "No hay pacientes en espera en el Triage.");
            }
        });

        // ==========================================
        // BOTÓN: TRASLADO LOGÍSTICO (INVENTARIO + ÁRBOL BST + GRAFO)
        // ==========================================
        btnTraslado.addActionListener(e -> {
            String lote = txtLote.getText().trim();
            Medicamento med = bdMedicamentos.get(cmbMedicinaLote.getSelectedItem().toString());
            Hospital origen = bdHospitales.get(cmbOrigen.getSelectedItem().toString());
            Hospital destino = bdHospitales.get(cmbDestino.getSelectedItem().toString());

            if (lote.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Especifique un ID de lote.");
                return;
            }

            // --- NUEVO: Pedir cantidad a trasladar ---
            String cantStr = JOptionPane.showInputDialog(null, "¿Cuántas unidades de " + med.getNombre() + " se enviarán en este lote?", "Cantidad a enviar", JOptionPane.QUESTION_MESSAGE);
            if (cantStr == null || cantStr.trim().isEmpty()) return; // El usuario canceló

            int cantidadTrasladar;
            try {
                cantidadTrasladar = Integer.parseInt(cantStr);
                if (cantidadTrasladar <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Debe ingresar una cantidad numérica válida mayor a 0.");
                return;
            }

            // --- NUEVO: Validar Inventario en Origen ---
            if (origen.getStock(med.getNombre()) < cantidadTrasladar) {
                JOptionPane.showMessageDialog(null,
                        "❌ STOCK INSUFICIENTE\nEl " + origen.getNombre() + " solo tiene " + origen.getStock(med.getNombre()) + " unidades de " + med.getNombre() + ".",
                        "Error de Inventario", JOptionPane.ERROR_MESSAGE);
                return; // Detenemos el traslado
            }

            // 1. Evaluar si la ruta existe y calcular tiempo (Dijkstra)
            String tiempoRuta = redHospitales.buscarRutaMasRapida(origen.getNombre(), destino.getNombre());
            if (tiempoRuta.contains("No existe")) {
                JOptionPane.showMessageDialog(null, "No hay conexión terrestre directa entre los hospitales.");
                return;
            }

            // 2. Registrar en Inventario (Árbol BST) validando Reglas de Vértice
            String resultado = motorLogistico.registrarLote(lote, med, origen, destino);

            if (resultado.equals("DUPLICADO")) {
                JOptionPane.showMessageDialog(null, "Lote duplicado en el Árbol BST.", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (resultado.equals("ERROR_FRIO")) {
                JOptionPane.showMessageDialog(null, "❌ BLOQUEO DE CADENA DE FRÍO: " + destino.getNombre() + " no tiene infraestructura.", "Fallo Bioseguridad", JOptionPane.ERROR_MESSAGE);
            } else if (resultado.equals("ERROR_CONTROLADO")) {
                JOptionPane.showMessageDialog(null, "❌ BLOQUEO LEGAL: " + destino.getNombre() + " no maneja psicotrópicos.", "Fallo Sanitario", JOptionPane.ERROR_MESSAGE);
            } else {

                // --- NUEVO: Ejecutar el movimiento contable ---
                origen.reducirStock(med.getNombre(), cantidadTrasladar);
                destino.aumentarStock(med.getNombre(), cantidadTrasladar);

                JOptionPane.showMessageDialog(null,
                        "✅ TRASLADO APROBADO Y CONTABILIZADO\n" +
                                "Se enviaron " + cantidadTrasladar + " unidades a " + destino.getNombre() + ".\n" +
                                "Tiempo de Ruta (Dijkstra): " + tiempoRuta + "\n\n" +
                                "Stock restante en Origen: " + origen.getStock(med.getNombre()),
                        "Logística y Kardex", JOptionPane.INFORMATION_MESSAGE);

                actualizarTablaLogistica();
                txtLote.setText("");
            }
        });

        // ==========================================
        // BOTÓN: RECALL SANITARIO
        // ==========================================
        btnRecall.addActionListener(e -> {
            String loteBuscado = JOptionPane.showInputDialog(null, "Ingrese el ID del Lote para Recall Inmediato:");
            if (loteBuscado == null || loteBuscado.trim().isEmpty()) return;

            NodoLote encontrado = motorLogistico.buscarLote(loteBuscado.trim());

            if (encontrado != null) {
                JOptionPane.showMessageDialog(null,
                        "🔴 RECALL COMPLETO\nEl lote '" + loteBuscado + "' de " + encontrado.getMedicamento().getNombre() + " ha sido inmovilizado en: " + encontrado.getDestino().getNombre(),
                        "Alerta de Sanidad Pública", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Lote limpio. No se encuentra en el Árbol Logístico.");
            }
        });

        // ==========================================
        // BOTÓN: AUDITORÍA DE INVENTARIOS (REPORTE GLOBAL)
        // ==========================================
        btnVerInventario.addActionListener(e -> {
            StringBuilder reporte = new StringBuilder("=== AUDITORÍA GLOBAL DE INVENTARIOS ===\n\n");

            // Iteramos sobre todos los hospitales en el HashMap
            for (Hospital h : bdHospitales.values()) {
                reporte.append("🏥 ").append(h.getNombre()).append(":\n");
                boolean tieneAlgo = false;

                // Recorremos el catálogo de medicinas para listar lo que tiene cada uno
                for (String nombreMed : bdMedicamentos.keySet()) {
                    int stock = h.getStock(nombreMed);
                    if (stock > 0) {
                        reporte.append("   - ").append(nombreMed).append(": ").append(stock).append(" un.\n");
                        tieneAlgo = true;
                    }
                }
                if (!tieneAlgo) reporte.append("   (Sin stock disponible)\n");
                reporte.append("\n");
            }

            // Mostramos el reporte en un JTextArea dentro de un JScrollPane para que se vea bien
            JTextArea textArea = new JTextArea(reporte.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(null, scrollPane, "Reporte de Inventarios", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // =========================================================================
    // MODULOS DE CARGA DE DATOS MÁSTER (CATÁLOGOS MAESTROS DE ECUADOR EXPANDIDOS)
    // =========================================================================
    private void inicializarBasesDeDatos() {
        // 1. Catálogo de Medicinas Maestro
        bdMedicamentos.put("Fentanilo IV", new Medicamento("Fentanilo IV", false, true));
        bdMedicamentos.put("Morfina Ampollas", new Medicamento("Morfina Ampollas", false, true));
        bdMedicamentos.put("Insulina Glargina", new Medicamento("Insulina Glargina", true, false));
        bdMedicamentos.put("Vacuna Pfizer COVID19", new Medicamento("Vacuna Pfizer COVID19", true, false));
        bdMedicamentos.put("Amoxicilina Suspensión", new Medicamento("Amoxicilina Suspensión", false, false));
        bdMedicamentos.put("Paracetamol IV", new Medicamento("Paracetamol IV", false, false));
        bdMedicamentos.put("Epinefrina", new Medicamento("Epinefrina", true, false));

        // 2. Instanciación de Hospitales (Vértices del Grafo)
        Hospital h1 = new Hospital("Hospital Eugenio Espejo (Quito)", 3, true, true);
        Hospital h2 = new Hospital("Hospital Baca Ortiz (Quito)", 3, true, true);
        Hospital h3 = new Hospital("Centro de Salud Conocoto (Nivel 1)", 1, false, false);
        Hospital h4 = new Hospital("Bodega MSP Central Zonal 9", 0, true, true);
        Hospital h5 = new Hospital("Hospital Enrique Garcés (Sur)", 2, true, false);
        Hospital h6 = new Hospital("Subcentro de Salud Alangasí", 1, false, false);

        // 3. ASIGNACIÓN DEL STOCK INICIAL INTEGRAL POR TODO EL NETWORK

        // --- h4: Bodega MSP Central Zonal 9 (Gran Repositorio de Distribución) ---
        h4.setStockInicial("Fentanilo IV", 600);
        h4.setStockInicial("Morfina Ampollas", 450);
        h4.setStockInicial("Insulina Glargina", 1200);
        h4.setStockInicial("Vacuna Pfizer COVID19", 8000);
        h4.setStockInicial("Amoxicilina Suspensión", 2500);
        h4.setStockInicial("Paracetamol IV", 5000);
        h4.setStockInicial("Epinefrina", 900);

        // --- h1: Hospital Eugenio Espejo (Alta capacidad y uso de controlados) ---
        h1.setStockInicial("Fentanilo IV", 80);
        h1.setStockInicial("Morfina Ampollas", 60);
        h1.setStockInicial("Insulina Glargina", 150);
        h1.setStockInicial("Vacuna Pfizer COVID19", 400);
        h1.setStockInicial("Amoxicilina Suspensión", 300);
        h1.setStockInicial("Paracetamol IV", 600);
        h1.setStockInicial("Epinefrina", 120);

        // --- h2: Hospital Baca Ortiz (Enfoque pediátrico y vacunas) ---
        h2.setStockInicial("Fentanilo IV", 30);
        h2.setStockInicial("Morfina Ampollas", 25);
        h2.setStockInicial("Insulina Glargina", 200);
        h2.setStockInicial("Vacuna Pfizer COVID19", 1200);
        h2.setStockInicial("Amoxicilina Suspensión", 800);
        h2.setStockInicial("Paracetamol IV", 450);
        h2.setStockInicial("Epinefrina", 150);

        // --- h5: Hospital Enrique Garcés (Tiene frío, NO maneja controlados) ---
        h5.setStockInicial("Insulina Glargina", 300);
        h5.setStockInicial("Vacuna Pfizer COVID19", 800);
        h5.setStockInicial("Amoxicilina Suspensión", 500);
        h5.setStockInicial("Paracetamol IV", 700);
        h5.setStockInicial("Epinefrina", 180);

        // --- h3: Centro de Salud Conocoto (Nivel 1 - Solo fármacos base) ---
        h3.setStockInicial("Amoxicilina Suspensión", 150);
        h3.setStockInicial("Paracetamol IV", 200);

        // --- h6: Subcentro de Salud Alangasí (Nivel 1 - Solo fármacos base) ---
        h6.setStockInicial("Amoxicilina Suspensión", 90);
        h6.setStockInicial("Paracetamol IV", 120);

        // 4. Registro de Hospitales en la Tabla Hash Maestra
        bdHospitales.put(h1.getNombre(), h1); bdHospitales.put(h2.getNombre(), h2);
        bdHospitales.put(h3.getNombre(), h3); bdHospitales.put(h4.getNombre(), h4);
        bdHospitales.put(h5.getNombre(), h5); bdHospitales.put(h6.getNombre(), h6);

        // 5. Configuración del Grafo e Inyección de Aristas (Dijkstra)
        redHospitales = new GrafoHospitales(6);
        redHospitales.agregarHospital(h1); redHospitales.agregarHospital(h2);
        redHospitales.agregarHospital(h3); redHospitales.agregarHospital(h4);
        redHospitales.agregarHospital(h5); redHospitales.agregarHospital(h6);

        // Conexiones de la Red de Transporte Terrestre (Tiempos en minutos)
        redHospitales.addRuta(h4.getNombre(), h1.getNombre(), 15); // Bodega Central ➔ Eugenio Espejo
        redHospitales.addRuta(h4.getNombre(), h3.getNombre(), 35); // Bodega Central ➔ Conocoto
        redHospitales.addRuta(h4.getNombre(), h5.getNombre(), 25); // Bodega Central ➔ Enrique Garcés
        redHospitales.addRuta(h1.getNombre(), h2.getNombre(), 8);  // Eugenio Espejo ➔ Baca Ortiz
        redHospitales.addRuta(h3.getNombre(), h6.getNombre(), 12); // Conocoto ➔ Alangasí
    }

    private void cargarComboBoxes() {
        spnEdad.setModel(new SpinnerNumberModel(30, 0, 120, 1));

        if (cmbUrgencia.getItemCount() > 0) cmbUrgencia.removeAllItems();
        if (cmbHospitalTriage.getItemCount() > 0) cmbHospitalTriage.removeAllItems();
        if (cmbMedicina.getItemCount() > 0) cmbMedicina.removeAllItems();
        if (cmbMedicinaLote.getItemCount() > 0) cmbMedicinaLote.removeAllItems();
        if (cmbOrigen.getItemCount() > 0) cmbOrigen.removeAllItems();
        if (cmbDestino.getItemCount() > 0) cmbDestino.removeAllItems();

        cmbUrgencia.addItem("1 - ROJO (Resucitación)");
        cmbUrgencia.addItem("2 - NARANJA (Emergencia)");
        cmbUrgencia.addItem("3 - AMARILLO (Urgencia)");
        cmbUrgencia.addItem("4 - VERDE (Urgencia Menor)");
        cmbUrgencia.addItem("5 - AZUL (Sin Urgencia)");

        for (String med : bdMedicamentos.keySet()) {
            cmbMedicina.addItem(med);
            cmbMedicinaLote.addItem(med);
        }
        for (String hosp : bdHospitales.keySet()) {
            cmbHospitalTriage.addItem(hosp);
            cmbOrigen.addItem(hosp);
            cmbDestino.addItem(hosp);
        }
    }

    private void configurarTablas(){
        String[] columnasTriage = {"ID", "Paciente", "Fármaco Requerido", "Gravedad", "Centro Solicitante"};
        modeloTriage = new DefaultTableModel(null, columnasTriage);
        tablaTriage.setModel(modeloTriage);

        tablaTriage.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String nivel = (String) table.getValueAt(row, 3);
                if (nivel.equals("1")) c.setBackground(new Color(255, 102, 102));
                else if (nivel.equals("2")) c.setBackground(new Color(255, 178, 102));
                else if (nivel.equals("3")) c.setBackground(new Color(255, 255, 102));
                else if (nivel.equals("4")) c.setBackground(new Color(102, 255, 102));
                else c.setBackground(new Color(102, 178, 255));
                c.setForeground(Color.BLACK);
                if (isSelected) c.setBackground(Color.DARK_GRAY);
                return c;
            }
        });

        String[] columnasLogistica = {"Lote", "Medicamento", "Origen", "Destino"};
        modeloLogistica = new DefaultTableModel(null, columnasLogistica);
        tablaLogistica.setModel(modeloLogistica);
    }

    private void actualizarTablaTriage() {
        modeloTriage.setRowCount(0);
        PriorityQueue<Solicitud> copia = new PriorityQueue<>(colaTriage);
        while (!copia.isEmpty()) {
            Solicitud s = copia.poll();
            modeloTriage.addRow(new Object[]{
                    s.getIdSolicitud(), s.getNombrePaciente(), s.getMedicamento().getNombre(),
                    String.valueOf(s.getNivelUrgencia()), s.getHospitalSolicitante().getNombre()
            });
        }
    }

    private void actualizarTablaLogistica() {
        modeloLogistica.setRowCount(0);
        List<NodoLote> listaInorden = new ArrayList<>();
        motorLogistico.obtenerHistorialInorden(motorLogistico.getRaiz(), listaInorden);
        for (NodoLote nodo : listaInorden) {
            modeloLogistica.addRow(new Object[]{
                    nodo.getIdLote(), nodo.getMedicamento().getNombre(),
                    nodo.getOrigen().getNombre(), nodo.getDestino().getNombre()
            });
        }
        if (lblMetricas != null) lblMetricas.setText("Nodos en BST: " + listaInorden.size() + " | O(log n)");
    }

    private void limpiarCamposTriage() {
        txtId.setText(""); txtNombrePaciente.setText(""); txtSintomas.setText(""); spnEdad.setValue(30);
    }

    // =========================================================================
    // METODO MAIN: CON INICIO DE SESIÓN INTEGRADO (SISTEMA DE SEGURIDAD POR ROLES)
    // =========================================================================
    public static void main(String[] args) {
        // --- VENTANA DE LOGIN (Gatekeeper) ---
        JPanel panelLogin = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField campoUsuario = new JTextField();
        JPasswordField campoPassword = new JPasswordField();
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"Doctor / Doctora", "Enfermero / Enfermera"});

        panelLogin.add(new JLabel("Usuario Clínico (C.I.):"));
        panelLogin.add(campoUsuario);
        panelLogin.add(new JLabel("Contraseña Sanitaria:"));
        panelLogin.add(campoPassword);
        panelLogin.add(new JLabel("Rol de Acceso:"));
        panelLogin.add(cmbRol);

        int opcionLogin = JOptionPane.showConfirmDialog(null, panelLogin,
                "Autenticación SafeDose - MSP Ecuador", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opcionLogin == JOptionPane.OK_OPTION) {
            String usuario = campoUsuario.getText().trim();
            String password = new String(campoPassword.getPassword());
            String rol = cmbRol.getSelectedItem().toString();

            // Simulación de credenciales institucionales seguras
            if ((usuario.equals("doctor") && password.equals("1234")) || (usuario.equals("enfermera") && password.equals("5678"))) {

                JOptionPane.showMessageDialog(null, "Acceso concedido institucionalmente.\nRol: " + rol, "Credenciales Válidas", JOptionPane.INFORMATION_MESSAGE);

                // Ejecución segura de la Interfaz Principal
                JFrame frame = new JFrame("SafeDose Ecuador - Gestión Inteligente Hospitalaria");
                frame.setContentPane(new Ventana().principal);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Credenciales incorrectas o usuario no registrado en el Ministerio de Salud.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }
}