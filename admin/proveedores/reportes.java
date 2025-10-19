package admin.proveedores;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class reportes extends JDialog {
    // visual: filtros del reporte
    private JComboBox<Item> cbProv;
    private JTextField tfDesde, tfHasta;
    // visual: tabla de resultados
    private JTable tabla;
    private DefaultTableModel model;

    // lógica: clase auxiliar para representar proveedores
    static class Item {
        int id; String nombre;
        public String toString(){ return nombre; }
        Item(int i,String n){ id=i; nombre=n; }
    }

    public reportes(Window owner){
        super(owner, "Reporte — Compras por proveedor", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(owner);

        // visual: contenedor principal
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(estilos.COLOR_FONDO);
        root.setBorder(new EmptyBorder(12,12,12,12));

        // visual: panel de filtros (proveedor + fechas)
        JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        f.setOpaque(true);
        f.setBackground(new Color(0xF7,0xE9,0xD0));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD8,0xC3,0xA3),1,true),
                new EmptyBorder(10,10,10,10)
        ));

        // visual: campos de filtro
        cbProv = new JComboBox<>();
        tfDesde = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        tfHasta = new JTextField(LocalDate.now().toString(), 10);
        JButton btn = estilos.botonBlanco("FILTRAR");

        // visual: orden de los elementos en el panel de filtros
        f.add(new JLabel("Proveedor:")); f.add(cbProv);
        f.add(new JLabel("Desde:")); f.add(tfDesde);
        f.add(new JLabel("Hasta:")); f.add(tfHasta);
        f.add(btn);

        // visual: tabla con resultados del reporte
        model = new DefaultTableModel(new String[]{"Proveedor","Pedidos","Total comprado"},0){
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(30);

        // visual: composición general de la ventana
        root.add(f, BorderLayout.NORTH);
        root.add(new JScrollPane(tabla), BorderLayout.CENTER);
        setContentPane(root);

        // BD: cargar lista de proveedores para el combo
        cargarProveedores();
        // lógica: evento del botón FILTRAR
        btn.addActionListener(e -> cargar());
        // BD: carga inicial del reporte
        cargar();
    }

    // BD: obtiene los proveedores para el combo de filtro
    private void cargarProveedores(){
        cbProv.removeAllItems();
        cbProv.addItem(new Item(0,"Todos los proveedores"));
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor,nombre FROM proveedor ORDER BY nombre");
             ResultSet rs = ps.executeQuery()){
            while (rs.next())
                cbProv.addItem(new Item(rs.getInt(1), rs.getString(2)));
        } catch (Exception ignore){}
    }

    // BD + lógica: carga los datos del reporte en la tabla
    private void cargar(){
        model.setRowCount(0);
        int id = ((Item)cbProv.getSelectedItem()).id;
        String desde=tfDesde.getText().trim(), hasta=tfHasta.getText().trim();

        // lógica: filtro dinámico para el WHERE
        String ws="WHERE DATE(p.fecha_creado) BETWEEN ? AND ?";
        if (id>0) ws+=" AND p.id_proveedor="+id;

        // BD: consulta de totales por proveedor y cantidad de pedidos
        String sql = """
            SELECT pr.nombre,
                   COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0) AS total,
                   COUNT(DISTINCT p.id_pedido) AS pedidos
            FROM pedido p
            JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
            LEFT JOIN pedido_detalle pd ON pd.id_pedido=p.id_pedido
        """+ws+"""
            GROUP BY pr.nombre
            ORDER BY total DESC
        """;

        // BD: ejecución de la consulta y llenado de la tabla
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, desde);
            ps.setString(2, hasta);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getString("nombre"),
                            rs.getInt("pedidos"),
                            "$ "+nf2(rs.getDouble("total"))
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando reporte:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }

        // lógica: mensaje si no hay resultados
        if (model.getRowCount()==0)
            model.addRow(new Object[]{"Sin datos para el rango.", "", ""});
    }

    // helpers: formato numérico con coma decimal y puntos de miles
    private String nf2(double n){
        String s=String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }
}
