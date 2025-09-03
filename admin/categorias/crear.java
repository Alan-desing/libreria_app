package admin.categorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class crear extends JDialog {

    private JTextField txtNombre;
    private boolean guardado = false;

    public crear(Window owner) {
        super(owner, "Nueva categoría", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 260);
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
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

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

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnCrear  = estilos.botonBlanco("CREAR");
        actions.add(btnCancel);
        actions.add(btnCrear);

        gc.gridy=2; gc.gridwidth=2;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCancel.addActionListener(e -> dispose());
        btnCrear.addActionListener(e -> onGuardar());
    }

    public boolean fueGuardado(){ return guardado; }

    private void onGuardar(){
        String nombre = (txtNombre.getText()==null?"":txtNombre.getText().trim());

        // Validaciones (como en la web)
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
            // unicidad
            try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM categoria WHERE nombre=? LIMIT 1")){
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe una categoría con ese nombre.");
                        return;
                    }
                }
            }
            // insert
            try (PreparedStatement ps = cn.prepareStatement("INSERT INTO categoria(nombre) VALUES (?)")){
                ps.setString(1, nombre);
                ps.executeUpdate();
            }

            guardado = true;
            JOptionPane.showMessageDialog(this, "Categoría creada correctamente.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al crear:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
