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

// visual: panel principal de reportes y estadísticas
public class panel_reportes extends JPanel {

    // visual: constantes de espaciado y fuentes
    private static final int PAD_OUTER_LEFT  = 12;
    private static final int PAD_OUTER_OTHER = 10;
    private static final int PAD_CARD = 10;
    private static final int PAD_CARD_TOP = 6;
    private static final int FONT_BASE = 13;
    private static final int ROW_H = 24;
    private static final int ROW_H_HDR = 26;

    // visual: componentes de filtros
    private JTextField tfDesde, tfHasta;
    private JComboBox<Item> cbSucursal, cbCategoria, cbProveedor;
    private JComboBox<String> cbGran;
    private JButton btnFiltrar;
    private JLabel tagRango;

    // visual: indicadores KPI
    private JLabel kpiVentas, kpiTickets, kpiUnidades, kpiMargen;

    // visual: modelo de serie y gráfico
    private DefaultTableModel modelSerie;
    private LineChartPanel chart;

    // visual: modelos de tablas secundarias
    private DefaultTableModel mCat, mProv, mSuc, mTop, mNoSale, mRot;

    // lógica: catálogos cargados desde BD
    private final List<Item> sucursales = new ArrayList<>();
    private final List<Item> categorias = new ArrayList<>();
    private final List<Item> proveedores = new ArrayList<>();

    // visual + lógica: constructor principal
    public panel_reportes(){
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        // visual: contenedor principal con scroll vertical
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

        // visual: bloque superior (título, filtros, KPIs y gráfico)
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(header());
        stack.add(filtrosFila());
        stack.add(kpisGridCompact());
        stack.add(boxChart());
        card.add(stack, BorderLayout.NORTH);

        // visual: bloque central con tablas
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

        // visual: scroll vertical sin horizontal
        JScrollPane scroller = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroller.setBorder(null);
        scroller.setViewportBorder(null);
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);

        add(scroller, BorderLayout.CENTER);

        // lógica: evento de filtrado
        btnFiltrar.addActionListener(e -> recargarTodo());

