package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class minimos extends JDialog {

    private JTable tabla;
    private DefaultTableModel model;
    private PlaceholderTextField txtBuscar;
    private JComboBox<Item> cbCategoria;
    private JButton btnFiltrar;
    private JButton btnGuardar;
    private boolean huboCambios = false;

    public minimos(Window owner){
        super(owner, "Editar mínimos en lote", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(estilos.COLOR_FONDO);

        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.PAGE_START;

        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                BorderFactory.createEmptyBorder(16,16,18,16)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Editar mínimos en lote");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        JButton btnVolver = estilos.botonSm("Volver");
        btnVolver.addActionListener(e -> dispose());
        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headRight.setOpaque(false);
        headRight.add(btnVolver);
        head.add(headRight, BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));

        // Filtros
        txtBuscar = new PlaceholderTextField("Buscar por nombre o ID…");
        estilos.estilizarCampo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(360, 40));
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(220, 38));
        cargarCategorias();

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120, 38));
        btnFiltrar.addActionListener(e -> cargarTabla());

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy=0; g.insets=new Insets(6,0,6,8); g.fill=GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.weightx=1; filaFiltros.add(txtBuscar, g);
        g.gridx=1; g.weightx=0; filaFiltros.add(cbCategoria, g);
        g.gridx=2; filaFiltros.add(btnFiltrar, g);

        // Tabla
        String[] cols = {"ID", "Producto", "Categoría", "Stock", "Mínimo"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){ return c==4; }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 17));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setBackground(new Color(0xFF,0xF3,0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));
        tabla.setSelectionBackground(new Color(0xF2,0xE7,0xD6));

        // Editor numérico para "Mínimo"
        JTextField minEditor = new JTextField();
        minEditor.setHorizontalAlignment(SwingConstants.LEFT);
        minEditor.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                if (!Character.isDigit(ch) && ch!='\b') e.consume();
            }
        });
        tabla.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(minEditor));

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                BorderFactory.createEmptyBorder(6,6,6,6)
        ));
        sc.setPreferredSize(new Dimension(0, 420));

        // Acciones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        btnGuardar = estilos.botonBlanco("GUARDAR MÍNIMOS");
        btnGuardar.addActionListener(e -> guardarMinimos());
        actions.add(btnGuardar);

        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);
        card.add(Box.createVerticalStrut(10));
        card.add(actions);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Carga inicial
        cargarTabla();
    }

    public boolean huboCambios(){ return huboCambios; }

    private void cargarCategorias(){
        cbCategoria.removeAllItems();
        cbCategoria.addItem(new Item(0, "Todas las categorías"));
        String sql = "SELECT id_categoria, nombre FROM categoria ORDER BY nombre";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                cbCategoria.addItem(new Item(rs.getInt("id_categoria"), rs.getString("nombre")));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando categorías:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarTabla(){
        String q = txtBuscar.getText()==null ? "" : txtBuscar.getText().trim();
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat==null) ? 0 : cat.id();

        String baseFrom = """
              FROM producto p
              LEFT JOIN subcategoria sc ON sc.id_subcategoria = p.id_subcategoria
              LEFT JOIN categoria c     ON c.id_categoria     = sc.id_categoria
              LEFT JOIN inventario i    ON i.id_producto      = p.id_producto
            """;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (!q.isEmpty()){
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("(p.nombre LIKE ? OR p.id_producto = ?)");
            params.add("%"+q+"%");
            try { params.add(Integer.parseInt(q)); } catch(Exception e){ params.add(0); }
        }
        if (idCat>0){
            where.append(where.length()==0 ? " WHERE " : " AND ");
            where.append("c.id_categoria = ?");
            params.add(idCat);
        }

        String sql = """
            SELECT p.id_producto, p.nombre, c.nombre AS categoria,
                   COALESCE(i.stock_actual,0) AS stock_actual,
                   COALESCE(i.stock_minimo,0) AS stock_minimo
            """ + baseFrom + where + " GROUP BY p.id_producto, p.nombre, categoria, i.stock_actual, i.stock_minimo"
            + " ORDER BY p.nombre ASC";

        model.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int bind=1;
            for (Object v : params){
                if (v instanceof Integer iv) ps.setInt(bind++, iv);
                else ps.setString(bind++, String.valueOf(v));
            }
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getInt("id_producto"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock_actual"),
                            rs.getInt("stock_minimo")
                    });
                }
            }
            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","# Sin resultados.","","",""});
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando datos:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarMinimos(){
        try (Connection cn = DB.get()){
            cn.setAutoCommit(false);
            try (PreparedStatement up = cn.prepareStatement("UPDATE inventario SET stock_minimo=? WHERE id_producto=?")) {
                for (int r=0; r<model.getRowCount(); r++){
                    Object idObj = model.getValueAt(r, 0);
                    if (!(idObj instanceof Integer) && !(idObj instanceof Long)) continue;
                    int id = Integer.parseInt(String.valueOf(idObj));
                    int min = 0;
                    try { min = Integer.parseInt(String.valueOf(model.getValueAt(r, 4))); }
                    catch (Exception ignore){ min = 0; }
                    up.setInt(1, Math.max(0, min));
                    up.setInt(2, id);
                    up.addBatch();
                }
                up.executeBatch();
            }
            cn.commit();
            huboCambios = true;
            JOptionPane.showMessageDialog(this,"Mínimos guardados.","Inventario",JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"No se pudieron guardar:\n"+ex.getMessage(),"Inventario",JOptionPane.ERROR_MESSAGE);
        }
        cargarTabla();
    }

    // ===== Helpers =====
    static class Item { private final int id; private final String n; Item(int i,String n){id=i;this.n=n;} int id(){return id;} public String toString(){return n;} }
    static class PlaceholderTextField extends JTextField {
        private final String holder;
        PlaceholderTextField(String h){ holder=h; setFont(new Font("Arial", Font.PLAIN, 14)); setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(155,142,127));
                Insets in=getInsets();
                g2.drawString(holder, in.left+4, getHeight()/2 + g2.getFontMetrics().getAscent()/2 - 2);
                g2.dispose();
            }
        }
    }
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
