package admin.categorias;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class editar extends JDialog {

    // Lógica: id de la categoría a editar
    private final int id;

    // Visual: campo del nombre
    private JTextField txtNombre;

    // Lógica: flag para avisar si se guardó
    private boolean guardado = false;

    // Visual + Lógica: constructor. Carga datos y arma la UI
    public editar(Window owner, int id) {
        super(owner, "Editar categoría", ModalityType.APPLICATION_MODAL);
        this.id = id;
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

        // Visual: label + input
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

        // Visual: acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnGuardar  = estilos.botonBlanco("GUARDAR CAMBIOS");
        actions.add(btnCancel);
        actions.add(btnGuardar);

        gc.gridy=2; gc.gridwidth=2;
        card.add(actions, gc);

        // Visual: agrego card
        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        // Lógica: listeners
        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());

        // Lógica: carga datos de la categoría
        cargar();
    }

    // Lógica: consulta si guardó para refrescar el panel
    public boolean fueGuardado(){ return guardado; }

    // Lógica + BD: trae el nombre actual y lo muestra
    private void cargar(){
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria WHERE id_categoria=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    txtNombre.setText(rs.getString("nombre"));
                } else {
                    JOptionPane.showMessageDialog(this, "Categoría no encontrada.");
                    dispose();
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando categoría:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // Lógica + BD: valida y actualiza el nombre
    private void onGuardar(){
        String nombre = (txtNombre.getText()==null?"":txtNombre.getText().trim());

        // Lógica: validaciones básicas
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
            // Lógica + BD: unicidad del nombre (excluyendo el mismo ID)
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT 1 FROM categoria WHERE nombre=? AND id_categoria<>? LIMIT 1")){
                ps.setString(1, nombre);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe otra categoría con ese nombre.");
                        return;
                    }
                }
            }
            // BD: update
            try (PreparedStatement ps = cn.prepareStatement("UPDATE categoria SET nombre=? WHERE id_categoria=?")){
                ps.setString(1, nombre);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            // Lógica: éxito → avisamos y cerramos
            guardado = true;
            JOptionPane.showMessageDialog(this, "Categoría actualizada.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al guardar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
