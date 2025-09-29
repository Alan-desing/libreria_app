package admin.categorias;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class crear extends JDialog {

    // Visual: campo para el nombre de la categoría
    private JTextField txtNombre;

    // Lógica: indica al panel si se guardó correctamente
    private boolean guardado = false;

    // Visual + Lógica: constructor. Arma el form y conecta eventos
    public crear(Window owner) {
        super(owner, "Nueva categoría", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 260);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        // Visual: card contenedora
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                new EmptyBorder(18,18,18,18)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        // Visual: label y campo de nombre
        JLabel lb = new JLabel("Nombre");
        lb.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(lb, gc);

        txtNombre = new JTextField();
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        txtNombre.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=1;
        card.add(txtNombre, gc);

        // Visual: acciones (Cancelar / CREAR)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnCrear  = estilos.botonBlanco("CREAR");
        actions.add(btnCancel);
        actions.add(btnCrear);

        gc.gridy=2; gc.gridwidth=2;
        card.add(actions, gc);

        // Visual: agregamos card a la ventana
        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        // Lógica: listeners
        btnCancel.addActionListener(e -> dispose());
        btnCrear.addActionListener(e -> onGuardar());
    }

    // Lógica: avisa al panel padre si se creó con éxito
    public boolean fueGuardado(){ return guardado; }

    // Lógica + BD: validaciones y alta de categoría
    private void onGuardar(){
        String nombre = (txtNombre.getText()==null?"":txtNombre.getText().trim());

        // Lógica: validaciones básicas (igual que en la web)
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }
        if (nombre.length() < 3){
            JOptionPane.showMessageDialog(this, "El nombre debe tener al menos 3 caracteres.");
            return;
        }
        if (nombre.length() > 120){
            JOptionPane.showMessageDialog(this, "Máximo 120 caracteres.");
            return;
        }

        try (Connection cn = DB.get()) {
            // Lógica + BD: validar unicidad del nombre
            try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM categoria WHERE nombre=? LIMIT 1")){
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe una categoría con ese nombre.");
                        return;
                    }
                }
            }
            // BD: insertar categoría
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO categoria(nombre) VALUES (?)")){
                ps.setString(1, nombre);
                ps.executeUpdate();
            }

            // Lógica: éxito → avisamos y cerramos
            guardado = true;
            JOptionPane.showMessageDialog(this, "Categoría creada correctamente.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al crear:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // BD: helper local de conexión
        static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }

}
