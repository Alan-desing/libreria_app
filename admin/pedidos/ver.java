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

public class ver extends JDialog {

    private final int idPedido;
    private JLabel lbProveedor, lbSucursal, lbEstado, lbCreado, lbFechaEstado, lbUsuario, lbObs, lbTotal;
    private JTable tabla;
    private DefaultTableModel model;
    private JButton btnAprobar, btnEnviar, btnRecibir, btnCancelar, btnCerrar;

    private boolean cambios = false;

    public ver(Window owner, int idPedido) {
        super(owner, "Pedido #"+idPedido, ModalityType.APPLICATION_MODAL);
        this.idPedido = idPedido;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 640);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new BorderLayout());

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH; gbc.anchor=GridBagConstraints.PAGE_START;

        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,16,16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));

        JLabel h1 = new JLabel("Pedido #"+idPedido);
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(h1, BorderLayout.WEST);
        head.setBorder(new EmptyBorder(0,0,8,0));
        card.add(head);

        // Datos
        JPanel grid = new JPanel(new GridLayout(3,2,16,8));
        grid.setOpaque(false);

        lbProveedor = new JLabel("—");
        lbSucursal  = new JLabel("—");
        lbEstado    = new JLabel("—");
        lbCreado    = new JLabel("—");
        lbFechaEstado = new JLabel("—");
        lbUsuario   = new JLabel("—");
        lbObs       = new JLabel("—");

        grid.add(labeled("Proveedor:", lbProveedor));
        grid.add(labeled("Sucursal:", lbSucursal));
        grid.add(labeled("Estado:", lbEstado));
        grid.add(labeled("Creado:", lbCreado));
        grid.add(labeled("Fecha estado:", lbFechaEstado));
        grid.add(labeled("Usuario:", lbUsuario));

        card.add(grid);
        card.add(Box.createVerticalStrut(6));
        card.add(labeled("Observación:", lbObs));
        card.add(Box.createVerticalStrut(8));

        // Detalle
        JLabel h2 = new JLabel("Renglones");
        h2.setFont(new Font("Arial", Font.BOLD, 18));
        h2.setForeground(estilos.COLOR_TITULO);
        JPanel head2 = new JPanel(new BorderLayout());
        head2.setOpaque(false);
        head2.add(h2, BorderLayout.WEST);
        card.add(head2);

        String[] cols = {"Producto","Cantidad","Precio","Subtotal"};
        model = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){ return false; }
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

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        card.add(sc);

        // Total
        lbTotal = new JLabel("$ 0,00");
        lbTotal.setFont(new Font("Arial", Font.BOLD, 18));
        lbTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.add(new JLabel(" "), BorderLayout.WEST);
        totalRow.add(lbTotal, BorderLayout.EAST);
        totalRow.setBorder(new EmptyBorder(6,0,6,0));
        card.add(totalRow);

        // Acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        actions.setOpaque(false);
        btnAprobar = estilos.botonBlanco("Aprobar");
        btnEnviar  = estilos.botonBlanco("Enviar");
        btnRecibir = estilos.botonBlanco("Recibir");
        btnCancelar= estilos.botonSm("Cancelar");
        btnCerrar  = estilos.botonSm("Cerrar");
        actions.add(btnAprobar);
        actions.add(btnEnviar);
        actions.add(btnRecibir);
        actions.add(btnCancelar);
        actions.add(btnCerrar);
        actions.setBorder(new EmptyBorder(10,0,0,0));
        card.add(actions);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Eventos
        btnCerrar.addActionListener(e -> dispose());
        btnAprobar.addActionListener(e -> doAccion("aprobar"));
        btnEnviar.addActionListener(e -> doAccion("enviar"));
        btnRecibir.addActionListener(e -> doAccion("recibir"));
        btnCancelar.addActionListener(e -> doAccion("cancelar"));

        // Cargar datos
        cargar();
    }

    private JPanel labeled(String t, JComponent c){
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel(t);
        p.add(l, BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void cargar(){
        model.setRowCount(0);
        try (Connection cn = DB.get()){
            Pedido ped = getPedido(cn, idPedido);
            if (ped==null){
                JOptionPane.showMessageDialog(this, "Pedido inexistente.", "Aviso", JOptionPane.WARNING_MESSAGE);
                dispose(); return;
            }
            lbProveedor.setText(ped.proveedor);
            lbSucursal.setText(ped.sucursal);
            lbEstado.setText(ped.nombreEstado + " ("+ped.idEstado+")");
            lbCreado.setText(ped.fechaCreado);
            lbFechaEstado.setText(ped.fechaEstado);
            lbUsuario.setText(ped.usuario==null? "—": ped.usuario);
            lbObs.setText(ped.observacion==null? "": ped.observacion);

            List<ItemRow> det = getDetalle(cn, idPedido);
            double total = 0.0;
            for (ItemRow r: det){
                double sub = r.cantidad * r.precio;
                total += sub;
                model.addRow(new Object[]{ r.producto, r.cantidad, "$ "+nf2(r.precio), "$ "+nf2(sub) });
            }
            lbTotal.setText("Total   $ "+nf2(total));

            // Habilitar acciones según estado
            btnAprobar.setEnabled(ped.idEstado==1);
            btnEnviar.setEnabled(ped.idEstado==2);
            btnRecibir.setEnabled(ped.idEstado==3);
            btnCancelar.setEnabled(ped.idEstado==1 || ped.idEstado==2 || ped.idEstado==3);

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doAccion(String accion){
        try (Connection cn = DB.get()){
            cn.setAutoCommit(false);
            try {
                Pedido ped = getPedido(cn, idPedido);
                if (ped==null) throw new Exception("Pedido inexistente.");
                // Permisos por estado (como la web)
                if (accion.equals("aprobar") && ped.idEstado==1){
                    updateEstado(cn, 2);
                } else if (accion.equals("enviar") && ped.idEstado==2){
                    updateEstado(cn, 3);
                } else if (accion.equals("recibir") && ped.idEstado==3){
                    // actualizar inventario por sucursal/producto
                    for (ItemRow r : getDetalle(cn, idPedido)){
                        actualizarInventario(cn, ped.idSucursal, r.idProducto, r.cantidad);
                    }
                    updateEstado(cn, 4);
                } else if (accion.equals("cancelar") && (ped.idEstado==1 || ped.idEstado==2 || ped.idEstado==3)){
                    updateEstado(cn, 5);
                } else {
                    throw new Exception("Acción no permitida para el estado actual.");
                }
                cn.commit();
                cambios = true;
                cargar();
                JOptionPane.showMessageDialog(this, "Acción realizada: "+accion, "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex){
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error realizando acción:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateEstado(Connection cn, int nuevo) throws Exception {
        try (PreparedStatement up = cn.prepareStatement(
                "UPDATE pedido SET id_estado_pedido=?, fecha_estado=NOW() WHERE id_pedido=?")){
            up.setInt(1, nuevo);
            up.setInt(2, idPedido);
            up.executeUpdate();
        }
    }

    private void actualizarInventario(Connection cn, int idSucursal, int idProducto, int cant) throws Exception {
        // Busca registro inventario; si existe suma, si no crea
        try (PreparedStatement sel = cn.prepareStatement(
                "SELECT id_inventario, stock_actual FROM inventario WHERE id_sucursal=? AND id_producto=? LIMIT 1")){
            sel.setInt(1, idSucursal);
            sel.setInt(2, idProducto);
            try (ResultSet rs = sel.executeQuery()){
                if (rs.next()){
                    int idInv = rs.getInt("id_inventario");
                    int nuevoStock = rs.getInt("stock_actual") + cant;
                    try (PreparedStatement upd = cn.prepareStatement(
                            "UPDATE inventario SET stock_actual=?, actualizado_en=NOW() WHERE id_inventario=?")){
                        upd.setInt(1, nuevoStock);
                        upd.setInt(2, idInv);
                        upd.executeUpdate();
                    }
                } else {
                    int stockMin = 0; String ubic = "";
                    try (PreparedStatement ins = cn.prepareStatement(
                            "INSERT INTO inventario (id_sucursal,id_producto,stock_actual,stock_minimo,ubicacion,actualizado_en) " +
                                    "VALUES (?,?,?,?,?,NOW())")){
                        ins.setInt(1, idSucursal);
                        ins.setInt(2, idProducto);
                        ins.setInt(3, cant);
                        ins.setInt(4, stockMin);
                        ins.setString(5, ubic);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    public boolean huboCambios(){ return cambios; }

    /* ====== Query helpers ====== */

    private Pedido getPedido(Connection cn, int id) throws Exception {
        String sql = """
            SELECT p.*, pr.nombre AS proveedor, s.nombre AS sucursal, u.nombre AS usuario,
                   e.nombre_estado
              FROM pedido p
              JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
              JOIN sucursal  s  ON s.id_sucursal=p.id_sucursal
              LEFT JOIN usuario  u ON u.id_usuario=p.id_usuario
              LEFT JOIN estado_pedido e ON e.id_estado_pedido=p.id_estado_pedido
             WHERE p.id_pedido=?
        """;
        try (PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()) return null;
                Pedido p = new Pedido();
                p.idPedido = id;
                p.idEstado = rs.getInt("id_estado_pedido");
                p.idSucursal = rs.getInt("id_sucursal");
                p.proveedor = rs.getString("proveedor");
                p.sucursal  = rs.getString("sucursal");
                p.usuario   = rs.getString("usuario");
                p.nombreEstado = rs.getString("nombre_estado");
                p.fechaCreado  = rs.getString("fecha_creado");
                p.fechaEstado  = rs.getString("fecha_estado");
                p.observacion  = rs.getString("observacion");
                return p;
            }
        }
    }

    private List<ItemRow> getDetalle(Connection cn, int id) throws Exception {
        List<ItemRow> out = new ArrayList<>();
        String sql = """
            SELECT d.id_producto, pr.nombre AS producto, d.cantidad_solicitada, d.precio_unitario
              FROM pedido_detalle d
              JOIN producto pr ON pr.id_producto=d.id_producto
             WHERE d.id_pedido=?
        """;
        try (PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    out.add(new ItemRow(
                            rs.getInt("id_producto"),
                            rs.getString("producto"),
                            rs.getInt("cantidad_solicitada"),
                            rs.getDouble("precio_unitario")
                    ));
                }
            }
        }
        return out;
    }

    private String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }

    /* ====== tipos ====== */

    static class Pedido {
        int idPedido, idEstado, idSucursal;
        String proveedor, sucursal, usuario, nombreEstado, fechaCreado, fechaEstado, observacion;
    }
    static class ItemRow {
        final int idProducto; final String producto; final int cantidad; final double precio;
        ItemRow(int idProducto, String producto, int cantidad, double precio){
            this.idProducto=idProducto; this.producto=producto; this.cantidad=cantidad; this.precio=precio;
        }
    }

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
