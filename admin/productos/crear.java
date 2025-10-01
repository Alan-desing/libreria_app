package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class crear {

    // Opción simple para combos (id, nombre)
    static class Opcion {
        final int id; final String nombre;
        Opcion(int id, String nombre){ this.id=id; this.nombre=nombre; }
        @Override public String toString(){ return nombre; }
    }

    public static void abrir(Component parent) {
        Window owner = parent==null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(owner, "Nuevo producto", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Campos
        JTextField tfNombre = new JTextField();
        JTextField tfCodigo = new JTextField();
        JTextArea  taDesc   = new JTextArea(3, 20);
        JTextField tfPC     = new JTextField("0");
        JTextField tfPV     = new JTextField("0");
        JTextField tfUbi    = new JTextField();
        JCheckBox  cbActivo = new JCheckBox("Disponible para vender", true);

        JComboBox<Opcion> cbSubcat   = new JComboBox<>();
        JComboBox<Opcion> cbProveedor= new JComboBox<>();

        // Cargar combos (subcategoría y proveedor)
        cargarOpciones(cbSubcat, "SELECT id_subcategoria, nombre FROM subcategoria ORDER BY nombre",
                "id_subcategoria", "nombre");
        cargarOpciones(cbProveedor, "SELECT id_proveedor, nombre FROM proveedor ORDER BY nombre",
                "id_proveedor", "nombre");
        cbSubcat.insertItemAt(new Opcion(0,"—"), 0); cbSubcat.setSelectedIndex(0);
        cbProveedor.insertItemAt(new Opcion(0,"—"), 0); cbProveedor.setSelectedIndex(0);

        // Layout
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y=0;
        fila(form, c, y++, "Nombre", tfNombre);
        fila(form, c, y++, "Código", tfCodigo);

        c.gridx=0; c.gridy=y; c.gridwidth=1; form.add(new JLabel("Proveedor"), c);
        c.gridx=1; c.gridy=y++; c.gridwidth=1; form.add(cbProveedor, c);

        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(new JLabel("Descripción"), c); y++;
        c.gridx=0; c.gridy=y; c.gridwidth=2; c.fill=GridBagConstraints.BOTH; c.weightx=1; c.weighty=1;
        JScrollPane sp = new JScrollPane(taDesc); form.add(sp, c);
        c.fill=GridBagConstraints.HORIZONTAL; c.weighty=0; y++;

        fila(form, c, y++, "Precio compra", tfPC);
        fila(form, c, y++, "Precio venta", tfPV);
        fila(form, c, y++, "Ubicación", tfUbi);

        c.gridx=0; c.gridy=y; c.gridwidth=1; form.add(new JLabel("Subcategoría"), c);
        c.gridx=1; c.gridy=y++; form.add(cbSubcat, c);

        c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(cbActivo, c); y++;

        // Botones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        actions.add(btnCancelar); actions.add(btnGuardar);

        btnCancelar.addActionListener(e -> dlg.dispose());
        btnGuardar.addActionListener(e -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isEmpty()){
                JOptionPane.showMessageDialog(dlg,"El nombre es obligatorio.","Validación",JOptionPane.WARNING_MESSAGE);
                tfNombre.requestFocus(); return;
            }

            String codigo = tfCodigo.getText().trim();
            String desc   = taDesc.getText().trim();
            double pc     = parseDouble(tfPC.getText(), 0);
            double pv     = parseDouble(tfPV.getText(), 0);
            int idsub     = ((Opcion)cbSubcat.getSelectedItem()).id;
            int idprov    = ((Opcion)cbProveedor.getSelectedItem()).id;
            String ubi    = tfUbi.getText().trim();
            int activo    = cbActivo.isSelected()?1:0;

            String sql = "INSERT INTO producto(nombre,descripcion,codigo,precio_compra,precio_venta,ubicacion,id_subcategoria,id_proveedor,activo,creado_en) " +
                         "VALUES (?,?,?,?,?,?,?,?,?,NOW())";

            try (Connection cn = conexion_bd.getConnection();
                 PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre);
                ps.setString(2, desc);
                ps.setString(3, codigo);
                ps.setDouble(4, pc);
                ps.setDouble(5, pv);
                ps.setString(6, ubi);
                if (idsub>0) ps.setInt(7, idsub); else ps.setNull(7, Types.INTEGER);
                if (idprov>0) ps.setInt(8, idprov); else ps.setNull(8, Types.INTEGER);
                ps.setInt(9, activo);
                ps.executeUpdate();

                int newId = 0;
                try (ResultSet gk = ps.getGeneratedKeys()) { if (gk.next()) newId = gk.getInt(1); }
                JOptionPane.showMessageDialog(dlg, "Producto creado (#"+newId+").");
                dlg.dispose();
            } catch (Exception ex){
                JOptionPane.showMessageDialog(dlg, "Error al guardar:\n"+ex.getMessage(),
                        "BD", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Contenedor
        JPanel root = new JPanel(new BorderLayout(0,10));
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setSize(560, 520);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    private static void fila(JPanel p, GridBagConstraints c, int y, String label, JComponent comp){
        c.gridx=0; c.gridy=y; c.gridwidth=1; c.weightx=0;
        p.add(new JLabel(label), c);
        c.gridx=1; c.gridy=y; c.gridwidth=1; c.weightx=1;
        p.add(comp, c);
    }

    private static void cargarOpciones(JComboBox<Opcion> cb, String sql, String idCol, String txtCol){
        cb.removeAllItems();
        List<Opcion> ops = new ArrayList<>();
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                ops.add(new Opcion(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(cb, "Error cargando datos:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
        for (Opcion o: ops) cb.addItem(o);
    }

    private static double parseDouble(String s, double def){
        try { return Double.parseDouble(s.replace(",", ".").trim()); } catch(Exception e){ return def; }
    }
}
