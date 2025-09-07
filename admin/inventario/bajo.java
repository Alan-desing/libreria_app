package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class bajo extends JDialog {

    private JTable tabla;
    private DefaultTableModel model;

    private JComboBox<Item> cbCategoria;
    private JButton btnFiltrar;
    private boolean huboCambios = false;

    public bajo(Window owner) {
        super(owner, "Productos con stock bajo", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 18, 16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Productos con stock bajo");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        JButton btnCerrar = estilos.botonSm("Cerrar");
        head.add(btnCerrar, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);

        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(240, 38));
        filtros.add(cbCategoria);

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120, 38));
        filtros.add(btnFiltrar);

        // Tabla
        String[] cols = {"ID", "Producto", "Categoría", "Stock", "Mínimo", "Ajustar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 5) ? JButton.class : Object.class;
            }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 16));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        tabla.getTableHeader().setReorderingAllowed(false);
        JTableHeader th = tabla.getTableHeader();
        th.setBackground(new Color(0xFF,0xF3,0xD9));

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(90);

        // ID con "#"
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#" + String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // botón Ajustar en cada fila
        tabla.getColumnModel().getColumn(5).setCellRenderer(new ButtonCellRenderer(false));
        tabla.getColumnModel().getColumn(5).setCellEditor(new ButtonCellEditor(tabla, id -> onAjustar(id), false));

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420));

        card.add(head);
        card.add(filtros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8, 8, 8, 8);
        add(card, root);

        btnCerrar.addActionListener(e -> dispose());
        btnFiltrar.addActionListener(e -> cargarTabla());

        cargarCategorias();
        cargarTabla();
    }

    public boolean huboCambios(){ return huboCambios; }

    private void cargarCategorias(){
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        String sql = "SELECT id_categoria, nombre FROM categoria ORDER BY nombre";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            while(rs.next()){
                cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
        }catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando categorías:\n"+ex.getMessage(),"BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarTabla(){
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat==null)?0:cat.id;

        String w = " WHERE COALESCE(i.stock_actual,0) <= COALESCE(i.stock_minimo,0) ";
        List<Object> params = new ArrayList<>();
        if (idCat>0){
            w += " AND c.id_categoria=? ";
            params.add(idCat);
        }

        String sql = """
            SELECT p.id_producto, p.nombre, c.nombre AS categoria,
                   COALESCE(i.stock_actual,0) AS stock_actual,
                   COALESCE(i.stock_minimo,0) AS stock_minimo
            FROM producto p
            LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
            LEFT JOIN categoria c ON c.id_categoria=sc.id_categoria
            LEFT JOIN inventario i ON i.id_producto=p.id_producto
        """ + w + " ORDER BY c.nombre, p.nombre";

        model.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){

            int bind=1;
            for (Object v: params){
                if (v instanceof Integer iv) ps.setInt(bind++, iv);
                else ps.setString(bind++, String.valueOf(v));
            }

            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            String.valueOf(rs.getInt("id_producto")),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock_actual"),
                            rs.getInt("stock_minimo"),
                            "Ajustar"
                    });
                }
            }

            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","No hay productos en bajo stock.","","","",""});
            }

        }catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando listado:\n"+ex.getMessage(),"BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAjustar(int idProd){
        Window owner = SwingUtilities.getWindowAncestor(this);
        try{
            ajustar dlg = new ajustar(owner, idProd);
            dlg.setVisible(true);
            if (dlg.fueGuardado()){
                huboCambios = true;
                cargarTabla();
            }
        }catch(Throwable ex){
            JOptionPane.showMessageDialog(this, "No se pudo abrir Ajustar:\n"+ex.getMessage(), "Inventario", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== helpers =====
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
    static class Item { final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        public String toString(){ return label; }
    }

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

    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button;
        private final Consumer<Integer> onClick;

        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.button = danger ? estilos.botonSmDanger("Ajustar") : estilos.botonSm("Ajustar");
            // *** FIX CROSS-VERSION: lambda sin tipar el parámetro y método sin args ***
            this.button.addActionListener(e -> handle());
        }

        private void handle(){
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
