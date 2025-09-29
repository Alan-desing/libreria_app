package admin.sucursales;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class transferir extends JDialog {
    private boolean ok=false;

    private JComboBox<Item> cbOrigen, cbDestino;
    private JTextField tfObs;
    private JPanel rowsWrap;
    private JButton btnAgregar, btnConfirmar, btnCancelar;

    static class Item {
        int id; String nombre;
        Item(int i, String n){ id=i; nombre=n; }
        @Override public String toString(){ return nombre; }
    }

    static class Row {
        JComboBox<Item> cbProd;
        JSpinner spCant;
        JPanel panel;
    }
    private final List<Row> filas = new ArrayList<>();

    public transferir(Window owner, int idOrigenSel){
        super(owner, "Transferir productos entre sucursales", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(estilos.COLOR_FONDO);

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220),1,true),
                new EmptyBorder(16,16,16,16) // <-- FIX: 4 parámetros
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Encabezado
        JLabel h = new JLabel("Datos de la transferencia");
        h.setFont(new Font("Arial", Font.BOLD, 16));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(h); card.add(Box.createVerticalStrut(6));

        // Combos
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets=new Insets(6,6,6,6);
        g.fill=GridBagConstraints.HORIZONTAL;
        g.gridy=0; g.gridx=0; g.weightx=0.5;

        cbOrigen  = new JComboBox<>(); estilizarCombo(cbOrigen);
        cbDestino = new JComboBox<>(); estilizarCombo(cbDestino);
        tfObs = new JTextField(); includes.estilos.estilizarCampo(tfObs);
        tfObs.setPreferredSize(new Dimension(520,38));

        addLabeled(grid,g,"Origen",cbOrigen); g.gridx=1; addLabeled(grid,g,"Destino",cbDestino);
        g.gridx=0; g.gridy=1; g.gridwidth=2; addLabeled(grid,g,"Observación (opcional)", tfObs);

        card.add(grid);
        card.add(Box.createVerticalStrut(10));

        JLabel h2 = new JLabel("Productos");
        h2.setFont(new Font("Arial", Font.BOLD, 16));
        h2.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(h2); card.add(Box.createVerticalStrut(6));

        rowsWrap = new JPanel();
        rowsWrap.setOpaque(false);
        rowsWrap.setLayout(new BoxLayout(rowsWrap, BoxLayout.Y_AXIS));
        JScrollPane scRows = new JScrollPane(rowsWrap,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scRows.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        scRows.setPreferredSize(new Dimension(0, 260));
        card.add(scRows);

        btnAgregar = estilos.botonBlanco("+ Agregar producto");
        btnAgregar.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(6));
        card.add(btnAgregar);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        btnConfirmar = estilos.botonRedondeado("Confirmar transferencia");
        btnCancelar  = estilos.botonBlanco("Cancelar");
        btnConfirmar.setPreferredSize(new Dimension(240,38));
        btnCancelar.setPreferredSize(new Dimension(140,38));
        actions.add(btnConfirmar); actions.add(btnCancelar);

        root.add(card, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);

        setMinimumSize(new Dimension(900, 620));
        pack();
        setLocationRelativeTo(owner);

        // Data
        cargarSucursales();
        if (idOrigenSel>0) selectById(cbOrigen, idOrigenSel);
        cargarProductos(); // para prototipos de filas
        addRow(); // primera fila

        // Events
        btnAgregar.addActionListener(e -> addRow());
        btnCancelar.addActionListener(e -> dispose());
        btnConfirmar.addActionListener(e -> onConfirm());
        getRootPane().setDefaultButton(btnConfirmar);
    }

    private void estilizarCombo(JComboBox<?> cb){ estilos.estilizarCombo(cb); cb.setPreferredSize(new Dimension(320,38)); }

    private void addLabeled(JPanel grid, GridBagConstraints g, String label, JComponent comp){
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setForeground(new Color(60,60,60));
        lb.setBorder(new EmptyBorder(0,2,4,2));
        row.add(lb, BorderLayout.NORTH); row.add(comp, BorderLayout.CENTER);
        grid.add(row, g);
    }

    private void addRow(){
        Row r = new Row();
        r.cbProd = new JComboBox<>();
        estilizarCombo(r.cbProd);
        fillProductos(r.cbProd);
        r.spCant = new JSpinner(new SpinnerNumberModel(1,1,999999,1));
        ((JSpinner.DefaultEditor) r.spCant.getEditor()).getTextField().setColumns(8);

        JPanel line = new JPanel(new GridBagLayout());
        line.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets=new Insets(4,4,4,4);
        g.gridy=0; g.fill=GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.weightx=1; line.add(r.cbProd, g);
        g.gridx=1; g.weightx=0; line.add(r.spCant, g);

        JButton btnX = estilos.botonSmDanger("Quitar");
        btnX.addActionListener(e -> { rowsWrap.remove(line); filas.remove(r); rowsWrap.revalidate(); rowsWrap.repaint(); });
        g.gridx=2; line.add(btnX, g);

        r.panel = line;
        filas.add(r);
        rowsWrap.add(line);
        rowsWrap.revalidate(); rowsWrap.repaint();
    }

    /* ===== Data load ===== */
    private final List<Item> sucursales = new ArrayList<>();
    private final List<Item> productos  = new ArrayList<>();

    private void cargarSucursales(){
        sucursales.clear();
        try (Connection cn = conexion_bd.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_sucursal, nombre FROM sucursal ORDER BY nombre")){
            while (rs.next()) sucursales.add(new Item(rs.getInt(1), rs.getString(2)));
        } catch (Exception ex){ JOptionPane.showMessageDialog(this,"Error cargando sucursales:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE); }
        cbOrigen.removeAllItems(); cbDestino.removeAllItems();
        for (Item it: sucursales){ cbOrigen.addItem(it); cbDestino.addItem(new Item(it.id, it.nombre)); }
    }
    private void cargarProductos(){
        productos.clear();
        try (Connection cn = conexion_bd.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_producto, nombre FROM producto WHERE activo=1 ORDER BY nombre")){
            while (rs.next()) productos.add(new Item(rs.getInt(1), rs.getString(2)));
        } catch (Exception ex){ JOptionPane.showMessageDialog(this,"Error cargando productos:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE); }
    }
    private void fillProductos(JComboBox<Item> cb){
        cb.removeAllItems();
        for (Item it: productos) cb.addItem(new Item(it.id, it.nombre));
    }
    private void selectById(JComboBox<Item> cb, int id){
        for (int i=0;i<cb.getItemCount();i++){ if (cb.getItemAt(i).id==id){ cb.setSelectedIndex(i); return; } }
    }

    /* ===== Confirmar ===== */
    private void onConfirm(){
        Item origen = (Item) cbOrigen.getSelectedItem();
        Item destino= (Item) cbDestino.getSelectedItem();
        if (origen==null || destino==null || origen.id<=0 || destino.id<=0 || origen.id==destino.id){
            JOptionPane.showMessageDialog(this, "Seleccioná origen y destino distintos.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (filas.isEmpty()){
            JOptionPane.showMessageDialog(this, "Agregá al menos un producto.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Recolectar items
        class P { int id; int cant; }
        List<P> items = new ArrayList<>();
        for (Row r: filas){
            Item p = (Item) r.cbProd.getSelectedItem();
            int cant = (int) r.spCant.getValue();
            if (p==null || p.id<=0 || cant<=0){
                JOptionPane.showMessageDialog(this, "Completá todas las filas correctamente.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            P o = new P(); o.id=p.id; o.cant=cant; items.add(o);
        }

        String obs = tfObs.getText().trim();

        // Transacción
        try (Connection cn = conexion_bd.getConnection()){
            cn.setAutoCommit(false);

            int idTipoMov = getIdTipoMov(cn, "Transferencia"); // fallback si no existe
            int idUsuario = 0; // si manejás usuario en escritorio, podés setearlo

            int idMov;
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO movimiento (id_tipo_movimiento,id_usuario,id_sucursal_origen,id_sucursal_destino,fecha_hora,observacion) VALUES (?,?,?,?,NOW(),?)",
                    Statement.RETURN_GENERATED_KEYS)){
                ps.setInt(1, idTipoMov);
                ps.setInt(2, idUsuario);
                ps.setInt(3, origen.id);
                ps.setInt(4, destino.id);
                ps.setString(5, obs);
                ps.executeUpdate();
                try (ResultSet gk = ps.getGeneratedKeys()){
                    if (!gk.next()) throw new SQLException("No se obtuvo ID de movimiento.");
                    idMov = gk.getInt(1);
                }
            }

            PreparedStatement selInv = cn.prepareStatement("SELECT id_inventario, stock_actual FROM inventario WHERE id_sucursal=? AND id_producto=? LIMIT 1");
            PreparedStatement insInv = cn.prepareStatement("INSERT INTO inventario (id_sucursal,id_producto,stock_actual,stock_minimo,ubicacion,actualizado_en) VALUES (?,?,?,?,?,NOW())");
            PreparedStatement updInv = cn.prepareStatement("UPDATE inventario SET stock_actual=?, actualizado_en=NOW() WHERE id_inventario=?");
            PreparedStatement insDet = cn.prepareStatement("INSERT INTO movimiento_detalle (id_movimiento,id_producto,cantidad,precio_unitario) VALUES (?,?,?,0)");

            for (P it: items){
                // Debitar ORIGEN
                selInv.setInt(1, origen.id); selInv.setInt(2, it.id);
                ResultSet r = selInv.executeQuery();
                if (!r.next() || r.getInt("stock_actual") < it.cant){
                    throw new SQLException("Stock insuficiente en origen para producto #"+it.id);
                }
                int idInvOrigen = r.getInt("id_inventario");
                int nuevoOrigen = r.getInt("stock_actual") - it.cant;
                updInv.setInt(1, nuevoOrigen); updInv.setInt(2, idInvOrigen);
                updInv.executeUpdate();

                // Acreditar DESTINO
                selInv.setInt(1, destino.id); selInv.setInt(2, it.id);
                ResultSet r2 = selInv.executeQuery();
                if (!r2.next()){
                    insInv.setInt(1, destino.id);
                    insInv.setInt(2, it.id);
                    insInv.setInt(3, it.cant);
                    insInv.setInt(4, 0); // minimo
                    insInv.setString(5, ""); // ubicacion
                    insInv.executeUpdate();
                } else {
                    int idInvDest = r2.getInt("id_inventario");
                    int nuevoDest = r2.getInt("stock_actual") + it.cant;
                    updInv.setInt(1, nuevoDest); updInv.setInt(2, idInvDest);
                    updInv.executeUpdate();
                }

                // Detalle
                insDet.setInt(1, idMov);
                insDet.setInt(2, it.id);
                insDet.setInt(3, it.cant);
                insDet.executeUpdate();
            }

            cn.commit();
            ok=true;
            JOptionPane.showMessageDialog(this, "Transferencia registrada (#"+ ok +").", "OK", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception ex){
            try { conexion_bd.getConnection().rollback(); } catch(Exception ignore){}
            JOptionPane.showMessageDialog(this, "No se pudo completar la transferencia:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getIdTipoMov(Connection cn, String nombre) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_tipo_movimiento FROM tipo_movimiento WHERE LOWER(nombre_tipo)=LOWER(?) LIMIT 1")){
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) return rs.getInt(1);
            }
        }
        // fallback: 2 suele ser Transferencia en varios esquemas; si no existe, creamos uno básico
        try (PreparedStatement ins = cn.prepareStatement(
                "INSERT INTO tipo_movimiento (nombre_tipo) VALUES (?)", Statement.RETURN_GENERATED_KEYS)){
            ins.setString(1, nombre);
            ins.executeUpdate();
            try (ResultSet gk = ins.getGeneratedKeys()){ if (gk.next()) return gk.getInt(1); }
        }
        return 2;
    }

    public boolean fueOk(){ return ok; }
}
