package admin.productos;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class panel_productos extends JPanel {

    private JTable tabla;
    private DefaultTableModel model;

    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbCategoria;  // Item(id, nombre)
    private JComboBox<String> cbStock;
    private JButton btnFiltrarFila;
    private JButton btnAgregar;

    // guardo los mínimos por fila para colorear el stock (no se muestran)
    private final List<Integer> minsFila = new ArrayList<>();

    public panel_productos() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // ===== Título (fuera de la card, como en la web) =====
        JLabel h1 = new JLabel("Productos");
        h1.setFont(new Font("Arial", Font.BOLD, 28));
        h1.setForeground(estilos.COLOR_TITULO);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 8, 28));
        header.add(h1, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ===== Shell que centra la “card” =====
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // ===== Card =====
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(new Color(0xFF, 0xF9, 0xEF));                 // #fff9ef
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(0xE6,0xD9,0xBF), 1, true),
                BorderFactory.createEmptyBorder(14, 14, 16, 14)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(980, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Fila superior: SOLO “+ Añadir Producto” =====
        JPanel filaTop = new JPanel();
        filaTop.setOpaque(false);
        filaTop.setLayout(new BoxLayout(filaTop, BoxLayout.X_AXIS));
        filaTop.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnAgregar = estilos.botonRedondeado("+ Añadir Producto");
        btnAgregar.setPreferredSize(new Dimension(220, 40));
        btnAgregar.setMaximumSize(new Dimension(240, 40));

        filaTop.add(Box.createHorizontalGlue());
        filaTop.add(btnAgregar);

        // ===== Buscador =====
        txtBuscar = new PlaceholderTextField("Buscar…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));
        txtBuscar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ===== Fila de filtros (buscador + categoría + stock + FILTRAR) =====
        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // 1) buscador
        g.gridx = 0; g.weightx = 1.0;
        filaFiltros.add(txtBuscar, g);

        // 2) categoría (cargada desde BD)
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(220, 38));
        cbCategoria.setMinimumSize(new Dimension(180, 38));
        g.gridx = 1; g.weightx = 0;
        filaFiltros.add(cbCategoria, g);

        // 3) stock
        String[] stocks = {"Stock: Todos", "Bajo (≤ mínimo)", "Sin stock"};
        cbStock = new JComboBox<>(stocks);
        estilos.estilizarCombo(cbStock);
        cbStock.setPreferredSize(new Dimension(200, 38));
        cbStock.setMinimumSize(new Dimension(160, 38));
        g.gridx = 2;
        filaFiltros.add(cbStock, g);

        // 4) botón Filtrar
        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));
        g.gridx = 3;
        filaFiltros.add(btnFiltrarFila, g);

        // ===== Tabla =====
        String[] cols = {"ID", "Nombre", "Categoría", "Stock", "Precio"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return String.class; // todo como texto para forzar alineación izquierda
            }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));  // celdas más grandes
        tabla.setRowHeight(32);                           // filas más altas
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 17)); // cabecera más grande
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tabla.getTableHeader().setBackground(new Color(0xFF,0xF3,0xD9)); // header crema

        // apariencia más "ligera" tipo web
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));          // línea suave
        tabla.setIntercellSpacing(new Dimension(0, 1));   // separador fino
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2E7D6)); // selección suave
        tabla.setSelectionForeground(new Color(0x333333));

        // anchos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);

        // renderer general: alineación a la IZQUIERDA para todas las columnas
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // renderer de "Stock" con color (usa minsFila oculto)
        DefaultTableCellRenderer stockRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel)c).setHorizontalAlignment(SwingConstants.LEFT);
                c.setForeground(table.getForeground());
                c.setFont(table.getFont());
                if (!isSelected) {
                    int stock;
                    try { stock = Integer.parseInt(String.valueOf(value).replaceAll("\\D","")); }
                    catch (Exception e) { stock = 0; }
                    int min = (row >= 0 && row < minsFila.size()) ? minsFila.get(row) : 0;

                    if (stock <= 0) {
                        c.setForeground(new Color(180, 40, 40));   // rojo
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (stock <= min) {
                        c.setForeground(new Color(180, 120, 20));  // ámbar
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        };
        tabla.getColumnModel().getColumn(3).setCellRenderer(stockRenderer); // aplicar a "Stock"

        JScrollPane sc = new JScrollPane(tabla);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(0xE6,0xD9,0xBF), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 380));

        // ===== Ensamble =====
        card.add(filaTop);
        card.add(Box.createVerticalStrut(10));
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(10));
        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // ==== Eventos ====
        btnFiltrarFila.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla()); // Enter filtra

        // ==== Carga inicial ====
        cargarCategorias();
        cargarTabla();
    }

    // ======================= Cargar categorías (BD)
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

    // ======================= Cargar tabla (BD + filtros)
    private void cargarTabla() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat == null) ? 0 : cat.id();
        String stockSel = (String) cbStock.getSelectedItem();
        String stockFlag = ""; // "", "bajo", "sin"
        if (stockSel != null) {
            if (stockSel.startsWith("Bajo")) stockFlag = "bajo";
            else if (stockSel.startsWith("Sin")) stockFlag = "sin";
        }

        String baseFrom = """
                FROM producto p
                LEFT JOIN subcategoria sc ON sc.id_subcategoria = p.id_subcategoria
                LEFT JOIN categoria c     ON c.id_categoria     = sc.id_categoria
                LEFT JOIN inventario i    ON i.id_producto      = p.id_producto
                """;

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

        String having = "";
        if ("bajo".equals(stockFlag)) {
            having = " HAVING COALESCE(SUM(i.stock_actual),0) <= COALESCE(MIN(i.stock_minimo),0)";
        } else if ("sin".equals(stockFlag)) {
            having = " HAVING COALESCE(SUM(i.stock_actual),0) = 0";
        }

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

        model.setRowCount(0);
        minsFila.clear();

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
                    int id    = rs.getInt("id_producto");
                    String nm = rs.getString("nombre");
                    String catNom = rs.getString("categoria");
                    int stock = rs.getInt("stock_total");
                    int min   = rs.getInt("stock_min");
                    double precio = rs.getDouble("precio_venta");

                    // guardo el mínimo (para colorear el stock) pero no lo muestro
                    minsFila.add(min);

                    // Precio formateado ($ 1.234,56) como texto para que quede a la IZQUIERDA
                    String precioTxt = "$ " + String.format("%,.2f", precio)
                            .replace(',', 'X').replace('.', ',').replace('X','.');

                    model.addRow(new Object[]{
                            String.valueOf(id),
                            nm,
                            (catNom==null? "—" : catNom),
                            String.valueOf(stock),
                            precioTxt
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando productos:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ====== Helper: conexión simple a MySQL ====== */
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = ""; // tu clave si la tenés
            return DriverManager.getConnection(url, user, pass);
        }
    }

    // ===== Placeholder en JTextField =====
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

    // ===== Item para combo categoría (id, nombre) =====
    static class Item {
        private final int id; private final String nombre;
        Item(int id, String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        public String toString(){ return nombre; }
    }
}
