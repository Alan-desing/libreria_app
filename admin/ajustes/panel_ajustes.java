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

    public panel_ajustes() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Shell con márgenes (como en otros paneles)
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Card principal (contenedor visual)
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

        // Encabezado
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

        // Secciones
        card.add(sectionCard("Parámetros globales", "Stock mínimo, impuestos y moneda", buildParametrosPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Datos de la empresa", "Información usada en reportes, comprobantes y pie del sistema", buildEmpresaPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Estados de pedidos", null, buildEstadosPanel()));
        card.add(Box.createVerticalStrut(10));
        card.add(sectionCard("Tipos de movimiento de inventario", null, buildTiposPanel()));
        card.add(Box.createVerticalStrut(6)); // respiro inferior

        // Scroll para toda la Card (soluciona el “no se ve abajo”)
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

        // Eventos
        btnRestaurar.addActionListener(e -> onRestaurar());

        // Carga inicial
        cargarOpciones();
        cargarEstados();
        cargarTipos();
    }

    /* ====================== UI builders ====================== */

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

    private JPanel buildParametrosPanel() {
        JPanel g = grid2cols();

        spStockMin = new JSpinner(new SpinnerNumberModel(5, 0, 999_999, 1));
        tfIVA = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        tfIVA.setValue(21.00);
        tfMonedaSimbolo = new JTextField("$");
        tfMonedaCodigo = new JTextField("ARS");

        int i = 0;
        addField(g, i++, "Stock mínimo general", wrapField(spStockMin));
        addField(g, i++, "Impuesto IVA (%)", wrapField(tfIVA));
        addField(g, i++, "Símbolo moneda", wrapField(tfMonedaSimbolo));
        addField(g, i++, "Código moneda (ISO 4217)", wrapField(tfMonedaCodigo));

        JButton btn = estilos.botonRedondeado("Guardar");
        btn.addActionListener(e -> onGuardarParametros());
        GridBagConstraints c = gbcBase();
        c.gridx = 0; c.gridy = (i + 1) / 2; c.gridwidth = 2; c.weightx = 1;
        g.add(btn, c);

        styleField(spStockMin.getEditor());
        styleField(tfIVA);
        styleField(tfMonedaSimbolo);
        styleField(tfMonedaCodigo);

        return g;
    }

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

        JButton btn = estilos.botonRedondeado("Guardar");
        btn.addActionListener(e -> onGuardarEmpresa());
        GridBagConstraints c = gbcBase();
        c.gridx = 0; c.gridy = (i + 1) / 2; c.gridwidth = 2; c.weightx = 1;
        g.add(btn, c);

        styleField(tfEmpNombre);
        styleField(tfEmpRazon);
        styleField(tfEmpCUIT);
        styleField(tfEmpTel);
        styleField(tfEmpDir);
        styleField(tfEmpEmail);
        styleField(tfEmpWeb);

        return g;
    }

    private JPanel buildEstadosPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        inNuevoEstado = new JTextField();
        inNuevoEstado.setPreferredSize(new Dimension(320, 38));
        styleField(inNuevoEstado);
        JButton btnAdd = estilos.botonBlanco("+ Agregar");
        actions.add(inNuevoEstado);
        actions.add(btnAdd);
        wrap.add(actions, BorderLayout.NORTH);

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

    private JPanel buildTiposPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        inNuevoTipo = new JTextField();
        inNuevoTipo.setPreferredSize(new Dimension(320, 38));
        styleField(inNuevoTipo);
        JButton btnAdd = estilos.botonBlanco("+ Agregar");
        actions.add(inNuevoTipo);
        actions.add(btnAdd);
        wrap.add(actions, BorderLayout.NORTH);

        modelTipos = new DefaultTableModel(new String[]{"ID", "Nombre", "Guardar", "Eliminar"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c >= 2; }
            @Override public Class<?> getColumnClass(int c) { return (c >= 2) ? JButton.class : Object.class; }
        };

        tablaTipos = tableBase(modelTipos);
        tablaTipos.getColumnModel().getColumn(2).setCellRenderer(new BtnCellRenderer(false));
        tablaTipos.getColumnModel().getColumn(3).setCellRenderer(new BtnCellRenderer(true));
        tablaTipos.getColumnModel().getColumn(2).setCellEditor(new BtnCellEditor(tablaTipos, row -> onGuardarTipo(row), false));
        tablaTipos.getColumnModel().getColumn(3).setCellEditor(new BtnCellEditor(tablaTipos, row -> onEliminarTipo(row), true));

        JScrollPane sc = scrollFor(tablaTipos);
        wrap.add(sc, BorderLayout.CENTER);

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

    private JPanel grid2cols() {
        JPanel g = new JPanel(new GridBagLayout());
        g.setOpaque(false);
        return g;
    }

    private GridBagConstraints gbcBase() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        return c;
    }

    /** ÚNICA variante “auto”: coloca por índice en 2 columnas. */
    private void addField(JPanel g, int idx, String label, JComponent field) {
        GridBagConstraints c = gbcBase();
        c.gridx = (idx % 2 == 0) ? 0 : 1;
        c.gridy = idx / 2;
        g.add(makeLabeled(label, field), c);
    }

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

    private JComponent wrapField(JComponent comp) {
        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        comp.setPreferredSize(new Dimension(420, 38));
        styleField(comp);
        w.add(comp, BorderLayout.CENTER);
        return w;
    }

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

    private JTable tableBase(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(new Font("Arial", Font.PLAIN, 17));
        t.setRowHeight(32);

        JTableHeader th = t.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 17));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF, 0xF3, 0xD9)); // crema

        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEDE3D2));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setRowMargin(0);
        t.setSelectionBackground(new Color(0xF2, 0xE7, 0xD6));
        t.setSelectionForeground(new Color(0x33, 0x33, 0x33));

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        t.setDefaultRenderer(Object.class, left);

        if (t.getColumnCount() >= 4) {
            t.getColumnModel().getColumn(0).setPreferredWidth(70);
            t.getColumnModel().getColumn(1).setPreferredWidth(380);
            t.getColumnModel().getColumn(2).setPreferredWidth(110);
            t.getColumnModel().getColumn(3).setPreferredWidth(110);
        }
        return t;
    }

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

    private void onGuardarParametros() {
        try {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("stock_minimo_general", String.valueOf(((Number) spStockMin.getValue()).intValue()));
            Number iva = (Number) tfIVA.getValue();
            if (iva == null) iva = 0;
            m.put("impuesto_iva", String.format(java.util.Locale.US, "%.2f", iva.doubleValue()));
            m.put("moneda_simbolo", tfMonedaSimbolo.getText().trim());
            m.put("moneda_codigo", tfMonedaCodigo.getText().trim());
            acciones.saveOpts(m);
            ok("Parámetros guardados.");
        } catch (Exception ex) {
            error("Error guardando parámetros:\n" + ex.getMessage());
        }
    }

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

    private void onEliminarEstado(int viewRow) {
        int mr = tablaEstados.convertRowIndexToModel(viewRow);
        int id = parseInt(modelEstados.getValueAt(mr, 0));
        if (id <= 0) return;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar estado?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                acciones.delEstado(id);
                cargarEstados();
            } catch (Exception ex) { error("Error eliminando estado:\n" + ex.getMessage()); }
        }
    }

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

    private void onEliminarTipo(int viewRow) {
        int mr = tablaTipos.convertRowIndexToModel(viewRow);
        int id = parseInt(modelTipos.getValueAt(mr, 0));
        if (id <= 0) return;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar tipo?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                acciones.delTipo(id);
                cargarTipos();
            } catch (Exception ex) { error("Error eliminando tipo:\n" + ex.getMessage()); }
        }
    }

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

    /* ====================== Carga de datos ====================== */

    private void cargarOpciones() {
        Map<String, String> def = new LinkedHashMap<>();
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

        try (Connection cn = conexion_bd.getConnection()) {
            ensureAjusteTable(cn);
            try (PreparedStatement ps = cn.prepareStatement("SELECT clave, valor FROM ajuste");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) def.put(rs.getString(1), rs.getString(2));
            }
        } catch (Exception ignore) { }

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
        if (modelEstados.getRowCount() == 0) modelEstados.addRow(new Object[]{"", "Sin estados.", "", ""});
    }

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
        if (modelTipos.getRowCount() == 0) modelTipos.addRow(new Object[]{"", "Sin tipos.", "", ""});
    }

    /* ====================== Utils ====================== */

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

    private int parseInt(Object o) { try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return -1; } }

    private void ok(String s) { JOptionPane.showMessageDialog(this, s, "Ajustes", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String s) { JOptionPane.showMessageDialog(this, s, "Ajustes", JOptionPane.ERROR_MESSAGE); }

    /* ====== Renderers / Editors de botones ====== */
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
            return danger ? estilos.botonSmDanger(String.valueOf(v)) : estilos.botonSm(String.valueOf(v));
        }
    }

    static class BtnCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JTable table;
        private final JButton btn;
        private final java.util.function.IntConsumer onClick;

        BtnCellEditor(JTable table, java.util.function.IntConsumer onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.btn = danger ? estilos.botonSmDanger("ELIMINAR") : estilos.botonSm("GUARDAR");
            this.btn.addActionListener(e -> {
                int vr = this.table.getEditingRow(); // usar el campo → evita warning
                if (vr >= 0) this.onClick.accept(vr);
                fireEditingStopped();
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
