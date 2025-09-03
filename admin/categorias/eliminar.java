package admin.categorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class eliminar extends JDialog {

    private final int id;
    private boolean eliminado = false;

    private JLabel lbNombre;
    private JLabel lbSubcats;
    private JLabel lbProds;
    private JButton btnEliminar;

    public eliminar(Window owner, int id) {
        super(owner, "Eliminar categoría", ModalityType.APPLICATION_MODAL);
        this.id = id;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(560, 300);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(18,18,18,18)
        ));

        JLabel title = new JLabel("¿Eliminar esta categoría?");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(estilos.COLOR_TITULO);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        lbNombre = new JLabel("Categoría: —");
        lbSubcats = new JLabel("Subcategorías asociadas: —");
        lbProds = new JLabel("Productos asociados: —");
        for (JLabel l : new JLabel[]{lbNombre, lbSubcats, lbProds}) {
            l.setFont(new Font("Arial", Font.PLAIN, 15));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(l);
            card.add(Box.createVerticalStrut(2));
        }

        card.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnCancel = estilos.botonSm("Cancelar");
        btnEliminar = estilos.botonSmDanger("Eliminar");
        actions.add(btnCancel);
        actions.add(btnEliminar);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(actions);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        btnCancel.addActionListener(e -> dispose());
        btnEliminar.addActionListener(e -> onEliminar());

        cargar();
    }

    public boolean fueEliminado(){ return eliminado; }

    private void cargar(){
        String sql = """
            SELECT
              c.id_categoria, c.nombre,
              COUNT(DISTINCT sc.id_subcategoria) AS subcats,
              COUNT(DISTINCT p.id_producto)      AS productos
            FROM categoria c
            LEFT JOIN subcategoria sc ON sc.id_categoria = c.id_categoria
            LEFT JOIN producto p      ON p.id_subcategoria = sc.id_subcategoria
            WHERE c.id_categoria = ?
            """;
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){
                    JOptionPane.showMessageDialog(this, "Categoría no encontrada.");
                    dispose();
                    return;
                }
                String nombre = rs.getString("nombre");
                int subcats   = rs.getInt("subcats");
                int prods     = rs.getInt("productos");

                lbNombre.setText("Categoría: " + nombre);
                lbSubcats.setText("Subcategorías asociadas: " + subcats);
                lbProds.setText("Productos asociados: " + prods);

                if (subcats>0 || prods>0){
                    btnEliminar.setEnabled(false);
                    JLabel tip = new JLabel("💡 Primero elimina o reasigna las subcategorías/productos.");
                    tip.setForeground(new Color(120, 110, 95));
                    tip.setAlignmentX(Component.LEFT_ALIGNMENT);
                    tip.setBorder(new EmptyBorder(6,0,0,0));
                    ((JPanel)getContentPane().getComponent(0)).add(tip);
                    revalidate(); repaint();
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error consultando categoría:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void onEliminar(){
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar definitivamente?", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r!=JOptionPane.YES_OPTION) return;

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM categoria WHERE id_categoria=?")) {
            ps.setInt(1, id);
            int n = ps.executeUpdate();
            if (n>0){
                eliminado = true;
                JOptionPane.showMessageDialog(this, "Categoría eliminada.");
            } else {
                JOptionPane.showMessageDialog(this, "No se eliminó (¿ID inexistente?)");
            }
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo eliminar:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
