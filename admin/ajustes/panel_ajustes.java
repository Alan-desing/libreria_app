package admin.ajustes;

import includes.conexion_bd;
import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panel de administración de ajustes generales del sistema.
 * Permite editar parámetros globales, datos de empresa,
 * estados de pedidos y tipos de movimientos de inventario.
 */
public class panel_ajustes extends JPanel {

    /* ====== Inputs: Parámetros globales ====== */
    private JSpinner spStockMin;
    private JFormattedTextField tfIVA;
    private JTextField tfMonedaSimbolo, tfMonedaCodigo;

    /* ====== Inputs: Empresa ====== */
    private JTextField tfEmpNombre, tfEmpRazon, tfEmpCUIT, tfEmpTel, tfEmpDir, tfEmpEmail, tfEmpWeb;

    /* ====== Tablas ====== */
    private DefaultTableModel modelEstados, modelTipos;
    private JTable tablaEstados, tablaTipos;

    /* ====== Add rows ====== */
    private JTextField inNuevoEstado, inNuevoTipo;

    // visual: constructor principal, crea toda la interfaz y secciones
    public panel_ajustes() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Shell general con márgenes y layout flexible
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Card principal (contenedor blanco central)
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16, 16, 18, 16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Header: título y botón de restauración
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Ajustes");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        JButton btnRestaurar = estilos.botonBlanco("Restaurar .sql…");
        btnRestaurar.setPreferredSize(new Dimension(150, 40));
        head.add(btnRestaurar, BorderLayout.EAST);
        head.setBorder(new EmptyBorder(0, 0, 8, 0));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(head);

