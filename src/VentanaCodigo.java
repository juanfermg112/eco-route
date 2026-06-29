import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class VentanaCodigo {

    private static final Color COLOR_ROJO     = new Color(220, 50,  50);
    private static final Color COLOR_AMARILLO = new Color(240, 200,  0);
    private static final Color COLOR_VERDE    = new Color(50,  160,  50);
    private static final Color COLOR_BLANCO   = new Color(220, 220, 220);
    private static final Color COLOR_AZUL     = new Color(50,  100, 200);

    // =================================================================
    // --- COMPONENTES DEL DISEÑADOR VISUAL (.FORM) UNIFICADOS ---
    // =================================================================
    private JPanel principal;
    private JTabbedPane tblHistorialTraslados;

    // Pestaña 1: Triage Médico
    private JTextField txtId;
    private JTextField txtNombrePaciente;
    private JComboBox<String> cmbGravedad;
    private JTable tblTriaje;
    private JSpinner spnEdad;
    private JTextField txtSintomas;
    private JComboBox<String> cmbHospitalTriage;
    private JComboBox<String> comboBox1;
    private JTextField textCantidad;
    private JButton btnIngresarOrden;
    private JButton btnAtenderSiguiente;
    private JButton deshacerLifoButton;

    // Pestaña 2: Control Recall
    private JComboBox<String> cmbHospitalOrigen;
    private JComboBox<String> cmbMedicamentoRecall;
    private JComboBox<String> cmbLoteTrasladar;
    private JComboBox<String> cmbHospitalDestino;
    private JButton btnAlertaCaducidad;
    private JButton btnVerInventarioLote;
    private JButton btnFallaLote;
    private JButton btnTrasladarLote;
    private JTable tblTraslado;

    // Pestaña 3: Agregar Lote
    private JTextField txtIdLotes;
    private JComboBox<String> cmbHospitalReceptor;
    private JComboBox<String> comboBox2;
    private JTextField txtCantidadUnidades;
    private JTextField txtDiasCaducar;
    private JButton btnRegistrarIngresoLote;
    private JScrollPane tblInventarioLote;
    private JTable tblInventariofi;

    // --- VARIABLES PASIVAS DE TOLERANCIA POR CACHÉ ---
    private JComboBox<String> cmbHospital;
    private JTable table1;
    private JScrollPane tblInventarioLotes;

    // --- MODELOS DE LAS TABLAS ---
    private DefaultTableModel modeloTriage;
    private DefaultTableModel modeloLogistica;
    private DefaultTableModel modeloInventario;

    // --- ESTRUCTURAS DE DATOS EN BACKEND ---
    private PriorityQueue<Solicitud> colaTriage = new PriorityQueue<>();
    private ArbolLotes motorLogistico = new ArbolLotes();
    private GrafoHospitales redHospitales;
    private Stack<Solicitud> pilaDeshacer = new Stack<>();

    private HashMap<String, Medicamento> bdMedicamentos = new HashMap<>();
    private HashMap<String, Hospital> bdHospitales = new HashMap<>();

    public VentanaCodigo() {
        inicializarBasesDeDatos();
        configurarTablas();
        cargarComboBoxes();
        registrarEventosUI();
    }

    private String normalizarNombre(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .trim();
    }

    private Hospital buscarHospitalSeguro(String nombreCombo) {
        if (nombreCombo == null) return null;
        String busqueda = normalizarNombre(nombreCombo);
        for (Map.Entry<String, Hospital> entrada : bdHospitales.entrySet()) {
            if (normalizarNombre(entrada.getKey()).equals(busqueda)) {
                return entrada.getValue();
            }
        }
        return null;
    }

    private void configurarTablas() {
        modeloTriage = new DefaultTableModel(
                null, new String[]{"ID", "Paciente", "Edad", "Síntomas", "Fármaco", "Cant. Nec.", "Nivel", "Centro Solicitante"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblTriaje.setModel(modeloTriage);
        tblTriaje.setRowHeight(22);

        DefaultTableCellRenderer rendererTriage = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tabla, Object valor, boolean selec, boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(tabla, valor, selec, foco, fila, col);
                if (!selec && tabla.getValueAt(fila, 6) != null) {
                    String nivelStr = tabla.getValueAt(fila, 6).toString();
                    if (nivelStr.contains("1") || nivelStr.contains("ROJO")) {
                        setBackground(COLOR_ROJO); setForeground(Color.WHITE);
                    } else if (nivelStr.contains("2") || nivelStr.contains("AMARILLO")) {
                        setBackground(COLOR_AMARILLO); setForeground(Color.BLACK);
                    } else if (nivelStr.contains("3") || nivelStr.contains("VERDE")) {
                        setBackground(COLOR_VERDE); setForeground(Color.WHITE);
                    } else if (nivelStr.contains("4") || nivelStr.contains("BLANCO")) {
                        setBackground(COLOR_BLANCO); setForeground(Color.BLACK);
                    } else if (nivelStr.contains("5") || nivelStr.contains("AZUL")) {
                        setBackground(COLOR_AZUL); setForeground(Color.WHITE);
                    } else {
                        setBackground(Color.WHITE); setForeground(Color.BLACK);
                    }
                }
                return this;
            }
        };

        for (int i = 0; i < 8; i++) tblTriaje.getColumnModel().getColumn(i).setCellRenderer(rendererTriage);

        ajustarAnchoColumnasDinamico();

        modeloLogistica = new DefaultTableModel(
                null, new String[]{"ID Lote", "Medicamento", "Origen", "Destino"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        if (tblTraslado != null) tblTraslado.setModel(modeloLogistica);

        modeloInventario = new DefaultTableModel(
                null, new String[]{"Hospital", "Medicamento", "ID Lote", "Cantidad", "Días p/ Caducar", "Estado"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (tblInventariofi != null) tblInventariofi.setModel(modeloInventario);
        if (table1 != null) table1.setModel(modeloInventario);
    }

    private void ajustarAnchoColumnasDinamico() {
        if (tblTriaje == null) return;
        tblTriaje.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        for (int col = 0; col < tblTriaje.getColumnCount(); col++) {
            int anchoMaximo = 70;

            Object headerValue = tblTriaje.getColumnModel().getColumn(col).getHeaderValue();
            if (headerValue != null) {
                anchoMaximo = Math.max(anchoMaximo, tblTriaje.getTableHeader().getFontMetrics(tblTriaje.getTableHeader().getFont()).stringWidth(headerValue.toString()) + 20);
            }

            for (int fila = 0; fila < tblTriaje.getRowCount(); fila++) {
                Object valor = tblTriaje.getValueAt(fila, col);
                if (valor != null) {
                    anchoMaximo = Math.max(anchoMaximo, tblTriaje.getFontMetrics(tblTriaje.getFont()).stringWidth(valor.toString()) + 25);
                }
            }
            tblTriaje.getColumnModel().getColumn(col).setPreferredWidth(anchoMaximo);
        }
    }

    private void cargarComboBoxes() {
        spnEdad.setModel(new SpinnerNumberModel(30, 0, 110, 1));

        cmbGravedad.removeAllItems();
        cmbGravedad.addItem("1 — ROJO    | Emergencia Vital (atención inmediata)");
        cmbGravedad.addItem("2 — AMARILLO| Urgencia inminente (máx. 30 min)");
        cmbGravedad.addItem("3 — VERDE   | Urgencia moderada (hasta 120 min)");
        cmbGravedad.addItem("4 — BLANCO  | Consulta priorizada");
        cmbGravedad.addItem("5 — AZUL    | Consulta no priorizada");

        for (String med : bdMedicamentos.keySet()) {
            if (comboBox1 != null) comboBox1.addItem(med);
            if (cmbMedicamentoRecall != null) cmbMedicamentoRecall.addItem(med);
            if (comboBox2 != null) comboBox2.addItem(med);
        }
        for (String hosp : bdHospitales.keySet()) {
            if (cmbHospitalTriage != null) cmbHospitalTriage.addItem(hosp);
            if (cmbHospital != null) cmbHospital.addItem(hosp);
            if (cmbHospitalOrigen != null) cmbHospitalOrigen.addItem(hosp);
            if (cmbHospitalDestino != null) cmbHospitalDestino.addItem(hosp);
            if (cmbHospitalReceptor != null) cmbHospitalReceptor.addItem(hosp);
        }

        refrescarTablaInventario();
        recargarLotesCombo();
    }

    private void registrarEventosUI() {
        // ==========================================
        // ACCIONES DE LA PESTAÑA 1: TRIAGE MÉDICO
        // ==========================================
        btnIngresarOrden.addActionListener(e -> {
            try {
                String id = txtId.getText().trim();
                String nombre = txtNombrePaciente.getText().trim();
                int edad = (int) spnEdad.getValue();
                String sintomas = txtSintomas.getText().trim();

                if (id.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "❌ Error: El ID de la solicitud es obligatorio.", "Campo Vacío", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!id.matches("^[a-zA-Z0-9-]+$") || id.startsWith("-")) {
                    JOptionPane.showMessageDialog(null, "❌ Error: El ID no puede ser un número negativo ni contener caracteres especiales.", "ID Inválido", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (edad > 110) {
                    JOptionPane.showMessageDialog(null, "❌ Error: La edad ingresada no puede ser mayor a 110 años.", "Edad Inválida", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (nombre.isEmpty() || sintomas.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "❌ Error: Todos los campos son obligatorios.", "Campos Incompletos", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Hospital hosp = null;
                if (cmbHospitalTriage != null && cmbHospitalTriage.getSelectedItem() != null) {
                    hosp = buscarHospitalSeguro(cmbHospitalTriage.getSelectedItem().toString());
                } else if (cmbHospital != null && cmbHospital.getSelectedItem() != null) {
                    hosp = buscarHospitalSeguro(cmbHospital.getSelectedItem().toString());
                }
                if (hosp == null) hosp = bdHospitales.get("Hospital Eugenio Espejo");

                Medicamento med = null;
                if (comboBox1 != null && comboBox1.getSelectedItem() != null) {
                    med = bdMedicamentos.get(comboBox1.getSelectedItem().toString());
                }
                if (med == null) med = bdMedicamentos.get("Paracetamol IV");

                int urgenciaIdx = cmbGravedad.getSelectedIndex() + 1;

                int cant = 1;
                if (textCantidad != null && textCantidad.getText() != null) {
                    try {
                        String textoCant = textCantidad.getText().trim();
                        if (!textoCant.isEmpty()) cant = Integer.parseInt(textoCant);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "❌ Error: La cantidad debe ser un entero mayor a cero.", "Cantidad Inválida", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                if (cant <= 0) {
                    JOptionPane.showMessageDialog(null, "❌ Error: La cantidad debe ser un entero mayor a cero.", "Cantidad Inválida", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (Solicitud s : colaTriage) {
                    if (s.getIdSolicitud().equalsIgnoreCase(id)) {
                        JOptionPane.showMessageDialog(null, "❌ Error: Ya existe una solicitud con el ID: " + id, "ID Duplicado", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                Solicitud nuevaSolicitud = new Solicitud(id, nombre, edad, sintomas, hosp, med, urgenciaIdx);
                colaTriage.add(nuevaSolicitud);
                pilaDeshacer.push(nuevaSolicitud);

                actualizarTablaTriage();
                limpiarCamposTriage();
                JOptionPane.showMessageDialog(null, "✅ Orden de Triaje registrada con éxito.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error inesperado al procesar el ingreso: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnAtenderSiguiente.addActionListener(e -> {
            if (colaTriage.isEmpty()) {
                JOptionPane.showMessageDialog(null, "La cola de triage está vacía."); return;
            }

            Solicitud atendida = colaTriage.poll();
            pilaDeshacer.remove(atendida);
            Hospital hosp = atendida.getHospitalSolicitante();
            Medicamento med = atendida.getMedicamento();

            int cantSolicitada = 1;
            if (textCantidad != null && textCantidad.getText() != null) {
                try {
                    cantSolicitada = Integer.parseInt(textCantidad.getText().trim());
                } catch (Exception ex) {
                    cantSolicitada = 1;
                }
            }
            if (cantSolicitada <= 0) cantSolicitada = 1;

            String[] etiquetas = {"", "🔴 ROJO", "🟡 AMARILLO", "🟢 VERDE", "⬜ BLANCO", "🔵 AZUL"};
            String nivelStr = etiquetas[Math.min(atendida.getNivelUrgencia(), 5)];

            StringBuilder msj = new StringBuilder();
            msj.append("═══════════════════════════════\n");
            msj.append("  ORDEN ATENDIDA — ").append(nivelStr).append("\n");
            msj.append("═══════════════════════════════\n");
            msj.append("Paciente  : ").append(atendida.getNombrePaciente()).append("\n");
            msj.append("Hospital  : ").append(hosp.getNombre()).append("\n");
            msj.append("Fármaco   : ").append(med.getNombre()).append("\n");
            msj.append("Cantidad  : ").append(cantSolicitada).append(" unidades\n");
            msj.append("───────────────────────────────\n");

            int stockLocal = hosp.getStockTotal(med.getNombre());

            if (stockLocal >= cantSolicitada) {
                hosp.despacharMedicina(med.getNombre(), cantSolicitada);
                msj.append("✅ FUENTE: Stock propio de ").append(hosp.getNombre()).append("\n");
                msj.append("   Lotes despachados por FEFO.\n");
                msj.append("   Stock restante: ").append(hosp.getStockTotal(med.getNombre())).append(" un.\n");
            } else {
                msj.append("⚠️  Stock local insuficiente (").append(stockLocal).append(" un.)\n");
                msj.append("   Buscando en la Red de Salud...\n\n");

                Hospital mejorFuente = null;
                int tiempoMinimo = Integer.MAX_VALUE;

                for (Hospital candidato : bdHospitales.values()) {
                    if (candidato != hosp && candidato.getStockTotal(med.getNombre()) >= cantSolicitada) {
                        int t = redHospitales.obtenerTiempoRutaInt(candidato.getNombre(), hosp.getNombre());
                        if (t != -1 && t < tiempoMinimo) {
                            tiempoMinimo = t;
                            mejorFuente = candidato;
                        }
                    }
                }

                if (mejorFuente != null) {
                    mejorFuente.despacharMedicina(med.getNombre(), cantSolicitada);
                    msj.append("🚚 FUENTE: ").append(mejorFuente.getNombre()).append("\n");
                    msj.append("   ➔ Destino: ").append(hosp.getNombre()).append("\n");
                    msj.append("   ⏳ Tiempo de llegada (Dijkstra): ").append(tiempoMinimo).append(" min\n");
                    msj.append("   Stock restante en fuente: ").append(mejorFuente.getStockTotal(med.getNombre())).append(" un.\n");
                } else {
                    msj.append("❌ COLAPSO LOGÍSTICO\n");
                    msj.append("   Ningún hospital de la red de distribución cuenta con suficiente stock.\n");
                }
            }
            msj.append("═══════════════════════════════");

            JTextArea area = new JTextArea(msj.toString());
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JOptionPane.showMessageDialog(null, area, "Sistema Inteligente de Triage", JOptionPane.INFORMATION_MESSAGE);
            actualizarTablaTriage();
            refrescarTablaInventario();
        });

        deshacerLifoButton.addActionListener(e -> {
            if (!pilaDeshacer.isEmpty()) {
                Solicitud deshecha = pilaDeshacer.pop();
                colaTriage.remove(deshecha);
                JOptionPane.showMessageDialog(null, "🔄 Eliminado de la cola: " + deshecha.getNombrePaciente());
                actualizarTablaTriage();
            } else {
                JOptionPane.showMessageDialog(null, "No hay ingresos recientes para deshacer.");
            }
        });

        // ==========================================
        // ACCIONES DE LA PESTAÑA 2: CONTROL RECALL
        // ==========================================
        if (cmbHospitalOrigen != null) cmbHospitalOrigen.addActionListener(e -> recargarLotesCombo());
        if (cmbMedicamentoRecall != null) cmbMedicamentoRecall.addActionListener(e -> recargarLotesCombo());

        btnTrasladarLote.addActionListener(e -> {
            if (cmbLoteTrasladar.getItemCount() == 0 || cmbLoteTrasladar.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(null, "No hay lotes disponibles con esa combinación.");
                return;
            }

            String nomOrigen = cmbHospitalOrigen.getSelectedItem().toString();
            String nomDestino = cmbHospitalDestino.getSelectedItem().toString();
            String nomMed = cmbMedicamentoRecall.getSelectedItem().toString();
            String idLote = cmbLoteTrasladar.getSelectedItem().toString().split(" ")[0];

            if (nomOrigen.equals(nomDestino)) {
                JOptionPane.showMessageDialog(null, "El origen y destino no pueden ser el mismo hospital.");
                return;
            }

            Hospital origen = buscarHospitalSeguro(nomOrigen);
            Hospital destino = buscarHospitalSeguro(nomDestino);

            String ruta = redHospitales.buscarRutaMasRapida(origen.getNombre(), destino.getNombre());
            if (ruta.contains("No existe")) {
                JOptionPane.showMessageDialog(null, "❌ No hay ruta válida en el grafo entre esos hospitales.");
                return;
            }

            LoteInventario loteMovido = origen.extraerLotePorId(idLote, nomMed);
            if (loteMovido == null) {
                JOptionPane.showMessageDialog(null, "No se encontró el lote en el inventario de origen.");
                return;
            }

            destino.recibirLote(loteMovido);
            motorLogistico.registrarLote(idLote, loteMovido.getMedicamento(), origen, destino);

            JOptionPane.showMessageDialog(null,
                    "✅ TRASLADO REGISTRADO\n"
                            + "Lote     : " + idLote + "\n"
                            + "Origen   : " + origen.getNombre() + "\n"
                            + "Destino  : " + destino.getNombre() + "\n"
                            + "Ruta     : " + ruta,
                    "Traslado Exitoso", JOptionPane.INFORMATION_MESSAGE);

            actualizarTablaLogistica();
            refrescarTablaInventario();
            recargarLotesCombo();
        });

        btnFallaLote.addActionListener(e -> {
            String idBuscado = JOptionPane.showInputDialog(null,
                    "Ingrese el ID del Lote bajo sospecha sanitaria:",
                    "⛔ Activar Recall", JOptionPane.WARNING_MESSAGE);
            if (idBuscado == null || idBuscado.trim().isEmpty()) return;
            idBuscado = idBuscado.trim();

            if (!idBuscado.matches("^[a-zA-Z0-9-]+$") || idBuscado.startsWith("-")) {
                JOptionPane.showMessageDialog(null, "❌ Error: El ID ingresado no es válido.", "ID Inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }

            NodoLote nodoEncontrado = motorLogistico.buscarLote(idBuscado);

            boolean borrado = false;
            int hospitalesAfectados = 0;
            for (Hospital h : bdHospitales.values()) {
                if (h.retirarLotePorId(idBuscado)) {
                    borrado = true;
                    hospitalesAfectados++;
                }
            }

            if (borrado) {
                StringBuilder msj = new StringBuilder();
                msj.append("⛔ RECALL EJECUTADO — LOTE RETIRADO\n\n");
                msj.append("ID Lote  : ").append(idBuscado).append("\n");
                msj.append("Afectó   : ").append(hospitalesAfectados).append(" hospital(es)\n");
                if (nodoEncontrado != null) {
                    msj.append("Trazabilidad  : ").append(nodoEncontrado.getOrigen().getNombre())
                            .append(" ➔ ").append(nodoEncontrado.getDestino().getNombre()).append("\n");
                }
                JOptionPane.showMessageDialog(null, msj.toString(), "Contención Sanitaria Completada", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "El lote '" + idBuscado + "' no existe en la red nacional.", "Búsqueda Fallida", JOptionPane.INFORMATION_MESSAGE);
            }

            refrescarTablaInventario();
            recargarLotesCombo();
        });

        btnVerInventarioLote.addActionListener(e -> {
            StringBuilder sb = new StringBuilder("=== KARDEX DETALLADO POR LOTES ===\n\n");
            for (Hospital h : bdHospitales.values()) {
                sb.append("🏥 ").append(h.getNombre()).append("\n");
                boolean tieneStock = false;
                for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                    PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                    while (!copia.isEmpty()) {
                        LoteInventario l = copia.poll();
                        sb.append("   [").append(l.getIdLote()).append("] ").append(entrada.getKey()).append(" | ").append(l.getCantidad()).append(" un.\n");
                        tieneStock = true;
                    }
                }
                if (!tieneStock) sb.append("   (Bodega vacía)\n");
                sb.append("\n");
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(650, 420));
            JOptionPane.showMessageDialog(null, scroll, "Inventario Detallado", JOptionPane.INFORMATION_MESSAGE);
        });

        btnAlertaCaducidad.addActionListener(e -> {
            List<String[]> filas = new ArrayList<>();
            for (Hospital h : bdHospitales.values()) {
                for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                    PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                    while (!copia.isEmpty()) {
                        LoteInventario l = copia.poll();
                        filas.add(new String[]{h.getNombre(), l.getIdLote(), l.getMedicamento().getNombre(), String.valueOf(l.getCantidad()), String.valueOf(l.getDiasParaCaducar())});
                    }
                }
            }
            if (filas.isEmpty()) { JOptionPane.showMessageDialog(null, "Sin inventario."); return; }

            int n = filas.size();
            for (int i = 0; i < n - 1; i++)
                for (int j = 0; j < n - i - 1; j++)
                    if (Integer.parseInt(filas.get(j)[4]) > Integer.parseInt(filas.get(j+1)[4])) {
                        String[] tmp = filas.get(j); filas.set(j, filas.get(j+1)); filas.set(j+1, tmp);
                    }

            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Hospital","ID Lote","Medicamento","Cant.","Días","Estado"}, 0);
            for (String[] f : filas) {
                int dias = Integer.parseInt(f[4]);
                modelo.addRow(new Object[]{f[0],f[1],f[2],f[3],dias, dias<=30?"⛔ CRÍTICO":dias<=90?"⚠️ ALERTA":"✅ OK"});
            }
            JTable tabla = new JTable(modelo);
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(800, 350));
            JOptionPane.showMessageDialog(null, scroll, "Alerta Caducidad — Bubble Sort O(n²)", JOptionPane.WARNING_MESSAGE);
        });

        // ==========================================
        // ACCIONES DE LA PESTAÑA 3: AGREGAR LOTE (FEFO)
        // ==========================================
        btnRegistrarIngresoLote.addActionListener(e -> {
            String idLote = txtIdLotes.getText().trim();
            if (cmbHospitalReceptor.getSelectedItem() == null || comboBox2.getSelectedItem() == null) return;

            String nomHosp = cmbHospitalReceptor.getSelectedItem().toString();
            String nomMed = comboBox2.getSelectedItem().toString();

            if (idLote.isEmpty()) {
                JOptionPane.showMessageDialog(null, "❌ Error: El ID del lote es obligatorio.", "Campo Vacío", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!idLote.matches("^[a-zA-Z0-9-]+$") || idLote.startsWith("-")) {
                JOptionPane.showMessageDialog(null, "❌ Error: El ID del Lote no puede ser un número negativo ni contener caracteres especiales.", "ID Inválido", JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Hospital h : bdHospitales.values()) {
                for (PriorityQueue<LoteInventario> cola : h.getBodegas().values()) {
                    for (LoteInventario loteExistente : cola) {
                        if (loteExistente.getIdLote().equalsIgnoreCase(idLote)) {
                            JOptionPane.showMessageDialog(null, "❌ Error Sanitario: El lote '" + idLote + "' ya existe en " + h.getNombre(), "Lote Duplicado", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }

            try {
                int cant = Integer.parseInt(txtCantidadUnidades.getText().trim());
                int dias = Integer.parseInt(txtDiasCaducar.getText().trim());

                if (cant <= 0) {
                    JOptionPane.showMessageDialog(null, "❌ Error: La cantidad ingresada debe ser un número entero estrictamente mayor a cero.", "Cantidad Inválida", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (dias <= 0) {
                    JOptionPane.showMessageDialog(null, "❌ Error: Los días para caducar deben ser estrictamente mayores a cero.", "Días Inválidos", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Hospital hosp = buscarHospitalSeguro(nomHosp);
                Medicamento med = bdMedicamentos.get(nomMed);
                hosp.recibirLote(new LoteInventario(idLote, med, cant, dias));

                JOptionPane.showMessageDialog(null, "✅ LOTE REGISTRADO CON ÉXITO\nHospital: " + hosp.getNombre() + "\nLote: " + idLote);

                txtIdLotes.setText(""); txtCantidadUnidades.setText(""); txtDiasCaducar.setText("");
                refrescarTablaInventario();
                recargarLotesCombo();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "❌ Error: Cantidad y días de caducidad deben ser números enteros válidos.", "Error Formato", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void recargarLotesCombo() {
        if (cmbLoteTrasladar == null) return;
        cmbLoteTrasladar.removeAllItems();
        if (cmbHospitalOrigen.getSelectedItem() == null || cmbMedicamentoRecall.getSelectedItem() == null) return;
        String nomHosp = cmbHospitalOrigen.getSelectedItem().toString();
        String nomMed = cmbMedicamentoRecall.getSelectedItem().toString();
        Hospital hosp = buscarHospitalSeguro(nomHosp);
        if (hosp == null) return;
        for (LoteInventario l : hosp.getLotesDisponibles(nomMed)) {
            cmbLoteTrasladar.addItem(l.getIdLote() + " | " + l.getCantidad() + " un. | " + l.getDiasParaCaducar() + " días");
        }
    }

    private void refrescarTablaInventario() {
        if (modeloInventario == null) return;
        modeloInventario.setRowCount(0);
        for (Hospital h : bdHospitales.values()) {
            for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                while (!copia.isEmpty()) {
                    LoteInventario l = copia.poll();
                    String est = l.getDiasParaCaducar() <= 30 ? "⛔ CRÍTICO" : l.getDiasParaCaducar() <= 90 ? "⚠️ ALERTA"  : "✅ OK";
                    modeloInventario.addRow(new Object[]{
                            h.getNombre(), l.getMedicamento().getNombre(),
                            l.getIdLote(), l.getCantidad(), l.getDiasParaCaducar(), est});
                }
            }
        }
    }

    private void actualizarTablaTriage() {
        if (modeloTriage == null) return;
        modeloTriage.setRowCount(0);
        PriorityQueue<Solicitud> copia = new PriorityQueue<>(colaTriage);

        String cantUI = "1";
        if (textCantidad != null && !textCantidad.getText().trim().isEmpty()) {
            cantUI = textCantidad.getText().trim();
        }

        while (!copia.isEmpty()) {
            Solicitud s = copia.poll();
            modeloTriage.addRow(new Object[]{
                    s.getIdSolicitud(),
                    s.getNombrePaciente(),
                    s.getEdad(),
                    s.getSintomas(),
                    s.getMedicamento().getNombre(),
                    cantUI + " un.",
                    s.getNivelUrgencia(),
                    (s.getHospitalSolicitante() != null ? s.getHospitalSolicitante().getNombre() : "Desconocido")
            });
        }
    }

    private void actualizarTablaLogistica() {
        if (modeloLogistica == null) return;
        modeloLogistica.setRowCount(0);
        List<NodoLote> lista = new ArrayList<>();
        motorLogistico.obtenerHistorialInorden(motorLogistico.getRaiz(), lista);
        for (NodoLote n : lista)
            modeloLogistica.addRow(new Object[]{
                    n.getIdLote(), n.getMedicamento().getNombre(),
                    n.getOrigen().getNombre(), n.getDestino().getNombre()});
    }

    private void limpiarCamposTriage() {
        txtId.setText("");
        txtNombrePaciente.setText("");
        txtSintomas.setText("");
    }

    private void inicializarBasesDeDatos() {
        bdMedicamentos.put("Fentanilo IV", new Medicamento("Fentanilo IV", false, true));
        bdMedicamentos.put("Morfina Ampollas", new Medicamento("Morfina Ampollas", false, true));
        bdMedicamentos.put("Insulina Glargina", new Medicamento("Insulina Glargina", true,  false));
        bdMedicamentos.put("Vacuna Pfizer COVID19", new Medicamento("Vacuna Pfizer COVID19", true,  false));
        bdMedicamentos.put("Amoxicilina Suspensión", new Medicamento("Amoxicilina Suspensión", false, false));
        bdMedicamentos.put("Paracetamol IV", new Medicamento("Paracetamol IV", false, false));
        bdMedicamentos.put("Epinefrina", new Medicamento("Epinefrina", true,  false));
        bdMedicamentos.put("Salbutamol Inhalador", new Medicamento("Salbutamol Inhalador", false, false));
        bdMedicamentos.put("Metformina 500mg", new Medicamento("Metformina 500mg", false, false));
        bdMedicamentos.put("Omeprazol IV", new Medicamento("Omeprazol IV", false, false));

        Hospital h1  = new Hospital("Hospital Eugenio Espejo", 3, true,  true);
        Hospital h2  = new Hospital("Hospital Baca Ortiz (Pediatría)", 3, true,  true);
        Hospital h3  = new Hospital("Hospital Pablo Arturo Suárez", 3, true,  true);
        Hospital h4  = new Hospital("Hospital Enrique Garcés", 3, true,  true);
        Hospital h5  = new Hospital("Centro de Salud Conocoto", 1, false, false);
        Hospital h6  = new Hospital("Centro de Salud Calderón", 1, false, false);
        Hospital h7  = new Hospital("Centro de Salud Quitumbe", 1, false, false);
        Hospital h8  = new Hospital("Centro de Salud Cotocollao", 1, false, false);
        Hospital h9  = new Hospital("Clínica Pichincha (Convenio)", 2, true,  false);
        Hospital h10 = new Hospital("Bodega MSP Central Zonal 9", 0, true,  true);

        h10.recibirLote(new LoteInventario("LT-MSP-01", bdMedicamentos.get("Paracetamol IV"),        2000, 300));
        h10.recibirLote(new LoteInventario("LT-MSP-02", bdMedicamentos.get("Paracetamol IV"),         500,  90));
        h10.recibirLote(new LoteInventario("LT-MSP-03", bdMedicamentos.get("Fentanilo IV"),           500, 180));
        h10.recibirLote(new LoteInventario("LT-MSP-04", bdMedicamentos.get("Vacuna Pfizer COVID19"), 1000,  20));
        h10.recibirLote(new LoteInventario("LT-MSP-05", bdMedicamentos.get("Insulina Glargina"),      300, 120));
        h10.recibirLote(new LoteInventario("LT-MSP-06", bdMedicamentos.get("Metformina 500mg"),      5000, 365));

        h1.recibirLote(new LoteInventario("LT-EE-01", bdMedicamentos.get("Paracetamol IV"),  100,  10));
        h1.recibirLote(new LoteInventario("LT-EE-02", bdMedicamentos.get("Epinefrina"),       50, 200));
        h1.recibirLote(new LoteInventario("LT-EE-03", bdMedicamentos.get("Omeprazol IV"),    200,  45));

        h2.recibirLote(new LoteInventario("LT-BO-01", bdMedicamentos.get("Amoxicilina Suspensión"), 300, 150));
        h2.recibirLote(new LoteInventario("LT-BO-02", bdMedicamentos.get("Morfina Ampollas"),        80,  25));
        h2.recibirLote(new LoteInventario("LT-BO-03", bdMedicamentos.get("Insulina Glargina"),      120, 210));

        h3.recibirLote(new LoteInventario("LT-PAS-01", bdMedicamentos.get("Salbutamol Inhalador"), 400, 180));
        h3.recibirLote(new LoteInventario("LT-PAS-02", bdMedicamentos.get("Metformina 500mg"),      600,  60));

        h4.recibirLote(new LoteInventario("LT-EG-01", bdMedicamentos.get("Paracetamol IV"), 250,  75));
        h4.recibirLote(new LoteInventario("LT-EG-02", bdMedicamentos.get("Omeprazol IV"),   100, 320));

        h9.recibirLote(new LoteInventario("LT-CP-01", bdMedicamentos.get("Epinefrina"),        30,  15));
        h9.recibirLote(new LoteInventario("LT-CP-02", bdMedicamentos.get("Insulina Glargina"),  60, 100));

        bdHospitales.put(h1.getNombre(), h1);   bdHospitales.put(h2.getNombre(), h2);
        bdHospitales.put(h3.getNombre(), h3);   bdHospitales.put(h4.getNombre(), h4);
        bdHospitales.put(h5.getNombre(), h5);   bdHospitales.put(h6.getNombre(), h6);
        bdHospitales.put(h7.getNombre(), h7);   bdHospitales.put(h8.getNombre(), h8);
        bdHospitales.put(h9.getNombre(), h9);   bdHospitales.put(h10.getNombre(), h10);

        redHospitales = new GrafoHospitales(10);
        for (Hospital h : new Hospital[]{h1,h2,h3,h4,h5,h6,h7,h8,h9,h10})
            redHospitales.agregarHospital(h);

        redHospitales.addRuta(h10.getNombre(), h1.getNombre(),  15);
        redHospitales.addRuta(h10.getNombre(), h2.getNombre(),  20);
        redHospitales.addRuta(h10.getNombre(), h3.getNombre(),  25);
        redHospitales.addRuta(h10.getNombre(), h4.getNombre(),  18);
        redHospitales.addRuta(h10.getNombre(), h5.getNombre(),  35);
        redHospitales.addRuta(h10.getNombre(), h6.getNombre(),  30);
        redHospitales.addRuta(h10.getNombre(), h7.getNombre(),  28);
        redHospitales.addRuta(h10.getNombre(), h8.getNombre(),  22);
        redHospitales.addRuta(h1.getNombre(),  h2.getNombre(),   8);
        redHospitales.addRuta(h1.getNombre(),  h3.getNombre(),  12);
        redHospitales.addRuta(h1.getNombre(),  h9.getNombre(),   5);
        redHospitales.addRuta(h2.getNombre(),  h4.getNombre(),  10);
        redHospitales.addRuta(h3.getNombre(),  h8.getNombre(),  15);
        redHospitales.addRuta(h4.getNombre(),  h7.getNombre(),  12);
        redHospitales.addRuta(h5.getNombre(),  h7.getNombre(),  20);
        redHospitales.addRuta(h6.getNombre(),  h8.getNombre(),  18);
        redHospitales.addRuta(h9.getNombre(),  h10.getNombre(), 10);
    }

    // =================================================================
    // --- NUEVO MÉTODO: GESTIÓN DE ACCESO DE USUARIOS CLINICOS ---
    // =================================================================
    private static boolean mostrarLogin() {
        HashMap<String, String> credenciales = new HashMap<>();
        credenciales.put("admin", "admin123");
        credenciales.put("doctor", "doc456");
        credenciales.put("farmacia", "far789");
        credenciales.put("auditor", "aud000");

        JTextField txtUsuario = new JTextField(15);
        JPasswordField txtClave = new JPasswordField(15);
        JComboBox<String> cmbRol = new JComboBox<>(new String[]{"Doctor / Doctora", "Administrador/a", "Farmacéutico/a", "Auditor/a"});

        JPanel panelLogin = new JPanel(new GridLayout(3, 2, 5, 5));
        panelLogin.add(new JLabel("Usuario Clínico (C.I.):"));
        panelLogin.add(txtUsuario);
        panelLogin.add(new JLabel("Contraseña Sanitaria:"));
        panelLogin.add(txtClave);
        panelLogin.add(new JLabel("Rol de Acceso:"));
        panelLogin.add(cmbRol);

        int resultado = JOptionPane.showConfirmDialog(null, panelLogin,
                "Autenticación SafeDose - MSP Ecuador", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (resultado == JOptionPane.OK_OPTION) {
            String usuarioInput = txtUsuario.getText().trim();
            String claveInput = new String(txtClave.getPassword());

            if (credenciales.containsKey(usuarioInput) && credenciales.get(usuarioInput).equals(claveInput)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "❌ Credenciales de Salud Incorrectas.", "Acceso Denegado", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // Ejecutamos la autenticación antes de cargar la maquetación gráfica principal
        if (!mostrarLogin()) {
            System.exit(0); // Cierra la ejecución si cancelan o fallan las credenciales
        }

        JFrame frame = new JFrame("SafeDose Ecuador — v3");
        frame.setContentPane(new VentanaCodigo().principal);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(860, 650));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}