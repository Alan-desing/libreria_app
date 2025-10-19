package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ver {

    // lógica: ventana de detalle del producto con ficha, stock y movimientos
    public static void abrir(Component parent, int idProducto){
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(owner, "Producto #"+idProducto, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setSize(720, 560);
        dlg.setLocationRelativeTo(parent);

        // visual: paneles principales (ficha + stock + movimientos)
        JPanel ficha = new JPanel(new GridLayout(0,1,4,4));
        ficha.setBorder(BorderFactory.createTitledBorder("Ficha"));

        DefaultTableModel mStock = new DefaultTableModel(new Object[]{"Sucursal","Stock"}, 0){
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable tStock = new JTable(mStock);
        JScrollPane spStock = new JScrollPane(tStock);
        spStock.setBorder(BorderFactory.createTitledBorder("Stock por sucursal"));

        DefaultTableModel mMov = new DefaultTableModel(
                new Object[]{"Fecha","Tipo","Cant.","Prev → Nuevo","Motivo","Usuario"}, 0){
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable tMov = new JTable(mMov);
        JScrollPane spMov = new JScrollPane(tMov);
        spMov.setBorder(BorderFactory.createTitledBorder("Últimos movimientos"));

        JPanel content = new JPanel(new BorderLayout(8,8));
        JPanel tablas = new JPanel(new GridLayout(1,2,8,8));
        tablas.add(spStock); tablas.add(spMov);

        // visual: botones de acción
        JButton btnEditar = new JButton("Editar…");
        JButton btnCerrar = new JButton("Cerrar");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(btnEditar); actions.add(btnCerrar);

        // eventos
        btnCerrar.addActionListener(e -> dlg.dispose());
        btnEditar.addActionListener(e -> {
            editar.abrir(dlg, idProducto);   // abrir edición
            cargarTodo(idProducto, ficha, mStock, mMov); // refrescar datos
        });

        // armado de la vista
        content.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        content.add(ficha, BorderLayout.NORTH);
        content.add(tablas, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        dlg.setContentPane(content);

        // lógica: cargar toda la información inicial
        cargarTodo(idProducto, ficha, mStock, mMov);

        dlg.setVisible(true);
    }

    // lógica: carga completa de ficha, stock y movimientos
    private static void cargarTodo(int id, JPanel ficha, DefaultTableModel mStock, DefaultTableModel mMov){
        ficha.removeAll();
        mStock.setRowCount(0);
        mMov.setRowCount(0);

        try (Connection cn = conexion_bd.getConnection()){

            // BD: obtener datos principales del producto con joins
            String qProd = """
                SELECT p.*, sc.nombre AS subcategoria, c.nombre AS categoria, prov.nombre AS proveedor
                FROM producto p
                LEFT JOIN subcategoria sc ON sc.id_subcategoria = p.id_subcategoria
                LEFT JOIN categoria c     ON c.id_categoria     = sc.id_categoria
                LEFT JOIN proveedor prov  ON prov.id_proveedor  = p.id_proveedor
                WHERE p.id_producto = ?
            """;
            try (PreparedStatement ps = cn.prepareStatement(qProd)){
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    if (!rs.next()){
                        JOptionPane.showMessageDialog(ficha,
                                "Producto no encontrado",
                                "BD", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // visual: mostrar datos en la ficha
                    String nombre = rs.getString("nombre");
                    String codigo = rs.getString("codigo");
                    String categoria = rs.getString("categoria");
                    String subcategoria = rs.getString("subcategoria");
                    String proveedor = rs.getString("proveedor");
                    double pc = rs.getDouble("precio_compra");
                    double pv = rs.getDouble("precio_venta");
                    String ubi = rs.getString("ubicacion");
                    boolean activo = rs.getInt("activo") == 1;
                    String desc = rs.getString("descripcion");

                    ficha.add(label("Nombre: ", nombre));
                    ficha.add(label("Código: ", (codigo == null || codigo.isBlank()) ? "—" : codigo));
                    ficha.add(label("Categoría: ",
                            (categoria == null ? "—" : categoria) +
                            (subcategoria != null && !subcategoria.isBlank() ? " / " + subcategoria : "")));
                    ficha.add(label("Proveedor: ", proveedor == null ? "—" : proveedor));
                    ficha.add(label("Precio compra: ", "$ " + fmt(pc)));
                    ficha.add(label("Precio venta: ", "$ " + fmt(pv)));
                    ficha.add(label("Ubicación: ", ubi == null ? "" : ubi));
                    ficha.add(label("Estado: ", activo ? "Activo" : "Inactivo"));

                    if (desc != null && !desc.isBlank()){
                        JTextArea ta = new JTextArea(desc);
                        ta.setLineWrap(true);
                        ta.setWrapStyleWord(true);
                        ta.setEditable(false);
                        ta.setBackground(ficha.getBackground());
                        JPanel block = new JPanel(new BorderLayout());
                        block.add(new JLabel("Descripción:"), BorderLayout.NORTH);
                        block.add(ta, BorderLayout.CENTER);
                        ficha.add(block);
                    }
                }
            }

            // BD: obtener stock por sucursal
            String qStock = """
                SELECT s.nombre, COALESCE(i.stock_actual,0) AS stock
                FROM sucursal s
                LEFT JOIN inventario i ON i.id_sucursal = s.id_sucursal AND i.id_producto = ?
                ORDER BY s.nombre
            """;
            int total = 0;
            try (PreparedStatement ps = cn.prepareStatement(qStock)){
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    while (rs.next()){
                        String suc = rs.getString(1);
                        int st = rs.getInt(2);
                        total += st;
                        mStock.addRow(new Object[]{suc, st});
                    }
                }
            }
            mStock.addRow(new Object[]{"Total", total});

            // BD: obtener últimos movimientos de inventario
            String qMov = """
                SELECT m.creado_en, m.tipo, m.cantidad, m.stock_prev, m.stock_nuevo, m.motivo, u.nombre AS usuario
                FROM inventario_mov m
                LEFT JOIN usuario u ON u.id_usuario = m.id_usuario
                WHERE m.id_producto = ?
                ORDER BY m.creado_en DESC
                LIMIT 20
            """;
            try (PreparedStatement ps = cn.prepareStatement(qMov)){
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    while (rs.next()){
                        mMov.addRow(new Object[]{
                                rs.getString("creado_en"),
                                rs.getString("tipo"),
                                rs.getInt("cantidad"),
                                rs.getInt("stock_prev") + " → " + rs.getInt("stock_nuevo"),
                                valOrDash(rs.getString("motivo")),
                                valOrDash(rs.getString("usuario"))
                        });
                    }
                }
            }

            ficha.revalidate();
            ficha.repaint();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(ficha,
                    "Error al cargar:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // visual: formatear valores y crear componentes de ficha
    private static String fmt(double v){
        return String.format("%,.2f", v)
                .replace(',', 'X')
                .replace('.', ',')
                .replace('X','.');
    }

    private static JPanel label(String k, String v){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        JLabel l1 = new JLabel(k);
        l1.setFont(l1.getFont().deriveFont(Font.BOLD));
        p.add(l1);
        p.add(new JLabel(v == null ? "" : v));
        return p;
    }

    private static String valOrDash(String s){
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
