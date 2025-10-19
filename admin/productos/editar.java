package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class editar {

    // lógica: clase interna simple para usar en combos (id, nombre)
    static class Opcion {
        final int id; final String nombre;
        Opcion(int id, String nombre){ this.id=id; this.nombre=nombre; }
        @Override public String toString(){ return nombre; }
    }

    // lógica: método principal para abrir el diálogo de edición
    public static void abrir(Component parent, int idProducto){
        // BD: traer los datos del producto a editar
        String sqlSel = "SELECT * FROM producto WHERE id_producto=?";
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sqlSel)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){
                    JOptionPane.showMessageDialog(parent,"Producto no encontrado","BD",JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // lógica: obtener los datos actuales del producto
                String nombre = rs.getString("nombre");
                String codigo = rs.getString("codigo");
                String desc   = rs.getString("descripcion");
                double pc     = rs.getDouble("precio_compra");
                double pv     = rs.getDouble("precio_venta");
                String ubi    = rs.getString("ubicacion");
                int idSubAct  = rs.getInt("id_subcategoria");
                int idProvAct = rs.getInt("id_proveedor");
                boolean activo= rs.getInt("activo")==1;

                // visual: ventana de edición
                Window owner = parent==null ? null : SwingUtilities.getWindowAncestor(parent);
                JDialog dlg = new JDialog(owner, "Editar producto #"+idProducto, Dialog.ModalityType.APPLICATION_MODAL);
                dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

                // visual: campos del formulario con valores actuales
                JTextField tfNombre = new JTextField(nombre!=null?nombre:"");
                JTextField tfCodigo = new JTextField(codigo!=null?codigo:"");
                JTextArea  taDesc   = new JTextArea(desc!=null?desc:"", 3, 20);
                JTextField tfPC     = new JTextField(String.valueOf(pc));
                JTextField tfPV     = new JTextField(String.valueOf(pv));
                JTextField tfUbi    = new JTextField(ubi!=null?ubi:"");
                JCheckBox  cbActivo = new JCheckBox("Disponible para vender", activo);

                JComboBox<Opcion> cbSubcat    = new JComboBox<>();
                JComboBox<Opcion> cbProveedor = new JComboBox<>();

                // BD: cargar listas de subcategorías y proveedores
                cargarOpciones(cbSubcat,    "SELECT id_subcategoria, nombre FROM subcategoria ORDER BY nombre");
                cargarOpciones(cbProveedor, "SELECT id_proveedor, nombre FROM proveedor ORDER BY nombre");

                // visual: agregar opción vacía al inicio
                cbSubcat.insertItemAt(new Opcion(0,"—"), 0);
                cbProveedor.insertItemAt(new Opcion(0,"—"), 0);

                // lógica: seleccionar los valores actuales en los combos
                seleccionar(cbSubcat, idSubAct);
                seleccionar(cbProveedor, idProvAct);

                // visual: estructura del formulario
                JPanel form = new JPanel(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.insets=new Insets(6,6,6,6); c.fill=GridBagConstraints.HORIZONTAL;

                int y=0;
                fila(form,c,y++,"Nombre",tfNombre);
                fila(form,c,y++,"Código",tfCodigo);

                c.gridx=0; c.gridy=y; form.add(new JLabel("Proveedor"),c);
                c.gridx=1; c.gridy=y++; form.add(cbProveedor,c);

                c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(new JLabel("Descripción"), c); y++;
                c.gridx=0; c.gridy=y; c.gridwidth=2; c.fill=GridBagConstraints.BOTH; c.weightx=1; c.weighty=1;
                form.add(new JScrollPane(taDesc), c);
                c.fill=GridBagConstraints.HORIZONTAL; c.weighty=0; y++;

                fila(form,c,y++,"Precio compra",tfPC);
                fila(form,c,y++,"Precio venta",tfPV);
                fila(form,c,y++,"Ubicación",tfUbi);

                c.gridx=0; c.gridy=y; c.gridwidth=1; form.add(new JLabel("Subcategoría"),c);
                c.gridx=1; c.gridy=y++; form.add(cbSubcat,c);

                c.gridx=0; c.gridy=y; c.gridwidth=2; form.add(cbActivo,c); y++;

                // visual: botones de acción
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton btnCancelar=new JButton("Cancelar");
                JButton btnGuardar=new JButton("Guardar cambios");
                actions.add(btnCancelar); actions.add(btnGuardar);

                // lógica: eventos de los botones
                btnCancelar.addActionListener(ev -> dlg.dispose());
                btnGuardar.addActionListener(ev -> {
                    String nom = tfNombre.getText().trim();
                    if (nom.isEmpty()){
                        JOptionPane.showMessageDialog(dlg,"El nombre es obligatorio.","Validación",JOptionPane.WARNING_MESSAGE);
                        tfNombre.requestFocus(); return;
                    }

                    // lógica: obtención de datos del formulario
                    String cod = tfCodigo.getText().trim();
                    String dsc = taDesc.getText().trim();
                    double vpc = parseDouble(tfPC.getText(), 0);
                    double vpv = parseDouble(tfPV.getText(), 0);
                    int idSub  = ((Opcion)cbSubcat.getSelectedItem()).id;
                    int idProv = ((Opcion)cbProveedor.getSelectedItem()).id;
                    String ubi2= tfUbi.getText().trim();
                    int act    = cbActivo.isSelected()?1:0;

                    // BD: actualización de datos del producto
                    String sqlUp = "UPDATE producto SET nombre=?, descripcion=?, codigo=?, precio_compra=?, precio_venta=?, ubicacion=?, id_subcategoria=?, id_proveedor=?, activo=?, actualizado_en=NOW() WHERE id_producto=?";
                    try (PreparedStatement up = cn.prepareStatement(sqlUp)){
                        up.setString(1, nom);
                        up.setString(2, dsc);
                        up.setString(3, cod);
                        up.setDouble(4, vpc);
                        up.setDouble(5, vpv);
                        up.setString(6, ubi2);
                        if (idSub>0) up.setInt(7, idSub); else up.setNull(7, Types.INTEGER);
                        if (idProv>0) up.setInt(8, idProv); else up.setNull(8, Types.INTEGER);
                        up.setInt(9, act);
                        up.setInt(10, idProducto);
                        up.executeUpdate();
                        JOptionPane.showMessageDialog(dlg,"Producto actualizado.");
                        dlg.dispose();
                    } catch(Exception ex){
                        JOptionPane.showMessageDialog(dlg,"Error al actualizar:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
                    }
                });

                // visual: contenedor principal del diálogo
                JPanel root = new JPanel(new BorderLayout(0,10));
                root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
                root.add(form, BorderLayout.CENTER);
                root.add(actions, BorderLayout.SOUTH);

                dlg.setContentPane(root);
                dlg.setSize(580, 540);
                dlg.setLocationRelativeTo(parent);
                dlg.setVisible(true);
            }
        } catch(Exception ex){
            JOptionPane.showMessageDialog(parent,"Error:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    // helpers: agrega una fila al formulario (label + campo)
    private static void fila(JPanel p, GridBagConstraints c, int y, String label, JComponent comp){
        c.gridx=0; c.gridy=y; c.gridwidth=1; c.weightx=0;
        p.add(new JLabel(label), c);
        c.gridx=1; c.gridy=y; c.gridwidth=1; c.weightx=1;
        p.add(comp, c);
    }

    // BD: carga de opciones en combos desde la base
    private static void cargarOpciones(JComboBox<Opcion> cb, String sql){
        cb.removeAllItems();
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                cb.addItem(new Opcion(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(cb,"Error cargando datos:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }

    // helpers: selecciona en el combo el ítem con el id indicado
    private static void seleccionar(JComboBox<Opcion> cb, int id){
        int n = cb.getItemCount();
        for (int i=0;i<n;i++){
            Opcion op = cb.getItemAt(i);
            if (op!=null && op.id==id){ cb.setSelectedIndex(i); return; }
        }
        // si no lo encuentra, seleccionar “—” si existe
        for (int i=0;i<n;i++){
            Opcion op = cb.getItemAt(i);
            if (op!=null && op.id==0){ cb.setSelectedIndex(i); return; }
        }
    }

    // helpers: conversión segura de texto a double
    private static double parseDouble(String s, double def){
        try { return Double.parseDouble(s.replace(",", ".").trim()); } catch(Exception e){ return def; }
    }
}