        // Secciones principales del panel (cada una en su card interna)
        card.add(sectionCard("Parámetros globales", "Stock mínimo, impuestos y moneda", buildParametrosPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Datos de la empresa", "Información usada en reportes, comprobantes y pie del sistema", buildEmpresaPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Estados de pedidos", null, buildEstadosPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Tipos de movimiento de inventario", null, buildTiposPanel()));
        card.add(Box.createVerticalStrut(6));

        // Scroll general que contiene toda la card
        JPanel scrollContent = new JPanel();
        scrollContent.setOpaque(false);
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.add(card);

        JScrollPane allScroll = new JScrollPane(
                scrollContent,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        allScroll.setBorder(null);
        allScroll.getVerticalScrollBar().setUnitIncrement(16);

        shell.add(allScroll, gbc);
        add(shell, BorderLayout.CENTER);

        // eventos: botón de restaurar base de datos
        btnRestaurar.addActionListener(e -> onRestaurar());

        // carga inicial de datos desde BD
        cargarOpciones();
        cargarEstados();
        cargarTipos();
    }

    /* ====================== UI builders ====================== */

    // visual: genera una tarjeta (sección) con título, subtítulo y contenido
    private JPanel sectionCard(String titulo, String subtitulo, JComponent content) {
        JPanel sc = new JPanel();
        sc.setOpaque(true);
        sc.setBackground(Color.WHITE);
        sc.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                                        new EmptyBorder(12, 12, 12, 12)));
        sc.setLayout(new BoxLayout(sc, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Arial", Font.BOLD, 18));
        t.setForeground(estilos.COLOR_TITULO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(t);

        if (subtitulo != null && !subtitulo.isBlank()) {
            JLabel s = new JLabel(subtitulo);
            s.setForeground(new Color(110, 110, 110));
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.add(s);
        }
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        sc.add(head);
        sc.add(Box.createVerticalStrut(8));

        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.add(content);

        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, sc.getPreferredSize().height));
        return sc;
    }

    // visual: formulario para los parámetros globales
    private JPanel buildParametrosPanel() {
        JPanel g = grid2cols();

        spStockMin = new JSpinner(new SpinnerNumberModel(5, 0, 999_999, 1));
        tfIVA = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        tfIVA.setValue(21.00);
        tfMonedaSimbolo = new JTextField("$");
        tfMonedaCodigo = new JTextField("ARS");

        // agrega los campos con sus etiquetas
        int i = 0;
        addField(g, i++, "Stock mínimo general", wrapField(spStockMin));
        addField(g, i++, "Impuesto IVA (%)", wrapField(tfIVA));
        addField(g, i++, "Símbolo moneda", wrapField(tfMonedaSimbolo));
        addField(g, i++, "Código moneda (ISO 4217)", wrapField(tfMonedaCodigo));

        // botón de guardar
        JButton btn = estilos.botonRedondeado("Guardar");
        btn.addActionListener(e -> onGuardarParametros());
        GridBagConstraints c = gbcBase();
        c.gridx = 0; c.gridy = (i + 1) / 2; c.gridwidth = 2; c.weightx = 1;
        g.add(btn, c);

        // aplica estilo a los campos
        styleField(spStockMin.getEditor());
        styleField(tfIVA);
        styleField(tfMonedaSimbolo);
        styleField(tfMonedaCodigo);

        return g;
    }

    // visual: formulario con los datos generales de la empresa
    private JPanel buildEmpresaPanel() {
        JPanel g = grid2cols();

        tfEmpNombre = new JTextField();
        tfEmpRazon = new JTextField();
        tfEmpCUIT = new JTextField();
        tfEmpTel = new JTextField();
        tfEmpDir = new JTextField();
        tfEmpEmail = new JTextField();
        tfEmpWeb = new JTextField();

        int i = 0;
        addField(g, i++, "Nombre comercial", wrapField(tfEmpNombre));
        addField(g, i++, "Razón social", wrapField(tfEmpRazon));
        addField(g, i++, "CUIT", wrapField(tfEmpCUIT));
        addField(g, i++, "Teléfono", wrapField(tfEmpTel));
        addField(g, i++, "Dirección", wrapField(tfEmpDir));
        addField(g, i++, "Email", wrapField(tfEmpEmail));
        addField(g, i++, "Sitio web", wrapField(tfEmpWeb));

        // botón de guardar
        JButton btn = estilos.botonRedondeado("Guardar");
        btn.addActionListener(e -> onGuardarEmpresa());
        GridBagConstraints c = gbcBase();
        c.gridx = 0; c.gridy = (i + 1) / 2; c.gridwidth = 2; c.weightx = 1;
        g.add(btn, c);

        // aplica estilo a cada campo
        styleField(tfEmpNombre);
        styleField(tfEmpRazon);
        styleField(tfEmpCUIT);
        styleField(tfEmpTel);
        styleField(tfEmpDir);
        styleField(tfEmpEmail);
        styleField(tfEmpWeb);

        return g;
    }

    // visual: sección de gestión de estados de pedido
    private JPanel buildEstadosPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        // parte superior con campo + botón agregar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        inNuevoEstado = new JTextField();
        inNuevoEstado.setPreferredSize(new Dimension(320, 38));
        styleField(inNuevoEstado);
        JButton btnAdd = estilos.botonBlanco("+ Agregar");
        actions.add(inNuevoEstado);
        actions.add(btnAdd);
        wrap.add(actions, BorderLayout.NORTH);

        // modelo y tabla de estados
        modelEstados = new DefaultTableModel(new String[]{"ID", "Nombre", "Guardar", "Eliminar"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c >= 2; }
            @Override public Class<?> getColumnClass(int c) { return (c >= 2) ? JButton.class : Object.class; }
        };

        tablaEstados = tableBase(modelEstados);
        tablaEstados.getColumnModel().getColumn(2).setCellRenderer(new BtnCellRenderer(false));
        tablaEstados.getColumnModel().getColumn(3).setCellRenderer(new BtnCellRenderer(true));
        tablaEstados.getColumnModel().getColumn(2).setCellEditor(new BtnCellEditor(tablaEstados, row -> onGuardarEstado(row), false));
        tablaEstados.getColumnModel().getColumn(3).setCellEditor(new BtnCellEditor(tablaEstados, row -> onEliminarEstado(row), true));

        JScrollPane sc = scrollFor(tablaEstados);
        wrap.add(sc, BorderLayout.CENTER);

        // acción del botón agregar
        btnAdd.addActionListener(e -> {
            String nombre = inNuevoEstado.getText().trim();
            if (nombre.isEmpty()) return;
            try {
                acciones.addEstado(nombre);
                inNuevoEstado.setText("");
                cargarEstados();
            } catch (Exception ex) {
                error("Error agregando estado:\n" + ex.getMessage());
            }
        });

        return wrap;
    }
    // visual: sección de gestión de tipos de movimiento de inventario
    private JPanel buildTiposPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        // parte superior: campo + botón agregar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        inNuevoTipo = new JTextField();
        inNuevoTipo.setPreferredSize(new Dimension(320, 38));
        styleField(inNuevoTipo);
        JButton btnAdd = estilos.botonBlanco("+ Agregar");
        actions.add(inNuevoTipo);
        actions.add(btnAdd);
        wrap.add(actions, BorderLayout.NORTH);

