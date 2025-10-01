package admin.alertas;

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

public class panel_alertas extends JPanel {

    /* ====== KPIs ====== */
    private JLabel kpiActivas, kpiAtendidas, kpiTotal;
    private JTextField tfDiasSinVentas;

    /* ====== Chips ====== */
    // Estado: 0=Activas, 1=Atendidas, 2=Todas
    private final Map<Integer, JToggleButton> chipEstado = new LinkedHashMap<>();
    private int estadoSel = 0;
    // Tipo: 0=Todas, 1=SB, 2=SS, 3=NV
    private final Map<Integer, JToggleButton> chipTipo = new LinkedHashMap<>();
    private int tipoSel = 0;

    /* ====== Filtros ====== */
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbProveedor, cbCategoria, cbSucursal;
    private JTextField tfDesde, tfHasta;
    private JButton btnFiltrar;

    /* ====== Acciones ====== */
    private JButton btnGenerar, btnAplicarDias;
    private JButton btnAtenderSel, btnReabrirSel, btnEliminarSel;

    /* ====== Tabla ====== */
    private JTable tabla;
    private DefaultTableModel model;

    /* ====== Catálogos ====== */
    private List<Item> proveedores = new ArrayList<>();
    private List<Item> categorias  = new ArrayList<>();
    private List<Item> sucursales  = new ArrayList<>();

    public panel_alertas() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH; // <<<<<< clave: llenar también en vertical
        gbc.anchor=GridBagConstraints.PAGE_START;

        // Card principal
        JPanel card = cardShell();
        card.setLayout(new BorderLayout(0, 10));
        card.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stack superior con todo menos la tabla
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        /* ====== Header ====== */
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel h1 = new JLabel("Alertas");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        header.add(h1, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnGenerar = estilos.botonRedondeado("Generar ahora");
        btnGenerar.setPreferredSize(new Dimension(160,40));
        actions.add(btnGenerar);

        tfDiasSinVentas = new JTextField("30");
        tfDiasSinVentas.setToolTipText("Días sin ventas");
        tfDiasSinVentas.setPreferredSize(new Dimension(80,38));
        estilizarCampo(tfDiasSinVentas);
        actions.add(tfDiasSinVentas);

        btnAplicarDias = estilos.botonBlanco("Aplicar días sin ventas");
        btnAplicarDias.setPreferredSize(new Dimension(220,38));
        actions.add(btnAplicarDias);

        header.add(actions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));
        stack.add(header);

        /* ====== KPIs ====== */
        stack.add(kpisFila());

        /* ====== Chips ====== */
        stack.add(chipsEstadoFila());
        stack.add(chipsTipoFila());

        /* ====== Filtros ====== */
        stack.add(filtrosFila());

        /* ====== Toolbar masiva ====== */
        stack.add(toolbarMasiva());

        // Colocar el stack arriba y la tabla al centro del card
        card.add(stack, BorderLayout.NORTH);

