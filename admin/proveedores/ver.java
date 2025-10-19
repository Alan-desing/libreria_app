package admin.proveedores;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ver extends JDialog {
    // lógica: id del proveedor actual
    private final int id;
    // visual: etiquetas informativas y resumen
    private JLabel lbNombre, lbContacto, lbEmail, lbTel, lbDir, lbPedidos, lbTotal;
    // visual: tabla de últimos pedidos
    private JTable tabla;
    private DefaultTableModel model;

    public ver(Window owner, int idProveedor){
        super(owner, "Proveedor", ModalityType.APPLICATION_MODAL);
        this.id=idProveedor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // visual: contenedor raíz
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(estilos.COLOR_FONDO);
        root.setBorder(new EmptyBorder(12,12,12,12));

        // visual: layout vertical principal
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // visual: tarjetas superiores (datos + resumen)
        JPanel top = new JPanel(new GridLayout(1,2,12,12));
        top.setOpaque(false);

        // visual: card de datos del proveedor
        JPanel c1 = card();
        c1.setLayout(new BoxLayout(c1, BoxLayout.Y_AXIS));
        c1.add(title("Datos de contacto"));
        lbNombre   = info(c1,"Nombre:");
        lbContacto = info(c1,"Contacto:");
        lbEmail    = info(c1,"Email:");
        lbTel      = info(c1,"Teléfono:");
        lbDir      = info(c1,"Dirección:");

        // lógica: botón para abrir ventana de edición
        JButton btnEditar = estilos.botonBlanco("Editar");
        btnEditar.addActionListener(e -> {
            editar dlg = new editar(owner, id);
            dlg.setVisible(true);
            cargar();
            cargarUltimos();
        });
        c1.add(Box.createVerticalStrut(6));
        c1.add(btnEditar);

        // visual: card con resumen de compras
        JPanel c2 = card();
        c2.setLayout(new BoxLayout(c2, BoxLayout.Y_AXIS));
        c2.add(title("Resumen de compras"));
        lbPedidos = info(c2,"Pedidos:");
        lbTotal   = info(c2,"Total comprado:");

        top.add(c1);
        top.add(c2);

        // visual: card con tabla de últimos pedidos
        JPanel tableCard = card();
        tableCard.setLayout(new BorderLayout());
        JLabel th = new JLabel("Últimos pedidos");
        th.setFont(new Font("Arial", Font.BOLD, 16));
        th.setBorder(new EmptyBorder(6,6,6,6));
        tableCard.add(th, BorderLayout.NORTH);

        // visual: tabla de pedidos recientes
        model = new DefaultTableModel(new String[]{"ID","Fecha","Monto"}, 0){
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(30);
        JScrollPane scTable = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scTable.setBorder(BorderFactory.createEmptyBorder(0,6,6,6));
        scTable.setPreferredSize(new Dimension(0, 260));
        tableCard.add(scTable, BorderLayout.CENTER);

        // visual: ensamblado general
        content.add(top);
        content.add(Box.createVerticalStrut(12));
        content.add(tableCard);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // visual: propiedades generales de la ventana
        setMinimumSize(new Dimension(920, 620));
        pack();
        setLocationRelativeTo(owner);

        // BD: carga inicial de datos
        cargar();
        cargarUltimos();
    }

    // visual: estilo base para tarjetas
    private JPanel card(){
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220),1,true),
                new EmptyBorder(12,12,12,12)
        ));
        return p;
    }

    // visual: estilo para títulos dentro de cards
    private JLabel title(String t){
        JLabel l=new JLabel(t);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        l.setBorder(new EmptyBorder(0,0,8,0));
        return l;
    }

    // visual: agrega una etiqueta con texto informativo
    private JLabel info(JPanel parent, String label){
        JLabel l = new JLabel(label+" —");
        l.setFont(new Font("Arial", Font.PLAIN, 15));
        l.setBorder(new EmptyBorder(2,0,2,0));
        parent.add(l);
        return l;
    }

    // BD: carga los datos del proveedor y su resumen general
    private void cargar(){
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM proveedor WHERE id_proveedor=?")){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    lbNombre.setText("Nombre: "+nn(rs.getString("nombre")));
                    lbContacto.setText("Contacto: "+nn(rs.getString("contacto_referencia")));
                    lbEmail.setText("Email: "+nn(rs.getString("email")));
                    lbTel.setText("Teléfono: "+nn(rs.getString("telefono")));
                    lbDir.setText("Dirección: "+nn(rs.getString("direccion")));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando proveedor:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }

        // BD: consulta de totales y cantidad de pedidos
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("""
                     SELECT COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0) total,
                            COUNT(DISTINCT p.id_pedido) pedidos
                     FROM pedido p
                     JOIN pedido_detalle pd ON pd.id_pedido=p.id_pedido
                     WHERE p.id_proveedor=?""")){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    lbPedidos.setText("Pedidos: "+rs.getInt("pedidos"));
                    double tot=rs.getDouble("total");
                    lbTotal.setText("Total comprado: $ "+nf2(tot));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando totales:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // BD: carga los últimos pedidos del proveedor
    private void cargarUltimos(){
        model.setRowCount(0);
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("""
                     SELECT p.id_pedido, p.fecha_creado,
                            COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0) monto
                     FROM pedido p
                     LEFT JOIN pedido_detalle pd ON pd.id_pedido=p.id_pedido
                     WHERE p.id_proveedor=?
                     GROUP BY p.id_pedido, p.fecha_creado
                     ORDER BY p.fecha_creado DESC
                     LIMIT 20""")){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getInt("id_pedido"),
                            rs.getString("fecha_creado"),
                            "$ "+nf2(rs.getDouble("monto"))
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando pedidos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // helpers: texto vacío o nulo → guion
    private String nn(String s){ return (s==null||s.isBlank())?"—":s; }

    // helpers: formato de número con coma decimal
    private String nf2(double n){
        String s=String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }
}
