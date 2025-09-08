package admin.usuarios;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class panel_usuarios extends JPanel {

    // Visual: tabla visible y su modelo de datos
    private JTable tabla;
    private DefaultTableModel model;

    // Visual: filtros superiores (buscador, rol, estado y botón)
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbRol;
    private JComboBox<Item> cbEstado;
    private JButton btnFiltrarFila;

    // Visual: botón para crear usuario
    private JButton btnAgregar;

    // Visual + Lógica: constructor que arma toda la pantalla y engancha eventos
    public panel_usuarios() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Visual: contenedor externo (márgenes y centrado de la card)
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Visual: card blanca con borde crema (toda la vista vive adentro)
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(16, 16, 18, 16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Visual: header con título a la izquierda y “Añadir Usuario” a la derecha
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Usuarios");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        btnAgregar = estilos.botonRedondeado("+ Añadir Usuario");
        btnAgregar.setPreferredSize(new Dimension(220, 40));
        btnAgregar.setMaximumSize(new Dimension(240, 40));
        head.add(btnAgregar, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        // Visual: fila de filtros (buscador + rol + estado + botón FILTRAR)
        txtBuscar = new PlaceholderTextField("Buscar por nombre o email…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        // Visual: buscador
        g.gridx = 0; g.weightx = 1.0;
        filaFiltros.add(txtBuscar, g);

        // Visual: combo de rol
        cbRol = new JComboBox<>();
        estilos.estilizarCombo(cbRol);
        cbRol.setPreferredSize(new Dimension(180, 38));
        g.gridx = 1; g.weightx = 0;
        filaFiltros.add(cbRol, g);

        // Visual: combo de estado
        cbEstado = new JComboBox<>();
        estilos.estilizarCombo(cbEstado);
        cbEstado.setPreferredSize(new Dimension(180, 38));
        g.gridx = 2;
        filaFiltros.add(cbEstado, g);

        // Visual: botón FILTRAR
        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));
        g.gridx = 3;
        filaFiltros.add(btnFiltrarFila, g);

        // Visual: definición de columnas de la tabla
        String[] cols = {"ID", "Nombre", "Email", "Rol", "Estado", "Creado", "Editar", "eliminar"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6 || c == 7; } // Lógica: solo acciones
            @Override public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 6 || columnIndex == 7) ? JButton.class : Object.class;
            }
        };

        // Visual: estilos de la tabla (fuentes, colores, selección)
        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 17));
        tabla.getTableHeader().setReorderingAllowed(false);
        JTableHeader th = tabla.getTableHeader();
        th.setBackground(new Color(0xFF,0xF3,0xD9)); // crema

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        // Visual: anchos sugeridos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(260);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(90);

        // Visual: alineación por defecto a la izquierda
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // Visual: renderer del ID para mostrarlo con “#”
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#" + String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Visual + Lógica: botones por fila (Editar / eliminar)
        tabla.getColumnModel().getColumn(6).setCellRenderer(new ButtonCellRenderer(false)); // Editar
        tabla.getColumnModel().getColumn(7).setCellRenderer(new ButtonCellRenderer(true));  // eliminar
        tabla.getColumnModel().getColumn(6).setCellEditor(new ButtonCellEditor(tabla, id -> onEditar(id), false));
        tabla.getColumnModel().getColumn(7).setCellEditor(new ButtonCellEditor(tabla, id -> onEliminar(id), true));

        // Visual: scroll con borde crema
        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420));

        // Visual: ensamblado final
        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        // Visual: agregamos la card al shell y el shell al panel
        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Lógica: integración con diálogos (crear/editar/eliminar) y filtros
        btnAgregar.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            crear dlg = new crear(owner);
            dlg.setVisible(true);
            if (dlg.fueGuardado()) cargarTabla();
        });
        btnFiltrarFila.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());

        // Lógica/BD: carga inicial de combos y tabla
        cargarRoles();
        cargarEstados();
        cargarTabla();
    }

    // Lógica/BD: llena combo de roles desde tabla rol
    private void cargarRoles() {
        cbRol.removeAllItems();
        cbRol.addItem(new Item(0, "Rol: Todos"));
        String sql = "SELECT id_rol, nombre_rol FROM rol ORDER BY nombre_rol";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbRol.addItem(new Item(rs.getInt("id_rol"), rs.getString("nombre_rol")));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando roles:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica/BD: llena combo de estados desde tabla estado_usuario
    private void cargarEstados() {
        cbEstado.removeAllItems();
        cbEstado.addItem(new Item(0, "Estado: Todos"));
        String sql = "SELECT id_estado_usuario, nombre_estado FROM estado_usuario ORDER BY nombre_estado";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbEstado.addItem(new Item(rs.getInt("id_estado_usuario"), rs.getString("nombre_estado")));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando estados:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica/BD: arma la consulta con filtros aplicados y llena la tabla
    private void cargarTabla() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();

        Item rol    = (Item) cbRol.getSelectedItem();
        int idRol   = (rol == null) ? 0 : rol.id();

        Item est    = (Item) cbEstado.getSelectedItem();
        int idEst   = (est == null) ? 0 : est.id();

        // BD: FROM + JOINs para traer rol y estado del usuario
        String baseFrom = """
                FROM usuario u
                LEFT JOIN rol r            ON r.id_rol = u.id_rol
                LEFT JOIN estado_usuario e ON e.id_estado_usuario = u.id_estado_usuario
                """;

        // Lógica: WHERE dinámico (texto, rol y estado)
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (!q.isEmpty()) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("(u.nombre LIKE ? OR u.email LIKE ?)");
            params.add("%"+q+"%");
            params.add("%"+q+"%");
        }
        if (idRol > 0) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("u.id_rol = ?");
            params.add(idRol);
        }
        if (idEst > 0) {
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("u.id_estado_usuario = ?");
            params.add(idEst);
        }

        // BD: SELECT final (ordenado por nombre)
        String sql = """
                SELECT
                    u.id_usuario,
                    u.nombre,
                    u.email,
                    u.creado_en,
                    r.nombre_rol,
                    e.nombre_estado
                """ + baseFrom + where + """
                 ORDER BY u.nombre ASC
                """;

        // Visual: limpiar tabla antes de cargar
        model.setRowCount(0);

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            // Lógica/BD: bind de parámetros seguro
            int bind = 1;
            for (Object v : params) {
                if (v instanceof Integer iv) ps.setInt(bind++, iv);
                else ps.setString(bind++, String.valueOf(v));
            }

            // Lógica + Visual: volcamos resultados en el modelo
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id         = rs.getInt("id_usuario");
                    String nombre  = rs.getString("nombre");
                    String email   = rs.getString("email");
                    String rolNom  = rs.getString("nombre_rol");
                    String estNom  = rs.getString("nombre_estado");
                    String creado  = rs.getString("creado_en");

                    model.addRow(new Object[]{
                            String.valueOf(id),
                            (nombre==null? "—" : nombre),
                            (email==null? "—" : email),
                            (rolNom==null? "—" : rolNom),
                            (estNom==null? "—" : estNom),
                            (creado==null? "—" : creado),
                            "Editar",
                            "eliminar"
                    });
                }
            }

            // Visual: fila informativa si no hay resultados
            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","Sin resultados.","","","","","",""});
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando usuarios:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: abre diálogo de edición y refresca al guardar
    private void onEditar(int idUsuario){
        Window owner = SwingUtilities.getWindowAncestor(this);
        editar dlg = new editar(owner, idUsuario);
        dlg.setVisible(true);
        if (dlg.fueActualizado()) cargarTabla();
    }

    // Lógica: abre diálogo de eliminación y refresca si se eliminó
    private void onEliminar(int idUsuario){
        Window owner = SwingUtilities.getWindowAncestor(this);
        eliminar dlg = new eliminar(owner, idUsuario);
        dlg.setVisible(true);
        if (dlg.fueEliminado()) cargarTabla();
    }

    // Lógica/BD: helper de conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }

    // Visual: campo de texto con placeholder (texto guía al estar vacío)
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
                g2.setColor(new Color(155, 142, 127));
                g2.setFont(getFont());
                Insets in = getInsets();
                int x = in.left + 4;
                int y = getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }

    // Lógica: item simple para combos (id + nombre visible)
    static class Item {
        private final int id; private final String nombre;
        Item(int id, String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        public String toString(){ return nombre; }
    }

    // Visual: renderer de botón por celda (usa helpers de estilos)
    static class ButtonCellRenderer extends JButton implements TableCellRenderer {
        private final boolean danger;
        ButtonCellRenderer(boolean danger){
            this.danger = danger;
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return danger ? estilos.botonSmDanger(String.valueOf(value))
                          : estilos.botonSm(String.valueOf(value));
        }
    }

    // Lógica: editor del botón por celda → llama al callback con el id del usuario
    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button;
        private final Consumer<Integer> onClick;

        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger) {
            this.table = table;
            this.onClick = onClick;
            this.button = danger ? estilos.botonSmDanger("eliminar")
                                 : estilos.botonSm("Editar");
            this.button.addActionListener(this::handle);
        }

        private void handle(ActionEvent e){
            int viewRow = table.getEditingRow();
            if (viewRow >= 0){
                int modelRow = table.convertRowIndexToModel(viewRow);
                Object idObj = table.getModel().getValueAt(modelRow, 0);
                int id = 0;
                try { id = Integer.parseInt(String.valueOf(idObj)); } catch (Exception ignore){}
                onClick.accept(id);
            }
            fireEditingStopped();
        }

        @Override public Object getCellEditorValue() { return null; }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            button.setText(String.valueOf(value)); // Visual: texto del botón en la celda
            return button;
        }
    }
}
