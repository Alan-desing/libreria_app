package admin.inventario;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class ajustar extends JDialog {

    // Lógica: ID del producto al que se le ajusta el inventario
    private final int idProducto;

    // Visual: controles principales del formulario (tipo, cantidad, motivo)
    private JComboBox<String> cbTipo;
    private JSpinner spCantidad;
    private JTextField txtMotivo;

    // Visual: controles para mínimo opcional (spinner + check para aplicar o no)
    private JSpinner spNuevoMin;
    private JCheckBox chkAplicarMin;

    // Lógica: bandera para avisar al panel padre si se guardó correctamente
    private boolean guardado = false;

    // Visual + lógica: constructor que arma la UI y engancha los eventos
    public ajustar(Window owner, int idProducto) {
        super(owner, "Ajustar inventario", ModalityType.APPLICATION_MODAL);
        this.idProducto = idProducto;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(580, 540);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        // Visual: card contenedora del formulario (fondo blanco + borde suave)
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                new EmptyBorder(18,18,18,18)
        ));

        // Visual: layout base del formulario (márgenes, alineación y fill)
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.weightx=1;

        // Visual: título auxiliar con el ID del producto
        JLabel t1 = new JLabel("ID de producto: #"+idProducto);
        t1.setFont(new Font("Arial", Font.BOLD, 15));
        gc.gridx=0; gc.gridy=0;
        card.add(t1, gc);

        // Visual: etiqueta “Tipo de operación”
        JLabel lb1 = new JLabel("Tipo de operación");
        lb1.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=1; card.add(lb1, gc);

        // Visual: combo con los tipos (Ingreso/Egreso/Ajuste)
        cbTipo = new JComboBox<>(new String[]{
                "Seleccionar…","Ingreso (+)","Egreso (−)","Ajuste (fijar exacto)"
        });
        estilos.estilizarCombo(cbTipo);
        gc.gridy=2; card.add(cbTipo, gc);

        // Visual: etiqueta “Cantidad”
        JLabel lb2 = new JLabel("Cantidad");
        lb2.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=3; card.add(lb2, gc);

        // Visual: spinner de cantidad (mínimo 1)
        spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999999, 1));
        ((JSpinner.DefaultEditor)spCantidad.getEditor()).getTextField().setColumns(8);
        gc.gridy=4; card.add(spCantidad, gc);

        // Visual: etiqueta “Motivo” (opcional)
        JLabel lb3 = new JLabel("Motivo (opcional, máx 200)");
        lb3.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=5; card.add(lb3, gc);

        // Visual: campo de texto para motivo
        txtMotivo = new JTextField();
        txtMotivo.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=6; card.add(txtMotivo, gc);

        // Visual: etiqueta “Nuevo mínimo” (opcional)
        JLabel lb4 = new JLabel("Nuevo mínimo (opcional)");
        lb4.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=7; card.add(lb4, gc);

        // Visual: spinner para ingresar un nuevo stock mínimo
        spNuevoMin = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
        ((JSpinner.DefaultEditor)spNuevoMin.getEditor()).getTextField().setColumns(8);
        gc.gridy=8; card.add(spNuevoMin, gc);

        // Visual: check para decidir si se aplica el nuevo mínimo o no
        chkAplicarMin = new JCheckBox("Aplicar nuevo mínimo");
        chkAplicarMin.setOpaque(false);
        gc.gridy=9; card.add(chkAplicarMin, gc);

        // Visual: fila de acciones (Cancelar / Guardar)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnGuardar = estilos.botonBlanco("GUARDAR");
        actions.add(btnCancel);
        actions.add(btnGuardar);

        gc.gridy=10; card.add(actions, gc);

        // Visual: agregamos la card al root del diálogo
        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        // Lógica: eventos de botones
        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());
    }

    // Lógica: permite al panel padre saber si se guardó para refrescar datos
    public boolean fueGuardado(){ return guardado; }

    // Lógica + BD: valida el formulario, calcula el nuevo stock, persiste cambios y registra el movimiento
    private void onGuardar(){
        String tipoSel = (String) cbTipo.getSelectedItem();
        int cant  = (int) spCantidad.getValue();
        String motivo = txtMotivo.getText()==null?"":txtMotivo.getText().trim();
        int nuevoMin = (int) spNuevoMin.getValue();
        boolean aplicarMin = chkAplicarMin.isSelected();
        String tipo="";

        // Lógica: traducimos la opción visible a un valor interno consistente
        if ("Ingreso (+)".equals(tipoSel)) tipo="ingreso";
        else if ("Egreso (−)".equals(tipoSel)) tipo="egreso";
        else if ("Ajuste (fijar exacto)".equals(tipoSel)) tipo="ajuste";

        // Lógica: validaciones simples de formulario
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

        // Lógica + BD: ejecutamos el ajuste
        try (Connection cn = DB.get()){

            // BD: asegura que el producto tenga fila en inventario
            try (Statement s = cn.createStatement()){
                s.executeUpdate(
                        "INSERT IGNORE INTO inventario(id_producto, stock_actual, stock_minimo) " +
                        "VALUES ("+idProducto+", 0, 0)"
                );
            }

            // BD: lee y bloquea la fila actual para evitar pisadas (FOR UPDATE)
            // Nota: idealmente manejaríamos transacción completa si sumamos más operaciones encadenadas.
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
                        // int minActual = rs.getInt(2); // si necesitamos conocer el mínimo actual, está acá
                    }
                }
            }

            // Lógica: calculamos el nuevo stock según el tipo
            int nuevo = prev;
            if ("ingreso".equals(tipo)) nuevo = prev + cant;
            else if ("egreso".equals(tipo)) nuevo = Math.max(0, prev - cant);
            else if ("ajuste".equals(tipo)) nuevo = cant;

            // BD: actualiza el stock_actual
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE inventario SET stock_actual=? WHERE id_producto=?")){
                ps.setInt(1, nuevo);
                ps.setInt(2, idProducto);
                ps.executeUpdate();
            }

            // BD: si corresponde, actualiza el stock_minimo
            if (aplicarMin){
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE inventario SET stock_minimo=? WHERE id_producto=?")){
                    ps.setInt(1, nuevoMin);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }
            }

            // BD: garantiza que exista la tabla de movimientos (por si la BD está limpia)
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

            // Lógica: por ahora usamos 0 como id de usuario.
            // hay que revisar esto cuando tengamos sesión/usuario logueado en la app
            int uid = 0;

            // BD: registra el movimiento con los datos de la operación
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

            // Lógica: marcamos éxito y cerramos
            guardado = true;
            JOptionPane.showMessageDialog(this, "Inventario actualizado.");
            dispose();

        } catch (Exception ex){
            // Lógica: informamos error si algo falla en BD
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
