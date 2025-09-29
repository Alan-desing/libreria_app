package admin.productos;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class panel_productos extends JPanel {

    // Visual: tabla visible y su modelo de datos
    private JTable tabla;
    private DefaultTableModel model;

    // Visual: filtros superiores (buscador, categoría, estado de stock y botón)
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbCategoria;
    private JComboBox<String> cbStock;
    private JButton btnFiltrarFila;

    // Visual: acción para crear (falta implementar diálogo crear)
    private JButton btnAgregar;

    // Lógica: guarda el "mínimo" por cada fila para pintar el badge de stock
    private final List<Integer> minsFila = new ArrayList<>();

    // Visual + Lógica: constructor de toda la pantalla 
    public panel_productos() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Visual: contenedor externo (márgenes y centrado de la card)
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14)); // alineación con el sidebar
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Visual: card blanca con borde crema 
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

        // Visual: header con título a la izquierda y “Añadir Producto” a la derecha
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Productos");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        btnAgregar = estilos.botonRedondeado("+ Añadir Producto");
        btnAgregar.setPreferredSize(new Dimension(220, 40));
        btnAgregar.setMaximumSize(new Dimension(240, 40));
        head.add(btnAgregar, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Visual: fila de filtros (buscador + categoría + stock + botón FILTRAR)
        txtBuscar = new PlaceholderTextField("Buscar…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // Visual: buscador (por nombre)
        g.gridx = 0; g.weightx = 1.0;
        filaFiltros.add(txtBuscar, g);

        // Visual: combo de categorías (se carga al final del constructor)
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(220, 38));
        cbCategoria.setMinimumSize(new Dimension(180, 38));
        g.gridx = 1; g.weightx = 0;
        filaFiltros.add(cbCategoria, g);

        // Visual: combo de estado de stock (todos / bajo / sin)
        String[] stocks = {"Stock: Todos", "Bajo (≤ mínimo)", "Sin stock"};
        cbStock = new JComboBox<>(stocks);
        estilos.estilizarCombo(cbStock);
        cbStock.setPreferredSize(new Dimension(200, 38));
        cbStock.setMinimumSize(new Dimension(160, 38));
        g.gridx = 2;
        filaFiltros.add(cbStock, g);

        // Visual: botón FILTRAR
        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));
        g.gridx = 3;
        filaFiltros.add(btnFiltrarFila, g);

        // Visual: definición de columnas de la tabla
        String[] cols = {"ID", "Nombre", "Categoría", "Stock", "Precio", "editar", "eliminar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5 || c == 6; } // Lógica: solo acciones
            @Override public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 5 || columnIndex == 6) ? JButton.class : Object.class;
            }
        };

        // Visual: estilos de la tabla (fuentes, colores, selección)
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

        // Visual: anchos 
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(90);

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

        // Visual: badge de stock (verde/amarillo/rojo) según comparación con el mínimo
        tabla.getColumnModel().getColumn(3).setCellRenderer(new StockBadgeRenderer());

        // Visual + Lógica: botones por fila (Editar / Eliminar)
        tabla.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer(false)); // editar
        tabla.getColumnModel().getColumn(6).setCellRenderer(new ButtonCellRenderer(true));  // eliminar
        tabla.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor(tabla, id -> onEditar(id), false));
        tabla.getColumnModel().getColumn(6).setCellEditor(new ButtonCellEditor(tabla, id -> onEliminar(id), true));

        // Visual: scroll 
        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420)); // scroll

        // Visual: ensamblado final
        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        // Visual: agregamos la card al shell y el shell al panel
        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Lógica: eventos de filtros
        btnFiltrarFila.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla()); // Enter

        // Lógica/BD: carga inicial de combos y datos
        cargarCategorias();
        cargarTabla();
    }

    // Lógica/BD: carga el combo de categorías desde la tabla categoria
    private void cargarCategorias() {
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        String sql = "SELECT id_categoria, nombre FROM categoria ORDER BY nombre";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbCategoria.addItem(new Item(rs.getInt("id_categoria"), rs.getString("nombre")));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando categorías:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica/BD: arma la consulta con filtros aplicados y llena la tabla
    private void cargarTabla() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat == null) ? 0 : cat.id();
        String stockSel = (String) cbStock.getSelectedItem();
        String stockFlag = ""; // "", "bajo", "sin"
        if (stockSel != null) {
            if (stockSel.startsWith("Bajo")) stockFlag = "bajo";
            else if (stockSel.startsWith("Sin"))  stockFlag = "sin";
        }

        // BD: FROM + JOINs para traer subcategoría/categoría e inventario
        String baseFrom = """
                FROM producto p
                LEFT JOIN subcategoria sc ON sc.id_subcategoria = p.id_subcategoria
                LEFT JOIN categoria c     ON c.id_categoria     = sc.id_categoria
                LEFT JOIN inventario i    ON i.id_producto      = p.id_producto
                """;

        // Lógica: WHERE dinámico (texto + categoría)
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (!q.isEmpty()) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("p.nombre LIKE ?");
            params.add("%"+q+"%");
        }
        if (idCat > 0) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("c.id_categoria = ?");
            params.add(idCat);
        }

        // Lógica: usamos HAVING para filtrar por stock total en el SELECT agregado
        String having = "";
        if ("bajo".equals(stockFlag)) {
            having = " HAVING COALESCE(SUM(i.stock_actual),0) <= COALESCE(MIN(i.stock_minimo),0)";
        } else if ("sin".equals(stockFlag)) {
            having = " HAVING COALESCE(SUM(i.stock_actual),0) = 0";
        }

        // BD: SELECT final (agrupamos por producto + precio)
        String sql = """
                SELECT
                    p.id_producto,
                    p.nombre,
                    c.nombre AS categoria,
                    COALESCE(SUM(i.stock_actual),0) AS stock_total,
                    COALESCE(MIN(i.stock_minimo),0) AS stock_min,
                    p.precio_venta
                """ + baseFrom + where + """
                 GROUP BY p.id_producto, p.nombre, categoria, p.precio_venta
                """ + having + """
                 ORDER BY p.nombre ASC
                """;

        // Visual: vaciamos la tabla y limpiamos el cache de mínimos antes de cargar
        model.setRowCount(0);
        minsFila.clear();

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            // Lógica/BD: bind de parámetros seguro
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer iv) ps.setInt(i+1, iv);
                else ps.setString(i+1, v.toString());
            }

            // Lógica + Visual: recorremos el ResultSet y volcamos al modelo
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id    = rs.getInt("id_producto");
                    String nm = rs.getString("nombre");
                    String catNom = rs.getString("categoria");
                    int stock = rs.getInt("stock_total");
                    int min   = rs.getInt("stock_min");
                    double precio = rs.getDouble("precio_venta");

                    // Lógica: guardamos el mínimo para que el renderer pinte bien el badge
                    minsFila.add(min);

                    // Visual: precio formateado con separador local
                    String precioTxt = "$ " + String.format("%,.2f", precio)
                            .replace(',', 'X').replace('.', ',').replace('X','.');

                    model.addRow(new Object[]{
                            String.valueOf(id),
                            nm,
                            (catNom==null? "—" : catNom),
                            String.valueOf(stock),
                            precioTxt,
                            "Editar",
                            "Eliminar"
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando productos:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: acción de “Editar” por fila (provisorio)
    private void onEditar(int idProducto){
        JOptionPane.showMessageDialog(this,
                "Abrir pantalla de edición para ID: " + idProducto,
                "Editar", JOptionPane.INFORMATION_MESSAGE);
    }

    // Lógica/BD: acción de “Eliminar” por fila (provisorio, inline)
    private void onEliminar(int idProducto){
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el producto #" + idProducto + "?\nEsta acción no se puede deshacer.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r!=JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            int n = ps.executeUpdate();
            if (n>0){
                JOptionPane.showMessageDialog(this, "Producto eliminado.");
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

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
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

    // Lógica: item simple para combo de categorías (id + nombre)
    static class Item {
        private final int id; private final String nombre;
        Item(int id, String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        public String toString(){ return nombre; }
    }

    // Visual: renderer de la columna "Stock" (pinta pill verde/amarillo/rojo)
    class StockBadgeRenderer implements TableCellRenderer {
        private final PillLabel lbl = new PillLabel();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            int stock = 0;
            try { stock = Integer.parseInt(String.valueOf(value).replaceAll("\\D","")); }
            catch (Exception ignore){}
            int min = (row >= 0 && row < minsFila.size()) ? minsFila.get(row) : 0;

            if (stock <= 0){
                lbl.configure(String.valueOf(stock), estilos.BADGE_NO_BG, estilos.BADGE_NO_BORDER, estilos.BADGE_NO_FG);
            } else if (stock <= min){
                lbl.configure(String.valueOf(stock), estilos.BADGE_WARN_BG, estilos.BADGE_WARN_BORDER, estilos.BADGE_WARN_FG);
            } else {
                lbl.configure(String.valueOf(stock), estilos.BADGE_OK_BG, estilos.BADGE_OK_BORDER, estilos.BADGE_OK_FG);
            }
            lbl.setSelection(isSelected);
            return lbl;
        }
    }

    // Visual: componente pill redondeado usado por el renderer de stock
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

    // Visual: renderer de botón por celda 
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

    // Lógica: editor del botón por celda → llama al callback con el id del producto
    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button;
        private final Consumer<Integer> onClick;

        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.button = danger ? estilos.botonSmDanger("Eliminar")
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
