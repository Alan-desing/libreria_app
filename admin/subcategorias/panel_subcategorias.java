package admin.subcategorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de Subcategorías — versión desktop (match con la web).
 * Incluye:
 *  - Filtro por texto y por categoría
 *  - Tabla con métricas: productos y stock total por subcategoría
 *  - Botones Editar / Eliminar (acciones placeholder por ahora)
 */
public class panel_subcategorias extends JPanel {

    // Filtros
    private JTextField txtBuscar;
    private JComboBox<Item> cbCategoria;
    private JButton btnFiltrar, btnLimpiar, btnNuevo;

    // Tabla
    private JTable tabla;
    private DefaultTableModel model;

    public panel_subcategorias() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // ====== CONTENEDOR con scroll (como Inicio) ======
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        content.setAlignmentX(Component.CENTER_ALIGNMENT);

        JScrollPane scroll = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        scroll.setBorder(null);

        Color crema = estilos.COLOR_FONDO;
        content.setOpaque(true);
        content.setBackground(crema);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(crema);
        scroll.setBackground(crema);

        add(scroll, BorderLayout.CENTER);

        // ====== Card principal ======
        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        content.add(card);

        // Header del card (título + botón "Añadir")
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel("Subcategorías");
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setForeground(estilos.COLOR_TITULO);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        btnNuevo = estilos.botonRedondeado("+ Añadir Subcategoría");
        right.add(btnNuevo);

        head.add(h, BorderLayout.WEST);
        head.add(right, BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(head);

        // ====== Filtros ======
        card.add(crearFiltros());

        // ====== Tabla ======
        card.add(crearTabla());

        // Acciones
        btnFiltrar.addActionListener(e -> cargarTabla());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            if (cbCategoria.getItemCount() > 0) cbCategoria.setSelectedIndex(0);
            cargarTabla();
        });
        btnNuevo.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Abrir formulario: admin/subcategorias/crear.php\n(Implementación futura)",
                        "Nuevo", JOptionPane.INFORMATION_MESSAGE));

        // Carga inicial
        cargarCategorias();
        cargarTabla();
    }

    /* ===================== UI building ===================== */

    private JPanel crearFiltros() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel cardFiltros = new JPanel();
        cardFiltros.setOpaque(true);
        cardFiltros.setBackground(Color.WHITE);
        cardFiltros.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        cardFiltros.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        txtBuscar = new JTextField(22);
        estilizarInput(txtBuscar);
        txtBuscar.setToolTipText("Buscar subcategoría…");

        cbCategoria = new JComboBox<>();
        cbCategoria.setFont(new Font("Arial", Font.PLAIN, 14));
        cbCategoria.setPreferredSize(new Dimension(220, 34));

        btnFiltrar = estilos.botonSm("Filtrar");
        btnLimpiar = estilos.botonSm("Limpiar");

        cardFiltros.add(txtBuscar);
        cardFiltros.add(cbCategoria);
        cardFiltros.add(btnFiltrar);
        cardFiltros.add(btnLimpiar);

        wrap.add(cardFiltros, BorderLayout.CENTER);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return wrap;
    }

    private JComponent crearTabla() {
        String[] cols = {"ID", "Subcategoría", "Categoría", "Productos", "Stock total", "editar", "eliminar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 5 || c == 6; // sólo botones
            }
        };

        tabla = new JTable(model);
        tabla.setRowHeight(30);
        tabla.setFont(new Font("Arial", Font.PLAIN, 16));
        JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 16));
        th.setBackground(new Color(0xFF, 0xF3, 0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));

        // Column widths
        setColW(0, 80);  // ID
        setColW(3, 140); // productos
        setColW(4, 140); // stock
        setColW(5, 100); // editar
        setColW(6, 110); // eliminar

        // Renderers
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        tabla.getColumnModel().getColumn(5).setCellRenderer(new BtnRenderer("Editar"));
        tabla.getColumnModel().getColumn(5).setCellEditor(new BtnEditor("Editar", (row) -> {
            int id = parseId(model.getValueAt(row, 0));
            JOptionPane.showMessageDialog(this,
                    "Abrir ruta: admin/subcategorias/editar.php?id=" + id,
                    "Editar", JOptionPane.INFORMATION_MESSAGE);
        }));

        tabla.getColumnModel().getColumn(6).setCellRenderer(new BtnRenderer("eliminar"));
        tabla.getColumnModel().getColumn(6).setCellEditor(new BtnEditor("eliminar", (row) -> {
            int id = parseId(model.getValueAt(row, 0));
            int r = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar subcategoría #" + id + "?\n(Acción a implementar)",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                /* implementar eliminación real si lo querés acá.*/
            }
        }));

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        return sc;
    }

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

    private void estilizarInput(JTextField f) {
        f.setFont(new Font("Arial", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9, 0xD9, 0xD9), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        f.setBackground(Color.WHITE);
    }

    private void setColW(int col, int w) {
        TableColumn c = tabla.getColumnModel().getColumn(col);
        c.setPreferredWidth(w);
        c.setMinWidth(60);
    }

    private int parseId(Object v) {
        try {
            String s = String.valueOf(v).replace("#", "").trim();
            return Integer.parseInt(s);
        } catch (Exception e) { return -1; }
    }

    /* ===================== Carga de datos ===================== */

    private void cargarCategorias() {
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_categoria, nombre FROM categoria ORDER BY nombre")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
        } catch (Exception ex) {
            showErr(ex);
        }
        cbCategoria.setSelectedIndex(0);
    }

    private void cargarTabla() {
        model.setRowCount(0);

        String q = txtBuscar.getText().trim();
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
                    model.addRow(new Object[]{
                            "#" + rs.getInt("id_subcategoria"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("productos"),
                            rs.getInt("stock_total"),
                            "Editar",
                            "eliminar"
                    });
                }
            }
        } catch (Exception ex) {
            showErr(ex);
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"", "Sin resultados.", "", "", "", "", ""});
        }
    }

    /* ===================== Helpers ===================== */

    private void showErr(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "BD", JOptionPane.ERROR_MESSAGE);
    }

    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }

    // Botón renderer simple
    static class BtnRenderer extends JButton implements TableCellRenderer {
        BtnRenderer(String txt){
            super(txt);
            setFocusPainted(false);
            setFont(new Font("Arial", Font.PLAIN, 14));
            setBackground(new Color(0x8BA069));   // similar a btn-sm de la web
            setForeground(Color.WHITE);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // Editor que dispara callback con el índice de fila
    static class BtnEditor extends DefaultCellEditor {
        private final JButton btn = new JButton();
        private final RowAction action;  // campo que queremos usar
        private int row = -1;

        // Renombré el parámetro para evitar sombra con el campo
        BtnEditor(String text, RowAction rowAction) {
            super(new JTextField());
            this.action = rowAction;
            btn.setText(text);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Arial", Font.PLAIN, 14));
            btn.setBackground(new Color(0x8BA069));
            btn.setForeground(Color.WHITE);
            btn.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    if (row >= 0) BtnEditor.this.action.onClick(row); // usa el CAMPO
                }
            });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return btn;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }

    interface RowAction { void onClick(int row); }

    /* ====== Conexión BD (igual a panel_inicio) ====== */
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