        // tabla de tipos: columnas con ID, nombre y botones
        modelTipos = new DefaultTableModel(new String[]{"ID", "Nombre", "Guardar", "Eliminar"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c >= 2; } // solo columnas de botones
            @Override public Class<?> getColumnClass(int c) { return (c >= 2) ? JButton.class : Object.class; }
        };

        // configuración de la tabla
        tablaTipos = tableBase(modelTipos);
        tablaTipos.getColumnModel().getColumn(2).setCellRenderer(new BtnCellRenderer(false));
        tablaTipos.getColumnModel().getColumn(3).setCellRenderer(new BtnCellRenderer(true));
        tablaTipos.getColumnModel().getColumn(2).setCellEditor(new BtnCellEditor(tablaTipos, row -> onGuardarTipo(row), false));
        tablaTipos.getColumnModel().getColumn(3).setCellEditor(new BtnCellEditor(tablaTipos, row -> onEliminarTipo(row), true));

        JScrollPane sc = scrollFor(tablaTipos);
        wrap.add(sc, BorderLayout.CENTER);

        // acción del botón agregar
        btnAdd.addActionListener(e -> {
            String nombre = inNuevoTipo.getText().trim();
            if (nombre.isEmpty()) return;
            try {
                acciones.addTipo(nombre);
                inNuevoTipo.setText("");
                cargarTipos();
            } catch (Exception ex) {
                error("Error agregando tipo:\n" + ex.getMessage());
            }
        });

