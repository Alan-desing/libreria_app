package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class toggle {

    // lógica: alterna el estado activo/inactivo de un producto
    public static void ejecutar(Component parent, int idProducto){
        try (Connection cn = conexion_bd.getConnection()){

            // BD: obtener estado actual del producto
            int actual;
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT activo FROM producto WHERE id_producto=?")) {
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(parent,
                                "Producto inexistente",
                                "BD", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    actual = rs.getInt(1);
                }
            }

            // lógica: invertir el estado (1→0 o 0→1)
            int nuevo = (actual == 1) ? 0 : 1;

            // BD: actualizar el campo activo
            try (PreparedStatement up = cn.prepareStatement(
                    "UPDATE producto SET activo=?, actualizado_en=NOW() WHERE id_producto=?")) {
                up.setInt(1, nuevo);
                up.setInt(2, idProducto);
                up.executeUpdate();
            }

            // UX: notificación según el nuevo estado
            JOptionPane.showMessageDialog(parent,
                    nuevo == 1 ? "Producto activado." : "Producto desactivado.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Error:\n" + ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }
}
