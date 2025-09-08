package admin.inicio;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class panel_inicio extends JPanel {

    //  labels
    private JLabel kpiProdTotal, kpiProdSin;
    private JLabel kpiCatTotal, kpiSubcatTotal;
    private JLabel kpiProvTotal, kpiStockTotal;
    private JLabel kpiVentasHoy, kpiBajoStock;

    // fecha
    private JTextField tfDesde, tfHasta;
    private JButton btnAplicar, btnLimpiar;


    private DefaultTableModel mdlVentasMes;
    private DefaultTableModel mdlTopProd;
    private DefaultTableModel mdlVentasRec;
    private DefaultTableModel mdlStockCat;
    private DefaultTableModel mdlLowList;
    private DefaultTableModel mdlPedPend;
    private DefaultTableModel mdlMovRec;
    private DefaultTableModel mdlAlertas;

    public panel_inicio() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        // ancho máximo 
        content.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        content.setAlignmentX(Component.CENTER_ALIGNMENT);

        // scroll
        JScrollPane scroll = new JScrollPane(
                content,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        Color crema = estilos.COLOR_FONDO;
        content.setOpaque(true);
        content.setBackground(crema);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(crema);
        scroll.setBackground(crema);

        
        content.add(crearFilaKPIs());

        /*  Filtro  */
        content.add(crearFiltroRango());

        /*  filas */
        content.add(crearFilaDos(crearCardVentasMes(), crearCardTopProd()));
       
        content.add(crearFilaDos(crearCardVentasRec(), crearCardStockCat()));
       
        content.add(crearFilaDos(crearCardLowList(), crearCardPedPend()));
        
        content.add(crearFilaDos(crearCardMovRec(), crearCardAlertas()));
        /* Acciones */
        content.add(crearCardAcciones());
        content.add(Box.createVerticalStrut(12)); 

        // Fechas 
        LocalDate hoy = LocalDate.now();
        tfHasta.setText(hoy.toString());
        tfDesde.setText(hoy.minusDays(30).toString());

        // Eventos
        btnAplicar.addActionListener(e -> cargarTodo());
        btnLimpiar.addActionListener(e -> {
            tfHasta.setText(LocalDate.now().toString());
            tfDesde.setText(LocalDate.now().minusDays(30).toString());
            cargarTodo();
        });

        // Carga inicial
        cargarTodo();
    }


    private JPanel crearFilaKPIs() {
        JPanel fila = new JPanel(new GridLayout(1,4,16,16));
        fila.setOpaque(false);

        // Productos
        kpiProdTotal = big();
        kpiProdSin   = smallMuted();
        JPanel c1 = cardKPISimple("Productos", kpiProdTotal, "Sin stock: ", kpiProdSin);

        // Categorías
        kpiCatTotal    = big();
        kpiSubcatTotal = smallMuted();
        JPanel c2 = cardKPISimple("Categorías", kpiCatTotal, "Subcategorías: ", kpiSubcatTotal);

        // Proveedores
        kpiProvTotal  = big();
        kpiStockTotal = smallMuted();
        JPanel c3 = cardKPISimple("Proveedores", kpiProvTotal, "Stock total: ", kpiStockTotal);

        // Ventas hoy
        kpiVentasHoy = big();
        kpiBajoStock = smallMutedRed();
        JPanel c4 = cardKPISimple("Ventas hoy", kpiVentasHoy, "Bajo stock: ", kpiBajoStock);

        fila.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));
        fila.add(c1); fila.add(c2); fila.add(c3); fila.add(c4);
        return fila;
    }

    private JPanel crearFiltroRango() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8,0,8,0));

        JPanel card = cardShell();
        card.setLayout(new BorderLayout());

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        inner.setOpaque(false);

        JLabel lb = new JLabel("Rango");
        lb.setForeground(new Color(90,90,90));

        tfDesde = new JTextField(10);
        tfHasta = new JTextField(10);
        estilizarInput(tfDesde);
        estilizarInput(tfHasta);

        JLabel flecha = new JLabel(" → ");

        btnAplicar = estilos.botonSm("aplicar");
        btnLimpiar = estilos.botonSm("Limpiar");

        inner.add(lb);
        inner.add(tfDesde);
        inner.add(flecha);
        inner.add(tfHasta);
        inner.add(btnAplicar);
        inner.add(btnLimpiar);

        card.add(inner, BorderLayout.WEST);
        row.add(card, BorderLayout.CENTER);
        return row;
    }

    private JPanel crearFilaDos(JPanel leftCard, JPanel rightCard) {
        JPanel fila = new JPanel(new GridLayout(1,2,16,16));
        fila.setOpaque(false);
        fila.add(leftCard);
        fila.add(rightCard);
        fila.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        return fila;
    }

    private JPanel crearCardVentasMes() {
        String[] cols = {"Mes", "Total $"};
        mdlVentasMes = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlVentasMes);
        setColWidth(t,0,240);
        return makeCard("Ventas por mes (últimos 12)", null, tableWrap(t));
    }

    private JPanel crearCardTopProd() {
        String[] cols = {"Producto", "Unid.", "Ingresos $"};
        mdlTopProd = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlTopProd);
        setColWidth(t,1,120); setColWidth(t,2,140);
        JLabel r = new JLabel(); // derecha vacía
        return makeCard("Top productos", r, tableWrap(t));
    }

    private JPanel crearCardVentasRec() {
        String[] cols = {"#", "Fecha/Hora", "Total $"};
        mdlVentasRec = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlVentasRec);
        setColWidth(t,0,70); setColWidth(t,2,140);
        return makeCard("Ventas recientes", null, tableWrap(t));
    }

    private JPanel crearCardStockCat() {
        String[] cols = {"Categoría", "Stock total"};
        mdlStockCat = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlStockCat);
        setColWidth(t,1,140);
        return makeCard("Stock por categoría", null, tableWrap(t));
    }

    private JPanel crearCardLowList() {
        String[] cols = {"Producto", "Stock", "Mínimo"};
        mdlLowList = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlLowList);
        // badge rojo en "Stock"
        t.getColumnModel().getColumn(1).setCellRenderer(new BadgeNoRenderer());
        setColWidth(t,1,90); setColWidth(t,2,100);
        return makeCard("Alertas: bajo stock", null, tableWrap(t));
    }

    private JPanel crearCardPedPend() {
        String[] cols = {"#", "Proveedor", "Estado"};
        mdlPedPend = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlPedPend);
        setColWidth(t,0,70); setColWidth(t,2,140);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(estilos.botonSm("Ir a pedidos"));

        return makeCard("Pedidos pendientes", right, tableWrap(t));
    }

    private JPanel crearCardMovRec() {
        String[] cols = {"#", "Tipo", "Unidades"};
        mdlMovRec = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlMovRec);
        setColWidth(t,0,70); setColWidth(t,2,120);
        return makeCard("Movimientos de stock (recientes)", null, tableWrap(t));
    }

    private JPanel crearCardAlertas() {
        String[] cols = {"#", "Tipo", "Producto", "Atendida"};
        mdlAlertas = new DefaultTableModel(cols, 0){ @Override public boolean isCellEditable(int r,int c){return false;} };
        JTable t = buildTable(mdlAlertas);
        setColWidth(t,0,70); setColWidth(t,3,120);
        return makeCard("Alertas (tabla)", null, tableWrap(t));
    }

    private JPanel crearCardAcciones() {
        JPanel body = new JPanel(new GridLayout(1,3,16,0));
        body.setOpaque(false);
        JButton b1 = estilos.botonRedondeado("+ Nuevo producto");
        JButton b2 = estilos.botonRedondeado("+ Ingreso de stock");
        JButton b3 = estilos.botonRedondeado("Venta rápida");
        body.add(b1); body.add(b2); body.add(b3);
        return makeCard("Acciones rápidas", null, body);
    }

    /*  Carga de datos */

    private void cargarTodo() {
        cargarKPIs();
        cargarVentasMes();
        cargarTopProd();
        cargarVentasRec();
        cargarStockCat();
        cargarLowList();
        cargarPedPend();
        cargarMovRec();
        cargarAlertas();
    }

    private void cargarKPIs() {
        try (Connection cn = DB.get()) {
            int prodTotal = getInt(cn, "SELECT COUNT(*) n FROM producto", null);
            int prodSin   = getInt(cn, """
                    SELECT COUNT(*) n FROM (
                      SELECT p.id_producto
                      FROM producto p
                      LEFT JOIN inventario i ON i.id_producto=p.id_producto
                      GROUP BY p.id_producto
                      HAVING COALESCE(SUM(i.stock_actual),0) <= 0
                    ) t
                    """, null);
            int catTotal  = getInt(cn, "SELECT COUNT(*) n FROM categoria", null);
            int subcTotal = getInt(cn, "SELECT COUNT(*) n FROM subcategoria", null);
            int provTotal = getInt(cn, "SELECT COUNT(*) n FROM proveedor", null);
            int stockTot  = getInt(cn, "SELECT COALESCE(SUM(stock_actual),0) n FROM inventario", null);
            double ventasHoy = getDouble(cn, """
                    SELECT COALESCE(SUM(v.total),0) total
                    FROM venta v
                    WHERE DATE(v.fecha_hora)=CURDATE()
                    """, null);
            int bajo = getInt(cn, """
                    SELECT COUNT(*) n FROM (
                      SELECT p.id_producto
                      FROM producto p
                      LEFT JOIN inventario i ON i.id_producto=p.id_producto
                      GROUP BY p.id_producto
                      HAVING COALESCE(SUM(i.stock_actual),0) <= COALESCE(MIN(i.stock_minimo),0)
                    ) t
                    """, null);

            kpiProdTotal.setText(nf0(prodTotal));
            kpiProdSin.setText(nf0(prodSin));

            kpiCatTotal.setText(nf0(catTotal));
            kpiSubcatTotal.setText(nf0(subcTotal));

            kpiProvTotal.setText(nf0(provTotal));
            kpiStockTotal.setText(nf0(stockTot));

            kpiVentasHoy.setText("$ " + nf2(ventasHoy));
            kpiBajoStock.setText(nf0(bajo));
        } catch (Exception ex) {
            showErr(ex);
        }
    }

    private void cargarVentasMes() {
        mdlVentasMes.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT DATE_FORMAT(v.fecha_hora,'%b %Y') etiqueta,
                           SUM(v.total) total
                    FROM venta v
                    WHERE v.fecha_hora >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
                    GROUP BY DATE_FORMAT(v.fecha_hora,'%Y-%m'), etiqueta
                    ORDER BY DATE_FORMAT(v.fecha_hora,'%Y-%m')
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlVentasMes.addRow(new Object[]{
                            rs.getString("etiqueta"),
                            "$ " + nf2(rs.getDouble("total"))
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlVentasMes.getRowCount()==0) mdlVentasMes.addRow(new Object[]{"Sin datos.", ""});
    }

    private void cargarTopProd() {
        mdlTopProd.setRowCount(0);
        String d = tfDesde.getText().trim();
        String h = tfHasta.getText().trim();
        if (d.isEmpty() || h.isEmpty()) {
            d = LocalDate.now().minusDays(30).toString();
            h = LocalDate.now().toString();
        }
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT p.nombre,
                           SUM(vd.cantidad) unidades,
                           SUM(vd.cantidad * vd.precio_unitario) total
                    FROM venta_detalle vd
                    JOIN venta v    ON v.id_venta = vd.id_venta
                    JOIN producto p ON p.id_producto = vd.id_producto
                    WHERE v.fecha_hora >= ? AND v.fecha_hora < DATE_ADD(?, INTERVAL 1 DAY)
                    GROUP BY p.id_producto, p.nombre
                    ORDER BY unidades DESC
                    LIMIT 10
                    """)) {
            ps.setString(1, d);
            ps.setString(2, h);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlTopProd.addRow(new Object[]{
                            rs.getString("nombre"),
                            nf0(rs.getInt("unidades")),
                            "$ " + nf2(rs.getDouble("total"))
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlTopProd.getRowCount()==0) mdlTopProd.addRow(new Object[]{"Sin ventas en el rango.", "", ""});
    }

    private void cargarVentasRec() {
        mdlVentasRec.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT v.id_venta, v.fecha_hora, v.total
                    FROM venta v
                    ORDER BY v.fecha_hora DESC
                    LIMIT 10
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlVentasRec.addRow(new Object[]{
                            "#"+rs.getInt("id_venta"),
                            rs.getString("fecha_hora"),
                            "$ " + nf2(rs.getDouble("total"))
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlVentasRec.getRowCount()==0) mdlVentasRec.addRow(new Object[]{"", "Sin ventas registradas.", ""});
    }

    private void cargarStockCat() {
        mdlStockCat.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT c.nombre categoria, COALESCE(SUM(i.stock_actual),0) cant
                    FROM producto p
                    LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
                    LEFT JOIN categoria c     ON c.id_categoria=sc.id_categoria
                    LEFT JOIN inventario i    ON i.id_producto=p.id_producto
                    GROUP BY c.id_categoria, c.nombre
                    ORDER BY cant DESC, categoria ASC
                    LIMIT 8
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlStockCat.addRow(new Object[]{
                            rs.getString("categoria"),
                            nf0(rs.getInt("cant"))
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlStockCat.getRowCount()==0) mdlStockCat.addRow(new Object[]{"Sin datos.", ""});
    }

    private void cargarLowList() {
        mdlLowList.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT p.nombre,
                           COALESCE(SUM(i.stock_actual),0) st,
                           COALESCE(MIN(i.stock_minimo),0) smin
                    FROM producto p
                    LEFT JOIN inventario i ON i.id_producto=p.id_producto
                    GROUP BY p.id_producto, p.nombre
                    HAVING COALESCE(SUM(i.stock_actual),0) <= COALESCE(MIN(i.stock_minimo),0)
                    ORDER BY st ASC, p.nombre ASC
                    LIMIT 10
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlLowList.addRow(new Object[]{
                            rs.getString("nombre"),
                            rs.getInt("st"),
                            rs.getInt("smin")
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlLowList.getRowCount()==0) mdlLowList.addRow(new Object[]{"Sin alertas.", "", ""});
    }

    private void cargarPedPend() {
        mdlPedPend.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT p.id_pedido, pr.nombre AS proveedor, ep.nombre_estado AS estado
                    FROM pedido p
                    LEFT JOIN estado_pedido ep ON ep.id_estado_pedido=p.id_estado_pedido
                    LEFT JOIN proveedor pr     ON pr.id_proveedor=p.id_proveedor
                    WHERE p.id_estado_pedido = 1
                    ORDER BY p.id_pedido DESC
                    LIMIT 10
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlPedPend.addRow(new Object[]{
                            "#"+rs.getInt("id_pedido"),
                            rs.getString("proveedor"),
                            rs.getString("estado")
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlPedPend.getRowCount()==0) mdlPedPend.addRow(new Object[]{"", "No hay pedidos pendientes.", ""});
    }

    private void cargarMovRec() {
        mdlMovRec.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT m.id_movimiento, tm.nombre_tipo AS tipo,
                           SUM(md.cantidad) as unidades
                    FROM movimiento m
                    LEFT JOIN tipo_movimiento tm ON tm.id_tipo_movimiento=m.id_tipo_movimiento
                    LEFT JOIN movimiento_detalle md ON md.id_movimiento=m.id_movimiento
                    GROUP BY m.id_movimiento, tm.nombre_tipo
                    ORDER BY m.id_movimiento DESC
                    LIMIT 10
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mdlMovRec.addRow(new Object[]{
                            "#"+rs.getInt("id_movimiento"),
                            rs.getString("tipo"),
                            nf0(rs.getInt("unidades"))
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlMovRec.getRowCount()==0) mdlMovRec.addRow(new Object[]{"", "Sin movimientos.", ""});
    }

    private void cargarAlertas() {
        mdlAlertas.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("""
                    SELECT a.id_alerta, ta.nombre_tipo AS tipo, p.nombre AS producto, a.atendida
                    FROM alerta a
                    LEFT JOIN tipo_alerta ta ON ta.id_tipo_alerta=a.id_tipo_alerta
                    LEFT JOIN producto p     ON p.id_producto=a.id_producto
                    ORDER BY a.id_alerta DESC
                    LIMIT 10
                    """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String atendida = (rs.getObject("atendida")==null) ? "—"
                            : (rs.getInt("atendida")==1 ? "Sí" : "No");
                    mdlAlertas.addRow(new Object[]{
                            "#"+rs.getInt("id_alerta"),
                            rs.getString("tipo"),
                            rs.getString("producto"),
                            atendida
                    });
                }
            }
        } catch (Exception ex) { showErr(ex); }
        if (mdlAlertas.getRowCount()==0) mdlAlertas.addRow(new Object[]{"", "Sin alertas registradas.", "", ""});
    }

    private JPanel cardShell() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(16,16,16,16)
        ));
        return p;
    }
    private JPanel makeCard(String titulo, JComponent right, JComponent body){
        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel(titulo);
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setForeground(estilos.COLOR_TITULO);
        head.add(h, BorderLayout.WEST);
        if (right!=null) head.add(right, BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));
        card.add(head);
        card.add(body);
        return card;
    }
    private JPanel cardKPISimple(String titulo, JLabel big, String sub, JLabel small){
        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(titulo);
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setForeground(estilos.COLOR_TITULO);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(h, BorderLayout.WEST);
        top.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));

        JLabel subLb = new JLabel(sub);
        subLb.setForeground(new Color(110,110,110));

        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        subRow.setOpaque(false);
        subRow.add(subLb);
        subRow.add(small);

        card.add(top);
        big.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(big);
        card.add(Box.createVerticalStrut(4));
        card.add(subRow);
        return card;
    }

    private JScrollPane tableWrap(JTable t){
        JScrollPane sc = new JScrollPane(t,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(6,6,6,6)
        ));
        return sc;
    }

    private JTable buildTable(DefaultTableModel m){
        JTable t = new JTable(m);
        t.setFont(new Font("Arial", Font.PLAIN, 16));
        t.setRowHeight(30);
        JTableHeader th = t.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 16));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF,0xF3,0xD9));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEDE3D2));
        t.setIntercellSpacing(new Dimension(0,1));
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        t.setDefaultRenderer(Object.class, left);
        return t;
    }
    private void setColWidth(JTable t, int col, int w){
        TableColumn c = t.getColumnModel().getColumn(col);
        c.setPreferredWidth(w); c.setMinWidth(60);
    }
    private void estilizarInput(JTextField f){
        f.setFont(new Font("Arial", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true),
                new EmptyBorder(8,12,8,12)
        ));
        f.setBackground(Color.WHITE);
    }
    private JLabel big(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.BOLD, 26));
        l.setForeground(new Color(50,50,50));
        return l;
    }
    private JLabel smallMuted(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.PLAIN, 14));
        l.setForeground(new Color(120,120,120));
        return l;
    }
    private JLabel smallMutedRed(){
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.PLAIN, 14));
        l.setForeground(new Color(0xB9,0x4A,0x48));
        return l;
    }

    static class BadgeNoRenderer implements TableCellRenderer {
        private final PillLabel lbl = new PillLabel();
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            int v = 0; try { v = Integer.parseInt(String.valueOf(value).replaceAll("\\D","")); } catch(Exception ignore){}
            lbl.configure(String.valueOf(v),
                    estilos.BADGE_NO_BG, estilos.BADGE_NO_BORDER, estilos.BADGE_NO_FG);
            lbl.setSelection(isSelected);
            return lbl;
        }
    }
    static class PillLabel extends JComponent {
        private String text=""; private Color bg=Color.LIGHT_GRAY,border=Color.GRAY,fg=Color.BLACK;
        private boolean selected=false;
        void configure(String t, Color bg, Color border, Color fg){
            this.text=t; this.bg=bg; this.border=border; this.fg=fg;
            setPreferredSize(new Dimension(60,22));
        }
        void setSelection(boolean b){ this.selected=b; }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(); int arc=h;
            g2.setColor(selected?new Color(bg.getRed(),bg.getGreen(),bg.getBlue(),230):bg);
            g2.fillRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(border);
            g2.drawRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(Font.BOLD,12f));
            FontMetrics fm=g2.getFontMetrics();
            int tw=fm.stringWidth(text); int tx=(w-tw)/2; int ty=h/2+fm.getAscent()/2-3;
            g2.drawString(text, Math.max(8,tx), ty);
            g2.dispose();
        }
    }

    private static int getInt(Connection cn, String sql, Object[] params) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (params!=null) for (int i=0;i<params.length;i++) ps.setObject(i+1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next()? rs.getInt(1):0; }
        }
    }
    private static double getDouble(Connection cn, String sql, Object[] params) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (params!=null) for (int i=0;i<params.length;i++) ps.setObject(i+1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next()? rs.getDouble(1):0.0; }
        }
    }
    private String nf0(int n){ return String.format("%,d", n).replace(',', '.'); }
    private String nf2(double n){
        String s = String.format("%,.2f", n);
        return s.replace(',', 'X').replace('.', ',').replace('X','.');
    }
    private void showErr(Exception ex){
        JOptionPane.showMessageDialog(this, "Error: "+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
    }

    /* Conexión  */
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root";
            String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
