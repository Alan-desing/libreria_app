import javax.swing.*;
import java.awt.*;
// import java.awt.event.*; No se usa por ahora

public class login_frame extends JFrame {
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public login_frame() {
        setTitle("Login - Sistema Librería");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Componentes
        panel.add(new JLabel("Usuario/Email:"));
        txtUsuario = new JTextField();
        panel.add(txtUsuario);

        panel.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        panel.add(txtPassword);

        btnLogin = new JButton("Iniciar sesión");
        panel.add(new JLabel()); // espacio vacío
        panel.add(btnLogin);

        add(panel);

        // Acción de login
        btnLogin.addActionListener(e -> verificarLogin());
    }

    private void verificarLogin() {
        String usuario = txtUsuario.getText().trim();   // acá va el EMAIL
        String pass = new String(txtPassword.getPassword());

        boolean valido = conexion_bd.verificarLogin(usuario, pass);

        if (valido) {
            JOptionPane.showMessageDialog(this, "Bienvenido " + usuario);
            dispose();
            new dashboard_frame().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
        }
    }

}
