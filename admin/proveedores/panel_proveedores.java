package admin.proveedores;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class panel_proveedores extends JPanel {

    // Filtros
    private PlaceholderTextField txtBuscar;
    private JButton btnFiltrarFila, btnNuevo, btnReporte;

    // Tabla
    private JTable tabla;
    private DefaultTableModel model;

    public panel_proveedores() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // Shell con márgenes (igual a categorías)
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.PAGE_START;

        // Card blanca (igual)
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,18,16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        /* ====== Encabezado (título + acciones) ====== */
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Proveedores");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnNuevo = estilos.botonRedondeado("+ Nuevo Proveedor");
        btnNuevo.setPreferredSize(new Dimension(220, 40));
        btnNuevo.setMaximumSize(new Dimension(240, 40));
        btnReporte = estilos.botonBlanco("Reporte compras");
        btnReporte.setPreferredSize(new Dimension(160, 40));
        actions.add(btnNuevo);
        actions.add(btnReporte);
        head.add(actions, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(new EmptyBorder(0,0,8,0));

        /* ====== Fila de filtros (como Categorías) ====== */
        txtBuscar = new PlaceholderTextField("Buscar por nombre, email, teléfono o contacto…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));

        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.insets = new Insets(6, 0, 6, 8); g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.weightx = 1.0; filaFiltros.add(txtBuscar, g);
        g.gridx = 1; g.weightx = 0;   filaFiltros.add(btnFiltrarFila, g);

        JPanel filtrosBox = new JPanel(new BorderLayout());
        filtrosBox.setOpaque(true);
        filtrosBox.setBackground(new Color(0xF7,0xE9,0xD0));
        filtrosBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3), 1, true),
                new EmptyBorder(12,12,12,12)
        ));
        filtrosBox.add(filaFiltros, BorderLayout.CENTER);
        filtrosBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        filtrosBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        /* ====== Tabla (estética idéntica) ====== */
        String[] cols = {"Nombre","Contacto","Email","Teléfono","Dirección","Pedidos","Total comprado","Ver","Editar","Eliminar"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){ return c>=7; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex>=7 ? JButton.class : Object.class;
            }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));
        tabla.setRowHeight(32);

        JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 17));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF,0xF3,0xD9)); // crema igual

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        // Alineación por defecto a la izquierda
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // Total comprado alineado a derecha
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(6).setCellRenderer(right);

        // Botones
        tabla.getColumnModel().getColumn(7).setCellRenderer(new BtnCellRenderer(false));
        tabla.getColumnModel().getColumn(8).setCellRenderer(new BtnCellRenderer(false));
        tabla.getColumnModel().getColumn(9).setCellRenderer(new BtnCellRenderer(true));
        tabla.getColumnModel().getColumn(7).setCellEditor(new BtnCellEditor(tabla, id -> onVer(id), false, 0));
        tabla.getColumnModel().getColumn(8).setCellEditor(new BtnCellEditor(tabla, id -> onEditar(id), false, 0));
        tabla.getColumnModel().getColumn(9).setCellEditor(new BtnCellEditor(tabla, id -> onEliminar(id), true, 0));

        // Anchos orientativos (como en categorías)
        int[] widths = {220,160,220,120,220,90,140,70,70,90};
        for (int i=0;i<widths.length;i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420)); // alto como categorías

        // Ensamble
        card.add(head);
        card.add(filtrosBox);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        /* ====== Eventos ====== */
        btnFiltrarFila.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());

        btnNuevo.addActionListener(e -> {
            crear dlg = new crear(SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
            if (dlg.fueGuardado()) cargarTabla();
        });
        btnReporte.addActionListener(e -> new reportes(SwingUtilities.getWindowAncestor(this)).setVisible(true));

        /* ====== Carga inicial ====== */
        cargarTabla();
    }

    /* ================== Datos ================== */
    private void cargarTabla(){
        model.setRowCount(0);
        String q = txtBuscar.getText()==null ? "" : txtBuscar.getText().trim();

        String where=""; List<Object> params=new ArrayList<>();
        if(!q.isEmpty()){
            where = " WHERE (p.nombre LIKE ? OR p.email LIKE ? OR p.telefono LIKE ? OR p.contacto_referencia LIKE ?)";
            for(int i=0;i<4;i++) params.add("%"+q+"%");
        }
        String sql = """
            SELECT p.id_proveedor, p.nombre, p.email, p.telefono, p.direccion, p.contacto_referencia,
                   COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0) AS total_compras,
                   COUNT(DISTINCT pe.id_pedido) AS pedidos
            FROM proveedor p
            LEFT JOIN pedido pe ON pe.id_proveedor=p.id_proveedor
            LEFT JOIN pedido_detalle pd ON pd.id_pedido=pe.id_pedido
        """ + where + """
            GROUP BY p.id_proveedor, p.nombre, p.email, p.telefono, p.direccion, p.contacto_referencia
            ORDER BY p.nombre ASC
            LIMIT 300
        """;

        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i=0;i<params.size();i++) ps.setString(i+1, String.valueOf(params.get(i)));
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getString("nombre"),
                            nullToDash(rs.getString("contacto_referencia")),
                            nullToDash(rs.getString("email")),
                            nullToDash(rs.getString("telefono")),
                            nullToDash(rs.getString("direccion")),
                            rs.getInt("pedidos"),
                            "$ "+nf2(rs.getDouble("total_compras")),
                            "Ver","Editar","Eliminar"
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando proveedores:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }

        if (model.getRowCount()==0){
            model.addRow(new Object[]{"Sin resultados.","","","","","","","",""});
        }
    }

    /* ================== Utils ================== */
    private String nullToDash(String s){ return (s==null || s.isBlank()) ? "—" : s; }
    private String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }

    private int getIdAtRow(int viewRow){
        int r = tabla.convertRowIndexToModel(viewRow);
        String nombre = String.valueOf(model.getValueAt(r,0));
        String email  = String.valueOf(model.getValueAt(r,2));
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT id_proveedor FROM proveedor WHERE nombre=? AND (email=? OR (?='—' AND (email IS NULL OR email=''))) LIMIT 1")){
            ps.setString(1, nombre);
            ps.setString(2, email.equals("—")? "" : email);
            ps.setString(3, email);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) return rs.getInt(1);
            }
        } catch(Exception ignore){}
        return -1;
    }

    private void onVer(int id){
        if (id<=0) id = getIdAtRow(tabla.getEditingRow());
        if (id<=0) return;
        new ver(SwingUtilities.getWindowAncestor(this), id).setVisible(true);
    }
    private void onEditar(int id){
        if (id<=0) id = getIdAtRow(tabla.getEditingRow());
        if (id<=0) return;
        editar dlg = new editar(SwingUtilities.getWindowAncestor(this), id);
        dlg.setVisible(true);
        if (dlg.huboCambios()) cargarTabla();
    }
    private void onEliminar(int id){
        if (id<=0) id = getIdAtRow(tabla.getEditingRow());
        if (id<=0) return;

        try (Connection cn = conexion_bd.getConnection()){
            int c1 = getInt(cn, "SELECT COUNT(*) FROM pedido WHERE id_proveedor="+id);
            int c2 = getInt(cn, "SELECT COUNT(*) FROM producto WHERE id_proveedor="+id);
            if (c1>0 || c2>0){
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar: tiene registros vinculados (pedidos/productos).",
                        "Proveedores", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error validando vínculos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "¿Eliminar proveedor?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION){
            try (Connection cn = conexion_bd.getConnection();
                 PreparedStatement ps = cn.prepareStatement("DELETE FROM proveedor WHERE id_proveedor=?")){
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarTabla();
            } catch (Exception ex){
                JOptionPane.showMessageDialog(this, "Error eliminando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int getInt(Connection cn, String sql) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){ return rs.next()? rs.getInt(1):0; }
    }

    /* ====== Renderers / Editors ====== */
    static class BtnCellRenderer extends JButton implements TableCellRenderer {
        private final boolean danger;
        BtnCellRenderer(boolean danger){
            this.danger=danger;
            setOpaque(true); setBorderPainted(false); setFocusPainted(false);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            return danger ? estilos.botonSmDanger(String.valueOf(v))
                          : estilos.botonSm(String.valueOf(v));
        }
    }
    static class BtnCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table; private final JButton btn; private final Consumer<Integer> onClick;
        BtnCellEditor(JTable table, Consumer<Integer> onClick, boolean danger, int idCol){
            this.table=table; this.onClick=onClick;
            this.btn = danger? estilos.botonSmDanger("Eliminar") : estilos.botonSm("Ver");
            this.btn.addActionListener(this::go);
        }
        private void go(ActionEvent e){
            int vr = table.getEditingRow();
            onClick.accept(vr>=0? vr : -1);
            fireEditingStopped();
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            btn.setText(String.valueOf(v)); return btn;
        }
    }

    /* ====== Placeholder igual al de categorías ====== */
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder){
            this.placeholder = placeholder;
            setFont(new Font("Arial", Font.PLAIN, 14));
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(155,142,127));
                g2.setFont(getFont());
                Insets in=getInsets();
                int x=in.left+4;
                int y=getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2;
                g2.drawString(placeholder, x, y);
                g2.dispose();
            }
        }
    }
}
