package admin.inventario;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class movimientos extends JDialog {

    private final int idProducto;

    private JComboBox<String> cbTipo;
    private JTextField txtDesde, txtHasta;
    private JButton btnFiltrar, btnCerrar;
    private JTable tabla;
    private DefaultTableModel model;

    public movimientos(Window owner, int idProducto) {
        super(owner, "Movimientos de inventario", ModalityType.APPLICATION_MODAL);
        this.idProducto = idProducto;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(920, 620);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true),
                new EmptyBorder(16,16,16,16)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx=1;

        // Título con info de producto
        JLabel lbTitle = new JLabel();
        lbTitle.setFont(new Font("Arial", Font.BOLD, 15));
        gc.gridx=0; gc.gridy=0;
        card.add(lbTitle, gc);

        // Info de producto
        String prodNom="—";
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("SELECT nombre FROM producto WHERE id_producto=?")){
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) prodNom = rs.getString(1);
            }
        } catch (Exception ignore){}
        lbTitle.setText("Movimientos — #"+idProducto+" "+prodNom);

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);

        cbTipo = new JComboBox<>(new String[]{"Tipo: Todos", "Ingreso", "Egreso", "Ajuste"});
        estilos.estilizarCombo(cbTipo);
        filtros.add(cbTipo);

        txtDesde = new JTextField();
        txtDesde.setPreferredSize(new Dimension(120, 32));
        txtDesde.setToolTipText("Desde (YYYY-MM-DD)");
        filtros.add(txtDesde);

        txtHasta = new JTextField();
        txtHasta.setPreferredSize(new Dimension(120, 32));
        txtHasta.setToolTipText("Hasta (YYYY-MM-DD)");
        filtros.add(txtHasta);

        btnFiltrar = estilos.botonBlanco("FILTRAR");
        filtros.add(btnFiltrar);

        gc.gridy=1;
        card.add(filtros, gc);

        // Tabla
        String[] cols = {"ID","Fecha","Tipo","Cantidad","Stock (prev → nuevo)","Motivo","Usuario"};
        model = new DefaultTableModel(cols, 0){
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(model);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(new CompoundBorder(new LineBorder(estilos.COLOR_BORDE_SUAVE,1,true), new EmptyBorder(6,6,6,6)));
        gc.gridy=2; gc.weighty=1; gc.fill = GridBagConstraints.BOTH;
        card.add(sp, gc);

        // Acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnCerrar = estilos.botonSm("Volver");
        actions.add(btnCerrar);

        gc.gridy=3; gc.weighty=0; gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCerrar.addActionListener(e -> dispose());
        btnFiltrar.addActionListener(e -> cargar());

        // asegurar tabla movimientos
        try (Connection cn = DB.get()){
            cn.createStatement().execute("""
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
                  KEY (id_producto),
                  KEY (id_usuario),
                  KEY (creado_en)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (Exception ignore){}

        cargar();
    }

    private void cargar(){
        String tipoSel = (String) cbTipo.getSelectedItem();
        String tipo="";
        if (tipoSel!=null){
            if (tipoSel.startsWith("Ingreso")) tipo="ingreso";
            else if (tipoSel.startsWith("Egreso")) tipo="egreso";
            else if (tipoSel.startsWith("Ajuste")) tipo="ajuste";
        }
        String desde = txtDesde.getText()==null? "" : txtDesde.getText().trim();
        String hasta = txtHasta.getText()==null? "" : txtHasta.getText().trim();

        String where = " WHERE m.id_producto=? ";
        List<Object> params = new ArrayList<>();
        params.add(idProducto);

        if (!tipo.isEmpty()){
            where += " AND m.tipo=? ";
            params.add(tipo);
        }
        if (!desde.isEmpty() && desde.matches("^\\d{4}-\\d{2}-\\d{2}$")){
            where += " AND m.creado_en >= CONCAT(?, ' 00:00:00') ";
            params.add(desde);
        }
        if (!hasta.isEmpty() && hasta.matches("^\\d{4}-\\d{2}-\\d{2}$")){
            where += " AND m.creado_en <= CONCAT(?, ' 23:59:59') ";
            params.add(hasta);
        }

        String sql = """
            SELECT m.id_mov, m.creado_en, m.tipo, m.cantidad,
                   m.stock_prev, m.stock_nuevo, m.motivo,
                   u.nombre AS usuario
            FROM inventario_mov m
            LEFT JOIN usuario u ON u.id_usuario = m.id_usuario
        """ + where + " ORDER BY m.creado_en DESC, m.id_mov DESC";

        model.setRowCount(0);

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)){

            int bind=1;
            for (Object v: params){
                if (v instanceof Integer iv) ps.setInt(bind++, iv);
                else ps.setString(bind++, String.valueOf(v));
            }

            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    model.addRow(new Object[]{
                            "#"+rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getInt(4),
                            rs.getInt(5) + " → " + rs.getInt(6),
                            rs.getString(7)==null? "" : rs.getString(7),
                            rs.getString(8)==null? "—" : rs.getString(8)
                    });
                }
            }

            if (model.getRowCount()==0){
                model.addRow(new Object[]{"","Sin movimientos para los filtros.","","","","",""});
            }

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error consultando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
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
