package admin.subcategorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;

public class editar extends JDialog {

    private final int idSub;
    private JComboBox<Item> cbCategoria;
    private JTextField txtNombre;

    private boolean guardado = false;

    public editar(Window owner, int idSubcategoria){
        super(owner, "Editar subcategoría", ModalityType.APPLICATION_MODAL);
        this.idSub = idSubcategoria;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setMinimumSize(new Dimension(540, 380));
        setLocationRelativeTo(owner);
        cargarDatos();
    }

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

        g.gridx=0; g.gridy=0; g.weightx=0;
        form.add(new JLabel("Categoría"), g);

        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(cbCategoria, g);

        g.gridx=0; g.gridy=1; g.weightx=0;
        form.add(new JLabel("Nombre"), g);

        txtNombre = new JTextField();
        estilos.estilizarCampo(txtNombre);
        txtNombre.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(txtNombre, g);

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

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());
    }

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

                        // seleccionar cat
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
            // duplicado excluyéndome
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

    public boolean fueGuardado(){ return guardado; }

    /* ====== helpers ====== */
    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
