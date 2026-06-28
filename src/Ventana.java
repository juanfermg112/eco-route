import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Ventana {

    // ============================================================
    // COLORES DE TRIAGE (imagen de referencia MSP)
    // ============================================================
    private static final Color COLOR_ROJO    = new Color(220, 50,  50);   // Nivel 1
    private static final Color COLOR_AMARILLO= new Color(240, 200,  0);   // Nivel 2
    private static final Color COLOR_VERDE   = new Color(50,  160,  50);  // Nivel 3
    private static final Color COLOR_BLANCO  = new Color(220, 220, 220);  // Nivel 4
    private static final Color COLOR_AZUL    = new Color(50,  100, 200);  // Nivel 5

    // ============================================================
    // COMPONENTES UI — Tab Triage
    // ============================================================
    private JPanel principal;
    private JTabbedPane tabPrincipal;
    private JTextField txtId;
    private JTextField txtNombrePaciente;
    private JSpinner   spnEdad;
    private JTextField txtSintomas;
    private JComboBox<String> cmbHospitalTriage;
    private JComboBox<String> cmbMedicina;
    private JComboBox<String> cmbUrgencia;
    private JTextField txtCantidadTriage;
    private JButton btnIngresar;
    private JButton btnSiguiente;
    private JButton btnDeshacer;
    private JTable  tablaTriage;
    private DefaultTableModel modeloTriage;

    // ============================================================
    // COMPONENTES UI — Tab Logística
    // ============================================================
    private JComboBox<String> cmbOrigenTraslado;
    private JComboBox<String> cmbDestinoTraslado;
    private JComboBox<String> cmbMedTraslado;
    private JComboBox<String> cmbLoteDisponible;  // solo lotes del origen
    private JButton btnTraslado;
    private JButton btnRecall;
    private JButton btnVerInventario;
    private JButton btnOrdenamiento;
    private JTable  tablaLogistica;
    private DefaultTableModel modeloLogistica;

    // ============================================================
    // COMPONENTES UI — Tab Agregar Lote
    // ============================================================
    private JComboBox<String> cmbHospitalLote;
    private JComboBox<String> cmbMedicinaNueva;
    private JTextField txtIdLoteNuevo;
    private JTextField txtCantidadLote;
    private JTextField txtDiasCaducidad;
    private JButton    btnAgregarLote;
    private JTable     tablaInventarioDetalle;
    private DefaultTableModel modeloInventario;

    // ============================================================
    // MOTORES DE DATOS
    // ============================================================
    private PriorityQueue<Solicitud> colaTriage     = new PriorityQueue<>();
    private ArbolLotes               motorLogistico  = new ArbolLotes();
    private GrafoHospitales          redHospitales;
    private Stack<Solicitud>         pilaDeshacer    = new Stack<>();

    private HashMap<String, Medicamento> bdMedicamentos = new HashMap<>();
    private HashMap<String, Hospital>    bdHospitales   = new HashMap<>();

    // ============================================================
    // CONSTRUCTOR — arranca con login
    // ============================================================
    public Ventana() {
        inicializarBasesDeDatos();
        if (!mostrarLogin()) {
            System.exit(0); // Si cancela o falla el login, cierra
        }
        construirUI();
        configurarTablas();
        cargarComboBoxes();
        registrarEventos();
    }

    // ============================================================
    // LOGIN
    // Usuarios hardcodeados por perfil (sin BD externa).
    // HashMap<usuario, contraseña> — búsqueda O(1).
    // ============================================================
    private boolean mostrarLogin() {
        HashMap<String, String> credenciales = new HashMap<>();
        credenciales.put("admin",       "admin123");   // Administrador MSP
        credenciales.put("doctor",      "doc456");     // Médico Solicitante
        credenciales.put("farmacia",    "far789");     // Farmacéutico
        credenciales.put("auditor",     "aud000");     // Auditor

        // Panel del formulario de login
        JTextField txtUsuario = new JTextField(15);
        JPasswordField txtClave = new JPasswordField(15);

        JPanel panelLogin = new JPanel(new GridLayout(0, 2, 8, 8));
        panelLogin.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelLogin.add(new JLabel("Usuario:"));    panelLogin.add(txtUsuario);
        panelLogin.add(new JLabel("Contraseña:")); panelLogin.add(txtClave);

        // Intentos máximos: 3
        for (int intento = 1; intento <= 3; intento++) {
            int resultado = JOptionPane.showConfirmDialog(
                    null, panelLogin,
                    "SafeDose Ecuador — Inicio de Sesión (" + intento + "/3)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (resultado != JOptionPane.OK_OPTION) return false; // Canceló

            String usuario = txtUsuario.getText().trim().toLowerCase();
            String clave   = new String(txtClave.getPassword()).trim();

            if (credenciales.containsKey(usuario) && credenciales.get(usuario).equals(clave)) {
                JOptionPane.showMessageDialog(null,
                        "✅ Bienvenido, " + usuario.toUpperCase() + "\nAcceso concedido al sistema SafeDose.",
                        "Autenticación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(null,
                        "❌ Credenciales incorrectas. Intento " + intento + " de 3.",
                        "Error de Acceso", JOptionPane.ERROR_MESSAGE);
                txtUsuario.setText(""); txtClave.setText("");
            }
        }
        JOptionPane.showMessageDialog(null, "🔒 Acceso bloqueado. Contacte al administrador.");
        return false;
    }

    // ============================================================
    // CONSTRUCCIÓN DE LA UI
    // ============================================================
    private void construirUI() {
        principal    = new JPanel(new BorderLayout());
        tabPrincipal = new JTabbedPane();

        tabPrincipal.addTab("🚨 Triage Médico",  construirPanelTriage());
        tabPrincipal.addTab("🚚 Control Recall", construirPanelLogistica());
        tabPrincipal.addTab("📦 Agregar Lote",   construirPanelAgregarLote());

        principal.add(tabPrincipal, BorderLayout.CENTER);
    }

    /** Panel de Triage: formulario de ingreso + tabla con colores por nivel */
    private JPanel construirPanelTriage() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        txtId             = new JTextField();
        txtNombrePaciente = new JTextField();
        spnEdad           = new JSpinner(new SpinnerNumberModel(30, 0, 120, 1));
        txtSintomas       = new JTextField();
        cmbHospitalTriage = new JComboBox<>();
        cmbMedicina       = new JComboBox<>();
        cmbUrgencia       = new JComboBox<>();
        txtCantidadTriage = new JTextField("1");

        form.add(new JLabel("ID Solicitud:"));         form.add(txtId);
        form.add(new JLabel("Nombre Paciente:"));      form.add(txtNombrePaciente);
        form.add(new JLabel("Edad:"));                 form.add(spnEdad);
        form.add(new JLabel("Síntomas:"));             form.add(txtSintomas);
        form.add(new JLabel("Hospital Solicitante:")); form.add(cmbHospitalTriage);
        form.add(new JLabel("Medicamento:"));          form.add(cmbMedicina);
        form.add(new JLabel("Nivel de Urgencia:"));    form.add(cmbUrgencia);
        form.add(new JLabel("Cantidad necesaria:"));   form.add(txtCantidadTriage);

        JPanel botones = new JPanel(new FlowLayout());
        btnIngresar  = new JButton("Ingresar Orden");
        btnSiguiente = new JButton("Atender Siguiente");
        btnDeshacer  = new JButton("Deshacer (LIFO)");
        botones.add(btnIngresar); botones.add(btnSiguiente); botones.add(btnDeshacer);

        tablaTriage = new JTable();
        JScrollPane scroll = new JScrollPane(tablaTriage);
        scroll.setBorder(BorderFactory.createTitledBorder("Cola de Triage — Ordenada por Urgencia"));

        panel.add(form,    BorderLayout.NORTH);
        panel.add(botones, BorderLayout.CENTER);
        panel.add(scroll,  BorderLayout.SOUTH);
        return panel;
    }

    /** Panel de Logística: traslado de lotes existentes + recall */
    private JPanel construirPanelLogistica() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        cmbOrigenTraslado  = new JComboBox<>();
        cmbDestinoTraslado = new JComboBox<>();
        cmbMedTraslado     = new JComboBox<>();
        cmbLoteDisponible  = new JComboBox<>();

        form.add(new JLabel("Hospital Origen:"));    form.add(cmbOrigenTraslado);
        form.add(new JLabel("Medicamento:"));        form.add(cmbMedTraslado);
        form.add(new JLabel("Lote a trasladar:"));   form.add(cmbLoteDisponible);
        form.add(new JLabel("Hospital Destino:"));   form.add(cmbDestinoTraslado);

        JPanel botones = new JPanel(new FlowLayout());
        btnTraslado      = new JButton("🚚 Trasladar Lote");
        btnRecall        = new JButton("⛔ Fallo Lote (Recall)");
        btnVerInventario = new JButton("📋 Ver Inventario por Lotes");
        btnOrdenamiento  = new JButton("🔔 Alerta Caducidad");
        botones.add(btnTraslado); botones.add(btnRecall);
        botones.add(btnVerInventario); botones.add(btnOrdenamiento);

        tablaLogistica = new JTable();
        JScrollPane scroll = new JScrollPane(tablaLogistica);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de Traslados (BST Inorden)"));

        panel.add(form,    BorderLayout.NORTH);
        panel.add(botones, BorderLayout.CENTER);
        panel.add(scroll,  BorderLayout.SOUTH);
        return panel;
    }

    /** Panel de ingreso de nuevos lotes (stock que llega del proveedor) */
    private JPanel construirPanelAgregarLote() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        txtIdLoteNuevo   = new JTextField();
        cmbHospitalLote  = new JComboBox<>();
        cmbMedicinaNueva = new JComboBox<>();
        txtCantidadLote  = new JTextField();
        txtDiasCaducidad = new JTextField();

        form.add(new JLabel("ID del Lote (ej: LT-HEG-05):")); form.add(txtIdLoteNuevo);
        form.add(new JLabel("Hospital/Centro receptor:"));      form.add(cmbHospitalLote);
        form.add(new JLabel("Medicamento:"));                   form.add(cmbMedicinaNueva);
        form.add(new JLabel("Cantidad (unidades):"));           form.add(txtCantidadLote);
        form.add(new JLabel("Días para caducar:"));             form.add(txtDiasCaducidad);

        btnAgregarLote = new JButton("✅ Registrar Ingreso de Lote");
        JPanel panelBoton = new JPanel(new FlowLayout());
        panelBoton.add(btnAgregarLote);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form,        BorderLayout.CENTER);
        norte.add(panelBoton,  BorderLayout.SOUTH);

        tablaInventarioDetalle = new JTable();
        modeloInventario = new DefaultTableModel(
                null,
                new String[]{"Hospital", "Medicamento", "ID Lote", "Cantidad", "Días p/ Caducar", "Estado"});
        tablaInventarioDetalle.setModel(modeloInventario);
        JScrollPane scroll = new JScrollPane(tablaInventarioDetalle);
        scroll.setBorder(BorderFactory.createTitledBorder("📦 Inventario Actual por Lotes (FEFO)"));

        panel.add(norte,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ============================================================
    // DATOS: 10 hospitales, stock variado, grafo 10x10
    // ============================================================
    private void inicializarBasesDeDatos() {
        bdMedicamentos.put("Fentanilo IV",           new Medicamento("Fentanilo IV",           false, true));
        bdMedicamentos.put("Morfina Ampollas",        new Medicamento("Morfina Ampollas",        false, true));
        bdMedicamentos.put("Insulina Glargina",       new Medicamento("Insulina Glargina",       true,  false));
        bdMedicamentos.put("Vacuna Pfizer COVID19",   new Medicamento("Vacuna Pfizer COVID19",   true,  false));
        bdMedicamentos.put("Amoxicilina Suspensión",  new Medicamento("Amoxicilina Suspensión",  false, false));
        bdMedicamentos.put("Paracetamol IV",          new Medicamento("Paracetamol IV",          false, false));
        bdMedicamentos.put("Epinefrina",              new Medicamento("Epinefrina",              true,  false));
        bdMedicamentos.put("Salbutamol Inhalador",    new Medicamento("Salbutamol Inhalador",    false, false));
        bdMedicamentos.put("Metformina 500mg",        new Medicamento("Metformina 500mg",        false, false));
        bdMedicamentos.put("Omeprazol IV",            new Medicamento("Omeprazol IV",            false, false));

        Hospital h1  = new Hospital("Hospital Eugenio Espejo",         3, true,  true);
        Hospital h2  = new Hospital("Hospital Baca Ortiz (Pediatría)", 3, true,  true);
        Hospital h3  = new Hospital("Hospital Pablo Arturo Suárez",    3, true,  true);
        Hospital h4  = new Hospital("Hospital Enrique Garcés",         3, true,  true);
        Hospital h5  = new Hospital("Centro de Salud Conocoto",        1, false, false);
        Hospital h6  = new Hospital("Centro de Salud Calderón",        1, false, false);
        Hospital h7  = new Hospital("Centro de Salud Quitumbe",        1, false, false);
        Hospital h8  = new Hospital("Centro de Salud Cotocollao",      1, false, false);
        Hospital h9  = new Hospital("Clínica Pichincha (Convenio)",    2, true,  false);
        Hospital h10 = new Hospital("Bodega MSP Central Zonal 9",      0, true,  true);

        // Stock inicial con múltiples lotes y distintos días
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
        h3.recibirLote(new LoteInventario("LT-PAS-02", bdMedicamentos.get("Metformina 500mg"),     600,  60));

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

    // ============================================================
    // EVENTOS
    // ============================================================
    private void registrarEventos() {

        // ----------------------------------------------------------
        // 1. INGRESAR ORDEN AL TRIAGE
        // ----------------------------------------------------------
        btnIngresar.addActionListener(e -> {
            try {
                String id       = txtId.getText().trim();
                String nombre   = txtNombrePaciente.getText().trim();
                int    edad     = (int) spnEdad.getValue();
                String sintomas = txtSintomas.getText().trim();
                Hospital    hosp = bdHospitales.get(cmbHospitalTriage.getSelectedItem().toString());
                Medicamento med  = bdMedicamentos.get(cmbMedicina.getSelectedItem().toString());
                int urgencia     = cmbUrgencia.getSelectedIndex() + 1; // 1–5
                int cant         = Integer.parseInt(txtCantidadTriage.getText().trim());

                if (id.isEmpty() || nombre.isEmpty() || sintomas.isEmpty() || cant <= 0) {
                    JOptionPane.showMessageDialog(null, "Complete todos los campos. Cantidad > 0.");
                    return;
                }

                // Guardamos la referencia ANTES de insertar para que el push apunte
                // al objeto correcto independientemente de su posición en la PriorityQueue.
                Solicitud nuevaSolicitud = new Solicitud(id, nombre, edad, sintomas, hosp, med, urgencia);
                colaTriage.add(nuevaSolicitud);
                pilaDeshacer.push(nuevaSolicitud); // LIFO: apunta al objeto exacto recién creado
                actualizarTablaTriage();
                limpiarCamposTriage();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "La cantidad debe ser un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ----------------------------------------------------------
        // 2. DESHACER — Stack LIFO
        // ----------------------------------------------------------
        btnDeshacer.addActionListener(e -> {
            if (!pilaDeshacer.isEmpty()) {
                Solicitud deshecha = pilaDeshacer.pop();
                colaTriage.remove(deshecha);
                JOptionPane.showMessageDialog(null, "🔄 Eliminado de la cola: " + deshecha.getNombrePaciente());
                actualizarTablaTriage();
            } else {
                JOptionPane.showMessageDialog(null, "No hay ingresos recientes en la pila.");
            }
        });

        // ----------------------------------------------------------
        // 3. ATENDER SIGUIENTE — PriorityQueue + Dijkstra detallado
        // ----------------------------------------------------------
        btnSiguiente.addActionListener(e -> {
            if (colaTriage.isEmpty()) {
                JOptionPane.showMessageDialog(null, "La cola de triage está vacía."); return;
            }

            Solicitud atendida = colaTriage.poll();
            pilaDeshacer.remove(atendida);
            Hospital    hosp = atendida.getHospitalSolicitante();
            Medicamento med  = atendida.getMedicamento();
            int cantSolicitada = 10;
            try { cantSolicitada = Integer.parseInt(txtCantidadTriage.getText()); } catch (Exception ex) {}

            // Etiqueta del nivel para el mensaje
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
                // Caso A: el propio hospital tiene stock
                hosp.despacharMedicina(med.getNombre(), cantSolicitada);
                msj.append("✅ FUENTE: Stock propio de ").append(hosp.getNombre()).append("\n");
                msj.append("   Lotes despachados por FEFO.\n");
                msj.append("   Stock restante: ").append(hosp.getStockTotal(med.getNombre())).append(" un.\n");
            } else {
                // Caso B: buscar en la red con Dijkstra
                msj.append("⚠️  Stock local insuficiente (").append(stockLocal).append(" un.)\n");
                msj.append("   Buscando en la Red de Salud...\n\n");

                Hospital mejorFuente   = null;
                int      tiempoMinimo  = Integer.MAX_VALUE;

                for (Hospital candidato : bdHospitales.values()) {
                    if (candidato != hosp && candidato.getStockTotal(med.getNombre()) >= cantSolicitada) {
                        int t = redHospitales.obtenerTiempoRutaInt(candidato.getNombre(), hosp.getNombre());
                        if (t != -1 && t < tiempoMinimo) {
                            tiempoMinimo = t;
                            mejorFuente  = candidato;
                        }
                    }
                }

                if (mejorFuente != null) {
                    mejorFuente.despacharMedicina(med.getNombre(), cantSolicitada);
                    msj.append("🚚 FUENTE: ").append(mejorFuente.getNombre()).append("\n");
                    msj.append("   ➔ Destino: ").append(hosp.getNombre()).append("\n");
                    msj.append("   ⏳ Tiempo de llegada (Dijkstra): ").append(tiempoMinimo).append(" min\n");
                    msj.append("   Stock restante en fuente: ")
                            .append(mejorFuente.getStockTotal(med.getNombre())).append(" un.\n");
                } else {
                    msj.append("❌ COLAPSO LOGÍSTICO\n");
                    msj.append("   Ningún hospital de la red tiene\n");
                    msj.append("   suficiente stock de ").append(med.getNombre()).append(".\n");
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

        // ----------------------------------------------------------
        // 4. TRASLADO DE LOTE EXISTENTE
        // El combo de lotes se recarga cuando cambia origen o medicamento.
        // Se extrae el LoteInventario del origen y se inserta en el destino.
        // No se crea ningún objeto nuevo — se mueve el mismo lote.
        // ----------------------------------------------------------
        cmbOrigenTraslado.addActionListener(e -> recargarLotesCombo());
        cmbMedTraslado.addActionListener(e    -> recargarLotesCombo());

        btnTraslado.addActionListener(e -> {
            if (cmbLoteDisponible.getItemCount() == 0) {
                JOptionPane.showMessageDialog(null, "No hay lotes disponibles con esa combinación.");
                return;
            }

            String     nomOrigen  = cmbOrigenTraslado.getSelectedItem().toString();
            String     nomDestino = cmbDestinoTraslado.getSelectedItem().toString();
            String     nomMed     = cmbMedTraslado.getSelectedItem().toString();
            String     idLote     = cmbLoteDisponible.getSelectedItem().toString().split(" ")[0]; // "LT-XX-01 | 300 un."

            if (nomOrigen.equals(nomDestino)) {
                JOptionPane.showMessageDialog(null, "El origen y destino no pueden ser el mismo hospital.");
                return;
            }

            Hospital origen  = bdHospitales.get(nomOrigen);
            Hospital destino = bdHospitales.get(nomDestino);

            // Verificar que existe ruta en el grafo
            String ruta = redHospitales.buscarRutaMasRapida(nomOrigen, nomDestino);
            if (ruta.contains("No existe")) {
                JOptionPane.showMessageDialog(null, "❌ No hay ruta en el grafo entre esos hospitales.");
                return;
            }

            // Extraer el lote físico del origen — O(n) reconstruyendo la cola
            LoteInventario loteMovido = origen.extraerLotePorId(idLote, nomMed);
            if (loteMovido == null) {
                JOptionPane.showMessageDialog(null, "No se encontró el lote en el inventario de origen.");
                return;
            }

            // Insertar el mismo objeto en el destino
            destino.recibirLote(loteMovido);

            // Registrar en el árbol BST para trazabilidad
            motorLogistico.registrarLote(idLote, loteMovido.getMedicamento(), origen, destino);

            JOptionPane.showMessageDialog(null,
                    "✅ TRASLADO REGISTRADO\n"
                            + "Lote     : " + idLote + "\n"
                            + "Fármaco  : " + nomMed + "\n"
                            + "Origen   : " + nomOrigen + "\n"
                            + "Destino  : " + nomDestino + "\n"
                            + "Tiempo   : " + ruta,
                    "Traslado Exitoso", JOptionPane.INFORMATION_MESSAGE);

            actualizarTablaLogistica();
            refrescarTablaInventario();
            recargarLotesCombo();
        });

        // ----------------------------------------------------------
        // 5. RECALL — borra el lote de TODOS los hospitales
        // Primero busca en el BST para confirmar trazabilidad,
        // luego llama a retirarLotePorId() en cada hospital.
        // ----------------------------------------------------------
        btnRecall.addActionListener(e -> {
            String idBuscado = JOptionPane.showInputDialog(null,
                    "Ingrese el ID del Lote bajo sospecha sanitaria:",
                    "⛔ Activar Recall", JOptionPane.WARNING_MESSAGE);
            if (idBuscado == null || idBuscado.trim().isEmpty()) return;
            idBuscado = idBuscado.trim();

            // Buscar en el árbol BST para mostrar la trazabilidad
            NodoLote nodoEncontrado = motorLogistico.buscarLote(idBuscado);

            // Intentar borrar de todos los hospitales
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
                    msj.append("Trazab.  : ").append(nodoEncontrado.getOrigen().getNombre())
                            .append(" ➔ ").append(nodoEncontrado.getDestino().getNombre()).append("\n");
                }
                msj.append("\nEl lote ha sido eliminado de todos\nlos inventarios de la red.");
                JOptionPane.showMessageDialog(null, msj.toString(),
                        "Contención Sanitaria Completada", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                        "ℹ️  El lote '" + idBuscado + "' no se encontró\nen ningún inventario activo.",
                        "Búsqueda Fallida", JOptionPane.INFORMATION_MESSAGE);
            }

            refrescarTablaInventario();
            recargarLotesCombo();
        });

        // ----------------------------------------------------------
        // 6. VER INVENTARIO DETALLADO POR LOTES
        // ----------------------------------------------------------
        btnVerInventario.addActionListener(e -> {
            StringBuilder sb = new StringBuilder("=== KARDEX DETALLADO POR LOTES ===\n\n");
            for (Hospital h : bdHospitales.values()) {
                sb.append("🏥 ").append(h.getNombre()).append("\n");
                boolean tieneStock = false;
                for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                    PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                    while (!copia.isEmpty()) {
                        LoteInventario l = copia.poll();
                        String est = l.getDiasParaCaducar() <= 30 ? "⛔CRÍTICO" :
                                l.getDiasParaCaducar() <= 90 ? "⚠️ALERTA" : "✅OK";
                        sb.append("   [").append(l.getIdLote()).append("] ")
                                .append(entrada.getKey()).append(" | ")
                                .append(l.getCantidad()).append(" un. | ")
                                .append(l.getDiasParaCaducar()).append(" días | ").append(est).append("\n");
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

        // ----------------------------------------------------------
        // 7. ALERTA CADUCIDAD — Bubble Sort O(n²)
        // ----------------------------------------------------------
        btnOrdenamiento.addActionListener(e -> {
            List<String[]> filas = new ArrayList<>();
            for (Hospital h : bdHospitales.values()) {
                for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                    PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                    while (!copia.isEmpty()) {
                        LoteInventario l = copia.poll();
                        filas.add(new String[]{h.getNombre(), l.getIdLote(),
                                l.getMedicamento().getNombre(), String.valueOf(l.getCantidad()),
                                String.valueOf(l.getDiasParaCaducar())});
                    }
                }
            }
            if (filas.isEmpty()) { JOptionPane.showMessageDialog(null, "Sin inventario."); return; }

            // Bubble Sort ascendente por días (índice 4)
            int n = filas.size();
            for (int i = 0; i < n - 1; i++)
                for (int j = 0; j < n - i - 1; j++)
                    if (Integer.parseInt(filas.get(j)[4]) > Integer.parseInt(filas.get(j+1)[4])) {
                        String[] tmp = filas.get(j); filas.set(j, filas.get(j+1)); filas.set(j+1, tmp);
                    }

            DefaultTableModel modelo = new DefaultTableModel(
                    new String[]{"Hospital","ID Lote","Medicamento","Cant.","Días","Estado"}, 0);
            for (String[] f : filas) {
                int dias = Integer.parseInt(f[4]);
                modelo.addRow(new Object[]{f[0],f[1],f[2],f[3],dias,
                        dias<=30?"⛔ CRÍTICO":dias<=90?"⚠️ ALERTA":"✅ OK"});
            }
            JTable tabla = new JTable(modelo);
            tabla.setEnabled(false);
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(800, 350));
            JOptionPane.showMessageDialog(null, scroll,
                    "Alerta Caducidad — Bubble Sort O(n²)", JOptionPane.WARNING_MESSAGE);
        });

        // ----------------------------------------------------------
        // 8. AGREGAR LOTE NUEVO (Tab Agregar Lote)
        // ----------------------------------------------------------
        btnAgregarLote.addActionListener(e -> {
            String idLote  = txtIdLoteNuevo.getText().trim();
            String nomHosp = cmbHospitalLote.getSelectedItem().toString();
            String nomMed  = cmbMedicinaNueva.getSelectedItem().toString();
            if (idLote.isEmpty()) { JOptionPane.showMessageDialog(null,"Ingrese un ID de lote."); return; }
            try {
                int cant = Integer.parseInt(txtCantidadLote.getText().trim());
                int dias = Integer.parseInt(txtDiasCaducidad.getText().trim());
                if (cant <= 0 || dias <= 0) {
                    JOptionPane.showMessageDialog(null,"Cantidad y días deben ser > 0."); return;
                }
                Hospital    hosp = bdHospitales.get(nomHosp);
                Medicamento med  = bdMedicamentos.get(nomMed);
                hosp.recibirLote(new LoteInventario(idLote, med, cant, dias));

                JOptionPane.showMessageDialog(null,
                        "✅ LOTE REGISTRADO\nHospital: " + hosp.getNombre()
                                + "\nLote: " + idLote + " | " + cant + " un. | " + dias + " días");

                txtIdLoteNuevo.setText(""); txtCantidadLote.setText(""); txtDiasCaducidad.setText("");
                refrescarTablaInventario();
                recargarLotesCombo();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null,"Cantidad y días deben ser enteros.");
            }
        });
    }

    // ============================================================
    // MÉTODOS DE SOPORTE
    // ============================================================

    /**
     * Recarga el combo de lotes disponibles según el hospital origen
     * y el medicamento seleccionado en el tab de Logística.
     * Llama a getLotesDisponibles() que hace una copia de la cola FEFO — O(n log n).
     */
    private void recargarLotesCombo() {
        cmbLoteDisponible.removeAllItems();
        if (cmbOrigenTraslado.getSelectedItem() == null || cmbMedTraslado.getSelectedItem() == null) return;
        String nomHosp = cmbOrigenTraslado.getSelectedItem().toString();
        String nomMed  = cmbMedTraslado.getSelectedItem().toString();
        Hospital hosp  = bdHospitales.get(nomHosp);
        if (hosp == null) return;
        for (LoteInventario l : hosp.getLotesDisponibles(nomMed)) {
            // Formato: "LT-XX-01 | 300 un. | 90 días"
            cmbLoteDisponible.addItem(l.getIdLote() + " | " + l.getCantidad() + " un. | " + l.getDiasParaCaducar() + " días");
        }
    }

    /** Refresca la tabla de inventario por lotes del tab Agregar Lote. */
    private void refrescarTablaInventario() {
        modeloInventario.setRowCount(0);
        for (Hospital h : bdHospitales.values()) {
            for (Map.Entry<String, PriorityQueue<LoteInventario>> entrada : h.getBodegas().entrySet()) {
                PriorityQueue<LoteInventario> copia = new PriorityQueue<>(entrada.getValue());
                while (!copia.isEmpty()) {
                    LoteInventario l = copia.poll();
                    String est = l.getDiasParaCaducar() <= 30 ? "⛔ CRÍTICO" :
                            l.getDiasParaCaducar() <= 90 ? "⚠️ ALERTA"  : "✅ OK";
                    modeloInventario.addRow(new Object[]{
                            h.getNombre(), l.getMedicamento().getNombre(),
                            l.getIdLote(), l.getCantidad(), l.getDiasParaCaducar(), est});
                }
            }
        }
    }

    private void configurarTablas() {
        // Tabla triage con renderer de colores por urgencia
        modeloTriage = new DefaultTableModel(
                null, new String[]{"ID","Paciente","Fármaco","Nivel","Centro Solicitante"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaTriage.setModel(modeloTriage);
        tablaTriage.setRowHeight(22);

        // Renderer: pinta toda la fila con el color del nivel de urgencia
        DefaultTableCellRenderer rendererTriage = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tabla, Object valor, boolean selec, boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(tabla, valor, selec, foco, fila, col);
                if (!selec) {
                    int nivel = 0;
                    try { nivel = Integer.parseInt(tabla.getValueAt(fila, 3).toString()); } catch (Exception ex) {}
                    switch (nivel) {
                        case 1: setBackground(COLOR_ROJO);     setForeground(Color.WHITE); break;
                        case 2: setBackground(COLOR_AMARILLO); setForeground(Color.BLACK); break;
                        case 3: setBackground(COLOR_VERDE);    setForeground(Color.WHITE); break;
                        case 4: setBackground(COLOR_BLANCO);   setForeground(Color.BLACK); break;
                        case 5: setBackground(COLOR_AZUL);     setForeground(Color.WHITE); break;
                        default: setBackground(Color.WHITE);   setForeground(Color.BLACK); break;
                    }
                }
                return this;
            }
        };
        for (int i = 0; i < 5; i++) tablaTriage.getColumnModel().getColumn(i).setCellRenderer(rendererTriage);

        modeloLogistica = new DefaultTableModel(
                null, new String[]{"ID Lote","Medicamento","Origen","Destino"}) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaLogistica.setModel(modeloLogistica);
    }

    private void cargarComboBoxes() {
        spnEdad.setModel(new SpinnerNumberModel(30, 0, 120, 1));

        // 5 niveles de triage según imagen MSP
        cmbUrgencia.removeAllItems();
        cmbUrgencia.addItem("1 — ROJO    | Emergencia Vital (atención inmediata)");
        cmbUrgencia.addItem("2 — AMARILLO| Urgencia inminente (máx. 30 min)");
        cmbUrgencia.addItem("3 — VERDE   | Urgencia moderada (hasta 120 min)");
        cmbUrgencia.addItem("4 — BLANCO  | Consulta priorizada");
        cmbUrgencia.addItem("5 — AZUL    | Consulta no priorizada");

        for (String med : bdMedicamentos.keySet()) {
            cmbMedicina.addItem(med);
            cmbMedTraslado.addItem(med);
            cmbMedicinaNueva.addItem(med);
        }
        for (String hosp : bdHospitales.keySet()) {
            cmbHospitalTriage.addItem(hosp);
            cmbOrigenTraslado.addItem(hosp);
            cmbDestinoTraslado.addItem(hosp);
            cmbHospitalLote.addItem(hosp);
        }

        refrescarTablaInventario();
        recargarLotesCombo(); // Carga lotes iniciales en el combo de traslado
    }

    private void actualizarTablaTriage() {
        modeloTriage.setRowCount(0);
        PriorityQueue<Solicitud> copia = new PriorityQueue<>(colaTriage);
        while (!copia.isEmpty()) {
            Solicitud s = copia.poll();
            modeloTriage.addRow(new Object[]{
                    s.getIdSolicitud(), s.getNombrePaciente(),
                    s.getMedicamento().getNombre(), s.getNivelUrgencia(),
                    s.getHospitalSolicitante().getNombre()});
        }
    }

    private void actualizarTablaLogistica() {
        modeloLogistica.setRowCount(0);
        List<NodoLote> lista = new ArrayList<>();
        motorLogistico.obtenerHistorialInorden(motorLogistico.getRaiz(), lista);
        for (NodoLote n : lista)
            modeloLogistica.addRow(new Object[]{
                    n.getIdLote(), n.getMedicamento().getNombre(),
                    n.getOrigen().getNombre(), n.getDestino().getNombre()});
    }

    private void limpiarCamposTriage() {
        txtId.setText(""); txtNombrePaciente.setText(""); txtSintomas.setText("");
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("SafeDose Ecuador — v3");
        frame.setContentPane(new Ventana().principal);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(860, 650));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}