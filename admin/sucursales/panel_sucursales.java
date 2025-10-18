package admin.sucursales;

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

public class panel_sucursales extends JPanel {

    // lógica: Filtros de búsqueda
    private PlaceholderTextField txtBuscar;
    private JButton btnFiltrarFila, btnNueva, btnTransferir;

    // lógica: Tabla y modelo de datos
    private JTable tabla;
    private DefaultTableModel model;

    public panel_sucursales(){
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // visual: Panel shell con márgenes
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.PAGE_START;

        // visual: Card blanca principal
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                new EmptyBorder(16,16,18,16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // visual: Encabezado con título y botones
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Sucursales");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        // visual: Botones Nuevo y Transferir
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnNueva = estilos.botonRedondeado("+ Nueva Sucursal");
        btnNueva.setPreferredSize(new Dimension(220, 40));
        btnNueva.setMaximumSize(new Dimension(240, 40));
        btnTransferir = estilos.botonBlanco("Transferir productos");
        btnTransferir.setPreferredSize(new Dimension(180, 40));
        actions.add(btnNueva);
        actions.add(btnTransferir);
        head.add(actions, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(new EmptyBorder(0,0,8,0));

        // visual: Fila de filtros
        txtBuscar = new PlaceholderTextField("Buscar por nombre, dirección, email o teléfono…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(520, 40));
        txtBuscar.setMaximumSize(new Dimension(520, 40));

        btnFiltrarFila = estilos.botonBlanco("FILTRAR");
        btnFiltrarFila.setPreferredSize(new Dimension(120, 38));

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        filaFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy=0; g.insets=new Insets(6,0,6,8); g.fill=GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.weightx=1.0; filaFiltros.add(txtBuscar, g);
        g.gridx=1; g.weightx=0;   filaFiltros.add(btnFiltrarFila, g);

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

        // visual: Configuración de la tabla
        String colVentasMes = "Ventas ("+mesY()+")";
        String[] cols = {"ID","Nombre","Dirección","Contacto","Stock","Bajo stock", colVentasMes,"Ver","Editar","Transferir"};
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
        th.setBackground(new Color(0xFF,0xF3,0xD9));

        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setRowMargin(0);
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        // visual: Alineación por defecto
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // visual: Columna de ventas alineada a la derecha
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(6).setCellRenderer(right);

        // visual: Columna de ID con #
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setText("#"+String.valueOf(v));
                setHorizontalAlignment(SwingConstants.LEFT);
                return comp;
            }
        });

        // visual y lógica: Botones en la tabla (Ver, Editar, Transferir)
        tabla.getColumnModel().getColumn(7).setCellRenderer(new BtnCellRenderer(false));
        tabla.getColumnModel().getColumn(8).setCellRenderer(new BtnCellRenderer(false));
        tabla.getColumnModel().getColumn(9).setCellRenderer(new BtnCellRenderer(false));
        tabla.getColumnModel().getColumn(7).setCellEditor(new BtnCellEditor(tabla, id -> onVer(id), false));
        tabla.getColumnModel().getColumn(8).setCellEditor(new BtnCellEditor(tabla, id -> onEditar(id), false));
        tabla.getColumnModel().getColumn(9).setCellEditor(new BtnCellEditor(tabla, id -> onTransferir(id), false));

        // visual: Anchos de columna
        int[] widths = {80,220,260,220,90,110,140,80,80,110};
        for (int i=0;i<widths.length;i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        sc.setPreferredSize(new Dimension(0, 420));

        // visual: Ensamble del panel
        card.add(head);
        card.add(filtrosBox);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // lógica: Eventos
        btnFiltrarFila.addActionListener(e -> cargar());
        txtBuscar.addActionListener(e -> cargar());

        btnNueva.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            ver dlg = new ver(owner, 0, true);
            dlg.setVisible(true);
            if (dlg.huboCambios()) cargar();
        });
        btnTransferir.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            transferir dlg = new transferir(owner, 0);
            dlg.setVisible(true);
            if (dlg.fueOk()) cargar();
        });

        // lógica: Carga inicial de datos
        cargar();
    }

    // lógica: Carga los datos desde la base de datos
    private void cargar(){
        model.setRowCount(0);
        String q = txtBuscar.getText()==null? "" : txtBuscar.getText().trim();

        String where = "";
        List<Object> params = new ArrayList<>();
        if (!q.isEmpty()){
            where = " WHERE (s.nombre LIKE ? OR s.email LIKE ? OR s.direccion LIKE ? OR s.telefono LIKE ?)";
            for (int i=0;i<4;i++) params.add("%"+q+"%");
        }

        String sql = """
            SELECT
              s.id_sucursal, s.nombre, s.direccion, s.email, s.telefono, s.creado_en,
              COALESCE(SUM(i.stock_actual),0) AS stock_total,
              COALESCE(SUM(CASE WHEN i.stock_actual < i.stock_minimo THEN 1 ELSE 0 END),0) AS bajo_stock_items,
              (
                SELECT COALESCE(SUM(vd.cantidad*vd.precio_unitario),0)
                FROM venta v JOIN venta_detalle vd ON vd.id_venta=v.id_venta
                WHERE v.id_sucursal=s.id_sucursal
                  AND DATE_FORMAT(v.fecha_hora,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m')
              ) AS ventas_mes
            FROM sucursal s
            LEFT JOIN inventario i ON i.id_sucursal = s.id_sucursal
        """ + where + """
            GROUP BY s.id_sucursal, s.nombre, s.direccion, s.email, s.telefono, s.creado_en
            ORDER BY s.nombre
            LIMIT 300
        """;

        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)){

            for (int i=0;i<params.size();i++) ps.setString(i+1, String.valueOf(params.get(i)));

            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int id   = rs.getInt("id_sucursal");
                    String nombre = nn(rs.getString("nombre"));
                    String dir    = nn(rs.getString("direccion"));
                    String contacto = (nn(rs.getString("telefono"))+" / "+nn(rs.getString("email"))).replace("— / —","—");
                    int stock = rs.getInt("stock_total");
                    int bajo  = rs.getInt("bajo_stock_items");
                    double ventasMes = rs.getDouble("ventas_mes");

                    model.addRow(new Object[]{
                            id, nombre, dir, contacto,
                            stock, bajo, "$ "+nf2(ventasMes),
                            "Ver","Editar","Transferir"
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,
                    "Error cargando sucursales:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }

        if (model.getRowCount()==0){
            model.addRow(new Object[]{"—","Sin resultados.","","","","","","","",""});
        }
    }

    // lógica: Acciones para botones de la tabla
    private int idAt(int viewRow){
        if (viewRow<0) return -1;
        int mr = tabla.convertRowIndexToModel(viewRow);
        Object o = model.getValueAt(mr, 0);
        try { return Integer.parseInt(String.valueOf(o)); } catch(Exception ignore){ return -1; }
    }
    private void onVer(int id){
        if (id<=0) id = idAt(tabla.getEditingRow());
        if (id<=0) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        ver dlg = new ver(owner, id, false);
        dlg.setVisible(true);
    }
    private void onEditar(int id){
        if (id<=0) id = idAt(tabla.getEditingRow());
        if (id<=0) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        ver dlg = new ver(owner, id, true);
        dlg.setVisible(true);
        if (dlg.huboCambios()) cargar();
    }
    private void onTransferir(int id){
        if (id<=0) id = idAt(tabla.getEditingRow());
        Window owner = SwingUtilities.getWindowAncestor(this);
        transferir dlg = new transferir(owner, id);
        dlg.setVisible(true);
        if (dlg.fueOk()) cargar();
    }

    // lógica y visual: Renderers y editores para botones en la tabla
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
        BtnCellEditor(JTable table, Consumer<Integer> onClick, boolean danger){
            this.table=table; this.onClick=onClick;
            this.btn = danger? estilos.botonSmDanger("Eliminar") : estilos.botonSm("Ver");
            this.btn.addActionListener(this::go);
        }
        private void go(ActionEvent e){
            int vr = table.getEditingRow();
            int id = -1;
            if (vr>=0){
                int mr = table.convertRowIndexToModel(vr);
                Object o = table.getModel().getValueAt(mr, 0);
                try { id = Integer.parseInt(String.valueOf(o)); } catch(Exception ignore){}
            }
            onClick.accept(id);
            fireEditingStopped();
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            btn.setText(String.valueOf(v)); return btn;
        }
    }

    // lógica: Utilidades
    private static String nn(String s){ return (s==null||s.isBlank())?"—":s; }
    private static String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }
    private static String mesY(){
        String[] m={"enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"};
        java.util.Calendar c = java.util.Calendar.getInstance();
        return Character.toUpperCase(m[c.get(java.util.Calendar.MONTH)].charAt(0))
                + m[c.get(java.util.Calendar.MONTH)].substring(1)
                + " " + c.get(java.util.Calendar.YEAR);
    }

    // visual: Campo de texto con placeholder
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
