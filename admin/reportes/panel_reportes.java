package admin.reportes;

import includes.conexion_bd;
import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Panel de Reportes y Estadísticas — Los Lapicitos
 * Ajustado para que todo el contenido quede dentro de la pantalla
 * sin scroll horizontal y con alturas/anchos controlados.
 */
public class panel_reportes extends JPanel {

    /* ====== Constantes de layout (reducidas) ====== */
    private static final int PAD_OUTER_LEFT  = 12;
    private static final int PAD_OUTER_OTHER = 10;
    private static final int PAD_CARD = 10;
    private static final int PAD_CARD_TOP = 6;
    private static final int FONT_BASE = 13;
    private static final int ROW_H = 24;
    private static final int ROW_H_HDR = 26;

    /* ====== Filtros ====== */
    private JTextField tfDesde, tfHasta;
    private JComboBox<Item> cbSucursal, cbCategoria, cbProveedor;
    private JComboBox<String> cbGran;
    private JButton btnFiltrar;
    private JLabel tagRango;

    /* ====== KPIs ====== */
    private JLabel kpiVentas, kpiTickets, kpiUnidades, kpiMargen;

    /* ====== Serie + Chart ====== */
    private DefaultTableModel modelSerie;
    private LineChartPanel chart;

    /* ====== Modelos de tablas ====== */
    private DefaultTableModel mCat, mProv, mSuc, mTop, mNoSale, mRot;

    /* ====== Catálogos ====== */
    private final List<Item> sucursales = new ArrayList<>();
    private final List<Item> categorias = new ArrayList<>();
    private final List<Item> proveedores = new ArrayList<>();

    public panel_reportes(){
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        /* ====== Contenedor principal (solo scroll vertical) ====== */
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(
                0, PAD_OUTER_LEFT, PAD_OUTER_OTHER, PAD_OUTER_OTHER));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH;
        gbc.anchor=GridBagConstraints.PAGE_START;