        return wrap;
    }

    /* ===== Helpers de layout ===== */

    // visual: panel base con GridBag para formularios en dos columnas
    private JPanel grid2cols() {
        JPanel g = new JPanel(new GridBagLayout());
        g.setOpaque(false);
        return g;
    }

    // visual: configuración base para celdas de GridBag
    private GridBagConstraints gbcBase() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        return c;
    }

    // visual: helper que ubica un campo por índice (dos columnas automáticas)
    private void addField(JPanel g, int idx, String label, JComponent field) {
        GridBagConstraints c = gbcBase();
        c.gridx = (idx % 2 == 0) ? 0 : 1; // columna 0 o 1
        c.gridy = idx / 2; // fila
        g.add(makeLabeled(label, field), c);
    }

    // visual: envuelve un campo con su etiqueta superior
    private JPanel makeLabeled(String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        l.setForeground(new Color(80, 70, 60));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    // visual: aplica estilo y tamaño uniforme a un input
    private JComponent wrapField(JComponent comp) {
        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        comp.setPreferredSize(new Dimension(420, 38));
        styleField(comp);
        w.add(comp, BorderLayout.CENTER);
        return w;
    }

    // visual: aplica el estilo “estilos.estilizarCampo” a distintos tipos de campo
    private void styleField(JComponent c) {
        if (c instanceof JTextComponent tc) {
            estilos.estilizarCampo(tc);
        } else if (c instanceof JSpinner.DefaultEditor de && de.getTextField() != null) {
            estilos.estilizarCampo(de.getTextField());
        } else if (c instanceof JFormattedTextField ftf) {
            estilos.estilizarCampo(ftf);
        } else if (c instanceof JPanel jp) {
            for (Component child : jp.getComponents()) {
                if (child instanceof JTextComponent tc2) estilos.estilizarCampo(tc2);
            }
        }
    }

    // visual: configuración base de todas las tablas de este panel
    private JTable tableBase(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Arial", Font.PLAIN, 17));
        t.setRowHeight(32);

        JTableHeader th = t.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 17));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF, 0xF3, 0xD9));

        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEDE3D2));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setRowMargin(0);
        t.setSelectionBackground(new Color(0xF2, 0xE7, 0xD6));
        t.setSelectionForeground(new Color(0x33, 0x33, 0x33));

        // alineación izquierda por defecto
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        t.setDefaultRenderer(Object.class, left);

        // define anchos de columnas estándar
        if (t.getColumnCount() >= 4) {
            t.getColumnModel().getColumn(0).setPreferredWidth(70);
            t.getColumnModel().getColumn(1).setPreferredWidth(380);
            t.getColumnModel().getColumn(2).setPreferredWidth(110);
            t.getColumnModel().getColumn(3).setPreferredWidth(110);
        }
        return t;
    }

    // visual: crea el scroll estilizado para tablas
    private JScrollPane scrollFor(JTable tabla) {
        JScrollPane sc = new JScrollPane(
                tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        sc.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                                        new EmptyBorder(6, 6, 6, 6)));
        sc.setPreferredSize(new Dimension(0, 320));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sc;
    }

    /* ====================== Eventos ====================== */

    // lógica: guarda los parámetros globales en BD
    private void onGuardarParametros() {
        try {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("stock_minimo_general", String.valueOf(((Number) spStockMin.getValue()).intValue()));
            Number iva = (Number) tfIVA.getValue();
            if (iva == null) iva = 0;
            m.put("impuesto_iva", String.format(java.util.Locale.US, "%.2f", iva.doubleValue()));
            m.put("moneda_simbolo", tfMonedaSimbolo.getText().trim());
            m.put("moneda_codigo", tfMonedaCodigo.getText().trim());
            acciones.saveOpts(m); // guarda mediante helper
            ok("Parámetros guardados.");
        } catch (Exception ex) {
            error("Error guardando parámetros:\n" + ex.getMessage());
        }
    }

    // lógica: guarda los datos de la empresa
    private void onGuardarEmpresa() {
        try {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("empresa_nombre", tfEmpNombre.getText().trim());
            m.put("empresa_razon", tfEmpRazon.getText().trim());
            m.put("empresa_cuit", tfEmpCUIT.getText().trim());
            m.put("empresa_direccion", tfEmpDir.getText().trim());
            m.put("empresa_telefono", tfEmpTel.getText().trim());
            m.put("empresa_email", tfEmpEmail.getText().trim());
            m.put("empresa_web", tfEmpWeb.getText().trim());
            acciones.saveEmpresa(m);
            ok("Datos de la empresa guardados.");
        } catch (Exception ex) {
            error("Error guardando empresa:\n" + ex.getMessage());
        }
    }

    // lógica: guarda una fila existente de la tabla “estados”
    private void onGuardarEstado(int viewRow) {
        int mr = tablaEstados.convertRowIndexToModel(viewRow);
        int id = parseInt(modelEstados.getValueAt(mr, 0));
        String nombre = String.valueOf(modelEstados.getValueAt(mr, 1)).trim();
        if (id <= 0 || nombre.isEmpty()) return;
        try {
            acciones.updEstado(id, nombre);
            ok("Estado actualizado.");
            cargarEstados();
        } catch (Exception ex) {
            error("Error actualizando estado:\n" + ex.getMessage());
        }
    }
    // lógica: eliminar un estado de pedido desde la tabla
    private void onEliminarEstado(int viewRow) {
        int mr = tablaEstados.convertRowIndexToModel(viewRow);
        int id = parseInt(modelEstados.getValueAt(mr, 0));
        if (id <= 0) return;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar estado?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                acciones.delEstado(id);
                cargarEstados();
            } catch (Exception ex) {
                error("Error eliminando estado:\n" + ex.getMessage());
            }
        }
    }

    // lógica: guardar cambios en un tipo de movimiento
    private void onGuardarTipo(int viewRow) {
        int mr = tablaTipos.convertRowIndexToModel(viewRow);
        int id = parseInt(modelTipos.getValueAt(mr, 0));
        String nombre = String.valueOf(modelTipos.getValueAt(mr, 1)).trim();
        if (id <= 0 || nombre.isEmpty()) return;
        try {
            acciones.updTipo(id, nombre);
            ok("Tipo actualizado.");
            cargarTipos();
        } catch (Exception ex) {
            error("Error actualizando tipo:\n" + ex.getMessage());
        }
    }

    // lógica: eliminar un tipo de movimiento desde la tabla
    private void onEliminarTipo(int viewRow) {
        int mr = tablaTipos.convertRowIndexToModel(viewRow);
        int id = parseInt(modelTipos.getValueAt(mr, 0));
        if (id <= 0) return;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar tipo?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                acciones.delTipo(id);
                cargarTipos();
            } catch (Exception ex) {
                error("Error eliminando tipo:\n" + ex.getMessage());
            }
        }
    }

    // lógica: restaurar base de datos desde un archivo .sql
    private void onRestaurar() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccioná un .sql para restaurar");
        int r = fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                restaurar.ejecutar(f);
                ok("Restauración completada.");
                cargarEstados();
                cargarTipos();
                cargarOpciones();
            } catch (Exception ex) {
                error("Error al restaurar:\n" + ex.getMessage());
            }
        }
    }

    // lógica: carga general de valores guardados (opciones del sistema)
    private void cargarOpciones() {
        Map<String, String> def = new LinkedHashMap<>();
        // valores por defecto
        def.put("stock_minimo_general", "5");
        def.put("impuesto_iva", "21.00");
        def.put("moneda_simbolo", "$");
        def.put("moneda_codigo", "ARS");
        def.put("empresa_nombre", "Librería Los Lapicitos");
        def.put("empresa_razon", "Los Lapicitos SRL");
        def.put("empresa_cuit", "");
        def.put("empresa_direccion", "");
        def.put("empresa_telefono", "");
        def.put("empresa_email", "");
        def.put("empresa_web", "");

        // BD: leer tabla ajuste si existe
        try (Connection cn = conexion_bd.getConnection()) {
            ensureAjusteTable(cn);
            try (PreparedStatement ps = cn.prepareStatement("SELECT clave, valor FROM ajuste");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) def.put(rs.getString(1), rs.getString(2));
            }
        } catch (Exception ignore) { }

        // asignar valores a los campos
        try { spStockMin.setValue(Integer.parseInt(def.get("stock_minimo_general"))); } catch (Exception ignore) { }
        try { tfIVA.setValue(Double.parseDouble(def.get("impuesto_iva"))); } catch (Exception ignore) { }
        tfMonedaSimbolo.setText(def.get("moneda_simbolo"));
        tfMonedaCodigo.setText(def.get("moneda_codigo"));
        tfEmpNombre.setText(def.get("empresa_nombre"));
        tfEmpRazon.setText(def.get("empresa_razon"));
        tfEmpCUIT.setText(def.get("empresa_cuit"));
        tfEmpDir.setText(def.get("empresa_direccion"));
        tfEmpTel.setText(def.get("empresa_telefono"));
        tfEmpEmail.setText(def.get("empresa_email"));
        tfEmpWeb.setText(def.get("empresa_web"));
    }

    // lógica: carga de estados de pedido desde BD
    private void cargarEstados() {
        modelEstados.setRowCount(0);
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_estado_pedido, nombre_estado FROM estado_pedido ORDER BY id_estado_pedido");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelEstados.addRow(new Object[]{rs.getInt(1), rs.getString(2), "GUARDAR", "ELIMINAR"});
            }
        } catch (Exception ex) {
            error("Error cargando estados:\n" + ex.getMessage());
        }
        if (modelEstados.getRowCount() == 0)
            modelEstados.addRow(new Object[]{"", "Sin estados.", "", ""});
    }

    // lógica: carga de tipos de movimiento desde BD
    private void cargarTipos() {
        modelTipos.setRowCount(0);
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_tipo_movimiento, nombre_tipo FROM tipo_movimiento ORDER BY id_tipo_movimiento");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelTipos.addRow(new Object[]{rs.getInt(1), rs.getString(2), "GUARDAR", "ELIMINAR"});
            }
        } catch (Exception ex) {
            error("Error cargando tipos:\n" + ex.getMessage());
        }
        if (modelTipos.getRowCount() == 0)
            modelTipos.addRow(new Object[]{"", "Sin tipos.", "", ""});
    }

    // BD: asegura que exista la tabla “ajuste”
    private static void ensureAjusteTable(Connection cn) throws Exception {
        try (Statement st = cn.createStatement()) {
            st.execute("""
              CREATE TABLE IF NOT EXISTS ajuste(
                clave VARCHAR(64) PRIMARY KEY,
                valor TEXT NOT NULL,
                actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
              ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    // utilidad: parse seguro a int
    private int parseInt(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); }
        catch (Exception e) { return -1; }
    }

    // visual: mensajes estándar
    private void ok(String s) {
        JOptionPane.showMessageDialog(this, s, "Ajustes", JOptionPane.INFORMATION_MESSAGE);
    }
    private void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Ajustes", JOptionPane.ERROR_MESSAGE);
    }

    // visual: render de celda tipo botón (GUARDAR / ELIMINAR)
    static class BtnCellRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        private final boolean danger;
        BtnCellRenderer(boolean danger) {
            this.danger = danger;
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            return danger
                    ? estilos.botonSmDanger(String.valueOf(v))
                    : estilos.botonSm(String.valueOf(v));
        }
    }

    // lógica + visual: editor de celda con botón interactivo
    static class BtnCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JTable table;
        private final JButton btn;
        private final java.util.function.IntConsumer onClick;

        BtnCellEditor(JTable table, java.util.function.IntConsumer onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.btn = danger
                    ? estilos.botonSmDanger("ELIMINAR")
                    : estilos.botonSm("GUARDAR");

            // acción del botón dentro de la celda
            this.btn.addActionListener(e -> {
                final int vr = this.table.getEditingRow(); // fila actual
                fireEditingStopped(); // detiene edición antes del callback
                if (vr >= 0)
                    SwingUtilities.invokeLater(() -> this.onClick.accept(vr)); // ejecuta callback
            });
        }

        @Override public Object getCellEditorValue() { return null; }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            btn.setText(String.valueOf(v));
            return btn;
        }
    }
}