        /* ====== Tabla ====== */
        String[] cols = {
                "Sel", "#", "Tipo", "Producto", "Sucursal", "Stock",
                "Proveedor", "Creada", "ver", "estado", "eliminar"
        };
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){
                return c==0 || c==8 || c==9 || c==10;
            }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex==0) return Boolean.class;
                if (columnIndex==8 || columnIndex==9 || columnIndex==10) return JButton.class;
                return Object.class;
            }
        };
        tabla = new JTable(model);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // <<<<<< no achicar columnas; permitir scroll horizontal
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
        tabla.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(300);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(170);
        tabla.getColumnModel().getColumn(8).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(9).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(10).setPreferredWidth(110);

        // Render ID como "#"
        tabla.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText("#"+String.valueOf(value));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        // Render Tipo como badge (SB/SS/NV)
        tabla.getColumnModel().getColumn(2).setCellRenderer(new TipoBadgeRenderer());

        // Stock alineado
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.getColumnModel().getColumn(5).setCellRenderer(right);

        // Botones
        tabla.getColumnModel().getColumn(8).setCellRenderer(new ButtonCellRenderer(false)); // Ver
        tabla.getColumnModel().getColumn(8).setCellEditor(new ButtonCellEditor(tabla, id -> onVer(id), false));

        tabla.getColumnModel().getColumn(9).setCellRenderer(new ButtonCellRenderer(false)); // Atender/Reabrir
        tabla.getColumnModel().getColumn(9).setCellEditor(new EstadoCellEditor(tabla, this::onToggleEstado));

        tabla.getColumnModel().getColumn(10).setCellRenderer(new ButtonCellRenderer(true)); // Eliminar
        tabla.getColumnModel().getColumn(10).setCellEditor(new ButtonCellEditor(tabla, id -> onEliminar(id), true));

        JScrollPane sc = new JScrollPane(
                tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        sc.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_CREMA,1,true), new EmptyBorder(6,6,6,6)));
        // No ponemos preferredSize fija: el BorderLayout.CENTER hará que use TODO el espacio disponible
        card.add(sc, BorderLayout.CENTER);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        /* ====== Eventos ====== */
        btnFiltrar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());
        btnGenerar.addActionListener(e -> generarAlertas());
        btnAplicarDias.addActionListener(e -> cargarTabla());

        btnAtenderSel.addActionListener(e -> accionMasiva("atender"));
        btnReabrirSel.addActionListener(e -> accionMasiva("reabrir"));
        btnEliminarSel.addActionListener(e -> accionMasiva("eliminar"));

        chipEstado.forEach((k,b)-> b.addActionListener(e -> { estadoSel=k; marcarChip(chipEstado,k); cargarTabla(); }));
        chipTipo.forEach((k,b)-> b.addActionListener(e -> { tipoSel=k; marcarChip(chipTipo,k); cargarTabla(); }));

        /* ====== Carga inicial ====== */
        tfHasta.setText(LocalDate.now().toString());
        tfDesde.setText(LocalDate.now().minusDays(30).toString());
        cargarCombos();
        cargarKPIs();
        cargarTabla();
    }

    /* ====== UI helpers ====== */
    private JPanel kpisFila(){
        JPanel row = new JPanel(new GridLayout(1,3,16,16));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0,0,8,0));
        kpiActivas   = kpiBig(); kpiAtendidas = kpiBig(); kpiTotal = kpiBig();
        row.add(kpiCard("Activas", kpiActivas, "Alertas pendientes"));
        row.add(kpiCard("Atendidas", kpiAtendidas, "Histórico reciente"));
        row.add(kpiCard("Total", kpiTotal, "Activas + Atendidas"));
        return row;
    }
    private JPanel chipsEstadoFila(){
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); chips.setOpaque(false);
        String[] labels = {"Activas","Atendidas","Todas"};
        for (int i=0;i<labels.length;i++){ var b = makeChip(labels[i]); chipEstado.put(i,b); chips.add(b); }
        marcarChip(chipEstado, 0);
        JLabel h = new JLabel("Estado"); h.setFont(new Font("Arial", Font.BOLD, 16)); h.setForeground(estilos.COLOR_TITULO);
        JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.add(h,BorderLayout.WEST);
        wrap.add(head,BorderLayout.NORTH);
        JPanel pad=new JPanel(new BorderLayout()); pad.setOpaque(false); pad.setBorder(new EmptyBorder(6,0,6,0)); pad.add(chips,BorderLayout.WEST);
        wrap.add(pad,BorderLayout.CENTER);
        return wrap;
    }
    private JPanel chipsTipoFila(){
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); chips.setOpaque(false);
        String[] labels = {"Todas","Stock bajo","Sin stock","Sin ventas"};
        for (int i=0;i<labels.length;i++){ var b = makeChip(labels[i]); chipTipo.put(i,b); chips.add(b); }
        marcarChip(chipTipo, 0);
        JLabel h = new JLabel("Tipo"); h.setFont(new Font("Arial", Font.BOLD, 16)); h.setForeground(estilos.COLOR_TITULO);
        JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.add(h,BorderLayout.WEST);
        wrap.add(head,BorderLayout.NORTH);
        JPanel pad=new JPanel(new BorderLayout()); pad.setOpaque(false); pad.setBorder(new EmptyBorder(6,0,8,0)); pad.add(chips,BorderLayout.WEST);
        wrap.add(pad,BorderLayout.CENTER);
        return wrap;
    }
    private JPanel filtrosFila(){
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(true);
        box.setBackground(new Color(0xF7,0xE9,0xD0));
        box.setBorder(new CompoundBorder(new LineBorder(new Color(0xD8,0xC3,0xA3),1,true),new EmptyBorder(12,12,12,12)));

        txtBuscar = new PlaceholderTextField("Buscar producto / código…");
        estilos.estilizarCampo(txtBuscar); txtBuscar.setPreferredSize(new Dimension(260,38));

        cbProveedor = new JComboBox<>(); estilos.estilizarCombo(cbProveedor); cbProveedor.setPreferredSize(new Dimension(220,38));
        cbCategoria = new JComboBox<>(); estilos.estilizarCombo(cbCategoria); cbCategoria.setPreferredSize(new Dimension(220,38));
        cbSucursal  = new JComboBox<>(); estilos.estilizarCombo(cbSucursal);  cbSucursal.setPreferredSize(new Dimension(200,38));

        tfDesde = new JTextField(); tfHasta = new JTextField(); estilizarFecha(tfDesde); estilizarFecha(tfHasta);
        tfDesde.setPreferredSize(new Dimension(160,38)); tfHasta.setPreferredSize(new Dimension(160,38));

        btnFiltrar = estilos.botonBlanco("FILTRAR"); btnFiltrar.setPreferredSize(new Dimension(120,38));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy=0; g.insets=new Insets(4,4,4,8); g.fill=GridBagConstraints.HORIZONTAL;
        int x=0;
        g.gridx=x++; g.weightx=1;  box.add(txtBuscar,g);
        g.gridx=x++; g.weightx=0;  box.add(cbProveedor,g);
        g.gridx=x++;               box.add(cbCategoria,g);
        g.gridx=x++;               box.add(cbSucursal,g);
        g.gridx=x++;               box.add(tfDesde,g);
        g.gridx=x++;               box.add(tfHasta,g);
        g.gridx=x;                 box.add(btnFiltrar,g);

        row.add(box, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(0,0,12,0));
        return row;
    }
    private JPanel toolbarMasiva(){
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        bar.setOpaque(false);
        btnAtenderSel = estilos.botonSm("Atender seleccionadas");
        btnReabrirSel = estilos.botonSm("Reabrir seleccionadas");
        btnEliminarSel= estilos.botonSmDanger("Eliminar seleccionadas");
        bar.add(btnAtenderSel); bar.add(btnReabrirSel); bar.add(btnEliminarSel);
        bar.setBorder(new EmptyBorder(0,0,8,0));
        return bar;
    }

    private JPanel kpiCard(String titulo, JLabel big, String sub){
        JPanel p = cardInner(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel h = new JLabel(titulo); h.setFont(new Font("Arial", Font.BOLD, 18)); h.setForeground(estilos.COLOR_TITULO);
        JLabel s = new JLabel(sub); s.setForeground(new Color(110,110,110));
        p.add(h); p.add(big); p.add(Box.createVerticalStrut(4)); p.add(s);
        return p;
    }
    private JLabel kpiBig(){ JLabel l=new JLabel("—"); l.setFont(new Font("Arial", Font.BOLD, 26)); l.setForeground(new Color(50,50,50)); return l; }
    private JPanel cardShell(){ JPanel p=new JPanel(); p.setOpaque(true); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_CREMA,1,true), new EmptyBorder(16,16,18,16))); return p; }
    private JPanel cardInner(){ JPanel p=new JPanel(); p.setOpaque(true); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_CREMA,1,true), new EmptyBorder(16,16,16,16))); return p; }
    private void estilizarCampo(JTextField f){ f.setFont(new Font("Arial",Font.PLAIN,14));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0xD9,0xD9,0xD9),1,true), new EmptyBorder(8,12,8,12))); f.setBackground(Color.WHITE); }
    private void estilizarFecha(JTextField f){ estilizarCampo(f); f.setToolTipText("AAAA-MM-DD"); }

    private JToggleButton makeChip(String text){
        JToggleButton b=new JToggleButton(text); b.setFocusPainted(false); b.setContentAreaFilled(true); b.setOpaque(true);
        b.setFont(new Font("Arial", Font.PLAIN,14));
        b.setBorder(new CompoundBorder(new LineBorder(new Color(0xD8,0xC3,0xA3),1,true), new EmptyBorder(6,12,6,12)));
        b.setBackground(new Color(0xF7,0xE9,0xD0)); b.setForeground(new Color(0x33,0x33,0x33));
        b.addChangeListener(e->{
            if (b.isSelected()){
                b.setBackground(new Color(0xFF,0xF3,0xD9));
                b.setBorder(new CompoundBorder(new LineBorder(new Color(0xF1,0xD5,0xA3),1,true),new EmptyBorder(6,12,6,12)));
            } else {
                b.setBackground(new Color(0xF7,0xE9,0xD0));
                b.setBorder(new CompoundBorder(new LineBorder(new Color(0xD8,0xC3,0xA3),1,true),new EmptyBorder(6,12,6,12)));
            }
        });
        return b;
    }
    private void marcarChip(Map<Integer,JToggleButton> map, int key){
        ButtonGroup bg=new ButtonGroup(); map.forEach((k,b)->{ bg.add(b); b.setSelected(k==key); });
    }

    /* ====== Carga de catálogos ====== */
    private void cargarCombos(){
        proveedores.clear(); categorias.clear(); sucursales.clear();
        proveedores.add(new Item(0,"Todos los proveedores"));
        categorias.add(new Item(0,"Todas las categorías"));
        sucursales.add(new Item(0,"Todas las sucursales"));

        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_proveedor,nombre FROM proveedor ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) proveedores.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_categoria,nombre FROM categoria ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) categorias.add(new Item(rs.getInt(1), rs.getString(2)));
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_sucursal,nombre FROM sucursal ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()){
                while (rs.next()) sucursales.add(new Item(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando catálogos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
        cbProveedor.setModel(new DefaultComboBoxModel<>(proveedores.toArray(new Item[0])));
        cbCategoria.setModel(new DefaultComboBoxModel<>(categorias.toArray(new Item[0])));
        cbSucursal.setModel(new DefaultComboBoxModel<>(sucursales.toArray(new Item[0])));
    }

    /* ====== KPIs ====== */
    private void cargarKPIs(){
        int act=0, aten=0;
        try (Connection cn = DB.get()){
            act  = getInt(cn, "SELECT COUNT(*) FROM alerta WHERE atendida=0");
            aten = getInt(cn, "SELECT COUNT(*) FROM alerta WHERE atendida=1");
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error KPIs:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
        kpiActivas.setText(nf0(act));
        kpiAtendidas.setText(nf0(aten));
        kpiTotal.setText(nf0(act+aten));
    }

    /* ====== Tabla ====== */
    private void cargarTabla(){
        model.setRowCount(0);

        String q = txtBuscar.getText()==null? "" : txtBuscar.getText().trim();
        int idProv = ((Item)cbProveedor.getSelectedItem()).id();
        int idCat  = ((Item)cbCategoria.getSelectedItem()).id();
        int idSuc  = ((Item)cbSucursal.getSelectedItem()).id();
        String desde = tfDesde.getText().trim();
        String hasta = tfHasta.getText().trim();

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // estado
        if (estadoSel==0) { where.append(" AND a.atendida=0 "); }
        else if (estadoSel==1) { where.append(" AND a.atendida=1 "); }

        // tipo
        if (tipoSel>0) { where.append(" AND a.id_tipo_alerta=? "); params.add(tipoSel); }

        if (!q.isEmpty()){
            where.append(" AND (p.nombre LIKE ? OR p.codigo LIKE ?) ");
            params.add("%"+q+"%"); params.add("%"+q+"%");
        }
        if (idProv>0){ where.append(" AND p.id_proveedor=? "); params.add(idProv); }
        if (idCat>0) { where.append(" AND sc.id_categoria=? "); params.add(idCat); }
        if (idSuc>0) { where.append(" AND i.id_sucursal=? ");  params.add(idSuc); }
        if (!desde.isEmpty()){ where.append(" AND DATE(a.fecha_creada)>=? "); params.add(desde); }
        if (!hasta.isEmpty()){ where.append(" AND DATE(a.fecha_creada)<=? "); params.add(hasta); }

        String sql = """
            SELECT a.id_alerta, a.id_tipo_alerta, a.atendida, a.fecha_creada,
                   p.id_producto, p.nombre AS producto, p.codigo,
                   i.id_inventario, i.id_sucursal, i.stock_actual, i.stock_minimo,
                   s.nombre AS sucursal,
                   pr.nombre AS proveedor,
                   ta.nombre_tipo
              FROM alerta a
              JOIN inventario i ON i.id_inventario=a.id_inventario
              JOIN sucursal s   ON s.id_sucursal=i.id_sucursal
              JOIN producto p   ON p.id_producto=a.id_producto
              LEFT JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
              JOIN tipo_alerta ta ON ta.id_tipo_alerta=a.id_tipo_alerta
              LEFT JOIN subcategoria sc ON sc.id_subcategoria=p.id_subcategoria
            """ + where + """
             ORDER BY a.atendida ASC, a.fecha_creada DESC
             LIMIT 500
        """;

        try (Connection cn = DB.get(); PreparedStatement ps = cn.prepareStatement(sql)){
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int id = rs.getInt("id_alerta");
                    boolean atendida = rs.getInt("atendida")==1;
                    String producto = rs.getString("producto");
                    String codigo   = rs.getString("codigo");
                    String suc      = rs.getString("sucursal");
                    int stock = rs.getInt("stock_actual");
                    int min   = rs.getInt("stock_minimo");
                    String prov     = rs.getString("proveedor");
                    String creada   = rs.getString("fecha_creada");
                    String tipoTxt  = rs.getString("nombre_tipo");

                    model.addRow(new Object[]{
                            Boolean.FALSE,
                            id,
                            tipoTxt,
                            producto + "  " + (codigo==null? "" : "(" + codigo + ")"),
                            suc,
                            stock + " / min " + min,
                            prov==null? "—" : prov,
                            creada,
                            "Ver",
                            atendida ? "Reabrir" : "Atender",
                            "Eliminar"
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando alertas:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }

        if (model.getRowCount()==0){
            model.addRow(new Object[]{false,"","Sin resultados.","","","","","","","",""});
        }

        cargarKPIs();
    }

    /* ====== Acciones fila ====== */
    private void onVer(int id){
        Window owner = SwingUtilities.getWindowAncestor(this);
        ver dlg = new ver(owner, id);
        dlg.setVisible(true);
        if (dlg.huboCambios()) cargarTabla();
    }
    private void onToggleEstado(int id){
        try (Connection cn = DB.get()){
            boolean estaAtendida = getInt(cn, "SELECT atendida FROM alerta WHERE id_alerta="+id)==1;
            String sql = estaAtendida
                    ? "UPDATE alerta SET atendida=0, fecha_atendida=NULL, atendida_por=NULL WHERE id_alerta=?"
                    : "UPDATE alerta SET atendida=1, fecha_atendida=NOW(), atendida_por=? WHERE id_alerta=?";
            try (PreparedStatement ps = cn.prepareStatement(sql)){
                if (estaAtendida){
                    ps.setInt(1, id);
                } else {
                    int uid = getInt(cn, "SELECT COALESCE(MAX(id_usuario),0) FROM usuario");
                    ps.setInt(1, uid); ps.setInt(2, id);
                }
                ps.executeUpdate();
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo cambiar estado:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }
    private void onEliminar(int id){
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar alerta #"+id+"?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok!=JOptionPane.YES_OPTION) return;
        try (Connection cn = DB.get(); PreparedStatement ps = cn.prepareStatement("DELETE FROM alerta WHERE id_alerta=?")){
            ps.setInt(1, id); ps.executeUpdate();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo eliminar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    /* ====== Acciones masivas ====== */
    private List<Integer> idsSeleccionados(){
        List<Integer> ids = new ArrayList<>();
        for (int r=0; r<model.getRowCount(); r++){
            Object sel = model.getValueAt(r,0);
            if (sel instanceof Boolean b && b){
                Object idObj = model.getValueAt(r,1);
                try { ids.add(Integer.parseInt(String.valueOf(idObj))); } catch (Exception ignore){}
            }
        }
        return ids;
    }
    private void accionMasiva(String op){
        List<Integer> ids = idsSeleccionados();
        if (ids.isEmpty()){ JOptionPane.showMessageDialog(this, "No seleccionaste alertas."); return; }

        String sql;
        if ("atender".equals(op)){
            sql = "UPDATE alerta SET atendida=1, fecha_atendida=NOW(), atendida_por=NULL WHERE id_alerta=?";
        } else if ("reabrir".equals(op)){
            sql = "UPDATE alerta SET atendida=0, fecha_atendida=NULL, atendida_por=NULL WHERE id_alerta=?";
        } else { // eliminar
            int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar "+ids.size()+" alerta(s)?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok!=JOptionPane.YES_OPTION) return;
            sql = "DELETE FROM alerta WHERE id_alerta=?";
        }

        try (Connection cn = DB.get(); PreparedStatement ps = cn.prepareStatement(sql)){
            for (Integer id : ids){ ps.setInt(1, id); ps.addBatch(); }
            ps.executeBatch();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Acción masiva falló:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    /* ====== Generar (sin e-mail) ====== */
    private void generarAlertas(){
        int dias = 30;
        try { dias = Math.max(1, Integer.parseInt(tfDiasSinVentas.getText().trim())); } catch (Exception ignore){}
        try (Connection cn = DB.get()){
            cn.setAutoCommit(false);
            int sb=0, ss=0, nv=0;

            // helper: si no existe alerta activa del tipo, insertarla
            PreparedStatement chk = cn.prepareStatement("SELECT 1 FROM alerta WHERE id_inventario=? AND id_tipo_alerta=? AND atendida=0 LIMIT 1");
            PreparedStatement ins = cn.prepareStatement("INSERT INTO alerta (id_producto,id_inventario,id_tipo_alerta,atendida,fecha_creada) VALUES (?,?,?,0,NOW())");

            // STOCK BAJO
            try (PreparedStatement ps = cn.prepareStatement("""
                SELECT i.id_inventario, i.id_producto
                  FROM inventario i
                 WHERE i.stock_actual>0 AND i.stock_actual<=i.stock_minimo
            """); ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int inv=rs.getInt(1), prod=rs.getInt(2);
                    if (!existeAlertaActiva(chk, inv, 1)){ insertarAlerta(ins, prod, inv, 1); sb++; }
                }
            }
            // SIN STOCK
            try (PreparedStatement ps = cn.prepareStatement("""
                SELECT i.id_inventario, i.id_producto
                  FROM inventario i
                 WHERE i.stock_actual=0
            """); ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int inv=rs.getInt(1), prod=rs.getInt(2);
                    if (!existeAlertaActiva(chk, inv, 2)){ insertarAlerta(ins, prod, inv, 2); ss++; }
                }
            }
            // SIN VENTAS (dias)
            try (PreparedStatement ps = cn.prepareStatement("""
                SELECT DISTINCT p.id_producto, i.id_inventario
                  FROM producto p
                  JOIN inventario i ON i.id_producto=p.id_producto
                 WHERE p.activo=1
                   AND NOT EXISTS (
                      SELECT 1 FROM venta_detalle vd
                      JOIN venta v ON v.id_venta=vd.id_venta
                      WHERE vd.id_producto=p.id_producto
                        AND v.fecha_hora >= DATE_SUB(NOW(), INTERVAL ? DAY)
                   )
            """)){
                ps.setInt(1, dias);
                try (ResultSet rs = ps.executeQuery()){
                    while (rs.next()){
                        int prod=rs.getInt(1), inv=rs.getInt(2);
                        if (!existeAlertaActiva(chk, inv, 3)){ insertarAlerta(ins, prod, inv, 3); nv++; }
                    }
                }
            }

            cn.commit();
            JOptionPane.showMessageDialog(this,
                    "Generadas: " + (sb+ss+nv) + "  (SB "+sb+" | SS "+ss+" | NV "+nv+")",
                    "Alertas", JOptionPane.INFORMATION_MESSAGE);
            cargarTabla();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error generando alertas:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }
    private boolean existeAlertaActiva(PreparedStatement chk, int idInventario, int tipo) throws Exception {
        chk.clearParameters(); chk.setInt(1,idInventario); chk.setInt(2,tipo);
        try (ResultSet rs = chk.executeQuery()){ return rs.next(); }
    }
    private void insertarAlerta(PreparedStatement ins, int idProducto, int idInventario, int tipo) throws Exception {
        ins.clearParameters(); ins.setInt(1,idProducto); ins.setInt(2,idInventario); ins.setInt(3,tipo); ins.executeUpdate();
    }

    /* ====== Renderers / Celdas ====== */
    static class TipoBadgeRenderer implements TableCellRenderer {
        private final PillLabel lbl = new PillLabel();
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean hf, int r, int c) {
            String txt = String.valueOf(v);
            Color bg=estilos.BADGE_NO_BG, bd=estilos.BADGE_NO_BORDER, fg=estilos.BADGE_NO_FG;
            if (txt.toLowerCase().contains("stock bajo")) { bg = new Color(0xFF,0xF2,0xCC); bd=new Color(0xFF,0xE0,0x8A); fg=new Color(0x6B,0x55,0x00); }
            else if (txt.toLowerCase().contains("sin stock")) { bg = new Color(0xFF,0xD6,0xD6); bd=new Color(0xFF,0x9C,0x9C); fg=new Color(0x8B,0x00,0x00); }
            else if (txt.toLowerCase().contains("sin ventas")){ bg = new Color(0xE5,0xF0,0xFF); bd=new Color(0xB9,0xD2,0xFF); fg=new Color(0x1F,0x5F,0xA6); }
            lbl.configure(txt,bg,bd,fg); lbl.setSelection(sel); return lbl;
        }
    }
    static class ButtonCellRenderer extends JButton implements TableCellRenderer {
        private final boolean danger;
        ButtonCellRenderer(boolean danger){ this.danger=danger; setOpaque(true); setBorderPainted(false); setFocusPainted(false); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean hf, int r, int c) {
            return danger ? estilos.botonSmDanger(String.valueOf(v)) : estilos.botonSm(String.valueOf(v));
        }
    }
    static class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table; private final JButton button; private final Consumer<Integer> onClick;
        ButtonCellEditor(JTable table, Consumer<Integer> onClick, boolean danger){
            this.table=table; this.onClick=onClick;
            this.button = danger? estilos.botonSmDanger("Eliminar") : estilos.botonSm(String.valueOf("Ver"));
            this.button.addActionListener(this::handle);
        }
        private void handle(ActionEvent e){
            int viewRow = this.table.getEditingRow();
            if (viewRow>=0){
                int modelRow = this.table.convertRowIndexToModel(viewRow);
                Object idObj = this.table.getModel().getValueAt(modelRow, 1);
                int id=0; try{ id=Integer.parseInt(String.valueOf(idObj)); }catch(Exception ignore){}
                this.onClick.accept(id);
            }
            fireEditingStopped();
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable table, Object v, boolean s, int r, int c) {
            return button;
        }
    }
    /** Botón que alterna entre Atender / Reabrir según estado actual en BD */
    static class EstadoCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button;
        private final Consumer<Integer> onClick;

        EstadoCellEditor(JTable table, Consumer<Integer> onClick){
            this.table = table;
            this.onClick = onClick;
            this.button = estilos.botonSm("Atender");

            this.button.addActionListener(e -> {
                int viewRow = this.table.getEditingRow();
                if (viewRow>=0){
                    int modelRow = this.table.convertRowIndexToModel(viewRow);
                    Object idObj = this.table.getModel().getValueAt(modelRow, 1); // columna 1 = ID
                    int id=0; try{ id=Integer.parseInt(String.valueOf(idObj)); }catch(Exception ignore){}
                    this.onClick.accept(id);
                }
                fireEditingStopped();
            });
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable table, Object v, boolean s, int r, int c) {
            String label = String.valueOf(v);
            button.setText(label);
            return button;
        }
    }
    static class PillLabel extends JComponent {
        private String text=""; private Color bg=Color.LIGHT_GRAY,border=Color.GRAY,fg=Color.BLACK;
        private boolean selected=false;
        void configure(String t, Color bg, Color border, Color fg){ this.text=t; this.bg=bg; this.border=border; this.fg=fg; setPreferredSize(new Dimension(110,22)); }
        void setSelection(boolean b){ this.selected=b; }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight(), arc=h;
            g2.setColor(selected?new Color(bg.getRed(),bg.getGreen(),bg.getBlue(),230):bg);
            g2.fillRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(border); g2.drawRoundRect(4,(h-18)/2,w-8,18,arc,arc);
            g2.setColor(fg); g2.setFont(getFont().deriveFont(Font.BOLD,12f));
            FontMetrics fm=g2.getFontMetrics();
            int tw=fm.stringWidth(text), tx=(w-tw)/2, ty=h/2+fm.getAscent()/2-3;
            g2.drawString(text, Math.max(8,tx), ty);
            g2.dispose();
        }
    }

    /* ====== Utils ====== */
    private void bind(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i=0;i<params.size();i++){
            Object v=params.get(i);
            if (v instanceof Integer iv) ps.setInt(i+1, iv);
            else ps.setString(i+1, String.valueOf(v));
        }
    }
    private String nf0(int n){ return String.format("%,d", n).replace(',', '.'); }

    /* ====== DB helpers ====== */
    static class DB { static Connection get() throws Exception { return conexion_bd.getConnection(); } }
    private static int getInt(Connection cn, String sql) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()){ return rs.next()? rs.getInt(1) : 0; }
    }

    /* ====== Tipos ====== */
    static class Item {
        private final int id; private final String nombre;
        Item(int id, String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        @Override public String toString(){ return nombre; }
    }

    /* ====== Input con placeholder ====== */
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder){ this.placeholder=placeholder; setFont(new Font("Arial", Font.PLAIN, 14)); setOpaque(true); }
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
