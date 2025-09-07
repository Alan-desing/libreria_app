package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class minimos extends JDialog {

    private JTextField txtBuscar;
    private JComboBox<Item> cbCategoria;
    private JButton btnFiltrar, btnGuardar, btnCerrar;
    private JTable tabla;
    private DefaultTableModel model;

    private boolean cambios=false;

    public minimos(Window owner) {
        super(owner, "Editar mínimos (lote)", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(920, 620);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                new EmptyBorder(16,16,16,16)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx=1;

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);

        txtBuscar = new JTextField();
        txtBuscar.setPreferredSize(new Dimension(260, 36));
        filtros.add(txtBuscar);

        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(250, 36));
        filtros.add(cbCategoria);

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        filtros.add(btnFiltrar);

        gc.gridx=0; gc.gridy=0;
        card.add(filtros, gc);

        // Tabla
        String[] cols = {"ID","Producto","Categoría","Stock","Mínimo"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c) { return c==4; } // mínimo editable
            @Override public Class<?> getColumnClass(int columnIndex) {
                return Object.class;
            }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(320);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true), new EmptyBorder(6,6,6,6)));
        gc.gridy=1; gc.weighty=1; gc.fill = GridBagConstraints.BOTH;
        card.add(sp, gc);

        // Acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnGuardar = estilos.botonBlanco("GUARDAR MÍNIMOS");
        btnCerrar  = estilos.botonSm("Cerrar");
        actions.add(btnGuardar);
        actions.add(btnCerrar);

        gc.gridy=2; gc.weighty=0; gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCerrar.addActionListener(e -> dispose());
        btnFiltrar.addActionListener(e -> cargar());
        btnGuardar.addActionListener(e -> guardar());

        cargarCategorias();
        cargar();
    }

    public boolean huboCambios(){ return cambios; }

    private void cargarCategorias(){
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre");
             ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando categorías:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargar(){
        String q = txtBuscar.getText()==null? "" : txtBuscar.getText().trim();
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat==null)?0:cat.id();

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (!q.isEmpty()){
            where.append(where.length()==0? " WHERE " : " AND ");
            where.append("(p.nombre LIKE ? OR p.id_producto = ?)");
            params.add("%"+q+"%");
            try { params.add(Integer.parseInt(q)); } catch (Exception ex){ params.add(0); }
        }
        if (idCat>0){
            where.append(where.length()==0? " WHERE " : " AND ");
            where.append("c.id_categoria = ?");
            params.add(idCat);
        }

        String sql = """
            SELECT p.id_producto, p.nombre, c.nombre,
                   COALESCE(i.stock_actual,0) AS stock_actual,
                   COALESCE(i.stock_minimo,0) AS stock_minimo
            FROM producto p
            LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
            LEFT JOIN categoria c ON c.id_categoria=sc.id_categoria
            LEFT JOIN inventario i ON i.id_producto=p.id_producto
        """ + where + " ORDER BY c.nombre, p.nombre";

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
                            String.valueOf(rs.getInt(1)),
                            rs.getString(2),
                            rs.getString(3)==null? "—" : rs.getString(3),
                            rs.getInt(4),
                            rs.getInt(5)
                    });
                }
            }

            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","Sin resultados.","","",""});
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error listando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar(){
        // recorre la tabla y actualiza mínimos válidos
        try (Connection cn = DB.get()){
            cn.setAutoCommit(false);
            try {
                for (int r=0; r<model.getRowCount(); r++){
                    String idS = String.valueOf(model.getValueAt(r,0)).replace("#","");
                    int idp;
                    try { idp = Integer.parseInt(idS); } catch (Exception ignore){ continue; }

                    Object valMin = model.getValueAt(r,4);
                    int nm;
                    try { nm = Integer.parseInt(String.valueOf(valMin)); }
                    catch (Exception ignore){ continue; }
                    if (nm<0) continue;

                    // asegurar inventario
                    try (PreparedStatement ps = cn.prepareStatement("""
                        INSERT IGNORE INTO inventario(id_producto, stock_actual, stock_minimo)
                        VALUES (?,0,0)
                    """)){
                        ps.setInt(1, idp);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = cn.prepareStatement("""
                        UPDATE inventario SET stock_minimo=? WHERE id_producto=?
                    """)){
                        ps.setInt(1, nm);
                        ps.setInt(2, idp);
                        ps.executeUpdate();
                    }
                }
                cn.commit();
                cambios = true;
                JOptionPane.showMessageDialog(this, "Mínimos actualizados.");
            } catch (Exception ex){
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al guardar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
        int id(){ return id; }
    }

    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
