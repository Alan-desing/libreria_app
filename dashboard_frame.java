import javax.swing.*;
import java.awt.*;
//import java.awt.event.*;  No se usa por ahora

public class dashboard_frame extends JFrame {
    private JPanel panelCentral;

    public dashboard_frame() {
        setTitle("Gestión de Inventario - Admin");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Encabezado
        JLabel titulo = new JLabel("Gestión de Inventario", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setOpaque(true);
        titulo.setBackground(new Color(230, 170, 90));
        titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);

        // Panel lateral
        JPanel panelMenu = new JPanel();
        panelMenu.setLayout(new GridLayout(8, 1, 5, 5));
        panelMenu.setBackground(new Color(250, 230, 180));
        String[] secciones = {"Productos", "Categorías", "Pedidos", "Inventario", "Proveedores", "Usuarios", "Sucursales", "Reportes"};

        for (String s : secciones) {
            JButton btn = new JButton(s);
            btn.setFocusPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBackground(new Color(250, 230, 180));
            btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

            btn.addActionListener(e -> mostrarPanel(s));
            panelMenu.add(btn);
        }

        add(panelMenu, BorderLayout.WEST);

        // Panel central (donde se cargan los paneles de cada sección)
        panelCentral = new JPanel(new BorderLayout());
        JLabel labelBienvenida = new JLabel("Bienvenido, Admin", SwingConstants.CENTER);
        panelCentral.add(labelBienvenida, BorderLayout.CENTER);

        add(panelCentral, BorderLayout.CENTER);
    }

    private void mostrarPanel(String seccion) {
        panelCentral.removeAll();
        JLabel label = new JLabel("Sección: " + seccion, SwingConstants.CENTER);
        panelCentral.add(label, BorderLayout.CENTER);
        panelCentral.revalidate();
        panelCentral.repaint();
    }
}
