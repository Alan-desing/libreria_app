package admin.categorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class panel_categorias extends JPanel {

    private JTable tabla;
    private DefaultTableModel model;

    private PlaceholderTextField txtBuscar;
    private JComboBox<String> cbHas; // Todas / Con productos / Sin productos
    private JButton btnFiltrarFila;
    private JButton btnAgregar;

    // (para el badge de stock solo necesitamos el valor directo)
    public panel_categorias() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // ===== Shell centrado =====
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14)); // alineación con el sidebar
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // ===== Card (blanca) =====
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 18, 16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Head: “Categorías” + botón verde =====
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Categorías");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        btnAgregar = estilos.botonRedondeado("+ Añadir Categoría");
        btnAgregar.setPreferredSize(new Dimension(220, 40));
        btnAgregar.setMaximumSize(new Dimension(240, 40));
        head.add(btnAgregar, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // ===== Filtros =====
        txtBuscar = new PlaceholderTextField("Buscar categoría…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // buscador
        g.gridx = 0; g.weightx = 1.0;
        filaFiltros.add(txtBuscar, g);

        // selector “Todas / Con productos / Sin productos”
        cbHas = new JComboBox<>(new String[]{"Todas", "Con productos", "Sin productos"});
        estilos.estilizarCombo(cbHas);
        cbHas.setPreferredSize(new Dimension(200, 38));
        cbHas.setMinimumSize(new Dimension(160, 38));
        g.gridx = 1; g.weightx = 0;
        filaFiltros.add(cbHas, g);

        // botón Filtrar
        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));
        g.gridx = 2;
        filaFiltros.add(btnFiltrarFila, g);

        // ===== Tabla =====
        String[] cols = {"ID", "Nombre", "Subcategorías", "Productos", "Stock total", "editar", "eliminar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5 || c == 6; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 5 || columnIndex == 6) ? JButton.class : Object.class;
            }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 17));
        tabla.getTableHeader().setReorderingAllowed(false);
        JTableHeader th = tabla.getTableHeader();
        th.setBackground(new Color(0xFF,0xF3,0xD9)); // header crema

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(90);

        // alineación izquierda por defecto
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // ID como "#3"
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#" + String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // stock total con badge (verde si >0, rojo si =0)
        tabla.getColumnModel().getColumn(4).setCellRenderer(new StockTotalBadgeRenderer());

        // botones por fila
        tabla.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer(false)); // editar
        tabla.getColumnModel().getColumn(6).setCellRenderer(new ButtonCellRenderer(true));  // eliminar
        tabla.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor(tabla, id -> onEditar(id), false));
        tabla.getColumnModel().getColumn(6).setCellEditor(new ButtonCellEditor(tabla, id -> onEliminar(id), true));

        // Scroll fijo (como en productos)
        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420));

        // ===== Ensamble =====
        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Eventos
        btnFiltrarFila.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());

        // Carga inicial
        cargarTabla();
    }

    // ======================= Cargar tabla (BD + filtros)
    private void cargarTabla() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        String hasSel = (String) cbHas.getSelectedItem();
        String hasFlag = ""; // "", "con", "sin"
        if (hasSel != null) {
            if (hasSel.startsWith("Con")) hasFlag = "con";
            else if (hasSel.startsWith("Sin")) hasFlag = "sin";
        }

        String baseFrom = """
                FROM categoria c
                LEFT JOIN subcategoria sc ON sc.id_categoria = c.id_categoria
                LEFT JOIN producto p      ON p.id_subcategoria = sc.id_subcategoria
                LEFT JOIN inventario i    ON i.id_producto     = p.id_producto
                """;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (!q.isEmpty()) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("c.nombre LIKE ?");
            params.add("%"+q+"%");
        }

        String having = "";
        if ("con".equals(hasFlag)) {
            having = " HAVING COUNT(DISTINCT p.id_producto) > 0";
        } else if ("sin".equals(hasFlag)) {
            having = " HAVING COUNT(DISTINCT p.id_producto) = 0";
        }

        String sql = """
                SELECT
                    c.id_categoria,
                    c.nombre,
                    COUNT(DISTINCT sc.id_subcategoria) AS subcategorias,
                    COUNT(DISTINCT p.id_producto)      AS productos,
                    COALESCE(SUM(i.stock_actual),0)    AS stock_total
                """ + baseFrom + where + """
                 GROUP BY c.id_categoria, c.nombre
                """ + having + """
                 ORDER BY c.nombre ASC
                """;

        model.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            // bind dinámico
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer iv) ps.setInt(i+1, iv);
                else ps.setString(i+1, v.toString());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id          = rs.getInt("id_categoria");
                    String nombre   = rs.getString("nombre");
                    int subcats     = rs.getInt("subcategorias");
                    int productos   = rs.getInt("productos");
                    int stockTotal  = rs.getInt("stock_total");

                    model.addRow(new Object[]{
                            String.valueOf(id),
                            nombre,
                            subcats,
                            productos,
                            stockTotal,   // renderer lo transforma en badge
                            "Editar",
                            "eliminar"
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando categorías:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==== Acciones ====
    private void onEditar(int idCat){
        JOptionPane.showMessageDialog(this,
                "Abrir pantalla de edición para Categoría ID: " + idCat,
                "Editar", JOptionPane.INFORMATION_MESSAGE);
    }
    private void onEliminar(int idCat){
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la categoría #" + idCat + "?\nEsta acción no se puede deshacer.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r!=JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM categoria WHERE id_categoria = ?";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCat);
            int n = ps.executeUpdate();
            if (n>0){
                JOptionPane.showMessageDialog(this, "Categoría eliminada.");
                cargarTabla();
            }else{
                JOptionPane.showMessageDialog(this, "No se eliminó (¿ID inexistente?)");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo eliminar:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ====== Conexión simple (igual que productos) ====== */
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }

    // ===== Placeholder =====
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font("Arial", Font.PLAIN, 14));
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(155, 142, 127));
                g2.setFont(getFont());
                Insets in = getInsets();
                int x = in.left + 4;
                int y = getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }

    // ===== Badge para “Stock total” =====
    static class StockTotalBadgeRenderer implements TableCellRenderer {
        private final PillLabel lbl = new PillLabel();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            int st = 0;
            try { st = Integer.parseInt(String.valueOf(value).replaceAll("\\D","")); }
            catch (Exception ignore){}

            if (st <= 0){
                lbl.configure(String.valueOf(st),
                        estilos.BADGE_NO_BG, estilos.BADGE_NO_BORDER, estilos.BADGE_NO_FG);
            } else {
                lbl.configure(String.valueOf(st),
                        estilos.BADGE_OK_BG, estilos.BADGE_OK_BORDER, estilos.BADGE_OK_FG);
            }
            lbl.setSelection(isSelected);
            return lbl;
        }
    }

    // ===== Píldora (reutilizable) =====
    static class PillLabel extends JComponent {
        private String text = "";
        private Color bg = Color.LIGHT_GRAY, border = Color.GRAY, fg = Color.BLACK;
        private boolean selected = false;

        void configure(String t, Color bg, Color border, Color fg){
            this.text = t; this.bg = bg; this.border = border; this.fg = fg;
            setPreferredSize(new Dimension(52, 24));
        }
        void setSelection(boolean b){ this.selected = b; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int arc = h;
            g2.setColor(selected ? new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 230) : bg);
            g2.fillRoundRect(4, (h-20)/2, w-8, 20, arc, arc);
            g2.setColor(border);
            g2.drawRoundRect(4, (h-20)/2, w-8, 20, arc, arc);

            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int tx = (w - tw)/2;
            int ty = h/2 + fm.getAscent()/2 - 3;
            g2.drawString(text, Math.max(8, tx), ty);
            g2.dispose();
        }
    }

    // ===== Botón en tabla (renderer) =====
    static class ButtonCellRenderer extends JButton implements TableCellRenderer {
        private final boolean danger;
        ButtonCellRenderer(boolean danger){
            this.danger = danger;
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return danger ? estilos.botonSmDanger(String.valueOf(value))
                          : estilos.botonSm(String.valueOf(value));
        }
    }

    // ===== Botón en tabla (editor con acción) =====
    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button;
        private final Consumer<Integer> onClick;

        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.button = danger ? estilos.botonSmDanger("eliminar")
                                 : estilos.botonSm("Editar");
            this.button.addActionListener(this::handle);
        }

        private void handle(ActionEvent e){
            int viewRow = table.getEditingRow();
            if (viewRow >= 0){
                int modelRow = table.convertRowIndexToModel(viewRow);
                Object idObj = table.getModel().getValueAt(modelRow, 0);
                int id = 0;
                try { id = Integer.parseInt(String.valueOf(idObj)); } catch (Exception ignore){}
                onClick.accept(id);
            }
            fireEditingStopped();
        }

        @Override public Object getCellEditorValue() { return null; }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            button.setText(String.valueOf(value));
            return button;
        }
    }
}