        JPanel card = cardShell();
        card.setLayout(new BorderLayout(0, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* ====== Stack superior ====== */
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(header());
        stack.add(filtrosFila());       // ← ahora en 2 filas
        stack.add(kpisGridCompact());   // ← 2x2 compacto
        stack.add(boxChart());
        card.add(stack, BorderLayout.NORTH);

        /* ====== Centro: serie + tablas ====== */
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] cols = {"Período","Ventas","Tickets","Unidades"};
        modelSerie = new DefaultTableModel(cols,0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable tablaSerie = makeTable(modelSerie);
        JScrollPane scSerie = scrollForTable(tablaSerie);
        scSerie.setPreferredSize(new Dimension(10, 180));
        center.add(boxSection("Serie del período", scSerie));

        center.add(boxTabla("Ventas por categoría",
                makeTable(mCat = new DefaultTableModel(
                        new String[]{"Categoría","Unidades","Importe"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        center.add(boxTabla("Ventas por proveedor",
                makeTable(mProv = new DefaultTableModel(
                        new String[]{"Proveedor","Unidades","Importe","Margen"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        center.add(boxTabla("Ventas por sucursal",
                makeTable(mSuc = new DefaultTableModel(
                        new String[]{"Sucursal","Tickets","Unidades","Importe"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        center.add(boxTabla("Top productos vendidos",
                makeTable(mTop = new DefaultTableModel(
                        new String[]{"ID","Producto","Unidades","Importe"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        center.add(boxTabla("Productos sin ventas (período)",
                makeTable(mNoSale = new DefaultTableModel(
                        new String[]{"ID","Producto","Stock actual"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        center.add(boxTabla("Rotación de productos (rápidos ↔ lentos)",
                makeTable(mRot = new DefaultTableModel(
                        new String[]{"ID","Producto","Vendidas","Stock actual","Importe"}, 0) {
                    @Override public boolean isCellEditable(int r,int c){return false;}
                })));

        card.add(center, BorderLayout.CENTER);

        content.add(card, gbc);

        JScrollPane scroller = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER  // ← SIN scroll horizontal del panel
        );
        scroller.setBorder(null);
        scroller.setViewportBorder(null);
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);

        add(scroller, BorderLayout.CENTER);

        /* ====== Eventos ====== */
        btnFiltrar.addActionListener(e -> recargarTodo());

        /* ====== Inicial ====== */
        tfHasta.setText(LocalDate.now().toString());
        tfDesde.setText(LocalDate.now().minusDays(14).toString());
        cbGran.setSelectedItem("Día");
        cargarCombos();
        recargarTodo();
    }

    /* ======================= Secciones UI ======================= */

    private JPanel header(){
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel h1 = new JLabel("Reportes y estadísticas");
        h1.setFont(new Font("Arial", Font.BOLD, 16));
        h1.setForeground(estilos.COLOR_TITULO);
        header.add(h1, BorderLayout.WEST);
        header.setBorder(new EmptyBorder(0,0,2,0));
        return header;
    }

    private JPanel boxChart(){
        JPanel wrap = cardInner();
        wrap.setLayout(new BorderLayout());
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200)); // más compacto

        JLabel h = new JLabel("Evolución (diaria/semanal)");
        h.setFont(new Font("Arial", Font.BOLD, 15));
        h.setForeground(estilos.COLOR_TITULO);
        h.setBorder(new EmptyBorder(0,0,4,0));
        wrap.add(h, BorderLayout.NORTH);

        chart = new LineChartPanel();
        chart.setPreferredSize(new Dimension(0, 170));
        chart.setMinimumSize(new Dimension(0, 160));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        chart.setBorder(new EmptyBorder(4,0,4,0));
        wrap.add(chart, BorderLayout.CENTER);

        return wrap;
    }

    /** Filtros en DOS filas para que no desborden */
    private JPanel filtrosFila(){
        JPanel wrap = cardInner();
        wrap.setLayout(new BorderLayout());
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(true);
        box.setBackground(new Color(0xF7,0xE9,0xD0));
        box.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3), 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.insets=new Insets(2,2,2,6);
        g.fill=GridBagConstraints.HORIZONTAL;
        g.weighty=0;

        tfDesde = new JTextField(); estilizarFecha(tfDesde);
        tfHasta = new JTextField(); estilizarFecha(tfHasta);

        cbSucursal = new JComboBox<>();
        cbCategoria = new JComboBox<>();
        cbProveedor = new JComboBox<>();
        estilizarCombo(cbSucursal); estilizarCombo(cbCategoria); estilizarCombo(cbProveedor);

        cbGran = new JComboBox<>(new String[]{"Día","Semana"});
        estilizarCombo(cbGran);

        btnFiltrar = estilos.botonBlanco("APLICAR");
        btnFiltrar.setPreferredSize(new Dimension(90,28));
        btnFiltrar.setFont(new Font("Arial", Font.BOLD, FONT_BASE-1));

        // ---- fila 1
        int y=0, x=0;
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Desde"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.15; box.add(tfDesde, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Hasta"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.15; box.add(tfHasta, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Sucursal"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbSucursal, g);

        // ---- fila 2
        y++; x=0;
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Categoría"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbCategoria, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Proveedor"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbProveedor, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Granularidad"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.18; box.add(cbGran, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(btnFiltrar, g);

        wrap.add(box, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        south.setOpaque(false);
        tagRango = new JLabel("");
        tagRango.setFont(new Font("Arial", Font.PLAIN, FONT_BASE-2));
        tagRango.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3),1,true),
                new EmptyBorder(2,6,2,6)
        ));
        south.add(tagRango);
        wrap.add(south, BorderLayout.SOUTH);

        return wrap;
    }

    private JLabel lbl(String s){
        JLabel l = new JLabel(s);
        l.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        return l;
    }

    /** KPIs compactos en 2x2 */
    private JPanel kpisGridCompact(){
        JPanel row = new JPanel(new GridLayout(2,2,8,8));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        kpiVentas   = kpiBig();
        kpiTickets  = kpiBig();
        kpiUnidades = kpiBig();
        kpiMargen   = kpiBig();

        row.add(kpiCard("Ventas", kpiVentas, ""));
        row.add(kpiCard("Tickets", kpiTickets, ""));
        row.add(kpiCard("Unidades", kpiUnidades, ""));
        row.add(kpiCard("Margen (aprox.)", kpiMargen, ""));
        return row;
    }

    private JPanel boxSection(String titulo, JComponent content){
        JPanel wrap = cardInner();
        wrap.setLayout(new BorderLayout());
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel h = new JLabel(titulo);
        h.setFont(new Font("Arial", Font.BOLD, 15));
        h.setForeground(estilos.COLOR_TITULO);
        h.setBorder(new EmptyBorder(0,0,4,0));
        wrap.add(h, BorderLayout.NORTH);
        wrap.add(content, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel boxTabla(String titulo, JTable table){
        return boxSection(titulo, scrollForTable(table));
    }

    private JTable makeTable(DefaultTableModel model){
        JTable t = new JTable(model);
        t.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        t.setRowHeight(ROW_H);
        JTableHeader th = t.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, FONT_BASE));
        th.setPreferredSize(new Dimension(10, ROW_H_HDR));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF,0xF3,0xD9));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEDE3D2));
        t.setIntercellSpacing(new Dimension(0,1));
        t.setRowMargin(0);

        // Mantener visible y ajustado al ancho
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c=0;c<model.getColumnCount();c++){
            String name = model.getColumnName(c).toLowerCase(Locale.ROOT);
            if (name.contains("importe") || name.contains("margen") || name.contains("unidades")
                    || name.contains("tickets") || name.contains("vendidas") || name.contains("stock")){
                t.getColumnModel().getColumn(c).setCellRenderer(right);
            }
        }

        // Anchos preferidos suaves
        for (int c = 0; c < model.getColumnCount(); c++) {
            String name = model.getColumnName(c).toLowerCase(Locale.ROOT);
            int w = switch (name) {
                case "id", "tickets", "unidades", "vendidas", "stock actual" -> 80;
                case "período", "periodo" -> 100;
                case "producto", "proveedor", "categoría", "categoria", "sucursal" -> 200;
                case "ventas", "importe", "margen" -> 120;
                default -> 110;
            };
            t.getColumnModel().getColumn(c).setPreferredWidth(w);
            t.getColumnModel().getColumn(c).setMinWidth(60);
        }
        return t;
    }

    private JScrollPane scrollForTable(JTable t){
        JScrollPane sc = new JScrollPane(
                t,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(3,3,3,3)
        ));
        sc.getViewport().setBackground(Color.WHITE);
        sc.setPreferredSize(new Dimension(0, 180));
        sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        sc.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sc;
    }

    private JPanel cardShell(){
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(PAD_CARD_TOP, PAD_CARD, PAD_CARD, PAD_CARD)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
    private JPanel cardInner(){
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(PAD_CARD_TOP, PAD_CARD, PAD_CARD, PAD_CARD)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel kpiBig(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.BOLD, 20));
        l.setForeground(new Color(50,50,50));
        return l;
    }
    private JPanel kpiCard(String titulo, JLabel big, String sub){
        JPanel p = cardInner();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel h = new JLabel(titulo);
        h.setFont(new Font("Arial", Font.BOLD, 14));
        h.setForeground(estilos.COLOR_TITULO);
        JLabel s = new JLabel(sub);
        s.setFont(new Font("Arial", Font.PLAIN, FONT_BASE-2));
        s.setForeground(new Color(110,110,110));
        p.add(h); p.add(Box.createVerticalStrut(2)); p.add(big); p.add(Box.createVerticalStrut(2)); p.add(s);
        return p;
    }

    private void estilizarFecha(JTextField f){
        f.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true),
                new EmptyBorder(5,8,5,8)
        ));
        f.setBackground(Color.WHITE);
        f.setToolTipText("AAAA-MM-DD");
        f.setPreferredSize(new Dimension(110, 28));
    }
    private void estilizarCombo(JComboBox<?> cb){
        cb.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        cb.setBorder(new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true));
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(140, 28));
        cb.setMaximumRowCount(15);
    }

    /* ======================= Carga de datos ======================= */

    private void recargarTodo(){
        String desde = tfDesde.getText().trim();
        String hasta = tfHasta.getText().trim();
        Item itSuc = (Item) cbSucursal.getSelectedItem();
        Item itCat = (Item) cbCategoria.getSelectedItem();
        Item itProv = (Item) cbProveedor.getSelectedItem();
        int idSuc = itSuc == null ? 0 : itSuc.id();
        int idCat = itCat == null ? 0 : itCat.id();
        int idProv = itProv == null ? 0 : itProv.id();
        String gran = "Semana".equals(cbGran.getSelectedItem()) ? "semana" : "dia";

        tagRango.setText(desde + " → " + hasta);

        cargarKPIs(desde, hasta, idSuc);
        cargarSerie(desde, hasta, idSuc, gran);
        cargarCat(desde, hasta, idSuc, idCat, idProv);
        cargarProv(desde, hasta, idSuc, idCat, idProv);
        cargarSuc(desde, hasta, idSuc);
        cargarTop(desde, hasta, idSuc, idCat, idProv);
        cargarNoSale(desde, hasta, idSuc, idCat, idProv);
        cargarRot(desde, hasta, idSuc, idCat, idProv);
    }

    private void cargarCombos(){
        sucursales.clear(); categorias.clear(); proveedores.clear();
        sucursales.add(new Item(0,"Todas las sucursales"));
        categorias.add(new Item(0,"Todas las categorías"));
        proveedores.add(new Item(0,"Todos los proveedores"));
        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_sucursal, nombre FROM sucursal ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) sucursales.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) categorias.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor, nombre FROM proveedor ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) proveedores.add(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
        cbSucursal.setModel(new DefaultComboBoxModel<>(sucursales.toArray(new Item[0])));
        cbCategoria.setModel(new DefaultComboBoxModel<>(categorias.toArray(new Item[0])));
        cbProveedor.setModel(new DefaultComboBoxModel<>(proveedores.toArray(new Item[0])));
    }

    private void cargarKPIs(String desde, String hasta, int idSuc){
        String where = " WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')";
        if (idSuc > 0) where += " AND v.id_sucursal=?";

        String sql = """
            SELECT
              COALESCE(SUM(v.total),0) AS ventas,
              COUNT(DISTINCT v.id_venta) AS tickets,
              COALESCE(SUM(vd.cantidad),0) AS unidades,
              COALESCE(SUM((vd.precio_unitario - p.precio_compra)*vd.cantidad),0) AS margen
            FROM venta v
            LEFT JOIN venta_detalle vd ON vd.id_venta=v.id_venta
            LEFT JOIN producto p ON p.id_producto=vd.id_producto
        """ + where;

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setString(1, desde); ps.setString(2, hasta);
            if (idSuc>0) ps.setInt(3, idSuc);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    kpiVentas.setText("$ " + nf2(rs.getDouble("ventas")));
                    kpiTickets.setText(nf0(rs.getInt("tickets")));
                    kpiUnidades.setText(nf0(rs.getInt("unidades")));
                    kpiMargen.setText("$ " + nf2(rs.getDouble("margen")));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error KPIs:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarSerie(String desde, String hasta, int idSuc, String gran){
        modelSerie.setRowCount(0);

        String selFecha, groupBy;
        if ("semana".equals(gran)){
            selFecha = "YEARWEEK(v.fecha_hora, 3) AS grp, DATE_FORMAT(MIN(v.fecha_hora),'%Y-%m-%d') AS etiqueta";
            groupBy  = "YEARWEEK(v.fecha_hora, 3)";
        } else {
            selFecha = "DATE(v.fecha_hora) AS grp, DATE(v.fecha_hora) AS etiqueta";
            groupBy  = "DATE(v.fecha_hora)";
        }
        String where = " WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')";
        if (idSuc>0) where += " AND v.id_sucursal=?";

        String sql = """
            SELECT %s,
                   COALESCE(SUM(v.total),0) AS ventas,
                   COUNT(DISTINCT v.id_venta) AS tickets,
                   COALESCE(SUM(vd.cantidad),0) AS unidades
            FROM venta v
            LEFT JOIN venta_detalle vd ON vd.id_venta=v.id_venta
        """.formatted(selFecha) + where + """
            GROUP BY %s
            ORDER BY etiqueta ASC
        """.formatted(groupBy);

        List<PuntoSerie> puntos = new ArrayList<>();
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setString(1, desde); ps.setString(2, hasta);
            if (idSuc>0) ps.setInt(3, idSuc);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    String etiqueta = rs.getString("etiqueta");
                    double ventas = rs.getDouble("ventas");
                    int tickets = rs.getInt("tickets");
                    int unidades = rs.getInt("unidades");
                    modelSerie.addRow(new Object[]{ etiqueta, "$ "+nf2(ventas), nf0(tickets), nf0(unidades) });
                    puntos.add(new PuntoSerie(etiqueta, ventas, tickets, unidades));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error serie:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
        chart.setData(puntos);
    }

    private void cargarCat(String desde, String hasta, int idSuc, int idCat, int idProv){
        mCat.setRowCount(0);
        StringBuilder where = new StringBuilder(" WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')");
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ where.append(" AND v.id_sucursal=?"); params.add(idSuc); }
        if (idCat>0){ where.append(" AND sc.id_categoria=?"); params.add(idCat); }
        if (idProv>0){ where.append(" AND p.id_proveedor=?"); params.add(idProv); }

        String sql = """
          SELECT c.nombre AS categoria,
                 COALESCE(SUM(vd.cantidad),0) AS unidades,
                 COALESCE(SUM(vd.precio_unitario*vd.cantidad),0) AS importe
          FROM venta v
          JOIN venta_detalle vd ON vd.id_venta=v.id_venta
          JOIN producto p ON p.id_producto=vd.id_producto
          LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
          LEFT JOIN categoria c ON c.id_categoria=sc.id_categoria
        """ + where + """
          GROUP BY c.id_categoria, c.nombre
          ORDER BY importe DESC, categoria ASC
        """;
        queryFillTable(sql, params, rs -> mCat.addRow(new Object[]{
                nvl(rs.getString("categoria"), "—"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
    }

    private void cargarProv(String desde, String hasta, int idSuc, int idCat, int idProv){
        mProv.setRowCount(0);
        StringBuilder where = new StringBuilder(" WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')");
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ where.append(" AND v.id_sucursal=?"); params.add(idSuc); }
        if (idCat>0){ where.append(" AND sc.id_categoria=?"); params.add(idCat); }
        if (idProv>0){ where.append(" AND p.id_proveedor=?"); params.add(idProv); }

        String sql = """
          SELECT pr.nombre AS proveedor,
                 COALESCE(SUM(vd.cantidad),0) AS unidades,
                 COALESCE(SUM(vd.precio_unitario*vd.cantidad),0) AS importe,
                 COALESCE(SUM((vd.precio_unitario - p.precio_compra)*vd.cantidad),0) AS margen
          FROM venta v
          JOIN venta_detalle vd ON vd.id_venta=v.id_venta
          JOIN producto p ON p.id_producto=vd.id_producto
          LEFT JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
          LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
        """ + where + """
          GROUP BY pr.id_proveedor, pr.nombre
          ORDER BY importe DESC, proveedor ASC
        """;
        queryFillTable(sql, params, rs -> mProv.addRow(new Object[]{
                nvl(rs.getString("proveedor"), "—"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe")),
                "$ " + nf2(rs.getDouble("margen"))
        }));
    }

    private void cargarSuc(String desde, String hasta, int idSuc){
        mSuc.setRowCount(0);
        String where = " WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')";
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ where += " AND v.id_sucursal=?"; params.add(idSuc); }

        String sql = """
          SELECT s.nombre AS sucursal,
                 COALESCE(SUM(vd.cantidad),0) AS unidades,
                 COALESCE(SUM(vd.precio_unitario*vd.cantidad),0) AS importe,
                 COUNT(DISTINCT v.id_venta) AS tickets
          FROM venta v
          LEFT JOIN sucursal s ON s.id_sucursal=v.id_sucursal
          LEFT JOIN venta_detalle vd ON vd.id_venta=v.id_venta
        """ + where + """
          GROUP BY s.id_sucursal, s.nombre
          ORDER BY importe DESC, sucursal ASC
        """;
        queryFillTable(sql, params, rs -> mSuc.addRow(new Object[]{
                nvl(rs.getString("sucursal"), "—"),
                nf0(rs.getInt("tickets")),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
    }

    private void cargarTop(String desde, String hasta, int idSuc, int idCat, int idProv){
        mTop.setRowCount(0);
        StringBuilder where = new StringBuilder(" WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')");
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ where.append(" AND v.id_sucursal=?"); params.add(idSuc); }
        if (idCat>0){ where.append(" AND sc.id_categoria=?"); params.add(idCat); }
        if (idProv>0){ where.append(" AND p.id_proveedor=?"); params.add(idProv); }

        String sql = """
          SELECT p.id_producto, p.nombre,
                 COALESCE(SUM(vd.cantidad),0) AS unidades,
                 COALESCE(SUM(vd.cantidad*vd.precio_unitario),0) AS importe
          FROM venta v
          JOIN venta_detalle vd ON vd.id_venta=v.id_venta
          JOIN producto p ON p.id_producto=vd.id_producto
          LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
        """ + where + """
          GROUP BY p.id_producto, p.nombre
          ORDER BY unidades DESC, importe DESC
          LIMIT 15
        """;
        queryFillTable(sql, params, rs -> mTop.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
        if (mTop.getRowCount()==0) mTop.addRow(new Object[]{"","Sin datos.","",""});
    }

    private void cargarNoSale(String desde, String hasta, int idSuc, int idCat, int idProv){
        mNoSale.setRowCount(0);
        StringBuilder sql = new StringBuilder("""
          SELECT p.id_producto, p.nombre, COALESCE(i.stock_actual,0) AS stock_actual
          FROM producto p
          LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
          LEFT JOIN inventario i ON i.id_producto=p.id_producto
        """);
        if (idSuc>0) sql.append(" AND i.id_sucursal=").append(idSuc).append(" ");

        sql.append(" WHERE p.activo=1 ");

        if (idCat>0) sql.append(" AND sc.id_categoria=").append(idCat).append(" ");
        if (idProv>0) sql.append(" AND p.id_proveedor=").append(idProv).append(" ");

        sql.append("""
          AND NOT EXISTS (
            SELECT 1 FROM venta v
            JOIN venta_detalle vd2 ON vd2.id_venta=v.id_venta
            WHERE vd2.id_producto=p.id_producto
              AND v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')
        """);
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ sql.append(" AND v.id_sucursal=?"); params.add(idSuc); }
        sql.append(") ORDER BY p.nombre ASC LIMIT 50");

        queryFillTable(sql.toString(), params, rs -> mNoSale.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("stock_actual"))
        }));
        if (mNoSale.getRowCount()==0) mNoSale.addRow(new Object[]{"","Todos tuvieron ventas o no hay datos.",""});
    }

    private void cargarRot(String desde, String hasta, int idSuc, int idCat, int idProv){
        mRot.setRowCount(0);
        StringBuilder where = new StringBuilder(" WHERE v.fecha_hora BETWEEN CONCAT(?,' 00:00:00') AND CONCAT(?,' 23:59:59')");
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (idSuc>0){ where.append(" AND v.id_sucursal=?"); params.add(idSuc); }
        if (idCat>0){ where.append(" AND sc.id_categoria=?"); params.add(idCat); }
        if (idProv>0){ where.append(" AND p.id_proveedor=?"); params.add(idProv); }

        String sql = """
          SELECT p.id_producto, p.nombre,
                 COALESCE(SUM(vd.cantidad),0) AS vendidas,
                 COALESCE(SUM(vd.cantidad*vd.precio_unitario),0) AS importe,
                 COALESCE(MAX(i.stock_actual),0) AS stock_actual
          FROM producto p
          LEFT JOIN venta_detalle vd ON vd.id_producto=p.id_producto
          LEFT JOIN venta v ON v.id_venta=vd.id_venta
          LEFT JOIN inventario i ON i.id_producto=p.id_producto
          LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
        """ + where + """
          GROUP BY p.id_producto, p.nombre
          HAVING vendidas IS NOT NULL
          ORDER BY vendidas DESC
          LIMIT 50
        """;
        queryFillTable(sql, params, rs -> mRot.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("vendidas")),
                nf0(rs.getInt("stock_actual")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
        if (mRot.getRowCount()==0) mRot.addRow(new Object[]{"","Sin datos.","","",""});
    }

    private void queryFillTable(String sql, List<Object> params, RowAdder adder){
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){ adder.add(rs); }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error consultando:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ======================= Utils ======================= */

    private interface RowAdder { void add(ResultSet rs) throws Exception; }

    static class Item {
        private final int id; private final String nombre;
        Item(int id,String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        @Override public String toString(){ return nombre; }
    }
    static class DB {
        static Connection get() throws Exception { return conexion_bd.getConnection(); }
    }
    private static void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i=0;i<params.size();i++){
            Object v=params.get(i);
            if (v instanceof Integer iv) ps.setInt(i+1, iv);
            else if (v instanceof Double dv) ps.setDouble(i+1, dv);
            else ps.setString(i+1, String.valueOf(v));
        }
    }
    private static String nvl(String s, String alt){ return (s==null || s.isBlank())? alt : s; }
    private static String nf0(int n){ return String.format("%,d", n).replace(',', '.'); }
    private static String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }

    /* ======================= Mini Chart (sin libs) ======================= */

    static class LineChartPanel extends JPanel {
        private List<PuntoSerie> data = new ArrayList<>();
        private int hoverIdx = -1;

        LineChartPanel(){
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(
                    new LineBorder(new Color(0xE5,0xD8,0xC2),1,true),
                    new EmptyBorder(6,8,6,8)
            ));
            setToolTipText("");
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int idx = pickIndex(e.getX());
                    if (idx != hoverIdx){ hoverIdx = idx; repaint(); }
                }
            });
        }

        void setData(List<PuntoSerie> puntos){
            this.data = (puntos==null)? new ArrayList<>() : puntos;
            this.hoverIdx = -1;
            repaint();
        }

        private int pickIndex(int mouseX){
            if (data.isEmpty()) return -1;
            Insets in = getInsets();
            int w = getWidth()-in.left-in.right;
            int n = data.size();
            if (n==1) {
                int cx = in.left + w/2;
                return Math.abs(mouseX - cx) < 8? 0 : -1;
            }
            int usableW = w - 70;
            int startX = in.left + 50;
            int step = (n>1)? (usableW/(n-1)) : usableW;
            int best=-1, bestDist=9999;
            for (int i=0;i<n;i++){
                int x = startX + i*step;
                int d = Math.abs(mouseX - x);
                if (d < bestDist){ best = i; bestDist = d; }
            }
            return bestDist<=10? best : -1;
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(Renderings.AA_K, Renderings.AA_V);
            g2.setRenderingHint(Renderings.TA_K, Renderings.TA_V);

            Insets in = getInsets();
            int W = getWidth(), H = getHeight();
            int leftAxisW = 50, rightAxisW = 50, bottom = 18, top = 10;
            int x0 = in.left + leftAxisW;
            int x1 = W - in.right - rightAxisW;
            int y0 = H - in.bottom - bottom;
            int y1 = in.top + top;

            g2.setColor(new Color(0xFB,0xF7,0xF0));
            g2.fillRect(x0, y1, (x1-x0), (y0-y1));
            g2.setColor(new Color(0xE5,0xD8,0xC2));
            g2.drawRect(x0, y1, (x1-x0), (y0-y1));

            if (data.isEmpty()){
                String s="Sin datos en el período.";
                g2.setColor(new Color(120,120,120));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(s, x0 + ((x1-x0)-fm.stringWidth(s))/2, y1 + (y0-y1)/2);
                g2.dispose(); return;
            }

            final double maxVentas = Math.max(1.0,
                    data.stream().mapToDouble(p->p.ventas).max().orElse(1.0));
            final double maxDer = Math.max(1.0, Math.max(
                    data.stream().mapToInt(p->p.tickets).max().orElse(1),
                    data.stream().mapToInt(p->p.unidades).max().orElse(1)
            ));

            int n = data.size();
            int usableW = Math.max(1, x1-x0-6);
            int step = (n>1)? (usableW/(n-1)) : usableW;

            g2.setColor(new Color(230,230,230));
            for (int i=0;i<5;i++){
                int y = y1 + i*(y0-y1)/4;
                g2.drawLine(x0, y, x1, y);
            }

            g2.setColor(new Color(90,90,90));
            g2.drawLine(x0, y0, x1, y0);
            g2.drawLine(x0, y1, x0, y0);
            g2.drawLine(x1, y1, x1, y0);

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            for (int i=0;i<=4;i++){
                double v = maxVentas * (i/4.0);
                int y = y0 - (int)((v/maxVentas)*(y0-y1));
                String lab = "$" + abrevia(v);
                int tw = g2.getFontMetrics().stringWidth(lab);
                g2.drawString(lab, x0 - 6 - tw, y+4);
            }
            for (int i=0;i<=4;i++){
                double v = maxDer * (i/4.0);
                int y = y0 - (int)((v/maxDer)*(y0-y1));
                String lab = abrevia(v);
                g2.drawString(lab, x1 + 6, y+4);
            }

            Stroke stroke = new BasicStroke(2f);
            g2.setStroke(stroke);

            g2.setColor(new Color(0x33,0x66,0xCC));
            drawSeries(g2, x0, y0, y1, step,
                    data.stream().map(d->d.ventas/maxVentas).collect(Collectors.toList()));

            g2.setColor(new Color(0x99,0x33,0x33));
            drawSeriesRight(g2, x0, x1, y0, y1, step,
                    data.stream().map(d->d.tickets/(double)maxDer).collect(Collectors.toList()));

            g2.setColor(new Color(0x33,0x99,0x33));
            drawSeriesRight(g2, x0, x1, y0, y1, step,
                    data.stream().map(d->d.unidades/(double)maxDer).collect(Collectors.toList()));

            if (hoverIdx>=0 && hoverIdx < n){
                int px = x0 + hoverIdx*step;
                g2.setColor(new Color(0,0,0,28));
                g2.drawLine(px, y1, px, y0);

                PuntoSerie p = data.get(hoverIdx);
                String tip1 = p.etiqueta;
                String tip2 = "Ventas: $"+fmt2(p.ventas);
                String tip3 = "Tickets: "+p.tickets;
                String tip4 = "Unidades: "+p.unidades;

                List<String> lines = List.of(tip1, tip2, tip3, tip4);
                FontMetrics fm = g2.getFontMetrics();
                int w = lines.stream().mapToInt(fm::stringWidth).max().orElse(0) + 14;
                int h = lines.size()* (fm.getHeight()) + 10;
                int tx = Math.min(Math.max(px+10, x0), x1-w);
                int ty = y1 + 6;
                g2.setColor(new Color(255,255,255,235));
                g2.fillRoundRect(tx, ty, w, h, 8, 8);
                g2.setColor(new Color(180,180,180));
                g2.drawRoundRect(tx, ty, w, h, 8, 8);
                g2.setColor(new Color(60,60,60));
                int yy = ty + fm.getAscent() + 6;
                for (String line: lines){
                    g2.drawString(line, tx+7, yy);
                    yy += fm.getHeight();
                }
            }

            String lg = "Ventas  •  Tickets  •  Unidades";
            g2.setColor(new Color(70,70,70));
            g2.drawString(lg, x0, y0+16);

            g2.dispose();
        }

        private void drawSeries(Graphics2D g2, int x0, int y0, int y1, int step, List<Double> norm){
            int n = norm.size();
            int prevX=-1, prevY=-1;
            for (int i=0;i<n;i++){
                int x = x0 + i*step;
                int y = y0 - (int)(norm.get(i) * (y0 - y1));
                if (i>0) g2.drawLine(prevX, prevY, x, y);
                prevX=x; prevY=y;
            }
        }
        private void drawSeriesRight(Graphics2D g2, int x0, int x1, int y0, int y1, int step, List<Double> norm){
            drawSeries(g2, x0, y0, y1, step, norm);
        }

        @Override public String getToolTipText(MouseEvent event) {
            if (data==null || data.isEmpty() || hoverIdx<0 || hoverIdx>=data.size()) return null;
            PuntoSerie p = data.get(hoverIdx);
            return "<html><b>"+p.etiqueta+"</b><br>Ventas: $"+fmt2(p.ventas)+
                    "<br>Tickets: "+p.tickets+"<br>Unidades: "+p.unidades+"</html>";
        }

        private static String abrevia(double v){
            if (v>=1_000_000) return fmt1(v/1_000_000)+"M";
            if (v>=1_000) return fmt1(v/1_000)+"k";
            return fmt1(v);
        }
        private static String fmt1(double d){ return String.format(Locale.US, "%.1f", d); }
        private static String fmt2(double d){ return String.format(Locale.US, "%,.2f", d).replace(',', 'X').replace('.', ',').replace('X','.'); }

        static class Renderings {
            static final RenderingHints.Key AA_K = RenderingHints.KEY_ANTIALIASING;
            static final Object              AA_V = RenderingHints.VALUE_ANTIALIAS_ON;
            static final RenderingHints.Key TA_K = RenderingHints.KEY_TEXT_ANTIALIASING;
            static final Object             TA_V = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        }
    }

    static class PuntoSerie {
        final String etiqueta;
        final double ventas;
        final int tickets;
        final int unidades;
        PuntoSerie(String etiqueta, double ventas, int tickets, int unidades){
            this.etiqueta=etiqueta; this.ventas=ventas; this.tickets=tickets; this.unidades=unidades;
        }
    }
}
