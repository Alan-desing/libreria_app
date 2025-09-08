package admin.usuarios;

import includes.estilos;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public class editar extends JDialog {

    // Lógica: ID del usuario a editar
    private final int idUsuario;

    // Visual: campos del formulario (nombre, email, rol, estado, contraseñas opcionales)
    private JTextField txtNombre, txtEmail;
    private JComboBox<Item> cbRol, cbEstado;
    private JPasswordField txtPass, txtPass2;

    // Lógica: bandera para informar al panel si se actualizó
    private boolean actualizado = false;

    // Visual + Lógica: constructor (arma UI, carga combos y datos)
    public editar(Window owner, int idUsuario) {
        super(owner, "Editar usuario", ModalityType.APPLICATION_MODAL);
        this.idUsuario = idUsuario;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        // Visual: card blanca con borde suave
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(18,18,18,18)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        // Visual: campo Nombre
        JLabel lbNom = new JLabel("Nombre");
        lbNom.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(lbNom, gc);

        txtNombre = new JTextField();
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        txtNombre.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=1;
        card.add(txtNombre, gc);

        // Visual: campo Email
        JLabel lbEmail = new JLabel("Email");
        lbEmail.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=2;
        card.add(lbEmail, gc);

        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        txtEmail.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=3;
        card.add(txtEmail, gc);

        // Visual: fila Rol/Estado
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0,0,0,8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0; g.weightx=1;

        JPanel col1 = new JPanel(new BorderLayout(0,4));
        col1.setOpaque(false);
        JLabel lbRol = new JLabel("Rol");
        lbRol.setFont(new Font("Arial", Font.BOLD, 14));
        col1.add(lbRol, BorderLayout.NORTH);
        cbRol = new JComboBox<>();
        estilos.estilizarCombo(cbRol);
        col1.add(cbRol, BorderLayout.CENTER);
        fila.add(col1, g);

        JPanel col2 = new JPanel(new BorderLayout(0,4));
        col2.setOpaque(false);
        JLabel lbEst = new JLabel("Estado");
        lbEst.setFont(new Font("Arial", Font.BOLD, 14));
        col2.add(lbEst, BorderLayout.NORTH);
        cbEstado = new JComboBox<>();
        estilos.estilizarCombo(cbEstado);
        col2.add(cbEstado, BorderLayout.CENTER);
        g.gridx=1; g.weightx=1;
        fila.add(col2, g);

        gc.gridy=4; gc.gridx=0; gc.gridwidth=2;
        card.add(fila, gc);

        // Visual: sección de cambio de contraseña (opcional)
        JLabel sec = new JLabel("Cambiar contraseña (opcional)");
        sec.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=5;
        card.add(sec, gc);

        JLabel lbPass = new JLabel("Nueva contraseña");
        lbPass.setFont(new Font("Arial", Font.PLAIN, 13));
        gc.gridy=6;
        card.add(lbPass, gc);

        txtPass = new JPasswordField();
        txtPass.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPass.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=7;
        card.add(txtPass, gc);

        JLabel lbPass2 = new JLabel("Repetir contraseña");
        lbPass2.setFont(new Font("Arial", Font.PLAIN, 13));
        gc.gridy=8;
        card.add(lbPass2, gc);

        txtPass2 = new JPasswordField();
        txtPass2.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPass2.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=9;
        card.add(txtPass2, gc);

        // Visual: acciones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnGuardar = estilos.botonBlanco("GUARDAR");
        actions.add(btnCancel);
        actions.add(btnGuardar);

        gc.gridy=10; gc.gridwidth=2;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        // Lógica: eventos
        btnCancel.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onGuardar());

        // Lógica/BD: combos + datos actuales del usuario
        cargarCombos();
        cargarUsuario();
    }

    // Lógica: informa al panel si actualizó correctamente
    public boolean fueActualizado(){ return actualizado; }

    // Lógica/BD: carga combos de rol y estado
    private void cargarCombos(){
        cbRol.removeAllItems();
        cbEstado.removeAllItems();
        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_rol, nombre_rol FROM rol ORDER BY nombre_rol");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) cbRol.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_estado_usuario, nombre_estado FROM estado_usuario ORDER BY nombre_estado");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) cbEstado.addItem(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica/BD: trae datos del usuario a editar y selecciona sus combos
    private void cargarUsuario(){
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_usuario, nombre, email, id_rol, id_estado_usuario " +
                     "FROM usuario WHERE id_usuario=?")){
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){
                    JOptionPane.showMessageDialog(this, "Usuario no encontrado");
                    dispose();
                    return;
                }
                txtNombre.setText(rs.getString("nombre"));
                txtEmail.setText(rs.getString("email"));
                int idRol = rs.getInt("id_rol");
                int idEst = rs.getInt("id_estado_usuario");

                selectById(cbRol, idRol);
                selectById(cbEstado, idEst);
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando usuario:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: helper para seleccionar un ítem de combo por ID
    private static void selectById(JComboBox<Item> combo, int id){
        for (int i=0;i<combo.getItemCount();i++){
            Item it = combo.getItemAt(i);
            if (it.id==id){ combo.setSelectedIndex(i); return; }
        }
    }

    // Lógica/BD: valida, evita duplicados de email, actualiza datos y contraseña opcional
    private void onGuardar(){
        String nombre = (txtNombre.getText()==null?"":txtNombre.getText().trim());
        String email  = (txtEmail.getText()==null?"":txtEmail.getText().trim());
        String pass   = new String(txtPass.getPassword());
        String pass2  = new String(txtPass2.getPassword());

        Item rol = (Item) cbRol.getSelectedItem();
        Item est = (Item) cbEstado.getSelectedItem();

        // Lógica: validaciones
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }
        if (!isEmail(email)){
            JOptionPane.showMessageDialog(this, "Email inválido.");
            return;
        }
        if (rol==null || rol.id<=0){
            JOptionPane.showMessageDialog(this, "Seleccioná un rol.");
            return;
        }
        if (est==null || est.id<=0){
            JOptionPane.showMessageDialog(this, "Seleccioná un estado.");
            return;
        }
        if (!pass.isEmpty()){
            if (pass.length()<6){
                JOptionPane.showMessageDialog(this, "La nueva contraseña debe tener al menos 6 caracteres.");
                return;
            }
            if (!pass.equals(pass2)){
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
                return;
            }
        }

        try (Connection cn = DB.get()){
            // Lógica/BD: validar email duplicado en otro usuario
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT 1 FROM usuario WHERE email=? AND id_usuario<>? LIMIT 1")){
                ps.setString(1, email);
                ps.setInt(2, idUsuario);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe otro usuario con ese email.");
                        return;
                    }
                }
            }

            // Lógica/BD: transacción para actualizar datos y (opcional) contraseña
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE usuario SET nombre=?, email=?, id_rol=?, id_estado_usuario=?, actualizado_en=NOW() " +
                        "WHERE id_usuario=?")){
                    ps.setString(1, nombre);
                    ps.setString(2, email);
                    ps.setInt(3, rol.id);
                    ps.setInt(4, est.id);
                    ps.setInt(5, idUsuario);
                    ps.executeUpdate();
                }

                if (!pass.isEmpty()){
                    String hash = BCrypt.hashpw(pass, BCrypt.gensalt(10));
                    if (hash.startsWith("$2a$") || hash.startsWith("$2b$")) {
                        hash = "$2y$" + hash.substring(4);
                    }
                    try (PreparedStatement ps = cn.prepareStatement(
                            "UPDATE usuario SET contrasena=? WHERE id_usuario=?")){
                        ps.setString(1, hash);
                        ps.setInt(2, idUsuario);
                        ps.executeUpdate();
                    }
                }

                cn.commit();
                actualizado = true;
                JOptionPane.showMessageDialog(this, "Usuario actualizado.");
                dispose();
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

    // Lógica: validador básico de email
    private static boolean isEmail(String s){
        if (s==null) return false;
        return Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matcher(s).find();
    }

    // Lógica: item simple para combos (id + etiqueta)
    static class Item{
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }

    // Lógica/BD: helper de conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
