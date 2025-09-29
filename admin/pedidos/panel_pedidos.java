package admin.pedidos;

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
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class panel_pedidos extends JPanel {

    /* ====== UI ====== */
    // KPIs
    private JLabel kpiBorrador, kpiPendientes, kpiRecibidosMes, kpiMontoMes;

    // Chips de estado (0=Todos, 1=Borrador, 2=Aprobado, 3=Enviado, 4=Recibido, 5=Cancelado)
    private final Map<Integer, JToggleButton> chipMap = new LinkedHashMap<>();
    private int estadoSel = 0;

    // Filtros
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbProveedor;
    private JComboBox<Item> cbSucursal;
    private JTextField tfDesde, tfHasta;
    private JButton btnFiltrar;

    // Acciones
    private JButton btnNuevo;

    // Tabla
    private JTable tabla;
    private DefaultTableModel model;

    // Catálogos (para combos)
    private List<Item> proveedores = new ArrayList<>();
    private List<Item> sucursales  = new ArrayList<>();

    // Estados (id → etiqueta)
    private static final Map<Integer, String> ESTADOS = Map.of(
            1, "Borrador", 2, "Aprobado", 3, "Enviado", 4, "Recibido", 5, "Cancelado"
    );

    public panel_pedidos() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.PAGE_START;

        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        /* ====== Header ====== */
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel h1 = new JLabel("Pedidos");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        header.add(h1, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnNuevo = estilos.botonRedondeado("+ Nuevo Pedido");
        btnNuevo.setPreferredSize(new Dimension(180, 40));
        actions.add(btnNuevo);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        card.add(header);

        /* ====== KPIs (4) ====== */
        card.add(kpisFila());

        /* ====== Chips de estado ====== */
        card.add(chipsFila());

        /* ====== Barra de filtros ====== */
        card.add(filtrosFila());

        /* ====== Tabla ====== */
        String[] cols = {"ID", "Proveedor", "Sucursal", "Fecha", "Estado", "Renglones", "Monto total", "ver"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){ return c==7; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex==7 ? JButton.class : Object.class;
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
        tabla.setIntercellSpacing(new Dimension(0,1));
        tabla.setRowMargin(0);

        // Anchos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(80);

        // Render ID tipo "#4"
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#"+String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Estado como badge
        tabla.getColumnModel().getColumn(4).setCellRenderer(new EstadoBadgeRenderer());
        // Monto total alineado derecha
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(6).setCellRenderer(right);

        // Botón Ver
        tabla.getColumnModel().getColumn(7).setCellRenderer(new ButtonCellRenderer(false));
        tabla.getColumnModel().getColumn(7).setCellEditor(new ButtonCellEditor(tabla, id -> onVer(id), false));

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        // === FIX: altura visible del scroll/tabla para que no quede colapsada ===
        sc.setPreferredSize(new Dimension(0, 420));
        sc.setMinimumSize(new Dimension(0, 300));
        // ======================================================================

        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        /* ====== Eventos ====== */
        btnNuevo.addActionListener(e -> onNuevo());
        btnFiltrar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());

        // Chips
        chipMap.forEach((k,btn)-> btn.addActionListener(e -> {
            estadoSel = k;
            marcarChipActivo(k);
            cargarTabla();
        }));

        /* ====== Carga inicial ====== */
        tfHasta.setText(LocalDate.now().toString());
        tfDesde.setText(LocalDate.now().minusDays(30).toString());
        cargarCombos();
        cargarKPIs();
        cargarTabla();
    }

    /* ====== Secciones UI helpers ====== */

    private JPanel kpisFila() {
        JPanel row = new JPanel(new GridLayout(1,4,16,16));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));

        kpiBorrador     = kpiBig();
        kpiPendientes   = kpiBig();
        kpiRecibidosMes = kpiBig();
        kpiMontoMes     = kpiBig();

        row.add(kpiCard("Borradores", kpiBorrador, "Pedidos en estado borrador"));
        row.add(kpiCard("Pendientes de recibir", kpiPendientes, "Aprobado o Enviado"));
        row.add(kpiCard("Recibidos este mes", kpiRecibidosMes, mesY(LocalDate.now())));
        row.add(kpiCard("Monto del mes", kpiMontoMes, "Total de pedidos creados"));
        return row;
    }

    // ====== CAMBIO: chips sin estilos.chip() ======
    private JPanel chipsFila() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);

        String[] labels = {"Todos","Borrador","Aprobado","Enviado","Recibido","Cancelado"};
        for (int i=0;i<labels.length;i++){
            JToggleButton b = makeChip(labels[i]); // <- chip local (sin estilos.*)
            chipMap.put(i, b);
            chips.add(b);
        }
        marcarChipActivo(0);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel("Pedidos");
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setForeground(estilos.COLOR_TITULO);
        head.add(h, BorderLayout.WEST);
        wrap.add(head, BorderLayout.NORTH);

        JPanel pad = new JPanel(new BorderLayout());
        pad.setOpaque(false);
        pad.setBorder(new EmptyBorder(8,0,8,0));
        pad.add(chips, BorderLayout.WEST);
        wrap.add(pad, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel filtrosFila() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(true);
        box.setBackground(new Color(0xF7,0xE9,0xD0));
        box.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3), 1, true),
                new EmptyBorder(12,12,12,12)
        ));

        txtBuscar = new PlaceholderTextField("Buscar pedido / proveedor…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(260,38));

        cbProveedor = new JComboBox<>();
        estilos.estilizarCombo(cbProveedor);
        cbProveedor.setPreferredSize(new Dimension(220,38));

        cbSucursal = new JComboBox<>();
        estilos.estilizarCombo(cbSucursal);
        cbSucursal.setPreferredSize(new Dimension(200,38));

        tfDesde = new JTextField();
        tfHasta = new JTextField();
        estilizarFecha(tfDesde); estilizarFecha(tfHasta);
        tfDesde.setPreferredSize(new Dimension(170,38));
        tfHasta.setPreferredSize(new Dimension(170,38));

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120,38));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy=0; g.insets=new Insets(4,4,4,8); g.fill=GridBagConstraints.HORIZONTAL;

        int x=0;
        g.gridx=x++; g.weightx=1;  box.add(txtBuscar, g);
        g.gridx=x++; g.weightx=0;  box.add(cbProveedor, g);
        g.gridx=x++;               box.add(cbSucursal, g);
        g.gridx=x++;               box.add(tfDesde, g);
        g.gridx=x++;               box.add(tfHasta, g);
        g.gridx=x;                 box.add(btnFiltrar, g);

        row.add(box, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(0,0,12,0));
        return row;
    }

    private JPanel kpiCard(String titulo, JLabel big, String sub){
        JPanel p = cardInner();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(titulo);
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setForeground(estilos.COLOR_TITULO);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subLb = new JLabel(sub);
        subLb.setForeground(new Color(110,110,110));
        subLb.setAlignmentX(Component.LEFT_ALIGNMENT);
        big.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(h);
        p.add(big);
        p.add(Box.createVerticalStrut(4));
        p.add(subLb);
        return p;
    }

    private JLabel kpiBig(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.BOLD, 26));
        l.setForeground(new Color(50,50,50));
        return l;
    }

    private JPanel cardShell(){
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,18,16)
        ));
        return p;
    }
    private JPanel cardInner(){
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,16,16)
        ));
        return p;
    }

    private void estilizarFecha(JTextField f){
        f.setFont(new Font("Arial", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true),
                new EmptyBorder(8,12,8,12)
        ));
        f.setBackground(Color.WHITE);
        f.setToolTipText("AAAA-MM-DD");
    }

    // ====== helpers locales para chips ======
    private JToggleButton makeChip(String text) {
        JToggleButton b = new JToggleButton(text);
        styleChip(b);
        return b;
    }
    private void styleChip(JToggleButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setFont(new Font("Arial", Font.PLAIN, 14));
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        b.setBackground(new Color(0xF7,0xE9,0xD0));
        b.setForeground(new Color(0x33,0x33,0x33));
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setBackground(new Color(0xFF,0xF3,0xD9));
                b.setBorder(new CompoundBorder(
                        new LineBorder(new Color(0xF1,0xD5,0xA3), 1, true),
                        new EmptyBorder(6, 12, 6, 12)
                ));
            } else {
                b.setBackground(new Color(0xF7,0xE9,0xD0));
                b.setBorder(new CompoundBorder(
                        new LineBorder(new Color(0xD8,0xC3,0xA3), 1, true),
                        new EmptyBorder(6, 12, 6, 12)
                ));
            }
        });
    }

    private void marcarChipActivo(int key){
        ButtonGroup bg = new ButtonGroup();
        chipMap.forEach((k,b)->{
            bg.add(b);
            b.setSelected(k==key);
        });
    }

    /* ====== Acciones ====== */

    private void onNuevo(){
        Window owner = SwingUtilities.getWindowAncestor(this);
        crear dlg = new crear(owner); // implementar en este paquete
        dlg.setVisible(true);
        if (dlg.fueGuardado()) {
            cargarKPIs();
            cargarTabla();
        }
    }

    private void onVer(int idPedido){
        Window owner = SwingUtilities.getWindowAncestor(this);
        ver dlg = new ver(owner, idPedido); // implementar en este paquete
        dlg.setVisible(true);
        if (dlg.huboCambios()){
            cargarKPIs();
            cargarTabla();
        }
    }

    /* ====== Carga de datos ====== */

    private void cargarCombos() {
        proveedores.clear(); sucursales.clear();
        proveedores.add(new Item(0, "Todos los proveedores"));
        sucursales.add(new Item(0, "Todas las sucursales"));

        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_proveedor, nombre FROM proveedor ORDER BY nombre")){
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) proveedores.add(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_sucursal, nombre FROM sucursal ORDER BY nombre")){
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) sucursales.add(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }

        cbProveedor.setModel(new DefaultComboBoxModel<>(proveedores.toArray(new Item[0])));
        cbSucursal.setModel(new DefaultComboBoxModel<>(sucursales.toArray(new Item[0])));
    }

    private void cargarKPIs(){
        int borr = 0, pend = 0, recMes = 0;
        double montoMes = 0.0;
        try (Connection cn = DB.get()){
            borr = getInt(cn, "SELECT COUNT(*) FROM pedido WHERE id_estado_pedido=1");
            pend = getInt(cn, "SELECT COUNT(*) FROM pedido WHERE id_estado_pedido IN (2,3)");
            recMes = getInt(cn, """
                SELECT COUNT(*) FROM pedido
                WHERE id_estado_pedido=4
                  AND DATE_FORMAT(fecha_estado,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m')
            """);
            montoMes = getDouble(cn, """
                SELECT COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0)
                FROM pedido p
                JOIN pedido_detalle pd ON pd.id_pedido=p.id_pedido
                WHERE DATE_FORMAT(p.fecha_creado,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m')
            """);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error KPIs:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }

        kpiBorrador.setText(nf0(borr));
        kpiPendientes.setText(nf0(pend));
        kpiRecibidosMes.setText(nf0(recMes));
        kpiMontoMes.setText("$ " + nf2(montoMes));
    }

    private void cargarTabla() {
        model.setRowCount(0);

        String q = txtBuscar.getText()==null ? "" : txtBuscar.getText().trim();
        int idProv = ((Item)cbProveedor.getSelectedItem()).id();
        int idSuc  = ((Item)cbSucursal.getSelectedItem()).id();
        String desde = tfDesde.getText().trim();
        String hasta = tfHasta.getText().trim();

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (!q.isEmpty()){
            addWhere(where, "(pr.nombre LIKE ? OR p.id_pedido LIKE ?)");
            params.add("%"+q+"%");
            params.add("%"+q+"%");
        }
        if (idProv>0){ addWhere(where, "p.id_proveedor=?"); params.add(idProv); }
        if (idSuc>0) { addWhere(where, "p.id_sucursal=?");  params.add(idSuc);  }
        if (estadoSel>0){ addWhere(where, "p.id_estado_pedido=?"); params.add(estadoSel); }
        if (!desde.isEmpty()){ addWhere(where, "DATE(p.fecha_creado)>=?"); params.add(desde); }
        if (!hasta.isEmpty()){ addWhere(where, "DATE(p.fecha_creado)<=?"); params.add(hasta); }

        String sql = """
            SELECT p.id_pedido, p.fecha_creado, p.id_estado_pedido,
                   pr.nombre AS proveedor, s.nombre AS sucursal,
                   COUNT(DISTINCT pd.id_pedido_detalle) AS renglones,
                   COALESCE(SUM(pd.cantidad_solicitada*pd.precio_unitario),0) AS monto_total
            FROM pedido p
            JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
            JOIN sucursal  s  ON s.id_sucursal=p.id_sucursal
            LEFT JOIN pedido_detalle pd ON pd.id_pedido=p.id_pedido
        """ + where + """
            GROUP BY p.id_pedido, p.fecha_creado, p.id_estado_pedido, proveedor, sucursal
            ORDER BY p.fecha_creado DESC
            LIMIT 200
        """;

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int id         = rs.getInt("id_pedido");
                    String prov    = rs.getString("proveedor");
                    String suc     = rs.getString("sucursal");
                    String fecha   = rs.getString("fecha_creado");
                    int idEstado   = rs.getInt("id_estado_pedido");
                    int renglones  = rs.getInt("renglones");
                    double monto   = rs.getDouble("monto_total");
                    model.addRow(new Object[]{
                            id, prov, suc, fecha,
                            ESTADOS.getOrDefault(idEstado,"—"),
                            renglones,
                            "$ " + nf2(monto),
                            "Ver"
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando pedidos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }

        if (model.getRowCount()==0){
            model.addRow(new Object[]{"", "Sin resultados.", "", "", "", "", "", ""});
        }
    }

    /* ====== Utils ====== */

    private void addWhere(StringBuilder w, String cond){
        w.append(w.length()==0 ? " WHERE " : " AND ").append(cond);
    }
    private void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i=0;i<params.size();i++){
            Object v=params.get(i);
            if (v instanceof Integer iv) ps.setInt(i+1, iv);
            else if (v instanceof Double dv) ps.setDouble(i+1, dv);
            else ps.setString(i+1, String.valueOf(v));
        }
    }

    private String nf0(int n){ return String.format("%,d", n).replace(',', '.'); }
    private String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }
    private String mesY(LocalDate d){
        String[] m = {"Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio",
                "Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        return m[d.getMonthValue()-1]+" "+d.getYear();
    }

    /* ====== Renderers y celdas ====== */

    static class EstadoBadgeRenderer implements TableCellRenderer {
        private final PillLabel lbl = new PillLabel();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String txt = String.valueOf(value);
            Color bg=estilos.BADGE_NO_BG, bd=estilos.BADGE_NO_BORDER, fg=estilos.BADGE_NO_FG;
            if ("Aprobado".equalsIgnoreCase(txt)) { bg=estilos.BADGE_OK_BG; bd=estilos.BADGE_OK_BORDER; fg=estilos.BADGE_OK_FG; }
            else if ("Enviado".equalsIgnoreCase(txt)) { bg=new Color(0xD6,0xEA,0xFE); bd=new Color(0xAE,0xD8,0xFB); fg=new Color(0x1F,0x5F,0xA6); }
            else if ("Recibido".equalsIgnoreCase(txt)) { bg=new Color(0xDA,0xF7,0xE0); bd=new Color(0xB7,0xEE,0xC3); fg=new Color(0x23,0x7A,0x36); }
            else if ("Cancelado".equalsIgnoreCase(txt)) { bg=new Color(0xF9,0xDF,0xDE); bd=new Color(0xF4,0xC7,0xC6); fg=new Color(0xB9,0x4A,0x48); }
            else if ("Borrador".equalsIgnoreCase(txt)) { bg=new Color(0xEE,0xF0,0xF3); bd=new Color(0xDD,0xE2,0xE8); fg=new Color(0x55,0x66,0x77); }
            lbl.configure(txt, bg, bd, fg);
            lbl.setSelection(isSelected);
            return lbl;
        }
    }

    static class PillLabel extends JComponent {
        private String text=""; private Color bg=Color.LIGHT_GRAY,border=Color.GRAY,fg=Color.BLACK;
        private boolean selected=false;
        void configure(String t, Color bg, Color border, Color fg){
            this.text=t; this.bg=bg; this.border=border; this.fg=fg;
            setPreferredSize(new Dimension(92,22));
        }
        void setSelection(boolean b){ this.selected=b; }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight(), arc=h;
            g2.setColor(selected?new Color(bg.getRed(),bg.getGreen(),bg.getBlue(),230):bg);
            g2.fillRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(border);
            g2.drawRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(Font.BOLD,12f));
            FontMetrics fm=g2.getFontMetrics();
            int tw=fm.stringWidth(text), tx=(w-tw)/2, ty=h/2+fm.getAscent()/2-3;
            g2.drawString(text, Math.max(8,tx), ty);
            g2.dispose();
        }
    }

    static class ButtonCellRenderer extends JButton implements TableCellRenderer {
        private final boolean danger;
        ButtonCellRenderer(boolean danger){
            this.danger = danger;
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return danger ? estilos.botonSmDanger(String.valueOf(value))
                          : estilos.botonSm(String.valueOf(value));
        }
    }
    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table; private final JButton button; private final Consumer<Integer> onClick;
        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger){
            this.table=table; this.onClick=onClick;
            this.button = danger? estilos.botonSmDanger("eliminar") : estilos.botonSm("Ver");
            this.button.addActionListener(this::handle);
        }
        private void handle(ActionEvent e){
            int viewRow = table.getEditingRow();
            if (viewRow>=0){
                int modelRow = table.convertRowIndexToModel(viewRow);
                Object idObj = table.getModel().getValueAt(modelRow, 0);
                int id=0; try{ id=Integer.parseInt(String.valueOf(idObj)); }catch(Exception ignore){}
                onClick.accept(id);
            }
            fireEditingStopped();
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText(String.valueOf(value));
            return button;
        }
    }

    /* ====== DB helpers ====== */

    // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
    private static int getInt(Connection cn, String sql) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { return rs.next()? rs.getInt(1):0; }
    }
    private static double getDouble(Connection cn, String sql) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { return rs.next()? rs.getDouble(1):0.0; }
    }

    /* ====== Tipos auxiliares ====== */

    static class Item {
        private final int id; private final String nombre;
        Item(int id, String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        @Override public String toString(){ return nombre; }
    }

    /* ====== Inputs con placeholder ====== */

    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder){
            this.placeholder=placeholder;
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
