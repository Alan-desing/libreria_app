package admin.productos;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class panel_productos extends JPanel {

    private JTable tabla;

    public panel_productos() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // ===== Header =====
        JLabel h1 = new JLabel("Productos");
        h1.setFont(estilos.FUENTE_TITULO.deriveFont(Font.BOLD, 32f));
        h1.setForeground(estilos.COLOR_TITULO);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(24, 32, 8, 32));
        header.add(h1, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ===== Contenido =====
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(0, 32, 32, 32));
        add(content, BorderLayout.CENTER);

        // Fila superior: botón + filtrar
        JPanel filaTop = new JPanel();
        filaTop.setOpaque(false);
        filaTop.setLayout(new BoxLayout(filaTop, BoxLayout.X_AXIS));

        JButton btnAgregar = estilos.botonRedondeado("+Añadir Producto");
        btnAgregar.setPreferredSize(new Dimension(220, 44));
        btnAgregar.setMaximumSize(new Dimension(220, 44));

        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.setFocusPainted(false);
        btnFiltrar.setFont(estilos.FUENTE_TEXTO);
        btnFiltrar.setBackground(Color.WHITE);
        btnFiltrar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 210, 170)),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));

        filaTop.add(Box.createHorizontalGlue());
        filaTop.add(btnAgregar);
        filaTop.add(Box.createHorizontalStrut(16));
        filaTop.add(btnFiltrar);

        // Buscador con placeholder
        PlaceholderTextField txtBuscar = new PlaceholderTextField("Buscar…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtBuscar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tabla
        String[] cols = {"ID", "Nombre", "Categoria", "Stock", "Precio"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(model);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(estilos.FUENTE_TEXTO.deriveFont(Font.BOLD, 14f));
        tabla.setFont(estilos.FUENTE_TEXTO);

        JScrollPane sc = new JScrollPane(tabla);
        sc.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 210, 170)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 380));

        // Ensamble
        content.add(filaTop);
        content.add(Box.createVerticalStrut(16));
        content.add(txtBuscar);
        content.add(Box.createVerticalStrut(16));
        content.add(sc);
    }

    // ===== Campo con placeholder (igual que en login) =====
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(estilos.FUENTE_INPUT);
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160,160,160));
                g2.setFont(getFont());
                Insets in = getInsets();
                int x = in.left + 4;
                int y = getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }
}
