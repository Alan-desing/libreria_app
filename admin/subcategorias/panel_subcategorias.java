package admin.subcategorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class panel_subcategorias extends JPanel {

    // Visual: filtros superiores (buscador, categoría, botones de acción)
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbCategoria;
    private JButton btnFiltrar, btnLimpiar, btnNuevo;

    // Visual: tabla visible y su modelo de datos
    private JTable tabla;
    private DefaultTableModel model;

    // Visual + Lógica: constructor que arma toda la pantalla y engancha eventos
    public panel_subcategorias() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Visual: contenedor externo (márgenes y centrado de la card)
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        GridBagConstraints root = new GridBagConstraints();
        root.gridx = 0;
        root.gridy = 0;
        root.weightx = 1;
        root.weighty = 1;                         // Visual: ocupa alto para anclar arriba
        root.fill = GridBagConstraints.HORIZONTAL;
        root.anchor = GridBagConstraints.PAGE_START;

        // Visual: card blanca con borde crema (toda la vista vive adentro)
        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        shell.add(card, root);
        add(shell, BorderLayout.CENTER);

        // Visual: header con título a la izquierda y "Añadir" a la derecha
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel("Subcategorías");
        h.setFont(new Font("Arial", Font.BOLD, 20));
        h.setForeground(estilos.COLOR_TITULO);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        btnNuevo = estilos.botonRedondeado("+ Añadir Subcategoría");
        btnNuevo.setPreferredSize(new Dimension(220, 40));
        right.add(btnNuevo);

        head.add(h, BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        card.add(head);

        // Visual: fila de filtros (buscador + combo + FILTRAR/LIMPIAR)
        card.add(crearFiltros());

        // Visual: tabla (columnas, renderers/editores y scroll)
        card.add(crearTabla());

        // Lógica: acciones de filtros y creación
        btnFiltrar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            if (cbCategoria.getItemCount() > 0) cbCategoria.setSelectedIndex(0);
            cargarTabla();
        });
        btnNuevo.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            crear dlg = new crear(owner);
            dlg.setVisible(true);
            if (dlg.fueGuardado()) cargarTabla();
        });

        // Lógica: carga inicial de combos y datos
        cargarCategorias();
        cargarTabla();
    }

    // Visual: fila de filtros (estructura y estilos)
    private JComponent crearFiltros() {
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(0, 0, 0, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // Visual: buscador
        txtBuscar = new PlaceholderTextField("Buscar subcategoría…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 38));
        txtBuscar.setMinimumSize(new Dimension(320, 38));
        g.gridx = 0; g.weightx = 1.0;
        fila.add(txtBuscar, g);

        // Visual: combo de categoría
        cbCategoria = new JComboBox<>();
        cbCategoria.setPreferredSize(new Dimension(220, 38));
        estilos.estilizarCombo(cbCategoria);
        g.gridx = 1; g.weightx = 0;
        fila.add(cbCategoria, g);

        // Visual: botones FILTRAR / LIMPIAR
        btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120, 38));
        g.gridx = 2;
        fila.add(btnFiltrar, g);

        btnLimpiar = estilos.botonBlanco("LIMPIAR");
        btnLimpiar.setPreferredSize(new Dimension(120, 38));
        g.gridx = 3;
        fila.add(btnLimpiar, g);

        fila.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0)); // Visual: separación con la tabla
        return fila;
    }

    // Visual: tabla con modelo, columnas, renderers/editores y scroll
    private JComponent crearTabla() {
        String[] cols = {"ID", "Subcategoría", "Categoría", "Productos", "Stock total", "editar", "eliminar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5 || c == 6; } // Lógica: solo acciones
            @Override public Class<?> getColumnClass(int c) { return (c==5 || c==6) ? JButton.class : Object.class; }
        };

        // Visual: estilos de la tabla (fuentes, colores, selección)
        tabla = new JTable(model);
        tabla.setRowHeight(32);
        tabla.setFont(new Font("Arial", Font.PLAIN, 16));
        JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 17));
        th.setBackground(new Color(0xFF, 0xF3, 0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        // Visual: anchos sugeridos
        setColW(0, 80);   // ID
        setColW(1, 260);
        setColW(2, 200);
        setColW(3, 140);  // productos
        setColW(4, 140);  // stock total
        setColW(5, 90);   // editar
        setColW(6, 90);   // eliminar

        // Visual: alineación por defecto a la izquierda
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // Visual: renderer del ID para mostrarlo con “#”
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#" + String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Visual: badge de "Stock total" (verde si > 0, gris si 0)
        tabla.getColumnModel().getColumn(4).setCellRenderer(new StockTotalBadgeRenderer());

        // Visual + Lógica: botones por fila (Editar / eliminar)
        tabla.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer(false));
        tabla.getColumnModel().getColumn(6).setCellRenderer(new ButtonCellRenderer(true));
        tabla.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor(tabla, id -> onEditar(id), false));
        tabla.getColumnModel().getColumn(6).setCellEditor(new ButtonCellEditor(tabla, id -> onEliminar(id), true));

        // Visual: scroll con borde crema
        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        return sc;
    }

    // Visual: card base (fondo blanco + borde crema)
    private JPanel cardShell() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        return p;
    }

    // Visual: helper para width de columnas (mantiene mínimo)
    private void setColW(int col, int w) {
        TableColumn c = tabla.getColumnModel().getColumn(col);
        c.setPreferredWidth(w);
        c.setMinWidth(60);
    }

    // Lógica/BD: carga el combo de categorías desde la tabla categoria
    private void cargarCategorias() {
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_categoria, nombre FROM categoria ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex) { showErr(ex); }
        cbCategoria.setSelectedIndex(0);
    }

    // Lógica/BD: arma la consulta con filtros aplicados y llena la tabla
    private void cargarTabla() {
        model.setRowCount(0);

        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        Item sel = (Item) cbCategoria.getSelectedItem();
        int idCat = (sel != null) ? sel.id : 0;

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT
              sc.id_subcategoria,
              sc.nombre,
              c.nombre AS categoria,
              COUNT(DISTINCT p.id_producto)   AS productos,
              COALESCE(SUM(i.stock_actual),0) AS stock_total
            FROM subcategoria sc
            LEFT JOIN categoria c  ON c.id_categoria=sc.id_categoria
            LEFT JOIN producto p   ON p.id_subcategoria=sc.id_subcategoria
            LEFT JOIN inventario i ON i.id_producto=p.id_producto
            """);

        List<Object> params = new ArrayList<>();
        List<Integer> types = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (!q.isEmpty()) {
            where.add("sc.nombre LIKE ?");
            params.add("%" + q + "%");
            types.add(Types.VARCHAR);
        }
        if (idCat > 0) {
            where.add("sc.id_categoria = ?");
            params.add(idCat);
            types.add(Types.INTEGER);
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }

        sql.append("""
            GROUP BY sc.id_subcategoria, sc.nombre, categoria
            ORDER BY categoria ASC, sc.nombre ASC
        """);

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                int t = types.get(i);
                if (t == Types.INTEGER) ps.setInt(i + 1, (Integer) params.get(i));
                else ps.setString(i + 1, (String) params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id_subcategoria");
                    model.addRow(new Object[]{
                            String.valueOf(id),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("productos"),
                            rs.getInt("stock_total"),
                            "Editar",
                            "eliminar"
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }

        // Visual: si no hay resultados mostramos una fila informativa
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"", "Sin resultados.", "", "", "", "", ""});
        }
    }

    // Lógica: abre el diálogo de edición y refresca al guardar
    private void onEditar(int idSub) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        editar dlg = new editar(owner, idSub);
        dlg.setVisible(true);
        if (dlg.fueGuardado()) cargarTabla();
    }

    // Lógica: abre el diálogo de eliminación y refresca si se eliminó
    private void onEliminar(int idSub) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        eliminar dlg = new eliminar(owner, idSub);
        dlg.setVisible(true);
        if (dlg.fueEliminado()) cargarTabla();
    }

    // Visual: popup de error simple
    private void showErr(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "BD", JOptionPane.ERROR_MESSAGE);
    }

    // Lógica: item simple para el combo (id + etiqueta visible)
    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }

    // Visual: badge de "Stock total" (verde si >0, gris si 0)
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

    // Visual: componente pill redondeado usado por el badge
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

    // Visual: renderer de botón por celda (usa helpers de estilos)
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

    // Lógica: editor de botón por celda → llama al callback con el id de subcategoría
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
            button.setText(String.valueOf(value)); // Visual: texto del botón en la celda
            return button;
        }
    }

    // Lógica/BD: helper de conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }

    // Visual: campo de texto con placeholder (texto guía al estar vacío)
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
}
