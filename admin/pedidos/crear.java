package admin.pedidos;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class crear extends JDialog {

    // visual: campos y botones principales
    private JComboBox<Item> cbProveedor, cbSucursal, cbProductoTpl;
    private JTextField tfObs;
    private JTable tabla;
    private DefaultTableModel model;
    private JButton btnAgregarRenglon, btnGuardar, btnCancelar;

    // lógica: control de guardado y pedido generado
    private boolean guardado = false;
    private int idPedidoCreado = 0;

    public crear(Window owner) {
        super(owner, "Nuevo pedido", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new BorderLayout());

        // visual: panel base
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH; gbc.anchor=GridBagConstraints.PAGE_START;

        // visual: tarjeta blanca principal
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,16,16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));

        // visual: encabezado
        JLabel h1 = new JLabel("Nuevo pedido");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(h1, BorderLayout.WEST);
        head.setBorder(new EmptyBorder(0,0,8,0));
        card.add(head);

        // visual: fila de combos (proveedor / sucursal)
        cbProveedor = new JComboBox<>();
        cbSucursal  = new JComboBox<>();
        estilos.estilizarCombo(cbProveedor);
        estilos.estilizarCombo(cbSucursal);

        JPanel grid2 = new JPanel(new GridLayout(1,2,16,0));
        grid2.setOpaque(false);
        grid2.add(labeled("Proveedor", cbProveedor));
        grid2.add(labeled("Sucursal", cbSucursal));
        card.add(grid2);

        // visual: campo de observación
        tfObs = new JTextField();
        estilos.estilizarCampo(tfObs);
        tfObs.setPreferredSize(new Dimension(0, 36));
        card.add(Box.createVerticalStrut(8));
        card.add(labeled("Observación", tfObs));

        // visual: encabezado de tabla + botón agregar
        JPanel headRows = new JPanel(new BorderLayout());
        headRows.setOpaque(false);
        JLabel h2 = new JLabel("Renglones");
        h2.setFont(new Font("Arial", Font.BOLD, 18));
        h2.setForeground(estilos.COLOR_TITULO);
        headRows.add(h2, BorderLayout.WEST);
        btnAgregarRenglon = estilos.botonSm("+ Agregar renglón");
        headRows.add(btnAgregarRenglon, BorderLayout.EAST);
        headRows.setBorder(new EmptyBorder(10,0,6,0));
        card.add(headRows);

        // visual: tabla de renglones
        String[] cols = {"Producto", "Cantidad", "Precio unit.", "—"};
        model = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return true; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex){
                    case 0 -> Item.class;
                    case 1 -> Integer.class;
                    case 2 -> Double.class;
                    default -> Object.class;
                };
            }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(30);
        tabla.setFont(new Font("Arial", Font.PLAIN, 16));
        JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 16));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF,0xF3,0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0,1));

        // visual: editor de producto (combo plantilla)
        cbProductoTpl = new JComboBox<>();
        estilos.estilizarCombo(cbProductoTpl);
        tabla.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cbProductoTpl));

        // visual: botón “Quitar” por fila
        tabla.getColumnModel().getColumn(3).setCellRenderer((t,val,sel,focus,row,col)-> estilos.botonSm("Quitar"));
        tabla.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JTextField()){
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean isSelected, int row, int column){
                JButton b = estilos.botonSm("Quitar");
                b.addActionListener(e -> {
                    stopCellEditing();
                    if (row>=0 && row<model.getRowCount()) model.removeRow(row);
                });
                return b;
            }
        });

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        card.add(sc);

        // visual: footer con botones de acción
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        btnGuardar = estilos.botonBlanco("Guardar Borrador");
        btnCancelar = estilos.botonSm("Cancelar");
        actions.add(btnGuardar);
        actions.add(btnCancelar);
        actions.setBorder(new EmptyBorder(12,0,0,0));
        card.add(actions);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // lógica: eventos
        btnAgregarRenglon.addActionListener(e -> addRow());
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());

        // lógica: carga inicial de combos y fila por defecto
        cargarCombos();
        addRow();
    }

    // visual: etiqueta + componente (usado en combos y campo)
    private JPanel labeled(String label, JComponent comp){
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BorderLayout());
        JLabel l = new JLabel(label);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    // lógica: agrega una fila nueva a la tabla
    private void addRow(){
        model.addRow(new Object[]{ (Item)cbProductoTpl.getItemAt(0), 1, 0.00, "Quitar" });
    }

    // BD: carga de catálogos para combos
    private void cargarCombos(){
        DefaultComboBoxModel<Item> prov = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Item> suc  = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Item> prod = new DefaultComboBoxModel<>();
        try (Connection cn = DB.get()){
            // proveedores
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor,nombre FROM proveedor ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) prov.addElement(new Item(rs.getInt(1), rs.getString(2)));
            }
            // sucursales
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_sucursal,nombre FROM sucursal ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) suc.addElement(new Item(rs.getInt(1), rs.getString(2)));
            }
            // productos
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_producto,nombre,precio_compra FROM producto ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) prod.addElement(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
        cbProveedor.setModel(prov);
        cbSucursal.setModel(suc);
        cbProductoTpl.setModel(prod);
    }

    // lógica + BD: guarda el pedido y sus detalles
    private void onGuardar(){
        // lógica: validación básica
        Item prov = (Item) cbProveedor.getSelectedItem();
        Item suc  = (Item) cbSucursal.getSelectedItem();
        if (prov==null || suc==null){
            JOptionPane.showMessageDialog(this, "Proveedor y Sucursal son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Row> det = new ArrayList<>();
        for (int i=0;i<model.getRowCount();i++){
            Object pObj = model.getValueAt(i,0);
            Object cObj = model.getValueAt(i,1);
            Object uObj = model.getValueAt(i,2);
            if (!(pObj instanceof Item it)) continue;
            int cant = toInt(cObj);
            double precio = toDouble(uObj);
            if (it.id<=0 || cant<=0 || precio<0) continue;
            det.add(new Row(it.id, cant, precio));
        }
        if (det.isEmpty()){
            JOptionPane.showMessageDialog(this, "Agregá al menos un renglón válido.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String obs = tfObs.getText()==null? "" : tfObs.getText().trim();

        // BD: inserta pedido y sus renglones
        try (Connection cn = DB.get()){
            cn.setAutoCommit(false);
            try {
                int idUsuario = nullUserId(cn);
                int estadoBorrador = 1;

                // inserta cabecera de pedido
                try (PreparedStatement ins = cn.prepareStatement(
                        "INSERT INTO pedido (id_proveedor,id_sucursal,id_usuario,id_estado_pedido,fecha_creado,fecha_estado,observacion) " +
                        "VALUES (?,?,?,?,NOW(),NOW(),?)",
                        Statement.RETURN_GENERATED_KEYS)){
                    ins.setInt(1, prov.id);
                    ins.setInt(2, suc.id);
                    if (idUsuario>0) ins.setInt(3, idUsuario); else ins.setNull(3, Types.INTEGER);
                    ins.setInt(4, estadoBorrador);
                    ins.setString(5, obs);
                    ins.executeUpdate();
                    try (ResultSet gk = ins.getGeneratedKeys()){
                        if (gk.next()) idPedidoCreado = gk.getInt(1);
                    }
                }

                // inserta detalle de pedido
                try (PreparedStatement d = cn.prepareStatement(
                        "INSERT INTO pedido_detalle (id_pedido,id_producto,cantidad_solicitada,precio_unitario) VALUES (?,?,?,?)")){
                    for (Row r : det){
                        d.setInt(1, idPedidoCreado);
                        d.setInt(2, r.idProd);
                        d.setInt(3, r.cant);
                        d.setDouble(4, r.precio);
                        d.addBatch();
                    }
                    d.executeBatch();
                }

                cn.commit();
                guardado = true;
                JOptionPane.showMessageDialog(this, "Pedido #"+idPedidoCreado+" creado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex){
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error guardando pedido:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: ID de usuario (placeholder si no hay sesión)
    private int nullUserId(Connection cn){ return 0; }

    public boolean fueGuardado(){ return guardado; }
    public int getIdPedidoCreado(){ return idPedidoCreado; }

    // utils: conversiones seguras
    private int toInt(Object o){
        try { return Integer.parseInt(String.valueOf(o).replaceAll("\\D","")); } catch(Exception e){ return 0; }
    }
    private double toDouble(Object o){
        try { return Double.parseDouble(String.valueOf(o).replace(',','.')); } catch(Exception e){ return 0.0; }
    }

    // tipos auxiliares
    static class Row {
        final int idProd; final int cant; final double precio;
        Row(int idProd, int cant, double precio){ this.idProd=idProd; this.cant=cant; this.precio=precio; }
    }
    static class Item {
        final int id; final String nombre;
        Item(int id,String nombre){ this.id=id; this.nombre=nombre; }
        @Override public String toString(){ return nombre; }
    }

    // BD: helper unificado de conexión
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
