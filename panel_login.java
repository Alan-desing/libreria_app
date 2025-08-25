import admin.escritorio_admin;
import includes.estilos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.net.URL;

/**
 * Login a pantalla completa, con placeholders y alineación ajustada
 * - Usa includes.estilos para colores/fuentes/botón
 * - Mantiene la lógica de verificación con conexion_bd
 */
public class panel_login extends JFrame {

    private static final String IMG_PATH = "includes/img/libro.png";

    private PlaceholderTextField txtUsuario;
    private PlaceholderPasswordField txtPassword;
    private JButton btnLogin;

    private JLabel lblImagen;
    private Image imgOriginal;

    public panel_login() {
        setTitle("Login - Sistema Librería");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // pantalla completa
        getContentPane().setBackground(estilos.COLOR_FONDO);

        // ===== Barra superior (más gruesa) =====
        JPanel barraSuperior = new JPanel();
        barraSuperior.setBackground(estilos.COLOR_BARRA);
        barraSuperior.setPreferredSize(new Dimension(0, 64));

        // ===== Split principal (izq: contenido / der: imagen) =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setBorder(null);
        split.setBackground(estilos.COLOR_FONDO);

        JPanel izquierda = construirColumnaIzquierda();
        JPanel derecha   = construirColumnaDerecha();

        split.setLeftComponent(izquierda);
        split.setRightComponent(derecha);
        split.setResizeWeight(0.58); // más espacio a la izquierda

        // ===== Root =====
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(estilos.COLOR_FONDO);
        root.add(barraSuperior, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);

        // Mantener proporción + reescalar imagen en resize
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                split.setDividerLocation(0.58);
                escalarImagenSinDistorsion();
            }
        });

        // Acción de login
        btnLogin.addActionListener(e -> verificarLogin());
    }

    /**
     * Construye la columna izquierda (centrada y con margen izquierdo).
     */
    private JPanel construirColumnaIzquierda() {
        JPanel cont = new JPanel(new GridBagLayout());
        cont.setBackground(estilos.COLOR_FONDO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 80, 0, 0); // margen a la izquierda

        final int contentWidth = 540;
        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new BoxLayout(bloque, BoxLayout.Y_AXIS));
        bloque.setMaximumSize(new Dimension(contentWidth, Integer.MAX_VALUE));
        bloque.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Título =====
        JLabel titulo = new JLabel("Librería Los Lapicitos");
        titulo.setFont(estilos.FUENTE_TITULO.deriveFont(Font.BOLD, 48f));
        titulo.setForeground(estilos.COLOR_TITULO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Párrafo =====
        JLabel parrafo = new JLabel("<html><div style='width:" + contentWidth + "px'>"
                + "Bienvenido a tu sistema de gestión de inventario.<br>"
                + "Mantén el control de tus productos, ventas y pedidos de forma fácil y rápida."
                + "</div></html>");
        parrafo.setFont(estilos.FUENTE_TEXTO.deriveFont(18f));
        parrafo.setForeground(new Color(70,70,70));
        parrafo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Formulario =====
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUsuario = new PlaceholderTextField("Correo electrónico");
        estilos.estilizarCampo(txtUsuario);
        txtUsuario.setMaximumSize(new Dimension(contentWidth, 48));

        txtPassword = new PlaceholderPasswordField("Contraseña");
        estilos.estilizarCampo(txtPassword);
        txtPassword.setMaximumSize(new Dimension(contentWidth, 48));

        btnLogin = estilos.botonRedondeado("Iniciar sesión");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setPreferredSize(new Dimension(220, 48));
        btnLogin.setMaximumSize(new Dimension(220, 48));

        form.add(Box.createVerticalStrut(15));
        form.add(txtUsuario);
        form.add(Box.createVerticalStrut(15));
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(20));
        form.add(btnLogin);

        bloque.add(titulo);
        bloque.add(Box.createVerticalStrut(14));
        bloque.add(parrafo);
        bloque.add(Box.createVerticalStrut(20));
        bloque.add(form);

        cont.add(bloque, gbc);

        return cont;
    }

    /**
     * Construye columna derecha con imagen escalada sin distorsión.
     */
    private JPanel construirColumnaDerecha() {
        JPanel cont = new JPanel(new BorderLayout());
        cont.setBackground(estilos.COLOR_FONDO);
        cont.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 64));

        lblImagen = new JLabel("", SwingConstants.CENTER);
        lblImagen.setOpaque(false);

        // carga imagen
        URL res = getClass().getClassLoader().getResource(IMG_PATH);
        if (res != null) {
            imgOriginal = new ImageIcon(res).getImage();
        } else {
            File f1 = new File(IMG_PATH);
            File f2 = new File("src/" + IMG_PATH);
            if      (f1.exists()) imgOriginal = new ImageIcon(f1.getAbsolutePath()).getImage();
            else if (f2.exists()) imgOriginal = new ImageIcon(f2.getAbsolutePath()).getImage();
        }

        if (imgOriginal != null) {
            escalarImagenSinDistorsion();
        } else {
            lblImagen.setText("<html><div style='text-align:center;'>No se encontró la imagen:<br/>" + IMG_PATH + "</div></html>");
            lblImagen.setFont(estilos.FUENTE_TEXTO);
        }

        cont.add(lblImagen, BorderLayout.CENTER);

        lblImagen.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                escalarImagenSinDistorsion();
            }
        });

        return cont;
    }

    /** Escala manteniendo proporción. */
    private void escalarImagenSinDistorsion() {
        if (imgOriginal == null || lblImagen == null) return;

        int availW = Math.max(lblImagen.getWidth(), 1);
        int availH = Math.max(lblImagen.getHeight(), 1);

        int imgW = imgOriginal.getWidth(null);
        int imgH = imgOriginal.getHeight(null);
        if (imgW <= 0 || imgH <= 0) return;

        double scale = Math.min(availW / (double) imgW, availH / (double) imgH);
        int newW = Math.max(1, (int) Math.round(imgW * scale));
        int newH = Math.max(1, (int) Math.round(imgH * scale));

        Image scaled = imgOriginal.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        lblImagen.setIcon(new ImageIcon(scaled));
        lblImagen.repaint();
    }

    // ---------- Lógica de login ----------
    private void verificarLogin() {
        String usuario = txtUsuario.getText().trim();
        String pass    = new String(txtPassword.getPassword());

        boolean valido = conexion_bd.verificarLogin(usuario, pass);

        if (valido) {
            JOptionPane.showMessageDialog(this, "Bienvenido " + usuario);
            dispose();
            new escritorio_admin().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
        }
    }

    // ---------- Campos con placeholder ----------
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder) {
            super();
            this.placeholder = placeholder;
            setFont(estilos.FUENTE_INPUT);
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().length() == 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(150,150,150));
                g2.setFont(getFont());
                Insets in = getInsets();
                int x = in.left + 4;
                int y = getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }

    static class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;
        PlaceholderPasswordField(String placeholder) {
            super();
            this.placeholder = placeholder;
            setFont(estilos.FUENTE_INPUT);
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(150,150,150));
                g2.setFont(getFont());
                Insets in = getInsets();
                int x = in.left + 4;
                int y = getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new panel_login().setVisible(true));
    }
}
