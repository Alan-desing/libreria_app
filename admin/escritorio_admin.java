package admin;
import includes.estilos;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

// ⬇️ Importa cada panel según tu árbol de carpetas
import admin.productos.panel_productos;
import admin.categorias.panel_categorias;
import admin.pedidos.panel_pedidos;
import admin.inventario.panel_inventario;
import admin.proveedores.panel_proveedores;
import admin.usuarios.panel_usuarios;
import admin.sucursales.panel_sucursales;
import admin.reportes.panel_reportes;

public class escritorio_admin extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel panelCentral = new JPanel(cards);

    // claves de las vistas
    private static final String V_PRODUCTOS    = "productos";
    private static final String V_CATEGORIAS   = "categorias";
    private static final String V_PEDIDOS      = "pedidos";
    private static final String V_INVENTARIO   = "inventario";
    private static final String V_PROVEEDORES  = "proveedores";
    private static final String V_USUARIOS     = "usuarios";
    private static final String V_SUCURSALES   = "sucursales";
    private static final String V_REPORTES     = "reportes";

    public escritorio_admin() {
        setTitle("Gestión de Inventario - Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new BorderLayout());

        // ===== Top bar =====
        JLabel titulo = new JLabel("Gestión de Inventario", SwingConstants.CENTER);
        titulo.setFont(estilos.FUENTE_TITULO.deriveFont(Font.BOLD, 22f));
        titulo.setOpaque(true);
        titulo.setBackground(estilos.COLOR_BARRA);
        titulo.setForeground(new Color(95, 86, 86)); // similar al mock
        titulo.setPreferredSize(new Dimension(0, 56));
        add(titulo, BorderLayout.NORTH);

        // ===== Sidebar =====
        JPanel panelMenu = construirSidebar();
        add(panelMenu, BorderLayout.WEST);

        // ===== Centro con CardLayout =====
        panelCentral.setBackground(estilos.COLOR_FONDO);

        // Vistas reales (instancia cada panel de su paquete)  👇 CAMBIADO: (String, Component)
        // Vistas reales (CardLayout)  ✅ componente primero
        panelCentral.add(new panel_productos(),   V_PRODUCTOS);
        panelCentral.add(new panel_categorias(),  V_CATEGORIAS);
        panelCentral.add(new panel_pedidos(),     V_PEDIDOS);
        panelCentral.add(new panel_inventario(),  V_INVENTARIO);
        panelCentral.add(new panel_proveedores(), V_PROVEEDORES);
        panelCentral.add(new panel_usuarios(),    V_USUARIOS);
        panelCentral.add(new panel_sucursales(),  V_SUCURSALES);
        panelCentral.add(new panel_reportes(),    V_REPORTES);


        add(panelCentral, BorderLayout.CENTER);

        // vista inicial
        cards.show(panelCentral, V_PRODUCTOS);
        }

    private JPanel construirSidebar() {
        JPanel side = new JPanel();
        side.setBackground(new Color(0xF6,0xE7,0xB5)); // beige suave (similar boceto)
        side.setLayout(new GridBagLayout());
        side.setPreferredSize(new Dimension(220, 0));

        String[][] items = {
                {"Productos",    V_PRODUCTOS},
                {"Categorías",   V_CATEGORIAS},
                {"Pedidos",      V_PEDIDOS},
                {"Inventario",   V_INVENTARIO},
                {"Proveedores",  V_PROVEEDORES},
                {"Usuarios",     V_USUARIOS},
                {"Sucursales",   V_SUCURSALES},
                {"Reportes",     V_REPORTES}
        };

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));

        ButtonGroup grupo = new ButtonGroup();
        Map<String, JToggleButton> mapa = new LinkedHashMap<>();

        for (String[] it : items) {
            String texto = it[0];
            String clave = it[1];

            JToggleButton btn = new JToggleButton(texto);
            btn.setFocusPainted(false);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setFont(estilos.FUENTE_TEXTO.deriveFont(16f));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            btn.setBackground(new Color(0xF6,0xE7,0xB5));
            btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));

            // estilo "selected" similar al boceto
            btn.addChangeListener(e -> {
                if (btn.isSelected()) {
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(230, 210, 170)),
                            BorderFactory.createEmptyBorder(10, 14, 10, 10)
                    ));
                } else {
                    btn.setBackground(new Color(0xF6,0xE7,0xB5));
                    btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
                }
            });

            btn.addActionListener(ev -> {
                cards.show(panelCentral, clave);
                mapa.values().forEach(b -> b.setSelected(false));
                btn.setSelected(true);
            });

            grupo.add(btn);
            mapa.put(clave, btn);
            col.add(btn);
            col.add(Box.createVerticalStrut(12));
        }

        // marcar “Productos” por defecto
        SwingUtilities.invokeLater(() -> mapa.get(V_PRODUCTOS).setSelected(true));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        side.add(col, gbc);
        return side;
    }
}
