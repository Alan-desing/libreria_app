package admin.subcategorias;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;

public class editar extends JDialog {

    // Lógica: ID de la subcategoría que vamos a editar
    private final int idSub;

    // Visual: campos del formulario (categoría + nombre)
    private JComboBox<Item> cbCategoria;
    private JTextField txtNombre;

    // Lógica: bandera para informar al panel si se guardó
    private boolean guardado = false;

    // Visual + Lógica: constructor (arma UI, centra y carga datos)
    public editar(Window owner, int idSubcategoria){
        super(owner, "Editar subcategoría", ModalityType.APPLICATION_MODAL);
        this.idSub = idSubcategoria;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setMinimumSize(new Dimension(540, 380));
        setLocationRelativeTo(owner);
        cargarDatos();
    }

    // Visual: card con título, formulario y acciones inferiores
    private void buildUI(){
        getContentPane().setLayout(new GridBagLayout());
        getContentPane().setBackground(estilos.COLOR_FONDO);

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(18,18,18,18)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Editar subcategoría");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(estilos.COLOR_TITULO);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets=new Insets(6,2,6,2); g.fill=GridBagConstraints.HORIZONTAL;

        // Visual: combo de categoría
        g.gridx=0; g.gridy=0; g.weightx=0;
        form.add(new JLabel("Categoría"), g);
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(cbCategoria, g);

        // Visual: campo nombre
        g.gridx=0; g.gridy=1; g.weightx=0;
        form.add(new JLabel("Nombre"), g);
        txtNombre = new JTextField();
        estilos.estilizarCampo(txtNombre);
        txtNombre.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(txtNombre, g);

        // Visual: acciones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton btnCancel = estilos.botonSmBlanco("Cancelar");
        JButton btnSave   = estilos.botonBlanco("GUARDAR CAMBIOS");
        actions.add(btnCancel);
        actions.add(btnSave);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(form);
        card.add(Box.createVerticalStrut(12));
        card.add(actions);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.insets=new Insets(12,12,12,12);
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1;
        getContentPane().add(card, gbc);

        // Lógica: eventos
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());
    }

    // Lógica/BD: carga combo de categorías y datos actuales de la subcategoría
    private void cargarDatos(){
        cbCategoria.removeAllItems();
        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre")){
                try (ResultSet rs = ps.executeQuery()){
                    while (rs.next()) cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_subcategoria, id_categoria, nombre FROM subcategoria WHERE id_subcategoria=?")){
                ps.setInt(1, idSub);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        int idCat = rs.getInt("id_categoria");
                        String nombre = rs.getString("nombre");
                        txtNombre.setText(nombre);

                        // Visual: seleccionar en el combo la categoría asociada
                        ComboBoxModel<Item> model = cbCategoria.getModel();
                        for (int i=0;i<model.getSize();i++){
                            if (model.getElementAt(i).id == idCat){
                                cbCategoria.setSelectedIndex(i);
                                break;
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "No se encontró la subcategoría.", "Datos", JOptionPane.WARNING_MESSAGE);
                        dispose();
                    }
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando datos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // Lógica/BD: validación, control de duplicados y actualización
    private void onSave(){
        Item cat = (Item) cbCategoria.getSelectedItem();
        String nombre = txtNombre.getText()==null?"":txtNombre.getText().trim();

        java.util.List<String> errs = new java.util.ArrayList<>();
        if (cat==null || cat.id<=0) errs.add("Seleccioná una categoría.");
        if (nombre.isEmpty()) errs.add("El nombre es obligatorio.");
        if (nombre.length()<3) errs.add("El nombre debe tener al menos 3 caracteres.");
        if (nombre.length()>120) errs.add("Máximo 120 caracteres.");
        if (!errs.isEmpty()){
            JOptionPane.showMessageDialog(this, String.join("\n", errs), "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection cn = DB.get()){
            // Lógica/BD: chequear duplicado (misma categoría y nombre, excluyendo el propio id)
            try (PreparedStatement chk = cn.prepareStatement(
                    "SELECT 1 FROM subcategoria WHERE id_categoria=? AND nombre=? AND id_subcategoria<>? LIMIT 1")){
                chk.setInt(1, cat.id);
                chk.setString(2, nombre);
                chk.setInt(3, idSub);
                try (ResultSet rs = chk.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe otra subcategoría con ese nombre en esa categoría.", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            // Lógica/BD: update
            try (PreparedStatement up = cn.prepareStatement(
                    "UPDATE subcategoria SET id_categoria=?, nombre=? WHERE id_subcategoria=?")){
                up.setInt(1, cat.id);
                up.setString(2, nombre);
                up.setInt(3, idSub);
                up.executeUpdate();
            }
            guardado = true;
            JOptionPane.showMessageDialog(this, "Subcategoría actualizada.");
            dispose();

        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error guardando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: permite al panel saber si guardó bien
    public boolean fueGuardado(){ return guardado; }

    // Lógica: item simple para el combo (id + etiqueta visible)
    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
