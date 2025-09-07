package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class movimientos extends JDialog {

    private final int idProducto;
    private JTable tabla;
    private DefaultTableModel model;
    private JComboBox<String> cbTipo;
    private JFormattedTextField dpDesde, dpHasta;

    public movimientos(Window owner, int idProducto){
        super(owner, "Movimientos", ModalityType.APPLICATION_MODAL);
        this.idProducto = idProducto;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 680);
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

        // Título (busca nombre de producto)
        String titulo = "Movimientos — #"+idProducto;
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("SELECT nombre FROM producto WHERE id_producto=?")){
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) titulo += " " + rs.getString("nombre");
            }
        } catch (Exception ignore){}

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h1 = new JLabel(titulo);
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
        cbTipo = new JComboBox<>(new String[]{"Todos","ingreso","egreso","ajuste"});
        estilos.estilizarCombo(cbTipo);
        cbTipo.setPreferredSize(new Dimension(160, 38));

        dpDesde = new JFormattedTextField("dd/mm/aaaa");
        dpHasta = new JFormattedTextField("dd/mm/aaaa");
        estilos.estilizarCampo(dpDesde);
        estilos.estilizarCampo(dpHasta);
        dpDesde.setPreferredSize(new Dimension(160, 38));
        dpHasta.setPreferredSize(new Dimension(160, 38));

        JButton btnFiltrar = estilos.botonBlanco("FILTRAR");
        btnFiltrar.setPreferredSize(new Dimension(120, 38));
        btnFiltrar.addActionListener(e -> cargarTabla());

        JPanel filaFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaFiltros.setOpaque(false);
        filaFiltros.add(cbTipo);
        filaFiltros.add(dpDesde);
        filaFiltros.add(dpHasta);
        filaFiltros.add(btnFiltrar);

        // Tabla
        String[] cols = {"ID", "Fecha", "Tipo", "Cantidad", "Stock (prev → nuevo)", "Motivo", "Usuario"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };

        tabla = new JTable(model);
        tabla.setFont(new Font("Arial", Font.PLAIN, 17));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 17));
        tabla.getTableHeader().setBackground(new Color(0xFF,0xF3,0xD9));
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEDE3D2));

        JScrollPane sc = new JScrollPane(tabla,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(estilos.COLOR_BORDE_CREMA,1,true),
                BorderFactory.createEmptyBorder(6,6,6,6)
        ));
        sc.setPreferredSize(new Dimension(0, 420));

        card.add(head);
        card.add(filaFiltros);
        card.add(Box.createVerticalStrut(8));
        card.add(sc);

        shell.add(card, gbc);
        add(shell, BorderLayout.CENTER);

        // asegurar tabla de movimientos
        crearTablaSiNoExiste();
        cargarTabla();
    }

    private void crearTablaSiNoExiste(){
        String sql = """
            CREATE TABLE IF NOT EXISTS inventario_mov (
              id_mov INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
              id_producto INT UNSIGNED NOT NULL,
              tipo ENUM('ingreso','egreso','ajuste') NOT NULL,
              cantidad INT NOT NULL,
              motivo VARCHAR(200) DEFAULT NULL,
              stock_prev INT NOT NULL,
              stock_nuevo INT NOT NULL,
              id_usuario INT UNSIGNED DEFAULT NULL,
              creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY (id_producto), KEY (id_usuario)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
        try (Connection cn = DB.get(); Statement st = cn.createStatement()){
            st.execute(sql);
        } catch (Exception ignore){}
    }

    private void cargarTabla(){
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE id_producto=? ");
        params.add(idProducto);

        String tipoSel = (String) cbTipo.getSelectedItem();
        if (tipoSel!=null && !"Todos".equalsIgnoreCase(tipoSel)){
            where.append(" AND tipo=? "); params.add(tipoSel.toLowerCase());
        }
        // notas: si quisieras filtrar fechas reales, parseá dd/mm/aaaa -> yyyy-mm-dd
        // y agregá BETWEEN en creado_en. Dejo los campos para que mantengas el layout.

        String sql = "SELECT id_mov, tipo, cantidad, motivo, stock_prev, stock_nuevo, id_usuario, creado_en "
                   + "FROM inventario_mov " + where + " ORDER BY id_mov DESC";
        model.setRowCount(0);
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int b=1;
            for (Object v : params){
                if (v instanceof Integer iv) ps.setInt(b++, iv);
                else ps.setString(b++, String.valueOf(v));
            }
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            rs.getInt("id_mov"),
                            rs.getString("creado_en"),
                            rs.getString("tipo"),
                            rs.getInt("cantidad"),
                            rs.getInt("stock_prev")+" → "+rs.getInt("stock_nuevo"),
                            (rs.getString("motivo")==null ? "—" : rs.getString("motivo")),
                            (rs.getObject("id_usuario")==null ? "—" : String.valueOf(rs.getInt("id_usuario")))
                    });
                }
            }
            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","# Sin movimientos","","","","",""});
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,"Error cargando movimientos:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
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
