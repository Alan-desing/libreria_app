package admin;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

// Paneles de administrador
import admin.inicio.panel_inicio;
import admin.productos.panel_productos;
import admin.categorias.panel_categorias;
import admin.subcategorias.panel_subcategorias;
import admin.pedidos.panel_pedidos;
import admin.inventario.panel_inventario;
import admin.usuarios.panel_usuarios;
import admin.proveedores.panel_proveedores;
import admin.sucursales.panel_sucursales;
import admin.ajustes.panel_ajustes;
import admin.alertas.panel_alertas;
import admin.auditorias.panel_auditorias;
import admin.reportes.panel_reportes;

public class escritorio_admin extends JFrame {

    // Lógica: CardLayout para cambiar vistas al estilo de paneles
    private final CardLayout cards = new CardLayout();
    private final JPanel panelCentral = new JPanel(cards);

    // Lógicas: claves internas para cada vista del CardLayout
    private static final String V_INICIO       = "inicio";
    private static final String V_PRODUCTOS    = "productos";
    private static final String V_CATEGORIAS   = "categorias";
    private static final String V_SUBCATS      = "subcategorias";
    private static final String V_INVENTARIO   = "inventario";
    private static final String V_PEDIDOS      = "pedidos";
    private static final String V_PROVEEDORES  = "proveedores";
    private static final String V_SUCURSALES   = "sucursales";
    private static final String V_ALERTAS      = "alertas";          
    private static final String V_REPORTES     = "reportes";         
    private static final String V_USUARIOS     = "usuarios";
    private static final String V_AJUSTES      = "ajustes";
    private static final String V_AUDITORIAS   = "auditorias";       
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
        panelCentral.add(new panel_inicio(),          V_INICIO);
        panelCentral.add(new panel_productos(),       V_PRODUCTOS);
        panelCentral.add(new panel_categorias(),      V_CATEGORIAS);
        panelCentral.add(new panel_subcategorias(),   V_SUBCATS);
        panelCentral.add(new panel_inventario(),      V_INVENTARIO);
        panelCentral.add(new panel_pedidos(),         V_PEDIDOS);
        panelCentral.add(new panel_proveedores(),     V_PROVEEDORES);
        panelCentral.add(new panel_sucursales(),      V_SUCURSALES);

        // === NUEVOS: registro de vistas reales (sin placeholders) ===
        panelCentral.add(new panel_alertas(),         V_ALERTAS);
        panelCentral.add(new panel_reportes(),        V_REPORTES);
        panelCentral.add(new panel_usuarios(),        V_USUARIOS);
        panelCentral.add(new panel_ajustes(),         V_AJUSTES);
        panelCentral.add(new panel_auditorias(),      V_AUDITORIAS);

        add(panelCentral, BorderLayout.CENTER);

        // Lógica: vista inicial
        setTituloVista("Inicio");
        cards.show(panelCentral, V_INICIO);
    }

    // Visual: cabecera con franja superior y título de vista
    private JComponent crearTop() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(estilos.COLOR_FONDO);

        JPanel barra = new JPanel();
        barra.setBackground(estilos.COLOR_BARRA);
        barra.setPreferredSize(new Dimension(0, 48));
        wrap.add(barra, BorderLayout.NORTH);

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

    private void setTituloVista(String nombreSeccion){
        lblTituloVista.setText("Panel administrativo — " + nombreSeccion);
    }

    // Visual + Lógica: construcción del sidebar con botones y navegación
    private JPanel construirSidebar() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(estilos.COLOR_FONDO);
        side.setPreferredSize(new Dimension(220, 0)); // un poco más angosto

        JPanel box = new JPanel();
        box.setOpaque(true);
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(8, 8, 8, 8) // antes 12
        ));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // Orden idéntico al de la web (según imagen)
        String[][] items = {
                {"Inicio",                  V_INICIO},
                {"Productos",               V_PRODUCTOS},
                {"Categorías",              V_CATEGORIAS},
                {"Subcategorías",           V_SUBCATS},
                {"Inventario",              V_INVENTARIO},
                {"Pedidos",                 V_PEDIDOS},
                {"Proveedores",             V_PROVEEDORES},
                {"Sucursales",              V_SUCURSALES},
                {"Alertas",                 V_ALERTAS},            // nuevo
                {"Reportes y estadísticas", V_REPORTES},           // nuevo
                {"Usuarios",                V_USUARIOS},
                {"Ajustes",                 V_AJUSTES},
                {"Auditorías",              V_AUDITORIAS},         // nuevo
                {"Salir",                   V_SALIR}
        };

        ButtonGroup grupo = new ButtonGroup();
        Map<String, JToggleButton> mapa = new LinkedHashMap<>();

        for (String[] it : items) {
            final String texto = it[0];
            final String clave = it[1];

            JToggleButton btn = new JToggleButton(texto);
            btn.setFocusPainted(false);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setFont(estilos.FUENTE_TEXTO.deriveFont(14f));          // antes 16f
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));    // antes 44
            btn.setBackground(Color.WHITE);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(0,0,0,0), 1, true),
                    new EmptyBorder(8, 10, 8, 10)                        // antes 10,12,10,12
            ));

            btn.addChangeListener(e -> {
                if (btn.isSelected()) {
                    btn.setBackground(new Color(0xFF,0xF7,0xE8));
                    btn.setBorder(new CompoundBorder(
                            new LineBorder(new Color(0xF1,0xD5,0xA3), 1, true),
                            new EmptyBorder(8, 10, 8, 10)
                    ));
                } else {
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(new CompoundBorder(
                            new LineBorder(new Color(0,0,0,0), 1, true),
                            new EmptyBorder(8, 10, 8, 10)
                    ));
                }
            });

            btn.addActionListener(ev -> {
                if (V_SALIR.equals(clave)) {
                    int r = JOptionPane.showConfirmDialog(this,
                            "¿Salir del panel administrativo?",
                            "Confirmar salida", JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) dispose();
                    return;
                }
                cards.show(panelCentral, clave);

                String nombre = switch (clave) {
                    case V_INICIO       -> "Inicio";
                    case V_PRODUCTOS    -> "Productos";
                    case V_CATEGORIAS   -> "Categorías";
                    case V_SUBCATS      -> "Subcategorías";
                    case V_INVENTARIO   -> "Inventario";
                    case V_PEDIDOS      -> "Pedidos";
                    case V_PROVEEDORES  -> "Proveedores";
                    case V_SUCURSALES   -> "Sucursales";
                    case V_ALERTAS      -> "Alertas";
                    case V_REPORTES     -> "Reportes y estadísticas";
                    case V_USUARIOS     -> "Usuarios";
                    case V_AJUSTES      -> "Ajustes";
                    case V_AUDITORIAS   -> "Auditorías";
                    default             -> "Admin";
                };
                setTituloVista(nombre);

                mapa.values().forEach(b -> b.setSelected(false));
                btn.setSelected(true);
            });

            grupo.add(btn);
            mapa.put(clave, btn);
            box.add(btn);
            box.add(Box.createVerticalStrut(6)); // antes 8
        }

        // Selección por defecto
        SwingUtilities.invokeLater(() -> mapa.get(V_INICIO).setSelected(true));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
        gbc.insets = new Insets(10, 10, 10, 10); // antes 14
        gbc.fill = GridBagConstraints.BOTH;
        side.add(box, gbc);

        return side;
    }
}
