package admin.usuarios;

import includes.estilos;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public class crear extends JDialog {

    private JTextField txtNombre, txtEmail;
    private JComboBox<Item> cbRol, cbEstado;
    private JPasswordField txtPass, txtPass2;

    private boolean guardado = false;

    public crear(Window owner) {
        super(owner, "Nuevo usuario", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 460);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

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

        // Nombre
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

        // Email
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

        // Fila Rol/Estado
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

        // Contraseñas
        JLabel lbPass = new JLabel("Contraseña");
        lbPass.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=5;
        card.add(lbPass, gc);

        txtPass = new JPasswordField();
        txtPass.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPass.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=6;
        card.add(txtPass, gc);

        JLabel lbPass2 = new JLabel("Repetir contraseña");
        lbPass2.setFont(new Font("Arial", Font.BOLD, 14));
        gc.gridy=7;
        card.add(lbPass2, gc);

        txtPass2 = new JPasswordField();
        txtPass2.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPass2.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        gc.gridy=8;
        card.add(txtPass2, gc);

        // Acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        JButton btnCrear  = estilos.botonBlanco("CREAR");
        actions.add(btnCancel);
        actions.add(btnCrear);

        gc.gridy=9; gc.gridwidth=2;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCancel.addActionListener(e -> dispose());
        btnCrear.addActionListener(e -> onGuardar());

        // Cargar combos
        cargarCombos();
    }

    public boolean fueGuardado(){ return guardado; }

    private void cargarCombos(){
        cbRol.removeAllItems();
        cbEstado.removeAllItems();
        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_rol, nombre_rol FROM rol ORDER BY nombre_rol");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    cbRol.addItem(new Item(rs.getInt("id_rol"), rs.getString("nombre_rol")));
                }
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_estado_usuario, nombre_estado FROM estado_usuario ORDER BY nombre_estado");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    cbEstado.addItem(new Item(rs.getInt("id_estado_usuario"), rs.getString("nombre_estado")));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGuardar(){
        String nombre = (txtNombre.getText()==null?"":txtNombre.getText().trim());
        String email  = (txtEmail.getText()==null?"":txtEmail.getText().trim());
        String pass   = new String(txtPass.getPassword());
        String pass2  = new String(txtPass2.getPassword());

        Item rol = (Item) cbRol.getSelectedItem();
        Item est = (Item) cbEstado.getSelectedItem();

        // Validaciones (idénticas a la web)
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
        if (pass.length()<6){
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!pass.equals(pass2)){
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
            return;
        }

        try (Connection cn = DB.get()){
            // unicidad email
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT 1 FROM usuario WHERE email=? LIMIT 1")){
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe un usuario con ese email.");
                        return;
                    }
                }
            }

            // Hash BCrypt (compat. PHP $2y$) sin regex -> evita "Illegal group reference"
            String hash = BCrypt.hashpw(pass, BCrypt.gensalt(10));
            if (hash.startsWith("$2a$") || hash.startsWith("$2b$")) {
                hash = "$2y$" + hash.substring(4);
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO usuario(nombre,email,contrasena,id_rol,id_estado_usuario,creado_en,actualizado_en) " +
                    "VALUES (?,?,?,?,?,NOW(),NOW())")){
                ps.setString(1, nombre);
                ps.setString(2, email);
                ps.setString(3, hash);
                ps.setInt(4, rol.id);
                ps.setInt(5, est.id);
                ps.executeUpdate();
            }

            guardado = true;
            JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al crear:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean isEmail(String s){
        if (s==null) return false;
        return Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matcher(s).find();
    }

    static class Item{
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }

    // Conexión local (sin conexion_bd)
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
