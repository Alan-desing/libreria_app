package admin;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

// Paneles ya implementadas
import admin.inicio.panel_inicio;
import admin.productos.panel_productos;
import admin.categorias.panel_categorias;
import admin.subcategorias.panel_subcategorias;
import admin.pedidos.panel_pedidos;
import admin.inventario.panel_inventario;
import admin.reportes.panel_reportes;
import admin.usuarios.panel_usuarios;

public class escritorio_admin extends JFrame {

    // Lógica: CardLayout para cambiar vistas al estilo de paneles
    private final CardLayout cards = new CardLayout();
    private final JPanel panelCentral = new JPanel(cards);

    // Lógica: claves internas para cada vista del CardLayout
    private static final String V_INICIO       = "inicio";
    private static final String V_PRODUCTOS    = "productos";
    private static final String V_CATEGORIAS   = "categorias";
    private static final String V_SUBCATS      = "subcategorias";
    private static final String V_INVENTARIO   = "inventario";
    private static final String V_PEDIDOS      = "pedidos";
    private static final String V_ALERTAS      = "alertas";
    private static final String V_REPORTES     = "reportes";
    private static final String V_VENTAS       = "ventas";
    private static final String V_USUARIOS     = "usuarios";
    private static final String V_ROLES        = "roles";
    private static final String V_AJUSTES      = "ajustes";
    private static final String V_SALIR        = "salir";

    // Visual: título de la sección activa
    private JLabel lblTituloVista;

    public escritorio_admin() {
        setTitle("Los Lapicitos — Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new BorderLayout());

        // Visual: encabezado superior (franja + título)
        add(crearTop(), BorderLayout.NORTH);

        // Visual: sidebar con navegación
        add(construirSidebar(), BorderLayout.WEST);

        // Visual: contenedor central de vistas
        panelCentral.setBackground(estilos.COLOR_FONDO);

        // Lógica: registrar cada vista en el CardLayout
        panelCentral.add(new panel_inicio(),         V_INICIO);
        panelCentral.add(new panel_productos(),      V_PRODUCTOS);
        panelCentral.add(new panel_categorias(),     V_CATEGORIAS);
        panelCentral.add(new panel_subcategorias(),  V_SUBCATS);
        panelCentral.add(new panel_inventario(),     V_INVENTARIO);
        panelCentral.add(new panel_pedidos(),        V_PEDIDOS);
        panelCentral.add(new panel_reportes(),       V_REPORTES);
        panelCentral.add(new panel_usuarios(),       V_USUARIOS);

        // Visual: placeholders para secciones aún no desarrolladas
        panelCentral.add(new Placeholder("Alertas"),            V_ALERTAS);
        panelCentral.add(new Placeholder("Ventas"),             V_VENTAS);
        panelCentral.add(new Placeholder("Roles y permisos"),   V_ROLES);
        panelCentral.add(new Placeholder("Ajustes"),            V_AJUSTES);

        add(panelCentral, BorderLayout.CENTER);

        // Lógica: vista inicial
        setTituloVista("Inicio");
        cards.show(panelCentral, V_INICIO);
    }

    // Visual: cabecera con franja superior y título de vista
    private JComponent crearTop() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(estilos.COLOR_FONDO);

        // Visual: franja amarilla
        JPanel barra = new JPanel();
        barra.setBackground(estilos.COLOR_BARRA);
        barra.setPreferredSize(new Dimension(0, 48));
        wrap.add(barra, BorderLayout.NORTH);

        // Visual: etiqueta de título centrada
        lblTituloVista = new JLabel("Panel administrativo — Inicio", SwingConstants.CENTER);
        lblTituloVista.setForeground(estilos.COLOR_TITULO);
        lblTituloVista.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel tituloWrap = new JPanel(new BorderLayout());
        tituloWrap.setOpaque(false);
        tituloWrap.setBorder(new EmptyBorder(8, 0, 8, 0));
        tituloWrap.add(lblTituloVista, BorderLayout.CENTER);

        wrap.add(tituloWrap, BorderLayout.CENTER);
        return wrap;
    }

