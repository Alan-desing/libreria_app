package admin.productos;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Productos — versión “card” centrada, similar a la web.
 * - Contenedor central con GridBagLayout para centrar horizontalmente.
 * - Tarjeta (card) con borde, radios y padding.
 * - Encabezado, buscador, acciones y tabla con estilo crema.
 */
public class panel_productos extends JPanel {

    private JTable tabla;

    public panel_productos() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // ===== Título (fuera de la card, como en la web) =====
        JLabel h1 = new JLabel("Productos");
        h1.setFont(new Font("Arial", Font.BOLD, 28));
        h1.setForeground(estilos.COLOR_TITULO);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 8, 28));
        header.add(h1, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ===== Shell que centra la “card” =====
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // ===== Card =====
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(new Color(0xFF, 0xF9, 0xEF));                 // #fff9ef
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(0xE6,0xD9,0xBF), 1, true), // borde crema redondeado
                BorderFactory.createEmptyBorder(14, 14, 16, 14)                         // padding
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(980, Integer.MAX_VALUE));      // ancho como en la web
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== Fila superior: acciones (botón + filtrar) =====
        JPanel filaTop = new JPanel();
        filaTop.setOpaque(false);
        filaTop.setLayout(new BoxLayout(filaTop, BoxLayout.X_AXIS));
        filaTop.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnAgregar = estilos.botonRedondeado("+ Añadir Producto");
        btnAgregar.setPreferredSize(new Dimension(220, 40));
        btnAgregar.setMaximumSize(new Dimension(240, 40));

        // usar helper de botón blanco (hover suave + borde redondeado)
        JButton btnFiltrarTop = estilos.botonBlanco("Filtrar");

        filaTop.add(Box.createHorizontalGlue()); // empuja a la derecha
        filaTop.add(btnAgregar);
        filaTop.add(Box.createHorizontalStrut(10));
        filaTop.add(btnFiltrarTop);

        // ===== Buscador (≈ mitad del card) =====
        PlaceholderTextField txtBuscar = new PlaceholderTextField("Buscar…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));
        txtBuscar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ===== Fila de filtros (buscador + categoría + stock + filtrar) =====
        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // 1) buscador
        g.gridx = 0; g.weightx = 1.0;
        filaFiltros.add(txtBuscar, g);

        // 2) categoría
        String[] categorias = {"Todas las categorías", "Arte y Dibujo", "Libros", "Papelería"};
        JComboBox<String> cbCategoria = new JComboBox<>(categorias);
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(220, 38));
        cbCategoria.setMinimumSize(new Dimension(180, 38));
        g.gridx = 1; g.weightx = 0;
        filaFiltros.add(cbCategoria, g);

        // 3) stock
        String[] stocks = {"Stock: Todos", "Bajo (≤ mínimo)", "Sin stock"};
        JComboBox<String> cbStock = new JComboBox<>(stocks);
        estilos.estilizarCombo(cbStock);
        cbStock.setPreferredSize(new Dimension(200, 38));
        cbStock.setMinimumSize(new Dimension(160, 38));
        g.gridx = 2;
        filaFiltros.add(cbStock, g);

        // 4) botón Filtrar (de la fila)
        JButton btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));
        g.gridx = 3;
        filaFiltros.add(btnFiltrarFila, g);

        // ===== Tabla =====
        String[] cols = {"ID", "Nombre", "Categoría", "Stock", "Precio"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 14));
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tabla.getTableHeader().setBackground(new Color(0xFF,0xF3,0xD9)); // header crema
        tabla.getTableHeader().setOpaque(true);

        JScrollPane sc = new JScrollPane(tabla);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(0xE6,0xD9,0xBF), 1, true),  // redondeado
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 380));

        // ===== Ensamble dentro de la card =====
        card.add(filaTop);
        card.add(Box.createVerticalStrut(10));
        card.add(filaFiltros);                // <<--- fila nueva con los filtros
        card.add(Box.createVerticalStrut(10));
        card.add(sc);

        // agregar card al shell centrado
        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);
    }

    // ===== Placeholder en JTextField =====
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font("Arial", Font.PLAIN, 14));
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(155, 142, 127)); // gris marrón suave
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
