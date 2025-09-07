package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class ajustar extends JDialog {

    private final int idProducto;

    private JComboBox<String> cbTipo;
    private JSpinner spCantidad;
    private JTextField txtMotivo;

    // para que el "mínimo (opcional)" sea realmente opcional
    private JSpinner spNuevoMin;
    private JCheckBox chkAplicarMin;

    private boolean guardado = false;

    public ajustar(Window owner, int idProducto) {
        super(owner, "Ajustar inventario", ModalityType.APPLICATION_MODAL);
        this.idProducto = idProducto;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(580, 540);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                new EmptyBorder(18,18,18,18)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.weightx=1;

        JLabel t1 = new JLabel("ID de producto: #"+idProducto);
        t1.setFont(new Font("Arial", Font.BOLD, 15));
        gc.gridx=0; gc.gridy=0;
        card.add(t1, gc);

        // Tipo
        JLabel lb1 = new JLabel("Tipo de operación");
        lb1.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=1; card.add(lb1, gc);

        cbTipo = new JComboBox<>(new String[]{
                "Seleccionar…","Ingreso (+)","Egreso (−)","Ajuste (fijar exacto)"
        });
        estilos.estilizarCombo(cbTipo);
        gc.gridy=2; card.add(cbTipo, gc);

        // Cantidad
        JLabel lb2 = new JLabel("Cantidad");
        lb2.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=3; card.add(lb2, gc);

        spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
        ((JSpinner.DefaultEditor)spCantidad.getEditor()).getTextField().setColumns(8);
        gc.gridy=4; card.add(spCantidad, gc);

        // Motivo
        JLabel lb3 = new JLabel("Motivo (opcional, máx 200)");
        lb3.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=5; card.add(lb3, gc);

        txtMotivo = new JTextField();
        txtMotivo.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=6; card.add(txtMotivo, gc);

        // Nuevo mínimo (opcional)
        JLabel lb4 = new JLabel("Nuevo mínimo (opcional)");
        lb4.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=7; card.add(lb4, gc);

        spNuevoMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        ((JSpinner.DefaultEditor)spNuevoMin.getEditor()).getTextField().setColumns(8);
        gc.gridy=8; card.add(spNuevoMin, gc);

        chkAplicarMin = new JCheckBox("Aplicar nuevo mínimo");
        chkAplicarMin.setOpaque(false);
        gc.gridy=9; card.add(chkAplicarMin, gc);

        // Acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnGuardar = estilos.botonBlanco("GUARDAR");
        actions.add(btnCancel);
        actions.add(btnGuardar);

        gc.gridy=10; card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());
    }

    public boolean fueGuardado(){ return guardado; }

    private void onGuardar(){
        String tipoSel = (String) cbTipo.getSelectedItem();
        int cant  = (int) spCantidad.getValue();
        String motivo = txtMotivo.getText()==null?"":txtMotivo.getText().trim();
        int nuevoMin = (int) spNuevoMin.getValue();
        boolean aplicarMin = chkAplicarMin.isSelected();
        String tipo="";

        if ("Ingreso (+)".equals(tipoSel)) tipo="ingreso";
        else if ("Egreso (−)".equals(tipoSel)) tipo="egreso";
        else if ("Ajuste (fijar exacto)".equals(tipoSel)) tipo="ajuste";

        if (tipo.isEmpty()){
            JOptionPane.showMessageDialog(this, "Seleccioná el tipo de operación.");
            return;
        }
        if (cant<=0){
            JOptionPane.showMessageDialog(this, "La cantidad debe ser > 0.");
            return;
        }
        if (motivo.length()>200){
            JOptionPane.showMessageDialog(this, "Motivo demasiado largo (máximo 200).");
            return;
        }

        try (Connection cn = DB.get()){

            // Asegurar fila en inventario
            try (Statement s = cn.createStatement()){
                s.executeUpdate(
                        "INSERT IGNORE INTO inventario(id_producto, stock_actual, stock_minimo) " +
                        "VALUES ("+idProducto+", 0, 0)"
                );
            }

            // Bloquear/leer actual
            int prev=0;
            try (PreparedStatement ps = cn.prepareStatement("""
                SELECT stock_actual, stock_minimo
                FROM inventario
                WHERE id_producto=? FOR UPDATE
            """)){
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        prev = rs.getInt(1);
                        // int minActual = rs.getInt(2); // si lo necesitás, lo dejás
                    }
                }
            }

            // Calcular nuevo stock
            int nuevo = prev;
            if ("ingreso".equals(tipo)) nuevo = prev + cant;
            else if ("egreso".equals(tipo)) nuevo = Math.max(0, prev - cant);
            else if ("ajuste".equals(tipo)) nuevo = cant;

            // Actualizar stock
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE inventario SET stock_actual=? WHERE id_producto=?")){
                ps.setInt(1, nuevo);
                ps.setInt(2, idProducto);
                ps.executeUpdate();
            }

            // Actualizar mínimo (opcional)
            if (aplicarMin){
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE inventario SET stock_minimo=? WHERE id_producto=?")){
                    ps.setInt(1, nuevoMin);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }
            }

            // Registrar movimiento
            try (Statement s = cn.createStatement()){
                s.execute("""
                    CREATE TABLE IF NOT EXISTS inventario_mov (
                      id_mov INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                      id_producto INT UNSIGNED NOT NULL,
                      tipo ENUM('ingreso','egreso','ajuste') NOT NULL,
                      cantidad INT NOT NULL,
                      motivo VARCHAR(200) DEFAULT NULL,
                      stock_prev INT NOT NULL,
                      stock_nuevo INT NOT NULL,
                      id_usuario INT UNSIGNED DEFAULT NULL,
                      creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      KEY (id_producto), KEY (id_usuario), KEY (creado_en)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            }

            int uid = 0; // si luego tenés sesión en la app, poné el id real
            try (PreparedStatement ps = cn.prepareStatement("""
                INSERT INTO inventario_mov(id_producto,tipo,cantidad,motivo,stock_prev,stock_nuevo,id_usuario)
                VALUES(?,?,?,?,?,?,?)
            """)){
                ps.setInt(1, idProducto);
                ps.setString(2, tipo);
                ps.setInt(3, cant);
                ps.setString(4, motivo.isEmpty()?null:motivo);
                ps.setInt(5, prev);
                ps.setInt(6, nuevo);
                ps.setInt(7, uid);
                ps.executeUpdate();
            }

            guardado = true;
            JOptionPane.showMessageDialog(this, "Inventario actualizado.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
