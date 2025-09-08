package admin.subcategorias;

import includes.estilos;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;

public class eliminar extends JDialog {

    // Lógica: ID de la subcategoría a eliminar
    private final int idSub;

    // Lógica: bandera para informar al panel si se eliminó
    private boolean eliminado = false;

    // Visual: labels con datos de la subcategoría
    private JLabel lblNombre = new JLabel("-");
    private JLabel lblProductos = new JLabel("-");

    // Visual: botones inferiores
    private JButton btnVolver, btnEliminar;

    // Lógica: valores leídos para evaluar si se puede eliminar
    private int cantProductos = 0;
    private String nombre = "";

    // Visual + Lógica: constructor (arma UI, centra y evalúa)
    public eliminar(Window owner, int idSubcategoria){
        super(owner, "Eliminar subcategoría", ModalityType.APPLICATION_MODAL);
        this.idSub = idSubcategoria;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setMinimumSize(new Dimension(520, 320));
        setLocationRelativeTo(owner);
        cargarYEvaluar();
    }

    // Visual: card (título, info y acciones)
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

        JLabel title = new JLabel("Eliminar subcategoría");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(estilos.COLOR_TITULO);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new GridLayout(0,1,0,6));
        info.add(new JLabel("Vas a eliminar la subcategoría:"));
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        info.add(lblNombre);
        info.add(new JLabel("Productos asociados:"));
        lblProductos.setFont(new Font("Arial", Font.PLAIN, 14));
        info.add(lblProductos);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        btnVolver   = estilos.botonSmBlanco("Cancelar");
        btnEliminar = estilos.botonSmDanger("eliminar");
        actions.add(btnVolver);
        actions.add(btnEliminar);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(info);
        card.add(Box.createVerticalStrut(12));
        card.add(actions);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.insets=new Insets(12,12,12,12);
        gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1;
        getContentPane().add(card, gbc);

        // Lógica: eventos
        btnVolver.addActionListener(e -> dispose());
        btnEliminar.addActionListener(e -> onEliminar());
    }

    // Lógica/BD: consulta nombre y cantidad de productos; deshabilita si no se puede
    private void cargarYEvaluar(){
        try (Connection cn = DB.get()){
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT sc.nombre, COUNT(DISTINCT p.id_producto) AS productos " +
                    "FROM subcategoria sc " +
                    "LEFT JOIN producto p ON p.id_subcategoria=sc.id_subcategoria " +
                    "WHERE sc.id_subcategoria=?")){
                ps.setInt(1, idSub);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        nombre = rs.getString("nombre");
                        cantProductos = rs.getInt("productos");
                    }
                }
            }
            // Visual: pintar datos
            lblNombre.setText("• " + (nombre==null?("-"):nombre));
            lblProductos.setText(String.valueOf(cantProductos));

            // Lógica: si tiene productos asociados, no se puede eliminar
            if (cantProductos > 0){
                btnEliminar.setEnabled(false);
                btnEliminar.setToolTipText("No se puede eliminar: la subcategoría tiene productos asociados.");
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error consultando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // Lógica/BD: confirma y elimina (si no hay productos asociados)
    private void onEliminar(){
        if (cantProductos > 0){
            JOptionPane.showMessageDialog(this, "No se puede eliminar: la subcategoría tiene productos asociados.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar definitivamente la subcategoría?\nEsta acción no se puede deshacer.", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM subcategoria WHERE id_subcategoria=?")){
            ps.setInt(1, idSub);
            int n = ps.executeUpdate();
            if (n>0){
                eliminado = true;
                JOptionPane.showMessageDialog(this, "Subcategoría eliminada.");
            } else {
                JOptionPane.showMessageDialog(this, "No se eliminó (¿ID inexistente?)");
            }
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al eliminar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lógica: permite al panel saber si se eliminó
    public boolean fueEliminado(){ return eliminado; }

    // Lógica/BD: helper de conexión local
    static class DB {
        static Connection get() throws Exception {
            String url  = "jdbc:mysql://127.0.0.1:3306/libreria?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Argentina/Buenos_Aires";
            String user = "root"; String pass = "";
            return DriverManager.getConnection(url, user, pass);
        }
    }

    // Lógica: item simple para combos (si lo necesitás luego)
    static class Item {
        final int id; final String label;
        Item(int id, String label){ this.id=id; this.label=label; }
        @Override public String toString(){ return label; }
    }
}
