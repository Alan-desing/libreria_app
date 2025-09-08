package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class bajo extends JDialog {

    // Visual: tabla y modelo para listar productos en “bajo stock”
    private JTable tabla;
    private DefaultTableModel model;

    // Visual: filtros (por categoría) + botón aplicar
    private JComboBox<Item> cbCategoria;   // id_categoria, nombre
    private JButton btnFiltrar;

    // Lógica: flag para avisar al panel padre si hubo cambios (para refrescar al volver)
    private boolean huboCambios = false;

    // Visual + lógica: constructor. Arma la UI y conecta eventos
    public bajo(Window owner) {
        super(owner, "Productos con stock bajo", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(owner);

        setLayout(new BorderLayout());
        getContentPane().setBackground(estilos.COLOR_FONDO);

        // Visual: shell con márgenes para centrar la card
        JPanel shell = new JPanel(new GridBagLayout());
        shell.setOpaque(false);
        shell.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0;
        gbc.weightx=1; gbc.weighty=1;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        gbc.anchor=GridBagConstraints.PAGE_START;

        // Visual: card blanca con borde crema (contenedor principal)
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

        // Visual: header con título y botón Volver
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel("Productos con stock bajo");
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(estilos.COLOR_TITULO);
        head.add(h1, BorderLayout.WEST);

        JButton btnVolver = estilos.botonSm("Volver");
        btnVolver.addActionListener(e -> dispose());
        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headRight.setOpaque(false);
        headRight.add(btnVolver);
        head.add(headRight, BorderLayout.EAST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));

        // Visual: filtros (categoría + botón “FILTRAR”)
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(240, 38));
        cargarCategorias(); // BD: llena el combo al abrir

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120, 38));
        btnFiltrar.addActionListener(e -> cargarTabla()); // Lógica: aplica filtro

        JPanel filaFiltros = new JPanel(new GridBagLayout());
        filaFiltros.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy=0; g.insets=new Insets(6,0,6,8); g.fill=GridBagConstraints.HORIZONTAL;

        // Visual: orden de filtros (categoría → botón)
        g.gridx=0; g.weightx=0; filaFiltros.add(cbCategoria, g);
        g.gridx=1; filaFiltros.add(btnFiltrar, g);

        // Visual: definición de columnas (tiene botones “Ajustar” y “Pedido”)
        String[] cols = {"ID", "Producto", "Categoría", "Stock", "Mínimo", "Ajustar", "Pedido"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){ return c==5 || c==6; } // solo botones
            @Override public Class<?> getColumnClass(int ci){
                return (ci==5 || ci==6) ? JButton.class : Object.class;
            }
        };

        // Visual: configuración de la tabla (fuentes, header, grilla, selección)
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
        tabla.setSelectionForeground(new Color(0x33,0x33,0x33));

        // Visual: alineación por defecto a la izquierda
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        tabla.setDefaultRenderer(Object.class, left);

        // Visual: renderer del ID con “#”
        tabla.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                Component comp = super.getTableCellRendererComponent(t,v,s,f,r,c);
                setText("#"+String.valueOf(v));
                setHorizontalAlignment(SwingConstants.LEFT); return comp;
            }
        });

        // Visual: badge rojo para “Stock” (en esta vista todo es bajo, lo marcamos en rojo directo)
        tabla.getColumnModel().getColumn(3).setCellRenderer((t, val, sel, foc, row, col) -> {
            String txt = String.valueOf(val);
            return estilos.badgeRoja(txt); // usa el helper de estilos (pill rojo)
        });

        // Visual + lógica: botones por fila (Ajustar y Pedido)
        tabla.getColumnModel().getColumn(5).setCellRenderer(new BtnRenderer("Ajustar"));
        tabla.getColumnModel().getColumn(6).setCellRenderer(new BtnRenderer("Borrador"));
        tabla.getColumnModel().getColumn(5).setCellEditor(new BtnEditor(tabla, id -> abrirAjustar(id)));
        tabla.getColumnModel().getColumn(6).setCellEditor(new BtnEditor(tabla, id -> abrirPedido(id)));

        // Visual: scroll para la tabla con borde crema
        JScrollPane sc = new JScrollPane(tabla, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                BorderFactory.createEmptyBorder(6,6,6,6)
        ));
        sc.setPreferredSize(new Dimension(0, 420));

        // Visual: ensamblado de card (header → filtros → tabla)
        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        // Visual: agregamos la card al shell y mostramos
        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // Lógica: carga inicial de datos (ya viene filtrado por “bajo stock”)
        cargarTabla();
    }

    // Lógica: usado por el panel padre para saber si hay que refrescar al volver
    public boolean huboCambios(){ return huboCambios; }

    // BD: carga el combo de categorías desde la tabla categoria
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

    // BD + lógica: carga la tabla solo con productos cuyo stock_actual <= stock_minimo
    private void cargarTabla(){
        Item cat = (Item) cbCategoria.getSelectedItem();
        int idCat = (cat==null) ? 0 : cat.id();

        String where = " WHERE COALESCE(i.stock_actual,0) <= COALESCE(i.stock_minimo,0) ";
        List<Object> params = new ArrayList<>();
        if (idCat>0){ where += " AND c.id_categoria=? "; params.add(idCat); }

        String sql = """
            SELECT p.id_producto, p.nombre, c.nombre AS categoria,
                   COALESCE(i.stock_actual,0) AS stock_actual,
                   COALESCE(i.stock_minimo,0) AS stock_minimo
              FROM producto p
              LEFT JOIN subcategoria sc ON sc.id_subcategoria = p.id_subcategoria
              LEFT JOIN categoria c     ON c.id_categoria     = sc.id_categoria
              LEFT JOIN inventario i    ON i.id_producto      = p.id_producto
            """ + where + " ORDER BY c.nombre, p.nombre";

        model.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            // Lógica: bind de parámetros si se filtró por categoría
            for (int i=0;i<params.size();i++){
                ps.setInt(i+1, (int)params.get(i));
            }
            // Visual: volcamos resultados a la tabla
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getInt("id_producto"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock_actual"),
                            rs.getInt("stock_minimo"),
                            "Ajustar",
                            "Borrador"
                    });
                }
            }
            // Visual: si no hay filas, mostramos un mensaje informativo
            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","# No hay productos en bajo stock.","","","","",""});
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando bajo stock:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: abre el diálogo de “ajustar” para el id de producto seleccionado
    private void abrirAjustar(int idProd){
        try {
            ajustar dlg = new ajustar(SwingUtilities.getWindowAncestor(this), idProd);
            dlg.setVisible(true);
            if (dlg.fueGuardado()){
                huboCambios = true; // avisamos que hay cambios para refrescar
                cargarTabla();
            }
        } catch (Throwable ex){
            JOptionPane.showMessageDialog(this,"No se pudo abrir Ajustar:\n"+ex.getMessage(),"Inventario",JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: hook para crear un borrador de pedido (por ahora informativo)
    private void abrirPedido(int idProd){
        // Nota: reemplazar por la navegación real a tu módulo de Pedidos cuando esté listo
        JOptionPane.showMessageDialog(this,
                "Crear borrador de pedido para producto #"+idProd+" (implementá aquí el redireccionamiento).",
                "Pedidos", JOptionPane.INFORMATION_MESSAGE);
    }

    // Lógica: clase simple para combos (id + nombre)
    static class Item {
        private final int id; private final String n;
        Item(int id, String n){ this.id=id; this.n=n; }
        int id(){ return id; }
        public String toString(){ return n; }
    }

    // Visual: renderer de botón por celda (usa tu estilo para mantener consistencia)
    static class BtnRenderer extends JButton implements TableCellRenderer {
        BtnRenderer(String txt){ super(txt); setOpaque(true); setBorderPainted(false); setFocusPainted(false); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
            return estilos.botonSm(String.valueOf(v));
        }
    }

    // Lógica: editor del botón por celda, llama al callback con el id de la fila
    static class BtnEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table; private final JButton btn = estilos.botonSm("Acción");
        private final java.util.function.IntConsumer onClick;
        BtnEditor(JTable table, java.util.function.IntConsumer onClick){
            this.table=table; this.onClick=onClick;
            btn.addActionListener(this::handle);
        }
        private void handle(ActionEvent e){
            int vr = table.getEditingRow();
            if (vr>=0){
                int mr = table.convertRowIndexToModel(vr);
                Object idObj = table.getModel().getValueAt(mr, 0);
                int id=0; try{ id = Integer.parseInt(String.valueOf(idObj)); }catch(Exception ignore){}
                if (id>0) onClick.accept(id);
            }
            fireEditingStopped();
        }
        @Override public Object getCellEditorValue(){ return null; }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){
            btn.setText(String.valueOf(v)); return btn;
        }
    }

    // BD: helper local de conexión (mismo que en otros módulos de inventario)
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}