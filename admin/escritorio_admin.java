package admin;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final String V_PRODUCTOS    = "productos";
    private static final String V_CATEGORIAS   = "categorias";
    private static final String V_PEDIDOS      = "pedidos";
    private static final String V_INVENTARIO   = "inventario";
    private static final String V_PROVEEDORES  = "proveedores";
    private static final String V_USUARIOS     = "usuarios";
    private static final String V_SUCURSALES   = "sucursales";
    private static final String V_REPORTES     = "reportes";

    public escritorio_admin() {
        setTitle("Los Lapicitos — Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new BorderLayout());

        // ===== Top (solo franja amarilla, sin texto) =====
        add(crearTop(), BorderLayout.NORTH);

        // ===== Sidebar =====
        add(construirSidebar(), BorderLayout.WEST);

        // ===== Centro (CardLayout) =====
        panelCentral.setBackground(estilos.COLOR_FONDO);
        panelCentral.add(new panel_productos(),   V_PRODUCTOS);
        panelCentral.add(new panel_categorias(),  V_CATEGORIAS);
        panelCentral.add(new panel_pedidos(),     V_PEDIDOS);
        panelCentral.add(new panel_inventario(),  V_INVENTARIO);
        panelCentral.add(new panel_proveedores(), V_PROVEEDORES);
        panelCentral.add(new panel_usuarios(),    V_USUARIOS);
        panelCentral.add(new panel_sucursales(),  V_SUCURSALES);
        panelCentral.add(new panel_reportes(),    V_REPORTES);
        add(panelCentral, BorderLayout.CENTER);

        cards.show(panelCentral, V_PRODUCTOS);
    }

    private JComponent crearTop() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(estilos.COLOR_FONDO);

        // Franja amarilla
        JPanel barra = new JPanel();
        barra.setBackground(estilos.COLOR_BARRA);
        barra.setPreferredSize(new Dimension(0, 48));
        wrap.add(barra, BorderLayout.NORTH);

        return wrap;
    }

    private JPanel construirSidebar() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(estilos.COLOR_FONDO);
        side.setPreferredSize(new Dimension(240, 0));

        JPanel box = new JPanel();
        box.setOpaque(true);
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        String[][] items = {
                {"Productos",       V_PRODUCTOS},
                {"Categorías",      V_CATEGORIAS},
                {"Inventario",      V_INVENTARIO},
                {"Pedidos",         V_PEDIDOS},
                {"Proveedores",     V_PROVEEDORES},
                {"Usuarios",        V_USUARIOS},
                {"Sucursales",      V_SUCURSALES},
                {"Reportes",        V_REPORTES}
        };

        ButtonGroup grupo = new ButtonGroup();
        Map<String, JToggleButton> mapa = new LinkedHashMap<>();

        for (String[] it : items) {
            final String texto = it[0];
            final String clave = it[1];

            JToggleButton btn = new JToggleButton(texto);
            btn.setFocusPainted(false);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setFont(estilos.FUENTE_TEXTO.deriveFont(16f));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            btn.setBackground(Color.WHITE);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(0,0,0,0), 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            btn.addChangeListener(e -> {
                if (btn.isSelected()) {
                    btn.setBackground(new Color(0xFF,0xF7,0xE8));
                    btn.setBorder(new CompoundBorder(
                            new LineBorder(new Color(0xF1,0xD5,0xA3), 1, true),
                            new EmptyBorder(10, 12, 10, 12)
                    ));
                } else {
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(new CompoundBorder(
                            new LineBorder(new Color(0,0,0,0), 1, true),
                            new EmptyBorder(10, 12, 10, 12)
                    ));
                }
            });

            btn.addActionListener(ev -> {
                switch (clave){
                    case V_PRODUCTOS, V_CATEGORIAS, V_PEDIDOS, V_INVENTARIO,
                         V_PROVEEDORES, V_USUARIOS, V_SUCURSALES, V_REPORTES -> {
                        cards.show(panelCentral, clave);
                    }
                }
                mapa.values().forEach(b -> b.setSelected(false));
                btn.setSelected(true);
            });

            grupo.add(btn);
            mapa.put(clave, btn);
            box.add(btn);
            box.add(Box.createVerticalStrut(8));
        }

        SwingUtilities.invokeLater(() -> mapa.get(V_PRODUCTOS).setSelected(true));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.fill = GridBagConstraints.BOTH;
        side.add(box, gbc);

        return side;
    }
}