        // lógica: valores iniciales
        tfHasta.setText(LocalDate.now().toString());
        tfDesde.setText(LocalDate.now().minusDays(14).toString());
        cbGran.setSelectedItem("Día");
        cargarCombos();
        recargarTodo();
    }

    // visual: encabezado del panel
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

    // visual: contenedor del gráfico principal
    private JPanel boxChart(){
        JPanel wrap = cardInner();
        wrap.setLayout(new BorderLayout());
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

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

    // visual: sección de filtros en dos filas
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

        // visual: primera fila de filtros
        int y=0, x=0;
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Desde"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.15; box.add(tfDesde, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Hasta"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.15; box.add(tfHasta, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Sucursal"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbSucursal, g);

        // visual: segunda fila de filtros
        y++; x=0;
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Categoría"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbCategoria, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Proveedor"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.25; box.add(cbProveedor, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(lbl("Granularidad"), g);
        g.gridy=y; g.gridx=x++; g.weightx=0.18; box.add(cbGran, g);
        g.gridy=y; g.gridx=x++; g.weightx=0;    box.add(btnFiltrar, g);

        wrap.add(box, BorderLayout.CENTER);

        // visual: etiqueta inferior con rango seleccionado
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

    // visual: etiqueta simple con estilo base
    private JLabel lbl(String s){
        JLabel l = new JLabel(s);
        l.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        return l;
    }

    // visual: cuadrícula de KPIs compacta (2x2)
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

    // visual: envoltorio para secciones con título y contenido
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

    // visual: contenedor de tabla dentro de sección
    private JPanel boxTabla(String titulo, JTable table){
        return boxSection(titulo, scrollForTable(table));
    }

    // visual: creación y formato de tablas
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

        // visual: ajuste de ancho de columnas
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

        // visual: ancho preferido por tipo de columna
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

    // visual: scroll para tablas sin scroll horizontal
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

    // visual: contenedor base con borde crema
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

    // visual: contenedor interno blanco con borde crema
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

    // visual: estilo de etiqueta KPI principal
    private JLabel kpiBig(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.BOLD, 20));
        l.setForeground(new Color(50,50,50));
        return l;
    }

    // visual: tarjeta contenedora para KPI individual
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

    // visual: estilo para campo de fecha
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

    // visual: estilo para combo genérico
    private void estilizarCombo(JComboBox<?> cb){
        cb.setFont(new Font("Arial", Font.PLAIN, FONT_BASE));
        cb.setBorder(new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true));
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(140, 28));
        cb.setMaximumRowCount(15);
    }


        // lógica: carga completa de datos según los filtros seleccionados
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

        // visual: muestra rango de fechas seleccionado
        tagRango.setText(desde + " → " + hasta);

        // lógica: ejecuta carga de todas las secciones de datos
        cargarKPIs(desde, hasta, idSuc);
        cargarSerie(desde, hasta, idSuc, gran);
        cargarCat(desde, hasta, idSuc, idCat, idProv);
        cargarProv(desde, hasta, idSuc, idCat, idProv);
        cargarSuc(desde, hasta, idSuc);
        cargarTop(desde, hasta, idSuc, idCat, idProv);
        cargarNoSale(desde, hasta, idSuc, idCat, idProv);
        cargarRot(desde, hasta, idSuc, idCat, idProv);
    }

    // lógica: carga los valores de los combos (sucursal, categoría y proveedor)
    private void cargarCombos(){
        sucursales.clear(); categorias.clear(); proveedores.clear();
        sucursales.add(new Item(0,"Todas las sucursales"));
        categorias.add(new Item(0,"Todas las categorías"));
        proveedores.add(new Item(0,"Todos los proveedores"));
        try (Connection cn = DB.get()){
            // lógica: obtiene listado de sucursales
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_sucursal, nombre FROM sucursal ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) sucursales.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            // lógica: obtiene listado de categorías
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) categorias.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            // lógica: obtiene listado de proveedores
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor, nombre FROM proveedor ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) proveedores.add(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            // visual: muestra error de base de datos
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }

        // visual: asigna los modelos a los combos
        cbSucursal.setModel(new DefaultComboBoxModel<>(sucursales.toArray(new Item[0])));
        cbCategoria.setModel(new DefaultComboBoxModel<>(categorias.toArray(new Item[0])));
        cbProveedor.setModel(new DefaultComboBoxModel<>(proveedores.toArray(new Item[0])));
    }

    // lógica: consulta y muestra los indicadores KPI (ventas, tickets, unidades, margen)
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
                    // visual: muestra los valores obtenidos en los indicadores
                    kpiVentas.setText("$ " + nf2(rs.getDouble("ventas")));
                    kpiTickets.setText(nf0(rs.getInt("tickets")));
                    kpiUnidades.setText(nf0(rs.getInt("unidades")));
                    kpiMargen.setText("$ " + nf2(rs.getDouble("margen")));
                }
            }
        } catch (Exception ex){
            // visual: mensaje de error en caso de fallo de consulta
            JOptionPane.showMessageDialog(this,"Error KPIs:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: genera y carga la serie temporal de ventas, tickets y unidades
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
                    // visual: agrega fila a la tabla de serie
                    modelSerie.addRow(new Object[]{ etiqueta, "$ "+nf2(ventas), nf0(tickets), nf0(unidades) });
                    // lógica: guarda los puntos para graficar
                    puntos.add(new PuntoSerie(etiqueta, ventas, tickets, unidades));
                }
            }
        } catch (Exception ex){
            // visual: mensaje de error si falla la consulta
            JOptionPane.showMessageDialog(this,"Error serie:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }

        // visual: actualiza el gráfico con los nuevos datos
        chart.setData(puntos);
    }

    // lógica: carga ventas agrupadas por categoría
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

        // lógica: ejecuta la consulta y llena la tabla de categorías
        queryFillTable(sql, params, rs -> mCat.addRow(new Object[]{
                nvl(rs.getString("categoria"), "—"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
    }

    // lógica: carga de ventas agrupadas por proveedor
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

        // lógica: ejecuta la consulta y agrega resultados a la tabla de proveedores
        queryFillTable(sql, params, rs -> mProv.addRow(new Object[]{
                nvl(rs.getString("proveedor"), "—"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe")),
                "$ " + nf2(rs.getDouble("margen"))
        }));
    }

    // lógica: carga de ventas agrupadas por sucursal
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

        // lógica: llena la tabla de sucursales con los resultados
        queryFillTable(sql, params, rs -> mSuc.addRow(new Object[]{
                nvl(rs.getString("sucursal"), "—"),
                nf0(rs.getInt("tickets")),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));
    }

    // lógica: carga el ranking de productos más vendidos
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

        // lógica: llena la tabla de top productos vendidos
        queryFillTable(sql, params, rs -> mTop.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("unidades")),
                "$ " + nf2(rs.getDouble("importe"))
        }));

        // visual: muestra mensaje si no hay datos
        if (mTop.getRowCount()==0) mTop.addRow(new Object[]{"","Sin datos.","",""});
    }

    // lógica: carga productos sin ventas en el período
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

        // lógica: ejecuta consulta y llena la tabla de productos sin ventas
        queryFillTable(sql.toString(), params, rs -> mNoSale.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("stock_actual"))
        }));

        // visual: muestra mensaje si no hay resultados
        if (mNoSale.getRowCount()==0) mNoSale.addRow(new Object[]{"","Todos tuvieron ventas o no hay datos.",""});
    }

    // lógica: carga la rotación de productos (ventas y stock)
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

        // lógica: ejecuta la consulta y llena la tabla de rotación
        queryFillTable(sql, params, rs -> mRot.addRow(new Object[]{
                "#"+rs.getInt("id_producto"),
                rs.getString("nombre"),
                nf0(rs.getInt("vendidas")),
                nf0(rs.getInt("stock_actual")),
                "$ " + nf2(rs.getDouble("importe"))
        }));

        // visual: muestra mensaje si no hay datos
        if (mRot.getRowCount()==0) mRot.addRow(new Object[]{"","Sin datos.","","",""});
    }

    // lógica: ejecuta consultas genéricas y agrega resultados a una tabla
    private void queryFillTable(String sql, List<Object> params, RowAdder adder){
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){ adder.add(rs); }
            }
        } catch (Exception ex){
            // visual: mensaje de error si falla la consulta
            JOptionPane.showMessageDialog(this, "Error consultando:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ======================= Utils ======================= */

    // lógica: interfaz funcional para llenar filas desde un ResultSet
    private interface RowAdder { void add(ResultSet rs) throws Exception; }

    // lógica: estructura para ítems genéricos de los combos
    static class Item {
        private final int id; private final String nombre;
        Item(int id,String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        @Override public String toString(){ return nombre; }
    }

    // lógica: acceso simplificado a la conexión con la base de datos
    static class DB {
        static Connection get() throws Exception { return conexion_bd.getConnection(); }
    }

    // lógica: asigna parámetros a una PreparedStatement según su tipo
    private static void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i=0;i<params.size();i++){
            Object v=params.get(i);
            if (v instanceof Integer iv) ps.setInt(i+1, iv);
            else if (v instanceof Double dv) ps.setDouble(i+1, dv);
            else ps.setString(i+1, String.valueOf(v));
        }
    }

    // lógica: utilidades de formato y valores nulos
    private static String nvl(String s, String alt){ return (s==null || s.isBlank())? alt : s; }
    private static String nf0(int n){ return String.format("%,d", n).replace(',', '.'); }
    private static String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }

        // visual: panel que dibuja el gráfico de líneas sin usar librerías externas
    static class LineChartPanel extends JPanel {
        private List<PuntoSerie> data = new ArrayList<>();
        private int hoverIdx = -1;

        // visual: configuración inicial del panel y eventos del mouse
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

        // visual: actualiza los datos del gráfico y repinta
        void setData(List<PuntoSerie> puntos){
            this.data = (puntos==null)? new ArrayList<>() : puntos;
            this.hoverIdx = -1;
            repaint();
        }

        // lógica: determina el punto más cercano al cursor del mouse
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

        // visual: dibuja el gráfico y sus elementos (series, ejes, etiquetas, tooltip)
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

            // visual: fondo del área de dibujo
            g2.setColor(new Color(0xFB,0xF7,0xF0));
            g2.fillRect(x0, y1, (x1-x0), (y0-y1));
            g2.setColor(new Color(0xE5,0xD8,0xC2));
            g2.drawRect(x0, y1, (x1-x0), (y0-y1));

            // visual: mensaje si no hay datos
            if (data.isEmpty()){
                String s="Sin datos en el período.";
                g2.setColor(new Color(120,120,120));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(s, x0 + ((x1-x0)-fm.stringWidth(s))/2, y1 + (y0-y1)/2);
                g2.dispose(); return;
            }

            // lógica: obtiene valores máximos para escalar las series
            final double maxVentas = Math.max(1.0,
                    data.stream().mapToDouble(p->p.ventas).max().orElse(1.0));
            final double maxDer = Math.max(1.0, Math.max(
                    data.stream().mapToInt(p->p.tickets).max().orElse(1),
                    data.stream().mapToInt(p->p.unidades).max().orElse(1)
            ));

            int n = data.size();
            int usableW = Math.max(1, x1-x0-6);
            int step = (n>1)? (usableW/(n-1)) : usableW;

            // visual: líneas de guía del fondo
            g2.setColor(new Color(230,230,230));
            for (int i=0;i<5;i++){
                int y = y1 + i*(y0-y1)/4;
                g2.drawLine(x0, y, x1, y);
            }

            // visual: dibuja ejes del gráfico
            g2.setColor(new Color(90,90,90));
            g2.drawLine(x0, y0, x1, y0);
            g2.drawLine(x0, y1, x0, y0);
            g2.drawLine(x1, y1, x1, y0);

            // visual: etiquetas numéricas de los ejes
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

            // visual: dibuja series de ventas, tickets y unidades
            g2.setColor(new Color(0x33,0x66,0xCC));
            drawSeries(g2, x0, y0, y1, step,
                    data.stream().map(d->d.ventas/maxVentas).collect(Collectors.toList()));

            g2.setColor(new Color(0x99,0x33,0x33));
            drawSeriesRight(g2, x0, x1, y0, y1, step,
                    data.stream().map(d->d.tickets/(double)maxDer).collect(Collectors.toList()));

            g2.setColor(new Color(0x33,0x99,0x33));
            drawSeriesRight(g2, x0, x1, y0, y1, step,
                    data.stream().map(d->d.unidades/(double)maxDer).collect(Collectors.toList()));

            // visual: muestra tooltip al pasar el mouse sobre un punto
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

                // visual: caja y texto del tooltip
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

            // visual: leyenda inferior
            String lg = "Ventas  •  Tickets  •  Unidades";
            g2.setColor(new Color(70,70,70));
            g2.drawString(lg, x0, y0+16);

            g2.dispose();
        }

        // visual: dibuja una serie sobre el gráfico principal (eje izquierdo)
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

        // visual: dibuja una serie usando el eje derecho
        private void drawSeriesRight(Graphics2D g2, int x0, int x1, int y0, int y1, int step, List<Double> norm){
            drawSeries(g2, x0, y0, y1, step, norm);
        }

        // visual: genera texto del tooltip al pasar el mouse sobre un punto
        @Override public String getToolTipText(MouseEvent event) {
            if (data==null || data.isEmpty() || hoverIdx<0 || hoverIdx>=data.size()) return null;
            PuntoSerie p = data.get(hoverIdx);
            return "<html><b>"+p.etiqueta+"</b><br>Ventas: $"+fmt2(p.ventas)+
                    "<br>Tickets: "+p.tickets+"<br>Unidades: "+p.unidades+"</html>";
        }

        // lógica: formatea valores numéricos con abreviaciones (k, M)
        private static String abrevia(double v){
            if (v>=1_000_000) return fmt1(v/1_000_000)+"M";
            if (v>=1_000) return fmt1(v/1_000)+"k";
            return fmt1(v);
        }

        // lógica: formatos auxiliares para números
        private static String fmt1(double d){ return String.format(Locale.US, "%.1f", d); }
        private static String fmt2(double d){ return String.format(Locale.US, "%,.2f", d).replace(',', 'X').replace('.', ',').replace('X','.'); }

        // lógica: constantes para suavizado de renderizado
        static class Renderings {
            static final RenderingHints.Key AA_K = RenderingHints.KEY_ANTIALIASING;
            static final Object              AA_V = RenderingHints.VALUE_ANTIALIAS_ON;
            static final RenderingHints.Key TA_K = RenderingHints.KEY_TEXT_ANTIALIASING;
            static final Object             TA_V = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        }
    }

    // lógica: estructura que almacena los datos de cada punto de la serie
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

