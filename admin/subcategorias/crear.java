package admin.subcategorias;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;

public class crear extends JDialog {

    // Visual: campos del formulario (categoría + nombre)
    private JComboBox<Item> cbCategoria;
    private JTextField txtNombre;

    // Lógica: bandera para informar al panel si se creó
    private boolean guardado = false;

    // Visual + Lógica: constructor que arma la pantalla, centra y enfoca
    public crear(Window owner){
        super(owner, "Nueva subcategoría", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setMinimumSize(new Dimension(520, 360));
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() { @Override public void windowOpened(WindowEvent e) { txtNombre.requestFocusInWindow(); } });
    }

    // Visual: arma la card (título, formulario, acciones)
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

        JLabel title = new JLabel("Nueva subcategoría");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(estilos.COLOR_TITULO);

        JPanel wrTitle = new JPanel(new BorderLayout());
        wrTitle.setOpaque(false);
        wrTitle.add(title, BorderLayout.CENTER);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,2,6,2);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx=0; g.gridy=0; g.weightx=0;

        // Visual: combo de categoría
        JLabel lc = new JLabel("Categoría");
        form.add(lc, g);
        cbCategoria = new JComboBox<>();
        estilos.estilizarCombo(cbCategoria);
        cbCategoria.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(cbCategoria, g);

        // Visual: campo de nombre
        JLabel ln = new JLabel("Nombre");
        g.gridx=0; g.gridy=1; g.weightx=0;
        form.add(ln, g);
        txtNombre = new JTextField();
        estilos.estilizarCampo(txtNombre);
        txtNombre.setPreferredSize(new Dimension(260, 36));
        g.gridx=1; g.weightx=1;
        form.add(txtNombre, g);

        // Visual: acciones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton btnCancel = estilos.botonSmBlanco("Cancelar");
        JButton btnCrear  = estilos.botonBlanco("CREAR");
        actions.add(btnCancel);
        actions.add(btnCrear);

        // Visual: ensamblado
        card.add(wrTitle);
        card.add(Box.createVerticalStrut(12));
        card.add(form);
        card.add(Box.createVerticalStrut(12));
        card.add(actions);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.insets=new Insets(12,12,12,12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx=1;
        getContentPane().add(card, gbc);

        // Lógica: eventos
        btnCancel.addActionListener(e -> dispose());
        btnCrear.addActionListener(e -> onCrear());

        // Lógica/BD: llenar categorías
        cargarCategorias();
    }

    // Lógica/BD: carga el combo de categorías
    private void cargarCategorias(){
        cbCategoria.removeAllItems();
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("SELECT id_categoria, nombre FROM categoria ORDER BY nombre")){
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    cbCategoria.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error obteniendo categorías:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica/BD: valida, evita duplicado (id_categoria,nombre) e inserta
    private void onCrear(){
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
            // Lógica/BD: check de duplicado por (id_categoria, nombre)
            try (PreparedStatement chk = cn.prepareStatement(
                    "SELECT 1 FROM subcategoria WHERE id_categoria=? AND nombre=? LIMIT 1")){
                chk.setInt(1, cat.id);
                chk.setString(2, nombre);
                try (ResultSet rs = chk.executeQuery()){
                    if (rs.next()){
                        JOptionPane.showMessageDialog(this, "Ya existe una subcategoría con ese nombre en la categoría seleccionada.", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            // Lógica/BD: insert
            try (PreparedStatement ins = cn.prepareStatement(
                    "INSERT INTO subcategoria(id_categoria, nombre) VALUES(?,?)")){
                ins.setInt(1, cat.id);
                ins.setString(2, nombre);
                ins.executeUpdate();
            }
            guardado = true;
            JOptionPane.showMessageDialog(this, "Subcategoría creada correctamente.");
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al crear:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: permite al panel saber si se creó correctamente
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
