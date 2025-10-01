package admin.auditorias;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class panel_auditorias extends JPanel {

    /* ====== Filtros ====== */
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbUsuario;
    private JComboBox<String> cbAccion;
    private JComboBox<String> cbEntidad;
    private JTextField tfDesde, tfHasta;
    private JButton btnFiltrar;

    /* ====== Tabla ====== */
    private JTable tabla;
    private DefaultTableModel model;

    /* ====== Catálogos ====== */
    private final List<Item> usuarios = new ArrayList<>();
    private static final String[] ACCIONES = {
            "", "ALTA","MODIFICACION","BAJA","CAMBIO_ESTADO",
            "MOV_TRASLADO","MOV_ENTRADA","MOV_SALIDA",
            "INGRESO","EGRESO","AJUSTE","VENTA","ALERTA_CREADA","ALERTA_ATENDIDA"
    };
    private static final String[] ENTIDADES = {
            "", "producto","usuario","pedido","movimiento","inventario_mov","venta","alerta"
    };

    public panel_auditorias(){
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.BOTH; // <- para que el card crezca en alto
        gbc.anchor=GridBagConstraints.PAGE_START;

        JPanel card = cardShell();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1100,Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        /* ====== Header ====== */
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel h1 = new JLabel("Panel administrativo — Auditorías");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        header.add(h1, BorderLayout.WEST);
        header.setBorder(new EmptyBorder(0,0,8,0));
        card.add(header);

        /* ====== Filtros (2 filas) ====== */
        card.add(filtrosBloque());

        /* ====== Tabla ====== */
        String[] cols = {"Fecha","Usuario","Acción","Entidad","ID","Detalle"};
        model = new DefaultTableModel(cols,0){ @Override public boolean isCellEditable(int r,int c){ return false; } };
        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 15));
        tabla.setRowHeight(28);
        JTableHeader th = tabla.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 15));
        th.setReorderingAllowed(false);
        th.setBackground(new Color(0xFF,0xF3,0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setIntercellSpacing(new Dimension(0,1));

        // Acción como badge
        tabla.getColumnModel().getColumn(2).setCellRenderer(new AccionBadgeRenderer());

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                new EmptyBorder(6,6,6,6)
        ));
        // Altura visible para que no quede colapsada
        sc.setPreferredSize(new Dimension(0, 420));
        sc.setMinimumSize(new Dimension(0, 320));
        sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.add(sc);

        shell.add(card,gbc);
        add(shell,BorderLayout.CENTER);

        /* ====== Eventos ====== */
        btnFiltrar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());

        /* ====== Carga inicial ====== */
        tfHasta.setText(LocalDate.now().toString());
        tfDesde.setText(LocalDate.now().minusDays(30).toString());
        cargarUsuarios();
        cargarTabla();
    }

    /* ====== Bloque de filtros (dos filas) ====== */
    private JPanel filtrosBloque(){
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel box = new JPanel();
        box.setOpaque(true);
        box.setBackground(new Color(0xF7,0xE9,0xD0));
        box.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8,0xC3,0xA3),1,true),
                new EmptyBorder(12,12,12,12)
        ));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // Fila 1: buscar, usuario, acción, entidad
        JPanel row1 = new JPanel(new GridBagLayout());
        row1.setOpaque(false);

        txtBuscar=new PlaceholderTextField("Buscar en detalle…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(260,36));

        cbUsuario=new JComboBox<>();
        estilos.estilizarCombo(cbUsuario);
        cbUsuario.setPreferredSize(new Dimension(200,36));

        cbAccion=new JComboBox<>(ACCIONES);
        estilos.estilizarCombo(cbAccion);
        cbAccion.setPreferredSize(new Dimension(180,36));

        cbEntidad=new JComboBox<>(ENTIDADES);
        estilos.estilizarCombo(cbEntidad);
        cbEntidad.setPreferredSize(new Dimension(180,36));

        GridBagConstraints g1=new GridBagConstraints();
        g1.gridy=0; g1.insets=new Insets(4,4,4,8); g1.fill=GridBagConstraints.HORIZONTAL;

        int x=0;
        g1.gridx=x++; g1.weightx=1;  row1.add(txtBuscar,g1);
        g1.gridx=x++; g1.weightx=0;  row1.add(cbUsuario,g1);
        g1.gridx=x++;                row1.add(cbAccion,g1);
        g1.gridx=x++;                row1.add(cbEntidad,g1);

        // Fila 2: desde, hasta, FILTRAR
        JPanel row2 = new JPanel(new GridBagLayout());
        row2.setOpaque(false);

        tfDesde=new JTextField(); tfHasta=new JTextField();
        estilizarFecha(tfDesde); estilizarFecha(tfHasta);
        tfDesde.setPreferredSize(new Dimension(170,36));
        tfHasta.setPreferredSize(new Dimension(170,36));

        btnFiltrar=estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120,36));

        GridBagConstraints g2=new GridBagConstraints();
        g2.gridy=0; g2.insets=new Insets(4,4,4,8); g2.fill=GridBagConstraints.HORIZONTAL;

        int y=0;
        g2.gridx=y++; g2.weightx=0; row2.add(labelWrap("Desde", tfDesde), g2);
        g2.gridx=y++;               row2.add(labelWrap("Hasta", tfHasta), g2);
        g2.gridx=y;   g2.weightx=1; // empuja el botón a la derecha
        row2.add(new JPanel(){ { setOpaque(false); } }, g2);
        g2.gridx=y+1; g2.weightx=0; row2.add(btnFiltrar, g2);

        box.add(row1);
        box.add(Box.createVerticalStrut(6));
        box.add(row2);

        wrap.add(box, BorderLayout.CENTER);
        wrap.setBorder(new EmptyBorder(0,0,12,0));
        return wrap;
    }

    private JPanel labelWrap(String label, JComponent field){
        JPanel p = new JPanel(new BorderLayout(6,2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        l.setForeground(new Color(90,90,90));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /* ====== Datos ====== */
    private void cargarUsuarios(){
        usuarios.clear();
        usuarios.add(new Item(0,"Todos los usuarios"));
        try(Connection cn=DB.get();
            PreparedStatement ps=cn.prepareStatement("SELECT id_usuario, nombre FROM usuario ORDER BY nombre");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()) usuarios.add(new Item(rs.getInt(1), rs.getString(2)));
        } catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando usuarios:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
        cbUsuario.setModel(new DefaultComboBoxModel<>(usuarios.toArray(new Item[0])));
    }

    private void cargarTabla(){
        model.setRowCount(0);

        String q = txtBuscar.getText()==null ? "" : txtBuscar.getText().trim();
        Item usr = (Item) cbUsuario.getSelectedItem();
        String accion  = (String) cbAccion.getSelectedItem();
        String entidad = (String) cbEntidad.getSelectedItem();
        String desde = tfDesde.getText().trim();
        String hasta = tfHasta.getText().trim();

        String unionSql = """
          SELECT CAST(p.creado_en AS DATETIME) AS fecha, NULL AS id_usuario,
                 CAST('ALTA' AS CHAR) AS accion, CAST('producto' AS CHAR) AS entidad,
                 p.id_producto AS id_entidad,
                 CAST(CONCAT('Producto: ', COALESCE(p.nombre,'')) AS CHAR) AS detalle
          FROM producto p
          WHERE p.creado_en IS NOT NULL

          UNION ALL
          SELECT CAST(p.actualizado_en AS DATETIME), NULL,
                 CAST('MODIFICACION' AS CHAR), CAST('producto' AS CHAR),
                 p.id_producto,
                 CAST(CONCAT('Producto actualizado: ', COALESCE(p.nombre,'')) AS CHAR)
          FROM producto p
          WHERE p.actualizado_en IS NOT NULL AND p.actualizado_en <> p.creado_en

          UNION ALL
          SELECT CAST(p.fecha_baja AS DATETIME), p.baja_por,
                 CAST('BAJA' AS CHAR), CAST('producto' AS CHAR),
                 p.id_producto,
                 CAST(CONCAT('Motivo: ', COALESCE(p.motivo_baja,'')) AS CHAR)
          FROM producto p
          WHERE p.baja_por IS NOT NULL AND p.fecha_baja IS NOT NULL

          UNION ALL
          SELECT CAST(u.creado_en AS DATETIME), NULL,
                 CAST('ALTA' AS CHAR), CAST('usuario' AS CHAR),
                 u.id_usuario,
                 CAST(CONCAT('Usuario: ', COALESCE(u.nombre,'')) AS CHAR)
          FROM usuario u
          WHERE u.creado_en IS NOT NULL

          UNION ALL
          SELECT CAST(u.actualizado_en AS DATETIME), NULL,
                 CAST('MODIFICACION' AS CHAR), CAST('usuario' AS CHAR),
                 u.id_usuario,
                 CAST(CONCAT('Usuario actualizado: ', COALESCE(u.nombre,'')) AS CHAR)
          FROM usuario u
          WHERE u.actualizado_en IS NOT NULL AND u.actualizado_en <> u.creado_en

          UNION ALL
          SELECT CAST(pe.fecha_creado AS DATETIME), pe.id_usuario,
                 CAST('ALTA' AS CHAR), CAST('pedido' AS CHAR),
                 pe.id_pedido,
                 CAST(CONCAT('Proveedor #', COALESCE(pe.id_proveedor,0), ' — Sucursal #', COALESCE(pe.id_sucursal,0)) AS CHAR)
          FROM pedido pe
          WHERE pe.fecha_creado IS NOT NULL

          UNION ALL
          SELECT CAST(pe.fecha_estado AS DATETIME), pe.id_usuario,
                 CAST('CAMBIO_ESTADO' AS CHAR), CAST('pedido' AS CHAR),
                 pe.id_pedido,
                 CAST(CONCAT('Estado: ', COALESCE(ep.nombre_estado,'—')) AS CHAR)
          FROM pedido pe
          LEFT JOIN estado_pedido ep ON ep.id_estado_pedido = pe.id_estado_pedido
          WHERE pe.fecha_estado IS NOT NULL

          UNION ALL
          SELECT CAST(m.fecha_hora AS DATETIME), m.id_usuario,
                 CAST(CONCAT('MOV_', UPPER(COALESCE(tm.nombre_tipo,'desconocido'))) AS CHAR),
                 CAST('movimiento' AS CHAR),
                 m.id_movimiento,
                 CAST(CONCAT('Origen #', COALESCE(m.id_sucursal_origen,0),' → Destino #', COALESCE(m.id_sucursal_destino,0),
                     IF(m.observacion IS NULL OR m.observacion='', '', CONCAT(' — ', m.observacion))) AS CHAR)
          FROM movimiento m
          LEFT JOIN tipo_movimiento tm ON tm.id_tipo_movimiento = m.id_tipo_movimiento
          WHERE m.fecha_hora IS NOT NULL

          UNION ALL
          SELECT CAST(im.creado_en AS DATETIME), im.id_usuario,
                 CAST(UPPER(COALESCE(im.tipo,'ajuste')) AS CHAR),
                 CAST('inventario_mov' AS CHAR),
                 im.id_mov,
                 CAST(CONCAT('Prod #', COALESCE(im.id_producto,0),' Cant ', COALESCE(im.cantidad,0),
                     ' (prev=', COALESCE(im.stock_prev,0), ', nuevo=', COALESCE(im.stock_nuevo,0),')',
                     IF(im.motivo IS NULL OR im.motivo='', '', CONCAT(' — ', im.motivo))) AS CHAR)
          FROM inventario_mov im
          WHERE im.creado_en IS NOT NULL

          UNION ALL
          SELECT CAST(v.fecha_hora AS DATETIME), v.id_usuario,
                 CAST('VENTA' AS CHAR),
                 CAST('venta' AS CHAR),
                 v.id_venta,
                 CAST(CONCAT('Total $', ROUND(COALESCE(v.total,0),2)) AS CHAR)
          FROM venta v
          WHERE v.fecha_hora IS NOT NULL

          UNION ALL
          SELECT CAST(a.fecha_creada AS DATETIME), NULL,
                 CAST('ALERTA_CREADA' AS CHAR),
                 CAST('alerta' AS CHAR),
                 a.id_alerta,
                 CAST(CONCAT('Prod #', COALESCE(a.id_producto,0)) AS CHAR)
          FROM alerta a
          WHERE a.fecha_creada IS NOT NULL

          UNION ALL
          SELECT CAST(a.fecha_atendida AS DATETIME), a.atendida_por,
                 CAST('ALERTA_ATENDIDA' AS CHAR),
                 CAST('alerta' AS CHAR),
                 a.id_alerta,
                 CAST('Atendida' AS CHAR)
          FROM alerta a
          WHERE a.atendida = 1 AND a.fecha_atendida IS NOT NULL
        """;

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT a.*, u.nombre AS usuario_nombre
            FROM (""").append(unionSql).append("""
            ) a
            LEFT JOIN usuario u ON u.id_usuario = a.id_usuario
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (!q.isEmpty())      { sql.append(" AND a.detalle LIKE ?"); params.add("%"+q+"%"); }
        if (usr!=null && usr.id()>0) { sql.append(" AND a.id_usuario = ?"); params.add(usr.id()); }
        if (accion!=null && !accion.isEmpty()) { sql.append(" AND a.accion = ?"); params.add(accion); }
        if (entidad!=null && !entidad.isEmpty()) { sql.append(" AND a.entidad = ?"); params.add(entidad); }
        if (!desde.isEmpty())  { sql.append(" AND a.fecha >= ?"); params.add(desde+" 00:00:00"); }
        if (!hasta.isEmpty())  { sql.append(" AND a.fecha <= ?"); params.add(hasta+" 23:59:59"); }
        sql.append(" ORDER BY a.fecha DESC LIMIT 200");

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("fecha"),
                            rs.getString("usuario_nombre")==null? "—" : rs.getString("usuario_nombre"),
                            rs.getString("accion"),
                            rs.getString("entidad"),
                            "#"+rs.getInt("id_entidad"),
                            rs.getString("detalle")
                    });
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando auditorías:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }

        if (model.getRowCount()==0){
            model.addRow(new Object[]{"Sin resultados.","","","","",""});
        }
    }

    /* ====== Helpers ====== */
    private void estilizarFecha(JTextField f){
        f.setFont(new Font("Arial",Font.PLAIN,14));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD9,0xD9,0xD9),1,true),
                new EmptyBorder(8,12,8,12)
        ));
        f.setBackground(Color.WHITE);
        f.setToolTipText("AAAA-MM-DD");
    }
    private void bind(PreparedStatement ps, List<Object> params) throws Exception{
        for (int i=0;i<params.size();i++){
            Object v=params.get(i);
            if (v instanceof Integer iv) ps.setInt(i+1, iv);
            else ps.setString(i+1, String.valueOf(v));
        }
    }
    private JPanel cardShell(){
        JPanel p=new JPanel();
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                new EmptyBorder(16,16,18,16)
        ));
        return p;
    }

    /* ====== Renderers ====== */
    static class AccionBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,
                                                       boolean hasFocus,int row,int column){
            JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
            String txt=String.valueOf(value);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD,12f));
            l.setOpaque(true);
            Color bg=new Color(0xDF,0xEF,0xFD), fg=new Color(0x22,0x44,0x66);
            if("ALTA".equalsIgnoreCase(txt) || "VENTA".equalsIgnoreCase(txt) || "ALERTA_ATENDIDA".equalsIgnoreCase(txt)){
                bg=new Color(0xE6,0xFF,0xEA); fg=new Color(0x23,0x7A,0x36);
            } else if("BAJA".equalsIgnoreCase(txt) || "EGRESO".equalsIgnoreCase(txt)){
                bg=new Color(0xFF,0xE4,0xCC); fg=new Color(0xB9,0x4A,0x48);
            }
            l.setBackground(bg); l.setForeground(fg);
            return l;
        }
    }

    /* ====== Aux ====== */
    static class Item {
        private final int id; private final String nombre;
        Item(int id,String nombre){ this.id=id; this.nombre=nombre; }
        int id(){ return id; }
        @Override public String toString(){ return nombre; }
    }
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;
        PlaceholderTextField(String placeholder){ this.placeholder=placeholder; setFont(new Font("Arial",Font.PLAIN,14)); }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(getText().isEmpty() && !isFocusOwner()){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(155,142,127));
                g2.setFont(getFont());
                Insets in=getInsets();
                int x=in.left+4; int y=getHeight()/2+g2.getFontMetrics().getAscent()/2-2;
                g2.drawString(placeholder,x,y);
                g2.dispose();
            }
        }
    }

    /* ====== DB helper ====== */
    static class DB { static Connection get() throws Exception { return conexion_bd.getConnection(); } }
}
