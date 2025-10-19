package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class eliminar {

    // lógica: método principal para ejecutar la eliminación de un producto
    public static void ejecutar(Component parent, int idProducto, Runnable onSuccess){
        // UX: confirmación antes de eliminar definitivamente
        int r = JOptionPane.showConfirmDialog(parent,
                "¿Eliminar producto #"+idProducto+"?",
                "Eliminar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if(r != JOptionPane.YES_OPTION) return;

        // BD: eliminación física del registro
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "DELETE FROM producto WHERE id_producto=?")) {
            ps.setInt(1, idProducto);
            int n = ps.executeUpdate();

            // lógica: mostrar resultado según el número de filas afectadas
            if (n > 0) {
                JOptionPane.showMessageDialog(parent, "Producto eliminado");
                if (onSuccess != null) onSuccess.run(); // callback tras éxito
            } else {
                JOptionPane.showMessageDialog(parent, "No se eliminó (ID inexistente)");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
        }
    }
}
