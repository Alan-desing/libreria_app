package admin.usuarios;

import includes.estilos;
import includes.conexion_bd;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class eliminar extends JDialog {

    // Lógica: ID del usuario a eliminar
    private final int idUsuario;

    // Lógica: bandera para informar al panel si se eliminó
    private boolean eliminado = false;

    // Visual + Lógica: constructor (arma UI, trae datos y valida restricciones)
    public eliminar(Window owner, int idUsuario) {
        super(owner, "Eliminar usuario", ModalityType.APPLICATION_MODAL);
        this.idUsuario = idUsuario;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(560, 400);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(estilos.COLOR_FONDO);
        setLayout(new GridBagLayout());

        // Visual: card blanca con borde suave
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(estilos.COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(18,18,18,18)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        // Visual: título del cuadro
        JLabel title = new JLabel("Confirmar eliminación");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        gc.gridx=0; gc.gridy=0;
        card.add(title, gc);

        // Visual: área informativa (datos/validaciones)
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setOpaque(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font("Arial", Font.PLAIN, 14));

        // Lógica/BD: datos y validaciones previas a la eliminación
        String nombre = "—", email = "—", rolNombre = "—";
        int idRol = 0;
        int admins = 0, ventas = 0, audits = 0;
        final int ADMIN_ROLE_ID = 1;

        try (Connection cn = DB.get()){
            // Lógica/BD: obtener datos del usuario
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT u.id_usuario, u.nombre, u.email, u.id_rol, r.nombre_rol " +
                    "FROM usuario u LEFT JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=?")){
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()){
                    if (!rs.next()){
                        JOptionPane.showMessageDialog(this, "Usuario no encontrado");
                        dispose();
                        return;
                    }
                    nombre = rs.getString("nombre");
                    email  = rs.getString("email");
                    idRol  = rs.getInt("id_rol");
                    rolNombre = rs.getString("nombre_rol");
                }
            }
            // Lógica/BD: contar administradores actuales
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COUNT(*) c FROM usuario WHERE id_rol=?")){
                ps.setInt(1, ADMIN_ROLE_ID);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) admins = rs.getInt(1);
                }
            }
            // Lógica/BD: contar ventas asociadas
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COUNT(*) c FROM venta WHERE id_usuario=?")){
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) ventas = rs.getInt(1);
                }
            }
            // Lógica/BD: contar auditoría asociada (si existe)
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COUNT(*) c FROM auditoria WHERE id_usuario=?")){
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.next()) audits = rs.getInt(1);
                }
            } catch (SQLException exAud) {
                // Lógica: si no existe la tabla de auditoría, no bloqueamos por esto
                audits = 0;
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error verificando datos:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        // Visual: composición del mensaje (datos y bloqueos)
        StringBuilder sb = new StringBuilder();
        sb.append("Vas a eliminar al usuario: ").append(nombre).append(" (").append(email).append(").\n\n")
          .append("Rol: ").append(rolNombre==null?"—":rolNombre).append("\n")
          .append("Administradores activos: ").append(admins).append("\n")
          .append("Ventas asociadas: ").append(ventas).append("\n")
          .append("Registros de auditoría: ").append(audits).append("\n\n");

        boolean bloqueado = false;
        if (idRol==ADMIN_ROLE_ID && admins<=1){
            sb.append("⚠ No se puede eliminar: es el único administrador.\n");
            bloqueado = true;
        }
        if (ventas>0){
            sb.append("⚠ No se puede eliminar: el usuario posee ventas registradas. Sugerencia: pasarlo a estado INACTIVO.\n");
            bloqueado = true;
        }
        if (audits>0){
            sb.append("⚠ No se puede eliminar: el usuario posee registros de auditoría. Sugerencia: pasarlo a estado INACTIVO.\n");
            bloqueado = true;
        }

        info.setText(sb.toString());
        gc.gridy=1; gc.weighty=1;
        card.add(info, gc);

        // Visual: acciones inferiores (volver + eliminar si procede)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnVolver   = estilos.botonSm("Volver");
        JButton btnEliminar = estilos.botonSmDanger("Eliminar");
        actions.add(btnVolver);
        if (!bloqueado) actions.add(btnEliminar);

        gc.gridy=2; gc.weighty=0;
        card.add(actions, gc);

        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(8,8,8,8);
        add(card, root);

        // Lógica: eventos
        btnVolver.addActionListener(e -> dispose());
        btnEliminar.addActionListener(e -> onEliminar());
    }

    // Lógica: permite al panel saber si se eliminó
    public boolean fueEliminado(){ return eliminado; }

    // Lógica/BD: confirma y elimina el usuario (si no está bloqueado)
    private void onEliminar(){
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar definitivamente?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r!=JOptionPane.YES_OPTION) return;

        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM usuario WHERE id_usuario=?")){
            ps.setInt(1, idUsuario);
            int n = ps.executeUpdate();
            if (n>0){
                eliminado = true;
                JOptionPane.showMessageDialog(this, "Usuario eliminado.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "No se eliminó (¿ID inexistente?)");
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo eliminar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