    // Lógica: actualizar el texto del título al cambiar de vista
    private void setTituloVista(String nombreSeccion){
        lblTituloVista.setText("Panel administrativo — " + nombreSeccion);
    }

    // Visual + Lógica: construcción del sidebar con botones y navegación
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

        // Lógica: definición de items del menú en orden
        String[][] items = {
                {"inicio",             V_INICIO},
                {"Productos",          V_PRODUCTOS},
                {"categorias",         V_CATEGORIAS},
                {"subcategorias",      V_SUBCATS},
                {"Inventario",         V_INVENTARIO},
                {"Pedidos",            V_PEDIDOS},
                {"Alertas",            V_ALERTAS},
                {"Reportes",           V_REPORTES},
                {"Ventas",             V_VENTAS},
                {"Usuarios",           V_USUARIOS},
                {"Roles y permisos",   V_ROLES},
                {"Ajustes",            V_AJUSTES},
                {"Salir",              V_SALIR}
        };

        ButtonGroup grupo = new ButtonGroup();
        Map<String, JToggleButton> mapa = new LinkedHashMap<>();

        // Visual + Lógica: creación de cada botón de navegación
        for (String[] it : items) {
            final String texto = it[0];
            final String clave = it[1];

            // Visual: botón tipo toggle para indicar selección
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

            // Visual: efecto de selección (resaltado)
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

            // Lógica: handler de click para cambiar la vista o salir
            btn.addActionListener(ev -> {
                if (V_SALIR.equals(clave)) {
                    int r = JOptionPane.showConfirmDialog(this,
                            "¿Salir del panel administrativo?",
                            "Confirmar salida", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) dispose();
                    return;
                }
                // Lógica: cambio de panel activo
                cards.show(panelCentral, clave);

                // Lógica: actualización del título según la clave
                String nombre = switch (clave) {
                    case V_INICIO      -> "Inicio";
                    case V_PRODUCTOS   -> "Productos";
                    case V_CATEGORIAS  -> "Categorías";
                    case V_SUBCATS     -> "Subcategorías";
                    case V_INVENTARIO  -> "Inventario";
                    case V_PEDIDOS     -> "Pedidos";
                    case V_ALERTAS     -> "Alertas";
                    case V_REPORTES    -> "Reportes";
                    case V_VENTAS      -> "Ventas";
                    case V_USUARIOS    -> "Usuarios";
                    case V_ROLES       -> "Roles y permisos";
                    case V_AJUSTES     -> "Ajustes";
                    default            -> "Admin";
                };
                setTituloVista(nombre);

                // Visual: marcar seleccionado y deseleccionar el resto
                mapa.values().forEach(b -> b.setSelected(false));
                btn.setSelected(true);
            });

            grupo.add(btn);
            mapa.put(clave, btn);
            box.add(btn);
            box.add(Box.createVerticalStrut(8));
        }

        // Visual: selección por defecto en “Inicio”
        SwingUtilities.invokeLater(() -> mapa.get(V_INICIO).setSelected(true));

        // Visual: ubicación del contenedor en el sidebar
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.fill = GridBagConstraints.BOTH;
        side.add(box, gbc);

        return side;
    }

    // Visual: tarjeta placeholder para secciones no implementadas aún
    static class Placeholder extends JPanel {
        Placeholder(String nombre){
            setLayout(new GridBagLayout());
            setBackground(estilos.COLOR_FONDO);
            JPanel card = new JPanel();
            card.setBackground(Color.WHITE);
            card.setBorder(new CompoundBorder(
                    new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                    new EmptyBorder(24,24,24,24)
            ));
            card.add(new JLabel(nombre + " — En construcción"));
            add(card, new GridBagConstraints());
        }
    }
}
